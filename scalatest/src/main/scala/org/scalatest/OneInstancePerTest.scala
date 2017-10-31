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

import org.scalactic.Requirements._

/**
 * Trait that facilitates a style of testing in which each test is run in its own instance
 * of the suite class to isolate each test from the side effects of the other tests in the
 * suite.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''': Trait `OneInstancePerTest` is intended primarily to serve as a supertrait for
 * <a href="ParallelTestExecution.html">`ParallelTestExecution`</a> and the <a href="path/package.html">path traits</a>, to
 * facilitate porting JUnit tests to ScalaTest, and to make it easy for users who prefer JUnit's approach to isolation to obtain similar
 * behavior in ScalaTest.
 * </td></tr></table>
 * 
 * If you mix this trait into a <a href="Suite.html">`Suite`</a>, you can initialize shared reassignable
 * fixture variables as well as shared mutable fixture objects in the constructor of the
 * class. Because each test will run in its own instance of the class, each test will
 * get a fresh copy of the instance variables. This is the approach to test isolation taken,
 * for example, by the JUnit framework. `OneInstancePerTest` can, therefore,
 * be handy when porting JUnit tests to ScalaTest.
 * 
 *
 * Here's an example of `OneInstancePerTest` being used in a `FunSuite`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.FunSuite
 * import org.scalatest.OneInstancePerTest
 * import collection.mutable.ListBuffer
 * 
 * class MySuite extends FunSuite with OneInstancePerTest {
 * 
 *   val builder = new StringBuilder("ScalaTest is ")
 *   val buffer = new ListBuffer[String]
 * 
 *   test("easy") {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   test("fun") {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *   }
 * }
 * }}}
 *
 * `OneInstancePerTest` is supertrait to <a href="ParallelTestExecution.html">`ParallelTestExecution`</a>, in which
 * running each test in its own instance is intended to make it easier to write suites of tests that run in parallel (by reducing the likelihood
 * of concurrency bugs in those suites.) `OneInstancePerTest` is also supertrait to the ''path'' traits,
 * <a href="path/FunSpec.html">`path.FunSpec`</a> and <a href="path/FreeSpec.html">`path.FreeSpec`</a>, to make it obvious
 * these traits run each test in a new, isolated instance.
 * 
 * 
 * For the details on how `OneInstancePerTest` works, see the documentation for methods `runTests` and `runTest`,
 * which this trait overrides.
 * 
 * 
 * @author Bill Venners
 */
trait OneInstancePerTest extends SuiteMixin {
  
  this: Suite =>

  /**
   * Modifies the behavior of `super.runTest` to facilitate running each test in its
   * own instance of this `Suite`'s class.
   *
   * This trait's implementation of `runTest` 
   * uses the `runTestInNewInstance` flag of the passed `Args` object to determine whether this instance is the general instance responsible
   * for running all tests in the suite (`runTestInNewInstance` is `true`), or a test-specific instance
   * responsible for running just one test (`runTestInNewInstance` is `false`).
   * Note that these `Boolean` values are reverse those used by `runTests`, because `runTests` always inverts the `Boolean` value
   * of `runTestInNewInstance` when invoking `runTest`.
   * 
   * 
   * If `runTestInNewInstance` is `true`, this trait's implementation of this method creates a new instance of this class (by
   * invoking `newInstance` on itself), then invokes `run` on the new instance,
   * passing in `testName`, wrapped in a `Some`, and `args` unchanged.
   * (''I.e.'', the `Args` object passed to `runTest` is forwarded as is to `run`
   * on the new instance, including with `runTestInNewInstance` set.)
   * If the invocation of either `newInstance` on this
   * `Suite` or `run` on a newly created instance of this `Suite`
   * completes abruptly with an exception, then this `runTests` method will complete
   * abruptly with the same exception.
   * 
   * 
   * If `runTestInNewInstance` is `false`, this trait's implementation of this method simply invokes `super.runTest`,
   * passing along the same `testName` and `args` objects.
   * 
   *
   * @param testName the name of one test to execute.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
   */
  protected abstract override def runTest(testName: String, args: Args): Status = {

    if (args.runTestInNewInstance) {
      // In initial instance, so create a new test-specific instance for this test and invoke run on it.
      val oneInstance = newInstance
      oneInstance.run(Some(testName), args)
    }
    else // Therefore, in test-specific instance, so run the test.
      super.runTest(testName, args)
  }

