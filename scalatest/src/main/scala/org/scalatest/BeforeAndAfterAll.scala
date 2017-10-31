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
 * Stackable trait that can be mixed into suites that need methods invoked before and after executing the
 * suite.
 *
 * This trait allows code to be executed before and/or after all the tests and nested suites of a
 * suite are run. This trait overrides `run` and calls the
 * `beforeAll` method, then calls `super.run`. After the `super.run`
 * invocation completes, whether it returns normally or completes abruptly with an exception,
 * this trait's `run` method will invoke `afterAll`.
 * 
 *
 * Trait `BeforeAndAfterAll` defines `beforeAll`
 * and `afterAll` methods that take no parameters. This trait's implementation of these
 * methods do nothing.
 * 
 *
 * For example, the following `ExampleSpec` mixes in `BeforeAndAfterAll` and
 * in `beforeAll`, creates and writes to a temp file.
 * Each test class, `ExampleSpec` and all its nested
 * suites--`OneSpec`, `TwoSpec`, `RedSpec`,
 * and `BlueSpec`--tests that the file exists. After all of the nested suites
 * have executed, `afterAll` is invoked, which
 * deletes the file. 
 * (Note: if you're unfamiliar with the `withFixture(OneArgTest)` approach to shared fixtures, check out
 * the documentation for trait <a href="fixture/FlatSpec.html">`fixture.FlatSpec`</a>.)
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.beforeandafterall
 *
 * import org.scalatest._
 * import java.io._
 * 
 * trait TempFileExistsSpec extends fixture.FlatSpecLike {
 * 
 *   protected val tempFileName = "tmp.txt"
 * 
 *   type FixtureParam = File
 *   override def withFixture(test: OneArgTest) = {
 *     val file = new File(tempFileName)
 *     withFixture(test.toNoArgTest(file)) // loan the fixture to the test
 *   }
 * 
 *   "The temp file" should ("exist in " + suiteName) in { file =&gt;
 *     assert(file.exists)
 *   }
 * }
 * 
 * class OneSpec extends TempFileExistsSpec
 * class TwoSpec extends TempFileExistsSpec
 * class RedSpec extends TempFileExistsSpec
 * class BlueSpec extends TempFileExistsSpec
 * 
 * class ExampleSpec extends Suites(
 *   new OneSpec,
 *   new TwoSpec,
 *   new RedSpec,
 *   new BlueSpec
 * ) with TempFileExistsSpec with BeforeAndAfterAll {
 * 
 *   // Set up the temp file needed by the test, taking
 *   // a file name from the config map
 *   override def beforeAll() {
 *     val writer = new FileWriter(tempFileName)
 *     try writer.write("Hello, suite of tests!")
 *     finally writer.close()
 *   }
 * 
 *   // Delete the temp file
 *   override def afterAll() {
 *     val file = new File(tempFileName)
 *     file.delete()
 *   }
 * }
 * }}}
 *
 * If you do supply a mapping for `"tempFileName"` in the config map, you'll see that the temp file is available to all the tests:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new ExampleSpec)
 * <span class="stGreen">ExampleSpec:
 * OneSpec:
 * The temp file
 * - should exist in OneSpec
 * TwoSpec:
 * The temp file
 * - should exist in TwoSpec
 * RedSpec:
 * The temp file
 * - should exist in RedSpec
 * BlueSpec:
 * The temp file
 * - should exist in BlueSpec
 * The temp file
 * - should exist in ExampleSpec</span>
 * }}}
 *
 * '''Note: this trait uses the `Status` result of `Suite`'s "run" methods
 * to ensure that the code in `afterAll` is executed after
 * all the tests and nested suites are executed even if a `Distributor` is passed.'''
 * 
 *
 * Note that it is ''not'' guaranteed that `afterAll` is invoked from the same thread as `beforeAll`,
 * so if there's any shared state between `beforeAll` and `afterAll` you'll need to make sure they are
 * synchronized correctly.
 * 
 *
 *
 * @author Bill Venners
 */
trait BeforeAndAfterAll extends SuiteMixin { this: Suite =>

  /**
   * Flag to indicate whether to invoke beforeAll and afterAll even when there are no tests expected.
   *
   * The default value is `false`, which means beforeAll and afterAll will not be invoked 
   * if there are no tests expected. Whether tests are expected is determined by invoking `expectedTestCount` passing in
   * the passed filter. Because this count does not include tests excluded based on tags, such as ignored tests, this prevents
   * any side effects in `beforeAll` or `afterAll` if no tests will ultimately be executed anyway.
   * If you always want to see the side effects even if no tests are expected, override this `val` and set it to true.
   * 
   */
  val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = false

