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

import scala.collection.GenSet
import java.io.Serializable
import scala.concurrent.{ExecutionException, Future, Promise}
import scala.util.{Try, Success, Failure}

/**
 * The result status of running a test or a suite, which is used to support parallel and asynchronous execution of tests.
 *
 * This trait is the result type of the "run" lifecycle methods of trait <a href="Suite.html#lifecycle-methods">`Suite`</a>:
 * `run`, `runNestedSuites`, `runTests`, and `runTest`. It can be used to determine whether
 * a test or suite has completed, and if so, whether it succeeded, and if not, whether an exception was thrown that was
 * not yet reported via a ScalaTest event. A `Status` is like a domain-specific `Future[Boolean]`, where:
 * 
 *
 * <ul>
 * <li>an activity in which no test failed and no suite aborted is represented by `Success(true)`</li>
 * <li>an activity during which at least one test failed or one suite aborted, but all exceptions that occured
 *     were reported by a ScalaTest events (such as <a href="exceptions/TestFailedException.html">`TestFailedException`</a>)
 *     is represented by `Success(false)`</li>
 * <li>an activity during which at least one test failed or one suite aborted and at least one exception occurred that was
 *     ''not'' reported via a ScalaTest event is represented by `Failure(unreportedException)`</li>
 * </ul>
 *
 * Note that pending and canceled tests will not cause a `Status` to fail. Only failed tests
 * and aborted suites will cause a `Status` to fail.
 * 
 *
 * One use case of `Status` is to ensure that "after" code (such as an `afterEach` or `afterAll` method)
 * does not execute until after the relevant entity (one test, one suite, or all of a suite's tests or nested suites) has completed.
 * Another use case is to implement the default behavior of asynchronous styles, in which subsequent each test does not begin
 * execution until after the previous test has completed.
 * 
 */
sealed trait Status { thisStatus =>

  // SKIP-SCALATESTJS-START
  /**
   * Blocking call that waits until the entity represented by this `Status` (one test, one suite, or all of
   * a suite's tests or nested suites) has completed, then returns `true` if no tests failed and no
   * suites aborted, else returns `false`, or if an unreported exception has been installed, completes
   * abruptly with that exception.
   * 
   * This method only reports `false` if there was a failed test or aborted suite in the context of the "run" lifecycle method
   * from which it was returned.
   * For example, if you call `succeeds` on a `Status` returned by `runTest`, `succeeds`
   * will (after that test has completed) return `false` if the test whose name was passed to `runTest` fails,
   * else it will return `true`.
   * In other words, so long as the test doesn't fail &#8212;whether the test succeeds, is canceled, or is pending&#8212;`succeeds`
   * will return `true`. 
   * If you call `succeeds` on a `Status` returned by `runTests`, by contrast, `succeeds`
   * will (after the suite's
   * tests have completed) return `true` only if none of the tests in the suite fail. If any test in the suite fails,
   * `succeeds` will return `false`.
   * If you call `succeeds` on a `Status` returned by `runNestedSuites`, `succeeds` will
   * return true only if no tests fail and no suites abort when running all nested suites (and their nested suites, recursively).
   * Similarly, if you call `succeeds` on a `Status` returned by `run`, `succeeds` will
   * return true only if no tests fail and no suites abort when running all tests nested suites (and their nested suites, recursively).
   * 
   *
   * If this `Status` fails with an "unreported exception," an exception that occurred during the
   * activity represented by this `Status` that was not reported to the `Reporter` via a
   * ScalaTest event, the `succeeds` method will complete abruptly with that exception. If the
   * original exception was a run-aborting exception, such as `StackOverflowError`, the 
   * `unreportedException` method will return a `java.util.ExecutionException` that contains
   * the original run-aborting exception as its cause. The `succeeds` method will in that case
   * complete abruptly with the `ExecutionException` that wraps the original run-aborting exception.
   * 
   *
   * ''Note: because blocking is not possible on Scala.js, this method is not available on Scala.js.''
   * 
   *
   * @return after waiting until completion, `true` if no tests failed and no suites aborted, `false` otherwise
   * @throws unreportedException if an exception occurred during the activity represented by this `Status` that was not reported
   *            via a ScalaTest event and therefore was installed as an unreported exception on this `Status`.
   */
  def succeeds(): Boolean
  // SKIP-SCALATESTJS-END

