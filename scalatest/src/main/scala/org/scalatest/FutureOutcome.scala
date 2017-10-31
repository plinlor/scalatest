/*
 * Copyright 2001-2016 Artima, Inc.
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

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.scalactic.{Or, Good, Bad}
import scala.util.{Try, Success, Failure}
import exceptions.TestCanceledException
import exceptions.TestPendingException
import Suite.anExceptionThatShouldCauseAnAbort
import scala.concurrent.ExecutionException

/*
Note, the reason Outcome or Throwable is used here instead of Try[Outcome] is
to avoid confusion over the Try that comes back from the Future[Outcome]. Only
run-aborting exceptions will be contained in scala.util.Failures in this case.
Other exceptions will show up as Success(org.scalatest.Failed) or Success(org.scalatest.Canceled).
And this confusion of Success(Failed) is what the Or is intended to alleviate.
*/

/**
 * Wrapper class for `Future[Outcome]` that presents a more convenient API for 
 * manipulation in `withFixture` methods in async styles.
 *
 * This type serves as the result type of both test functions and `withFixture` methods
 * in ScalaTest's async styles. A `Future[Outcome]` is not used as this result type
 * for two reasons. First, `Outcome` treats exceptions specially, and as a result
 * methods on `Future` would usually not yield the desired `Future[Outcome]` result.
 * Only run-aborting exceptions should result in a failed `Future[Outcome]`. Any other thrown exception
 * other than `TestCanceledException` or `TestPendingException`
 * should result in a successful`Future` containing a `org.scalatest.Failed`.
 * A thrown `TestCanceledException` should result in a successful `Future`
 * containing an `org.scalatest.Canceled`; A thrown `TestPendingException` should result in
 * a successful `Future` containing a `org.scalatest.Pending`.
 * If manipulating a `Future[Outcome]` directly, by contrast, any thrown exception would result in 
 * a failed `Future`.
 * 
 *
 * Additionally, to be consistent with corresponding transformations in traditional testing styles, 
 * methods registering callbacks should return a new future outcome that doesn't complete until
 * both the original future outcome has completed and the subsequent callback has completed execution.
 * Additionally, if the callback itself throws an exception, that exception should determine the result
 * of the future outcome returned by the callback registration method. This behavior is rather inconvenient
 * to obtain on the current `Future` API, so `FutureOutcome` provides well-named
 * methods that have this behavior.
 * 
 *
 * Lastly, the `FutureOutcome` is intended to help prevent confusion by eliminating the need
 * to work with types like `scala.util.Success(org.scalatest.Failed)`. For this purpose a
 * `org.scalactic.Or` is used instead of a `scala.util.Try` to describe results
 * of `FutureOutcome`.
 * 
 *
 * A `FutureOutcome` represents a computation that can result in an `Outcome` or an "abort." An abort means
 * that a run-aborting exception occurred during the computation. Any other, non-run-aborting exception will be represented
 * as an non-`Succeeded` `Outcome`: one of `Failed`, `Canceled`, or `Pending`.
 * 
 * 
 * The methods of `FutureOutcome` include the following callback registration methods:
 * 
 *
 * <ul>
 * <li>`onSucceededThen` - registers a callback to be executed if the future outcome is `Succeeded`.</li>
 * <li>`onFailedThen` - registers a callback to be executed if the future outcome is `Failed`.</li>
 * <li>`onCanceledThen` - registers a callback to be executed if the future outcome is `Canceled`.</li>
 * <li>`onPendingThen` - registers a callback to be executed if the future outcome is `Pending`.</li>
 * <li>`onOutcomeThen` - registers a callback to be executed if the future outcome is actually an `Outcome`
 *      and not an abort.</li>
 * <li>`onAbortedThen` - registers a callback to be executed if the future outcome aborts.</li>
 * <li>`onCompletedThen` - registers a callback to be executed upon completion no matter how the future outcome completes.</li>
 * </ul>
 *
 * The callback methods listed previously can be used to perform a side effect once a `FutureOutcome` completes. To change an
 * `Outcome` into a different `Outcome` asynchronously, use the `change` registration method, which takes a function
 * from `Outcome` to `Outcome`. The other methods on `FutureOutcome`, `isCompleted` and
 * `value`, allow you to poll a `FutureOutcome`. None of the methods on `FutureOutcome` block.
 * Lastly, because an implicit <a href="enablers/Futuristic.html">`Futuristic`</a> instance is provided for
 * `FutureOutcome`, you can use <a href="CompleteLastly.html">`complete`-`lastly` syntax</a>
 * with `FutureOutcome`.
 * 
 */
