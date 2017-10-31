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
 * Supertrait for typeclasses that enable the `be readable` matcher syntax.
 *
 * A `Readability[T]` provides access to the "readable nature" of type `T` in such
 * a way that `be readable` matcher syntax can be used with type `T`. A `T`
 * can be any type for which the concept of being readable makes sense, such as `java.io.File`.
 * You can enable the `be readable` matcher syntax on your own type `U` by defining a
 * `Readability[U]` for the type and making it available implicitly.
 * 
 * ScalaTest provides an implicit `Readability` instance for `java.io.File` and arbitary
 * object with `isReadable()` or `isReadable` in the `Readability` companion object.
 * 
 */
trait Readability[-T] {

  /**
   * Determines whether the passed thing is readable, ''i.e.'', the passed file is readable.
   *
   * @param thing the thing to check for readability
   * @return `true` if the passed thing is readable, `false` otherwise
   *
   */
  def isReadable(thing: T): Boolean
}

/**
 * Companion object for `Readability` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`java.io.File`</li>
 * <li>arbitary object with a `isReadable()` method that returns `Boolean`</li>
 * <li>arbitary object with a parameterless `isReadable` method that returns `Boolean`</li>
 * </ul>
 */
object Readability {

  /**
   * Enable `Readability` implementation for `java.io.File`.
   *
   * @tparam FILE any subtype of `java.io.File`
   * @return `Readability[FILE]` that supports `java.io.File` in `be readable` syntax
   */
  implicit def readabilityOfFile[FILE <: java.io.File]: Readability[FILE] =
    new Readability[FILE] {
      def isReadable(file: FILE): Boolean = file.canRead
    }

  import scala.language.reflectiveCalls

  /**
   * Enable `Readability` implementation for any arbitrary object with a `isReadable()` method that returns `Boolean`
   *
   * @tparam T any type that has a `isReadable()` method that returns `Boolean`
   * @return `Readability[T]` that supports `T` in `be readable` syntax
   */
  implicit def readabilityOfAnyRefWithIsReadableMethod[T <: AnyRef { def isReadable(): Boolean}]: Readability[T] = 
    new Readability[T] {
      def isReadable(obj: T): Boolean = obj.isReadable
    }

  /**
   * Enable `Readability` implementation for any arbitrary object with a parameterless `isReadable` method that returns `Boolean`
   *
   * @tparam T any type that has a parameterless `isReadable` method that returns `Boolean`
   * @return `Readability[T]` that supports `T` in `be readable` syntax
   */
  implicit def readabilityOfAnyRefWithParameterlessIsReadableMethod[T <: AnyRef { def isReadable: Boolean}]: Readability[T] = 
    new Readability[T] {
      def isReadable(obj: T): Boolean = obj.isReadable
    }
}