  /**
   * Non-blocking call that indicates whether the entity represented by this
   * `Status` (one test, one suite, or all of a suite's tests or nested suites) has completed. Because this is non-blocking,
   * you can use this to poll the completion status.
   * 
   * Note: this method will not indicate whether a test has failed, suite has aborted, or an unreported exception has been installed.
   * It just indicates whether the `Status` has completed or not by returning `true` or `false`.
   * 
   *
   * @return `true` if the test or suite run is already completed, `false` otherwise.
   */
  def isCompleted: Boolean

  // SKIP-SCALATESTJS-START
  /**
   * Blocking call that waits until the entity represented by this `Status` (one test, one suite, or all of
   * a suite's tests or nested suites) has completed, then either returns normally, or if an unreported exception has
   * been installed, completes abruptly with that unreported exception.
   *
   * If this `Status` fails with an "unreported exception," an exception that occurred during the
   * activity represented by this `Status` that was not reported to the `Reporter` via a
   * ScalaTest event, the `waitUntilCompleted` method will complete abruptly with that exception. If the
   * original exception was a run-aborting exception, such as `StackOverflowError`, the 
   * `unreportedException` method will return a `java.util.ExecutionException` that contains
   * the original run-aborting exception as its cause. The `waitUntilCompleted` method will in that case
   * complete abruptly with the `ExecutionException` that wraps the original run-aborting exception.
   * 
   *
   * ''Note: because blocking is not possible on Scala.js, this method is not available on Scala.js.''
   * 
   *
   * @throws unreportedException if an exception occurred during the activity represented by this `Status` that was not reported
   *            via a ScalaTest event and therefore was installed as an unreported exception on this `Status`.
   */
  def waitUntilCompleted()
  // SKIP-SCALATESTJS-END

  /**
   * Registers the passed callback function to be executed when this status completes.
   *
   * If an unreported exception has been installed on this `Status`, the 
   * `Try` passed into the callback function will be a `Failure` containing that exception. Otherwise
   * the `Try` will be a `Success` containing true if no tests failed
   * or suites aborted during the activity represented by this `Status`, else `false`. 
   * 
   *
   * The callback functions registered with `whenCompleted` will be executed ''after'' the `Status`
   * has completed, in an undefined order. If the `Status` has already completed, functions passed to this method will be
   * executed immediately by the calling thread before returning.
   * 
   *
   * Any exception thrown by a callback function will be propagated back on the thread used to invoke the callback.
   * 
   *
   * Internally ScalaTest uses this method to register callbacks that
   * fire completion events (`TestSucceeded`, `TestFailed`,
   * `SuiteCompleted`, ''etc.'') to the `Reporter`.
   * 
   *
   * @param callback the callback function to execute once this `Status` has completed
   */
  def whenCompleted(callback: Try[Boolean] => Unit)

