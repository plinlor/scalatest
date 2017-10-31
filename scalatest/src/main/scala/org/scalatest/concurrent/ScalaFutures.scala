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

import org.scalatest.Resources
import org.scalatest.Suite.anExceptionThatShouldCauseAnAbort
import org.scalatest.time.Span
import scala.util.Failure
import scala.util.Success

/**
 * Provides an implicit conversion from `scala.concurrent.Future[T]` to
 * <a href="Futures$FutureConcept.html">`FutureConcept[T]`</a>.
 *
 * This trait enables you to invoke the methods defined on `FutureConcept` on a Scala `Future`, as well as to pass a Scala future
 * to the `whenReady` methods of supertrait `Futures`.
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
trait ScalaFutures extends Futures {

  import scala.language.implicitConversions

  /**
   * Implicitly converts a `scala.concurrent.Future[T]` to
   * `FutureConcept[T]`, allowing you to invoke the methods
   * defined on `FutureConcept` on a Scala `Future`, as well as to pass a Scala future
   * to the `whenReady` methods of supertrait <a href="Futures.html">`Futures`</a>.
   *
   * See the documentation for supertrait <a href="Futures.html">`Futures`</a> for the details on the syntax this trait provides
   * for testing with Java futures.
   * 
   *
   * If the `eitherValue` method of the underlying Scala future returns a `scala.Some` containing a
   * `scala.util.Failure` containing a `java.util.concurrent.ExecutionException`, and this
   * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The
   * `ExecutionException` will be be included as the `TestFailedException`'s cause only if the
   * `ExecutionException`'s cause is `null`.
   * 
   *
   * The `isExpired` method of the returned `FutureConcept` will always return `false`, because
   * the underlying type, `scala.concurrent.Future`, does not support the notion of expiration. Likewise, the `isCanceled`
   * method of the returned `FutureConcept` will always return `false`, because
   * the underlying type, `scala.concurrent.Future`, does not support the notion of cancelation.
   * 
   *
   * @param scalaFuture a `scala.concurrent.Future[T]` to convert
   * @return a `FutureConcept[T]` wrapping the passed `scala.concurrent.Future[T]`
   */
  implicit def convertScalaFuture[T](scalaFuture: scala.concurrent.Future[T]): FutureConcept[T] =
    new FutureConcept[T] {
      def eitherValue: Option[Either[Throwable, T]] =
         scalaFuture.value.map {
           case Success(o) => Right(o)
           case Failure(e) => Left(e)
         }
      def isExpired: Boolean = false // Scala Futures themselves don't support the notion of a timeout
      def isCanceled: Boolean = false // Scala Futures don't seem to be cancelable either
/*
      def futureValue(implicit config: PatienceConfig): T = {
        try Await.ready(scalaFuture, Duration.fromNanos(config.timeout.totalNanos))
        catch {
          case e: TimeoutException => 
        }
      }
*/
    }
  //SCALATESTJS-ONLY override private[concurrent] val jsAdjustment: Int = -1
}

/**
 * Companion object that facilitates the importing of `ScalaFutures` members as
 * an alternative to mixing in the trait. One use case is to import `ScalaFutures`'s members so you can use
 * them in the Scala interpreter.
 */
object ScalaFutures extends ScalaFutures
