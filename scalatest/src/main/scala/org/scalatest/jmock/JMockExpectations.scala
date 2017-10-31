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
package org.scalatest.jmock

import org.jmock.Expectations
import org.hamcrest.Matcher

/**
 * Subclass of `org.jmock.Expectations` that provides `withArg`
 * alternatives to the `with` methods defined in its superclass.
 *
 * `JMockCycle`'s `expecting` method of passes an instance of this class
 * to the function passed into `expectations`. Because `JMockExpectations`
 * extends `org.jmock.Expectations`, all of the `Expectations` methods are
 * available to be invoked on instances of this class, in addition to
 * several overloaded `withArg` methods defined in this class. These `withArg` methods simply
 * invoke corresponding `with` methods on `this`. Because `with` is
 * a keyword in Scala, to invoke these directly you must surround them in back ticks, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * oneOf (mockCollaborator).documentAdded(`with`("Document"))
 * }}}
 *
 * By importing the members of the `JMockExpectations` object passed to
 * a `JMockCycle`'s `executing` method, you can
 * instead call `withArg` with no back ticks needed:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * oneOf (mockCollaborator).documentAdded(withArg("Document"))
 * }}}
 *
 * @author Bill Venners
 */
final class JMockExpectations extends Expectations {

  /**
   * Invokes `with` on this instance, passing in the passed value.
   */
  def withArg[T](value: T): T = `with`(value)

  /**
   * Invokes `with` on this instance, passing in the passed value.
   */
  def withArg(value: Int): Int = `with`(value)

  /**
   * Invokes `with` on this instance, passing in the passed value.
   */
  def withArg(value: Short): Short = `with`(value)

  /**
   * Invokes `with` on this instance, passing in the passed value.
   */
  def withArg(value: Byte): Byte = `with`(value)

  /**
   * Invokes `with` on this instance, passing in the passed value.
   */
  def withArg(value: Long): Long = `with`(value)

  /**
   * Invokes `with` on this instance, passing in the passed value.
   */
  def withArg(value: Boolean): Boolean = `with`(value)

  /**
   * Invokes `with` on this instance, passing in the passed value.
   */
  def withArg(value: Float): Float = `with`(value)

  /**
   * Invokes `with` on this instance, passing in the passed value.
   */
  def withArg(value: Double): Double = `with`(value)

  /**
   * Invokes `with` on this instance, passing in the passed value.
   */
  def withArg(value: Char): Char = `with`(value)

  /**
   * Invokes `with` on this instance, passing in the passed matcher.
   */
  def withArg[T](matcher: Matcher[T]): T = `with`(matcher)

  /**
   * Invokes `with` on this instance, passing in the passed matcher.
   */
  def withArg(matcher: Matcher[Int]): Int = `with`(matcher)

  /**
   * Invokes `with` on this instance, passing in the passed matcher.
   */
  def withArg(matcher: Matcher[Short]): Short = `with`(matcher)

  /**
   * Invokes `with` on this instance, passing in the passed matcher.
   */
  def withArg(matcher: Matcher[Byte]): Byte = `with`(matcher)

  /**
   * Invokes `with` on this instance, passing in the passed matcher.
   */
  def withArg(matcher: Matcher[Long]): Long = `with`(matcher)

  /**
   * Invokes `with` on this instance, passing in the passed matcher.
   */
  def withArg(matcher: Matcher[Boolean]): Boolean = `with`(matcher)

  /**
   * Invokes `with` on this instance, passing in the passed matcher.
   */
  def withArg(matcher: Matcher[Float]): Float = `with`(matcher)

  /**
   * Invokes `with` on this instance, passing in the passed matcher.
   */
  def withArg(matcher: Matcher[Double]): Double = `with`(matcher)

  /**
   * Invokes `with` on this instance, passing in the passed matcher.
   */
  def withArg(matcher: Matcher[Char]): Char = `with`(matcher)
}
