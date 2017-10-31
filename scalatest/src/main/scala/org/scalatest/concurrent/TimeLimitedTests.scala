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

import org.scalatest.TestSuiteMixin
import org.scalatest.TestSuite
import TimeLimits._
import org.scalatest.Resources
import org.scalatest.time.Span
import org.scalatest.exceptions.TimeoutField
import org.scalatest.Outcome
import org.scalatest.Exceptional
import org.scalatest.exceptions.StackDepthExceptionHelper.getStackDepthFun

/**
 * Trait that when mixed into a suite class establishes a time limit for its tests.
 *
 * '''
 * Unfortunately this trait experienced a potentially breaking change in 3.0: previously
 * this trait declared a `defaultTestInterruptor` `val` of type
 * `Interruptor`, in 3.0 that was renamed to `defaultTestSignaler`
 * and given type `Signaler`. The reason is that the default `Interruptor`, `ThreadInterruptor`,
 * did not make sense on Scala.js&#8212;in fact, the entire notion of interruption did not make
 * sense on Scala.js. `Signaler`'s default is `DoNotSignal`, which is a better
 * default on Scala.js, and works fine as a default on the JVM.
 * `Timeouts` was left the same in 3.0, so existing code using it would
 * continue to work as before, but after a deprecation period `Timeouts` will be
 * supplanted by `TimeLimits`, which uses `Signaler`. `TimeLimitedTests`
 * now uses `TimeLimits` instead of `Timeouts`, so if you overrode the default
 * `Interruptor` before, you'll need to change it to the equivalent `Signaler`.
 * And if you were depending on the default being a `ThreadInterruptor`, you'll need to
 * override `defaultTestSignaler` and set it to `ThreadSignaler`.
 * '''
 *
 * This trait overrides `withFixture`, wrapping a `super.withFixture(test)` call
 * in a `failAfter` invocation, specifying a time limit obtained by invoking `timeLimit`
 * and a <a href="Signaler.html">`Signaler`</a> by invoking `defaultTestSignaler`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * failAfter(timeLimit) {
 *   super.withFixture(test)
 * } (defaultTestSignaler)
 * }}}
 *
 * Note that the `failAfter` method executes the body of the by-name passed to it using the same
 * thread that invoked `failAfter`. This means that the same thread will run the `withFixture` method
 * as well as each test, so no extra synchronization is required. A second thread is used to run a timer, and if the timeout
 * expires, that second thread will attempt to signal the main test thread via the `defaultTestSignaler`.
 * 
 * 
 * The `timeLimit` field is abstract in this trait. Thus you must specify a time limit when you use it.
 * For example, the following code specifies that each test must complete within 200 milliseconds:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.FunSpec
 * import org.scalatest.concurrent.TimeLimitedTests
 * import org.scalatest.time.SpanSugar._
 * 
 * class ExampleSpec extends FunSpec with TimeLimitedTests {
 *
 *   // Note: You may need to either write 200.millis or (200 millis), or
 *   // place a semicolon or blank line after plain old 200 millis, to
 *   // avoid the semicolon inference problems of postfix operator notation.
 *   val timeLimit = 200 millis
 *
 *   describe("A time-limited test") {
 *     it("should succeed if it completes within the time limit") {
 *       Thread.sleep(100)
 *     }
 *     it("should fail if it is taking too darn long") {
 *       Thread.sleep(300)
 *     }
 *   }
 * }
 * }}}
 *
 * If you run the above `ExampleSpec`, the second test will fail with the error message: 
 * 
 * 
 * `The test did not complete within the specified 200 millisecond time limit.`
 * 
 * 
 * The `failAfter` method uses an `Signaler` to attempt to signal the main test thread if the timeout
 * expires. The default `Signaler` returned by the `defaultTestSignaler` method is a
 * <a href="DoNotSignal$.html">`DoNotSignal`</a>, which does not signal the main test thread to stop. If you wish to change this
 * signaling strategy, override `defaultTestSignaler` to return a different `Signaler`. For example,
 * here's how you'd change the default to <a href="ThreadSignaler$.html">`ThreadSignaler`</a>, which will
 * interrupt the main test thread when time is up:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.FunSpec
 * import org.scalatest.concurrent.{ThreadSignaler, TimeLimitedTests}
 * import org.scalatest.time.SpanSugar._
 *
 * class ExampleSignalerSpec extends FunSpec with TimeLimitedTests {
 *
 * val timeLimit = 200 millis
 *
 * override val defaultTestSignaler = ThreadSignaler
 *
 * describe("A time-limited test") {
 *     it("should succeed if it completes within the time limit") {
 *       Thread.sleep(100)
 *     }
 *     it("should fail if it is taking too darn long") {
 *       Thread.sleep(300)
 *     }
 *   }
 * }
 * }}}
 * 
 * Like the previous incarnation of `ExampleSuite`, the second test will fail with an error message that indicates
 * a timeout expired. But whereas in the previous case, the `Thread.sleep` would be interrupted after 200 milliseconds,
 * in this case it is never interrupted. In the previous case, the failed test requires a little over 200 milliseconds to run.
 * In this case, because the `sleep(300)` is never interrupted, the failed test requires a little over 300 milliseconds
 * to run.
 * 
 */
trait TimeLimitedTests extends TestSuiteMixin { this: TestSuite =>

  /**
   * A stackable implementation of `withFixture` that wraps a call to `super.withFixture` in a 
   * `failAfter` invocation.
   * 
   * @param test the test on which to enforce a time limit
   */
  abstract override def withFixture(test: NoArgTest): Outcome = {
    try {
      // TODO: should pass in the prettifier also
      failAfterImpl(timeLimit, defaultTestSignaler, org.scalactic.Prettifier.default, test.pos, test.pos.map(getStackDepthFun(_)).getOrElse(getStackDepthFun("TimeLimits.scala", "failAfter", 2))) {
        super.withFixture(test)
      }
    }
    catch {
      case e: org.scalatest.exceptions.ModifiableMessage[_] with TimeoutField => 
        Exceptional(e.modifyMessage(opts => Some(Resources.testTimeLimitExceeded(e.timeout.prettyString))))
      case t: Throwable => 
        Exceptional(t)
    }
  }

  /**
   * The time limit, in milliseconds, in which each test in a `Suite` that mixes in
   * `TimeLimitedTests` must complete.
   */
  def timeLimit: Span
  
  /**
   * The default <a href="Signaler.html">`Signaler`</a> strategy used to interrupt tests that exceed their time limit.
   * 
   * This trait's implementation of this method returns <a href="DoNotSignal$.html">`DoNotSignal`</a>, which does not signal/interrupt
   * the main test and future thread. Override this method to change the test signaling strategy.
   * 
   * 
   * @return a `ThreadInterruptor`
   */
  val defaultTestSignaler: Signaler = DoNotSignal
}

/*
Will need to add cancelAfter to the doc comment in 2.0.
*/

