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

import org.scalatest._
import org.scalatest.exceptions.StackDepthExceptionHelper.getStackDepth
import org.scalatest.Suite.anExceptionThatShouldCauseAnAbort
import scala.annotation.tailrec
import org.scalatest.time.Span
import exceptions.{TestCanceledException, TestFailedException, TestPendingException, TimeoutField}
import PatienceConfiguration._
import org.scalactic.source
import exceptions.StackDepthException

/**
 * Trait that facilitates testing with futures.
 *
 * This trait defines a <a href="Futures$FutureConcept.html">`FutureConcept`</a> trait that can be used to implicitly wrap
 * different kinds of futures, thereby providing a uniform testing API for futures.
 * The three ways this trait enables you to test futures are:
 * 
 *
 * 1. Invoking `isReadyWithin`, to assert that a future is ready within a a specified time period.
 * Here's an example:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * assert(result.isReadyWithin(100 millis))
 * }}}
 * 
 * 2. Invoking `futureValue`, to obtain a futures result within a specified or implicit time period,
 * like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * assert(result.futureValue === 7)
 *
 * // Or, if you expect the future to fail:
 * assert(result.failed.futureValue.isInstanceOf[ArithmeticException])
 * }}}
 * 
 * 3. Passing the future to `whenReady`, and performing assertions on the result value passed
 * to the given function, as in:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * whenReady(result) { s =&gt;
 *   s should be ("hello")
 * }
 * }}}
 *
 * The `whenReady` construct periodically inspects the passed
 * future, until it is either ready or the configured timeout has been surpassed. If the future becomes
 * ready before the timeout, `whenReady` passes the future's value to the specified function.
 * 
 *
 * To make `whenReady` more broadly applicable, the type of future it accepts is a `FutureConcept[T]`,
 * where `T` is the type of value promised by the future. Passing a future to `whenReady` requires
 * an implicit conversion from the type of future you wish to pass (the ''modeled type'') to
 * `FutureConcept[T]`. Subtrait `JavaFutures` provides an implicit conversion from
 * `java.util.concurrent.Future[T]` to `FutureConcept[T]`.
 * 
 *
 * For example, the following invocation of `whenReady` would succeed (not throw an exception):
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest._
 * import Matchers._
 * import concurrent.Futures._
 * import java.util.concurrent._
 * 
 * val exec = Executors.newSingleThreadExecutor
 * val task = new Callable[String] { def call() = { Thread.sleep(50); "hi" } }
 * whenReady(exec.submit(task)) { s =&gt;
 *   s should be ("hi")
 * }
 * }}}
 *
 * However, because the default timeout is 150 milliseconds, the following invocation of
 * `whenReady` would ultimately produce a `TestFailedException`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val task = new Callable[String] { def call() = { Thread.sleep(500); "hi" } }
 * whenReady(exec.submit(task)) { s =&gt;
 *   s should be ("hi")
 * }
 * }}}
 *
 * Assuming the default configuration parameters, a `timeout` of 150 milliseconds and an
 * `interval` of 15 milliseconds,
 * were passed implicitly to `whenReady`, the detail message of the thrown
 * `TestFailedException` would look like:
 * 
 *
 * `The future passed to whenReady was never ready, so whenReady timed out. Queried 95 times, sleeping 10 milliseconds between each query.`
 * 
 *
 * <a name="defaultPatience"></a>==Configuration of `whenReady`==
 *
 * The `whenReady` methods of this trait can be flexibly configured.
 * The two configuration parameters for `whenReady` along with their 
 * default values and meanings are described in the following table:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''Configuration Parameter'''
 * </th>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''Default Value'''
 * </th>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''Meaning'''
 * </th>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * timeout
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * scaled(150 milliseconds)
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * the maximum amount of time to allow unsuccessful queries before giving up and throwing `TestFailedException`
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * interval
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * scaled(15 milliseconds)
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * the amount of time to sleep between each query
 * </td>
 * </tr>
 * </table>
 *
 * The default values of both timeout and interval are passed to the `scaled` method, inherited
 * from `ScaledTimeSpans`, so that the defaults can be scaled up
 * or down together with other scaled time spans. See the documentation for trait <a href="ScaledTimeSpans.html">`ScaledTimeSpans`</a>
 * for more information.
 * 
 *
 * The `whenReady` methods of trait `Futures` each take a `PatienceConfig`
 * object as an implicit parameter. This object provides values for the two configuration parameters. Trait
 * `Futures` provides an implicit `val` named `defaultPatience` with each
 * configuration parameter set to its default value. 
 * If you want to set one or more configuration parameters to a different value for all invocations of
 * `whenReady` in a suite you can override this
 * val (or hide it, for example, if you are importing the members of the `Futures` companion object rather
 * than mixing in the trait). For example, if
 * you always want the default `timeout` to be 2 seconds and the default `interval` to be 5 milliseconds, you
 * can override `defaultPatience`, like this:
 *
 * {{{  <!-- class="stHighlight" -->
 * implicit override val defaultPatience =
 *   PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))
 * }}}
 *
 * Or, hide it by declaring a variable of the same name in whatever scope you want the changed values to be in effect:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * implicit val defaultPatience =
 *   PatienceConfig(timeout =  Span(2, Seconds), interval = Span(5, Millis))
 * }}}
 *
 * In addition to taking a `PatienceConfig` object as an implicit parameter, the `whenReady` methods of trait
 * `Futures` include overloaded forms that take one or two `PatienceConfigParam`
 * objects that you can use to override the values provided by the implicit `PatienceConfig` for a single `whenReady`
 * invocation. For example, if you want to set `timeout` to 6 seconds for just one particular `whenReady` invocation,
 * you can do so like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * whenReady (exec.submit(task), timeout(Span(6, Seconds))) { s =&gt;
 *   s should be ("hi")
 * }
 * }}}
 *
 * This invocation of `eventually` will use 6000 for `timeout` and whatever value is specified by the 
 * implicitly passed `PatienceConfig` object for the `interval` configuration parameter.
 * If you want to set both configuration parameters in this way, just list them separated by commas:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * whenReady (exec.submit(task), timeout(Span(6, Seconds)), interval(Span(500, Millis))) { s =&gt;
 *   s should be ("hi")
 * }
 * }}}
 *
 * You can also import or mix in the members of <a href="../time/SpanSugar.html">`SpanSugar`</a> if
 * you want a more concise DSL for expressing time spans:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * whenReady (exec.submit(task), timeout(6 seconds), interval(500 millis)) { s =&gt;
 *   s should be ("hi")
 * }
 * }}}
 *
 * ''Note: The `whenReady` construct was in part inspired by the `whenDelivered` matcher of the 
 * <a href="http://github.com/jdegoes/blueeyes" target="_blank">BlueEyes</a> project, a lightweight, asynchronous web framework for Scala.''
 * 
 *
 * @author Bill Venners
 */