  // TODO: We are not yet propagating installed unreported exceptions in thenRun. Write the tests and implement the code.
  /**
   * Registers a `Status`-producing by-name function to execute after this
   * `Status` completes, returning a `Status` that mirrors the `Status`
   * returned by the by-name.
   *
   * The `Status` returned by this method will complete when the status produced by the 
   * `Status` produced by the passed-by name completes. The returned `Status`
   * will complete with the same `succeeds` and `unreportedException` values.
   * But unlike the `Status` produced by the by-name, the returned `Status` will
   * be available immediately.
   * 
   *
   * If the by-name function passed to this method completes abruptly with a ''non-run-aborting'' exception,
   * that exception will be caught and installed as the `unreportedException` on the
   * `Status` returned by this method. The `Status` returned by this method
   * will then complete. The thread that attempted to evaluate the by-name function will be allowed
   * to continue (`i.e.`, the non-run-aborting exception will ''not'' be rethrown
   * on that thread).
   * 
   *
   * If the by-name function passed to this method completes abruptly with a ''run-aborting'' exception,
   * such as `StackOverflowError`, that exception will be caught and a new
   * `java.util.concurrent.ExecutionException` that contains the run-aborting exception as its
   * cause will be installed as the `unreportedException` on the
   * `Status` returned by this method. The `Status` returned by this method
   * will then complete. The original run-aborting exception will then be rethrown on the
   * thread that attempted to evaluate the by-name function.
   * 
   *
   * If an unreported exception is installed on this `Status`, the passed by-name function will
   * ''not'' be executed. Instead, the same unreported exception will be installed on the `Status`
   * returned by this method.
   * 
   *
   * Internally, ScalaTest uses this method in async styles to ensure that by default, each subsequent test in an async-style
   * suite begins execution only after the previous test has completed. This method is ''not'' used if
   * `ParallelTestExection` is mixed into an async style. Instead, tests are allowed to begin
   * execution concurrently.
   * 
   *
   * @param status A `Status`-producing by-name function to invoke after this `Status` has completed.
   * @return a `Status ` that represents the status of executing the by-name function passed to this method.
   */
  final def thenRun(f: => Status): Status = {
    val returnedStatus = new ScalaTestStatefulStatus
    whenCompleted { _ =>
      try {
        val innerStatus = f
        innerStatus.whenCompleted { tri =>
          tri match {
            case Success(false) =>
              returnedStatus.setFailed()
            case Failure(ex) =>
              returnedStatus.setFailed()
              returnedStatus.setFailedWith(ex)
            case _ =>
          }
          returnedStatus.setCompleted()
        }
      }
      catch {
        case ex: Throwable =>
          if (Suite.anExceptionThatShouldCauseAnAbort(ex)) {
            returnedStatus.setFailedWith(new ExecutionException(ex))
            returnedStatus.setCompleted()
            throw ex
          }
          else {
            returnedStatus.setFailedWith(ex)
            returnedStatus.setCompleted()
          }
      }
    }
    returnedStatus
  }

  /**
   * Converts this `Status` to a `Future[Boolean]` where `Success(true)` means
   * no tests failed and suites aborted, `Success(false)`, means at least one test failed or one
   * suite aborted and any thrown exception was was reported to the `Reporter` via a ScalaTest
   * event, `Failure(unreportedException)` means
   * an exception, `unreportedException`, was thrown that was not reported to the `Reporter`
   * via a ScalaTest event.
   *
   * @return a `Future[Boolean]` representing this `Status`.
   */
  final def toFuture: Future[Boolean] = {
    val promise = Promise[Boolean]
    whenCompleted { t => promise.complete(t) }
    promise.future
  }