class FutureOutcome(private[scalatest] val underlying: Future[Outcome]) {
  // TODO: add tests for pretty toString

  /**
   * Registers a callback function to be executed after this future completes, returning
   * a new future that completes only after the callback has finished execution.
   *
   * The resulting `FutureOutcome` will have the same result as this `FutureOutcome`, unless
   * the callback completes abruptly with an exception. In that case, the resulting `FutureOutcome`
   * will be determined by the type of the thrown exception:
   * 
   *
   * <ul>
   * <li>`TestPendingException`</li> - `Good(Pending)`
   * <li>`TestCanceledException`</li> - `Good(Canceled(&lt;the exception&gt;))`
   * <li>Any non-run-aborting `Throwable`</li> - `Good(Failed(&lt;the exception&gt;))`
   * <li>A run-aborting `Throwable`</li> - `Bad(&lt;the run-aborting exception&gt;)`
   * </ul>
   *
   * For more information on ''run-aborting'' exceptions, see the <a href="Suite.html#errorHandling">Run-aborting exceptions</a> section
   * in the main Scaladoc for trait `Suite`.
   * 
   *
   * @param callback a side-effecting function to execute when this `FutureOutcome` completes
   * @param executionContext an execution context that provides a strategy for executing the callback function
   * @return a new `FutureOutcome` that will complete only after this `FutureOutcome`
   *    and, subsequently, the passed callback function have completed execution.
   */
  def onCompletedThen(callback: (Outcome Or Throwable) => Unit)(implicit executionContext: ExecutionContext): FutureOutcome = {
    FutureOutcome {
      underlying recoverWith {
        case ex =>
          try {
            callback(Bad(ex))
            Future.failed(ex)
          }
          catch {
            case _: TestPendingException => Future.successful(Pending)
            case ex: TestCanceledException => Future.successful(Canceled(ex))
            case ex: Throwable if !anExceptionThatShouldCauseAnAbort(ex) => Future.successful(Failed(ex))
            case ex: Throwable => Future.failed(new ExecutionException(ex))
          }
      } flatMap { outcome =>
        try {
          callback(Good(outcome))
          Future.successful(outcome)
        }
        catch {
          case _: TestPendingException => Future.successful(Pending)
          case ex: TestCanceledException => Future.successful(Canceled(ex))
          case ex: Throwable if !anExceptionThatShouldCauseAnAbort(ex) => Future.successful(Failed(ex))
          case ex: Throwable => Future.failed(new ExecutionException(ex))
        }
      }
    }
  }

  /**
   * Registers a callback function to be executed if this future completes with
   * `Succeeded`, returning a new future that completes only after the
   * callback has finished execution.
   *
   * The resulting `FutureOutcome` will have the same result as this `FutureOutcome`, unless
   * the callback completes abruptly with an exception. In that case, the resulting `FutureOutcome`
   * will be determined by the type of the thrown exception:
   * 
   *
   * <ul>
   * <li>`TestPendingException`</li> - `Good(Pending)`
   * <li>`TestCanceledException`</li> - `Good(Canceled(&lt;the exception&gt;))`
   * <li>Any non-run-aborting `Throwable`</li> - `Good(Failed(&lt;the exception&gt;))`
   * <li>A run-aborting `Throwable`</li> - `Bad(&lt;the run-aborting exception&gt;)`
   * </ul>
   *
   * For more information on ''run-aborting'' exceptions, see the <a href="Suite.html#errorHandling">Run-aborting exceptions</a> section
   * in the main Scaladoc for trait `Suite`.
   * 
   *
   * @param callback a side-effecting function to execute if and when this `FutureOutcome` completes with `Succeeded`
   * @param executionContext an execution context that provides a strategy for executing the callback function
   * @return a new `FutureOutcome` that will complete only after this `FutureOutcome`
   *    has completed and, if this `FutureOutcome` completes with `Succeeded`, the
   *    passed callback function has completed execution.
   */
  def onSucceededThen(callback: => Unit)(implicit executionContext: ExecutionContext): FutureOutcome = {
    FutureOutcome {
      underlying flatMap { outcome =>
        if (outcome.isSucceeded) {
          try {
            callback
            Future.successful(outcome)
          }
          catch {
            case _: TestPendingException => Future.successful(Pending)
            case ex: TestCanceledException => Future.successful(Canceled(ex))
            case ex: Throwable if !anExceptionThatShouldCauseAnAbort(ex) => Future.successful(Failed(ex))
            case ex: Throwable => Future.failed(new ExecutionException(ex))
          }
        } else Future.successful(outcome)
      }
    }
  }

