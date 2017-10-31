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

import java.net.Socket

/**
 * Strategy for interrupting an operation in which `close` is called on the `java.net.Socket` passed to
 * the constructor.
 *
 * This class can be used for configuration when using traits <a href="Timeouts.html">`Timeouts`</a>
 * and <a href="TimeLimitedTests.html">`TimeLimitedTests`</a>.
 */
@deprecated("Please use org.scalatest.concurrent.Signaler instead.")
class SocketInterruptor(socket: Socket) extends Interruptor {

  /**
   * Invokes `close` on the `java.net.Socket` passed to this class's constructor.
   *
   * @param testThread unused by this strategy
   */
  def apply(testThread: Thread): Unit = {
    socket.close()
  }
}

/**
 * Companion object that provides a factory method for a `SocketInterruptor`.
 */
object SocketInterruptor {

  /**
   * Factory method for a `SocketInterruptor`.
   *
   * @param socket the `java.net.Socket` to pass to the `SocketInterruptor` constructor
   */
  def apply(socket: Socket) = new SocketInterruptor(socket)
}
