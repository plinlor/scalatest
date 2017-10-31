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
package org.scalatest

import org.scalactic._
import org.scalatest.enablers._
import org.scalatest.matchers._
import org.scalatest.words._
import org.scalatest.words.ResultOfNoElementsOfApplication
import org.scalatest.words.ResultOfOneElementOfApplication
import scala.collection.GenTraversable
import scala.reflect.{classTag, ClassTag}
import scala.util.matching.Regex
import DefaultEquality.areEqualComparingArraysStructurally
import MatchersHelper.transformOperatorChars
import TripleEqualsSupport.Spread
import TripleEqualsSupport.TripleEqualsInvocation
import TripleEqualsSupport.TripleEqualsInvocationOnSpread
// SKIP-SCALATESTJS-START
import MatchersHelper.accessProperty
import MatchersHelper.matchSymbolToPredicateMethod
// SKIP-SCALATESTJS-END
import scala.language.experimental.macros
import scala.language.higherKinds
import MatchersHelper.endWithRegexWithGroups
import MatchersHelper.fullyMatchRegexWithGroups
import MatchersHelper.includeRegexWithGroups
import MatchersHelper.indicateFailure
import MatchersHelper.indicateSuccess
import MatchersHelper.newTestFailedException
import MatchersHelper.startWithRegexWithGroups
import exceptions.NotAllowedException
import exceptions.TestFailedException

// TODO: drop generic support for be as an equality comparison, in favor of specific ones.
// TODO: Put links from ShouldMatchers to wherever I reveal the matrix and algo of how properties are checked dynamically.
// TODO: double check that I wrote tests for (length (7)) and (size (8)) in parens
// TODO: document how to turn off the === implicit conversion
// TODO: Document you can use JMock, EasyMock, etc.

