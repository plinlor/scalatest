/*
 * Copyright 2001-2016 Artima, Inc.
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

import org.scalatest.exceptions.TimeoutField
import org.scalatest.time.Span
import org.scalatest._

/**
 * Trait that when mixed into an asynchronous suite class establishes a time limit for its tests.
 *
 * This trait overrides `withFixture`, wrapping a `super.withFixture(test)` call
 * in a `failAfter` invocation, specifying a timeout obtained by invoking `timeLimit`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * failAfter(timeLimit) {
 *   super.withFixture(test)
 * }
 * }}}
 *
 * Note that the `failAfter` method executes the body of the by-name passed to it using the same
 * thread that invoked `failAfter`. This means that the calling of `withFixture` method
 * will be run using the same thread, but the test body may be run using a different thread, depending on the
 * `executionContext` set at the `AsyncTestSuite` level.
 * 
 *
 * The `timeLimit` field is abstract in this trait. Thus you must specify a time limit when you use it.
 * For example, the following code specifies that each test must complete within 200 milliseconds:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.AsyncFunSpec
 * import org.scalatest.concurrent.AsyncTimeLimitedTests
 * import org.scalatest.time.SpanSugar._
 *
 * class ExampleSpec extends AsyncFunSpec with AsyncTimeLimitedTests {
 *
 *   // Note: You may need to either write 200.millis or (200 millis), or
 *   // place a semicolon or blank line after plain old 200 millis, to
 *   // avoid the semicolon inference problems of postfix operator notation.
 *   val timeLimit = 200 millis
 *
 *   describe("An asynchronous time-limited test") {
 *     it("should succeed if it completes within the time limit") {
 *       Thread.sleep(100)
 *       succeed
 *     }
 *     it("should fail if it is taking too darn long") {
 *       Thread.sleep(300)
 *       succeed
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
 * Different from <a href="TimeLimitedTests.html">`TimeLimitedTests`</a>, `AsyncTimeLimitedTests` does not
 * support `Interruptor` for now.
 * 
 *
 * @author Bill Venners
 * @author Chua Chee Seng
 */
trait AsyncTimeLimitedTests extends AsyncTestSuiteMixin with TimeLimits { this: AsyncTestSuite =>

  /**
    * A stackable implementation of `withFixture` that wraps a call to `super.withFixture` in a
    * `failAfter` invocation.
    *
    * @param test the test on which to enforce a time limit
    */
  abstract override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    try {
      failAfter(timeLimit) {
        super.withFixture(test)
      } change { outcome =>
        outcome match {
          case Exceptional(e: org.scalatest.exceptions.ModifiableMessage[_] with TimeoutField) =>
            Exceptional(e.modifyMessage(opts => Some(Resources.testTimeLimitExceeded(e.timeout.prettyString))))
          case other => other
        }
      }
    }
    catch {
      case e: org.scalatest.exceptions.ModifiableMessage[_] with TimeoutField =>
        throw e.modifyMessage(opts => Some(Resources.testTimeLimitExceeded(e.timeout.prettyString)))
      case other: Throwable => throw other
    }
  }

  /**
   * The time limit, in [[org.scalatest.time.Span `Span`]], in which each test in a `AsyncTestSuite` that mixes in
   * `AsyncTimeLimitedTests` must complete.
   */
  def timeLimit: Span
}
