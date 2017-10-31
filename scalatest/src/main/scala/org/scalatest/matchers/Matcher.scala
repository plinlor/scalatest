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

import org.scalactic._
import org.scalatest.enablers._
import org.scalatest.words._
import org.scalatest.FailureMessages
import org.scalatest.MatchersHelper.andMatchersAndApply
import org.scalatest.MatchersHelper.orMatchersAndApply
import org.scalatest.Resources
import scala.collection.GenTraversable
import scala.reflect.ClassTag
import scala.util.matching.Regex
import TripleEqualsSupport.Spread
import TripleEqualsSupport.TripleEqualsInvocation

/**
 * Trait extended by objects that can match a value of the specified type. The value to match is
 * passed to the matcher's `apply` method. The result is a `MatchResult`.
 * A matcher is, therefore, a function from the specified type, `T`, to a `MatchResult`.
 * <p> <!-- needed otherwise the heading below shows up in the wrong place. dumb scaladoc algo -->
 *
 * ==Creating custom matchers==
 * 
 * If none of the built-in matcher syntax satisfies a particular need you have, you can create
 * custom `Matcher`s that allow
 * you to place your own syntax directly after `should`. For example, although you can ensure that a `java.io.File` has a name
 * that ends with a particular extension like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * file.getName should endWith (".txt")
 * }}}
 * 
 * You might prefer 
 * to create a custom `Matcher[java.io.File]`
 * named `endWithExtension`, so you could write expressions like:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * file should endWithExtension ("txt")
 * file should not endWithExtension "txt"
 * file should (exist and endWithExtension ("txt"))
 * }}}
 * 
 * One good way to organize custom matchers is to place them inside one or more
 * traits that you can then mix into the suites that need them. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest._
 * import matchers._
 *
 * trait CustomMatchers {
 *
 *   class FileEndsWithExtensionMatcher(expectedExtension: String) extends Matcher[java.io.File] {
 *
 *     def apply(left: java.io.File) = {
 *       val name = left.getName
 *       MatchResult(
 *         name.endsWith(expectedExtension),
 *         s"""File $name did not end with extension "$expectedExtension"""",
 *         s"""File $name ended with extension "$expectedExtension""""
 *       )
 *     }
 *   }
 *
 *   def endWithExtension(expectedExtension: String) = new FileEndsWithExtensionMatcher(expectedExtension)
 * }
 *
 * // Make them easy to import with:
 * // import CustomMatchers._
 * object CustomMatchers extends CustomMatchers
 * }}}
 *
 * Note: the `CustomMatchers` companion object exists to make it easy to bring the
 * matchers defined in this trait into scope via importing, instead of mixing in the trait. The ability
 * to import them is useful, for example, when you want to use the matchers defined in a trait in the Scala interpreter console.
 * 
 *
 * This trait contains one matcher class, `FileEndsWithExtensionMatcher`, and a `def` named `endWithExtension` that returns a new
 * instance of `FileEndsWithExtensionMatcher`. Because the class extends `Matcher[java.io.File]`,
 * the compiler will only allow it be used to match against instances of `java.io.File`. A matcher must declare an
 * `apply` method that takes the type decared in `Matcher`'s type parameter, in this case `java.io.File`.
 * The apply method will return a `MatchResult` whose `matches` field will indicate whether the match succeeded.
 * The `failureMessage` field will provide a programmer-friendly error message indicating, in the event of a match failure, what caused
 * the match to fail. 
 * 
 *
 * The `FileEndsWithExtensionMatcher` matcher in this example determines success by determining if the passed `java.io.File` ends with
 * the desired extension. It does this in the first argument passed to the `MatchResult` factory method:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * name.endsWith(expectedExtension)
 * }}}
 *
 * In other words, if the file name has the expected extension, this matcher matches.
 * The next argument to `MatchResult`'s factory method produces the failure message string:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * s"""File $name did not end with extension "$expectedExtension"""",
 * }}}
 *
 * For example, consider this matcher expression:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest._
 * import Matchers._
 * import java.io.File
 * import CustomMatchers._
 * 
 * new File("essay.text") should endWithExtension ("txt")
 * }}}
 *
 * Because the passed `java.io.File` has the name `essay.text`, but the expected extension is `"txt"`, the failure
 * message would be:
 * 
 *
 * {{{
 * File essay.text did not have extension "txt"
 * }}}
 *
 * For more information on the fields in a `MatchResult`, including the subsequent field (or fields) that follow the failure message,
 * please see the documentation for <a href="MatchResult.html">`MatchResult`</a>.
 * 
 *
 * <a name="otherways"></a>
 * ==Creating dynamic matchers==
 *
 * There are other ways to create new matchers besides defining one as shown above. For example, you might check that a file is hidden like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * new File("secret.txt") should be ('hidden)
 * }}}
 *
 * If you wanted to get rid of the tick mark, you could simply define `hidden` like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val hidden = 'hidden
 * }}}
 *
 * Now you can check that an file is hidden without the tick mark:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * new File("secret.txt") should be (hidden)
 * }}}
 *
 * You could get rid of the parens with by using `shouldBe`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * new File("secret.txt") shouldBe hidden
 * }}}
 *
 * ==Creating matchers using logical operators==
 *
 * You can also use ScalaTest matchers' logical operators to combine existing matchers into new ones, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val beWithinTolerance = be &gt;= 0 and be &lt;= 10
 * }}}
 *
 * Now you could check that a number is within the tolerance (in this case, between 0 and 10, inclusive), like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * num should beWithinTolerance
 * }}}
 *
 * When defining a full blown matcher, one shorthand is to use one of the factory methods in `Matcher`'s companion
 * object. For example, instead of writing this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val beOdd =
 *   new Matcher[Int] {
 *     def apply(left: Int) =
 *       MatchResult(
 *         left % 2 == 1,
 *         left + " was not odd",
 *         left + " was odd"
 *       )
 *   }
 * }}}
 *
 * You could alternately write this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val beOdd =
 *   Matcher { (left: Int) =&gt;
 *     MatchResult(
 *       left % 2 == 1,
 *       left + " was not odd",
 *       left + " was odd"
 *     )
 *   }
 * }}}
 *
 * Either way you define the `beOdd` matcher, you could use it like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * 3 should beOdd
 * 4 should not (beOdd)
 * }}}
 *
 * <a name="composingMatchers"></a>
 * ==Composing matchers==
 *
 * You can also compose matchers. For example, the `endWithExtension` matcher from the example above
 * can be more easily created by composing a function with the existing `endWith` matcher:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; import Matchers._
 * import Matchers._
 *
 * scala&gt; import java.io.File
 * import java.io.File
 *
 * scala&gt; def endWithExtension(ext: String) = endWith(ext) compose { (f: File) =&gt; f.getPath }
 * endWithExtension: (ext: String)org.scalatest.matchers.Matcher[java.io.File]
 * }}}
 *
 * Now you have a `Matcher[File]` whose `apply` method first
 * invokes the converter function to convert the passed `File` to a `String`,
 * then passes the resulting `String` to `endWith`. Thus, you could use this version 
 * `endWithExtension` like the previous one:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; new File("output.txt") should endWithExtension("txt")
 * }}}
 *
 * In addition, by composing twice, you can modify the type of both sides of a match statement
 * with the same function, like this:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; val f = be &gt; (_: Int)
 * f: Int =&gt; org.scalatest.matchers.Matcher[Int] = &lt;function1&gt;
 *
 * scala&gt; val g = (_: String).toInt
 * g: String =&gt; Int = &lt;function1&gt;
 *
 * scala&gt; val beAsIntsGreaterThan = (f compose g) andThen (_ compose g)
 * beAsIntsGreaterThan: String =&gt; org.scalatest.matchers.Matcher[String] = &lt;function1&gt;
 *
 * scala&gt; "8" should beAsIntsGreaterThan ("7")
 * }}}
 *
 * At thsi point, however, the error message for the `beAsIntsGreaterThan`
 * gives no hint that the `Int`s being compared were parsed from `String`s:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; "7" should beAsIntsGreaterThan ("8")
 * org.scalatest.exceptions.TestFailedException: 7 was not greater than 8
 * }}}
 *
 * To modify error message, you can use trait <a href="MatcherProducers.html">`MatcherProducers`</a>, which
 * also provides a `composeTwice` method that performs the `compose` ...
 * `andThen` ... `compose` operation:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import matchers._
 * import matchers._
 *
 * scala&gt; import MatcherProducers._
 * import MatcherProducers._
 *
 * scala&gt; val beAsIntsGreaterThan = f composeTwice g // means: (f compose g) andThen (_ compose g)
 * beAsIntsGreaterThan: String =&gt; org.scalatest.matchers.Matcher[String] = &lt;function1&gt;
 *
 * scala&gt; "8" should beAsIntsGreaterThan ("7")
 * }}}
 *
 * Of course, the error messages is still the same:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; "7" should beAsIntsGreaterThan ("8")
 * org.scalatest.exceptions.TestFailedException: 7 was not greater than 8
 * }}}
 *
 * To modify the error messages, you can use `mapResult` from `MatcherProducers`. Here's an example:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; val beAsIntsGreaterThan =
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
 * beAsIntsGreaterThan: String =&gt; org.scalatest.matchers.Matcher[String] = &lt;function1&gt;
 * }}}
 *
 * The `mapResult` method takes a function that accepts a `MatchResult` and produces a new
 * `MatchResult`, which can contain modified arguments and modified error messages. In this example,
 * the error messages are being modified by wrapping the old arguments in <a href="LazyArg.html">`LazyArg`</a>
 * instances that lazily apply the given prettification functions to the `toString` result of the old args.
 * Now the error message is clearer:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; "7" should beAsIntsGreaterThan ("8")
 * org.scalatest.exceptions.TestFailedException: "7".toInt was not greater than "8".toInt
 * }}}
 *
 * ==Matcher's variance==
 *
 * `Matcher` is contravariant in its type parameter, `T`, to make its use more flexible.
 * As an example, consider the hierarchy:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class Fruit
 * class Orange extends Fruit
 * class ValenciaOrange extends Orange
 * }}}
 *
 * Given an orange:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val orange = Orange
 * }}}
 *
 * The expression "`orange should`" will, via an implicit conversion in `Matchers`,
 * result in an object that has a `should`
 * method that takes a `Matcher[Orange]`. If the static type of the matcher being passed to `should` is
 * `Matcher[Valencia]` it shouldn't (and won't) compile. The reason it shouldn't compile is that
 * the left value is an `Orange`, but not necessarily a `Valencia`, and a
 * `Matcher[Valencia]` only knows how to match against a `Valencia`. The reason
 * it won't compile is given that `Matcher` is contravariant in its type parameter, `T`, a
 * `Matcher[Valencia]` is ''not'' a subtype of `Matcher[Orange]`.
 * 
 *
 * By contrast, if the static type of the matcher being passed to `should` is `Matcher[Fruit]`,
 * it should (and will) compile. The reason it ''should'' compile is that given the left value is an `Orange`,
 * it is also a `Fruit`, and a `Matcher[Fruit]` knows how to match against `Fruit`s.
 * The reason it ''will'' compile is that given  that `Matcher` is contravariant in its type parameter, `T`, a
 * `Matcher[Fruit]` is indeed a subtype of `Matcher[Orange]`.
 * 
 *
 * @author Bill Venners
 */
