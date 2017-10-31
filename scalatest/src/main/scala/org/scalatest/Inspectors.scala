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

import scala.collection.GenTraversable
import scala.annotation.tailrec
import scala.collection.GenSeq
import Suite.indentLines
import FailureMessages.decorateToStringValue
import enablers.Collecting
import scala.language.higherKinds
import enablers.InspectorAsserting
import org.scalactic._

/**
 * Provides nestable ''inspector methods'' (or just ''inspectors'') that enable assertions to be made about collections.
 *
 * For example, the `forAll` method enables you to state that something should be true about all elements of a collection, such
 * as that all elements should be positive:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; import Assertions._
 * import Assertions._
 *
 * scala&gt; import Inspectors._
 * import Inspectors._
 *
 * scala&gt; val xs = List(1, 2, 3, 4, 5)
 * xs: List[Int] = List(1, 2, 3, 4, 5)
 *
 * scala&gt; forAll (xs) { x =&gt; assert(x &gt; 0) }
 * }}}
 *
 * Or, with matchers:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import Matchers._
 * import Matchers._
 *
 * scala&gt; forAll (xs) { x =&gt; x should be &gt; 0 }
 * }}}
 *
 * To make assertions about nested collections, you can nest the inspector method invocations.
 * For example, given the following list of lists of `Int`:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; val yss =
 *      |   List(
 *      |     List(1, 2, 3),
 *      |     List(1, 2, 3),
 *      |     List(1, 2, 3)
 *      |   )
 * yss: List[List[Int]] = List(List(1, 2, 3), List(1, 2, 3), List(1, 2, 3))
 * }}}
 *
 * You can assert that all `Int` elements in all nested lists are positive by nesting two `forAll` method invocations, like this:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; forAll (yss) { ys =&gt;
 *      |   forAll (ys) { y =&gt; y should be &gt; 0 }
 *      | }
 * }}}
 *
 * The full list of inspector methods are:
 * 
 *
 * <ul>
 * <li>`forAll` - succeeds if the assertion holds true for every element</li>
 * <li>`forAtLeast` - succeeds if the assertion holds true for at least the specified number of elements</li>
 * <li>`forAtMost` - succeeds if the assertion holds true for at most the specified number of elements</li>
 * <li>`forBetween` - succeeds if the assertion holds true for between the specified minimum and maximum number of elements, inclusive</li>
 * <li>`forEvery` - same as `forAll`, but lists all failing elements if it fails (whereas `forAll` just reports the first failing element)</li>
 * <li>`forExactly` - succeeds if the assertion holds true for exactly the specified number of elements</li>
 * </ul>
 *
 * The error messages produced by inspector methods are designed to make sense no matter how deeply you nest the method invocations. 
 * Here's an example of a nested inspection that fails and the resulting error message:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; forAll (yss) { ys =&gt;
 *      |   forAll (ys) { y =&gt; y should be &lt; 2 }
 *      | }
 * org.scalatest.exceptions.TestFailedException: forAll failed, because: 
 *   at index 0, forAll failed, because: 
 *     at index 1, 2 was not less than 2 (&lt;console&gt;:20) 
 *   in List(1, 2, 3) (&lt;console&gt;:20) 
 * in List(List(1, 2, 3), List(1, 2, 3), List(1, 2, 3))
 *      at org.scalatest.InspectorsHelper$.forAll(Inspectors.scala:146)
 *      ...
 * }}}
 *
 * One way the error message is designed to help you understand the error is by using indentation that mimics the indentation of the
 * source code (optimistically assuming the source will be nicely indented). The error message above indicates the outer `forAll` failed
 * because its initial `List` (''i.e.'', at index 0) failed
 * the assertion, which was that all elements of that initial `List[Int]` at index 0 should be less than 2. This assertion failed because index 1 of
 * that inner list contained the value 2, which was indeed &ldquo;not less than 2.&rdquo; The error message for the inner list is an indented line inside the error message
 * for the outer list. The actual contents of each list are displayed at the end in inspector error messages, also indented appropriately. The actual contents
 * are placed at the end so that for very large collections, the contents will not drown out and make it difficult to find the messages that describe
 * actual causes of the failure.
 * 
 *
 * The `forAll` and `forEvery` methods are similar in that both succeed only if the assertion holds for all elements of the collection.
 * They differ in that `forAll` will only report the first element encountered that failed the assertion, but `forEvery` will report ''all''
 * elements that fail the assertion. The tradeoff is that while `forEvery` gives more information, it may take longer to run because it must inspect every element
 * of the collection. The `forAll` method can simply stop inspecting once it encounters the first failing element. Here's an example that
 * shows the difference in the `forAll` and `forEvery` error messages:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; forAll (xs) { x =&gt; x should be &lt; 3 }
 * org.scalatest.exceptions.TestFailedException: forAll failed, because: 
 *   at index 2, 3 was not less than 3 (&lt;console&gt;:18) 
 * in List(1, 2, 3, 4, 5)
 *      at org.scalatest.InspectorsHelper$.forAll(Inspectors.scala:146)
 *      ...
 *
 * scala&gt; forEvery (xs) { x =&gt; x should be &lt; 3 }
 * org.scalatest.exceptions.TestFailedException: forEvery failed, because: 
 *   at index 2, 3 was not less than 3 (&lt;console&gt;:18), 
 *   at index 3, 4 was not less than 3 (&lt;console&gt;:18), 
 *   at index 4, 5 was not less than 3 (&lt;console&gt;:18) 
 * in List(1, 2, 3, 4, 5)
 *      at org.scalatest.InspectorsHelper$.forEvery(Inspectors.scala:226)
 *      ...
 * }}}
 *
 * Note that if you're using matchers, you can alternatively use ''inspector shorthands'' for writing non-nested
 * inspections. Here's an example:
 * 
 * 
 * {{{
 * scala&gt; all (xs) should be &gt; 3
 * org.scalatest.exceptions.TestFailedException: 'all' inspection failed, because: 
 *   at index 0, 1 was not greater than 3 
 * in List(1, 2, 3, 4, 5)
 *      at org.scalatest.InspectorsHelper$.forAll(Inspectors.scala:146)
 * }}}
 *
 * You can use `Inspectors` on any `scala.collection.GenTraversable`, `java.util.Collection`,
 * `java.util.Map` (with <a href="Entry.html">`Entry`</a>), `Array`, or `String`. 
 * Here are some examples:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 * 
 * scala&gt; import Inspectors._
 * import Inspectors._
 * 
 * scala&gt; import Matchers._
 * import Matchers._
 * 
 * scala&gt; forAll (Array(1, 2, 3)) { e =&gt; e should be &lt; 5 }
 * 
 * scala&gt; import collection.JavaConverters._
 * import collection.JavaConverters._
 * 
 * scala&gt; val js = List(1, 2, 3).asJava
 * js: java.util.List[Int] = [1, 2, 3]
 * 
 * scala&gt; forAll (js) { j =&gt; j should be &lt; 5 }
 * 
 * scala&gt; val jmap = Map("a" -&gt; 1, "b" -&gt; 2).asJava 
 * jmap: java.util.Map[String,Int] = {a=1, b=2}
 * 
 * scala&gt; forAtLeast(1, jmap) { e =&gt; e shouldBe Entry("b", 2) }
 * 
 * scala&gt; forAtLeast(2, "hello, world!") { c =&gt; c shouldBe 'o' }
 * }}}
 */
