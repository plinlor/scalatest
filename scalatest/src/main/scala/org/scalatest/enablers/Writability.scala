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
 * Supertrait for typeclasses that enable the `be` `writable` matcher syntax.
 *
 * A `Writability[T]` provides access to the "writable nature" of type `T` in such
 * a way that `be` `writable` matcher syntax can be used with type `T`. A `T`
 * can be any type for which the concept of being writable makes sense, such as `java.io.File`. ScalaTest provides
 * implicit implementation for `java.io.File`. You can enable the `be` `writable` matcher syntax on your own
 * type `U` by defining a `Writability[U]` for the type and making it available implicitly.
 * 
 * ScalaTest provides an implicit `Writability` instance for `java.io.File` and arbitary
 * object with `isWritable()` or `isWritable` in the <a href="Writability$.html">`Writability` companion object</a>.
 * 
 */
trait Writability[-T] {

  /**
   * Determines whether the passed thing is writable, ''i.e.'', the passed file is writable.
   *
   * @param thing the thing to check for writability
   * @return `true` if the passed thing is writable, `false` otherwise
   */
  def isWritable(thing: T): Boolean
}

/**
 * Companion object for `Writability` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`java.io.File`</li>
 * <li>arbitary object with a `isWritable()` method that returns `Boolean`</li>
 * <li>arbitary object with a parameterless `isWritable` method that returns `Boolean`</li>
 * </ul>
 */
object Writability {

  /**
   * Enable `Writability` implementation for `java.io.File`.
   *
   * @tparam FILE any subtype of `java.io.File`
   * @return `Writability[FILE]` that supports `java.io.File` in `be` `writable` syntax
   */
  implicit def writabilityOfFile[FILE <: java.io.File]: Writability[FILE] =
    new Writability[FILE] {
      def isWritable(file: FILE): Boolean = file.canWrite
    }

  import scala.language.reflectiveCalls

  /**
   * Enable `Writability` implementation for any arbitrary object with a `isWritable()` method that returns `Boolean`
   *
   * @tparam T any type that has a `isWritable()` method that returns `Boolean`
   * @return `Writability[T]` that supports `T` in `be` `writable` syntax
   */
  implicit def writabilityOfAnyRefWithIsWritableMethod[T <: AnyRef { def isWritable(): Boolean}]: Writability[T] = 
    new Writability[T] {
      def isWritable(obj: T): Boolean = obj.isWritable
    }

  /**
   * Enable `Writability` implementation for any arbitrary object with a parameterless `isWritable` method that returns `Boolean`
   *
   * @tparam T any type that has a parameterless `isWritable` method that returns `Boolean`
   * @return `Writability[T]` that supports `T` in `be` `writable` syntax
   */
  implicit def writabilityOfAnyRefWithParameterlessIsWritableMethod[T <: AnyRef { def isWritable: Boolean}]: Writability[T] = 
    new Writability[T] {
      def isWritable(obj: T): Boolean = obj.isWritable
    }
}

