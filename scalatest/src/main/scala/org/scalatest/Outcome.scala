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
import Requirements._
import org.scalatest.exceptions.StackDepthException

/**
 * Superclass for the possible outcomes of running a test.
 *
 * `Outcome` is the result type of the `withFixture` methods of traits
 * <a href="Suite.html#withFixture">`Suite`</a> and <a href="fixture/Suite.html#withFixture">`fixture.Suite`</a>, as well as their
 * <a href="Suite$NoArgTest.html">`NoArgTest`</a> and <a href="fixture/Suite$OneArgTest.html">`OneArgTest`</a> function types.
 * The four possible outcomes are:
 * 
 *
 * <ul>
 * <li><a href="Succeeded$.html">`Succeeded`</a> - indicates a test succeeded</li>
 * <li><a href="Failed.html">`Failed`</a> - indicates a test failed and contains an exception describing the failure</li>
 * <li><a href="Canceled.html">`Canceled`</a> - indicates a test was canceled and contains an exception describing the cancelation</li>
 * <li><a href="Pending$.html">`Pending`</a> - indicates a test was pending</li>
 * </ul>
 *
 * Note that "ignored" does not appear as a type of `Outcome`, because tests are
 * marked as ignored on the outside and skipped over as the suite executes. So an ignored test never runs, and therefore
 * never has an outcome. By contrast, a test is determined to be pending by running the test
 * and observing the actual outcome. If the test body completes abruptly with a `TestPendingException`,
 * then the outcome was that the test was pending.
 * 
 */
sealed abstract class Outcome extends Product with Serializable {

  /**
   * Indicates whether this `Outcome` represents a test that succeeded.
   *
   * This class's implementation of this method always returns `false`.
   * 
   *
   * @return true if this `Outcome` is an instance of `Succeeded`.
   */
  val isSucceeded: Boolean = false

  /**
   * Indicates whether this `Outcome` represents a test that failed.
   *
   * This class's implementation of this method always returns `false`.
   * 
   *
   * @return true if this `Outcome` is an instance of `Failed`.
   */
  val isFailed: Boolean = false

  /**
   * Indicates whether this `Outcome` represents a test that was canceled.
   *
   * This class's implementation of this method always returns `false`.
   * 
   *
   * @return true if this `Outcome` is an instance of `Canceled`.
   */
  val isCanceled: Boolean = false

  /**
   * Indicates whether this `Outcome` represents a test that was pending.
   *
   * This class's implementation of this method always returns `false`.
   * 
   *
   * @return true if this `Outcome` is an instance of `Pending`.
   */
  val isPending: Boolean = false

  /**
   * Indicates whether this `Outcome` represents a test that either failed or was canceled, in which case this `Outcome` will contain an exception.
   *
   * @return true if this `Outcome` is an instance of either `Failed` or `Canceled`.
   */
  val isExceptional: Boolean = false

  /**
   * Converts this `Outcome` to an `Option[Throwable]`.
   *
   * This class's implementation of this method always returns `None`.
   * 
   *
   * @return a `Some` wrapping the contained exception if this `Outcome` is an instance of either `Failed` or `Canceled`.
   */
  def toOption: Option[Throwable] = None
  
  /**
   * Converts this `Outcome` to a `Succeeded`.
   *
   * When this `Outcome` instance is not Succeeded, it behaves as followed:
   * 
   * 
   * <ul>
   *   <li>Failed(ex) - throws ex</li> 
   *   <li>Canceled(tce) - throws tce</li>
   *   <li>Pending - throws TestPendingException</li> 
   * </ul>
   *
   * @return Succeeded if this `Outcome` instance is a Succeeded.
   */
  def toSucceeded: Succeeded.type

  // Used internally to resuse the old code that was catching these exceptions when running tests. Eventually I would
  // like to rewrite that old code to use the result type, but it will still needs to catch and handle these exceptions
  // in the same way in case they come back from a user's withFixture implementation.
  private[scalatest] def toUnit: Unit = {
    this match {
      case Succeeded =>
      case Exceptional(e) => throw e
      case Pending => throw new exceptions.TestPendingException
    }
  }
}

/**
 * Companion object for trait `Outcome` that contains an implicit method that enables 
 * collections of `Outcome`s to be flattened into a collections of contained exceptions.
 */
object Outcome {

  import scala.language.implicitConversions

