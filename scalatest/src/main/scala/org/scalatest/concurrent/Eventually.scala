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
import exceptions.{TestFailedDueToTimeoutException,  TestPendingException}
import org.scalatest.Suite.anExceptionThatShouldCauseAnAbort
import scala.annotation.tailrec
import time.{Nanosecond, Span, Nanoseconds}
import PatienceConfiguration._
import org.scalactic.source
import org.scalatest.exceptions.StackDepthException

/**
 * Trait that provides the `eventually` construct, which periodically retries executing
 * a passed by-name parameter, until it either succeeds or the configured timeout has been surpassed.
 *
 * The by-name parameter "succeeds" if it returns a result. It "fails" if it throws any exception that
 * would normally cause a test to fail. (These are any exceptions except <a href="../exceptions/TestPendingException.html">`TestPendingException`</a> and
 * `Error`s listed in the
 * <a href="../Suite.html#errorHandling">Treatment of `java.lang.Error`s</a> section of the
 * documentation of trait `Suite`.)
 * 
 *
 * For example, the following invocation of `eventually` would succeed (not throw an exception):
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val xs = 1 to 125
 * val it = xs.iterator
 * eventually { it.next should be (3) }
 * }}}
 *
 * However, because the default timeout is 150 milliseconds, the following invocation of
 * `eventually` would ultimately produce a `TestFailedDueToTimeoutException`:
 * 
 *
 * <a name="secondExample"></a>
 * {{{  <!-- class="stHighlight" -->
 * val xs = 1 to 125
 * val it = xs.iterator
 * eventually { Thread.sleep(50); it.next should be (110) }
 * }}}
 *
 * Assuming the default configuration parameters, a `timeout` of 150 milliseconds and an `interval` of 15 milliseconds,
 * were passed implicitly to `eventually`, the detail message of the thrown
 * <a href="../exceptions/TestFailedDueToTimeoutException.html">`TestFailedDueToTimeoutException`</a> would look like:
 * 
 *
 * `The code passed to eventually never returned normally. Attempted 2 times over 166.682 milliseconds. Last failure message: 2 was not equal to 110.`
 * 
 *
 * The cause of the thrown `TestFailedDueToTimeoutException` will be the exception most recently thrown by the block of code passed to eventually. (In
 * the previous example, the cause would be the `TestFailedException` with the detail message `2 was not equal to 100`.)
 * 
 *
 * <a name="patienceConfig"></a>==Configuration of `eventually`==
 *
 * The `eventually` methods of this trait can be flexibly configured.
 * The two configuration parameters for `eventually` along with their 
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
 * `timeout`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * `scaled(150 milliseconds)`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * the maximum amount of time to allow unsuccessful attempts before giving up and throwing `TestFailedDueToTimeoutException`
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * `interval`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * `scaled(15 milliseconds)`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * the amount of time to sleep between each attempt
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
 * The `eventually` methods of trait `Eventually` each take a `PatienceConfig`
 * object as an implicit parameter. This object provides values for the two configuration parameters. (These configuration parameters
 * are called "patience" because they determine how ''patient'' tests will be with asynchronous operations: how long
 * they will tolerate failures before giving up and how long they will wait before checking again after a failure.) Trait
 * `Eventually` provides an implicit `val` named `patienceConfig` with each
 * configuration parameter set to its default value. 
 * If you want to set one or more configuration parameters to a different value for all invocations of
 * `eventually` in a suite you can override this
 * val (or hide it, for example, if you are importing the members of the `Eventually` companion object rather
 * than mixing in the trait). For example, if
 * you always want the default `timeout` to be 2 seconds and the default `interval` to be 5 milliseconds, you
 * can override `patienceConfig`, like this:
 *
 * {{{  <!-- class="stHighlight" -->
 * implicit override val patienceConfig =
 *   PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))
 * }}}
 *
 * Or, hide it by declaring a variable of the same name in whatever scope you want the changed values to be in effect:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * implicit val patienceConfig =
 *   PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(5, Millis)))
 * }}}
 *
 * Passing your new default values to `scaled` is optional, but a good idea because it allows them to 
 * be easily scaled if run on a slower or faster system.
 * 
 *
 * In addition to taking a `PatienceConfig` object as an implicit parameter, the `eventually` methods of trait
 * `Eventually` include overloaded forms that take one or two `PatienceConfigParam`
 * objects that you can use to override the values provided by the implicit `PatienceConfig` for a single `eventually`
 * invocation. For example, if you want to set `timeout` to 5 seconds for just one particular `eventually` invocation,
 * you can do so like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * eventually (timeout(Span(5, Seconds))) { Thread.sleep(10); it.next should be (110) }
 * }}}
 *
 * This invocation of `eventually` will use 5 seconds for the `timeout` and whatever value is specified by the
 * implicitly passed `PatienceConfig` object for the `interval` configuration parameter.
 * If you want to set both configuration parameters in this way, just list them separated by commas:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * eventually (timeout(Span(5, Seconds)), interval(Span(5, Millis))) { it.next should be (110) }
 * }}}
 *
 * You can also import or mix in the members of <a href="../time/SpanSugar.html">`SpanSugar`</a> if
 * you want a more concise DSL for expressing time spans:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * eventually (timeout(5 seconds), interval(5 millis)) { it.next should be (110) }
 * }}}
 *
 * Note that ScalaTest will not scale any time span that is not explicitly passed to `scaled` to make
 * the meaning of the code as obvious as possible. Thus
 * if you ask for "`timeout(5 seconds)`" you will get exactly that: a timeout of five seconds. If you want such explicitly
 * given values to be scaled, you must pass them to `scale` explicitly like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * eventually (timeout(scaled(5 seconds))) { it.next should be (110) }
 * }}}
 *
 * The previous code says more clearly that the timeout will be five seconds, unless scaled higher or lower by the `scaled` method.
 * 
 *
 * <a name="simpleBackoff"></a>==Simple backoff algorithm==
 *
 * The `eventually` methods employ a very simple backoff algorithm to try and maximize the speed of tests. If an asynchronous operation
 * completes quickly, a smaller interval will yield a faster test. But if an asynchronous operation takes a while, a small interval will keep the CPU
 * busy repeatedly checking and rechecking a not-ready operation, to some extent taking CPU cycles away from other processes that could proceed. To
 * strike the right balance between these design tradeoffs, the `eventually` methods will check more frequently during the initial interval.
 * 
 *
 * 
 * Rather than sleeping an entire interval if the initial attempt fails, `eventually` will only sleep 1/10 of the configured interval. It
 * will continue sleeping only 1/10 of the configured interval until the configured interval has passed, after which it sleeps the configured interval
 * between attempts. Here's an example in which the timeout is set equal to the interval:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val xs = 1 to 125
 * val it = xs.iterator
 * eventually(timeout(100 milliseconds), interval(100 milliseconds)) { it.next should be (110) }
 * }}}
 *
 * Even though this call to `eventually` will time out after only one interval, approximately, the error message will likely report that more
 * than one (and less than ten) attempts were made:
 * 
 *
 * `The code passed to eventually never returned normally. Attempted 6 times over 100.485 milliseconds. Last failure message: 6 was not equal to 110.`
 *
 *
 * Note that if the initial attempt takes longer than the configured interval to complete, `eventually` will never sleep for 
 * a 1/10 interval. You can observe this behavior in the <a href="#secondExample">second example</a> above in which the first statement in the block of code passed to `eventually`
 * was `Thread.sleep(50)`. 
 * 
 *
 * <a name="patienceConfig"></a>==Usage note: `Eventually` intended primarily for integration testing==
 *
 * Although the default timeouts of trait `Eventually` are tuned for unit testing, the use of `Eventually` in unit tests is
 * a choice you should question. Usually during unit testing you'll want to mock out subsystems that would require `Eventually`, such as
 * network services with varying and unpredictable response times. This will allow your unit tests to run as fast as possible while still testing
 * the focused bits of behavior they are designed to test.
 *
 * Nevertheless, because sometimes it will make sense to use `Eventually` in unit tests (and 
 * because it is destined to happen anyway even when it isn't the best choice), `Eventually` by default uses
 * timeouts tuned for unit tests: Calls to `eventually` are more likely to succeed on fast development machines, and if a call does time out, 
 * it will do so quickly so the unit tests can move on.
 * 
 *
 * When you are using `Eventually` for integration testing, therefore, the default timeout and interval may be too small. A
 * good way to override them is by mixing in trait <a href="IntegrationPatience.html">`IntegrationPatience`</a> or a similar trait of your
 * own making. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class ExampleSpec extends FeatureSpec with Eventually with IntegrationPatience {
 *   // Your integration tests here...
 * }
 * }}}
 *
 * Trait `IntegrationPatience` increases the default timeout from 150 milliseconds to 15 seconds, the default
 * interval from 15 milliseconds to 150 milliseconds. If need be, you can do fine tuning of the timeout and interval by
 * specifying a <a href="../tools/Runner$.html#scalingTimeSpans">time span scale factor</a> when you
 * run your tests.
 * 
 *
 * @author Bill Venners
 * @author Chua Chee Seng
 */
