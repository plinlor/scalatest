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
import Requirements._

import scala.reflect.ClassTag
import Assertions.NormalResult
import DefaultEquality.areEqualComparingArraysStructurally
import exceptions.StackDepthException
import exceptions.StackDepthException.toExceptionFunction
import exceptions.TestFailedException
import exceptions.TestPendingException
import org.scalactic.anyvals.NonEmptyArray

/**
 * Trait that contains ScalaTest's basic assertion methods.
 *
 * You can use the assertions provided by this trait in any ScalaTest `Suite`,
 * because <a href="Suite.html">`Suite`</a>
 * mixes in this trait. This trait is designed to be used independently of anything else in ScalaTest, though, so you
 * can mix it into anything. (You can alternatively import the methods defined in this trait. For details, see the documentation
 * for the <a href="Assertions$.html">`Assertions` companion object</a>.
 * 
 *
 * In any Scala program, you can write assertions by invoking `assert` and passing in a `Boolean` expression,
 * such as:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val left = 2
 * val right = 1
 * assert(left == right)
 * }}}
 *
 * If the passed expression is `true`, `assert` will return normally. If `false`,
 * Scala's `assert` will complete abruptly with an `AssertionError`. This behavior is provided by
 * the `assert` method defined in object `Predef`, whose members are implicitly imported into every
 * Scala source file. This `Assertions` trait defines another `assert` method that hides the
 * one in `Predef`. It behaves the same, except that if `false` is passed it throws
 * <a href="exceptions/TestFailedException.html">`TestFailedException`</a> instead of `AssertionError`. 
 * Why? Because unlike `AssertionError`, `TestFailedException` carries information about exactly
 * which item in the stack trace represents
 * the line of test code that failed, which can help users more quickly find an offending line of code in a failing test.
 * In addition, ScalaTest's `assert` provides better error messages than Scala's `assert`.
 *
 * If you pass the previous `Boolean` expression, `left == right` to `assert` in a ScalaTest test,
 * a failure will be reported that, because `assert` is implemented as a macro,
 * includes reporting the left and right values.
 * For example, given the same code as above but using ScalaTest assertions:
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.Assertions._
 * val left = 2
 * val right = 1
 * assert(left == right)
 * }}}
 *
 * The detail message in the thrown `TestFailedException` from this `assert`
 * will be: "2 did not equal 1".
 * 
 *
 * ScalaTest's `assert` macro works by recognizing patterns in the AST of the expression passed to `assert` and,
 * for a finite set of common expressions, giving an error message that an equivalent ScalaTest matcher
 * expression would give. Here are some examples, where `a` is 1, `b` is 2, `c` is 3, `d`
 * is 4, `xs` is `List(a, b, c)`, and `num` is 1.0:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assert(a == b || c &gt;= d)
 * // Error message: 1 did not equal 2, and 3 was not greater than or equal to 4
 *
 * assert(xs.exists(_ == 4))
 * // Error message: List(1, 2, 3) did not contain 4
 *
 * assert("hello".startsWith("h") &amp;&amp; "goodbye".endsWith("y"))
 * // Error message: "hello" started with "h", but "goodbye" did not end with "y"
 *
 * assert(num.isInstanceOf[Int])
 * // Error message: 1.0 was not instance of scala.Int
 *
 * assert(Some(2).isEmpty)
 * // Error message: Some(2) was not empty
 * }}}
 * 
 * For expressions that are not recognized, the macro currently prints out a string
 * representation of the (desugared) AST and adds `"was false"`. Here are some examples of
 * error messages for unrecognized expressions:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assert(None.isDefined)
 * // Error message: scala.None.isDefined was false
 *
 * assert(xs.exists(i =&gt; i &gt; 10))
 * // Error message: xs.exists(((i: Int) =&gt; i.&gt;(10))) was false
 * }}}
 * 
 * You can augment the standard error message by providing a `String` as a second argument
 * to `assert`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val attempted = 2
 * assert(attempted == 1, "Execution was attempted " + left + " times instead of 1 time")
 * }}}
 *
 * Using this form of `assert`, the failure report will be more specific to your problem domain, thereby
 * helping you debug the problem. This `Assertions` trait also mixes in the
 * <a href="../scalactic/TripleEquals.html">`TripleEquals`</a>, which gives you a `===` operator
 * that allows you to customize <a href="../scalactic/Equality.html">`Equality`</a>, perform equality checks with numeric
 * <a href="../scalactic/Tolerance.html">`Tolerance`</a>, and enforce type constraints at compile time with
 * sibling traits <a href="TypeCheckedTripleEquals.html">`TypeCheckedTripleEquals`</a> and
 * <a href="ConversionCheckedTripleEquals.html">`ConversionCheckedTripleEquals`</a>.
 * 
 *
 * <a name="expectedResults"></a>
 * ==Expected results==
 *
 * Although the `assert` macro provides a natural, readable extension to Scala's `assert` mechanism that
 * provides good error messages, as the operands become lengthy, the code becomes less readable. In addition, the error messages
 * generated for `==` and `===` comparisons
 * don't distinguish between actual and expected values. The operands are just called `left` and `right`,
 * because if one were named `expected` and the other `actual`, it would be difficult for people to
 * remember which was which. To help with these limitations of assertions, `Suite` includes a method called `assertResult` that
 * can be used as an alternative to `assert`. To use `assertResult`, you place
 * the expected value in parentheses after `assertResult`, followed by curly braces containing code
 * that should result in the expected value. For example:
 *
 * {{{  <!-- class="stHighlight" -->
 * val a = 5
 * val b = 2
 * assertResult(2) {
 *   a - b
 * }
 * }}}
 *
 * In this case, the expected value is `2`, and the code being tested is `a - b`. This assertion will fail, and
 * the detail message in the `TestFailedException` will read, "Expected 2, but got 3."
 * 
 *
 * <a name="forcingFailures"></a>
 * ==Forcing failures==
 *
 * If you just need the test to fail, you can write:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * fail()
 * }}}
 *
 * Or, if you want the test to fail with a message, write:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * fail("I've got a bad feeling about this")
 * }}}
 *
 * <a name="achievingSuccess"></a>
 * ==Achieving success==
 *
 * In async style tests, you must end your test body with either `Future[Assertion]` or
 * `Assertion`. ScalaTest's assertions (including matcher expressions) have result type
 * `Assertion`, so ending with an assertion will satisfy the compiler.
 * If a test body or function body passed to `Future.map` does
 * ''not'' end with type `Assertion`, however, you can fix the type error by placing
 * `succeed` at the end of the
 * test or function body:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * succeed // Has type Assertion
 * }}}
 *
 * <a name="interceptedExceptions"></a>
 * <a name="expectedExceptions"></a>
 * ==Expected exceptions==
 *
 * Sometimes you need to test whether a method throws an expected exception under certain circumstances, such
 * as when invalid arguments are passed to the method. You can do this in the JUnit 3 style, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val s = "hi"
 * try {
 *   s.charAt(-1)
 *   fail()
 * }
 * catch {
 *   case _: IndexOutOfBoundsException =&gt; // Expected, so continue
 * }
 * }}}
 *
 * If `charAt` throws `IndexOutOfBoundsException` as expected, control will transfer
 * to the catch case, which does nothing. If, however, `charAt` fails to throw an exception,
 * the next statement, `fail()`, will be run. The `fail` method always completes abruptly with
 * a `TestFailedException`, thereby signaling a failed test.
 * 
 *
 * To make this common use case easier to express and read, ScalaTest provides two methods:
 * `assertThrows` and `intercept`.
 * Here's how you use `assertThrows`:
 * 
 *
 * <a name="assertThrowsMethod"></a>
 * {{{  <!-- class="stHighlight" -->
 * val s = "hi"
 * assertThrows[IndexOutOfBoundsException] { // Result type: Assertion
 *   s.charAt(-1)
 * }
 * }}}
 *
 * This code behaves much like the previous example. If `charAt` throws an instance of `IndexOutOfBoundsException`,
 * `assertThrows` will return `Succeeded`. But if `charAt` completes normally, or throws a different
 * exception, `assertThrows` will complete abruptly with a `TestFailedException`.
 * 
 *
 * The `intercept` method behaves the same as `assertThrows`, except that instead of returning `Succeeded`,
 * `intercept` returns the caught exception so that you can inspect it further if you wish. For example, you may need
 * to ensure that data contained inside the exception have expected values. Here's an example:
 * 
 *
 * <a name="interceptMethod"></a>
 * {{{  <!-- class="stHighlight" -->
 * val s = "hi"
 * val caught =
 *   intercept[IndexOutOfBoundsException] { // Result type: IndexOutOfBoundsException
 *     s.charAt(-1)
 *   }
 * assert(caught.getMessage.indexOf("-1") != -1)
 * }}}
 *
 * <a name="checkingThatCodeDoesNotCompile"></a>
 * ==Checking that a snippet of code does or does not compile==
 * 
 * Often when creating libraries you may wish to ensure that certain arrangements of code that
 * represent potential &ldquo;user errors&rdquo; do not compile, so that your library is more error resistant.
 * ScalaTest's `Assertions` trait includes the following syntax for that purpose:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assertDoesNotCompile("val a: String = 1")
 * }}}
 *
 * If you want to ensure that a snippet of code does not compile because of a type error (as opposed
 * to a syntax error), use:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assertTypeError("val a: String = 1")
 * }}}
 *
 * Note that the `assertTypeError` call will only succeed if the given snippet of code does not
 * compile because of a type error. A syntax error will still result on a thrown `TestFailedException`.
 * 
 *
 * If you want to state that a snippet of code ''does'' compile, you can make that
 * more obvious with:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assertCompiles("val a: Int = 1")
 * }}}
 *
 * Although the previous three constructs are implemented with macros that determine at compile time whether
 * the snippet of code represented by the string does or does not compile, errors 
 * are reported as test failures at runtime.
 * 
 *
 * <a name="assumptions"></a>
 * ==Assumptions==
 *
 * Trait `Assertions` also provides methods that allow you to ''cancel'' a test.
 * You would cancel a test if a resource required by the test was unavailable. For example, if a test
 * requires an external database to be online, and it isn't, the test could be canceled to indicate
 * it was unable to run because of the missing database. Such a test ''assumes'' a database is
 * available, and you can use the `assume` method to indicate this at the beginning of
 * the test, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assume(database.isAvailable)
 * }}}
 *
 * For each overloaded `assert` method, trait `Assertions` provides an
 * overloaded `assume` method with an identical signature and behavior, except the
 * `assume` methods throw <a href="exceptions/TestCanceledException.html">`TestCanceledException`</a> whereas the
 * `assert` methods throw `TestFailedException`. As with `assert`,
 * `assume` hides a Scala method in `Predef` that performs a similar
 * function, but throws `AssertionError`. And just as you can with `assert`,
 * you will get an error message extracted by a macro from the AST passed to `assume`, and can
 * optionally provide a clue string to augment this error message. Here are some examples:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assume(database.isAvailable, "The database was down again")
 * assume(database.getAllUsers.count === 9)
 * }}}
 *
 * <a name="forcingCancelations"></a>
 * ==Forcing cancelations==
 *
 * For each overloaded `fail` method, there's a corresponding `cancel` method
 * with an identical signature and behavior, except the `cancel` methods throw
 * `TestCanceledException` whereas the `fail` methods throw
 * `TestFailedException`. Thus if you just need to cancel a test, you can write:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * cancel()
 * }}}
 *
 * If you want to cancel the test with a message, just place the message in the parentheses:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * cancel("Can't run the test because no internet connection was found")
 * }}}
 *
 * <a name="gettingAClue"></a>
 * ==Getting a clue==
 *
 * If you want more information that is provided by default by the methods if this trait,
 * you can supply a "clue" string in one of several ways.
 * The extra information (or "clues") you provide will
 * be included in the detail message of the thrown exception. Both
 * `assert` and `assertResult` provide a way for a clue to be
 * included directly, `intercept` does not.
 * Here's an example of clues provided directly in `assert`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assert(1 + 1 === 3, "this is a clue")
 * }}}
 *
 * and in `assertResult`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assertResult(3, "this is a clue") { 1 + 1 }
 * }}}
 *
 * The exceptions thrown by the previous two statements will include the clue
 * string, `"this is a clue"`, in the exception's detail message.
 * To get the same clue in the detail message of an exception thrown
 * by a failed `intercept` call requires using `withClue`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * withClue("this is a clue") {
 *   intercept[IndexOutOfBoundsException] {
 *     "hi".charAt(-1)
 *   }
 * }
 * }}}
 *
 * The `withClue` method will only prepend the clue string to the detail
 * message of exception types that mix in the `ModifiableMessage` trait.
 * See the documentation for <a href="ModifiableMessage.html">`ModifiableMessage`</a> for more information.
 * If you wish to place a clue string after a block of code, see the documentation for
 * <a href="AppendedClues.html">`AppendedClues`</a>.
 * 
 *
 * ''Note: ScalaTest's `assertTypeError` construct is in part inspired by the `illTyped` macro
 * of <a href="https://github.com/milessabin/shapeless" target="_blank">shapeless</a>.''
 * 
 *
 * @author Bill Venners
 */
