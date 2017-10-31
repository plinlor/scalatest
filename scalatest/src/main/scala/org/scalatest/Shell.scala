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

/*
Delete this later:
class ArithmeticSuite extends FunSuite with matchers.Matchers {
  test("addition works") {
    1 + 1 should equal (2)
  }
  ignore("subtraction works") {
    1 - 1 should equal (0)
  }
  test("multiplication works") {
    1 * 1 should equal (2)
  }
  test("division works") (pending)
}
*/

/**
 * Trait whose instances provide a <a href="run$.html">`run`</a> method and configuration fields that implement
 * the ''ScalaTest shell'': its DSL for the Scala interpreter.
 *
 * The main command of the ScalaTest shell is `run`, which you can use to run a suite of tests.
 * The shell also provides several commands for configuring a call to `run`:
 * 
 *
 * <ul>
 * <li>`color` (the default) - display results in color (green for success; red for failure; yellow for warning; blue for statistics)</li>
 * <li>`nocolor` - display results without color</li>
 * <li>`durations` - display durations of (''i.e.'', how long it took to run) tests and suites</li>
 * <li>`nodurations` (the default) - do not display durations of tests and suites</li>
 * <li>`shortstacks` - display short (''i.e.'', truncated to show just the most useful portion) stack traces for all exceptions</li>
 * <li>`fullstacks` - display full stack trackes for all exceptions</li>
 * <li>`nostacks` (the default) - display no stack trace for `StackDepth` exceptions and a short stack trace for non-`StackDepth`
 *   exceptions</li>
 * <li>`stats` - display statistics before and after the run, such as expected test count before the run and tests succeeded, failed, pending,
 * ''etc.'', counts after the run</li>
 * <li>`nostats` (the default) not display statistics before or after the run</li>
 * </ul>
 *
 * The default configuration is `color`, `nodurations`, `nostacks`, and `nostats`.
 * 
 *
 * All of these commands are fields of trait `org.scalatest.Shell`. Each configuration command is a field that refers to
 * another `Shell` instance with every configuration parameter
 * the same except for the one you've asked to change. For example, `durations` provides a
 * `Shell` instance that has every parameter configured the same way, except with durations enabled. When you invoke
 * `run` on that, you will get a run with durations enabled and every other configuration parameter at its default value.
 *
 * The other useful "command"
 * to know about, though not technically part of the shell, is the `apply` factory method in the <a href="Suites$.html">`Suites`</a> 
 * singleton object. This allows you to easily create composite suites out of nested suites, which you can then pass to `run`. This
 * will be demonstrated later in this documentation.
 * 
 *
 * ==Using the ScalaTest shell==
 *
 * The package object of the `org.scalatest` package, although it does not extend `Shell`, declares all the
 * same members as `Shell`. Its `run` method runs with all the `Shell` configuration parameters set
 * to their default values. A good way to use the ScalaTest shell, therefore, is to import the members of package `org.scalatest`:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> import org.scalatest._
 * import org.scalatest._</span>
 * }}}
 *
 * One thing importing `org.scalatest._` allows you to do is access any of ScalaTest's classes and traits by shorter
 * names, for example:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> class ArithmeticSuite extends FunSuite with matchers.Matchers {
 *      |   test("addition works") { 
 *      |     1 + 1 should equal (2)
 *      |   } 
 *      |   ignore("subtraction works") {
 *      |     1 - 1 should equal (0)
 *      |   }
 *      |   test("multiplication works") {
 *      |     1 * 1 should equal (2) 
 *      |   }
 *      |   test("division works") (pending)
 *      | } 
 * defined class ArithmeticSuite</span>
 * }}}
 *
 * But importing `org.scalatest._` also brings into scope the commands of the `Shell`, so you can, for
 * example, invoke `run` without qualification:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> run(new ArithmeticSuite)</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED ***
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * }}}
 *
 * ==Configuring a single run==
 *
 * To configure a single run, you can prefix run by one or more configuration commands, separated by dots. For example, to enable
 * durations during a single run, you would write:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> durations.run(new ArithmeticSuite)</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works (102 milliseconds)</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED *** (36 milliseconds)
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * }}}
 *
 * To enable statistics during a single run, you would write:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> stats.run(new ArithmeticSuite)</span>
 * <span style="color: #00dddd">Run starting. Expected test count is: 3</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED ***
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * <span style="color: #00dddd">Run completed in 386 milliseconds.
 * Total number of tests run: 2
 * Suites: completed 1, aborted 0
 * Tests: succeeded 1, failed 1, ignored 1, pending 1</span>
 * <span style="color: #dd2233">*** 1 TEST FAILED ***</span>
 * }}}
 *
 * And to enable both durations and statistics during a single run, you could write:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> durations.stats.run(new ArithmeticSuite)</span>
 * <span style="color: #00dddd">Run starting. Expected test count is: 3</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works (102 milliseconds)</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED (36 milliseconds)***
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * <span style="color: #00dddd">Run completed in 386 milliseconds.
 * Total number of tests run: 2
 * Suites: completed 1, aborted 0
 * Tests: succeeded 1, failed 1, ignored 1, pending 1</span>
 * <span style="color: #dd2233">*** 1 TEST FAILED ***</span>
 * }}}
 *
 * The order doesn't matter when you are chaining multiple configuration commands. You'll get the same
 * result whether you write `durations.stats.run` or `stats.durations.run`.
 * 
 *
 * To disable color, use `nocolor`:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> nocolor.run(new ArithmeticSuite)
 * ArithmeticSuite:
 * - addition works
 * - subtraction works !!! IGNORED !!!
 * - multiplication works *** FAILED ***
 *   1 did not equal 2 (<console>:16)
 * - division works (pending)</span>
 * }}}
 *
 * To enable short stack traces during a single run, use `shortstacks`:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> shortstacks.run(new ArithmeticSuite)</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works (101 milliseconds)</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED *** (33 milliseconds)
 *   1 did not equal 2 (<console>:16)
 *   org.scalatest.exceptions.TestFailedException:
 *   ...
 *   at line2$object$$iw$$iw$$iw$$iw$ArithmeticSuite$$anonfun$3.apply$mcV$sp(<console>:16)
 *   at line2$object$$iw$$iw$$iw$$iw$ArithmeticSuite$$anonfun$3.apply(<console>:16)
 *   at line2$object$$iw$$iw$$iw$$iw$ArithmeticSuite$$anonfun$3.apply(<console>:16)
 *   at org.scalatest.FunSuite$$anon$1.apply(FunSuite.scala:992)
 *   at org.scalatest.Suite$class.withFixture(Suite.scala:1661)
 *   at line2$object$$iw$$iw$$iw$$iw$ArithmeticSuite.withFixture(<console>:8)
 *   at org.scalatest.FunSuite$class.invokeWithFixture$1(FunSuite.scala:989)
 *   ...</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * }}}
 *
 * ==Changing the default configuration==
 *
 * If you want to change the default for multiple runs, you can import the members of your favorite `Shell` configuration. For example,
 * if you ''always'' like to run with durations and statistics enabled, you could write:
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> import stats.durations._
 * import stats.durations._</span>
 * }}}
 *
 * Now anytime you run statistics and durations will, by default, be enabled:
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> run(new ArithmeticSuite)</span>
 * <span style="color: #00dddd">Run starting. Expected test count is: 3</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works (9 milliseconds)</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED *** (10 milliseconds)
 *   1 did not equal 2 (<console>:18)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * <span style="color: #00dddd">Run completed in 56 milliseconds.
 * Total number of tests run: 2
 * Suites: completed 1, aborted 0
 * Tests: succeeded 1, failed 1, ignored 1, pending 1</span>
 * <span style="color: #dd2233">*** 1 TEST FAILED ***</span>
 * }}}
 *
 * ==Running multiple suites==
 *
 * If you want to run multiple suites, you can use the factory method in the <a href="Suites$.html">`Suites`</a> 
 * singleton object. If you wrap a comma-separated list of suite instances inside `Suites(...)`, for example,
 * you'll get a suite instance that contains no tests, but whose nested suites includes the suite instances you placed between
 * the parentheses. You can place `Suites` inside `Suites` to any level of depth, creating a tree of
 * suites to pass to `run`. Here's a (contrived) example in which `ArithmeticSuite` is executed four times:
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> run(Suites(new ArithmeticSuite, new ArithmeticSuite, Suites(new ArithmeticSuite, new ArithmeticSuite)))</span>
 * <span style="color: #00dddd">Run starting. Expected test count is: 12</span>
 * <span style="color: #00cc00">Suites:
 * ArithmeticSuite:
 * - addition works (0 milliseconds)</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED *** (1 millisecond)
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works (1 millisecond)</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED *** (0 milliseconds)
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * <span style="color: #00cc00">Suites:
 * ArithmeticSuite:
 * - addition works (0 milliseconds)</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED *** (0 milliseconds)
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works (0 milliseconds)</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED *** (0 milliseconds)
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * <span style="color: #00dddd">Run completed in 144 milliseconds.
 * Total number of tests run: 8
 * Suites: completed 6, aborted 0
 * Tests: succeeded 4, failed 4, ignored 4, pending 4</span>
 * <span style="color: #dd2233">*** 4 TESTS FAILED ***</span>
 * }}}
 * 
 * ==Running a single test==
 *
 * The `run` command also allows you to specify the name of a test to run and/or a config map. You can run
 * a particular test in a suite, for example, by specifying the test name after the suite instance in your call to `run`, like this:
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala> run(new ArithmeticSuite, "addition works")</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works</span>
 * }}}
 */
