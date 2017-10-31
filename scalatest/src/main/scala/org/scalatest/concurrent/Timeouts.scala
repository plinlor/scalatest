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

import java.util.TimerTask
import java.util.Timer
import org.scalatest.exceptions.StackDepthException
import org.scalatest.Resources
import org.scalatest.exceptions.StackDepthException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.Selector
import java.net.Socket
import org.scalatest.Exceptional
import org.scalatest.time.Span
import org.scalatest.exceptions.TestFailedDueToTimeoutException
import org.scalatest.exceptions.TestCanceledException
import org.scalactic._

/**
 * '''This trait has been deprecated and will be removed in a later version of ScalaTest. Please use trait
 * <a href="TimeLimits.scala">TimeLimits</a> instead.'''
 *
 * '''
 * `TimeLimits` differs from `Timeouts` in two ways. First, its behavior is driven by a <a href="">`Timed`</a>
 * typeclass, so that it can treat `Future`s (and <a href="FutureOutcome.html">`FutureOutcome`</a>s) differently than
 * non-`Future`s. Second, where `Timeouts` `failAfter` and `cancelAfter` take an implicit
 * `Interruptor` strategy, the corresponding methods in `TimeLimits` take an implicit  `Signaler` strategy. 
 * Although the `Signaler` hierarchy corresponds exactly to the `Interruptor` hierarchy, the default is different.
 * For `Timeouts`, the default is `ThreadInterruptor`; For `Signaler`, the default is
 * `DoNotSignal`.
 * '''
 * 
 *
 * Trait that provides a `failAfter` and `cancelAfter` construct, which allows you to specify a time limit for an
 * operation passed as a by-name parameter, as well as a way to interrupt it if the operation exceeds its time limit.
 *
 * The time limit is passed as the first parameter, as a <a href="../time/Span.html">`Span`</a>. The operation is
 * passed as the second parameter. And an <a href="Interruptor.html">`Interruptor`</a>, a strategy for interrupting the operation, is
 * passed as an implicit third parameter.  Here's a simple example of its use:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * failAfter(Span(100, Millis)) {
 *   Thread.sleep(200)
 * }
 * }}}
 *
 * The above code, after 100 milliseconds, will produce a <a href="../exceptions/TestFailedDueToTimeoutException.html">`TestFailedDueToTimeoutException`</a> with a message
 * that indicates a timeout expired:
 * 
 *
 * `The code passed to failAfter did not complete within 100 milliseconds.`
 * 
 *
 * If you use `cancelAfter` in place of `failAfter`, a <a href="../exceptions/TestCanceledException.html">`TestCanceledException`</a> with a message
 * that indicates a timeout expired:
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
 * invoked `failAfter` or `cancelAfter`, so that it can detect when the timeout has expired and attempt to ''interrupt''
 * the main thread. Because different operations can require different interruption strategies, the `failAfter` or `cancelAfter`
 * method accepts an implicit third parameter of type `Interruptor` that is responsible for interrupting
 * the main thread.
 * 
 *
 * <a name="interruptorConfig"></a>==Configuring `failAfter` or `cancelAfter` with an `Interruptor`==
 *
 * This trait declares an implicit `val` named `defaultInterruptor`,
 * initialized with a <a href="ThreadInterruptor$.html">`ThreadInterruptor`</a>, which attempts to interrupt the main thread by invoking
 * `Thread.interrupt`. If you wish to use a different strategy, you can override this `val` (or hide
 * it, for example if you imported the members of `Timeouts` rather than mixing it in). Here's an example
 * in which the default interruption method is changed to <a href="DoNotInterrupt$.html">`DoNotInterrupt`</a>, which does not attempt to
 * interrupt the main thread in any way:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * override val defaultInterruptor = DoNotInterrupt
 * failAfter(100 millis) {
 *   Thread.sleep(500)
 * }
 * }}}
 *
 * As with the default `Interruptor`, the above code will eventually produce a 
 * `TestFailedDueToTimeoutException` with a message that indicates a timeout expired. However, instead
 * of throwing the exception after approximately 100 milliseconds, it will throw it after approximately 500 milliseconds.
 * 
 *
 * This illustrates an important feature of `failAfter` and `cancelAfter`: it will throw a
 * `TestFailedDueToTimeoutException` (or `TestCanceledException` in case of `cancelAfter`)
 * if the code passed as the by-name parameter takes longer than the specified timeout to execute, even if it
 * is allowed to run to completion beyond the specified timeout and returns normally.
 * 
 * 
 * ScalaTest provides the following `Interruptor` implementations:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''`Interruptor` implementation'''
 * </th>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''Usage'''
 * </th>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="ThreadInterruptor$.html">ThreadInterruptor</a>
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * The default interruptor, invokes `interrupt` on the main test thread. This will
 * set the interrupted status for the main test thread and,
 * if the main thread is blocked, will in some cases cause the main thread to complete abruptly with
 * an `InterruptedException`.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="DoNotInterrupt$.html">DoNotInterrupt</a>
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * Does not attempt to interrupt the main test thread in any way
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="SelectorInterruptor.html">SelectorInterruptor</a>
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * Invokes `wakeup` on the passed `java.nio.channels.Selector`, which
 * will cause the main thread, if blocked in `Selector.select`, to complete abruptly with a
 * `ClosedSelectorException`.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="SocketInterruptor.html">SocketInterruptor</a>
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
 * You may wish to create your own `Interruptor` in some situations. For example, if your operation is performing
 * a loop and can check a volatile flag each pass through the loop. You could in that case write an `Interruptor` that
 * sets that flag so that the next time around, the loop would exit.
 * 
 * 
 * @author Chua Chee Seng
 * @author Bill Venners
 */