  /**
   * Registers a callback function to be executed if this future completes with
   * `Failed`, returning a new future that completes only after the
   * callback has finished execution.
   *
   * The resulting `FutureOutcome` will have the same result as this `FutureOutcome`, unless
   * the callback completes abruptly with an exception. In that case, the resulting `FutureOutcome`
   * will be determined by the type of the thrown exception:
   * 
   *
   * <ul>
   * <li>`TestPendingException`</li> - `Good(Pending)`
   * <li>`TestCanceledException`</li> - `Good(Canceled(&lt;the exception&gt;))`
   * <li>Any non-run-aborting `Throwable`</li> - `Good(Failed(&lt;the exception&gt;))`
   * <li>A run-aborting `Throwable`</li> - `Bad(&lt;the run-aborting exception&gt;)`
   * </ul>
   *
   * For more information on ''run-aborting'' exceptions, see the <a href="Suite.html#errorHandling">Run-aborting exceptions</a> section
   * in the main Scaladoc for trait `Suite`.
   * 
   *
   * @param callback a side-effecting function to execute if and when this `FutureOutcome` completes with `Failed`
   * @param executionContext an execution context that provides a strategy for executing the callback function
   * @return a new `FutureOutcome` that will complete only after this `FutureOutcome`
   *    has completed and, if this `FutureOutcome` completes with `Failed`, the
   *    passed callback function has completed execution.
   */
  def onFailedThen(callback: Throwable => Unit)(implicit executionContext: ExecutionContext): FutureOutcome = {
    FutureOutcome {
      underlying flatMap { outcome =>
        outcome match {
          case Failed(originalEx) =>
            try {
              callback(originalEx)
              Future.successful(outcome)
            }
            catch {
              case _: TestPendingException => Future.successful(Pending)
              case ex: TestCanceledException => Future.successful(Canceled(ex))
              case ex: Throwable if !anExceptionThatShouldCauseAnAbort(ex) => Future.successful(Failed(ex))
              case ex: Throwable => Future.failed(new ExecutionException(ex))
            }
          case _ =>
            Future.successful(outcome)
        }
      }
    }
  }

  /**
   * Registers a callback function to be executed if this future completes with
   * `Canceled`, returning a new future that completes only after the
   * callback has finished execution.
   *
   * The resulting `FutureOutcome` will have the same result as this `FutureOutcome`, unless
   * the callback completes abruptly with an exception. In that case, the resulting `FutureOutcome`
   * will be determined by the type of the thrown exception:
   * 
   *
   * <ul>
   * <li>`TestPendingException`</li> - `Good(Pending)`
   * <li>`TestCanceledException`</li> - `Good(Canceled(&lt;the exception&gt;))`
   * <li>Any non-run-aborting `Throwable`</li> - `Good(Failed(&lt;the exception&gt;))`
   * <li>A run-aborting `Throwable`</li> - `Bad(&lt;the run-aborting exception&gt;)`
   * </ul>
   *
   * For more information on ''run-aborting'' exceptions, see the <a href="Suite.html#errorHandling">Run-aborting exceptions</a> section
   * in the main Scaladoc for trait `Suite`.
   * 
   *
   * @param callback a side-effecting function to execute if and when this `FutureOutcome` completes with `Canceled`
   * @param executionContext an execution context that provides a strategy for executing the callback function
   * @return a new `FutureOutcome` that will complete only after this `FutureOutcome`
   *    has completed and, if this `FutureOutcome` completes with `Canceled`, the
   *    passed callback function has completed execution.
   */
  def onCanceledThen(callback: TestCanceledException => Unit)(implicit executionContext: ExecutionContext): FutureOutcome = {
    FutureOutcome {
      underlying flatMap { outcome =>
        outcome match {
          case Canceled(originalEx) =>
            try {
              callback(originalEx)
              Future.successful(outcome)
            }
            catch {
              case _: TestPendingException => Future.successful(Pending)
              case ex: TestCanceledException => Future.successful(Canceled(ex))
              case ex: Throwable if !anExceptionThatShouldCauseAnAbort(ex) => Future.successful(Failed(ex))
              case ex: Throwable => Future.failed(new ExecutionException(ex))
            }
          case _ =>
            Future.successful(outcome)
        }
      }
    }
  }

