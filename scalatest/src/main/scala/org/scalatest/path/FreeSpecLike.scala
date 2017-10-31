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


/**
 * Implementation trait for class `path.FreeSpec`, which is
 * a sister class to `org.scalatest.FreeSpec` that isolates
 * tests by running each test in its own instance of the test class, and
 * for each test, only executing the ''path'' leading to that test.
 * 
 * <a href="FreeSpec.html">`path.FreeSpec`</a> is a class, not a trait,
 * to minimize compile time given there is a slight compiler overhead to
 * mixing in traits compared to extending classes. If you need to mix the
 * behavior of `path.FreeSpec` into some other class, you can use this
 * trait instead, because class `path.FreeSpec` does nothing more than
 * extend this trait and add a nice `toString` implementation.
 * 
 *
 * See the documentation of the class for a <a href="FreeSpec.html">detailed
 * overview of `path.FreeSpec`</a>.
 * 
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.FreeSpecFinder"))
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
trait FreeSpecLike extends org.scalatest.Suite with OneInstancePerTest with Informing with Notifying with Alerting with Documenting { thisSuite =>
  
  private final val engine = PathEngine.getEngine()
  import engine._

  // SKIP-SCALATESTJS-START
  override def newInstance: FreeSpecLike = this.getClass.newInstance.asInstanceOf[FreeSpecLike]
  // SKIP-SCALATESTJS-END
  //SCALATESTJS-ONLY override def newInstance: FreeSpecLike

  /**
   * Returns an `Informer` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor (including within a test, since
   * those are invoked during construction in a `path.FreeSpec`, it
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
   * `path.FreeSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an `Alerter` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `path.FreeSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def alert: Alerter = atomicAlerter.get

  /**
   * Returns a `Documenter` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor (including within a test, since
   * those are invoked during construction in a `path.FreeSpec`, it
   * will register the passed string for forwarding later when `run` is invoked. If invoked from inside a test function,
   * it will record the information and forward it to the current reporter only after the test completed, as `recordedEvents`
   * of the test completed event, such as `TestSucceeded`.  If invoked at any other time, it will print to the standard output.
   * This method can be called safely by any thread.
   */
  protected def markup: Documenter = atomicDocumenter.get

    /**
   * Register a test with the given spec text, optional tags, and test function value that takes no arguments.
   * An invocation of this method is called an &ldquo;example.&rdquo;
   *
   * This method will register the test for later execution via an invocation of one of the `execute`
   * methods. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for `testNames` for an example.) The resulting test name must not have been registered previously on
   * this `FreeSpec` instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param methodName caller's method name
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws NullArgumentException if `specText` or any passed test tag is `null`
   */
  private def registerTestToRun(specText: String, testTags: List[Tag], methodName: String, testFun: () => Unit /* Assertion */, pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -5
    handleTest(thisSuite, specText, Transformer(testFun), Resources.itCannotAppearInsideAnotherIt, "FreeSpecLike.scala", methodName, stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

  /**
   * Register a test to ignore, which has the given spec text, optional tags, and test function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the `execute`
   * methods. This method exists to make it easy to ignore an existing test by changing the call to `it`
   * to `ignore` without deleting or commenting out the actual test code. The test will not be executed, but a
   * report will be sent that indicates the test was ignored. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documentation
   * for `testNames` for an example.) The resulting test name must not have been registered previously on
   * this `FreeSpec` instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param methodName caller's method name
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws NullArgumentException if `specText` or any passed test tag is `null`
   */
  private def registerTestToIgnore(specText: String, testTags: List[Tag], methodName: String, testFun: () => Unit /* Assertion */, pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -5
    handleIgnoredTest(specText, Transformer(testFun), Resources.ignoreCannotAppearInsideAnIt, "FreeSpecLike.scala", methodName, stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

  /**
   * Class that supports the registration of tagged tests.
   *
   * Instances of this class are returned by the `taggedAs` method of 
   * class `FreeSpecStringWrapper`.
   * 
   *
   * @author Bill Venners
   */
  protected final class ResultOfTaggedAsInvocationOnString(specText: String, tags: List[Tag], pos: source.Position) {

    /**
     * Supports tagged test registration.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "complain on peek" taggedAs(SlowTest) in { ... }
     *                                       ^
     * }}}
     *
     * This trait's implementation of this method will decide whether to register the text (passed to the constructor
     * of `ResultOfTaggedAsInvocationOnString`) and invoke the passed function
     * based on whether or not this is part of the current "test path." For the details on this process, see
     * the <a href="#howItExecutes">How it executes</a> section of the main documentation for
     * trait `org.scalatest.path.FreeSpec`.
     * 
     */
    def in(testFun: => Unit /* Assertion */): Unit = {
      registerTestToRun(specText, tags, "in", testFun _, pos)
    }

    /**
     * Supports registration of tagged, pending tests.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "complain on peek" taggedAs(SlowTest) is (pending)
     *                                       ^
     * }}}
     *
     * For more information and examples of this method's use, see the
     * <a href="../FreeSpec.html#pendingTests">Pending tests</a> section in the main documentation for
     * sister trait `org.scalatest.FreeSpec`.
     * Note that this trait's implementation of this method will decide whether to register the text (passed to the constructor
     * of `ResultOfTaggedAsInvocationOnString`) and invoke the passed function
     * based on whether or not this is part of the current "test path." For the details on this process, see
     * the <a href="#howItExecutes">How it executes</a> section of the main documentation for
     * trait `org.scalatest.path.FreeSpec`.
     * 
     */
    def is(testFun: => PendingNothing): Unit = {
      registerTestToRun(specText, tags, "is", testFun _, pos)
    }

    /**
     * Supports registration of tagged, ignored tests.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "complain on peek" taggedAs(SlowTest) ignore { ... }
     *                                       ^
     * }}}
     *
     * For more information and examples of this method's use, see the
     * <a href="../FreeSpec.html#ignoredTests">Ignored tests</a> section in the main documentation for sister
     * trait `org.scalatest.FreeSpec`. Note that a separate instance will be created for an ignored test,
     * and the path to the ignored test will be executed in that instance, but the test function itself will not
     * be executed. Instead, a `TestIgnored` event will be fired.
     * 
     */
    def ignore(testFun: => Unit /* Assertion */): Unit = {
      registerTestToIgnore(specText, tags, "ignore", testFun _, pos)
    }
  }       

  /**
   * A class that via an implicit conversion (named `convertToFreeSpecStringWrapper`) enables
   * methods `in`, `is`, `taggedAs` and `ignore`,
   * as well as the dash operator (`-`), to be invoked on `String`s.
   *
   * @author Bill Venners
   */
  protected final class FreeSpecStringWrapper(string: String, pos: source.Position) {

    /**
     * Register some text that may surround one or more tests. The passed
     * passed function value may contain surrounding text registrations (defined with dash (`-`)) and/or tests
     * (defined with `in`). This class's implementation of this method will decide whether to
     * register the text (passed to the constructor of `FreeSpecStringWrapper`) and invoke the passed function
     * based on whether or not this is part of the current "test path." For the details on this process, see
     * the <a href="#howItExecutes">How it executes</a> section of the main documentation for trait
     * `org.scalatest.path.FreeSpec`.
     */
    def -(fun: => Unit): Unit = {

      // SKIP-SCALATESTJS-START
      val stackDepth = 3
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 5

      try {
        handleNestedBranch(string, None, fun, Resources.dashCannotAppearInsideAnIn, "FreeSpecLike.scala", "-", stackDepth, -2, None, Some(pos))
      }
      catch {
        case e: TestFailedException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideInClauseNotDashClause, Some(e), e.position.getOrElse(pos))
        case e: TestCanceledException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideInClauseNotDashClause, Some(e), e.position.getOrElse(pos))
        case tgce: TestRegistrationClosedException => throw tgce
        case e: DuplicateTestNameException => throw new NotAllowedException(FailureMessages.exceptionWasThrownInDashClause(Prettifier.default, UnquotedString(e.getClass.getName), string, e.getMessage), Some(e), e.position.getOrElse(pos))
        case other: Throwable if (!Suite.anExceptionThatShouldCauseAnAbort(other)) => throw new NotAllowedException(FailureMessages.exceptionWasThrownInDashClause(Prettifier.default, UnquotedString(other.getClass.getName), string, other.getMessage), Some(other), pos)
        case other: Throwable => throw other
      }
    }

    /**
     * Supports test registration.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "complain on peek" in { ... }
     *                    ^
     * }}}
     *
     * This trait's implementation of this method will decide whether to register the text (passed to the constructor
     * of `FreeSpecStringWrapper`) and invoke the passed function
     * based on whether or not this is part of the current "test path." For the details on this process, see
     * the <a href="#howItExecutes">How it executes</a> section of the main documentation for
     * trait `org.scalatest.path.FreeSpec`.
     * 
     */
    def in(f: => Unit /* Assertion */): Unit = {
      registerTestToRun(string, List(), "in", f _, pos)
    }

    /**
     * Supports ignored test registration.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "complain on peek" ignore { ... }
     *                    ^
     * }}}
     *
     * For more information and examples of this method's use, see the
     * <a href="../FreeSpec.html#ignoredTests">Ignored tests</a> section in the main documentation for sister
     * trait `org.scalatest.FreeSpec`. Note that a separate instance will be created for an ignored test,
     * and the path to the ignored test will be executed in that instance, but the test function itself will not
     * be executed. Instead, a `TestIgnored` event will be fired.
     * 
     */
    def ignore(f: => Unit /* Assertion */): Unit = {
      registerTestToIgnore(string, List(), "ignore", f _, pos)
    }

    /**
     * Supports pending test registration.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "complain on peek" is (pending)
     *                    ^
     * }}}
     *
     * For more information and examples of this method's use, see the
     * <a href="../FreeSpec.html#pendingTests">Pending tests</a> section in the main documentation for
     * sister trait `org.scalatest.FreeSpec`.
     * Note that this trait's implementation of this method will decide whether to register the text (passed to the constructor
     * of `FreeSpecStringWrapper`) and invoke the passed function
     * based on whether or not this is part of the current "test path." For the details on this process, see
     * the <a href="#howItExecutes">How it executes</a> section of the main documentation for
     * trait `org.scalatest.path.FreeSpec`.
     * 
     */
    def is(f: => PendingNothing): Unit = {
      registerTestToRun(string, List(), "is", f _, pos)
    }

    /**
     * Supports tagged test registration.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "complain on peek" taggedAs(SlowTest) in { ... }
     *                    ^
     * }}}
     *
     * For more information and examples of this method's use, see the
     * <a href="../FreeSpec.html#taggingTests">Tagging tests</a> section in the main documentation for sister
     * trait `org.scalatest.FreeSpec`.
     * 
     */
    def taggedAs(firstTestTag: Tag, otherTestTags: Tag*): ResultOfTaggedAsInvocationOnString = {
      val tagList = firstTestTag :: otherTestTags.toList
      new ResultOfTaggedAsInvocationOnString(string, tagList, pos)
    }
  }

  import scala.language.implicitConversions

  /**
   * Implicitly converts `String`s to `FreeSpecStringWrapper`, which enables
   * methods `in`, `is`, `taggedAs` and `ignore`,
   * as well as the dash operator (`-`), to be invoked on `String`s.
   */
  protected implicit def convertToFreeSpecStringWrapper(s: String)(implicit pos: source.Position): FreeSpecStringWrapper = new FreeSpecStringWrapper(s, pos)

  /**
   * Supports shared test registration in `path.FreeSpec`s.
   *
   * This field enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * behave like nonFullStack(stackWithOneItem)
   * ^
   * }}}
   *
   * For more information and examples of the use of <cod>behave`, see the
   * <a href="../FreeSpec.html#SharedTests">Shared tests section</a> in the main documentation for sister
   * trait `org.scalatest.FreeSpec`.
   * 
   */
  protected val behave = new BehaveWord

  /**
   * An immutable `Set` of test names. If this `FreeSpec` contains no tests, this method returns an
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
   * example itself, with all components separated by a space. For example, consider this `FreeSpec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.path
   *
   * class StackSpec extends path.FreeSpec {
   *   "A Stack" - {
   *     "when not empty" - {
   *       "must allow me to pop" in {}
   *     }
   *     "when not full" - {
   *       "must allow me to push" in {}
   *     }
   *   }
   * }
   * }}}
   *
   * Invoking `testNames` on this `FreeSpec` will yield a set that contains the following
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
   * The total number of tests that are expected to run when this `path.FreeSpec`'s `run` method
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
   * @param args the `Args` for this run
   *
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
   * to methods `test` and `ignore`.
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
   * Runs this `path.FreeSpec`, reporting test results that were registered when the tests
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
   * `path.FreeSpec` is not allowed to contain nested suites, this trait's implementation of
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
   * @throws NullArgumentException if any passed parameter is `null`.
   * @throws IllegalArgumentException if `testName` is defined, but no test with the specified test name
   *     exists in this `Suite`
   */
  final override def run(testName: Option[String], args: Args): Status = {
    // TODO enforce those throws specs

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
  }

  /**
   * This lifecycle method is unused by this trait, and is implemented to do nothing. If invoked, it will
   * just return immediately.
   *
   * Nested suites are not allowed in a `path.FreeSpec`. Because
   * a `path.FreeSpec` executes tests eagerly at construction time, registering the results of
   * those test runs and reporting them later, the order of nested suites versus test runs would be different
   * in a `org.scalatest.path.FreeSpec` than in an `org.scalatest.FreeSpec`. In an
   * `org.scalatest.FreeSpec`, nested suites are executed then tests are executed. In an
   * `org.scalatest.path.FreeSpec` it would be the opposite. To make the code easy to reason about,
   * therefore, this is just not allowed. If you want to add nested suites to a `path.FreeSpec`, you can
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
   * nested suites are not allowed in a `path.FreeSpec`. Because
   * a `path.FreeSpec` executes tests eagerly at construction time, registering the results of
   * those test runs and reporting them later, the order of nested suites versus test runs would be different
   * in a `org.scalatest.path.FreeSpec` than in an `org.scalatest.FreeSpec`. In an
   * `org.scalatest.FreeSpec`, nested suites are executed then tests are executed. In an
   * `org.scalatest.path.FreeSpec` it would be the opposite. To make the code easy to reason about,
   * therefore, this is just not allowed. If you want to add nested suites to a `path.FreeSpec`, you can
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
  final override val styleName: String = "org.scalatest.path.FreeSpec"
    
  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = {
    ensureTestResultsRegistered(thisSuite)
    createTestDataFor(testName, theConfigMap, this)
  }
}