trait Assertions extends TripleEquals  {

  //implicit val prettifier = Prettifier.default

  import language.experimental.macros

  /**
   * Assert that a boolean condition is true.
   * If the condition is `true`, this method returns normally.
   * Else, it throws `TestFailedException`.
   *
   * This method is implemented in terms of a Scala macro that will generate a more helpful error message
   * for expressions of this form:
   * 
   *
   * <ul>
   * <li>assert(a == b)</li>
   * <li>assert(a != b)</li>
   * <li>assert(a === b)</li>
   * <li>assert(a !== b)</li>
   * <li>assert(a &gt; b)</li>
   * <li>assert(a &gt;= b)</li>
   * <li>assert(a &lt; b)</li>
   * <li>assert(a &lt;= b)</li>
   * <li>assert(a startsWith "prefix")</li>
   * <li>assert(a endsWith "postfix")</li>
   * <li>assert(a contains "something")</li>
   * <li>assert(a eq b)</li>
   * <li>assert(a ne b)</li>
   * <li>assert(a &gt; 0 &amp;&amp; b &gt; 5)</li>
   * <li>assert(a &gt; 0 || b &gt; 5)</li>
   * <li>assert(a.isEmpty)</li>
   * <li>assert(!a.isEmpty)</li>
   * <li>assert(a.isInstanceOf[String])</li>
   * <li>assert(a.length == 8)</li>
   * <li>assert(a.size == 8)</li>
   * <li>assert(a.exists(_ == 8))</li>
   * </ul>
   *
   * At this time, any other form of expression will get a `TestFailedException` with message saying the given
   * expression was false.  In the future, we will enhance this macro to give helpful error messages in more situations.
   * In ScalaTest 2.0, however, this behavior was sufficient to allow the `===` that returns `Boolean`
   * to be the default in tests. This makes `===` consistent between tests and production
   * code.
   * 
   *
   * @param condition the boolean condition to assert
   * @throws TestFailedException if the condition is `false`.
   */
  def assert(condition: Boolean)(implicit prettifier: Prettifier, pos: source.Position): Assertion = macro AssertionsMacro.assert

