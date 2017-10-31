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
package org.scalatest.junit

import _root_.junit.framework.AssertionFailedError
import org.scalatest.exceptions.{PayloadField, ModifiablePayload, StackDepth, ModifiableMessage}
import org.scalactic.Requirements._
import org.scalactic.exceptions.NullArgumentException
import org.scalactic.source
import org.scalatest.exceptions.StackDepthException
import org.scalatest.exceptions.StackDepthExceptionHelper.getStackDepth

/**
 * Exception that indicates a test failed.
 *
 * The purpose of this exception is to encapsulate the same stack depth information provided by
 * <a href="../exceptions/TestFailedException.html">`TestFailedException`</a>, which is used
 * when running with ScalaTest, but be reported as
 * a failure not an error when running with JUnit.
 * The stack depth information indicates which line of test code failed, so that when running
 * with ScalaTest information can be presented to
 * the user that makes it quick to find the failing line of test code. (In other words, when
 * running with ScalaTest the user need not scan through the stack trace to find the correct filename
 * and line number of the failing test.)
 * 
 *
 * JUnit distinguishes between ''failures'' and ''errors''.
 * If a test fails because of a failed assertion, that is considered a ''failure'' in JUnit. If a test
 * fails for any other reason, either the test code or the application being tested threw an unexpected
 * exception, that is considered an ''error'' in JUnit. This class differs from
 * <a href="../exceptions/TestFailedException.html">`TestFailedException`</a> in that it extends
 * `junit.framework.AssertionFailedError`. Instances of this class are thrown by the
 * assertions provided by <a href="AssertionsForJUnit.html">`AssertionsForJUnit`</a>.
 * 
 *
 * The way JUnit 3 (JUnit 3.8 and earlier releases) decided whether an exception represented a failure or error
 * is that only thrown `junit.framework.AssertionFailedError`s were considered failures. Any other
 * exception type was considered an error. The exception type thrown by the JUnit 3 assertion methods declared
 * in `junit.framework.Assert` (such as `assertEquals`, `assertTrue`,
 * and `fail`) was, therefore, `AssertionFailedError`. In JUnit 4, `AssertionFailedError`
 * was made to extend `java.lang.AssertionError`, and the distinction between failures and errors
 * was essentially dropped. However, some tools that integrate with JUnit carry on this distinction, so even
 * if you are using JUnit 4 you may want to use `AssertionsForJUnit`.
 * 
 *
 * @param message an optional detail message for this `TestFailedException`.
 * @param cause an optional cause, the `Throwable` that caused this `TestFailedException` to be thrown.
 * @param failedCodeStackDepth the depth in the stack trace of this exception at which the line of test code that failed resides.
 * @param payload an optional payload, which ScalaTest will include in a resulting `JUnitTestFailedError` event
 *
 * @throws NullArgumentException if either `message` or `cause` is `null`, or `Some(null)`.
 *
 * @author Bill Venners
 */