  /**
   * Enables collections of `Outcome`s to be flattened into a collections of contained exceptions.
   *
   *
   * Here's an example:
   * 
   *
   * {{{  <!-- class="stREPL" -->
   * scala&gt; import org.scalatest._
   * import org.scalatest._
   *
   * scala&gt; import prop.TableDrivenPropertyChecks._
   * import prop.TableDrivenPropertyChecks._
   *
   * scala&gt; val squares = // (includes errors)
   *      |   Table(
   *      |     ("x", "square"),
   *      |     ( 0 ,     0   ),
   *      |     ( 1 ,     1   ),
   *      |     ( 2 ,     4   ),
   *      |     ( 3 ,     8   ),
   *      |     ( 4 ,    16   ),
   *      |     ( 5 ,    26   ),
   *      |     ( 6 ,    36   )
   *      |   )
   * squares: org.scalatest.prop.TableFor2[Int,Int] =
   *   TableFor2((x,square), (0,0), (1,1), (2,4), (3,8), (4,16), (5,26), (6,36))
   * }}}
   *
   * Given the above table, which includes some errors, you can obtain an `IndexedSeq` of the `Outcome`s
   * of executing an assertion on each row of the table with `outcomeOf`, like this:
   * 
   * 
   * {{{  <!-- class="stREPL" -->
   * scala&gt; import OutcomeOf._
   * import OutcomeOf._
   *
   * scala&gt; import Matchers._
   * import Matchers._
   *
   * scala&gt; val outcomes = for ((x, square) &lt;- squares) yield outcomeOf { square shouldEqual x * x }
   * outcomes: IndexedSeq[org.scalatest.Outcome] =
   *   Vector(Succeeded, Succeeded, Succeeded,
   *   Failed(org.scalatest.exceptions.TestFailedException: 8 did not equal 9), Succeeded,
   *   Failed(org.scalatest.exceptions.TestFailedException: 26 did not equal 25), Succeeded)
   * }}}
   *
   * Now you have a collection of all the outcomes, including successful ones. If you just want the `Failed` and `Canceled` outcomes, which
   * contain exceptions, you can filter out anything that isn't "exceptional," like this:
   * 
   * 
   * {{{  <!-- class="stREPL" -->
   * scala&gt; outcomes.filter(_.isExceptional)
   * res1: IndexedSeq[org.scalatest.Outcome] =
   *   Vector(Failed(org.scalatest.exceptions.TestFailedException: 8 did not equal 9),
   *   Failed(org.scalatest.exceptions.TestFailedException: 26 did not equal 25))
   * }}}
   *
   * But if you just wanted the contained exceptions, you can (thanks to this implicit method) invoke `flatten` on your collection:
   * 
   * 
   * {{{  <!-- class="stREPL" -->
   * scala&gt; outcomes.flatten
   * res2: IndexedSeq[Throwable] =
   *   Vector(org.scalatest.exceptions.TestFailedException: 8 did not equal 9,
   *   org.scalatest.exceptions.TestFailedException: 26 did not equal 25)
   * }}}
   */
  implicit def convertOutcomeToIterator(outcome: Outcome): Iterator[Throwable] =
    outcome match {
      case Exceptional(ex) => // Return an iterator with one Throwable in it
        new Iterator[Throwable] {
          private var spent: Boolean = false
          def hasNext: Boolean = !spent
          def next: Throwable =
            if (!spent) {
              spent = true
              ex
           } else throw new NoSuchElementException
        }
      case _ => // Return an empty iterator
        new Iterator[Throwable] {
          def hasNext: Boolean = false
          def next: Throwable = throw new NoSuchElementException
        }
    }
}

/**
 * Superclass for the two outcomes of running a test that contain an exception: `Failed` and `Canceled`.
 *
 * This class provides a `toOption` method that returns a `Some` wrapping the contained exception, and
 * an `isExceptional` field with the value `true`. It's companion object provides an extractor that
 * enables patterns that match a test that either failed or canceled, as in:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * outcome match {
 *   case Exceptional(ex) =&gt; // handle failed or canceled case
 *   case _ =&gt; // handle succeeded, pending, or omitted case
 * }
 * }}}
 *
 * @param ex the `Throwable` contained in this `Exceptional`.
 */
sealed abstract class Exceptional(ex: Throwable) extends Outcome {

  /**
   * Indicates that this `Outcome` represents a test that either failed or was canceled.
   *
   * @return true
   */
  override val isExceptional: Boolean = true

