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

import org.scalactic.Prettifier
import org.scalatest.Resources

/**
 * The result of a match operation, such as one performed by a <a href="Matcher.html">`Matcher`</a> or
 * <a href="BeMatcher.html">`BeMatcher`</a>, which 
 * contains one field that indicates whether the match succeeded, four fields that provide
 * raw failure messages to report under different circumstances, four fields providing
 * arguments used to construct the final failure messages using raw failure messages
 * and a <a href="../../Prettifier.html">`Prettifier`</a>.  Using the default constructor,
 * failure messages will be constructed lazily (when required).
 * 
 * A `MatchResult`'s `matches` field indicates whether a match succeeded. If it succeeded,
 * `matches` will be `true`.
 * There are four methods, `failureMessage`, `negatedfailureMessage`, `midSentenceFailureMessage`
 * and `negatedMidSentenceFailureMessage` that can be called to get final failure message strings, one of which will be
 * presented to the user in case of a match failure. If a match succeeds, none of these strings will be used, because no failure
 * message will be reported (''i.e.'', because there was no failure to report). If a match fails (`matches` is `false`),
 * the `failureMessage` (or `midSentenceFailure`&#8212;more on that below) will be reported to help the user understand what went wrong.
 * 
 *
 * ==Understanding `negatedFailureMessage`==
 *
 * The `negatedFailureMessage` exists so that it can become the `failureMessage` if the matcher is ''inverted'',
 * which happens, for instance, if it is passed to `not`. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val equalSeven = equal (7)
 * val notEqualSeven = not (equalSeven)
 * }}}
 *
 * The `Matcher[Int]` that results from passing 7 to `equal`, which is assigned to the `equalSeven`
 * variable, will compare `Int`s passed to its
 * `apply` method with 7. If 7 is passed, the `equalSeven` match will succeed. If anything other than 7 is passed, it
 * will fail. By contrast, the `notEqualSeven` matcher, which results from passing `equalSeven` to `not`, does
 * just the opposite. If 7 is passed, the `notEqualSeven` match will fail. If anything other than 7 is passed, it will succeed.
 * 
 *
 * For example, if 8 is passed, `equalSeven`'s `MatchResult` will contain:
 * 
 *
 * {{{ class="stExamples">
 *            expression: equalSeven(8)
 *               matches: false
 *        failureMessage: 8 did not equal 7
 * negatedFailureMessage: 8 equaled 7
 * }}}
 *
 * Although the `negatedFailureMessage` is nonsensical, it will not be reported to the user. Only the `failureMessage`,
 * which does actually explain what caused the failure, will be reported by the user. If you pass 8 to `notEqualSeven`'s `apply`
 * method, by contrast, the `failureMessage` and `negatedFailureMessage` will be:
 * 
 *
 * {{{ class="stExamples">
 *            expression: notEqualSeven(8)
 *               matches: true
 *        failureMessage: 8 equaled 7
 * negatedFailureMessage: 8 did not equal 7
 * }}}
 *
 * Note that the messages are swapped from the `equalSeven` messages. This swapping was effectively performed by the `not` matcher,
 * which in addition to swapping the `failureMessage` and `negatedFailureMessage`, also inverted the
 * `matches` value. Thus when you pass the same value to both `equalSeven` and `notEqualSeven` the `matches`
 * field of one `MatchResult` will be `true` and the other `false`. Because the
 * `matches` field of the `MatchResult` returned by `notEqualSeven(8)` is `true`,
 * the nonsensical `failureMessage`, "`8 equaled 7`", will ''not'' be reported to the user.
 * 
 *
 * If 7 is passed, by contrast, the `failureMessage` and `negatedFailureMessage` of `equalSeven`
 * will be:
 * 
 *
 * {{{ class="stExamples">
 *            expression: equalSeven(7)
 *               matches: true
 *        failureMessage: 7 did not equal 7
 * negatedFailureMessage: 7 equaled 7
 * }}}
 *
 * In this case `equalSeven`'s `failureMessage` is nonsensical, but because the match succeeded, the nonsensical message will
 * not be reported to the user.
 * If you pass 7 to `notEqualSeven`'s `apply`
 * method, you'll get:
 * 
 *
 * {{{ class="stExamples">
 *            expression: notEqualSeven(7)
 *               matches: false
 *        failureMessage: 7 equaled 7
 * negatedFailureMessage: 7 did not equal 7
 * }}}
 *
 * Again the messages are swapped from the `equalSeven` messages, but this time, the `failureMessage` makes sense
 * and explains what went wrong: the `notEqualSeven` match failed because the number passed did in fact equal 7. Since
 * the match failed, this failure message, "`7 equaled 7`", will be reported to the user.
 * 
 *
 * ==Understanding the "`midSentence`" messages==
 *
 * When a ScalaTest matcher expression that involves `and` or `or` fails, the failure message that
 * results is composed from the failure messages of the left and right matcher operatnds to `and` or `or`.
 * For example:
 * 
 *
 * {{{ class="stExamples">
 * 8 should (equal (7) or equal (9))
 * }}}
 *
 * This above expression would fail with the following failure message reported to the user:
 * 
 *
 * {{{ class="stExamples">
 * 8 did not equal 7, and 8 did not equal 9
 * }}}
 *
 * This works fine, but what if the failure messages being combined begin with a capital letter, such as:
 * 
 *
 * {{{ class="stExamples">
 * The name property did not equal "Ricky"
 * }}}
 *
 * A combination of two such failure messages might result in an abomination of English punctuation, such as:
 * 
 *
 * {{{ class="stExamples">
 * The name property did not equal "Ricky", and The name property did not equal "Bobby"
 * }}}
 *
 * Because ScalaTest is an internationalized application, taking all of its strings from a property file
 * enabling it to be localized, it isn't a good idea to force the first character to lower case. Besides,
 * it might actually represent a String value which should stay upper case. The `midSentenceFailureMessage`
 * exists for this situation. If the failure message is used at the beginning of the sentence, `failureMessage`
 * will be used. But if it appears mid-sentence, or at the end of the sentence, `midSentenceFailureMessage`
 * will be used. Given these failure message strings:
 * 
 *
 * {{{ class="stExamples">
 *            failureMessage: The name property did not equal "Bobby"
 * midSentenceFailureMessage: the name property did not equal "Bobby"
 * }}}
 *
 * The resulting failure of the `or` expression involving to matchers would make any English teacher proud:
 * 
 *
 * {{{ class="stExamples">
 * The name property did not equal "Ricky", and the name property did not equal "Bobby"
 * }}}
 *
 * @param matches indicates whether or not the matcher matched
 * @param rawFailureMessage raw failure message to report if a match fails
 * @param rawNegatedFailureMessage raw message with a meaning opposite to that of the failure message
 * @param rawMidSentenceFailureMessage raw failure message suitable for appearing mid-sentence
 * @param rawMidSentenceNegatedFailureMessage raw negated failure message suitable for appearing mid-sentence
 * @param failureMessageArgs arguments for constructing failure message to report if a match fails
 * @param negatedFailureMessageArgs arguments for constructing message with a meaning opposite to that of the failure message
 * @param midSentenceFailureMessageArgs arguments for constructing failure message suitable for appearing mid-sentence
 * @param midSentenceNegatedFailureMessageArgs arguments for constructing negated failure message suitable for appearing mid-sentence
 *
 * @author Bill Venners
 * @author Chee Seng
 */