  /**
   * Registers a callback function to be executed if this future completes with
   * `Pending`, returning a new future that completes only after the
   * callback has finished execution.
   *
   * The resulting `FutureOutcome` will have the same result as this `FutureOutcome`, unless
   * the callback completes abruptly with an exception. In that case, the resulting `FutureOutcome`
   * will be determined by the type of the thrown exception:
   * 
   *
   * <ul>
   * <li>`TestPendingException`</li> - `Good(Pending)`
   * <li>`TestCanceledException`</li> - `Good(Canceled(&lt;the exception&gt;))`
   * <li>Any non-run-aborting `Throwable`</li> - `Good(Failed(&lt;the exception&gt;))`
   * <li>A run-aborting `Throwable`</li> - `Bad(&lt;the run-aborting exception&gt;)`
   * </ul>
   *
   * For more information on ''run-aborting'' exceptions, see the <a href="Suite.html#errorHandling">Run-aborting exceptions</a> section
   * in the main Scaladoc for trait `Suite`.
   * 
   *
   * @param callback a side-effecting function to execute if and when this `FutureOutcome` completes with `Pending`
   * @param executionContext an execution context that provides a strategy for executing the callback function
   * @return a new `FutureOutcome` that will complete only after this `FutureOutcome`
   *    has completed and, if this `FutureOutcome` completes with `Pending`, the
   *    passed callback function has completed execution.
   */
  def onPendingThen(callback: => Unit)(implicit executionContext: ExecutionContext): FutureOutcome = {
    FutureOutcome {
      underlying flatMap { outcome =>
        if (outcome.isPending) {
          try {
            callback
            Future.successful(outcome)
          }
          catch {
            case _: TestPendingException => Future.successful(Pending)
            case ex: TestCanceledException => Future.successful(Canceled(ex))
            case ex: Throwable if !anExceptionThatShouldCauseAnAbort(ex) => Future.successful(Failed(ex))
            case ex: Throwable => Future.failed(new ExecutionException(ex))
          }
        } else Future.successful(outcome)
      }
    }
  }

  /**
   * Registers a transformation function to be executed if this future completes with any
   * `Outcome` (''i.e.'', no run-aborting exception is thrown), returning
   * a new `FutureOutcome` representing the result of passing
   * this `FutureOutcome`'s `Outcome` result to the given transformation function.
   *
   * If the passed function completes abruptly with an exception, the resulting `FutureOutcome`
   * will be determined by the type of the thrown exception:
   * 
   *
   * <ul>
   * <li>`TestPendingException`</li> - `Good(Pending)`
   * <li>`TestCanceledException`</li> - `Good(Canceled(&lt;the exception&gt;))`
   * <li>Any non-run-aborting `Throwable`</li> - `Good(Failed(&lt;the exception&gt;))`
   * <li>A run-aborting `Throwable`</li> - `Bad(&lt;the run-aborting exception&gt;)`
   * </ul>
   *
   * For more information on ''run-aborting'' exceptions, see the <a href="Suite.html#errorHandling">Run-aborting exceptions</a> section
   * in the main Scaladoc for trait `Suite`.
   * 
   *
   * @param f a transformation function to execute if and when this `FutureOutcome` completes with an `Outcome`
   * @param executionContext an execution context that provides a strategy for executing the transformation function
   * @return a new `FutureOutcome` that will complete only after this `FutureOutcome`
   *    has completed and, if this `FutureOutcome` completes with a valid
   *    `Outcome`, the passed callback function has completed execution.
   */
  def change(f: Outcome => Outcome)(implicit executionContext: ExecutionContext): FutureOutcome = {
    FutureOutcome {
      underlying flatMap { outcome =>
        try Future.successful(f(outcome))
        catch {
          case _: TestPendingException => Future.successful(Pending)
          case ex: TestCanceledException => Future.successful(Canceled(ex))
          case ex: Throwable if !anExceptionThatShouldCauseAnAbort(ex) => Future.successful(Failed(ex))
          case ex: Throwable => Future.failed(new ExecutionException(ex))
        }
      }
    }
  }

