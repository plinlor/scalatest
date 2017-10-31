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
import org.scalatest.exceptions._
import Suite.anExceptionThatShouldCauseAnAbort
import Suite.autoTagClassAnnotations
import java.util.ConcurrentModificationException
import java.util.concurrent.atomic.AtomicReference
import words.{CanVerb, ResultOfAfterWordApplication, ShouldVerb, BehaveWord,
  MustVerb, StringVerbBlockRegistration, SubjectWithAfterWordRegistration}

/**
 * Implementation trait for class `WordSpec`, which facilitates a &ldquo;behavior-driven&rdquo; style of development (BDD), in which tests
 * are combined with text that specifies the behavior the tests verify.
 * 
 * <a href="WordSpec.html">`WordSpec`</a> is a class, not a trait, to minimize compile time given there is a slight compiler overhead to
 * mixing in traits compared to extending classes. If you need to mix the behavior of `WordSpec`
 * into some other class, you can use this trait instead, because class `WordSpec` does nothing more than extend this trait and add a nice `toString` implementation.
 * 
 *
 * See the documentation of the class for a <a href="WordSpec.html">detailed overview of `WordSpec`</a>.
 * 
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.WordSpecFinder"))
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
trait WordSpecLike extends TestSuite with TestRegistration with ShouldVerb with MustVerb with CanVerb with Informing with Notifying with Alerting with Documenting { thisSuite =>

  private final val engine = new Engine(Resources.concurrentWordSpecMod, "WordSpecLike")
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
   * `WordSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an `Alerter` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `WordSpec` is being executed, such as from inside a test function, it will forward the information to
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
    val stackDepthAdjustment = -1
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    engine.registerTest(testText, Transformer(testFun _), Resources.testCannotBeNestedInsideAnotherTest, "WordSpecLike.scala", "registerTest", 4, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
  }

  final def registerIgnoredTest(testText: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    engine.registerIgnoredTest(testText, Transformer(testFun _), Resources.testCannotBeNestedInsideAnotherTest, "WordSpecLike.scala", "registerIgnoredTest", 4, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

  /**
   * Register a test with the given spec text, optional tags, and test function value that takes no arguments.
   * An invocation of this method is called an &ldquo;example.&rdquo;
   *
   * This method will register the test for later execution via an invocation of one of the `execute`
   * methods. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for `testNames` for an example.) The resulting test name must not have been registered previously on
   * this `WordSpec` instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param methodName Caller's methodName
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
    engine.registerTest(specText, Transformer(testFun), Resources.inCannotAppearInsideAnotherIn, "WordSpecLike.scala", methodName, stackDepth, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
  }

  /**
   * Register a test to ignore, which has the given spec text, optional tags, and test function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the `execute`
   * methods. This method exists to make it easy to ignore an existing test by changing the call to `it`
   * to `ignore` without deleting or commenting out the actual test code. The test will not be executed, but a
   * report will be sent that indicates the test was ignored. The name of the test will be a concatenation of the text of all surrounding describers,
   * from outside in, and the passed spec text, with one space placed between each item. (See the documenation
   * for `testNames` for an example.) The resulting test name must not have been registered previously on
   * this `WordSpec` instance.
   *
   * @param specText the specification text, which will be combined with the descText of any surrounding describers
   * to form the test name
   * @param testTags the optional list of tags for this test
   * @param methodName Caller's methodName
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
    engine.registerIgnoredTest(specText, Transformer(testFun), Resources.ignoreCannotAppearInsideAnIn, "WordSpecLike.scala", methodName, stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

  private def exceptionWasThrownInClauseMessageFun(verb: String, className: UnquotedString, description: String, errorMessage: String): String =
    verb match {
      case "when" => FailureMessages.exceptionWasThrownInWhenClause(Prettifier.default, className, description, errorMessage)
      case "which" => FailureMessages.exceptionWasThrownInWhichClause(Prettifier.default, className, description, errorMessage)
      case "that" => FailureMessages.exceptionWasThrownInThatClause(Prettifier.default, className, description, errorMessage)
      case "should" => FailureMessages.exceptionWasThrownInShouldClause(Prettifier.default, className, description, errorMessage)
      case "must" => FailureMessages.exceptionWasThrownInMustClause(Prettifier.default, className, description, errorMessage)
      case "can" => FailureMessages.exceptionWasThrownInCanClause(Prettifier.default, className, description, errorMessage)
    }

  private def registerBranch(description: String, childPrefix: Option[String], verb: String, methodName:String, stackDepth: Int, adjustment: Int, pos: source.Position, fun: () => Unit): Unit = {

    def registrationClosedMessageFun: String =
      verb match {
        case "should" => Resources.shouldCannotAppearInsideAnIn
        case "when" => Resources.whenCannotAppearInsideAnIn
        case "which" => Resources.whichCannotAppearInsideAnIn
        case "that" => Resources.thatCannotAppearInsideAnIn
        case "must" => Resources.mustCannotAppearInsideAnIn
        case "can" => Resources.canCannotAppearInsideAnIn
      }

    try {
      registerNestedBranch(description, childPrefix, fun(), registrationClosedMessageFun, "WordSpecLike.scala", methodName, stackDepth, adjustment, None, Some(pos))
    }
    catch {
      case e: TestFailedException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideItOrTheyClauseNotShouldMustWhenThatWhichOrCanClause, Some(e), e.position.getOrElse(pos))
      case e: TestCanceledException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideItOrTheyClauseNotShouldMustWhenThatWhichOrCanClause, Some(e), e.position.getOrElse(pos))
      case nae: NotAllowedException => throw nae
      case trce: TestRegistrationClosedException => throw trce
      case e: DuplicateTestNameException => throw new NotAllowedException(exceptionWasThrownInClauseMessageFun(verb, UnquotedString(e.getClass.getName), description, e.getMessage), Some(e), e.position.getOrElse(pos))
      case other: Throwable if (!Suite.anExceptionThatShouldCauseAnAbort(other)) => throw new NotAllowedException(exceptionWasThrownInClauseMessageFun(verb, UnquotedString(other.getClass.getName), if (description.endsWith(" " + verb)) description.substring(0, description.length - (" " + verb).length) else description, other.getMessage), Some(other), pos)
      case other: Throwable => throw other
    }
  }
  
  private def registerShorthandBranch(childPrefix: Option[String], notAllowMessage: => String, methodName:String, stackDepth: Int, adjustment: Int, pos: source.Position, fun: () => Unit): Unit = {

    // Shorthand syntax only allow at top level, and only after "..." when, "..." should/can/must, or it should/can/must
    if (engine.currentBranchIsTrunk) {
      val currentBranch = engine.atomic.get.currentBranch
      // headOption because subNodes are in reverse order
      currentBranch.subNodes.headOption match {
        case Some(last) => 
          last match {
            case DescriptionBranch(_, descriptionText, _, _) =>

              def registrationClosedMessageFun: String =
                methodName match {
                  case "when" => Resources.whenCannotAppearInsideAnIn
                  case "which" => Resources.whichCannotAppearInsideAnIn
                  case "that" => Resources.thatCannotAppearInsideAnIn
                  case "should" => Resources.shouldCannotAppearInsideAnIn
                  case "must" => Resources.mustCannotAppearInsideAnIn
                  case "can" => Resources.canCannotAppearInsideAnIn
                }
              try {
                registerNestedBranch(descriptionText, childPrefix, fun(), registrationClosedMessageFun, "WordSpecLike.scala", methodName, stackDepth, adjustment, None, Some(pos))
              }
              catch {
                case e: TestFailedException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideItOrTheyClauseNotShouldMustWhenThatWhichOrCanClause, Some(e), e.position.getOrElse(pos))
                case e: TestCanceledException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideItOrTheyClauseNotShouldMustWhenThatWhichOrCanClause, Some(e), e.position.getOrElse(pos))
                case nae: NotAllowedException => throw nae
                case trce: TestRegistrationClosedException => throw trce
                case e: DuplicateTestNameException => throw new NotAllowedException(exceptionWasThrownInClauseMessageFun(methodName, UnquotedString(e.getClass.getName), descriptionText, e.getMessage), Some(e), e.position.getOrElse(pos))
                case other: Throwable if (!Suite.anExceptionThatShouldCauseAnAbort(other)) => throw new NotAllowedException(exceptionWasThrownInClauseMessageFun(methodName, UnquotedString(other.getClass.getName), if (descriptionText.endsWith(" " + methodName)) descriptionText.substring(0, descriptionText.length - (" " + methodName).length) else descriptionText, other.getMessage), Some(other), pos)
                case other: Throwable => throw other
              }

            case _ => 
              throw new NotAllowedException(notAllowMessage, None, pos)
          }
        case None => 
          throw new NotAllowedException(notAllowMessage, None, pos)
      }
    }
    else
      throw new NotAllowedException(notAllowMessage, None, pos)
  }

  /**
   * Class that supports the registration of tagged tests.
   *
   * Instances of this class are returned by the `taggedAs` method of 
   * class `WordSpecStringWrapper`.
   * 
   *
   * @author Bill Venners
   */
  protected final class ResultOfTaggedAsInvocationOnString(specText: String, tags: List[Tag]) {

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
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def in(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
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
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def is(testFun: => PendingStatement)(implicit pos: source.Position): Unit = {
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
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def ignore(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
      registerTestToIgnore(specText, tags, "ignore", testFun _, pos)
    }
  }       

  /**
   * A class that via an implicit conversion (named `convertToWordSpecStringWrapper`) enables
   * methods `when`, `which`, `in`, `is`, `taggedAs`
   * and `ignore` to be invoked on `String`s.
   *
   * This class provides much of the syntax for `WordSpec`, however, it does not add
   * the verb methods (`should`, `must`, and `can`) to `String`.
   * Instead, these are added via the `ShouldVerb`, `MustVerb`, and `CanVerb`
   * traits, which `WordSpec` mixes in, to avoid a conflict with implicit conversions provided
   * in `Matchers` and `MustMatchers`. 
   * 
   *
   * @author Bill Venners
   */
  protected final class WordSpecStringWrapper(string: String) {

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
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def in(f: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
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
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def ignore(f: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
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
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def is(f: => PendingStatement)(implicit pos: source.Position): Unit = {
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
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def taggedAs(firstTestTag: Tag, otherTestTags: Tag*) = {
      val tagList = firstTestTag :: otherTestTags.toList
      new ResultOfTaggedAsInvocationOnString(string, tagList)
    }

    /**
     * Registers a `when` clause.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack" when { ... }
     *           ^
     * }}}
     *
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def when(f: => Unit)(implicit pos: source.Position): Unit = {
      // SKIP-SCALATESTJS-START
      val stackDepth = 4
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 6
      registerBranch(string, Some("when"), "when", "when", stackDepth, -2, pos, f _)
    }

    /**
     * Registers a `when` clause that is followed by an ''after word''.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * val theUser = afterWord("the user")
     *
     * "A Stack" when theUser { ... }
     *           ^
     * }}}
     *
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def when(resultOfAfterWordApplication: ResultOfAfterWordApplication)(implicit pos: source.Position): Unit = {
      registerBranch(string, Some("when " + resultOfAfterWordApplication.text), "when", "when", 4, -2, pos, resultOfAfterWordApplication.f)
    }

    /**
     * Registers a `that` clause.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "a rerun button" that {
     *                  ^
     * }}}
     *
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def that(f: => Unit)(implicit pos: source.Position): Unit = {
      // SKIP-SCALATESTJS-START
      val stackDepth = 4
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 6
      registerBranch(string.trim + " that", None, "that", "that", stackDepth, -2, pos, f _)
    }

    /**
     * Registers a `which` clause.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "a rerun button," which {
     *                  ^
     * }}}
     *
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def which(f: => Unit)(implicit pos: source.Position): Unit = {
      // SKIP-SCALATESTJS-START
      val stackDepth = 4
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 6
      registerBranch(string.trim + " which", None, "which", "which", stackDepth, -2, pos, f _)
    }

    /**
     * Registers a `that` clause that is followed by an ''after word''.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * def is = afterWord("is")
     *
     * "a rerun button" that is {
     *                  ^
     * }}}
     *
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def that(resultOfAfterWordApplication: ResultOfAfterWordApplication)(implicit pos: source.Position): Unit = {
      registerBranch(string.trim + " that " + resultOfAfterWordApplication.text.trim, None, "that", "that", 4, -2, pos, resultOfAfterWordApplication.f)
    }
    
    /**
     * Registers a `which` clause that is followed by an ''after word''.
     *
     * For example, this method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * def is = afterWord("is")
     *
     * "a rerun button," which is {
     *                  ^
     * }}}
     *
     * For more information and examples of this method's use, see the <a href="WordSpec.html">main documentation</a> for trait `WordSpec`.
     * 
     */
    def which(resultOfAfterWordApplication: ResultOfAfterWordApplication)(implicit pos: source.Position): Unit = {
      registerBranch(string.trim + " which " + resultOfAfterWordApplication.text.trim, None, "which", "which", 4, -2, pos, resultOfAfterWordApplication.f)
    }
  }

  /**
   * Class whose instances are ''after word''s, which can be used to reduce text duplication.
   *
   * If you are repeating a word or phrase at the beginning of each string inside
   * a block, you can "move the word or phrase" out of the block with an after word.
   * You create an after word by passing the repeated word or phrase to the `afterWord` method.
   * Once created, you can place the after word after `when`, a verb
   * (`should`, `must`, or `can`), or
   * `which`. (You can't place one after `in` or `is`, the
   * words that introduce a test.) Here's an example that has after words used in all three
   * places:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.WordSpec
   * 
   * class ScalaTestGUISpec extends WordSpec {
   * 
   *   def theUser = afterWord("the user")
   *   def display = afterWord("display")
   *   def is = afterWord("is")
   * 
   *   "The ScalaTest GUI" when theUser {
   *     "clicks on an event report in the list box" should display {
   *       "a blue background in the clicked-on row in the list box" in {}
   *       "the details for the event in the details area" in {}
   *       "a rerun button" which is {
   *         "enabled if the clicked-on event is rerunnable" in {}
   *         "disabled if the clicked-on event is not rerunnable" in {}
   *       }
   *     }
   *   }
   * }
   * }}}
   *
   * Running the previous `WordSpec` in the Scala interpreter would yield:
   * 
   *
   * {{{  <!-- class="stREPL" -->
   * scala> (new ScalaTestGUISpec).execute()
   * <span class="stGreen">The ScalaTest GUI (when the user clicks on an event report in the list box) 
   * - should display a blue background in the clicked-on row in the list box
   * - should display the details for the event in the details area
   * - should display a rerun button that is enabled if the clicked-on event is rerunnable
   * - should display a rerun button that is disabled if the clicked-on event is not rerunnable</span>
   * }}}
   */
  protected final class AfterWord(text: String) {

    /**
     * Supports the use of ''after words''.
     *
     * This method transforms a block of code into a `ResultOfAfterWordApplication`, which
     * is accepted by `when`, `should`, `must`, `can`, and `which`
     * methods.  For more information, see the <a href="WordSpec.html#AfterWords">main documentation`</a> for trait `WordSpec`.
     * 
     */
    def apply(f: => Unit) = new ResultOfAfterWordApplication(text, f _)
  }

  /**
   * Creates an ''after word'' that an be used to reduce text duplication.
   *
   * If you are repeating a word or phrase at the beginning of each string inside
   * a block, you can "move the word or phrase" out of the block with an after word.
   * You create an after word by passing the repeated word or phrase to the `afterWord` method.
   * Once created, you can place the after word after `when`, a verb
   * (`should`, `must`, or `can`), or
   * `which`. (You can't place one after `in` or `is`, the
   * words that introduce a test.) Here's an example that has after words used in all three
   * places:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.WordSpec
   * 
   * class ScalaTestGUISpec extends WordSpec {
   * 
   *   def theUser = afterWord("the user")
   *   def display = afterWord("display")
   *   def is = afterWord("is")
   * 
   *   "The ScalaTest GUI" when theUser {
   *     "clicks on an event report in the list box" should display {
   *       "a blue background in the clicked-on row in the list box" in {}
   *       "the details for the event in the details area" in {}
   *       "a rerun button" which is {
   *         "enabled if the clicked-on event is rerunnable" in {}
   *         "disabled if the clicked-on event is not rerunnable" in {}
   *       }
   *     }
   *   }
   * }
   * }}}
   *
   * Running the previous `WordSpec` in the Scala interpreter would yield:
   * 
   *
   * {{{  <!-- class="stREPL" -->
   * scala> (new ScalaTestGUISpec).execute()
   * <span class="stGreen">The ScalaTest GUI (when the user clicks on an event report in the list box) 
   * - should display a blue background in the clicked-on row in the list box
   * - should display the details for the event in the details area
   * - should display a rerun button that is enabled if the clicked-on event is rerunnable
   * - should display a rerun button that is disabled if the clicked-on event is not rerunnable</span>
   * }}}
   */
  protected def afterWord(text: String) = new AfterWord(text)

  // SKIP-SCALATESTJS-START
  private[scalatest] val stackDepth = 3
  // SKIP-SCALATESTJS-END
  //SCALATESTJS-ONLY private[scalatest] val stackDepth: Int = 10
  
  /**
   * Class that supports shorthand scope registration via the instance referenced from `WordSpecLike`'s `it` field.
   *
   * This class enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack" when { ... }
   * 
   * it should { ... }
   * ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the main documentation 
   * for `WordSpec`.
   * 
   */
  protected final class ItWord {
    
    /**
     * Supports the registration of scope with `should` in a `WordSpecLike`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack" when { ... }
     * 
     * it should { ... }
     *    ^
     * }}}
     *
     * For examples of scope registration, see the <a href="WordSpec.html">main documentation</a>
     * for `WordSpec`.
     * 
     */
    def should(right: => Unit)(implicit pos: source.Position): Unit = {
      registerShorthandBranch(Some("should"), Resources.itMustAppearAfterTopLevelSubject, "should", stackDepth, -2, pos, right _)
    }
    
    /**
     * Supports the registration of scope with `must` in a `WordSpecLike`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack" when { ... }
     * 
     * it must { ... }
     *    ^
     * }}}
     *
     * For examples of scope registration, see the <a href="WordSpec.html">main documentation</a>
     * for `WordSpec`.
     * 
     */
    def must(right: => Unit)(implicit pos: source.Position): Unit = {
      registerShorthandBranch(Some("must"), Resources.itMustAppearAfterTopLevelSubject, "must", stackDepth, -2, pos, right _)
    }
    
    /**
     * Supports the registration of scope with `can` in a `WordSpecLike`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack" when { ... }
     * 
     * it can { ... }
     *    ^
     * }}}
     *
     * For examples of scope registration, see the <a href="WordSpec.html">main documentation</a>
     * for `WordSpec`.
     * 
     */
    def can(right: => Unit)(implicit pos: source.Position): Unit = {
      registerShorthandBranch(Some("can"), Resources.itMustAppearAfterTopLevelSubject, "can", stackDepth, -2, pos, right _)
    }
    
    /**
     * Supports the registration of scope with `when` in a `WordSpecLike`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack" should { ... }
     * 
     * it when { ... }
     *    ^
     * }}}
     *
     * For examples of scope registration, see the <a href="WordSpec.html">main documentation</a>
     * for `WordSpec`.
     * 
     */
    def when(right: => Unit)(implicit pos: source.Position): Unit = {
      registerShorthandBranch(Some("when"), Resources.itMustAppearAfterTopLevelSubject, "when", stackDepth, -2, pos, right _)
    }
  }
  
  /**
   * Supports shorthand scope registration in `WordSpecLike`s.
   *
   * This field enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack" when { ... }
   * 
   * it should { ... }
   * ^
   * }}}
   *
   * For more information and examples of the use of the `it` field, see the main documentation 
   * for `WordSpec`.
   * 
   */
  protected val it = new ItWord
  
  /**
   * Class that supports shorthand scope registration via the instance referenced from `WordSpecLike`'s `they` field.
   *
   * This class enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "Basketball players" when { ... }
   * 
   * they should { ... }
   * ^
   * }}}
   *
   * For more information and examples of the use of the `they` field, see the main documentation 
   * for `WordSpec`.
   * 
   */
  protected final class TheyWord {
    
    /**
     * Supports the registration of scope with `should` in a `WordSpecLike`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "Basketball players" when { ... }
     * 
     * they should { ... }
     *      ^
     * }}}
     *
     * For examples of scope registration, see the <a href="WordSpec.html">main documentation</a>
     * for `WordSpec`.
     * 
     */
    def should(right: => Unit)(implicit pos: source.Position): Unit = {
      registerShorthandBranch(Some("should"), Resources.theyMustAppearAfterTopLevelSubject, "should", stackDepth, -2, pos, right _)
    }
    
    /**
     * Supports the registration of scope with `must` in a `WordSpecLike`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "Basketball players" when { ... }
     * 
     * they must { ... }
     *      ^
     * }}}
     *
     * For examples of scope registration, see the <a href="WordSpec.html">main documentation</a>
     * for `WordSpec`.
     * 
     */
    def must(right: => Unit)(implicit pos: source.Position): Unit = {
      registerShorthandBranch(Some("must"), Resources.theyMustAppearAfterTopLevelSubject, "must", stackDepth, -2, pos, right _)
    }
    
    /**
     * Supports the registration of scope with `can` in a `WordSpecLike`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "Basketball players" when { ... }
     * 
     * they can { ... }
     *      ^
     * }}}
     *
     * For examples of scope registration, see the <a href="WordSpec.html">main documentation</a>
     * for `WordSpec`.
     * 
     */
    def can(right: => Unit)(implicit pos: source.Position): Unit = {
      registerShorthandBranch(Some("can"), Resources.theyMustAppearAfterTopLevelSubject, "can", stackDepth, -2, pos, right _)
    }
    
    /**
     * Supports the registration of scope with `when` in a `WordSpecLike`.
     *
     * This method supports syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "Basketball players" should { ... }
     * 
     * they when { ... }
     *      ^
     * }}}
     *
     * For examples of scope registration, see the <a href="WordSpec.html">main documentation</a>
     * for `WordSpec`.
     * 
     */
    def when(right: => Unit)(implicit pos: source.Position): Unit = {
      registerShorthandBranch(Some("when"), Resources.theyMustAppearAfterTopLevelSubject, "when", stackDepth, -2, pos, right _)
    }
  }
  
  /**
   * Supports shorthand scope registration in `WordSpecLike`s.
   *
   * This field enables syntax such as the following test registration:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack" when { ... }
   * 
   * they should { ... }
   * ^
   * }}}
   *
   * For more information and examples of the use of the `they` field, see the main documentation 
   * for `WordSpec`.
   * 
   */
  protected val they = new TheyWord

  import scala.language.implicitConversions

  /**
   * Implicitly converts `String`s to `WordSpecStringWrapper`, which enables
   * methods `when`, `which`, `in`, `is`, `taggedAs`
   * and `ignore` to be invoked on `String`s.
   */
  protected implicit def convertToWordSpecStringWrapper(s: String): WordSpecStringWrapper = new WordSpecStringWrapper(s)

  // Used to enable should/can/must to take a block (except one that results in type string. May
  // want to mention this as a gotcha.)
  /*
import org.scalatest.WordSpec

class MySpec extends WordSpec {

  "bla bla bla" should {
     "do something" in {
        assert(1 + 1 === 2)
      }
      "now it is a string"
   }
}
delme.scala:6: error: no implicit argument matching parameter type (String, String, String) => org.scalatest.verb.ResultOfStringPassedToVerb was found.
  "bla bla bla" should {
                ^
one error found
  
   */
  /**
   * Supports the registration of subjects.
   *
   * For example, this method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack" should { ...
   *           ^
   * }}}
   *
   * This function is passed as an implicit parameter to a `should` method
   * provided in `ShouldVerb`, a `must` method
   * provided in `MustVerb`, and a `can` method
   * provided in `CanVerb`. When invoked, this function registers the
   * subject and executes the block.
   * 
   */
  protected implicit val subjectRegistrationFunction: StringVerbBlockRegistration =
    new StringVerbBlockRegistration {
      def apply(left: String, verb: String, pos: source.Position, f: () => Unit): Unit = registerBranch(left, Some(verb), verb, "apply", 6, -2, pos, f)
    }

  /**
   * Supports the registration of subject descriptions with after words.
   *
   * For example, this method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * def provide = afterWord("provide")
   *
   * "The ScalaTest Matchers DSL" can provide { ... }
   *                              ^
   * }}}
   *
   * This function is passed as an implicit parameter to a `should` method
   * provided in `ShouldVerb`, a `must` method
   * provided in `MustVerb`, and a `can` method
   * provided in `CanVerb`. When invoked, this function registers the
   * subject and executes the block.
   * 
   */
  protected implicit val subjectWithAfterWordRegistrationFunction: SubjectWithAfterWordRegistration =
    new SubjectWithAfterWordRegistration {
      def apply(left: String, verb: String, resultOfAfterWordApplication: ResultOfAfterWordApplication, pos: source.Position): Unit = {
        val afterWordFunction =
          () => {
            // SKIP-SCALATESTJS-START
            val stackDepth = 10
            // SKIP-SCALATESTJS-END
            //SCALATESTJS-ONLY val stackDepth = 15
            registerBranch(resultOfAfterWordApplication.text, None, verb, "apply", stackDepth, -2, pos, resultOfAfterWordApplication.f)
          }
        // SKIP-SCALATESTJS-START
        val stackDepth = 7
        // SKIP-SCALATESTJS-END
        //SCALATESTJS-ONLY val stackDepth = 9
        registerBranch(left, Some(verb), verb, "apply", stackDepth, -2, pos, afterWordFunction)
      }
    }

  /**
   * A `Map` whose keys are `String` names of tagged tests and whose associated values are
   * the `Set` of tags for the test. If this `WordSpec` contains no tags, this method returns an empty `Map`.
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
   * Run zero to many of this `WordSpec`'s tests.
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
   * An immutable `Set` of test names. If this `WordSpec` contains no tests, this method returns an
   * empty `Set`.
   *
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the text of each surrounding describer, in order from outside in, and the text of the
   * example itself, with all components separated by a space. For example, consider this `WordSpec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.WordSpec
   *
   * class StackSpec {
   *   "A Stack" when {
   *     "not empty" must {
   *       "allow me to pop" in {}
   *     }
   *     "not full" must {
   *       "allow me to push" in {}
   *     }
   *   }
   * }
   * }}}
   *
   * Invoking `testNames` on this `WordSpec` will yield a set that contains the following
   * two test name strings:
   * 
   *
   * {{{ class="stExamples">
   * "A Stack (when not empty) must allow me to pop"
   * "A Stack (when not full) must allow me to push"
   * }}}
   */
  override def testNames: Set[String] = {
    InsertionOrderSet(atomic.get.testNamesList)
  }

  override def run(testName: Option[String], args: Args): Status = {

    runImpl(thisSuite, testName, args, super.run)
  }

  /**
   * Supports shared test registration in `WordSpec`s.
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
  final override val styleName: String = "org.scalatest.WordSpec"
    
  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = createTestDataFor(testName, theConfigMap, this)
}
