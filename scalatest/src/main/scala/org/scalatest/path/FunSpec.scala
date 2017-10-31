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
package org.scalatest.path

import org.scalatest._

/**
 * A sister class to `org.scalatest.FunSpec` that isolates tests by running each test in its own
 * instance of the test class, and for each test, only executing the ''path'' leading to that test.
 *
 * Class `path.FunSpec` behaves similarly to class `org.scalatest.FunSpec`, except that tests
 * are isolated based on their path. The purpose of `path.FunSpec` is to facilitate writing
 * specification-style tests for mutable objects in a clear, boilerpate-free way. To test mutable objects, you need to
 * mutate them. Using a path class, you can make a statement in text, then implement that statement in code (including
 * mutating state), and nest and combine these test/code pairs in any way you wish. Each test will only see
 * the side effects of code that is in blocks that enclose the test. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.path
 * import org.scalatest.matchers.Matchers
 * import scala.collection.mutable.ListBuffer
 *
 * class ExampleSpec extends path.FunSpec with Matchers {
 *
 *   describe("A ListBuffer") {
 *
 *     val buf = ListBuffer.empty[Int] // This implements "A ListBuffer"
 *
 *     it("should be empty when created") {
 *
 *       // This test sees:
 *       //   val buf = ListBuffer.empty[Int]
 *       // So buf is: ListBuffer()
 *
 *       buf should be ('empty)
 *     }
 *
 *     describe("when 1 is appended") {
 *
 *       buf += 1 // This implements "when 1 is appended", etc...
 *
 *       it("should contain 1") {
 *
 *         // This test sees:
 *         //   val buf = ListBuffer.empty[Int]
 *         //   buf += 1
 *         // So buf is: ListBuffer(1)
 *
 *         buf.remove(0) should equal (1)
 *         buf should be ('empty)
 *       }
 *
 *       describe("when 2 is appended") {
 *
 *         buf += 2
 *
 *         it("should contain 1 and 2") {
 *
 *           // This test sees:
 *           //   val buf = ListBuffer.empty[Int]
 *           //   buf += 1
 *           //   buf += 2
 *           // So buf is: ListBuffer(1, 2)
 *
 *           buf.remove(0) should equal (1)
 *           buf.remove(0) should equal (2)
 *           buf should be ('empty)
 *         }
 *
 *         describe("when 2 is removed") {
 *
 *           buf -= 2
 *
 *           it("should contain only 1 again") {
 *
 *             // This test sees:
 *             //   val buf = ListBuffer.empty[Int]
 *             //   buf += 1
 *             //   buf += 2
 *             //   buf -= 2
 *             // So buf is: ListBuffer(1)
 *
 *             buf.remove(0) should equal (1)
 *             buf should be ('empty)
 *           }
 *         }
 *
 *         describe("when 3 is appended") {
 *
 *           buf += 3
 *
 *           it("should contain 1, 2, and 3") {
 *
 *             // This test sees:
 *             //   val buf = ListBuffer.empty[Int]
 *             //   buf += 1
 *             //   buf += 2
 *             //   buf += 3
 *             // So buf is: ListBuffer(1, 2, 3)
 *
 *             buf.remove(0) should equal (1)
 *             buf.remove(0) should equal (2)
 *             buf.remove(0) should equal (3)
 *             buf should be ('empty)
 *           }
 *         }
 *       }
 *
 *       describe("when 88 is appended") {
 *
 *         buf += 88
 *
 *         it("should contain 1 and 88") {
 *
 *           // This test sees:
 *           //   val buf = ListBuffer.empty[Int]
 *           //   buf += 1
 *           //   buf += 88
 *           // So buf is: ListBuffer(1, 88)
 *
 *           buf.remove(0) should equal (1)
 *           buf.remove(0) should equal (88)
 *           buf should be ('empty)
 *         }
 *       }
 *     }
 *
 *     it("should have size 0 when created") {
 *
 *       // This test sees:
 *       //   val buf = ListBuffer.empty[Int]
 *       // So buf is: ListBuffer()
 *
 *       buf should have size 0
 *     }
 *   }
 * }
 * }}}
 *
 * Note that the above class is organized by writing a bit of specification text that opens a new block followed
 * by, at the top of the new block, some code that "implements" or "performs" what is described in the text. This is repeated as
 * the mutable object (here, a `ListBuffer`), is prepared for the enclosed tests. For example:
 *
 * {{{  <!-- class="stHighlight" -->
 * describe("A ListBuffer") {
 *   val buf = ListBuffer.empty[Int]
 * }}}
 *
 * Or:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * describe("when 2 is appended") {
 *   buf += 2
 * }}}
 *
 * Note also that although each test mutates the `ListBuffer`, none of the other tests observe those
 * side effects:
 *
 * {{{  <!-- class="stHighlight" -->
 * it("should contain 1") {
 *
 *   buf.remove(0) should equal (1)
 *   // ...
 * }
 *
 * describe("when 2 is appended") {
 *
 *   buf += 2
 *
 *   it("should contain 1 and 2") {
 *
 *     // This test does not see the buf.remove(0) from the previous test,
 *     // so the first element in the ListBuffer is again 1
 *     buf.remove(0) should equal (1)
 *     buf.remove(0) should equal (2)
 * }}}
 *
 * This kind of isolation of tests from each other is a consequence of running each test in its own instance of the test
 * class, and can also be achieved by simply mixing `OneInstancePerTest` into a regular
 * `org.scalatest.FunSpec`. However, `path.FunSpec` takes isolation one step further: a test
 * in a `path.FunSpec` does not observe side effects performed outside tests in earlier blocks that do not
 * enclose it. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * describe("when 2 is removed") {
 *
 *   buf -= 2
 *
 *   // ...
 * }
 *
 * describe("when 3 is appended") {
 *
 *   buf += 3
 *
 *   it("should contain 1, 2, and 3") {
 *
 *     // This test does not see the buf -= 2 from the earlier "when 2 is removed" block,
 *     // because that block does not enclose this test, so the second element in the
 *     // ListBuffer is still 2
 *     buf.remove(0) should equal (1)
 *     buf.remove(0) should equal (2)
 *     buf.remove(0) should equal (3)
 * }}}
 *
 * Running the full `ExampleSpec`, shown above, in the Scala interpeter would give you:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala> import org.scalatest._
 * import org.scalatest._
 *
 * scala> run(new ExampleSpec)
 * <span class="stGreen">ExampleSpec:
 * A ListBuffer
 * - should be empty when created
 * &nbsp; when 1 is appended
 * &nbsp; - should contain 1
 * &nbsp;   when 2 is appended
 * &nbsp;   - should contain 1 and 2
 * &nbsp;     when 2 is removed
 * &nbsp;     - should contain only 1 again
 * &nbsp;     when 3 is appended
 * &nbsp;     - should contain 1, 2, and 3
 * &nbsp;   when 88 is appended
 * &nbsp;   - should contain 1 and 88
 * - should have size 0 when created</span>
 * }}}
 *
 * ''Note: class `path.FunSpec`'s approach to isolation was inspired in part by the
 * <a href="https://github.com/orfjackal/specsy">specsy</a> framework, written by Esko Luontola.''
 * 
 *
 * <a name="sharedFixtures"></a>==Shared fixtures==
 *
 * A test ''fixture'' is objects or other artifacts (such as files, sockets, database
 * connections, ''etc.'') used by tests to do their work.
 * If a fixture is used by only one test, then the definitions of the fixture objects can
 * be local to the method. If multiple tests need to share an immutable fixture, you can simply
 * assign them to instance variables. If multiple tests need to share mutable fixture objects or `var`s,
 * there's one and only one way to do it in a `path.FunSpec`: place the mutable objects lexically before
 * the test. Any mutations needed by the test must be placed lexically before and/or after the test.
 * As used here, "Lexically before" means that the code needs to be executed during construction of that test's
 * instance of the test class to ''reach'' the test (or put another way, the
 * code is along the "path to the test.") "Lexically after" means that the code needs to be executed to exit the
 * constructor after the test has been executed.
 * 
 *
 * The reason lexical placement is the one and only one way to share fixtures in a `path.FunSpec` is because
 * all of its lifecycle methods are overridden and declared `final`. Thus you can't mix in `BeforeAndAfter` or
 * `BeforeAndAfterEach`, because both override `runTest`, which is `final` in
 * a `path.FunSpec`. You also can't override `withFixture`, because `path.FreeSpec`
 * extends <a href="../Suite.html">`Suite`</a> not <a href="../TestSuite.html">`TestSuite`</a>,
 * where `withFixture` is defined. In short:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black; width: 70%; margin: auto">
 * <tr>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 15px; text-align: left; border: 1px solid black; font-size: 125%; font-weight: bold">
 * In a `path.FunSpec`, if you need some code to execute before a test, place that code lexically before
 * the test. If you need some code to execute after a test, place that code lexically after the test.
 * </th>
 * </tr>
 * </table>
 * 
 *
 * The reason the life cycle methods are final, by the way, is to prevent users from attempting to combine
 * a `path.FunSpec`'s approach to isolation with other ways ScalaTest provides to share fixtures or
 * execute tests, because doing so could make the resulting test code hard to reason about. A
 * `path.FunSpec`'s execution model is a bit magical, but because it executes in one and only one
 * way, users should be able to reason about the code.
 * To help you visualize how a `path.FunSpec` is executed, consider the following variant of
 * `ExampleSpec` that includes print statements:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.path
 * import org.scalatest.matchers.Matchers
 * import scala.collection.mutable.ListBuffer
 *
 * class ExampleSpec extends path.FunSpec with Matchers {
 *
 *   println("Start of: ExampleSpec")
 *   describe("A ListBuffer") {
 *
 *     println("Start of: A ListBuffer")
 *     val buf = ListBuffer.empty[Int]
 *
 *     it("should be empty when created") {
 *
 *       println("In test: should be empty when created; buf is: " + buf)
 *       buf should be ('empty)
 *     }
 *
 *     describe("when 1 is appended") {
 *
 *       println("Start of: when 1 is appended")
 *       buf += 1
 *
 *       it("should contain 1") {
 *
 *         println("In test: should contain 1; buf is: " + buf)
 *         buf.remove(0) should equal (1)
 *         buf should be ('empty)
 *       }
 *
 *       describe("when 2 is appended") {
 *
 *         println("Start of: when 2 is appended")
 *         buf += 2
 *
 *         it("should contain 1 and 2") {
 *
 *           println("In test: should contain 1 and 2; buf is: " + buf)
 *           buf.remove(0) should equal (1)
 *           buf.remove(0) should equal (2)
 *           buf should be ('empty)
 *         }
 *
 *         describe("when 2 is removed") {
 *
 *           println("Start of: when 2 is removed")
 *           buf -= 2
 *
 *           it("should contain only 1 again") {
 *
 *             println("In test: should contain only 1 again; buf is: " + buf)
 *             buf.remove(0) should equal (1)
 *             buf should be ('empty)
 *           }
 *
 *           println("End of: when 2 is removed")
 *         }
 *
 *         describe("when 3 is appended") {
 *
 *           println("Start of: when 3 is appended")
 *           buf += 3
 *
 *           it("should contain 1, 2, and 3") {
 *
 *             println("In test: should contain 1, 2, and 3; buf is: " + buf)
 *             buf.remove(0) should equal (1)
 *             buf.remove(0) should equal (2)
 *             buf.remove(0) should equal (3)
 *             buf should be ('empty)
 *           }
 *           println("End of: when 3 is appended")
 *         }
 *
 *         println("End of: when 2 is appended")
 *       }
 *
 *       describe("when 88 is appended") {
 *
 *         println("Start of: when 88 is appended")
 *         buf += 88
 *
 *         it("should contain 1 and 88") {
 *
 *           println("In test: should contain 1 and 88; buf is: " + buf)
 *           buf.remove(0) should equal (1)
 *           buf.remove(0) should equal (88)
 *           buf should be ('empty)
 *         }
 *
 *         println("End of: when 88 is appended")
 *       }
 *
 *       println("End of: when 1 is appended")
 *     }
 *
 *     it("should have size 0 when created") {
 *
 *       println("In test: should have size 0 when created; buf is: " + buf)
 *       buf should have size 0
 *     }
 *
 *     println("End of: A ListBuffer")
 *   }
 *   println("End of: ExampleSpec")
 *   println()
 * }
 * }}}
 *
 * Running the above version of `ExampleSpec` in the Scala interpreter will give you output similar to:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala> import org.scalatest._
 * import org.scalatest._
 *
 * scala> run(new ExampleSpec)
 * <span class="stGreen">ExampleSpec:</span>
 * Start of: ExampleSpec
 * Start of: A ListBuffer
 * In test: should be empty when created; buf is: ListBuffer()
 * End of: A ListBuffer
 * End of: ExampleSpec
 *
 * Start of: ExampleSpec
 * Start of: A ListBuffer
 * Start of: when 1 is appended
 * In test: should contain 1; buf is: ListBuffer(1)
 * ExampleSpec:
 * End of: when 1 is appended
 * End of: A ListBuffer
 * End of: ExampleSpec
 *
 * Start of: ExampleSpec
 * Start of: A ListBuffer
 * Start of: when 1 is appended
 * Start of: when 2 is appended
 * In test: should contain 1 and 2; buf is: ListBuffer(1, 2)
 * End of: when 2 is appended
 * End of: when 1 is appended
 * End of: A ListBuffer
 * End of: ExampleSpec
 *
 * Start of: ExampleSpec
 * Start of: A ListBuffer
 * Start of: when 1 is appended
 * Start of: when 2 is appended
 * Start of: when 2 is removed
 * In test: should contain only 1 again; buf is: ListBuffer(1)
 * End of: when 2 is removed
 * End of: when 2 is appended
 * End of: when 1 is appended
 * End of: A ListBuffer
 * End of: ExampleSpec
 *
 * Start of: ExampleSpec
 * Start of: A ListBuffer
 * Start of: when 1 is appended
 * Start of: when 2 is appended
 * Start of: when 3 is appended
 * In test: should contain 1, 2, and 3; buf is: ListBuffer(1, 2, 3)
 * End of: when 3 is appended
 * End of: when 2 is appended
 * End of: when 1 is appended
 * End of: A ListBuffer
 * End of: ExampleSpec
 *
 * Start of: ExampleSpec
 * Start of: A ListBuffer
 * Start of: when 1 is appended
 * Start of: when 88 is appended
 * In test: should contain 1 and 88; buf is: ListBuffer(1, 88)
 * End of: when 88 is appended
 * End of: when 1 is appended
 * End of: A ListBuffer
 * End of: ExampleSpec
 *
 * Start of: ExampleSpec
 * Start of: A ListBuffer
 * In test: should have size 0 when created; buf is: ListBuffer()
 * End of: A ListBuffer
 * End of: ExampleSpec
 *
 * <span class="stGreen">A ListBuffer
 * - should be empty when created
 *   when 1 is appended
 * &nbsp; - should contain 1
 * &nbsp;   when 2 is appended
 * &nbsp;   - should contain 1 and 2
 * &nbsp;     when 2 is removed
 * &nbsp;     - should contain only 1 again
 * &nbsp;     when 3 is appended
 * &nbsp;     - should contain 1, 2, and 3
 * &nbsp;   when 88 is appended
 * &nbsp;   - should contain 1 and 88
 * - should have size 0 when created</span>
 * }}}
 *
 * Note that each test is executed in order of appearance in the `path.FunSpec`, and that only
 * those `println` statements residing in blocks that enclose the test being run are executed. Any
 * `println` statements in blocks that do not form the "path" to a test are not executed in the
 * instance of the class that executes that test.
 * 
 *
 * <a name="howItExecutes"></a>
 * ==How it executes==
 *
 * To provide its special brand of test isolation, `path.FunSpec` executes quite differently from its
 * sister class in `org.scalatest`. An `org.scalatest.FunSpec`
 * registers tests during construction and executes them when `run` is invoked. An
 * `org.scalatest.path.FunSpec`, by contrast, runs each test in its own instance ''while that
 * instance is being constructed''. During construction, it registers not the tests to run, but the results of
 * running those tests. When `run` is invoked on a `path.FunSpec`, it reports the registered
 * results and does not run the tests again. If `run` is invoked a second or third time, in fact,
 * a `path.FunSpec` will each time report the same results registered during construction. If you want
 * to run the tests of a `path.FunSpec` anew, you'll need to create a new instance and invoke
 * `run` on that.
 *
 * A `path.FunSpec` will create one instance for each "leaf" node it contains. The main kind of leaf node is
 * a test, such as:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // One instance will be created for each test
 * it("should be empty when created") {
 *   buf should be ('empty)
 * }
 * }}}
 *
 * However, an empty scope (a scope that contains no tests or nested scopes) is also a leaf node:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 *  // One instance will be created for each empty scope
 * describe("when 99 is added") {
 *   // A scope is "empty" and therefore a leaf node if it has no
 *   // tests or nested scopes, though it may have other code (which
 *   // will be executed in the instance created for that leaf node)
 *   buf += 99
 * }
 * }}}
 *
 * The tests will be executed sequentially, in the order of appearance. The first test (or empty scope,
 * if that is first) will be executed when a class that mixes in `path.FunSpec` is
 * instantiated. Only the first test will be executed during this initial instance, and of course, only
 * the path to that test. Then, the first time the client uses the initial instance (by invoking one of `run`,
 * `expectedTestsCount`, `tags`, or `testNames` on the instance), the initial instance will,
 * before doing anything else, ensure that any remaining tests are executed, each in its own instance.
 * 
 *
 * To ensure that the correct path is taken in each instance, and to register its test results, the initial
 * `path.FunSpec` instance must communicate with the other instances it creates for running any subsequent
 * leaf nodes. It does so by setting a thread-local variable prior to creating each instance (a technique
 * suggested by Esko Luontola). Each instance
 * of `path.FunSpec` checks the thread-local variable. If the thread-local is not set, it knows it
 * is an initial instance and therefore executes every block it encounters until it discovers, and executes the
 * first test (or empty scope, if that's the first leaf node). It then discovers, but does not execute the next
 * leaf node, or discovers there are no other leaf nodes remaining to execute. It communicates the path to the next
 * leaf node, if any, and the result of running the test it did execute, if any, back to the initial instance. The
 * initial instance repeats this process until all leaf nodes have been executed and all test results registered.
 * 
 *
 * <a name="ignoredTests"></a>
 * ==Ignored tests==
 *
 * You mark a test as ignored in an `org.scalatest.path.FunSpec` in the same manner as in
 * an `org.scalatest.FunSpec`. Please see the <a href="../FunSpec.html#ignoredTests">Ignored tests</a> section
 * in its documentation for more information.
 * 
 *
 * Note that a separate instance will be created for an ignored test,
 * and the path to the ignored test will be executed in that instance, but the test function itself will not
 * be executed. Instead, a `TestIgnored` event will be fired.
 * 
 *
 * <a name="informers"></a>
 * ==Informers==
 *
 * You output information using `Informer`s in an `org.scalatest.path.FunSpec` in the same manner
 * as in an `org.scalatest.FunSpec`. Please see the <a href="../FunSpec.html#informers">Informers</a>
 * section in its documentation for more information.
 * 
 *
 * <a name="pendingTests"></a>
 * ==Pending tests==
 *
 * You mark a test as pending in an `org.scalatest.path.FunSpec` in the same manner as in
 * an `org.scalatest.FunSpec`. Please see the <a href="../FunSpec.html#pendingTests">Pending tests</a>
 * section in its documentation for more information.
 * 
 * 
 * Note that a separate instance will be created for a pending test,
 * and the path to the ignored test will be executed in that instance, as well as the test function (up until it
 * completes abruptly with a `TestPendingException`).
 * 
 *
 * <a name="taggingTests"></a>
 * ==Tagging tests==
 *
 * You can place tests into groups by tagging them in an `org.scalatest.path.FunSpec` in the same manner
 * as in an `org.scalatest.FunSpec`. Please see the <a href="../FunSpec.html#taggingTests">Tagging tests</a>
 * section in its documentation for more information.
 * 
 *
 * Note that one difference between this class and its sister class
 * `org.scalatest.FunSpec` is that because tests are executed at construction time, rather than each
 * time run is invoked, an `org.scalatest.path.FunSpec` will always execute all non-ignored tests. When
 * `run` is invoked on a `path.FunSpec`, if some tests are excluded based on tags, the registered
 * results of running those tests will not be reported. (But those tests will have already run and the results
 * registered.) By contrast, because an `org.scalatest.FunSpec` only executes tests after `run`
 * has been called, and at that time the tags to include and exclude are known, only tests selected by the tags
 * will be executed.
 * 
 * 
 * In short, in an `org.scalatest.FunSpec`, tests not selected by the tags to include
 * and exclude specified for the run (via the `Filter` passed to `run`) will not be executed.
 * In an `org.scalatest.path.FunSpec`, by contrast, all non-ignored tests will be executed, each
 * during the construction of its own instance, and tests not selected by the tags to include and exclude specified
 * for a run will not be reported. (One upshot of this is that if you have tests that you want to tag as being slow so
 * you can sometimes exclude them during a run, you probably don't want to put them in a `path.FunSpec`. Because
 * in a `path.Freespec` the slow tests will be run regardless, with only their registered results not being ''reported''
 * if you exclude slow tests during a run.)
 * 
 *
 * <a name="SharedTests"></a>==Shared tests==
 * You can factor out shared tests in an `org.scalatest.path.FunSpec` in the same manner as in
 * an `org.scalatest.FunSpec`. Please see the <a href="../FunSpec.html#SharedTests">Shared tests</a>
 * section in its documentation for more information.
 * 
 *
 * <a name="nestedSuites"></a>==Nested suites==
 *
 * Nested suites are not allowed in a `path.FunSpec`. Because
 * a `path.FunSpec` executes tests eagerly at construction time, registering the results of those test runs
 * and reporting them later when `run` is invoked, the order of nested suites versus test runs would be
 * different in a `org.scalatest.path.FunSpec` than in an `org.scalatest.FunSpec`. In
 * `org.scalatest.FunSpec`'s implementation of `run`, nested suites are executed then tests
 * are executed. A `org.scalatest.path.FunSpec` with nested suites would execute these in the opposite
 * order: first tests then nested suites. To help make `path.FunSpec` code easier to
 * reason about by giving readers of one less difference to think about, nested suites are not allowed. If you want
 * to add nested suites to a `path.FunSpec`, you can instead wrap them all in a
 * <a href="../Suites.html">`Suites`</a> object. They will
 * be executed in the order of appearance (unless a <a href="../Distributor">Distributor</a> is passed, in which case
 * they will execute in parallel).
 * 

 * 
 *
 * <a name="durations"></a>==Durations==
 * Many ScalaTest events include a duration that indicates how long the event being reported took to execute. For
 * example, a `TestSucceeded` event provides a duration indicating how long it took for that test
 * to execute. A `SuiteCompleted` event provides a duration indicating how long it took for that entire
 * suite of tests to execute.
 * 
 *
 * In the test completion events fired by a `path.FunSpec` (`TestSucceeded`,
 * `TestFailed`, or `TestPending`), the durations reported refer
 * to the time it took for the tests to run. This time is registered with the test results and reported along
 * with the test results each time `run` is invoked.
 * By contrast, the suite completion events fired for a `path.FunSpec` represent the amount of time
 * it took to report the registered results. (These events are not fired by `path.FunSpec`, but instead
 * by the entity that invokes `run` on the `path.FunSpec`.) As a result, the total time
 * for running the tests of a `path.FunSpec`, calculated by summing the durations of all the individual
 * test completion events, may be greater than the duration reported for executing the entire suite.
 * 
 *
 * @author Bill Venners
 * @author Chua Chee Seng
 */
@Finders(Array("org.scalatest.finders.FunSpecFinder"))
// SKIP-SCALATESTJS-START
class FunSpec extends FunSpecLike {
// SKIP-SCALATESTJS-END
//SCALATESTJS-ONLY abstract class FunSpec extends FunSpecLike {

  /**
   * Returns a user friendly string for this suite, composed of the
   * simple name of the class (possibly simplified further by removing dollar signs if added by the Scala interpeter) and, if this suite
   * contains nested suites, the result of invoking `toString` on each
   * of the nested suites, separated by commas and surrounded by parentheses.
   *
   * @return a user-friendly string for this suite
   */
  override def toString: String = Suite.suiteToString(None, this)
}


