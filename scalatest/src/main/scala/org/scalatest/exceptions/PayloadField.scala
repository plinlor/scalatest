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
package org.scalatest.exceptions

/**
 * Trait implemented by exception types that carry an optional payload.
 *
 * Many ScalaTest events include an optional "payload" field that can be used
 * to pass information to a custom reporter. This trait facilitates such customization, 
 * by allowing test code to include a payload in an exception (such as `TestFailedException`).
 * ScalaTest looks for this trait and fires any payloads it finds in the relevant ScalaTest event
 * stimulated by the exception, such as a <a href="../events/TestFailed.html">`TestFailed`</a> event stimulated by a `TestFailedException`.
 * (Although in its initial
 * release there is only two subclasses of `PayloadField` in ScalaTest,
 * <a href="TestFailedException.html">`TestFailedException`</a> and
 * <a href="TestCanceledException.html">`TestCanceledException`</a>,
 * in future version of ScalaTest, there could be more)
 * 
 *
 * For an example of how payloads could be used, see the documentation for trait <a href="../Payloads.html">`Payloads`</a>.
 * 
 *
 * @author Bill Venners
 */
trait PayloadField { this: Throwable =>
  
  /**
   * The optional payload.
   */
  val payload: Option[Any]
}

