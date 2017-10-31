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
package org.scalatest

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions // To convert Assertion to Future[Assertion]
import enablers.Futuristic

/*
 * TODO: Fill in here and also add a lifecycle-methods to Suite, which is linked to
 * from SuiteMixin.
 *
 * <p>
 * This trait provides a final override of <code>withFixture(NoArgTest)</code>, declared in
 * supertrait <code>Suite</code>, because the <code>withFixture(NoArgTest)</code> lifecycle
 * method assumes synchronous testing. Here is its signature:
 * </p>
 *
 * <pre class="stHighlight">
 * def withFixture(test: NoArgTest): Outcome
 * </pre>
 *
 * <p>
 * The test function interface, <a href="Suite$NoArgTest.html"><code>NoArgTest</code></a>, offers an <code>apply</code> method
 * that also returns <a href="Outcome.html"><code>Outcome</code></a>:
 * </p>
 *
 * <pre class="stHighlight">
 * // In trait NoArgTest:
 * def apply(): Outcome
 * </pre>
 *
 * <p>
 * Because the result of a test is an <code>Outcome</code>, when the test function returns, the test body must have determined an outcome already. It
 * will already be one of <a href="Succeeded$.html"><code>Succeeded</code></a>, <a href="Failed.html"><code>Failed</code></a>, <a href="Canceled.html"><code>Canceled</code></a>, or <a href="Pending$.html"></code>Pending</code></a>. This is
 * also true when <code>withFixture(NoArgTest)</code> returns: because the result type of <code>withFixture(NoArgTest)</code> is <code>Outcome</code>,
 * the test has by definition already finished execution.
 * </p>
 *
 */