trait Futures extends PatienceConfiguration {

  private[concurrent] val jsAdjustment: Int = 0

  /**
   * Concept trait for futures, instances of which are passed to the `whenReady`
   * methods of trait <a href="Futures.html">`Futures`</a>.
   *
   * See the documentation for trait <a href="Futures.html">`Futures`</a> for the details on the syntax this trait
   * provides for testing with futures.
   * 
   *
   * @author Bill Venners
   */
  trait FutureConcept[T] { thisFuture =>

    /**
     * Queries this future for its value.
     *
     * If the future is not ready, this method will return `None`. If ready, it will either return an exception
     * or a `T`.
     * 
     */
    def eitherValue: Option[Either[Throwable, T]]

    /**
     * Indicates whether this future has expired (timed out).
     *
     * The timeout detected by this method is different from the timeout supported by `whenReady`. This timeout
     * is a timeout of the underlying future. If the underlying future does not support timeouts, this method must always
     * return `false`.
     * 
     */
    def isExpired: Boolean

    /**
     * Indicates whether this future has been canceled.
     *
     * If the underlying future does not support the concept of cancellation, this method must always return `false`.
     * 
     */
    def isCanceled: Boolean

    /**
     * Indicates whether this future is ready within the specified timeout.
     *
     * If the `eitherValue` method of the underlying Scala future returns a `scala.Some` containing a
     * `scala.util.Failure` containing a `java.util.concurrent.ExecutionException`, and this
     * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The
     * `ExecutionException` will be be included as the `TestFailedException`'s cause only if the
     * `ExecutionException`'s cause is `null`.
     * 
     *
     * @param timeout
     * @param config
     * @return
     */
    final def isReadyWithin(timeout: Span)(implicit config: PatienceConfig, pos: source.Position): Boolean = {
      try {
        futureValueImpl(pos)(PatienceConfig(timeout, config.interval))
        true
      }
      catch {
        case e: TimeoutField => false
      }
    }