  /**
   * Converts this `Exceptional` to a `Some` that wraps the contained exception.
   *
   * @return A `Some` wrapping the exception contained in this `Exceptional`.
   */
  override def toOption: Option[Throwable] = Some(ex)
}

/**
 * Companion object to class `Exceptional` that provides a factory method and an extractor that enables
 * patterns that match both `Failed` and `Canceled` outcomes and 
 * extracts the contained exception and a factory method.
 */
object Exceptional {

  /**
   * Creates an `Exceptional` instance given the passed `Throwable`.
   *
   * If the passed `Throwable` is an instance of `TestCanceledException`, this
   * method will return `Canceled` containing that `TestCanceledException`. Otherwise,
   * it returns a `Failed` containing the `Throwable`.
   * 
   *
   * For example, trait <a href="SeveredStackTraces.html">`SeveredStackTraces`</a> uses this
   * factory method to sever the stack trace of the exception contained in either a `Failed` and `Canceled` 
   * like this:
   * 
   * 
   * {{{  <!-- class="stHighlight" -->
   * abstract override def withFixture(test: NoArgTest): Outcome = {
   *   super.withFixture(test) match {
   *     case Exceptional(e: StackDepth) =&gt; Exceptional(e.severedAtStackDepth)
   *     case o =&gt; o
   *   }
   * }
   * }}}
   *
   * @return a `Failed` or `Canceled` containing the passed exception.
   */
  def apply(e: Throwable): Exceptional = 
    e match {
      case tce: exceptions.TestCanceledException => Canceled(tce)
      case _ => Failed(e)
    }

  /**
   * Extractor enabling patterns that match both `Failed` and `Canceled` outcomes, 
   * extracting the contained exception.
   *
   * For example, trait <a href="SeveredStackTraces.html">`SeveredStackTraces`</a> uses this
   * extractor to sever the stack trace of the exception contained in either a `Failed` and `Canceled` 
   * like this:
   * 
   * 
   * {{{  <!-- class="stHighlight" -->
   * abstract override def withFixture(test: NoArgTest): Outcome = {
   *   super.withFixture(test) match {
   *     case Exceptional(e: StackDepth) =&gt; Exceptional(e.severedAtStackDepth)
   *     case o =&gt; o
   *   }
   * }
   * }}}
   *
   * @param res the `Outcome` to extract the throwable from.
   * @return a `Some` wrapping the contained throwable if `res` is an instance of
   *     either `Failed` or `Canceled`, else `None`.
   */
  def unapply(res: Outcome): Option[Throwable] = 
    res match {
      case Failed(ex) => Some(ex)
      case Canceled(ex) => Some(ex)
      case _ => None
    }
}

/**
 * Outcome for a test that succeeded.
 *
 * Note: the difference between this `Succeeded` object and the similarly named <a href="SucceededStatus$.html">`SucceededStatus`</a>
 * object is that this object indicates one test (or assertion) succeeded, whereas the `SucceededStatus` object indicates the absence of any failed tests or
 * aborted suites during a run. Both are used as the result type of <a href="Suite.html#lifecycle-methods">`Suite`</a> lifecycle methods, but `Succeeded`
 * is a possible result of `withFixture`, whereas `SucceededStatus` is a possible result of `run`, `runNestedSuites`,
 * `runTests`, or `runTest`. In short, `Succeeded` is always just about one test (or assertion), whereas `SucceededStatus` could be
 * about something larger: multiple tests or an entire suite.
 * 
 */
case object Succeeded extends Outcome with compatible.Assertion {

  /**
   * Indicates that this `Outcome` represents a test that succeeded.
   *
   * This class's implementation of this method always returns `true`.
   * 
   *
   * @return true
   */
  override val isSucceeded: Boolean = true

  /**
   * Converts this `Outcome` to a `Succeeded`.
   *
   * @return This Succeeded instance.
   */
  def toSucceeded: Succeeded.type = this
}

/**
 * Outcome for a test that failed, containing an exception describing the cause of the failure.
 *
 * Note: the difference between this `Failed` class and the similarly named <a href="FailedStatus$.html">`FailedStatus`</a>
 * object is that an instance of this class indicates one test failed, whereas the `FailedStatus` object indicates either one or more tests failed
 * and/or one or more suites aborted during a run. Both are used as the result type of `Suite` lifecycle methods, but `Failed`
 * is a possible result of `withFixture`, whereas `FailedStatus` is a possible result of `run`, `runNestedSuites`,
 * `runTests`, or `runTest`. In short, `Failed` is always just about one test, whereas `FailedStatus` could be
 * about something larger: multiple tests or an entire suite.
 * 
 *
 * @param ex the `Throwable` contained in this `Failed`.
 */