  // TODO: Make sure to test what happens when before and after code throw exceptions.
  /**
   * An exception that was thrown during the activity represented by this `Status` that
   * was not reported via a ScalaTest event fired to the `Reporter`.
   *
   * When a test executes, "non-run-aborting" thrown exceptions are reported by events
   * fired to the reporter. A <a href="exceptions/TestPendingException.html">`TestPendingException`</a> is reported via a
   * <a href="events/TestPending.html">`TestPending`</a> event. A <a href="exceptions/TestCanceledException.html">`TestCanceledException`</a> is reported via a
   * <a href="events/TestCanceled.html">`TestCanceled`</a> event. Any other non-run-aborting exceptions, including
   * <a href="exceptions/TestFailedException.html">`TestFailedException`</a> will be reported via a
   * <a href="events/TestFailed.html">`TestFailed`</a> event.
   * 
   *
   * Run-aborting exceptions indicate critical
   * problems, such as `OutOfMemoryError`, that instead of being reported via a test completion event
   * should instead cause the entire suite to abort. In synchronous testing styles, this exception will be allowed
   * to just propagate up the call stack. But in async styles, the thread or threads executing the test will often
   * be taken from the async suite's execution context. Instead of propagating these run-aborting exceptions up
   * the call stack, they will be installed as an "unreported exception" in the test's `Status`.
   * They are "unreported" because no test completion event will be fired to report them. For more explanation and
   * a list of run-aborting exception types, see <a href="Suite.html#errorHandling">Treatment of `java.lang.Error`s</a>.
   * 
   *
   * Another way for an unreported exception to occur is if an exception of any type is thrown outside of the
   * body of an actual test. For example, traits `BeforeAndAfter`,  `BeforeAndAfterEach`,
   * and `BeforeAndAfterEachTestData` execute code before and after tests. Traits
   * `BeforeAndAfterAll` and `BeforeAndAfterAllConfigMap` execute code before
   * and after all tests and nested suites of a suite. If any "before" or "after"
   * code completes abruptly with an exception (of any type, not just run-aborting types) on a thread taken
   * from an async suite's execution context, this exception will
   * installed as an `unreportedException` of the relevant `Status`.
   * 
   *
   * In addition, ScalaTest `Suite` exposes four "run" lifecycle methods--`run`,
   * `runNestedSuites`, `runTests`, and `runTest`--that users can override to customize
   * the framework. If a "run" lifecycle methods completes abruptly with an exception, that exception occurs outside
   * the context of a test body. As a result, such exceptions will be
   * installed as an `unreportedException` of the relevant `Status`.
   * 
   *
   * The `toFuture` method on `Status` returns a `Future[Boolean]`. If the `Future`
   * succeeds with the `Boolean` value of `true`, that indicates no tests failed and no suites aborted
   * during the activity represented
   * by this `Status`. If a test failed or suite aborted, and that event was reported by a fired ScalaTest
   * <a href="events.Event.html">`Event`</a>, the
   * `Future` will succeed with the value `false`. If an unreported exception has been installed
   * on the `Status`, however, the `Future` will fail with that exception.
   * 
   *
   * @return a optional unreported `Throwable`
   */
  def unreportedException: Option[Throwable] = None

  // TODO: Currently we are attempting to execution the after code. This is a change from how
  // ScalaTest has behaved from the beginning, and it is inconsistent also with thenRun. So 
  // I think I want to go back to how we were doing it before, if the before code blows up
  // or runTest, etc., then we don't attempt the after code.
  /**
   * Registers a by-name function (producing an optional exception) to execute
   * after this `Status` completes.
   *
   * If the by-name function passed to this method completes abruptly with a ''non-run-aborting'' exception,
   * that exception will be caught and installed as the `unreportedException` on the
   * `Status` returned by this method. The `Status` returned by this method
   * will then complete. The thread that attempted to evaluate the by-name function will be allowed
   * to continue (`i.e.`, the non-run-aborting exception will ''not'' be rethrown
   * on that thread).
   * 
   *
   * If the by-name function passed to this method completes abruptly with a ''run-aborting'' exception,
   * such as `StackOverflowError`, that exception will be caught and a new
   * `java.util.concurrent.ExecutionException` that contains the run-aborting exception as its
   * cause will be installed as the `unreportedException` on the
   * `Status` returned by this method. The `Status` returned by this method
   * will then complete. The original run-aborting exception will then be rethrown on the
   * thread that attempted to evaluate the by-name function.
   * 
   *
   * If an unreported exception is installed on this `Status`, the passed by-name function will
   * ''not'' be executed. Instead, the same unreported exception will be installed on the `Status`
   * returned by this method.
   * 
   *
   * Internally, ScalaTest uses this method in traits `BeforeAndAfter`,
   * `BeforeAndAfterEach`, and `BeforeAndAfterEachTestData` to ensure "after" code is
   * executed after the relevant test has completed, and in traits `BeforeAndAfterAll` and
   * `BeforeAndAfterAllConfigMap` to ensure "after" code is executed after the
   * relevant tests and nested suites have completed.
   * 
   *
   * @param f A by-name function to invoke after this `Status` has completed.
   * @return a `Status` that represents this `Status`,
   *         modified by any exception thrown by the passed by-name function.
   */
  final def withAfterEffect(f: => Unit): Status = {
    val returnedStatus = new ScalaTestStatefulStatus
    whenCompleted { tri =>
      tri match {
        case Success(result) =>
          try {
            f
            if (!result) returnedStatus.setFailed()
          }
          catch {
            case ex: Throwable if Suite.anExceptionThatShouldCauseAnAbort(ex) =>
              val execEx = new ExecutionException(ex)
              returnedStatus.setFailedWith(execEx)
              throw ex

            case ex: Throwable => returnedStatus.setFailedWith(ex)
          }
          finally {
            returnedStatus.setCompleted()
          }

        case Failure(originalEx) =>
          try {
            f
            returnedStatus.setFailedWith(originalEx)
          }
          catch {
            case ex: Throwable =>
              returnedStatus.setFailedWith(originalEx)
              println("ScalaTest can't report this exception because another preceded it, so printing its stack trace:")
              ex.printStackTrace()
          }
          finally {
            returnedStatus.setCompleted()
          }
      }
    }
    returnedStatus
  }
}