trait Eventually extends PatienceConfiguration {

  /**
   * Invokes the passed by-name parameter repeatedly until it either succeeds, or a configured maximum
   * amount of time has passed, sleeping a configured interval between attempts.
   *
   * The by-name parameter "succeeds" if it returns a result. It "fails" if it throws any exception that
   * would normally cause a test to fail. (These are any exceptions except <a href="TestPendingException">`TestPendingException`</a> and
   * `Error`s listed in the
   * <a href="Suite.html#errorHandling">Treatment of `java.lang.Error`s</a> section of the
   * documentation of trait `Suite`.)
   * 
   *
   * The maximum amount of time in milliseconds to tolerate unsuccessful attempts before giving up and throwing
   * `TestFailedException` is configured by the value contained in the passed
   * `timeout` parameter.
   * The interval to sleep between attempts is configured by the value contained in the passed
   * `interval` parameter.
   * 
   *
   * @tparam result type of the by-name parameter `fun`
   * @param timeout the `Timeout` configuration parameter
   * @param interval the `Interval` configuration parameter
   * @param fun the by-name parameter to repeatedly invoke
   * @return the result of invoking the `fun` by-name parameter, the first time it succeeds
   */
  def eventually[T](timeout: Timeout, interval: Interval)(fun: => T)(implicit pos: source.Position): T =
    eventually(fun)(PatienceConfig(timeout.value, interval.value), pos)

