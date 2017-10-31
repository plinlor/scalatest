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

import org.scalactic._

/**
 * Provides an implicit conversion that adds `will` methods to `String`
 * to support the syntax of `FlatSpec`, `WordSpec`, `fixture.FlatSpec`,
 * and `fixture.WordSpec`.
 *
 * For example, this trait enables syntax such as the following test registration in `FlatSpec`
 * and `fixture.FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (when empty)" will "be empty" in { ... }
 *                        ^
 * }}}
 *
 * It also enables syntax such as the following shared test registration in `FlatSpec`
 * and `fixture.FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (with one item)" will behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
 *                           ^
 * }}}
 *
 * In addition, it supports the registration of subject descriptions in `WordSpec`
 * and `fixture.WordSpec`, such as:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (when empty)" will { ...
 *                        ^
 * }}}
 *
 * And finally, it also supportds the registration of subject descriptions with after words
 * in `WordSpec` and `fixture.WordSpec`. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 *    def provide = afterWord("provide")
 *
 *   "The ScalaTest Matchers DSL" will provide {
 *                                ^
 * }}}
 *
 * The reason this implicit conversion is provided in a separate trait, instead of being provided
 * directly in `FlatSpec`, `WordSpec`, `fixture.FlatSpec`, and
 * `fixture.WordSpec`, is because an implicit conversion provided directly would conflict
 * with the implicit conversion that provides `will` methods on `String`
 * in the `Matchers` trait. By contrast, there is no conflict with
 * the separate `WillVerb` trait approach, because:
 * 
 *
 * <ol>
 * <li>`FlatSpec`, `WordSpec`, `fixture.FlatSpec`, and `fixture.WordSpec`
 * mix in `WillVerb` directly, and</li>
 * <li>`Matchers` extends `WillVerb`, overriding the
 * `convertToStringWillWrapper` implicit conversion function.</li>
 * </ol>
 *
 * So whether or not
 * a `FlatSpec`, `WordSpec`, `fixture.FlatSpec`, or `fixture.WordSpec`
 * mixes in `Matchers`, there will only be one
 * implicit conversion in scope that adds `will` methods to `String`s.
 * 
 *
 * 
 * Also, because the class of the result of the overriding `convertToStringWillWrapper`
 * implicit conversion method provided in `Matchers` extends this trait's
 * `StringWillWrapperForVerb` class, the four uses of `will` provided here
 * are still available. These four `will` are in fact available to any class
 * that mixes in `Matchers`, but each takes an implicit parameter that is provided
 * only in `FlatSpec` and `fixture.FlatSpec`, or `WordSpec` and
 * `fixture.WordSpec`.
 * 
 *
 * @author Bill Venners
 */
private[scalatest] trait WillVerb {

  // This can't be final or abstract, because it is instantiated directly by the implicit conversion, and
  // extended by something in Matchers.
  /**
   * This class supports the syntax of `FlatSpec`, `WordSpec`, `fixture.FlatSpec`,
   * and `fixture.WordSpec`.
   *
   * This class is used in conjunction with an implicit conversion to enable `will` methods to
   * be invoked on `String`s.
   * 
   *
   * @author Bill Venners
   */
  trait StringWillWrapperForVerb {

    // Don't use "left" because that conflicts with Scalaz's left method on strings
    val leftSideString: String

    val pos: source.Position

    /**
     * Supports test registration in `FlatSpec` and `fixture.FlatSpec`.
     *
     * For example, this method enables syntax such as the following in `FlatSpec`
     * and `fixture.FlatSpec`:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack (when empty)" will "be empty" in { ... }
     *                        ^
     * }}}
     *
     * `FlatSpec` passes in a function via the implicit parameter that takes
     * three strings and results in a `ResultOfStringPassedToVerb`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"will"`, and right, and returns the result.
     * 
     */
    def will(right: String)(implicit svsi: StringVerbStringInvocation): ResultOfStringPassedToVerb = {
      svsi(leftSideString, "will", right, pos)
    }

    /**
     * Supports shared test registration in `FlatSpec` and `fixture.FlatSpec`.
     *
     * For example, this method enables syntax such as the following in `FlatSpec`
     * and `fixture.FlatSpec`:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack (with one item)" will behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
     *                           ^
     * }}}
     *
     * `FlatSpec` and `fixture.FlatSpec` passes in a function via the implicit parameter that takes
     * a string and results in a `BehaveWord`. This method
     * simply invokes this function, passing in leftSideString, and returns the result.
     * 
     */
    def will(right: BehaveWord)(implicit svbli: StringVerbBehaveLikeInvocation): BehaveWord = {
      svbli(leftSideString, pos)
    }

    /**
     * Supports the registration of subject descriptions in `WordSpec`
     * and `fixture.WordSpec`.
     *
     * For example, this method enables syntax such as the following in `WordSpec`
     * and `fixture.WordSpec`:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack (when empty)" will { ...
     *                        ^
     * }}}
     *
     * `WordSpec` passes in a function via the implicit parameter of type `StringVerbBlockRegistration`,
     * a function that takes two strings and a no-arg function and results in `Unit`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"will"`, and the right by-name parameter transformed into a
     * no-arg function.
     * 
     */
    def will(right: => Unit)(implicit fun: StringVerbBlockRegistration): Unit = {
      fun(leftSideString, "will", pos, right _)
    }

    /**
     * Supports the registration of subject descriptions with after words
     * in `WordSpec` and `fixture.WordSpec`.
     *
     * For example, this method enables syntax such as the following in `WordSpec`
     * and `fixture.WordSpec`:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     *    def provide = afterWord("provide")
     *
     *   "The ScalaTest Matchers DSL" will provide {
     *                                ^
     * }}}
     *
     * `WordSpec` passes in a function via the implicit parameter that takes
     * two strings and a `ResultOfAfterWordApplication` and results in `Unit`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"will"`, and the `ResultOfAfterWordApplication` passed to `will`.
     * 
     */
    def will(resultOfAfterWordApplication: ResultOfAfterWordApplication)(implicit swawr: SubjectWithAfterWordRegistration): Unit = {
      swawr(leftSideString, "will", resultOfAfterWordApplication, pos)
    }
  }

  import scala.language.implicitConversions

  /**
   * Implicitly converts an object of type `String` to a `StringWillWrapperForVerb`,
   * to enable `will` methods to be invokable on that object.
   */
  implicit def convertToStringWillWrapperForVerb(o: String)(implicit position: source.Position): StringWillWrapperForVerb =
    new StringWillWrapperForVerb {
      val leftSideString = o.trim
      val pos = position
    }
}

