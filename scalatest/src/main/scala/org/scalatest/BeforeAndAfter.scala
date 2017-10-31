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

import java.util.concurrent.atomic.AtomicReference
import exceptions.NotAllowedException
import org.scalactic.source

/**
 * Trait that can be mixed into suites that need code executed before and after running each test.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * Use trait `BeforeAndAfter` when you need to perform the same side-effects before and/or after tests, rather than at the beginning
 * or end of tests. ''Note: For more insight into where `BeforeAndAfter` fits into the big picture, see the ''
 * <a href="FlatSpec.html#sharedFixtures">Shared fixtures</a> section in the documentation for your chosen style trait.''
 * </td></tr></table>
 * 
 * A test ''fixture'' is composed of the objects and other artifacts (files, sockets, database
 * connections, ''etc.'') tests use to do their work.
 * When multiple tests need to work with the same fixtures, it is important to try and avoid
 * duplicating the fixture code across those tests. The more code duplication you have in your
 * tests, the greater drag the tests will have on refactoring the actual production code.
 * Trait `BeforeAndAfter` offers one way to eliminate such code duplication:
 * a `before` clause that will register code to be run before each test,
 * and an `after` clause that will register code to be run after.
 * 
 *
 * Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.beforeandafter
 * 
 * import org.scalatest._
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends FlatSpec with BeforeAndAfter {
 * 
 *   val builder = new StringBuilder
 *   val buffer = new ListBuffer[String]
 * 
 *   before {
 *     builder.append("ScalaTest is ")
 *   }
 * 
 *   after {
 *     builder.clear()
 *     buffer.clear()
 *   }
 * 
 *   "Testing" should "be easy" in {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   it should "be fun" in {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *   }
 * }
 * }}}
 *
 * The `before` and `after` methods can each only be called once per `Suite`,
 * and cannot be invoked after `run` has been invoked.  If either of the registered before or after functions
 * complete abruptly with an exception, it will be reported as an aborted suite and no more tests will be attempted in that suite.
 * 
 *
 * Note that the only way `before` and `after` code can communicate with test code is via some side-effecting mechanism, commonly by
 * reassigning instance `var`s or by changing the state of mutable objects held from instance `val`s (as in this example). If using
 * instance `var`s or mutable objects held from instance `val`s you wouldn't be able to run tests in parallel in the same instance
 * of the test class unless you synchronized access to the shared, mutable state. This is why ScalaTest's <a href="ParallelTestExecution.html">`ParallelTestExecution`</a> trait extends
 * <a href="OneInstancePerTest.html">`OneInstancePerTest`</a>. By running each test in its own instance of the class, each test has its own copy of the instance variables, so you
 * don't need to synchronize. Were you to mix `ParallelTestExecution` into the `ExampleSuite` above, the tests would run in parallel just fine
 * without any synchronization needed on the mutable `StringBuilder` and `ListBuffer[String]` objects.
 * 
 *
 * Although `BeforeAndAfter` provides a minimal-boilerplate way to execute code before and after tests, it isn't designed to enable stackable
 * traits, because the order of execution would be non-obvious.  If you want to factor out before and after code that is common to multiple test suites, you 
 * should use trait <a href="BeforeAndAfterEach.html">`BeforeAndAfterEach`</a> instead.
 * 
 *
 * The advantage this trait has over `BeforeAndAfterEach` is that its syntax is more concise. 
 * The main disadvantage is that it is not stackable, whereas `BeforeAndAfterEach` is. ''I.e.'', 
 * you can write several traits that extend `BeforeAndAfterEach` and provide `beforeEach` methods
 * that include a call to `super.beforeEach`, and mix them together in various combinations. By contrast,
 * only one call to the `before` registration function is allowed in a suite or spec that mixes
 * in `BeforeAndAfter`. In addition, `BeforeAndAfterEach` allows you to access
 * the config map and test name via the <a href="TestData.html">`TestData`</a> passed to its `beforeEach` and
 * `afterEach` methods, whereas `BeforeAndAfter`
 * gives you no access to the config map.
 * 
 *
 * @author Bill Venners
 */
trait BeforeAndAfter extends SuiteMixin { this: Suite =>

  private val beforeFunctionAtomic = new AtomicReference[Option[() => Any]](None)
  private val afterFunctionAtomic = new AtomicReference[Option[() => Any]](None)
  @volatile private var runHasBeenInvoked = false

