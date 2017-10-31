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
package exceptions

import org.scalactic.Requirements._
import org.scalactic.exceptions.NullArgumentException
import org.scalactic.source
import StackDepthExceptionHelper.posOrElseStackDepthFun

/**
 * Exception that indicates a property check failed.
 *
 * @param messageFun a function that returns a detail message (not optional) for this `PropertyCheckFailedException`.
 * @param cause an optional cause, the `Throwable` that caused this `PropertyCheckFailedException` to be thrown.
 * @param posOrStackDepthFun either a source position or a function that returns the depth in the stack trace of this exception at which the line of test code that failed resides.
 * @param payload an optional payload, which ScalaTest will include in a resulting `TestFailed` event
 * @param undecoratedMessage just a short message that has no redundancy with args, labels, etc. The regular "message" has everything in it.
 * @param args the argument values that caused the property check to fail.
 * @param optionalArgNames an optional list of string names for the arguments.
 *
 * @throws NullArgumentException if any parameter is `null` or `Some(null)`.
 *
 * @author Bill Venners
 */
abstract class PropertyCheckFailedException(
  messageFun: StackDepthException => String,
  cause: Option[Throwable],
  posOrStackDepthFun: Either[source.Position, StackDepthException => Int],
  payload: Option[Any],
  val undecoratedMessage: String,
  val args: List[Any],
  optionalArgNames: Option[List[String]]
) extends TestFailedException((sde: StackDepthException) => Some(messageFun(sde)), cause, posOrStackDepthFun, payload) {

  /**
    * Constructs a `PropertyCheckFailedException` with given error message function, optional cause, stack depth function,
    * optional payload, undecorated message, arguments values and optional argument names.
    *
    * @param messageFun a function that returns a detail message (not optional) for this `PropertyCheckFailedException`
    * @param cause an optional cause, the `Throwable` that caused this `PropertyCheckFailedException` to be thrown.
    * @param failedCodeStackDepthFun a function that returns the depth in the stack trace of this exception at which the line of test code that failed resides
    * @param payload an optional payload, which ScalaTest will include in a resulting `TestFailed` event
    * @param undecoratedMessage just a short message that has no redundancy with args, labels, etc. The regular "message" has everything in it.
    * @param args the argument values that caused the property check to fail.
    * @param optionalArgNames an optional list of string names for the arguments.
    */
  def this(
    messageFun: StackDepthException => String,
    cause: Option[Throwable],
    failedCodeStackDepthFun: StackDepthException => Int,
    payload: Option[Any],
    undecoratedMessage: String,
    args: List[Any],
    optionalArgNames: Option[List[String]]
  ) = this(messageFun, cause, Right(failedCodeStackDepthFun), payload, undecoratedMessage, args, optionalArgNames)

  requireNonNull(
    messageFun, cause, posOrStackDepthFun, undecoratedMessage, args,
    optionalArgNames)

  cause match {
    case Some(null) => throw new NullArgumentException("cause was a Some(null)")
    case _ =>
  }

  optionalArgNames match {
    case Some(null) => throw new NullArgumentException("optionalArgNames was a Some(null)")
    case _ =>
  }

  /**
   * A list of names for the arguments that caused the property check to fail.
   *
   * If the `optionalArgNames` class parameter is defined, this method returns
   * the `List[String]` contained in the `Some`. Otherwise, it returns
   * a list that gives `"arg0"` for the zeroeth argument, `"arg1"` for the
   * first argument, `"arg2"` for the second argument, and so on.
   * 
   */
  def argNames: List[String] =
    optionalArgNames match {
      case Some(argNames) => argNames
      case None => (for (idx <- 0 until args.length) yield { "arg" + idx }).toList
    }
}