  /**
   * Invokes the passed by-name parameter repeatedly until it either succeeds, or a configured maximum
   * amount of time has passed, sleeping a configured interval between attempts.
   *
   * The by-name parameter "succeeds" if it returns a result. It "fails" if it throws any exception that
   * would normally cause a test to fail. (These are any exceptions except <a href="TestPendingException">`TestPendingException`</a> and
   * `Error`s listed in the
   * <a href="Suite.html#errorHandling">Treatment of `java.lang.Error`s</a> section of the
   * documentation of trait `Suite`.)
   * 
   *
   * The maximum amount of time in milliseconds to tolerate unsuccessful attempts before giving up and throwing
   * `TestFailedException` is configured by the value contained in the passed
   * `timeout` parameter.
   * The interval to sleep between attempts is configured by the `interval` field of
   * the `PatienceConfig` passed implicitly as the last parameter.
   * 
   *
   * @param timeout the `Timeout` configuration parameter
   * @param fun the by-name parameter to repeatedly invoke
   * @param config the `PatienceConfig` object containing the (unused) `timeout` and
   *          (used) `interval` parameters
   * @return the result of invoking the `fun` by-name parameter, the first time it succeeds
   */
  def eventually[T](timeout: Timeout)(fun: => T)(implicit config: PatienceConfig, pos: source.Position): T =
    eventually(fun)(PatienceConfig(timeout.value, config.interval), pos)

  /**
   * Invokes the passed by-name parameter repeatedly until it either succeeds, or a configured maximum
   * amount of time has passed, sleeping a configured interval between attempts.
   *
   * The by-name parameter "succeeds" if it returns a result. It "fails" if it throws any exception that
   * would normally cause a test to fail. (These are any exceptions except <a href="TestPendingException">`TestPendingException`</a> and
   * `Error`s listed in the
   * <a href="Suite.html#errorHandling">Treatment of `java.lang.Error`s</a> section of the
   * documentation of trait `Suite`.)
   * 
   *
   * The maximum amount of time in milliseconds to tolerate unsuccessful attempts before giving up is configured by the `timeout` field of
   * the `PatienceConfig` passed implicitly as the last parameter.
   * The interval to sleep between attempts is configured by the value contained in the passed
   * `interval` parameter.
   * 
   *
   * @param interval the `Interval` configuration parameter
   * @param fun the by-name parameter to repeatedly invoke
   * @param config the `PatienceConfig` object containing the (used) `timeout` and
   *          (unused) `interval` parameters
   * @return the result of invoking the `fun` by-name parameter, the first time it succeeds
   */
  def eventually[T](interval: Interval)(fun: => T)(implicit config: PatienceConfig, pos: source.Position): T =
    eventually(fun)(PatienceConfig(config.timeout, interval.value), pos)

