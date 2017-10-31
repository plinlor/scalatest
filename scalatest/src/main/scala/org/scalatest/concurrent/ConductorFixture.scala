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
import org.scalatest.fixture
import org.scalatest.OutcomeOf.outcomeOf

/**
 * Trait that can pass a new `Conductor` fixture into tests.
 *
 * Here's an example of the use of this trait to test the `ArrayBlockingQueue`
 * class from `java.util.concurrent`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.fixture
 * import org.scalatest.concurrent.ConductorFixture
 * import org.scalatest.matchers.Matchers
 * import java.util.concurrent.ArrayBlockingQueue
 *
 * class ArrayBlockingQueueSuite extends fixture.FunSuite with ConductorFixture with Matchers {
 * 
 *   test("calling put on a full queue blocks the producer thread") { conductor =&gt; import conductor._
 *
 *     val buf = new ArrayBlockingQueue[Int](1)
 * 
 *     thread("producer") {
 *       buf put 42
 *       buf put 17
 *       beat should be (1)
 *     }
 * 
 *     thread("consumer") {
 *       waitForBeat(1)
 *       buf.take should be (42)
 *       buf.take should be (17)
 *     }
 * 
 *     whenFinished {
 *       buf should be ('empty)
 *     }
 *   }
 *
 *   test("calling take on an empty queue blocks the consumer thread") { conductor =&gt; import conductor._
 *
 *     val buf = new ArrayBlockingQueue[Int](1)
 *
 *     thread("producer") {
 *       waitForBeat(1)
 *       buf put 42
 *       buf put 17
 *     }
 *
 *     thread("consumer") {
 *       buf.take should be (42)
 *       buf.take should be (17)
 *       beat should be (1)
 *     }
 *
 *     whenFinished {
 *       buf should be ('empty)
 *     }
 *   }
 * }
 * }}}
 *
 * For an explanation of how these tests work, see the documentation for <a href="Conductors.html">`Conductors`</a>.
 * 
 *
 * @author Bill Venners
 */
trait ConductorFixture extends TestSuiteMixin with Conductors { this: fixture.TestSuite =>

  /**
   * Defines type `Fixture` to be `Conductor`.
   */
  type FixtureParam = Conductor
  
  /**
   * Creates a new `Conductor`, passes the `Conductor` to the
   * specified test function, and ensures that `conduct` gets invoked
   * on the `Conductor`.
   *
   * After the test function returns (so long as it returns normally and doesn't
   * complete abruptly with an exception), this method will determine whether the
   * `conduct` method has already been called (by invoking
   * `conductingHasBegun` on the `Conductor`). If not,
   * this method will invoke `conduct` to ensure that the
   * multi-threaded scenario is actually conducted.
   * 
   *
   * This trait is stackable with other traits that override `withFixture(NoArgTest)`, because
   * instead of invoking the test function directly, it delegates responsibility for invoking the test
   * function to `withFixture(NoArgTest)`.
   * 
   */
  def withFixture(test: OneArgTest): Outcome = {
    val conductor = new Conductor
    withFixture(test.toNoArgTest(conductor)) match {
      case Succeeded if !conductor.conductingHasBegun =>
        outcomeOf { conductor.conduct() }
      case other => other
    }
  }
}