  private[scalatest] def newAssertionFailedException(optionalMessage: Option[String], optionalCause: Option[Throwable], pos: source.Position): Throwable =
    new exceptions.TestFailedException(toExceptionFunction(optionalMessage), optionalCause, pos)

  private[scalatest] def newTestCanceledException(optionalMessage: Option[String], optionalCause: Option[Throwable], pos: source.Position): Throwable =
    new exceptions.TestCanceledException(toExceptionFunction(optionalMessage), optionalCause, pos, None)

  /**
   * Assert that a boolean condition, described in `String`
   * `message`, is true.
   * If the condition is `true`, this method returns normally.
   * Else, it throws `TestFailedException` with a helpful error message
   * appended with the `String` obtained by invoking `toString` on the
   * specified `clue` as the exception's detail message.
   *
   * This method is implemented in terms of a Scala macro that will generate a more helpful error message
   * for expressions of this form:
   * 
   *
   * <ul>
   * <li>assert(a == b, "a good clue")</li>
   * <li>assert(a != b, "a good clue")</li>
   * <li>assert(a === b, "a good clue")</li>
   * <li>assert(a !== b, "a good clue")</li>
   * <li>assert(a &gt; b, "a good clue")</li>
   * <li>assert(a &gt;= b, "a good clue")</li>
   * <li>assert(a &lt; b, "a good clue")</li>
   * <li>assert(a &lt;= b, "a good clue")</li>
   * <li>assert(a startsWith "prefix", "a good clue")</li>
   * <li>assert(a endsWith "postfix", "a good clue")</li>
   * <li>assert(a contains "something", "a good clue")</li>
   * <li>assert(a eq b, "a good clue")</li>
   * <li>assert(a ne b, "a good clue")</li>
   * <li>assert(a &gt; 0 &amp;&amp; b &gt; 5, "a good clue")</li>
   * <li>assert(a &gt; 0 || b &gt; 5, "a good clue")</li>
   * <li>assert(a.isEmpty, "a good clue")</li>
   * <li>assert(!a.isEmpty, "a good clue")</li>
   * <li>assert(a.isInstanceOf[String], "a good clue")</li>
   * <li>assert(a.length == 8, "a good clue")</li>
   * <li>assert(a.size == 8, "a good clue")</li>
   * <li>assert(a.exists(_ == 8), "a good clue")</li>
   * </ul>
   *
   * At this time, any other form of expression will just get a `TestFailedException` with message saying the given
   * expression was false.  In the future, we will enhance this macro to give helpful error messages in more situations.
   * In ScalaTest 2.0, however, this behavior was sufficient to allow the `===` that returns `Boolean`
   * to be the default in tests. This makes `===` consistent between tests and production
   * code.
   * 
   *
   * @param condition the boolean condition to assert
   * @param clue An objects whose `toString` method returns a message to include in a failure report.
   * @throws TestFailedException if the condition is `false`.
   * @throws NullArgumentException if `message` is `null`.
   */
  def assert(condition: Boolean, clue: Any)(implicit prettifier: Prettifier, pos: source.Position): Assertion = macro AssertionsMacro.assertWithClue

  /**
   * Assume that a boolean condition is true.
   * If the condition is `true`, this method returns normally.
   * Else, it throws `TestCanceledException`.
   *
   * This method is implemented in terms of a Scala macro that will generate a more helpful error message
   * for expressions of this form:
   * 
   *
   * <ul>
   * <li>assume(a == b)</li>
   * <li>assume(a != b)</li>
   * <li>assume(a === b)</li>
   * <li>assume(a !== b)</li>
   * <li>assume(a &gt; b)</li>
   * <li>assume(a &gt;= b)</li>
   * <li>assume(a &lt; b)</li>
   * <li>assume(a &lt;= b)</li>
   * <li>assume(a startsWith "prefix")</li>
   * <li>assume(a endsWith "postfix")</li>
   * <li>assume(a contains "something")</li>
   * <li>assume(a eq b)</li>
   * <li>assume(a ne b)</li>
   * <li>assume(a &gt; 0 &amp;&amp; b &gt; 5)</li>
   * <li>assume(a &gt; 0 || b &gt; 5)</li>
   * <li>assume(a.isEmpty)</li>
   * <li>assume(!a.isEmpty)</li>
   * <li>assume(a.isInstanceOf[String])</li>
   * <li>assume(a.length == 8)</li>
   * <li>assume(a.size == 8)</li>
   * <li>assume(a.exists(_ == 8))</li>
   * </ul>
   *
   * At this time, any other form of expression will just get a `TestCanceledException` with message saying the given
   * expression was false.  In the future, we will enhance this macro to give helpful error messages in more situations.
   * In ScalaTest 2.0, however, this behavior was sufficient to allow the `===` that returns `Boolean`
   * to be the default in tests. This makes `===` consistent between tests and production
   * code.
   * 
   *
   * @param condition the boolean condition to assume
   * @throws TestCanceledException if the condition is `false`.
   */
  def assume(condition: Boolean)(implicit prettifier: Prettifier, pos: source.Position): Assertion = macro AssertionsMacro.assume