    /**
     * Returns the result of this `FutureConcept`, once it is ready, or throws either the
     * exception returned by the future (''i.e.'', `value` returned a `Left`)
     * or `TestFailedException`.
     *
     * The maximum amount of time to wait for the future to become ready before giving up and throwing
     * `TestFailedException` is configured by the value contained in the passed
     * `timeout` parameter.
     * The interval to sleep between queries of the future (used only if the future is polled) is configured by the value contained in the passed
     * `interval` parameter.
     * 
     *
     * This method invokes the overloaded `futureValue` form with only one (implicit) argument
     * list that contains only one argument, a `PatienceConfig`, passing a new
     * `PatienceConfig` with the `Timeout` specified as `timeout` and
     * the `Interval` specified as `interval`.
     * 
     *
     * If the `eitherValue` method of the underlying Scala future returns a `scala.Some` containing a
     * `scala.util.Failure` containing a `java.util.concurrent.ExecutionException`, and this
     * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The
     * `ExecutionException` will be be included as the `TestFailedException`'s cause only if the
     * `ExecutionException`'s cause is `null`.
     * 
     *
     * @param timeout the `Timeout` configuration parameter
     * @param interval the `Interval` configuration parameter
     * @return the result of the future once it is ready, if `value` is defined as a `Right`
     * @throws Throwable if once ready, the `value` of this future is defined as a
     *       `Left` (in this case, this method throws that same exception)
     * @throws TestFailedException if the future is cancelled, expires, or is still not ready after
     *     the specified timeout has been exceeded
     */
    final def futureValue(timeout: Timeout, interval: Interval)(implicit pos: source.Position): T = {
      futureValueImpl(pos)(PatienceConfig(timeout.value, interval.value))
    }

    /**
     * Returns the result of this `FutureConcept`, once it is ready, or throws either the
     * exception returned by the future (''i.e.'', `value` returned a `Left`)
     * or `TestFailedException`.
     *
     * The maximum amount of time to wait for the future to become ready before giving up and throwing
     * `TestFailedException` is configured by the value contained in the passed
     * `timeout` parameter.
     * The interval to sleep between queries of the future (used only if the future is polled) is configured by the `interval` field of
     * the `PatienceConfig` passed implicitly as the last parameter.
     * 
     *
     * This method invokes the overloaded `futureValue` form with only one (implicit) argument
     * list that contains only one argument, a `PatienceConfig`, passing a new
     * `PatienceConfig` with the `Timeout` specified as `timeout` and
     * the `Interval` specified as `config.interval`.
     * 
     *
     * If the `eitherValue` method of the underlying Scala future returns a `scala.Some` containing a
     * `scala.util.Failure` containing a `java.util.concurrent.ExecutionException`, and this
     * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The
     * `ExecutionException` will be be included as the `TestFailedException`'s cause only if the
     * `ExecutionException`'s cause is `null`.
     * 
     *
     * @param timeout the `Timeout` configuration parameter
     * @param config an `PatienceConfig` object containing `timeout` and
     *          `interval` parameters that are unused by this method
     * @return the result of the future once it is ready, if `eitherValue` is defined as a `Right`
     * @throws Throwable if once ready, the `eitherValue` of this future is defined as a
     *       `Left` (in this case, this method throws that same exception)
     * @throws TestFailedException if the future is cancelled, expires, or is still not ready after
     *     the specified timeout has been exceeded
     */
    final def futureValue(timeout: Timeout)(implicit config: PatienceConfig, pos: source.Position): T = {
      futureValueImpl(pos)(PatienceConfig(timeout.value, config.interval))
    }

