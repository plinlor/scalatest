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
import org.scalatest.exceptions.StackDepthExceptionHelper.getStackDepth
import words.{ResultOfTaggedAsInvocation, ResultOfStringPassedToVerb, BehaveWord, ShouldVerb, MustVerb, CanVerb, StringVerbStringInvocation, StringVerbBehaveLikeInvocation}

/**
 * Implementation trait for class `AsyncFlatSpec`, which facilitates a
 * &ldquo;behavior-driven&rdquo; style of development (BDD), in which tests
 * are combined with text that specifies the behavior the tests verify.
 *
 * <a href="AsyncFlatSpec.html">`AsyncFlatSpec`</a> is a class, not a trait,
 * to minimize compile time given there is a slight compiler overhead to
 * mixing in traits compared to extending classes. If you need to mix the
 * behavior of `AsyncFlatSpec` into some other class, you can use this
 * trait instead, because class `AsyncFlatSpec` does nothing more than
 * extend this trait and add a nice `toString` implementation.
 * 
 *
 * See the documentation of the class for a <a href="AsyncFlatSpec.html">detailed
 * overview of `AsyncFlatSpec`</a>.
 * 
 *
 * @author Bill Venners
 */
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
@Finders(Array("org.scalatest.finders.FlatSpecFinder"))
trait AsyncFlatSpecLike extends AsyncTestSuite with AsyncTestRegistration with ShouldVerb with MustVerb with CanVerb with Informing with Notifying with Alerting with Documenting { thisSuite =>

  private[scalatest] def transformPendingToOutcome(testFun: () => PendingStatement): () => AsyncOutcome =
    () => {
      PastOutcome(
        try { testFun; Succeeded }
        catch {
          case ex: exceptions.TestCanceledException => Canceled(ex)
          case _: exceptions.TestPendingException => Pending
          case tfe: exceptions.TestFailedException => Failed(tfe)
          case ex: Throwable if !Suite.anExceptionThatShouldCauseAnAbort(ex) => Failed(ex)
        }
      )
    }

