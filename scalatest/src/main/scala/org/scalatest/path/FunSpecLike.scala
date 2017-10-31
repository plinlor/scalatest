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
package org.scalatest.path

import org.scalatest._
import org.scalatest.exceptions._
import org.scalactic.{source, Prettifier}
import org.scalatest.words.BehaveWord
import Suite.autoTagClassAnnotations
import org.scalatest.PathEngine.isInTargetPath

/**
 * Implementation trait for class `path.FunSpec`, which is
 * a sister class to `org.scalatest.FunSpec` that isolates
 * tests by running each test in its own instance of the test class,
 * and for each test, only executing the ''path'' leading to that test.
 * 
 * <a href="FunSpec.html">`path.FunSpec`</a> is a class, not a trait,
 * to minimize compile time given there is a slight compiler overhead to
 * mixing in traits compared to extending classes. If you need to mix the
 * behavior of `path.FunSpec` into some other class, you can use this
 * trait instead, because class `path.FunSpec` does nothing more than
 * extend this trait and add a nice `toString` implementation.
 * 
 *
 * See the documentation of the class for a <a href="FunSpec.html">detailed
 * overview of `path.FunSpec`</a>.
 * 
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.FunSpecFinder"))
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
trait FunSpecLike extends org.scalatest.Suite with OneInstancePerTest with Informing with Notifying with Alerting with Documenting { thisSuite =>
  
  private final val engine = PathEngine.getEngine()
  import engine._

  // SKIP-SCALATESTJS-START
  override def newInstance: FunSpecLike = this.getClass.newInstance.asInstanceOf[FunSpecLike]
  // SKIP-SCALATESTJS-END
  //SCALATESTJS-ONLY override def newInstance: FunSpecLike

  /**
   * Returns an `Informer` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor (including within a test, since
   * those are invoked during construction in a `path.FunSpec`, it
   * will register the passed string for forwarding later when `run` is invoked. If invoked from inside a test function,
   * it will record the information and forward it to the current reporter only after the test completed, as `recordedEvents`
   * of the test completed event, such as `TestSucceeded`.  If invoked at any other time, it will print to the standard output.
   * This method can be called safely by any thread.
   */
  protected def info: Informer = atomicInformer.get

  /**
   * Returns a `Notifier` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `path.FunSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an `Alerter` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `path.FunSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def alert: Alerter = atomicAlerter.get

  /**
   * Returns a `Documenter` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor (including within a test, since
   * those are invoked during construction in a `path.FunSpec`, it
   * will register the passed string for forwarding later when `run` is invoked. If invoked from inside a test function,
   * it will record the information and forward it to the current reporter only after the test completed, as `recordedEvents`
   * of the test completed event, such as `TestSucceeded`.  If invoked at any other time, it will print to the standard output.
   * This method can be called safely by any thread.
   */
  protected def markup: Documenter = atomicDocumenter.get

  /**
   * Class that, via an instance referenced from the `it` field,
   * supports test (and shared test) registration in `FunSpec`s.
   *
   * This class supports syntax such as the following test registration:
   * 
   *
   * {{{ class="stExamples">
   * it("should be empty")
   * ^
   * }}}
   *
   * and the following shared test registration:
   * 
   *
   * {{{ class="stExamples">
   * it should behave like nonFullStack(stackWithOneItem)
   * ^
   * }}}
   *
   * For more information and examples, see the <a href="FunSpec.html">main documentation for `path.FunSpec`</a>.
   * 
   */
  protected class ItWord {

    /**
     * Supports test registration.
     *
     * This trait's implementation of this method will decide whether to register the text and invoke the passed function
     * based on whether or not this is part of the current "test path." For the details on this process, see
     * the <a href="#howItExecutes">How it executes</a> section of the main documentation for
     * trait `org.scalatest.path.FunSpec`.
     * 
     *
     * @param testText the test text, which will be combined with the descText of any surrounding describers
     * to form the test name
     * @param testTags the optional list of tags for this test
     * @param testFun the test function
     * @throws DuplicateTestNameException if a test with the same name has been registered previously
     * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
     * @throws NullArgumentException if `specText` or any passed test tag is `null`
     */
    def apply(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */)(implicit pos: source.Position): Unit = {
      // SKIP-SCALATESTJS-START
      val stackDepth = 3
      val stackDepthAdjustment = -2
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 5
      //SCALATESTJS-ONLY val stackDepthAdjustment = -4
      handleTest(thisSuite, testText, Transformer(testFun _), Resources.itCannotAppearInsideAnotherItOrThey, "FunSpecLike.scala", "apply", stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
    }
    
    /**
     * Supports the registration of shared tests.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{ class="stExamples">
     * it should behave like nonFullStack(stackWithOneItem)
     *    ^
     * }}}
     *
     * For examples of shared tests, see the <a href="../FunSpec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait `org.scalatest.FunSpec`.
     * 
     */
    def should(behaveWord: BehaveWord) = behaveWord

    /**
     * Supports the registration of shared tests.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{ class="stExamples">
     * it must behave like nonFullStack(stackWithOneItem)
     *    ^
     * }}}
     *
     * For examples of shared tests, see the <a href="../FunSpec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait `org.scalatest.FunSpec`.
     * 
     */
    def must(behaveWord: BehaveWord) = behaveWord
  }

  /**
   * Supports test (and shared test) registration in `FunSpec`s.
   *
   * This field supports syntax such as the following:
   * 
   *
   * {{{ class="stExamples">
   * it("should be empty")
   * ^
   * }}}
   *
   * {{{ class="stExamples"
   * it should behave like nonFullStack(stackWithOneItem)
   * ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the main documentation for this trait.
   * 
   */
  protected val it = new ItWord

  /**
   * Class that, via an instance referenced from the `they` field,
   * supports test (and shared test) registration in `FunSpec`s.
   *
   * This class supports syntax such as the following test registration:
   * 
   *
   * {{{ class="stExamples">
   * they("should be empty")
   * ^
   * }}}
   *
   * and the following shared test registration:
   * 
   *
   * {{{ class="stExamples">
   * they should behave like nonFullStack(stackWithOneItem)
   * ^
   * }}}
   *
   * For more information and examples, see the <a href="FunSpec.html">main documentation for `path.FunSpec`</a>.
   * 
   */
  protected class TheyWord {

    /**
     * Supports test registration.
     *
     * This trait's implementation of this method will decide whether to register the text and invoke the passed function
     * based on whether or not this is part of the current "test path." For the details on this process, see
     * the <a href="#howItExecutes">How it executes</a> section of the main documentation for
     * trait `org.scalatest.path.FunSpec`.
     * 
     *
     * @param testText the test text, which will be combined with the descText of any surrounding describers
     * to form the test name
     * @param testTags the optional list of tags for this test
     * @param testFun the test function
     * @throws DuplicateTestNameException if a test with the same name has been registered previously
     * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
     * @throws NullArgumentException if `specText` or any passed test tag is `null`
     */
    def apply(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */)(implicit pos: source.Position): Unit = {
      handleTest(thisSuite, testText, Transformer(testFun _), Resources.theyCannotAppearInsideAnotherItOrThey, "FunSpecLike.scala", "apply", 3, -2, None, Some(pos), testTags: _*)
    }
 
    /**
     * Supports the registration of shared tests.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{ class="stExamples">
     * they should behave like nonFullStack(stackWithOneItem)
     *      ^
     * }}}
     *
     * For examples of shared tests, see the <a href="../FunSpec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait `org.scalatest.FunSpec`.
     * 
     */
    def should(behaveWord: BehaveWord) = behaveWord

    /**
     * Supports the registration of shared tests.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{ class="stExamples">
     * they must behave like nonFullStack(stackWithOneItem)
     *      ^
     * }}}
     *
     * For examples of shared tests, see the <a href="../FunSpec.html#SharedTests">Shared tests section</a>
     * in the main documentation for trait `org.scalatest.FunSpec`.
     * 
     */
    def must(behaveWord: BehaveWord) = behaveWord
  }

  /**
   * Supports test (and shared test) registration in `FunSpec`s.
   *
   * This field supports syntax such as the following:
   * 
   *
   * {{{ class="stExamples">
   * it("should be empty")
   * ^
   * }}}
   *
   * {{{ class="stExamples"
   * it should behave like nonFullStack(stackWithOneItem)
   * ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the main documentation for this trait.
   * 
   */
  protected val they = new TheyWord
  
  /**
   * Supports registration of a test to ignore.
   *
   * For more information and examples of this method's use, see the
   * <a href="../FunSpec.html#ignoredTests">Ignored tests</a> section in the main documentation for sister
   * trait `org.scalatest.FunSpec`. Note that a separate instance will be created for an ignored test,
   * and the path to the ignored test will be executed in that instance, but the test function itself will not
   * be executed. Instead, a `TestIgnored` event will be fired.
   * 
   *
   * @param testText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws NullArgumentException if `specText` or any passed test tag is `null`
   */
  protected def ignore(testText: String, testTags: Tag*)(testFun: => Unit /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -2
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    // Might not actually register it. Only will register it if it is its turn.
    handleIgnoredTest(testText, Transformer(testFun _), Resources.ignoreCannotAppearInsideAnItOrAThey, "FunSpecLike.scala", "ignore", stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }
  
  /**
   * Describe a &ldquo;subject&rdquo; being specified and tested by the passed function value. The
   * passed function value may contain more describers (defined with `describe`) and/or tests
   * (defined with `it`).
   *
   * This class's implementation of this method will decide whether to
   * register the description text and invoke the passed function
   * based on whether or not this is part of the current "test path." For the details on this process, see
   * the <a href="#howItExecutes">How it executes</a> section of the main documentation for trait
   * `org.scalatest.path.FunSpec`.
   * 
   */
  protected def describe(description: String)(fun: => Unit)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6

    try {
      handleNestedBranch(description, None, fun, Resources.describeCannotAppearInsideAnIt, "FunSpecLike.scala", "describe", stackDepth, -2, None, Some(pos))
    }
    catch {
      case e: TestFailedException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideItOrTheyClauseNotDescribeClause, Some(e), e.position.getOrElse(pos))
      case e: TestCanceledException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideItOrTheyClauseNotDescribeClause, Some(e), e.position.getOrElse(pos))
      case e: DuplicateTestNameException => throw new NotAllowedException(FailureMessages.exceptionWasThrownInDescribeClause(Prettifier.default, UnquotedString(e.getClass.getName), description, e.getMessage), Some(e), e.position.getOrElse(pos))
      case other: Throwable if (!Suite.anExceptionThatShouldCauseAnAbort(other)) => throw new NotAllowedException(FailureMessages.exceptionWasThrownInDescribeClause(Prettifier.default, UnquotedString(other.getClass.getName), description, other.getMessage), Some(other), pos)
      case other: Throwable => throw other
    }
  }
  
  /**
   * Supports shared test registration in `path.FunSpec`s.
   *
   * This field supports syntax such as the following:
   * 
   *
   * {{{ class="stExamples">
   * it should behave like nonFullStack(stackWithOneItem)
   *           ^
   * }}}
   *
   * For more information and examples of the use of <cod>behave`, see the
   * <a href="../FunSpec.html#SharedTests">Shared tests</a> section in the main documentation for sister
   * trait `org.scalatest.FunSpec`.
   * 
   */
  protected val behave = new BehaveWord

  /**
   * An immutable `Set` of test names. If this `FunSpec` contains no tests, this method returns an
   * empty `Set`.
   *
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * 
   *
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space. For example, consider this `FunSpec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.path
   *
   * class StackSpec extends path.FunSpec {
   *   describe("A Stack") {
   *     describe("when not empty") {
   *       "must allow me to pop" in {}
   *     }
   *     describe("when not full") {
   *       "must allow me to push" in {}
   *     }
   *   }
   * }
   * }}}
   *
   * Invoking `testNames` on this `FunSpec` will yield a set that contains the following
   * two test name strings:
   * 
   *
   * {{{
   * "A Stack when not empty must allow me to pop"
   * "A Stack when not full must allow me to push"
   * }}}
   *
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * 
   */
  final override def testNames: Set[String] = {
    ensureTestResultsRegistered(thisSuite)
    InsertionOrderSet(atomic.get.testNamesList)
  }

  /**
   * The total number of tests that are expected to run when this `path.FunSpec`'s `run` method
   * is invoked.
   *
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * 
   *
   * This trait's implementation of this method returns the size of the `testNames` `List`, minus
   * the number of tests marked as ignored as well as any tests excluded by the passed `Filter`.
   * 
   *
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * 
   *
   * @param filter a `Filter` with which to filter tests to count based on their tags
   */
  final override def expectedTestCount(filter: Filter): Int = {
    ensureTestResultsRegistered(thisSuite)
    super.expectedTestCount(filter)
  }

  /**
   * Runs a test.
   *
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * 
   *
   * This trait's implementation reports the test results registered with the name specified by
   * `testName`. Each test's name is a concatenation of the text of all describers surrounding a test,
   * from outside in, and the test's  spec text, with one space placed between each item. (See the documentation
   * for `testNames` for an example.)
   *
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * 
   *
   * @param testName the name of one test to execute.
   * @param reporter the `Reporter` to which results will be reported
   * @param stopper the `Stopper` that will be consulted to determine whether to stop execution early.
   * @param configMap a `Map` of properties that can be used by this `FreeSpec`'s executing tests.
   * @throws NullArgumentException if any of `testName`, `reporter`, `stopper`, or `configMap`
   *     is `null`.
   */
  final protected override def runTest(testName: String, args: Args): Status = {

    ensureTestResultsRegistered(thisSuite)
    
    def dontInvokeWithFixture(theTest: TestLeaf): Outcome = {
      theTest.testFun()
    }

    runTestImpl(thisSuite, testName, args, true, dontInvokeWithFixture)
  }

  /**
   * A `Map` whose keys are `String` tag names to which tests in this `path.FreeSpec`
   * belong, and values the `Set` of test names that belong to each tag. If this `path.FreeSpec`
   * contains no tags, this method returns an empty `Map`.
   *
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * 
   *
   * This trait's implementation returns tags that were passed as strings contained in `Tag` objects passed
   * to methods `it` and `ignore`.
   * 
   * 
   * In addition, this trait's implementation will also auto-tag tests with class level annotations.  
   * For example, if you annotate @Ignore at the class level, all test methods in the class will be auto-annotated with @Ignore.
   * 
   *
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * 
   */
  final override def tags: Map[String, Set[String]] = {
    ensureTestResultsRegistered(thisSuite)
    autoTagClassAnnotations(atomic.get.tagsMap, this)
  }

  /**
   * Runs this `path.FunSpec`, reporting test results that were registered when the tests
   * were run, each during the construction of its own instance.
   *
   * This trait's implementation of this method will first ensure that the results of all tests, each run its its
   * own instance executing only the path to the test, are registered. For details on this process see the
   * <a href="#howItExecutes">How it executes</a> section in the main documentation for this trait.
   * 
   *
   * <p>If `testName` is `None`, this trait's implementation of this method
   * will report the registered results for all tests except any excluded by the passed `Filter`.
   * If `testName` is defined, it will report the results of only that named test. Because a
   * `path.FunSpec` is not allowed to contain nested suites, this trait's implementation of
   * this method does not call `runNestedSuites`.
   * 
   *
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * 
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   *
   *@throws NullArgumentException if any passed parameter is `null`.
   * @throws IllegalArgumentException if `testName` is defined, but no test with the specified test name
   *     exists in this `Suite`
   */
  final override def run(testName: Option[String], args: Args): Status = {
    ensureTestResultsRegistered(thisSuite)
    runPathTestsImpl(thisSuite, testName, args, info, true, runTest)
  }

  /**
   * This lifecycle method is unused by this trait, and will complete abruptly with
   * `UnsupportedOperationException` if invoked.
   *
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * 
   */
  final protected override def runTests(testName: Option[String], args: Args): Status = {
    throw new UnsupportedOperationException
    // ensureTestResultsRegistered(isAnInitialInstance, this)
    // runTestsImpl(thisSuite, testName, reporter, stopper, filter, configMap, distributor, tracker, info, true, runTest)
  }

  /**
   * This lifecycle method is unused by this trait, and is implemented to do nothing. If invoked, it will
   * just return immediately.
   *
   * Nested suites are not allowed in a `path.FunSpec`. Because
   * a `path.FunSpec` executes tests eagerly at construction time, registering the results of
   * those test runs and reporting them later, the order of nested suites versus test runs would be different
   * in a `org.scalatest.path.FunSpec` than in an `org.scalatest.FunSpec`. In an
   * `org.scalatest.FunSpec`, nested suites are executed then tests are executed. In an
   * `org.scalatest.path.FunSpec` it would be the opposite. To make the code easy to reason about,
   * therefore, this is just not allowed. If you want to add nested suites to a `path.FunSpec`, you can
   * instead wrap them all in a <a href="../Suites.html">`Suites`</a>
   * object and put them in whatever order you wish.
   * 
   *
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * 
   */
  final protected override def runNestedSuites(args: Args): Status = SucceededStatus

  /**
   * Returns an empty list.
   *
   * This lifecycle method is unused by this trait. If invoked, it will return an empty list, because
   * nested suites are not allowed in a `path.FunSpec`. Because
   * a `path.FunSpec` executes tests eagerly at construction time, registering the results of
   * those test runs and reporting them later, the order of nested suites versus test runs would be different
   * in a `org.scalatest.path.FunSpec` than in an `org.scalatest.FunSpec`. In an
   * `org.scalatest.FunSpec`, nested suites are executed then tests are executed. In an
   * `org.scalatest.path.FunSpec` it would be the opposite. To make the code easy to reason about,
   * therefore, this is just not allowed. If you want to add nested suites to a `path.FunSpec`, you can
   * instead wrap them all in a <a href="../Suites.html">`Suites`</a>
   * object and put them in whatever order you wish.
   * 
   *
   * This trait's implementation of this method is  marked as final. For insight onto why, see the
   * <a href="#sharedFixtures">Shared fixtures</a> section in the main documentation for this trait.
   * 
   */
  final override def nestedSuites: collection.immutable.IndexedSeq[Suite] = Vector.empty

  /**
   * Suite style name.
   */
  final override val styleName: String = "org.scalatest.path.FunSpec"
    
  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = {
    ensureTestResultsRegistered(thisSuite)
    createTestDataFor(testName, theConfigMap, this)
  }
}

