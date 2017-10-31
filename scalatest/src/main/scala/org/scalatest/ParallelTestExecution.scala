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

import org.scalatest.time.Span
import org.scalatest.tools.{TestSpecificReporter, DistributedTestRunnerSuite, TestSortingReporter}

/**
 * Trait that causes that the tests of any suite it is mixed into to be run in parallel if
 * a `Distributor` is passed to `runTests`.
 *
 * ScalaTest's normal approach for running suites of tests in parallel is to run different suites in parallel,
 * but the tests of any one suite sequentially. This approach should provide sufficient distribution of the work load
 * in most cases, but some suites may encapsulate multiple long-running tests. Such suites may dominate the execution
 * time of the run. If so, mixing in this trait into just those suites will allow their long-running tests to run in parallel with each
 * other, thereby helping to reduce the total time required to run an entire run.
 * 
 *
 * To make it easier for users to write tests that run in parallel, this trait runs each test in its own instance of the class.
 * Running each test in its own instance enables tests to use the same instance `vars` and mutable objects referenced from
 * instance variables without needing to synchronize. Although ScalaTest provides functional approaches to
 * factoring out common test code that can help avoid such issues, running each test in its own instance is an insurance policy that makes 
 * running tests in parallel easier and less error prone.
 * 
 *
 * For the details on how `ParallelTestExecution` works, see the documentation for methods `run`, `runTests`, and `runTest`,
 * which this trait overrides.
 * 
 *
 * Note: This trait's implementation of `runTest` is `final`, to ensure that behavior
 * related to individual tests are executed by the same thread that executes the actual test. This means,
 * for example, that you won't be allowed to write `...with ParallelTestExecution with BeforeAndAfter`.
 * Instead, you'd need to put `ParallelTestExecution` last, as
 * in: `with BeforeAndAfter with ParallelTestExecution`. For more details, see the documentation
 * for the `runTest` method.
 * 
 *
 * @author Bill Venners
 */
trait ParallelTestExecution extends OneInstancePerTest { this: Suite =>

  /**
   * Modifies the behavior of `super.runTests` to facilitate parallel test execution.
   *
   * This trait's implementation of this method always invokes `super.runTests` to delegate
   * to `OneInstancePerTest`'s implementation, but it may pass in a modified `args` object.
   * If `args.runTestInNewInstance` is `false` and `args.distributor` is defined,
   * this trait's implementation of this method will wrap the passed `args.reporter` in a new `Reporter`
   * that can sort events fired by parallel tests back into sequential order, with a timeout. It will pass this new reporter to
   * `super.runTests` (in `args.reporter`) as well as a defined `DistributedTestSorter`
   * (in args.distributedTestSorter) that can be used to communicate with the sorting reporter. Otherwise, if `args.runTestInNewInstance` is
   * `true` or `args.distributor` is empty, this trait's implementation of this method simply calls `super.runTests`,
   * passing along the same `testName` and `args`.
   * 
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
   */
  protected abstract override def runTests(testName: Option[String], args: Args): Status = {
    val newArgs =
      if (args.runTestInNewInstance)
        args // This is the test-specific instance
      else {
        args.distributor match {  // This is the initial instance
          case Some(distributor) =>
            val testSortingReporter = new TestSortingReporter(suiteId, args.reporter, sortingTimeout, testNames.size, args.distributedSuiteSorter, System.err)
            args.copy(reporter = testSortingReporter, distributedTestSorter = Some(testSortingReporter))
          case None =>
            args
        }
      }

    // Always call super.runTests, which is OneInstancePerTest's runTests. But if RTINI is NOT
    // set, that means we are in the initial instance. In that case, we wrap the reporter in
    // a new TestSortingReporter, and wrap the distributor in a new DistributorWrapper that
    // knows is passed the TestSortingReporter. We then call super.runTests, which is OIPT's runTests.
    super.runTests(testName, newArgs)
  }

