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

import org.scalactic.Requirements._
import org.scalactic.exceptions.NullArgumentException
import org.scalactic.source
import StackDepthExceptionHelper.posOrElseStackDepthFun

/**
 * Exception that indicates an action that is only allowed during a suite's test registration phase,
 * such as registering a test to run or ignore, was attempted after registration had already closed.
 *
 * In suites that register tests as functions, such as <a href="../FunSuite.html">`FunSuite`</a> and <a href="../FunSpec.html">`FunSpec`</a>, tests
 * are normally registered during construction. Although it is not the usual approach, tests can also
 * be registered after construction by invoking methods that register tests on the already constructed suite so
 * long as `run` has not been invoked on that suite.
 * As soon as `run` is invoked for the first time, registration of tests is "closed," meaning
 * that any further attempts to register a test will fail (and result in an instance of this exception class being thrown). This
 * can happen, for example, if an attempt is made to nest tests, such as in a `FunSuite`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * test("this test is fine") {
 *   test("but this nested test is not allowed") {
 *   }
 * }
 * }}}
 *
 * This exception encapsulates information about the stack depth at which the line of code that made this attempt resides,
 * so that information can be presented to the user that makes it quick to find the problem line of code. (In other words,
 * the user need not scan through the stack trace to find the correct filename and line number of the offending code.)
 * 
 *
 * @param message the exception's detail message
 * @param posOrStackDepthFun either a source position or a function that return the depth in the stack trace of this exception at which the line of code that attempted
 *   to register the test after registration had been closed.
 *
 * @throws NullArgumentException if either `message` or `failedCodeStackDepthFun` is `null`
 *
 * @author Bill Venners
 */
class TestRegistrationClosedException(
  message: String,
  posOrStackDepthFun: Either[source.Position, StackDepthException => Int]
) extends StackDepthException((_: StackDepthException) => Some(message), None, posOrStackDepthFun) {

  requireNonNull(message, posOrStackDepthFun)

  /**
    * Constructs a `TestRegistrationClosedException` with a `message` and a source position.
    *
    * @param message the exception's detail message
    * @param pos the source position.
    *
    * @throws NullArgumentException if `message` or `pos` is `null`
    */
  def this(
    message: String,
    pos: source.Position
  ) = this(message, Left(pos))

  /**
   * Constructs a `TestRegistrationClosedException` with a `message` and a pre-determined 
   * and `failedCodeStackDepth`. (This was the primary constructor form prior to ScalaTest 1.5.)
   *
   * @param message the exception's detail message
   * @param failedCodeStackDepth the depth in the stack trace of this exception at which the line of test code that failed resides.
   *
   * @throws NullArgumentException if `message` is `null`
   */
  def this(message: String, failedCodeStackDepth: Int) =
    this(message, Right((e: StackDepthException) => failedCodeStackDepth))

  /**
    * Constructs a `TestRegistrationClosedException` with a `message` and a pre-determined
    * and `failedCodeStackDepthFun`.
    *
    * @param message the exception's detail message
    * @param failedCodeStackDepthFun a function that return the depth in the stack trace of this exception at which the line of code that attempted.
    *
    * @throws NullArgumentException if `message` or `failedCodeStackDepthFun` is `null`
    */
  def this(message: String, failedCodeStackDepthFun: StackDepthException => Int) =
    this(message, Right(failedCodeStackDepthFun))

  /**
   * Returns an exception of class `TestRegistrationClosedException` with `failedExceptionStackDepth` set to 0 and 
   * all frames above this stack depth severed off. This can be useful when working with tools (such as IDEs) that do not
   * directly support ScalaTest. (Tools that directly support ScalaTest can use the stack depth information delivered
   * in the StackDepth exceptions.)
   */
  def severedAtStackDepth: TestRegistrationClosedException = {
    val truncated = getStackTrace.drop(failedCodeStackDepth)
    val e = new TestRegistrationClosedException(message, posOrStackDepthFun)
    e.setStackTrace(truncated)
    e
  }

  /**
   * Indicates whether this object can be equal to the passed object.
   */
  override def canEqual(other: Any): Boolean = other.isInstanceOf[TestRegistrationClosedException]

  /**
   * Indicates whether this object is equal to the passed object. If the passed object is
   * a `TestRegistrationClosedException`, equality requires equal `message`,
   * `cause`, and `failedCodeStackDepth` fields, as well as equal
   * return values of `getStackTrace`.
   */
  override def equals(other: Any): Boolean =
    other match {
      case that: TestRegistrationClosedException => super.equals(that)
      case _ => false
    }

  /**
   * Returns a hash code value for this object.
   */
  // Don't need to change it. Implementing it only so as to not freak out people who know
  // that if you override equals you must override hashCode.
  override def hashCode: Int = super.hashCode
}

// I pass in a message here so different situations can be described better in the
// error message, such as an it inside an it, an ignore inside an it, a describe inside an it, etc.