/**
 * Singleton status that represents an already completed run with no tests failed and no suites aborted.
 *
 * Note: the difference between this `SucceededStatus` object and the similarly named <a href="Succeeded$.html">`Succeeded`</a>
 * object is that the `Succeeded` object indicates one test succeeded, whereas this `SucceededStatus` object indicates the absence
 * of any failed tests or aborted suites during a run. Both are used as the result type of <a href="Suite.html#lifecycle-methods">`Suite`</a> lifecycle methods, but `Succeeded`
 * is a possible result of `withFixture`, whereas `SucceededStatus` is a possible result of `run`, `runNestedSuites`,
 * `runTests`, or `runTest`. In short, `Succeeded` is always just about one test, whereas `SucceededStatus` could be
 * about something larger: multiple tests or an entire suite.
 * 
 */
object SucceededStatus extends Status with Serializable {

  // SKIP-SCALATESTJS-START
  /**
   * Always returns `true`.
   * 
   * @return `true`
   */
  def succeeds() = true
  // SKIP-SCALATESTJS-END

  /**
   * Always returns `true`.
   * 
   * @return `true`
   */
  def isCompleted = true

  // SKIP-SCALATESTJS-START
  /**
   * Always returns immediately.
   */
  def waitUntilCompleted(): Unit = {}
  // SKIP-SCALATESTJS-END

  /**
   * Executes the passed function immediately on the calling thread.
   */
  def whenCompleted(f: Try[Boolean] => Unit): Unit = { f(Success(true)) }
}

/**
 * Singleton status that represents an already completed run with at least one failed test or aborted suite.
 *
 * Note: the difference between this `FailedStatus` object and the similarly named <a href="Failed.html">`Failed`</a>
 * class is that a `Failed` instance indicates one test failed, whereas this `FailedStatus` object indicates either one or more tests failed
 * and/or one or more suites aborted during a run. Both are used as the result type of `Suite` lifecycle methods, but `Failed`
 * is a possible result of `withFixture`, whereas `FailedStatus` is a possible result of `run`, `runNestedSuites`,
 * `runTests`, or `runTest`. In short, `Failed` is always just about one test, whereas `FailedStatus` could be
 * about something larger: multiple tests or an entire suite.
 * 
 */
object FailedStatus extends Status with Serializable {

  // SKIP-SCALATESTJS-START
  /**
   * Always returns `false`.
   * 
   * @return `true`
   */
  def succeeds() = false
  // SKIP-SCALATESTJS-END

  /**
   * Always returns `true`.
   * 
   * @return `true`
   */
  def isCompleted = true

  // SKIP-SCALATESTJS-START
  /**
   * Always returns immediately.
   */
  def waitUntilCompleted(): Unit = {}
  // SKIP-SCALATESTJS-END

  /**
   * Executes the passed function immediately on the calling thread.
   */
  def whenCompleted(f: Try[Boolean] => Unit): Unit = { f(Success(false)) }
}

