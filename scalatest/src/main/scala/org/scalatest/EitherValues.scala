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
package org.scalatest

import org.scalactic._
import org.scalatest.exceptions.StackDepthException
import org.scalatest.exceptions.TestFailedException

/**
 * Trait that provides an implicit conversion that adds `left.value` and `right.value` methods
 * to `Either`, which will return the selected value of the `Either` if defined,
 * or throw `TestFailedException` if not.
 *
 * This construct allows you to express in one statement that an `Either` should be ''left'' or ''right''
 * and that its value should meet some expectation. Here's are some examples:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * either1.right.value should be &gt; 9
 * either2.left.value should be ("Muchas problemas")
 * }}}
 *
 * Or, using assertions instead of matcher expressions:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assert(either1.right.value &gt; 9)
 * assert(either2.left.value === "Muchas problemas")
 * }}}
 *
 * Were you to simply invoke `right.get` or `left.get` on the `Either`, 
 * if the `Either` wasn't defined as expected (''e.g.'', it was a `Left` when you expected a `Right`), it
 * would throw a `NoSuchElementException`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val either: Either[String, Int] = Left("Muchas problemas")
 *
 * either.right.get should be &gt; 9 // either.right.get throws NoSuchElementException
 * }}}
 *
 * The `NoSuchElementException` would cause the test to fail, but without providing a <a href="exceptions/StackDepth.html">stack depth</a> pointing
 * to the failing line of test code. This stack depth, provided by <a href="exceptions/TestFailedException.html">`TestFailedException`</a> (and a
 * few other ScalaTest exceptions), makes it quicker for
 * users to navigate to the cause of the failure. Without `EitherValues`, to get
 * a stack depth exception you would need to make two statements, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val either: Either[String, Int] = Left("Muchas problemas")
 *
 * either should be ('right) // throws TestFailedException
 * either.right.get should be &gt; 9
 * }}}
 *
 * The `EitherValues` trait allows you to state that more concisely:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val either: Either[String, Int] = Left("Muchas problemas")
 *
 * either.right.value should be &gt; 9 // either.right.value throws TestFailedException
 * }}}
 */
trait EitherValues {

  import scala.language.implicitConversions

  /**
   * Implicit conversion that adds a `value` method to `LeftProjection`.
   *
   * @param either the `LeftProjection` on which to add the `value` method
   */
  implicit def convertLeftProjectionToValuable[L, R](leftProj: Either.LeftProjection[L, R])(implicit pos: source.Position): LeftValuable[L, R] = new LeftValuable(leftProj, pos)

  /**
   * Implicit conversion that adds a `value` method to `RightProjection`.
   *
   * @param either the `RightProjection` on which to add the `value` method
   */
  implicit def convertRightProjectionToValuable[L, R](rightProj: Either.RightProjection[L, R])(implicit pos: source.Position): RightValuable[L, R] = new RightValuable(rightProj, pos)

  /**
   * Wrapper class that adds a `value` method to `LeftProjection`, allowing
   * you to make statements like:
   *
   * {{{  <!-- class="stHighlight" -->
   * either.left.value should be &gt; 9
   * }}}
   *
   * @param leftProj A `LeftProjection` to convert to `LeftValuable`, which provides the
   *   `value` method.
   */
  class LeftValuable[L, R](leftProj: Either.LeftProjection[L, R], pos: source.Position) {

    /**
     * Returns the `Left` value contained in the wrapped `LeftProjection`, if defined as a `Left`, else throws `TestFailedException` with
     * a detail message indicating the `Either` was defined as a `Right`, not a `Left`.
     */
    def value: L = {
      try {
        leftProj.get
      }
      catch {
        case cause: NoSuchElementException => 
          throw new TestFailedException((_: StackDepthException) => Some(Resources.eitherLeftValueNotDefined), Some(cause), pos)
      }
    }
  }

  /**
   * Wrapper class that adds a `value` method to `RightProjection`, allowing
   * you to make statements like:
   *
   * {{{  <!-- class="stHighlight" -->
   * either.right.value should be &gt; 9
   * }}}
   *
   * @param rightProj A `RightProjection` to convert to `RightValuable`, which provides the
   *   `value` method.
   */
  class RightValuable[L, R](rightProj: Either.RightProjection[L, R], pos: source.Position) {

    /**
     * Returns the `Right` value contained in the wrapped `RightProjection`, if defined as a `Right`, else throws `TestFailedException` with
     * a detail message indicating the `Either` was defined as a `Right`, not a `Left`.
     */
    def value: R = {
      try {
        rightProj.get
      }
      catch {
        case cause: NoSuchElementException => 
          throw new TestFailedException((_: StackDepthException) => Some(Resources.eitherRightValueNotDefined), Some(cause), pos)
      }
    }
  }
}

/**
 * Companion object that facilitates the importing of `ValueEither` members as 
 * an alternative to mixing it in. One use case is to import `EitherValues`'s members so you can use
 * `left.value` and `right.value` on `Either` in the Scala interpreter:
 *
 * {{{  <!-- class="stREPL" -->
 * $ scala -cp scalatest-1.7.jar
 * Welcome to Scala version 2.9.1.final (Java HotSpot(TM) 64-Bit Server VM, Java 1.6.0_29).
 * Type in expressions to have them evaluated.
 * Type :help for more information.
 * 
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 * 
 * scala&gt; import matchers.Matchers._
 * import matchers.Matchers._
 * 
 * scala&gt; import EitherValues._
 * import EitherValues._
 * 
 * scala&gt; val e: Either[String, Int] = Left("Muchas problemas")
 * e: Either[String,Int] = Left(Muchas problemas)
 * 
 * scala&gt; e.left.value should be ("Muchas problemas")
 * 
 * scala&gt; e.right.value should be &lt; 9
 * org.scalatest.TestFailedException: The Either on which rightValue was invoked was not defined.
 *   at org.scalatest.EitherValues$RightValuable.value(EitherValues.scala:148)
 *   at .&lt;init&gt;(&lt;console&gt;:18)
 *   ...
 * }}}
 */
object EitherValues extends EitherValues
