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

import org.scalactic.Requirements._
import scala.collection.JavaConverters._
import org.scalactic.Equality
import org.scalatest.FailureMessages
import scala.annotation.tailrec
import scala.collection.GenTraversable

/**
 * Supertrait for typeclasses that enable `contain key` matcher syntax.
 *
 * A `KeyMapping[M]` provides access to the "key mapping nature" of type `M` in such
 * a way that `contain key` matcher syntax can be used with type `M`. A `M`
 * can be any type for which `contain key` syntax makes sense. ScalaTest provides implicit implementations
 * for `scala.collection.GenMap` and `java.util.Map`. You can enable the `contain key`
 * matcher syntax on your own type `U` by defining a `KeyMapping[U]` for the type and making it
 * available implicitly.
 * 
 * ScalaTest provides implicit `KeyMapping` instances for `scala.collection.GenMap`,
 * and `java.util.Map` in the `KeyMapping` companion object.
 * 
 */
trait KeyMapping[-M] {

  /**
   * Check if the passed `map` contains the passed `key`.
   *
   * @param map a map about which an assertion is being made
   * @param key key of which should be contained in the passed map
   * @return true if the passed map contains the passed key
   */
  def containsKey(map: M, key: Any): Boolean
}

/**
 * Companion object for `KeyMapping` that provides implicit implementations for `scala.collection.GenMap` and `java.util.Map`.
 */
object KeyMapping {

  import scala.language.higherKinds

  /**
   * Enable `KeyMapping` implementation for `scala.collection.GenMap`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of key in the `scala.collection.GenMap`
   * @tparam K the type of the key in the `scala.collection.GenMap`
   * @tparam V the type of the value in the `scala.collection.GenMap`
   * @tparam MAP any subtype of `scala.collection.GenMap`
   * @return `KeyMapping[MAP[K, V]]` that supports `scala.collection.GenMap` in `contain key` syntax
   */
  implicit def keyMappingNatureOfGenMap[K, V, MAP[k, v] <: scala.collection.GenMap[k, v]](implicit equality: Equality[K]): KeyMapping[MAP[K, V]] = 
    new KeyMapping[MAP[K, V]] {
      def containsKey(map: MAP[K, V], key: Any): Boolean = {
        requireNonNull(map)
        map.keySet.exists((k: K) => equality.areEqual(k, key))
      }
    }

  import scala.language.implicitConversions

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `K`
   * into `KeyMapping` of type `MAP[K, V]`, where `MAP` is a subtype of `scala.collection.GenMap`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (Map("one" -> 1) should contain key "ONE") (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `KeyMapping[Map[String, Int]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `K`
   * @tparam K the type of the key in the `scala.collection.GenMap`
   * @tparam V the type of the value in the `scala.collection.GenMap`
   * @tparam MAP any subtype of `scala.collection.GenMap`
   * @return `KeyMapping` of type `MAP[K, V]`
   */
  implicit def convertEqualityToGenMapKeyMapping[K, V, MAP[k, v] <: scala.collection.GenMap[k, v]](equality: Equality[K]): KeyMapping[MAP[K, V]] = 
    keyMappingNatureOfGenMap(equality)

  /**
   * Enable `KeyMapping` implementation for `java.util.Map`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of key in the `java.util.Map`
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `KeyMapping[JMAP[K, V]]` that supports `java.util.Map` in `contain` `key` syntax
   */
  implicit def keyMappingNatureOfJavaMap[K, V, JMAP[k, v] <: java.util.Map[k, v]](implicit equality: Equality[K]): KeyMapping[JMAP[K, V]] = 
    new KeyMapping[JMAP[K, V]] {
      def containsKey(jMap: JMAP[K, V], key: Any): Boolean = {
        jMap.asScala.keySet.exists((k: K) => equality.areEqual(k, key))
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `K`
   * into `KeyMapping` of type `JMAP[K, V]`, where `JMAP` is a subtype of `java.util.Map`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * val javaMap = new java.util.HashMap[String, Int]()
   * javaMap.put("one", 1)
   * (javaMap should contain key "ONE") (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `KeyMapping[java.util.HashMap[String, Int]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `K`
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `KeyMapping` of type `JMAP[K, V]`
   */
  implicit def convertEqualityToJavaMapKeyMapping[K, V, JMAP[k, v] <: java.util.Map[k, v]](equality: Equality[K]): KeyMapping[JMAP[K, V]] = 
    keyMappingNatureOfJavaMap(equality)
}
