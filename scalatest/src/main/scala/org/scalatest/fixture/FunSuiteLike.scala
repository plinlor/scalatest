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
import org.scalactic.source
import org.scalatest.Suite.autoTagClassAnnotations

/**
 * Implementation trait for class `fixture.FunSuite`, which is
 * a sister class to `org.scalatest.FunSuite` that can pass a
 * fixture object into its tests.
 *
 * <a href="FunSuite.html">`fixture.FunSuite`</a> is a class,
 * not a trait, to minimize compile time given there is a slight compiler
 * overhead to mixing in traits compared to extending classes. If you need
 * to mix the behavior of `fixture.FunSuite` into some other
 * class, you can use this trait instead, because class
 * `fixture.FunSuite` does nothing more than extend this trait and add a nice `toString` implementation.
 * 
 *
 * See the documentation of the class for a <a href="FunSuite.html">detailed
 * overview of `fixture.FunSuite`</a>.
 * 
 *
 * @author Bill Venners
 */
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
@Finders(Array("org.scalatest.finders.FunSuiteFinder"))
trait FunSuiteLike extends TestSuite with TestRegistration with Informing with Notifying with Alerting with Documenting { thisSuite =>

  private final val engine = new FixtureEngine[FixtureParam](Resources.concurrentFixtureFunSuiteMod, "FixtureFunSuite")

  import engine._

  private[scalatest] val sourceFileName = "FunSuiteLike.scala"

  /**
   * Returns an `Informer` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked from inside a scope,
   * it will forward the information to the current reporter immediately.  If invoked from inside a test function,
   * it will record the information and forward it to the current reporter only after the test completed, as `recordedEvents`
   * of the test completed event, such as `TestSucceeded`. If invoked at any other time, it will print to the standard output.
   * This method can be called safely by any thread.
   */
  protected def info: Informer = atomicInformer.get

  /**
   * Returns a `Notifier` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `fixture.FunSuite` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an `Alerter` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `fixture.FunSuite` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def alert: Alerter = atomicAlerter.get

  /**
   * Returns a `Documenter` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked from inside a scope,
   * it will forward the information to the current reporter immediately.  If invoked from inside a test function,
   * it will record the information and forward it to the current reporter only after the test completed, as `recordedEvents`
   * of the test completed event, such as `TestSucceeded`. If invoked at any other time, it will print to the standard output.
   * This method can be called safely by any thread.
   */
  protected def markup: Documenter = atomicDocumenter.get