  /**
   * Registers a callback function to be executed if this future completes because
   * a run-aborting exception was thrown, returning a new future that completes only after the
   * callback has finished execution.
   *
   * The resulting `FutureOutcome` will have the same result as this `FutureOutcome`, unless
   * the callback completes abruptly with an exception. In that case, the resulting `FutureOutcome`
   * will be determined by the type of the thrown exception:
   * 
   *
   * <ul>
   * <li>`TestPendingException`</li> - `Good(Pending)`
   * <li>`TestCanceledException`</li> - `Good(Canceled(&lt;the exception&gt;))`
   * <li>Any non-run-aborting `Throwable`</li> - `Good(Failed(&lt;the exception&gt;))`
   * <li>A run-aborting `Throwable`</li> - `Bad(&lt;the run-aborting exception&gt;)`
   * </ul>
   *
   * For more information on ''run-aborting'' exceptions, see the <a href="Suite.html#errorHandling">Run-aborting exceptions</a> section
   * in the main Scaladoc for trait `Suite`.
   * 
   *
   * @param callback a side-effecting function to execute if and when this `FutureOutcome` completes with an abort.
   * @param executionContext an execution context that provides a strategy for executing the callback function
   * @return a new `FutureOutcome` that will complete only after this `FutureOutcome`
   *    has completed and, if this `FutureOutcome` completes abnormally with
   *    a run-aborting exception, the passed callback function has completed execution.
   */
  def onAbortedThen(callback: Throwable => Unit)(implicit executionContext: ExecutionContext): FutureOutcome = {
    FutureOutcome {
      underlying recoverWith {
        case originalEx =>
          try {
            callback(originalEx)
            Future.failed(originalEx)
          }
          catch {
            case _: TestPendingException => Future.successful(Pending)
            case ex: TestCanceledException => Future.successful(Canceled(ex))
            case ex: Throwable if !anExceptionThatShouldCauseAnAbort(ex) => Future.successful(Failed(ex))
            case ex: Throwable => Future.failed(new ExecutionException(ex))
          }
      }
    }
  }

  /**
   * Registers a callback function to be executed if this future completes with any
   * `Outcome` (''i.e.'', no run-aborting exception is thrown), returning
   * a new future that completes only after the callback has finished execution.
   *
   * The resulting `FutureOutcome` will have the same result as this `FutureOutcome`, unless
   * the callback completes abruptly with an exception. In that case, the resulting `FutureOutcome`
   * will be determined by the type of the thrown exception:
   * 
   *
   * <ul>
   * <li>`TestPendingException`</li> - `Good(Pending)`
   * <li>`TestCanceledException`</li> - `Good(Canceled(&lt;the exception&gt;))`
   * <li>Any non-run-aborting `Throwable`</li> - `Good(Failed(&lt;the exception&gt;))`
   * <li>A run-aborting `Throwable`</li> - `Bad(&lt;the run-aborting exception&gt;)`
   * </ul>
   *
   * For more information on ''run-aborting'' exceptions, see the <a href="Suite.html#errorHandling">Run-aborting exceptions</a> section
   * in the main Scaladoc for trait `Suite`.
   * 
   *
   * @param callback a side-effecting function to execute if and when this `FutureOutcome` completes with an `Outcome`
   *    (''i.e.'', not an abort)
   * @param executionContext an execution context that provides a strategy for executing the callback function
   * @return a new `FutureOutcome` that will complete only after this `FutureOutcome`
   *    has completed and, if this `FutureOutcome` completes with a valid
   *    `Outcome`, the passed callback function has completed execution.
   */
  def onOutcomeThen(callback: Outcome => Unit)(implicit executionContext: ExecutionContext): FutureOutcome = {
    FutureOutcome {
      underlying flatMap { outcome =>
        try {
          callback(outcome)
          Future.successful(outcome)
        }
        catch {
          case _: TestPendingException => Future.successful(Pending)
          case ex: TestCanceledException => Future.successful(Canceled(ex))
          case ex: Throwable if !anExceptionThatShouldCauseAnAbort(ex) => Future.successful(Failed(ex))
          case ex: Throwable => Future.failed(new ExecutionException(ex))
        }
      }
    }
  }

