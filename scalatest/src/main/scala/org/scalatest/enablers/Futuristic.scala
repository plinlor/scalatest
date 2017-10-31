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
package org.scalatest.enablers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import org.scalatest.FutureOutcome

/**
 * Supertrait for `Futureistic` typeclasses.
 *
 * Trait `Futureistic` is a typeclass trait for objects that can be used with
 * the `complete`-`lastly` syntax of trait
 * <a href="../CompleteLastly.html">`CompleteLastly`</a>.
 * 
 */
trait Futuristic[T] {

  /**
   * Register a cleanup function to be executed when the passed futuristic type completes,
   * returning a new instance of the same futuristic type that completes after the passed
   * futuristic and the subsequent cleanup function execution completes.
   *
   * The futuristic returned by this method will have the same result as the original futuristic passed to `withCleanup`,
   * unless the cleanup code throws an exception. If the cleanup code passed to `withCleanup` throws
   * an exception, the returned futuristic will fail with that exception.
   * 
   *
   * @param futuristic a future-like type
   * @param cleanup a cleanup function to execute once the passed futuristic type completes
   * @return a new instance of the passed futuristic type that completes after both the passed futuristic and the
   *   the subsequent cleanup function have completed.
   */
  def withCleanup(futuristic: T)(cleanup: => Unit): T
}

/**
 * Companion object for trait `Futuristic` that contains implicit `Futuristic` providers for
 * `FutureOutcome` and `Future[T]` for any type `T`.
 */
object Futuristic {

  /**
   * Provides a `Futuristic` value for `FutureOutcome` that performs cleanup using the
   * implicitly provided execution context.
   *
   * @param executionContext an execution context that provides a strategy for executing the cleanup function
   * @return a `Futuristic` instance for `FutureOutcome`
   */
  implicit def futuristicNatureOfFutureOutcome(implicit executionContext: ExecutionContext): Futuristic[FutureOutcome] =
    new Futuristic[FutureOutcome] {
      def withCleanup(futuristic: FutureOutcome)(cleanup: => Unit): FutureOutcome = {
        futuristic onCompletedThen { _ => cleanup }
      }
    }

  /**
   * Provides a `Futuristic` value for `Future[V]` for any type `V` that performs cleanup using the
   * implicitly provided execution context.
   *
   * @param executionContext an execution context that provides a strategy for executing the cleanup function
   * @return a `Futuristic` instance for `Future[V]`
   */
  implicit def futuristicNatureOfFutureOf[V](implicit executionContext: ExecutionContext): Futuristic[Future[V]] =
    new Futuristic[Future[V]] {
      def withCleanup(futuristic: Future[V])(cleanup: => Unit): Future[V] = {
        // First deal with Failure, and mimic finally semantics
        // but in future-space. The recoverWith will only execute
        // if this is a Failure, and will only return a Failure,
        // so the subsequent map will only happen if this recoverWith
        // does not happen.
        futuristic recoverWith {
          case firstEx: Throwable => 
            try {
              cleanup
              Future.failed(firstEx)
            }
            catch {
              case secondEx: Throwable =>
                Future.failed(secondEx)
            }
        } map { v => // Ensure cleanup happens for the Success case
          cleanup
          v
        }
      }
    }
}