  /**
   * Assume that a boolean condition, described in `String`
   * `message`, is true.
   * If the condition is `true`, this method returns normally.
   * Else, it throws `TestCanceledException` with a helpful error message
   * appended with `String` obtained by invoking `toString` on the
   * specified `clue` as the exception's detail message.
   *
   * This method is implemented in terms of a Scala macro that will generate a more helpful error message
   * for expressions of this form:
   * 
   *
   * <ul>
   * <li>assume(a == b, "a good clue")</li>
   * <li>assume(a != b, "a good clue")</li>
   * <li>assume(a === b, "a good clue")</li>
   * <li>assume(a !== b, "a good clue")</li>
   * <li>assume(a &gt; b, "a good clue")</li>
   * <li>assume(a &gt;= b, "a good clue")</li>
   * <li>assume(a &lt; b, "a good clue")</li>
   * <li>assume(a &lt;= b, "a good clue")</li>
   * <li>assume(a startsWith "prefix", "a good clue")</li>
   * <li>assume(a endsWith "postfix", "a good clue")</li>
   * <li>assume(a contains "something", "a good clue")</li>
   * <li>assume(a eq b, "a good clue")</li>
   * <li>assume(a ne b, "a good clue")</li>
   * <li>assume(a &gt; 0 &amp;&amp; b &gt; 5, "a good clue")</li>
   * <li>assume(a &gt; 0 || b &gt; 5, "a good clue")</li>
   * <li>assume(a.isEmpty, "a good clue")</li>
   * <li>assume(!a.isEmpty, "a good clue")</li>
   * <li>assume(a.isInstanceOf[String], "a good clue")</li>
   * <li>assume(a.length == 8, "a good clue")</li>
   * <li>assume(a.size == 8, "a good clue")</li>
   * <li>assume(a.exists(_ == 8), "a good clue")</li>
   * </ul>
   *
   * At this time, any other form of expression will just get a `TestCanceledException` with message saying the given
   * expression was false.  In the future, we will enhance this macro to give helpful error messages in more situations.
   * In ScalaTest 2.0, however, this behavior was sufficient to allow the `===` that returns `Boolean`
   * to be the default in tests. This makes `===` consistent between tests and production
   * code.
   * 
   *
   * @param condition the boolean condition to assume
   * @param clue An objects whose `toString` method returns a message to include in a failure report.
   * @throws TestCanceledException if the condition is `false`.
   * @throws NullArgumentException if `message` is `null`.
   */
  def assume(condition: Boolean, clue: Any)(implicit prettifier: Prettifier, pos: source.Position): Assertion = macro AssertionsMacro.assumeWithClue

  /**
   * Asserts that a given string snippet of code does not pass the Scala type checker, failing if the given
   * snippet does not pass the Scala parser.
   *
   * Often when creating libraries you may wish to ensure that certain arrangements of code that
   * represent potential &ldquo;user errors&rdquo; do not compile, so that your library is more error resistant.
   * ScalaTest's `Assertions` trait includes the following syntax for that purpose:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * assertTypeError("val a: String = 1")
   * }}}
   *
   * Although `assertTypeError` is implemented with a macro that determines at compile time whether
   * the snippet of code represented by the passed string type checks, errors (''i.e.'', 
   * snippets of code that ''do'' type check) are reported as test failures at runtime.
   * 
   *
   * Note that the difference between `assertTypeError` and `assertDoesNotCompile` is
   * that `assertDoesNotCompile` will succeed if the given code does not compile for any reason,
   * whereas `assertTypeError` will only succeed if the given code does not compile because of
   * a type error. If the given code does not compile because of a syntax error, for example, `assertDoesNotCompile`
   * will return normally but `assertTypeError` will throw a `TestFailedException`.
   * 
   *
   * @param code the snippet of code that should not type check
   */
  def assertTypeError(code: String)(implicit pos: source.Position): Assertion = macro CompileMacro.assertTypeErrorImpl

  /**
   * Asserts that a given string snippet of code does not pass either the Scala parser or type checker.
   *
   * Often when creating libraries you may wish to ensure that certain arrangements of code that
   * represent potential &ldquo;user errors&rdquo; do not compile, so that your library is more error resistant.
   * ScalaTest's `Assertions` trait includes the following syntax for that purpose:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * assertDoesNotCompile("val a: String = \"a string")
   * }}}
   *
   * Although `assertDoesNotCompile` is implemented with a macro that determines at compile time whether
   * the snippet of code represented by the passed string doesn't compile, errors (''i.e.'',
   * snippets of code that ''do'' compile) are reported as test failures at runtime.
   * 
   *
   * Note that the difference between `assertTypeError` and `assertDoesNotCompile` is
   * that `assertDoesNotCompile` will succeed if the given code does not compile for any reason,
   * whereas `assertTypeError` will only succeed if the given code does not compile because of
   * a type error. If the given code does not compile because of a syntax error, for example, `assertDoesNotCompile`
   * will return normally but `assertTypeError` will throw a `TestFailedException`.
   * 
   *
   * @param code the snippet of code that should not type check
   */
  def assertDoesNotCompile(code: String)(implicit pos: source.Position): Assertion = macro CompileMacro.assertDoesNotCompileImpl

  /**
   * Asserts that a given string snippet of code passes both the Scala parser and type checker.
   *
   * You can use this to make sure a snippet of code compiles:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * assertCompiles("val a: Int = 1")
   * }}}
   *
   * Although `assertCompiles` is implemented with a macro that determines at compile time whether
   * the snippet of code represented by the passed string compiles, errors (''i.e.'',
   * snippets of code that ''do not'' compile) are reported as test failures at runtime.
   * 
   *
   * @param code the snippet of code that should compile
   */
  def assertCompiles(code: String)(implicit pos: source.Position): Assertion = macro CompileMacro.assertCompilesImpl

  /**
   * Intercept and return an exception that's expected to
   * be thrown by the passed function value. The thrown exception must be an instance of the
   * type specified by the type parameter of this method. This method invokes the passed
   * function. If the function throws an exception that's an instance of the specified type,
   * this method returns that exception. Else, whether the passed function returns normally
   * or completes abruptly with a different exception, this method throws `TestFailedException`.
   *
   * Note that the type specified as this method's type parameter may represent any subtype of
   * `AnyRef`, not just `Throwable` or one of its subclasses. In
   * Scala, exceptions can be caught based on traits they implement, so it may at times make sense
   * to specify a trait that the intercepted exception's class must mix in. If a class instance is
   * passed for a type that could not possibly be used to catch an exception (such as `String`,
   * for example), this method will complete abruptly with a `TestFailedException`.
   * 
   *
   * Also note that the difference between this method and `assertThrows` is that this method
   * returns the expected exception, so it lets you perform further assertions on
   * that exception. By contrast, the `assertThrows` method returns `Succeeded`, which means it can
   * serve as the last statement in an async- or safe-style suite. `assertThrows` also indicates to the reader
   * of the code that nothing further is expected about the thrown exception other than its type.
   * The recommended usage is to use `assertThrows` by default, `intercept` only when you
   * need to inspect the caught exception further.
   * 
   *
   * @param f the function value that should throw the expected exception
   * @param classTag an implicit `ClassTag` representing the type of the specified
   * type parameter.
   * @return the intercepted exception, if it is of the expected type
   * @throws TestFailedException if the passed function does not complete abruptly with an exception
   *    that's an instance of the specified type.
   */
  def intercept[T <: AnyRef](f: => Any)(implicit classTag: ClassTag[T], pos: source.Position): T = {
    val clazz = classTag.runtimeClass
    val caught = try {
      f
      None
    }
    catch {
      case u: Throwable => {
        if (!clazz.isAssignableFrom(u.getClass)) {
          val s = Resources.wrongException(clazz.getName, u.getClass.getName)
          throw newAssertionFailedException(Some(s), Some(u), pos)
        }
        else {
          Some(u)
        }
      }
    }
    caught match {
      case None =>
        val message = Resources.exceptionExpected(clazz.getName)
        throw newAssertionFailedException(Some(message), None, pos)
      case Some(e) => e.asInstanceOf[T] // I know this cast will succeed, becuase isAssignableFrom succeeded above
    }
  }

