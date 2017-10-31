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

import org.scalactic.Requirements._
import org.scalactic.exceptions.NullArgumentException

/**
 * A `Suite` class mixing in <a href="SequentialNestedSuiteExecution.html">`SequentialNestedSuiteExecution`</a> that takes
 * zero to many `Suite`s, which will be returned from its `nestedSuites` method.
 *
 * For example, you can define a suite that always executes a list of
 * nested suites like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class StepsSuite extends Sequential(
 *   new Step1Suite,
 *   new Step2Suite,
 *   new Step3Suite,
 *   new Step4Suite,
 *   new Step5Suite
 * )
 * }}}
 *
 * When `StepsSuite` is executed, it will execute its
 * nested suites in the passed order: `Step1Suite`, `Step2Suite`,
 * `Step3Suite`, `Step4Suite`, and `Step5Suite`.
 * Because `Sequential` extends `SequentialNestedSuiteExecution`,
 * the distributor passed to `runNestedSuites` will always be `None`.
 * So not only will the suites passed to the constructor be executed sequentially, any
 * tests and nested suites of the passed suites will also be executed sequentually.
 * 
 *
 * The difference between `Sequential` and <a href="Stepwise.html">`Stepwise`</a>
 * is that although `Stepwise` executes its own nested suites sequentially, it passes
 * whatever distributor was passed to it to those nested suites. Thus the nested suites could run their own nested
 * suites and tests in parallel if that distributor is defined. By contrast, `Sequential` always
 * passes `None` for the distributor to the nested suites, so any and every test and nested suite 
 * contained within the nested suites passed to the `Sequential` construtor will be executed sequentially.
 * 
 * 
 * @param suitesToNest a sequence of `Suite`s to nest.
 *
 * @throws NullArgumentException if `suitesToNest`, or any suite
 * it contains, is `null`.
 *
 * @author Bill Venners
 */
class Sequential(suitesToNest: Suite*) extends Suite with SequentialNestedSuiteExecution { thisSuite => 

  requireNonNull(suitesToNest)

  for (s <- suitesToNest) {
    if (s == null)
      throw new NullArgumentException("A passed suite was null")
  }

  /**
   * Returns an immutable `IndexedSeq` containing the suites passed to the constructor in
   * the order they were passed.
   */
  override val nestedSuites: collection.immutable.IndexedSeq[Suite] = Vector.empty ++ suitesToNest

  /**
   * Returns a user friendly string for this suite, composed of the
   * simple name of the class (possibly simplified further by removing dollar signs if added by the Scala interpeter) and, if this suite
   * contains nested suites, the result of invoking `toString` on each
   * of the nested suites, separated by commas and surrounded by parentheses.
   *
   * @return a user-friendly string for this suite
   */
  override def toString: String = Suite.suiteToString(None, thisSuite)
}

/**
 * Companion object to class `Sequential` that offers an `apply` factory method
 * for creating a `Sequential` instance.
 *
 * One use case for this object is to run multiple specification-style suites in the Scala interpreter, like this:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; Sequential(new MyFirstSuite, new MyNextSuite).execute()
 * }}}
 */
object Sequential {

  /**
   * Factory method for creating a `Sequential` instance.
   */
  def apply(suitesToNest: Suite*): Sequential = new Sequential(suitesToNest: _*)
}