@deprecated("Please use org.scalatest.concurrent.TimeLimits instead")
trait Timeouts {

  /**
   * Implicit `Interruptor` value defining a default interruption strategy for the `failAfter` and `cancelAfter` method.
   *
   * To change the default `Interruptor` configuration, override or hide this `val` with another implicit
   * `Interruptor`.
   * 
   */
  implicit val defaultInterruptor: Interruptor = ThreadInterruptor

  /**
   * Executes the passed function, enforcing the passed time limit by attempting to interrupt the function if the
   * time limit is exceeded, and throwing `TestFailedDueToTimeoutException` if the time limit has been 
   * exceeded after the function completes.
   *
   * If the function completes ''before'' the timeout expires:
   * 
   *
   * <ul>
   * <li>If the function returns normally, this method will return normally.</li>
   * <li>If the function completes abruptly with an exception, this method will complete abruptly with that same exception.</li>
   * </ul>
   *
   * If the function completes ''after'' the timeout expires:
   * 
   *
   * <ul>
   * <li>If the function returns normally, this method will complete abruptly with a `TestFailedDueToTimeoutException`.</li>
   * <li>If the function completes abruptly with an exception, this method will complete abruptly with a `TestFailedDueToTimeoutException` that includes the exception thrown by the function as its cause.</li>
   * </ul>
   *
   * If the interrupted status of the main test thread (the thread that invoked `failAfter`) was not invoked
   * when `failAfter` was invoked, but is set after the operation times out, it is reset by this method before
   * it completes abruptly with a `TestFailedDueToTimeoutException`. The interrupted status will be set by
   * `ThreadInterruptor`, the default `Interruptor` implementation.
   * 
   *
   * @param timeout the maximimum amount of time allowed for the passed operation
   * @param fun the operation on which to enforce the passed timeout
   * @param interruptor a strategy for interrupting the passed operation
   */
  def failAfter[T](timeout: Span)(fun: => T)(implicit interruptor: Interruptor, pos: source.Position = implicitly[source.Position]): T = {
    timeoutAfter(
      timeout,
      fun,
      interruptor,
      t => new TestFailedDueToTimeoutException(
        (_: StackDepthException) => Some(Resources.timeoutFailedAfter(timeout.prettyString)), t, pos, None, timeout
      )
    )
  }

