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
package org.scalatest.check

import org.scalatest.enablers.CheckerAsserting
import org.scalatest.prop.Configuration
import org.scalatest.Assertion
import org.scalacheck.Arbitrary
import org.scalacheck.Shrink
import org.scalacheck.util.Pretty
import org.scalacheck.Prop
import org.scalacheck.Test
import org.scalactic._

/**
 * Trait that contains several &ldquo;check&rdquo; methods that perform ScalaCheck property checks.
 * If ScalaCheck finds a test case for which a property doesn't hold, the problem will be reported as a ScalaTest test failure.
 * 
 * To use ScalaCheck, you specify properties and, in some cases, generators that generate test data. You need not always
 * create generators, because ScalaCheck provides many default generators for you that can be used in many situations.
 * ScalaCheck will use the generators to generate test data and with that data run tests that check that the property holds.
 * Property-based tests can, therefore, give you a lot more testing for a lot less code than assertion-based tests.
 * Here's an example of using ScalaCheck from a `JUnitSuite`:
 * 
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.junit.JUnitSuite
 * import org.scalatest.prop.Checkers
 * import org.scalacheck.Arbitrary._
 * import org.scalacheck.Prop._
 *
 * class MySuite extends JUnitSuite with Checkers {
 *   @Test
 *   def testConcat() {
 *     check((a: List[Int], b: List[Int]) => a.size + b.size == (a ::: b).size)
 *   }
 * }
 * }}}
 * The `check` method, defined in `Checkers`, makes it easy to write property-based tests inside
 * ScalaTest, JUnit, and TestNG test suites. This example specifies a property that `List`'s `:::` method
 * should obey. ScalaCheck properties are expressed as function values that take the required
 * test data as parameters. ScalaCheck will generate test data using generators and 
repeatedly pass generated data to the function. In this case, the test data is composed of integer lists named `a` and `b`.
 * Inside the body of the function, you see:
 * 
 * {{{  <!-- class="stHighlight" -->
 * a.size + b.size == (a ::: b).size
 * }}}
 * The property in this case is a `Boolean` expression that will yield true if the size of the concatenated list is equal
 * to the size of each individual list added together. With this small amount
 * of code, ScalaCheck will generate possibly hundreds of value pairs for `a` and `b` and test each pair, looking for
 * a pair of integers for which the property doesn't hold. If the property holds true for every value ScalaCheck tries,
 * `check` returns normally. Otherwise, `check` will complete abruptly with a `TestFailedException` that
 * contains information about the failure, including the values that cause the property to be false.
 * 
 *
 * For more information on using ScalaCheck properties, see the documentation for ScalaCheck, which is available
 * from <a href="http://code.google.com/p/scalacheck/">http://code.google.com/p/scalacheck/</a>.
 * 
 *
 * To execute a suite that mixes in `Checkers` with ScalaTest's `Runner`, you must include ScalaCheck's jar file on the class path or runpath.
 * 
 *
 * <a name="propCheckConfig"></a>==Property check configuration==
 *
 * The property checks performed by the `check` methods of this trait can be flexibly configured via the services
 * provided by supertrait `Configuration`.  The five configuration parameters for property checks along with their
 * default values and meanings are described in the following table:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''Configuration Parameter'''
 * </th>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''Default Value'''
 * </th>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''Meaning'''
 * </th>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * minSuccessful
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * 100
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * the minimum number of successful property evaluations required for the property to pass
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * maxDiscarded
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * 500
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * the maximum number of discarded property evaluations allowed during a property check
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * minSize
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * 0
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * the minimum size parameter to provide to ScalaCheck, which it will use when generating objects for which size matters (such as strings or lists)
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * maxSize
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * 100
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * the maximum size parameter to provide to ScalaCheck, which it will use when generating objects for which size matters (such as strings or lists)
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * workers
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * 1
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * specifies the number of worker threads to use during property evaluation
 * </td>
 * </tr>
 * </table>
 *
 * The `check` methods of trait `Checkers` each take a `PropertyCheckConfiguration`
 * object as an implicit parameter. This object provides values for each of the five configuration parameters. Trait `Configuration`
 * provides an implicit `val` named `generatorDrivenConfig` with each configuration parameter set to its default value.
 * If you want to set one or more configuration parameters to a different value for all property checks in a suite you can override this
 * val (or hide it, for example, if you are importing the members of the `Checkers` companion object rather
 * than mixing in the trait.) For example, if
 * you want all parameters at their defaults except for `minSize` and `maxSize`, you can override
 * `generatorDrivenConfig`, like this:
 *
 * {{{  <!-- class="stHighlight" -->
 * implicit override val generatorDrivenConfig =
 *   PropertyCheckConfiguration(minSize = 10, sizeRange = 10)
 * }}}
 *
 * Or, if hide it by declaring a variable of the same name in whatever scope you want the changed values to be in effect:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * implicit val generatorDrivenConfig =
 *   PropertyCheckConfiguration(minSize = 10, sizeRange = 10)
 * }}}
 *
 * In addition to taking a `PropertyCheckConfiguration` object as an implicit parameter, the `check` methods of trait
 * `Checkers` also take a variable length argument list of `PropertyCheckConfigParam`
 * objects that you can use to override the values provided by the implicit `PropertyCheckConfiguration` for a single `check`
 * invocation. You place these configuration settings after the property or property function, For example, if you want to
 * set `minSuccessful` to 500 for just one particular `check` invocation,
 * you can do so like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * check((n: Int) => n + 0 == n, minSuccessful(500))
 * }}}
 *
 * This invocation of `check` will use 500 for `minSuccessful` and whatever values are specified by the
 * implicitly passed `PropertyCheckConfiguration` object for the other configuration parameters.
 * If you want to set multiple configuration parameters in this way, just list them separated by commas:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * check((n: Int) => n + 0 == n, minSuccessful(500), maxDiscardedFactor(0.6))
 * }}}
 *
 * The previous configuration approach works the same in `Checkers` as it does in `GeneratorDrivenPropertyChecks`.
 * Trait `Checkers` also provides one `check` method that takes an `org.scalacheck.Test.Parameters` object,
 * in case you want to configure ScalaCheck that way.
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalacheck.Prop
 * import org.scalacheck.Test.Parameters
 * import org.scalatest.prop.Checkers._
 *
 * check(Prop.forAll((n: Int) => n + 0 == n), Parameters.Default { override val minSuccessfulTests = 5 })
 * }}}
 *
 * For more information, see the documentation
 * for supertrait <a href="Configuration.html">`Configuration`</a>.
 * 
 *
 * @author Bill Venners
 */
