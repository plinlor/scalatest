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

/**
 * Provides an implicit conversion on functions that ''produce'' `Matcher`s, ''i.e.'',
 * `T =&gt; Matcher[T]` that enables you to modify error messages when composing `Matcher`s.
 *
 * For an example of using `MatcherProducers`, see the <a href="Matcher.html#composingMatchers">Composing matchers</a>
 * section in the main documentation for trait `Matcher`.
 * 
 *
 * @author Bill Venners
 * @author Chee Seng
 */
trait MatcherProducers {

  /**
   * Class used via an implicit conversion that adds `composeTwice`, `mapResult`, and
   * `mapArgs` methods to functions that produce a `Matcher`.
   */
  class Composifier[T](f: T => Matcher[T]) {

    /**
     * Produces a new &ldquo;matcher producer&rdquo; function of type `U =&gt; Matcher[U]` from the
     * `T =&gt; Matcher[T]` (named `f`) passed to the `Composifier` constructor and the given
     * `T =&gt; U` transformation  function, `g`.
     *
     * The result of `composeTwice` is the result of the following function composition expression:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * (f compose g) andThen (_ compose g)
     * }}}
     * 
     * You would use `composeTwice` if you want to create a new matcher producer from an existing one, by transforming
     * both the left and right sides of the expression with the same transformation function. As an example, the
     * expression &ldquo;`be &gt; 7`&rdquo; produces a `Matcher[Int]`:
     * 
     *
     * {{{  <!-- class="stREPL" -->
     * scala&gt; import org.scalatest._
     * import org.scalatest._
     *
     * scala&gt; import Matchers._
     * import Matchers._
     *
     * scala&gt; val beGreaterThanSeven = be &gt; 7
     * beGreaterThanSeven: org.scalatest.matchers.Matcher[Int] = be &gt; 7
     * }}}
     *
     * Given this `Matcher[Int]`, you can now use it in a `should` expression like this:
     * 
     *
     * {{{  <!-- class="stREPL" -->
     * scala&gt; 8 should beGreaterThanSeven
     *
     * scala&gt; 6 should beGreaterThanSeven
     * org.scalatest.exceptions.TestFailedException: 6 was not greater than 7
     * ...
     * }}}
     *
     * You can create a more general &ldquo;matcher producer&rdquo; function like this:
     * 
     *
     * {{{  <!-- class="stREPL" -->
     * scala&gt; val beGreaterThan = { (i: Int) =&gt; be &gt; i }
     * beGreaterThan: Int =&gt; org.scalatest.matchers.Matcher[Int] = &lt;function1&gt;
     *
     * scala&gt; 8 should beGreaterThan (7)
     *
     * scala&gt; 8 should beGreaterThan (9)
     * org.scalatest.exceptions.TestFailedException: 8 was not greater than 9
     * }}}
     * 
     * Given `beGreaterThan` matcher producer function, you can create matcher producer function
     * that takes a `String` and produces a `Matcher[String]` given a function from
     * `Int =&gt; String` using `composeTwice`:
     * 
     *
     * {{{  <!-- class="stREPL" -->
     * scala&gt; val stringToInt = { (s: String) =&gt; s.toInt }
     * stringToInt: String =&gt; Int = &lt;function1&gt;
     *
     * scala&gt; val beAsIntsGreaterThan = beGreaterThan composeTwice stringToInt
     * beAsIntsGreaterThan: String =&gt; org.scalatest.matchers.Matcher[String] = &lt;function1&gt;
     *
     * scala&gt; "7" should beAsIntsGreaterThan ("6")
     *
     * scala&gt; "7" should beAsIntsGreaterThan ("8")
     * org.scalatest.exceptions.TestFailedException: 7 was not greater than 8
     * ...
     * }}}
     * 
     * The `composeTwice` method is just a shorthand for this function composition expression:
     * 
     *
     * {{{  <!-- class="stREPL" -->
     * scala&gt; val beAsIntsGreaterThan =
     *     (beGreaterThan compose stringToInt) andThen (_ compose stringToInt)
     * beAsIntsGreaterThan: String =&gt; org.scalatest.matchers.Matcher[String] = &lt;function1&gt;
     *
     * scala&gt; "7" should beAsIntsGreaterThan ("6")
     *
     * scala&gt; "7" should beAsIntsGreaterThan ("8")
     * org.scalatest.exceptions.TestFailedException: 7 was not greater than 8
     * }}}
     * 
     * The first part of that expression, `beGreaterThan` `compose` `stringToInt`,
     * gives you a new matcher producer function that given a `String` will produce a `Matcher[Int]`:
     * 
     *
     * {{{  <!-- class="stREPL" -->
     * scala&gt; val beAsIntGreaterThan = beGreaterThan compose stringToInt
     * beAsIntGreaterThan: String =&gt; org.scalatest.matchers.Matcher[Int] = &lt;function1&gt;
     * }}}
     * 
     * This `compose` method is inherited from `Function1`: on any `Function1`,
     * `(f` `compose` `g)(x)` means `f(g(x))`. You can use this
     * matcher producer like this:
     * 
     *
     * {{{  <!-- class="stREPL" -->
     * scala&gt; 7 should beAsIntGreaterThan ("6")
     *
     * scala&gt; 7 should beAsIntGreaterThan ("8")
     * org.scalatest.exceptions.TestFailedException: 7 was not greater than 8
     * }}}
     *
     * To get a matcher producer that will allow you to put a string on the right-hand-side, you'll need to transform
     * the `String` `=&gt;` `Matcher[Int]` to a `String` `=&gt;`
     * `Matcher[String]`. To accomplish this you can first just apply the function to get a `Matcher[Int]`,
     * like this:
     * 
     * 
     * {{{  <!-- class="stREPL" -->
     * scala&gt; val beGreaterThanEight = beAsIntGreaterThan ("8")
     * beGreaterThanEight: org.scalatest.matchers.Matcher[Int] = be &gt; 8
     *
     * scala&gt; 9 should beGreaterThanEight
     *
     * scala&gt; 7 should beGreaterThanEight
     * org.scalatest.exceptions.TestFailedException: 7 was not greater than 8
     * }}}
     *
     * To transform `beGreaterThanEight`, a `Matcher[Int]`, to a `Matcher[String]`,
     * you can again use `compose`. A ScalaTest `Matcher[T]` is a Scala function type `T`
     * `=&gt;` `MatchResult`. To get a `Matcher[String]` therefore, just call
     * `compose` on the `Matcher[Int]` and pass in a function from `String` `=&gt;`
     * `Int`:
     * 
     *
     * {{{  <!-- class="stREPL" -->
     * scala&gt; val beAsIntGreaterThanEight = beGreaterThanEight compose stringToInt
     * beAsIntGreaterThanEight: org.scalatest.matchers.Matcher[String] = &lt;function1&gt;
     * }}}
     *
     * After the second call to `compose`, therefore, you have what you want:
     * 
     *
     * {{{  <!-- class="stREPL" -->
     * scala&gt; "9" should beAsIntGreaterThanEight
     *
     * scala&gt; "7" should beAsIntGreaterThanEight
     * org.scalatest.exceptions.TestFailedException: 7 was not greater than 8
     * }}}
     *
     * So in summary, what the result of `(beGreaterThan` `compose` `stringToInt)` `andThen`
     * `(_` `compose` `stringToInt)` will do once it is applied to a (right-hand-side)
     * `String`, is:
     * 
     * 
     * <ol>
     * <li>Transform `beGreaterThan` from an `Int` `=&gt;` `Matcher[Int]`
     * to a `String` `=&gt;` `Matcher[Int]` with the first `compose`</li>
     * <li>Apply the given (right-hand-side) `String` to that to get a `Matcher[Int]` (the first part
     * of `andThen`'s behavior)</li>
     * <li>Pass the resulting `Matcher[Int]` to the given function,  `_` `compose`
     * `stringToInt`, which will transform the `Matcher[Int]` to a `Matcher[String]` (the
     * second part of the `andThen` behavior).</li>
     * </ol>
     */
    def composeTwice[U](g: U => T): U => Matcher[U] = (f compose g) andThen (_ compose g)