trait Inspectors {

  
  /**
   * Ensure that all elements in a given collection pass the given inspection function, where "pass" means returning normally from the function (''i.e.'',
   * without throwing an exception).
   *
   *  The difference between `forAll` and `forEvery` is that
   * `forAll` will stop on the first failure, while `forEvery` will continue to inspect all elements after the
   * first failure (and report all failures).
   * 
   *
   * @param xs the collection of elements
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam E the type of element in the collection
   * @tparam C the type of collection
   *
   */
  def forAll[E, C[_], ASSERTION](xs: C[E])(fun: E => ASSERTION)(implicit collecting: Collecting[E, C[E]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forAll(collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  // SKIP-SCALATESTJS-START
  /**
   * Ensure that all elements in a given `java.util.Map` pass the given inspection function, where "pass" means returning normally from the function (''i.e.'',
   * without throwing an exception).
   *
   * The difference between `forAll` and `forEvery` is that
   * `forAll` will stop on the first failure, while `forEvery` will continue to inspect all `java.util.Map` elements after the
   * first failure (and report all failures).
   * 
   *
   * @param xs the `java.util.Map`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam K the type of key in the Java Map
   * @tparam V the type of value in the Java Map
   * @tparam JMAP subtype of `java.util.Map`
   *
   */
  def forAll[K, V, JMAP[k, v] <: java.util.Map[k, v], ASSERTION](xs: JMAP[K, V])(fun: org.scalatest.Entry[K, V] => ASSERTION)(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forAll(collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }
  // SKIP-SCALATESTJS-END

  /**
   * Ensure that all characters in a given `String` pass the given inspection function, where "pass" means returning normally from the function (''i.e.'',
   * without throwing an exception).
   *
   * The difference between `forAll` and `forEvery` is that
   * `forAll` will stop on the first failure, while `forEvery` will continue to inspect all characters in the `String` after the
   * first failure (and report all failures).
   * 
   *
   * @param xs the `String`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   *
   */
  def forAll[ASSERTION](xs: String)(fun: Char => ASSERTION)(implicit collecting: Collecting[Char, String], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forAll(collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  /**
   * Ensure that at least `min` number of elements of a given collection pass the given inspection function.
   *
   * @param min the minimum number of elements that must pass the inspection function
   * @param xs the collection of elements
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam E the type of element in the collection
   * @tparam C the type of collection
   *
   */
  def forAtLeast[E, C[_], ASSERTION](min: Int, xs: C[E])(fun: E => ASSERTION)(implicit collecting: Collecting[E, C[E]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forAtLeast(min, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  // SKIP-SCALATESTJS-START
  /**
   * Ensure that at least `min` number of elements in a given `java.util.Map` pass the given inspection function.
   *
   * @param min the minimum number of elements that must pass the inspection function
   * @param xs the `java.util.Map`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam K the type of key in the `java.util.Map`
   * @tparam V the type of value in the `java.util.Map`
   * @tparam JMAP subtype of `java.util.Map`
   *
   */
  def forAtLeast[K, V, JMAP[k, v] <: java.util.Map[k, v], ASSERTION](min: Int, xs: JMAP[K, V])(fun: org.scalatest.Entry[K, V] => ASSERTION)(implicit collecting: Collecting[org.scalatest.Entry[K, V],JMAP[K, V]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forAtLeast(min, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }
  // SKIP-SCALATESTJS-END

  /**
   * Ensure that at least `min` number of characters in a given `String` pass the given inspection function.
   *
   * @param min the minimum number of characters in `String` that must pass the inspection function
   * @param xs the `String`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   *
   */
  def forAtLeast[ASSERTION](min: Int, xs: String)(fun: Char => ASSERTION)(implicit collecting: Collecting[Char, String], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forAtLeast(min, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  private def shouldIncludeIndex[T, R](xs: GenTraversable[T]) = xs.isInstanceOf[GenSeq[T]]

  /**
   * Ensure that at most `max` number of elements of a given collection pass the given inspection function.
   *
   * @param max the maximum number of elements that must pass the inspection function
   * @param xs the collection of elements
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam E the type of element in the collection
   * @tparam C the type of collection
   */
  def forAtMost[E, C[_], ASSERTION](max: Int, xs: C[E])(fun: E => ASSERTION)(implicit collecting: Collecting[E, C[E]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forAtMost(max, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  // SKIP-SCALATESTJS-START
  /**
   * Ensure that at most `max` number of elements in a given `java.util.Map` pass the given inspection function.
   *
   * @param max the maximum number of elements in the `java.util.Map` that must pass the inspection function
   * @param xs the `java.util.Map`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam K the type of key in the `java.util.Map`
   * @tparam V the type of value in the `java.util.Map`
   * @tparam JMAP subtype of `java.util.Map`
   */
  def forAtMost[K, V, JMAP[k, v] <: java.util.Map[k, v], ASSERTION](max: Int, xs: JMAP[K, V])(fun: org.scalatest.Entry[K, V] => ASSERTION)(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forAtMost(max, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }
  // SKIP-SCALATESTJS-END

  /**
   * Ensure that at most `max` number of characters in a given `String` pass the given inspection function.
   *
   * @param max the maximum number of characters in `String` that must pass the inspection function
   * @param xs the `String`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   */
  def forAtMost[ASSERTION](max: Int, xs: String)(fun: Char => ASSERTION)(implicit collecting: Collecting[Char, String], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forAtMost(max, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  /**
   * Ensure that exactly `succeededCount` number of elements of a given collection pass the given inspection function.
   *
   * @param succeededCount the number of elements that must pass the inspection function
   * @param xs the collection of elements
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam E the type of element in the collection
   * @tparam C the type of collection
   */
  def forExactly[E, C[_], ASSERTION](succeededCount: Int, xs: C[E])(fun: E => ASSERTION)(implicit collecting: Collecting[E, C[E]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forExactly(succeededCount, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  // SKIP-SCALATESTJS-START
  /**
   * Ensure that exactly `succeededCount` number of elements in a given `java.util.Map` pass the given inspection function.
   *
   * @param succeededCount the number of elements in the `java.util.Map` that must pass the inspection function
   * @param xs the `java.util.Map`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam K the type of key in the `java.util.Map`
   * @tparam V the type of value in the `java.util.Map`
   * @tparam JMAP subtype of `java.util.Map`
   */
  def forExactly[K, V, JMAP[k, v] <: java.util.Map[k, v], ASSERTION](succeededCount: Int, xs: JMAP[K, V])(fun: org.scalatest.Entry[K, V] => ASSERTION)(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forExactly(succeededCount, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }
  // SKIP-SCALATESTJS-END

  /**
   * Ensure that exactly `succeededCount` number of characters in a given `String` pass the given inspection function.
   *
   * @param succeededCount the number of characters in the `String` that must pass the inspection function
   * @param xs the `String`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   */
  def forExactly[ASSERTION](succeededCount: Int, xs: String)(fun: Char => ASSERTION)(implicit collecting: Collecting[Char, String], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forExactly(succeededCount, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }
  
  private[scalatest] def forNo[E, C[_], ASSERTION](xs: C[E])(fun: E => ASSERTION)(implicit collecting: Collecting[E, C[E]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forNo(collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  // SKIP-SCALATESTJS-START
  private[scalatest] def forNo[K, V, JMAP[k, v] <: java.util.Map[k, v], ASSERTION](xs: JMAP[K, V])(fun: org.scalatest.Entry[K, V] => ASSERTION)(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forNo(collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }
  // SKIP-SCALATESTJS-END

  private[scalatest] def forNo[ASSERTION](xs: String)(fun: Char => ASSERTION)(implicit collecting: Collecting[Char, String], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forNo(collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  /**
   * Ensure the number of elements of a given collection that pass the given inspection function is between `from` and `upTo`.
   *
   * @param from the minimum number of elements that must pass the inspection number
   * @param upTo the maximum number of elements that must pass the inspection number
   * @param xs the collection of elements
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam E the type of element in the collection
   * @tparam C the type of collection
   */
  def forBetween[E, C[_], ASSERTION](from: Int, upTo: Int, xs: C[E])(fun: E => ASSERTION)(implicit collecting: Collecting[E, C[E]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forBetween(from, upTo, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  // SKIP-SCALATESTJS-START
  /**
   * Ensure the number of elements in a given `java.util.Map` that pass the given inspection function is between `from` and `upTo`.
   *
   * @param from the minimum number of elements in the `java.util.Map` that must pass the inspection number
   * @param upTo the maximum number of elements in the `java.util.Map` that must pass the inspection number
   * @param xs the `java.util.Map`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam K the type of key in the `java.util.Map`
   * @tparam V the type of value in the `java.util.Map`
   * @tparam JMAP subtype of `java.util.Map`
   */
  def forBetween[K, V, JMAP[k, v] <: java.util.Map[k, v], ASSERTION](from: Int, upTo: Int, xs: JMAP[K, V])(fun: org.scalatest.Entry[K, V] => ASSERTION)(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forBetween(from, upTo, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }
  // SKIP-SCALATESTJS-END

  /**
   * Ensure the number of characters of a given `String` that pass the given inspection function is between `from` and `upTo`.
   *
   * @param from the minimum number of characters in the `String` that must pass the inspection number
   * @param upTo the maximum number of characters in the `String` that must pass the inspection number
   * @param xs the `String`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   */
  def forBetween[ASSERTION](from: Int, upTo: Int, xs: String)(fun: Char => ASSERTION)(implicit collecting: Collecting[Char, String], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forBetween(from, upTo, collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  /**
   * Ensure that every element in a given collection passes the given inspection function, where "pass" means returning normally from the function (''i.e.'',
   * without throwing an exception).
   *
   * The difference between `forEvery` and `forAll` is that
   * `forEvery` will continue to inspect all elements after first failure, and report all failures,
   * whereas `forAll` will stop on (and only report) the first failure.
   * 
   *
   * @param xs the collection of elements
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam E the type of element in the collection
   * @tparam C the type of collection
   */
  def forEvery[E, C[_], ASSERTION](xs: C[E])(fun: E => ASSERTION)(implicit collecting: Collecting[E, C[E]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forEvery(collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }

  // SKIP-SCALATESTJS-START
  /**
   * Ensure that every element in a given `java.util.Map` passes the given inspection function, where "pass" means returning normally
   * from the function (''i.e.'', without throwing an exception).
   *
   * The difference between `forEvery` and `forAll` is that
   * `forEvery` will continue to inspect all elements in the `java.util.Map` after first failure, and report all failures,
   * whereas `forAll` will stop on (and only report) the first failure.
   * 
   *
   * @param xs the `java.util.Map`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   * @tparam K the type of key in the `java.util.Map`
   * @tparam V the type of value in the `java.util.Map`
   * @tparam JMAP subtype of `java.util.Map`
   */
  def forEvery[K, V, JMAP[k, v] <: java.util.Map[k, v], ASSERTION](xs: JMAP[K, V])(fun: org.scalatest.Entry[K, V] => ASSERTION)(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forEvery(collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }
  // SKIP-SCALATESTJS-END

  /**
   * Ensure that every character in a given `String` passes the given inspection function, where "pass" means returning normally from the function (''i.e.'',
   * without throwing an exception).
   *
   * The difference between `forEvery` and `forAll` is that
   * `forEvery` will continue to inspect all characters in the `String` after first failure, and report all failures,
   * whereas `forAll` will stop on (and only report) the first failure.
   * 
   *
   * @param xs the `String`
   * @param fun the inspection function
   * @param collecting the implicit `Collecting` that can transform `xs` into a `scala.collection.GenTraversable`
   */
  def forEvery[ASSERTION](xs: String)(fun: Char => ASSERTION)(implicit collecting: Collecting[Char, String], asserting: InspectorAsserting[ASSERTION], prettifier: Prettifier, pos: source.Position): asserting.Result = {
    asserting.forEvery(collecting.genTraversableFrom(xs), xs, false, prettifier, pos)(fun)
  }
}

/**
 * Companion object that facilitates the importing of `Inspectors` members as
 * an alternative to mixing it in. One use case is to import `Inspectors`'s members so you can use
 * them in the Scala interpreter.
 */
object Inspectors extends Inspectors

private[scalatest] object InspectorsHelper {

  def indentErrorMessages(messages: IndexedSeq[String]) = indentLines(1, messages)

  def shouldPropagate(throwable: Throwable): Boolean = 
    throwable match {
      case _: exceptions.NotAllowedException |
           _: exceptions.TestPendingException |
           _: exceptions.TestCanceledException => true
      case _ if Suite.anExceptionThatShouldCauseAnAbort(throwable) => true
      case _ => false
    }

}