  /**
   * Ensure that an expected exception is thrown by the passed function value. The thrown exception must be an instance of the
   * type specified by the type parameter of this method. This method invokes the passed
   * function. If the function throws an exception that's an instance of the specified type,
   * this method returns `Succeeded`. Else, whether the passed function returns normally
   * or completes abruptly with a different exception, this method throws `TestFailedException`.
   *
   * Note that the type specified as this method's type parameter may represent any subtype of
   * `AnyRef`, not just `Throwable` or one of its subclasses. In
   * Scala, exceptions can be caught based on traits they implement, so it may at times make sense
   * to specify a trait that the intercepted exception's class must mix in. If a class instance is
   * passed for a type that could not possibly be used to catch an exception (such as `String`,
   * for example), this method will complete abruptly with a `TestFailedException`.
   * 
   *
   * Also note that the difference between this method and `intercept` is that this method
   * does not return the expected exception, so it does not let you perform further assertions on
   * that exception. Instead, this method returns `Succeeded`, which means it can
   * serve as the last statement in an async- or safe-style suite. It also indicates to the reader
   * of the code that nothing further is expected about the thrown exception other than its type.
   * The recommended usage is to use `assertThrows` by default, `intercept` only when you
   * need to inspect the caught exception further.
   * 
   *
   * @param f the function value that should throw the expected exception
   * @param classTag an implicit `ClassTag` representing the type of the specified
   * type parameter.
   * @return the `Succeeded` singleton, if an exception of the expected type is thrown
   * @throws TestFailedException if the passed function does not complete abruptly with an exception
   *    that's an instance of the specified type.
   */
  def assertThrows[T <: AnyRef](f: => Any)(implicit classTag: ClassTag[T], pos: source.Position): Assertion = {
    val clazz = classTag.runtimeClass
    val threwExpectedException =
      try {
        f
        false
      }
      catch {
          case u: Throwable => {
          if (!clazz.isAssignableFrom(u.getClass)) {
            val s = Resources.wrongException(clazz.getName, u.getClass.getName)
            throw newAssertionFailedException(Some(s), Some(u), pos)
          }
          else true
        }
      }
    if (threwExpectedException) {
      Succeeded
    }
    else {
        val message = Resources.exceptionExpected(clazz.getName)
        throw newAssertionFailedException(Some(message), None, pos)
    }
  }

  /**
   * Trap and return any thrown exception that would normally cause a ScalaTest test to fail, or create and return a new `RuntimeException`
   * indicating no exception is thrown.
   *
   * This method is intended to be used in the Scala interpreter to eliminate large stack traces when trying out ScalaTest assertions and
   * matcher expressions. It is not intended to be used in regular test code. If you want to ensure that a bit of code throws an expected
   * exception, use `intercept`, not `trap`. Here's an example interpreter session without `trap`:
   * 
   *
   * {{{  <!-- class="stREPL" -->
   * scala&gt; import org.scalatest._
   * import org.scalatest._
   *
   * scala&gt; import Matchers._
   * import Matchers._
   *
   * scala&gt; val x = 12
   * a: Int = 12
   *
   * scala&gt; x shouldEqual 13
   * org.scalatest.exceptions.TestFailedException: 12 did not equal 13
   *    at org.scalatest.Assertions$class.newAssertionFailedException(Assertions.scala:449)
   *    at org.scalatest.Assertions$.newAssertionFailedException(Assertions.scala:1203)
   *    at org.scalatest.Assertions$AssertionsHelper.macroAssertTrue(Assertions.scala:417)
   *    at .&lt;init&gt;(&lt;console&gt;:15)
   *    at .&lt;clinit&gt;(&lt;console&gt;)
   *    at .&lt;init&gt;(&lt;console&gt;:7)
   *    at .&lt;clinit&gt;(&lt;console&gt;)
   *    at $print(&lt;console&gt;)
   *    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
   *    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
   *    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
   *    at java.lang.reflect.Method.invoke(Method.java:597)
   *    at scala.tools.nsc.interpreter.IMain$ReadEvalPrint.call(IMain.scala:731)
   *    at scala.tools.nsc.interpreter.IMain$Request.loadAndRun(IMain.scala:980)
   *    at scala.tools.nsc.interpreter.IMain.loadAndRunReq$1(IMain.scala:570)
   *    at scala.tools.nsc.interpreter.IMain.interpret(IMain.scala:601)
   *    at scala.tools.nsc.interpreter.IMain.interpret(IMain.scala:565)
   *    at scala.tools.nsc.interpreter.ILoop.reallyInterpret$1(ILoop.scala:745)
   *    at scala.tools.nsc.interpreter.ILoop.interpretStartingWith(ILoop.scala:790)
   *    at scala.tools.nsc.interpreter.ILoop.command(ILoop.scala:702)
   *    at scala.tools.nsc.interpreter.ILoop.processLine$1(ILoop.scala:566)
   *    at scala.tools.nsc.interpreter.ILoop.innerLoop$1(ILoop.scala:573)
   *    at scala.tools.nsc.interpreter.ILoop.loop(ILoop.scala:576)
   *    at scala.tools.nsc.interpreter.ILoop$$anonfun$process$1.apply$mcZ$sp(ILoop.scala:867)
   *    at scala.tools.nsc.interpreter.ILoop$$anonfun$process$1.apply(ILoop.scala:822)
   *    at scala.tools.nsc.interpreter.ILoop$$anonfun$process$1.apply(ILoop.scala:822)
   *    at scala.tools.nsc.util.ScalaClassLoader$.savingContextLoader(ScalaClassLoader.scala:135)
   *    at scala.tools.nsc.interpreter.ILoop.process(ILoop.scala:822)
   *    at scala.tools.nsc.MainGenericRunner.runTarget$1(MainGenericRunner.scala:83)
   *    at scala.tools.nsc.MainGenericRunner.process(MainGenericRunner.scala:96)
   *    at scala.tools.nsc.MainGenericRunner$.main(MainGenericRunner.scala:105)
   *    at scala.tools.nsc.MainGenericRunner.main(MainGenericRunner.scala)
   * }}}
   * 
   * That's a pretty tall stack trace. Here's what it looks like when you use `trap`:
   * 
   *
   * {{{  <!-- class="stREPL" -->
   * scala&gt; trap { x shouldEqual 13 }
   * res1: Throwable = org.scalatest.exceptions.TestFailedException: 12 did not equal 13
   * }}}
   *
   * Much less clutter. Bear in mind, however, that if ''no'' exception is thrown by the
   * passed block of code, the `trap` method will create a new <a href="Assertions$$NormalResult.html">`NormalResult`</a>
   * (a subclass of `Throwable` made for this purpose only) and return that. If the result was the `Unit` value, it
   * will simply say that no exception was thrown:
   * 
   *
   * {{{  <!-- class="stREPL" -->
   * scala&gt; trap { x shouldEqual 12 }
   * res2: Throwable = No exception was thrown.
   * }}}
   *
   * If the passed block of code results in a value other than `Unit`, the `NormalResult`'s `toString` will print the value:
   * 
   *
   * {{{  <!-- class="stREPL" -->
   * scala&gt; trap { "Dude!" }
   * res3: Throwable = No exception was thrown. Instead, result was: "Dude!"
   * }}}
   *
   * Although you can access the result value from the `NormalResult`, its type is `Any` and therefore not
   * very convenient to use. It is not intended that `trap` be used in test code. The sole intended use case for `trap` is decluttering
   * Scala interpreter sessions by eliminating stack traces when executing assertion and matcher expressions.
   * 
   */
  @deprecated("The trap method is no longer needed for demos in the REPL, which now abreviates stack traces, and will be removed in a future version of ScalaTest")
  def trap[T](f: => T): Throwable = {
    try { new NormalResult(f) }
    catch {
      case ex: Throwable if !Suite.anExceptionThatShouldCauseAnAbort(ex) => ex
    }
  }

