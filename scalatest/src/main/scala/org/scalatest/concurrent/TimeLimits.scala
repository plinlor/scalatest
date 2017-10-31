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
package org.scalatest.concurrent

import org.scalatest.exceptions.StackDepthExceptionHelper.getStackDepthFun
import org.scalatest.exceptions.StackDepthExceptionHelper.posOrElseStackDepthFun
import org.scalatest.{FailureMessages, UnquotedString}
import org.scalatest.exceptions.{StackDepthException, TestFailedDueToTimeoutException, TestCanceledException}
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.Selector
import java.net.Socket
import org.scalatest.Exceptional
import org.scalatest.time.Span
import org.scalatest.enablers.Timed
import org.scalactic._

/**
 * Trait that provides `failAfter` and `cancelAfter` methods, which allow you to specify a time limit for an
 * operation passed as a by-name parameter, as well as a way to signal it if the operation exceeds its time limit.
 *
 * The time limit is passed as the first parameter, as a <a href="../time/Span.html">`Span`</a>. The operation is
 * passed as the second parameter. A <a href="Signaler.html">`Signaler`</a>, a strategy for interrupting the operation, is
 * passed as an implicit third parameter.  Here's a simple example of its use:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * failAfter(Span(100, Millis)) {
 *   Thread.sleep(200)
 * }
 * }}}
 *
 * The above code will eventually produce a <a href="../exceptions/TestFailedDueToTimeoutException.html">`TestFailedDueToTimeoutException`</a> with a message
 * that indicates a time limit has been exceeded:
 * 
 *
 * `The code passed to failAfter did not complete within 100 milliseconds.`
 * 
 *
 * If you use `cancelAfter` in place of `failAfter`, a <a href="../exceptions/TestCanceledException.html">`TestCanceledException`</a> will be thrown
 * instead, also with a message that indicates a time limit has been exceeded:
 * 
 *
 * `The code passed to cancelAfter did not complete within 100 milliseconds.`
 * 
 *
 * If you prefer you can mix in or import the members of <a href="../time/SpanSugar.html">`SpanSugar`</a> and place a units value after the integer timeout.
 * Here are some examples:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.time.SpanSugar._
 *
 * failAfter(100 millis) {
 *   Thread.sleep(200)
 * }
 *
 * failAfter(1 second) {
 *   Thread.sleep(2000)
 * }
 * }}}
 *
 * The code passed via the by-name parameter to `failAfter` or `cancelAfter` will be executed by the thread that invoked
 * `failAfter` or `cancelAfter`, so that no synchronization is necessary to access variables declared outside the by-name.
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * var result = -1 // No need to make this volatile
 * failAfter(100 millis) {
 *   result = accessNetService()
 * }
 * result should be (99)
 * }}}
 *
 * The `failAfter` or `cancelAfter` method will create a timer that runs on a different thread than the thread that
 * invoked `failAfter` or `cancelAfter`, so that it can detect when the time limit has been exceeded and attempt to ''signal''
 * the main thread. Because different operations can require different signaling strategies, the `failAfter` and `cancelAfter`
 * methods accept an implicit third parameter of type `Signaler` that is responsible for signaling
 * the main thread.
 * 
 *
 * <a name="signalerConfig"></a>==Configuring `failAfter` or `cancelAfter` with a `Signaler`==
 *
 * The `Signaler` companion object declares an implicit `val` of type `Signaler` that returns
 * a `DoNotSignal`. This serves as the default signaling strategy.
 * If you wish to use a different strategy, you can declare an implicit `val` that establishes a different `Signaler`
 * as the policy.  Here's an example
 * in which the default signaling strategy is changed to <a href="ThreadSignaler.html">`ThreadSignaler`</a>, which does not attempt to
 * interrupt the main thread in any way:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * override val signaler: Signaler = ThreadSignaler
 * failAfter(100 millis) {
 *   Thread.sleep(500)
 * }
 * }}}
 *
 * As with the default `Signaler`, the above code will eventually produce a 
 * `TestFailedDueToTimeoutException` with a message that indicates a timeout expired. However, instead
 * of throwing the exception after approximately 500 milliseconds, it will throw it after approximately 100 milliseconds.
 * 
 *
 * This illustrates an important feature of `failAfter` and `cancelAfter`: it will throw a
 * `TestFailedDueToTimeoutException` (or `TestCanceledException` in case of `cancelAfter`)
 * if the code passed as the by-name parameter takes longer than the specified timeout to execute, even if it
 * is allowed to run to completion beyond the specified timeout and returns normally.
 * 
 * 
 * ScalaTest provides the following `Signaler` implementations:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''`Signaler` implementation'''
 * </th>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''Usage'''
 * </th>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="DoNotSignal$.html">DoNotSignal</a>
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * The default signaler, does not attempt to interrupt the main test thread in any way
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="ThreadSignaler$.html">ThreadSignaler</a>
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * Invokes `interrupt` on the main test thread. This will
 * set the interrupted status for the main test thread and,
 * if the main thread is blocked, will in some cases cause the main thread to complete abruptly with
 * an `InterruptedException`.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="SelectorSignaler.html">SelectorSignaler</a>
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * Invokes `wakeup` on the passed `java.nio.channels.Selector`, which
 * will cause the main thread, if blocked in `Selector.select`, to complete abruptly with a
 * `ClosedSelectorException`.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="SocketSignaler.html">SocketSignaler</a>
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * Invokes `close` on the `java.io.Socket`, which
 * will cause the main thread, if blocked in a read or write of an `java.io.InputStream` or
 * `java.io.OutputStream` that uses the `Socket`, to complete abruptly with a
 * `SocketException`.
 * </td>
 * </tr>
 * </table>
 *
 * You may wish to create your own `Signaler` in some situations. For example, if your operation is performing
 * a loop and can check a volatile flag each pass through the loop, you could write a `Signaler` that
 * sets that flag so that the next time around, the loop would exit.
 * 
 * 
 * @author Chua Chee Seng
 * @author Bill Venners
 */