/**
 * Trait that provides a domain specific language (DSL) for expressing assertions in tests
 * using the word `should`.
 *
 * For example, if you mix `Matchers` into
 * a suite class, you can write an equality assertion in that suite like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * result should equal (3)
 * }}}
 * 
 * Here `result` is a variable, and can be of any type. If the object is an
 * `Int` with the value 3, execution will continue (''i.e.'', the expression will result
 * in the unit value, `()`). Otherwise, a <a href="exceptions/TestFailedException.html">`TestFailedException`</a>
 * will be thrown with a detail message that explains the problem, such as `"7 did not equal 3"`.
 * This `TestFailedException` will cause the test to fail.
 * 
 * 
 * Here is a table of contents for this documentation:
 * 
 *
 * <ul>
 * <li><a href="#matchersMigration">Matchers migration in ScalaTest 2.0</a></li>
 * <li><a href="#checkingEqualityWithMatchers">Checking equality with matchers</a></li>
 * <li><a href="#checkingSizeAndLength">Checking size and length</a></li>
 * <li><a href="#checkingStrings">Checking strings</a></li>
 * <li><a href="#greaterAndLessThan">Greater and less than</a></li>
 * <li><a href="#checkingBooleanPropertiesWithBe">Checking `Boolean` properties with `be`</a></li>
 * <li><a href="#usingCustomBeMatchers">Using custom `BeMatchers`</a></li>
 * <li><a href="#checkingObjectIdentity">Checking object identity</a></li>
 * <li><a href="#checkingAnObjectsClass">Checking an object's class</a></li>
 * <li><a href="#checkingNumbersAgainstARange">Checking numbers against a range</a></li>
 * <li><a href="#checkingForEmptiness">Checking for emptiness</a></li>
 * <li><a href="#workingWithContainers">Working with "containers"</a></li>
 * <li><a href="#workingWithAggregations">Working with "aggregations"</a></li>
 * <li><a href="#workingWithSequences">Working with "sequences"</a></li>
 * <li><a href="#workingWithSortables">Working with "sortables"</a></li>
 * <li><a href="#workingWithIterators">Working with iterators</a></li>
 * <li><a href="#inspectorShorthands">Inspector shorthands</a></li>
 * <li><a href="#singleElementCollections">Single-element collections</a></li>
 * <li><a href="#javaCollectionsAndMaps">Java collections and maps</a></li>
 * <li><a href="#stringsAndArraysAsCollections">`String`s and `Array`s as collections</a></li>
 * <li><a href="#beAsAnEqualityComparison">Be as an equality comparison</a></li>
 * <li><a href="#beingNegative">Being negative</a></li>
 * <li><a href="#checkingThatCodeDoesNotCompile">Checking that a snippet of code does not compile</a></li>
 * <li><a href="#logicalExpressions">Logical expressions with `and` and `or`</a></li>
 * <li><a href="#workingWithOptions">Working with `Option`s</a></li>
 * <li><a href="#checkingArbitraryProperties">Checking arbitrary properties with `have`</a></li>
 * <li><a href="#lengthSizeHavePropertyMatchers">Using `length` and `size` with `HavePropertyMatcher`s</a></li>
 * <li><a href="#matchingAPattern">Checking that an expression matches a pattern</a></li>
 * <li><a href="#usingCustomMatchers">Using custom matchers</a></li>
 * <li><a href="#checkingForExpectedExceptions">Checking for expected exceptions</a></li>
 * <li><a href="#thosePeskyParens">Those pesky parens</a></li>
 * </ul>
 * 
 * Trait <a href="MustMatchers.html">`MustMatchers`</a> is an alternative to `Matchers` that provides the exact same
 * meaning, syntax, and behavior as `Matchers`, but uses the verb `must` instead of <!-- PRESERVE -->`should`.
 * The two traits differ only in the English semantics of the verb: <!-- PRESERVE -->`should`
 * is informal, making the code feel like conversation between the writer and the reader; `must` is more formal, making the code feel more like 
 * a written specification.
 * 
 *
 * <a name="checkingEqualityWithMatchers"></a>
 * ==Checking equality with matchers==
 *
 * ScalaTest matchers provides five different ways to check equality, each designed to address a different need. They are:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * result should equal (3) // can customize equality
 * result should === (3)   // can customize equality and enforce type constraints
 * result should be (3)    // cannot customize equality, so fastest to compile
 * result shouldEqual 3    // can customize equality, no parentheses required
 * result shouldBe 3       // cannot customize equality, so fastest to compile, no parentheses required
 * }}}
 *
 * The &ldquo;`left` `should` `equal` `(right)`&rdquo; syntax requires an
 * <a href="../scalactic/Equality.html">`org.scalactic.Equality[L]`</a> to be provided (either implicitly or explicitly), where
 * `L` is the left-hand type on which `should` is invoked. In the "`left` `should` `equal` `(right)`" case,
 * for example, `L` is the type of `left`. Thus if `left` is type `Int`, the "`left` `should`
 * `equal` `(right)`"
 * statement would require an `Equality[Int]`.
 * 
 * 
 * By default, an implicit `Equality[T]` instance is available for any type `T`, in which equality is implemented
 * by simply invoking `==`  on the `left`
 * value, passing in the `right` value, with special treatment for arrays. If either `left` or `right` is an array, `deep`
 * will be invoked on it before comparing with ''==''. Thus, the following expression
 * will yield false, because `Array`'s `equals` method compares object identity:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * Array(1, 2) == Array(1, 2) // yields false
 * }}}
 *
 * The next expression will by default ''not'' result in a `TestFailedException`, because default `Equality[Array[Int]]` compares
 * the two arrays structurally, taking into consideration the equality of the array's contents:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Array(1, 2) should equal (Array(1, 2)) // succeeds (i.e., does not throw TestFailedException)
 * }}}
 *
 * If you ever do want to verify that two arrays are actually the same object (have the same identity), you can use the
 * `be theSameInstanceAs` syntax, <a href="#checkingObjectIdentity">described below</a>.
 * 
 *
 * You can customize the meaning of equality for a type when using "`should` `equal`," "`should` `===`,"
 * or `shouldEqual` syntax by defining implicit `Equality` instances that will be used instead of default `Equality`. 
 * You might do this to normalize types before comparing them with `==`, for instance, or to avoid calling the `==` method entirely,
 * such as if you want to compare `Double`s with a tolerance.
 * For an example, see the main documentation of <a href="../scalactic/Equality.html">trait `Equality`</a>.
 * 
 *
 * You can always supply implicit parameters explicitly, but in the case of implicit parameters of type `Equality[T]`, Scalactic provides a
 * simple "explictly" DSL. For example, here's how you could explicitly supply an `Equality[String]` instance that normalizes both left and right
 * sides (which must be strings), by transforming them to lowercase:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest.Matchers._
 * import org.scalatest.Matchers._
 *
 * scala&gt; import org.scalactic.Explicitly._
 * import org.scalactic.Explicitly._
 *
 * scala&gt; import org.scalactic.StringNormalizations._
 * import org.scalactic.StringNormalizations._
 *
 * scala&gt; "Hi" should equal ("hi") (after being lowerCased)
 * }}}
 *
 * The `after` `being` `lowerCased` expression results in an `Equality[String]`, which is then passed
 * explicitly as the second curried parameter to `equal`. For more information on the explictly DSL, see the main documentation
 * for trait <a href="../scalactic/Explicitly.html">`Explicitly`</a>.
 * 
 *
 * The "`should` `be`" and `shouldBe` syntax do not take an `Equality[T]` and can therefore not be customized.
 * They always use the default approach to equality described above. As a result, "`should` `be`" and `shouldBe` will
 * likely be the fastest-compiling matcher syntax for equality comparisons, since the compiler need not search for
 * an implicit `Equality[T]` each time.
 * 
 *
 * The `should` `===` syntax (and its complement, `should` `!==`) can be used to enforce type
 * constraints at compile-time between the left and right sides of the equality comparison. Here's an example:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest.Matchers._
 * import org.scalatest.Matchers._
 *
 * scala&gt; import org.scalactic.TypeCheckedTripleEquals._
 * import org.scalactic.TypeCheckedTripleEquals._
 *
 * scala&gt; Some(2) should === (2)
 * &lt;console&gt;:17: error: types Some[Int] and Int do not adhere to the equality constraint
 * selected for the === and !== operators; the missing implicit parameter is of
 * type org.scalactic.CanEqual[Some[Int],Int]
 *               Some(2) should === (2)
 *                       ^
 * }}}
 *
 * By default, the "`Some(2)` `should` `===` `(2)`" statement would fail at runtime. By mixing in
 * the equality constraints provided by `TypeCheckedTripleEquals`, however, the statement fails to compile. For more information
 * and examples, see the main documentation for <a href="../scalactic/TypeCheckedTripleEquals.html">trait `TypeCheckedTripleEquals`</a>.
 * 
 *
 * <a name="checkingSizeAndLength"></a>
 * ==Checking size and length==
 * 
 * You can check the size or length of any type of object for which it
 * makes sense. Here's how checking for length looks:
 * 
 * {{{  <!-- class="stHighlight" -->
 * result should have length 3
 * }}}
 * 
 * Size is similar:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * result should have size 10
 * }}}
 * 
 * The `length` syntax can be used with `String`, `Array`, any `scala.collection.GenSeq`,
 * any `java.util.List`, and any type `T` for which an implicit `Length[T]` type class is 
 * available in scope.
 * Similarly, the `size` syntax can be used with `Array`, any `scala.collection.GenTraversable`,
 * any `java.util.Collection`, any `java.util.Map`, and any type `T` for which an implicit `Size[T]` type class is 
 * available in scope. You can enable the `length` or `size` syntax for your own arbitrary types, therefore,
 * by defining <a href="enablers/Length.html">`Length`</a> or <a href="enablers/Size.html">`Size`</a> type
 * classes for those types.
 * 
 *
 * In addition, the `length` syntax can be used with any object that has a field or method named `length`
 * or a method named `getLength`.   Similarly, the `size` syntax can be used with any
 * object that has a field or method named `size` or a method named `getSize`.
 * The type of a `length` or `size` field, or return type of a method, must be either `Int`
 * or `Long`. Any such method must take no parameters. (The Scala compiler will ensure at compile time that
 * the object on which `should` is being invoked has the appropriate structure.)
 * 
 *
 * <a name="checkingStrings"></a>
 * ==Checking strings==
 *
 * You can check for whether a string starts with, ends with, or includes a substring like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * string should startWith ("Hello")
 * string should endWith ("world")
 * string should include ("seven")
 * }}}
 * 
 * You can check for whether a string starts with, ends with, or includes a regular expression, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * string should startWith regex "Hel*o"
 * string should endWith regex "wo.ld"
 * string should include regex "wo.ld"
 * }}}
 * 
 * And you can check whether a string fully matches a regular expression, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * string should fullyMatch regex """(-)?(\d+)(\.\d*)?"""
 * }}}
 * 
 * The regular expression passed following the `regex` token can be either a `String`
 * or a `scala.util.matching.Regex`.
 * 
 *
 * With the `startWith`, `endWith`, `include`, and `fullyMatch`
 * tokens can also be used with an optional specification of required groups, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "abbccxxx" should startWith regex ("a(b*)(c*)" withGroups ("bb", "cc"))
 * "xxxabbcc" should endWith regex ("a(b*)(c*)" withGroups ("bb", "cc"))
 * "xxxabbccxxx" should include regex ("a(b*)(c*)" withGroups ("bb", "cc"))
 * "abbcc" should fullyMatch regex ("a(b*)(c*)" withGroups ("bb", "cc"))
 * }}}
 * 
 * You can check whether a string is empty with `empty`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * s shouldBe empty
 * }}}
 *
 * You can also use most of ScalaTest's matcher syntax for collections on `String` by
 * treating the `String`s as collections of characters. For examples, see the
 * <a href="#stringsAndArraysAsCollections">`String`s and `Array`s as collections</a> section below.
 * 
 * 
 * <a name="greaterAndLessThan"></a>
 * ==Greater and less than==
 * 
 * You can check whether any type for which an implicit `Ordering[T]` is available
 * is greater than, less than, greater than or equal, or less
 * than or equal to a value of type `T`. The syntax is:
 * 
 * {{{  <!-- class="stHighlight" -->
 * one should be &lt; 7
 * one should be &gt; 0
 * one should be &lt;= 7
 * one should be &gt;= 0
 * }}}
 *
 * <a name="checkingBooleanPropertiesWithBe"></a>
 * ==Checking `Boolean` properties with `be`==
 * 
 * If an object has a method that takes no parameters and returns boolean, you can check
 * it by placing a `Symbol` (after `be`) that specifies the name
 * of the method (excluding an optional prefix of "`is`"). A symbol literal
 * in Scala begins with a tick mark and ends at the first non-identifier character. Thus,
 * `'traversableAgain` results in a `Symbol` object at runtime, as does
 * `'completed` and `'file`. Here's an example:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * iter shouldBe 'traversableAgain
 * }}}
 * 
 * Given this code, ScalaTest will use reflection to look on the object referenced from
 * `emptySet` for a method that takes no parameters and results in `Boolean`,
 * with either the name `empty` or `isEmpty`. If found, it will invoke
 * that method. If the method returns `true`, execution will continue. But if it returns
 * `false`, a `TestFailedException` will be thrown that will contain a detail message, such as:
 * 
 * {{{  <!-- class="stHighlight" -->
 * non-empty iterator was not traversableAgain
 * }}}
 * 
 * This `be` syntax can be used with any reference (`AnyRef`) type.  If the object does
 * not have an appropriately named predicate method, you'll get a `TestFailedException`
 * at runtime with a detailed message that explains the problem.
 * (For the details on how a field or method is selected during this
 * process, see the documentation for <a href="words/BeWord.html">`BeWord`</a>.)
 * 
 * 
 * If you think it reads better, you can optionally put `a` or `an` after
 * `be`. For example, `java.io.File` has two predicate methods,
 * `isFile` and `isDirectory`. Thus with a `File` object
 * named `temp`, you could write:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * temp should be a 'file
 * }}}
 * 
 * Or, given `java.awt.event.KeyEvent` has a method `isActionKey` that takes
 * no arguments and returns `Boolean`, you could assert that a `KeyEvent` is
 * an action key with:
 *
 *
 * {{{  <!-- class="stHighlight" -->
 * keyEvent should be an 'actionKey
 * }}}
 * 
 * If you prefer to check `Boolean` properties in a type-safe manner, you can use a `BePropertyMatcher`.
 * This would allow you to write expressions such as:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * xs shouldBe traversableAgain
 * temp should be a file
 * keyEvent should be an actionKey
 * }}}
 * 
 * These expressions would fail to compile if `should` is used on an inappropriate type, as determined
 * by the type parameter of the `BePropertyMatcher` being used. (For example, `file` in this example
 * would likely be of type `BePropertyMatcher[java.io.File]`. If used with an appropriate type, such an expression will compile
 * and at run time the `Boolean` property method or field will be accessed directly; ''i.e.'', no reflection will be used.
 * See the documentation for <a href="matchers/BePropertyMatcher.html">`BePropertyMatcher`</a> for more information.
 * 
 *
 * <a name="usingCustomBeMatchers"></a>
 * ==Using custom `BeMatchers`==
 *
 * If you want to create a new way of using `be`, which doesn't map to an actual property on the
 * type you care about, you can create a `BeMatcher`. You could use this, for example, to create `BeMatcher[Int]`
 * called `odd`, which would match any odd `Int`, and `even`, which would match
 * any even `Int`. 
 * Given this pair of `BeMatcher`s, you could check whether an `Int` was odd or even with expressions like:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * num shouldBe odd
 * num should not be even
 * }}}
 *
 * For more information, see the documentation for <a href="matchers/BeMatcher.html">`BeMatcher`</a>.
 *
 * <a name="checkingObjectIdentity"></a>
 * ==Checking object identity==
 * 
 * If you need to check that two references refer to the exact same object, you can write:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * ref1 should be theSameInstanceAs ref2
 * }}}
 * 
 * <a name="checkingAnObjectsClass"></a>
 * ==Checking an object's class==
 * 
 * If you need to check that an object is an instance of a particular class or trait, you can supply the type to
 * &ldquo;`be` `a`&rdquo; or &ldquo;`be` `an`&rdquo;:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * result1 shouldBe a [Tiger]
 * result1 should not be an [Orangutan]
 * }}}
 * 
 * Because type parameters are erased on the JVM, we recommend you insert an underscore for any type parameters
 * when using this syntax. Both of the following test only that the result is an instance of `List[_]`, because at
 * runtime the type parameter has been erased:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * result shouldBe a [List[_]] // recommended
 * result shouldBe a [List[Fruit]] // discouraged
 * }}}
 * 
 * <a name="checkingNumbersAgainstARange"></a>
 * ==Checking numbers against a range==
 * 
 * Often you may want to check whether a number is within a
 * range. You can do that using the `+-` operator, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * sevenDotOh should equal (6.9 +- 0.2)
 * sevenDotOh should === (6.9 +- 0.2)
 * sevenDotOh should be (6.9 +- 0.2)
 * sevenDotOh shouldEqual 6.9 +- 0.2
 * sevenDotOh shouldBe 6.9 +- 0.2
 * }}}
 * 
 * Any of these expressions will cause a `TestFailedException` to be thrown if the floating point
 * value, `sevenDotOh` is outside the range `6.7` to `7.1`.
 * You can use `+-` with any type `T` for which an implicit `Numeric[T]` exists, such as integral types:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * seven should equal (6 +- 2)
 * seven should === (6 +- 2)
 * seven should be (6 +- 2)
 * seven shouldEqual 6 +- 2
 * seven shouldBe 6 +- 2
 * }}}
 * 
 * <a name="checkingForEmptiness"></a>
 * ==Checking for emptiness==
 *
 * You can check whether an object is "empty", like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * traversable shouldBe empty
 * javaMap should not be empty
 * }}}
 * 
 * The `empty` token can be used with any type `L` for which an implicit `Emptiness[L]` exists.
 * The `Emptiness` companion object provides implicits for `GenTraversable[E]`, `java.util.Collection[E]`, 
 * `java.util.Map[K, V]`, `String`, `Array[E]`, and `Option[E]`. In addition, the
 * `Emptiness` companion object provides structural implicits for types that declare an `isEmpty` method that
 * returns a `Boolean`. Here are some examples:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest.Matchers._
 * import org.scalatest.Matchers._
 *
 * scala&gt; List.empty shouldBe empty
 *
 * scala&gt; None shouldBe empty
 *
 * scala&gt; Some(1) should not be empty
 *
 * scala&gt; "" shouldBe empty
 *
 * scala&gt; new java.util.HashMap[Int, Int] shouldBe empty
 *
 * scala&gt; new { def isEmpty = true} shouldBe empty
 *
 * scala&gt; Array(1, 2, 3) should not be empty
 * }}}
 * 
 * <a name="workingWithContainers"></a>
 * ==Working with "containers"==
 *
 * You can check whether a collection contains a particular element like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * traversable should contain ("five")
 * }}}
 * 
 * The `contain` syntax shown above can be used with any type `C` that has a "containing" nature, evidenced by 
 * an implicit `org.scalatest.enablers.Containing[L]`, where `L` is left-hand type on
 * which `should` is invoked. In the `Containing`
 * companion object, implicits are provided for types `GenTraversable[E]`, `java.util.Collection[E]`, 
 * `java.util.Map[K, V]`, `String`, `Array[E]`, and `Option[E]`. 
 * Here are some examples:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest.Matchers._
 * import org.scalatest.Matchers._
 *
 * scala&gt; List(1, 2, 3) should contain (2)
 *
 * scala&gt; Map('a' -&gt; 1, 'b' -&gt; 2, 'c' -&gt; 3) should contain ('b' -&gt; 2)
 *
 * scala&gt; Set(1, 2, 3) should contain (2)
 *
 * scala&gt; Array(1, 2, 3) should contain (2)
 *
 * scala&gt; "123" should contain ('2')
 *
 * scala&gt; Some(2) should contain (2)
 * }}}
 * 
 * ScalaTest's implicit methods that provide the `Containing[L]` type classes require an `Equality[E]`, where
 * `E` is an element type. For example, to obtain a `Containing[Array[Int]]` you must supply an `Equality[Int]`,
 * either implicitly or explicitly. The `contain` syntax uses this `Equality[E]` to determine containership.
 * Thus if you want to change how containership is determined for an element type `E`, place an implicit `Equality[E]`
 * in scope or use the explicitly DSL. Although the implicit parameter required for the `contain` syntax is of type `Containing[L]`,
 * implicit conversions are provided in the `Containing` companion object from `Equality[E]` to the various
 * types of containers of `E`. Here's an example:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest.Matchers._
 * import org.scalatest.Matchers._
 *
 * scala&gt; List("Hi", "Di", "Ho") should contain ("ho")
 * org.scalatest.exceptions.TestFailedException: List(Hi, Di, Ho) did not contain element "ho"
 *         at ...
 *
 * scala&gt; import org.scalactic.Explicitly._
 * import org.scalactic.Explicitly._
 *
 * scala&gt; import org.scalactic.StringNormalizations._
 * import org.scalactic.StringNormalizations._
 *
 * scala&gt; (List("Hi", "Di", "Ho") should contain ("ho")) (after being lowerCased)
 * }}}
 *
 * Note that when you use the explicitly DSL with `contain` you need to wrap the entire
 * `contain` expression in parentheses, as shown here.
 * 
 *
 * {{{
 * (List("Hi", "Di", "Ho") should contain ("ho")) (after being lowerCased)
 * ^                                            ^
 * }}}
 *
 * In addition to determining whether an object contains another object, you can use `contain` to
 * make other determinations.
 * For example, the `contain` `oneOf` syntax ensures that one and only one of the specified elements are
 * contained in the containing object:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3, 4, 5) should contain oneOf (5, 7, 9)
 * Some(7) should contain oneOf (5, 7, 9)
 * "howdy" should contain oneOf ('a', 'b', 'c', 'd')
 * }}}
 *
 * Note that if multiple specified elements appear in the containing object, `oneOf` will fail:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; List(1, 2, 3) should contain oneOf (2, 3, 4)
 * org.scalatest.exceptions.TestFailedException: List(1, 2, 3) did not contain one (and only one) of (2, 3, 4)
 *         at ...
 * }}}
 *
 * If you really want to ensure one or more of the specified elements are contained in the containing object, 
 * use `atLeastOneOf`, described below, instead of `oneOf`. Keep in mind, `oneOf`
 * means "''exactly'' one of."
 * 
 *
 * Note also that with any `contain` syntax, you can place custom implicit `Equality[E]` instances in scope
 * to customize how containership is determined, or use the explicitly DSL. Here's an example:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * (Array("Doe", "Ray", "Me") should contain oneOf ("X", "RAY", "BEAM")) (after being lowerCased)
 * }}}
 *
 * If you have a collection of elements that you'd like to use in a "one of" comparison, you can use "oneElementOf," like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3, 4, 5) should contain oneElementOf List(5, 7, 9)
 * Some(7) should contain oneElementOf Vector(5, 7, 9)
 * "howdy" should contain oneElementOf Set('a', 'b', 'c', 'd')
 * (Array("Doe", "Ray", "Me") should contain oneElementOf List("X", "RAY", "BEAM")) (after being lowerCased)
 * }}}
 *
 * The `contain` `noneOf` syntax does the opposite of `oneOf`: it ensures none of the specified elements
 * are contained in the containing object:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3, 4, 5) should contain noneOf (7, 8, 9)
 * Some(0) should contain noneOf (7, 8, 9)
 * "12345" should contain noneOf ('7', '8', '9')
 * }}}
 *
 * If you have a collection of elements that you'd like to use in a "none of" comparison, you can use "noElementsOf," like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3, 4, 5) should contain noElementsOf List(7, 8, 9)
 * Some(0) should contain noElementsOf Vector(7, 8, 9)
 * "12345" should contain noElementsOf Set('7', '8', '9')
 * }}}
 *
 * <a name="workingWithAggregations"></a>
 * ==Working with "aggregations"==
 *
 * As mentioned, the "`contain`,"  "`contain` `oneOf`," and "`contain` `noneOf`" syntax requires a
 * `Containing[L]` be provided, where `L` is the left-hand type.  Other `contain` syntax, which
 * will be described in this section, requires an `Aggregating[L]` be provided, where again `L` is the left-hand type.
 * (An `Aggregating[L]` instance defines the "aggregating nature" of a type `L`.)
 * The reason, essentially, is that `contain` syntax that makes sense for `Option` is enabled by
 * `Containing[L]`, whereas syntax that does ''not'' make sense for `Option` is enabled
 * by `Aggregating[L]`. For example, it doesn't make sense to assert that an `Option[Int]` contains all of a set of integers, as it
 * could only ever contain one of them. But this does make sense for a type such as `List[Int]` that can aggregate zero to many integers. 
 * 
 * 
 * The `Aggregating` companion object provides implicit instances of `Aggregating[L]` 
 * for types `GenTraversable[E]`, `java.util.Collection[E]`, 
 * `java.util.Map[K, V]`, `String`, `Array[E]`. Note that these are the same types as are supported with
 * `Containing`, but with `Option[E]` missing.
 * Here are some examples:
 * 
 * 
 * The `contain` `atLeastOneOf` syntax, for example, works for any type `L` for which an `Aggregating[L]` exists. It ensures
 * that at least one of (''i.e.'', one or more of) the specified objects are contained in the containing object:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3) should contain atLeastOneOf (2, 3, 4)
 * Array(1, 2, 3) should contain atLeastOneOf (3, 4, 5)
 * "abc" should contain atLeastOneOf ('c', 'a', 't')
 * }}}
 *
 * Similar to `Containing[L]`, the implicit methods that provide the `Aggregating[L]` instances require an `Equality[E]`, where
 * `E` is an element type. For example, to obtain a `Aggregating[Vector[String]]` you must supply an `Equality[String]`,
 * either implicitly or explicitly. The `contain` syntax uses this `Equality[E]` to determine containership.
 * Thus if you want to change how containership is determined for an element type `E`, place an implicit `Equality[E]`
 * in scope or use the explicitly DSL. Although the implicit parameter required for the `contain` syntax is of type `Aggregating[L]`,
 * implicit conversions are provided in the `Aggregating` companion object from `Equality[E]` to the various
 * types of aggregations of `E`. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * (Vector(" A", "B ") should contain atLeastOneOf ("a ", "b", "c")) (after being lowerCased and trimmed)
 * }}}
 * 
 * If you have a collection of elements that you'd like to use in an "at least one of" comparison, you can use "atLeastOneElementOf," like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3) should contain atLeastOneElementOf List(2, 3, 4)
 * Array(1, 2, 3) should contain atLeastOneElementOf Vector(3, 4, 5)
 * "abc" should contain atLeastOneElementOf Set('c', 'a', 't')
 * (Vector(" A", "B ") should contain atLeastOneElementOf List("a ", "b", "c")) (after being lowerCased and trimmed)
 * }}}
 *
 * The "`contain` `atMostOneOf`" syntax lets you specify a set of objects at most one of which should be contained in the containing object:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3, 4, 5) should contain atMostOneOf (5, 6, 7)
 * }}}
 *
 * If you have a collection of elements that you'd like to use in a "at most one of" comparison, you can use "atMostOneElementOf," like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3, 4, 5) should contain atMostOneElementOf Vector(5, 6, 7)
 * }}}
 *
 * The "`contain` `allOf`" syntax lets you specify a set of objects that should all be contained in the containing object:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3, 4, 5) should contain allOf (2, 3, 5)
 * }}}
 *
 * If you have a collection of elements that you'd like to use in a "all of" comparison, you can use "allElementsOf," like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3, 4, 5) should contain allElementsOf Array(2, 3, 5)
 * }}}
 *
 * The "`contain` `only`" syntax lets you assert that the containing object contains ''only'' the specified objects, though it may
 * contain more than one of each:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3, 2, 1) should contain only (1, 2, 3)
 * }}}
 *
 * The "`contain` `theSameElementsAs`" and "`contain` `theSameElementsInOrderAs` syntax differ from the others
 * in that the right hand side is a `GenTraversable[_]` rather than a varargs of `Any`. (Note: in a future 2.0 milestone release, possibly
 * 2.0.M6, these will likely be widened to accept any type `R` for which an `Aggregating[R]` exists.)
 * 
 *
 * The "`contain` `theSameElementsAs`" syntax lets you assert that two aggregations contain the same objects:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 2, 3, 3, 3) should contain theSameElementsAs Vector(3, 2, 3, 1, 2, 3)
 * }}}
 *
 * The number of times any family of equal objects appears must also be the same in both the left and right aggregations.
 * The specified objects may appear multiple times, but must appear in the order they appear in the right-hand list. For example, if
 * the last 3 element is left out of the right-hand list in the previous example, the expression would fail because the left side
 * has three 3's and the right hand side has only two:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * List(1, 2, 2, 3, 3, 3) should contain theSameElementsAs Vector(3, 2, 3, 1, 2)
 * org.scalatest.exceptions.TestFailedException: List(1, 2, 2, 3, 3, 3) did not contain the same elements as Vector(3, 2, 3, 1, 2)
 *         at ...
 * }}}
 * 
 * Note that no `onlyElementsOf` matcher is provided, because it would have the same
 * behavior as `theSameElementsAs`. (''I.e.'', if you were looking for `onlyElementsOf`, please use `theSameElementsAs`
 * instead.)
 * 
 * 
 * 
 * <a name="workingWithSequences"></a>
 * ==Working with "sequences"==
 *
 * The rest of the `contain` syntax, which
 * will be described in this section, requires a `Sequencing[L]` be provided, where again `L` is the left-hand type.
 * (A `Sequencing[L]` instance defines the "sequencing nature" of a type `L`.)
 * The reason, essentially, is that `contain` syntax that implies an "order" of elements makes sense only for types that place elements in a sequence.
 * For example, it doesn't make sense to assert that a `Map[String, Int]` or `Set[Int]` contains all of a set of integers in a particular
 * order, as these types don't necessarily define an order for their elements. But this does make sense for a type such as `Seq[Int]` that does define
 * an order for its elements. 
 * 
 * 
 * The `Sequencing` companion object provides implicit instances of `Sequencing[L]` 
 * for types `GenSeq[E]`, `java.util.List[E]`, 
 * `String`, and `Array[E]`. 
 * Here are some examples:
 * 
 * 
 * Similar to `Containing[L]`, the implicit methods that provide the `Aggregating[L]` instances require an `Equality[E]`, where
 * `E` is an element type. For example, to obtain a `Aggregating[Vector[String]]` you must supply an `Equality[String]`,
 * either implicitly or explicitly. The `contain` syntax uses this `Equality[E]` to determine containership.
 * Thus if you want to change how containership is determined for an element type `E`, place an implicit `Equality[E]`
 * in scope or use the explicitly DSL. Although the implicit parameter required for the `contain` syntax is of type `Aggregating[L]`,
 * implicit conversions are provided in the `Aggregating` companion object from `Equality[E]` to the various
 * types of aggregations of `E`. Here's an example:
 * 
 *
 * The "`contain` `inOrderOnly`" syntax lets you assert that the containing object contains ''only'' the specified objects, in order. 
 * The specified objects may appear multiple times, but must appear in the order they appear in the right-hand list. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 2, 3, 3, 3) should contain inOrderOnly (1, 2, 3)
 * }}}
 *
 * The "`contain` `inOrder`" syntax lets you assert that the containing object contains ''only'' the specified objects in order, like
 * `inOrderOnly`, but allows other objects to appear in the left-hand aggregation as well:
 * contain more than one of each:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(0, 1, 2, 2, 99, 3, 3, 3, 5) should contain inOrder (1, 2, 3)
 * }}}
 *
 * If you have a collection of elements that you'd like to use in a "in order" comparison, you can use "inOrderElementsOf," like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * List(0, 1, 2, 2, 99, 3, 3, 3, 5) should contain inOrderElementsOf Array(1, 2, 3)
 * }}}
 *
 * Note that "order" in `inOrder`, `inOrderOnly`, and `theSameElementsInOrderAs` (described below)
 * in the `Aggregation[L]` instances built-in to ScalaTest is defined as "iteration order".
 * 
 *
 * Lastly, the "`contain` `theSameElementsInOrderAs`" syntax lets you assert that two aggregations contain
 * the same exact elements in the same (iteration) order:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3) should contain theSameElementsInOrderAs collection.mutable.TreeSet(3, 2, 1)
 * }}}
 *
 * The previous assertion succeeds because the iteration order of a`TreeSet` is the natural
 * ordering of its elements, which in this case is 1, 2, 3. An iterator obtained from the left-hand `List` will produce the same elements
 * in the same order.
 * 
 *
 * Note that no `inOrderOnlyElementsOf` matcher is provided, because it would have the same
 * behavior as `theSameElementsInOrderAs`. (''I.e.'', if you were looking for `inOrderOnlyElementsOf`, please use `theSameElementsInOrderAs`
 * instead.)
 * 
 * 
 * <a name="workingWithSortables"></a>
 * ==Working with "sortables"==
 *
 * You can also ask whether the elements of "sortable" objects (such as `Array`s, Java `List`s, and `GenSeq`s)
 * are in sorted order, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * List(1, 2, 3) shouldBe sorted
 * }}}
 *
 * <a name="workingWithIterators"></a>
 * ==Working with iterators==
 *
 * Althought it seems desireable to provide similar matcher syntax for Scala and Java iterators to that provided for sequences like
 * `Seq`s, `Array`, and `java.util.List`, the
 * ephemeral nature of iterators makes this problematic. Some syntax (such as `should` `contain`) is relatively straightforward to
 * support on iterators, but other syntax (such
 * as, for example, `Inspector` expressions on nested iterators) is not. Rather
 * than allowing inconsistencies between sequences and iterators in the API, we chose to not support any such syntax directly on iterators:
 *
 * {{{  <!-- class="stHighlight" -->
 * scala&gt; val it = List(1, 2, 3).iterator
 * it: Iterator[Int] = non-empty iterator
 *
 * scala&gt; it should contain (2)
 * &lt;console&gt;:15: error: could not find implicit value for parameter typeClass1: org.scalatest.enablers.Containing[Iterator[Int]]
 *            it should contain (2)
 *               ^
 * }}}
 *
 * Instead, you will need to convert your iterators to a sequence explicitly before using them in matcher expressions:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * scala&gt; it.toStream should contain (2)
 * }}}
 * 
 * We recommend you convert (Scala or Java) iterators to `Stream`s, as shown in the previous example, so that you can 
 * continue to reap any potential benefits provided by the laziness of the underlying iterator.
 * 
 *
 * <a name="inspectorShorthands"></a>
 * ==Inspector shorthands==
 *
 * You can use the <a href="Inspectors.html">`Inspectors`</a> syntax with matchers as well as assertions. If you have a multi-dimensional collection, such as a
 * list of lists, using `Inspectors` is your best option:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * val yss =
 *   List(
 *     List(1, 2, 3),
 *     List(1, 2, 3),
 *     List(1, 2, 3)
 *   )
 *
 * forAll (yss) { ys =&gt;
 *   forAll (ys) { y =&gt; y should be &gt; 0 }
 * }
 * }}}
 *
 * For assertions on one-dimensional collections, however, matchers provides "inspector shorthands." Instead of writing:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val xs = List(1, 2, 3)
 * forAll (xs) { x =&gt; x should be &lt; 10 }
 * }}}
 *
 * You can write:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * all (xs) should be &lt; 10
 * }}}
 *
 * The previous statement asserts that all elements of the `xs` list should be less than 10.
 * All of the inspectors have shorthands in matchers. Here is the full list:
 * 
 *
 * <ul>
 * <li>`all` - succeeds if the assertion holds true for every element</li>
 * <li>`atLeast` - succeeds if the assertion holds true for at least the specified number of elements</li>
 * <li>`atMost` - succeeds if the assertion holds true for at most the specified number of elements</li>
 * <li>`between` - succeeds if the assertion holds true for between the specified minimum and maximum number of elements, inclusive</li>
 * <li>`every` - same as `all`, but lists all failing elements if it fails (whereas `all` just reports the first failing element)</li>
 * <li>`exactly` - succeeds if the assertion holds true for exactly the specified number of elements</li>
 * </ul>
 *
 * Here are some examples:
 * 
 * 
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest.Matchers._
 * import org.scalatest.Matchers._
 *
 * scala&gt; val xs = List(1, 2, 3, 4, 5)
 * xs: List[Int] = List(1, 2, 3, 4, 5)
 *
 * scala&gt; all (xs) should be &gt; 0
 *
 * scala&gt; atMost (2, xs) should be &gt;= 4
 *
 * scala&gt; atLeast (3, xs) should be &lt; 5
 *
 * scala&gt; between (2, 3, xs) should (be &gt; 1 and be &lt; 5)
 *
 * scala&gt; exactly (2, xs) should be &lt;= 2
 *
 * scala&gt; every (xs) should be &lt; 10
 *
 * scala&gt; // And one that fails...
 *
 * scala&gt; exactly (2, xs) shouldEqual 2
 * org.scalatest.exceptions.TestFailedException: 'exactly(2)' inspection failed, because only 1 element
 *     satisfied the assertion block at index 1: 
 *   at index 0, 1 did not equal 2, 
 *   at index 2, 3 did not equal 2, 
 *   at index 3, 4 did not equal 2, 
 *   at index 4, 5 did not equal 2 
 * in List(1, 2, 3, 4, 5)
 *         at ...
 * }}}
 * 
 * Like <a href="">`Inspectors`</a>, objects used with inspector shorthands can be any type `T` for which a `Collecting[T, E]`
 * is availabe, which by default includes `GenTraversable`, 
 * Java `Collection`, Java `Map`, `Array`s, and `String`s.
 * Here are some examples:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 * 
 * scala&gt; import Matchers._
 * import Matchers._
 * 
 * scala&gt; all (Array(1, 2, 3)) should be &lt; 5
 * 
 * scala&gt; import collection.JavaConverters._
 * import collection.JavaConverters._
 * 
 * scala&gt; val js = List(1, 2, 3).asJava
 * js: java.util.List[Int] = [1, 2, 3]
 * 
 * scala&gt; all (js) should be &lt; 5
 * 
 * scala&gt; val jmap = Map("a" -&gt; 1, "b" -&gt; 2).asJava 
 * jmap: java.util.Map[String,Int] = {a=1, b=2}
 * 
 * scala&gt; atLeast(1, jmap) shouldBe Entry("b", 2)
 * 
 * scala&gt; atLeast(2, "hello, world!") shouldBe 'o'
 * }}}
 *
 * <a name="singleElementCollections"></a>
 * ==Single-element collections==
 *
 * To assert both that a collection contains just one "lone" element as well as something else about that element, you can use
 * the `loneElement` syntax provided by trait <a href="LoneElement.html">`LoneElement`</a>. For example, if a
 * `Set[Int]` should contain just one element, an `Int`
 * less than or equal to 10, you could write:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import LoneElement._
 * set.loneElement should be &lt;= 10
 * }}}
 *
 * You can invoke `loneElement` on any type `T` for which an implicit <a href="enablers/Collecting.html">`Collecting[E, T]`</a>
 * is available, where `E` is the element type returned by the `loneElement` invocation. By default, you can use `loneElement`
 * on `GenTraversable`, Java `Collection`, Java `Map`, `Array`, and `String`.
 * 
 *
 * <a name="javaCollectionsAndMaps"></a>
 * ==Java collections and maps==
 *
 * You can use similar syntax on Java collections (`java.util.Collection`) and maps (`java.util.Map`).
 * For example, you can check whether a Java `Collection` or `Map` is `empty`,
 * like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * javaCollection should be ('empty)
 * javaMap should be ('empty)
 * }}}
 * 
 * Even though Java's `List` type doesn't actually have a `length` or `getLength` method,
 * you can nevertheless check the length of a Java `List` (`java.util.List`) like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * javaList should have length 9
 * }}}
 * 
 * You can check the size of any Java `Collection` or `Map`, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * javaMap should have size 20
 * javaSet should have size 90
 * }}}
 * 
 * In addition, you can check whether a Java `Collection` contains a particular
 * element, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * javaCollection should contain ("five")
 * }}}
 * 
 * One difference to note between the syntax supported on Java and Scala collections is that
 * in Java, `Map` is not a subtype of `Collection`, and does not
 * actually define an element type. You can ask a Java `Map` for an "entry set"
 * via the `entrySet` method, which will return the `Map`'s key/value pairs
 * wrapped in a set of `java.util.Map.Entry`, but a `Map` is not actually
 * a collection of `Entry`. To make Java `Map`s easier to work with, however,
 * ScalaTest matchers allows you to treat a Java `Map` as a collection of `Entry`,
 * and defines a convenience implementation of `java.util.Map.Entry` in
 * <a href="Entry.html">`org.scalatest.Entry`</a>. Here's how you use it:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * javaMap should contain (Entry(2, 3))
 * javaMap should contain oneOf (Entry(2, 3), Entry(3, 4))
 * }}}
 * 
 * You can you alse just check whether a Java `Map` contains a particular key, or value, like this:
 * 
 * {{{  <!-- class="stHighlight" -->
 * javaMap should contain key 1
 * javaMap should contain value "Howdy"
 * }}}
 * 
 * <a name="stringsAndArraysAsCollections"></a>
 * ==`String`s and `Array`s as collections==
 * 
 * You can also use all the syntax described above for Scala and Java collections on `Array`s and
 * `String`s. Here are some examples:
 * 
 * 
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; import Matchers._
 * import Matchers._
 *
 * scala&gt; atLeast (2, Array(1, 2, 3)) should be &gt; 1
 *
 * scala&gt; atMost (2, "halloo") shouldBe 'o'
 *
 * scala&gt; Array(1, 2, 3) shouldBe sorted
 *
 * scala&gt; "abcdefg" shouldBe sorted
 *
 * scala&gt; Array(1, 2, 3) should contain atMostOneOf (3, 4, 5)
 *
 * scala&gt; "abc" should contain atMostOneOf ('c', 'd', 'e')
 * }}}
 *
 * <a name="beAsAnEqualityComparison"></a>
 * ==`be` as an equality comparison==
 * 
 * All uses of `be` other than those shown previously perform an equality comparison. They work
 * the same as `equal` when it is used with default equality. This redundancy between `be` and `equals` exists in part
 * because it enables syntax that sometimes sounds more natural. For example, instead of writing: 
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * result should equal (null)
 * }}}
 * 
 * You can write:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * result should be (null)
 * }}}
 * 
 * (Hopefully you won't write that too much given `null` is error prone, and `Option`
 * is usually a better, well, option.) 
 * As mentioned <a href="#checkingEqualityWithMatchers">previously</a>, the other difference between `equal`
 * and `be` is that `equal` delegates the equality check to an `Equality` typeclass, whereas
 * `be` always uses default equality.
 * Here are some other examples of `be` used for equality comparison:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * sum should be (7.0)
 * boring should be (false)
 * fun should be (true)
 * list should be (Nil)
 * option should be (None)
 * option should be (Some(1))
 * }}}
 * 
 * As with `equal` used with default equality, using `be` on arrays results in `deep` being called on both arrays prior to
 * calling `equal`. As a result,
 * the following expression would ''not'' throw a <a href="exceptions/TestFailedException.html">`TestFailedException`</a>:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Array(1, 2) should be (Array(1, 2)) // succeeds (i.e., does not throw TestFailedException)
 * }}}
 *
 * Because `be` is used in several ways in ScalaTest matcher syntax, just as it is used in many ways in English, one
 * potential point of confusion in the event of a failure is determining whether `be` was being used as an equality comparison or
 * in some other way, such as a property assertion. To make it more obvious when `be` is being used for equality, the failure
 * messages generated for those equality checks will include the word `equal` in them. For example, if this expression fails with a
 * `TestFailedException`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * option should be (Some(1))
 * }}}
 *
 * The detail message in that `TestFailedException` will include the words `"equal to"` to signify `be`
 * was in this case being used for equality comparison:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Some(2) was not equal to Some(1)
 * }}}
 *
 * <a name="beingNegative"></a>
 * ==Being negative==
 * 
 * If you wish to check the opposite of some condition, you can simply insert `not` in the expression.
 * Here are a few examples:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * result should not be (null)
 * sum should not be &lt;= (10)
 * mylist should not equal (yourList)
 * string should not startWith ("Hello")
 * }}}
 * 
 * <a name="checkingThatCodeDoesNotCompile"></a>
 * ==Checking that a snippet of code does not compile==
 * 
 * Often when creating libraries you may wish to ensure that certain arrangements of code that
 * represent potential &ldquo;user errors&rdquo; do not compile, so that your library is more error resistant.
 * ScalaTest `Matchers` trait includes the following syntax for that purpose:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "val a: String = 1" shouldNot compile
 * }}}
 *
 * If you want to ensure that a snippet of code does not compile because of a type error (as opposed
 * to a syntax error), use:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "val a: String = 1" shouldNot typeCheck
 * }}}
 *
 * Note that the `shouldNot` `typeCheck` syntax will only succeed if the given snippet of code does not
 * compile because of a type error. A syntax error will still result on a thrown `TestFailedException`.
 * 
 *
 * If you want to state that a snippet of code ''does'' compile, you can make that
 * more obvious with:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "val a: Int = 1" should compile
 * }}}
 *
 * Although the previous three constructs are implemented with macros that determine at compile time whether
 * the snippet of code represented by the string does or does not compile, errors 
 * are reported as test failures at runtime.
 * 
 *
 * <a name="logicalExpressions"></a>
 * ==Logical expressions with `and` and `or`==
 * 
 * You can also combine matcher expressions with `and` and/or `or`, however,
 * you must place parentheses or curly braces around the `and` or `or` expression. For example, 
 * this `and`-expression would not compile, because the parentheses are missing:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * map should contain key ("two") and not contain value (7) // ERROR, parentheses missing!
 * }}}
 * 
 * Instead, you need to write:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * map should (contain key ("two") and not contain value (7))
 * }}}
 * 
 * Here are some more examples:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * number should (be &gt; (0) and be &lt;= (10))
 * option should (equal (Some(List(1, 2, 3))) or be (None))
 * string should (
 *   equal ("fee") or
 *   equal ("fie") or
 *   equal ("foe") or
 *   equal ("fum")
 * )
 * }}}
 * 
 * Two differences exist between expressions composed of these `and` and `or` operators and the expressions you can write
 * on regular `Boolean`s using its `&amp;&amp;` and `||` operators. First, expressions with `and`
 * and `or` do not short-circuit. The following contrived expression, for example, would print `"hello, world!"`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "yellow" should (equal ("blue") and equal { println("hello, world!"); "green" })
 * }}}
 * 
 * In other words, the entire `and` or `or` expression is always evaluated, so you'll see any side effects
 * of the right-hand side even if evaluating
 * only the left-hand side is enough to determine the ultimate result of the larger expression. Failure messages produced by these
 * expressions will "short-circuit," however,
 * mentioning only the left-hand side if that's enough to determine the result of the entire expression. This "short-circuiting" behavior
 * of failure messages is intended
 * to make it easier and quicker for you to ascertain which part of the expression caused the failure. The failure message for the previous
 * expression, for example, would be:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * "yellow" did not equal "blue"
 * }}}
 * 
 * Most likely this lack of short-circuiting would rarely be noticeable, because evaluating the right hand side will usually not
 * involve a side effect. One situation where it might show up, however, is if you attempt to `and` a `null` check on a variable with an expression
 * that uses the variable, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * map should (not be (null) and contain key ("ouch"))
 * }}}
 * 
 * If `map` is `null`, the test will indeed fail, but with a `NullArgumentException`, not a
 * `TestFailedException`. Here, the `NullArgumentException` is the visible right-hand side effect. To get a
 * `TestFailedException`, you would need to check each assertion separately:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * map should not be (null)
 * map should contain key ("ouch")
 * }}}
 * 
 * If `map` is `null` in this case, the `null` check in the first expression will fail with
 * a `TestFailedException`, and the second expression will never be executed.
 * 
 *
 * The other difference with `Boolean` operators is that although `&amp;&amp;` has a higher precedence than `||`,
 * `and` and `or`
 * have the same precedence. Thus although the `Boolean` expression `(a || b &amp;&amp; c)` will evaluate the `&amp;&amp;` expression
 * before the `||` expression, like `(a || (b &amp;&amp; c))`, the following expression:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * traversable should (contain (7) or contain (8) and have size (9))
 * }}}
 * 
 * Will evaluate left to right, as:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * traversable should ((contain (7) or contain (8)) and have size (9))
 * }}}
 * 
 * If you really want the `and` part to be evaluated first, you'll need to put in parentheses, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * traversable should (contain (7) or (contain (8) and have size (9)))
 * }}}
 * 
 * <a name="workingWithOptions"></a>
 * ==Working with `Option`s==
 * 
 * You can work with options using ScalaTest's equality, `empty`,
 * `defined`, and `contain` syntax.
 * For example, if you wish to check whether an option is `None`, you can write any of:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * option shouldEqual None
 * option shouldBe None
 * option should === (None)
 * option shouldBe empty
 * }}}
 * 
 * If you wish to check an option is defined, and holds a specific value, you can write any of:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * option shouldEqual Some("hi")
 * option shouldBe Some("hi")
 * option should === (Some("hi"))
 * }}}
 * 
 * If you only wish to check that an option is defined, but don't care what it's value is, you can write:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * option shouldBe defined
 * }}}
 * 
 * If you mix in (or import the members of) <a href="OptionValues.html">`OptionValues`</a>,
 * you can write one statement that indicates you believe an option should be defined and then say something else about its value. Here's an example:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.OptionValues._
 * option.value should be &lt; 7
 * }}}
 * 
 * As mentioned previously, you can use also use ScalaTest's `contain`, `contain oneOf`, and
 * `contain noneOf` syntax with options:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * Some(2) should contain (2)
 * Some(7) should contain oneOf (5, 7, 9)
 * Some(0) should contain noneOf (7, 8, 9)
 * }}}
 * 
 *
 * <a name="checkingArbitraryProperties"></a>
 * ==Checking arbitrary properties with `have`==
 * 
 * Using `have`, you can check properties of any type, where a ''property'' is an attribute of any
 * object that can be retrieved either by a public field, method, or JavaBean-style `get`
 * or `is` method, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * book should have (
 *   'title ("Programming in Scala"),
 *   'author (List("Odersky", "Spoon", "Venners")),
 *   'pubYear (2008)
 * )
 * }}}
 * 
 * This expression will use reflection to ensure the `title`, `author`, and `pubYear` properties of object `book`
 * are equal to the specified values. For example, it will ensure that `book` has either a public Java field or method
 * named `title`, or a public method named `getTitle`, that when invoked (or accessed in the field case) results
 * in a the string `"Programming in Scala"`. If all specified properties exist and have their expected values, respectively,
 * execution will continue. If one or more of the properties either does not exist, or exists but results in an unexpected value,
 * a `TestFailedException` will be thrown that explains the problem. (For the details on how a field or method is selected during this
 * process, see the documentation for <a href="Matchers$HavePropertyMatcherGenerator.html">`HavePropertyMatcherGenerator`</a>.)
 * 
 * 
 * When you use this syntax, you must place one or more property values in parentheses after `have`, seperated by commas, where a ''property
 * value'' is a symbol indicating the name of the property followed by the expected value in parentheses. The only exceptions to this rule is the syntax
 * for checking size and length shown previously, which does not require parentheses. If you forget and put parentheses in, however, everything will
 * still work as you'd expect. Thus instead of writing:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * array should have length (3)
 * set should have size (90)
 * }}}
 * 
 * You can alternatively, write:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * array should have (length (3))
 * set should have (size (90))
 * }}}
 * 
 * If a property has a value different from the specified expected value, a `TestFailedError` will be thrown
 * with a detailed message that explains the problem. For example, if you assert the following on
 * a `book` whose title is `Moby Dick`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * book should have ('title ("A Tale of Two Cities"))
 * }}}
 *
 * You'll get a `TestFailedException` with this detail message:
 * 
 *
 * {{{
 * The title property had value "Moby Dick", instead of its expected value "A Tale of Two Cities",
 * on object Book("Moby Dick", "Melville", 1851)
 * }}}
 * 
 * If you prefer to check properties in a type-safe manner, you can use a `HavePropertyMatcher`.
 * This would allow you to write expressions such as:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * book should have (
 *   title ("Programming in Scala"),
 *   author (List("Odersky", "Spoon", "Venners")),
 *   pubYear (2008)
 * )
 * }}}
 * 
 * These expressions would fail to compile if `should` is used on an inappropriate type, as determined
 * by the type parameter of the `HavePropertyMatcher` being used. (For example, `title` in this example
 * might be of type `HavePropertyMatcher[org.publiclibrary.Book]`. If used with an appropriate type, such an expression will compile
 * and at run time the property method or field will be accessed directly; ''i.e.'', no reflection will be used.
 * See the documentation for <a href="matchers/HavePropertyMatcher.html">`HavePropertyMatcher`</a> for more information.
 * 
 *
 * <a name="lengthSizeHavePropertyMatchers"></a>
 * ==Using `length` and `size` with `HavePropertyMatcher`s==
 *
 * If you want to use `length` or `size` syntax with your own custom `HavePropertyMatcher`s, you 
 * can do so, but you must write `(of [&ldquo;the type&rdquo;])` afterwords. For example, you could write:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * book should have (
 *   title ("A Tale of Two Cities"),
 *   length (220) (of [Book]),
 *   author ("Dickens")
 * )
 * }}}
 *
 * Prior to ScalaTest 2.0, &ldquo;`length` `(22)`&rdquo; yielded a `HavePropertyMatcher[Any, Int]` that used reflection to dynamically look
 * for a `length` field or `getLength` method. In ScalaTest 2.0, &ldquo;`length` `(22)`&rdquo; yields a
 * `MatcherFactory1[Any, Length]`, so it is no longer a `HavePropertyMatcher`. The `(of [&lt;type&gt;])` syntax converts the
 * the `MatcherFactory1[Any, Length]` to a `HavePropertyMatcher[&lt;type&gt;, Int]`.
 * 
 *
 * <a name="matchingAPattern"></a>
 * ==Checking that an expression matches a pattern==
 *
 * ScalaTest's <a href="Inside.html">`Inside`</a> trait allows you to make assertions after a pattern match.
 * Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * case class Name(first: String, middle: String, last: String)
 *
 * val name = Name("Jane", "Q", "Programmer")
 *
 * inside(name) { case Name(first, _, _) =&gt;
 *   first should startWith ("S")
 * }
 * }}}
 * 
 * You can use `inside` to just ensure a pattern is matched, without making any further assertions, but a better
 * alternative for that kind of assertion is `matchPattern`. The `matchPattern` syntax allows you
 * to express that you expect a value to match a particular pattern, no more and no less:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * name should matchPattern { case Name("Sarah", _, _) =&gt; }
 * }}}
 *
 * <a name="usingCustomMatchers"></a>
 * ==Using custom matchers==
 * 
 * If none of the built-in matcher syntax (or options shown so far for extending the syntax) satisfy a particular need you have, you can create
 * custom `Matcher`s that allow
 * you to place your own syntax directly after `should`. For example, class `java.io.File` has a method `isHidden`, which
 * indicates whether a file of a certain path and name is hidden. Because the `isHidden` method takes no parameters and returns `Boolean`,
 * you can call it using `be` with a symbol or `BePropertyMatcher`, yielding assertions like:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * file should be ('hidden)  // using a symbol
 * file should be (hidden)   // using a BePropertyMatcher
 * }}}
 * 
 * If it doesn't make sense to have your custom syntax follow `be`, you might want to create a custom `Matcher`
 * instead, so your syntax can follow `should` directly. For example, you might want to be able to check whether
 * a `java.io.File`'s name ends with a particular extension, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // using a plain-old Matcher
 * file should endWithExtension ("txt")
 * }}}
 * 
 * ScalaTest provides several mechanism to make it easy to create custom matchers, including ways to compose new matchers
 * out of existing ones complete with new error messages.  For more information about how to create custom
 * `Matcher`s, please see the documentation for the <a href="matchers/Matcher.html">`Matcher`</a> trait.
 * 
 *
 * <a name="checkingForExpectedExceptions"></a>
 * ==Checking for expected exceptions==
 *
 * Sometimes you need to test whether a method throws an expected exception under certain circumstances, such
 * as when invalid arguments are passed to the method. With `Matchers` mixed in, you can
 * check for an expected exception like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * an [IndexOutOfBoundsException] should be thrownBy s.charAt(-1) 
 * }}}
 *
 * If `charAt` throws an instance of `StringIndexOutOfBoundsException`,
 * this expression will result in that exception. But if `charAt` completes normally, or throws a different
 * exception, this expression will complete abruptly with a `TestFailedException`.
 * 
 * If you need to further isnpect an expected exception, you can capture it using this syntax:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * val thrown = the [IndexOutOfBoundsException] thrownBy s.charAt(-1) 
 * }}}
 *
 * This expression returns the caught exception so that you can inspect it further if you wish, for
 * example, to ensure that data contained inside the exception has the expected values. Here's an
 * example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * thrown.getMessage should equal ("String index out of range: -1")
 * }}}
 *
 * If you prefer you can also capture and inspect an expected exception in one statement, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * the [ArithmeticException] thrownBy 1 / 0 should have message "/ by zero"
 * the [IndexOutOfBoundsException] thrownBy {
 *   s.charAt(-1) 
 * } should have message "String index out of range: -1"
 * }}}
 *
 * You can also state that no exception should be thrown by some code, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * noException should be thrownBy 0 / 1
 * }}}
 * 
 * <a name="thosePeskyParens"></a>
 * ==Those pesky parens==
 * 
 * Perhaps the most tricky part of writing assertions using ScalaTest matchers is remembering
 * when you need or don't need parentheses, but bearing in mind a few simple rules <!-- PRESERVE -->should help.
 * It is also reassuring to know that if you ever leave off a set of parentheses when they are
 * required, your code will not compile. Thus the compiler will help you remember when you need the parens.
 * That said, the rules are:
 * 
 *
 * 1. Although you don't always need them, you may choose to always put parentheses
 * around right-hand values, such as the `7` in `num should equal (7)`:
 * 
 *
 * {{{
 * result should equal <span class="stRed">(</span>4<span class="stRed">)</span>
 * array should have length <span class="stRed">(</span>3<span class="stRed">)</span>
 * book should have (
 *   'title <span class="stRed">(</span>"Programming in Scala"<span class="stRed">)</span>,
 *   'author <span class="stRed">(</span>List("Odersky", "Spoon", "Venners")<span class="stRed">)</span>,
 *   'pubYear <span class="stRed">(</span>2008<span class="stRed">)</span>
 * )
 * option should be <span class="stRed">(</span>'defined<span class="stRed">)</span>
 * catMap should (contain key <span class="stRed">(</span>9<span class="stRed">)</span> and contain value <span class="stRed">(</span>"lives"<span class="stRed">)</span>)</span>
 * keyEvent should be an <span class="stRed">(</span>'actionKey<span class="stRed">)</span>
 * javaSet should have size <span class="stRed">(</span>90<span class="stRed">)</span>
 * }}}
 *
 * 2. Except for `length`, `size` and `message`, you must always put parentheses around
 * the list of one or more property values following a `have`:
 * 
 *
 * {{{
 * file should (exist and have <span class="stRed">(</span>'name ("temp.txt")<span class="stRed">)</span>)
 * book should have <span class="stRed">(</span>
 *   title ("Programming in Scala"),
 *   author (List("Odersky", "Spoon", "Venners")),
 *   pubYear (2008)
 * <span class="stRed">)</span>
 * javaList should have length (9) // parens optional for length and size
 * }}}
 *
 * 3. You must always put parentheses around `and` and `or` expressions, as in:
 * 
 *
 * {{{
 * catMap should <span class="stRed">(</span>contain key (9) and contain value ("lives")<span class="stRed">)</span>
 * number should <span class="stRed">(</span>equal (2) or equal (4) or equal (8)<span class="stRed">)</span>
 * }}}
 * 
 * 4. Although you don't always need them, you may choose to always put parentheses
 * around custom `Matcher`s when they appear directly after `not`:
 * 
 * 
 * {{{
 * file should exist
 * file should not <span class="stRed">(</span>exist<span class="stRed">)</span>
 * file should (exist and have ('name ("temp.txt")))
 * file should (not <span class="stRed">(</span>exist<span class="stRed">)</span> and have ('name ("temp.txt"))
 * file should (have ('name ("temp.txt") or exist)
 * file should (have ('name ("temp.txt") or not <span class="stRed">(</span>exist<span class="stRed">)</span>)
 * }}}
 *
 * That's it. With a bit of practice it <!-- PRESERVE -->should become natural to you, and the compiler will always be there to tell you if you
 * forget a set of needed parentheses.
 * 
 *
 * ''Note: ScalaTest's matchers are in part inspired by the matchers of <a href="http://rspec.info" target="_blank">RSpec</a>,
 * <a href="https://github.com/hamcrest/JavaHamcrest" target="_blank">Hamcrest</a>, and
 * <a href="http://etorreborre.github.io/specs2/" target="_blank">specs2</a>, and its &ldquo;`shouldNot compile`&rdquo; syntax
 * by the `illTyped` macro of <a href="https://github.com/milessabin/shapeless" target="_blank">shapeless</a>.''
 * 
 *
 * @author Bill Venners
 * @author Chua Chee Seng
 */
