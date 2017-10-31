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

import enablers.Futuristic

/**
 * Trait that provides a `complete`-`lastly` construct, which ensures
 * cleanup code in `lastly` is executed whether the code passed to `complete`
 * completes abruptly with an exception or successfully results in a `Future`,
 * <a href="FutureOutcome.html">`FutureOutcome`</a>, or other type with an
 * implicit <a href="enablers/Futuristic.html">`Futuristic`</a> instance.
 *
 * This trait is mixed into ScalaTest's async testing styles, to make it easy to ensure
 * cleanup code will execute whether code that produces a "futuristic" value (any type `F`
 * for which a `Futuristic[F]` instance is implicitly available). ScalaTest provides
 * implicit `Futuristic` instances for `Future[T]` for any type `T`
 * and `FutureOutcome`.
 * 
 *
 * If the future-producing code passed to `complete` throws an
 * exception, the cleanup code passed to `lastly` will be executed immediately, and the same exception will
 * be rethrown, unless the code passed to `lastly` also completes abruptly with an exception. In that case,
 * `complete`-`lastly` will complete abruptly with the exception thrown by the code passed to
 * `lastly` (this mimics the behavior of `finally`).
 * 
 *
 * Otherwise, if the code passed to `complete` successfully returns a `Future` (or other "futuristic" type),
 * `complete`-`lastly`
 * will register the cleanup code to be performed once the future completes and return a new future that will complete
 * once the original future completes ''and'' the subsequent cleanup code has completed execution. The future returned by
 * `complete`-`lastly` will have the same result as the original future passed to `complete`,
 * unless the cleanup code throws an exception. If the cleanup code passed to `lastly` throws
 * an exception, the future returned by `lastly` will fail with that exception.
 * 
 *
 * The `complete`-`lastly` syntax is intended to be used to ensure cleanup code is executed
 * in async testing styles like `try`-`finally` is used in traditional testing styles.
 * Here's an example of `complete`-`lastly`
 * used in `withFixture` in an async testing style:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgAsyncTest) = {
 *
 *   // Perform setup here
 *
 *   complete {
 *     super.withFixture(test) // Invoke the test function
 *   } lastly {
 *     // Perform cleanup here
 *   }
 * }
 * }}}
 */
trait CompleteLastly {

  /**
   * Class that provides the `lastly` method of the `complete`-`lastly` syntax.
   *
   * @param futuristicBlock a by-name that produces a futuristic type
   * @param futuristic the futuristic typeclass instance
   */
  class ResultOfCompleteInvocation[T](futuristicBlock: => T, futuristic: Futuristic[T]) {

   /**
    * Registers cleanup code to be executed immediately if the future-producing code passed
    * to `complete` throws an exception, or otherwise asynchronously, when the future
    * returned by the code passed to `complete` itself completes.
    *
    * See the main documentation for trait `CompleteLastly` for more detail.
    * 
    *
    * @param lastlyBlock cleanup code to execute whether the code passed to `complete`
    *           throws an exception or succesfully returns a futuristic value.
    */
    def lastly(lastlyBlock: => Unit): T = {
      val result: T =
        try futuristicBlock // evaluate the by-name once
        catch {
          case ex: Throwable =>
            lastlyBlock  // execute the clean up
            throw ex // rethrow the same exception
        }
      futuristic.withCleanup(result) { lastlyBlock }
    }

    /**
     * Pretty string representation of this class.
     */
    override def toString = "ResultOfCompleteInvocation"
  }

  /**
   * Registers a block of code that produces any "futuristic" type (any type `F` for which
   * an implicit <a href="enablers/Futuristic.html">`Futuristic[F]`</a> instance is implicitly available), returning
   * an object that offers a `lastly` method. 
    *
    * See the main documentation for trait `CompleteLastly` for more detail.
    * 
    *
    * @param completeBlock cleanup code to execute whether the code passed to `complete`
    *           throws an exception or succesfully returns a futuristic value.
   */
  def complete[T](completeBlock: => T)(implicit futuristic: Futuristic[T]): ResultOfCompleteInvocation[T] =
    new ResultOfCompleteInvocation[T](completeBlock, futuristic)
}

/**
 * Companion object that facilitates the importing of `CompleteLastly` members as 
 * an alternative to mixing it in.
 */
object CompleteLastly extends CompleteLastly

