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

/**
 * Singleton object providing an `apply` method for the ScalaTest shell and a
 * `main` method for ScalaTest's simple runner.
 *
 * The `apply` method can be used in the ScalaTest Shell (its DSL for the Scala
 * interpreter) in this way:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; class ArithmeticSuite extends FunSuite with Matchers {
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
 * defined class ArithmeticSuite
 *
 * scala&gt; run(new ArithmeticSuite)</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED ***
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * }}}
 *
 * The last command is calling the `apply` method on the `run` singleton object. In other
 * words, you could alternatively call it this way:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">scala&gt; run.apply(new ArithmeticSuite)</span>
 * <span style="color: #00cc00">ArithmeticSuite:
 * - addition works</span>
 * <span style="color: #cfc923">- subtraction works !!! IGNORED !!!</span>
 * <span style="color: #dd2233">- multiplication works *** FAILED ***
 *   1 did not equal 2 (<console>:16)</span>
 * <span style="color: #cfc923">- division works (pending)</span>
 * }}}
 *
 * The `run` singleton object also serves a different purpose. Its `main` method
 * allows users to "run" `run` as a Scala application. ScalaTest's <a href="tools/Runner$.html">`Runner`</a> application is very
 * powerful, but doesn't provide the simplest out-of-box experience for people trying ScalaTest for the first time. For example,
 * to run an `ExampleSpec` in the unnamed package from the directory where it is compiled with
 * `Runner`'s standard out reporter requires this command:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">$ scala -cp scalatest-RELEASE.jar org.scalatest.tools.Runner -R . -o -s ExampleSpec</span>
 * }}}
 *
 * Running it with the `run` application is simpler:
 * 
 *
 * {{{ style="background-color: #2c415c; padding: 10px">
 * <span style="color: white">$ scala -cp scalatest-RELEASE.jar org.scalatest.run ExampleSpec</span>
 * }}}
 *
 *
 */
object run {

  private val defaultShell = ShellImpl()

  /**
   * Run the suites whose fully qualified names are passed as arguments.
   *
   * This method will invoke the main method of `org.scalatest.tools.Runner`, passing
   * in `"-R ."` to set the runpath to the current directory, `"-o"` to select the
   * standard out reporter, and each argument preceded by `-s`. For example, this `run`
   * command:
   * 
   *
   * {{{ style="background-color: #2c415c; padding: 10px">
   * <span style="color: white">$ scala -cp scalatest-RELEASE.jar org.scalatest.run ExampleSpec</span>
   * }}}
   *
   * Has the same effect as this `Runner` command:
   * 
   *
   * {{{ style="background-color: #2c415c; padding: 10px">
   * <span style="color: white">$ scala -cp scalatest-RELEASE.jar org.scalatest.tools.Runner -R . -o -s ExampleSpec</span>
   * }}}
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {
    tools.Runner.main(Array("-R", ".", "-o") ++ args.flatMap(s => Array("-s", s)))
  }

  /**
   * Run the passed suite, optionally passing in a test name and config map. 
   *
   * This method will invoke `execute` on the passed `suite`, passing in
   * the specified (or default) `testName` and `configMap` and the configuration values
   * passed to this `Shell`'s constructor (`colorPassed`, `durationsPassed`, `shortStacksPassed`,
   * `fullStacksPassed`, and `statsPassed`).
   * 
   */
  def apply(suite: Suite, testName: String = null, configMap: ConfigMap = ConfigMap.empty): Unit = {
    defaultShell.run(suite, testName, configMap)
  }
}