/**
 * The base trait of ScalaTest's ''asynchronous testing styles'', which defines a 
 * `withFixture` lifecycle method that accepts as its parameter a test function
 * that returns a <a href="FutureOutcome.html">`FutureOutcome`</a>.
 *
 * The `withFixture` method add by this trait has the 
 * following signature and implementation:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * def withFixture(test: NoArgAsyncTest): FutureOutcome = {
 *   test()
 * }
 * }}}
 *
 * This trait enables testing of asynchronous code without blocking.  Instead of returning
 * `Outcome` like <a href="TestSuite.html">`TestSuite`</a>'s 
 * `withFixture`, this trait's `withFixture` method returns a
 * `FutureOutcome`. Similarly, the `apply` method of test function interface,
 * `NoArgAsyncTest`, returns `FutureOutcome`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // In trait NoArgAsyncTest:
 * def apply(): FutureOutcome
 * }}}
 *
 * The `withFixture` method supports async testing, because when the test function returns,
 * the test body has not necessarily finished execution.
 * 
 *
 * The recommended way to ensure cleanup is performed after a test body finishes execution is
 * to use a `complete`-`lastly` clause, syntax that is defined in trait
 * <a href="CompleteLastly.html">`CompleteLastly`</a>, which this trait extends.
 * Using `cleanup`-`lastly` will ensure that cleanup will occur whether
 * `FutureOutcome`-producing code completes abruptly by throwing an exception, or returns
 * normally yielding a `FutureOutcome`. In the latter case,
 * `complete`-`lastly` will
 * register the cleanup code to execute asynchronously when the `FutureOutcome` completes.
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
 * override def withFixture(test: NoArgAsyncTest) = {
 *   // Perform setup here
 *   complete {
 *     super.withFixture(test) // Invoke the test function
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
 * override def withFixture(test: NoArgAsyncTest) = {
 *   // Perform setup here
 *   super.withFixture(test) // Invoke the test function
 * }
 * }}}
 *
 * The test function and `withFixture` method returns a
 * <a href="FutureOutcome.html">`FutureOutcome`</a>,
 * a ScalaTest class that wraps a Scala `Future[Outcome]` and offers methods
 * more specific to asynchronous test outcomes. In a Scala `Future`, any exception
 * results in a `scala.util.Failure`. In a `FutureOutcome`, a
 * thrown `TestPendingException` always results in a `Pending`,
 * a thrown `TestCanceledException` always results in a `Canceled`,
 * and any other exception, so long as it isn't suite-aborting, results in a
 * `Failed`. This is true of the asynchronous test code itself that's represented by
 * the `FutureOutcome` and any transformation or callback registered on the
 * `FutureOutcome` in `withFixture`.
 * 
 * 
 * If you want to perform an action only for certain outcomes, you'll need to 
 * register code performing that action on the `FutureOutcome` using
 * one of `FutureOutcome`'s callback registration methods:
 * 
 *
 * <ul>
 * <li>`onSucceededThen` - executed if the `Outcome` is a `Succeeded`.
 * <li>`onFailedThen` - executed if the `Outcome` is a `Failed`.
 * <li>`onCanceledThen` - executed if the `Outcome` is a `Canceled`.
 * <li>`onPendingThen` - executed if the `Outcome` is a `Pending`.
 * <li>`onOutcomeThen` - executed on any `Outcome` (''i.e.'', no
 *        suite-aborting exception is thrown).
 * <li>`onAbortedThen` - executed if a suite-aborting exception is thrown.
 * <li>`onCompletedThen` - executed whether the result is an `Outcome`
 *        or a thrown suite-aborting exception.
 * </ul>
 *
 * For example, if you want to perform an action if a test fails, you'd register the
 * callback using `onFailedThen`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgAsyncTest) = {
 *
 *   // Perform setup here
 *
 *   val futureOutcome = super.withFixture(test) // Invoke the test function
 *
 *   futureOutcome onFailedThen { ex =&gt;
 *     // perform action that you want to occur
 *     // only if a test fails here
 *   }
 * }
 * }}}
 *
 * Note that all callback registration methods, such as `onFailedThen` used in the
 * previous example, return a new `FutureOutcome` that won't complete until the
 * the original `FutureOutcome` ''and the callback'' has completed. If the callback
 * throws an exception, the resulting `FutureOutcome` will represent that exception.
 * For example, if a `FutureOutcome` results in `Failed`, but a callback
 * registered on that `FutureOutcome` with `onFailedThen` throws `TestPendingException`, the
 * result of the `FutureOutcome` returned by `onFailedThen` will
 * be `Pending`.
 * 
 *
 * Lastly, if you want to change the outcome in some way in `withFixture`, you'll need to use
 * the `change` method of `FutureOutcome`, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgAsyncTest) = {
 *
 *   // Perform setup here
 *
 *   val futureOutcome = super.withFixture(test) // Invoke the test function
 *
 *   futureOutcome change { outcome =&gt;
 *     // transform the outcome into a new outcome here
 *   }
 * }
 * }}}
 */
trait AsyncTestSuite extends Suite with RecoverMethods with CompleteLastly { thisAsyncTestSuite =>

  /**
   * An implicit execution context used by async styles to transform `Future[Assertion]` values
   * returned by tests into `FutureOutcome` values, and can be used within the async tests themselves,
   * for example, when mapping assertions onto futures.
   */
  private final val serialExecutionContext: ExecutionContext = new concurrent.SerialExecutionContext
  implicit def executionContext: ExecutionContext = serialExecutionContext

  private def anAsyncExceptionThatShouldCauseAnAbort(ex: Throwable): Boolean =
    ex match {
      // Not sure why a thrown OutOfMemoryError in our test is showing up nested inside
      // an ExecutionException, but since it is, look inside.
      case ee: java.util.concurrent.ExecutionException if ex.getCause != null => Suite.anExceptionThatShouldCauseAnAbort(ex.getCause)
      case other => Suite.anExceptionThatShouldCauseAnAbort(other)
    }

  /**
   * Transform the test outcome, `Registration` type to `AsyncOutcome`.
   *
   * @param testFun test function
   * @return function that returns `AsyncOutcome`
   */
  private[scalatest] def transformToOutcome(testFun: => Future[compatible.Assertion]): () => AsyncOutcome =
    () => {
      val futureSucceeded: Future[Succeeded.type] = testFun.map(_ => Succeeded)
      InternalFutureOutcome(
        futureSucceeded.recover {
          case ex: exceptions.TestCanceledException => Canceled(ex)
          case _: exceptions.TestPendingException => Pending
          case tfe: exceptions.TestFailedException => Failed(tfe)
          case ex: Throwable if !anAsyncExceptionThatShouldCauseAnAbort(ex) => Failed(ex)
        }
      )/* fills in executionContext here */
    }