class JUnitTestFailedError(
  val message: Option[String],
  val cause: Option[Throwable],
  val posOrStackDepth: Either[source.Position, Int],
  val payload: Option[Any]
) extends AssertionFailedError(if (message.isDefined) message.get else "") with StackDepth with ModifiableMessage[JUnitTestFailedError]  with PayloadField with ModifiablePayload[JUnitTestFailedError] {

  // TODO: CHange above to a message.getOrElse(""), and same in other exceptions most likely
  // TODO: Possibly change stack depth to stackDepthFun like in TFE, consider messageFun like in TDE

  requireNonNull(message, cause)
  message match {
    case Some(null) => throw new NullArgumentException("message was a Some(null)")
    case _ =>
  }

  cause match {
    case Some(null) => throw new NullArgumentException("cause was a Some(null)")
    case _ =>
  }

  if (cause.isDefined)
    super.initCause(cause.get)

  def this(
    message: Option[String],
    cause: Option[Throwable],
    pos: source.Position,
    payload: Option[Any]
  ) = this(message, cause, Left(pos), payload)

  // This is the olde general constructor
  def this(
    message: Option[String],
    cause: Option[Throwable],
    failedCodeStackDepth: Int,
    payload: Option[Any]
  ) = this(message, cause, Right(failedCodeStackDepth), payload)

  val position: Option[source.Position] = posOrStackDepth.left.toOption

  lazy val failedCodeFilePathname: Option[String] = position.map(_.filePathname)

  lazy val failedCodeStackDepth: Int =
     posOrStackDepth match {
       case Left(pos) => getStackDepth(this.getStackTrace, pos)
       case Right(sd) => sd
     }

  /*
  * Throws <code>IllegalStateException</code>, because <code>StackDepthException</code>s are
  * always initialized with a cause passed to the constructor of superclass <code>
  */
  override final def initCause(throwable: Throwable): Throwable = { throw new IllegalStateException }

  /**
   * Create a `JUnitTestFailedError` with specified stack depth and no detail message or cause.
   *
   * @param failedCodeStackDepth the depth in the stack trace of this exception at which the line of test code that failed resides.
   *
   */
  def this(failedCodeStackDepth: Int) = this(None, None, Right(failedCodeStackDepth), None)

  /**
   * Create a `JUnitTestFailedError` with a specified stack depth and detail message.
   *
   * @param message A detail message for this `JUnitTestFailedError`.
   * @param failedCodeStackDepth the depth in the stack trace of this exception at which the line of test code that failed resides.
   *
   * @throws NullArgumentException if `message` is `null`.
   */
  def this(message: String, failedCodeStackDepth: Int) =
    this(
      {
        requireNonNull(message)
        Some(message)
      },
      None,
      Right(failedCodeStackDepth),
      None
    )

  /**
   * Create a `JUnitTestFailedError` with the specified stack depth and cause.  The
   * `message` field of this exception object will be initialized to
   * `if (cause.getMessage == null) "" else cause.getMessage`.
   *
   * @param cause the cause, the `Throwable` that caused this `JUnitTestFailedError` to be thrown.
   * @param failedCodeStackDepth the depth in the stack trace of this exception at which the line of test code that failed resides.
   *
   * @throws NullArgumentException if `cause` is `null`.
   */
  def this(cause: Throwable, failedCodeStackDepth: Int) =
    this(
      {
        requireNonNull(cause)
        Some(if (cause.getMessage == null) "" else cause.getMessage)
      },
      Some(cause),
      Right(failedCodeStackDepth),
      None
    )

  /**
   * Create a `JUnitTestFailedError` with the specified stack depth, detail
   * message, and cause.
   *
   * <p>Note that the detail message associated with cause is
   * ''not'' automatically incorporated in this throwable's detail
   * message.
   *
   * @param message A detail message for this `JUnitTestFailedError`.
   * @param cause the cause, the `Throwable` that caused this `JUnitTestFailedError` to be thrown.
   * @param failedCodeStackDepth the depth in the stack trace of this exception at which the line of test code that failed resides.
   *
   * @throws NullArgumentException if either `message` or `cause` is `null`.
   */
  def this(message: String, cause: Throwable, failedCodeStackDepth: Int) =
    this(
      {
        requireNonNull(message)
        Some(message)
      },
      {
        requireNonNull(cause)
        Some(cause)
      },
      Right(failedCodeStackDepth),
      None
    )

  /**
   * Returns an exception of class `JUnitTestFailedError` with `failedExceptionStackDepth` set to 0 and 
   * all frames above this stack depth severed off. This can be useful when working with tools (such as IDEs) that do not
   * directly support ScalaTest. (Tools that directly support ScalaTest can use the stack depth information delivered
   * in the StackDepth exceptions.)
   */
  def severedAtStackDepth: JUnitTestFailedError = {
    val truncated = getStackTrace.drop(failedCodeStackDepth)
    val e = new JUnitTestFailedError(message, cause, posOrStackDepth, payload)
    e.setStackTrace(truncated)
    e
  }

  /**
   * Returns an instance of this exception's class, identical to this exception,
   * except with the detail message option string replaced with the result of passing
   * the current detail message to the passed function, `fun`.
   *
   * @param fun A function that, given the current optional detail message, will produce
   * the modified optional detail message for the result instance of `JUnitTestFailedError`.
   */
  def modifyMessage(fun: Option[String] => Option[String]): JUnitTestFailedError = {
    val mod = new JUnitTestFailedError(fun(message), cause, posOrStackDepth, payload)
    mod.setStackTrace(getStackTrace)
    mod
  }

  /**
   * Returns an instance of this exception's class, identical to this exception,
   * except with the payload option replaced with the result of passing
   * the current payload option to the passed function, `fun`.
   *
   * @param fun A function that, given the current optional payload, will produce
   * the modified optional payload for the result instance of `JUnitTestFailedError`.
   */
  def modifyPayload(fun: Option[Any] => Option[Any]): JUnitTestFailedError = {
    val currentPayload = payload
    val mod = new JUnitTestFailedError(message, cause, posOrStackDepth, fun(currentPayload))
    mod.setStackTrace(getStackTrace)
    mod
  }

  /**
   * Indicates whether this object can be equal to the passed object.
   */
  def canEqual(other: Any): Boolean = other.isInstanceOf[JUnitTestFailedError]

  /**
   * Indicates whether this object is equal to the passed object. If the passed object is
   * a `JUnitTestFailedError`, equality requires equal `message`,
   * `cause`, and `failedCodeStackDepth` fields, as well as equal
   * return values of `getStackTrace`.
   */
  override def equals(other: Any): Boolean =
    other match {
      case that: JUnitTestFailedError => 
        (that canEqual this) &&
        message == that.message &&
        cause == that.cause &&
        failedCodeStackDepth == that.failedCodeStackDepth &&
        getStackTrace.deep == that.getStackTrace.deep
      case _ => false
    }

  /**
   * Returns a hash code value for this object.
   */
  override def hashCode: Int =
    41 * (
      41 * (
        41 * (
          41 + message.hashCode
        ) + cause.hashCode
      ) + failedCodeStackDepth.hashCode
    ) + getStackTrace.hashCode
}
