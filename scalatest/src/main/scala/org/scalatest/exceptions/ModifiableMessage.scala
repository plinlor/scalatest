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
 * This trait facilitates the <code>withClue</code> construct provided by trait
 * <a href="../Assertions.html"><code>Assertions</code></a>. This construct enables extra information (or "clues") to
 * be included in the detail message of a thrown exception. Although both
 * <code>assert</code> and <code>expect</code> provide a way for a clue to be
 * included directly, <code>assertThrows</code>, <code>intercept</code>, and ScalaTest matcher expressions
 * do not. Here's an example of clues provided directly in <code>assert</code>:
 * 
 *
 * <pre class="stHighlight">
 * assert(1 + 1 === 3, "this is a clue")
 * </pre>
 *
 * and in <code>expect</code>:
 * 
 *
 * <pre class="stHighlight">
 * expect(3, "this is a clue") { 1 + 1 }
 * </pre>
 *
 * The exceptions thrown by the previous two statements will include the clue
 * string, <code>"this is a clue"</code>, in the exceptions detail message.
 * To get the same clue in the detail message of an exception thrown
 * by a failed <code>assertThrows</code> call requires using <code>withClue</code>:
 * 
 *
 * <pre class="stHighlight">
 * withClue("this is a clue") {
 *   assertThrows[IndexOutOfBoundsException] {
 *     "hi".charAt(-1)
 *   }
 * }
 * </pre>
 *
 * Similarly, to get a clue in the exception resulting from an exception arising out
 * of a ScalaTest matcher expression, you need to use <code>withClue</code>. Here's
 * an example:
 * 
 *
 * <pre class="stHighlight">
 * withClue("this is a clue") {
 *   1 + 1 should === (3)
 * }
 * </pre>
 *
 * Exception types that mix in this trait have a <code>modifyMessage</code> method, which
 * returns an exception identical to itself, except with the detail message option replaced with
 * the result of invoking the passed function, supplying the current detail message option
 * as the lone <code>String</code> parameter. 
 * 
 */
trait ModifiableMessage[T <: Throwable] { this: Throwable =>

  /**
   * Returns an instance of this exception's class, identical to this exception,
   * except with the detail message option replaced with
   * the result of invoking the passed function, <code>fun</code>, supplying the current detail message option
   * as the lone <code>Option[String]</code> parameter. 
   *
   * Implementations of this method may either mutate this exception or return
   * a new instance with the revised detail message.
   * 
   *
   * @param fun A function that returns the new detail message option given the old one.
   */
  def modifyMessage(fun: Option[String] => Option[String]): T
}

