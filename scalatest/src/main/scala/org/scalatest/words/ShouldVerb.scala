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
 * Provides an implicit conversion that adds `should` methods to `String`
 * to support the syntax of `FlatSpec`, `WordSpec`, `fixture.FlatSpec`,
 * and `fixture.WordSpec`.
 *
 * For example, this trait enables syntax such as the following test registration in `FlatSpec`
 * and `fixture.FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (when empty)" should "be empty" in { ... }
 *                        ^
 * }}}
 *
 * It also enables syntax such as the following shared test registration in `FlatSpec`
 * and `fixture.FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (with one item)" should behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
 *                           ^
 * }}}
 *
 * In addition, it supports the registration of subject descriptions in `WordSpec`
 * and `fixture.WordSpec`, such as:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (when empty)" should { ...
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
 *   "The ScalaTest Matchers DSL" should provide {
 *                                ^
 * }}}
 *
 * The reason this implicit conversion is provided in a separate trait, instead of being provided
 * directly in `FlatSpec`, `WordSpec`, `fixture.FlatSpec`, and
 * `fixture.WordSpec`, is because an implicit conversion provided directly would conflict
 * with the implicit conversion that provides `should` methods on `String`
 * in the `Matchers` trait. By contrast, there is no conflict with
 * the separate `ShouldVerb` trait approach, because:
 * 
 *
 * <ol>
 * <li>`FlatSpec`, `WordSpec`, `fixture.FlatSpec`, and `fixture.WordSpec`
 * mix in `ShouldVerb` directly, and</li>
 * <li>`Matchers` extends `ShouldVerb`, overriding the
 * `convertToStringShouldWrapper` implicit conversion function.</li>
 * </ol>
 *
 * So whether or not
 * a `FlatSpec`, `WordSpec`, `fixture.FlatSpec`, or `fixture.WordSpec`
 * mixes in `Matchers`, there will only be one
 * implicit conversion in scope that adds `should` methods to `String`s.
 * 
 *
 * 
 * Also, because the class of the result of the overriding `convertToStringShouldWrapper`
 * implicit conversion method provided in `Matchers` extends this trait's
 * `StringShouldWrapperForVerb` class, the four uses of `should` provided here
 * are still available. These four `should` are in fact available to any class
 * that mixes in `Matchers`, but each takes an implicit parameter that is provided
 * only in `FlatSpec` and `fixture.FlatSpec`, or `WordSpec` and
 * `fixture.WordSpec`.  
 * 
 *
 * @author Bill Venners
 */
trait ShouldVerb {

  // This can't be final or abstract, because it is instantiated directly by the implicit conversion, and
  // extended by something in Matchers.
  /**
   * This class supports the syntax of `FlatSpec`, `WordSpec`, `fixture.FlatSpec`,
   * and `fixture.WordSpec`.
   *
   * This class is used in conjunction with an implicit conversion to enable `should` methods to
   * be invoked on `String`s.
   * 
   *
   * @author Bill Venners
   */
  trait StringShouldWrapperForVerb {

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
     * "A Stack (when empty)" should "be empty" in { ... }
     *                        ^
     * }}}
     *
     * `FlatSpec` passes in a StringVerbStringInvocation via the implicit parameter that takes
     * three strings and results in a `ResultOfStringPassedToVerb`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"should"`, and right, and returns the result.
     * 
     */
    def should(right: String)(implicit svsi: StringVerbStringInvocation): ResultOfStringPassedToVerb = {
      svsi(leftSideString, "should", right, pos)
    }

    /**
     * Supports shared test registration in `FlatSpec` and `fixture.FlatSpec`.
     *
     * For example, this method enables syntax such as the following in `FlatSpec`
     * and `fixture.FlatSpec`:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack (with one item)" should behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
     *                           ^
     * }}}
     *
     * `FlatSpec` and `fixture.FlatSpec` passes in a function via the implicit parameter that takes
     * a string and results in a `BehaveWord`. This method
     * simply invokes this function, passing in leftSideString, and returns the result.
     * 
     */
    def should(right: BehaveWord)(implicit svbli: StringVerbBehaveLikeInvocation): BehaveWord = {
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
     * "A Stack (when empty)" should { ...
     *                        ^
     * }}}
     *
     * `WordSpec` passes in a function via the implicit parameter of type `StringVerbBlockRegistration`,
     * a function that takes two strings and a no-arg function and results in `Unit`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"should"`, and the right by-name parameter transformed into a
     * no-arg function.
     * 
     */
    def should(right: => Unit)(implicit fun: StringVerbBlockRegistration): Unit = {
      fun(leftSideString, "should", pos, right _)
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
     *   "The ScalaTest Matchers DSL" should provide {
     *                                ^
     * }}}
     *
     * `WordSpec` passes in a function via the implicit parameter that takes
     * two strings and a `ResultOfAfterWordApplication` and results in `Unit`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"should"`, and the `ResultOfAfterWordApplication` passed to `should`.
     * 
     */
    def should(resultOfAfterWordApplication: ResultOfAfterWordApplication)(implicit swawr: SubjectWithAfterWordRegistration): Unit = {
      swawr(leftSideString, "should", resultOfAfterWordApplication, pos)
    }
  }

  import scala.language.implicitConversions

  /**
   * Implicitly converts an object of type `String` to a `StringShouldWrapperForVerb`,
   * to enable `should` methods to be invokable on that object.
   */
  implicit def convertToStringShouldWrapperForVerb(o: String)(implicit position: source.Position): StringShouldWrapperForVerb =
    new StringShouldWrapperForVerb {
      val leftSideString = o.trim
      val pos = position
    }
}