// Used internally in ScalaTest. We don't use the StatefulStatus, because
// then user code could pattern match on it and then access the setCompleted
// and setFailed methods. We wouldn't want that.
private[scalatest] final class ScalaTestStatefulStatus extends Status with Serializable {

  @transient private final val latch = new CountDownLatch(1)

  private var succeeded = true

  private final val queue = new ConcurrentLinkedQueue[Try[Boolean] => Unit]

  private var asyncException: Option[Throwable] = None

  override def unreportedException: Option[Throwable] = {
    synchronized {
      asyncException
    }
  }

  // SKIP-SCALATESTJS-START
  def succeeds() = {
    waitUntilCompleted()
    synchronized { succeeded }
  }
  // SKIP-SCALATESTJS-END

  def isCompleted = synchronized { latch.getCount == 0L }

  // SKIP-SCALATESTJS-START
  def waitUntilCompleted(): Unit = {
    synchronized { latch }.await()
    unreportedException match {
      case Some(ue) => throw ue
      case None => // Do nothing
    }
  }
  // SKIP-SCALATESTJS-END

  def setFailed(): Unit = {
    synchronized {
      if (isCompleted)
        throw new IllegalStateException("status is already completed")
      succeeded = false
    }
  }

  /**
   * Sets the status to failed with an unreported exception, without changing the completion status.
   *
   * This method may be invoked repeatedly, even though invoking it once is sufficient to set the state of the `Status` to failed, but only
   * up until `setCompleted` has been called. Once `setCompleted` has been called, invoking this method will result in a
   * thrown `IllegalStateException`. Also, only the first exception passed will be reported as the unreported exception. Any exceptions
   * passed via subsequent invocations of `setFailedWith` after the first will have their stack traces printed to standard output.
   * 
   *
   * @throws IllegalStateException if this method is invoked on this instance after `setCompleted` has been invoked on this instance.
   * @param ex an unreported exception
   */
  def setFailedWith(ex: Throwable): Unit = {
    synchronized {
      if (isCompleted)
        throw new IllegalStateException("status is already completed")
      succeeded = false
      if (asyncException.isEmpty)
        asyncException = Some(ex)
      else {
        println("ScalaTest can't report this exception because another preceded it, so printing its stack trace:")
        ex.printStackTrace()
      }
    }
  }

  def setCompleted(): Unit = {
    // Moved the for loop after the countdown, to avoid what I think is a race condition whereby we register a call back while
    // we are iterating through the list of callbacks prior to adding the last one.
    val it =
      synchronized {
        // OLD, OUTDATED COMMENT, left in here to ponder the depths of its meaning a bit longer:
        // Only release the latch after the callbacks finish execution, to avoid race condition with other thread(s) that wait
        // for this Status to complete.
        latch.countDown()
        queue.iterator
      }
    val tri: Try[Boolean] =
      unreportedException match {
        case Some(ex) => Failure(ex)
        case None => Success(succeeded)
      }
    for (f <- it)
      f(tri)
  }

  def whenCompleted(f: Try[Boolean] => Unit): Unit = {
    var executeLocally = false
    synchronized {
      if (!isCompleted)
        queue.add(f)
      else
        executeLocally = true
    }
    if (executeLocally) {
      val tri: Try[Boolean] =
        unreportedException match {
          case Some(ex) => Failure(ex)
          case None => Success(succeeded)
        }
      f(tri)
    }
  }
}

/**
 * Status implementation that can change its state over time.
 *
 * A `StatefulStatus` begins its life in a successful state, and will remain successful unless `setFailed` is called.
 * Once `setFailed` is called, the status will remain at failed. The `setFailed` method can be called multiple times (even
 * though invoking it once is sufficient to permanently set the status to failed), but only up until `setCompleted` has been called.
 * After `setCompleted` has been called, any invocation of `setFailed` will be greeted with an `IllegalStateException`.
 * 
 *
 * Instances of this class are thread safe.
 * 
 */
final class StatefulStatus extends Status with Serializable {
  @transient private final val latch = new CountDownLatch(1)
  private var succeeded = true
  private final val queue = new ConcurrentLinkedQueue[Try[Boolean] => Unit]

  private var asyncException: Option[Throwable] = None

  override def unreportedException: Option[Throwable] = {
    synchronized {
      asyncException
    }
  }

  // SKIP-SCALATESTJS-START
  /**
   * Blocking call that waits until completion, as indicated by an invocation of `setCompleted` on this instance, then returns `false` 
   * if `setFailed` was called on this instance, else returns `true`.
   * 
   * @return `true` if no tests failed and no suites aborted, `false` otherwise
   */
  def succeeds() = {
    waitUntilCompleted()
    synchronized { succeeded }
  }
  // SKIP-SCALATESTJS-END