trait Matcher[-T] extends Function1[T, MatchResult] { outerInstance =>

  /**
   * Check to see if the specified object, `left`, matches, and report the result in
   * the returned `MatchResult`. The parameter is named `left`, because it is
   * usually the value to the left of a `should` or `must` invocation. For example,
   * in:
   *
   * {{{  <!-- class="stHighlight" -->
   * list should equal (List(1, 2, 3))
   * }}}
   *
   * The `equal (List(1, 2, 3))` expression results in a matcher that holds a reference to the
   * right value, `List(1, 2, 3)`. The `should` method invokes `apply`
   * on this matcher, passing in `list`, which is therefore the "`left`" value. The
   * matcher will compare the `list` (the `left` value) with `List(1, 2, 3)` (the right
   * value), and report the result in the returned `MatchResult`.
   *
   * @param left the value against which to match
   * @return the `MatchResult` that represents the result of the match
   */
  def apply(left: T): MatchResult

  /**
   * Compose this matcher with the passed function, returning a new matcher.
   *
   * This method overrides `compose` on `Function1` to
   * return a more specific function type of `Matcher`. For example, given
   * a `beOdd` matcher defined like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val beOdd =
   *   new Matcher[Int] {
   *     def apply(left: Int) =
   *       MatchResult(
   *         left % 2 == 1,
   *         left + " was not odd",
   *         left + " was odd"
   *       )
   *   }
   * }}}
   *
   * You could use `beOdd` like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * 3 should beOdd
   * 4 should not (beOdd)
   * }}}
   *
   * If for some odd reason, you wanted a `Matcher[String]` that 
   * checked whether a string, when converted to an `Int`,
   * was odd, you could make one by composing `beOdd` with
   * a function that converts a string to an `Int`, like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val beOddAsInt = beOdd compose { (s: String) => s.toInt }
   * }}}
   *
   * Now you have a `Matcher[String]` whose `apply` method first
   * invokes the converter function to convert the passed string to an `Int`,
   * then passes the resulting `Int` to `beOdd`. Thus, you could use
   * `beOddAsInt` like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "3" should beOddAsInt
   * "4" should not (beOddAsInt)
   * }}}
   */
  override def compose[U](g: U => T): Matcher[U] =
    new Matcher[U] {
      def apply(u: U) = outerInstance.apply(g(u))
    }

// TODO: mention not short circuited, and the precendence is even between and and or

  /**
   * Returns a matcher whose `apply` method returns a `MatchResult`
   * that represents the logical-and of the results of the wrapped and the passed matcher applied to
   * the same value.
   *
   * The reason `and` has an upper bound on its type parameter is so that the `Matcher`
   * resulting from an invocation of `and` will have the correct type parameter. If you call
   * `and` on a `Matcher[Orange]`, passing in a `Matcher[Valencia]`,
   * the result will have type `Matcher[Valencia]`. This is correct because both a
   * `Matcher[Orange]` and a `Matcher[Valencia]` know how to match a
   * `Valencia` (but a `Matcher[Valencia]` doesn't know how to
   * match any old `Orange`).  If you call
   * `and` on a `Matcher[Orange]`, passing in a `Matcher[Fruit]`,
   * the result will have type `Matcher[Orange]`. This is also correct because both a
   * `Matcher[Orange]` and a `Matcher[Fruit]` know how to match an
   * `Orange` (but a `Matcher[Orange]` doesn't know how to
   * match any old `Fruit`).
   * 
   *
   * @param the matcher to logical-and with this matcher
   * @return a matcher that performs the logical-and of this and the passed matcher
   */
  def and[U <: T](rightMatcher: Matcher[U]): Matcher[U] =
    new Matcher[U] {
      def apply(left: U): MatchResult = {
        andMatchersAndApply(left, outerInstance, rightMatcher)
      }
      override def toString: String = "(" + Prettifier.default(outerInstance) + ") and (" + Prettifier.default(rightMatcher) + ")"
    }

  import scala.language.higherKinds

  /**
   * Returns a `MatcherFactory` whose `matcher` method returns a `Matcher`,
   * which has `apply` method that returns a `MatchResult` that represents the logical-and
   * of the results of the wrapped and the passed `MatcherFactory` applied to the same value.
   *
   * @param rightMatcherFactory1 the `MatcherFactory` to logical-and with this `MatcherFactory`
   * @return a `MatcherFactory` that performs the logical-and of this and the passed `MatcherFactory`
   */
  def and[U, TC1[_]](rightMatcherFactory1: MatcherFactory1[U, TC1]): MatcherFactory1[T with U, TC1] =
    new MatcherFactory1[T with U, TC1] {
      def matcher[V <: T with U : TC1]: Matcher[V] = {
        new Matcher[V] {
          def apply(left: V): MatchResult = {
            val rightMatcher = rightMatcherFactory1.matcher
            andMatchersAndApply(left, outerInstance, rightMatcher)
          }
        }
      }
      override def toString: String = "(" + Prettifier.default(outerInstance) + ") and (" + Prettifier.default(rightMatcherFactory1) + ")"
    }

  /**
   * Returns a matcher whose `apply` method returns a `MatchResult`
   * that represents the logical-or of the results of this and the passed matcher applied to
   * the same value.
   *
   * The reason `or` has an upper bound on its type parameter is so that the `Matcher`
   * resulting from an invocation of `or` will have the correct type parameter. If you call
   * `or` on a `Matcher[Orange]`, passing in a `Matcher[Valencia]`,
   * the result will have type `Matcher[Valencia]`. This is correct because both a
   * `Matcher[Orange]` and a `Matcher[Valencia]` know how to match a
   * `Valencia` (but a `Matcher[Valencia]` doesn't know how to
   * match any old `Orange`).  If you call
   * `or` on a `Matcher[Orange]`, passing in a `Matcher[Fruit]`,
   * the result will have type `Matcher[Orange]`. This is also correct because both a
   * `Matcher[Orange]` and a `Matcher[Fruit]` know how to match an
   * `Orange` (but a `Matcher[Orange]` doesn't know how to
   * match any old `Fruit`).
   * 
   *
   * @param rightMatcher the matcher to logical-or with this matcher
   * @return a matcher that performs the logical-or of this and the passed matcher
   */
  def or[U <: T](rightMatcher: Matcher[U]): Matcher[U] =
    new Matcher[U] {
      def apply(left: U): MatchResult = {
        orMatchersAndApply(left, outerInstance, rightMatcher)
      }
      override def toString: String = "(" + Prettifier.default(outerInstance) + ") or (" + Prettifier.default(rightMatcher) + ")"
    }

