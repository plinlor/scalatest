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
package org.scalatest.exceptions

/**
 * Trait implemented by exception types that can modify their detail message.
 *
 * This trait facilitates the `withClue` construct provided by trait
 * <a href="../Assertions.html">`Assertions`</a>. This construct enables extra information (or "clues") to
 * be included in the detail message of a thrown exception. Although both
 * `assert` and `expect` provide a way for a clue to be
 * included directly, `assertThrows`, `intercept`, and ScalaTest matcher expressions
 * do not. Here's an example of clues provided directly in `assert`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assert(1 + 1 === 3, "this is a clue")
 * }}}
 *
 * and in `expect`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * expect(3, "this is a clue") { 1 + 1 }
 * }}}
 *
 * The exceptions thrown by the previous two statements will include the clue
 * string, `"this is a clue"`, in the exceptions detail message.
 * To get the same clue in the detail message of an exception thrown
 * by a failed `assertThrows` call requires using `withClue`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * withClue("this is a clue") {
 *   assertThrows[IndexOutOfBoundsException] {
 *     "hi".charAt(-1)
 *   }
 * }
 * }}}
 *
 * Similarly, to get a clue in the exception resulting from an exception arising out
 * of a ScalaTest matcher expression, you need to use `withClue`. Here's
 * an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * withClue("this is a clue") {
 *   1 + 1 should === (3)
 * }
 * }}}
 *
 * Exception types that mix in this trait have a `modifyMessage` method, which
 * returns an exception identical to itself, except with the detail message option replaced with
 * the result of invoking the passed function, supplying the current detail message option
 * as the lone `String` parameter. 
 * 
 */
trait ModifiableMessage[T <: Throwable] { this: Throwable =>

  /**
   * Returns an instance of this exception's class, identical to this exception,
   * except with the detail message option replaced with
   * the result of invoking the passed function, `fun`, supplying the current detail message option
   * as the lone `Option[String]` parameter. 
   *
   * Implementations of this method may either mutate this exception or return
   * a new instance with the revised detail message.
   * 
   *
   * @param fun A function that returns the new detail message option given the old one.
   */
  def modifyMessage(fun: Option[String] => Option[String]): T
}

