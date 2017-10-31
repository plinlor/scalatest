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
import Timeouts._
import org.scalatest.Resources
import org.scalatest.time.Span
import org.scalatest.exceptions.TimeoutField
import org.scalatest.Outcome
import org.scalatest.Exceptional

/**
 * Trait that when mixed into a suite class establishes a time limit for its tests.
 *
 * This trait overrides `withFixture`, wrapping a `super.withFixture(test)` call
 * in a `failAfter` invocation, specifying a timeout obtained by invoking `timeLimit`
 * and an <a href="Interruptor.html">`Interruptor`</a> by invoking `defaultTestInterruptor`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * failAfter(timeLimit) {
 *   super.withFixture(test)
 * } (defaultTestInterruptor)
 * }}}
 *
 * Note that the `failAfter` method executes the body of the by-name passed to it using the same
 * thread that invoked `failAfter`. This means that the same thread will run the `withFixture` method
 * as well as each test, so no extra synchronization is required. A second thread is used to run a timer, and if the timeout
 * expires, that second thread will attempt to interrupt the main test thread via the `defaultTestInterruptor`.
 * 
 * 
 * The `timeLimit` field is abstract in this trait. Thus you must specify a time limit when you use it.
 * For example, the following code specifies that each test must complete within 200 milliseconds:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.FunSpec
 * import org.scalatest.concurrent.DeprecatedTimeLimitedTests
 * import org.scalatest.time.SpanSugar._
 * 
 * class ExampleSpec extends FunSpec with DeprecatedTimeLimitedTests {
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
 * The `failAfter` method uses an `Interruptor` to attempt to interrupt the main test thread if the timeout
 * expires. The default `Interruptor` returned by the `defaultTestInterruptor` method is a
 * <a href="ThreadInterruptor$.html">`ThreadInterruptor`</a>, which calls `interrupt` on the main test thread. If you wish to change this
 * interruption strategy, override `defaultTestInterruptor` to return a different `Interruptor`. For example,
 * here's how you'd change the default to <a href="DoNotInterrupt$.html">`DoNotInterrupt`</a>, a very patient interruption strategy that does nothing to
 * interrupt the main test thread:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.FunSpec
 * import org.scalatest.concurrent.DeprecatedTimeLimitedTests
 * import org.scalatest.time.SpanSugar._
 * 
 * class ExampleSpec extends FunSpec with DeprecatedTimeLimitedTests {
 * 
 *   val timeLimit = 200 millis
 * 
 *   override val defaultTestInterruptor = DoNotInterrupt
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
 * Like the previous incarnation of `ExampleSuite`, the second test will fail with an error message that indicates
 * a timeout expired. But whereas in the previous case, the `Thread.sleep` would be interrupted after 200 milliseconds,
 * in this case it is never interrupted. In the previous case, the failed test requires a little over 200 milliseconds to run.
 * In this case, because the `sleep(300)` is never interrupted, the failed test requires a little over 300 milliseconds
 * to run.
 * 
 */
@deprecated("DeprecatedTimeLimitedTests is deprecated and will be removed in a future version of ScalaTest. Please use TimeLimitedTests instead.")
trait DeprecatedTimeLimitedTests extends TestSuiteMixin { this: TestSuite =>

  /**
   * A stackable implementation of `withFixture` that wraps a call to `super.withFixture` in a 
   * `failAfter` invocation.
   * 
   * @param test the test on which to enforce a time limit
   */
  abstract override def withFixture(test: NoArgTest): Outcome = {
    try {
      failAfter(timeLimit) {
        super.withFixture(test)
      } (defaultTestInterruptor)
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
   * `DeprecatedTimeLimitedTests` must complete.
   */
  def timeLimit: Span
  
  /**
   * The default <a href="Interruptor.html">`Interruptor`</a> strategy used to interrupt tests that exceed their time limit.
   * 
   * This trait's implementation of this method returns <a href="ThreadInterruptor$.html">`ThreadInterruptor`</a>, which invokes `interrupt`
   * on the main test thread. Override this method to change the test interruption strategy.
   * 
   * 
   * @return a `ThreadInterruptor`
   */
  val defaultTestInterruptor: Interruptor = ThreadInterruptor
}

/*
Will need to add cancelAfter to the doc comment in 2.0.
*/

