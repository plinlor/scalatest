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
import OutcomeOf.outcomeOf
import org.scalactic._

trait TestSuite extends org.scalatest.fixture.Suite with org.scalatest.TestSuite { thisTestSuite =>

  /**
   * A test function taking a fixture parameter and returning an `Outcome`.
   *
   * For more detail and examples, see the
   * <a href="FlatSpec.html">documentation for trait `fixture.FlatSpec`</a>.
   * 
   */
  protected trait OneArgTest extends (FixtureParam => Outcome) with TestData { thisOneArgTest =>

    /**
     * Runs the test, using the passed `FixtureParam`.
     *
     * @param fixture the `FixtureParam`
     * @return an instance of `Outcome`
     */
    def apply(fixture: FixtureParam): Outcome

    /**
     * Convert this `OneArgTest` to a `NoArgTest` whose
     * `name` and `configMap` methods return the same values
     * as this `OneArgTest`, and whose `apply` method invokes
     * this `OneArgTest`'s apply method,
     * passing in the given `fixture`.
     *
     * This method makes it easier to invoke the `withFixture` method
     * that takes a `NoArgTest`. For example, if a `fixture.Suite` 
     * mixes in `SeveredStackTraces`, it will inherit an implementation
     * of `withFixture(NoArgTest)` provided by
     * `SeveredStackTraces` that implements the stack trace severing
     * behavior. If the `fixture.Suite` does not delegate to that
     * `withFixture(NoArgTest)` method, the stack trace severing behavior
     * will not happen. Here's how that might look in a `fixture.Suite`
     * whose `FixtureParam` is `StringBuilder`:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * def withFixture(test: OneArgTest) = {
     *   withFixture(test.toNoArgTest(new StringBuilder))
     * }
     * }}}
     *
     * Invoking this method has no side effect. It just returns a `NoArgTest` whose
     * `apply` method invokes `apply` on this `OneArgTest`, passing
     * in the `FixtureParam` passed to `toNoArgTest`.
     * 
     *
     * @param fixture the `FixtureParam`
     * @return an new instance of `NoArgTest`
     */
    def toNoArgTest(fixture: FixtureParam) = 
      new NoArgTest {
        val name = thisOneArgTest.name
        val configMap = thisOneArgTest.configMap
        def apply(): Outcome = { thisOneArgTest(fixture) }
        val scopes = thisOneArgTest.scopes
        val text = thisOneArgTest.text
        val tags = thisOneArgTest.tags
        val pos = thisOneArgTest.pos
      }
  }

  /**
   * Companion object for `OneArgTest` that provides factory method to create new `OneArgTest`
   * instance by passing in a `OneArgTest` and a `FixtureParam` => `Outcome` function.
   */
  object OneArgTest {
    /**
     * Create new `OneArgTest` instance.
     *
     * @param test a `OneArgTest`
     * @param f a `FixtureParam` => `Outcome` function
     * @return a new instance of `OneArgTest`, which will call the passed `f` function in its `apply` method
     */
    def apply(test: OneArgTest)(f: FixtureParam => Outcome): OneArgTest = {
      new OneArgTest {
        def apply(fixture: FixtureParam): Outcome = { f(fixture) }
        val text: String = test.text
        val configMap: ConfigMap = test.configMap
        val scopes: collection.immutable.IndexedSeq[String] = test.scopes
        val name: String = test.name
        val tags: Set[String] = test.tags
        val pos: Option[source.Position] = test.pos
      }
    }
  }

  /**
   *  Run the passed test function with a fixture created by this method.
   *
   * This method should create the fixture object needed by the tests of the
   * current suite, invoke the test function (passing in the fixture object),
   * and if needed, perform any clean up needed after the test completes.
   * For more detail and examples, see the <a href="Suite.html">main documentation for this trait</a>.
   * 
   *
   * @param test the `OneArgTest` to invoke, passing in a fixture
   * @return an instance of `Outcome`
   */
  protected def withFixture(test: OneArgTest): Outcome

  private[fixture] class TestFunAndConfigMap(val name: String, test: FixtureParam => Any, val configMap: ConfigMap)
    extends OneArgTest {

    def apply(fixture: FixtureParam): Outcome = {
      outcomeOf { test(fixture) }
    }
    private val testData = testDataFor(name, configMap)
    val scopes = testData.scopes
    val text = testData.text
    val tags = testData.tags
    val pos = testData.pos
  }

  private[fixture] class FixturelessTestFunAndConfigMap(override val name: String, test: () => Any, override val configMap: ConfigMap)
    extends NoArgTest {

    def apply(): Outcome = {
      outcomeOf { test() }
    }
    private val testData = testDataFor(name, configMap)
    val scopes = testData.scopes
    val text = testData.text
    val tags = testData.tags
    val pos = testData.pos
  }

}

