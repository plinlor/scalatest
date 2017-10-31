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

/**
 * Supertrait for typeclasses that enable the `be defined` matcher syntax.
 *
 * A `Definition[T]` provides access to the "definition nature" of type `S` in such
 * a way that `be defined` matcher syntax can be used with type `T`. A `T`
 * can be any type for which the concept of being defined makes sense, such as `scala.Option`. ScalaTest provides
 * implicit implementation for `scala.Option`. You can enable the `be defined` matcher syntax on your own
 * type `U` by defining a `Definition[U]` for the type and making it available implicitly.
 * 
 * ScalaTest provides an implicit `Definition` instance for `scala.Option`,
 * arbitary object with `isDefined()` or `isDefined` in the `Definition` companion object.
 * 
 */
trait Definition[-T] {

  /**
   * Determines whether the passed is defined, ''i.e.'', the passed in `scala.Option` is defined.
   *
   * @param thing the thing to check for definition
   * @return `true` if passed thing is defined, `false` otherwise
   */
  def isDefined(thing: T): Boolean
}

/**
 * Companion object for `Definition` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`scala.Option`</li>
 * <li>arbitary object with a `isDefined()` method that returns `Boolean`</li>
 * <li>arbitary object with a parameterless `isDefined` method that returns `Boolean`</li>
 * </ul>
 */
object Definition {

  import scala.language.higherKinds

  /**
   * Provides `Definition` implementation for `scala.Option`
   *
   * @tparam E the type of the element in the `Option`
   * @tparam OPT any subtype of `Option`
   * @return `Definition[OPT[E]]` that supports `Option` in `be defined` syntax
   */
  implicit def definitionOfOption[E, OPT[e] <: scala.Option[e]]: Definition[OPT[E]] =
    new Definition[OPT[E]] {
      def isDefined(option: OPT[E]): Boolean = option.isDefined
    }

  import scala.language.reflectiveCalls
  
  /**
   * Provides `Definition` implementation for any arbitrary object with a `isDefined()` method that returns `Boolean`
   *
   * @tparam T any type that has a `isDefined()` method that returns `Boolean`
   * @return `Definition[T]` that supports `T` in `be defined` syntax
   */
  implicit def definitionOfAnyRefWithIsDefinedMethod[T <: AnyRef { def isDefined(): Boolean}]: Definition[T] = 
    new Definition[T] {
      def isDefined(obj: T): Boolean = obj.isDefined
    }

  /**
   * Provides `Definition` implementation for any arbitrary object with a `isDefined` method that returns `Boolean`
   *
   * @tparam T any type that has a parameterless `isDefined` method that returns `Boolean`
   * @return `Definition[T]` that supports `T` in `be defined` syntax
   */
  implicit def definitionOfAnyRefWithParameterlessIsDefinedMethod[T <: AnyRef { def isDefined: Boolean}]: Definition[T] = 
    new Definition[T] {
      def isDefined(obj: T): Boolean = obj.isDefined
    }
}

