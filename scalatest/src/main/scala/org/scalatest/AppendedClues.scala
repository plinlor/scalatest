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
import exceptions.ModifiableMessage

/**
 * Trait providing an implicit conversion that allows clues to be placed after a block of code.
 *
 * You can use the `withClue` construct provided by <a href="Assertions.html">`Assertions`</a>, which is
 * extended by every style trait in ScalaTest, to add extra information to reports of failed or canceled tests.
 * The `withClue` from `Assertions` places the "clue string" at the front, both
 * in the code and in the resulting message:
 *
 * {{{  <!-- class="stHighlight" -->
 * withClue("This is a prepended clue;") {
 *   1 + 1 should equal (3)
 * }
 * }}}
 *
 * The above expression will yield the failure message:
 * 
 *
 * `This is a prepended clue; 2 did not equal 3`
 * 
 *
 * If you mix in this trait, or import its members via its companion object, you can alternatively place
 * the clue string at the end, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * { 1 + 1 should equal (3) } withClue "now the clue comes after"
 * }}}
 *
 * The above expression will yield the failure message:
 * 
 *
 * `2 did not equal 3 now the clue comes after`
 * 
 *
 * If no space is already present, either at the beginning of the clue string or at the end
 * of the current message, a space will be placed between the two, unless the clue string
 * starts with one of the punctuation characters: comma (`,`), period (`.`),
 * or semicolon (`;`). For example, the failure message in the above example
 * includes an extra space inserted between ''3'' and ''now''.
 * 
 *
 * By contrast this code, which has a clue string starting with comma:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * { 1 + 1 should equal (3) } withClue ", now the clue comes after"
 * }}}
 *
 * Will yield a failure message with no extra inserted space:
 * 
 *
 * `2 did not equal 3, now the clue comes after`
 * 
 *
 * The `withClue` method will only append the clue string to the detail
 * message of exception types that mix in the `ModifiableMessage` trait.
 * See the documentation for <a href="exceptions/ModifiableMessage.html">`ModifiableMessage`</a> for more
 * information.
 * 
 *
 * Note: the reason this functionality is not provided by `Assertions` directly, like the
 * prepended `withClue` construct, is because appended clues require an implicit conversion.
 * ScalaTest only gives you one implicit conversion by default in any test class to minimize the
 * potential for conflicts with other implicit conversions you may be using. All other implicit conversions,
 * including the one provided by this trait, you must explicitly invite into your code through inheritance
 * or an import.
 * 
 *
 * @author Bill Venners
 */
trait AppendedClues {

  /**
   * Class that provides a `withClue` method that appends clue strings to any
   * <a href="exceptions/ModifiableMessage.html">`ModifiableMessage`</a> exception
   * thrown by the passed by-name parameter.
   *
   * @author Bill Venners
   */
  class Clueful[T](fun: => T) {

    /**
     * Executes the block of code passed as the constructor parameter to this `Clueful`, and, if it
     * completes abruptly with a `ModifiableMessage` exception,
     * appends the "clue" string passed to this method to the end of the detail message
     * of that thrown exception, then rethrows it. If clue does not begin in a white space
     * character or one of the punctuation characters: comma (`,`),
     * period (`.`), or semicolon (`;`), one space will be added
     * between it and the existing detail message (unless the detail message is
     * not defined).
     *
     * This method allows you to add more information about what went wrong that will be
     * reported when a test fails or cancels. For example, this code:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * { 1 + 1 should equal (3) } withClue ", not even for very large values of 1"
     * }}}
     *
     * Would yield a `TestFailed` exception whose message would be:
     * 
     *
     * {{{
     * 2 did not equal 3, not even for very large values of 1
     * }}}
     *
     * @throws NullArgumentException if the passed `clue` is `null`
     */
    def withClue(clue: Any): T = {
      requireNonNull(clue)
      def append(currentMessage: Option[String]) =
        currentMessage match {
          case Some(msg) => Some(AppendedClues.appendClue(msg, clue.toString))
          case None => Some(clue.toString)
        }
      try {
        val outcome = fun
        outcome match {
          case Failed(e: org.scalatest.exceptions.ModifiableMessage[_]) if clue.toString != "" =>
            Failed(e.modifyMessage(append)).asInstanceOf[T]
          case Canceled(e: org.scalatest.exceptions.ModifiableMessage[_]) if clue.toString != "" =>
            Canceled(e.modifyMessage(append)).asInstanceOf[T]
          case _ => outcome
        }
      }
      catch {
        case e: ModifiableMessage[_] =>
          if (clue.toString != "")
            throw e.modifyMessage(append)
          else
            throw e
      }
    }
  }

  import scala.language.implicitConversions

  /**
   * Implicit conversion that allows clues to be place after a block of code.
   */
  implicit def convertToClueful[T](fun: => T): Clueful[T] = new Clueful(fun)
}

/**
 * Companion object that facilitates the importing of `AppendedClues` members as 
 * an alternative to mixing it in. One use case is to import `AppendedClues`
 * members so you can use them in the Scala interpreter.
 */
object AppendedClues extends AppendedClues {
  private[scalatest] def appendClue(original: String, clue: String): String =
    clue.toString.headOption match {
      case Some(firstChar) if firstChar.isWhitespace ||
          firstChar == '.' || firstChar == ',' || firstChar == ';' => 
        original + clue
      case _ => original + " " + clue
   }
}

