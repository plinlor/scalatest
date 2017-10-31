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
 * Supertrait for typeclasses that enable the `exist` matcher syntax.
 *
 * An `Existence[S]` provides access to the "existence nature" of type `S` in such
 * a way that `exist` matcher syntax can be used with type `S`. A `S`
 * can be any type for which the concept of existence makes sense, such as `java.io.File`. ScalaTest provides
 * implicit implementations for `java.io.File`. You can enable the `exist` matcher syntax on your own
 * type `U` by defining a `Existence[U]` for the type and making it available implicitly.
 * 
 * ScalaTest provides an implicit `Existence` instance for `java.io.File`
 * in the `Existence` companion object.
 * 
 */
trait Existence[-S] {

  /**
   * Determines whether the passed thing exists, ''i.e.'', whether the passed `java.io.File` exists.
   *
   * @param thing the thing to check for existence
   * @return `true` if passed thing exists, `false` otherwise
   */
  def exists(thing: S): Boolean
}

/**
 * Companion object for `Existence` that provides implicit implementations for `java.io.File`.
 */
object Existence {

  /**
   * Enable `Existence` implementation for `java.io.File`
   *
   * @tparam FILE any subtype of `java.io.File`
   * @return `Existence[FILE]` that supports `java.io.File` in `exist` syntax
   */
  implicit def existenceOfFile[FILE <: java.io.File]: Existence[FILE] =
    new Existence[FILE] {
      def exists(file: FILE): Boolean = file.exists
    }
  
}