final case class MatchResult(
  matches: Boolean,
  rawFailureMessage: String,
  rawNegatedFailureMessage: String,
  rawMidSentenceFailureMessage: String,
  rawMidSentenceNegatedFailureMessage: String,
  failureMessageArgs: IndexedSeq[Any],
  negatedFailureMessageArgs: IndexedSeq[Any],
  midSentenceFailureMessageArgs: IndexedSeq[Any],
  midSentenceNegatedFailureMessageArgs: IndexedSeq[Any]
) {

  /**
   * Constructs a new `MatchResult` with passed `matches`, `rawFailureMessage`, and
   * `rawNegativeFailureMessage` fields. The `rawMidSentenceFailureMessage` will return the same
   * string as `rawFailureMessage`, and the `rawMidSentenceNegatedFailureMessage` will return the
   * same string as `rawNegatedFailureMessage`.  `failureMessageArgs`, `negatedFailureMessageArgs`,
   * `midSentenceFailureMessageArgs`, `midSentenceNegatedFailureMessageArgs` will be `Vector.empty`
   * and `Prettifier.default` will be used.
   *
   * @param matches indicates whether or not the matcher matched
   * @param rawFailureMessage raw failure message to report if a match fails
   * @param rawNegatedFailureMessage raw message with a meaning opposite to that of the failure message
   */
  def this(matches: Boolean, rawFailureMessage: String, rawNegatedFailureMessage: String) =
    this(
      matches,
      rawFailureMessage,
      rawNegatedFailureMessage,
      rawFailureMessage,
      rawNegatedFailureMessage,
      Vector.empty,
      Vector.empty,
      Vector.empty,
      Vector.empty
    )

  /**
   * Construct failure message to report if a match fails, using `rawFailureMessage`, `failureMessageArgs` and `prettifier`
   *
   * @return failure message to report if a match fails
   */
  def failureMessage(implicit prettifier: Prettifier): String = if (failureMessageArgs.isEmpty) rawFailureMessage else makeString(rawFailureMessage, failureMessageArgs, prettifier)

  /**
   * Construct message with a meaning opposite to that of the failure message, using `rawNegatedFailureMessage`, `negatedFailureMessageArgs` and `prettifier`
   *
   * @return message with a meaning opposite to that of the failure message
   */
  def negatedFailureMessage(implicit prettifier: Prettifier): String = if (negatedFailureMessageArgs.isEmpty) rawNegatedFailureMessage else makeString(rawNegatedFailureMessage, negatedFailureMessageArgs, prettifier)

  /**
   * Construct failure message suitable for appearing mid-sentence, using `rawMidSentenceFailureMessage`, `midSentenceFailureMessageArgs` and `prettifier`
   *
   * @return failure message suitable for appearing mid-sentence
   */
  def midSentenceFailureMessage(implicit prettifier: Prettifier): String = if (midSentenceFailureMessageArgs.isEmpty) rawMidSentenceFailureMessage else makeString(rawMidSentenceFailureMessage, midSentenceFailureMessageArgs, prettifier)

  /**
   * Construct negated failure message suitable for appearing mid-sentence, using `rawMidSentenceNegatedFailureMessage`, `midSentenceNegatedFailureMessageArgs` and `prettifier`
   *
   * @return negated failure message suitable for appearing mid-sentence
   */
  def midSentenceNegatedFailureMessage(implicit prettifier: Prettifier): String = if (midSentenceNegatedFailureMessageArgs.isEmpty) rawMidSentenceNegatedFailureMessage else makeString(rawMidSentenceNegatedFailureMessage, midSentenceNegatedFailureMessageArgs, prettifier)

  /**
   * Get a negated version of this MatchResult, matches field will be negated and all messages field will be substituted with its counter-part.
   *
   * @return a negated version of this MatchResult
   */
  def negated: MatchResult = MatchResult(!matches, rawNegatedFailureMessage, rawFailureMessage, rawMidSentenceNegatedFailureMessage, rawMidSentenceFailureMessage, negatedFailureMessageArgs, failureMessageArgs, midSentenceNegatedFailureMessageArgs, midSentenceFailureMessageArgs)

  private def makeString(rawString: String, args: IndexedSeq[Any], prettifier: Prettifier): String =
    Resources.formatString(rawString, args.map(arg => if (arg.isInstanceOf[LazyMessage]) arg.asInstanceOf[LazyMessage].message(prettifier) else prettifier(arg)).toArray)
}