    /**
     * Returns the result of this `FutureConcept`, once it is ready, or throws either the
     * exception returned by the future (''i.e.'', `eitherValue` returned a `Left`)
     * or `TestFailedException`.
     *
     * The maximum amount of time to wait for the future to become ready before giving up and throwing
     * `TestFailedException` is configured by the `timeout` field of
     * the `PatienceConfig` passed implicitly as the last parameter.
     * The interval to sleep between queries of the future (used only if the future is polled) is configured by the value contained in the passed
     * `interval` parameter.
     * 
     *
     * This method invokes the overloaded `futureValue` form with only one (implicit) argument
     * list that contains only one argument, a `PatienceConfig`, passing a new
     * `PatienceConfig` with the `Interval` specified as `interval` and
     * the `Timeout` specified as `config.timeout`.
     * 
     *
     * If the `eitherValue` method of the underlying Scala future returns a `scala.Some` containing a
     * `scala.util.Failure` containing a `java.util.concurrent.ExecutionException`, and this
     * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The
     * `ExecutionException` will be be included as the `TestFailedException`'s cause only if the
     * `ExecutionException`'s cause is `null`.
     * 
     *
     * @param interval the `Interval` configuration parameter
     * @param config an `PatienceConfig` object containing `timeout` and
     *          `interval` parameters that are unused by this method
     * @return the result of the future once it is ready, if `value` is defined as a `Right`
     * @throws Throwable if once ready, the `value` of this future is defined as a
     *       `Left` (in this case, this method throws that same exception)
     * @throws TestFailedException if the future is cancelled, expires, or is still not ready after
     *     the specified timeout has been exceeded
     */
    final def futureValue(interval: Interval)(implicit config: PatienceConfig, pos: source.Position): T = {
      futureValueImpl(pos)(PatienceConfig(config.timeout, interval.value))
    }

    /**
     * Returns the result of this `FutureConcept`, once it is ready, or throws either the
     * exception returned by the future (''i.e.'', `futureValue` returned a `Left`)
     * or `TestFailedException`.
     *
     * This trait's implementation of this method queries the future repeatedly until it either is
     * ready, or a configured maximum amount of time has passed, sleeping a configured interval between
     * attempts; and when ready, returns the future's value. For greater efficiency, implementations of
     * this trait may override this method so that it blocks the specified timeout while waiting for
     * the result, if the underlying future supports this.
     * 
     *
     * The maximum amount of time to wait for the future to become ready before giving up and throwing
     * `TestFailedException` is configured by the `timeout` field of
     * the `PatienceConfig` passed implicitly as the last parameter.
     * The interval to sleep between queries of the future (used only if the future is polled) is configured by the `interval` field of
     * the `PatienceConfig` passed implicitly as the last parameter.
     * 
     *
     * If the `eitherValue` method of the underlying Scala future returns a `scala.Some` containing a
     * `scala.util.Failure` containing a `java.util.concurrent.ExecutionException`, and this
     * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The
     * `ExecutionException` will be be included as the `TestFailedException`'s cause only if the
     * `ExecutionException`'s cause is `null`.
     * 
     *
     * @param config a `PatienceConfig` object containing `timeout` and
     *          `interval` parameters that are unused by this method
     * @return the result of the future once it is ready, if `value` is defined as a `Right`
     * @throws Throwable if once ready, the `value` of this future is defined as a
     *       `Left` (in this case, this method throws that same exception)
     * @throws TestFailedException if the future is cancelled, expires, or is still not ready after
     *     the specified timeout has been exceeded
     */
    def futureValue(implicit config: PatienceConfig, pos: source.Position): T = {
      futureValueImpl(pos)(config)
    }