sealed trait Shell {

  /**
   * A `Shell` whose `run` method will pass `true` for `execute`'s `color`
   * parameter, and pass for all other parameters the same values as this `Shell`.
   */
  val color: Shell

  /**
   * A `Shell` whose `run` method will pass `true` for `execute`'s `durations`
   * parameter, and pass for all other parameters the same values as this `Shell`.
   */
  val durations: Shell

  /**
   * A `Shell` whose `run` method will pass `true` for `execute`'s `shortstacks`
   * parameter and `false` for its `fullstacks` parameter, and pass for all other parameters the same values as
   * this `Shell`.
   */
  val shortstacks: Shell

  /**
   * A `Shell` whose `run` method will pass `false` for `execute`'s `shortstacks`
   * parameter and `true` for its `fullstacks` parameter, and pass for all other parameters the same values as this `Shell`.
   */
  val fullstacks: Shell

  /**
   * A `Shell` whose `run` method will pass `true` for `execute`'s `stats`
   * parameter, and pass for all other parameters the same values as this `Shell`.
   */
  val stats: Shell

  /**
   * Returns a copy of this `Shell` with `colorPassed` configuration parameter set to `false`.
   */
  val nocolor: Shell

  /**
   * Returns a copy of this `Shell` with `durationsPassed` configuration parameter set to `false`.
   */
  val nodurations: Shell