/**
 * Companion object for the `MatchResult` case class.
 *
 * @author Bill Venners
 */
object MatchResult {

  /**
   * Factory method that constructs a new `MatchResult` with passed `matches`, `failureMessage`, 
   * `negativeFailureMessage`, `midSentenceFailureMessage`, 
   * `midSentenceNegatedFailureMessage`, `failureMessageArgs`, and `negatedFailureMessageArgs` fields.
   * `failureMessageArgs`, and `negatedFailureMessageArgs` will be used in place of `midSentenceFailureMessageArgs`
   * and `midSentenceNegatedFailureMessageArgs`.
   *
   * @param matches indicates whether or not the matcher matched
   * @param rawFailureMessage raw failure message to report if a match fails
   * @param rawNegatedFailureMessage raw message with a meaning opposite to that of the failure message
   * @param rawMidSentenceFailureMessage raw failure message to report if a match fails
   * @param rawMidSentenceNegatedFailureMessage raw message with a meaning opposite to that of the failure message
   * @param failureMessageArgs arguments for constructing failure message to report if a match fails
   * @param negatedFailureMessageArgs arguments for constructing message with a meaning opposite to that of the failure message
   * @return a `MatchResult` instance
   */
  def apply(matches: Boolean, rawFailureMessage: String, rawNegatedFailureMessage: String, rawMidSentenceFailureMessage: String,
      rawMidSentenceNegatedFailureMessage: String, failureMessageArgs: IndexedSeq[Any], negatedFailureMessageArgs: IndexedSeq[Any]): MatchResult =
    new MatchResult(matches, rawFailureMessage, rawNegatedFailureMessage, rawMidSentenceFailureMessage, rawMidSentenceNegatedFailureMessage, failureMessageArgs, negatedFailureMessageArgs, failureMessageArgs, negatedFailureMessageArgs)