case class Failed(exception: Throwable) extends Exceptional(exception) {

  require(!exception.isInstanceOf[exceptions.TestCanceledException], "a TestCanceledException was passed to Failed's constructor")
  require(!exception.isInstanceOf[exceptions.TestPendingException], "a TestPendingException was passed to Failed's constructor")

  /**
   * Indicates that this `Outcome` represents a test that failed.
   *
   * This class's implementation of this method always returns `true`.
   * 
   *
   * @return true
   */
  override val isFailed: Boolean = true
  
  /**
   * Converts this `Outcome` to a `Succeeded`.
   * 
   * The implmentation of this class will re-throw the passed in exception. 
   * 
   */
  def toSucceeded: Succeeded.type = throw exception
}

object Failed {

  /**
    * Creates a `Failed` instance, with a `TestFailedException` set as its `exception` field.
    *
    * @return An instance of `Failed` with a `TestFailedException` set as its `exception` field.
    */
  def apply()(implicit pos: source.Position): Failed = new Failed(new exceptions.TestFailedException((_: StackDepthException) => None, None, pos))

  /**
    * Creates a `Failed` instance with the passed in message.
    *
    * @param message the message for the `TestFailedException` set as its `exception` field
    * @return An instance of `Failed` with a `TestFailedException` created from passed in `message` set as its `exception` field.
    */
  def apply(message: String)(implicit pos: source.Position): Failed = new Failed(new exceptions.TestFailedException((_: StackDepthException) => Some(message), None, pos))

  /**
    * Creates a `Failed` instance with the passed in message and cause.
    *
    * @param message the message for the `TestFailedException` set as its `exception` field
    * @param cause the cause for the `TestFailedException` set as its `exception` field
    * @return An instance of `Failed` with a `TestFailedException` created from passed in `message` and `cause` set as its `exception` field.
    */
  def apply(message: String, cause: Throwable)(implicit pos: source.Position): Failed = {
    // I always wrap this in a TFE because I need to do that to get the message in there.
    require(!cause.isInstanceOf[exceptions.TestCanceledException], "a TestCanceledException was passed to a factory method in object Failed")
    require(!cause.isInstanceOf[exceptions.TestPendingException], "a TestPendingException was passed to a factory method in object Failed")
    new Failed(new exceptions.TestFailedException((_: StackDepthException) => Some(message), Some(cause), pos))
  }

  /**
    * Creates a `Failed` with the passed in cause.
    *
    * @param cause the passed in cause
    * @return A `Failed` with `exception` field set to a newly created `TestFailedException` using the passed in `cause`.
    */
  def here(cause: Throwable)(implicit pos: source.Position): Failed = {
    require(!cause.isInstanceOf[exceptions.TestCanceledException], "a TestCanceledException was passed to the \"here\" factory method in object Failed")
    require(!cause.isInstanceOf[exceptions.TestPendingException], "a TestPendingException was passed to the \"here\" factory method in object Failed")

    new Failed(
      if (cause.getMessage != null)
        new exceptions.TestFailedException((_: StackDepthException) => Some(cause.getMessage), Some(cause), pos)
       else
        new exceptions.TestFailedException((_: StackDepthException) => None, Some(cause), pos)
     )
  }
}

/**
 * Outcome for a test that was canceled, containing an exception describing the cause of the cancelation.
 *
 * @param ex the `TestCanceledException` contained in this `Exceptional`.
 */
case class Canceled(exception: exceptions.TestCanceledException) extends Exceptional(exception) {

  /**
   * Indicates that this `Outcome` represents a test that was canceled.
   *
   * This class's implementation of this method always returns `true`.
   * 
   *
   * @return true
   */
  override val isCanceled: Boolean = true
  
  /**
   * Converts this `Outcome` to a `Succeeded`.
   * 
   * The implmentation of this class will re-throw the passed in exception. 
   * 
   */
  def toSucceeded: Succeeded.type = throw exception
}

/**
 * Companion object to class `Canceled` that provides, in addition to the extractor and factory method
 * provided by the compiler given its companion is a case class, a second factory method 
 * that produces a `Canceled` outcome given a string message.
 */