  /**
   * Defines a method to be run before any of this suite's tests or nested suites are run.
   *
   * This trait's implementation
   * of `run` invokes this `beforeAll()`
   * method. This trait's implementation of this method does nothing.
   * 
   */
  protected def beforeAll() = ()

  /**
   * Defines a method to be run after all of this suite's tests and nested suites have
   * been run.
   *
   * This trait's implementation
   * of `run` invokes this `afterAll()` method.
   * This trait's implementation of this method does nothing.
   * 
   */
  protected def afterAll() = ()

  /**
   * Execute a suite surrounded by calls to `beforeAll` and `afterAll`.
   *
   * This trait's implementation of this method ("this method") invokes `beforeAll(ConfigMap)`
   * before executing any tests or nested suites and `afterAll(ConfigMap)`
   * after executing all tests and nested suites. It runs the suite by invoking `super.run`, passing along
   * the parameters passed to it.
   * 
   *
   * If any invocation of `beforeAll` completes abruptly with an exception, this
   * method will complete abruptly with the same exception. If any call to
   * `super.run` completes abruptly with an exception, this method
   * will complete abruptly with the same exception, however, before doing so, it will
   * invoke `afterAll`. If `afterAll` ''also'' completes abruptly with an exception, this
   * method will nevertheless complete abruptly with the exception previously thrown by `super.run`.
   * If `super.run` returns normally, but `afterAll` completes abruptly with an
   * exception, this method will complete abruptly with the same exception.
   * 
   *
   * This method does not invoke either `beforeAll` or `afterAll` if `runTestsInNewInstance` is true so
   * that any side effects only happen once per test if `OneInstancePerTest` is being used. In addition, if no tests
   * are expected, then `beforeAll` and `afterAll` will be invoked only if the
   * `invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected` flag is true. By default, this flag is false, so that if 
   * all tests are excluded (such as if the entire suite class has been marked with `@Ignore`), then side effects
   * would happen only if at least one test will ultimately be executed in this suite or its nested suites.
   * 
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
  */
  abstract override def run(testName: Option[String], args: Args): Status = {
    val (runStatus, thrownException) =
      try {
        if (!args.runTestInNewInstance && (expectedTestCount(args.filter) > 0 || invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected))
          beforeAll()
        (super.run(testName, args), None)
      }
      catch {
        case e: Exception => (FailedStatus, Some(e))
      }

    try {
      val statusToReturn =
        if (!args.runTestInNewInstance && (expectedTestCount(args.filter) > 0 || invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected)) {
          // runStatus may not be completed, call afterAll only after it is completed
          runStatus withAfterEffect {
            try {
             afterAll()
            }
            catch {
              case laterException: Exception if !Suite.anExceptionThatShouldCauseAnAbort(laterException) && thrownException.isDefined =>
              // We will swallow the exception thrown from after if it is not test-aborting and exception was already thrown by before or test itself.
            }
          }
        }
        else runStatus
      thrownException match {
        case Some(e) => throw e
        case None =>
      }
      statusToReturn
    }
    catch {
      case laterException: Exception =>
        thrownException match { // If both before/run and after throw an exception, report the earlier exception
          case Some(earlierException) => throw earlierException
          case None => throw laterException
        }
    }

    /*thrownException match {
      case Some(earlierException) =>
        try {
          if (!args.runTestInNewInstance && (expectedTestCount(args.filter) > 0 || invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected))
            afterAll() // Make sure that afterAll is called even if run completes abruptly.
          runStatus
        }
        catch {
          case laterException: Exception => // Do nothing, will need to throw the earlier exception
          runStatus
        }
        finally {
          throw earlierException
        }
      case None =>
        if (!args.runTestInNewInstance && (expectedTestCount(args.filter) > 0 || invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected)) {
          // runStatus may not be completed, call afterAll only after it is completed
          runStatus withAfterEffectNew {
            try {
              afterAll()
            }
            catch {
              case laterException: Exception if !Suite.anExceptionThatShouldCauseAnAbort(laterException) && thrownException.isDefined =>
                // We will swallow the exception thrown from after if it is not test-aborting and exception it already thrown by before or test itself.
            }
          }
        }
        else runStatus
    }*/
  }
}