  /**
   * Non-blocking call that returns `true` if `setCompleted` has been invoked on this instance, `false` otherwise.
   * 
   * @return `true` if the test or suite run is already completed, `false` otherwise.
   */
  def isCompleted = synchronized { latch.getCount == 0L }

  // SKIP-SCALATESTJS-START
  /**
   * Blocking call that returns only after `setCompleted` has been invoked on this `StatefulStatus` instance.
   */
  def waitUntilCompleted(): Unit = {
    synchronized { latch }.await()
    unreportedException match {
      case Some(ue) => throw ue
      case None => // Do nothing
    }
  }
  // SKIP-SCALATESTJS-END

  /**
   * Sets the status to failed without changing the completion status.
   *
   * This method may be invoked repeatedly, even though invoking it once is sufficient to set the state of the `Status` to failed, but only
   * up until `setCompleted` has been called. Once `setCompleted` has been called, invoking this method will result in a
   * thrown `IllegalStateException`.
   * 
   *
   * @throws IllegalStateException if this method is invoked on this instance after `setCompleted` has been invoked on this instance.
   */
  def setFailed(): Unit = {
    synchronized {
      if (isCompleted)
        throw new IllegalStateException("status is already completed")
      succeeded = false
    }
  }

  /**
   * Sets the status to failed with an unreported exception, without changing the completion status.
   *
   * This method may be invoked repeatedly, even though invoking it once is sufficient to set the state of the `Status` to failed, but only
   * up until `setCompleted` has been called. Once `setCompleted` has been called, invoking this method will result in a
   * thrown `IllegalStateException`. Also, only the first exception passed will be reported as the unreported exception. Any exceptions
   * passed via subsequent invocations of `setFailedWith` after the first will have their stack traces printed to standard output.
   * 
   *
   * @throws IllegalStateException if this method is invoked on this instance after `setCompleted` has been invoked on this instance.
   * @param ex an unreported exception
   */
  def setFailedWith(ex: Throwable): Unit = {
    synchronized {
      if (isCompleted)
        throw new IllegalStateException("status is already completed")
      succeeded = false
      if (asyncException.isEmpty)
        asyncException = Some(ex)
      else {
        println("ScalaTest can't report this exception because another preceded it, so printing its stack trace:")
        ex.printStackTrace()
      }
    }
  }

  /**
   * Sets the status to completed.
   *
   * This method may be invoked repeatedly, even though invoking it once is sufficient to set the state of the `Status` to completed.
   * 
   *
   * '''TODO: Specify that this method invokes the callbacks on the invoking thread after it releases the lock
   * such that the Status has completed.'''
   * 
   */
  def setCompleted(): Unit = {
    // Moved the for loop after the countdown, to avoid what I think is a race condition whereby we register a call back while
    // we are iterating through the list of callbacks prior to adding the last one.
    val it =
      synchronized {
      // OLD, OUTDATED COMMENT, left in here to ponder the depths of its meaning a bit longer:
      // Only release the latch after the callbacks finish execution, to avoid race condition with other thread(s) that wait
      // for this Status to complete.
        latch.countDown()
        queue.iterator
      }
    val tri: Try[Boolean] =
      unreportedException match {
        case Some(ex) => Failure(ex)
        case None => Success(succeeded)
      }
    for (f <- it)
      f(tri)
  }

  /**
   * Registers the passed function to be executed when this status completes.
   *
   * You may register multiple functions, which on completion will be executed in an undefined
   * order.
   * 
   */
  def whenCompleted(f: Try[Boolean] => Unit): Unit = {
    var executeLocally = false
    synchronized {
      if (!isCompleted)
        queue.add(f)
      else
        executeLocally = true
    }
    if (executeLocally) {
      val tri: Try[Boolean] =
        unreportedException match {
          case Some(ex) => Failure(ex)
          case None => Success(succeeded)
        }
      f(tri)
    }
  }
}

