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

import org.scalactic._
import scala.concurrent.Future
import Suite.anExceptionThatShouldCauseAnAbort
import Suite.autoTagClassAnnotations
import java.util.ConcurrentModificationException
import java.util.concurrent.atomic.AtomicReference
import org.scalatest.exceptions.NotAllowedException

/**
 * Implementation trait for class `AsyncFeatureSpec`, which represents
 * a suite of tests in which each test represents one ''scenario'' of a
 * ''feature''.
 *
 * <a href="AsyncFeatureSpec.html">`AsyncFeatureSpec`</a> is a class, not a
 * trait, to minimize compile time given there is a slight compiler overhead to
 * mixing in traits compared to extending classes. If you need to mix the
 * behavior of `AsyncFeatureSpec` into some other class, you can use this
 * trait instead, because class `AsyncFeatureSpec` does nothing more than
 * extend this trait and add a nice `toString` implementation.
 * 
 *
 * See the documentation of the class for a <a href="AsyncFeatureSpec.html">detailed
 * overview of `AsyncFeatureSpec`</a>.
 * 
 *
 * @author Bill Venners
 */
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
@Finders(Array("org.scalatest.finders.FeatureSpecFinder"))
trait AsyncFeatureSpecLike extends AsyncTestSuite with AsyncTestRegistration with Informing with Notifying with Alerting with Documenting { thisSuite =>

  private final val engine = new AsyncEngine(Resources.concurrentFeatureSpecMod, "FeatureSpec")

