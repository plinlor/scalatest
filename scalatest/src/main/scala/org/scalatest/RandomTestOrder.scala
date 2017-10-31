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

import scala.util.Random
import org.scalatest.events.Event
import org.scalatest.time.Span
import org.scalatest.tools.{TestSortingReporter}
import scala.util.{Success, Failure}

/**
 * Trait that causes tests to be run in pseudo-random order.
 *
 * Although the tests are run in pseudo-random order, events will be fired in the &ldquo;normal&rdquo; order for the `Suite`
 * that mixes in this trait, as determined by `runTests`. 
 * 
 *
 * The purpose of this trait is to reduce the likelihood of unintentional order dependencies between tests
 * in the same test class.
 * 
 *
 * @author Chee Seng
 */
trait RandomTestOrder extends OneInstancePerTest { this: Suite =>

  private[scalatest] case class DeferredSuiteRun(suite: Suite with RandomTestOrder, testName: String, status: ScalaTestStatefulStatus)

  private val suiteRunQueue = new ConcurrentLinkedQueue[DeferredSuiteRun]

  /**
   * Modifies the behavior of `super.runTest` to facilitate pseudo-random order test execution.
   *
   * If `runTestInNewInstance` is `false`, this is the test-specific (distributed)
   * instance, so this trait's implementation of this method simply invokes `super.runTest`,
   * passing along the same `testName` and `args` object, delegating responsibility
   * for actually running the test to the super implementation. After `super.runTest` returns
   * (or completes abruptly by throwing an exception), it notifies `args.distributedTestSorter`
   * that it has completed running the test by invoking `completedTest` on it,
   * passing in the `testName`.
   * 
   *
   * If `runTestInNewInstance` is `true`, it notifies `args.distributedTestSorter`
   * that it is distributing the test by invoking `distributingTest` on it,
   * passing in the `testName`.  The test execution will be deferred to be run in pseudo-random order later.
   * 
   *
   * Note: this trait's implementation of this method is `final` to ensure that
   * any other desired `runTest` behavior is executed by the same thread that executes
   * the test. For example, if you were to mix in `BeforeAndAfter` after
   * `RandomTestOrder`, the `before` and `after` code would
   * be executed by the general instance on the main test thread, rather than by the test-specific
   * instance on the distributed thread. Marking this method `final` ensures that
   * traits like `BeforeAndAfter` can only be &lquot;super&rquot; to `RandomTestOrder`
   * and, therefore, that its `before` and `after` code will be run
   * by the same distributed thread that runs the test itself.
   * 
   *
   * @param testName the name of one test to execute.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
   */
  final protected abstract override def runTest(testName: String, args: Args): Status = {

    if (args.runTestInNewInstance) {
      // Tell the TSR that the test is being distributed
      for (sorter <- args.distributedTestSorter)
        sorter.distributingTest(testName)

      // defer the suite execution
      val status = new ScalaTestStatefulStatus
      suiteRunQueue.add(DeferredSuiteRun(newInstance, testName, status))
      status
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
   * import org.scalatest._
   *
   * class Outer {
   *   class InnerSuite extends Suite with RandomTestOrder {
   *     def testOne() {}
   *     def testTwo() {}
   *     override def newInstance = new InnerSuite
   *   }
   * }
   * }}}
   */
  override def newInstance: Suite with RandomTestOrder = this.getClass.newInstance.asInstanceOf[Suite with RandomTestOrder]

  /**
   * A maximum amount of time to wait for out-of-order events generated by running the tests
   * of this `Suite` in parallel while sorting the events back into a more
   * user-friendly, sequential order.
   *
   * The default implementation of this method returns the value specified via `-T` to
   * <a href="tools/Suite$.html">`Suite`</a>, or 2 seconds, if no `-T` was supplied.
   * 
   *
   * @return a maximum amount of time to wait for events while resorting them into sequential order
   */
  protected def sortingTimeout: Span = Suite.testSortingReporterTimeout

  /**
   * Modifies the behavior of `super.run` to facilitate pseudo-random order test execution.
   *
   * If both `testName` and `args.distributedTestSorter` are defined,
   * this trait's implementation of this method will create a "test-specific reporter" whose `apply`
   * method will invoke the `apply` method of the `DistributedTestSorter`, which takes
   * a test name as well as the event. It will then invoke `super.run` passing along
   * the same `testName` and an `Args` object that is the same except with the
   * original reporter replaced by the test-specific reporter.
   * 
   *
   * If either `testName` or `args.distributedTestSorter` is empty, it will create `TestSortingReporter`
   * and override `args`'s `reporter` and `distributedTestSorter` with it.  It then call `super.run`
   * to delegate the run to super's implementation, and to collect all children suites in `suiteRunQueue`.  After `super.run`
   * completed, it then shuffle the order of the suites collected in `suiteRunQueue` and run them.
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
        val testSortingReporter = new TestSortingReporter(suiteId, args.reporter, sortingTimeout, testNames.size, args.distributedSuiteSorter, System.err)
        val newArgs = args.copy(reporter = testSortingReporter, distributedTestSorter = Some(testSortingReporter))
        val status = super.run(testName, newArgs)
        // Random shuffle the deferred suite list, before executing them.
        Random.shuffle(suiteRunQueue.asScala.toList).map { case DeferredSuiteRun(suite, testName, statefulStatus) =>
          val status = suite.run(Some(testName), newArgs.copy(runTestInNewInstance = true))
          status.whenCompleted { tri => 
            tri match {
              case Success(result) =>
                if (!result)
                  statefulStatus.setFailed()
              case Failure(ex) =>
                  statefulStatus.setFailedWith(ex)
            }
            statefulStatus.setCompleted()
          }
        }
        status
    }
  }

  private[scalatest] def createTestSpecificReporter(testSorter: DistributedTestSorter, testName: String): Reporter = {
    class TestSpecificReporter(testSorter: DistributedTestSorter, testName: String) extends Reporter {
      def apply(event: Event): Unit = {
        testSorter.apply(testName, event)
      }
    }
    new TestSpecificReporter(testSorter, testName)
  }
}
