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
package org.scalatest

import org.scalactic._

/**
 * The base trait of ScalaTest's ''synchronous testing styles'', which defines a 
 * `withFixture` lifecycle method that accepts as its parameter a test function
 * that returns an <a href="Outcome.html">`Outcome`</a>.
 *
 * The `withFixture` method add by this trait has the 
 * following signature and implementation:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * def withFixture(test: NoArgTest): Outcome = {
 *   test()
 * }
 * }}}
 *
 * The `apply` method of test function interface,
 * `NoArgTest`, also returns `Outcome`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // In trait NoArgTest:
 * def apply(): Outcome
 * }}}
 *
 * Because the result of a test is an `Outcome`, when the test function returns, the test body must have determined an outcome already. It
 * will already be one of <a href="Succeeded$.html">`Succeeded`</a>, <a href="Failed.html">`Failed`</a>, <a href="Canceled.html">`Canceled`</a>, or <a href="Pending$.html">`Pending`</a>. This is
 * also true when `withFixture(NoArgTest)` returns: because the result type of `withFixture(NoArgTest)` is `Outcome`,
 * the test has by definition already finished execution.
 * 
 *
 * The recommended way to ensure cleanup is performed after a test body finishes execution is
 * to use a `try`-`finally` clause.
 * Using `try`-`finally` will ensure that cleanup will occur whether
 * the test function completes abruptly by throwing a suite-aborting exception, or returns
 * normally yielding an `Outcome`. Note that the only situation in which a test function
 * will complete abruptly with an exception is if the test body throws a suite-aborting exception.
 * Any other exception will be caught and reported as either a `Failed`, `Canceled`,
 * or `Pending`.
 * 
 *
 * The `withFixture` method is designed to be stacked, and to enable this, you should always call the `super` implementation
 * of `withFixture`, and let it invoke the test function rather than invoking the test function directly. In other words, instead of writing
 * &ldquo;`test()`&rdquo;, you should write &ldquo;`super.withFixture(test)`&rdquo;. Thus, the recommended
 * structure of a `withFixture` implementation that performs cleanup looks like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgTest) = {
 *   // Perform setup here
 *   try {
 *     super.withFixture(test) // Invoke the test function
 *   } finally {
 *     // Perform cleanup here
 *   }
 * }
 * }}}
 *
 * If you have no cleanup to perform, you can write `withFixture` like this instead:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgTest) = {
 *   // Perform setup here
 *   super.withFixture(test) // Invoke the test function
 * }
 * }}}
 *
 * If you want to perform an action only for certain outcomes, you can use
 * a pattern match.
 * For example, if you want to perform an action if a test fails, you'd 
 * match on `Failed`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgTest) = {
 *
 *   // Perform setup here
 *
 *   val outcome = super.withFixture(test) // Invoke the test function
 *
 *   outcome match {
 *     case failed: Failed =>
 *       // perform action that you want to occur
 *       // only if a test fails here
 *       failed
 *     case other => other
 *   }
 * }
 * }}}
 *
 * If you want to change the outcome in some way in `withFixture`, you can also
 * use a pattern match.
 * For example, if a particular exception intermittently causes a test to fail, and can
 * transform those failures into cancelations, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgTest) = {
 *
 *   super.withFixture(test) match {
 *     case Failed(ex: ParticularException) =>
 *       Canceled("Muting flicker", ex)
 *     case other => other
 *   }
 * }
 * }}}
 */
trait TestSuite extends Suite { thisTestSuite =>

  /**
   * A test function taking no arguments and returning an `Outcome`.
   *
   * For more detail and examples, see the relevant section in the 
   * <a href="FlatSpec.html#withFixtureNoArgTest">documentation for trait `fixture.FlatSpec`</a>.
   * 
   */
  protected trait NoArgTest extends (() => Outcome) with TestData {

    /**
     * Runs the body of the test, returning an `Outcome`.
     */
    def apply(): Outcome
  }

  // Keep this out of the public until there's a use case demonstrating its need
  private[scalatest] object NoArgTest {
    def apply(test: NoArgTest)(f: => Outcome): NoArgTest = {
      new NoArgTest {
        def apply(): Outcome = { f }
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
   * Run the passed test function in the context of a fixture established by this method.
   *
   * This method should set up the fixture needed by the tests of the
   * current suite, invoke the test function, and if needed, perform any clean
   * up needed after the test completes. Because the `NoArgTest` function
   * passed to this method takes no parameters, preparing the fixture will require
   * side effects, such as reassigning instance `var`s in this `Suite` or initializing
   * a globally accessible external database. If you want to avoid reassigning instance `var`s
   * you can use <a href="fixture/Suite.html">fixture.Suite</a>.
   * 
   *
   * This trait's implementation of `runTest` invokes this method for each test, passing
   * in a `NoArgTest` whose `apply` method will execute the code of the test.
   * 
   *
   * This trait's implementation of this method simply invokes the passed `NoArgTest` function.
   * 
   *
   * @param test the no-arg test function to run with a fixture
   */
  protected def withFixture(test: NoArgTest): Outcome = {
    test()
  }

  /**
   * Run an async test.
   *
   * This method is redefine in this trait solely to narrow its contract. Subclasses must implement
   * this method to call the `withFixture(NoArgTest)` method, which is defined in this trait.
   * 
   *
   * This trait's implementation of this method simply returns `SucceededStatus` 
   * and has no other effect.
   * 
   *
   * @param testName the name of one async test to execute.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method
   *     has completed, and whether or not it failed.
   *
   * @throws NullArgumentException if either `testName` or `args`
   *     is `null`.
   */
  protected override def runTest(testName: String, args: Args): Status = SucceededStatus
}

