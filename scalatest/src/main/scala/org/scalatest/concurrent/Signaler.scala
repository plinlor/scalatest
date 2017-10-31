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
package org.scalatest.concurrent

/**
 * Strategy for signaling an operation after a timeout expires.
 *
 * An instance of this trait is used for configuration when using traits
 * <a href="TimeLimits.html">`TimeLimits`</a> and <a href="TimeLimitedTests.html">`TimeLimitedTests`</a>.
 * 
 */
trait Signaler {

  /**
   * Signals an operation.
   *
   * This method may do anything to attempt to signal or interrupt an operation, or even do nothing.
   * When called by `failAfter` method of trait <a href="TimeLimits.html">`TimeLimits`</a>, the passed
   * `Thread` will represent the main test thread. This `Thread` is
   * passed in case it is useful, but need not be used by implementations of this method.
   * 
   */
  def apply(testThread: Thread): Unit
}

/**
 * Companion object that provides a factory method for a `Singlaer` defined
 * in terms of a function from a function of type `Thread` to `Unit`.
 */
object Signaler {

  /**
   * Factory method for a `Signaller` defined in terms of a function from a function of
   * type `Thread` to `Unit`.
   *
   * When this `apply` method is invoked, it will invoke the passed function's `apply`
   * method, forwarding along the passed `Thread`.
   *
   * @param fun the function representing the signaling strategy
   */
  def apply(fun: Thread => Unit) =
    new Signaler {
      def apply(testThread: Thread): Unit = { fun(testThread) }
    }

  /**
   * Implicit `Signaler` value defining a default signaling strategy for the `failAfter` and `cancelAfter` method
   * of trait [[org.scalatest.concurrent.TimeLimits]].
   */
  implicit def default: Signaler = DoNotSignal
}

