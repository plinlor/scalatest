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
package org.scalatest.words

import org.scalatest.matchers._
import org.scalactic._
import org.scalatest.Resources
import org.scalatest.Suite
import org.scalactic.DefaultEquality.areEqualComparingArraysStructurally

/**
 * This class is part of the ScalaTest matchers DSL. Please see the documentation for <a href="../Matchers.html">`Matchers`</a> for an overview of
 * the matchers DSL.
 *
 * @author Bill Venners
 */
trait MatcherWords {

  /**
   * This field enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * string should (fullyMatch regex ("Hel*o, wor.d") and not have length (99))
   *                ^
   * }}}
   **/
  val fullyMatch = new FullyMatchWord

  /**
   * This field enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * string should (startWith ("Four") and include ("year"))
   *                ^
   * }}}
   **/
  val startWith = new StartWithWord

  /**
   * This field enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * string should (endWith ("ago") and include ("score"))
   *                ^
   * }}}
   **/
  val endWith = new EndWithWord

  /**
   * This field enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * string should (include ("hope") and not startWith ("no"))
   *                ^
   * }}}
   **/
  val include = new IncludeWord

/*
    In HaveWord's methods key, value, length, and size, I can give type parameters.
    The type HaveWord can contain a key method that takes a S or what not, and returns a matcher, which
    stores the key value in a val and whose apply method checks the passed map for the remembered key. This one would be used in things like:

    map should { have key 9 and have value "bob" }

    There's an overloaded should method on Shouldifier that takes a HaveWord. This method results in
    a different type that also has a key method that takes an S. So when you say:

    map should have key 9

    what happens is that this alternate should method gets invoked. The result is this other class that
    has a key method, and its constructor takes the map and stores it in a val. So this time when key is
    invoked, it checks to make sure the passed key is in the remembered map, and does the assertion.

    length and size can probably use structural types, because I want to use length on string and array for
    starters, and other people may create classes that have length methods. Would be nice to be able to use them.
  */

  /**
   * This field enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * list should (have length (3) and not contain ('a'))
   *              ^
   * }}}
   **/
  val have = new HaveWord

  /**
   * This field enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * obj should (be theSameInstanceAs (string) and be theSameInstanceAs (string))
   *             ^
   * }}}
   **/
  val be = new BeWord

  /**
   * This field enables syntax such as the following:
   *
   * {{{  <!-- class="stHighlight" -->
   * list should (contain ('a') and have length (7))
   *              ^
   * }}}
   **/
  val contain = new ContainWord

  /**
   * This field enables syntax like the following: 
   *
   * {{{  <!-- class="stHighlight" -->
   * myFile should (not be an (directory) and not have ('name ("foo.bar")))
   *                ^
   * }}}
   **/
  val not = new NotWord
  
  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * "hi" should not have length (3)
   *                      ^
   * }}}
   **/
  val length = new LengthWord
  
  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * set should not have size (3)
   *                     ^
   * }}}
   **/
  val size = new SizeWord
  
  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * seq should be (sorted)
   *               ^
   * }}}
   **/
  val sorted = new SortedWord

  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * seq should be (defined)
   *               ^
   * }}}
   **/
  val defined = new DefinedWord
  
  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * noException should be thrownBy
   * ^
   * }}}
   **/
  def noException(implicit pos: source.Position) = new NoExceptionWord(pos)
  
  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * file should exist
   *             ^
   * }}}
   **/
  val exist = new ExistWord

  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * 
   * file should be (readable)
   *                 ^
   * }}}
   **/
  val readable = new ReadableWord
  
  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * file should be (writable)
   *                 ^
   * }}}
   **/
  val writable = new WritableWord
  
  /**
   * This field enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * 
   * list should be (empty)
   *                 ^
   * }}}
   **/
  val empty = new EmptyWord

  /**
   * This field enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   *
   * "val a: String = 1" shouldNot compile
   *                               ^
   * }}}
   **/
  val compile = new CompileWord

  /**
   * This field enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   *
   * "val a: String = 1" shouldNot typeCheck
   *                               ^
   * }}}
   **/
  val typeCheck = new TypeCheckWord

  /**
   * This field enables the following syntax:
   *
   * {{{  <!-- class="stHighlight" -->
   *
   * result should matchPattern { case Person("Bob", _) => }
   *               ^
   * }}}
   **/
  val matchPattern = new MatchPatternWord

  /**
   * This method enables the following syntax: 
   *
   * {{{  <!-- class="stHighlight" -->
   * result should equal (7)
   *               ^
   * }}}
   *
   * The `left should equal (right)` syntax works by calling `==` on the `left`
   * value, passing in the `right` value, on every type except arrays. If both `left` and right are arrays, `deep`
   * will be invoked on both `left` and `right` before comparing them with ''==''. Thus, even though this expression
   * will yield false, because `Array`'s `equals` method compares object identity:
   * 
   * 
   * {{{  <!-- class="stHighlight" -->
   * Array(1, 2) == Array(1, 2) // yields false
   * }}}
   *
   * The following expression will ''not'' result in a `TestFailedException`, because ScalaTest will compare
   * the two arrays structurally, taking into consideration the equality of the array's contents:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * Array(1, 2) should equal (Array(1, 2)) // succeeds (i.e., does not throw TestFailedException)
   * }}}
   *
   * If you ever do want to verify that two arrays are actually the same object (have the same identity), you can use the
   * `be theSameInstanceAs` syntax.
   * 
   *
   */
  def equal(right: Any): MatcherFactory1[Any, Equality] =
    new MatcherFactory1[Any, Equality] {
      def matcher[T <: Any : Equality]: Matcher[T] = {
        val equality = implicitly[Equality[T]]
        new Matcher[T] {
          def apply(left: T): MatchResult = {
            val (leftee, rightee) = Suite.getObjectsForFailureMessage(left, right) // TODO: to move this code to reporters
            MatchResult(
              equality.areEqual(left, right),
              Resources.rawDidNotEqual,
              Resources.rawEqualed,
              Vector(leftee, rightee), 
              Vector(left, right)
            )
          }
          override def toString: String = "equal (" + Prettifier.default(right) + ")"
        }
      }
      override def toString: String = "equal (" + Prettifier.default(right) + ")"
    }
}

object MatcherWords extends MatcherWords
