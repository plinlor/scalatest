/*
 * Copyright 2001-2014 Artima, Inc.
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
  * Trait that when mixed into a <a href="AsyncTestSuite.html">`fixture.AsyncTestSuite`</a> passes
  * the config map passed to `runTest` as a fixture into each test.
  *
  * Here's an example in which tests just check to make sure `"hello"` and `"world"`
  * are defined keys in the config map:
  * 
  *
  * {{{  <!-- class="stHighlight" -->
  * package org.scalatest.examples.fixture.configmapfixture
  *
  * import org.scalatest._
  *
  * class ExampleAsyncSpec extends fixture.AsyncFlatSpec with fixture.AsyncConfigMapFixture with Matchers {
  *
  *   "The config map" should "contain hello" in { configMap =&gt;
  *     // Use the configMap passed to runTest in the test
  *     configMap should contain key "hello"
  *   }
  *
  *   it should "contain world" in { configMap =&gt;
  *     configMap should contain key "world"
  *   }
  * }
  * }}}
  *
  * If you run this class without defining `"hello"` and `"world"`
  * in the confg map, the tests will fail:
  * 
  *
  * {{{  <!-- class="stREPL" -->
  * scala&gt; org.scalatest.run(new ExampleSpec)
  * <span class="stGreen">ExampleSpec:
  * The config map</span>
  * <span class="stRed">- should contain hello *** FAILED ***
  *   Map() did not contain key "hello" (<console>:20)
  * - should contain world *** FAILED ***
  *   Map() did not contain key "world" (<console>:24)</span>
  * }}}
  *
  * If you do define `"hello"` and `"world"` keys
  * in the confg map, the tests will success:
  * 
  *
  * {{{  <!-- class="stREPL" -->
  * scala&gt; org.scalatest.run(new ExampleSpec, configMap = Map("hello" -&gt; "hi", "world" -&gt; "globe"))
  * <span class="stGreen">ExampleSpec:
  * The config map
  * - should contain hello
  * - should contain world</span>
  * }}}
  *
  * @author Bill Venners
  * @author Chee Seng
  */
trait AsyncConfigMapFixture { this: fixture.AsyncTestSuite =>

  /**
    * The type of the `configMap`, which is `ConfigMap`.
    */
  type FixtureParam = ConfigMap

  /**
    * Invoke the test function, passing to the the test function the `configMap`
    * obtained by invoking `configMap` on the passed `OneArgTest`.
    *
    * To enable stacking of traits that define `withFixture(OneArgAsyncTest)`, this method does not
    * invoke the test function directly. Instead, it delegates responsibility for invoking the test function
    * to `withFixture(OneArgAsyncTest)`.
    * 
    *
    * @param test the `OneArgAsyncTest` to invoke, passing in the
    *   `configMap` fixture
    */
  def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    withFixture(test.toNoArgAsyncTest(test.configMap))
  }
}
