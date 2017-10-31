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

import scala.concurrent.Future

// TODO: Scaladoc
/**
 * The base trait of ScalaTest's "fixture" async testing styles, which enable you to pass fixture objects into tests.
 *
 * This trait provides a final override of `withFixture(OneArgTest)`, declared in
 * supertrait `fixture.Suite`, because the `withFixture(OneArgTest)` lifecycle
 * method assumes synchronous testing. Here is its signature:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * def withFixture(test: OneArgTest): Outcome
 * }}}
 *
 * The test function interface, <a href="Suite$OneArgTest.html">`OneArgTest`</a>, offers an `apply` method
 * that takes a `FixtureParam` and returns <a href="Outcome.html">`Outcome`</a>:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // In trait OneArgTest:
 * def apply(fixture: FixtureParam): Outcome
 * }}}
 *
 * Because the result of a test is an `Outcome`, when the test function returns, the test body must have determined an outcome already. It
 * will already be one of <a href="../Succeeded$.html">`Succeeded`</a>, <a href="../Failed.html">`Failed`</a>, <a href="../Canceled.html">`Canceled`</a>, or <a href="../Pending$.html">`Pending`</a>. This is
 * also true when `withFixture(OneArgTest)` returns: because the result type of `withFixture(OneArgTest)` is `Outcome`,
 * the test body has by definition has already finished execution.
 * 
 *
 * This trait overrides and makes abstract the `runTest` method. Subtraits must 
 * must implement this method to call `withFixture(OneArgAsyncTest)` instead of `withFixture(OneArgTest)`,
 * where `withFixture(OneArgAsyncTest)` is a new method declared in this trait with the following
 * signature and implementation:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * def withFixture(test: OneArgAsyncTest): FutureOutcome = {
 *   test()
 * }
 * }}}
 *
 * Instead of returning `Outcome` like `withFixture`, the `withFixture` method
 * returns a `FutureOutcome`. Similarly, the `apply` method of test function interface,
 * `OneArgAsyncTest`, returns `FutureOutcome`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // In trait OneArgAsyncTest:
 * def apply(fixture: FixtureParam): FutureOutcome
 * }}}
 *
 * The `withFixture` method supports async testing, because when the test function returns,
 * the test body has not necessarily finished execution.
 * 
 *
 * The recommended way to ensure cleanup is performed after a test body finishes execution is
 * to use the `complete`-`lastly` syntax, defined in supertrait
 * <a href="../CompleteLastly.html">`org.scalatest.CompleteLastly`</a>, which will ensure that
 * cleanup will occur whether future-producing code completes abruptly by throwing an exception, or returns
 * normally yielding a future. In the latter case, `complete`-`lastly` will register the cleanup code
 * to execute asynchronously when the future completes.
 * 
 *
 * To enable the stacking of traits that define `withFixture(NoArgAsyncTest)`, it is a good idea to let
 * `withFixture(NoArgAsyncTest)` invoke the test function instead of invoking the test
 * function directly. To do so, you'll need to convert the `OneArgAsyncTest` to a `NoArgAsyncTest`. You can do that by passing
 * the fixture object to the `toNoArgAsyncTest` method of `OneArgAsyncTest`. In other words, instead of
 * writing &ldquo;`test(theFixture)`&rdquo;, you'd delegate responsibility for
 * invoking the test function to the `withFixture(NoArgAsyncTest)` method of the same instance by writing:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * withFixture(test.toNoArgAsyncTest(theFixture))
 * }}}
 *
 * Thus, the recommended structure of a `withFixture` implementation that performs cleanup looks like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: OneArgAsyncTest) = {
 *
 *   // Perform setup here
 *   val theFixture = ...
 *
 *   complete {
 *     withFixture(test.toNoArgAsyncTest(theFixture)) // Invoke the test function
 *   } lastly {
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
 * override def withFixture(test: OneArgAsyncTest) = {
 *
 *   // Perform setup here
 *   val theFixture = ...
 *
 *   withFixture(test.toNoArgAsyncTest(theFixture)) // Invoke the test function
 * }
 * }}}
 *
 * If you want to perform an action only for certain outcomes, you'll need to 
 * register code performing that action as a callback on the `Future` using
 * one of `Future` registration methods: `onComplete`, `onSuccess`,
 * or `onFailure`. Note that if a test fails, that will be treated as a
 * `scala.util.Success(org.scalatest.Failure)`. So if you want to perform an 
 * action if a test fails, for example, you'd register the callaback using `onSuccess`,
 * like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: OneArgAsyncTest) = {
 *
 *   // Perform setup here
 *   val theFixture = ...
 *
 *   val futureOutcome =
 *       withFixture(test.toNoArgAsyncTest(theFixture)) // Invoke the test function
 *
 *   futureOutcome onFailedThen { _ =&gt;
 *     // perform action that you want to occur
 *     // only if a test fails here
 *   }
 * }
 * }}}
 *
 * Lastly, if you want to transform the outcome in some way in `withFixture`, you'll need to use either the
 * `map` or `transform` methods of `Future`, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: OneArgAsyncTest) = {
 *
 *   // Perform setup here
 *   val theFixture = ...
 *
 *   val futureOutcome =
 *       withFixture(test.toNoArgAsyncTest(theFixture)) // Invoke the test function
 *
 *   futureOutcome change { outcome =&gt;
 *     // transform the outcome into a new outcome here
 *   }
 * }
 * }}}
 * 
 * Note that a `NoArgAsyncTest`'s `apply` method will only return a `Failure` if
 * the test completes abruptly with an exception (such as `OutOfMemoryError`) that should
 * cause the suite to abort rather than the test to fail. Thus usually you would use `map`
 * to transform future outcomes, not `transform`, so that such suite-aborting exceptions pass through
 * unchanged. The suite will abort asynchronously with any exception returned in a `Failure`.
 * 
 */
