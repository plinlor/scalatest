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
package org.scalatest.words

import org.scalatest.MatchersHelper.checkExpectedException
import org.scalatest.Resources
import org.scalatest.MatchersHelper.indicateSuccess
import org.scalatest.MatchersHelper.indicateFailure
import org.scalactic._

/**
 * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
 * the matchers DSL.
 *
 * @author Bill Venners
 */
final class ResultOfATypeInvocation[T](val clazz: Class[T]) {

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * a [RuntimeException] should be thrownBy { ... }
   *                      ^
   * }}}
   **/
  def should(beWord: BeWord)(implicit prettifier: Prettifier, pos: source.Position): ResultOfBeWordForAType[T] =
    new ResultOfBeWordForAType[T](clazz, prettifier, pos)

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * a [RuntimeException] should not
   *                      ^
   * }}}
   *
   * This method is here to direct people trying to use the above syntax to use `noException` instead.
   */
  def should(notWord: NotWord): PleaseUseNoExceptionShouldSyntaxInstead =
    new PleaseUseNoExceptionShouldSyntaxInstead
  
  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * a [RuntimeException] shouldBe thrownBy { ... }
   *                      ^
   * }}}
   **/
  def shouldBe(thrownBy: ResultOfThrownByApplication)(implicit prettifier: Prettifier, pos: source.Position): org.scalatest.Assertion = {
    val caught = try {
      thrownBy.execute()
      None
    }
    catch {
      case u: Throwable => Some(u)
    }
    if (caught.isEmpty) {
      val message = Resources.exceptionExpected(clazz.getName)
      indicateFailure(message, None, pos)
    } else {
      val u = caught.get
      if (!clazz.isAssignableFrom(u.getClass)) {
        val s = Resources.wrongException(clazz.getName, u.getClass.getName)
        indicateFailure(s, Some(u), pos)
      } else indicateSuccess(Resources.exceptionThrown(u.getClass.getName))
    }
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * a [IllegalArgumentException] should (be thrownBy { ... })
   *                              ^
   * }}}
   **/
  def should(beThrownBy: ResultOfBeThrownBy)(implicit prettifier: Prettifier, pos: source.Position): org.scalatest.Assertion = {
    val throwables = beThrownBy.throwables
    val noThrowable = throwables.find(_.isEmpty)
    if (noThrowable.isDefined) {
      val message = Resources.exceptionExpected(clazz.getName)
      indicateFailure(message, None, pos)
    }
    else {
      val unmatch = throwables.map(_.get).find(t => !clazz.isAssignableFrom(t.getClass))
      if (unmatch.isDefined) {
        val u = unmatch.get
        val s = Resources.wrongException(clazz.getName, u.getClass.getName)
        indicateFailure(s, Some(u), pos)
      }
      else indicateSuccess(Resources.exceptionThrown(clazz.getClass.getName))
    }
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * a [RuntimeException] must be thrownBy { ... }
   *                      ^
   * }}}
   **/
  def must(beWord: BeWord)(implicit prettifier: Prettifier, pos: source.Position): ResultOfBeWordForAType[T] =
    new ResultOfBeWordForAType[T](clazz, prettifier, pos)

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * a [RuntimeException] must not
   *                      ^
   * }}}
   *
   * This method is here to direct people trying to use the above syntax to use `noException` instead.
   */
  def must(notWord: NotWord): PleaseUseNoExceptionShouldSyntaxInstead =
    new PleaseUseNoExceptionShouldSyntaxInstead

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * a [RuntimeException] mustBe thrownBy { ... }
   *                      ^
   * }}}
   **/
  def mustBe(thrownBy: ResultOfThrownByApplication)(implicit prettifier: Prettifier, pos: source.Position): org.scalatest.Assertion = {
    val caught = try {
      thrownBy.execute()
      None
    }
    catch {
      case u: Throwable => Some(u)
    }
    if (caught.isEmpty) {
      val message = Resources.exceptionExpected(clazz.getName)
      indicateFailure(message, None, pos)
    } else {
      val u = caught.get
      if (!clazz.isAssignableFrom(u.getClass)) {
        val s = Resources.wrongException(clazz.getName, u.getClass.getName)
        indicateFailure(s, Some(u), pos)
      } else indicateSuccess(Resources.exceptionThrown(u.getClass.getName))
    }
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * a [IllegalArgumentException] must (be thrownBy { ... })
   *                              ^
   * }}}
   **/
  def must(beThrownBy: ResultOfBeThrownBy)(implicit prettifier: Prettifier, pos: source.Position): org.scalatest.Assertion = {
    val throwables = beThrownBy.throwables
    val noThrowable = throwables.find(_.isEmpty)
    if (noThrowable.isDefined) {
      val message = Resources.exceptionExpected(clazz.getName)
      indicateFailure(message, None, pos)
    }
    else {
      val unmatch = throwables.map(_.get).find(t => !clazz.isAssignableFrom(t.getClass))
      if (unmatch.isDefined) {
        val u = unmatch.get
        val s = Resources.wrongException(clazz.getName, u.getClass.getName)
        indicateFailure(s, Some(u), pos)
      }
      else indicateSuccess(Resources.exceptionThrown(clazz.getClass.getName))
    }
  }
  
  override def toString: String = "a [" + clazz.getName + "]"
}