    private[concurrent] def futureValueImpl(pos: source.Position)(implicit config: PatienceConfig): T = {

      val startNanos = System.nanoTime

      @tailrec
      def tryTryAgain(attempt: Int): T = {
        val timeout = config.timeout
        val interval = config.interval
        if (thisFuture.isCanceled)
          throw new TestFailedException(
            (_: StackDepthException) => Some(Resources.futureWasCanceled),
            None,
            pos
          )
        if (thisFuture.isExpired)
          throw new TestFailedException(
            (_: StackDepthException) => Some(Resources.futureExpired(attempt.toString, interval.prettyString)),
            None,
            pos
          )
        thisFuture.eitherValue match {
          case Some(Right(v)) => v
          case Some(Left(tpe: TestPendingException)) => throw tpe
          case Some(Left(tce: TestCanceledException)) => throw tce
          case Some(Left(e)) if anExceptionThatShouldCauseAnAbort(e) => throw e
          case Some(Left(ee: java.util.concurrent.ExecutionException)) if ee.getCause != null =>
            val cause = ee.getCause
            cause match {
              case tpe: TestPendingException => throw tpe
              case tce: TestCanceledException => throw tce
              case e if anExceptionThatShouldCauseAnAbort(e) => throw e
              case _ =>
                throw new TestFailedException(
                  (_: StackDepthException) => Some {
                    if (cause.getMessage == null)
                      Resources.futureReturnedAnException(cause.getClass.getName)
                    else
                      Resources.futureReturnedAnExceptionWithMessage(cause.getClass.getName, cause.getMessage)
                  },
                  Some(cause),
                  pos
                )
            }
          case Some(Left(e)) =>
            throw new TestFailedException(
              (_: StackDepthException) => Some {
                if (e.getMessage == null)
                  Resources.futureReturnedAnException(e.getClass.getName)
                else
                  Resources.futureReturnedAnExceptionWithMessage(e.getClass.getName, e.getMessage)
              },
              Some(e),
              pos
            )
          case None =>
            val duration = System.nanoTime - startNanos
            if (duration < timeout.totalNanos)
              SleepHelper.sleep(interval.millisPart, interval.nanosPart)
            else {
              throw new TestFailedException(
                (_: StackDepthException) => Some(Resources.wasNeverReady(attempt.toString, interval.prettyString)),
                None,
                pos
              ) with TimeoutField {
                val timeout: Span = config.timeout
              }
            }

            tryTryAgain(attempt + 1)
        }
      }
      tryTryAgain(1)
    }
  }

  /**
   * Queries the passed future repeatedly until it either is ready, or a configured maximum
   * amount of time has passed, sleeping a configured interval between attempts; and when ready, passes the future's value
   * to the passed function.
   *
   * The maximum amount of time to tolerate unsuccessful queries before giving up and throwing
   * `TestFailedException` is configured by the value contained in the passed
   * `timeout` parameter.
   * The interval to sleep between attempts is configured by the value contained in the passed
   * `interval` parameter.
   * 
   *
   * If the `eitherValue` method of the underlying Scala future returns a `scala.Some` containing a
   * `scala.util.Failure` containing a `java.util.concurrent.ExecutionException`, and this
   * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The
   * `ExecutionException` will be be included as the `TestFailedException`'s cause only if the
   * `ExecutionException`'s cause is `null`.
   * 
   *
   * @param future the future to query
   * @param timeout the `Timeout` configuration parameter
   * @param interval the `Interval` configuration parameter
   * @param fun the function to which pass the future's value when it is ready
   * @param config an `PatienceConfig` object containing `timeout` and
   *          `interval` parameters that are unused by this method
   * @return the result of invoking the `fun` parameter
   */
  final def whenReady[T, U](future: FutureConcept[T], timeout: Timeout, interval: Interval)(fun: T => U)(implicit config: PatienceConfig, pos: source.Position): U = {
    val result = future.futureValueImpl(pos)(PatienceConfig(timeout.value, interval.value))
    fun(result)
  }
    // whenReady(future)(fun)(PatienceConfig(timeout.value, interval.value))