trait Checkers extends Configuration {

  private val asserting: CheckerAsserting[Assertion] { type Result = Assertion }  = CheckerAsserting.assertingNatureOfAssertion

  /**
   * Convert the passed 1-arg function into a property, and check it.
   *
   * @param f the function to be converted into a property and checked
   * @throws TestFailedException if a test case is discovered for which the property doesn't hold.
   */
  def check[A1, P](f: A1 => P, configParams: PropertyCheckConfigParam*)
    (implicit
      config: PropertyCheckConfigurable,
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      prettifier: Prettifier,
      pos: source.Position
    ): Assertion = {
    val params = getScalaCheckParams(configParams, config)
    asserting.check(Prop.forAll(f)(p, a1, s1, pp1), params, prettifier, pos)
  }

  /**
   * Convert the passed 2-arg function into a property, and check it.
   *
   * @param f the function to be converted into a property and checked
   * @throws TestFailedException if a test case is discovered for which the property doesn't hold.
   */
  def check[A1, A2, P](f: (A1,A2) => P, configParams: PropertyCheckConfigParam*)
    (implicit
      config: PropertyCheckConfigurable,
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty,
      prettifier: Prettifier,
      pos: source.Position
    ): Assertion = {
    val params = getScalaCheckParams(configParams, config)
    asserting.check(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2), params, prettifier, pos)
  }

  /**
   * Convert the passed 3-arg function into a property, and check it.
   *
   * @param f the function to be converted into a property and checked
   * @throws TestFailedException if a test case is discovered for which the property doesn't hold.
   */
  def check[A1, A2, A3, P](f: (A1,A2,A3) => P, configParams: PropertyCheckConfigParam*)
    (implicit
      config: PropertyCheckConfigurable,
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty,
      a3: Arbitrary[A3], s3: Shrink[A3], pp3: A3 => Pretty,
      prettifier: Prettifier,
      pos: source.Position
    ): Assertion = {
    val params = getScalaCheckParams(configParams, config)
    asserting.check(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2, a3, s3, pp3), params, prettifier, pos)
  }

  /**
   * Convert the passed 4-arg function into a property, and check it.
   *
   * @param f the function to be converted into a property and checked
   * @throws TestFailedException if a test case is discovered for which the property doesn't hold.
   */
  def check[A1, A2, A3, A4, P](f: (A1,A2,A3,A4) => P, configParams: PropertyCheckConfigParam*)
    (implicit
      config: PropertyCheckConfigurable,
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty,
      a3: Arbitrary[A3], s3: Shrink[A3], pp3: A3 => Pretty,
      a4: Arbitrary[A4], s4: Shrink[A4], pp4: A4 => Pretty,
      prettifier: Prettifier,
      pos: source.Position
    ): Assertion = {
    val params = getScalaCheckParams(configParams, config)
    asserting.check(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2, a3, s3, pp3, a4, s4, pp4), params, prettifier, pos)
  }

  /**
   * Convert the passed 5-arg function into a property, and check it.
   *
   * @param f the function to be converted into a property and checked
   * @throws TestFailedException if a test case is discovered for which the property doesn't hold.
   */
  def check[A1, A2, A3, A4, A5, P](f: (A1,A2,A3,A4,A5) => P, configParams: PropertyCheckConfigParam*)
    (implicit
      config: PropertyCheckConfigurable,
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty,
      a3: Arbitrary[A3], s3: Shrink[A3], pp3: A3 => Pretty,
      a4: Arbitrary[A4], s4: Shrink[A4], pp4: A4 => Pretty,
      a5: Arbitrary[A5], s5: Shrink[A5], pp5: A5 => Pretty,
      prettifier: Prettifier,
      pos: source.Position
    ): Assertion = {
    val params = getScalaCheckParams(configParams, config)
    asserting.check(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2, a3, s3, pp3, a4, s4, pp4, a5, s5, pp5), params, prettifier, pos)
  }

  /**
   * Convert the passed 6-arg function into a property, and check it.
   *
   * @param f the function to be converted into a property and checked
   * @throws TestFailedException if a test case is discovered for which the property doesn't hold.
   */
  def check[A1, A2, A3, A4, A5, A6, P](f: (A1,A2,A3,A4,A5,A6) => P, configParams: PropertyCheckConfigParam*)
    (implicit
      config: PropertyCheckConfigurable,
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty,
      a3: Arbitrary[A3], s3: Shrink[A3], pp3: A3 => Pretty,
      a4: Arbitrary[A4], s4: Shrink[A4], pp4: A4 => Pretty,
      a5: Arbitrary[A5], s5: Shrink[A5], pp5: A5 => Pretty,
      a6: Arbitrary[A6], s6: Shrink[A6], pp6: A6 => Pretty,
      prettifier: Prettifier,
      pos: source.Position
    ): Assertion = {
    val params = getScalaCheckParams(configParams, config)
    asserting.check(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2, a3, s3, pp3, a4, s4, pp4, a5, s5, pp5, a6, s6, pp6), params, prettifier, pos)
  }

  /**
   * Check a property with the given testing parameters.
   *
   * @param p the property to check
   * @param prms the test parameters
   * @throws TestFailedException if a test case is discovered for which the property doesn't hold.
   */
  def check(p: Prop, prms: Test.Parameters)(implicit prettifier: Prettifier, pos: source.Position): Assertion = {
    asserting.check(p, prms, prettifier, pos)
  }

  /**
   * Check a property.
   *
   * @param p the property to check
   * @throws TestFailedException if a test case is discovered for which the property doesn't hold.
   */
  def check(p: Prop, configParams: PropertyCheckConfigParam*)(implicit config: PropertyCheckConfigurable, prettifier: Prettifier, pos: source.Position): Assertion = {
    val params = getScalaCheckParams(configParams, config)
    asserting.check(p, params, prettifier, pos)
  }
}