  /**
   * Modifies the behavior of `super.runTest` to facilitate parallel test execution.
   *
   * This trait's implementation of this method only changes the supertrait implementation if
   * `args.distributor` is defined. If `args.distributor` is empty, it
   * simply invokes `super.runTests`, passing along the same `testName`
   * and `args` object.
   * 
   *
   * If `args.distributor` is defined, then it uses the `args.runTestInNewInstance`
   * flag to decide what to do. If `runTestInNewInstance`
   * is `true`, this is the general instance responsible for running all tests, so
   * it first notifies `args.distributedTestSorter` (if defined) that it is
   * distributing this test by invoking `distributingTest` on it, passing in the
   * `testName`. Then it wraps a new instance of this class, obtained by invoking
   * `newInstance` in a suite whose run method will ensure that only the test whose
   * name was passed to this method as `testName` is executed. Finally, this trait's
   * implementation of this method submits this wrapper suite to the distributor.
   * 
   *
   * If `runTestInNewInstance` is `false`, this is the test-specific (distributed)
   * instance, so this trait's implementation of this method simply invokes `super.runTest`,
   * passing along the same `testName` and `args` object, delegating responsibility
   * for actually running the test to the super implementation. After `super.runTest` returns
   * (or completes abruptly by throwing an exception), it notifies `args.distributedTestSorter`
   * (if defined) that it has completed running the test by invoking `completedTest` on it,
   * passing in the `testName`.
   * 
   *
   * Note: this trait's implementation of this method is `final` to ensure that
   * any other desired `runTest` behavior is executed by the same thread that executes
   * the test. For example, if you were to mix in `BeforeAndAfter` after
   * `ParallelTestExecution`, the `before` and `after` code would
   * be executed by the general instance on the main test thread, rather than by the test-specific
   * instance on the distributed thread. Marking this method `final` ensures that
   * traits like `BeforeAndAfter` can only be "super" to `ParallelTestExecution`
   * and, therefore, that its `before` and `after` code will be run
   * by the same distributed thread that runs the test itself.
   * 
   *
   * @param testName the name of one test to execute.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
   */
  final protected abstract override def runTest(testName: String, args: Args): Status = {

    args.distributor match {
      case Some(distribute) =>
        if (args.runTestInNewInstance) {
          // Tell the TSR that the test is being distributed
          for (sorter <- args.distributedTestSorter)
            sorter.distributingTest(testName)

          // It will be oneInstance, testName, args.copy(reporter = ...)
          distribute(new DistributedTestRunnerSuite(newInstance, testName, args), args.copy(tracker = args.tracker.nextTracker))
        }
        else {
          // In test-specific (distributed) instance, so just run the test. (RTINI was
          // removed by OIPT's implementation of runTests.)
          try {
            super.runTest(testName, args)
          }
          finally {
            // Tell the TSR that the distributed test has completed
            for (sorter <- args.distributedTestSorter)
              sorter.completedTest(testName)
          }
        }
      case None => super.runTest(testName, args)
    }
  }

  /**
   * Construct a new instance of this `Suite`.
   *
   * This trait's implementation of `runTests` invokes this method to create
   * a new instance of this `Suite` for each test. This trait's implementation
   * of this method uses reflection to call `this.getClass.newInstance`. This
   * approach will succeed only if this `Suite`'s class has a public, no-arg
   * constructor. In most cases this is likely to be true, because to be instantiated
   * by ScalaTest's `Runner` a `Suite` needs a public, no-arg
   * constructor. However, this will not be true of any `Suite` defined as
   * an inner class of another class or trait, because every constructor of an inner
   * class type takes a reference to the enclosing instance. In such cases, and in
   * cases where a `Suite` class is explicitly defined without a public,
   * no-arg constructor, you will need to override this method to construct a new
   * instance of the `Suite` in some other way.
   * 
   *
   * Here's an example of how you could override `newInstance` to construct
   * a new instance of an inner class:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.Suite
   *
   * class Outer {
   *   class InnerSuite extends Suite with ParallelTestExecution {
   *     def testOne() {}
   *     def testTwo() {}
   *     override def newInstance = new InnerSuite
   *   }
   * }
   * }}}
   */
  // SKIP-SCALATESTJS-START
  override def newInstance: Suite with ParallelTestExecution = {
    val instance = getClass.newInstance.asInstanceOf[Suite with ParallelTestExecution]
    instance
  }
  // SKIP-SCALATESTJS-END
  //SCALATESTJS-ONLY override def newInstance: Suite with ParallelTestExecution

  /**
   * A maximum amount of time to wait for out-of-order events generated by running the tests
   * of this `Suite` in parallel while sorting the events back into a more
   * user-friendly, sequential order.
   *
   * The default implementation of this method returns the value specified via `-T` to
   * <a href="tools/Runner$.html">`Runner`</a>, or 2 seconds, if no `-T` was supplied.
   * 
   *
   * @return a maximum amount of time to wait for events while resorting them into sequential order
   */
  protected def sortingTimeout: Span = Suite.testSortingReporterTimeout

  /**
   * Modifies the behavior of `super.run` to facilitate parallel test execution.
   *
   * This trait's implementation of this method only changes the supertrait implementation if both
   * `testName` and `args.distributedTestSorter` are defined. If either
   * `testName` or `args.distributedTestSorter` is empty, it
   * simply invokes `super.run`, passing along the same `testName`
   * and `args` object.
   * 
   *
   * If both `testName` and `args.distributedTestSorter` are defined, however,
   * this trait's implementation of this method will create a "test-specific reporter" whose `apply`
   * method will invoke the `apply` method of the `DistributedTestSorter`, which takes
   * a test name as well as the event. It will then invoke `super.run` passing along
   * the same `testName` and an `Args` object that is the same except with the
   * original reporter replaced by the test-specific reporter.
   * 
   *
   * @param testName an optional name of one test to execute. If `None`, all relevant tests should be executed.
   *                 I.e., `None` acts like a wildcard that means execute all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests and nested suites started by this method have completed, and whether or not a failure occurred.
   */
  abstract override def run(testName: Option[String], args: Args): Status = {
    (testName, args.distributedTestSorter) match {
      case (Some(name), Some(sorter)) =>
        super.run(testName, args.copy(reporter = createTestSpecificReporter(sorter, name)))
      case _ =>
        super.run(testName, args)
    }
  }

  protected[scalatest] def createTestSpecificReporter(testSorter: DistributedTestSorter, testName: String): Reporter =
    new TestSpecificReporter(testSorter, testName)
}