  /**
   * Queries the passed future repeatedly until it either is ready, or a configured maximum
   * amount of time has passed, sleeping a configured interval between attempts; and when ready, passes the future's value
   * to the passed function.
   *
   * The maximum amount of time in milliseconds to tolerate unsuccessful queries before giving up and throwing
   * `TestFailedException` is configured by the value contained in the passed
   * `timeout` parameter.
   * The interval to sleep between attempts is configured by the `interval` field of
   * the `PatienceConfig` passed implicitly as the last parameter.
   * 
   *
   * If the `eitherValue` method of the underlying Scala future returns a `scala.Some` containing a
   * `scala.util.Failure` containing a `java.util.concurrent.ExecutionException`, and this
   * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The
   * `ExecutionException` will be be included as the `TestFailedException`'s cause only if the
   * `ExecutionException`'s cause is `null`.
   * 
   *
   * @param future the future to query
   * @param timeout the `Timeout` configuration parameter
   * @param fun the function to which pass the future's value when it is ready
   * @param config an `PatienceConfig` object containing `timeout` and
   *          `interval` parameters that are unused by this method
   * @return the result of invoking the `fun` parameter
   */
  final def whenReady[T, U](future: FutureConcept[T], timeout: Timeout)(fun: T => U)(implicit config: PatienceConfig, pos: source.Position): U = {
    val result = future.futureValueImpl(pos)(PatienceConfig(timeout.value, config.interval))
    fun(result)
  }
    // whenReady(future)(fun)(PatienceConfig(timeout.value, config.interval))

  /**
   * Queries the passed future repeatedly until it either is ready, or a configured maximum
   * amount of time has passed, sleeping a configured interval between attempts; and when ready, passes the future's value
   * to the passed function.
   *
   * The maximum amount of time in milliseconds to tolerate unsuccessful attempts before giving up is configured by the `timeout` field of
   * the `PatienceConfig` passed implicitly as the last parameter.
   * The interval to sleep between attempts is configured by the value contained in the passed
   * `interval` parameter.
   * 
   *
   * @param future the future to query
   * @param interval the `Interval` configuration parameter
   * @param fun the function to which pass the future's value when it is ready
   * @param config an `PatienceConfig` object containing `timeout` and
   *          `interval` parameters that are unused by this method
   * @return the result of invoking the `fun` parameter
   */
  final def whenReady[T, U](future: FutureConcept[T], interval: Interval)(fun: T => U)(implicit config: PatienceConfig, pos: source.Position): U = {
    val result = future.futureValueImpl(pos)(PatienceConfig(config.timeout, interval.value))
    fun(result)
  }
    // whenReady(future)(fun)(PatienceConfig(config.timeout, interval.value))

  /**
   * Queries the passed future repeatedly until it either is ready, or a configured maximum
   * amount of time has passed, sleeping a configured interval between attempts; and when ready, passes the future's value
   * to the passed function.
   *
   * The maximum amount of time in milliseconds to tolerate unsuccessful attempts before giving up is configured by the `timeout` field of
   * the `PatienceConfig` passed implicitly as the last parameter.
   * The interval to sleep between attempts is configured by the `interval` field of
   * the `PatienceConfig` passed implicitly as the last parameter.
   * 
   *
   * If the `eitherValue` method of the underlying Scala future returns a `scala.Some` containing a
   * `scala.util.Failure` containing a `java.util.concurrent.ExecutionException`, and this
   * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The
   * `ExecutionException` will be be included as the `TestFailedException`'s cause only if the
   * `ExecutionException`'s cause is `null`.
   * 
   *
   *
   * @param future the future to query
   * @param fun the function to which pass the future's value when it is ready
   * @param config an `PatienceConfig` object containing `timeout` and
   *          `interval` parameters that are unused by this method
   * @return the result of invoking the `fun` parameter
   */
  final def whenReady[T, U](future: FutureConcept[T])(fun: T => U)(implicit config: PatienceConfig, pos: source.Position): U = {
    val result = future.futureValueImpl(pos)(config)
    fun(result)
  }
}