  /**
   * Invokes the passed by-name parameter repeatedly until it either succeeds, or a configured maximum
   * amount of time has passed, sleeping a configured interval between attempts.
   *
   * The by-name parameter "succeeds" if it returns a result. It "fails" if it throws any exception that
   * would normally cause a test to fail. (These are any exceptions except <a href="TestPendingException">`TestPendingException`</a> and
   * `Error`s listed in the
   * <a href="Suite.html#errorHandling">Treatment of `java.lang.Error`s</a> section of the
   * documentation of trait `Suite`.)
   * 
   *
   * The maximum amount of time in milliseconds to tolerate unsuccessful attempts before giving up is configured by the `timeout` field of
   * the `PatienceConfig` passed implicitly as the last parameter.
   * The interval to sleep between attempts is configured by the `interval` field of
   * the `PatienceConfig` passed implicitly as the last parameter.
   * 
   *
   * @param fun the by-name parameter to repeatedly invoke
   * @param config the `PatienceConfig` object containing the `timeout` and
   *          `interval` parameters
   * @return the result of invoking the `fun` by-name parameter, the first time it succeeds
   */
  def eventually[T](fun: => T)(implicit config: PatienceConfig, pos: source.Position): T = {
    val startNanos = System.nanoTime
    def makeAValiantAttempt(): Either[Throwable, T] = {
      try {
        Right(fun)
      }
      catch {
        case tpe: TestPendingException => throw tpe
        case e: Throwable if !anExceptionThatShouldCauseAnAbort(e) => Left(e)
      }
    }

    val initialInterval = Span(config.interval.totalNanos * 0.1, Nanoseconds) // config.interval scaledBy 0.1

    @tailrec
    def tryTryAgain(attempt: Int): T = {
      val timeout = config.timeout
      val interval = config.interval
      makeAValiantAttempt() match {
        case Right(result) => result
        case Left(e) => 
          val duration = System.nanoTime - startNanos
          if (duration < timeout.totalNanos) {
            if (duration < interval.totalNanos) // For first interval, we wake up every 1/10 of the interval.  This is mainly for optimization purpose. 
              Thread.sleep(initialInterval.millisPart, initialInterval.nanosPart)
            else
              Thread.sleep(interval.millisPart, interval.nanosPart)
          }
          else {
            val durationSpan = Span(1, Nanosecond) scaledBy duration // Use scaledBy to get pretty units
            throw new TestFailedDueToTimeoutException(
              (_: StackDepthException) =>
                Some(
                  if (e.getMessage == null)
                    Resources.didNotEventuallySucceed(attempt.toString, durationSpan.prettyString)
                  else
                    Resources.didNotEventuallySucceedBecause(attempt.toString, durationSpan.prettyString, e.getMessage)
                ),
              Some(e),
              Left(pos),
              None,
              config.timeout
            )
          }

          tryTryAgain(attempt + 1)
      }
    }
    tryTryAgain(1)
  }
}

/**
 * Companion object that facilitates the importing of `Eventually` members as 
 * an alternative to mixing in the trait. One use case is to import `Eventually`'s members so you can use
 * them in the Scala interpreter:
 *
 * {{{  <!-- class="stREPL" -->
 * $ scala -cp scalatest-1.8.jar
 * Welcome to Scala version 2.9.1.final (Java HotSpot(TM) 64-Bit Server VM, Java 1.6.0_29).
 * Type in expressions to have them evaluated.
 * Type :help for more information.
 *
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; import Matchers._
 * import Matchers._
 *
 * scala&gt; import concurrent.Eventually._
 * import concurrent.Eventually._
 *
 * scala&gt; val xs = 1 to 125
 * xs: scala.collection.immutable.Range.Inclusive = Range(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, ..., 125)
 * 
 * scala&gt; val it = xs.iterator
 * it: Iterator[Int] = non-empty iterator
 *
 * scala&gt; eventually { it.next should be (3) }
 *
 * scala&gt; eventually { Thread.sleep(999); it.next should be (3) }
 * org.scalatest.TestFailedException: The code passed to eventually never returned normally.
 *     Attempted 2 times, sleeping 10 milliseconds between each attempt.
 *   at org.scalatest.Eventually$class.tryTryAgain$1(Eventually.scala:313)
 *   at org.scalatest.Eventually$class.eventually(Eventually.scala:322)
 *   ...
 * }}}
 */
object Eventually extends Eventually
