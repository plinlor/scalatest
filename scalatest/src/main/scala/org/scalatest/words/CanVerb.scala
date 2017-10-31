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
 * Provides an implicit conversion that adds `can` methods to `String`
 * to support the syntax of `FlatSpec`, `WordSpec`, `org.scalatest.fixture.FlatSpec`,
 * and `fixture.WordSpec`.
 *
 * For example, this trait enables syntax such as the following test registration in `FlatSpec`
 * and `fixture.FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (when empty)" can "be empty" in { ... }
 *                        ^
 * }}}
 *
 * It also enables syntax such as the following shared test registration in `FlatSpec`
 * and `fixture.FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (with one item)" can behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
 *                           ^
 * }}}
 *
 * In addition, it supports the registration of subject descriptions in `WordSpec`
 * and `fixture.WordSpec`, such as:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (when empty)" can { ...
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
 *   "The ScalaTest Matchers DSL" can provide {
 *                                ^
 * }}}
 *
 * The reason this implicit conversion is provided in a separate trait, instead of being provided
 * directly in `FlatSpec`, `WordSpec`, `fixture.FlatSpec`, and
 * `fixture.WordSpec`, is primarily for design symmetry with `ShouldVerb`
 * and `MustVerb`. Both `ShouldVerb` and `MustVerb` must exist
 * as a separate trait because an implicit conversion provided directly would conflict
 * with the implicit conversion that provides `should` or `must` methods on `String`
 * in the `Matchers` and `MustMatchers` traits.
 * 
 *
 * @author Bill Venners
 */
trait CanVerb {

  // This one can be final, because it isn't extended by anything in the matchers DSL.
  /**
   * This class supports the syntax of `FlatSpec`, `WordSpec`, `fixture.FlatSpec`,
   * and `fixture.WordSpec`.
   *
   * This class is used in conjunction with an implicit conversion to enable `can` methods to
   * be invoked on `String`s.
   * 
   *
   * @author Bill Venners
   */
  trait StringCanWrapperForVerb {

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
     * "A Stack (when empty)" can "be empty" in { ... }
     *                        ^
     * }}}
     *
     * `FlatSpec` passes in a function via the implicit parameter that takes
     * three strings and results in a `ResultOfStringPassedToVerb`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"can"`, and right, and returns the result.
     * 
     */
    def can(right: String)(implicit svsi: StringVerbStringInvocation): ResultOfStringPassedToVerb = {
      svsi(leftSideString, "can", right, pos)
    }

    /**
     * Supports shared test registration in `FlatSpec` and `fixture.FlatSpec`.
     *
     * For example, this method enables syntax such as the following in `FlatSpec`
     * and `fixture.FlatSpec`:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * "A Stack (with one item)" can behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
     *                           ^
     * }}}
     *
     * `FlatSpec` and `fixture.FlatSpec` passes in a function via the implicit parameter that takes
     * a string and results in a `BehaveWord`. This method
     * simply invokes this function, passing in leftSideString, and returns the result.
     * 
     */
    def can(right: BehaveWord)(implicit svbli: StringVerbBehaveLikeInvocation): BehaveWord = {
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
     * "A Stack (when empty)" can { ...
     *                        ^
     * }}}
     *
     * `WordSpec` passes in a function via the implicit parameter of type `StringVerbBlockRegistration`,
     * a function that takes two strings and a no-arg function and results in `Unit`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"can"`, and the right by-name parameter transformed into a
     * no-arg function.
     * 
     */
    def can(right: => Unit)(implicit fun: StringVerbBlockRegistration): Unit = {
      fun(leftSideString, "can", pos, right _)
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
     *   "The ScalaTest Matchers DSL" can provide {
     *                                ^
     * }}}
     *
     * `WordSpec` passes in a function via the implicit parameter that takes
     * two strings and a `ResultOfAfterWordApplication` and results in `Unit`. This method
     * simply invokes this function, passing in leftSideString, the verb string
     * `"can"`, and the `ResultOfAfterWordApplication` passed to `can`.
     * 
     */
    def can(resultOfAfterWordApplication: ResultOfAfterWordApplication)(implicit swawr: SubjectWithAfterWordRegistration): Unit = {
      swawr(leftSideString, "can", resultOfAfterWordApplication, pos)
    }
  }

  import scala.language.implicitConversions

  /**
   * Implicitly converts an object of type `String` to a `StringCanWrapper`,
   * to enable `can` methods to be invokable on that object.
   */
  implicit def convertToStringCanWrapper(o: String)(implicit position: source.Position): StringCanWrapperForVerb =
    new StringCanWrapperForVerb {
      val leftSideString = o.trim
      val pos = position
    }
}