  /**
   * Factory method that constructs a new `MatchResult` with passed `matches`, `rawFailureMessage`,
   * `rawNegativeFailureMessage`, `rawMidSentenceFailureMessage`, and
   * `rawMidSentenceNegatedFailureMessage` fields.  All argument fields will have `Vector.empty` values.
   * This is suitable to create MatchResult with eager error messages, and its mid-sentence messages need to be different.
   *
   * @param matches indicates whether or not the matcher matched
   * @param rawFailureMessage raw failure message to report if a match fails
   * @param rawNegatedFailureMessage raw message with a meaning opposite to that of the failure message
   * @param rawMidSentenceFailureMessage raw failure message to report if a match fails
   * @param rawMidSentenceNegatedFailureMessage raw message with a meaning opposite to that of the failure message
   * @return a `MatchResult` instance
   */
  def apply(matches: Boolean, rawFailureMessage: String, rawNegatedFailureMessage: String, rawMidSentenceFailureMessage: String,
      rawMidSentenceNegatedFailureMessage: String): MatchResult =
    new MatchResult(matches, rawFailureMessage, rawNegatedFailureMessage, rawMidSentenceFailureMessage, rawMidSentenceNegatedFailureMessage, Vector.empty, Vector.empty, Vector.empty, Vector.empty)

  /**
   * Factory method that constructs a new `MatchResult` with passed `matches`, `rawFailureMessage`, and
   * `rawNegativeFailureMessage` fields. The `rawMidSentenceFailureMessage` will return the same
   * string as `rawFailureMessage`, and the `rawMidSentenceNegatedFailureMessage` will return the
   * same string as `rawNegatedFailureMessage`.  All argument fields will have `Vector.empty` values.
   * This is suitable to create MatchResult with eager error messages that have same mid-sentence messages.
   *
   * @param matches indicates whether or not the matcher matched
   * @param rawFailureMessage raw failure message to report if a match fails
   * @param rawNegatedFailureMessage raw message with a meaning opposite to that of the failure message
   * @return a `MatchResult` instance
   */
  def apply(matches: Boolean, rawFailureMessage: String, rawNegatedFailureMessage: String): MatchResult =
    new MatchResult(matches, rawFailureMessage, rawNegatedFailureMessage, rawFailureMessage, rawNegatedFailureMessage, Vector.empty, Vector.empty, Vector.empty, Vector.empty)

  /**
   * Factory method that constructs a new `MatchResult` with passed `matches`, `rawFailureMessage`,
   * `rawNegativeFailureMessage` and `args` fields.  The `rawMidSentenceFailureMessage` will return the same
   * string as `rawFailureMessage`, and the `rawMidSentenceNegatedFailureMessage` will return the
   * same string as `rawNegatedFailureMessage`.  All argument fields will use `args` as arguments.
   * This is suitable to create MatchResult with lazy error messages that have same mid-sentence messages and arguments.
   *
   * @param matches indicates whether or not the matcher matched
   * @param rawFailureMessage raw failure message to report if a match fails
   * @param rawNegatedFailureMessage raw message with a meaning opposite to that of the failure message
   * @param args arguments for error messages construction
   * @return a `MatchResult` instance
   */
  def apply(matches: Boolean, rawFailureMessage: String, rawNegatedFailureMessage: String, args: IndexedSeq[Any]) =
    new MatchResult(
      matches,
      rawFailureMessage,
      rawNegatedFailureMessage,
      rawFailureMessage,
      rawNegatedFailureMessage,
      args,
      args,
      args,
      args
    )

  /**
   * Factory method that constructs a new `MatchResult` with passed `matches`, `rawFailureMessage`,
   * `rawNegativeFailureMessage`, `failureMessageArgs` and `negatedFailureMessageArgs` fields.
   * The `rawMidSentenceFailureMessage` will return the same string as `rawFailureMessage`, and the
   * `rawMidSentenceNegatedFailureMessage` will return the same string as `rawNegatedFailureMessage`.
   * The `midSentenceFailureMessageArgs` will return the same as `failureMessageArgs`, and the
   * `midSentenceNegatedFailureMessageArgs` will return the same as `negatedFailureMessageArgs`.
   * This is suitable to create MatchResult with lazy error messages that have same mid-sentence and use different arguments for
   * negated messages.
   *
   * @param matches indicates whether or not the matcher matched
   * @param rawFailureMessage raw failure message to report if a match fails
   * @param rawNegatedFailureMessage raw message with a meaning opposite to that of the failure message
   * @param failureMessageArgs arguments for constructing failure message to report if a match fails
   * @param negatedFailureMessageArgs arguments for constructing message with a meaning opposite to that of the failure message
   * @return a `MatchResult` instance
   */
  def apply(matches: Boolean, rawFailureMessage: String, rawNegatedFailureMessage: String, failureMessageArgs: IndexedSeq[Any], negatedFailureMessageArgs: IndexedSeq[Any]) =
    new MatchResult(
      matches,
      rawFailureMessage,
      rawNegatedFailureMessage,
      rawFailureMessage,
      rawNegatedFailureMessage,
      failureMessageArgs,
      negatedFailureMessageArgs,
      failureMessageArgs,
      negatedFailureMessageArgs
    )
}

