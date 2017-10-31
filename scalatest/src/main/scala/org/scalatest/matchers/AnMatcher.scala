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
 * `AnMatcher` represents a noun that appears after the word `an`, thus a nounName is required.
 *
 * The value to match is passed to the `AnMatcher`'s `apply` method. The result is a `MatchResult`.
 * An `AnMatcher` is, therefore, a function from the specified type, `T`, to a `MatchResult`.
 * 
 *
 * Although `AnMatcher`
 * and `Matcher` represent very similar concepts, they have no inheritance relationship
 * because `Matcher` is intended for use right after `should` or `must`
 * whereas `AnMatcher` is intended for use right after `an`.
 * 
 *
 * As an example, you could create `AnMatcher[Int]`
 * called `oddNumber` that would match any odd `Int`, and one called `evenNumber` that would match
 * any even `Int`.
 * Given this pair of `AnMatcher`s, you could check whether an `Int` was odd or even number with expressions like:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * num should be an oddNumber
 * num should not be an evenNumber
 * }}}
 *
 * Here's is how you might define the oddNumber and evenNumber `AnMatchers`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Using AnMatcher.apply method
 * val oddNumber = AnMatcher[Int]("odd number"){ _ % 2 != 0 }
 *
 * // Or by extending AnMatcher trait
 * val evenNumber = new AnMatcher[Int] {
 *   val nounName = "even number"
 *   def apply(left: Int): MatchResult =
 *     MatchResult(
 *       left % 2 == 0,
 *       left + " was not an " + nounName,
 *       left + " was an " + nounName
 *     )
 * }
 * }}}
 *
 * Here's an rather contrived example of how you might use `oddNumber` and `evenNumber`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 *
 * val num1 = 1
 * num1 should be an oddNumber
 *
 * val num2 = num1 + 1
 * num2 should be an evenNumber
 *
 * num1 should be an evenNumber
 * }}}
 *
 * The last assertion in the above test will fail with this failure message:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * 1 was not an even number
 * }}}
 *
 * For more information on `MatchResult` and the meaning of its fields, please
 * see the documentation for <a href="MatchResult.html">`MatchResult`</a>. To understand why `AnMatcher`
 * is contravariant in its type parameter, see the section entitled "Matcher's variance" in the
 * documentation for <a href="Matcher.html">`Matcher`</a>.
 * 
 *
 * @tparam T The type used by this AnMatcher's apply method.
 * @author Bill Venners
 * @author Chee Seng
 */
private[scalatest] trait AnMatcher[-T] extends Function1[T, MatchResult] {
  /**
   * The name of the noun that this `AnMatcher` represents.
   */
  val nounName: String

  /**
   * Check to see if the specified object, `left`, matches, and report the result in
   * the returned `MatchResult`. The parameter is named `left`, because it is
   * usually the value to the left of a `should` or `must` invocation. For example,
   * in:
   *
   * {{{  <!-- class="stHighlight" -->
   * num should be an oddNumber
   * }}}
   *
   * The `num should be` expression results in a regular <a href="../Matchers$ResultOfBeWordForAny.html">`ResultOfBeWordForAny`</a> that hold
   * a reference to `num` and has a method named `an` that takes a `AnMatcher`.  The `an` method
   * calls `AnMatcher`'s apply method by passing in the `num`, and check if `num` matches.
   *
   * @param left the value against which to match
   * @return the `MatchResult` that represents the result of the match
   */
  def apply(left: T): MatchResult
}

/**
 * Companion object for trait `AnMatcher` that provides a
 * factory method that creates a `AnMatcher[T]` from a
 * passed noun name and function of type `(T =&gt; MatchResult)`.
 *
 * @author Bill Venners
 * @author Chee Seng
 */
private[scalatest] object AnMatcher {

  /**
   * Factory method that creates a `AnMatcher[T]` from a
   * passed noun name and function of type `(T =&gt; MatchResult)`.
   *
   * @param name the noun name
   * @param fun the function of type `(T =&gt; MatchResult)`
   * @return `AnMatcher` instance that has the passed noun name and matches using the passed function
   * @author Bill Venners
   * @author Chee Seng
   */
  def apply[T](name: String)(fun: T => Boolean)(implicit ev: ClassTag[T]) =
    new AnMatcher[T] {
      val nounName = name
      def apply(left: T): MatchResult = 
        MatchResult(
          fun(left), 
          Resources.rawWasNotAn,
          Resources.rawWasAn,
          Vector(left, UnquotedString(nounName))
        )
      override def toString: String = "AnMatcher[" + ev.runtimeClass.getName + "](" + Prettifier.default(name) + ", " + ev.runtimeClass.getName + " => Boolean)"
    }
  
}