  final def registerTest(testText: String, testTags: Tag*)(testFun: FixtureParam => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -1
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    engine.registerTest(testText, Transformer(testFun), Resources.testCannotBeNestedInsideAnotherTest, "FunSuite.scala", "registerTest", 4, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
  }

  final def registerIgnoredTest(testText: String, testTags: Tag*)(testFun: FixtureParam => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -4
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -5
    engine.registerIgnoredTest(testText, Transformer(testFun), Resources.testCannotBeNestedInsideAnotherTest, "FunSuite.scala", "registerIgnoredTest", 4, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

  class ResultOfTestInvocation(testName: String, testTags: Tag*) {
    def apply(testFun: FixtureParam => Any /* Assertion */)(implicit pos: source.Position): Unit = {
      // SKIP-SCALATESTJS-START
      val stackDepth = 3
      val stackDepthAdjustment = -2
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 5
      //SCALATESTJS-ONLY val stackDepthAdjustment = -5
      engine.registerTest(testName, Transformer(testFun), Resources.testCannotAppearInsideAnotherTest, sourceFileName, "apply", stackDepth, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
    }

    def apply(testFun: () => Any /* Assertion */)(implicit pos: source.Position): Unit = {
      // SKIP-SCALATESTJS-START
      val stackDepth = 3
      val stackDepthAdjustment = -2
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 6
      //SCALATESTJS-ONLY val stackDepthAdjustment = -6
      engine.registerTest(testName, Transformer(new NoArgTestWrapper(testFun)), Resources.testCannotAppearInsideAnotherTest, sourceFileName, "apply", stackDepth, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
    }
  }

  /**
   * Register a test with the specified name, optional tags, and function value that takes no arguments.
   * This method will register the test for later execution via an invocation of one of the `run`
   * methods. The passed test name must not have been registered previously on
   * this `FunSuite` instance.
   *
   * @param testName the name of the test
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws NotAllowedException if `testName` had been registered previously
   * @throws NullArgumentException if `testName` or any passed test tag is `null`
   */
  protected def test(testName: String, testTags: Tag*): ResultOfTestInvocation = new ResultOfTestInvocation(testName, testTags: _*)
/*
  protected def test(testName: String, testTags: Tag*)(testFun: FixtureParam => Any /* Assertion */) {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -2
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -6
    engine.registerTest(testName, Transformer(testFun), Resources.testCannotAppearInsideAnotherTest, sourceFileName, "test", stackDepth, stackDepthAdjustment, None, None, None, testTags: _*)
  }
*/

  class ResultOfIgnoreInvocation(testName: String, testTags: Tag*) {
    def apply(testFun: FixtureParam => Any /* Assertion */)(implicit pos: source.Position): Unit = {
      // SKIP-SCALATESTJS-START
      val stackDepth = 3
      val stackDepthAdjustment = -3
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 5
      //SCALATESTJS-ONLY val stackDepthAdjustment = -6
      engine.registerIgnoredTest(testName, Transformer(testFun), Resources.ignoreCannotAppearInsideATest, sourceFileName, "apply", stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
    }

    def apply(testFun: () => Any /* Assertion */)(implicit pos: source.Position): Unit = {
      // SKIP-SCALATESTJS-START
      val stackDepth = 3
      val stackDepthAdjustment = -3
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 5
      //SCALATESTJS-ONLY val stackDepthAdjustment = -6
      engine.registerIgnoredTest(testName, Transformer(new NoArgTestWrapper(testFun)), Resources.ignoreCannotAppearInsideATest, sourceFileName, "apply", stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
    }
  }

  /**
   * Register a test to ignore, which has the specified name, optional tags, and function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the `run`
   * methods. This method exists to make it easy to ignore an existing test by changing the call to `test`
   * to `ignore` without deleting or commenting out the actual test code. The test will not be run, but a
   * report will be sent that indicates the test was ignored. The passed test name must not have been registered previously on
   * this `FunSuite` instance.
   *
   * @param testName the name of the test
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws NotAllowedException if `testName` had been registered previously
   */
  protected def ignore(testName: String, testTags: Tag*): ResultOfIgnoreInvocation = new ResultOfIgnoreInvocation(testName, testTags: _*)
/*
  protected def ignore(testName: String, testTags: Tag*)(testFun: FixtureParam => Any /* Assertion */) {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -7
    engine.registerIgnoredTest(testName, Transformer(testFun), Resources.ignoreCannotAppearInsideATest, sourceFileName, "ignore", stackDepth, stackDepthAdjustment, None, testTags: _*)
  }
*/

  /**
   * An immutable `Set` of test names. If this `fixture.FunSuite` contains no tests, this method returns an empty `Set`.
   *
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's iterator will
   * return those names in the order in which the tests were registered.
   * 
   *
   * @return the `Set` of test names
   */
  override def testNames: Set[String] = {
    InsertionOrderSet(atomic.get.testNamesList)
  }

  /**
   * Run a test. This trait's implementation runs the test registered with the name specified by `testName`.
   *
   * @param testName the name of one test to run.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
   * @throws IllegalArgumentException if `testName` is defined but a test with that name does not exist on this `fixture.FunSuite`
   * @throws NullArgumentException if `testName` or `args` is `null`.
   */
  protected override def runTest(testName: String, args: Args): Status = {

    def invokeWithFixture(theTest: TestLeaf): Outcome = {
      theTest.testFun match {
        case transformer: org.scalatest.fixture.Transformer[_] =>
          transformer.exceptionalTestFun match {
            case wrapper: NoArgTestWrapper[_, _] =>
              withFixture(new FixturelessTestFunAndConfigMap(testName, wrapper.test, args.configMap))
            case fun => withFixture(new TestFunAndConfigMap(testName, fun, args.configMap))
          }
        case other =>
          other match {
            case wrapper: NoArgTestWrapper[_, _] =>
              withFixture(new FixturelessTestFunAndConfigMap(testName, wrapper.test, args.configMap))
            case fun => withFixture(new TestFunAndConfigMap(testName, fun, args.configMap))
          }
      }
    }

    runTestImpl(thisSuite, testName, args, true, invokeWithFixture)
  }

  /**
   * A `Map` whose keys are `String` tag names to which tests in this `fixture.FunSuite` belong, and values
   * the `Set` of test names that belong to each tag. If this `fixture.FunSuite` contains no tags, this method returns an empty
   * `Map`.
   *
   * This trait's implementation returns tags that were passed as strings contained in `Tag` objects passed to
   * methods `test` and `ignore`.
   * 
   *
   * In addition, this trait's implementation will also auto-tag tests with class level annotations.
   * For example, if you annotate @Ignore at the class level, all test methods in the class will be auto-annotated with @Ignore.
   * 
   */
  override def tags: Map[String, Set[String]] = autoTagClassAnnotations(atomic.get.tagsMap, this)

  /**
   * Run zero to many of this `fixture.FunSuiteLike`'s tests.
   * 
   *
   * This method takes a `testName` parameter that optionally specifies a test to invoke.
   * If `testName` is `Some`, this trait's implementation of this method
   * invokes `runTest` on this object with passed `args`.
   * 
   *
   * This method takes an `args` that contains a `Set` of tag names that should be included (`tagsToInclude`), and a `Set`
   * that should be excluded (`tagsToExclude`), when deciding which of this `Suite`'s tests to execute.
   * If `tagsToInclude` is empty, all tests will be executed
   * except those those belonging to tags listed in the `tagsToExclude` `Set`. If `tagsToInclude` is non-empty, only tests
   * belonging to tags mentioned in `tagsToInclude`, and not mentioned in `tagsToExclude`
   * will be executed. However, if `testName` is `Some`, `tagsToInclude` and `tagsToExclude` are essentially ignored.
   * Only if `testName` is `None` will `tagsToInclude` and `tagsToExclude` be consulted to
   * determine which of the tests named in the `testNames` `Set` should be run. For more information on trait tags, see the main documentation for this trait.
   * 
   *
   * If `testName` is `None`, this trait's implementation of this method
   * invokes `testNames` on this `Suite` to get a `Set` of names of tests to potentially execute.
   * (A `testNames` value of `None` essentially acts as a wildcard that means all tests in
   * this `Suite` that are selected by `tagsToInclude` and `tagsToExclude` should be executed.)
   * For each test in the `testName` `Set`, in the order
   * they appear in the iterator obtained by invoking the `elements` method on the `Set`, this trait's implementation
   * of this method checks whether the test should be run based on the `tagsToInclude` and `tagsToExclude` `Set`s.
   * If so, this implementation invokes `runTest` with passed `args`.
   * 
   *
   * @param testName an optional name of one test to execute. If `None`, all relevant tests should be executed.
   *                 I.e., `None` acts like a wildcard that means execute all relevant tests in this `FunSpec`.
   * @param args the `Args` to which results will be reported
   * @return a `Status` object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
   * @throws NullArgumentException if any of `testName` or `args` is `null`.
   */
  protected override def runTests(testName: Option[String], args: Args): Status = {
    runTestsImpl(thisSuite, testName, args, info, true, runTest)
  }

  override def run(testName: Option[String], args: Args): Status = {
    runImpl(thisSuite, testName, args: Args, super.run)
  }

  /**
   * Registers shared tests.
   *
   * This method enables the following syntax for shared tests in a `fixture.FunSuite`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * testsFor(nonEmptyStack(lastValuePushed))
   * }}}
   *
   * This method just provides syntax sugar intended to make the intent of the code clearer.
   * Because the parameter passed to it is
   * type `Unit`, the expression will be evaluated before being passed, which
   * is sufficient to register the shared tests. For examples of shared tests, see the
   * <a href="../FunSuite.html#SharedTests">Shared tests section</a> in the main documentation for
   * trait `FunSuite`.
   * 
   */
  protected def testsFor(unit: Unit): Unit = {}

  import scala.language.implicitConversions

  /**
   * Implicitly converts a function that takes no parameters and results in `PendingStatement` to
   * a function from `FixtureParam` to `Any`, to enable pending tests to registered as by-name parameters
   * by methods that require a test function that takes a `FixtureParam`.
   *
   * This method makes it possible to write pending tests as simply `(pending)`, without needing
   * to write `(fixture => pending)`.
   * 
   *
   * @param f a function
   * @return a function of `FixtureParam => Any`
   */
  protected implicit def convertPendingToFixtureFunction(f: => PendingStatement): (FixtureParam => Any /* Assertion */) = {
    fixture => { f; Succeeded }
  }

  /**
   * Implicitly converts a function that takes no parameters and results in `Any` to
   * a function from `FixtureParam` to `Any`, to enable no-arg tests to registered
   * by methods that require a test function that takes a `FixtureParam`.
   *
   * @param fun a function
   * @return a function of `FixtureParam => Any`
   */
/*
  protected implicit def convertNoArgToFixtureFunction(fun: () => Any /* Assertion */): (FixtureParam => Any /* Assertion */) =
    new NoArgTestWrapper(fun)
*/

  /**
   * Suite style name.
   *
   * @return `org.scalatest.fixture.FunSuite`
   */
  final override val styleName: String = "org.scalatest.fixture.FunSuite"

  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = createTestDataFor(testName, theConfigMap, this)
}
