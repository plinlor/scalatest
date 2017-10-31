/*
 * Copyright 2001-2013 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest.enablers

import scala.collection.JavaConverters._
import org.scalactic.Equality
import org.scalatest.FailureMessages
import scala.annotation.tailrec
import scala.collection.GenTraversable

/**
 * Supertrait for typeclasses that enable `contain value` matcher syntax.
 *
 * A `ValueMapping[M]` provides access to the "value mapping nature" of type `M` in such
 * a way that `contain` `value` matcher syntax can be used with type `M`. An `M`
 * can be any type for which `contain` `value` syntax makes sense. ScalaTest provides implicit implementations
 * for `scala.collection.GenMap` and `java.util.Map`. You can enable the `contain` `value`
 * matcher syntax on your own type `U` by defining a `ValueMapping[U]` for the type and making it
 * available implicitly.
 *
 * ScalaTest provides implicit `ValueMapping` instances for `scala.collection.GenMap`,
 * and `java.util.Map` in the <a href="ValueMapping$.html">`ValueMapping` companion object</a>.
 * 
 */
trait ValueMapping[-M] {

  /**
   * Implements `contain` `atLeastOneOf` syntax for aggregations of type `A`.
   *
   * @param aggregation an aggregation about which an assertion is being made
   * @param eles elements at least one of which should be contained in the passed aggregation
   * @return true if the passed aggregation contains at least one of the passed elements
   */

  /**
   * Check if the passed `map` contains the passed `value`.
   *
   * @param map a map about which an assertion is being made
   * @param value value of which should be contained in the passed map
   * @return true if the passed map contains the passed value
   */
  def containsValue(map: M, value: Any): Boolean
}

/**
 * Companion object for `ValueMapping` that provides implicit implementations for `scala.collection.GenMap` and `java.util.Map`.
 */
object ValueMapping {

  import scala.language.higherKinds

  /**
   * Enable `ValueMapping` implementation for `scala.collection.GenMap`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of value in the `scala.collection.GenMap`
   * @tparam K the type of the key in the `scala.collection.GenMap`
   * @tparam V the type of the value in the `scala.collection.GenMap`
   * @tparam MAP any subtype of `scala.collection.GenMap`
   * @return `ValueMapping[MAP[K, V]]` that supports `scala.collection.GenMap` in `contain value` syntax
   */
  implicit def valueMappingNatureOfGenMap[K, V, MAP[k, v] <: scala.collection.GenMap[k, v]](implicit equality: Equality[V]): ValueMapping[MAP[K, V]] = 
    new ValueMapping[MAP[K, V]] {
      def containsValue(map: MAP[K, V], value: Any): Boolean = {
        // map.values.exists((v: V) => equality.areEqual(v, value)) go back to this once I'm off 2.9
        map.iterator.map(_._2).exists((v: V) => equality.areEqual(v, value))
      }
    }

  import scala.language.implicitConversions

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `V`
   * into `ValueMapping` of type `MAP[K, V]`, where `MAP` is a subtype of `scala.collection.GenMap`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (Map(1 -> "one") should contain value "ONE") (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `ValueMapping[Map[Int, String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `V`
   * @tparam K the type of the key in the `scala.collection.GenMap`
   * @tparam V the type of the value in the `scala.collection.GenMap`
   * @tparam MAP any subtype of `scala.collection.GenMap`
   * @return `ValueMapping` of type `MAP[K, V]`
   */
  implicit def convertEqualityToGenMapValueMapping[K, V, MAP[k, v] <: scala.collection.GenMap[k, v]](equality: Equality[V]): ValueMapping[MAP[K, V]] = 
    valueMappingNatureOfGenMap(equality)

  /**
   * Enable `ValueMapping` implementation for `java.util.Map`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of value in the `java.util.Map`
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `ValueMapping[JMAP[K, V]]` that supports `java.util.Map` in `contain` `value` syntax
   */
  implicit def valueMappingNatureOfJavaMap[K, V, JMAP[k, v] <: java.util.Map[k, v]](implicit equality: Equality[V]): ValueMapping[JMAP[K, V]] = 
    new ValueMapping[JMAP[K, V]] {
      def containsValue(jMap: JMAP[K, V], value: Any): Boolean = {
        jMap.asScala.values.exists((v: V) => equality.areEqual(v, value))
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `V`
   * into `ValueMapping` of type `JMAP[K, V]`, where `JMAP` is a subtype of `java.util.Map`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * val javaMap = new java.util.HashMap[Int, String]()
   * javaMap.put(1, "one")
   * (javaMap should contain value "ONE") (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `ValueMapping[java.util.HashMap[Int, String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `V`
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `ValueMapping` of type `JMAP[K, V]`
   */
  implicit def convertEqualityToJavaMapValueMapping[K, V, JMAP[k, v] <: java.util.Map[k, v]](equality: Equality[V]): ValueMapping[JMAP[K, V]] = 
    valueMappingNatureOfJavaMap(equality)
}
