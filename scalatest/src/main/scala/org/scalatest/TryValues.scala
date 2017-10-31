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
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import java.util.NoSuchElementException
import org.scalatest.exceptions.StackDepthException
import org.scalatest.exceptions.TestFailedException

/**
 * Trait that provides an implicit conversion that adds `success` and `failure` methods
 * to `scala.util.Try`, enabling you to make assertions about the value of a `Success` or
 * the exception of a `Failure`.
 *
 * The `success` method will return the `Try` on which it is invoked as a `Success` if the `Try`
 * actually is a `Success`, or throw `TestFailedException` if not.
 * The `failure` method will return the `Try` on which it is invoked as a `Failure` if the `Try`
 * actually is a `Failure`, or throw `TestFailedException` if not.
 * 
 *
 * This construct allows you to express in one statement that an `Try` should be either a `Success`
 * or a `Failure` and that its value or exception, respectively,should meet some expectation. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * try1.success.value should be &gt; 9
 * try2.failure.exception should have message "/ by zero"
 * }}}
 *
 * Or, using assertions instead of a matchers:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assert(try1.success.value &gt; 9)
 * assert(try2.failure.exception.getMessage == "/ by zero")
 * }}}
 *
 * Were you to simply invoke `get` on the `Try`, 
 * if the `Try` wasn't a `Success`, it would throw the exception contained in the `Failure`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val try2 = Try { 1 / 0 }
 *
 * try2.get should be &lt; 9 // try2.get throws ArithmeticException
 * }}}
 *
 * The `ArithmeticException` would cause the test to fail, but without providing a <a href="exceptions/StackDepth.html">stack depth</a> pointing
 * to the failing line of test code. This stack depth, provided by <a href="exceptions/TestFailedException.html">`TestFailedException`</a> (and a
 * few other ScalaTest exceptions), makes it quicker for
 * users to navigate to the cause of the failure. Without <a href="TryValues.html">`TryValues`</a>, to get
 * a stack depth exception you would need to make two statements, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * try2 should be a 'success // throws TestFailedException
 * try2.get should be &lt; 9
 * }}}
 *
 * The `TryValues` trait allows you to state that more concisely:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * try2.success.value should be &lt; 9 // throws TestFailedException
 * }}}
 *
 */
trait TryValues {

  import scala.language.implicitConversions

  /**
   * Implicit conversion that adds `success` and `failure` methods to `Try`.
   *
   * @param theTry the `Try` to which to add the `success` and `failure` methods
   */
  implicit def convertTryToSuccessOrFailure[T](theTry: Try[T])(implicit pos: source.Position): SuccessOrFailure[T] = new SuccessOrFailure(theTry, pos)

  /**
   * Wrapper class that adds `success` and `failure` methods to `scala.util.Try`, allowing
   * you to make statements like:
   *
   * {{{  <!-- class="stHighlight" -->
   * try1.success.value should be &gt; 9
   * try2.failure.exception should have message "/ by zero"
   * }}}
   *
   * @param theTry An `Try` to convert to `SuccessOrFailure`, which provides the `success` and `failure` methods.
   */
  class SuccessOrFailure[T](theTry: Try[T], pos: source.Position) {

    /**
     * Returns the `Try` passed to the constructor as a `Failure`, if it is a `Failure`, else throws `TestFailedException` with
     * a detail message indicating the `Try` was not a `Failure`.
     */
    def failure: Failure[T] = {
      theTry match {
        case failure: Failure[T] => failure
        case _ => 
          throw new TestFailedException((_: StackDepthException) => Some(Resources.tryNotAFailure), None, pos)
      }
    }

    /**
     * Returns the `Try` passed to the constructor as a `Success`, if it is a `Success`, else throws `TestFailedException` with
     * a detail message indicating the `Try` was not a `Success`.
     */
    def success: Success[T] = {
      theTry match {
        case success: Success[T] => success
        case _ => 
          throw new TestFailedException((_: StackDepthException) => Some(Resources.tryNotASuccess), None, pos)
      }
    }
  }
}

/**
 * Companion object that facilitates the importing of `TryValues` members as 
 * an alternative to mixing it in. One use case is to import `TryValues`'s members so you can use
 * `success` and `failure` on `Try` in the Scala interpreter.
 * }}}
 */
object TryValues extends TryValues