  /**
   * Indicates whether this `FutureOutcome` has completed.
   *
   * This method does not block.
   * 
   *
   * @return `true` if this `FutureOutcome` has completed; `false` otherwise.
   */
  def isCompleted: Boolean = underlying.isCompleted

  /**
   * Returns a value that indicates whether this `FutureOutcome` has completed,
   * and if so, indicates its result.
   *
   * If this `FutureOutcome` has not yet completed, this method will return
   * `None`. Otherwise, this method will return a `Some` that contains
   * either a `Good[Outcome]`, if this `FutureOutcome` completed with
   * a valid `Outcome` result, or if it completed with a thrown run-aborting
   * exception, a `Bad[Throwable]`.
   * 
   *
   * For more information on ''run-aborting'' exceptions, see the <a href="Suite.html#errorHandling">Run-aborting exceptions</a> section
   * in the main Scaladoc for trait `Suite`.
   * 
   *
   * @return a `Some` containing an `Or` value that indicates the result of this
   *    `FutureOutcome` if it has completed; `None` otherwise.
   */
  def value: Option[Outcome Or Throwable] =
    underlying.value match {
      case None => None
      case Some(Success(outcome)) => Some(Good(outcome))
      case Some(Failure(ex)) => Some(Bad(ex))
    }

  /**
   * Converts this `FutureOutcome` to a `Future[Outcome]`.
   *
   * @return the underlying `Future[Outcome]`
   */
  def toFuture: Future[Outcome] = underlying
}

/**
 * Companion object to `FutureOutcomes` that contains factory methods for creating already-completed
 * `FutureOutcomes`.
 */
object FutureOutcome {
  // Make this private so only ScalaTest can make one, so we can "promise" that
  // you'll never need to look for things like a TestCanceledException being passed
  // to onAbortedThen.
  private[scalatest] def apply(underlying: Future[Outcome]): FutureOutcome = new FutureOutcome(underlying)

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Canceled` result.
   */
  def canceled(): FutureOutcome =
    FutureOutcome { Future.successful(Canceled()) }

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Canceled` result
   * whose `TestCanceledException` contains the specified message.
   *
   * @message the message string to include in the `Canceled`'s `TestCanceledException`.
   */
  def canceled(message: String): FutureOutcome =
    FutureOutcome { Future.successful(Canceled(message)) }

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Canceled` result
   * whose `TestCanceledException` contains the specified cause.
   *
   * @cause exception to include as the `Canceled`'s `TestCanceledException` cause.
   */
  def canceled(cause: Throwable): FutureOutcome =
    FutureOutcome { Future.successful(Canceled(cause)) }

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Canceled` result
   * whose `TestCanceledException` contains the specified message and cause.
   *
   * @message the message string to include in the `Canceled`'s `TestCanceledException`.
   * @cause exception to include as the `Canceled`'s `TestCanceledException` cause.
   */
  def canceled(message: String, cause: Throwable) =
    FutureOutcome { Future.successful(Canceled(message, cause)) }

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Succeeded` result.
   */
  def succeeded: FutureOutcome =
    FutureOutcome { Future.successful(Succeeded) }

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Failed` result.
   */
  def failed(): FutureOutcome =
    FutureOutcome { Future.successful(Failed()) }

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Failed` result
   * containing a `TestFailedException` with the specified message.
   *
   * @message the message string to include in the `Failed`'s `TestFailedException`.
   */
  def failed(message: String): FutureOutcome =
    FutureOutcome { Future.successful(Failed(message)) }

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Failed` result
   * containing a `TestFailedException` with the specified message and cause.
   *
   * @message the message string to include in the `Failed`'s `TestFailedException`.
   * @cause exception to include as the `Failed`'s `TestFailedException` cause.
   */
  def failed(message: String, cause: Throwable) =
    FutureOutcome { Future.successful(Failed(message, cause)) }

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Failed` result
   * containing a `TestFailedException` with the specified cause.
   *
   * @cause exception to include as the `Failed`'s `TestFailedException` cause.
   */
  def failed(cause: Throwable): FutureOutcome =
    FutureOutcome { Future.successful(Failed(cause)) }

  /**
   * Factory method that creates an already completed `FutureOutcome` with a `Pending` result.
   */
  def pending: FutureOutcome =
    FutureOutcome { Future.successful(Pending) }
}

/*
 FutureOutcome.fromOutcome(Canceled("..."))
*/