  /**
   * Returns a `MatcherFactory` whose `matcher` method returns a `Matcher`,
   * which has `apply` method that returns a `MatchResult` that represents the logical-or
   * of the results of the wrapped and the passed `MatcherFactory` applied to the same value.
   *
   * @param rightMatcherFactory1 the `MatcherFactory` to logical-or with this `MatcherFactory`
   * @return a `MatcherFactory` that performs the logical-or of this and the passed `MatcherFactory`
   */
  def or[U, TC1[_]](rightMatcherFactory1: MatcherFactory1[U, TC1]): MatcherFactory1[T with U, TC1] =
    new MatcherFactory1[T with U, TC1] {
      def matcher[V <: T with U : TC1]: Matcher[V] = {
        new Matcher[V] {
          def apply(left: V): MatchResult = {
            val rightMatcher = rightMatcherFactory1.matcher
            orMatchersAndApply(left, outerInstance, rightMatcher)
          }
          override def toString: String = "(" + Prettifier.default(outerInstance) + ") or (" + Prettifier.default(rightMatcherFactory1) + ")"
        }
      }
      override def toString: String = "(" + Prettifier.default(outerInstance) + ") or (" + Prettifier.default(rightMatcherFactory1) + ")"
    }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AndHaveWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and have length (3 - 1)
     *                   ^
     * }}}
     **/
    def length(expectedLength: Long): MatcherFactory1[T, Length] = and(MatcherWords.have.length(expectedLength))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and have size (3 - 1)
     *                                 ^
     * }}}
     **/
    def size(expectedSize: Long): MatcherFactory1[T, Size] = and(MatcherWords.have.size(expectedSize))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and have message ("A message from Mars")
     *                   ^
     * }}}
     **/
    def message(expectedMessage: String): MatcherFactory1[T, Messaging] = and(MatcherWords.have.message(expectedMessage))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and have size (3 - 1)
   *          ^
   * }}}
   **/
  def and(haveWord: HaveWord): AndHaveWord = new AndHaveWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AndContainWord(prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain (3 - 1)
     *                      ^
     * }}}
     **/
    def apply[U](expectedElement: Any): MatcherFactory1[T with U, Containing] = outerInstance.and(MatcherWords.contain(expectedElement))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain key ("one")
     *                      ^
     * }}}
     **/
    def key(expectedKey: Any): MatcherFactory1[T, KeyMapping] = outerInstance.and(MatcherWords.contain.key(expectedKey))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain value (1)
     *                      ^
     * }}}
     **/
    def value(expectedValue: Any): MatcherFactory1[T, ValueMapping] = outerInstance.and(MatcherWords.contain.value(expectedValue))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain theSameElementsAs List(1, 2, 3)
     *                      ^
     * }}}
     **/
    def theSameElementsAs(right: GenTraversable[_]): MatcherFactory1[T, Aggregating] = 
      outerInstance.and(MatcherWords.contain.theSameElementsAs(right))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain theSameElementsInOrderAs List(1, 2, 3)
     *                      ^
     * }}}
     **/
    def theSameElementsInOrderAs(right: GenTraversable[_]): MatcherFactory1[T, Sequencing] = 
      outerInstance.and(MatcherWords.contain.theSameElementsInOrderAs(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain inOrderOnly (1, 2, 3)
     *                      ^
     * }}}
     **/
    def inOrderOnly(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Sequencing] =
      outerInstance.and(MatcherWords.contain.inOrderOnly(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain allOf (1, 2, 3)
     *                      ^
     * }}}
     **/
    def allOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.contain.allOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain allElementsOf List(1, 2, 3)
     *                      ^
     * }}}
     **/
    def allElementsOf(elements: GenTraversable[Any]): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.contain.allElementsOf(elements))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain inOrder (1, 2, 3)
     *                      ^
     * }}}
     **/
    def inOrder(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Sequencing] =
      outerInstance.and(MatcherWords.contain.inOrder(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain inOrderElementsOf List(1, 2, 3)
     *                      ^
     * }}}
     **/
    def inOrderElementsOf(elements: GenTraversable[Any]): MatcherFactory1[T, Sequencing] =
      outerInstance.and(MatcherWords.contain.inOrderElementsOf(elements))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain oneOf (1, 2, 3)
     *                      ^
     * }}}
     **/
    def oneOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Containing] =
      outerInstance.and(MatcherWords.contain.oneOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain oneElementOf List(1, 2, 3)
     *                      ^
     * }}}
     **/
    def oneElementOf(elements: GenTraversable[Any]): MatcherFactory1[T, Containing] =
      outerInstance.and(MatcherWords.contain.oneElementOf(elements))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain atLeastOneOf (1, 2, 3)
     *                      ^
     * }}}
     **/
    def atLeastOneOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.contain.atLeastOneOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain atLeastOneElementOf (1, 2, 3)
     *                      ^
     * }}}
     **/
    def atLeastOneElementOf(elements: GenTraversable[Any]): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.contain.atLeastOneElementOf(elements))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain only (1, 2, 3)
     *                      ^
     * }}}
     **/
    def only(right: Any*): MatcherFactory1[T, Aggregating] = 
      outerInstance.and(MatcherWords.contain.only(right.toList: _*)(prettifier, pos))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain noneOf (1, 2, 3)
     *                      ^
     * }}}
     **/
    def noneOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Containing] =
      outerInstance.and(MatcherWords.contain.noneOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain noElementsOf (1, 2, 3)
     *                      ^
     * }}}
     **/
    def noElementsOf(elements: GenTraversable[Any]): MatcherFactory1[T, Containing] =
      outerInstance.and(MatcherWords.contain.noElementsOf(elements))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain atMostOneOf (1, 2, 3)
     *                      ^
     * }}}
     **/
    def atMostOneOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.contain.atMostOneOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and contain atMostOneElementOf List(1, 2, 3)
     *                      ^
     * }}}
     **/
    def atMostOneElementOf(elements: GenTraversable[Any]): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.contain.atMostOneElementOf(elements))
  }
  
  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and contain key ("one")
   *          ^
   * }}}
   **/
  def and(containWord: ContainWord)(implicit prettifier: Prettifier, pos: source.Position): AndContainWord = new AndContainWord(prettifier, pos)

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AndBeWord {

    // SKIP-SCALATESTJS-START
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and be a ('file)
     *                 ^
     * }}}
     **/
    def a(symbol: Symbol): Matcher[T with AnyRef] = and(MatcherWords.be.a(symbol))
    // SKIP-SCALATESTJS-END

    /**
     * This method enables the following syntax, where `file` is a <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and be a (file)
     *                 ^
     * }}}
     **/
    def a[U](bePropertyMatcher: BePropertyMatcher[U]): Matcher[T with AnyRef with U] = and(MatcherWords.be.a(bePropertyMatcher))

    /**
     * This method enables the following syntax, where `positiveNumber` and `validNumber` are <a href="AMatcher.html">`AMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and be a (validNumber)
     *                 ^
     * }}}
     **/
    def a[U](aMatcher: AMatcher[U]): Matcher[T with U] = and(MatcherWords.be.a(aMatcher))

    // SKIP-SCALATESTJS-START
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and be an ('apple)
     *                 ^
     * }}}
     **/
    def an(symbol: Symbol): Matcher[T with AnyRef] = and(MatcherWords.be.an(symbol))
    // SKIP-SCALATESTJS-END

    /**
     * This method enables the following syntax, where `apple` is a <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and be an (apple)
     *                 ^
     * }}}
     **/
    def an[U](bePropertyMatcher: BePropertyMatcher[U]): Matcher[T with AnyRef with U] = and(MatcherWords.be.an(bePropertyMatcher))
    
    /**
     * This method enables the following syntax, where `integerNumber` is an <a href="AnMatcher.html">`AnMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and be an (integerNumber)
     *                 ^
     * }}}
     **/
    def an[U](anMatcher: AnMatcher[U]): Matcher[T with U] = and(MatcherWords.be.an(anMatcher))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and be theSameInstanceAs (string)
     *                 ^
     * }}}
     **/
    def theSameInstanceAs(anyRef: AnyRef): Matcher[T with AnyRef] = and(MatcherWords.be.theSameInstanceAs(anyRef))
    
    /**
     * This method enables the following syntax, where `fraction` refers to a `PartialFunction`:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and be definedAt (8)
     *                 ^
     * }}}
     **/
    def definedAt[A, U <: PartialFunction[A, _]](right: A): Matcher[T with U] = and(MatcherWords.be.definedAt(right))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and be a ('file)
   *          ^
   * }}}
   **/
  def and(beWord: BeWord): AndBeWord = new AndBeWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AndFullyMatchWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and fullyMatch regex (decimal)
     *                         ^
     * }}}
     **/
    def regex(regexString: String): Matcher[T with String] = and(MatcherWords.fullyMatch.regex(regexString))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and fullyMatch regex ("a(b*)c" withGroup "bb")
     *                         ^
     * }}}
     **/
    def regex(regexWithGroups: RegexWithGroups): Matcher[T with String] = and(MatcherWords.fullyMatch.regex(regexWithGroups))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and fullyMatch regex (decimalRegex)
     *                         ^
     * }}}
     **/
    def regex(regex: Regex): Matcher[T with String] = and(MatcherWords.fullyMatch.regex(regex))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and fullyMatch regex (decimalRegex)
   *          ^
   * }}}
   **/
  def and(fullyMatchWord: FullyMatchWord): AndFullyMatchWord = new AndFullyMatchWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AndIncludeWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and include regex (decimal)
     *                      ^
     * }}}
     **/
    def regex(regexString: String): Matcher[T with String] = and(MatcherWords.include.regex(regexString))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and include regex ("a(b*)c" withGroup "bb")
     *                      ^
     * }}}
     **/
    def regex(regexWithGroups: RegexWithGroups): Matcher[T with String] = and(MatcherWords.include.regex(regexWithGroups))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and include regex (decimalRegex)
     *                      ^
     * }}}
     **/
    def regex(regex: Regex): Matcher[T with String] = and(MatcherWords.include.regex(regex))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and include regex ("wor.d")
   *          ^
   * }}}
   **/
  def and(includeWord: IncludeWord): AndIncludeWord = new AndIncludeWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AndStartWithWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and startWith regex (decimal)
     *                        ^
     * }}}
     **/
    def regex(regexString: String): Matcher[T with String] = and(MatcherWords.startWith.regex(regexString))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and startWith regex ("a(b*)c" withGroup "bb")
     *                        ^
     * }}}
     **/
    def regex(regexWithGroups: RegexWithGroups): Matcher[T with String] = and(MatcherWords.startWith.regex(regexWithGroups))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and startWith regex (decimalRegex)
     *                        ^
     * }}}
     **/
    def regex(regex: Regex): Matcher[T with String] = and(MatcherWords.startWith.regex(regex))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and startWith regex ("1.7")
   *          ^
   * }}}
   **/
  def and(startWithWord: StartWithWord): AndStartWithWord = new AndStartWithWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AndEndWithWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and endWith regex (decimal)
     *                      ^
     * }}}
     **/
    def regex(regexString: String): Matcher[T with String] = and(MatcherWords.endWith.regex(regexString))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and endWith regex ("a(b*)c" withGroup "bb")
     *                      ^
     * }}}
     **/
    def regex(regexWithGroups: RegexWithGroups): Matcher[T with String] = and(MatcherWords.endWith.regex(regexWithGroups))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and endWith regex (decimalRegex)
     *                      ^
     * }}}
     **/
    def regex(regex: Regex): Matcher[T with String] = and(MatcherWords.endWith.regex(regex))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and endWith regex (decimalRegex)
   *          ^
   * }}}
   **/
  def and(endWithWord: EndWithWord): AndEndWithWord = new AndEndWithWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AndNotWord {

    /**
     * Get the `Matcher` instance, currently used by macro only.
     */
    val owner = outerInstance

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not equal (3 - 1)
     *                  ^
     * }}}
     **/
    def equal(any: Any): MatcherFactory1[T, Equality] =
      outerInstance.and(MatcherWords.not.apply(MatcherWords.equal(any)))

    /**
     * This method enables the following syntax, for the "primitive" numeric types:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not equal (17.0 +- 0.2)
     *                  ^
     * }}}
     **/
    def equal[U](spread: Spread[U]): Matcher[T with U] = outerInstance.and(MatcherWords.not.equal(spread))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not equal (null)
     *                  ^
     * }}}
     **/
    def equal(o: Null): Matcher[T] = {
      outerInstance and {
        new Matcher[T] {
          def apply(left: T): MatchResult = {
            MatchResult(
              left != null,
              Resources.rawEqualedNull,
              Resources.rawDidNotEqualNull,
              Resources.rawMidSentenceEqualedNull,
              Resources.rawDidNotEqualNull,
              Vector.empty, 
              Vector(left)
            )
          }
          override def toString: String = "not equal null"
        }
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be (3 - 1)
     *                  ^
     * }}}
     **/
    def be(any: Any): Matcher[T] =
      outerInstance.and(MatcherWords.not.apply(MatcherWords.be(any)))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not have length (3)
     *                  ^
     * }}}
     **/
    def have(resultOfLengthWordApplication: ResultOfLengthWordApplication): MatcherFactory1[T, Length] =
      outerInstance.and(MatcherWords.not.apply(MatcherWords.have.length(resultOfLengthWordApplication.expectedLength)))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not have size (3)
     *                  ^
     * }}}
     **/
    def have(resultOfSizeWordApplication: ResultOfSizeWordApplication): MatcherFactory1[T, Size] =
      outerInstance.and(MatcherWords.not.apply(MatcherWords.have.size(resultOfSizeWordApplication.expectedSize)))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not have message ("Message from Mars!")
     *                  ^
     * }}}
     **/
    def have(resultOfMessageWordApplication: ResultOfMessageWordApplication): MatcherFactory1[T, Messaging] =
      outerInstance.and(MatcherWords.not.apply(MatcherWords.have.message(resultOfMessageWordApplication.expectedMessage)))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not have (author ("Melville"))
     *                  ^
     * }}}
     **/
    def have[U](firstPropertyMatcher: HavePropertyMatcher[U, _], propertyMatchers: HavePropertyMatcher[U, _]*): Matcher[T with U] =
      outerInstance.and(MatcherWords.not.apply(MatcherWords.have(firstPropertyMatcher, propertyMatchers: _*)))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be &lt; (6)
     *                  ^
     * }}}
     **/
    def be[U](resultOfLessThanComparison: ResultOfLessThanComparison[U]): Matcher[T with U] =
      outerInstance.and(MatcherWords.not.be(resultOfLessThanComparison))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be (null)
     *                  ^
     * }}}
     **/
    def be(o: Null): Matcher[T with AnyRef] = outerInstance.and(MatcherWords.not.be(o))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be &gt; (6)
     *                  ^
     * }}}
     **/
    def be[U](resultOfGreaterThanComparison: ResultOfGreaterThanComparison[U]): Matcher[T with U] =
      outerInstance.and(MatcherWords.not.be(resultOfGreaterThanComparison))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be &lt;= (2)
     *                  ^
     * }}}
     **/
    def be[U](resultOfLessThanOrEqualToComparison: ResultOfLessThanOrEqualToComparison[U]): Matcher[T with U] =
      outerInstance.and(MatcherWords.not.be(resultOfLessThanOrEqualToComparison))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be &gt;= (6)
     *                  ^
     * }}}
     **/
    def be[U](resultOfGreaterThanOrEqualToComparison: ResultOfGreaterThanOrEqualToComparison[U]): Matcher[T with U] =
      outerInstance.and(MatcherWords.not.be(resultOfGreaterThanOrEqualToComparison))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be === (6)
     *                  ^
     * }}}
     **/
    def be(tripleEqualsInvocation: TripleEqualsInvocation[_]): Matcher[T] =
      outerInstance.and(MatcherWords.not.be(tripleEqualsInvocation))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be ('empty)
     *                  ^
     * }}}
     **/
    def be(symbol: Symbol): Matcher[T with AnyRef] = outerInstance.and(MatcherWords.not.be(symbol))

    /**
     * This method enables the following syntax, where `odd` is a <a href="BeMatcher.html">`BeMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be (odd)
     *                  ^
     * }}}
     **/
    def be[U](beMatcher: BeMatcher[U]): Matcher[T with U] = outerInstance.and(MatcherWords.not.be(beMatcher))

    /**
     * This method enables the following syntax, where `directory` is a <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be (directory)
     *                  ^
     * }}}
     **/
    def be[U](bePropertyMatcher: BePropertyMatcher[U]): Matcher[T with AnyRef with U] = outerInstance.and(MatcherWords.not.be(bePropertyMatcher))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be a ('file)
     *                  ^
     * }}}
     **/
    def be(resultOfAWordApplication: ResultOfAWordToSymbolApplication): Matcher[T with AnyRef] = outerInstance.and(MatcherWords.not.be(resultOfAWordApplication))

    /**
     * This method enables the following syntax, where `validMarks` is an <a href="AMatcher.html">`AMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be a (validMarks)
     *                  ^
     * }}}
     **/
    def be[U](resultOfAWordApplication: ResultOfAWordToAMatcherApplication[U]): Matcher[T with U] = outerInstance.and(MatcherWords.not.be(resultOfAWordApplication))
    
    /**
     * This method enables the following syntax, where `directory` is a <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be a (directory)
     *                  ^
     * }}}
     **/
    def be[U <: AnyRef](resultOfAWordApplication: ResultOfAWordToBePropertyMatcherApplication[U]): Matcher[T with U] = outerInstance.and(MatcherWords.not.be(resultOfAWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be an ('apple)
     *                  ^
     * }}}
     **/
    def be(resultOfAnWordApplication: ResultOfAnWordToSymbolApplication): Matcher[T with AnyRef] = outerInstance.and(MatcherWords.not.be(resultOfAnWordApplication))

    /**
     * This method enables the following syntax, where `directory` is a <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be an (directory)
     *                  ^
     * }}}
     **/
    def be[T <: AnyRef](resultOfAnWordApplication: ResultOfAnWordToBePropertyMatcherApplication[T]) = outerInstance.and(MatcherWords.not.be(resultOfAnWordApplication))

    /**
     * This method enables the following syntax, where `invalidMarks` is an <a href="AnMatcher.html">`AnMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be an (invalidMarks)
     *                  ^
     * }}}
     **/
    def be[U](resultOfAnWordApplication: ResultOfAnWordToAnMatcherApplication[U]): Matcher[T with U] = outerInstance.and(MatcherWords.not.be(resultOfAnWordApplication))

    import language.experimental.macros

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be a [Book]
     *                  ^
     * }}}
     **/
    def be(aType: ResultOfATypeInvocation[_]): Matcher[T] = macro TypeMatcherMacro.andNotATypeMatcher
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be an [Apple]
     *                  ^
     * }}}
     **/
    def be(anType: ResultOfAnTypeInvocation[_]): Matcher[T] = macro TypeMatcherMacro.andNotAnTypeMatcher
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be theSameInstanceAs (otherString)
     *                  ^
     * }}}
     **/
    def be(resultOfTheSameInstanceAsApplication: ResultOfTheSameInstanceAsApplication): Matcher[T with AnyRef] = outerInstance.and(MatcherWords.not.be(resultOfTheSameInstanceAsApplication))

    /**
     * This method enables the following syntax, for the "primitive" numeric types:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be (17.0 +- 0.2)
     *                  ^
     * }}}
     **/
    def be[U](spread: Spread[U]): Matcher[T with U] = outerInstance.and(MatcherWords.not.be(spread))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be definedAt (8)
     *                  ^
     * }}}
     **/
    def be[A, U <: PartialFunction[A, _]](resultOfDefinedAt: ResultOfDefinedAt[A]): Matcher[T with U] =
      outerInstance.and(MatcherWords.not.be(resultOfDefinedAt))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be sorted
     *                  ^
     * }}}
     **/
    def be(sortedWord: SortedWord) = 
      outerInstance.and(MatcherWords.not.be(sortedWord))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be readable
     *                  ^
     * }}}
     **/
    def be(readableWord: ReadableWord) = 
      outerInstance.and(MatcherWords.not.be(readableWord))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be writable
     *                  ^
     * }}}
     **/
    def be(writableWord: WritableWord) = 
      outerInstance.and(MatcherWords.not.be(writableWord))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be empty
     *                  ^
     * }}}
     **/
    def be(emptyWord: EmptyWord) = 
      outerInstance.and(MatcherWords.not.be(emptyWord))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be defined
     *                  ^
     * }}}
     **/
    def be(definedWord: DefinedWord) = 
      outerInstance.and(MatcherWords.not.be(definedWord))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not fullyMatch regex (decimal)
     *                  ^
     * }}}
     **/
    def fullyMatch(resultOfRegexWordApplication: ResultOfRegexWordApplication): Matcher[T with String] =
      outerInstance.and(MatcherWords.not.fullyMatch(resultOfRegexWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not include regex (decimal)
     *                  ^
     * }}}
     **/
    def include(resultOfRegexWordApplication: ResultOfRegexWordApplication): Matcher[T with String] =
      outerInstance.and(MatcherWords.not.include(resultOfRegexWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not include ("1.7")
     *                  ^
     * }}}
     **/
    def include(expectedSubstring: String): Matcher[T with String] =
      outerInstance.and(MatcherWords.not.include(expectedSubstring))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not startWith regex (decimal)
     *                  ^
     * }}}
     **/
    def startWith(resultOfRegexWordApplication: ResultOfRegexWordApplication): Matcher[T with String] =
      outerInstance.and(MatcherWords.not.startWith(resultOfRegexWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not startWith ("1.7")
     *                  ^
     * }}}
     **/
    def startWith(expectedSubstring: String): Matcher[T with String] =
      outerInstance.and(MatcherWords.not.startWith(expectedSubstring))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not endWith regex (decimal)
     *                  ^
     * }}}
     **/
    def endWith(resultOfRegexWordApplication: ResultOfRegexWordApplication): Matcher[T with String] =
      outerInstance.and(MatcherWords.not.endWith(resultOfRegexWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not endWith ("1.7")
     *                  ^
     * }}}
     **/
    def endWith(expectedSubstring: String): Matcher[T with String] =
      outerInstance.and(MatcherWords.not.endWith(expectedSubstring))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain (3)
     *                  ^
     * }}}
     **/
    def contain[U](expectedElement: U): MatcherFactory1[T, Containing] =
      outerInstance.and(MatcherWords.not.contain(expectedElement))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain oneOf (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfOneOfApplication): MatcherFactory1[T, Containing] =
      outerInstance.and(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain oneElementOf (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfOneElementOfApplication): MatcherFactory1[T, Containing] =
      outerInstance.and(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain atLeastOneOf (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfAtLeastOneOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain atLeastOneElementOf (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfAtLeastOneElementOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain noneOf (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfNoneOfApplication): MatcherFactory1[T, Containing] =
      outerInstance.and(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain noElementsOf (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfNoElementsOfApplication): MatcherFactory1[T, Containing] =
      outerInstance.and(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain theSameElementsAs (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfTheSameElementsAsApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain theSameElementsInOrderAs (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfTheSameElementsInOrderAsApplication): MatcherFactory1[T, Sequencing] =
      outerInstance.and(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain only (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfOnlyApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain inOrderOnly (8, 1, 2)
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfInOrderOnlyApplication): MatcherFactory1[T, Sequencing] =
      outerInstance.and(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain allOf (8, 1, 2)
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfAllOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain allElementsOf (8, 1, 2)
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfAllElementsOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain inOrder (8, 1, 2)
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfInOrderApplication): MatcherFactory1[T, Sequencing] =
      outerInstance.and(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain inOrderElementsOf (List(8, 1, 2))
     *                  ^
     * }}}
     **/
    def contain(right: ResultOfInOrderElementsOfApplication): MatcherFactory1[T, Sequencing] =
      outerInstance.and(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain atMostOneOf (8, 1, 2)
     *                          ^
     * }}}
     **/
    def contain(right: ResultOfAtMostOneOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain atMostOneOf (List(8, 1, 2))
     *                          ^
     * }}}
     **/
    def contain(right: ResultOfAtMostOneElementOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.and(MatcherWords.not.contain(right))

// TODO: Write tests and impl for contain ResultOfKey/ValueWordApplication
    /**
     * This method enables the following syntax given a `Matcher`:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain key ("three")
     *                  ^
     * }}}
     **/
    def contain(resultOfKeyWordApplication: ResultOfKeyWordApplication): MatcherFactory1[T, KeyMapping] =
      outerInstance.and(MatcherWords.not.contain(resultOfKeyWordApplication))

    /**
     * This method enables the following syntax given a `Matcher`:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not contain value (3)
     *                  ^
     * }}}
     **/
    def contain(resultOfValueWordApplication: ResultOfValueWordApplication): MatcherFactory1[T, ValueMapping] =
      outerInstance.and(MatcherWords.not.contain(resultOfValueWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not matchPattern { case Person("Bob", _) =>}
     *                  ^
     * }}}
     **/
    def matchPattern(right: PartialFunction[Any, _]) = macro MatchPatternMacro.andNotMatchPatternMatcher
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and not contain value (3)
   *          ^
   * }}}
   **/
  def and(notWord: NotWord): AndNotWord = new AndNotWord
  
  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and exist
   *          ^
   * }}}
   **/
  def and(existWord: ExistWord): MatcherFactory1[T, Existence] = 
    outerInstance.and(MatcherWords.exist.matcherFactory)
    
  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher and not (exist)
   *          ^
   * }}}
   **/
  def and(notExist: ResultOfNotExist): MatcherFactory1[T, Existence] = 
    outerInstance.and(MatcherWords.not.exist)

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class OrHaveWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or have length (3 - 1)
     *                  ^
     * }}}
     **/
    def length(expectedLength: Long): MatcherFactory1[T, Length] = or(MatcherWords.have.length(expectedLength))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or have size (3 - 1)
     *                  ^
     * }}}
     **/
    def size(expectedSize: Long): MatcherFactory1[T, Size] = or(MatcherWords.have.size(expectedSize))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or have message ("Message from Mars!")
     *                  ^
     * }}}
     **/
    def message(expectedMessage: String): MatcherFactory1[T, Messaging] = or(MatcherWords.have.message(expectedMessage))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or have size (3 - 1)
   *          ^
   * }}}
   **/
  def or(haveWord: HaveWord): OrHaveWord = new OrHaveWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class OrContainWord(prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain (3 - 1)
     *                     ^
     * }}}
     **/
    def apply[U](expectedElement: Any): MatcherFactory1[T with U, Containing] = outerInstance.or(MatcherWords.contain(expectedElement))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain key ("one")
     *                     ^
     * }}}
     **/
    def key(expectedKey: Any): MatcherFactory1[T, KeyMapping] = outerInstance.or(MatcherWords.contain.key(expectedKey))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain value (1)
     *                     ^
     * }}}
     **/
    def value(expectedValue: Any): MatcherFactory1[T, ValueMapping] = outerInstance.or(MatcherWords.contain.value(expectedValue))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain theSameElementsAs List(1, 2, 3)
     *                     ^
     * }}}
     **/
    def theSameElementsAs(right: GenTraversable[_]): MatcherFactory1[T, Aggregating] = 
      outerInstance.or(MatcherWords.contain.theSameElementsAs(right))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain theSameElementsInOrderAs List(1, 2, 3)
     *                     ^
     * }}}
     **/
    def theSameElementsInOrderAs(right: GenTraversable[_]): MatcherFactory1[T, Sequencing] = 
      outerInstance.or(MatcherWords.contain.theSameElementsInOrderAs(right))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain allOf (1, 2, 3)
     *                     ^
     * }}}
     **/
    def allOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.contain.allOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain allElementsOf List(1, 2, 3)
     *                     ^
     * }}}
     **/
    def allElementsOf(elements: GenTraversable[Any]): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.contain.allElementsOf(elements))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain inOrder (1, 2, 3)
     *                     ^
     * }}}
     **/
    def inOrder(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Sequencing] =
      outerInstance.or(MatcherWords.contain.inOrder(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain inOrderElementsOf List(1, 2, 3)
     *                     ^
     * }}}
     **/
    def inOrderElementsOf(elements: GenTraversable[Any]): MatcherFactory1[T, Sequencing] =
      outerInstance.or(MatcherWords.contain.inOrderElementsOf(elements))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain oneOf (1, 2, 3)
     *                     ^
     * }}}
     **/
    def oneOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Containing] =
      outerInstance.or(MatcherWords.contain.oneOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain oneElementOf (1, 2, 3)
     *                     ^
     * }}}
     **/
    def oneElementOf(elements: GenTraversable[Any]): MatcherFactory1[T, Containing] =
      outerInstance.or(MatcherWords.contain.oneElementOf(elements))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain atLeastOneOf (1, 2, 3)
     *                     ^
     * }}}
     **/
    def atLeastOneOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.contain.atLeastOneOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain atLeastOneElementOf (1, 2, 3)
     *                     ^
     * }}}
     **/
    def atLeastOneElementOf(elements: GenTraversable[Any]): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.contain.atLeastOneElementOf(elements))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain only (1, 2, 3)
     *                     ^
     * }}}
     **/
    def only(right: Any*): MatcherFactory1[T, Aggregating] = 
      outerInstance.or(MatcherWords.contain.only(right.toList: _*)(prettifier, pos))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain inOrderOnly (1, 2, 3)
     *                     ^
     * }}}
     **/
    def inOrderOnly(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Sequencing] =
      outerInstance.or(MatcherWords.contain.inOrderOnly(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain noneOf (1, 2, 3)
     *                     ^
     * }}}
     **/
    def noneOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Containing] =
      outerInstance.or(MatcherWords.contain.noneOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain noElementsOf (1, 2, 3)
     *                     ^
     * }}}
     **/
    def noElementsOf(elements: GenTraversable[Any]): MatcherFactory1[T, Containing] =
      outerInstance.or(MatcherWords.contain.noElementsOf(elements))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain atMostOneOf (1, 2, 3)
     *                     ^
     * }}}
     **/
    def atMostOneOf(firstEle: Any, secondEle: Any, remainingEles: Any*): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.contain.atMostOneOf(firstEle, secondEle, remainingEles.toList: _*)(prettifier, pos))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or contain atMostOneOf List(1, 2, 3)
     *                     ^
     * }}}
     **/
    def atMostOneElementOf(elements: GenTraversable[Any]): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.contain.atMostOneElementOf(elements))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or contain value (1)
   *          ^
   * }}}
   **/
  def or(containWord: ContainWord)(implicit prettifier: Prettifier, pos: source.Position): OrContainWord = new OrContainWord(prettifier, pos)
  
  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class OrBeWord {

    // SKIP-SCALATESTJS-START
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or be a ('directory)
     *                ^
     * }}}
     **/
    def a(symbol: Symbol): Matcher[T with AnyRef] = or(MatcherWords.be.a(symbol))
    // SKIP-SCALATESTJS-END

    /**
     * This method enables the following syntax, where `directory` is a <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or be a (directory)
     *                ^
     * }}}
     **/
    def a[U](bePropertyMatcher: BePropertyMatcher[U]): Matcher[T with AnyRef with U] = or(MatcherWords.be.a(bePropertyMatcher))
    
    /**
     * This method enables the following syntax, where `positiveNumber` and `validNumber` are <a href="AMatcher.html">`AMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or be a (validNumber)
     *                ^
     * }}}
     **/
    def a[U](aMatcher: AMatcher[U]): Matcher[T with U] = or(MatcherWords.be.a(aMatcher))

    // SKIP-SCALATESTJS-START
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or be an ('apple)
     *                ^
     * }}}
     **/
    def an(symbol: Symbol): Matcher[T with AnyRef] = or(MatcherWords.be.an(symbol))
    // SKIP-SCALATESTJS-END

    /**
     * This method enables the following syntax, where `orange` and `apple` are <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or be an (apple)
     *                ^
     * }}}
     **/
    def an[U](bePropertyMatcher: BePropertyMatcher[U]): Matcher[T with AnyRef with U] = or(MatcherWords.be.an(bePropertyMatcher))

    /**
     * This method enables the following syntax, where `oddNumber` and `integerNumber` are <a href="AnMatcher.html">`AnMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or be an (integerNumber)
     *                ^
     * }}}
     **/
    def an[U](anMatcher: AnMatcher[U]): Matcher[T with U] = or(MatcherWords.be.an(anMatcher))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or be theSameInstanceAs (otherString)
     *                ^
     * }}}
     **/
    def theSameInstanceAs(anyRef: AnyRef): Matcher[T with AnyRef] = or(MatcherWords.be.theSameInstanceAs(anyRef))
    
    /**
     * This method enables the following syntax, where `fraction` refers to a `PartialFunction`:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or be definedAt (8)
     *                ^
     * }}}
     **/
    def definedAt[A, U <: PartialFunction[A, _]](right: A): Matcher[T with U] = or(MatcherWords.be.definedAt(right))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or be a ('directory)
   *          ^
   * }}}
   **/
  def or(beWord: BeWord): OrBeWord = new OrBeWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class OrFullyMatchWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or fullyMatch regex (decimal)
     *                        ^
     * }}}
     **/
    def regex(regexString: String): Matcher[T with String] = or(MatcherWords.fullyMatch.regex(regexString))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or fullyMatch regex ("a(b*)c" withGroup "bb")
     *                        ^
     * }}}
     **/
    def regex(regexWithGroups: RegexWithGroups): Matcher[T with String] = or(MatcherWords.fullyMatch.regex(regexWithGroups))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or fullyMatch regex (decimal)
     *                        ^
     * }}}
     **/
    def regex(regex: Regex): Matcher[T with String] = or(MatcherWords.fullyMatch.regex(regex))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or fullyMatch regex (decimal)
   *          ^
   * }}}
   **/
  def or(fullyMatchWord: FullyMatchWord): OrFullyMatchWord = new OrFullyMatchWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class OrIncludeWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or include regex (decimal)
     *                     ^
     * }}}
     **/
    def regex(regexString: String): Matcher[T with String] = or(MatcherWords.include.regex(regexString))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or include regex ("a(b*)c" withGroup "bb")
     *                     ^
     * }}}
     **/
    def regex(regexWithGroups: RegexWithGroups): Matcher[T with String] = or(MatcherWords.include.regex(regexWithGroups))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or include regex (decimal)
     *                     ^
     * }}}
     **/
    def regex(regex: Regex): Matcher[T with String] = or(MatcherWords.include.regex(regex))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or include regex ("1.7")
   *          ^
   * }}}
   **/
  def or(includeWord: IncludeWord): OrIncludeWord = new OrIncludeWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class OrStartWithWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or startWith regex (decimal)
     *                       ^
     * }}}
     **/
    def regex(regexString: String): Matcher[T with String] = or(MatcherWords.startWith.regex(regexString))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or startWith regex ("a(b*)c" withGroup "bb")
     *                       ^
     * }}}
     **/
    def regex(regexWithGroups: RegexWithGroups): Matcher[T with String] = or(MatcherWords.startWith.regex(regexWithGroups))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or startWith regex (decimal)
     *                       ^
     * }}}
     **/
    def regex(regex: Regex): Matcher[T with String] = or(MatcherWords.startWith.regex(regex))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or startWith regex ("1.7")
   *          ^
   * }}}
   **/
  def or(startWithWord: StartWithWord): OrStartWithWord = new OrStartWithWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class OrEndWithWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or endWith regex (decimal)
     *                     ^
     * }}}
     **/
    def regex(regexString: String): Matcher[T with String] = or(MatcherWords.endWith.regex(regexString))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or endWith regex ("d(e*)f" withGroup "ee")
     *                     ^
     * }}}
     **/
    def regex(regexWithGroups: RegexWithGroups): Matcher[T with String] = or(MatcherWords.endWith.regex(regexWithGroups))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or endWith regex (decimal)
     *                     ^
     * }}}
     **/
    def regex(regex: Regex): Matcher[T with String] = or(MatcherWords.endWith.regex(regex))
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or endWith regex ("7b")
   *          ^
   * }}}
   **/
  def or(endWithWord: EndWithWord): OrEndWithWord = new OrEndWithWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class OrNotWord {

    /**
     * Get the `Matcher` instance, currently used by macro only.
     */
    val owner = outerInstance

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not equal (2)
     *                 ^
     * }}}
     **/
    def equal(any: Any): MatcherFactory1[T, Equality] =
      outerInstance.or(MatcherWords.not.apply(MatcherWords.equal(any)))

    /**
     * This method enables the following syntax for the "primitive" numeric types:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not equal (17.0 +- 0.2)
     *                 ^
     * }}}
     **/
    def equal[U](spread: Spread[U]): Matcher[T with U] = outerInstance.or(MatcherWords.not.equal(spread))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not equal (null)
     *                 ^
     * }}}
     **/
    def equal(o: Null): Matcher[T] = {
      outerInstance or {
        new Matcher[T] {
          def apply(left: T): MatchResult = {
            MatchResult(
              left != null,
              Resources.rawEqualedNull,
              Resources.rawDidNotEqualNull,
              Resources.rawMidSentenceEqualedNull,
              Resources.rawDidNotEqualNull,
              Vector.empty, 
              Vector(left)
            )
          }
          override def toString: String = "not equal null"
        }
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be (2)
     *                 ^
     * }}}
     **/
    def be(any: Any): Matcher[T] =
      outerInstance.or(MatcherWords.not.apply(MatcherWords.be(any)))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not have length (3)
     *                 ^
     * }}}
     **/
    def have(resultOfLengthWordApplication: ResultOfLengthWordApplication): MatcherFactory1[T, Length] =
      outerInstance.or(MatcherWords.not.apply(MatcherWords.have.length(resultOfLengthWordApplication.expectedLength)))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not have size (3)
     *                 ^
     * }}}
     **/
    def have(resultOfSizeWordApplication: ResultOfSizeWordApplication): MatcherFactory1[T, Size] =
      outerInstance.or(MatcherWords.not.apply(MatcherWords.have.size(resultOfSizeWordApplication.expectedSize)))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not have message ("Message from Mars!")
     *                 ^
     * }}}
     **/
    def have(resultOfMessageWordApplication: ResultOfMessageWordApplication): MatcherFactory1[T, Messaging] =
      outerInstance.or(MatcherWords.not.apply(MatcherWords.have.message(resultOfMessageWordApplication.expectedMessage)))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not have (author ("Melville"))
     *                 ^
     * }}}
     **/
    def have[U](firstPropertyMatcher: HavePropertyMatcher[U, _], propertyMatchers: HavePropertyMatcher[U, _]*): Matcher[T with U] =
      outerInstance.or(MatcherWords.not.apply(MatcherWords.have(firstPropertyMatcher, propertyMatchers: _*)))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be (null)
     *                 ^
     * }}}
     **/
    def be(o: Null): Matcher[T with AnyRef] = outerInstance.or(MatcherWords.not.be(o))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be &lt; (8)
     *                 ^
     * }}}
     **/
    def be[U](resultOfLessThanComparison: ResultOfLessThanComparison[U]): Matcher[T with U] =
      outerInstance.or(MatcherWords.not.be(resultOfLessThanComparison))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be &gt; (6)
     *                 ^
     * }}}
     **/
    def be[U](resultOfGreaterThanComparison: ResultOfGreaterThanComparison[U]): Matcher[T with U] =
      outerInstance.or(MatcherWords.not.be(resultOfGreaterThanComparison))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be &lt;= (2)
     *                 ^
     * }}}
     **/
    def be[U](resultOfLessThanOrEqualToComparison: ResultOfLessThanOrEqualToComparison[U]): Matcher[T with U] =
      outerInstance.or(MatcherWords.not.be(resultOfLessThanOrEqualToComparison))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be &gt;= (6)
     *                 ^
     * }}}
     **/
    def be[U](resultOfGreaterThanOrEqualToComparison: ResultOfGreaterThanOrEqualToComparison[U]): Matcher[T with U] =
      outerInstance.or(MatcherWords.not.be(resultOfGreaterThanOrEqualToComparison))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be === (8)
     *                 ^
     * }}}
     **/
    def be(tripleEqualsInvocation: TripleEqualsInvocation[_]): Matcher[T] =
      outerInstance.or(MatcherWords.not.be(tripleEqualsInvocation))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be ('empty)
     *                 ^
     * }}}
     **/
    def be(symbol: Symbol): Matcher[T with AnyRef] = outerInstance.or(MatcherWords.not.be(symbol))

    /**
     * This method enables the following syntax, where `odd` is a <a href="BeMatcher.html">`BeMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be (odd)
     *                 ^
     * }}}
     **/
    def be[U](beMatcher: BeMatcher[U]): Matcher[T with U] = outerInstance.or(MatcherWords.not.be(beMatcher))

    /**
     * This method enables the following syntax, where `file` is a <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be (file)
     *                 ^
     * }}}
     **/
    def be[U](bePropertyMatcher: BePropertyMatcher[U]): Matcher[T with AnyRef with U] = outerInstance.or(MatcherWords.not.be(bePropertyMatcher))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be a ('file)
     *                 ^
     * }}}
     **/
    def be(resultOfAWordApplication: ResultOfAWordToSymbolApplication): Matcher[T with AnyRef] = outerInstance.or(MatcherWords.not.be(resultOfAWordApplication))

    /**
     * This method enables the following syntax, where `validMarks` is an <a href="AMatcher.html">`AMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be a (validMarks)
     *                 ^
     * }}}
     **/
    def be[U](resultOfAWordApplication: ResultOfAWordToAMatcherApplication[U]): Matcher[T with U] = outerInstance.or(MatcherWords.not.be(resultOfAWordApplication))
    
    /**
     * This method enables the following syntax, where `file` is a <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be a (file)
     *                 ^
     * }}}
     **/
    def be[U <: AnyRef](resultOfAWordApplication: ResultOfAWordToBePropertyMatcherApplication[U]): Matcher[T with U] = outerInstance.or(MatcherWords.not.be(resultOfAWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be an ('apple)
     *                    ^
     * }}}
     **/
    def be(resultOfAnWordApplication: ResultOfAnWordToSymbolApplication): Matcher[T with AnyRef] = outerInstance.or(MatcherWords.not.be(resultOfAnWordApplication))

    /**
     * This method enables the following syntax, where `apple` is a <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be an (apple)
     *                 ^
     * }}}
     **/
    def be[U <: AnyRef](resultOfAnWordApplication: ResultOfAnWordToBePropertyMatcherApplication[U]): Matcher[T with U] = outerInstance.or(MatcherWords.not.be(resultOfAnWordApplication))

    /**
     * This method enables the following syntax, where `invalidMarks` is an <a href="AnMatcher.html">`AnMatcher`</a>:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher and not be an (invalidMarks)
     *                  ^
     * }}}
     **/
    def be[U](resultOfAnWordApplication: ResultOfAnWordToAnMatcherApplication[U]): Matcher[T with U] = outerInstance.or(MatcherWords.not.be(resultOfAnWordApplication))

    import language.experimental.macros

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be a [Book]
     *                 ^
     * }}}
     **/
    def be(aType: ResultOfATypeInvocation[_]): Matcher[T] = macro TypeMatcherMacro.orNotATypeMatcher
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be an [Book]
     *                 ^
     * }}}
     **/
    def be(anType: ResultOfAnTypeInvocation[_]): Matcher[T] = macro TypeMatcherMacro.orNotAnTypeMatcher
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be theSameInstanceAs (string)
     *                    ^
     * }}}
     **/
    def be(resultOfTheSameInstanceAsApplication: ResultOfTheSameInstanceAsApplication): Matcher[T with AnyRef] = outerInstance.or(MatcherWords.not.be(resultOfTheSameInstanceAsApplication))

    /**
     * This method enables the following syntax for the "primitive" numeric types:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be (17.0 +- 0.2)
     *                 ^
     * }}}
     **/
    def be[U](spread: Spread[U]): Matcher[T with U] = outerInstance.or(MatcherWords.not.be(spread))
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be definedAt (8)
     *                 ^
     * }}}
     **/
    def be[A, U <: PartialFunction[A, _]](resultOfDefinedAt: ResultOfDefinedAt[A]): Matcher[T with U] =
      outerInstance.or(MatcherWords.not.be(resultOfDefinedAt))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be sorted
     *                 ^
     * }}}
     **/
    def be(sortedWord: SortedWord) = 
      outerInstance.or(MatcherWords.not.be(sortedWord))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be readable
     *                 ^
     * }}}
     **/
    def be(readableWord: ReadableWord) = 
      outerInstance.or(MatcherWords.not.be(readableWord))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be empty
     *                 ^
     * }}}
     **/
    def be(emptyWord: EmptyWord) = 
      outerInstance.or(MatcherWords.not.be(emptyWord))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be writable
     *                 ^
     * }}}
     **/
    def be(writableWord: WritableWord) = 
      outerInstance.or(MatcherWords.not.be(writableWord))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not be defined
     *                 ^
     * }}}
     **/
    def be(definedWord: DefinedWord) = 
      outerInstance.or(MatcherWords.not.be(definedWord))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not fullyMatch regex (decimal)
     *                 ^
     * }}}
     **/
    def fullyMatch(resultOfRegexWordApplication: ResultOfRegexWordApplication): Matcher[T with String] =
      outerInstance.or(MatcherWords.not.fullyMatch(resultOfRegexWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not include regex (decimal)
     *                 ^
     * }}}
     **/
    def include(resultOfRegexWordApplication: ResultOfRegexWordApplication): Matcher[T with String] =
      outerInstance.or(MatcherWords.not.include(resultOfRegexWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not include ("1.7")
     *                 ^
     * }}}
     **/
    def include(expectedSubstring: String): Matcher[T with String] =
      outerInstance.or(MatcherWords.not.include(expectedSubstring))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not startWith regex (decimal)
     *                 ^
     * }}}
     **/
    def startWith(resultOfRegexWordApplication: ResultOfRegexWordApplication): Matcher[T with String] =
      outerInstance.or(MatcherWords.not.startWith(resultOfRegexWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not startWith ("1.7")
     *                 ^
     * }}}
     **/
    def startWith(expectedSubstring: String): Matcher[T with String] =
      outerInstance.or(MatcherWords.not.startWith(expectedSubstring))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not endWith regex (decimal)
     *                 ^
     * }}}
     **/
    def endWith(resultOfRegexWordApplication: ResultOfRegexWordApplication): Matcher[T with String] =
      outerInstance.or(MatcherWords.not.endWith(resultOfRegexWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not endWith ("1.7")
     *                 ^
     * }}}
     **/
    def endWith(expectedSubstring: String): Matcher[T with String] =
      outerInstance.or(MatcherWords.not.endWith(expectedSubstring))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain (3)
     *                 ^
     * }}}
     **/
    def contain[U](expectedElement: U): MatcherFactory1[T, Containing] =
      outerInstance.or(MatcherWords.not.contain(expectedElement))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain oneOf (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfOneOfApplication): MatcherFactory1[T, Containing] =
      outerInstance.or(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain oneElementOf (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfOneElementOfApplication): MatcherFactory1[T, Containing] =
      outerInstance.or(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain atLeastOneOf (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfAtLeastOneOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain atLeastOneElementOf (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfAtLeastOneElementOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain noneOf (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfNoneOfApplication): MatcherFactory1[T, Containing] =
      outerInstance.or(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain noElementsOf (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfNoElementsOfApplication): MatcherFactory1[T, Containing] =
      outerInstance.or(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain theSameElementsAs (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfTheSameElementsAsApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain theSameElementsInOrderAs (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfTheSameElementsInOrderAsApplication): MatcherFactory1[T, Sequencing] =
      outerInstance.or(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain inOrderOnly (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfInOrderOnlyApplication): MatcherFactory1[T, Sequencing] =
      outerInstance.or(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain only (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfOnlyApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain allOf (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfAllOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain allElementsOf List(8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfAllElementsOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain inOrder (8, 1, 2)
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfInOrderApplication): MatcherFactory1[T, Sequencing] =
      outerInstance.or(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain inOrderElementsOf (List(8, 1, 2))
     *                 ^
     * }}}
     **/
    def contain(right: ResultOfInOrderElementsOfApplication): MatcherFactory1[T, Sequencing] =
      outerInstance.or(MatcherWords.not.contain(right))
      
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher not contain atMostOneOf (8, 1, 2)
     *              ^
     * }}}
     **/
    def contain(right: ResultOfAtMostOneOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher not contain atMostOneElementOf (List(8, 1, 2))
     *              ^
     * }}}
     **/
    def contain(right: ResultOfAtMostOneElementOfApplication): MatcherFactory1[T, Aggregating] =
      outerInstance.or(MatcherWords.not.contain(right))

    /**
     * This method enables the following syntax given a `Matcher`:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain key ("three")
     *                 ^
     * }}}
     **/
    def contain(resultOfKeyWordApplication: ResultOfKeyWordApplication): MatcherFactory1[T, KeyMapping] =
      outerInstance.or(MatcherWords.not.contain(resultOfKeyWordApplication))

    /**
     * This method enables the following syntax given a `Matcher`:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not contain value (3)
     *                 ^
     * }}}
     **/
    def contain(resultOfValueWordApplication: ResultOfValueWordApplication): MatcherFactory1[T, ValueMapping] =
      outerInstance.or(MatcherWords.not.contain(resultOfValueWordApplication))

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * aMatcher or not matchPattern { case Person("Bob", _) =>}
     *                 ^
     * }}}
     **/
    def matchPattern(right: PartialFunction[Any, _]) = macro MatchPatternMacro.orNotMatchPatternMatcher
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or not contain value (3)
   *          ^
   * }}}
   **/
  def or(notWord: NotWord): OrNotWord = new OrNotWord
  
  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or exist
   *          ^
   * }}}
   **/
  def or(existWord: ExistWord): MatcherFactory1[T, Existence] = 
    outerInstance.or(MatcherWords.exist.matcherFactory)
    
  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * aMatcher or not (exist)
   *          ^
   * }}}
   **/
  def or(notExist: ResultOfNotExist): MatcherFactory1[T, Existence] = 
    outerInstance.or(MatcherWords.not.exist)

  /**
   * Creates a new `Matcher` that will produce `MatchResult`s by applying the original `MatchResult`
   * produced by this `Matcher` to the passed `prettify` function.  In other words, the `MatchResult`
   * produced by this `Matcher` will be passed to `prettify` to produce the final `MatchResult`
   *
   * @param prettify a function to apply to the original `MatchResult` produced by this `Matcher`
   * @return a new `Matcher` that will produce `MatchResult`s by applying the original `MatchResult`
   *         produced by this `Matcher` to the passed `prettify` function
   */
  def mapResult(prettify: MatchResult => MatchResult): Matcher[T] =
    new Matcher[T] {
      def apply(o: T): MatchResult = prettify(outerInstance(o))
    }

  /**
   * Creates a new `Matcher` that will produce `MatchResult`s that contain error messages constructed
   * using arguments that are transformed by the passed `prettify` function.  In other words, the `MatchResult`
   * produced by this `Matcher` will use arguments transformed by `prettify` function to construct the final
   * error messages.
   *
   * @param prettify a function with which to transform the arguments of error messages.
   * @return a new `Matcher` that will produce `MatchResult`s that contain error messages constructed
   *         using arguments transformed by the passed `prettify` function.
   */
  def mapArgs(prettify: Any => String): Matcher[T] =
    new Matcher[T] {
      def apply(o: T): MatchResult = {
        val mr = outerInstance(o)
        mr.copy(
          failureMessageArgs = mr.failureMessageArgs.map((LazyArg(_) { prettify })),
          negatedFailureMessageArgs = mr.negatedFailureMessageArgs.map((LazyArg(_) { prettify })),
          midSentenceFailureMessageArgs = mr.midSentenceFailureMessageArgs.map((LazyArg(_) { prettify })),
          midSentenceNegatedFailureMessageArgs = mr.midSentenceNegatedFailureMessageArgs.map((LazyArg(_) { prettify }))
        )
      }
    }
}

/**
 * Companion object for trait `Matcher` that provides a
 * factory method that creates a `Matcher[T]` from a
 * passed function of type `(T =&gt; MatchResult)`.
 *
 * @author Bill Venners
 */
object Matcher {

  /**
   * Factory method that creates a `Matcher[T]` from a
   * passed function of type `(T =&gt; MatchResult)`.
   *
   * @author Bill Venners
   */
  def apply[T](fun: T => MatchResult)(implicit ev: ClassTag[T]): Matcher[T] =
    new Matcher[T] {
      def apply(left: T) = fun(left)
      override def toString: String = "Matcher[" + ev.runtimeClass.getName + "](" + ev.runtimeClass.getName + " => MatchResult)"
    }
}

