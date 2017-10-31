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

import Aggregating.tryEquality
import org.scalactic.Equality
import org.scalatest.FailureMessages
import scala.annotation.tailrec
import scala.collection.GenTraversable

/**
 * Supertrait for typeclasses that enable `be empty` matcher syntax.
 *
 * An `Emptiness[T]` provides access to the "emptiness" of type `T` in such
 * a way that `be empty` matcher syntax can be used with type `T`. A `T`
 * can be any type that in some way can be empty. ScalaTest provides implicit implementations for several types. 
 * You can enable the `be empty` matcher syntax on your own type `U` by defining an `Emptiness[U]`
 * for the type and making it available implicitly.
 * 
 * ScalaTest provides implicit `Emptiness` instances for `scala.collection.GenTraversable`,
 * `java.util.Collection`, `java.util.Map`, `String`, `Array`, 
 * and `scala.Option` in the `Emptiness` companion object.
 * 
 */
trait Emptiness[-T] {

  /**
   * Determines whether the passed thing is readable, ''i.e.'', the passed file is readable.
   *
   * @param thing the thing to check for emptiness
   * @return `true` if passed thing is empty, `false` otherwise
   */
  def isEmpty(thing: T): Boolean
}

/**
 * Companion object for `Emptiness` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`scala.collection.GenTraversable`</li>
 * <li>`String`</li>
 * <li>`Array`</li>
 * <li>`scala.Option`</li>
 * <li>`java.util.Collection`</li>
 * <li>`java.util.Map`</li>
 * <li>arbitary object with a `isEmpty()` method that returns `Boolean`</li>
 * <li>arbitary object with a parameterless `isEmpty` method that returns `Boolean`</li>
 * </ul>
 */
object Emptiness {

  import scala.language.higherKinds

  /**
   * Enable `Emptiness` implementation for `scala.collection.GenTraversable`
   *
   * @tparam E the type of the element in the `scala.collection.GenTraversable`
   * @tparam TRAV any subtype of `scala.collection.GenTraversable`
   * @return `Emptiness[TRAV[E]]` that supports `scala.collection.GenTraversable` in `be empty` syntax
   */
  implicit def emptinessOfGenTraversable[E, TRAV[e] <: scala.collection.GenTraversable[e]]: Emptiness[TRAV[E]] =
    new Emptiness[TRAV[E]] {
      def isEmpty(trav: TRAV[E]): Boolean = trav.isEmpty
    }
  
  /**
   * Enable `Emptiness` implementation for `Array`
   *
   * @tparam E the type of the element in the `Array`
   * @return `Emptiness[Array[E]]` that supports `Array` in `be empty` syntax
   */
  implicit def emptinessOfArray[E]: Emptiness[Array[E]] =
    new Emptiness[Array[E]] {
      def isEmpty(arr: Array[E]): Boolean = arr.length == 0
    }
  
  /**
   * Enable `Emptiness` implementation for `String`
   *
   * @return `Emptiness[String]` that supports `String` in `be empty` syntax
   */
  implicit def emptinessOfString: Emptiness[String] =
    new Emptiness[String] {
      def isEmpty(str: String): Boolean = str.isEmpty
    }
  
  /**
   * Enable `Emptiness` implementation for `scala.Option`
   *
   * @tparam E the type of the element in the `scala.Option`
   * @tparam OPT any subtype of `scala.Option`
   * @return `Emptiness[OPT[E]]` that supports `scala.Option` in `be empty` syntax
   */
  implicit def emptinessOfOption[E, OPT[e] <: Option[e]]: Emptiness[OPT[E]] =
    new Emptiness[OPT[E]] {
      def isEmpty(opt: OPT[E]): Boolean = opt.isEmpty
    }
  
  /**
   * Enable `Emptiness` implementation for `java.util.Collection`
   *
   * @tparam E the type of the element in the `java.util.Collection`
   * @tparam JCOL any subtype of `java.util.Collection`
   * @return `Emptiness[JCOL[E]]` that supports `java.util.Collection` in `be empty` syntax
   */
  implicit def emptinessOfJavaCollection[E, JCOL[e] <: java.util.Collection[e]]: Emptiness[JCOL[E]] =
    new Emptiness[JCOL[E]] {
      def isEmpty(jcol: JCOL[E]): Boolean = jcol.isEmpty
    }

  /**
   * Enable `Emptiness` implementation for `java.util.Map`
   *
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `Emptiness[JMAP[K, V]]` that supports `java.util.Map` in `be empty` syntax
   */
  implicit def emptinessOfJavaMap[K, V, JMAP[k, v] <: java.util.Map[k, v]]: Emptiness[JMAP[K, V]] =
    new Emptiness[JMAP[K, V]] {
      def isEmpty(jmap: JMAP[K, V]): Boolean = jmap.isEmpty
    }

  import scala.language.reflectiveCalls
  
  /**
   * Enable `Emptiness` implementation for any arbitrary object with a `isEmpty()` method that returns `Boolean`
   *
   * @tparam T any type that has a `isEmpty()` method that returns `Boolean`
   * @return `Emptiness[T]` that supports `T` in `be empty` syntax
   */
  implicit def emptinessOfAnyRefWithIsEmptyMethod[T <: AnyRef { def isEmpty(): Boolean}]: Emptiness[T] = 
    new Emptiness[T] {
      def isEmpty(obj: T): Boolean = obj.isEmpty
    }
  
  /**
   * Enable `Emptiness` implementation for any arbitrary object with a `isEmpty` method that returns `Boolean`
   *
   * @tparam T any type that has a parameterless `isEmpty` method that returns `Boolean`
   * @return `Emptiness[T]` that supports `T` in `be empty` syntax
   */
  implicit def emptinessOfAnyRefWithParameterlessIsEmptyMethod[T <: AnyRef { def isEmpty: Boolean}]: Emptiness[T] = 
    new Emptiness[T] {
      def isEmpty(obj: T): Boolean = obj.isEmpty
    }
}

