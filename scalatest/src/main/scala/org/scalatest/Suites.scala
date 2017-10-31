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
 * A `Suite` class that takes zero to many `Suite`s in its constructor,
 *  which will be returned from its `nestedSuites` method.
 *
 * For example, you can define a suite that always executes a list of
 * nested suites like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class StepsSuite extends Suites(
 *   new Step1Suite,
 *   new Step2Suite,
 *   new Step3Suite,
 *   new Step4Suite,
 *   new Step5Suite
 * )
 * }}}
 *
 * If `StepsSuite` is executed sequentially, it will execute its
 * nested suites in the passed order: `Step1Suite`, `Step2Suite`,
 * `Step3Suite`, `Step4Suite`, and `Step5Suite`.
 * If `StepsSuite` is executed in parallel, the nested suites will
 * be executed concurrently.
 * 
 *
 * @param suitesToNest a sequence of `Suite`s to nest.
 *
 * @throws NullPointerException if `suitesToNest`, or any suite
 * it contains, is `null`.
 *
 * @author Bill Venners
 */
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
class Suites(suitesToNest: Suite*) extends Suite { thisSuite =>

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
 * Companion object to class `Suites` that offers an `apply` factory method
 * for creating a `Suites` instance.
 *
 * One use case for this object is to run multiple specification-style suites in the Scala interpreter, like this:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; Suites(new MyFirstSuite, new MyNextSuite).execute()
 * }}}
 */
object Suites {

  /**
   * Factory method for creating a `Suites` instance.
   */
  def apply(suitesToNest: Suite*): Suites = new Suites(suitesToNest: _*)
}

