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
package org.scalatest.matchers

import org.scalatest._
import org.scalactic.Prettifier

import scala.reflect.ClassTag

/**
 * Trait extended by matcher objects that can match a value of the specified type.
 * `AMatcher` represents a noun that appears after the word `a`, thus a nounName is required.
 *
 * The value to match is passed to the `AMatcher`'s `apply` method. The result is a `MatchResult`.
 * An `AMatcher` is, therefore, a function from the specified type, `T`, to a `MatchResult`.
 * 
 *
 * Although `AMatcher`
 * and `Matcher` represent very similar concepts, they have no inheritance relationship
 * because `Matcher` is intended for use right after `should` or `must`
 * whereas `AMatcher` is intended for use right after `a`.
 * 
 *
 * As an example, you could create `AMatcher[Int]`
 * called `positiveNumber` that would match any positive `Int`, and one called `negativeNumber` that would match
 * any negative `Int`.
 * Given this pair of `AMatcher`s, you could check whether an `Int` was positive or negative with expressions like:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * num should be a positiveNumber
 * num should not be a negativeNumber
 * }}}
 *
 * Here's is how you might define the positiveNumber and negativeNumber `AMatchers`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Using AMatcher.apply method
 * val positiveNumber = AMatcher[Int]("positive number"){ _ > 0 }
 *
 * // Or by extending AMatcher trait
 * val negativeNumber = new AMatcher[Int] {
 *   val nounName = "negative number"
 *   def apply(left: Int): MatchResult =
 *     MatchResult(
 *       left < 0,
 *       left + " was not a " + nounName,
 *       left + " was a " + nounName
 *     )
 * }
 * }}}
 *
 * Here's an rather contrived example of how you might use `positiveNumber` and `negativeNumber`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 *
 * val num1 = 1
 * num1 should be a positiveNumber
 *
 * val num2 = num1 * -1
 * num2 should be a negativeNumber
 *
 * num1 should be a negativeNumber
 * }}}
 *
 * The last assertion in the above test will fail with this failure message:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * 1 was not a negative number
 * }}}
 *
 * For more information on `MatchResult` and the meaning of its fields, please
 * see the documentation for <a href="MatchResult.html">`MatchResult`</a>. To understand why `AMatcher`
 * is contravariant in its type parameter, see the section entitled "Matcher's variance" in the
 * documentation for <a href="Matcher.html">`Matcher`</a>.
 * 
 *
 * @tparam T The type used by this AMatcher's apply method.
 * @author Bill Venners
 * @author Chee Seng
 */
private[scalatest] trait AMatcher[-T] extends Function1[T, MatchResult] {
  /**
   * The name of the noun that this `AMatcher` represents.
   */
  val nounName: String

  /**
   * Check to see if the specified object, `left`, matches, and report the result in
   * the returned `MatchResult`. The parameter is named `left`, because it is
   * usually the value to the left of a `should` or `must` invocation. For example,
   * in:
   *
   * {{{  <!-- class="stHighlight" -->
   * num should be a positiveNumber
   * }}}
   *
   * The `num should be` expression results in a regular <a href="../Matchers$ResultOfBeWordForAny.html">`ResultOfBeWordForAny`</a> that hold
   * a reference to `num` and has a method named `a` that takes a `AMatcher`.  The `a` method
   * calls `AMatcher`'s apply method by passing in the `num`, and check if `num` matches.
   *
   * @param left the value against which to match
   * @return the `MatchResult` that represents the result of the match
   */
  def apply(left: T): MatchResult
}

/**
 * Companion object for trait `AMatcher` that provides a
 * factory method that creates a `AMatcher[T]` from a
 * passed noun name and function of type `(T =&gt; MatchResult)`.
 *
 * @author Bill Venners
 * @author Chee Seng
 */
private[scalatest] object AMatcher {

  /**
   * Factory method that creates a `AMatcher[T]` from a
   * passed noun name and function of type `(T =&gt; MatchResult)`.
   *
   * @param name the noun name
   * @param fun the function of type `(T =&gt; MatchResult)`
   * @return `AMatcher` instance that has the passed noun name and matches using the passed function
   * @author Bill Venners
   * @author Chee Seng
   */
  def apply[T](name: String)(fun: T => Boolean)(implicit ev: ClassTag[T]) =
    new AMatcher[T] {
      val nounName = name
      def apply(left: T): MatchResult = 
        MatchResult(
          fun(left), 
          Resources.rawWasNotA,
          Resources.rawWasA,
          Vector(left, UnquotedString(nounName))
        )
      override def toString: String = "AMatcher[" + ev.runtimeClass.getName + "](" + Prettifier.default(name) + ", " + ev.runtimeClass.getName + " => Boolean)"
    }
  
}