/**
 * Companion object that facilitates the importing of `Checkers` members as
 * an alternative to mixing it in. One use case is to import `Checkers` members so you can use
 * them in the Scala interpreter.
 *
 * @author Bill Venners
 */
object Checkers extends Checkers

  /*
   * Returns a ScalaCheck <code>Prop</code> that succeeds if the passed by-name
   * parameter, <code>fun</code>, returns normally; fails if it throws
   * an exception.
   *
   * <p>
   * This method enables ScalaTest assertions and matcher expressions to be used 
   * in property checks. Here's an example:
   * </p>
   *
   * <pre class="stHighlight">
   * check((s: String, t: String) => successOf(s + t should endWith (s)))
   * </pre>
   *
   * <p>
   * The detail message of the <code>TestFailedException</code> that will likely
   * be thrown by the matcher expression will be added as a label to the ScalaCheck
   * <code>Prop</code> returned by <code>successOf</code>. This, this property
   * check might fail with an exception like:
   * </p>
   *
   * <pre class="stHighlight">
   * org.scalatest.prop.GeneratorDrivenPropertyCheckFailedException: TestFailedException (included as this exception's cause) was thrown during property evaluation.
   * Label of failing property: "ab" did not end with substring "a" (script.scala:24)
   * > arg0 = "?" (1 shrinks)
   * > arg1 = "?" (1 shrinks)
   * 	at org.scalatest.prop.Checkers$class.check(Checkers.scala:252)
   * 	at org.scalatest.prop.Checkers$.check(Checkers.scala:354)
   *    ...
   * </pre>
   *
   * <p>
   * One use case for using matcher expressions in your properties is to 
   * get helpful error messages without using ScalaCheck labels. For example,
   * instead of:
   * </p>
   *
   * <pre class="stHighlight">
   * val complexProp = forAll { (m: Int, n: Int) =>
   *   val res = n * m
   *   (res >= m)    :| "result > #1" &&
   *   (res >= n)    :| "result > #2" &&
   *   (res < m + n) :| "result not sum"
   * }
   * </pre>
   * 
   * <p>
   * You could write:
   * </p>
   *
   * <pre class="stHighlight">
   * val complexProp = forAll { (m: Int, n: Int) =>
   *   successOf {
   *     val res = n * m
   *     res should be >= m
   *     res should be >= n
   *     res should be < (m + n)
   *   }
   * </pre>
   *
   * @param fun the expression to evaluate to determine what <code>Prop</code>
   *            to return
   * @return a ScalaCheck property that passes if the passed by-name parameter,
   *         <code>fun</code>, returns normally, fails if it throws an exception
  private def successOf(fun: => Unit): Prop =
    try {
      fun
      Prop.passed
    }
    catch {
      case e: StackDepth =>
        val msgPart = if (e.message.isDefined) e.message.get + " " else ""
        val fileLinePart =
          if (e.failedCodeFileNameAndLineNumberString.isDefined)
            "(" + e.failedCodeFileNameAndLineNumberString.get + ")"
          else
            ""
        val lbl = msgPart + fileLinePart
        Prop.exception(e).label(lbl)
      case e => Prop.exception(e) // Not sure what to do here
    }
   */