  /**
   * Assert that the value passed as `expected` equals the value passed as `actual`.
   * If the `actual` equals the `expected`
   * (as determined by `==`), `assertResult` returns
   * normally. Else, if `actual` is not equal to `expected`, `assertResult` throws a
   * `TestFailedException` whose detail message includes the expected and actual values, as well as the `String`
   * obtained by invoking `toString` on the passed `clue`.
   *
   * @param expected the expected value
   * @param clue An object whose `toString` method returns a message to include in a failure report.
   * @param actual the actual value, which should equal the passed `expected` value
   * @throws TestFailedException if the passed `actual` value does not equal the passed `expected` value.
   */
  def assertResult(expected: Any, clue: Any)(actual: Any)(implicit prettifier: Prettifier, pos: source.Position): Assertion = {
    if (!areEqualComparingArraysStructurally(actual, expected)) {
      val (act, exp) = Suite.getObjectsForFailureMessage(actual, expected)
      val s = FailureMessages.expectedButGot(prettifier, exp, act)
      val fullMsg = AppendedClues.appendClue(s, clue.toString)
      throw newAssertionFailedException(Some(fullMsg), None, pos)
    }
    Succeeded
  }

  /** 
   * Assert that the value passed as `expected` equals the value passed as `actual`.
   * If the `actual` value equals the `expected` value
   * (as determined by `==`), `assertResult` returns
   * normally. Else, `assertResult` throws a
   * `TestFailedException` whose detail message includes the expected and actual values.
   *
   * @param expected the expected value
   * @param actual the actual value, which should equal the passed `expected` value
   * @throws TestFailedException if the passed `actual` value does not equal the passed `expected` value.
   */
  def assertResult(expected: Any)(actual: Any)(implicit prettifier: Prettifier, pos: source.Position): Assertion = {
    if (!areEqualComparingArraysStructurally(actual, expected)) {
      val (act, exp) = Suite.getObjectsForFailureMessage(actual, expected)
      val s = FailureMessages.expectedButGot(prettifier, exp, act)
      throw newAssertionFailedException(Some(s), None, pos)
    }
    Succeeded
  }
  
/*
   * TODO: Delete this if sticking with Nothing instead of Unit as result type of fail.
   * <p>
   * The result type of this and the other overloaded <code>fail</code> methods is
   * <code>Unit</code> instead of <code>Nothing</code>, because <code>Nothing</code>
   * is a subtype of all other types. If the result type of <code>fail</code> were
   * <code>Nothing</code>, a block of code that ends in a call to <code>fail()</code> may
   * fail to compile if the block being passed as a by-name parameter or function to an
   * overloaded method. The reason is that the compiler selects which overloaded
   * method to call based on the static types of the parameters passed. Since
   * <code>Nothing</code> is an instance of everything, it can often make the overloaded
   * method selection ambiguous.
   * </p>
   *
   * <p>
   * For a concrete example, the <code>Conductor</code> class
   * in package <code>org.scalatest.concurrent</code> has two overloaded variants of the
   * <code>thread</code> method:
   * </p>
   *
   * <pre class="stHighlight">
   * def thread[T](fun: => T): Thread
   *
   * def thread[T](name: String)(fun: => T): Thread
   * </pre>
   *
   * <p>
   * Given these two overloaded methods, the following code will compile given the result type
   * of <code>fail</code> is <code>Unit</code>, but would not compile if the result type were
   * <code>Nothing</code>:
   * </p>
   *
   * <pre class="stHighlight">
   * thread { fail() }
   * </pre>
   *
   * <p>
   * If the result type of <code>fail</code> were <code>Nothing</code>, the type of the by-name parameter
   * would be inferred to be <code>Nothing</code>, which is a subtype of both <code>T</code> and
   * <code>String</code>. Thus the call is ambiguous, because the type matches the first parameter type
   * of both overloaded <code>thread</code> methods. <code>Unit</code>, by constrast, is <em>not</em>
   * a subtype of <code>String</code>, so it only matches one overloaded variant and compiles just fine.
   * </p>
*/
  /**
   * Throws `TestFailedException` to indicate a test failed.
   */
  def fail()(implicit pos: source.Position): Nothing = { throw newAssertionFailedException(None, None, pos) }

  /**
   * Throws `TestFailedException`, with the passed
   * `String` `message` as the exception's detail
   * message, to indicate a test failed.
   *
   * @param message A message describing the failure.
   * @throws NullArgumentException if `message` is `null`
   */
  def fail(message: String)(implicit pos: source.Position): Nothing = {

    requireNonNull(message)
     
    throw newAssertionFailedException(Some(message),  None, pos)
  }

  /**
   * Throws `TestFailedException`, with the passed
   * `String` `message` as the exception's detail
   * message and `Throwable` cause, to indicate a test failed.
   *
   * @param message A message describing the failure.
   * @param cause A `Throwable` that indicates the cause of the failure.
   * @throws NullArgumentException if `message` or `cause` is `null`
   */
  def fail(message: String, cause: Throwable)(implicit pos: source.Position): Nothing = {

    requireNonNull(message, cause)

    throw newAssertionFailedException(Some(message), Some(cause), pos)
  }

  /**
   * Throws `TestFailedException`, with the passed
   * `Throwable` cause, to indicate a test failed.
   * The `getMessage` method of the thrown `TestFailedException`
   * will return `cause.toString`.
   *
   * @param cause a `Throwable` that indicates the cause of the failure.
   * @throws NullArgumentException if `cause` is `null`
   */
  def fail(cause: Throwable)(implicit pos: source.Position): Nothing = {

    requireNonNull(cause)
        
    throw newAssertionFailedException(None, Some(cause), pos)
  }
  
  /**
   * Throws `TestCanceledException` to indicate a test was canceled.
   */
  def cancel()(implicit pos: source.Position): Nothing = { throw newTestCanceledException(None, None, pos) }

