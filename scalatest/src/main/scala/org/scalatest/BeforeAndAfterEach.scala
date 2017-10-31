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

/**
 * Stackable trait that can be mixed into suites that need code executed before and/or after running each test.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * Use trait `BeforeAndAfterEach` when you want to stack traits that perform side-effects before and/or after tests, rather
 * than at the beginning or end of tests. 
 * ''Note: For more insight into where `BeforeAndAfterEach` fits into the big picture, see the ''
 * <a href="FlatSpec.html#sharedFixtures">Shared fixtures</a>'' section in the documentation for your chosen style trait.
 * </td></tr></table>
 * 
 * A test ''fixture'' is composed of the objects and other artifacts (files, sockets, database
 * connections, ''etc.'') tests use to do their work.
 * When multiple tests need to work with the same fixtures, it is important to try and avoid
 * duplicating the fixture code across those tests. The more code duplication you have in your
 * tests, the greater drag the tests will have on refactoring the actual production code, and 
 * the slower your compile will likely be.
 * Trait `BeforeAndAfterEach` offers one way to eliminate such code duplication:
 * a `beforeEach` method that will be run before each test (like JUnit's `setUp`),
 * and an `afterEach` method that will be run after (like JUnit's `tearDown`).
 * 
 *
 * Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.composingbeforeandaftereach
 * 
 * import org.scalatest._
 * import collection.mutable.ListBuffer
 * 
 * trait Builder extends BeforeAndAfterEach { this: Suite =&gt;
 * 
 *   val builder = new StringBuilder
 * 
 *   override def beforeEach() {
 *     builder.append("ScalaTest is ")
 *     super.beforeEach() // To be stackable, must call super.beforeEach
 *   }
 * 
 *   override def afterEach() {
 *     try {
 *       super.afterEach() // To be stackable, must call super.afterEach
 *     }
 *     finally {
 *       builder.clear()
 *     }
 *   }
 * }
 * 
 * trait Buffer extends BeforeAndAfterEach { this: Suite =&gt;
 * 
 *   val buffer = new ListBuffer[String]
 * 
 *   override def afterEach() {
 *     try {
 *       super.afterEach() // To be stackable, must call super.afterEach
 *     }
 *     finally {
 *       buffer.clear()
 *     }
 *   }
 * }
 * 
 * class ExampleSpec extends FlatSpec with Builder with Buffer {
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
 *     buffer += "clear"
 *   }
 * }
 * }}}
 *
 * To get the same ordering as `withFixture`, place your `super.beforeEach` call at the end of each
 * `beforeEach` method, and the `super.afterEach` call at the beginning of each `afterEach`
 * method, as shown in the previous example. It is a good idea to invoke `super.afterEach` in a `try`
 * block and perform cleanup in a `finally` clause, as shown in the previous example, because this ensures the
 * cleanup code is performed even if `super.afterEach` throws an exception.
 * 
 *
 * The main advantage of `BeforeAndAfterEach` over `BeforeAndAfter` is that `BeforeAndAfterEach`.
 * enables trait stacking.
 * The main disadvantage of `BeforeAndAfterEach` compared to `BeforeAndAfter` is that `BeforeAndAfterEach`
 * requires more boilerplate. If you don't need trait stacking, use <a href="BeforeAndAfter.html">`BeforeAndAfter`</a> instead
 * of `BeforeAndAfterEach`.
 * If you want to make use of test data (the test name, config map, ''etc.'') in your `beforeEach`
 * or `afterEach` method, use trait <a href="BeforeAndAfterEachTestData.html">`BeforeAndAfterEachTestData`</a> instead.
 * 
 *
 * @author Bill Venners
 */
trait BeforeAndAfterEach extends SuiteMixin {

  this: Suite =>

  /**
   * Defines a method to be run before each of this suite's tests.
   *
   * This trait's implementation
   * of `runTest` invokes the overloaded form of this method that
   * takes a `configMap` before running
   * each test. This trait's implementation of that `beforeEach(Map[String, Any])` method simply invokes this
   * `beforeEach()` method. Thus this method can be used to set up a test fixture
   * needed by each test, when you don't need anything from the `configMap`.
   * This trait's implementation of this method does nothing.
   * 
   */
  protected def beforeEach() = ()

  /**
   * Defines a method to be run after each of this suite's tests.
   *
   * This trait's implementation
   * of `runTest` invokes the overloaded form of this method that
   * takes a `configMap` map after running
   * each test. This trait's implementation of that `afterEach(Map[String, Any])` method simply invokes this
   * `afterEach()` method. Thus this method can be used to tear down a test fixture
   * needed by each test, when you don't need anything from the `configMap`.
   * This trait's implementation of this method does nothing.
   * 
   */
  protected def afterEach() = ()

  /**
   * Run a test surrounded by calls to `beforeEach` and `afterEach`.
   *
   * This trait's implementation of this method ("this method") invokes
   * `beforeEach(configMap)`
   * before running each test and `afterEach(configMap)`
   * after running each test. It runs each test by invoking `super.runTest`, passing along
   * the two parameters passed to it.
   * 
   * 
   * If any invocation of `beforeEach` completes abruptly with an exception, this
   * method will complete abruptly with the same exception. If any call to
   * `super.runTest` completes abruptly with an exception, this method
   * will complete abruptly with the same exception, however, before doing so, it will
   * invoke `afterEach`. If <cod>afterEach` ''also'' completes abruptly with an exception, this
   * method will nevertheless complete abruptly with the exception previously thrown by `super.runTest`.
   * If `super.runTest` returns normally, but `afterEach` completes abruptly with an
   * exception, this method will complete abruptly with the exception thrown by `afterEach`.
   * 
   *
   * @param testName the name of one test to run.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
  */
/*
  abstract protected override def runTest(testName: String, args: Args): Status = {

    var thrownException: Option[Throwable] = None

    if (!args.runTestInNewInstance) beforeEach()
    try {
      super.runTest(testName, args)
    }
    catch {
      case e: Exception => 
        thrownException = Some(e)
        FailedStatus
    }
    finally {
      try {
        if (!args.runTestInNewInstance) afterEach() // Make sure that afterEach is called even if runTest completes abruptly.
        thrownException match {
          case Some(e) => throw e
          case None =>
        }
      }
      catch {
        case laterException: Exception =>
          thrownException match { // If both run and afterAll throw an exception, report the test exception
            case Some(earlierException) => throw earlierException
            case None => throw laterException
          }
      }
    }
  }
*/
  abstract protected override def runTest(testName: String, args: Args): Status = {

    var thrownException: Option[Throwable] = None

    val runTestStatus: Status =
      try {
        if (!args.runTestInNewInstance) beforeEach()
        super.runTest(testName, args)
      }
      catch {
        case e: Throwable if !Suite.anExceptionThatShouldCauseAnAbort(e) =>
          thrownException = Some(e)
          FailedStatus
      }
    // And if the exception should cause an abort, abort the afterAll too. (TODO: Update the Scaladoc.)
    try {
      val statusToReturn: Status =
        if (!args.runTestInNewInstance) {
          runTestStatus withAfterEffect {
            try {
              afterEach()
            }
            catch { 
              case e: Throwable if !Suite.anExceptionThatShouldCauseAnAbort(e) && thrownException.isDefined =>
                // We will swallow the exception thrown from afterEach if it is not test-aborting and exception was already thrown by beforeEach or test itself.
            }
          } // Make sure that afterEach is called even if runTest completes abruptly.
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
        thrownException match { // If both run and afterAll throw an exception, report the test exception
          case Some(earlierException) => throw earlierException
          case None => throw laterException
        }
    }
  }
}
