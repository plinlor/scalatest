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
import java.nio.channels.Selector

/**
 * Strategy for signaling an operation in which `wakeup` is called on the `java.nio.channels.Selector` passed to
 * the constructor.
 *
 * This class can be used for configuration when using traits <a href="TimeLimits.html">`TimeLimits`</a>
 * and <a href="TimeLimitedTests.html">`TimeLimitedTests`</a>.
 */
class SelectorSignaler(selector: Selector) extends Signaler {
  
  /**
   * Invokes `wakeup` on the `java.nio.channels.Selector` passed to this class's constructor.
   *
   * @param testThread unused by this strategy
   */
  def apply(testThread: Thread): Unit = {
    selector.wakeup()
  }
}

/**
 * Companion object that provides a factory method for a `SelectorSignaler`.
 */
object SelectorSignaler {

  /**
   * Factory method for a `SelectorSignaler`.
   *
   * @param selector the `java.nio.channels.Selector` to pass to the `SelectorSignaler` constructor
   */
  def apply(selector: Selector) = new SelectorSignaler(selector)
}