trait Matchers extends Assertions with Tolerance with ShouldVerb with MatcherWords with Explicitly { matchers =>

  import scala.language.implicitConversions

  // SKIP-SCALATESTJS-START
  // This guy is generally done through an implicit conversion from a symbol. It takes that symbol, and 
  // then represents an object with an apply method. So it gives an apply method to symbols.
  // book should have ('author ("Gibson"))
  //                   ^ // Basically this 'author symbol gets converted into this class, and its apply  method takes "Gibson"
  // TODO, put the documentation of the details of the algo for selecting a method or field to use here.
  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * This class is used as the result of an implicit conversion from class `Symbol`, to enable symbols to be
   * used in `have ('author ("Dickens"))` syntax. The name of the implicit conversion method is
   * `convertSymbolToHavePropertyMatcherGenerator`.
   * 
   *
   * Class `HavePropertyMatcherGenerator`'s primary constructor takes a `Symbol`. The 
   * `apply` method uses reflection to find and access a property that has the name specified by the
   * `Symbol` passed to the constructor, so it can determine if the property has the expected value
   * passed to `apply`.
   * If the symbol passed is `'title`, for example, the `apply` method
   * will use reflection to look for a public Java field named
   * "title", a public method named "title", or a public method named "getTitle". 
   * If a method, it must take no parameters. If multiple candidates are found,
   * the `apply` method will select based on the following algorithm:
   * 
   * 
   * <table class="stTable">
   * <tr><th class="stHeadingCell">Field</th><th class="stHeadingCell">Method</th><th class="stHeadingCell">"get" Method</th><th class="stHeadingCell">Result</th></tr>
   * <tr><td class="stTableCell">&nbsp;</td><td class="stTableCell">&nbsp;</td><td class="stTableCell">&nbsp;</td><td class="stTableCell">Throws `TestFailedException`, because no candidates found</td></tr>
   * <tr><td class="stTableCell">&nbsp;</td><td class="stTableCell">&nbsp;</td><td class="stTableCell">`getTitle()`</td><td class="stTableCell">Invokes `getTitle()`</td></tr>
   * <tr><td class="stTableCell">&nbsp;</td><td class="stTableCell">`title()`</td><td class="stTableCell">&nbsp;</td><td class="stTableCell">Invokes `title()`</td></tr>
   * <tr><td class="stTableCell">&nbsp;</td><td class="stTableCell">`title()`</td><td class="stTableCell">`getTitle()`</td><td class="stTableCell">Invokes `title()` (this can occur when `BeanProperty` annotation is used)</td></tr>
   * <tr><td class="stTableCell">`title`</td><td class="stTableCell">&nbsp;</td><td class="stTableCell">&nbsp;</td><td class="stTableCell">Accesses field `title`</td></tr>
   * <tr><td class="stTableCell">`title`</td><td class="stTableCell">&nbsp;</td><td class="stTableCell">`getTitle()`</td><td class="stTableCell">Invokes `getTitle()`</td></tr>
   * <tr><td class="stTableCell">`title`</td><td class="stTableCell">`title()`</td><td class="stTableCell">&nbsp;</td><td class="stTableCell">Invokes `title()`</td></tr>
   * <tr><td class="stTableCell">`title`</td><td class="stTableCell">`title()`</td><td class="stTableCell">`getTitle()`</td><td class="stTableCell">Invokes `title()` (this can occur when `BeanProperty` annotation is used)</td></tr>
   * </table>
   *
   * @author Bill Venners
   */
  final class HavePropertyMatcherGenerator(symbol: Symbol, prettifer: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * book should have ('title ("A Tale of Two Cities"))
     *                          ^
     * }}}
     * 
     * This class has an `apply` method that will produce a `HavePropertyMatcher[AnyRef, Any]`.
     * The implicit conversion method, `convertSymbolToHavePropertyMatcherGenerator`, will cause the 
     * above line of code to be eventually transformed into:
     * 
     * 
     * {{{  <!-- class="stHighlight" -->
     * book should have (convertSymbolToHavePropertyMatcherGenerator('title).apply("A Tale of Two Cities"))
     * }}}
     */
    def apply(expectedValue: Any): HavePropertyMatcher[AnyRef, Any] =
      new HavePropertyMatcher[AnyRef, Any] {

        /**
         * This method enables the following syntax:
         *
         * {{{  <!-- class="stHighlight" -->
         * book should have ('title ("A Tale of Two Cities"))
         * }}}
         * 
         * This method uses reflection to discover a field or method with a name that indicates it represents
         * the value of the property with the name contained in the `Symbol` passed to the 
         * `HavePropertyMatcherGenerator`'s constructor. The field or method must be public. To be a
         * candidate, a field must have the name `symbol.name`, so if `symbol` is `'title`,
         * the field name sought will be `"title"`. To be a candidate, a method must either have the name
         * `symbol.name`, or have a JavaBean-style `get` or `is`. If the type of the
         * passed `expectedValue` is `Boolean`, `"is"` is prepended, else `"get"`
         * is prepended. Thus if `'title` is passed as `symbol`, and the type of the `expectedValue` is
         * `String`, a method named `getTitle` will be considered a candidate (the return type
         * of `getTitle` will not be checked, so it need not be `String`. By contrast, if `'defined`
         * is passed as `symbol`, and the type of the `expectedValue` is `Boolean`, a method
         * named `isTitle` will be considered a candidate so long as its return type is `Boolean`.
         * 
         * TODO continue the story
         */
        def apply(objectWithProperty: AnyRef): HavePropertyMatchResult[Any] = {

          // If 'empty passed, propertyName would be "empty"
          val propertyName = symbol.name

          val isBooleanProperty =
            expectedValue match {
              case o: Boolean => true
              case _ => false
            }

          accessProperty(objectWithProperty, symbol, isBooleanProperty) match {

            case None =>

              // if propertyName is '>, mangledPropertyName would be "$greater"
              val mangledPropertyName = transformOperatorChars(propertyName)

              // methodNameToInvoke would also be "title"
              val methodNameToInvoke = mangledPropertyName

              // methodNameToInvokeWithGet would be "getTitle"
              val methodNameToInvokeWithGet = "get"+ mangledPropertyName(0).toUpper + mangledPropertyName.substring(1)

              throw newTestFailedException(Resources.propertyNotFound(methodNameToInvoke, expectedValue.toString, methodNameToInvokeWithGet), None, pos)

            case Some(result) =>

              new HavePropertyMatchResult[Any](
                result == expectedValue,
                propertyName,
                expectedValue,
                result
              )
          }
        }
        
        /**
         * Overrides to return pretty toString.
         */
        override def toString: String = "HavePropertyMatcher[AnyRef, Any](expectedValue = " + Prettifier.default(expectedValue) + ")"
      }
  }

  /**
   * This implicit conversion method converts a `Symbol` to a
   * `HavePropertyMatcherGenerator`, to enable the symbol to be used with the `have ('author ("Dickens"))` syntax.
   */
  implicit def convertSymbolToHavePropertyMatcherGenerator(symbol: Symbol)(implicit prettifier: Prettifier, pos: source.Position): HavePropertyMatcherGenerator = new HavePropertyMatcherGenerator(symbol, prettifier, pos)
  // SKIP-SCALATESTJS-END

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  class ResultOfBeWordForAny[T](left: T, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax (positiveNumber is a `AMatcher`):
     *
     * {{{  <!-- class="stHighlight" -->
     * 1 should be a positiveNumber
     *             ^
     * }}}
     */
    def a(aMatcher: AMatcher[T]): Assertion = {
      val matcherResult = aMatcher(left)
      if (matcherResult.matches != shouldBeTrue) {
        indicateFailure(if (shouldBeTrue) matcherResult.failureMessage(prettifier) else matcherResult.negatedFailureMessage(prettifier), None, pos)
      } else indicateSuccess(shouldBeTrue, matcherResult.negatedFailureMessage(prettifier), matcherResult.failureMessage(prettifier))
    }
    
    /**
     * This method enables the following syntax (positiveNumber is a `AnMatcher`):
     *
     * {{{  <!-- class="stHighlight" -->
     * 1 should be an oddNumber
     *             ^
     * }}}
     */
    def an(anMatcher: AnMatcher[T]): Assertion = {
      val matcherResult = anMatcher(left)
      if (matcherResult.matches != shouldBeTrue) {
        indicateFailure(if (shouldBeTrue) matcherResult.failureMessage(prettifier) else matcherResult.negatedFailureMessage(prettifier), None, pos)
      } else indicateSuccess(shouldBeTrue, matcherResult.negatedFailureMessage(prettifier), matcherResult.failureMessage(prettifier))
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should be theSameInstanceAs anotherObject
     *                  ^
     * }}}
     */
    def theSameInstanceAs(right: AnyRef)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      if ((toAnyRef(left) eq right) != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) FailureMessages.wasNotSameInstanceAs(prettifier, left, right) else FailureMessages.wasSameInstanceAs(prettifier, left, right), None, pos)
      else indicateSuccess(shouldBeTrue, FailureMessages.wasSameInstanceAs(prettifier, left, right), FailureMessages.wasNotSameInstanceAs(prettifier, left, right))
    }

    /* *
     * This method enables the following syntax:
     *
     * <pre class="stHighlight">
     * result should be a [String]
     *                  ^
     * </pre>
    def a[EXPECTED : ClassManifest] {
      val clazz = implicitly[ClassManifest[EXPECTED]].erasure.asInstanceOf[Class[EXPECTED]]
      if (clazz.isAssignableFrom(left.getClass)) {
        throw newTestFailedException(
          if (shouldBeTrue)
            FailureMessages.wasNotAnInstanceOf(prettifier, left, UnquotedString(clazz.getName), UnquotedString(left.getClass.getName))
          else
            FailureMessages.wasAnInstanceOf
        )
      }
    }
    */

    // SKIP-SCALATESTJS-START
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * fileMock should be a ('file)
     *                    ^
     * }}}
     */
    def a(symbol: Symbol)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      val matcherResult = matchSymbolToPredicateMethod(toAnyRef(left), symbol, true, true, prettifier, pos)
      if (matcherResult.matches != shouldBeTrue) {
        indicateFailure(if (shouldBeTrue) matcherResult.failureMessage(prettifier) else matcherResult.negatedFailureMessage(prettifier), None, pos)
      } else indicateSuccess(shouldBeTrue, matcherResult.negatedFailureMessage(prettifier), matcherResult.failureMessage(prettifier))
    }
    // SKIP-SCALATESTJS-END

    // TODO: Check the shouldBeTrues, are they sometimes always false or true?
    /**
     * This method enables the following syntax, where `badBook` is, for example, of type `Book` and
     * `goodRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * badBook should be a (goodRead)
     *                   ^
     * }}}
     */
    def a(bePropertyMatcher: BePropertyMatcher[T])(implicit ev: T <:< AnyRef): Assertion = { // TODO: Try expanding this to 2.10 AnyVals
      val result = bePropertyMatcher(left)
      if (result.matches != shouldBeTrue) {
        indicateFailure(if (shouldBeTrue) FailureMessages.wasNotA(prettifier, left, UnquotedString(result.propertyName)) else FailureMessages.wasA(prettifier, left, UnquotedString(result.propertyName)), None, pos)
      } else indicateSuccess(shouldBeTrue, FailureMessages.wasA(prettifier, left, UnquotedString(result.propertyName)), FailureMessages.wasNotA(prettifier, left, UnquotedString(result.propertyName)))
    }

    // SKIP-SCALATESTJS-START
    // TODO, in both of these, the failure message doesn't have a/an
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * fruit should be an ('orange)
     *                 ^
     * }}}
     */
    def an(symbol: Symbol)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      val matcherResult = matchSymbolToPredicateMethod(toAnyRef(left), symbol, true, false, prettifier, pos)
      if (matcherResult.matches != shouldBeTrue) {
        indicateFailure(if (shouldBeTrue) matcherResult.failureMessage(prettifier) else matcherResult.negatedFailureMessage(prettifier), None, pos)
      } else indicateSuccess(shouldBeTrue, matcherResult.negatedFailureMessage(prettifier), matcherResult.failureMessage(prettifier))
    }
    // SKIP-SCALATESTJS-END

    /**
     * This method enables the following syntax, where `badBook` is, for example, of type `Book` and
     * `excellentRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * book should be an (excellentRead)
     *                ^
     * }}}
     */ 
    def an(beTrueMatcher: BePropertyMatcher[T])(implicit ev: T <:< AnyRef): Assertion = { // TODO: Try expanding this to 2.10 AnyVals
      val beTrueMatchResult = beTrueMatcher(left)
      if (beTrueMatchResult.matches != shouldBeTrue) {
        indicateFailure(if (shouldBeTrue) FailureMessages.wasNotAn(prettifier, left, UnquotedString(beTrueMatchResult.propertyName)) else FailureMessages.wasAn(prettifier, left, UnquotedString(beTrueMatchResult.propertyName)), None, pos)
      } else indicateSuccess(shouldBeTrue, FailureMessages.wasAn(prettifier, left, UnquotedString(beTrueMatchResult.propertyName)), FailureMessages.wasNotAn(prettifier, left, UnquotedString(beTrueMatchResult.propertyName)))
    }

    /**
     * This method enables the following syntax, where `fraction` is, for example, of type `PartialFunction`:
     *
     * {{{  <!-- class="stHighlight" -->
     * fraction should be definedAt (6)
     *                    ^
     * }}}
     */
    def definedAt[U](right: U)(implicit ev: T <:< PartialFunction[U, _]): Assertion = {
      if (left.isDefinedAt(right) != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) FailureMessages.wasNotDefinedAt(prettifier, left, right) else FailureMessages.wasDefinedAt(prettifier, left, right), None, pos)
      else indicateSuccess(shouldBeTrue, FailureMessages.wasDefinedAt(prettifier, left, right), FailureMessages.wasNotDefinedAt(prettifier, left, right))
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfBeWordForAny([left], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfBeWordForAny(" + Prettifier.default(left) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class RegexWord {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * "eight" should not fullyMatch regex ("""(-)?(\d+)(\.\d*)?""".r)
     *                                     ^
     * }}}
     */
    def apply(regexString: String): ResultOfRegexWordApplication = new ResultOfRegexWordApplication(regexString, IndexedSeq.empty)

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * "eight" should not fullyMatch regex ("""(-)?(\d+)(\.\d*)?""")
     *                                     ^
     * }}}
     */
    def apply(regex: Regex): ResultOfRegexWordApplication = new ResultOfRegexWordApplication(regex, IndexedSeq.empty)

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should not fullyMatch regex ("a(b*)c" withGroup "bb") 
     *                                    ^
     * }}}
     */
    def apply(regexWithGroups: RegexWithGroups) = 
      new ResultOfRegexWordApplication(regexWithGroups.regex, regexWithGroups.groups)
    
    /**
     * Overrides to return "regex"
     */
    override def toString: String = "regex"
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class ResultOfIncludeWordForString(left: String, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should include regex ("world")
     *                       ^
     * }}}
     */
    def regex(rightRegexString: String): Assertion = regex(rightRegexString.r)

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should include regex ("a(b*)c" withGroup "bb")
     *                       ^
     * }}}
     */
    def regex(regexWithGroups: RegexWithGroups): Assertion = {
      val result = includeRegexWithGroups(left, regexWithGroups.regex, regexWithGroups.groups)
      if (result.matches != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) result.failureMessage(prettifier) else result.negatedFailureMessage(prettifier), None, pos)
      else indicateSuccess(shouldBeTrue, result.negatedFailureMessage(prettifier), result.failureMessage(prettifier))
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should include regex ("wo.ld".r)
     *                       ^
     * }}}
     */
    def regex(rightRegex: Regex): Assertion = {
      if (rightRegex.findFirstIn(left).isDefined != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) FailureMessages.didNotIncludeRegex(prettifier, left, rightRegex) else FailureMessages.includedRegex(prettifier, left, rightRegex), None, pos)
      else indicateSuccess(shouldBeTrue, FailureMessages.includedRegex(prettifier, left, rightRegex), FailureMessages.didNotIncludeRegex(prettifier, left, rightRegex))
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfIncludeWordForString([left], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfIncludeWordForString(" + Prettifier.default(left) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class ResultOfStartWithWordForString(left: String, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should startWith regex ("Hel*o")
     *                         ^
     * }}}
     */
    def regex(rightRegexString: String): Assertion = regex(rightRegexString.r)

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should startWith regex ("a(b*)c" withGroup "bb")
     *                         ^
     * }}}
     */
    def regex(regexWithGroups: RegexWithGroups): Assertion = {
      val result = startWithRegexWithGroups(left, regexWithGroups.regex, regexWithGroups.groups)
      if (result.matches != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) result.failureMessage(prettifier) else result.negatedFailureMessage(prettifier), None, pos)
      else indicateSuccess(shouldBeTrue, result.negatedFailureMessage(prettifier), result.failureMessage(prettifier))
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should startWith regex ("Hel*o".r)
     *                         ^
     * }}}
     */
    def regex(rightRegex: Regex): Assertion = {
      if (rightRegex.pattern.matcher(left).lookingAt != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) FailureMessages.didNotStartWithRegex(prettifier, left, rightRegex) else FailureMessages.startedWithRegex(prettifier, left, rightRegex), None, pos)
      else indicateSuccess(shouldBeTrue, FailureMessages.startedWithRegex(prettifier, left, rightRegex), FailureMessages.didNotStartWithRegex(prettifier, left, rightRegex))
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfStartWithWordForString([left], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfStartWithWordForString(" + Prettifier.default(left) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class ResultOfEndWithWordForString(left: String, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should endWith regex ("wor.d")
     *                       ^
     * }}}
     */
    def regex(rightRegexString: String): Assertion = regex(rightRegexString.r)
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should endWith regex ("a(b*)c" withGroup "bb")
     *                       ^
     * }}}
     */
    def regex(regexWithGroups: RegexWithGroups): Assertion = {
      val result = endWithRegexWithGroups(left, regexWithGroups.regex, regexWithGroups.groups)
      if (result.matches != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) result.failureMessage(prettifier) else result.negatedFailureMessage(prettifier), None, pos)
      else indicateSuccess(shouldBeTrue, result.negatedFailureMessage(prettifier), result.failureMessage(prettifier))
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should endWith regex ("wor.d".r)
     *                       ^
     * }}}
     */
    def regex(rightRegex: Regex): Assertion = {
      val allMatches = rightRegex.findAllIn(left)
      if ((allMatches.hasNext && (allMatches.end == left.length)) != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) FailureMessages.didNotEndWithRegex(prettifier, left, rightRegex) else FailureMessages.endedWithRegex(prettifier, left, rightRegex), None, pos)
      else indicateSuccess(shouldBeTrue, FailureMessages.endedWithRegex(prettifier, left, rightRegex), FailureMessages.didNotEndWithRegex(prettifier, left, rightRegex))
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfEndWithWordForString([left], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfEndWithWordForString(" + Prettifier.default(left) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class ResultOfFullyMatchWordForString(left: String, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should fullMatch regex ("Hel*o world")
     *                         ^
     * }}}
     */
    def regex(rightRegexString: String): Assertion = regex(rightRegexString.r)

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should fullMatch regex ("a(b*)c" withGroup "bb") 
     *                         ^
     * }}}
     */
    def regex(regexWithGroups: RegexWithGroups): Assertion = {
      val result = fullyMatchRegexWithGroups(left, regexWithGroups.regex, regexWithGroups.groups)
      if (result.matches != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) result.failureMessage(prettifier) else result.negatedFailureMessage(prettifier), None, pos)
      else indicateSuccess(shouldBeTrue, result.negatedFailureMessage(prettifier), result.failureMessage(prettifier))
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * string should fullymatch regex ("Hel*o world".r)
     *                          ^
     * }}}
     */
    def regex(rightRegex: Regex): Assertion = {
      if (rightRegex.pattern.matcher(left).matches != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) FailureMessages.didNotFullyMatchRegex(prettifier, left, rightRegex) else FailureMessages.fullyMatchedRegex(prettifier, left, rightRegex), None, pos)
      else indicateSuccess(shouldBeTrue, FailureMessages.fullyMatchedRegex(prettifier, left, rightRegex), FailureMessages.didNotFullyMatchRegex(prettifier, left, rightRegex))
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfFullyMatchWordForString([left], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfFullyMatchWordForString(" + Prettifier.default(left) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }
  
  // Going back to original, legacy one to get to a good place to check in.
/*
  def equal(right: Any): Matcher[Any] =
      new Matcher[Any] {
        def apply(left: Any): MatchResult = {
          val (leftee, rightee) = Suite.getObjectsForFailureMessage(left, right)
          MatchResult(
            areEqualComparingArraysStructurally(left, right),
            FailureMessages.didNotEqual(prettifier, leftee, rightee),
            FailureMessages.equaled(prettifier, left, right)
          )
        }
      }
*/

  /**
   * This method enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * result should equal (100 +- 1)
   *               ^
   * }}}
   */
  def equal[T](spread: Spread[T]): Matcher[T] = {
    new Matcher[T] {
      def apply(left: T): MatchResult = {
        MatchResult(
          spread.isWithin(left),
          Resources.rawDidNotEqualPlusOrMinus,
          Resources.rawEqualedPlusOrMinus,
          Vector(left, spread.pivot, spread.tolerance)
        )
      }
      override def toString: String = "equal (" + Prettifier.default(spread) + ")"
    }
  }

  /**
   * This method enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * result should equal (null)
   *               ^
   * }}}
   */
  def equal(o: Null): Matcher[AnyRef] = 
    new Matcher[AnyRef] {
      def apply(left: AnyRef): MatchResult = {
        MatchResult(
          left == null,
          Resources.rawDidNotEqualNull,
          Resources.rawEqualedNull,
          Resources.rawDidNotEqualNull,
          Resources.rawMidSentenceEqualedNull,
          Vector(left), 
          Vector.empty
        )
      }
      override def toString: String = "equal (" + Prettifier.default(o) + ")"
    }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class KeyWord {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * map should not contain key (10)
     *                            ^
     * }}}
     */
    def apply(expectedKey: Any): ResultOfKeyWordApplication = new ResultOfKeyWordApplication(expectedKey)

    /**
     * Overrides to return pretty toString.
     *
     * @return "key"
     */
    override def toString: String = "key"
  }

  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * map should not contain key (10)
   *                        ^
   * }}}
   */
  val key = new KeyWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class ValueWord {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * map should not contain key (10)
     *                            ^
     * }}}
     */
    def apply(expectedValue: Any): ResultOfValueWordApplication = new ResultOfValueWordApplication(expectedValue)

    /**
     * Overrides to return pretty toString.
     *
     * @return "value"
     */
    override def toString: String = "value"
  }

  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * map should not contain value (10)
   *                        ^
   * }}}
   */
  val value = new ValueWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AWord {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * badBook should not be a ('goodRead)
     *                         ^
     * }}}
     */
    def apply(symbol: Symbol): ResultOfAWordToSymbolApplication = new ResultOfAWordToSymbolApplication(symbol)

    /**
     * This method enables the following syntax, where, for example, `badBook` is of type `Book` and `goodRead`
     * is a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * badBook should not be a (goodRead)
     *                         ^
     * }}}
     */
    def apply[T](beTrueMatcher: BePropertyMatcher[T]): ResultOfAWordToBePropertyMatcherApplication[T] = new ResultOfAWordToBePropertyMatcherApplication(beTrueMatcher)
    
    /**
     * This method enables the following syntax, where, `positiveNumber` is an `AMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should not be a (positiveNumber)
     *                        ^
     * }}}
     */
    def apply[T](aMatcher: AMatcher[T]): ResultOfAWordToAMatcherApplication[T] = new ResultOfAWordToAMatcherApplication(aMatcher)

    /**
     * Overrides to return pretty toString.
     *
     * @return "a"
     */
    override def toString: String = "a"
  }

  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * badBook should not be a ('goodRead)
   *                       ^
   * }}}
   */
  val a = new AWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class AnWord {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * badBook should not be an ('excellentRead)
     *                          ^
     * }}}
     */
    def apply(symbol: Symbol): ResultOfAnWordToSymbolApplication = new ResultOfAnWordToSymbolApplication(symbol)

    /**
     * This method enables the following syntax, where, for example, `badBook` is of type `Book` and `excellentRead`
     * is a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * badBook should not be an (excellentRead)
     *                          ^
     * }}}
     */
    def apply[T](beTrueMatcher: BePropertyMatcher[T]): ResultOfAnWordToBePropertyMatcherApplication[T] = new ResultOfAnWordToBePropertyMatcherApplication(beTrueMatcher)
    
    /**
     * This method enables the following syntax, where, `positiveNumber` is an `AnMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should not be an (positiveNumber)
     *                         ^
     * }}}
     */
    def apply[T](anMatcher: AnMatcher[T]): ResultOfAnWordToAnMatcherApplication[T] = new ResultOfAnWordToAnMatcherApplication(anMatcher)

    /**
     * Overrides to return pretty toString.
     *
     * @return "an"
     */
    override def toString: String = "an"
  }

  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * badBook should not be an (excellentRead)
   *                       ^
   * }}}
   */
  val an = new AnWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class TheSameInstanceAsPhrase {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * oneString should not be theSameInstanceAs (anotherString)
     *                                           ^
     * }}}
     */
    def apply(anyRef: AnyRef): ResultOfTheSameInstanceAsApplication = new ResultOfTheSameInstanceAsApplication(anyRef)

    /**
     * Overrides to return pretty toString.
     *
     * @return "theSameInstanceAs"
     */
    override def toString: String = "theSameInstanceAs"
  }

  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * oneString should not be theSameInstanceAs (anotherString)
   *                         ^
   * }}}
   */
  val theSameInstanceAs: TheSameInstanceAsPhrase = new TheSameInstanceAsPhrase

  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * "eight" should not fullyMatch regex ("""(-)?(\d+)(\.\d*)?""".r)
   *                               ^
   * }}}
   */
  val regex = new RegexWord

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class ResultOfHaveWordForExtent[A](left: A, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * obj should have length (2L)
     *                 ^
     * }}}
     *
     * This method is ultimately invoked for objects that have a `length` property structure
     * of type `Long`,
     * but is of a type that is not handled by implicit conversions from nominal types such as
     * `scala.Seq`, `java.lang.String`, and `java.util.List`.
     * 
     */
    def length(expectedLength: Long)(implicit len: Length[A]): Assertion = {
      val leftLength = len.lengthOf(left)
      if ((leftLength == expectedLength) != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) FailureMessages.hadLengthInsteadOfExpectedLength(prettifier, left, leftLength, expectedLength) else FailureMessages.hadLength(prettifier, left, expectedLength), None, pos)
      else indicateSuccess(shouldBeTrue, FailureMessages.hadLength(prettifier, left, expectedLength), FailureMessages.hadLengthInsteadOfExpectedLength(prettifier, left, leftLength, expectedLength))
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * obj should have size (2L)
     *                 ^
     * }}}
     *
     * This method is ultimately invoked for objects that have a `size` property structure
     * of type `Long`,
     * but is of a type that is not handled by implicit conversions from nominal types such as
     * `Traversable` and `java.util.Collection`.
     * 
     */
    def size(expectedSize: Long)(implicit sz: Size[A]): Assertion = {
      val leftSize = sz.sizeOf(left)
      if ((leftSize == expectedSize) != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) FailureMessages.hadSizeInsteadOfExpectedSize(prettifier, left, leftSize, expectedSize) else FailureMessages.hadSize(prettifier, left, expectedSize), None, pos)
      else indicateSuccess(shouldBeTrue, FailureMessages.hadSize(prettifier, left, expectedSize), FailureMessages.hadSizeInsteadOfExpectedSize(prettifier, left, leftSize, expectedSize))
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * exception should have message ("file not found")
     *                       ^
     * }}}
     */
    def message(expectedMessage: String)(implicit messaging: Messaging[A]): Assertion = {
      val actualMessage = messaging.messageOf(left)
      if ((actualMessage== expectedMessage) != shouldBeTrue)
        indicateFailure(if (shouldBeTrue) FailureMessages.hadMessageInsteadOfExpectedMessage(prettifier, left, actualMessage, expectedMessage) else FailureMessages.hadExpectedMessage(prettifier, left, expectedMessage), None, pos)
      else indicateSuccess(shouldBeTrue, FailureMessages.hadExpectedMessage(prettifier, left, expectedMessage), FailureMessages.hadMessageInsteadOfExpectedMessage(prettifier, left, actualMessage, expectedMessage))
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfHaveWordForExtent([left], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfHaveWordForExtent(" + Prettifier.default(left) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * num should (not be &lt; (10) and not be &gt; (17))
   *                    ^
   * }}}
   */
  def <[T : Ordering] (right: T): ResultOfLessThanComparison[T] =
    new ResultOfLessThanComparison(right)

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * num should (not be &gt; (10) and not be &lt; (7))
   *                    ^
   * }}}
   */
  def >[T : Ordering] (right: T): ResultOfGreaterThanComparison[T] =
    new ResultOfGreaterThanComparison(right)

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * num should (not be &lt;= (10) and not be &gt; (17))
   *                    ^
   * }}}
   */
  def <=[T : Ordering] (right: T): ResultOfLessThanOrEqualToComparison[T] =
    new ResultOfLessThanOrEqualToComparison(right)

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * num should (not be &gt;= (10) and not be < (7))
   *                    ^
   * }}}
   */
  def >=[T : Ordering] (right: T): ResultOfGreaterThanOrEqualToComparison[T] =
    new ResultOfGreaterThanOrEqualToComparison(right)

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * list should (not be definedAt (7) and not be definedAt (9))
   *                     ^
   * }}}
   */
  def definedAt[T](right: T): ResultOfDefinedAt[T] = 
    new ResultOfDefinedAt(right)

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (oneOf(1, 2))
   *                               ^
   * }}}
   */
  def oneOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit pos: source.Position) = {
    val xs = firstEle :: secondEle :: remainingEles.toList
    if (xs.distinct.size != xs.size)
      throw new NotAllowedException(FailureMessages.oneOfDuplicate, pos)
    new ResultOfOneOfApplication(xs)
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (oneElementOf (List(1, 2)))
   *                               ^
   * }}}
   */
  def oneElementOf(elements: GenTraversable[Any]) = {
    val xs = elements.toList
    new ResultOfOneElementOfApplication(xs)
  }

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (atLeastOneOf(1, 2))
   *                               ^
   * }}}
   */
  def atLeastOneOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit pos: source.Position) = {
    val xs = firstEle :: secondEle :: remainingEles.toList
    if (xs.distinct.size != xs.size)
      throw new NotAllowedException(FailureMessages.atLeastOneOfDuplicate, pos)
    new ResultOfAtLeastOneOfApplication(xs)
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (atLeastOneElementOf (List(1, 2)))
   *                               ^
   * }}}
   */
  def atLeastOneElementOf(elements: GenTraversable[Any]) = {
    val xs = elements.toList
    new ResultOfAtLeastOneElementOfApplication(xs)
  }

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (noneOf(1, 2))
   *                               ^
   * }}}
   */
  def noneOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit pos: source.Position) = {
    val xs = firstEle :: secondEle :: remainingEles.toList
    if (xs.distinct.size != xs.size)
      throw new NotAllowedException(FailureMessages.noneOfDuplicate, pos)
    new ResultOfNoneOfApplication(xs)
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (noElementsOf List(1, 2))
   *                               ^
   * }}}
   */
  def noElementsOf(elements: GenTraversable[Any]) = {
    val xs = elements.toList
    new ResultOfNoElementsOfApplication(xs)
  }

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (theSameElementsAs(List(1, 2, 3)))
   *                               ^
   * }}}
   */
  def theSameElementsAs(xs: GenTraversable[_]) = new ResultOfTheSameElementsAsApplication(xs)
  
  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (theSameElementsInOrderAs(List(1, 2)))
   *                               ^
   * }}}
   */
  def theSameElementsInOrderAs(xs: GenTraversable[_]) = new ResultOfTheSameElementsInOrderAsApplication(xs)

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (only(1, 2))
   *                               ^
   * }}}
   */
  def only(xs: Any*)(implicit pos: source.Position) = {
    if (xs.isEmpty)
      throw new NotAllowedException(FailureMessages.onlyEmpty, pos)
    if (xs.distinct.size != xs.size)
      throw new NotAllowedException(FailureMessages.onlyDuplicate, pos)
    new ResultOfOnlyApplication(xs)
  }
  
  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (inOrderOnly(1, 2))
   *                               ^
   * }}}
   */
  def inOrderOnly[T](firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit pos: source.Position) = {
    val xs = firstEle :: secondEle :: remainingEles.toList
    if (xs.distinct.size != xs.size)
      throw new NotAllowedException(FailureMessages.inOrderOnlyDuplicate, pos)
    new ResultOfInOrderOnlyApplication(xs)
  }
  
  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (allOf(1, 2))
   *                               ^
   * }}}
   */
  def allOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit pos: source.Position) = {
    val xs = firstEle :: secondEle :: remainingEles.toList
    if (xs.distinct.size != xs.size)
      throw new NotAllowedException(FailureMessages.allOfDuplicate, pos)
    new ResultOfAllOfApplication(xs)
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (allElementsOf(1, 2))
   *                               ^
   * }}}
   */
  def allElementsOf[R](elements: GenTraversable[R]) = {
    val xs = elements.toList
    new ResultOfAllElementsOfApplication(xs)
  }
  
  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (inOrder(1, 2))
   *                               ^
   * }}}
   */
  def inOrder(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit pos: source.Position) = {
    val xs = firstEle :: secondEle :: remainingEles.toList
    if (xs.distinct.size != xs.size)
      throw new NotAllowedException(FailureMessages.inOrderDuplicate, pos)
    new ResultOfInOrderApplication(xs)
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (inOrderElementsOf List(1, 2))
   *                               ^
   * }}}
   */
  def inOrderElementsOf[R](elements: GenTraversable[R]) = {
    val xs = elements.toList
    new ResultOfInOrderElementsOfApplication(xs)
  }
  
  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (atMostOneOf(1, 2))
   *                               ^
   * }}}
   */
  def atMostOneOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit pos: source.Position) = {
    val xs = firstEle :: secondEle :: remainingEles.toList
    if (xs.distinct.size != xs.size)
      throw new NotAllowedException(FailureMessages.atMostOneOfDuplicate, pos)
    new ResultOfAtMostOneOfApplication(xs)
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * List(1, 2, 3) should contain (atMostOneElementOf (List(1, 2)))
   *                               ^
   * }}}
   */
  def atMostOneElementOf[R](elements: GenTraversable[R]) = {
    val xs = elements.toList
    new ResultOfAtMostOneElementOfApplication(xs)
  }
  
  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * a [RuntimeException] should be thrownBy {...}
   *                                ^
   * }}}
   */
  def thrownBy(fun: => Any) = new ResultOfThrownByApplication(fun)

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * exception should not have message ("file not found")
   *                           ^
   * }}}
   */
  def message(expectedMessage: String) = new ResultOfMessageWordApplication(expectedMessage)
  
/*
  // For safe keeping
  private implicit def nodeToCanonical(node: scala.xml.Node) = new Canonicalizer(node)

  private class Canonicalizer(node: scala.xml.Node) {

    def toCanonical: scala.xml.Node = {
      node match {
        case elem: scala.xml.Elem =>
          val canonicalizedChildren =
            for (child <- node.child if !child.toString.trim.isEmpty) yield {
              child match {
                case elem: scala.xml.Elem => elem.toCanonical
                case other => other
              }
            }
          new scala.xml.Elem(elem.prefix, elem.label, elem.attributes, elem.scope, canonicalizedChildren: _*)
        case other => other
      }
    }
  }
*/

/*
  class AType[T : ClassManifest] {

    private val clazz = implicitly[ClassManifest[T]].erasure.asInstanceOf[Class[T]]

    def isAssignableFromClassOf(o: Any): Boolean = clazz.isAssignableFrom(o.getClass)

    def className: String = clazz.getName
  }

  def a[T : ClassManifest]: AType[T] = new AType[T]
*/

  // This is where InspectorShorthands started

  private sealed trait Collected extends Product with Serializable
  private case object AllCollected extends Collected
  private case object EveryCollected extends Collected
  private case class BetweenCollected(from: Int, to: Int) extends Collected
  private case class AtLeastCollected(num: Int) extends Collected
  private case class AtMostCollected(num: Int) extends Collected
  private case object NoCollected extends Collected
  private case class ExactlyCollected(num: Int) extends Collected
  
  private[scalatest] def doCollected[T](collected: Collected, xs: scala.collection.GenTraversable[T], original: Any, prettifier: Prettifier, pos: source.Position)(fun: T => Assertion): Assertion = {

    val asserting = InspectorAsserting.assertingNatureOfAssertion

    collected match {
      case AllCollected =>
        asserting.forAll(xs, original, true, prettifier, pos) { e =>
          fun(e)
        }
      case AtLeastCollected(num) =>
        asserting.forAtLeast(num, xs, original, true, prettifier, pos) { e =>
          fun(e)
        }
      case EveryCollected =>
        asserting.forEvery(xs, original, true, prettifier, pos) { e =>
          fun(e)
        }
      case ExactlyCollected(num) =>
        asserting.forExactly(num, xs, original, true, prettifier, pos) { e =>
          fun(e)
        }
      case NoCollected =>
        asserting.forNo(xs, original, true, prettifier, pos) { e =>
          fun(e)
        }
      case BetweenCollected(from, to) =>
        asserting.forBetween(from, to, xs, original, true, prettifier, pos) { e =>
          fun(e)
        }
      case AtMostCollected(num) =>
        asserting.forAtMost(num, xs, original, true, prettifier, pos) { e =>
          fun(e)
        }
    }
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="InspectorsMatchers.html">`InspectorsMatchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  final class ResultOfNotWordForCollectedAny[T](collected: Collected, xs: scala.collection.GenTraversable[T], original: Any, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

     
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not equal (7)
     *                    ^
     * }}}
     */
    def equal(right: Any)(implicit equality: Equality[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((equality.areEqual(e, right)) != shouldBeTrue)
          indicateFailure(if (shouldBeTrue) FailureMessages.didNotEqual(prettifier, e, right) else FailureMessages.equaled(prettifier, e, right), None, pos)
        else indicateSuccess(shouldBeTrue, FailureMessages.equaled(prettifier, e, right), FailureMessages.didNotEqual(prettifier, e, right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be (7)
     *                    ^
     * }}}
     */
    def be(right: Any): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((e == right) != shouldBeTrue)
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotEqualTo(prettifier, e, right) else FailureMessages.wasEqualTo(prettifier, e, right), None, pos)
        else indicateSuccess(shouldBeTrue, FailureMessages.wasEqualTo(prettifier, e, right), FailureMessages.wasNotEqualTo(prettifier, e, right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be &lt;= (7)
     *                    ^
     * }}}
     */
    def be(comparison: ResultOfLessThanOrEqualToComparison[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (comparison(e) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotLessThanOrEqualTo(prettifier, e, comparison.right) else FailureMessages.wasLessThanOrEqualTo(prettifier, e, comparison.right), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasLessThanOrEqualTo(prettifier, e, comparison.right), FailureMessages.wasNotLessThanOrEqualTo(prettifier, e, comparison.right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be &gt;= (7)
     *                    ^
     * }}}
     */
    def be(comparison: ResultOfGreaterThanOrEqualToComparison[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (comparison(e) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotGreaterThanOrEqualTo(prettifier, e, comparison.right) else FailureMessages.wasGreaterThanOrEqualTo(prettifier, e, comparison.right), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasGreaterThanOrEqualTo(prettifier, e, comparison.right), FailureMessages.wasNotGreaterThanOrEqualTo(prettifier, e, comparison.right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be &lt; (7)
     *                    ^
     * }}}
     */
    def be(comparison: ResultOfLessThanComparison[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (comparison(e) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotLessThan(prettifier, e, comparison.right) else FailureMessages.wasLessThan(prettifier, e, comparison.right), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasLessThan(prettifier, e, comparison.right), FailureMessages.wasNotLessThan(prettifier, e, comparison.right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be &gt; (7)
     *                    ^
     * }}}
     */
    def be(comparison: ResultOfGreaterThanComparison[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (comparison(e) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotGreaterThan(prettifier, e, comparison.right) else FailureMessages.wasGreaterThan(prettifier, e, comparison.right), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasGreaterThan(prettifier, e, comparison.right), FailureMessages.wasNotGreaterThan(prettifier, e, comparison.right))
      }
    }

    /**
     * '''
     * The deprecation period for the "be ===" syntax has expired, and the syntax 
     * will now throw `NotAllowedException`.  Please use should equal, should ===, shouldEqual,
     * should be, or shouldBe instead.
     * '''
     * 
     * Note: usually syntax will be removed after its deprecation period. This was left in because otherwise the syntax could in some
     * cases still compile, but silently wouldn't work.
     * 
     */
    @deprecated("The deprecation period for the be === syntax has expired. Please use should equal, should ===, shouldEqual, should be, or shouldBe instead.")
    def be(comparison: TripleEqualsInvocation[_]): Nothing = {
      throw new NotAllowedException(FailureMessages.beTripleEqualsNotAllowed, pos)
    }

    /**
     * This method enables the following syntax, where `odd` refers to
     * a `BeMatcher[Int]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be (odd)
     *                    ^
     * }}}
     */
    def be(beMatcher: BeMatcher[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = beMatcher(e)
        if (result.matches != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) result.failureMessage(prettifier) else result.negatedFailureMessage(prettifier), None, pos)
        }
        else indicateSuccess(shouldBeTrue, result.negatedFailureMessage(prettifier), result.failureMessage(prettifier))
      }
    }
    
    /**
     * This method enables the following syntax, where `stack` is, for example, of type `Stack` and
     * `empty` refers to a `BePropertyMatcher[Stack]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be (empty)
     *                    ^
     * }}}
     */
    def be(bePropertyMatcher: BePropertyMatcher[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = bePropertyMatcher(e)
        if (result.matches != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNot(prettifier, e, UnquotedString(result.propertyName)) else FailureMessages.was(prettifier, e, UnquotedString(result.propertyName)), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.was(prettifier, e, UnquotedString(result.propertyName)), FailureMessages.wasNot(prettifier, e, UnquotedString(result.propertyName)))
      }
    }
    
    /**
     * This method enables the following syntax, where `notFileMock` is, for example, of type `File` and
     * `file` refers to a `BePropertyMatcher[File]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be a (file)
     *                    ^
     * }}}
     */
    def be[U >: T](resultOfAWordApplication: ResultOfAWordToBePropertyMatcherApplication[U]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = resultOfAWordApplication.bePropertyMatcher(e)
        if (result.matches != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotA(prettifier, e, UnquotedString(result.propertyName)) else FailureMessages.wasA(prettifier, e, UnquotedString(result.propertyName)), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasA(prettifier, e, UnquotedString(result.propertyName)), FailureMessages.wasNotA(prettifier, e, UnquotedString(result.propertyName)))
      }
    }
    
    /**
     * This method enables the following syntax, where `keyEvent` is, for example, of type `KeyEvent` and
     * `actionKey` refers to a `BePropertyMatcher[KeyEvent]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(keyEvents) should not be an (actionKey)
     *                           ^
     * }}}
     */
    def be[U >: T](resultOfAnWordApplication: ResultOfAnWordToBePropertyMatcherApplication[U]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = resultOfAnWordApplication.bePropertyMatcher(e)
        if (result.matches != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotAn(prettifier, e, UnquotedString(result.propertyName)) else FailureMessages.wasAn(prettifier, e, UnquotedString(result.propertyName)), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasAn(prettifier, e, UnquotedString(result.propertyName)), FailureMessages.wasNotAn(prettifier, e, UnquotedString(result.propertyName)))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be theSameInstanceAs (string)
     *                    ^
     * }}}
     */
    def be(resultOfSameInstanceAsApplication: ResultOfTheSameInstanceAsApplication): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        e match {
          case ref: AnyRef =>
            if ((resultOfSameInstanceAsApplication.right eq ref) != shouldBeTrue) {
              indicateFailure(if (shouldBeTrue) FailureMessages.wasNotSameInstanceAs(prettifier, e, resultOfSameInstanceAsApplication.right) else FailureMessages.wasSameInstanceAs(prettifier, e, resultOfSameInstanceAsApplication.right), None, pos)
            }
            else indicateSuccess(shouldBeTrue, FailureMessages.wasSameInstanceAs(prettifier, e, resultOfSameInstanceAsApplication.right), FailureMessages.wasNotSameInstanceAs(prettifier, e, resultOfSameInstanceAsApplication.right))
          case _ => 
            throw new IllegalArgumentException("theSameInstanceAs should only be used for AnyRef")
        }
      }
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be definedAt ("apple")
     *                    ^
     * }}}
     */
    def be[U](resultOfDefinedAt: ResultOfDefinedAt[U])(implicit ev: T <:< PartialFunction[U, _]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (e.isDefinedAt(resultOfDefinedAt.right) != shouldBeTrue)
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotDefinedAt(prettifier, e, resultOfDefinedAt.right) else FailureMessages.wasDefinedAt(prettifier, e, resultOfDefinedAt.right), None, pos)
        else indicateSuccess(shouldBeTrue, FailureMessages.wasDefinedAt(prettifier, e, resultOfDefinedAt.right), FailureMessages.wasNotDefinedAt(prettifier, e, resultOfDefinedAt.right))
      }
    }

    // TODO: Write tests and implement cases for:
    // have(length (9), title ("hi")) (this one we'll use this have method but add a HavePropertyMatcher* arg)
    // have(size (9), title ("hi")) (this one we'll use the next have method but add a HavePropertyMatcher* arg)
    // have(length(9), size (9), title ("hi")) (for this one we'll need a new overloaded have(ROLWA, ROSWA, HPM*))
    // have(size(9), length (9), title ("hi")) (for this one we'll need a new overloaded have(ROSWA, ROLWA, HPM*))
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not have length (0)
     *                    ^
     * }}}
     *
     */
    def have(resultOfLengthWordApplication: ResultOfLengthWordApplication)(implicit len: Length[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val right = resultOfLengthWordApplication.expectedLength
        val leftLength = len.lengthOf(e)
        if ((leftLength == right) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.hadLengthInsteadOfExpectedLength(prettifier, e, leftLength, right) else FailureMessages.hadLength(prettifier, e, right), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.hadLength(prettifier, e, right), FailureMessages.hadLengthInsteadOfExpectedLength(prettifier, e, leftLength, right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not have size (0)
     *                    ^
     * }}}
     *
     */
    def have(resultOfSizeWordApplication: ResultOfSizeWordApplication)(implicit sz: Size[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val right = resultOfSizeWordApplication.expectedSize
        val leftSize = sz.sizeOf(e)
        if ((leftSize == right) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.hadSizeInsteadOfExpectedSize(prettifier, e, leftSize, right) else FailureMessages.hadSize(prettifier, e, right), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.hadSize(prettifier, e, right), FailureMessages.hadSizeInsteadOfExpectedSize(prettifier, e, leftSize, right))
      }
    }

    /**
     * This method enables the following syntax, where `badBook` is, for example, of type `Book` and
     * `title ("One Hundred Years of Solitude")` results in a `HavePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(books) should not have (title ("One Hundred Years of Solitude"))
     *                       ^
     * }}}
     */
    def have[U >: T](firstPropertyMatcher: HavePropertyMatcher[U, _], propertyMatchers: HavePropertyMatcher[U, _]*): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
      
        val results =
          for (propertyVerifier <- firstPropertyMatcher :: propertyMatchers.toList) yield
            propertyVerifier(e)

        val firstFailureOption = results.find(pv => !pv.matches)

        val justOneProperty = propertyMatchers.length == 0

        // if shouldBeTrue is false, then it is like "not have ()", and should throw TFE if firstFailureOption.isDefined is false
        // if shouldBeTrue is true, then it is like "not (not have ()), which should behave like have ()", and should throw TFE if firstFailureOption.isDefined is true
        if (firstFailureOption.isDefined == shouldBeTrue) {
          firstFailureOption match {
            case Some(firstFailure) =>
              // This is one of these cases, thus will only get here if shouldBeTrue is true
              // 0 0 | 0 | 1
              // 0 1 | 0 | 1
              // 1 0 | 0 | 1
              indicateFailure(
                FailureMessages.propertyDidNotHaveExpectedValue(prettifier,
                  UnquotedString(firstFailure.propertyName),
                  firstFailure.expectedValue,
                  firstFailure.actualValue,
                  e
                ), 
                None,
                pos
              )
            case None =>
              // This is this cases, thus will only get here if shouldBeTrue is false
              // 1 1 | 1 | 0
              val failureMessage =
                if (justOneProperty) {
                  val firstPropertyResult = results.head // know this will succeed, because firstPropertyMatcher was required
                  FailureMessages.propertyHadExpectedValue(prettifier,
                    UnquotedString(firstPropertyResult.propertyName),
                    firstPropertyResult.expectedValue,
                    e
                  )
                }
                else FailureMessages.allPropertiesHadExpectedValues(prettifier, e)

              indicateFailure(failureMessage, None, pos)
          } 
        }
        else {
          if (shouldBeTrue)
            indicateSuccess(FailureMessages.allPropertiesHadExpectedValues(prettifier, e))
          else {
            firstFailureOption match {
              case Some(firstFailure) =>
                indicateSuccess(
                  FailureMessages.propertyDidNotHaveExpectedValue(prettifier,
                    UnquotedString(firstFailure.propertyName),
                    firstFailure.expectedValue,
                    firstFailure.actualValue,
                    e
                  )
                )
              case None =>
                val message =
                  if (justOneProperty) {
                    val firstPropertyResult = results.head // know this will succeed, because firstPropertyMatcher was required
                    FailureMessages.propertyHadExpectedValue(prettifier,
                      UnquotedString(firstPropertyResult.propertyName),
                      firstPropertyResult.expectedValue,
                      e
                    )
                  }
                  else FailureMessages.allPropertiesHadExpectedValues(prettifier, e)

                indicateSuccess(message)
            }
          }
        }
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be (null)
     *                    ^
     * }}}
     */
    def be(o: Null)(implicit ev: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((e == null) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotNull(prettifier, e) else FailureMessages.wasNull, None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasNull, FailureMessages.wasNotNull(prettifier, e))
      }
    }

    // SKIP-SCALATESTJS-START
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be ('empty)
     *                    ^
     * }}}
     */
    def be(symbol: Symbol)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val matcherResult = matchSymbolToPredicateMethod(toAnyRef(e), symbol, false, false, prettifier, pos)
        if (matcherResult.matches != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) matcherResult.failureMessage(prettifier) else matcherResult.negatedFailureMessage(prettifier), None, pos)
        }
        else indicateSuccess(shouldBeTrue, matcherResult.negatedFailureMessage(prettifier), matcherResult.failureMessage(prettifier))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be a ('file)
     *                    ^
     * }}}
     */
    def be(resultOfAWordApplication: ResultOfAWordToSymbolApplication)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val matcherResult = matchSymbolToPredicateMethod(toAnyRef(e), resultOfAWordApplication.symbol, true, true, prettifier, pos)
        if (matcherResult.matches != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) matcherResult.failureMessage(prettifier) else matcherResult.negatedFailureMessage(prettifier), None, pos)
        }
        else indicateSuccess(shouldBeTrue, matcherResult.negatedFailureMessage(prettifier), matcherResult.failureMessage(prettifier))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be an ('actionKey)
     *                    ^
     * }}}
     */
    def be(resultOfAnWordApplication: ResultOfAnWordToSymbolApplication)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val matcherResult = matchSymbolToPredicateMethod(toAnyRef(e), resultOfAnWordApplication.symbol, true, false, prettifier, pos)
        if (matcherResult.matches != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) matcherResult.failureMessage(prettifier) else matcherResult.negatedFailureMessage(prettifier), None, pos)
        }
        else indicateSuccess(shouldBeTrue, matcherResult.negatedFailureMessage(prettifier), matcherResult.failureMessage(prettifier))
      }
    }
    // SKIP-SCALATESTJS-END

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be sorted
     *                    ^
     * }}}
     */
    def be(sortedWord: SortedWord)(implicit sortable: Sortable[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (sortable.isSorted(e) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotSorted(prettifier, e) else FailureMessages.wasSorted(prettifier, e), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasSorted(prettifier, e), FailureMessages.wasNotSorted(prettifier, e))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be readable
     *                    ^
     * }}}
     */
    def be(readableWord: ReadableWord)(implicit readability: Readability[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (readability.isReadable(e) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotReadable(prettifier, e) else FailureMessages.wasReadable(prettifier, e), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasReadable(prettifier, e), FailureMessages.wasNotReadable(prettifier, e))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be writable
     *                    ^
     * }}}
     */
    def be(writableWord: WritableWord)(implicit writability: Writability[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (writability.isWritable(e) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotWritable(prettifier, e) else FailureMessages.wasWritable(prettifier, e), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasWritable(prettifier, e), FailureMessages.wasNotWritable(prettifier, e))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be empty
     *                    ^
     * }}}
     */
    def be(emptyWord: EmptyWord)(implicit emptiness: Emptiness[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (emptiness.isEmpty(e) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotEmpty(prettifier, e) else FailureMessages.wasEmpty(prettifier, e), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasEmpty(prettifier, e), FailureMessages.wasNotEmpty(prettifier, e))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not be defined
     *                    ^
     * }}}
     */
    def be(definedWord: DefinedWord)(implicit definition: Definition[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (definition.isDefined(e) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.wasNotDefined(prettifier, e) else FailureMessages.wasDefined(prettifier, e), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.wasDefined(prettifier, e), FailureMessages.wasNotDefined(prettifier, e))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain (null)
     *                     ^
     * }}}
     */
    def contain(nullValue: Null)(implicit containing: Containing[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((containing.contains(e, null)) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.didNotContainNull(prettifier, e) else FailureMessages.containedNull(prettifier, e), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.containedNull(prettifier, e), FailureMessages.didNotContainNull(prettifier, e))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain ("one")
     *                     ^
     * }}}
     */
    def contain(expectedElement: Any)(implicit containing: Containing[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val right = expectedElement
        if ((containing.contains(e, right)) != shouldBeTrue) {
          indicateFailure(if (shouldBeTrue) FailureMessages.didNotContainExpectedElement(prettifier, e, right) else FailureMessages.containedExpectedElement(prettifier, e, right), None, pos)
        }
        else indicateSuccess(shouldBeTrue, FailureMessages.containedExpectedElement(prettifier, e, right), FailureMessages.didNotContainExpectedElement(prettifier, e, right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain oneOf ("one")
     *                     ^
     * }}}
     */
    def contain(oneOf: ResultOfOneOfApplication)(implicit containing: Containing[T]): Assertion = {

      val right = oneOf.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (containing.containsOneOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainOneOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedOneOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos)
        else indicateSuccess(
          shouldBeTrue,
          FailureMessages.containedOneOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
          FailureMessages.didNotContainOneOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
        )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain oneElementOf ("one")
     *                     ^
     * }}}
     */
    def contain(oneElementOf: ResultOfOneElementOfApplication)(implicit containing: Containing[T]): Assertion = {

      val right = oneElementOf.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (containing.containsOneOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(if (shouldBeTrue) FailureMessages.didNotContainOneElementOf(prettifier, e, right) else FailureMessages.containedOneElementOf(prettifier, e, right), None, pos)
        else indicateSuccess(shouldBeTrue, FailureMessages.containedOneElementOf(prettifier, e, right), FailureMessages.didNotContainOneElementOf(prettifier, e, right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain atLeastOneOf ("one")
     *                     ^
     * }}}
     */
    def contain(atLeastOneOf: ResultOfAtLeastOneOfApplication)(implicit aggregating: Aggregating[T]): Assertion = {

      val right = atLeastOneOf.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsAtLeastOneOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos)
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain atLeastOneElementOf ("one")
     *                     ^
     * }}}
     */
    def contain(atLeastOneElementOf: ResultOfAtLeastOneElementOfApplication)(implicit evidence: Aggregating[T]): Assertion = {

      val right = atLeastOneElementOf.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (evidence.containsAtLeastOneOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(if (shouldBeTrue) FailureMessages.didNotContainAtLeastOneElementOf(prettifier, e, right) else FailureMessages.containedAtLeastOneElementOf(prettifier, e, right), None, pos)
        else indicateSuccess(shouldBeTrue, FailureMessages.containedAtLeastOneElementOf(prettifier, e, right), FailureMessages.didNotContainAtLeastOneElementOf(prettifier, e, right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain noneOf ("one")
     *                     ^
     * }}}
     */
    def contain(noneOf: ResultOfNoneOfApplication)(implicit containing: Containing[T]): Assertion = {

      val right = noneOf.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (containing.containsNoneOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.containedAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.didNotContainAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        else indicateSuccess(
          shouldBeTrue,
          FailureMessages.didNotContainAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
          FailureMessages.containedAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
        )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain noElementsOf ("one")
     *                     ^
     * }}}
     */
    def contain(noElementsOf: ResultOfNoElementsOfApplication)(implicit evidence: Containing[T]): Assertion = {

      val right = noElementsOf.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (evidence.containsNoneOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(if (shouldBeTrue) FailureMessages.containedAtLeastOneElementOf(prettifier, e, right) else FailureMessages.didNotContainAtLeastOneElementOf(prettifier, e, right), None, pos)
        else indicateSuccess(shouldBeTrue, FailureMessages.didNotContainAtLeastOneElementOf(prettifier, e, right), FailureMessages.containedAtLeastOneElementOf(prettifier, e, right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain theSameElementsAs ("one")
     *                     ^
     * }}}
     */
    def contain(theSameElementsAs: ResultOfTheSameElementsAsApplication)(implicit aggregating: Aggregating[T]): Assertion = {

      val right = theSameElementsAs.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsTheSameElementsAs(e, right) != shouldBeTrue)
          indicateFailure(if (shouldBeTrue) FailureMessages.didNotContainSameElements(prettifier, e, right) else FailureMessages.containedSameElements(prettifier, e, right), None, pos)
        else indicateSuccess(shouldBeTrue, FailureMessages.containedSameElements(prettifier, e, right), FailureMessages.didNotContainSameElements(prettifier, e, right))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain theSameElementsInOrderAs ("one")
     *                     ^
     * }}}
     */
    def contain(theSameElementsInOrderAs: ResultOfTheSameElementsInOrderAsApplication)(implicit sequencing: Sequencing[T]): Assertion = {

      val right = theSameElementsInOrderAs.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (sequencing.containsTheSameElementsInOrderAs(e, right) != shouldBeTrue)
          indicateFailure(if (shouldBeTrue) FailureMessages.didNotContainSameElementsInOrder(prettifier, e, right) else FailureMessages.containedSameElementsInOrder(prettifier, e, right), None, pos)
        else indicateSuccess(shouldBeTrue, FailureMessages.containedSameElementsInOrder(prettifier, e, right), FailureMessages.didNotContainSameElementsInOrder(prettifier, e, right))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain only ("one")
     *                     ^
     * }}}
     */
    def contain(only: ResultOfOnlyApplication)(implicit aggregating: Aggregating[T]): Assertion = {

      val right = only.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsOnly(e, right) != shouldBeTrue) {
          val withFriendlyReminder = right.size == 1 && (right(0).isInstanceOf[scala.collection.GenTraversable[_]] || right(0).isInstanceOf[Every[_]])
          indicateFailure(
            if (shouldBeTrue)
              if (withFriendlyReminder)
                FailureMessages.didNotContainOnlyElementsWithFriendlyReminder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
              else
                FailureMessages.didNotContainOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))

            else
              if (withFriendlyReminder)
                FailureMessages.containedOnlyElementsWithFriendlyReminder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
              else
                FailureMessages.containedOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        }
        else indicateSuccess(
          shouldBeTrue,
          FailureMessages.containedOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
          FailureMessages.didNotContainOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
        )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain inOrderOnly ("one", "two")
     *                     ^
     * }}}
     */
    def contain(only: ResultOfInOrderOnlyApplication)(implicit sequencing: Sequencing[T]): Assertion = {

      val right = only.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (sequencing.containsInOrderOnly(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainInOrderOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedInOrderOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos)
        else indicateSuccess(
          shouldBeTrue,
          FailureMessages.containedInOrderOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
          FailureMessages.didNotContainInOrderOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
        )
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain allOf ("one")
     *                     ^
     * }}}
     */
    def contain(only: ResultOfAllOfApplication)(implicit aggregating: Aggregating[T]): Assertion = {

      val right = only.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsAllOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAllOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedAllOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAllOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainAllOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain allElementsOf ("one")
     *                     ^
     * }}}
     */
    def contain(only: ResultOfAllElementsOfApplication)(implicit evidence: Aggregating[T]): Assertion = {

      val right = only.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (evidence.containsAllOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAllElementsOf(prettifier, e, right)
            else
              FailureMessages.containedAllElementsOf(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAllElementsOf(prettifier, e, right),
            FailureMessages.didNotContainAllElementsOf(prettifier, e, right)
          )
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain inOrder ("one")
     *                     ^
     * }}}
     */
    def contain(inOrder: ResultOfInOrderApplication)(implicit sequencing: Sequencing[T]): Assertion = {

      val right = inOrder.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (sequencing.containsInOrder(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAllOfElementsInOrder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedAllOfElementsInOrder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAllOfElementsInOrder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainAllOfElementsInOrder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain inOrderElementsOf (List("one"))
     *                     ^
     * }}}
     */
    def contain(inOrderElementsOf: ResultOfInOrderElementsOfApplication)(implicit evidence: Sequencing[T]): Assertion = {

      val right = inOrderElementsOf.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (evidence.containsInOrder(e, right.distinct) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAllElementsOfInOrder(prettifier, e, right)
            else
              FailureMessages.containedAllElementsOfInOrder(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAllElementsOfInOrder(prettifier, e, right),
            FailureMessages.didNotContainAllElementsOfInOrder(prettifier, e, right)
          )
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain atMostOneOf ("one")
     *                     ^
     * }}}
     */
    def contain(atMostOneOf: ResultOfAtMostOneOfApplication)(implicit aggregating: Aggregating[T]): Assertion = {

      val right = atMostOneOf.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsAtMostOneOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAtMostOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedAtMostOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAtMostOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainAtMostOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should not contain atMostOneElementOf List("one")
     *                     ^
     * }}}
     */
    def contain(atMostOneElementOf: ResultOfAtMostOneElementOfApplication)(implicit evidence: Aggregating[T]): Assertion = {

      val right = atMostOneElementOf.right

      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (evidence.containsAtMostOneOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAtMostOneElementOf(prettifier, e, right)
            else
              FailureMessages.containedAtMostOneElementOf(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAtMostOneElementOf(prettifier, e, right),
            FailureMessages.didNotContainAtMostOneElementOf(prettifier, e, right)
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(colOfMap) should not contain key ("three")
     *                          ^
     * }}}
     */
    def contain(resultOfKeyWordApplication: ResultOfKeyWordApplication)(implicit keyMapping: KeyMapping[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { map =>
        val expectedKey = resultOfKeyWordApplication.expectedKey
        if ((keyMapping.containsKey(map, expectedKey)) != shouldBeTrue) {
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainKey(prettifier, map, expectedKey)
            else
              FailureMessages.containedKey(prettifier, map, expectedKey),
            None,
            pos
          )
        }
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedKey(prettifier, map, expectedKey),
            FailureMessages.didNotContainKey(prettifier, map, expectedKey)
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(colOfMap) should not contain value (3)
     *                          ^
     * }}}
     */
    def contain(resultOfValueWordApplication: ResultOfValueWordApplication)(implicit valueMapping: ValueMapping[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { map =>
        val expectedValue = resultOfValueWordApplication.expectedValue
        if ((valueMapping.containsValue(map, expectedValue)) != shouldBeTrue) {
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainValue(prettifier, map, expectedValue)
            else
              FailureMessages.containedValue(prettifier, map, expectedValue),
            None,
            pos
          )
        }
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedValue(prettifier, map, expectedValue),
            FailureMessages.didNotContainValue(prettifier, map, expectedValue)
          )
      }
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should not startWith ("1.7")
     *                        ^
     * }}}
     */
    def startWith(right: String)(implicit ev: T <:< String): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((e.indexOf(right) == 0) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotStartWith(prettifier, e, right)
            else
              FailureMessages.startedWith(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.startedWith(prettifier, e, right),
            FailureMessages.didNotStartWith(prettifier, e, right)
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should not startWith regex ("Hel*o")
     *                        ^
     * }}}
     *
     * The regular expression passed following the `regex` token can be either a `String`
     * or a `scala.util.matching.Regex`.
     * 
     */
    def startWith(resultOfRegexWordApplication: ResultOfRegexWordApplication)(implicit ev: T <:< String): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = startWithRegexWithGroups(e, resultOfRegexWordApplication.regex, resultOfRegexWordApplication.groups)
        if (result.matches != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              result.failureMessage(prettifier)
            else
              result.negatedFailureMessage(prettifier),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            result.negatedFailureMessage(prettifier),
            result.failureMessage(prettifier)
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should not endWith ("1.7")
     *                        ^
     * }}}
     */
    def endWith(expectedSubstring: String)(implicit ev: T <:< String): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((e endsWith expectedSubstring) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotEndWith(prettifier, e, expectedSubstring)
            else
              FailureMessages.endedWith(prettifier, e, expectedSubstring),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.endedWith(prettifier, e, expectedSubstring),
            FailureMessages.didNotEndWith(prettifier, e, expectedSubstring)
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should not endWith regex ("wor.d")
     *                        ^
     * }}}
     */
    def endWith(resultOfRegexWordApplication: ResultOfRegexWordApplication)(implicit ev: T <:< String): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = endWithRegexWithGroups(e, resultOfRegexWordApplication.regex, resultOfRegexWordApplication.groups)
        if (result.matches != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              result.failureMessage(prettifier)
            else
              result.negatedFailureMessage(prettifier),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            result.negatedFailureMessage(prettifier),
            result.failureMessage(prettifier)
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should not include regex ("wo.ld")
     *                        ^
     * }}}
     *
     * The regular expression passed following the `regex` token can be either a `String`
     * or a `scala.util.matching.Regex`.
     * 
     */
    def include(resultOfRegexWordApplication: ResultOfRegexWordApplication)(implicit ev: T <:< String): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = includeRegexWithGroups(e, resultOfRegexWordApplication.regex, resultOfRegexWordApplication.groups)
        if (result.matches != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              result.failureMessage(prettifier)
            else
              result.negatedFailureMessage(prettifier),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            result.negatedFailureMessage(prettifier),
            result.failureMessage(prettifier)
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should not include ("world")
     *                        ^
     * }}}
     */
    def include(expectedSubstring: String)(implicit ev: T <:< String): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((e.indexOf(expectedSubstring) >= 0) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotIncludeSubstring(prettifier, e, expectedSubstring)
            else
              FailureMessages.includedSubstring(prettifier, e, expectedSubstring),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.includedSubstring(prettifier, e, expectedSubstring),
            FailureMessages.didNotIncludeSubstring(prettifier, e, expectedSubstring)
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should not fullyMatch regex ("""(-)?(\d+)(\.\d*)?""")
     *                        ^
     * }}}
     *
     * The regular expression passed following the `regex` token can be either a `String`
     * or a `scala.util.matching.Regex`.
     * 
     */
    def fullyMatch(resultOfRegexWordApplication: ResultOfRegexWordApplication)(implicit ev: T <:< String): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = fullyMatchRegexWithGroups(e, resultOfRegexWordApplication.regex, resultOfRegexWordApplication.groups)
        if (result.matches != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              result.failureMessage(prettifier)
            else
              result.negatedFailureMessage(prettifier),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            result.negatedFailureMessage(prettifier),
            result.failureMessage(prettifier)
          )
      }
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfNotWordForCollectedAny([collected], [xs], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfNotWordForCollectedAny(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="InspectorsMatchers.html">`InspectorsMatchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  final class ResultOfContainWordForCollectedAny[T](collected: Collected, xs: scala.collection.GenTraversable[T], original: Any, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain oneOf (1, 2)
     *                       ^
     * }}}
     */
    def oneOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit containing: Containing[T]): Assertion = {
      val right = firstEle :: secondEle :: remainingEles.toList
      if (right.distinct.size != right.size)
        throw new NotAllowedException(FailureMessages.oneOfDuplicate, pos)
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (containing.containsOneOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainOneOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedOneOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
        )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedOneOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainOneOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain oneElementOf List(1, 2)
     *                       ^
     * }}}
     */
    def oneElementOf(elements: GenTraversable[Any])(implicit containing: Containing[T]): Assertion = {
      val right = elements.toList
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (containing.containsOneOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainOneElementOf(prettifier, e, right)
            else
              FailureMessages.containedOneElementOf(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedOneElementOf(prettifier, e, right),
            FailureMessages.didNotContainOneElementOf(prettifier, e, right)
          )
      }
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain atLeastOneOf (1, 2)
     *                       ^
     * }}}
     */
    def atLeastOneOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit aggregating: Aggregating[T]): Assertion = {
      val right = firstEle :: secondEle :: remainingEles.toList
      if (right.distinct.size != right.size)
        throw new NotAllowedException(FailureMessages.atLeastOneOfDuplicate, pos)
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsAtLeastOneOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
        )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain atLeastOneElementOf List(1, 2)
     *                       ^
     * }}}
     */
    def atLeastOneElementOf(elements: GenTraversable[Any])(implicit aggregating: Aggregating[T]): Assertion = {
      val right = elements.toList
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsAtLeastOneOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAtLeastOneElementOf(prettifier, e, right)
            else
              FailureMessages.containedAtLeastOneElementOf(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAtLeastOneElementOf(prettifier, e, right),
            FailureMessages.didNotContainAtLeastOneElementOf(prettifier, e, right)
          )
      }
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain noneOf (1, 2)
     *                       ^
     * }}}
     */
    def noneOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit containing: Containing[T]): Assertion = {
      val right = firstEle :: secondEle :: remainingEles.toList
      if (right.distinct.size != right.size)
        throw new NotAllowedException(FailureMessages.noneOfDuplicate, pos)
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (containing.containsNoneOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.containedAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.didNotContainAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.didNotContainAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.containedAtLeastOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain noElementsOf (1, 2)
     *                       ^
     * }}}
     */
    def noElementsOf(elements: GenTraversable[Any])(implicit containing: Containing[T]): Assertion = {
      val right = elements.toList
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (containing.containsNoneOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.containedAtLeastOneElementOf(prettifier, e, right)
            else
              FailureMessages.didNotContainAtLeastOneElementOf(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.didNotContainAtLeastOneElementOf(prettifier, e, right),
            FailureMessages.containedAtLeastOneElementOf(prettifier, e, right)
          )
      }
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain theSameElementsAs (1, 2)
     *                       ^
     * }}}
     */
    def theSameElementsAs(right: GenTraversable[_])(implicit aggregating: Aggregating[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsTheSameElementsAs(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainSameElements(prettifier, e, right)
            else
              FailureMessages.containedSameElements(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedSameElements(prettifier, e, right),
            FailureMessages.didNotContainSameElements(prettifier, e, right)
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain theSameElementsInOrderAs (1, 2)
     *                       ^
     * }}}
     */
    def theSameElementsInOrderAs(right: GenTraversable[_])(implicit sequencing: Sequencing[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (sequencing.containsTheSameElementsInOrderAs(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainSameElementsInOrder(prettifier, e, right)
            else
              FailureMessages.containedSameElementsInOrder(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedSameElementsInOrder(prettifier, e, right),
            FailureMessages.didNotContainSameElementsInOrder(prettifier, e, right)
          )
      }
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain only (1, 2)
     *                       ^
     * }}}
     */
    def only(right: Any*)(implicit aggregating: Aggregating[T]): Assertion = {
      if (right.isEmpty)
        throw new NotAllowedException(FailureMessages.onlyEmpty, pos)
      if (right.distinct.size != right.size)
        throw new NotAllowedException(FailureMessages.onlyDuplicate, pos)
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsOnly(e, right) != shouldBeTrue) {
          val withFriendlyReminder = right.size == 1 && (right(0).isInstanceOf[scala.collection.GenTraversable[_]] || right(0).isInstanceOf[Every[_]])
          indicateFailure(
            if (shouldBeTrue)
              if (withFriendlyReminder)
                FailureMessages.didNotContainOnlyElementsWithFriendlyReminder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
              else
                FailureMessages.didNotContainOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              if (withFriendlyReminder)
                FailureMessages.containedOnlyElementsWithFriendlyReminder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
              else
                FailureMessages.containedOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        }
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain inOrderOnly (1, 2)
     *                       ^
     * }}}
     */
    def inOrderOnly(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit sequencing: Sequencing[T]): Assertion = {
      val right = firstEle :: secondEle :: remainingEles.toList
      if (right.distinct.size != right.size)
        throw new NotAllowedException(FailureMessages.inOrderOnlyDuplicate, pos)
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (sequencing.containsInOrderOnly(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainInOrderOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedInOrderOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedInOrderOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainInOrderOnlyElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain allOf (1, 2)
     *                       ^
     * }}}
     */
    def allOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit aggregating: Aggregating[T]): Assertion = {
      val right = firstEle :: secondEle :: remainingEles.toList
      if (right.distinct.size != right.size)
        throw new NotAllowedException(FailureMessages.allOfDuplicate, pos)
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsAllOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAllOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedAllOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAllOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainAllOfElements(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain allElementsOf (1, 2)
     *                       ^
     * }}}
     */
    def allElementsOf(elements: GenTraversable[Any])(implicit aggregating: Aggregating[T]): Assertion = {
      val right = elements.toList
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsAllOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAllElementsOf(prettifier, e, right)
            else
              FailureMessages.containedAllElementsOf(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAllElementsOf(prettifier, e, right),
            FailureMessages.didNotContainAllElementsOf(prettifier, e, right)
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain inOrder (1, 2)
     *                       ^
     * }}}
     */
    def inOrder(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit sequencing: Sequencing[T]): Assertion = {
      val right = firstEle :: secondEle :: remainingEles.toList
      if (right.distinct.size != right.size)
        throw new NotAllowedException(FailureMessages.inOrderDuplicate, pos)
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (sequencing.containsInOrder(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAllOfElementsInOrder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedAllOfElementsInOrder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAllOfElementsInOrder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainAllOfElementsInOrder(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * option should contain inOrderElementsOf (1, 2)
     *                       ^
     * }}}
     */
    def inOrderElementsOf(elements: GenTraversable[Any])(implicit sequencing: Sequencing[T]): Assertion = {
      val right = elements.toList
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (sequencing.containsInOrder(e, right.distinct) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAllElementsOfInOrder(prettifier, e, right)
            else
              FailureMessages.containedAllElementsOfInOrder(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAllElementsOfInOrder(prettifier, e, right),
            FailureMessages.didNotContainAllElementsOfInOrder(prettifier, e, right)
          )
      }
    }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should contain atMostOneOf (1, 2)
     *                        ^
     * }}}
     */
    def atMostOneOf(firstEle: Any, secondEle: Any, remainingEles: Any*)(implicit aggregating: Aggregating[T]): Assertion = {
      val right = firstEle :: secondEle :: remainingEles.toList
      if (right.distinct.size != right.size)
        throw new NotAllowedException(FailureMessages.atMostOneOfDuplicate, pos)
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsAtMostOneOf(e, right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAtMostOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
            else
              FailureMessages.containedAtMostOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAtMostOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", "))),
            FailureMessages.didNotContainAtMostOneOf(prettifier, e, UnquotedString(right.map(r => FailureMessages.decorateToStringValue(prettifier, r)).mkString(", ")))
          )
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should contain atMostOneElementOf (1, 2)
     *                        ^
     * }}}
     */
    def atMostOneElementOf(elements: GenTraversable[Any])(implicit aggregating: Aggregating[T]): Assertion = {
      val right = elements.toList
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (aggregating.containsAtMostOneOf(e, right.distinct) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainAtMostOneElementOf(prettifier, e, right)
            else
              FailureMessages.containedAtMostOneElementOf(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedAtMostOneElementOf(prettifier, e, right),
            FailureMessages.didNotContainAtMostOneElementOf(prettifier, e, right)
          )
      }
    }

   /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(colOfMap) should contain key ("one")
     *                              ^
     * }}}
     */
    def key(expectedKey: Any)(implicit keyMapping: KeyMapping[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { map =>
        if (keyMapping.containsKey(map, expectedKey) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainKey(prettifier, map, expectedKey)
            else
              FailureMessages.containedKey(prettifier, map, expectedKey),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedKey(prettifier, map, expectedKey),
            FailureMessages.didNotContainKey(prettifier, map, expectedKey)
          )
      }
    }

   /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(colOfMap) should contain value (1)
     *                              ^
     * }}}
     */
    def value(expectedValue: Any)(implicit valueMapping: ValueMapping[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { map =>
        if (valueMapping.containsValue(map, expectedValue) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.didNotContainValue(prettifier, map, expectedValue)
            else
              FailureMessages.containedValue(prettifier, map, expectedValue),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.containedValue(prettifier, map, expectedValue),
            FailureMessages.didNotContainValue(prettifier, map, expectedValue)
          )
      }
    }
    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfContainWordForCollectedAny([collected], [xs], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfContainWordForCollectedAny(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="InspectorsMatchers.html">`InspectorsMatchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  sealed class ResultOfBeWordForCollectedAny[T](collected: Collected, xs: scala.collection.GenTraversable[T], original: Any, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    // TODO: Missing should(AMatcher) and should(AnMatcher)

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should be theSameInstanceAs anotherObject
     *                   ^
     * }}}
     */
    def theSameInstanceAs(right: AnyRef)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((toAnyRef(e) eq right) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.wasNotSameInstanceAs(prettifier, e, right)
            else
              FailureMessages.wasSameInstanceAs(prettifier, e, right),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.wasSameInstanceAs(prettifier, e, right),
            FailureMessages.wasNotSameInstanceAs(prettifier, e, right)
          )
      }
    }

    // SKIP-SCALATESTJS-START
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should be a ('file)
     *                   ^
     * }}}
     */
    def a(symbol: Symbol)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val matcherResult = matchSymbolToPredicateMethod(toAnyRef(e), symbol, true, true, prettifier, pos)
        if (matcherResult.matches != shouldBeTrue) {
          indicateFailure(
            if (shouldBeTrue)
              matcherResult.failureMessage(prettifier)
            else
              matcherResult.negatedFailureMessage(prettifier),
            None,
            pos
          )
        }
        else
          indicateSuccess(
            shouldBeTrue,
            matcherResult.negatedFailureMessage(prettifier),
            matcherResult.failureMessage(prettifier)
          )
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should be an ('orange)
     *                   ^
     * }}}
     */
    def an(symbol: Symbol)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val matcherResult = matchSymbolToPredicateMethod(toAnyRef(e), symbol, true, false, prettifier, pos)
        if (matcherResult.matches != shouldBeTrue) {
          indicateFailure(
            if (shouldBeTrue)
              matcherResult.failureMessage(prettifier)
            else
              matcherResult.negatedFailureMessage(prettifier),
            None,
            pos
          )
        }
        else
          indicateSuccess(
            shouldBeTrue,
            matcherResult.negatedFailureMessage(prettifier),
            matcherResult.failureMessage(prettifier)
          )
      }
    }
    // SKIP-SCALATESTJS-END
    
    /**
     * This method enables the following syntax, where `badBook` is, for example, of type `Book` and
     * `goodRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(books) should be a (goodRead)
     *                      ^
     * }}}
     */
    def a[U <: T](bePropertyMatcher: BePropertyMatcher[U])(implicit ev: T <:< AnyRef): Assertion = { // TODO: Try supporting 2.10 AnyVals
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = bePropertyMatcher(e.asInstanceOf[U])
        if (result.matches != shouldBeTrue) {
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.wasNotA(prettifier, e, UnquotedString(result.propertyName))
            else
              FailureMessages.wasA(prettifier, e, UnquotedString(result.propertyName)),
            None,
            pos
          )
        }
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.wasA(prettifier, e, UnquotedString(result.propertyName)),
            FailureMessages.wasNotA(prettifier, e, UnquotedString(result.propertyName))
          )
      }
    }

    /**
     * This method enables the following syntax, where `badBook` is, for example, of type `Book` and
     * `excellentRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(books) should be an (excellentRead)
     *                      ^
     * }}}
     */
    def an[U <: T](beTrueMatcher: BePropertyMatcher[U])(implicit ev: T <:< AnyRef): Assertion = { // TODO: Try supporting 2.10 AnyVals
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val beTrueMatchResult = beTrueMatcher(e.asInstanceOf[U])
        if (beTrueMatchResult.matches != shouldBeTrue) {
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.wasNotAn(prettifier, e, UnquotedString(beTrueMatchResult.propertyName))
            else
              FailureMessages.wasAn(prettifier, e, UnquotedString(beTrueMatchResult.propertyName)),
            None,
            pos
          )
        }
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.wasAn(prettifier, e, UnquotedString(beTrueMatchResult.propertyName)),
            FailureMessages.wasNotAn(prettifier, e, UnquotedString(beTrueMatchResult.propertyName))
          )
      }
    }

    /**
     * This method enables the following syntax, where `fraction` is, for example, of type `PartialFunction`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should be definedAt (6)
     *                   ^
     * }}}
     */
    def definedAt[U](right: U)(implicit ev: T <:< PartialFunction[U, _]): Assertion = {
      doCollected(collected, xs, xs, prettifier, pos) { e =>
      if (e.isDefinedAt(right) != shouldBeTrue)
        indicateFailure(
          if (shouldBeTrue)
            FailureMessages.wasNotDefinedAt(prettifier, e, right)
          else
            FailureMessages.wasDefinedAt(prettifier, e, right),
          None,
          pos
        )
        else
        indicateSuccess(
          shouldBeTrue,
          FailureMessages.wasDefinedAt(prettifier, e, right),
          FailureMessages.wasNotDefinedAt(prettifier, e, right)
        )
      }
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfBeWordForCollectedAny([collected], [xs], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfBeWordForCollectedAny(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  // SKIP-SCALATESTJS-START
  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="InspectorsMatchers.html">`InspectorsMatchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  final class ResultOfBeWordForCollectedArray[T](collected: Collected, xs: scala.collection.GenTraversable[Array[T]], original: Any, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position)
    extends ResultOfBeWordForCollectedAny(collected, xs, original, shouldBeTrue, prettifier, pos) {
  
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(colOfArray) should be ('empty)
     *                           ^
     * }}}
     */
    def apply(right: Symbol): Matcher[Array[T]] =
      new Matcher[Array[T]] {
        def apply(left: Array[T]): MatchResult = matchSymbolToPredicateMethod(left.deep, right, false, false, prettifier, pos)
      }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfBeWordForCollectedArray([collected], [xs], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfBeWordForCollectedArray(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }
  // SKIP-SCALATESTJS-END
  
  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="InspectorsMatchers.html">`InspectorsMatchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  final class ResultOfCollectedAny[T](collected: Collected, xs: scala.collection.GenTraversable[T], original: Any, prettifier: Prettifier, pos: source.Position) {

// TODO: shouldBe null works, b ut should be (null) does not when type is Any: 
/*
scala> val ys = List(null, null, 1)
ys: List[Any] = List(null, null, 1)

scala> all (ys) shouldBe null
<console>:15: error: ambiguous reference to overloaded definition,
both method shouldBe in class ResultOfCollectedAny of type (spread: org.scalactic.Spread[Any])Unit
and  method shouldBe in class ResultOfCollectedAny of type (beMatcher: org.scalatest.matchers.BeMatcher[Any])Unit
match argument types (Null)
              all (ys) shouldBe null
                       ^

scala> all (ys) should be (null)
org.scalatest.exceptions.TestFailedException: org.scalatest.Matchers$ResultOfCollectedAny@18515783 was not null
	at org.scalatest.MatchersHelper$.newTestFailedException(MatchersHelper.scala:163)
	at org.scalatest.Matchers$ShouldMethodHelper$.shouldMatcher(Matchers.scala:5529)
	at org.scalatest.Matchers$AnyShouldWrapper.should(Matchers.scala:5563)
	at .<init>(<console>:15)
	at .<clinit>(<console>)
*/

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should be (3)
     *         ^
     * }}}
     */
    def should(rightMatcher: Matcher[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = rightMatcher(e)
        MatchFailed.unapply(result)(prettifier) match {
          case Some(failureMessage) =>
            indicateFailure(failureMessage, None, pos)
          case None => indicateSuccess(result.negatedFailureMessage(prettifier))
        }
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) shouldEqual 7
     *          ^
     * }}}
     */
    def shouldEqual(right: Any)(implicit equality: Equality[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!equality.areEqual(e, right)) {
          val (eee, rightee) = Suite.getObjectsForFailureMessage(e, right)
          indicateFailure(FailureMessages.didNotEqual(prettifier, eee, rightee), None, pos)
        }
        else indicateSuccess(FailureMessages.equaled(prettifier, e, right))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldEqual 7.1 +- 0.2
     *        ^doCollected
     * }}}
     */
    def shouldEqual(spread: Spread[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!spread.isWithin(e)) {
          indicateFailure(FailureMessages.didNotEqualPlusOrMinus(prettifier, e, spread.pivot, spread.tolerance), None, pos)
        }
        else indicateSuccess(FailureMessages.equaledPlusOrMinus(prettifier, e, spread.pivot, spread.tolerance))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe sorted
     *         ^
     * }}}
     */
    def shouldBe(sortedWord: SortedWord)(implicit sortable: Sortable[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!sortable.isSorted(e))
          indicateFailure(FailureMessages.wasNotSorted(prettifier, e), None, pos)
        else indicateSuccess(FailureMessages.wasSorted(prettifier, e))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe readable
     *         ^
     * }}}
     */
    def shouldBe(readableWord: ReadableWord)(implicit readability: Readability[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!readability.isReadable(e))
          indicateFailure(FailureMessages.wasNotReadable(prettifier, e), None, pos)
        else indicateSuccess(FailureMessages.wasReadable(prettifier, e))
      }
    }
 
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe writable
     *         ^
     * }}}
     */
    def shouldBe(writableWord: WritableWord)(implicit writability: Writability[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!writability.isWritable(e))
          indicateFailure(FailureMessages.wasNotWritable(prettifier, e), None, pos)
        else indicateSuccess(FailureMessages.wasWritable(prettifier, e))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe empty
     *         ^
     * }}}
     */
    def shouldBe(emptyWord: EmptyWord)(implicit emptiness: Emptiness[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!emptiness.isEmpty(e))
          indicateFailure(FailureMessages.wasNotEmpty(prettifier, e), None, pos)
        else indicateSuccess(FailureMessages.wasEmpty(prettifier, e))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe defined
     *         ^
     * }}}
     */
    def shouldBe(definedWord: DefinedWord)(implicit definition: Definition[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!definition.isDefined(e))
          indicateFailure(FailureMessages.wasNotDefined(prettifier, e), None, pos)
        else indicateSuccess(FailureMessages.wasDefined(prettifier, e))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe a [Type]
     *         ^
     * }}}
     */
    def shouldBe(aType: ResultOfATypeInvocation[_]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!aType.clazz.isAssignableFrom(e.getClass))
          indicateFailure(FailureMessages.wasNotAnInstanceOf(prettifier, e, UnquotedString(aType.clazz.getName), UnquotedString(e.getClass.getName)), None, pos)
        else indicateSuccess(FailureMessages.wasAnInstanceOf(prettifier, e, UnquotedString(aType.clazz.getName)))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe an [Type]
     *         ^
     * }}}
     */
    def shouldBe(anType: ResultOfAnTypeInvocation[_]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!anType.clazz.isAssignableFrom(e.getClass))
          indicateFailure(FailureMessages.wasNotAnInstanceOf(prettifier, e, UnquotedString(anType.clazz.getName), UnquotedString(e.getClass.getName)), None, pos)
        else indicateSuccess(FailureMessages.wasAnInstanceOf(prettifier, e, UnquotedString(anType.clazz.getName)))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldEqual null
     *        ^
     * }}}
     */
    def shouldEqual(right: Null)(implicit ev: T <:< AnyRef): Assertion = { 
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (e != null) {
          indicateFailure(FailureMessages.didNotEqualNull(prettifier, e), None, pos)
        }
        else indicateSuccess(FailureMessages.equaledNull)
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should equal (3)
     *         ^
     * }}}
     */
    def should[TYPECLASS1[_]](rightMatcherFactory1: MatcherFactory1[T, TYPECLASS1])(implicit typeClass1: TYPECLASS1[T]): Assertion = {
      val rightMatcher = rightMatcherFactory1.matcher
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = rightMatcher(e)
        MatchFailed.unapply(result)(prettifier) match {
          case Some(failureMessage) =>
            indicateFailure(failureMessage, None, pos)
          case None => indicateSuccess(result.negatedFailureMessage(prettifier))
        }
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should (equal (expected) and have length 12)
     *         ^
     * }}}
     */
    def should[TYPECLASS1[_], TYPECLASS2[_]](rightMatcherFactory2: MatcherFactory2[T, TYPECLASS1, TYPECLASS2])(implicit typeClass1: TYPECLASS1[T], typeClass2: TYPECLASS2[T]): Assertion = {
      val rightMatcher = rightMatcherFactory2.matcher
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = rightMatcher(e)
        MatchFailed.unapply(result)(prettifier) match {
          case Some(failureMessage) =>
            indicateFailure(failureMessage, None, pos)
          case None => indicateSuccess(result.negatedFailureMessage(prettifier))
        }
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should be theSameInstanceAs anotherObject
     *         ^
     * }}}
     */
    def should(beWord: BeWord): ResultOfBeWordForCollectedAny[T] =
      new ResultOfBeWordForCollectedAny[T](collected, xs, original, true, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not equal (3)
     *         ^
     * }}}
     */
    def should(notWord: NotWord): ResultOfNotWordForCollectedAny[T] = 
      new ResultOfNotWordForCollectedAny(collected, xs, original, false, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (results) should have length (3)
     *        ^
     * all (results) should have size (3)
     *        ^
     * }}}
     */
    def should(haveWord: HaveWord): ResultOfHaveWordForCollectedExtent[T] =
      new ResultOfHaveWordForCollectedExtent(collected, xs, original, true, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) shouldBe 7
     *          ^
     * }}}
     */
    def shouldBe(right: Any): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (e != right) {
          val (eee, rightee) = Suite.getObjectsForFailureMessage(e, right)
          indicateFailure(FailureMessages.wasNot(prettifier, eee, rightee), None, pos)
        }
        else indicateSuccess(FailureMessages.was(prettifier, e, right))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(4, 5, 6) shouldBe &lt; (7) 
     *              ^
     * }}} 
     */
    def shouldBe(comparison: ResultOfLessThanComparison[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!comparison(e)) {
          indicateFailure(
            FailureMessages.wasNotLessThan(prettifier,
              e,
              comparison.right
            ), 
            None,
            pos
          ) 
        }
        else indicateSuccess(FailureMessages.wasLessThan(prettifier, e, comparison.right))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(4, 5, 6) shouldBe &lt;= (7) 
     *              ^
     * }}} 
     */
    def shouldBe(comparison: ResultOfLessThanOrEqualToComparison[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!comparison(e)) {
          indicateFailure(
            FailureMessages.wasNotLessThanOrEqualTo(prettifier,
              e,
              comparison.right
            ), 
            None,
            pos
          ) 
        }
        else indicateSuccess(FailureMessages.wasLessThanOrEqualTo(prettifier, e, comparison.right))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(8, 9, 10) shouldBe &gt; (7) 
     *               ^
     * }}} 
     */
    def shouldBe(comparison: ResultOfGreaterThanComparison[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!comparison(e)) {
          indicateFailure(
            FailureMessages.wasNotGreaterThan(prettifier,
              e,
              comparison.right
            ), 
            None,
            pos
          ) 
        }
        else indicateSuccess(FailureMessages.wasGreaterThan(prettifier, e, comparison.right))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(8, 9, 10) shouldBe &gt;= (7) 
     *               ^
     * }}} 
     */
    def shouldBe(comparison: ResultOfGreaterThanOrEqualToComparison[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!comparison(e)) {
          indicateFailure(
            FailureMessages.wasNotGreaterThanOrEqualTo(prettifier,
              e,
              comparison.right
            ), 
            None,
            pos
          ) 
        }
        else indicateSuccess(FailureMessages.wasGreaterThanOrEqualTo(prettifier, e, comparison.right))
      }
    }

    /**
     * This method enables the following syntax, where `odd` refers to a `BeMatcher[Int]`:
     *
     * {{{  <!-- class="stHighlight" -->testing
     * all(xs) shouldBe odd
     *         ^
     * }}}
     */
    def shouldBe(beMatcher: BeMatcher[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = beMatcher.apply(e)
        if (!result.matches)
          indicateFailure(result.failureMessage(prettifier), None, pos)
        else indicateSuccess(result.negatedFailureMessage(prettifier))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe 7.1 +- 0.2
     *         ^
     * }}}
     */
    def shouldBe(spread: Spread[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!spread.isWithin(e))
          indicateFailure(FailureMessages.wasNotPlusOrMinus(prettifier, e, spread.pivot, spread.tolerance), None, pos)
        else indicateSuccess(FailureMessages.wasPlusOrMinus(prettifier, e, spread.pivot, spread.tolerance))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe theSameInstanceAs (anotherObject)
     *         ^
     * }}}
     */
    def shouldBe(resultOfSameInstanceAsApplication: ResultOfTheSameInstanceAsApplication)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (toAnyRef(e) ne resultOfSameInstanceAsApplication.right)
          indicateFailure(
            FailureMessages.wasNotSameInstanceAs(prettifier,
              e,
              resultOfSameInstanceAsApplication.right
            ),
            None,
            pos
          )
        else indicateSuccess(FailureMessages.wasSameInstanceAs(prettifier, e, resultOfSameInstanceAsApplication.right))
      }
    }

    // SKIP-SCALATESTJS-START
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe 'empty
     *         ^
     * }}}
     */
    def shouldBe(symbol: Symbol)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val matcherResult = matchSymbolToPredicateMethod(toAnyRef(e), symbol, false, true, prettifier, pos)
        if (!matcherResult.matches) 
          indicateFailure(matcherResult.failureMessage(prettifier), None, pos)
        else indicateSuccess(matcherResult.negatedFailureMessage(prettifier))
      }
    }
    
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe a ('empty)
     *         ^
     * }}}
     */
    def shouldBe(resultOfAWordApplication: ResultOfAWordToSymbolApplication)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val matcherResult = matchSymbolToPredicateMethod(toAnyRef(e), resultOfAWordApplication.symbol, true, true, prettifier, pos)
        if (!matcherResult.matches) {
          indicateFailure(matcherResult.failureMessage(prettifier), None, pos)
        }
        else indicateSuccess(matcherResult.negatedFailureMessage(prettifier))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe an ('empty)
     *         ^
     * }}}
     */
    def shouldBe(resultOfAnWordApplication: ResultOfAnWordToSymbolApplication)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val matcherResult = matchSymbolToPredicateMethod(toAnyRef(e), resultOfAnWordApplication.symbol, true, false, prettifier, pos)
        if (!matcherResult.matches) {
          indicateFailure(matcherResult.failureMessage(prettifier), None, pos)
        }
        else indicateSuccess(matcherResult.negatedFailureMessage(prettifier))
      }
    }
    // SKIP-SCALATESTJS-END

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe null
     *         ^
     * }}}
     */
    def shouldBe(o: Null)(implicit ev: T <:< AnyRef): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (e != null)
         indicateFailure(FailureMessages.wasNotNull(prettifier, e), None, pos)
        else indicateSuccess(FailureMessages.wasNull)
      }
    }

    /**
     * This method enables the following syntax, where `excellentRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe excellentRead
     *         ^
     * }}}
     */
    def shouldBe[U <: T](bePropertyMatcher: BePropertyMatcher[U])(implicit ev: T <:< AnyRef): Assertion = { // TODO: Try supporting this with 2.10 AnyVals
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = bePropertyMatcher(e.asInstanceOf[U])
        if (!result.matches) 
          indicateFailure(FailureMessages.wasNot(prettifier, e, UnquotedString(result.propertyName)), None, pos)
        else indicateSuccess(FailureMessages.was(prettifier, e, UnquotedString(result.propertyName)))
      }
    }

    /**
     * This method enables the following syntax, where `goodRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe a (goodRead)
     *         ^
     * }}}
     */
    def shouldBe[U <: T](resultOfAWordApplication: ResultOfAWordToBePropertyMatcherApplication[U])(implicit ev: T <:< AnyRef): Assertion = {// TODO: Try supporting this with 2.10 AnyVals
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = resultOfAWordApplication.bePropertyMatcher(e.asInstanceOf[U])
        if (!result.matches)
          indicateFailure(FailureMessages.wasNotA(prettifier, e, UnquotedString(result.propertyName)), None, pos)
        else indicateSuccess(FailureMessages.was(prettifier, e, UnquotedString(result.propertyName)))
      }
    }

    /**
     * This method enables the following syntax, where `excellentRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldBe an (excellentRead)
     *         ^
     * }}}
     */
    def shouldBe[U <: T](resultOfAnWordApplication: ResultOfAnWordToBePropertyMatcherApplication[U])(implicit ev: T <:< AnyRef): Assertion = {// TODO: Try supporting this with 2.10 AnyVals
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = resultOfAnWordApplication.bePropertyMatcher(e.asInstanceOf[U])
        if (!result.matches)
          indicateFailure(FailureMessages.wasNotAn(prettifier, e, UnquotedString(result.propertyName)), None, pos)
        else indicateSuccess(FailureMessages.wasAn(prettifier, e, UnquotedString(result.propertyName)))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldNot (be (3))
     *         ^
     * }}}
     */
    def shouldNot[U <: T](rightMatcherX1: Matcher[U]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        try  {
          val result = rightMatcherX1.apply(e.asInstanceOf[U])
          if (result.matches)
            indicateFailure(result.negatedFailureMessage(prettifier), None, pos)
          else indicateSuccess(result.failureMessage(prettifier))
        }
        catch {
          case tfe: TestFailedException =>
            indicateFailure(tfe.getMessage, tfe.cause, pos)
        }
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldNot (equal (3))
     *         ^
     * }}}
     */
    def shouldNot[TYPECLASS1[_]](rightMatcherFactory1: MatcherFactory1[T, TYPECLASS1])(implicit typeClass1: TYPECLASS1[T]): Assertion = {
      val rightMatcher = rightMatcherFactory1.matcher
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = rightMatcher(e)
        MatchSucceeded.unapply(result)(prettifier) match {
          case Some(negatedFailureMessage) =>
            indicateFailure(negatedFailureMessage, None, pos)
          case None => indicateSuccess(result.failureMessage(prettifier))
        }
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should === (b)
     *          ^
     * }}}
     */
    def should[U](inv: TripleEqualsInvocation[U])(implicit constraint: T CanEqual U): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((constraint.areEqual(e, inv.right)) != inv.expectingEqual)
          indicateFailure(
            if (inv.expectingEqual)
              FailureMessages.didNotEqual(prettifier, e, inv.right)
            else
              FailureMessages.equaled(prettifier, e, inv.right),
            None,
            pos
          )
        else indicateSuccess(inv.expectingEqual, FailureMessages.equaled(prettifier, e, inv.right), FailureMessages.didNotEqual(prettifier, e, inv.right))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should === (100 +- 1)
     *          ^
     * }}}
     */
    def should(inv: TripleEqualsInvocationOnSpread[T])(implicit ev: Numeric[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if ((inv.spread.isWithin(e)) != inv.expectingEqual)
          indicateFailure(
            if (inv.expectingEqual)
              FailureMessages.didNotEqualPlusOrMinus(prettifier, e, inv.spread.pivot, inv.spread.tolerance)
            else
              FailureMessages.equaledPlusOrMinus(prettifier, e, inv.spread.pivot, inv.spread.tolerance),
            None,
            pos
          )
        else indicateSuccess(inv.expectingEqual, FailureMessages.equaledPlusOrMinus(prettifier, e, inv.spread.pivot, inv.spread.tolerance), FailureMessages.didNotEqualPlusOrMinus(prettifier, e, inv.spread.pivot, inv.spread.tolerance))
      }
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldNot be theSameInstanceAs anotherInstance
     *         ^
     * }}}
     */
    def shouldNot(beWord: BeWord): ResultOfBeWordForCollectedAny[T] =
      new ResultOfBeWordForCollectedAny[T](collected, xs, original, false, prettifier, pos)

   /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should contain oneOf (1, 2, 3)
     *          ^
     * }}}
     */
    def should(containWord: ContainWord): ResultOfContainWordForCollectedAny[T] = {
      new ResultOfContainWordForCollectedAny(collected, xs, original, true, prettifier, pos)
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) shouldNot contain (oneOf (1, 2, 3))
     *          ^
     * }}}
     */
    def shouldNot(containWord: ContainWord): ResultOfContainWordForCollectedAny[T] = {
      new ResultOfContainWordForCollectedAny(collected, xs, original, false, prettifier, pos)
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should exist
     *         ^
     * }}}
     */
    def should(existWord: ExistWord)(implicit existence: Existence[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (!existence.exists(e))
          indicateFailure(
            FailureMessages.doesNotExist(prettifier, e),
            None,
            pos
          )
        else indicateSuccess(FailureMessages.exists(prettifier, e))
      }
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) should not (exist)
     *         ^
     * }}}
     */
    def should(notExist: ResultOfNotExist)(implicit existence: Existence[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (existence.exists(e))
          indicateFailure(
            FailureMessages.exists(prettifier, e),
            None,
            pos
          )
        else indicateSuccess(FailureMessages.doesNotExist(prettifier, e))
      }
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(xs) shouldNot exist
     *         ^
     * }}}
     */
    def shouldNot(existWord: ExistWord)(implicit existence: Existence[T]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        if (existence.exists(e))
          indicateFailure(
            FailureMessages.exists(prettifier, e),
            None,
            pos
          )
        else indicateSuccess(FailureMessages.doesNotExist(prettifier, e))
      }
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should startWith regex ("Hel*o")
     *             ^
     * }}}
     */
    def should(startWithWord: StartWithWord)(implicit ev: T <:< String): ResultOfStartWithWordForCollectedString = 
      new ResultOfStartWithWordForCollectedString(collected, xs.asInstanceOf[GenTraversable[String]], original, true, prettifier, pos)
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should endWith regex ("wo.ld")
     *             ^
     * }}}
     */
    def should(endWithWord: EndWithWord)(implicit ev: T <:< String): ResultOfEndWithWordForCollectedString = 
      new ResultOfEndWithWordForCollectedString(collected, xs.asInstanceOf[GenTraversable[String]], original, true, prettifier, pos)
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should include regex ("wo.ld")
     *             ^
     * }}}
     */
    def should(includeWord: IncludeWord)(implicit ev: T <:< String): ResultOfIncludeWordForCollectedString = 
      new ResultOfIncludeWordForCollectedString(collected, xs.asInstanceOf[GenTraversable[String]], original, true, prettifier, pos)
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should fullyMatch regex ("""(-)?(\d+)(\.\d*)?""")
     *             ^
     * }}}
     */
    def should(fullyMatchWord: FullyMatchWord)(implicit ev: T <:< String): ResultOfFullyMatchWordForCollectedString = 
      new ResultOfFullyMatchWordForCollectedString(collected, xs.asInstanceOf[GenTraversable[String]], original, true, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) shouldNot fullyMatch regex ("""(-)?(\d+)(\.\d*)?""")
     *             ^
     * }}}
     */
    def shouldNot(fullyMatchWord: FullyMatchWord)(implicit ev: T <:< String): ResultOfFullyMatchWordForCollectedString = 
      new ResultOfFullyMatchWordForCollectedString(collected, xs.asInstanceOf[GenTraversable[String]], original, false, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) shouldNot startWith regex ("Hel*o")
     *             ^
     * }}}
     */
    def shouldNot(startWithWord: StartWithWord)(implicit ev: T <:< String): ResultOfStartWithWordForCollectedString = 
      new ResultOfStartWithWordForCollectedString(collected, xs.asInstanceOf[GenTraversable[String]], original, false, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) shouldNot endWith regex ("wo.ld")
     *             ^
     * }}}
     */
    def shouldNot(endWithWord: EndWithWord)(implicit ev: T <:< String): ResultOfEndWithWordForCollectedString = 
      new ResultOfEndWithWordForCollectedString(collected, xs.asInstanceOf[GenTraversable[String]], original, false, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) shouldNot include regex ("wo.ld")
     *             ^
     * }}}
     */
    def shouldNot(includeWord: IncludeWord)(implicit ev: T <:< String): ResultOfIncludeWordForCollectedString = 
      new ResultOfIncludeWordForCollectedString(collected, xs.asInstanceOf[GenTraversable[String]], original, false, prettifier, pos)

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfCollectedAny([collected], [xs])"
     */
    override def toString: String = "ResultOfCollectedAny(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ")"
  }
  
  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   */
  final class ResultOfHaveWordForCollectedExtent[A](collected: Collected, xs: scala.collection.GenTraversable[A], original: Any, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should have length (12)
     *                      ^
     * }}}
     */
    def length(expectedLength: Long)(implicit len: Length[A]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val eLength = len.lengthOf(e)
        if ((eLength == expectedLength) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.hadLengthInsteadOfExpectedLength(prettifier, e, eLength, expectedLength)
            else
              FailureMessages.hadLength(prettifier, e, expectedLength),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.hadLength(prettifier, e, expectedLength),
            FailureMessages.hadLengthInsteadOfExpectedLength(prettifier, e, eLength, expectedLength)
          )
      }
    }
    
    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all (xs) should have size (12)
     *                      ^
     * }}}
     */
    def size(expectedSize: Long)(implicit sz: Size[A]): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val eSize = sz.sizeOf(e)
        if ((eSize == expectedSize) != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              FailureMessages.hadSizeInsteadOfExpectedSize(prettifier, e, eSize, expectedSize)
            else
              FailureMessages.hadSize(prettifier, e, expectedSize),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            FailureMessages.hadSize(prettifier, e, expectedSize),
            FailureMessages.hadSizeInsteadOfExpectedSize(prettifier, e, eSize, expectedSize)
          )
      }
    }
    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfHaveWordForCollectedExtent([collected], [xs], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfHaveWordForCollectedExtent(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="InspectorsMatchers.html">`InspectorsMatchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  final class ResultOfStartWithWordForCollectedString(collected: Collected, xs: scala.collection.GenTraversable[String], original: Any, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should startWith regex ("Hel*o")
     *                              ^
     * }}}
     */
    def regex(rightRegexString: String): Assertion = { checkRegex(rightRegexString.r) }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should fullMatch regex ("a(b*)c" withGroup "bb") 
     *                              ^
     * }}}
     */
    def regex(regexWithGroups: RegexWithGroups): Assertion = { checkRegex(regexWithGroups.regex, regexWithGroups.groups) }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should startWith regex ("Hel*o".r)
     *                              ^
     * }}}
     */
    def regex(rightRegex: Regex): Assertion = { checkRegex(rightRegex) }
    
    private def checkRegex(rightRegex: Regex, groups: IndexedSeq[String] = IndexedSeq.empty): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = startWithRegexWithGroups(e, rightRegex, groups)
        if (result.matches != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              result.failureMessage(prettifier)
            else
              result.negatedFailureMessage(prettifier),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            result.negatedFailureMessage(prettifier),
            result.failureMessage(prettifier)
          )
      }
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfStartWithWordForCollectedString([collected], [xs], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfStartWithWordForCollectedString(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }
  
  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="InspectorsMatchers.html">`InspectorsMatchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  final class ResultOfIncludeWordForCollectedString(collected: Collected, xs: scala.collection.GenTraversable[String], original: Any, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should include regex ("world")
     *                            ^
     * }}}
     */
    def regex(rightRegexString: String): Assertion = { checkRegex(rightRegexString.r) }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should include regex ("a(b*)c" withGroup "bb") 
     *                            ^
     * }}}
     */
    def regex(regexWithGroups: RegexWithGroups): Assertion = { checkRegex(regexWithGroups.regex, regexWithGroups.groups) }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should include regex ("wo.ld".r)
     *                            ^
     * }}}
     */
    def regex(rightRegex: Regex): Assertion = { checkRegex(rightRegex) }
    
    private def checkRegex(rightRegex: Regex, groups: IndexedSeq[String] = IndexedSeq.empty): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = includeRegexWithGroups(e, rightRegex, groups)
        if (result.matches != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              result.failureMessage(prettifier)
            else
              result.negatedFailureMessage(prettifier),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            result.negatedFailureMessage(prettifier),
            result.failureMessage(prettifier)
          )
      }
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfIncludeWordForCollectedString([collected], [xs], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfIncludeWordForCollectedString(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }
  
  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="InspectorsMatchers.html">`InspectorsMatchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  final class ResultOfEndWithWordForCollectedString(collected: Collected, xs: scala.collection.GenTraversable[String], original: Any, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should endWith regex ("wor.d")
     *                            ^
     * }}}
     */
    def regex(rightRegexString: String): Assertion = { checkRegex(rightRegexString.r) }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should endWith regex ("a(b*)c" withGroup "bb") 
     *                            ^
     * }}}
     */
    def regex(regexWithGroups: RegexWithGroups): Assertion = { checkRegex(regexWithGroups.regex, regexWithGroups.groups) }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should endWith regex ("wor.d".r)
     *                            ^
     * }}}
     */
    def regex(rightRegex: Regex): Assertion = { checkRegex(rightRegex) }
    
    private def checkRegex(rightRegex: Regex, groups: IndexedSeq[String] = IndexedSeq.empty): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = endWithRegexWithGroups(e, rightRegex, groups)
        if (result.matches != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              result.failureMessage(prettifier)
            else
              result.negatedFailureMessage(prettifier),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            result.negatedFailureMessage(prettifier),
            result.failureMessage(prettifier)
          )
      }
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfEndWithWordForCollectedString([collected], [xs], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfEndWithWordForCollectedString(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }
  
  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="InspectorsMatchers.html">`InspectorsMatchers`</a> for an overview of
   * the matchers DSL.
   *
   * @author Bill Venners
   * @author Chee Seng
   */
  final class ResultOfFullyMatchWordForCollectedString(collected: Collected, xs: scala.collection.GenTraversable[String], original: Any, shouldBeTrue: Boolean, prettifier: Prettifier, pos: source.Position) {

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should fullMatch regex ("Hel*o world")
     *                              ^
     * }}}
     */
    def regex(rightRegexString: String): Assertion = { checkRegex(rightRegexString.r) }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should fullMatch regex ("a(b*)c" withGroup "bb") 
     *                              ^
     * }}}
     */
    def regex(regexWithGroups: RegexWithGroups): Assertion = { checkRegex(regexWithGroups.regex, regexWithGroups.groups) }

    /**
     * This method enables the following syntax: 
     *
     * {{{  <!-- class="stHighlight" -->
     * all(string) should fullymatch regex ("Hel*o world".r)
     *                               ^
     * }}}
     */
    def regex(rightRegex: Regex): Assertion = { checkRegex(rightRegex) }
    
    private def checkRegex(rightRegex: Regex, groups: IndexedSeq[String] = IndexedSeq.empty): Assertion = {
      doCollected(collected, xs, original, prettifier, pos) { e =>
        val result = fullyMatchRegexWithGroups(e, rightRegex, groups)
        if (result.matches != shouldBeTrue)
          indicateFailure(
            if (shouldBeTrue)
              result.failureMessage(prettifier)
            else
              result.negatedFailureMessage(prettifier),
            None,
            pos
          )
        else
          indicateSuccess(
            shouldBeTrue,
            result.negatedFailureMessage(prettifier),
            result.failureMessage(prettifier)
          )
      }
    }

    /**
     * Overrides to return pretty toString.
     *
     * @return "ResultOfFullyMatchWordForCollectedString([collected], [xs], [shouldBeTrue])"
     */
    override def toString: String = "ResultOfFullyMatchWordForCollectedString(" + Prettifier.default(collected) + ", " + Prettifier.default(xs) + ", " + Prettifier.default(shouldBeTrue) + ")"
  }

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * all(xs) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def all[E, C[_]](xs: C[E])(implicit collecting: Collecting[E, C[E]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[E] =
    new ResultOfCollectedAny(AllCollected, collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `java.util.Map`:
   *
   * {{{  <!-- class="stHighlight" -->
   * all(jmap) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def all[K, V, JMAP[k, v] <: java.util.Map[k, v]](xs: JMAP[K, V])(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[org.scalatest.Entry[K, V]] =
    new ResultOfCollectedAny(AllCollected, collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `String`:
   *
   * {{{  <!-- class="stHighlight" -->
   * all(str) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def all(xs: String)(implicit collecting: Collecting[Char, String], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[Char] =
    new ResultOfCollectedAny(AllCollected, collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * atLeast(1, xs) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def atLeast[E, C[_]](num: Int, xs: C[E])(implicit collecting: Collecting[E, C[E]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[E] =
    new ResultOfCollectedAny(AtLeastCollected(num), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `java.util.Map`:
   *
   * {{{  <!-- class="stHighlight" -->
   * atLeast(1, jmap) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def atLeast[K, V, JMAP[k, v] <: java.util.Map[k, v]](num: Int, xs: JMAP[K, V])(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[org.scalatest.Entry[K, V]] =
    new ResultOfCollectedAny(AtLeastCollected(num), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `String`:
   *
   * {{{  <!-- class="stHighlight" -->
   * atLeast(1, str) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def atLeast(num: Int, xs: String)(implicit collecting: Collecting[Char, String], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[Char] =
    new ResultOfCollectedAny(AtLeastCollected(num), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * every(xs) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def every[E, C[_]](xs: C[E])(implicit collecting: Collecting[E, C[E]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[E] =
    new ResultOfCollectedAny(EveryCollected, collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `java.util.Map`:
   *
   * {{{  <!-- class="stHighlight" -->
   * every(jmap) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def every[K, V, JMAP[k, v] <: java.util.Map[k, v]](xs: JMAP[K, V])(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[org.scalatest.Entry[K, V]] =
    new ResultOfCollectedAny(EveryCollected, collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `String`:
   *
   * {{{  <!-- class="stHighlight" -->
   * every(str) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def every(xs: String)(implicit collecting: Collecting[Char, String], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[Char] =
    new ResultOfCollectedAny(EveryCollected, collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * exactly(xs) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def exactly[E, C[_]](num: Int, xs: C[E])(implicit collecting: Collecting[E, C[E]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[E] =
    new ResultOfCollectedAny(ExactlyCollected(num), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `java.util.Map`:
   *
   * {{{  <!-- class="stHighlight" -->
   * exactly(jmap) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def exactly[K, V, JMAP[k, v] <: java.util.Map[k, v]](num: Int, xs: JMAP[K, V])(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[org.scalatest.Entry[K, V]] =
    new ResultOfCollectedAny(ExactlyCollected(num), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `String`:
   *
   * {{{  <!-- class="stHighlight" -->
   * exactly(str) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def exactly(num: Int, xs: String)(implicit collecting: Collecting[Char, String], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[Char] =
    new ResultOfCollectedAny(ExactlyCollected(num), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * no(xs) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def no[E, C[_]](xs: C[E])(implicit collecting: Collecting[E, C[E]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[E] =
    new ResultOfCollectedAny(NoCollected, collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `java.util.Map`:
   *
   * {{{  <!-- class="stHighlight" -->
   * no(jmap) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def no[K, V, JMAP[k, v] <: java.util.Map[k, v]](xs: JMAP[K, V])(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[org.scalatest.Entry[K, V]] =
    new ResultOfCollectedAny(NoCollected, collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `String`:
   *
   * {{{  <!-- class="stHighlight" -->
   * no(str) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def no(xs: String)(implicit collecting: Collecting[Char, String], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[Char] =
    new ResultOfCollectedAny(NoCollected, collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * between(1, 3, xs) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def between[E, C[_]](from: Int, upTo:Int, xs: C[E])(implicit collecting: Collecting[E, C[E]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[E] =
    new ResultOfCollectedAny(BetweenCollected(from, upTo), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `java.util.Map`:
   *
   * {{{  <!-- class="stHighlight" -->
   * between(1, 3, jmap) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def between[K, V, JMAP[k, v] <: java.util.Map[k, v]](from: Int, upTo:Int, xs: JMAP[K, V])(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[org.scalatest.Entry[K, V]] =
    new ResultOfCollectedAny(BetweenCollected(from, upTo), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `String`:
   *
   * {{{  <!-- class="stHighlight" -->
   * between(1, 3, str) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def between(from: Int, upTo:Int, xs: String)(implicit collecting: Collecting[Char, String], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[Char] =
    new ResultOfCollectedAny(BetweenCollected(from, upTo), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   * atMost(3, xs) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def atMost[E, C[_]](num: Int, xs: C[E])(implicit collecting: Collecting[E, C[E]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[E] =
    new ResultOfCollectedAny(AtMostCollected(num), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `java.util.Map`:
   *
   * {{{  <!-- class="stHighlight" -->
   * atMost(3, jmap) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def atMost[K, V, JMAP[k, v] <: java.util.Map[k, v]](num: Int, xs: JMAP[K, V])(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[org.scalatest.Entry[K, V]] =
    new ResultOfCollectedAny(AtMostCollected(num), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax for `String`:
   *
   * {{{  <!-- class="stHighlight" -->
   * atMost(3, str) should fullymatch regex ("Hel*o world".r)
   * ^
   * }}}
   */
  def atMost(num: Int, xs: String)(implicit collecting: Collecting[Char, String], prettifier: Prettifier, pos: source.Position): ResultOfCollectedAny[Char] =
    new ResultOfCollectedAny(AtMostCollected(num), collecting.genTraversableFrom(xs), xs, prettifier, pos)

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * a [RuntimeException] should be thrownBy { ... }
   * ^
   * }}}
   */
  def a[T: ClassTag]: ResultOfATypeInvocation[T] =
    new ResultOfATypeInvocation(classTag.runtimeClass.asInstanceOf[Class[T]])

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * an [Exception] should be thrownBy { ... }
   * ^
   * }}}
   */
  def an[T : ClassTag]: ResultOfAnTypeInvocation[T] =
    new ResultOfAnTypeInvocation(classTag.runtimeClass.asInstanceOf[Class[T]])

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * the [FileNotFoundException] should be thrownBy { ... }
   * ^
   * }}}
   */
  def the[T : ClassTag](implicit pos: source.Position): ResultOfTheTypeInvocation[T] =
    new ResultOfTheTypeInvocation(classTag.runtimeClass.asInstanceOf[Class[T]], pos)

  // This is where ShouldMatchers.scala started 

  private object ShouldMethodHelper {

    def shouldMatcher[T](left: T, rightMatcher: Matcher[T], prettifier: Prettifier, pos: source.Position): Assertion = {
      val result = rightMatcher(left)
      MatchFailed.unapply(result)(prettifier) match {
        case Some(failureMessage) => indicateFailure(failureMessage, None, pos)
        case None => indicateSuccess(result.negatedFailureMessage(prettifier))
      }
    }

    def shouldNotMatcher[T](left: T, rightMatcher: Matcher[T], prettifier: Prettifier, pos: source.Position): Assertion = {
      val result = rightMatcher(left)
      MatchSucceeded.unapply(result)(prettifier) match {
        case Some(negatedFailureMessage) => indicateFailure(negatedFailureMessage, None, pos)
        case None => indicateSuccess(result.failureMessage(prettifier))
      }
    }
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * This class is used in conjunction with an implicit conversion to enable `should` methods to
   * be invoked on objects of type `Any`.
   * 
   *
   * @author Bill Venners
   */
  sealed class AnyShouldWrapper[T](val leftSideValue: T, val pos: source.Position, val prettifier: Prettifier) {

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should be (3)
     *        ^
     * }}}
     */
    def should(rightMatcherX1: Matcher[T]): Assertion = {
      ShouldMethodHelper.shouldMatcher(leftSideValue, rightMatcherX1, prettifier, pos)
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should equal (3)
     *        ^
     * }}}
     */
    def should[TYPECLASS1[_]](rightMatcherFactory1: MatcherFactory1[T, TYPECLASS1])(implicit typeClass1: TYPECLASS1[T]): Assertion = {
      ShouldMethodHelper.shouldMatcher(leftSideValue, rightMatcherFactory1.matcher, prettifier, pos)
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should (equal (expected) and have length 3)
     *        ^
     * }}}
     */
    def should[TYPECLASS1[_], TYPECLASS2[_]](rightMatcherFactory2: MatcherFactory2[T, TYPECLASS1, TYPECLASS2])(implicit typeClass1: TYPECLASS1[T], typeClass2: TYPECLASS2[T]): Assertion = {
      ShouldMethodHelper.shouldMatcher(leftSideValue, rightMatcherFactory2.matcher, prettifier, pos)
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * a shouldEqual b
     *   ^
     * }}}
     */
    def shouldEqual(right: Any)(implicit equality: Equality[T]): Assertion = {
      if (!equality.areEqual(leftSideValue, right)) {
        val (leftee, rightee) = Suite.getObjectsForFailureMessage(leftSideValue, right)
        indicateFailure(FailureMessages.didNotEqual(prettifier, leftee, rightee), None, pos)
      }
      else indicateSuccess(FailureMessages.equaled(prettifier, leftSideValue, right))
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldEqual 7.1 +- 0.2
     *        ^
     * }}}
     */
    def shouldEqual(spread: Spread[T]): Assertion = {
      if (!spread.isWithin(leftSideValue)) {
        indicateFailure(FailureMessages.didNotEqualPlusOrMinus(prettifier, leftSideValue, spread.pivot, spread.tolerance), None, pos)
      }
      else indicateSuccess(FailureMessages.equaledPlusOrMinus(prettifier, leftSideValue, spread.pivot, spread.tolerance))
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldEqual null
     *        ^
     * }}}
     */
    def shouldEqual(right: Null)(implicit ev: T <:< AnyRef): Assertion = { 
      if (leftSideValue != null) {
        indicateFailure(FailureMessages.didNotEqualNull(prettifier, leftSideValue), None, pos)
      }
      else indicateSuccess(FailureMessages.equaledNull)
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should not equal (3)
     *        ^
     * }}}
     */
    def should(notWord: NotWord): ResultOfNotWordForAny[T] = new ResultOfNotWordForAny[T](leftSideValue, false, prettifier, pos)

    // In 2.10, will work with AnyVals. TODO: Also, Need to ensure Char works
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * a should === (b)
     *        ^
     * }}}
     */
    def should[U](inv: TripleEqualsInvocation[U])(implicit constraint: T CanEqual U): Assertion = {
      if ((constraint.areEqual(leftSideValue, inv.right)) != inv.expectingEqual)
        indicateFailure(
          if (inv.expectingEqual)
            FailureMessages.didNotEqual(prettifier, leftSideValue, inv.right)
          else
            FailureMessages.equaled(prettifier, leftSideValue, inv.right),
          None,
          pos
        )
      else
        indicateSuccess(
          inv.expectingEqual,
          FailureMessages.equaled(prettifier, leftSideValue, inv.right),
          FailureMessages.didNotEqual(prettifier, leftSideValue, inv.right)
        )
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should === (100 +- 1)
     *        ^
     * }}}
     */
    def should(inv: TripleEqualsInvocationOnSpread[T])(implicit ev: Numeric[T]): Assertion = {
      if ((inv.spread.isWithin(leftSideValue)) != inv.expectingEqual)
        indicateFailure(
          if (inv.expectingEqual)
            FailureMessages.didNotEqualPlusOrMinus(prettifier, leftSideValue, inv.spread.pivot, inv.spread.tolerance)
          else
            FailureMessages.equaledPlusOrMinus(prettifier, leftSideValue, inv.spread.pivot, inv.spread.tolerance),
          None,
          pos
        )
      else
        indicateSuccess(
          inv.expectingEqual,
          FailureMessages.equaledPlusOrMinus(prettifier, leftSideValue, inv.spread.pivot, inv.spread.tolerance),
          FailureMessages.didNotEqualPlusOrMinus(prettifier, leftSideValue, inv.spread.pivot, inv.spread.tolerance)
        )
    }

    // TODO: Need to make sure this works in inspector shorthands. I moved this
    // up here from NumericShouldWrapper.
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should be a aMatcher
     *        ^
     * }}}
     */
    def should(beWord: BeWord): ResultOfBeWordForAny[T] = new ResultOfBeWordForAny(leftSideValue, true, prettifier, pos)
  
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * aDouble shouldBe 8.8
     *         ^
     * }}}
     */
    def shouldBe(right: Any): Assertion = {
      if (!areEqualComparingArraysStructurally(leftSideValue, right)) {
        val (leftee, rightee) = Suite.getObjectsForFailureMessage(leftSideValue, right)
        val localPrettifier = prettifier // Grabbing a local copy so we don't attempt to serialize AnyShouldWrapper (since first param to indicateFailure is a by-name)
        indicateFailure(FailureMessages.wasNotEqualTo(localPrettifier, leftee, rightee), None, pos)
      }
      else indicateSuccess(FailureMessages.wasEqualTo(prettifier, leftSideValue, right))
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * 5 shouldBe &lt; (7) 
     *   ^
     * }}}
     */
    def shouldBe(comparison: ResultOfLessThanComparison[T]): Assertion = {
      if (!comparison(leftSideValue)) {
        indicateFailure(
          FailureMessages.wasNotLessThan(prettifier,
            leftSideValue,
            comparison.right
          ),
          None,
          pos
        ) 
      }
      else indicateSuccess(FailureMessages.wasLessThan(prettifier, leftSideValue, comparison.right))
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * 8 shouldBe &gt; (7) 
     *   ^
     * }}} 
     */
    def shouldBe(comparison: ResultOfGreaterThanComparison[T]): Assertion = {
      if (!comparison(leftSideValue)) {
        indicateFailure(
          FailureMessages.wasNotGreaterThan(prettifier,
            leftSideValue,
            comparison.right
          ),
          None,
          pos
        ) 
      }
      else indicateSuccess(FailureMessages.wasGreaterThan(prettifier, leftSideValue, comparison.right))
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * 5 shouldBe &lt;= (7) 
     *   ^
     * }}} 
     */
    def shouldBe(comparison: ResultOfLessThanOrEqualToComparison[T]): Assertion = {
      if (!comparison(leftSideValue)) {
        indicateFailure(
          FailureMessages.wasNotLessThanOrEqualTo(prettifier,
            leftSideValue,
            comparison.right
          ),
          None,
          pos
        ) 
      }
      else indicateSuccess(FailureMessages.wasLessThanOrEqualTo(prettifier, leftSideValue, comparison.right))
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * 8 shouldBe &gt;= (7) 
     *   ^
     * }}} 
     */
    def shouldBe(comparison: ResultOfGreaterThanOrEqualToComparison[T]): Assertion = {
      if (!comparison(leftSideValue)) {
        indicateFailure(
          FailureMessages.wasNotGreaterThanOrEqualTo(prettifier,
            leftSideValue,
            comparison.right
          ),
          None,
          pos
        ) 
      }
      else indicateSuccess(FailureMessages.wasGreaterThanOrEqualTo(prettifier, leftSideValue, comparison.right))
    }
    
    /**
     * This method enables the following syntax, where `odd` refers to a `BeMatcher[Int]`:
     *
     * {{{  <!-- class="stHighlight" -->testing
     * 1 shouldBe odd
     *   ^
     * }}}
     */
    def shouldBe(beMatcher: BeMatcher[T]): Assertion = {
      val result = beMatcher.apply(leftSideValue)
      if (!result.matches)
        indicateFailure(result.failureMessage(prettifier), None, pos)
      else indicateSuccess(result.negatedFailureMessage(prettifier))
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldBe 7.1 +- 0.2
     *        ^
     * }}}
     */
    def shouldBe(spread: Spread[T]): Assertion = {
      if (!spread.isWithin(leftSideValue)) {
        indicateFailure(FailureMessages.wasNotPlusOrMinus(prettifier, leftSideValue, spread.pivot, spread.tolerance), None, pos)
      }
      else indicateSuccess(FailureMessages.wasPlusOrMinus(prettifier, leftSideValue, spread.pivot, spread.tolerance))
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldBe sorted
     *        ^
     * }}}
     */
    def shouldBe(right: SortedWord)(implicit sortable: Sortable[T]): Assertion = {
      if (!sortable.isSorted(leftSideValue))
        indicateFailure(FailureMessages.wasNotSorted(prettifier, leftSideValue), None, pos)
      else indicateSuccess(FailureMessages.wasSorted(prettifier, leftSideValue))
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * aDouble shouldBe a [Book]
     *         ^
     * }}}
     */
    def shouldBe(aType: ResultOfATypeInvocation[_]): Assertion = macro TypeMatcherMacro.shouldBeATypeImpl
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * aDouble shouldBe an [Book]
     *         ^
     * }}}
     */
    def shouldBe(anType: ResultOfAnTypeInvocation[_]): Assertion = macro TypeMatcherMacro.shouldBeAnTypeImpl
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldBe readable
     *        ^
     * }}}
     */
    def shouldBe(right: ReadableWord)(implicit readability: Readability[T]): Assertion = {
      if (!readability.isReadable(leftSideValue))
        indicateFailure(FailureMessages.wasNotReadable(prettifier, leftSideValue), None, pos)
      else indicateSuccess(FailureMessages.wasReadable(prettifier, leftSideValue))
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldBe writable
     *        ^
     * }}}
     */
    def shouldBe(right: WritableWord)(implicit writability: Writability[T]): Assertion = {
      if (!writability.isWritable(leftSideValue))
        indicateFailure(FailureMessages.wasNotWritable(prettifier, leftSideValue), None, pos)
      else indicateSuccess(FailureMessages.wasWritable(prettifier, leftSideValue))
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldBe empty
     *        ^
     * }}}
     */
    def shouldBe(right: EmptyWord)(implicit emptiness: Emptiness[T]): Assertion = {
      if (!emptiness.isEmpty(leftSideValue))
        indicateFailure(FailureMessages.wasNotEmpty(prettifier, leftSideValue), None, pos)
      else indicateSuccess(FailureMessages.wasEmpty(prettifier, leftSideValue))
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldBe defined
     *        ^
     * }}}
     */
    def shouldBe(right: DefinedWord)(implicit definition: Definition[T]): Assertion = {
      if (!definition.isDefined(leftSideValue))
        indicateFailure(FailureMessages.wasNotDefined(prettifier, leftSideValue), None, pos)
      else indicateSuccess(FailureMessages.wasDefined(prettifier, leftSideValue))
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldNot be (3)
     *        ^
     * }}}
     */
    def shouldNot(beWord: BeWord): ResultOfBeWordForAny[T] = new ResultOfBeWordForAny(leftSideValue, false, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldNot (be (3))
     *        ^
     * }}}
     */
    def shouldNot(rightMatcherX1: Matcher[T]): Assertion = {
      ShouldMethodHelper.shouldNotMatcher(leftSideValue, rightMatcherX1, prettifier, pos)
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldNot (be readable)
     *        ^
     * }}}
     */
    def shouldNot[TYPECLASS1[_]](rightMatcherFactory1: MatcherFactory1[T, TYPECLASS1])(implicit typeClass1: TYPECLASS1[T]): Assertion = {
      ShouldMethodHelper.shouldNotMatcher(leftSideValue, rightMatcherFactory1.matcher, prettifier, pos)
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldNot have length (3)
     *        ^
     * result shouldNot have size (3)
     *        ^
     * exception shouldNot have message ("file not found")
     *           ^
     * }}}
     */
    def shouldNot(haveWord: HaveWord): ResultOfHaveWordForExtent[T] =
      new ResultOfHaveWordForExtent(leftSideValue, false, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result should have length (3)
     *        ^
     * result should have size (3)
     *        ^
     * }}}
     */
    def should(haveWord: HaveWord): ResultOfHaveWordForExtent[T] =
      new ResultOfHaveWordForExtent(leftSideValue, true, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldBe null
     *        ^
     * }}}
     */
    def shouldBe(right: Null)(implicit ev: T <:< AnyRef): Assertion = {
      if (leftSideValue != null) {
        indicateFailure(FailureMessages.wasNotNull(prettifier, leftSideValue), None, pos)
      }
      else indicateSuccess(FailureMessages.wasNull)
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * result shouldBe theSameInstanceAs (anotherObject)
     *        ^
     * }}}
     */
    def shouldBe(resultOfSameInstanceAsApplication: ResultOfTheSameInstanceAsApplication)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      if (resultOfSameInstanceAsApplication.right ne toAnyRef(leftSideValue)) {
        indicateFailure(
          FailureMessages.wasNotSameInstanceAs(prettifier,
            leftSideValue,
            resultOfSameInstanceAsApplication.right
          ),
          None,
          pos
        )
      }
      else indicateSuccess(FailureMessages.wasSameInstanceAs(prettifier, leftSideValue, resultOfSameInstanceAsApplication.right))
    }

    // SKIP-SCALATESTJS-START
// TODO: Remember to write tests for inspector shorthands uncovering the bug below, always a empty because always true true passed to matchSym
    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * list shouldBe 'empty
     *      ^
     * }}}
     */
    def shouldBe(symbol: Symbol)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      val matcherResult = matchSymbolToPredicateMethod(toAnyRef(leftSideValue), symbol, false, true, prettifier, pos)
      if (!matcherResult.matches) 
        indicateFailure(matcherResult.failureMessage(prettifier), None, pos)
      else indicateSuccess(matcherResult.negatedFailureMessage(prettifier))
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * list shouldBe a ('empty)
     *      ^
     * }}}
     */
    def shouldBe(resultOfAWordApplication: ResultOfAWordToSymbolApplication)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      val matcherResult = matchSymbolToPredicateMethod(toAnyRef(leftSideValue), resultOfAWordApplication.symbol, true, true, prettifier, pos)
      if (!matcherResult.matches) {
        indicateFailure(
          matcherResult.failureMessage(prettifier),
          None,
          pos
        )
      }
      else indicateSuccess(matcherResult.negatedFailureMessage(prettifier))
    }

    /**
     * This method enables the following syntax:
     *
     * {{{  <!-- class="stHighlight" -->
     * list shouldBe an ('empty)
     *      ^
     * }}}
     */
    def shouldBe(resultOfAnWordApplication: ResultOfAnWordToSymbolApplication)(implicit toAnyRef: T <:< AnyRef): Assertion = {
      val matcherResult = matchSymbolToPredicateMethod(toAnyRef(leftSideValue), resultOfAnWordApplication.symbol, true, false, prettifier, pos)
      if (!matcherResult.matches) {
        indicateFailure(
          matcherResult.failureMessage(prettifier),
          None,
          pos
        )
      }
      else indicateSuccess(matcherResult.negatedFailureMessage(prettifier))
    }
    // SKIP-SCALATESTJS-END
    
    /**
     * This method enables the following syntax, where `excellentRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * programmingInScala shouldBe excellentRead
     *                    ^
     * }}}
     */
    def shouldBe(bePropertyMatcher: BePropertyMatcher[T])(implicit ev: T <:< AnyRef): Assertion = { // TODO: Try expanding this to 2.10 AnyVal
      val result = bePropertyMatcher(leftSideValue)
      if (!result.matches) 
        indicateFailure(FailureMessages.wasNot(prettifier, leftSideValue, UnquotedString(result.propertyName)), None, pos)
      else indicateSuccess(FailureMessages.was(prettifier, leftSideValue, UnquotedString(result.propertyName)))
    }
    
    /**
     * This method enables the following syntax, where `goodRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * programmingInScala shouldBe a (goodRead)
     *                    ^
     * }}}
     */
    def shouldBe[U >: T](resultOfAWordApplication: ResultOfAWordToBePropertyMatcherApplication[U])(implicit ev: T <:< AnyRef): Assertion = {// TODO: Try expanding this to 2.10 AnyVal
      val result = resultOfAWordApplication.bePropertyMatcher(leftSideValue)
      if (!result.matches) {
        indicateFailure(FailureMessages.wasNotA(prettifier, leftSideValue, UnquotedString(result.propertyName)), None, pos)
      }
      else indicateSuccess(FailureMessages.wasA(prettifier, leftSideValue, UnquotedString(result.propertyName)))
    }
    
    /**
     * This method enables the following syntax, where `excellentRead` refers to a `BePropertyMatcher[Book]`:
     *
     * {{{  <!-- class="stHighlight" -->
     * programmingInScala shouldBe an (excellentRead)
     *                    ^
     * }}}
     */
    def shouldBe[U >: T](resultOfAnWordApplication: ResultOfAnWordToBePropertyMatcherApplication[U])(implicit ev: T <:< AnyRef): Assertion = {// TODO: Try expanding this to 2.10 AnyVal
      val result = resultOfAnWordApplication.bePropertyMatcher(leftSideValue)
      if (!result.matches) {
        indicateFailure(FailureMessages.wasNotAn(prettifier, leftSideValue, UnquotedString(result.propertyName)), None, pos)
      }
      else indicateSuccess(FailureMessages.wasAn(prettifier, leftSideValue, UnquotedString(result.propertyName)))
    }

/*
    def shouldBe[U](right: AType[U]) {
      if (!right.isAssignableFromClassOf(leftSideValue)) {
        throw newTestFailedException(FailureMessages.wasNotAnInstanceOf(prettifier, leftSideValue, UnquotedString(right.className), UnquotedString(leftSideValue.getClass.getName)))
      }
    }
*/

   /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * xs should contain oneOf (1, 2, 3)
     *    ^
     * }}}
     */
    def should(containWord: ContainWord): ResultOfContainWord[T] = {
      new ResultOfContainWord(leftSideValue, true, prettifier, pos)
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * xs shouldNot contain (oneOf (1, 2, 3))
     *    ^
     * }}}
     */
    def shouldNot(contain: ContainWord): ResultOfContainWord[T] = 
      new ResultOfContainWord(leftSideValue, false, prettifier, pos)
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * file should exist
     *      ^
     * }}}
     */
    def should(existWord: ExistWord)(implicit existence: Existence[T]): Assertion = {
      if (!existence.exists(leftSideValue))
        indicateFailure(FailureMessages.doesNotExist(prettifier, leftSideValue), None, pos)
      else indicateSuccess(FailureMessages.exists(prettifier, leftSideValue))
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * file should not (exist)
     *      ^
     * }}}
     */
    def should(notExist: ResultOfNotExist)(implicit existence: Existence[T]): Assertion = {
      if (existence.exists(leftSideValue))
        indicateFailure(FailureMessages.exists(prettifier, leftSideValue), None, pos)
      else indicateSuccess(FailureMessages.doesNotExist(prettifier, leftSideValue))
    }
    
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * file shouldNot exist
     *      ^
     * }}}
     */
    def shouldNot(existWord: ExistWord)(implicit existence: Existence[T]): Assertion = {
      if (existence.exists(leftSideValue))
        indicateFailure(FailureMessages.exists(prettifier, leftSideValue), None, pos)
      else indicateSuccess(FailureMessages.doesNotExist(prettifier, leftSideValue))
    }

    // From StringShouldWrapper
    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string should include regex ("hi")
     *        ^
     * }}}
     */
    def should(includeWord: IncludeWord)(implicit ev: T <:< String): ResultOfIncludeWordForString = {
      new ResultOfIncludeWordForString(leftSideValue, true, prettifier, pos)
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string should startWith regex ("hello")
     *        ^
     * }}}
     */
    def should(startWithWord: StartWithWord)(implicit ev: T <:< String): ResultOfStartWithWordForString = {
      new ResultOfStartWithWordForString(leftSideValue, true, prettifier, pos)
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string should endWith regex ("world")
     *        ^
     * }}}
     */
    def should(endWithWord: EndWithWord)(implicit ev: T <:< String): ResultOfEndWithWordForString = {
      new ResultOfEndWithWordForString(leftSideValue, true, prettifier, pos)
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string shouldNot startWith regex ("hello")
     *        ^
     * }}}
     */
    def shouldNot(startWithWord: StartWithWord)(implicit ev: T <:< String): ResultOfStartWithWordForString = 
      new ResultOfStartWithWordForString(leftSideValue, false, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string shouldNot endWith regex ("world")
     *        ^
     * }}}
     */
    def shouldNot(endWithWord: EndWithWord)(implicit ev: T <:< String): ResultOfEndWithWordForString = 
      new ResultOfEndWithWordForString(leftSideValue, false, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string shouldNot include regex ("hi")
     *        ^
     * }}}
     */
    def shouldNot(includeWord: IncludeWord)(implicit ev: T <:< String): ResultOfIncludeWordForString = 
      new ResultOfIncludeWordForString(leftSideValue, false, prettifier, pos)
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * This class is used in conjunction with an implicit conversion to enable `should` methods to
   * be invoked on `String`s.
   * 
   *
   * @author Bill Venners
   */
  final class StringShouldWrapper(val leftSideString: String, pos: source.Position, prettifier: Prettifier) extends AnyShouldWrapper(leftSideString, pos, prettifier) with StringShouldWrapperForVerb {

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string should fullyMatch regex ("a(b*)c" withGroup "bb") 
     *                                          ^
     * }}}
     */
    def withGroup(group: String): RegexWithGroups = 
      new RegexWithGroups(leftSideString.r, IndexedSeq(group))

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string should fullyMatch regex ("a(b*)(c*)" withGroups ("bb", "cc"))
     *                                             ^
     * }}}
     */
    def withGroups(groups: String*): RegexWithGroups =
      new RegexWithGroups(leftSideString.r, IndexedSeq(groups: _*))

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string should fullyMatch regex ("""(-)?(\d+)(\.\d*)?""")
     *        ^
     * }}}
     */
    def should(fullyMatchWord: FullyMatchWord): ResultOfFullyMatchWordForString = {
      new ResultOfFullyMatchWordForString(leftSideString, true, prettifier, pos)
    }

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string shouldNot fullyMatch regex ("""(-)?(\d+)(\.\d*)?""")
     *        ^
     * }}}
     */
    def shouldNot(fullyMatchWord: FullyMatchWord): ResultOfFullyMatchWordForString = 
      new ResultOfFullyMatchWordForString(leftSideString, false, prettifier, pos)

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string should compile
     *        ^
     * }}}
     */
    def should(compileWord: CompileWord)(implicit pos: source.Position): Assertion = macro CompileMacro.shouldCompileImpl

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string shouldNot compile
     *        ^
     * }}}
     */
    def shouldNot(compileWord: CompileWord)(implicit pos: source.Position): Assertion = macro CompileMacro.shouldNotCompileImpl

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * string shouldNot typeCheck
     *        ^
     * }}}
     */
    def shouldNot(typeCheckWord: TypeCheckWord)(implicit pos: source.Position): Assertion = macro CompileMacro.shouldNotTypeCheckImpl

/*
    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string should include regex ("hi")
     *        ^
     * </pre>
     */
    def should(includeWord: IncludeWord): ResultOfIncludeWordForString = {
      new ResultOfIncludeWordForString(leftSideString, true)
    }

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string should startWith regex ("hello")
     *        ^
     * </pre>
     */
    def should(startWithWord: StartWithWord): ResultOfStartWithWordForString = {
      new ResultOfStartWithWordForString(leftSideString, true)
    }

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string should endWith regex ("world")
     *        ^
     * </pre>
     */
    def should(endWithWord: EndWithWord): ResultOfEndWithWordForString = {
      new ResultOfEndWithWordForString(leftSideString, true)
    }

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string should fullyMatch regex ("""(-)?(\d+)(\.\d*)?""")
     *        ^
     * </pre>
     */
    def should(fullyMatchWord: FullyMatchWord): ResultOfFullyMatchWordForString = {
      new ResultOfFullyMatchWordForString(leftSideString, true)
    }

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string should not have length (3)
     *        ^
     * </pre>
     */
    override def should(notWord: NotWord): ResultOfNotWordForString = {
      new ResultOfNotWordForString(leftSideString, false)
    }

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string should fullyMatch regex ("a(b*)c" withGroup "bb") 
     *                                          ^
     * </pre>
     */
    def withGroup(group: String): RegexWithGroups = 
      new RegexWithGroups(leftSideString.r, IndexedSeq(group))

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string should fullyMatch regex ("a(b*)(c*)" withGroups ("bb", "cc"))
     *                                             ^
     * </pre>
     */
    def withGroups(groups: String*): RegexWithGroups = 
      new RegexWithGroups(leftSideString.r, IndexedSeq(groups: _*))

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string shouldNot fullyMatch regex ("""(-)?(\d+)(\.\d*)?""")
     *        ^
     * </pre>
     */
    def shouldNot(fullyMatchWord: FullyMatchWord): ResultOfFullyMatchWordForString = 
      new ResultOfFullyMatchWordForString(leftSideString, false)

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string shouldNot startWith regex ("hello")
     *        ^
     * </pre>
     */
    def shouldNot(startWithWord: StartWithWord): ResultOfStartWithWordForString = 
      new ResultOfStartWithWordForString(leftSideString, false)

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string shouldNot endWith regex ("world")
     *        ^
     * </pre>
     */
    def shouldNot(endWithWord: EndWithWord): ResultOfEndWithWordForString = 
      new ResultOfEndWithWordForString(leftSideString, false)

    /**
     * This method enables syntax such as the following:
     *
     * <pre class="stHighlight">
     * string shouldNot include regex ("hi")
     *        ^
     * </pre>
     */
    def shouldNot(includeWord: IncludeWord): ResultOfIncludeWordForString = 
      new ResultOfIncludeWordForString(leftSideString, false)
*/
  }

  /**
   * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="Matchers.html">`Matchers`</a> for an overview of
   * the matchers DSL.
   *
   * This class is used in conjunction with an implicit conversion to enable `withGroup` and `withGroups` methods to
   * be invoked on `Regex`s.
   * 
   *
   * @author Bill Venners
   */
  final class RegexWrapper(regex: Regex) {

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * regex should fullyMatch regex ("a(b*)c" withGroup "bb") 
     *                                         ^
     * }}}
     */
    def withGroup(group: String): RegexWithGroups = 
      new RegexWithGroups(regex, IndexedSeq(group))

    /**
     * This method enables syntax such as the following:
     *
     * {{{  <!-- class="stHighlight" -->
     * regex should fullyMatch regex ("a(b*)(c*)" withGroups ("bb", "cc"))
     *                                            ^
     * }}}
     */
    def withGroups(groups: String*): RegexWithGroups = 
      new RegexWithGroups(regex, IndexedSeq(groups: _*))
  }

  /**
   * Implicitly converts an object of type `T` to a `AnyShouldWrapper[T]`,
   * to enable `should` methods to be invokable on that object.
   */
  implicit def convertToAnyShouldWrapper[T](o: T)(implicit pos: source.Position, prettifier: Prettifier): AnyShouldWrapper[T] = new AnyShouldWrapper(o, pos, prettifier)

  /**
   * Implicitly converts an object of type `java.lang.String` to a `StringShouldWrapper`,
   * to enable `should` methods to be invokable on that object.
   */
  implicit def convertToStringShouldWrapper(o: String)(implicit pos: source.Position, prettifier: Prettifier): StringShouldWrapper = new StringShouldWrapper(o, pos, prettifier)

  /**
   * Implicitly converts an object of type `scala.util.matching.Regex` to a `RegexWrapper`,
   * to enable `withGroup` and `withGroups` methods to be invokable on that object.
   */
  implicit def convertToRegexWrapper(o: Regex): RegexWrapper = new RegexWrapper(o)

  /**
   * This method enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * book should have (message ("A TALE OF TWO CITIES") (of [Book]), title ("A Tale of Two Cities"))
   *                                                     ^
   * }}}
   */
  def of[T](implicit ev: ClassTag[T]): ResultOfOfTypeInvocation[T] = new ResultOfOfTypeInvocation[T]
}

/**
 * Companion object that facilitates the importing of `Matchers` members as 
 * an alternative to mixing it the trait. One use case is to import `Matchers` members so you can use
 * them in the Scala interpreter.
 *
 * @author Bill Venners
 */
object Matchers extends Matchers
