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
package org.scalatest.prop

import org.scalatest.enablers.WheneverAsserting

/**
 * Trait that contains the `whenever` clause that can be used in table- or generator-driven property checks.
 *
 * @author Bill Venners
 */
trait Whenever {

  /**
   * Evaluates the passed code block if the passed boolean condition is true, else throws `DiscardedEvaluationException`.
   *
   * The `whenever` method can be used inside property check functions to discard invocations of the function with
   * data for which it is known the property would fail. For example, given the following `Fraction` class:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * class Fraction(n: Int, d: Int) {
   *
   *   require(d != 0)
   *   require(d != Integer.MIN_VALUE)
   *   require(n != Integer.MIN_VALUE)
   *
   *   val numer = if (d < 0) -1 * n else n
   *   val denom = d.abs
   *
   *   override def toString = numer + " / " + denom
   * }
   * }}}
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.prop.TableDrivenPropertyChecks._
   *
   * val fractions =
   *   Table(
   *     ("n", "d"),
   *     (  1,   2),
   *     ( -1,   2),
   *     (  1,  -2),
   *     ( -1,  -2),
   *     (  3,   1),
   *     ( -3,   1),
   *     ( -3,   0),
   *     (  3,  -1),
   *     (  3,  Integer.MIN_VALUE),
   *     (Integer.MIN_VALUE, 3),
   *     ( -3,  -1)
   *   )
   * }}}
   *
   * Imagine you wanted to check a property against this class with data that includes some
   * value that are rejected by the constructor, such as a denominator of zero, which should
   * result in an `IllegalArgumentException`. You could use `whenever`
   * to discard any rows in the `fraction` that represent illegal arguments, like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.matchers.Matchers._
   *
   * forAll (fractions) { (n: Int, d: Int) =>
   *
   *   whenever (d != 0 && d != Integer.MIN_VALUE
   *       && n != Integer.MIN_VALUE) {
   *
   *     val f = new Fraction(n, d)
   *
   *     if (n < 0 && d < 0 || n > 0 && d > 0)
   *       f.numer should be > 0
   *     else if (n != 0)
   *       f.numer should be < 0
   *     else
   *       f.numer should === (0)
   *
   *     f.denom should be > 0
   *   }
   * }
   * }}}
   *
   * In this example, rows 6, 8, and 9 have values that would cause a false to be passed
   * to `whenever`. (For example, in row 6, `d` is 0, which means `d` `!=` `0`
   * will be false.) For those rows, `whenever` will throw `DiscardedEvaluationException`,
   * which will cause the `forAll` method to discard that row.
   * 
   *
   * @param condition the boolean condition that determines whether `whenever` will evaluate the
   *    `fun` function (`condition` is true) or throws `DiscardedEvaluationException` (`condition` is false)
   * @param fun the function to evaluate if the specified `condition` is true
   */
  def whenever[T](condition: Boolean)(fun: => T)(implicit wa: WheneverAsserting[T]): wa.Result =
    wa.whenever(condition)(fun)
}