  /**
   * Throws `TestCanceledException`, with the passed
   * `String` `message` as the exception's detail
   * message, to indicate a test was canceled.
   *
   * @param message A message describing the cancellation.
   * @throws NullArgumentException if `message` is `null`
   */
  def cancel(message: String)(implicit pos: source.Position): Nothing = {

    requireNonNull(message)
     
    throw newTestCanceledException(Some(message),  None, pos)
  }

  /**
   * Throws `TestCanceledException`, with the passed
   * `String` `message` as the exception's detail
   * message and `Throwable` cause, to indicate a test failed.
   *
   * @param message A message describing the failure.
   * @param cause A `Throwable` that indicates the cause of the failure.
   * @throws NullArgumentException if `message` or `cause` is `null`
   */
  def cancel(message: String, cause: Throwable)(implicit pos: source.Position): Nothing = {

    requireNonNull(message, cause)

    throw newTestCanceledException(Some(message), Some(cause), pos)
  }

  /**
   * Throws `TestCanceledException`, with the passed
   * `Throwable` cause, to indicate a test failed.
   * The `getMessage` method of the thrown `TestCanceledException`
   * will return `cause.toString`.
   *
   * @param cause a `Throwable` that indicates the cause of the cancellation.
   * @throws NullArgumentException if `cause` is `null`
   */
  def cancel(cause: Throwable)(implicit pos: source.Position): Nothing = {

    requireNonNull(cause)
        
    throw newTestCanceledException(None, Some(cause), pos)
  }
  
  /**
   * Executes the block of code passed as the second parameter, and, if it
   * completes abruptly with a `ModifiableMessage` exception,
   * prepends the "clue" string passed as the first parameter to the beginning of the detail message
   * of that thrown exception, then rethrows it. If clue does not end in a white space
   * character, one space will be added
   * between it and the existing detail message (unless the detail message is
   * not defined).
   *
   * This method allows you to add more information about what went wrong that will be
   * reported when a test fails. Here's an example:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * withClue("(Employee's name was: " + employee.name + ")") {
   *   intercept[IllegalArgumentException] {
   *     employee.getTask(-1)
   *   }
   * }
   * }}}
   *
   * If an invocation of `intercept` completed abruptly with an exception, the resulting message would be something like:
   * 
   *
   * {{{
   * (Employee's name was Bob Jones) Expected IllegalArgumentException to be thrown, but no exception was thrown
   * }}}
   *
   * @throws NullArgumentException if the passed `clue` is `null`
  */
  def withClue[T](clue: Any)(fun: => T): T = {
    requireNonNull(clue)
    val prepend = (currentMessage: Option[String]) => {
      currentMessage match {
        case Some(msg) =>
          if (clue.toString.last.isWhitespace) // TODO: shouldn't I also check if the head of msg isWhite?
            Some(clue.toString + msg)
          else
            Some(clue.toString + " " + msg)
        case None => Some(clue.toString)
      }
    }
    try {
      val outcome = fun
      outcome match {
        case Failed(e: org.scalatest.exceptions.ModifiableMessage[_]) if clue.toString != "" =>
          Failed(e.modifyMessage(prepend)).asInstanceOf[T]
        case Canceled(e: org.scalatest.exceptions.ModifiableMessage[_]) if clue.toString != "" =>
          Canceled(e.modifyMessage(prepend)).asInstanceOf[T]
        case _ => outcome
      }
    }
    catch {
      case e: org.scalatest.exceptions.ModifiableMessage[_] =>
        if (clue != "")
          throw e.modifyMessage(prepend)
        else
          throw e
    }
  }

/* Hold off on this for now. See how people do with the simple one that takes an Any.
  def withClueFunction(sfun: Option[String] => Option[String])(fun: => Unit) {
    fun
  }
*/
  /**
   * Throws `TestPendingException` to indicate a test is pending.
   *
   * A ''pending test'' is one that has been given a name but is not yet implemented. The purpose of
   * pending tests is to facilitate a style of testing in which documentation of behavior is sketched
   * out before tests are written to verify that behavior (and often, the before the behavior of
   * the system being tested is itself implemented). Such sketches form a kind of specification of
   * what tests and functionality to implement later.
   * 
   *
   * To support this style of testing, a test can be given a name that specifies one
   * bit of behavior required by the system being tested. The test can also include some code that
   * sends more information about the behavior to the reporter when the tests run. At the end of the test,
   * it can call method `pending`, which will cause it to complete abruptly with `TestPendingException`.
   * Because tests in ScalaTest can be designated as pending with `TestPendingException`, both the test name and any information
   * sent to the reporter when running the test can appear in the report of a test run. (In other words,
   * the code of a pending test is executed just like any other test.) However, because the test completes abruptly
   * with `TestPendingException`, the test will be reported as pending, to indicate
   * the actual test, and possibly the functionality it is intended to test, has not yet been implemented.
   * 
   *
   * Note: This method always completes abruptly with a `TestPendingException`. Thus it always has a side
   * effect. Methods with side effects are usually invoked with parentheses, as in `pending()`. This
   * method is defined as a parameterless method, in flagrant contradiction to recommended Scala style, because it 
   * forms a kind of DSL for pending tests. It enables tests in suites such as `FunSuite` or `FunSpec`
   * to be denoted by placing "`(pending)`" after the test name, as in:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * test("that style rules are not laws") (pending)
   * }}}
   *
   * Readers of the code see "pending" in parentheses, which looks like a little note attached to the test name to indicate
   * it is pending. Whereas "`(pending())` looks more like a method call, "`(pending)`" lets readers
   * stay at a higher level, forgetting how it is implemented and just focusing on the intent of the programmer who wrote the code.
   * 
   */
  def pending: Assertion with PendingStatement = { throw new TestPendingException }

  /**
   * Execute the passed block of code, and if it completes abruptly, throw `TestPendingException`, else
   * throw `TestFailedException`.
   *
   * This method can be used to temporarily change a failing test into a pending test in such a way that it will
   * automatically turn back into a failing test once the problem originally causing the test to fail has been fixed.
   * At that point, you need only remove the `pendingUntilFixed` call. In other words, a
   * `pendingUntilFixed` surrounding a block of code that isn't broken is treated as a test failure.
   * The motivation for this behavior is to encourage people to remove `pendingUntilFixed` calls when
   * there are no longer needed.
   * 
   *
   * This method facilitates a style of testing in which tests are written before the code they test. Sometimes you may
   * encounter a test failure that requires more functionality than you want to tackle without writing more tests. In this
   * case you can mark the bit of test code causing the failure with `pendingUntilFixed`. You can then write more
   * tests and functionality that eventually will get your production code to a point where the original test won't fail anymore.
   * At this point the code block marked with `pendingUntilFixed` will no longer throw an exception (because the
   * problem has been fixed). This will in turn cause `pendingUntilFixed` to throw `TestFailedException`
   * with a detail message explaining you need to go back and remove the `pendingUntilFixed` call as the problem orginally
   * causing your test code to fail has been fixed.
   * 
   *
   * @param f a block of code, which if it completes abruptly, should trigger a `TestPendingException` 
   * @throws TestPendingException if the passed block of code completes abruptly with an `Exception` or `AssertionError`
   */
  def pendingUntilFixed(f: => Unit)(implicit pos: source.Position): Assertion with PendingStatement = {
    val isPending =
      try {
        f
        false
      }
      catch {
        case _: Exception => true
        case _: AssertionError => true
      }
      if (isPending)
        throw new TestPendingException
      else
        throw new TestFailedException((sde: StackDepthException) => Some(Resources.pendingUntilFixed), None, pos)
  }