trait AsyncTestSuite extends org.scalatest.fixture.Suite with org.scalatest.AsyncTestSuite {

  /**
   * Transform the test outcome, `Registration` type to `AsyncOutcome`.
   *
   * @param testFun test function
   * @return function that returns `AsyncOutcome`
   */
  private[scalatest] def transformToOutcome(testFun: FixtureParam => Future[compatible.Assertion]): FixtureParam => AsyncOutcome =
    (fixture: FixtureParam) => {
      val futureUnit = testFun(fixture)
      InternalFutureOutcome(
        futureUnit.map(u => Succeeded).recover {
          case ex: exceptions.TestCanceledException => Canceled(ex)
          case _: exceptions.TestPendingException => Pending
          case tfe: exceptions.TestFailedException => Failed(tfe)
          case ex: Throwable if !Suite.anExceptionThatShouldCauseAnAbort(ex) => Failed(ex)
        }
      )
    }

  /**
   * A test function taking no arguments and returning an `FutureOutcome`.
   *
   * For more detail and examples, see the relevant section in the
   * <a href="AsyncFlatSpec.html#withFixtureNoArgAsyncTest">documentation for trait `fixture.AsyncFlatSpec`</a>.
   * 
   */
  trait OneArgAsyncTest extends (FixtureParam => FutureOutcome) with TestData { thisOneArgAsyncTest =>

    /**
     * Using the passed `FixtureParam`, produces a `FutureOutcome` representing
     * the future outcome of this asynchronous test.
     *
     * @param fixture the `FixtureParam`
     * @return an instance of `FutureOutcome`
     */
    def apply(fixture: FixtureParam): FutureOutcome

    /**
     * Convert this `OneArgAsyncTest` to a `NoArgAsyncTest` whose
     * `name` and `configMap` methods return the same values
     * as this `OneArgAsyncTest`, and whose `apply` method invokes
     * this `OneArgAsyncTest`'s apply method,
     * passing in the given `fixture`.
     *
     * This method makes it easier to invoke the `withFixture` method
     * that takes a `NoArgAsyncTest`.
     * Here's how that might look in a `fixture.AsyncTestSuite`
     * whose `FixtureParam` is `StringBuilder`:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * def withFixture(test: OneArgAsyncTest) = {
     *   withFixture(test.toNoArgAsyncTest(new StringBuilder))
     * }
     * }}}
     *
     * Invoking this method has no side effect. It just returns a `NoArgAsyncTest` whose
     * `apply` method invokes `apply` on this `OneArgAsyncTest`, passing
     * in the `FixtureParam` passed to `toNoArgAsyncTest`.
     * 
     *
     * @param fixture the `FixtureParam`
     * @return an new instance of `NoArgAsyncTest`
     */
    def toNoArgAsyncTest(fixture: FixtureParam): NoArgAsyncTest = 
      new NoArgAsyncTest {
        val name = thisOneArgAsyncTest.name
        val configMap = thisOneArgAsyncTest.configMap
        def apply(): FutureOutcome = { thisOneArgAsyncTest(fixture) }
        val scopes = thisOneArgAsyncTest.scopes
        val text = thisOneArgAsyncTest.text
        val tags = thisOneArgAsyncTest.tags
        val pos = thisOneArgAsyncTest.pos
      }
  }

  /**
   *  Run the passed test function with a fixture created by this method.
   *
   * This method should create the fixture object needed by the tests of the
   * current suite, invoke the test function (passing in the fixture object),
   * and if needed, register any clean up needed after the test completes as
   * a callback on the `FutureOutcome` returned by the test function.
   * For more detail and examples, see the
   * <a href="AsyncTestSuite.html">main documentation for this trait</a>.
   * 
   *
   * @param test the `OneArgAsyncTest` to invoke, passing in a fixture
   * @return an instance of `FutureOutcome`
   */
  def withFixture(test: OneArgAsyncTest): FutureOutcome
}
