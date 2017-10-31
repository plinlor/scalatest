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
package org.scalatest.fixture

import org.scalatest._

/**
  * Trait that when mixed into a <a href="AsyncTestSuite.html">`fixture.AsyncTestSuite`</a> passes the
  * <a href="../TestData.html">`TestData`</a> passed to `withFixture` as a fixture into each test.
  *
  * For example, here's how you could access the test's name in each test using `AsyncTestDataFixture`:
  * 
  *
  * {{{  <!-- class="stHighlight" -->
  * package org.scalatest.examples.fixture.testdatafixture
  *
  * import org.scalatest._
  *
  * class ExampleAsyncSpec extends fixture.AsyncFlatSpec with fixture.AsyncTestDataFixture {
  *
  *   "Accessing the test data" should "be easy!" in { td =&gt;
  *     assert(td.name == "Accessing the test data should be easy!")
  *   }
  *
  *   it should "be fun!" in { td =&gt;
  *     assert(td.name == "Accessing the test data should be fun!")
  *   }
  * }
  * }}}
  *
  * @author Bill Venners
  */
trait AsyncTestDataFixture { this: fixture.AsyncTestSuite =>

  /**
    * The type of the fixture, which is `TestData`.
    */
  type FixtureParam = TestData

  /**
    * Invoke the test function, passing to the the test function
    * the `TestData` for the test.
    *
    * To enable stacking of traits that define `withFixture(NoArgTest)`, this method does not
    * invoke the test function directly. Instead, it delegates responsibility for invoking the test function
    * to `withFixture(NoArgTest)`.
    * 
    *
    * @param test the `OneArgTest` to invoke, passing in the
    *   `TestData` fixture
    * @return an `Outcome` instance
    */
  def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    withFixture(test.toNoArgAsyncTest(test))
  }
}
