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
import Suite.anExceptionThatShouldCauseAnAbort
import Suite.autoTagClassAnnotations
import java.util.ConcurrentModificationException
import java.util.concurrent.atomic.AtomicReference
import words.BehaveWord

/**
 * Implementation trait for class `FreeSpec`, which 
 * facilitates a &ldquo;behavior-driven&rdquo; style of development (BDD),
 * in which tests are nested inside text clauses denoted with the dash
 * operator (`-`).
 * 
 * <a href="FreeSpec.html">`FreeSpec`</a> is a class, not a trait,
 * to minimize compile time given there is a slight compiler overhead to
 * mixing in traits compared to extending classes. If you need to mix the
 * behavior of `FreeSpec` into some other class, you can use this
 * trait instead, because class `FreeSpec` does nothing more than
 * extend this trait and add a nice `toString` implementation.
 * 
 *
 * See the documentation of the class for a <a href="FreeSpec.html">detailed
 * overview of `FreeSpec`</a>.
 * 
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.FreeSpecFinder"))
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
trait FreeSpecLike extends TestSuite with TestRegistration with Informing with Notifying with Alerting with Documenting { thisSuite =>

  private final val engine = new Engine(Resources.concurrentFreeSpecMod, "FreeSpec")
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
   * Returns a `Notifier` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `FreeSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an `Alerter` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `FreeSpec` is being executed, such as from inside a test function, it will forward the information to
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

  final def registerTest(testText: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -2
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    engine.registerTest(testText, Transformer(testFun _), Resources.testCannotBeNestedInsideAnotherTest, "FreeSpecLike.scala", "registerTest", 5, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
  }

  final def registerIgnoredTest(testText: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    engine.registerIgnoredTest(testText, Transformer(testFun _), Resources.testCannotBeNestedInsideAnotherTest, "FreeSpecLike.scala", "registerIgnoredTest", 4, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

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
  private def registerTestToRun(specText: String, testTags: List[Tag], methodName: String, testFun: () => Any /* Assertion */, pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -5
    engine.registerTest(specText, Transformer(testFun), Resources.inCannotAppearInsideAnotherIn, "FreeSpecLike.scala", methodName, stackDepth, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
  }

  /**
   * Register a test to ignore, which has the given spec text, optional tags, and test function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the `execute`
   * methods. This method exists to make it easy to ignore an existing test by changing the call to `it`
   * to `ignore` without deleting or commenting out the actual test code. The test will not be executed, but a
   * report will be sent that indicates the test was ignored. The name of the test will be a concatenation of the text of all surrounding describers,
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
  private def registerTestToIgnore(specText: String, testTags: List[Tag], methodName: String, testFun: () => Any /* Assertion */, pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -4
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -6
    engine.registerIgnoredTest(specText, Transformer(testFun), Resources.ignoreCannotAppearInsideAnIn, "FreeSpecLike.scala", methodName, stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
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
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait `FreeSpec`.
     * 
     */
    def in(testFun: => Any /* Assertion */): Unit = {
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
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait `FreeSpec`.
     * 
     */
    def is(testFun: => PendingStatement): Unit = {
      registerTestToRun(specText, tags, "is", () => { testFun; succeed }, pos)
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
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait `FreeSpec`.
     * 
     */
    def ignore(testFun: => Any /* Assertion */): Unit = {
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
     * Register some text that may surround one or more tests. Thepassed function value may contain surrounding text
     * registrations (defined with dash (`-`)) and/or tests (defined with `in`). This trait's
     * implementation of this method will register the text (passed to the contructor of `FreeSpecStringWrapper`
     * and immediately invoke the passed function.
     */
    def -(fun: => Unit): Unit = {

      // SKIP-SCALATESTJS-START
      val stackDepth = 3
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 5

      try {
        registerNestedBranch(string, None, fun, Resources.dashCannotAppearInsideAnIn, "FreeSpecLike.scala", "-", stackDepth, -2, None, Some(pos))
      }
      catch {
        case e: exceptions.TestFailedException => throw new exceptions.NotAllowedException(FailureMessages.assertionShouldBePutInsideInClauseNotDashClause, Some(e), e.position.getOrElse(pos))
        case e: exceptions.TestCanceledException => throw new exceptions.NotAllowedException(FailureMessages.assertionShouldBePutInsideInClauseNotDashClause, Some(e), e.position.getOrElse(pos))
        case tgce: exceptions.TestRegistrationClosedException => throw tgce
        case e: exceptions.DuplicateTestNameException => throw new exceptions.NotAllowedException(FailureMessages.exceptionWasThrownInDashClause(Prettifier.default, UnquotedString(e.getClass.getName), string, e.getMessage), Some(e), e.position.getOrElse(pos))
        case other: Throwable if (!Suite.anExceptionThatShouldCauseAnAbort(other)) => throw new exceptions.NotAllowedException(FailureMessages.exceptionWasThrownInDashClause(Prettifier.default, UnquotedString(other.getClass.getName), string, other.getMessage), Some(other), pos)
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
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait `FreeSpec`.
     * 
     */
    def in(f: => Any /* Assertion */): Unit = {
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
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait `FreeSpec`.
     * 
     */
    def ignore(f: => Any /* Assertion */): Unit = {
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
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait `FreeSpec`.
     * 
     */
    def is(f: => PendingStatement): Unit = {
      registerTestToRun(string, List(), "is", () => { f; succeed }, pos)
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
     * For more information and examples of this method's use, see the <a href="FreeSpec.html">main documentation</a> for trait `FreeSpec`.
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
   * A `Map` whose keys are `String` names of tagged tests and whose associated values are
   * the `Set` of tags for the test. If this `FreeSpec` contains no tags, this method returns an empty `Map`.
   *
   * This trait's implementation returns tags that were passed as strings contained in `Tag` objects passed to 
   * `taggedAs`. 
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
   * from outside in, and the test's  spec text, with one space placed between each item. (See the documentation
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

    def invokeWithFixture(theTest: TestLeaf): Outcome = {
      val theConfigMap = args.configMap
      val testData = testDataFor(testName, theConfigMap)
      withFixture(
        new NoArgTest {
          val name = testData.name
          def apply(): Outcome = { theTest.testFun() }
          val configMap = testData.configMap
          val scopes = testData.scopes
          val text = testData.text
          val tags = testData.tags
          val pos = testData.pos
        }
      )
    }

    runTestImpl(thisSuite, testName, args, true, invokeWithFixture)
  }

  /**
   * Run zero to many of this `FreeSpec`'s tests.
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
    runTestsImpl(thisSuite, testName, args, info, true, runTest)
  }

  /**
   * An immutable `Set` of test names. If this `FreeSpec` contains no tests, this method returns an
   * empty `Set`.
   *
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space. For example, consider this `FreeSpec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.FreeSpec
   *
   * class StackSpec extends FreeSpec {
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
   */
  override def testNames: Set[String] = {
    InsertionOrderSet(atomic.get.testNamesList)
  }

  override def run(testName: Option[String], args: Args): Status = {
    runImpl(thisSuite, testName, args, super.run)
  }

  /**
   * Supports shared test registration in `FreeSpec`s.
   *
   * This field enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * behave like nonFullStack(stackWithOneItem)
   * ^
   * }}}
   *
   * For more information and examples of the use of <cod>behave`, see the <a href="#sharedTests">Shared tests section</a>
   * in the main documentation for this trait.
   * 
   */
  protected val behave = new BehaveWord
  
  /**
   * Suite style name.
   */
  final override val styleName: String = "org.scalatest.FreeSpec"
    
  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = createTestDataFor(testName, theConfigMap, this)
}
