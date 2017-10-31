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
 * Provides an implicit conversion that adds `must` methods to `String`
 * to support the syntax of `FlatSpec`, `WordSpec`, `fixture.FlatSpec`,
 * and `fixture.WordSpec`.
 *
 * For example, this trait enables syntax such as the following test registration in `FlatSpec`
 * and `fixture.FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (when empty)" must "be empty" in { ... }
 *                        ^
 * }}}
 *
 * It also enables syntax such as the following shared test registration in `FlatSpec`
 * and `fixture.FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (with one item)" must behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
 *                           ^
 * }}}
 *
 * In addition, it supports the registration of subject descriptions in `WordSpec`
 * and `fixture.WordSpec`, such as:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (when empty)" must { ...
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
 *   "The ScalaTest Matchers DSL" must provide {
 *                                ^
 * }}}
 *
 * The reason this implicit conversion is provided in a separate trait, instead of being provided
 * directly in `FlatSpec`, `WordSpec`, `fixture.FlatSpec`, and
 * `fixture.WordSpec`, is because an implicit conversion provided directly would conflict
 * with the implicit conversion that provides `must` methods on `String`
 * in the `MustMatchers` trait. By contrast, there is no conflict with
 * the separate `MustVerb` trait approach, because:
 * 
 *
 * <ol>
 * <li>`FlatSpec`, `WordSpec`, `fixture.FlatSpec`, and `fixture.WordSpec`
 * mix in `MustVerb` directly, and</li>
 * <li>`MustMatchers` extends `MustVerb`, overriding the
 * `convertToStringMustWrapper` implicit conversion function.</li>
 * </ol>
 *
 * So whether or not
 * a `FlatSpec`, `WordSpec`, `fixture.FlatSpec`, or `fixture.WordSpec`
 * mixes in `MustMatchers`, there will only be one
 * implicit conversion in scope that adds `must` methods to `String`s.
 * 
 *
 * 
 * Also, because the class of the result of the overriding `convertToStringMustWrapper`
 * implicit conversion method provided in `MustMatchers` extends this trait's
 * `StringMustWrapperForVerb` class, the four uses of `must` provided here
 * are still available. These four `must` are in fact available to any class
 * that mixes in `MustMatchers`, but each takes an implicit parameter that is provided
 * only in `FlatSpec` and `fixture.FlatSpec`, or `WordSpec` and
 * `fixture.WordSpec`.  
 * 
 *
 * @author Bill Venners
 */
trait MustVerb {

  /**
   * This class supports the syntax of `FlatSpec`, `WordSpec`, `fixture.FlatSpec`,
   * and `fixture.WordSpec`.
   *
   * This class is used in conjunction with an implicit conversion to enable `must` methods to
   * be invoked on `String`s.
   * 
   *
   * @author Bill Venners
   */
  trait StringMustWrapperForVerb {

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
     * "A Stack (when empty)" must "be empty" in { ... }
     *                        ^
     * }}}
     *
     * `FlatSpec` passes in a function via the implicit parameter that takes
     * three strings and results in a `ResultOfStringPassedToVerb`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"must"`, and right, and returns the result.
     * 
     */
    def must(right: String)(implicit svsi: StringVerbStringInvocation): ResultOfStringPassedToVerb = {
      svsi(leftSideString, "must", right, pos)
    }

    /**
     * Supports shared test registration in `FlatSpec` and `fixture.FlatSpec`.
     *
     * For example, this method enables syntax such as the following in `FlatSpec`
     * and `fixture.FlatSpec`:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack (with one item)" must behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
     *                           ^
     * }}}
     *
     * `FlatSpec` and `fixture.FlatSpec` passes in a function via the implicit parameter that takes
     * a string and results in a `BehaveWord`. This method
     * simply invokes this function, passing in leftSideString, and returns the result.
     * 
     */
    def must(right: BehaveWord)(implicit svbli: StringVerbBehaveLikeInvocation): BehaveWord = {
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
     * "A Stack (when empty)" must { ...
     *                        ^
     * }}}
     *
     * `WordSpec` passes in a function via the implicit parameter of type `StringVerbBlockRegistration`,
     * a function that takes two strings and a no-arg function and results in `Unit`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"must"`, and the right by-name parameter transformed into a
     * no-arg function.
     * 
     */
    def must(right: => Unit)(implicit fun: StringVerbBlockRegistration): Unit = {
      fun(leftSideString, "must", pos, right _)
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
     *   "The ScalaTest Matchers DSL" must provide {
     *                                ^
     * }}}
     *
     * `WordSpec` passes in a function via the implicit parameter that takes
     * two strings and a `ResultOfAfterWordApplication` and results in `Unit`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"must"`, and the `ResultOfAfterWordApplication` passed to `must`.
     * 
     */
    def must(resultOfAfterWordApplication: ResultOfAfterWordApplication)(implicit swawr: SubjectWithAfterWordRegistration): Unit = {
      swawr(leftSideString, "must", resultOfAfterWordApplication, pos)
    }
  }

  import scala.language.implicitConversions

  /**
   * Implicitly converts an object of type `String` to a `StringMustWrapper`,
   * to enable `must` methods to be invokable on that object.
   */
  implicit def convertToStringMustWrapperForVerb(o: String)(implicit position: source.Position): StringMustWrapperForVerb =
    new StringMustWrapperForVerb {
      val leftSideString = o.trim
      val pos = position
    }
}