  import engine._

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
   * `FunSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an `Alerter` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `FunSpec` is being executed, such as from inside a test function, it will forward the information to
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

  final def registerAsyncTest(testText: String, testTags: Tag*)(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
    engine.registerAsyncTest(Resources.scenario(testText.trim), transformToOutcome(testFun), Resources.testCannotBeNestedInsideAnotherTest, None, None, pos, testTags: _*)
  }

  final def registerIgnoredAsyncTest(testText: String, testTags: Tag*)(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
    engine.registerIgnoredAsyncTest(Resources.scenario(testText.trim), transformToOutcome(testFun), Resources.testCannotBeNestedInsideAnotherTest, None, pos, testTags: _*)
  }

  @deprecated("use Scenario instead", "ScalaTest 3.1.1")
  protected def scenario(specText: String, testTags: Tag*)(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit =
    Scenario(specText, testTags: _*)(testFun)(pos)

  /**
   * Register a test with the given spec text, optional tags, and test function value that takes no arguments.
   * An invocation of this method is called an &ldquo;example.&rdquo;
   *
   * This method will register the test for later execution via an invocation of one of the `execute`
   * methods. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for `testNames` for an example.) The resulting test name must not have been registered previously on
   * this `FeatureSpec` instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws NullArgumentException if `specText` or any passed test tag is `null`
   */
  protected def Scenario(specText: String, testTags: Tag*)(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
    engine.registerAsyncTest(Resources.scenario(specText.trim), transformToOutcome(testFun), Resources.scenarioCannotAppearInsideAnotherScenario, None, None, pos, testTags: _*)
  }

  /**
   * Register a test to ignore, which has the given spec text, optional tags, and test function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the `execute`
   * methods. This method exists to make it easy to ignore an existing test by changing the call to `it`
   * to `ignore` without deleting or commenting out the actual test code. The test will not be executed, but a
   * report will be sent that indicates the test was ignored. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for `testNames` for an example.) The resulting test name must not have been registered previously on
   * this `FeatureSpec` instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws NullArgumentException if `specText` or any passed test tag is `null`
   */
  protected def ignore(specText: String, testTags: Tag*)(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
    engine.registerIgnoredAsyncTest(Resources.scenario(specText), transformToOutcome(testFun), Resources.ignoreCannotAppearInsideAScenario, None, pos, testTags: _*)
  }

  @deprecated("use Feature instead", "ScalaTest 3.1.1")
  protected def feature(description: String)(fun: => Unit)(implicit pos: source.Position): Unit = Feature(description)(fun)(pos)

  /**
   * Describe a &ldquo;subject&rdquo; being specified and tested by the passed function value. The
   * passed function value may contain more describers (defined with `describe`) and/or tests
   * (defined with `it`). This trait's implementation of this method will register the
   * description string and immediately invoke the passed function.
   */
  protected def Feature(description: String)(fun: => Unit)(implicit pos: source.Position): Unit = {
    if (!currentBranchIsTrunk)
      throw new NotAllowedException(Resources.cantNestFeatureClauses, pos)

    try {
      registerNestedBranch(Resources.feature(description.trim), None, fun, Resources.featureCannotAppearInsideAScenario, None, pos)
    }
    catch {
      case e: exceptions.TestFailedException => throw new exceptions.NotAllowedException(FailureMessages.assertionShouldBePutInsideScenarioClauseNotFeatureClause, Some(e), e.position.getOrElse(pos))
      case e: exceptions.TestCanceledException => throw new exceptions.NotAllowedException(FailureMessages.assertionShouldBePutInsideScenarioClauseNotFeatureClause, Some(e), e.position.getOrElse(pos))
      case nae: exceptions.NotAllowedException => throw nae
      case e: exceptions.DuplicateTestNameException => throw new exceptions.NotAllowedException(FailureMessages.exceptionWasThrownInFeatureClause(Prettifier.default, UnquotedString(e.getClass.getName), description, e.getMessage), Some(e), e.position.getOrElse(pos))
      case other: Throwable if (!Suite.anExceptionThatShouldCauseAnAbort(other)) => throw new exceptions.NotAllowedException(FailureMessages.exceptionWasThrownInFeatureClause(Prettifier.default, UnquotedString(other.getClass.getName), description, other.getMessage), Some(other), pos)
      case other: Throwable => throw other
    }
  }

  /**
   * A `Map` whose keys are `String` names of tagged tests and whose associated values are
   * the `Set` of tag names for the test. If this `FeatureSpec` contains no tags, this method returns an empty `Map`.
   *
   * This trait's implementation returns tags that were passed as strings contained in `Tag` objects passed to
   * methods `scenario` and `ignore`.
   * 
   *
   * In addition, this trait's implementation will also auto-tag tests with class level annotations.
   * For example, if you annotate `@Ignore` at the class level, all test methods in the class will be auto-annotated with
   * `org.scalatest.Ignore`.
   * 
   */
  override def tags: Map[String, Set[String]] = autoTagClassAnnotations(atomic.get.tagsMap, this)

  /**
   * Run a test. This trait's implementation runs the test registered with the name specified by
   * `testName`. Each test's name is a concatenation of the text of all describers surrounding a test,
   * from outside in, and the test's  spec text, with one space placed between each item. (See the documenation
   * for `testNames` for an example.)
   *
   * @param testName the name of one test to execute.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
   *
   * @throws NullArgumentException if any of `testName`, `reporter`, `stopper`, or `configMap`
   *     is `null`.
   */
  protected override def runTest(testName: String, args: Args): Status = {
    def invokeWithAsyncFixture(theTest: TestLeaf): AsyncOutcome = {
      val theConfigMap = args.configMap
      val testData = testDataFor(testName, theConfigMap)
      InternalFutureOutcome(
        withFixture(
          new NoArgAsyncTest {
            val name = testData.name
            def apply(): FutureOutcome = { theTest.testFun().toFutureOutcome }
            val configMap = testData.configMap
            val scopes = testData.scopes
            val text = testData.text
            val tags = testData.tags
            val pos = testData.pos
          }
        ).underlying
      )
    }

    runTestImpl(thisSuite, testName, args, true, parallelAsyncTestExecution, invokeWithAsyncFixture)
  }

  /**
   * Run zero to many of this `FeatureSpec`'s tests.
   *
   * This method takes a `testName` parameter that optionally specifies a test to invoke.
   * If `testName` is `Some`, this trait's implementation of this method
   * invokes `runTest` on this object, passing in:
   * 
   *
   * <ul>
   * <li>`testName` - the `String` value of the `testName` `Option` passed
   *   to this method</li>
   * <li>`reporter` - the `Reporter` passed to this method, or one that wraps and delegates to it</li>
   * <li>`stopper` - the `Stopper` passed to this method, or one that wraps and delegates to it</li>
   * <li>`configMap` - the `configMap` passed to this method, or one that wraps and delegates to it</li>
   * </ul>
   *
   * This method takes a `Set` of tag names that should be included (`tagsToInclude`), and a `Set`
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
   * If so, this implementation invokes `runTest`, passing in:
   * 
   *
   * <ul>
   * <li>`testName` - the `String` name of the test to run (which will be one of the names in the `testNames` `Set`)</li>
   * <li>`reporter` - the `Reporter` passed to this method, or one that wraps and delegates to it</li>
   * <li>`stopper` - the `Stopper` passed to this method, or one that wraps and delegates to it</li>
   * <li>`configMap` - the `configMap` passed to this method, or one that wraps and delegates to it</li>
   * </ul>
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
   *
   * @throws NullArgumentException if any of the passed parameters is `null`.
   * @throws IllegalArgumentException if `testName` is defined, but no test with the specified test name
   *     exists in this `Suite`
   */
  protected override def runTests(testName: Option[String], args: Args): Status = {
    runTestsImpl(thisSuite, testName, args, false, parallelAsyncTestExecution, runTest)
  }

  /**
   * An immutable `Set` of test names. If this `FeatureSpec` contains no tests, this method returns an
   * empty `Set`.
   *
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space. For example, consider this `FeatureSpec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.FeatureSpec
   *
   * class StackSpec extends FeatureSpec {
   *   Feature("A Stack") {
   *     Scenario("(when not empty) must allow me to pop") {}
   *     Scenario("(when not full) must allow me to push") {}
   *   }
   * }
   * }}}
   *
   * Invoking `testNames` on this `FeatureSpec` will yield a set that contains the following
   * two test name strings:
   * 
   *
   * {{{
   * "A Stack (when not empty) must allow me to pop"
   * "A Stack (when not full) must allow me to push"
   * }}}
   */
  // override def testNames: Set[String] = ListSet(atomic.get.testsList.map(_.testName): _*)
  override def testNames: Set[String] = {
    InsertionOrderSet(atomic.get.testNamesList)
  }

  override def run(testName: Option[String], args: Args): Status = {
    runImpl(thisSuite, testName, args, parallelAsyncTestExecution, super.run)
  }

  @deprecated("use ScenariosFor instead", "ScalaTest 3.1.1")
  protected def scenariosFor(unit: Unit): Unit = ScenariosFor(unit)

  /**
   * Registers shared scenarios.
   *
   * This method enables the following syntax for shared scenarios in a `FeatureSpec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * ScenariosFor(nonEmptyStack(lastValuePushed))
   * }}}
   *
   * This method just provides syntax sugar intended to make the intent of the code clearer.
   * Because the parameter passed to it is
   * type `Unit`, the expression will be evaluated before being passed, which
   * is sufficient to register the shared scenarios. For examples of shared scenarios, see the
   * <a href="#sharedScenarios">Shared scenarios section</a> in the main documentation for this trait.
   * 
   */
  protected def ScenariosFor(unit: Unit): Unit = {}

  /**
   * Suite style name.
   */
  final override val styleName: String = "org.scalatest.FeatureSpec"

  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = createTestDataFor(testName, theConfigMap, this)
}