/**
 * Composite `Status` that aggregates its completion and failed states of set of other `Status`es passed to its constructor.
 *
 * @param status the `Status`es out of which this status is composed.
 */
final class CompositeStatus(statuses: Set[Status]) extends Status with Serializable {
  
  // TODO: Ensure this is visible to another thread, because I'm letting the reference
  // escape with my for loop below prior to finishing this object's construction.
  @transient private final val latch = new CountDownLatch(statuses.size)

  @volatile private var succeeded = true
  // This is set possibly by the whenCompleted function registered on all the
  // inner statuses. If any of them are Failures, then that first one goes in
  // as this Composite's unreported exception. Any subsequent ones are just printed.
  // Then if it is the last inner status to complete, that unreported exception is passed
  // to the callback functions registered with this composite status.
  private var asyncException: Option[Throwable] = None

  private final val queue = new ConcurrentLinkedQueue[Try[Boolean] => Unit]

  for (status <- statuses) {
    status.whenCompleted { tri =>
      val youCompleteMe: Boolean =
        synchronized {
          latch.countDown()

          tri match {
            case Success(res) =>
              if (!res)
                succeeded = false
            case Failure(ex) =>
              succeeded = false
              if (asyncException.isEmpty)
                asyncException = Some(ex)
              else {
                println("ScalaTest can't report this exception because another preceded it, so printing its stack trace:") 
                ex.printStackTrace()
              }
          }

          latch.getCount == 0
        }
      if (youCompleteMe) {
        val tri: Try[Boolean] =
          unreportedException match {
            case Some(ex) => Failure(ex)
            case None => Success(succeeded)
          }
        for (f <- queue.iterator)
          f(tri)
      }
    }
  }

  // SKIP-SCALATESTJS-START
  /**
   * Blocking call that waits until all composite `Status`es have completed, then returns
   * `true` only if all of the composite `Status`es succeeded. If any `Status` passed in the `statuses` set fails, this method
   * will return `false`.
   * 
   * @return `true` if all composite `Status`es succeed, `false` otherwise.
   */
  def succeeds() = {
    synchronized { latch }.await()
    synchronized { statuses }.forall(_.succeeds())
  }
  // SKIP-SCALATESTJS-END

  /**
   * Non-blocking call to check if the test or suite run is completed, returns `true` if all composite `Status`es have completed, 
   * `false` otherwise.  You can use this to poll the run status.
   * 
   * @return `true` if all composite `Status`es have completed, `false` otherwise.
   */
  def isCompleted = synchronized { statuses }.forall(_.isCompleted)

  // SKIP-SCALATESTJS-START
  /**
   * Blocking call that returns only after all composite `Status`s have completed.
   */
  def waitUntilCompleted(): Unit = {
    // statuses.foreach(_.waitUntilCompleted())
    synchronized { latch }.await()
  }
  // SKIP-SCALATESTJS-END

  /**
   * Registers the passed function to be executed when this status completes.
   *
   * You may register multiple functions, which on completion will be executed in an undefined
   * order.
   * 
   */
  def whenCompleted(f: Try[Boolean] => Unit): Unit = {
    var executeLocally = false
    synchronized {
      if (!isCompleted)
        queue.add(f)
      else
        executeLocally = true
    }
    if (executeLocally) {
      val tri: Try[Boolean] =
        unreportedException match {
          case Some(ex) => Failure(ex)
          case None => Success(succeeded)
        }
      f(tri)
    }
  }

  /**
   * An optional exception that has not been reported to the reporter for this run.
   *
   * This will be defined if any of the composite `Status`s (passed to this `Status`'s 
   * constructor) has a defined `unreportedException`. If more than one composite `Status`
   * has a defined `unreportedException`, one of them (not specified) will be reported by this method
   * and the others will have their stack traces printed to standard output.
   * 
   */
  override def unreportedException: Option[Throwable] = {
    synchronized {
      if (asyncException.isDefined) asyncException
      else {
        val optStatusWithUnrepEx = statuses.find(_.unreportedException.isDefined)
        for {
          status <- optStatusWithUnrepEx
          unrepEx <- status.unreportedException 
        } yield unrepEx
      }
    }
  }
}