  private final val engine = new AsyncEngine(Resources.concurrentSpecMod, "Spec")

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
   * `FlatSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an `Alerter` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `FlatSpec` is being executed, such as from inside a test function, it will forward the information to
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
    engine.registerAsyncTest(testText, transformToOutcome(testFun), Resources.testCannotBeNestedInsideAnotherTest, None, None, pos, testTags: _*)
  }

  final def registerIgnoredAsyncTest(testText: String, testTags: Tag*)(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
    engine.registerIgnoredAsyncTest(testText, transformToOutcome(testFun), Resources.testCannotBeNestedInsideAnotherTest, None, pos, testTags: _*)
  }

  /**
   * Register a test with the given spec text, optional tags, and test function value that takes no arguments.
   * An invocation of this method is called an &ldquo;example.&rdquo;
   *
   * This method will register the test for later execution via an invocation of one of the `execute`
   * methods. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for `testNames` for an example.) The resulting test name must not have been registered previously on
   * this `FlatSpec` instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param methodName Method name of the caller
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws NullArgumentException if `specText` or any passed test tag is `null`
   */
  private def registerTestToRun(specText: String, methodName: String, testTags: List[Tag], testFun: () => Future[compatible.Assertion], pos: source.Position): Unit = {
    def transformToOutcomeParam: Future[compatible.Assertion] = testFun()
    def testRegistrationClosedMessageFun: String =
      methodName match {
        case "in" => Resources.inCannotAppearInsideAnotherInOrIs
        case "is" => Resources.isCannotAppearInsideAnotherInOrIs
      }
    engine.registerAsyncTest(specText, transformToOutcome(transformToOutcomeParam), testRegistrationClosedMessageFun, None, None, pos, testTags: _*)
  }

  private def registerPendingTestToRun(specText: String, methodName: String, testTags: List[Tag], testFun: () => PendingStatement, pos: source.Position): Unit = {
    //def transformPendingToOutcomeParam: PendingStatement = testFun()
    def testRegistrationClosedMessageFun: String =
      methodName match {
        case "in" => Resources.inCannotAppearInsideAnotherInOrIs
        case "is" => Resources.isCannotAppearInsideAnotherInOrIs
      }
    engine.registerAsyncTest(specText, transformPendingToOutcome(testFun), testRegistrationClosedMessageFun, None, None, pos, testTags: _*)
  }

  /**
   * Class that supports the registration of a &ldquo;subject&rdquo; being specified and tested via the
   * instance referenced from `FlatSpec`'s `behavior` field.
   *
   * This field enables syntax such as the following subject registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * behavior of "A Stack"
   * ^
   * }}}
   *
   * For more information and examples of the use of the `behavior` field, see the <a href="FlatSpec.html">main documentation</a>
   * for trait `FlatSpec`.
   * 
   */
  protected final class BehaviorWord {

    /**
     * Supports the registration of a &ldquo;subject&rdquo; being specified and tested via the
     * instance referenced from `FlatSpec`'s `behavior` field.
     *
     * This method enables syntax such as the following subject registration:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * behavior of "A Stack"
     *          ^
     * }}}
     *
     * For more information and examples of the use of this method, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def of(description: String)(implicit pos: source.Position): Unit = {
      registerFlatBranch(description, Resources.behaviorOfCannotAppearInsideAnIn, pos)
    }
  }

  /**
   * Supports the registration of a &ldquo;subject&rdquo; being specified and tested.
   *
   * This field enables syntax such as the following subject registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * behavior of "A Stack"
   * ^
   * }}}
   *
   * For more information and examples of the use of the `behavior` field, see the main documentation
   * for this trait.
   * 
   */
  protected val behavior = new BehaviorWord

  /**
   * Class that supports the registration of tagged tests via the `ItWord` instance
   * referenced from `FlatSpec`'s `it` field.
   *
   * This class enables syntax such as the following tagged test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
   *                                                                      ^
   * }}}
   *
   * It also enables syntax such as the following registration of an ignored, tagged test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should "pop values in last-in-first-out order" taggedAs(SlowTest) ignore { ... }
   *                                                                      ^
   * }}}
   *
   * In addition, it enables syntax such as the following registration of a pending, tagged test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should "pop values in last-in-first-out order" taggedAs(SlowTest) is (pending)
   *                                                                      ^
   * }}}
   *
   * For more information and examples of the use of the `it` field to register tagged tests, see
   * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
   * For examples of tagged test registration, see
   * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
   * 
   */
  protected final class ItVerbStringTaggedAs(verb: String, name: String, tags: List[Tag]) {

    /**
     * Supports the registration of tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it must "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
     *                                                                    ^
     * }}}
     *
     * For examples of tagged test registration, see
     * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
     * 
     */
    def in(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToRun(verb.trim + " " + name.trim, "in", tags, testFun _, pos)
    }

    /**
     * Supports the registration of pending, tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it must "pop values in last-in-first-out order" taggedAs(SlowTest) is (pending)
     *                                                                    ^
     * }}}
     *
     * For examples of pending test registration, see the <a href="FlatSpec.html#pendingTests">Pending tests section</a> in the main documentation
     * for trait `FlatSpec`.  And for examples of tagged test registration, see
     * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
     * 
     */
    def is(testFun: => PendingStatement)(implicit pos: source.Position): Unit = {
      registerPendingTestToRun(verb.trim + " " + name.trim, "is", tags, testFun _, pos)
    }

    /**
     * Supports the registration of ignored, tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it must "pop values in last-in-first-out order" taggedAs(SlowTest) ignore { ... }
     *                                                                    ^
     * }}}
     *
     * For examples of ignored test registration, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a> in the main documentation
     * for trait `FlatSpec`.  And for examples of tagged test registration, see
     * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
     * 
     */
    def ignore(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToIgnore(verb.trim + " " + name.trim, tags, "ignore", testFun _, pos)
    }
  }

  /**
   * Class that supports test registration via the `ItWord` instance referenced from `FlatSpec`'s `it` field.
   *
   * This class enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should "pop values in last-in-first-out order" in { ... }
   *                                                   ^
   * }}}
   *
   * It also enables syntax such as the following registration of an ignored test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should "pop values in last-in-first-out order" ignore { ... }
   *                                                   ^
   * }}}
   *
   * In addition, it enables syntax such as the following registration of a pending test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should "pop values in last-in-first-out order" is (pending)
   *                                                   ^
   * }}}
   *
   * And finally, it also enables syntax such as the following tagged test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
   *                                                   ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the <a href="FlatSpec.html">main documentation</a>
   * for trait `FlatSpec`.
   * 
   */
  protected final class ItVerbString(verb: String, name: String) {

    /**
     * Supports the registration of tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it must "pop values in last-in-first-out order" in { ... }
     *                                                 ^
     * }}}
     *
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def in(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToRun(verb.trim + " " + name.trim, "in", List(), testFun _, pos)
    }

    /**
     * Supports the registration of pending tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it must "pop values in last-in-first-out order" is (pending)
     *                                                 ^
     * }}}
     *
     * For examples of pending test registration, see the <a href="FlatSpec.html#pendingTests">Pending tests section</a> in the main documentation
     * for trait `FlatSpec`.
     * 
     */
    def is(testFun: => PendingStatement)(implicit pos: source.Position): Unit = {
      registerPendingTestToRun(verb.trim + " " + name.trim, "is", List(), testFun _, pos)
    }

    /**
     * Supports the registration of ignored tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it must "pop values in last-in-first-out order" ignore { ... }
     *                                                 ^
     * }}}
     *
     * For examples of ignored test registration, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a> in the main documentation
     * for trait `FlatSpec`.
     * 
     */
    def ignore(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToIgnore(verb.trim + " " + name.trim, List(), "ignore", testFun _, pos)
    }

    /**
     * Supports the registration of tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it must "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
     *                                                 ^
     * }}}
     *
     * For examples of tagged test registration, see the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation
     * for trait `FlatSpec`.
     * 
     */
    def taggedAs(firstTestTag: Tag, otherTestTags: Tag*) = {
      val tagList = firstTestTag :: otherTestTags.toList
      new ItVerbStringTaggedAs(verb, name, tagList)
    }
  }

  /**
   * Class that supports test (and shared test) registration via the instance referenced from `FlatSpec`'s `it` field.
   *
   * This class enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should "pop values in last-in-first-out order" in { ... }
   * ^
   * }}}
   *
   * It also enables syntax such as the following shared test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should behave like nonEmptyStack(lastItemPushed)
   * ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the main documentation
   * for this trait.
   * 
   */
  protected final class ItWord {

    /**
     * Supports the registration of tests with `should` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it should "pop values in last-in-first-out order" in { ... }
     *    ^
     * }}}
     *
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def should(string: String) = new ItVerbString("should", string)

    /**
     * Supports the registration of tests with `must` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it must "pop values in last-in-first-out order" in { ... }
     *    ^
     * }}}
     *
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def must(string: String) = new ItVerbString("must", string)

    /**
     * Supports the registration of tests with `can` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it can "pop values in last-in-first-out order" in { ... }
     *    ^
     * }}}
     *
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def can(string: String) = new ItVerbString("can", string)

    /**
     * Supports the registration of shared tests with `should` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it should behave like nonFullStack(stackWithOneItem)
     *    ^
     * }}}
     *
     * For examples of shared tests, see the <a href="FlatSpec.html#sharedTests">Shared tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def should(behaveWord: BehaveWord) = behaveWord

    /**
     * Supports the registration of shared tests with `must` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it must behave like nonFullStack(stackWithOneItem)
     *    ^
     * }}}
     *
     * For examples of shared tests, see the <a href="FlatSpec.html#sharedTests">Shared tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def must(behaveWord: BehaveWord) = behaveWord

    /**
     * Supports the registration of shared tests with `can` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * it can behave like nonFullStack(stackWithOneItem)
     *    ^
     * }}}
     *
     * For examples of shared tests, see the <a href="FlatSpec.html#sharedTests">Shared tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def can(behaveWord: BehaveWord) = behaveWord
  }

  /**
   * Supports test (and shared test) registration in `FlatSpec`s.
   *
   * This field enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should "pop values in last-in-first-out order" in { ... }
   * ^
   * }}}
   *
   * It also enables syntax such as the following shared test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should behave like nonEmptyStack(lastItemPushed)
   * ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the main documentation
   * for this trait.
   * 
   */
  protected val it = new ItWord

  /**
   * Class that supports registration of ignored, tagged tests via the `IgnoreWord` instance referenced
   * from `FlatSpec`'s `ignore` field.
   *
   * This class enables syntax such as the following registration of an ignored, tagged test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * ignore should "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
   *                                                                          ^
   * }}}
   *
   * In addition, it enables syntax such as the following registration of an ignored, tagged, pending test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * ignore should "pop values in last-in-first-out order" taggedAs(SlowTest) is (pending)
   *                                                                          ^
   * }}}
   *
   * Note: the `is` method is provided for completeness and design symmetry, given there's no way
   * to prevent changing `is` to `ignore` and marking a pending test as ignored that way.
   * Although it isn't clear why someone would want to mark a pending test as ignored, it can be done.
   * 
   *
   * For more information and examples of the use of the `ignore` field, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
   * in the main documentation for trait `FlatSpec`. For examples of tagged test registration, see
   * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
   * 
   */
  protected final class IgnoreVerbStringTaggedAs(verb: String, name: String, tags: List[Tag]) {

    /**
     * Supports the registration of ignored, tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * ignore must "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
     *                                                                        ^
     * }}}
     *
     * For examples of the registration of ignored tests, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`. For examples of tagged test registration, see
     * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
     * 
     */
    def in(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToIgnore(verb.trim + " " + name.trim, tags, "in", testFun _, pos)
    }