object Canceled {

  /**
    * Creates a `Canceled` instance, with a `TestCanceledException` set as its `exception` field.
    *
    * @return An instance of `Canceled` with a `TestCanceledException` set as its `exception` field.
    */
  def apply()(implicit pos: source.Position): Canceled = new Canceled(new exceptions.TestCanceledException((_: StackDepthException) => None, None, Left(pos), None))

  /**
    * Creates a `Canceled` instance with the passed in message and cause.
    *
    * @param message the message for the `TestCanceledException` set as its `exception` field
    * @param cause the cause for the `TestCanceledException` set as its `exception` field
    * @return An instance of `Canceled` with a `TestCanceledException` created from passed in `message` and `cause` set as its `exception` field.
    */
  def apply(message: String, cause: Throwable)(implicit pos: source.Position): Canceled = // TODO write tests for NPEs
    new Canceled(new exceptions.TestCanceledException((_: StackDepthException) => Some(message), Some(cause), Left(pos), None))

  /**
    * Creates a `Canceled` instance with the passed in `Throwable`.  If the passed in `Throwable` is a `TestCanceledException`,
    * it will be set as `exception` field, in other case a new `TestCanceledException` will be created using `ex` as its `cause`
    *
    * @param ex the passed in `Throwable`
    * @return An instance of `Canceled` with `ex` set as its `exception` field if `ex` is a `TestCanceledException`, or a newly created `TestCanceledException` with `ex` set as its `cause` if `ex` is not a `TestCanceledException`.
    */
  def apply(ex: Throwable)(implicit pos: source.Position): Canceled = { // TODO write tests for NPEs
    ex match {
      case tce: exceptions.TestCanceledException => 
        new Canceled(tce)
      case _ =>
        val msg = ex.getMessage
        if (msg == null)
          new Canceled(new exceptions.TestCanceledException((_: StackDepthException) => None, Some(ex), Left(pos), None))
        else 
          new Canceled(new exceptions.TestCanceledException((_: StackDepthException) => Some(msg), Some(ex), Left(pos), None))
    }
  }

  /**
   * Creates a `Canceled` outcome given a string message.
   *
   * For example, trait `CancelAfterFailure` uses this factory method to create
   * a `Canceled` status if a `cancelRemaining` flag is set, which will
   * be the case if a test failed previously while running the suite:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * abstract override def withFixture(test: NoArgTest): Outcome = {
   *   if (cancelRemaining) 
   *     Canceled("Canceled by CancelOnFailure because a test failed previously")
   *   else
   *     super.withFixture(test) match {
   *       case failed: Failed =&gt;
   *         cancelRemaining = true
   *         failed
   *       case outcome =&gt; outcome
   *     }
   *  }
   * }}}
   */
  def apply(message: String)(implicit pos: source.Position): Canceled = {
    requireNonNull(message)
    val e = new exceptions.TestCanceledException((_: StackDepthException) => Some(message), None, Left(pos), None)
    //e.fillInStackTrace()
    Canceled(e)
  }

  /**
    * Creates a `Canceled` with the passed in cause.
    *
    * @param cause the passed in cause
    * @return A `Canceled` with `exception` field set to a newly created `TestCanceledException` using the passed in `cause`.
    */
  def here(cause: Throwable)(implicit pos: source.Position): Canceled = {
    new Canceled(
      if (cause.getMessage != null)
        new exceptions.TestCanceledException((_: StackDepthException) => Some(cause.getMessage), Some(cause), Left(pos), None)
       else
        new exceptions.TestCanceledException((_: StackDepthException) => None, Some(cause), Left(pos), None)
     )
  }
}

/**
 * Outcome for a test that was pending, which contains an optional string giving more information on what exactly is needed
 * for the test to become non-pending.
 *
 * @param message an optional message describing the reason the test is pending
 */
case object Pending extends Outcome {

  /**
   * Indicates that this `Outcome` represents a test that was pending.
   *
   * This class's implementation of this method always returns `true`.
   * 
   *
   * @return true
   */
  override val isPending: Boolean = true
  
  /**
   * Converts this `Outcome` to a `Succeeded`.
   * 
   * The implmentation of this class will throw `TestPendingException` with the passed in message. 
   * 
   */
  def toSucceeded: Succeeded.type = throw new exceptions.TestPendingException
}