  /**
   * Registers code to be executed before each of this suite's tests.
   *
   * This trait's implementation
   * of `runTest` executes the code passed to this method before running
   * each test. Thus the code passed to this method can be used to set up a test fixture
   * needed by each test.
   * 
   *
   * @throws NotAllowedException if invoked more than once on the same `Suite` or if
   *                             invoked after `run` has been invoked on the `Suite`
   */
  protected def before(fun: => Any)(implicit pos: source.Position): Unit = {
    if (runHasBeenInvoked)
      throw new NotAllowedException("You cannot call before after run has been invoked (such as, from within a test). It is probably best to move it to the top level of the Suite class so it is executed during object construction.", pos)
    val success = beforeFunctionAtomic.compareAndSet(None, Some(() => fun))
    if (!success)
      throw new NotAllowedException("You are only allowed to call before once in each Suite that mixes in BeforeAndAfter.", pos)
  }

  /**
   * Registers code to be executed after each of this suite's tests.
   *
   * This trait's implementation of `runTest` executes the code passed to this method after running
   * each test. Thus the code passed to this method can be used to tear down a test fixture
   * needed by each test.
   * 
   *
   * @throws NotAllowedException if invoked more than once on the same `Suite` or if
   *                             invoked after `run` has been invoked on the `Suite`
   */
  protected def after(fun: => Any)(implicit pos: source.Position): Unit = {
    if (runHasBeenInvoked)
      throw new NotAllowedException("You cannot call after after run has been invoked (such as, from within a test. It is probably best to move it to the top level of the Suite class so it is executed during object construction.", pos)
    val success = afterFunctionAtomic.compareAndSet(None, Some(() => fun))
    if (!success)
      throw new NotAllowedException("You are only allowed to call after once in each Suite that mixes in BeforeAndAfter.", pos)
  }

  /**
   * Run a test surrounded by calls to the code passed to `before` and `after`, if any.
   *
   * This trait's implementation of this method ("this method") invokes
   * the function registered with `before`, if any,
   * before running each test and the function registered with `after`, if any,
   * after running each test. It runs each test by invoking `super.runTest`, passing along
   * the five parameters passed to it.
   * 
   * 
   * If any invocation of the function registered with `before` completes abruptly with an exception, this
   * method will complete abruptly with the same exception. If any call to
   * `super.runTest` completes abruptly with an exception, this method
   * will complete abruptly with the same exception, however, before doing so, it will
   * invoke the function registered with `after`, if any. If the function registered with `after`
   * ''also'' completes abruptly with an exception, this
   * method will nevertheless complete abruptly with the exception previously thrown by `super.runTest`.
   * If `super.runTest` returns normally, but the function registered with `after` completes abruptly with an
   * exception, this method will complete abruptly with the exception thrown by the function registered with `after`.
   * 
   *
   * @param testName the name of one test to run.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
  */
  abstract protected override def runTest(testName: String, args: Args): Status = {

    // Do I need to make this volatile?
    var thrownException: Option[Throwable] = None

    val runTestStatus: Status =
      try {
        beforeFunctionAtomic.get match {
          case Some(fun) => if (!args.runTestInNewInstance) fun()
          case None =>
        }
        super.runTest(testName, args)
      }
      catch {
        case e: Throwable if !Suite.anExceptionThatShouldCauseAnAbort(e) =>
          thrownException = Some(e)
          FailedStatus // I think if this happens, we just want to try the after code, swallowing exceptions, right here. No
                       // need to do it asynchronously. I suspect that would simplify the code.
      }
    // And if the exception should cause an abort, abort the afterAll too. (TODO: Update the Scaladoc.)
    try {
      val statusToReturn: Status =
        if (!args.runTestInNewInstance) {
          // Make sure that afterEach is called even if runTest completes abruptly.
          runTestStatus withAfterEffect {
            try {
              afterFunctionAtomic.get match {
                case Some(fun) => fun()
                case None =>
              }
            }
            catch {
              case ex: Throwable if !Suite.anExceptionThatShouldCauseAnAbort(ex) && thrownException.isDefined =>
                // We will swallow the exception thrown from after if it is not test-aborting and exception was already thrown by before or test itself.
            }
          }
        }
        else
          runTestStatus
      thrownException match {
        case Some(e) => throw e
        case None =>
      }
      statusToReturn
    }
    catch {
      case laterException: Exception =>
        thrownException match { // If both run and after throw an exception, report the test exception
          case Some(earlierException) => throw earlierException
          case None => throw laterException
        }
    }
  }

  /**
   * This trait's implementation of run sets a flag indicating run has been invoked, after which
   * any invocation to `before` or `after` will complete abruptly
   * with a `NotAllowedException`.
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests and nested suites started by this method have completed, and whether or not a failure occurred.
   */
  abstract override def run(testName: Option[String], args: Args): Status = {
    runHasBeenInvoked = true
    super.run(testName, args)
  }
}
