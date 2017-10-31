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
import java.util.NoSuchElementException
import org.scalatest.exceptions.StackDepthException
import org.scalatest.exceptions.TestFailedException

/**
 * Trait that provides an implicit conversion that adds a `value` method
 * to `Option`, which will return the value of the option if it is defined,
 * or throw `TestFailedException` if not.
 *
 * This construct allows you to express in one statement that an option should be defined
 * and that its value should meet some expectation. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * opt.value should be &gt; 9
 * }}}
 *
 * Or, using an assertion instead of a matcher expression:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assert(opt.value &gt; 9)
 * }}}
 *
 * Were you to simply invoke `get` on the `Option`, 
 * if the option wasn't defined, it would throw a `NoSuchElementException`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val opt: Option[Int] = None
 *
 * opt.get should be &gt; 9 // opt.get throws NoSuchElementException
 * }}}
 *
 * The `NoSuchElementException` would cause the test to fail, but without providing a <a href="exceptions/StackDepth.html">stack depth</a> pointing
 * to the failing line of test code. This stack depth, provided by <a href="exceptions/TestFailedException.html">`TestFailedException`</a> (and a
 * few other ScalaTest exceptions), makes it quicker for
 * users to navigate to the cause of the failure. Without `OptionValues`, to get
 * a stack depth exception you would need to make two statements, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val opt: Option[Int] = None
 *
 * opt should be ('defined) // throws TestFailedException
 * opt.get should be &gt; 9
 * }}}
 *
 * The `OptionValues` trait allows you to state that more concisely:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val opt: Option[Int] = None
 *
 * opt.value should be &gt; 9 // opt.value throws TestFailedException
 * }}}
 */
trait OptionValues {

  import scala.language.implicitConversions

  /**
   * Implicit conversion that adds a `value` method to `Option`.
   *
   * @param opt the `Option` on which to add the `value` method
   */
  implicit def convertOptionToValuable[T](opt: Option[T])(implicit pos: source.Position): Valuable[T] = new Valuable(opt, pos)

  /**
   * Wrapper class that adds a `value` method to `Option`, allowing
   * you to make statements like:
   *
   * {{{  <!-- class="stHighlight" -->
   * opt.value should be &gt; 9
   * }}}
   *
   * @param opt An option to convert to `Valuable`, which provides the `value` method.
   */
  class Valuable[T](opt: Option[T], pos: source.Position) {

    /**
     * Returns the value contained in the wrapped `Option`, if defined, else throws `TestFailedException` with
     * a detail message indicating the option was not defined.
     */
    def value: T = {
      try {
        opt.get
      }
      catch {
        case cause: NoSuchElementException => 
          throw new TestFailedException((_: StackDepthException) => Some(Resources.optionValueNotDefined), Some(cause), pos)
      }
    }
  }
}

/**
 * Companion object that facilitates the importing of `OptionValues` members as 
 * an alternative to mixing it in. One use case is to import `OptionValues`'s members so you can use
 * `value` on option in the Scala interpreter:
 *
 * {{{  <!-- class="stREPL" -->
 * $ scala -cp scalatest-1.7.jar
 * Welcome to Scala version 2.9.1.final (Java HotSpot(TM) 64-Bit Server VM, Java 1.6.0_29).
 * Type in expressions to have them evaluated.
 * Type :help for more information.
 *
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; import matchers.Matchers._
 * import matchers.Matchers._
 *
 * scala&gt; import OptionValues._
 * import OptionValues._
 *
 * scala&gt; val opt1: Option[Int] = Some(1)
 * opt1: Option[Int] = Some(1)
 * 
 * scala&gt; val opt2: Option[Int] = None
 * opt2: Option[Int] = None
 * 
 * scala&gt; opt1.value should be &lt; 10
 * 
 * scala&gt; opt2.value should be &lt; 10
 * org.scalatest.TestFailedException: The Option on which value was invoked was not defined.
 *   at org.scalatest.OptionValues$Valuable.value(OptionValues.scala:68)
 *   at .&lt;init&gt;(&lt;console&gt;:18)
 *   ...
 * }}}
 *
 */
object OptionValues extends OptionValues

