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
 * Strategy for interrupting an operation in which `interrupt` is called on the `Thread` passed
 * to `apply`.
 *
 * This object can be used for configuration when using traits <a href="Timeouts.html">`Timeouts`</a>
 * and <a href="TimeLimitedTests.html">`TimeLimitedTests`</a>.
 */
@deprecated("Please use org.scalatest.concurrent.Signaler instead.")
object ThreadInterruptor extends Interruptor {

  /**
   * Invokes `interrupt` on the passed `Thread`.
   *
   * @param testThread the `Thread` to interrupt
   */
  def apply(testThread: Thread): Unit = {
    testThread.interrupt()
  }
}