trait TimeLimits {

   /*
   * <p>
   * To change the default <code>Signaler</code> configuration, define an implicit
   * <code>Signaler</code> in scope.
   * </p>
   */
  // implicit val defaultSignaler: Signaler = ThreadSignaler

  /**
   * Executes the passed function, enforcing the passed time limit by attempting to signal the operation if the
   * time limit is exceeded, and "failing" if the time limit has been 
   * exceeded after the function completes, where what it means to "fail" is determined by the implicitly passed `Timed[T]`
   * instance.
   *
   * The <a href= "../enablers/Timed$.html">`Timed`</a> companion object offers three implicits, one for `FutureOutcome`, one for `Future[U]`
   * and one for any other type. The implicit `Timed[FutureOutcome]` defines failure as failing the `FutureOutcome` with a `TestFailedDueToTimeoutException`:
   * no exception will be thrown. The implicit `Timed[Future[U]]` defines failure as failing the `Future[U]` with a `TestFailedDueToTimeoutException`:
   * no exception will be thrown. The implicit for any other type defines failure as throwing
   * `TestFailedDueToTimeoutException`. For the details, see the Scaladoc of the implicit `Timed` providers
   * in the <a href= "../enablers/Timed$.html">`Timed`</a> companion object.
   * 
   *
   * @param timeout the maximimum amount of time allowed for the passed operation
   * @param fun the operation on which to enforce the passed timeout
   * @param signaler a strategy for signaling the passed operation
   * @param prettifier a `Prettifier` for prettifying error messages
   * @param pos the `Position` of the caller site
   * @param timed the `Timed` type class that provides the behavior implementation of the timing restriction.
   */
  def failAfter[T](timeout: Span)(fun: => T)(implicit signaler: Signaler, prettifier: Prettifier = implicitly[Prettifier], pos: source.Position = implicitly[source.Position], timed: Timed[T] = implicitly[Timed[T]]): T = {
    failAfterImpl(timeout, signaler, prettifier, Some(pos), getStackDepthFun(pos))(fun)(timed)
  }

