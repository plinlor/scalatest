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

import scala.reflect.ClassTag

/**
 * Trait extended by matcher objects, which may appear after the word `be`, that can match a value of the specified type.
 * The value to match is passed to the `BeMatcher`'s `apply` method. The result is a `MatchResult`.
 * A `BeMatcher` is, therefore, a function from the specified type, `T`, to a `MatchResult`.
 *
 * Although `BeMatcher`
 * and `Matcher` represent very similar concepts, they have no inheritance relationship
 * because `Matcher` is intended for use right after `should` or `must`
 * whereas `BeMatcher` is intended for use right after `be`.
 * 
 *
 * As an example, you could create `BeMatcher[Int]`
 * called `odd` that would match any odd `Int`, and one called `even` that would match
 * any even `Int`. 
 * Given this pair of `BeMatcher`s, you could check whether an `Int` was odd or even with expressions like:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * num should be (odd)
 * num should not be (even)
 * }}}
 *
 * Here's is how you might define the odd and even `BeMatchers`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * trait CustomMatchers {
 *
 *   class OddMatcher extends BeMatcher[Int] {
 *     def apply(left: Int) =
 *       MatchResult(
 *         left % 2 == 1,
 *         left.toString + " was even",
 *         left.toString + " was odd"
 *       )
 *   }
 *   val odd = new OddMatcher
 *   val even = not (odd)
 * }
 *
 * // Make them easy to import with:
 * // import CustomMatchers._
 * object CustomMatchers extends CustomMatchers
 * }}}
 *
 * These `BeMatcher`s are defined inside a trait to make them easy to mix into any
 * suite or spec that needs them.
 * The `CustomMatchers` companion object exists to make it easy to bring the
 * `BeMatcher`s defined in this trait into scope via importing, instead of mixing in the trait. The ability
 * to import them is useful, for example, when you want to use the matchers defined in a trait in the Scala interpreter console.
 * 
 *
 * Here's an rather contrived example of how you might use `odd` and `even`: 
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class DoubleYourPleasureSuite extends FunSuite with MustMatchers with CustomMatchers {
 *
 *   def doubleYourPleasure(i: Int): Int = i * 2
 *
 *   test("The doubleYourPleasure method must return proper odd or even values")
 *
 *     val evenNum = 2
 *     evenNum must be (even)
 *     doubleYourPleasure(evenNum) must be (even)
 *
 *     val oddNum = 3
 *     oddNum must be (odd)
 *     doubleYourPleasure(oddNum) must be (odd) // This will fail
 *   }
 * }
 * }}}
 *
 * The last assertion in the above test will fail with this failure message:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * 6 was even
 * }}}
 *
 * For more information on `MatchResult` and the meaning of its fields, please
 * see the documentation for <a href="MatchResult.html">`MatchResult`</a>. To understand why `BeMatcher`
 * is contravariant in its type parameter, see the section entitled "Matcher's variance" in the
 * documentation for <a href="../Matcher.html">`Matcher`</a>.
 * 
 *
 * @author Bill Venners
*/
trait BeMatcher[-T] extends Function1[T, MatchResult] { thisBeMatcher =>

  /**
   * Check to see if the specified object, `left`, matches, and report the result in
   * the returned `MatchResult`. The parameter is named `left`, because it is
   * usually the value to the left of a `should` or `must` invocation. For example,
   * in:
   *
   * {{{  <!-- class="stHighlight" -->
   * num should be (odd)
   * }}}
   *
   * The `be (odd)` expression results in a regular <a href="../Matcher.html">`Matcher`</a> that holds
   * a reference to `odd`, the
   * `BeMatcher` passed to `be`. The `should` method invokes `apply`
   * on this matcher, passing in `num`, which is therefore the "`left`" value. The
   * matcher will pass `num` (the `left` value) to the `BeMatcher`'s `apply`
   * method.
   *
   * @param left the value against which to match
   * @return the `MatchResult` that represents the result of the match
   */
  def apply(left: T): MatchResult

  /**
   * Compose this `BeMatcher` with the passed function, returning a new `BeMatcher`.
   *
   * This method overrides `compose` on `Function1` to
   * return a more specific function type of `BeMatcher`. For example, given
   * an `odd` matcher defined like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val odd =
   *   new BeMatcher[Int] {
   *     def apply(left: Int) =
   *       MatchResult(
   *         left % 2 == 1,
   *         left.toString + " was even",
   *         left.toString + " was odd"
   *       )
   *   }
   * }}}
   *
   * You could use `odd` like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * 3 should be (odd)
   * 4 should not be (odd)
   * }}}
   *
   * If for some odd reason, you wanted a `BeMatcher[String]` that 
   * checked whether a string, when converted to an `Int`,
   * was odd, you could make one by composing `odd` with
   * a function that converts a string to an `Int`, like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val oddAsInt = odd compose { (s: String) => s.toInt }
   * }}}
   *
   * Now you have a `BeMatcher[String]` whose `apply` method first
   * invokes the converter function to convert the passed string to an `Int`,
   * then passes the resulting `Int` to `odd`. Thus, you could use
   * `oddAsInt` like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "3" should be (oddAsInt)
   * "4" should not be (oddAsInt)
   * }}}
   */
  override def compose[U](g: U => T): BeMatcher[U] =
    new BeMatcher[U] {
      def apply(u: U) = thisBeMatcher.apply(g(u))
    }
}

/**
 * Companion object for trait `BeMatcher` that provides a
 * factory method that creates a `BeMatcher[T]` from a
 * passed function of type `(T =&gt; MatchResult)`.
 *
 * @author Bill Venners
 */
object BeMatcher {

  /**
   * Factory method that creates a `BeMatcher[T]` from a
   * passed function of type `(T =&gt; MatchResult)`.
   *
   * @author Bill Venners
   */
  def apply[T](fun: T => MatchResult)(implicit ev: ClassTag[T]): BeMatcher[T] =
    new BeMatcher[T] {
      def apply(left: T) = fun(left)
      override def toString: String = "BeMatcher[" + ev.runtimeClass.getName + "](" + ev.runtimeClass.getName + " => MatchResult)"
    }
}