  /**
   * The `Succeeded` singleton.
   *
   * You can use `succeed` to solve a type error when an async test 
   * does not end in either `Future[Assertion]` or `Assertion`.
   * Because `Assertion` is a type alias for `Succeeded.type`,
   * putting `succeed` at the end of a test body (or at the end of a
   * function being used to map the final future of a test body) will solve
   * the type error.
   * 
   */
  final val succeed: Assertion = Succeeded
}

/**
 * Companion object that facilitates the importing of `Assertions` members as 
 * an alternative to mixing it in. One use case is to import `Assertions` members so you can use
 * them in the Scala interpreter:
 *
 * {{{  <!-- class="stREPL" -->
 * $scala -classpath scalatest.jar
 * Welcome to Scala version 2.7.3.final (Java HotSpot(TM) Client VM, Java 1.5.0_16).
 * Type in expressions to have them evaluated.
 * Type :help for more information.
 * &nbsp;
 * scala&gt; import org.scalatest.Assertions._
 * import org.scalatest.Assertions._
 * &nbsp;
 * scala&gt; assert(1 === 2)
 * org.scalatest.TestFailedException: 1 did not equal 2
 *      at org.scalatest.Assertions$class.assert(Assertions.scala:211)
 *      at org.scalatest.Assertions$.assert(Assertions.scala:511)
 *      at .&lt;init&gt;(&lt;console&gt;:7)
 *      at .&lt;clinit&gt;(&lt;console&gt;)
 *      at RequestResult$.&lt;init&gt;(&lt;console&gt;:3)
 *      at RequestResult$.&lt;clinit&gt;(&lt;console&gt;)
 *      at RequestResult$result(&lt;console&gt;)
 *      at sun.reflect.NativeMethodAccessorImpl.invoke...
 *&nbsp;
 * scala&gt; assertResult(3) { 1 + 3 }
 * org.scalatest.TestFailedException: Expected 3, but got 4
 *      at org.scalatest.Assertions$class.expect(Assertions.scala:447)
 *      at org.scalatest.Assertions$.expect(Assertions.scala:511)
 *      at .&lt;init&gt;(&lt;console&gt;:7)
 *      at .&lt;clinit&gt;(&lt;console&gt;)
 *      at RequestResult$.&lt;init&gt;(&lt;console&gt;:3)
 *      at RequestResult$.&lt;clinit&gt;(&lt;console&gt;)
 *      at RequestResult$result(&lt;console&gt;)
 *      at sun.reflect.NativeMethodAccessorImpl.in...
 *&nbsp;
 * scala&gt; val caught = intercept[StringIndexOutOfBoundsException] { "hi".charAt(-1) }
 * caught: StringIndexOutOfBoundsException = java.lang.StringIndexOutOfBoundsException: String index out of range: -1
 * }}}
 *
 * @author Bill Venners
 */
object Assertions extends Assertions {

  @deprecated("The trap method is no longer needed for demos in the REPL, which now abreviates stack traces, so NormalResult will be removed in a future version of ScalaTest")
  case class NormalResult(result: Any) extends Throwable {
    override def toString = if (result == ()) Resources.noExceptionWasThrown else Resources.resultWas(Prettifier.default(result))
  }

  private[scalatest] def areEqualComparingArraysStructurally(left: Any, right: Any): Boolean = {
    // Prior to 2.0 this only called .deep if both sides were arrays. Loosened it
    // when nearing 2.0.M6 to call .deep if either left or right side is an array.
    // TODO: this is the same algo as in scalactic.DefaultEquality. Put that one in
    // a singleton and use it in both places.
    left match {
      case leftArray: Array[_] =>
        right match {
          case rightArray: Array[_] => leftArray.deep == rightArray.deep
          case rightNonEmptyArray: NonEmptyArray[_] => leftArray.deep == rightNonEmptyArray.toArray.deep
          case _ => leftArray.deep == right
        }
      case leftNonEmptyArray: NonEmptyArray[_] =>
        right match {
          case rightArray: Array[_] => leftNonEmptyArray.toArray.deep == rightArray.deep
          case rightNonEmptyArray: NonEmptyArray[_] => leftNonEmptyArray.toArray.deep == rightNonEmptyArray.toArray.deep
          case _ => leftNonEmptyArray.toArray.deep == right
        }

      case other => {
        right match {
          case rightArray: Array[_] => left == rightArray.deep
          case rightNonEmptyArray: NonEmptyArray[_] => left == rightNonEmptyArray.toArray.deep
          case _ => left == right
        }
      }
    }
  }

  /**
    * Helper class used by code generated by the `assert` macro.
    */
  class AssertionsHelper {

    private def append(currentMessage: Option[String], clue: Any) = {
      val clueStr = clue.toString
      if (clueStr.isEmpty)
        currentMessage
      else {
        currentMessage match {
          case Some(msg) =>
            // clue.toString.head is guaranteed to work, because the previous if check that clue.toString != ""
            val firstChar = clueStr.head
            if (firstChar.isWhitespace || firstChar == '.' || firstChar == ',' || firstChar == ';')
              Some(msg + clueStr)
            else
              Some(msg + " " + clueStr)
          case None => Some(clueStr)
        }
      }
    }

    /**
      * Assert that the passed in `Bool` is `true`, else fail with `TestFailedException`.
      *
      * @param bool the `Bool` to assert for
      * @param clue optional clue to be included in `TestFailedException`'s error message when assertion failed
      */
    def macroAssert(bool: Bool, clue: Any, prettifier: Prettifier, pos: source.Position): Assertion = {
      requireNonNull(clue)(prettifier, pos)
      if (!bool.value) {
        val failureMessage = if (Bool.isSimpleWithoutExpressionText(bool)) None else Some(bool.failureMessage)
        throw newAssertionFailedException(append(failureMessage, clue), None, pos)
      }
      Succeeded
    }

    /**
      * Assume that the passed in `Bool` is `true`, else throw `TestCanceledException`.
      *
      * @param bool the `Bool` to assume for
      * @param clue optional clue to be included in `TestCanceledException`'s error message when assertion failed
      */
    def macroAssume(bool: Bool, clue: Any, prettifier: Prettifier, pos: source.Position): Assertion = {
      requireNonNull(clue)(prettifier, pos)
      if (!bool.value) {
        val failureMessage = if (Bool.isSimpleWithoutExpressionText(bool)) None else Some(bool.failureMessage)
        throw newTestCanceledException(append(failureMessage, clue), None, pos)
      }
      Succeeded
    }
  }

  /**
    * Helper instance used by code generated by macro assertion.
    */
  val assertionsHelper = new AssertionsHelper
}