  private[scalatest] def failAfterImpl[T](timeout: Span, signaler: Signaler, prettifier: Prettifier, pos: Option[source.Position], stackDepthFun: StackDepthException => Int)(fun: => T)(implicit timed: Timed[T]): T = {
    val stackTraceElements = Thread.currentThread.getStackTrace()
    timed.timeoutAfter(
      timeout,
      fun,
      signaler,
      (cause: Option[Throwable]) => {
        val e = new TestFailedDueToTimeoutException(
          (_: StackDepthException) => Some(FailureMessages.timeoutFailedAfter(prettifier, UnquotedString(timeout.prettyString))),
          cause,
          posOrElseStackDepthFun(pos, stackDepthFun),
          None,
          timeout
        )
        e.setStackTrace(stackTraceElements)
        e
      }
    )
  }

  // TODO: Consider creating a TestCanceledDueToTimeoutException
  /**
   * Executes the passed function, enforcing the passed time limit by attempting to signal the operation if the
   * time limit is exceeded, and "canceling" if the time limit has been 
   * exceeded after the function completes, where what it means to "cancel" is determined by the implicitly passed `Timed[T]`
   * instance.
   *
   * The <a href= "../enablers/Timed$.html">`Timed`</a> companion object offers three implicits, one for `FutureOutcome`, one for `Future[U]`
   * and one for any other type. The implicit `Timed[FutureOutcome]` defines cancelation as canceling the `FutureOutcome`:
   * no exception will be thrown. The implicit `Timed[Future[U]]` defines canceling as failing the `Future[U]` with a `TestCanceledException`:
   * no exception will be thrown. The implicit for any other type defines failure as throwing
   * `TestCanceledException`. For the details, see the Scaladoc of the implicit `Timed` providers
   * in the <a href= "../enablers/Timed$.html">`Timed`</a> companion object.
   * 
   *
   * @param timeout the maximimum amount of time allowed for the passed operation
   * @param f the operation on which to enforce the passed timeout
   * @param signaler a strategy for signaling the passed operation
   * @param prettifier a `Prettifier` for prettifying error messages
   * @param pos the `Position` of the caller site
   * @param timed the `Timed` type class that provides the behavior implementation of the timing restriction.
   */
  def cancelAfter[T](timeout: Span)(fun: => T)(implicit signaler: Signaler, prettifier: Prettifier = implicitly[Prettifier], pos: source.Position = implicitly[source.Position], timed: Timed[T] = implicitly[Timed[T]]): T = {
    cancelAfterImpl(timeout, signaler, prettifier, Some(pos), getStackDepthFun(pos))(fun)(timed)
  }

  private[scalatest] def cancelAfterImpl[T](timeout: Span, signaler: Signaler, prettifier: Prettifier, pos: Option[source.Position], stackDepthFun: StackDepthException => Int)(fun: => T)(implicit timed: Timed[T]): T = {
    val stackTraceElements = Thread.currentThread.getStackTrace()
    timed.timeoutAfter(
      timeout,
      fun,
      signaler,
      (cause: Option[Throwable]) => {
        val e = new TestCanceledException(
          (_: StackDepthException) => Some(FailureMessages.timeoutCanceledAfter(prettifier, UnquotedString(timeout.prettyString))),
          cause,
          posOrElseStackDepthFun(pos, stackDepthFun),
          None
        )
        e.setStackTrace(stackTraceElements)
        e
      }
    )
  }
}

/**
 * Companion object that facilitates the importing of `Timeouts` members as 
 * an alternative to mixing in the trait. One use case is to import `Timeouts`'s members so you can use
 * them in the Scala interpreter.
 */
object TimeLimits extends TimeLimits