  /**
   * Implicitly converts an `Assertion` to a `Future[Assertion]`.
   *
   * This implicit conversion is used to allow synchronous tests to be included along with
   * asynchronous tests in an `AsyncTestSuite`. It will be 
   * 
   *
   * @param assertion the `Assertion` to convert
   * @return a `Future[Assertion]` that has already completed successfully
   *     (containing the `Succeeded` singleton).
   */
  implicit def convertAssertionToFutureAssertion(assertion: compatible.Assertion): Future[compatible.Assertion] = Future.successful(assertion)

  protected[scalatest] def parallelAsyncTestExecution: Boolean = thisAsyncTestSuite.isInstanceOf[org.scalatest.ParallelTestExecution] ||
      thisAsyncTestSuite.isInstanceOf[org.scalatest.RandomTestOrder]

  // TODO: Document how exceptions are treated. I.e., that TestConceledException becomes Success(Canceled), 
  // TestPendingException becomes Success(Pending), non-test-fatal exceptions become Success(Failed), and
  // test-fatal exceptions become Failure(ex)
  /**
   * A test function taking no arguments and returning a `FutureOutcome`.
   *
   * For more detail and examples, see the relevant section in the
   * <a href="AsyncFlatSpec.html#withFixtureNoArgTest">documentation for trait `AsyncFlatSpec`</a>.
   * 
   */
  trait NoArgAsyncTest extends (() => FutureOutcome) with TestData {
    /**
     * Runs the body of the test, returning a `FutureOutcome`.
     */
    def apply(): FutureOutcome
  }

  /**
   * Run the passed test function in the context of a fixture established by this method.
   *
   * This method should set up the fixture needed by the tests of the
   * current suite, invoke the test function, and if needed, register a callback
   * on the resulting `FutureOutcome` to perform any clean
   * up needed after the test completes. Because the `NoArgAsyncTest` function
   * passed to this method takes no parameters, preparing the fixture will require
   * side effects, such as reassigning instance `var`s in this `Suite` or initializing
   * a globally accessible external database. If you want to avoid reassigning instance `var`s
   * you can use <a href="fixture/AsyncTestSuite.html">fixture.AsyncTestSuite</a>.
   * 
   *
   * This trait's implementation of `runTest` invokes this method for each test, passing
   * in a `NoArgAsyncTest` whose `apply` method will execute the code of the test
   * and returns its result.
   * 
   *
   * This trait's implementation of this method simply invokes the passed `NoArgAsyncTest` function.
   * 
   *
   * @param test the no-arg async test function to run with a fixture
   */
  def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    test()
  }

  /*
   * OLD SCALADOC FOR WITHCLEANUP
   *
   * Ensures a cleanup function is executed whether a future-producing function that produces a
   * valid future or completes abruptly with an exception. 
   *
   * <p>
   * If the by-name passed as the first parameter, <code>future</code>, completes abruptly with an exception
   * this method will catch that exception, invoke the cleanup function passed as the second parameter,
   * <code>cleanup</code>, then rethrow the exception. Otherwise, this method will register the cleanup
   * function to be invoked after the resulting future completes.
   * </p>
   *
   * @param future a by-name that will produce a future
   * @param cleanup a by-name that must be invoked when the future completes, or immediately if
   *            an exception is thrown by the future-producing function
   * @return the future produced by the first by-name parameter, with an invocation of the second
   *            by-name parameter registered to execute when the future completes.
   */

  /**
   * Run an async test.
   *
   * This method is redefine in this trait solely to narrow its contract. Subclasses must implement
   * this method to call the `withFixture(NoArgAsyncTest)` method, which is defined in
   * this trait.
   * 
   *
   * This trait's implementation of this method simply returns `SucceededStatus` 
   * and has no other effect.
   * 
   *
   * @param testName the name of one async test to execute.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed.
   *
   * @throws NullArgumentException if either `testName` or `args`
   *     is `null`.
   */
  protected override def runTest(testName: String, args: Args): Status = SucceededStatus
}