  // TODO: Consider creating a TestCanceledDueToTimeoutException
  /**
   * Executes the passed function, enforcing the passed time limit by attempting to interrupt the function if the
   * time limit is exceeded, and throwing `TestCanceledException` if the time limit has been
   * exceeded after the function completes.
   *
   * If the function completes ''before'' the timeout expires:
   * 
   *
   * <ul>
   * <li>If the function returns normally, this method will return normally.</li>
   * <li>If the function completes abruptly with an exception, this method will complete abruptly with that same exception.</li>
   * </ul>
   *
   * If the function completes ''after'' the timeout expires:
   * 
   *
   * <ul>
   * <li>If the function returns normally, this method will complete abruptly with a `TestCanceledException`.</li>
   * <li>If the function completes abruptly with an exception, this method will complete abruptly with a `TestCanceledException` that includes the exception thrown by the function as its cause.</li>
   * </ul>
   *
   * If the interrupted status of the main test thread (the thread that invoked `cancelAfter`) was not invoked
   * when `cancelAfter` was invoked, but is set after the operation times out, it is reset by this method before
   * it completes abruptly with a `TestCanceledException`. The interrupted status will be set by
   * `ThreadInterruptor`, the default `Interruptor` implementation.
   * 
   *
   * @param timeout the maximimum amount of time allowed for the passed operation
   * @param f the operation on which to enforce the passed timeout
   * @param interruptor a strategy for interrupting the passed operation
   */
  def cancelAfter[T](timeout: Span)(f: => T)(implicit interruptor: Interruptor, pos: source.Position = implicitly[source.Position]): T = {
    timeoutAfter(timeout, f, interruptor, t => new TestCanceledException((sde: StackDepthException) => Some(Resources.timeoutCanceledAfter(timeout.prettyString)), t, pos, None))
  }

  /*private def timeoutAfter[T](timeout: Span, f: => T, interruptor: Interruptor, exceptionFun: Option[Throwable] => StackDepthException): T = {
    val timer = new Timer()
    val task = new TimeoutTask(Thread.currentThread(), interruptor)
    timer.schedule(task, timeout.totalNanos / 1000 / 1000)
    try {
      val result = f
      timer.cancel()
      if (task.timedOut) {
        if (task.needToResetInterruptedStatus)
          Thread.interrupted() // To reset the flag probably. He only does this if it was not set before and was set after, I think.
        throw exceptionFun(None)
      }
      result
    }
    catch {
      case t: Throwable => 
        timer.cancel() // Duplicate code could be factored out I think. Maybe into a finally? Oh, not that doesn't work. So a method.
        if(task.timedOut) {
          if (task.needToResetInterruptedStatus)
            Thread.interrupted() // Clear the interrupt status (There's a race condition here, but not sure we an do anything about that.)
          throw exceptionFun(Some(t))
        }
        else
          throw t
    }
  }*/
  
  private def timeoutAfter[T](timeout: Span, f: => T, interruptor: Interruptor, exceptionFun: Option[Throwable] => StackDepthException): T = {
    val timer = new Timer
    val task = new TimeoutTask(Thread.currentThread(), interruptor)
    timer.schedule(task, timeout.totalNanos / 1000 / 1000) // TODO: Probably use a sleep so I can use nanos
    try {
      val result = f
      timer.cancel()
      result match {
        case Exceptional(ex) => throw ex  // If the result is Exceptional, the exception is already wrapped, just re-throw it to get the old behavior.
        case _ => 
          if (task.timedOut) { 
            if (task.needToResetInterruptedStatus)
              Thread.interrupted() // To reset the flag probably. He only does this if it was not set before and was set after, I think.
            throw exceptionFun(None)
          }
      }
      result
    }
    catch {
      case t: Throwable => 
        timer.cancel() // Duplicate code could be factored out I think. Maybe into a finally? Oh, not that doesn't work. So a method.
        if(task.timedOut) {
          if (task.needToResetInterruptedStatus)
            Thread.interrupted() // Clear the interrupt status (There's a race condition here, but not sure we an do anything about that.)
          throw exceptionFun(Some(t))
        }
        else
          throw t
    }
  }
}

/**
 * Companion object that facilitates the importing of <code>Timeouts</code> members as 
 * an alternative to mixing in the trait. One use case is to import <code>Timeouts</code>'s members so you can use
 * them in the Scala interpreter.
 */
object Timeouts extends Timeouts
