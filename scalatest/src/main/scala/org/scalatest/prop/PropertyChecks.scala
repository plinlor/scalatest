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
package prop

/**
 * Trait that facilitates property checks on data supplied by tables and generators.
 *
 * This trait extends both <a href="TableDrivenPropertyChecks.html">`TableDrivenPropertyChecks`</a> and
 * <a href="GeneratorDrivenPropertyChecks.html">`GeneratorDrivenPropertyChecks`</a>. Thus by mixing in
 * this trait you can perform property checks on data supplied either by tables or generators. For the details of
 * table- and generator-driven property checks, see the documentation for each by following the links above.
 * 
 *
 * For a quick example of using both table and generator-driven property checks in the same suite of tests, however,
 * imagine you want to test this `Fraction` class:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class Fraction(n: Int, d: Int) {
 *
 *   require(d != 0)
 *   require(d != Integer.MIN_VALUE)
 *   require(n != Integer.MIN_VALUE)
 *
 *   val numer = if (d &lt; 0) -1 * n else n
 *   val denom = d.abs
 *
 *   override def toString = numer + " / " + denom
 * }
 * }}}
 *
 * If you mix in `PropertyChecks`, you could use a generator-driven property check to test that the passed values for numerator and
 * denominator are properly normalized, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * forAll { (n: Int, d: Int) =&gt;
 *
 *   whenever (d != 0 && d != Integer.MIN_VALUE
 *       && n != Integer.MIN_VALUE) {
 *
 *     val f = new Fraction(n, d)
 *
 *     if (n &lt; 0 && d &lt; 0 || n &gt; 0 && d &gt; 0)
 *       f.numer should be &gt; 0
 *     else if (n != 0)
 *       f.numer should be &lt; 0
 *     else
 *       f.numer shouldEqual 0
 *
 *     f.denom should be &gt; 0
 *   }
 * }
 * }}}
 *
 * And you could use a table-driven property check to test that all combinations of invalid values passed to the `Fraction` constructor
 * produce the expected `IllegalArgumentException`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val invalidCombos =
 *   Table(
 *     ("n",               "d"),
 *     (Integer.MIN_VALUE, Integer.MIN_VALUE),
 *     (1,                 Integer.MIN_VALUE),
 *     (Integer.MIN_VALUE, 1),
 *     (Integer.MIN_VALUE, 0),
 *     (1,                 0)
 *   )
 *
 * forAll (invalidCombos) { (n: Int, d: Int) =&gt;
 *   an [IllegalArgumentException] should be thrownBy {
 *     new Fraction(n, d)
 *   }
 * }
 * }}}
 *
 * @author Bill Venners
 */
trait PropertyChecks extends TableDrivenPropertyChecks with GeneratorDrivenPropertyChecks

/**
 * Companion object that facilitates the importing of `PropertyChecks` members as 
 * an alternative to mixing it in. One use case is to import `PropertyChecks` members so you can use
 * them in the Scala interpreter.
 *
 * @author Bill Venners
 */
object PropertyChecks extends PropertyChecks

