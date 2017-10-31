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
 * Supertrait for `Messaging` typeclasses.
 *
 * Trait `Messaging` is a typeclass trait for objects that can be queried for message.
 * Objects of type T for which an implicit `Messaging[T]` is available can be used
 * with the `should have message` syntax.
 * You can enable the `have message` matcher syntax on your own
 * type `U` by defining a `Messaging[U]` for the type and making it available implicitly.
 * 
 *
 * ScalaTest provides an implicit `Messaging` instance for `java.lang.Throwable` and
 * arbitary object with `message()`, `message`, `getMessage()` or `getMessage`
 * method in the `Messaging` companion object.
 * 
 *
 * @author Bill Venners
 * @author Chee Seng
 */
trait Messaging[T] {

  /**
   * Returns the message of the passed object.
   *
   * @param obj object whose message to return
   * @return the message of the passed object
   */
  def messageOf(obj: T): String
}

/**
 * Companion object for `Messaging` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`java.lang.Throwable`</li>
 * <li>arbitary object with a `message()` method that returns `String`</li>
 * <li>arbitary object with a parameterless `message` method that returns `String`</li>
 * <li>arbitary object with a `getMessage()` method that returns `String`</li>
 * <li>arbitary object with a parameterless `getMessage` method that returns `String`</li>
 * </ul>
 */
object Messaging {

  /**
   * Enable `Messaging` implementation for `java.lang.Throwable`
   *
   * @tparam EX any subtype of `java.lang.Throwable`
   * @return `Messaging[EX]` that supports `java.lang.Throwable` in `have message` syntax
   */
  implicit def messagingNatureOfThrowable[EX <: Throwable]: Messaging[EX] = 
    new Messaging[EX] {
      def messageOf(exception: EX): String = exception.getMessage
    }

  import scala.language.reflectiveCalls

  /**
   * Provides `Messaging` implementation for any arbitrary object with a `message()` method that returns `String`
   *
   * @tparam T any type that has a `message()` method that returns `String`
   * @return `Messaging[T]` that supports `T` in `have message` syntax
   */
  implicit def messagingNatureOfAnyRefWithMessageMethod[T <: AnyRef { def message(): String}]: Messaging[T] = 
    new Messaging[T] {
      def messageOf(obj: T): String = obj.message
    }

  /**
   * Provides `Messaging` implementation for any arbitrary object with a parameterless `message` method that returns `String`
   *
   * @tparam T any type that has a parameterless `message` method that returns `String`
   * @return `Messaging[T]` that supports `T` in `have message` syntax
   */
  implicit def messagingNatureOfAnyRefWithParameterlessMessageMethod[T <: AnyRef { def message: String}]: Messaging[T] = 
    new Messaging[T] {
      def messageOf(obj: T): String = obj.message
    }

  /**
   * Provides `Messaging` implementation for any arbitrary object with a `getMessage()` method that returns `String`
   *
   * @tparam T any type that has a `getMessage()` method that returns `String`
   * @return `Messaging[T]` that supports `T` in `have message` syntax
   */
  implicit def messagingNatureOfAnyRefWithGetMessageMethod[T <: AnyRef { def getMessage(): String}]: Messaging[T] = 
    new Messaging[T] {
      def messageOf(obj: T): String = obj.getMessage
    }

  /**
   * Provides `Messaging` implementation for any arbitrary object with a parameterless `getMessage` method that returns `String`
   *
   * @tparam T any type that has a parameterless `getMessage` method that returns `String`
   * @return `Messaging[T]` that supports `T` in `have message` syntax
   */
  implicit def messagingNatureOfAnyRefWithParameterlessGetMessageMethod[T <: AnyRef { def getMessage: String}]: Messaging[T] = 
    new Messaging[T] {
      def messageOf(obj: T): String = obj.getMessage
    }
}


