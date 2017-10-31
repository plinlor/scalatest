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
 * Stackable trait that can be mixed into suites that need code that makes use of test data (test name, tags, config map, ''etc.'') executed
 * before and/or after running each test.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * Use trait `BeforeAndAfterEachTestData` when you want to stack traits that perform side-effects before and/or after tests, rather
 * than at the beginning or end of tests, when you need access to any test data (such as the config map) in the before and/or after code.
 * ''Note: For more insight into where `BeforeAndAfterEachTestData` fits into the big picture, see the ''
 * <a href="FlatSpec.html#sharedFixtures">Shared fixtures</a> section in the documentation for your chosen style trait.''
 * </td></tr></table>
 * 
 * A test ''fixture'' is composed of the objects and other artifacts (files, sockets, database
 * connections, ''etc.'') tests use to do their work.
 * When multiple tests need to work with the same fixtures, it is important to try and avoid
 * duplicating the fixture code across those tests. The more code duplication you have in your
 * tests, the greater drag the tests will have on refactoring the actual production code.
 * Trait `BeforeAndAfterEachTestData` offers one way to eliminate such code duplication:
 * a `beforeEach(TestData)` method that will be run before each test (like JUnit's `setUp`),
 * and an `afterEach(TestData)` method that will be run after (like JUnit's `tearDown`).
 * 
 *
 * Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.composingbeforeandaftereachtestdata
 * 
 * import org.scalatest._
 * import collection.mutable.ListBuffer
 * 
 * trait Builder extends BeforeAndAfterEachTestData { this: Suite =&gt;
 * 
 *   val builder = new StringBuilder
 * 
 *   override def beforeEach(td: TestData) {
 *     builder.append(td.name)
 *     super.beforeEach(td) // To be stackable, must call super.beforeEach(TestData)
 *   }
 * 
 *   override def afterEach(td: TestData) {
 *     try {
 *       super.afterEach(td) // To be stackable, must call super.afterEach(TestData)
 *     }
 *     finally {
 *       builder.clear()
 *     }
 *   }
 * }
 * 
 * trait Buffer extends BeforeAndAfterEachTestData { this: Suite =&gt;
 * 
 *   val buffer = new ListBuffer[String]
 * 
 *   override def afterEach(td: TestData) {
 *     try {
 *       super.afterEach(td) // To be stackable, must call super.afterEach(TestData)
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
 *     builder.append("!")
 *     assert(builder.toString === "Testing should be easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   it should "be fun" in {
 *     builder.append("!")
 *     assert(builder.toString === "Testing should be fun!")
 *     assert(buffer.isEmpty)
 *     buffer += "clear"
 *   }
 * }
 * }}}
 *
 * To get the same ordering as `withFixture`, place your `super.beforeEach(TestData)` call at the end of each
 * `beforeEach(TestData)` method, and the `super.afterEach(TestData)` call at the beginning of each `afterEach(TestData)`
 * method, as shown in the previous example. It is a good idea to invoke `super.afterEach(TestData)` in a `try`
 * block and perform cleanup in a `finally` clause, as shown in the previous example, because this ensures the
 * cleanup code is performed even if `super.afterEach(TestData)` throws an exception.
 * 
 *
 * Besides enabling trait stacking, the other main advantage of `BeforeAndAfterEachTestData` over `BeforeAndAfter`
 * is that `BeforeAndAfterEachTestData` allows you to make use of test data (such as the test name and config map) in your before
 * and/or after code, whereas `BeforeAndAfter` does not.
 * 
 *
 * The main disadvantage of `BeforeAndAfterEachTestData` compared to `BeforeAndAfter` and `BeforeAndAfterEach` is
 * that `BeforeAndAfterEachTestData` requires more boilerplate. If you don't need trait stacking or access to the test data, use
 * <a href="BeforeAndAfter.html">`BeforeAndAfter`</a> instead
 * of `BeforeAndAfterEachTestData`.
 * If you need trait stacking, but not access to the `TestData`, use
 * <a href="BeforeAndAfterEach.html">`BeforeAndAfterEach`</a> instead.
 * 
 *
 * @author Bill Venners
 */
trait BeforeAndAfterEachTestData extends SuiteMixin {

  this: Suite =>

  /**
   * Defines a method (that takes a `TestData`) to be run before
   * each of this suite's tests.
   *
   * This trait's implementation
   * of `runTest` invokes this method before running
   * each test (passing in a TestData that includes the `configMap` passed to it), thus this
   * method can be used to set up a test fixture
   * needed by each test. This trait's implementation of this method does nothing.
   * 
   */
  protected def beforeEach(testData: TestData): Unit = {
  }

  /**
   * Defines a method (that takes a `TestData`) to be run after
   * each of this suite's tests.
   *
   * This trait's implementation
   * of `runTest` invokes this method after running
   * each test (passing in a `TestData` containing the `configMap` passed
   * to it), thus this method can be used to tear down a test fixture
   * needed by each test. This trait's implementation of this method does nothing.
   * 
   */
  protected def afterEach(testData: TestData): Unit = {
  }

  /**
   * Run a test surrounded by calls to `beforeEach` and `afterEach`.
   *
   * This trait's implementation of this method ("this method") invokes
   * `beforeEach(TestData)`
   * before running each test and `afterEach(TestData)`
   * after running each test. It runs each test by invoking `super.runTest`, passing along
   * the two parameters passed to it.
   * 
   * 
   * If any invocation of `beforeEach(TestData)` completes abruptly with an exception, this
   * method will complete abruptly with the same exception. If any call to
   * `super.runTest` completes abruptly with an exception, this method
   * will complete abruptly with the same exception, however, before doing so, it will
   * invoke `afterEach(TestData)`. If `afterEach(TestData)` ''also'' completes abruptly with an exception, this
   * method will nevertheless complete abruptly with the exception previously thrown by `super.runTest`.
   * If `super.runTest` returns normally, but `afterEach(TestData)` completes abruptly with an
   * exception, this method will complete abruptly with the exception thrown by `afterEach(TestData)`.
   * 
   *
   * @param testName the name of one test to run.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
  */
  abstract protected override def runTest(testName: String, args: Args): Status = {

    var thrownException: Option[Throwable] = None

    val runTestStatus: Status =
      try {
        if (!args.runTestInNewInstance) beforeEach(testDataFor(testName, args.configMap))
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
              afterEach(testDataFor(testName, args.configMap))
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