  /**
   * Modifies the behavior of `super.runTests` to facilitate running each test in its
   * own instance of this `Suite`'s class.
   *
   * This trait's implementation of `runTest` 
   * uses the `runTestInNewInstance` flag of the passed `Args` object to determine whether this instance is the general instance responsible
   * for running all tests in the suite (`runTestInNewInstance` is `false`), or a test-specific instance
   * responsible for running just one test (`runTestInNewInstance` is `true`). Note that these `Boolean` values are
   * reverse those used by `runTest`, because `runTests` always inverts the `Boolean` value of
   * `runTestInNewInstance` when invoking `runTest`.
   * 
   * 
   * If `runTestInNewInstance` is `false`, this trait's implementation of this method will invoke
   * `super.runTests`, passing along `testName` and `args`, but with the 
   * `runTestInNewInstance` flag set to `true`. By setting `runTestInNewInstance` to
   * `true`, `runTests` is telling `runTest` to create a new instance to run each test.
   * 
   *
   * If `runTestInNewInstance` is `true`, this trait's implementation of this method will invoke
   * `runTest` directly, passing in `testName.get` and the `args` object, with
   * the `runTestInNewInstance` flag set to `false`. By setting `runTestInNewInstance` to
   * `false`, `runTests` is telling `runTest` that this is the test-specific instance,
   * so it should just run the specified test.
   * 
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
   *
   * @throws NullPointerException if any of the passed parameters is `null`.
   * @throws IllegalArgumentException if `testName` is defined, but no test with the specified test name
   *     exists in this `Suite`, or if `runTestInNewInstance` is `true`, but `testName`
   *     is empty.
   */
  protected abstract override def runTests(testName: Option[String], args: Args): Status = {

    requireNonNull(testName, args)

    if (args.runTestInNewInstance) {
      if (testName.isEmpty)
        throw new IllegalArgumentException("args.runTestInNewInstance was true, but testName was not defined")
      // In test-specific instance, so run the test. (We are removing RTINI
      // so that runTest will realize it is in the test-specific instance.)
      runTest(testName.get, args.copy(runTestInNewInstance = false))
    }
    else {
      // In initial instance, so set the RTINI flag and call super.runTests, which
      // will go through any scopes and call runTest as usual. If this method was called
      // via super.runTests from PTE, the TestSortingReporter and WrappedDistributor
      // will already be in place.
      super.runTests(testName, args.copy(runTestInNewInstance = true))
    }
  }
  
/*
Just read through the code again to refresh my memory of how the runTestInNewInstance flag works.
The reason I was a bit confused is it kind of means the opposite thing in runTests as runTest. In
runTests, it will initially be not set, i.e., the first time someone calls into run. OIPT.runTests
will notice this and SET it, as a note to self, and call super.runTests. super.runTests will do
its thing, including executing scopes where need be, and call runTest. That will end up back in this
trait's runTest, which will look and see the flag is set. So that means DO run it in a new instance.
This traits' runTest will create that new instance and call run, leaving the flag set. Reason is that
in the test-specific instance, this same code will execute, but this time, the flag will be set already
on entry into runTests. So this time, runTests, knows this is the test-specific instance, so it just
direclty calls runTest, but sets the flag to false. In runTest, now the flag is false, so it just
executes the test in this test-specific instance. So in short,

This instance is the general instance iff:
- In runTests, the runTestInNewInstance flag is false on entry
- In runTest, if the runTestInNewInstance flag is true on entry

This is the test-specific instance iff:
- In runTests, the runTestInNewInstance flag is true on entry
- In runTest, if the runTestInNewInstance flag is false on entry

This is why in BeforeAndAfterAll, we only execute the beforeAll/afterAll code if the flag is false,
  because we only want to do that from the general instance. This is done from run itself.

In BeforeAndAfter and BeforeAndAfterEach, we want to only execute beforeEach/afterEach code in
  the test-specific instance. This is done from runTest, so that means we should only
  do it if the flag is false.
*/
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
   *   class InnerSuite extends Suite with OneInstancePerTest {
   *     def testOne() {}
   *     def testTwo() {}
   *     override def newInstance = new InnerSuite
   *   }
   * }
   * }}}
   */
  // SKIP-SCALATESTJS-START
  def newInstance: Suite with OneInstancePerTest = this.getClass.newInstance.asInstanceOf[Suite with OneInstancePerTest]
  // SKIP-SCALATESTJS-END
  //SCALATESTJS-ONLY def newInstance: Suite with OneInstancePerTest
}