    /**
     * Supports the registration of ignored, tagged, pending tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * ignore must "pop values in last-in-first-out order" taggedAs(SlowTest) is (pending)
     *                                                                        ^
     * }}}
     *
     * Note: this `is` method is provided for completeness and design symmetry, given there's no way
     * to prevent changing `is` to `ignore` and marking a pending test as ignored that way.
     * Although it isn't clear why someone would want to mark a pending test as ignored, it can be done.
     * 
     *
     * For examples of pending test registration, see the <a href="FlatSpec.html#pendingTests">Pending tests section</a> in the main documentation
     * for trait `FlatSpec`.  For examples of the registration of ignored tests,
     * see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`. For examples of tagged test registration, see
     * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
     * 
     */
    def is(testFun: => PendingStatement)(implicit pos: source.Position): Unit = {
      registerPendingTestToIgnore(verb.trim + " " + name.trim, tags, "is", testFun _, pos)
    }
    // Note: no def ignore here, so you can't put two ignores in the same line
  }

  /**
   * Class that supports registration of ignored tests via the `IgnoreWord` instance referenced
   * from `FlatSpec`'s `ignore` field.
   *
   * This class enables syntax such as the following registration of an ignored test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * ignore should "pop values in last-in-first-out order" in { ... }
   *                                                       ^
   * }}}
   *
   * In addition, it enables syntax such as the following registration of an ignored, pending test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * ignore should "pop values in last-in-first-out order" is (pending)
   *                                                       ^
   * }}}
   *
   * Note: the `is` method is provided for completeness and design symmetry, given there's no way
   * to prevent changing `is` to `ignore` and marking a pending test as ignored that way.
   * Although it isn't clear why someone would want to mark a pending test as ignored, it can be done.
   * 
   *
   * And finally, it also enables syntax such as the following ignored, tagged test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * ignore should "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
   *                                                       ^
   * }}}
   *
   * For more information and examples of the use of the `ignore` field, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
   * in the main documentation for trait `FlatSpec`.
   * 
   */
  protected final class IgnoreVerbString(verb: String, name: String) {

    /**
     * Supports the registration of ignored tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * ignore must "pop values in last-in-first-out order" in { ... }
     *                                                     ^
     * }}}
     *
     * For examples of the registration of ignored tests, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def in(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToIgnore(verb.trim + " " + name.trim, List(), "in", testFun _, pos)
    }

    /**
     * Supports the registration of ignored, pending tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * ignore must "pop values in last-in-first-out order" is (pending)
     *                                                     ^
     * }}}
     *
     * Note: this `is` method is provided for completeness and design symmetry, given there's no way
     * to prevent changing `is` to `ignore` and marking a pending test as ignored that way.
     * Although it isn't clear why someone would want to mark a pending test as ignored, it can be done.
     * 
     *
     * For examples of pending test registration, see the <a href="FlatSpec.html#pendingTests">Pending tests section</a> in the main documentation
     * for trait `FlatSpec`.  For examples of the registration of ignored tests,
     * see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def is(testFun: => PendingStatement)(implicit pos: source.Position): Unit = {
      registerPendingTestToIgnore(verb.trim + " " + name.trim, List(), "is", testFun _, pos)
    }

    /**
     * Supports the registration of ignored, tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * ignore must "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
     *                                                     ^
     * }}}
     *
     * For examples of tagged test registration, see the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation
     * for trait `FlatSpec`.  For examples of the registration of ignored tests,
     * see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def taggedAs(firstTestTag: Tag, otherTestTags: Tag*) = {
      val tagList = firstTestTag :: otherTestTags.toList
      new IgnoreVerbStringTaggedAs(verb, name, tagList)
    }
  }

  /**
   * Class that supports registration of ignored tests via the `ItWord` instance
   * referenced from `FlatSpec`'s `ignore` field.
   *
   * This class enables syntax such as the following registration of an ignored test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * ignore should "pop values in last-in-first-out order" in { ... }
   * ^
   * }}}
   *
   * For more information and examples of the use of the `ignore` field, see <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
   * in the main documentation for this trait.
   * 
   */
  protected final class IgnoreWord {

    /**
     * Supports the registration of ignored tests with `should` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * ignore should "pop values in last-in-first-out order" in { ... }
     *        ^
     * }}}
     *
     * For more information and examples of the use of the `ignore` field, see <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def should(string: String) = new IgnoreVerbString("should", string)

    /**
     * Supports the registration of ignored tests with `must` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * ignore must "pop values in last-in-first-out order" in { ... }
     *        ^
     * }}}
     *
     * For more information and examples of the use of the `ignore` field, see <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def must(string: String) = new IgnoreVerbString("must", string)

    /**
     * Supports the registration of ignored tests with `can` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * ignore can "pop values in last-in-first-out order" in { ... }
     *        ^
     * }}}
     *
     * For more information and examples of the use of the `ignore` field, see <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def can(string: String) = new IgnoreVerbString("can", string)
  }

  /**
   * Supports registration of ignored tests in `FlatSpec`s.
   *
   * This field enables syntax such as the following registration of an ignored test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * ignore should "pop values in last-in-first-out order" in { ... }
   * ^
   * }}}
   *
   * For more information and examples of the use of the `ignore` field, see the <a href="#ignoredTests">Ignored tests section</a>
   * in the main documentation for this trait.
   * 
   */
  protected val ignore = new IgnoreWord

  /**
   * Class that supports the registration of tagged tests via the `TheyWord` instance
   * referenced from `FlatSpec`'s `they` field.
   *
   * This class enables syntax such as the following tagged test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
   *                                                                        ^
   * }}}
   *
   * It also enables syntax such as the following registration of an ignored, tagged test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should "pop values in last-in-first-out order" taggedAs(SlowTest) ignore { ... }
   *                                                                        ^
   * }}}
   *
   * In addition, it enables syntax such as the following registration of a pending, tagged test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should "pop values in last-in-first-out order" taggedAs(SlowTest) is (pending)
   *                                                                        ^
   * }}}
   *
   * For more information and examples of the use of the `they` field to register tagged tests, see
   * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
   * For examples of tagged test registration, see
   * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
   * 
   */
  protected final class TheyVerbStringTaggedAs(verb: String, name: String, tags: List[Tag]) {

    /**
     * Supports the registration of tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they must "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
     *                                                                      ^
     * }}}
     *
     * For examples of tagged test registration, see
     * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
     * 
     */
    def in(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToRun(verb.trim + " " + name.trim, "in", tags, testFun _, pos)
    }

    /**
     * Supports the registration of pending, tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they must "pop values in last-in-first-out order" taggedAs(SlowTest) is (pending)
     *                                                                      ^
     * }}}
     *
     * For examples of pending test registration, see the <a href="FlatSpec.html#pendingTests">Pending tests section</a> in the main documentation
     * for trait `FlatSpec`.  And for examples of tagged test registration, see
     * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
     * 
     */
    def is(testFun: => PendingStatement)(implicit pos: source.Position): Unit = {
      registerPendingTestToRun(verb.trim + " " + name.trim, "is", tags, testFun _, pos)
    }

    /**
     * Supports the registration of ignored, tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they must "pop values in last-in-first-out order" taggedAs(SlowTest) ignore { ... }
     *                                                                      ^
     * }}}
     *
     * For examples of ignored test registration, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a> in the main documentation
     * for trait `FlatSpec`.  And for examples of tagged test registration, see
     * the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation for trait `FlatSpec`.
     * 
     */
    def ignore(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToIgnore(verb.trim + " " + name.trim, tags, "ignore", testFun _, pos)
    }
  }

  /**
   * Class that supports test registration via the `TheyWord` instance referenced from `FlatSpec`'s `they` field.
   *
   * This class enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should "pop values in last-in-first-out order" in { ... }
   *                                                     ^
   * }}}
   *
   * It also enables syntax such as the following registration of an ignored test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should "pop values in last-in-first-out order" ignore { ... }
   *                                                     ^
   * }}}
   *
   * In addition, it enables syntax such as the following registration of a pending test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should "pop values in last-in-first-out order" is (pending)
   *                                                     ^
   * }}}
   *
   * And finally, it also enables syntax such as the following tagged test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
   *                                                     ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the <a href="FlatSpec.html">main documentation</a>
   * for trait `FlatSpec`.
   * 
   */
  protected final class TheyVerbString(verb: String, name: String) {

    /**
     * Supports the registration of tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they must "pop values in last-in-first-out order" in { ... }
     *                                                   ^
     * }}}
     *
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def in(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToRun(verb.trim + " " + name.trim, "in", List(), testFun _, pos)
    }

    /**
     * Supports the registration of pending tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they must "pop values in last-in-first-out order" is (pending)
     *                                                   ^
     * }}}
     *
     * For examples of pending test registration, see the <a href="FlatSpec.html#pendingTests">Pending tests section</a> in the main documentation
     * for trait `FlatSpec`.
     * 
     */
    def is(testFun: => PendingStatement)(implicit pos: source.Position): Unit = {
      registerPendingTestToRun(verb.trim + " " + name.trim, "is", List(), testFun _, pos)
    }

    /**
     * Supports the registration of ignored tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they must "pop values in last-in-first-out order" ignore { ... }
     *                                                   ^
     * }}}
     *
     * For examples of ignored test registration, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a> in the main documentation
     * for trait `FlatSpec`.
     * 
     */
    def ignore(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToIgnore(verb.trim + " " + name.trim, List(), "ignore", testFun _, pos)
    }

    /**
     * Supports the registration of tagged tests in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they must "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
     *                                                   ^
     * }}}
     *
     * For examples of tagged test registration, see the <a href="FlatSpec.html#taggingTests">Tagging tests section</a> in the main documentation
     * for trait `FlatSpec`.
     * 
     */
    def taggedAs(firstTestTag: Tag, otherTestTags: Tag*) = {
      val tagList = firstTestTag :: otherTestTags.toList
      new ItVerbStringTaggedAs(verb, name, tagList)
    }
  }

  /**
   * Class that supports test (and shared test) registration via the instance referenced from `FlatSpec`'s `it` field.
   *
   * This class enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should "pop values in last-in-first-out order" in { ... }
   * ^
   * }}}
   *
   * It also enables syntax such as the following shared test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should behave like nonEmptyStack(lastItemPushed)
   * ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the main documentation
   * for this trait.
   * 
   */
  protected final class TheyWord {

    /**
     * Supports the registration of tests with `should` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they should "pop values in last-in-first-out order" in { ... }
     *      ^
     * }}}
     *
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def should(string: String) = new ItVerbString("should", string)

    /**
     * Supports the registration of tests with `must` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they must "pop values in last-in-first-out order" in { ... }
     *      ^
     * }}}
     *
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def must(string: String) = new ItVerbString("must", string)

    /**
     * Supports the registration of tests with `can` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they can "pop values in last-in-first-out order" in { ... }
     *      ^
     * }}}
     *
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def can(string: String) = new ItVerbString("can", string)

    /**
     * Supports the registration of shared tests with `should` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they should behave like nonFullStack(stackWithOneItem)
     *      ^
     * }}}
     *
     * For examples of shared tests, see the <a href="FlatSpec.html#sharedTests">Shared tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def should(behaveWord: BehaveWord) = behaveWord

    /**
     * Supports the registration of shared tests with `must` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they must behave like nonFullStack(stackWithOneItem)
     *      ^
     * }}}
     *
     * For examples of shared tests, see the <a href="FlatSpec.html#sharedTests">Shared tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def must(behaveWord: BehaveWord) = behaveWord

    /**
     * Supports the registration of shared tests with `can` in a `FlatSpec`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * they can behave like nonFullStack(stackWithOneItem)
     *      ^
     * }}}
     *
     * For examples of shared tests, see the <a href="FlatSpec.html#sharedTests">Shared tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def can(behaveWord: BehaveWord) = behaveWord
  }

  /**
   * Supports test (and shared test) registration in `FlatSpec`s.
   *
   * This field enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should "pop values in last-in-first-out order" in { ... }
   * ^
   * }}}
   *
   * It also enables syntax such as the following shared test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * they should behave like nonEmptyStack(lastItemPushed)
   * ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the main documentation
   * for this trait.
   * 
   */
  protected val they = new TheyWord

  /**
   * Class that supports test registration in shorthand form.
   *
   * For example, this class enables syntax such as the following test registration
   * in shorthand form:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack (when empty)" should "be empty" in { ... }
   *                                          ^
   * }}}
   *
   * This class also enables syntax such as the following ignored test registration
   * in shorthand form:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack (when empty)" should "be empty" ignore { ... }
   *                                          ^
   * }}}
   *
   * This class is used via an implicit conversion (named `convertToInAndIgnoreMethods`)
   * from `ResultOfStringPassedToVerb`. The `ResultOfStringPassedToVerb` class
   * does not declare any methods named `in`, because the
   * type passed to `in` differs in a `FlatSpec` and a `fixture.FlatSpec`.
   * A `fixture.FlatSpec` needs two `in` methods, one that takes a no-arg
   * test function and another that takes a one-arg test function (a test that takes a
   * `Fixture` as its parameter). By constrast, a `FlatSpec` needs
   * only one `in` method that takes a by-name parameter. As a result,
   * `FlatSpec` and `fixture.FlatSpec` each provide an implicit conversion
   * from `ResultOfStringPassedToVerb` to a type that provides the appropriate
   * `in` methods.
   * 
   *
   * @author Bill Venners
   */
  protected final class InAndIgnoreMethods(resultOfStringPassedToVerb: ResultOfStringPassedToVerb) {

    import resultOfStringPassedToVerb.rest
import resultOfStringPassedToVerb.verb

    /**
     * Supports the registration of tests in shorthand form.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack" must "pop values in last-in-first-out order" in { ... }
     *                                                        ^
     * }}}
     *
     * For examples of test registration, see the <a href="FlatSpec.html">main documentation</a>
     * for trait `FlatSpec`.
     * 
     */
    def in(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToRun(verb.trim + " " + rest.trim, "in", List(), testFun _, pos)
    }

    /**
     * Supports the registration of ignored tests in shorthand form.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack" must "pop values in last-in-first-out order" ignore { ... }
     *                                                        ^
     * }}}
     *
     * For examples of ignored test registration, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def ignore(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToIgnore(verb.trim + " " + rest.trim, List(), "ignore", testFun _, pos)
    }
  }

  import scala.language.implicitConversions

  /**
   * Implicitly converts an object of type `ResultOfStringPassedToVerb` to an
   * `InAndIgnoreMethods`, to enable `in` and `ignore`
   * methods to be invokable on that object.
   */
  protected implicit def convertToInAndIgnoreMethods(resultOfStringPassedToVerb: ResultOfStringPassedToVerb): InAndIgnoreMethods =
    new InAndIgnoreMethods(resultOfStringPassedToVerb)

  /**
   * Class that supports tagged test registration in shorthand form.
   *
   * For example, this class enables syntax such as the following tagged test registration
   * in shorthand form:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack (when empty)" should "be empty" taggedAs() in { ... }
   *                                                     ^
   * }}}
   *
   * This class also enables syntax such as the following tagged, ignored test registration
   * in shorthand form:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack (when empty)" should "be empty" taggedAs(SlowTest) ignore { ... }
   *                                                             ^
   * }}}
   *
   * This class is used via an implicit conversion (named `convertToInAndIgnoreMethodsAfterTaggedAs`)
   * from `ResultOfTaggedAsInvocation`. The `ResultOfTaggedAsInvocation` class
   * does not declare any methods named `in`, because the
   * type passed to `in` differs in a `FlatSpec` and a `fixture.FlatSpec`.
   * A `fixture.FlatSpec` needs two `in` methods, one that takes a no-arg
   * test function and another that takes a one-arg test function (a test that takes a
   * `Fixture` as its parameter). By constrast, a `FlatSpec` needs
   * only one `in` method that takes a by-name parameter. As a result,
   * `FlatSpec` and `fixture.FlatSpec` each provide an implicit conversion
   * from `ResultOfTaggedAsInvocation` to a type that provides the appropriate
   * `in` methods.
   * 
   *
   * @author Bill Venners
   */
  protected final class InAndIgnoreMethodsAfterTaggedAs(resultOfTaggedAsInvocation: ResultOfTaggedAsInvocation) {

    import resultOfTaggedAsInvocation.verb
    import resultOfTaggedAsInvocation.rest
    import resultOfTaggedAsInvocation.{tags => tagsList}

    /**
     * Supports the registration of tagged tests in shorthand form.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack" must "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
     *                                                                           ^
     * }}}
     *
     * For examples of tagged test registration, see the <a href="FlatSpec.html#taggingTests">Tagging tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def in(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToRun(verb.trim + " " + rest.trim, "in", tagsList, testFun _, pos)
    }

    /**
     * Supports the registration of tagged, ignored tests in shorthand form.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack" must "pop values in last-in-first-out order" taggedAs(SlowTest) ignore { ... }
     *                                                                           ^
     * }}}
     *
     * For examples of ignored test registration, see the <a href="FlatSpec.html#ignoredTests">Ignored tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * For examples of tagged test registration, see the <a href="FlatSpec.html#taggingTests">Tagging tests section</a>
     * in the main documentation for trait `FlatSpec`.
     * 
     */
    def ignore(testFun: => Future[compatible.Assertion])(implicit pos: source.Position): Unit = {
      registerTestToIgnore(verb.trim + " " + rest.trim, tagsList, "ignore", testFun _, pos)
    }
  }

  /**
   * Implicitly converts an object of type `ResultOfTaggedAsInvocation` to an
   * `InAndIgnoreMethodsAfterTaggedAs`, to enable `in` and `ignore`
   * methods to be invokable on that object.
   */
  protected implicit def convertToInAndIgnoreMethodsAfterTaggedAs(resultOfTaggedAsInvocation: ResultOfTaggedAsInvocation): InAndIgnoreMethodsAfterTaggedAs =
    new InAndIgnoreMethodsAfterTaggedAs(resultOfTaggedAsInvocation)

  /**
   * Supports the shorthand form of test registration.
   *
   * For example, this method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack (when empty)" should "be empty" in { ... }
   *                        ^
   * }}}
   *
   * This function is passed as an implicit parameter to a `should` method
   * provided in `ShouldVerb`, a `must` method
   * provided in `MustVerb`, and a `can` method
   * provided in `CanVerb`. When invoked, this function registers the
   * subject description (the first parameter to the function) and returns a `ResultOfStringPassedToVerb`
   * initialized with the verb and rest parameters (the second and third parameters to
   * the function, respectively).
   * 
   */
  protected implicit val shorthandTestRegistrationFunction: StringVerbStringInvocation =
    new StringVerbStringInvocation {
      def apply(subject: String, verb: String, rest: String, pos: source.Position): ResultOfStringPassedToVerb = {
        registerFlatBranch(subject, Resources.shouldCannotAppearInsideAnIn, pos)
        new ResultOfStringPassedToVerb(verb, rest) {

          def is(testFun: => PendingStatement): Unit = {
            registerPendingTestToRun(verb.trim + " " + rest.trim, "is", List(), testFun _, pos)
          }
            // Note, won't have an is method that takes fixture => PendingStatement one, because don't want
          // to say is (fixture => pending), rather just say is (pending)
          def taggedAs(firstTestTag: Tag, otherTestTags: Tag*) = {
            val tagList = firstTestTag :: otherTestTags.toList
            new ResultOfTaggedAsInvocation(verb, rest, tagList) {
              // "A Stack" should "bla bla" taggedAs(SlowTest) is (pending)
              //                                               ^
              def is(testFun: => PendingStatement): Unit = {
                registerPendingTestToRun(verb.trim + " " + rest.trim, "is", tags, testFun _, pos)
              }
            }
          }
        }
      }
    }

  /**
   * Supports the shorthand form of shared test registration.
   *
   * For example, this method enables syntax such as the following in:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack (with one item)" should behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
   *                           ^
   * }}}
   *
   * This function is passed as an implicit parameter to a `should` method
   * provided in `ShouldVerb`, a `must` method
   * provided in `MustVerb`, and a `can` method
   * provided in `CanVerb`. When invoked, this function registers the
   * subject description (the  parameter to the function) and returns a `BehaveWord`.
   * 
   */
  protected implicit val shorthandSharedTestRegistrationFunction: StringVerbBehaveLikeInvocation =
    new StringVerbBehaveLikeInvocation {
      def apply(subject: String, pos: source.Position): BehaveWord = {
        registerFlatBranch(subject, Resources.shouldCannotAppearInsideAnIn, pos)
        new BehaveWord
      }
    }

  // TODO: I got a:
  // runsuite:
  // [scalatest] *** RUN ABORTED ***
  // [scalatest]   An exception or error caused a run to abort: Duplicate test name: should return the new exception with the clue string appended, separated by a space char if passed a function that does that (Engine.scala:464)
  // Shouldn't be Engine.scala clearly
  /**
   * Register a test to ignore, which has the given spec text, optional tags, and test function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the `execute`
   * methods. This method exists to make it easy to ignore an existing test by changing the call to `it`
   * to `ignore` without deleting or commenting out the actual test code. The test will not be executed, but a
   * report will be sent that indicates the test was ignored. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for `testNames` for an example.) The resulting test name must not have been registered previously on
   * this `FlatSpec` instance.
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
  private def registerTestToIgnore(specText: String, testTags: List[Tag], methodName: String, testFun: () => Future[compatible.Assertion], pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -5
    def transformToOutcomeParam: Future[compatible.Assertion] = testFun()
    engine.registerIgnoredAsyncTest(specText, transformToOutcome(transformToOutcomeParam), Resources.ignoreCannotAppearInsideAnInOrAnIs, None, pos, testTags: _*)
  }

  private def registerPendingTestToIgnore(specText: String, testTags: List[Tag], methodName: String, testFun: () => PendingStatement, pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -5
    engine.registerIgnoredAsyncTest(specText, transformPendingToOutcome(testFun), Resources.ignoreCannotAppearInsideAnInOrAnIs, None, pos, testTags: _*)
  }

  /**
   * A `Map` whose keys are `String` names of tagged tests and whose associated values are
   * the `Set` of tags for the test. If this `FlatSpec` contains no tags, this method returns an empty `Map`.
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
    // Therefore, in test-specific instance, so run the test.
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
   * Run zero to many of this `FlatSpec`'s tests.
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
   * @param testName an optional name of one test to execute. If `None`, all relevant tests should be executed.
   *                 I.e., `None` acts like a wildcard that means execute all relevant tests in this `FlatSpec`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
   *
   * @throws NullArgumentException if any of `testName`, `reporter`, `stopper`, `tagsToInclude`,
   *     `tagsToExclude`, or `configMap` is `null`.
   */
  protected override def runTests(testName: Option[String], args: Args): Status = {
    runTestsImpl(thisSuite, testName, args, true, parallelAsyncTestExecution, runTest)
  }

  /**
   * An immutable `Set` of test names. If this `FlatSpec` contains no tests, this method returns an
   * empty `Set`.
   *
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space. For example, consider this `FlatSpec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.FlatSpec
   *
   * class StackSpec extends FlatSpec {
   *
   *   "A Stack (when not empty)" must "allow me to pop" in {}
   *   it must "not be empty" in {}
   *
   *   "A Stack (when not full)" must "allow me to push" in {}
   *   it must "not be full" in {}
   * }
   * }}}
   *
   * Invoking `testNames` on this `FlatSpec` will yield a set that contains the following
   * two test name strings:
   * 
   *
   * {{{
   * "A Stack (when not empty) must allow me to pop"
   * "A Stack (when not empty) must not be empty"
   * "A Stack (when not full) must allow me to push"
   * "A Stack (when not full) must not be full"
   * }}}
   */
  override def testNames: Set[String] = {
    InsertionOrderSet(atomic.get.testNamesList)
  }

  override def run(testName: Option[String], args: Args): Status = {

    runImpl(thisSuite, testName, args, parallelAsyncTestExecution, super.run)
  }

  /**
   * Supports shared test registration in `FlatSpec`s.
   *
   * This field supports syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should behave like nonFullStack(stackWithOneItem)
   *           ^
   * }}}
   *
   * For more information and examples of the use of `behave`, see the <a href="#sharedTests">Shared tests section</a>
   * in the main documentation for this trait.
   * 
   */
  protected val behave = new BehaveWord

  /**
   * Suite style name.
   */
  final override val styleName: String = "org.scalatest.FlatSpec"

  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = createTestDataFor(testName, theConfigMap, this)
}
