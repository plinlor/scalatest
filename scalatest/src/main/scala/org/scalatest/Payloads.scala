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

/**
 * Trait facilitating the inclusion of a payload in a thrown ScalaTest exception.
 *
 * This trait includes a `withPayload` construct 
 * that enables a payload object (or modified
 * payload object) to be included as the payload of a thrown exception.
 *
 * Many ScalaTest events include an optional "payload" field that can be used
 * to pass information to a custom reporter. This trait facilitates such customization, 
 * by making it easy to insert a payload into a thrown exception, such as a `TestFailedException`.
 * The thrown exception must mix in `Payload`.
 * ScalaTest looks for trait `Payload` and fires any payloads it finds in the relevant ScalaTest event
 * stimulated by the exception, such as a <a href="events/TestFailed.html">`TestFailed`</a> event stimulated by a <a href="exceptions/TestFailedException.html">`TestFailedException`</a>.
 * Here's an example in which a GUI snapshot is included as a payload when a test fails:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * withPayload(generateGUISnapshot()) {
 *   1 + 1 should === (3)
 * }
 * }}}
 *
 * @author Bill Venners
 */
trait Payloads {

  /**
   * Executes the block of code passed as the second parameter, and, if it
   * completes abruptly with a `ModifiablePayload` exception,
   * replaces the current payload contained in the exception, if any, with the one passed
   * as the first parameter.
   *
   * This method allows you to insert a payload into a thrown `Payload` exception (such as
   * a `TestFailedException`), so that payload can be included in events fired to a custom reporter
   * that can make use of the payload.  
   * Here's an example in which a GUI snapshot is included as a payload when a test fails:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * withPayload(generateGUISnapshot()) {
   *   1 + 1 should === (3)
   * }
   * }}}
   *
  */
  def withPayload[T](payload: => Any)(fun: => T): T = {
    try {
      val outcome: T = fun
      outcome match {
        case Failed(e: org.scalatest.exceptions.ModifiablePayload[_]) if payload != null =>
          Failed(e.modifyPayload((currentPayload: Option[Any]) => Some(payload))).asInstanceOf[T]
        case Canceled(e: org.scalatest.exceptions.ModifiablePayload[_]) if payload != null =>
          Canceled(e.modifyPayload((currentPayload: Option[Any]) => Some(payload))).asInstanceOf[T]
        case _ => outcome
      }
    }
    catch {
      case e: org.scalatest.exceptions.ModifiablePayload[_] =>
        if (payload != null)
          throw e.modifyPayload((currentPayload: Option[Any]) => Some(payload))
        else
          throw e
    }
  }
}

/**
 * Companion object that facilitates the importing of `Payloads` members as 
 * an alternative to mixing it in. One use case is to import `Payloads`
 * members so you can use them in the Scala interpreter.
 */
object Payloads extends Payloads