  /**
   * Returns a copy of this `Shell` with `shortStacksPassed` configuration parameter set to `false`.
   */
  val nostacks: Shell

  /**
   * Returns a copy of this `Shell` with `statsPassed` configuration parameter set to `false`.
   */
  val nostats: Shell

  /**
   * Run the passed suite, optionally passing in a test name and config map. 
   *
   * This method will invoke `execute` on the passed `suite`, passing in
   * the specified (or default) `testName` and `configMap` and a set of configuration values. A
   * particular `Shell` instance will always pass the same configuration values (`color`,
   * `durations`, `shortstacks`, `fullstacks`, and `stats`) to `execute` each time
   * this method is invoked.
   * 
   */
  def run(suite: Suite, testName: String = null, configMap: ConfigMap = ConfigMap.empty): Unit
}

// parameters were private, but after pulling out the trait so I don't import copy() as part
// of the package object, I made the whole case class private[scalatest], so I made these normal
// so that I could write some tests against it.
private[scalatest] final case class ShellImpl(
  colorPassed: Boolean = true,
  durationsPassed: Boolean = false,
  shortstacksPassed: Boolean = false,
  fullstacksPassed: Boolean = false,
  statsPassed: Boolean = false
) extends Shell {

  lazy val color: Shell = copy(colorPassed = true)
  lazy val durations: Shell = copy(durationsPassed = true)
  lazy val shortstacks: Shell = copy(shortstacksPassed = true, fullstacksPassed = false)
  lazy val fullstacks: Shell = copy(fullstacksPassed = true, shortstacksPassed = false)
  lazy val stats: Shell = copy(statsPassed = true)
  lazy val nocolor: Shell = copy(colorPassed = false)
  lazy val nodurations: Shell = copy(durationsPassed = false)
  lazy val nostacks: Shell = copy(shortstacksPassed = false, fullstacksPassed = false)
  lazy val nostats: Shell = copy(statsPassed = false)

  def run(suite: Suite, testName: String = null, configMap: ConfigMap = ConfigMap.empty): Unit = {
    suite.execute(testName, configMap, colorPassed, durationsPassed, shortstacksPassed, fullstacksPassed, statsPassed)
  }
}