    /**
     * Returns a function that given a `T` will return a `Matcher[T]` that will produce the
     * `MatchResult` produced by `f` (passed to the `Composifier` constructor)
     * transformed by the given `prettify` function.
     *
     * @param prettify a function with which to transform `MatchResult`s.
     * @return a new `Matcher` producer function that produces prettified error messages
     */
    def mapResult(prettify: MatchResult => MatchResult): T => Matcher[T] =
      (o: T) => f(o) mapResult prettify

    /**
     * Returns a function that given a `T` will return a `Matcher[T]` that will produce the
     * `MatchResult` produced by `f` (passed to the `Composifier` constructor)
     * with arguments transformed by the given `prettify` function.
     *
     * @param prettify a function with which to transform the arguments of error messages.
     * @return a new `Matcher` producer function that produces prettified error messages
     */
    def mapArgs(prettify: Any => String): T => Matcher[T] =
      (o: T) => f(o) mapArgs prettify
  }

  import scala.language.implicitConversions

  /**
   * Implicit conversion that converts a function of `T =&gt; Matcher[T]` to an object that has
   * `composeTwice`, `mapResult` and `mapArgs` methods.
   *
   * The following shows how this trait is used to compose twice and modify error messages:
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest._
   * import matchers._
   * import MatcherProducers._
   *
   * val f = be &gt; (_: Int)
   * val g = (_: String).toInt
   *
   * // f composeTwice g means: (f compose g) andThen (_ compose g)
   * val beAsIntsGreaterThan =
   *   f composeTwice g mapResult { mr =&gt;
   *     mr.copy(
   *       failureMessageArgs =
   *         mr.failureMessageArgs.map((LazyArg(_) { "\"" + _.toString + "\".toInt"})),
   *       negatedFailureMessageArgs =
   *         mr.negatedFailureMessageArgs.map((LazyArg(_) { "\"" + _.toString + "\".toInt"})),
   *       midSentenceFailureMessageArgs =
   *         mr.midSentenceFailureMessageArgs.map((LazyArg(_) { "\"" + _.toString + "\".toInt"})),
   *       midSentenceNegatedFailureMessageArgs =
   *         mr.midSentenceNegatedFailureMessageArgs.map((LazyArg(_) { "\"" + _.toString + "\".toInt"}))
   *     )
   *   }
   *
   * "7" should beAsIntsGreaterThan ("8")
   * }}}
   *
   * The last assertion will fail with message like this:
   *
   * {{{  <!-- class="stHighlight" -->
   * "7".toInt was not greater than "8".toInt
   * }}}
   *
   * @param f a function that takes a `T` and return a `Matcher[T]`
   * @tparam T the type used by function `f`
   * @return an object that has `composeTwice`, `mapResult` and `mapArgs` methods.
   */
  implicit def convertToComposifier[T](f: T => Matcher[T]): Composifier[T] = new Composifier(f) 
}

/**
 * Companion object that facilitates the importing of `MatcherProducers` members as
 * an alternative to mixing it in. One use case is to import `MatcherProducers`'s members so you can use
 * `MatcherProducers` in the Scala interpreter.
 */
object MatcherProducers extends MatcherProducers

