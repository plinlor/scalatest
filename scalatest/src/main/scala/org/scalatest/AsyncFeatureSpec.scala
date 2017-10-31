/*
 * Copyright 2001-2014 Artima, Inc.
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
 * Enables testing of asynchronous code without blocking,
 * using a style consistent with traditional `FeatureSpec` tests.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * `AsyncFeatureSpec` is intended to enable users of <a href="FeatureSpec.html">`FeatureSpec`</a>
 * to write non-blocking asynchronous tests that are consistent with their traditional `FeatureSpec` tests. 
 * ''Note: `AsyncFeatureSpec` is intended for use in special situations where non-blocking asynchronous
 * testing is needed, with class `FeatureSpec` used for general needs.''
 * </td></tr></table>
 * 
 * Given a `Future` returned by the code you are testing,
 * you need not block until the `Future` completes before
 * performing assertions against its value. You can instead map those
 * assertions onto the `Future` and return the resulting
 * `Future[Assertion]` to ScalaTest. The test will complete
 * asynchronously, when the `Future[Assertion]` completes.
 * 
 *
 * Although not required, `FeatureSpec` is often used together with <a href="GivenWhenThen.html">`GivenWhenThen`</a> to express acceptance requirements
 * in more detail.
 * Here's an example `AsyncFeatureSpec`:
 * 
 *
 * <a name="initialExample"></a>
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec
 * 
 * import org.scalatest._
 * import scala.concurrent.Future
 * import scala.concurrent.ExecutionContext
 * 
 * // Defining actor messages
 * case object IsOn
 * case object PressPowerButton
 * 
 * class TVSetActor { // Simulating an actor
 *   private var on: Boolean = false
 *   def !(msg: PressPowerButton.type): Unit =
 *     synchronized {
 *       on = !on
 *     }
 *   def ?(msg: IsOn.type)(implicit c: ExecutionContext): Future[Boolean] =
 *     Future {
 *       synchronized { on }
 *     }
 * }
 * 
 * class TVSetActorSpec extends AsyncFeatureSpec with GivenWhenThen {
 * 
 *   implicit override def executionContext =
 *     scala.concurrent.ExecutionContext.Implicits.global
 * 
 *   info("As a TV set owner")
 *   info("I want to be able to turn the TV on and off")
 *   info("So I can watch TV when I want")
 *   info("And save energy when I'm not watching TV")
 * 
 *   Feature("TV power button") {
 *     Scenario("User presses power button when TV is off") {
 * 
 *       Given("a TV set that is switched off")
 *       val tvSetActor = new TVSetActor
 * 
 *       When("the power button is pressed")
 *       tvSetActor ! PressPowerButton
 * 
 *       Then("the TV should switch on")
 *       val futureBoolean = tvSetActor ? IsOn
 *       futureBoolean map { isOn =&gt; assert(isOn) }
 *     }
 * 
 *     Scenario("User presses power button when TV is on") {
 * 
 *       Given("a TV set that is switched on")
 *       val tvSetActor = new TVSetActor
 *       tvSetActor ! PressPowerButton
 * 
 *       When("the power button is pressed")
 *       tvSetActor ! PressPowerButton
 * 
 *       Then("the TV should switch off")
 *       val futureBoolean = tvSetActor ? IsOn
 *       futureBoolean map { isOn =&gt; assert(!isOn) }
 *     }
 *   }
 * }
 * }}}
 *
 * Note: for more information on the calls to `Given`, `When`, and `Then`, see the documentation 
 * for trait <a href="GivenWhenThen.html">`GivenWhenThen`</a> and the <a href="#informers">`Informers` section</a> below.
 * 
 *
 * An `AsyncFeatureSpec` contains ''feature clauses'' and ''scenarios''. You define a feature clause
 * with `feature`, and a scenario with `scenario`. Both
 * `feature` and `scenario` are methods, defined in
 * `AsyncFeatureSpec`, which will be invoked
 * by the primary constructor of `TVSetActorSpec`. 
 * A feature clause describes a feature of the ''subject'' (class or other entity) you are specifying
 * and testing. In the previous example, 
 * the subject under specification and test is a TV set. The feature being specified and tested is 
 * the behavior of a TV set when its power button is pressed. With each scenario you provide a
 * string (the ''spec text'') that specifies the behavior of the subject for
 * one scenario in which the feature may be used, and a block of code that tests that behavior.
 * You place the spec text between the parentheses, followed by the test code between curly
 * braces.  The test code will be wrapped up as a function passed as a by-name parameter to
 * `scenario`, which will register the test for later execution.
 * The result type of the by-name in an `AsyncFeatureSpec` must
 * be `Future[Assertion]`.
 * 
 *
 * Starting with version 3.0.0, ScalaTest assertions and matchers have result type `Assertion`.
 * The result type of the first test in the example above, therefore, is `Future[Assertion]`.
 * When an `AsyncFeatureSpec` is constructed, any test that results in `Assertion` will
 * be implicitly converted to `Future[Assertion]` and registered. The implicit conversion is from `Assertion`
 * to `Future[Assertion]` only, so you must end synchronous tests in some ScalaTest assertion
 * or matcher expression. If a test would not otherwise end in type `Assertion`, you can
 * place `succeed` at the end of the test. `succeed`, a field in trait `Assertions`,
 * returns the `Succeeded` singleton:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; succeed
 * res2: org.scalatest.Assertion = Succeeded
 * }}}
 *
 * Thus placing `succeed` at the end of a test body will satisfy the type checker.
 * 
 *
 * An `AsyncFeatureSpec`'s lifecycle has two phases: the ''registration'' phase and the
 * ''ready'' phase. It starts in registration phase and enters ready phase the first time
 * `run` is called on it. It then remains in ready phase for the remainder of its lifetime.
 * 
 *
 * Scenarios can only be registered with the `scenario` method while the `AsyncFeatureSpec` is
 * in its registration phase. Any attempt to register a scenario after the `AsyncFeatureSpec` has
 * entered its ready phase, ''i.e.'', after `run` has been invoked on the `AsyncFeatureSpec`,
 * will be met with a thrown <a href="exceptions/TestRegistrationClosedException.html">`TestRegistrationClosedException`</a>. The
 * recommended style
 * of using `AsyncFeatureSpec` is to register tests during object construction as is done in all
 * the examples shown here. If you keep to the recommended style, you should never see a
 * `TestRegistrationClosedException`.
 * 
 *
 * Each scenario represents one test. The name of the test is the spec text passed to the `scenario` method.
 * The feature name does not appear as part of the test name. In a `AsyncFeatureSpec`, therefore, you must take care
 * to ensure that each test has a unique name (in other words, that each `scenario` has unique spec text).
 * 
 *
 * When you run a `AsyncFeatureSpec`, it will send <a href="events/Formatter.html">`Formatter`</a>s in the events it sends to the
 * <a href="Reporter.html">`Reporter`</a>. ScalaTest's built-in reporters will report these events in such a way
 * that the output is easy to read as an informal specification of the ''subject'' being tested.
 * For example, were you to run `TVSetSpec` from within the Scala interpreter:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new TVSetActorSpec)
 * }}}
 *
 * You would see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">TVSetActorSpec:
 * As a TV set owner 
 * I want to be able to turn the TV on and off 
 * So I can watch TV when I want 
 * And save energy when I'm not watching TV 
 * Feature: TV power button
 *   Scenario: User presses power button when TV is off
 *     Given a TV set that is switched off 
 *     When the power button is pressed 
 *     Then the TV should switch on 
 *   Scenario: User presses power button when TV is on
 *     Given a TV set that is switched on 
 *     When the power button is pressed 
 *     Then the TV should switch off</span>
 * }}}
 *
 * Or, to run just the &ldquo;`Feature: TV power button Scenario: User presses power button when TV is on`&rdquo; method, you could pass that test's name, or any unique substring of the
 * name, such as `"TV is on"`. Here's an example:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new TVSetActorSpec, "TV is on")
 * <span class="stGreen">TVSetActorSpec:
 * As a TV set owner 
 * I want to be able to turn the TV on and off 
 * So I can watch TV when I want 
 * And save energy when I'm not watching TV 
 * Feature: TV power button
 *   Scenario: User presses power button when TV is on
 *     Given a TV set that is switched on 
 *     When the power button is pressed 
 *     Then the TV should switch off</span>
 * }}}
 *
 * <a name="asyncExecutionModel"></a>==Asynchronous execution model==
 *
 * `AsyncFeatureSpec` extends <a href="AsyncTestSuite.html">`AsyncTestSuite`</a>, which provides an
 * implicit `scala.concurrent.ExecutionContext`
 * named `executionContext`. This
 * execution context is used by `AsyncFeatureSpec` to 
 * transform the `Future[Assertion]`s returned by each test
 * into the <a href="FutureOutcome.html">`FutureOutcome`</a> returned by the `test` function
 * passed to `withFixture`.
 * This `ExecutionContext` is also intended to be used in the tests,
 * including when you map assertions onto futures.
 * 
 * 
 * On both the JVM and Scala.js, the default execution context provided by ScalaTest's asynchronous
 * testing styles confines execution to a single thread per test. On JavaScript, where single-threaded
 * execution is the only possibility, the default execution context is
 * `scala.scalajs.concurrent.JSExecutionContext.Implicits.queue`. On the JVM, 
 * the default execution context is a ''serial execution context'' provided by ScalaTest itself.
 * 
 * 
 * When ScalaTest's serial execution context is called upon to execute a task, that task is recorded
 * in a queue for later execution. For example, one task that will be placed in this queue is the
 * task that transforms the `Future[Assertion]` returned by an asynchronous test body
 * to the `FutureOutcome` returned from the `test` function.
 * Other tasks that will be queued are any transformations of, or callbacks registered on, `Future`s that occur
 * in your test body, including any assertions you map onto `Future`s. Once the test body returns,
 * the thread that executed the test body will execute the tasks in that queue one after another, in the order they
 * were enqueued.
 * 
 *
 * ScalaTest provides its serial execution context as the default on the JVM for three reasons. First, most often
 * running both tests and suites in parallel does not give a significant performance boost compared to
 * just running suites in parallel. Thus parallel execution of `Future` transformations within
 * individual tests is not generally needed for performance reasons.
 * 
 *
 * Second, if multiple threads are operating in the same suite
 * concurrently, you'll need to make sure access to any mutable fixture objects by multiple threads is synchronized.
 * Although access to mutable state along
 * the same linear chain of `Future` transformations need not be synchronized,
 * this does not hold true for callbacks, and in general it is easy to make a mistake. Simply put: synchronizing access to
 * shared mutable state is difficult and error prone.
 * Because ScalaTest's default execution context on the JVM confines execution of `Future` transformations
 * and call backs to a single thread, you need not (by default) worry about synchronizing access to mutable state
 * in your asynchronous-style tests.
 * 
 *
 * Third, asynchronous-style tests need not be complete when the test body returns, because the test body returns
 * a `Future[Assertion]`. This `Future[Assertion]` will often represent a test that has not yet
 * completed. As a result, when using a more traditional execution context backed by a thread-pool, you could
 * potentially start many more tests executing concurrently than there are threads in the thread pool. The more
 * concurrently execute tests you have competing for threads from the same limited thread pool, the more likely it
 * will be that tests will intermitently fail due to timeouts.
 * 
 * 
 * Using ScalaTest's serial execution context on the JVM will ensure the same thread that produced the `Future[Assertion]`
 * returned from a test body is also used to execute any tasks given to the execution context while executing the test
 * body&#8212;''and that thread will not be allowed to do anything else until the test completes.''
 * If the serial execution context's task queue ever becomes empty while the `Future[Assertion]` returned by
 * that test's body has not yet completed, the thread will ''block'' until another task for that test is enqueued. Although
 * it may seem counter-intuitive, this blocking behavior means the total number of tests allowed to run concurrently will be limited
 * to the total number of threads executing suites. This fact means you can tune the thread pool such that maximum performance
 * is reached while avoiding (or at least, reducing the likelihood of) tests that fail due to timeouts because of thread competition.
 * 
 *
 * This thread confinement strategy does mean, however, that when you are using the default execution context on the JVM, you
 * must be sure to ''never block'' in the test body waiting for a task to be completed by the
 * execution context. If you block, your test will never complete. This kind of problem will be obvious, because the test will
 * consistently hang every time you run it. (If a test is hanging, and you're not sure which one it is, 
 * enable <a href="Runner.scala#slowpokeNotifications">slowpoke notifications</a>.) If you really do 
 * want to block in your tests, you may wish to just use a
 * traditional <a href="FeatureSpec.html">`FeatureSpec`</a> with
 * <a href="concurrent/ScalaFutures.html">`ScalaFutures`</a> instead. Alternatively, you could override
 * the `executionContext` and use a traditional `ExecutionContext` backed by a thread pool. This
 * will enable you to block in an asynchronous-style test on the JVM, but you'll need to worry about synchronizing access to
 * shared mutable state.
 * 
 *
 * To use a different execution context, just override `executionContext`. For example, if you prefer to use
 * the `runNow` execution context on Scala.js instead of the default `queue`, you would write:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // on Scala.js
 * implicit override def executionContext =
 *     scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
 * }}}
 *
 * If you prefer on the JVM to use the global execution context, which is backed by a thread pool, instead of ScalaTest's default
 * serial execution contex, which confines execution to a single thread, you would write:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // on the JVM (and also compiles on Scala.js, giving
 * // you the queue execution context)
 * implicit override def executionContext =
 *     scala.concurrent.ExecutionContext.Implicits.global
 * }}}
 *
 * <a name="serialAndParallel"></a>==Serial and parallel test execution==
 *
 * By default (unless you mix in `ParallelTestExecution`), tests in an `AsyncFeatureSpec` will be executed one after
 * another, ''i.e.'', serially. This is true whether those tests return `Assertion` or `Future[Assertion]`,
 * no matter what threads are involved. This default behavior allows
 * you to re-use a shared fixture, such as an external database that needs to be cleaned
 * after each test, in multiple tests in async-style suites. This is implemented by registering each test, other than the first test, to run
 * as a ''continuation'' after the previous test completes.
 * 
 *
 * If you want the tests of an `AsyncFeatureSpec` to be executed in parallel, you
 * must mix in `ParallelTestExecution` and enable parallel execution of tests in your build.
 * You enable parallel execution in <a href="tools/Runner$.html">`Runner`</a> with the `-P` command line flag. 
 * In the ScalaTest Maven Plugin, set `parallel` to `true`.
 * In `sbt`, parallel execution is the default, but to be explicit you can write:
 * 
 * {{{
 * parallelExecution in Test := true // the default in sbt
 * }}}
 * 
 * On the JVM, if both <a href="ParallelTestExecution.html">`ParallelTestExecution`</a> is mixed in and 
 * parallel execution is enabled in the build, tests in an async-style suite will be started in parallel, using threads from
 * the <a href="Distributor">`Distributor`</a>, and allowed to complete in parallel, using threads from the
 * `executionContext`. If you are using ScalaTest's serial execution context, the JVM default, asynchronous tests will
 * run in parallel very much like traditional (such as <a href="FeatureSpec.html">`FeatureSpec`</a>) tests run in
 * parallel: 1) Because `ParallelTestExecution` extends
 * `OneInstancePerTest`, each test will run in its own instance of the test class, you need not worry about synchronizing
 * access to mutable instance state shared by different tests in the same suite.
 * 2) Because the serial execution context will confine the execution of each test to the single thread that executes the test body,
 * you need not worry about synchronizing access to shared mutable state accessed by transformations and callbacks of `Future`s
 * inside the test.
 * 
 * 
 * If <a href="ParallelTestExecution.html">`ParallelTestExecution`</a> is mixed in but
 * parallel execution of suites is ''not'' enabled, asynchronous tests on the JVM will be started sequentially, by the single thread
 * that invoked `run`, but without waiting for one test to complete before the next test is started. As a result,
 * asynchronous tests will be allowed to ''complete'' in parallel, using threads
 * from the `executionContext`. If you are using the serial execution context, however, you'll see
 * the same behavior you see when parallel execution is disabled and a traditional suite that mixes in `ParallelTestExecution`
 * is executed: the tests will run sequentially. If you use an execution context backed by a thread-pool, such as `global`,
 * however, even though tests will be started sequentially by one thread, they will be allowed to run concurrently using threads from the
 * execution context's thread pool.
 * 
 * 
 * The latter behavior is essentially what you'll see on Scala.js when you execute a suite that mixes in `ParallelTestExecution`.
 * Because only one thread exists when running under JavaScript, you can't "enable parallel execution of suites." However, it may
 * still be useful to run tests in parallel on Scala.js, because tests can invoke API calls that are truly asynchronous by calling into 
 * external APIs that take advantage of non-JavaScript threads. Thus on Scala.js, `ParallelTestExecution` allows asynchronous
 * tests to run in parallel, even though they must be started sequentially. This may give you better performance when you are using API
 * calls in your Scala.js tests that are truly asynchronous.
 * 
 *
 * <a name="futuresAndExpectedExceptions"></a>==Futures and expected exceptions==
 *
 * If you need to test for expected exceptions in the context of futures, you can use the
 * `recoverToSucceededIf` and `recoverToExceptionIf` methods of trait
 * <a href="RecoverMethods.html">`RecoverMethods`</a>. Because this trait is mixed into
 * supertrait `AsyncTestSuite`, both of these methods are
 * available by default in an `AsyncFeatureSpec`.
 * 
 *
 * If you just want to ensure that a future fails with a particular exception type, and do
 * not need to inspect the exception further, use `recoverToSucceededIf`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * recoverToSucceededIf[IllegalStateException] { // Result type: Future[Assertion]
 *   emptyStackActor ? Peek
 * }
 * }}}
 *
 * The `recoverToSucceededIf` method performs a job similar to
 * <a href="Assertions.html#assertThrowsMethod">`assertThrows`</a>, except
 * in the context of a future. It transforms a `Future` of any type into a
 * `Future[Assertion]` that succeeds only if the original future fails with the specified
 * exception. Here's an example in the REPL:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest.RecoverMethods._
 * import org.scalatest.RecoverMethods._
 *
 * scala&gt; import scala.concurrent.Future
 * import scala.concurrent.Future
 *
 * scala&gt; import scala.concurrent.ExecutionContext.Implicits.global
 * import scala.concurrent.ExecutionContext.Implicits.global
 *
 * scala&gt; recoverToSucceededIf[IllegalStateException] {
 *      |   Future { throw new IllegalStateException }
 *      | }
 * res0: scala.concurrent.Future[org.scalatest.Assertion] = ...
 *
 * scala&gt; res0.value
 * res1: Option[scala.util.Try[org.scalatest.Assertion]] = Some(Success(Succeeded))
 * }}}
 *
 * Otherwise it fails with an error message similar to those given by `assertThrows`:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; recoverToSucceededIf[IllegalStateException] {
 *      |   Future { throw new RuntimeException }
 *      | }
 * res2: scala.concurrent.Future[org.scalatest.Assertion] = ...
 *
 * scala&gt; res2.value
 * res3: Option[scala.util.Try[org.scalatest.Assertion]] =
 *     Some(Failure(org.scalatest.exceptions.TestFailedException: Expected exception
 *       java.lang.IllegalStateException to be thrown, but java.lang.RuntimeException
 *       was thrown))
 *
 * scala&gt; recoverToSucceededIf[IllegalStateException] {
 *      |   Future { 42 }
 *      | }
 * res4: scala.concurrent.Future[org.scalatest.Assertion] = ...
 *
 * scala&gt; res4.value
 * res5: Option[scala.util.Try[org.scalatest.Assertion]] =
 *     Some(Failure(org.scalatest.exceptions.TestFailedException: Expected exception
 *       java.lang.IllegalStateException to be thrown, but no exception was thrown))
 * }}}
 *
 * The `recoverToExceptionIf` method differs from the `recoverToSucceededIf` in
 * its behavior when the assertion succeeds: `recoverToSucceededIf` yields a `Future[Assertion]`,
 * whereas `recoverToExceptionIf` yields a `Future[T]`, where `T` is the
 * expected exception type.
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * recoverToExceptionIf[IllegalStateException] { // Result type: Future[IllegalStateException]
 *   emptyStackActor ? Peek
 * }
 * }}}
 *
 * In other words, `recoverToExpectionIf` is to
 * <a href="Assertions.html#interceptMethod">`intercept`</a> as
 * `recovertToSucceededIf` is to <a href="Assertions.html#assertThrowsMethod">`assertThrows`</a>. The first one allows you to
 * perform further assertions on the expected exception. The second one gives you a result type that will satisfy the type checker
 * at the end of the test body. Here's an example showing `recoverToExceptionIf` in the REPL:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; val futureEx =
 *      |   recoverToExceptionIf[IllegalStateException] {
 *      |     Future { throw new IllegalStateException("hello") }
 *      |   }
 * futureEx: scala.concurrent.Future[IllegalStateException] = ...
 *
 * scala&gt; futureEx.value
 * res6: Option[scala.util.Try[IllegalStateException]] =
 *     Some(Success(java.lang.IllegalStateException: hello))
 *
 * scala&gt; futureEx map { ex =&gt; assert(ex.getMessage == "world") }
 * res7: scala.concurrent.Future[org.scalatest.Assertion] = ...
 *
 * scala&gt; res7.value
 * res8: Option[scala.util.Try[org.scalatest.Assertion]] =
 *     Some(Failure(org.scalatest.exceptions.TestFailedException: "[hello]" did not equal "[world]"))
 * }}}
 *
 * <a name="ignoredTests"></a>==Ignored tests==
 *
 * To support the common use case of temporarily disabling a test, with the
 * good intention of resurrecting the test at a later time, `AsyncFeatureSpec` provides registration
 * methods that start with `ignore` instead of `scenario`. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.ignore
 *
 * import org.scalatest.AsyncFeatureSpec
 * import scala.concurrent.Future
 *
 * class AddSpec extends AsyncFeatureSpec {
 *
 *   def addSoon(addends: Int*): Future[Int] = Future { addends.sum }
 *   def addNow(addends: Int*): Int = addends.sum
 *
 *   Feature("The add methods") {
 *
 *     ignore("addSoon will eventually compute a sum of passed Ints") {
 *       val futureSum: Future[Int] = addSoon(1, 2)
 *       // You can map assertions onto a Future, then return
 *       // the resulting Future[Assertion] to ScalaTest:
 *       futureSum map { sum =&gt; assert(sum == 3) }
 *     }
 *
 *     Scenario("addNow will immediately compute a sum of passed Ints") {
 *       val sum: Int = addNow(1, 2)
 *       // You can also write synchronous tests. The body
 *       // must have result type Assertion:
 *       assert(sum == 3)
 *     }
 *   }
 * }
 * }}}
 *
 * If you run class `AddSpec` with:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new AddSpec)
 * }}}
 *
 * It will run only the second test and report that the first test was ignored:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">AddSpec:</span>
 * <span class="stGreen">Feature: The add methods</span>
 * <span class="stYellow">- Scenario: addSoon will eventually compute a sum of passed Ints !!! IGNORED !!!</span>
 * <span class="stGreen">- Scenario: addNow will immediately compute a sum of passed Ints</span>
 *
 * }}}
 *
 * If you wish to temporarily ignore an entire suite of tests, you can (on the JVM, not Scala.js) annotate the test class with `@Ignore`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.ignoreall
 *
 * import org.scalatest.AsyncFeatureSpec
 * import scala.concurrent.Future
 * import org.scalatest.Ignore
 *
 * @Ignore
 * class AddSpec extends AsyncFeatureSpec {
 *
 *   def addSoon(addends: Int*): Future[Int] = Future { addends.sum }
 *   def addNow(addends: Int*): Int = addends.sum
 *
 *   Feature("The add methods") {
 *
 *     Scenario("addSoon will eventually compute a sum of passed Ints") {
 *       val futureSum: Future[Int] = addSoon(1, 2)
 *       // You can map assertions onto a Future, then return
 *       // the resulting Future[Assertion] to ScalaTest:
 *       futureSum map { sum =&gt; assert(sum == 3) }
 *     }
 *
 *     Scenario("addNow will immediately compute a sum of passed Ints") {
 *       val sum: Int = addNow(1, 2)
 *       // You can also write synchronous tests. The body
 *       // must have result type Assertion:
 *       assert(sum == 3)
 *     }
 *   }
 * }
 * }}}
 *
 * When you mark a test class with a tag annotation, ScalaTest will mark each test defined in that class with that tag.
 * Thus, marking the `AddSpec` in the above example with the `@Ignore` tag annotation means that both tests
 * in the class will be ignored. If you run the above `AddSpec` in the Scala interpreter, you'll see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">AddSpec:</span>
 * <span class="stGreen">Feature: The add methods</span>
 * <span class="stYellow">- Scenario: addSoon will eventually compute a sum of passed Ints !!! IGNORED !!!</span>
 * <span class="stYellow">- Scenario: addNow will immediately compute a sum of passed Ints !!! IGNORED !!!</span>
 * }}}
 *
 * Note that marking a test class as ignored won't prevent it from being discovered by ScalaTest. Ignored classes
 * will be discovered and run, and all their tests will be reported as ignored. This is intended to keep the ignored
 * class visible, to encourage the developers to eventually fix and &ldquo;un-ignore&rdquo; it. If you want to
 * prevent a class from being discovered at all (on the JVM, not Scala.js), use the <a href="DoNotDiscover.html">`DoNotDiscover`</a>
 * annotation instead.
 * 
 *
 * If you want to ignore all tests of a suite on Scala.js, where annotations can't be inspected at runtime, you'll need
 * to change `it` to `ignore` at each test site. To make a suite non-discoverable on Scala.js, ensure it
 * does not declare a public no-arg constructor.  You can either declare a public constructor that takes one or more
 * arguments, or make the no-arg constructor non-public.  Because this technique will also make the suite non-discoverable
 * on the JVM, it is a good approach for suites you want to run (but not be discoverable) on both Scala.js and the JVM.
 * 
 *
 * <a name="informers"></a>==Informers==
 *
 * One of the parameters to `AsyncFeatureSpec`'s `run` method is a `Reporter`, which
 * will collect and report information about the running suite of tests.
 * Information about suites and tests that were run, whether tests succeeded or failed, 
 * and tests that were ignored will be passed to the <a href="Reporter.html">`Reporter`</a> as the suite runs.
 * Most often the default reporting done by `AsyncFeatureSpec`'s methods will be sufficient, but
 * occasionally you may wish to provide custom information to the `Reporter` from a test.
 * For this purpose, an <a href="Informer.html">`Informer`</a> that will forward information to the current `Reporter`
 * is provided via the `info` parameterless method.
 * You can pass the extra information to the `Informer` via its `apply` method.
 * The `Informer` will then pass the information to the `Reporter` via an <a href="events/InfoProvided.html">`InfoProvided`</a> event.
 * 
 * 
 * One use case for the `Informer` is to pass more information about a scenario to the reporter. For example,
 * the `GivenWhenThen` trait provides methods that use the implicit `info` provided by `AsyncFeatureSpec`
 * to pass such information to the reporter. You can see this in action in the <a href="#initialExample">initial example</a> of this trait's documentation.
 * 
 *
 * <a name="documenters"></a>==Documenters==
 *
 * `AsyncFeatureSpec` also provides a `markup` method that returns a <a href="Documenter.html">`Documenter`</a>, which allows you to send
 * to the `Reporter` text formatted in <a href="http://daringfireball.net/projects/markdown/" target="_blank">Markdown syntax</a>.
 * You can pass the extra information to the `Documenter` via its `apply` method.
 * The `Documenter` will then pass the information to the `Reporter` via an <a href="events/MarkupProvided.html">`MarkupProvided`</a> event.
 * 
 *
 * Here's an example `FlatSpec` that uses `markup`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.markup
 *
 * import collection.mutable
 * import org.scalatest._
 *
 * class SetSpec extends AsyncFeatureSpec with GivenWhenThen {
 *
 *   markup { """
 *
 * Mutable Set
 * -----------
 *
 * A set is a collection that contains no duplicate elements.
 *
 * To implement a concrete mutable set, you need to provide implementations
 * of the following methods:
 *
 *     def contains(elem: A): Boolean
 *     def iterator: Iterator[A]
 *     def += (elem: A): this.type
 *     def -= (elem: A): this.type

 * If you wish that methods like `take`,
 * `drop`, `filter` return the same kind of set,
 * you should also override:
 *
 *      def empty: This

 * It is also good idea to override methods `foreach` and
 * `size` for efficiency.
 *
 *   """ }
 *
 *   Feature("An element can be added to an empty mutable Set") {
 *     Scenario("When an element is added to an empty mutable Set") {
 *       Given("an empty mutable Set")
 *       val set = mutable.Set.empty[String]
 *
 *       When("an element is added")
 *       set += "clarity"
 *
 *       Then("the Set should have size 1")
 *       assert(set.size === 1)
 *
 *       And("the Set should contain the added element")
 *       assert(set.contains("clarity"))
 *
 *       markup("This test finished with a **bold** statement!")
 *       succeed
 *     }
 *   }
 * }
 * }}}
 *
 * Although all of ScalaTest's built-in reporters will display the markup text in some form,
 * the HTML reporter will format the markup information into HTML. Thus, the main purpose of `markup` is to
 * add nicely formatted text to HTML reports. Here's what the above `SetSpec` would look like in the HTML reporter:
 * 
 *
 * <img class="stScreenShot" src="../../lib/featureSpec.gif">
 *
 * <a name="notifiersAlerters"></a>==Notifiers and alerters==
 *
 * ScalaTest records text passed to `info` and `markup` during tests, and sends the recorded text in the `recordedEvents` field of
 * test completion events like `TestSucceeded` and `TestFailed`. This allows string reporters (like the standard out reporter) to show
 * `info` and `markup` text ''after'' the test name in a color determined by the outcome of the test. For example, if the test fails, string
 * reporters will show the `info` and `markup` text in red. If a test succeeds, string reporters will show the `info`
 * and `markup` text in green. While this approach helps the readability of reports, it means that you can't use `info` to get status
 * updates from long running tests.
 * 
 *
 * To get immediate (''i.e.'', non-recorded) notifications from tests, you can use `note` (a <a href="Notifier.html">`Notifier`</a>) and `alert`
 * (an <a href="Alerter.html">`Alerter`</a>). Here's an example showing the differences:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.note
 *
 * import collection.mutable
 * import org.scalatest._
 *
 * class SetSpec extends AsyncFeatureSpec {
 *
 *   Feature("An element can be added to an empty mutable Set") {
 *     Scenario("When an element is added to an empty mutable Set") {
 *
 *       info("info is recorded")
 *       markup("markup is *also* recorded")
 *       note("notes are sent immediately")
 *       alert("alerts are also sent immediately")
 *
 *       val set = mutable.Set.empty[String]
 *       set += "clarity"
 *       assert(set.size === 1)
 *       assert(set.contains("clarity"))
 *     }
 *   }
 * }
 * }}}
 *
 * Because `note` and `alert` information is sent immediately, it will appear ''before'' the test name in string reporters, and its color will
 * be unrelated to the ultimate outcome of the test: `note` text will always appear in green, `alert` text will always appear in yellow.
 * Here's an example:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * <span class="stGreen">SetSpec:
 * Feature: An element can be added to an empty mutable Set
 *   + notes are sent immediately</span>
 *   <span class="stYellow">+ alerts are also sent immediately</span>
 *   <span class="stGreen">Scenario: When an element is added to an empty mutable Set
 *     info is recorded
 *   + markup is *also* recorded</span>
 * }}}
 *
 * Another example is <a href="tools/Runner$.html#slowpokeNotifications">slowpoke notifications</a>.
 * If you find a test is taking a long time to complete, but you're not sure which test, you can enable 
 * slowpoke notifications. ScalaTest will use an `Alerter` to fire an event whenever a test has been running
 * longer than a specified amount of time.
 * 
 *
 * In summary, use `info` and `markup` for text that should form part of the specification output. Use
 * `note` and `alert` to send status notifications. (Because the HTML reporter is intended to produce a
 * readable, printable specification, `info` and `markup` text will appear in the HTML report, but
 * `note` and `alert` text will not.)
 * 
 *
 * <a name="pendingTests"></a>==Pending tests==
 *
 * A ''pending test'' is one that has been given a name but is not yet implemented. The purpose of
 * pending tests is to facilitate a style of testing in which documentation of behavior is sketched
 * out before tests are written to verify that behavior (and often, before the behavior of
 * the system being tested is itself implemented). Such sketches form a kind of specification of
 * what tests and functionality to implement later.
 * 
 *
 * To support this style of testing, a test can be given a name that specifies one
 * bit of behavior required by the system being tested. At the end of the test,
 * it can call method `pending`, which will cause it to complete abruptly with `TestPendingException`.
 * 
 *
 * Because tests in ScalaTest can be designated as pending with `TestPendingException`, both the test name and any information
 * sent to the reporter when running the test can appear in the report of a test run. (In other words,
 * the code of a pending test is executed just like any other test.) However, because the test completes abruptly
 * with `TestPendingException`, the test will be reported as pending, to indicate
 * the actual test, and possibly the functionality, has not yet been implemented. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.pending
 *
 * import org.scalatest.AsyncFeatureSpec
 * import scala.concurrent.Future
 *
 * class AddSpec extends AsyncFeatureSpec {
 *
 *   def addSoon(addends: Int*): Future[Int] = Future { addends.sum }
 *   def addNow(addends: Int*): Int = addends.sum
 *
 *   Feature("The add methods") {
 *
 *     Scenario("addSoon will eventually compute a sum of passed Ints") (pending)
 *
 *     Scenario("addNow will immediately compute a sum of passed Ints") {
 *       val sum: Int = addNow(1, 2)
 *       // You can also write synchronous tests. The body
 *       // must have result type Assertion:
 *       assert(sum == 3)
 *     }
 *   }
 * }
 * }}}
 *
 * (Note: "`(pending)`" is the body of the test. Thus the test contains just one statement, an invocation
 * of the `pending` method, which throws `TestPendingException`.)
 * If you run this version of `AddSpec` with:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new AddSpec)
 * }}}
 *
 * It will run both tests, but report that first test is pending. You'll see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">AddSpec:</span>
 * <span class="stGreen">Feature: The add methods</span>
 * <span class="stYellow">- Scenario: addSoon will eventually compute a sum of passed Ints (pending)</span>
 * <span class="stGreen">- Scenario: addNow will immediately compute a sum of passed Ints</span>
 * }}}
 *
 * One difference between an ignored test and a pending one is that an ignored test is intended to be used during
 * significant refactorings of the code under test, when tests break and you don't want to spend the time to fix
 * all of them immediately. You can mark some of those broken tests as ignored temporarily, so that you can focus the red
 * bar on just failing tests you actually want to fix immediately. Later you can go back and fix the ignored tests.
 * In other words, by ignoring some failing tests temporarily, you can more easily notice failed tests that you actually
 * want to fix. By contrast, a pending test is intended to be used before a test and/or the code under test is written.
 * Pending indicates you've decided to write a test for a bit of behavior, but either you haven't written the test yet, or
 * have only written part of it, or perhaps you've written the test but don't want to implement the behavior it tests
 * until after you've implemented a different bit of behavior you realized you need first. Thus ignored tests are designed
 * to facilitate refactoring of existing code whereas pending tests are designed to facilitate the creation of new code.
 * 
 *
 * One other difference between ignored and pending tests is that ignored tests are implemented as a test tag that is
 * excluded by default. Thus an ignored test is never executed. By contrast, a pending test is implemented as a
 * test that throws `TestPendingException` (which is what calling the `pending` method does). Thus
 * the body of pending tests are executed up until they throw `TestPendingException`.
 * 
 *
 * <a name="taggingTests"></a>==Tagging tests==
 *
 * An `AsyncFeatureSpec`'s tests may be classified into groups by ''tagging'' them with string names.
 * As with any suite, when executing an `AsyncFeatureSpec`, groups of tests can
 * optionally be included and/or excluded. To tag an `AsyncFeatureSpec`'s tests,
 * you pass objects that extend class `org.scalatest.Tag` to methods
 * that register tests. Class `Tag` takes one parameter, a string name.  If you have
 * created tag annotation interfaces as described in the <a href="Tag.html">`Tag` documentation</a>, then you
 * will probably want to use tag names on your test functions that match. To do so, simply
 * pass the fully qualified names of the tag interfaces to the `Tag` constructor. For example, if you've
 * defined a tag annotation interface with fully qualified name,
 * `com.mycompany.tags.DbTest`, then you could
 * create a matching tag for `AsyncFeatureSpec`s like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.tagging
 *
 * import org.scalatest.Tag
 *
 * object DbTest extends Tag("com.mycompany.tags.DbTest")
 * }}}
 *
 * Given these definitions, you could place `AsyncFeatureSpec` tests into groups with tags like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.AsyncFeatureSpec
 * import org.scalatest.tagobjects.Slow
 * import scala.concurrent.Future
 *
 * class AddSpec extends AsyncFeatureSpec {
 *
 *   def addSoon(addends: Int*): Future[Int] = Future { addends.sum }
 *   def addNow(addends: Int*): Int = addends.sum
 *
 *   Feature("The add methods") {
 *
 *     Scenario("addSoon will eventually compute a sum of passed Ints",
 *          Slow) {
 *
 *       val futureSum: Future[Int] = addSoon(1, 2)
 *       // You can map assertions onto a Future, then return
 *       // the resulting Future[Assertion] to ScalaTest:
 *       futureSum map { sum =&gt; assert(sum == 3) }
 *     }
 *
 *     Scenario("addNow will immediately compute a sum of passed Ints",
 *         Slow, DbTest) {
 *
 *       val sum: Int = addNow(1, 2)
 *       // You can also write synchronous tests. The body
 *       // must have result type Assertion:
 *       assert(sum == 3)
 *     }
 *   }
 * }
 * }}}
 *
 * This code marks both tests with the `org.scalatest.tags.Slow` tag,
 * and the second test with the `com.mycompany.tags.DbTest` tag.
 * 
 *
 * The `run` method takes a `Filter`, whose constructor takes an optional
 * `Set[String]` called `tagsToInclude` and a `Set[String]` called
 * `tagsToExclude`. If `tagsToInclude` is `None`, all tests will be run
 * except those those belonging to tags listed in the
 * `tagsToExclude` `Set`. If `tagsToInclude` is defined, only tests
 * belonging to tags mentioned in the `tagsToInclude` set, and not mentioned in `tagsToExclude`,
 * will be run.
 * 
 *
 * It is recommended, though not required, that you create a corresponding tag annotation when you
 * create a `Tag` object. A tag annotation (on the JVM, not Scala.js) allows you to tag all the tests of an `AsyncFeatureSpec` in
 * one stroke by annotating the class. For more information and examples, see the
 * <a href="Tag.html">documentation for class `Tag`</a>. On Scala.js, to tag all tests of a suite, you'll need to
 * tag each test individually at the test site.
 * 
 *
 * <a name="sharedFixtures"></a>
 * ==Shared fixtures==
 *
 * A test ''fixture'' is composed of the objects and other artifacts (files, sockets, database
 * connections, ''etc.'') tests use to do their work.
 * When multiple tests need to work with the same fixtures, it is important to try and avoid
 * duplicating the fixture code across those tests. The more code duplication you have in your
 * tests, the greater drag the tests will have on refactoring the actual production code.
 * 
 *
 * ScalaTest recommends three techniques to eliminate such code duplication in async styles:
 * 
 *
 * <ul>
 * <li>Refactor using Scala</li>
 * <li>Override `withFixture`</li>
 * <li>Mix in a ''before-and-after'' trait</li>
 * </ul>
 *
 * <p>Each technique is geared towards helping you reduce code duplication without introducing
 * instance `var`s, shared mutable objects, or other dependencies between tests. Eliminating shared
 * mutable state across tests will make your test code easier to reason about and eliminate the need to
 * synchronize access to shared mutable state on the JVM.
 * 
 *
 * The following sections describe these techniques, including explaining the recommended usage
 * for each. But first, here's a table summarizing the options:
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 *
 * <tr>
 *   <td colspan="2" style="background-color: #CCCCCC; border-width: 1px; padding: 3px; padding-top: 7px; border: 1px solid black; text-align: left">
 *     '''Refactor using Scala when different tests need different fixtures.'''
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="#getFixtureMethods">get-fixture methods</a>
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     The ''extract method'' refactor helps you create a fresh instances of mutable fixture objects in each test
 *     that needs them, but doesn't help you clean them up when you're done.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="#loanFixtureMethods">loan-fixture methods</a>
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     Factor out dupicate code with the ''loan pattern'' when different tests need different fixtures ''that must be cleaned up afterwards''.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td colspan="2" style="background-color: #CCCCCC; border-width: 1px; padding: 3px; padding-top: 7px; border: 1px solid black; text-align: left">
 *     '''Override `withFixture` when most or all tests need the same fixture.'''
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="#withFixtureNoArgAsyncTest">
 *       `withFixture(NoArgAsyncTest)`</a>
 *     </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     The recommended default approach when most or all tests need the same fixture treatment. This general technique
 *     allows you, for example, to perform side effects at the beginning and end of all or most tests,
 *     transform the outcome of tests, retry tests, make decisions based on test names, tags, or other test data.
 *     Use this technique unless:
 *     
 *  <dl>
 *  <dd style="display: list-item; list-style-type: disc; margin-left: 1.2em;">Different tests need different fixtures (refactor using Scala instead)</dd>
 *  <dd style="display: list-item; list-style-type: disc; margin-left: 1.2em;">An exception in fixture code should abort the suite, not fail the test (use a ''before-and-after'' trait instead)</dd>
 *  <dd style="display: list-item; list-style-type: disc; margin-left: 1.2em;">You have objects to pass into tests (override `withFixture(''One''ArgAsyncTest)` instead)</dd>
 *  </dl>
 *  </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="#withFixtureOneArgAsyncTest">
 *       `withFixture(OneArgAsyncTest)`
 *     </a>
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     Use when you want to pass the same fixture object or objects as a parameter into all or most tests.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td colspan="2" style="background-color: #CCCCCC; border-width: 1px; padding: 3px; padding-top: 7px; border: 1px solid black; text-align: left">
 *     '''Mix in a before-and-after trait when you want an aborted suite, not a failed test, if the fixture code fails.'''
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="#beforeAndAfter">`BeforeAndAfter`</a>
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     Use this boilerplate-buster when you need to perform the same side-effects before and/or after tests, rather than at the beginning or end of tests.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="#composingFixtures">`BeforeAndAfterEach`</a>
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     Use when you want to ''stack traits'' that perform the same side-effects before and/or after tests, rather than at the beginning or end of tests.
 *   </td>
 * </tr>
 *
 * </table>
 *
 * <a name="getFixtureMethods"></a>
 * ====Calling get-fixture methods====
 *
 * If you need to create the same mutable fixture objects in multiple tests, and don't need to clean them up after using them, the simplest approach is to write one or
 * more ''get-fixture'' methods. A get-fixture method returns a new instance of a needed fixture object (or a holder object containing
 * multiple fixture objects) each time it is called. You can call a get-fixture method at the beginning of each
 * test that needs the fixture, storing the returned object or objects in local variables. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.getfixture
 *
 * import org.scalatest.AsyncFeatureSpec
 * import scala.concurrent.Future
 *
 * class ExampleSpec extends AsyncFeatureSpec {
 *
 *   def fixture: Future[String] = Future { "ScalaTest is designed to " }
 *
 *   Feature("Simplicity") {
 *     Scenario("User needs to read test code written by others") {
 *       val future = fixture
 *       val result = future map { s =&gt; s + "encourage clear code!" }
 *       result map { s =&gt;
 *         assert(s == "ScalaTest is designed to encourage clear code!")
 *       }
 *     }
 *
 *     Scenario("User needs to understand what the tests are doing") {
 *       val future = fixture
 *       val result = future map { s =&gt; s + "be easy to reason about!" }
 *       result map { s =&gt;
 *         assert(s == "ScalaTest is designed to be easy to reason about!")
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * If you need to configure fixture objects differently in different tests, you can pass configuration into the get-fixture method.
 * For example, you could pass in an initial value for a fixture object as a parameter to the get-fixture method.
 * 
 *
 * <a name="withFixtureNoArgAsyncTest"></a>
 * ====Overriding `withFixture(NoArgAsyncTest)`====
 *
 * Although the get-fixture method approach takes care of setting up a fixture at the beginning of each
 * test, it doesn't address the problem of cleaning up a fixture at the end of the test. If you just need to perform a side-effect at the beginning or end of
 * a test, and don't need to actually pass any fixture objects into the test, you can override `withFixture(NoArgAsyncTest)`, a
 * method defined in trait <a href="AsyncTestSuite.html">`AsyncTestSuite`</a>, a supertrait of `AsyncFeatureSpec`.
 * 
 *
 * Trait `AsyncFeatureSpec`'s `runTest` method passes a no-arg async test function to
 * `withFixture(NoArgAsyncTest)`. It is `withFixture`'s
 * responsibility to invoke that test function. The default implementation of `withFixture` simply
 * invokes the function and returns the result, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Default implementation in trait AsyncTestSuite
 * protected def withFixture(test: NoArgAsyncTest): FutureOutcome = {
 *   test()
 * }
 * }}}
 *
 * You can, therefore, override `withFixture` to perform setup before invoking the test function,
 * and/or perform cleanup after the test completes. The recommended way to ensure cleanup is performed after a test completes is
 * to use the `complete`-`lastly` syntax, defined in supertrait <a href="CompleteLastly.html">`CompleteLastly`</a>.
 * The `complete`-`lastly` syntax will ensure that
 * cleanup will occur whether future-producing code completes abruptly by throwing an exception, or returns
 * normally yielding a future. In the latter case, `complete`-`lastly` will register the cleanup code
 * to execute asynchronously when the future completes.
 * 
 *
 * The `withFixture` method is designed to be stacked, and to enable this, you should always call the `super` implementation
 * of `withFixture`, and let it invoke the test function rather than invoking the test function directly. In other words, instead of writing
 * &ldquo;`test()`&rdquo;, you should write &ldquo;`super.withFixture(test)`&rdquo;, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgTest) = {
 *
 *   // Perform setup here
 *
 *   complete {
 *     super.withFixture(test) // Invoke the test function
 *   } lastly {
 *     // Perform cleanup here
 *   }
 * }
 * }}}
 *
 * If you have no cleanup to perform, you can write `withFixture` like this instead:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgTest) = {
 *
 *   // Perform setup here
 *
 *   super.withFixture(test) // Invoke the test function
 * }
 * }}}
 *
 * If you want to perform an action only for certain outcomes, you'll need to
 * register code performing that action as a callback on the `Future` using
 * one of `Future`'s registration methods: `onComplete`, `onSuccess`,
 * or `onFailure`. Note that if a test fails, that will be treated as a
 * `scala.util.Success(org.scalatest.Failed)`. So if you want to perform an
 * action if a test fails, for example, you'd register the callback using `onSuccess`.
 * 
 *
 * Here's an example in which `withFixture(NoArgAsyncTest)` is used to take a
 * snapshot of the working directory if a test fails, and
 * send that information to the standard output stream:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.noargasynctest
 *
 * import java.io.File
 * import org.scalatest._
 * import scala.concurrent.Future
 *
 * class ExampleSpec extends AsyncFeatureSpec {
 *
 *   override def withFixture(test: NoArgAsyncTest) = {
 *
 *     super.withFixture(test) onFailedThen { _ =&gt;
 *       val currDir = new File(".")
 *       val fileNames = currDir.list()
 *       info("Dir snapshot: " + fileNames.mkString(", "))
 *     }
 *   }
 *
 *   def addSoon(addends: Int*): Future[Int] = Future { addends.sum }
 *
 *   Feature("addSoon") {
 *     Scenario("succeed case") {
 *       addSoon(1, 1) map { sum =&gt; assert(sum == 2) }
 *     }
 *
 *     Scenario("fail case") {
 *       addSoon(1, 1) map { sum =&gt; assert(sum == 3) }
 *     }
 *   }
 * }
 * }}}
 *
 * Running this version of `ExampleSpec` in the interpreter in a directory with two files, `hello.txt` and `world.txt`
 * would give the following output:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new ExampleSpec)
 * <span class="stGreen">ExampleSpec:
 * Feature: addSoon
 * - Scenario: succeed case</span>
 * <span class="stRed">- Scenario: fail case *** FAILED ***
 *   2 did not equal 3 (<console>:33)</span>
 * }}}
 *
 * Note that the <a href="Suite$NoArgTest.html">`NoArgAsyncTest`</a> passed to `withFixture`, in addition to
 * an `apply` method that executes the test, also includes the test name and the <a href="ConfigMap.html">config
 * map</a> passed to `runTest`. Thus you can also use the test name and configuration objects in your `withFixture`
 * implementation.
 * 
 *
 * Lastly, if you want to transform the outcome in some way in `withFixture`, you'll need to use either the
 * `map` or `transform` methods of `Future`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgAsyncTest) = {
 *
 *   // Perform setup here
 *
 *   val futureOutcome = super.withFixture(test) // Invoke the test function
 *
 *   futureOutcome change { outcome =&gt;
 *     // transform the outcome into a new outcome here
 *   }
 * }
 * }}}
 *
 * Note that a `NoArgAsyncTest`'s `apply` method will return a `scala.util.Failure` only if
 * the test completes abruptly with a "test-fatal" exception (such as `OutOfMemoryError`) that should
 * cause the suite to abort rather than the test to fail. Thus usually you would use `map`
 * to transform future outcomes, not `transform`, so that such test-fatal exceptions pass through
 * unchanged. The suite will abort asynchronously with any exception returned from `NoArgAsyncTest`'s
 * apply method in a `scala.util.Failure`.
 * 
 *
 * <a name="loanFixtureMethods"></a>
 * ====Calling loan-fixture methods====
 *
 * If you need to both pass a fixture object into a test ''and'' perform cleanup at the end of the test, you'll need to use the ''loan pattern''.
 * If different tests need different fixtures that require cleanup, you can implement the loan pattern directly by writing ''loan-fixture'' methods.
 * A loan-fixture method takes a function whose body forms part or all of a test's code. It creates a fixture, passes it to the test code by invoking the
 * function, then cleans up the fixture after the function returns.
 * 
 *
 * The following example shows three tests that use two fixtures, a database and a file. Both require cleanup after, so each is provided via a
 * loan-fixture method. (In this example, the database is simulated with a `StringBuffer`.)
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.loanfixture
 *
 * import java.util.concurrent.ConcurrentHashMap
 *
 * import scala.concurrent.Future
 * import scala.concurrent.ExecutionContext
 *
 * object DbServer { // Simulating a database server
 *   type Db = StringBuffer
 *   private final val databases = new ConcurrentHashMap[String, Db]
 *   def createDb(name: String): Db = {
 *     val db = new StringBuffer // java.lang.StringBuffer is thread-safe
 *     databases.put(name, db)
 *     db
 *   }
 *   def removeDb(name: String): Unit = {
 *     databases.remove(name)
 *   }
 * }
 *
 * // Defining actor messages
 * sealed abstract class StringOp
 * case object Clear extends StringOp
 * case class Append(value: String) extends StringOp
 * case object GetValue
 *
 * class StringActor { // Simulating an actor
 *   private final val sb = new StringBuilder
 *   def !(op: StringOp): Unit =
 *     synchronized {
 *       op match {
 *         case Append(value) =&gt; sb.append(value)
 *         case Clear =&gt; sb.clear()
 *       }
 *     }
 *   def ?(get: GetValue.type)(implicit c: ExecutionContext): Future[String] =
 *     Future {
 *       synchronized { sb.toString }
 *     }
 * }
 *
 * import org.scalatest._
 * import DbServer._
 * import java.util.UUID.randomUUID
 *
 * class ExampleSpec extends AsyncFeatureSpec {
 *
 *   def withDatabase(testCode: Future[Db] =&gt; Future[Assertion]) = {
 *     val dbName = randomUUID.toString // generate a unique db name
 *     val futureDb = Future { createDb(dbName) } // create the fixture
 *     complete {
 *       val futurePopulatedDb =
 *         futureDb map { db =&gt;
 *           db.append("ScalaTest is designed to ") // perform setup
 *         }
 *       testCode(futurePopulatedDb) // "loan" the fixture to the test code
 *     } lastly {
 *       removeDb(dbName) // ensure the fixture will be cleaned up
 *     }
 *   }
 *
 *   def withActor(testCode: StringActor =&gt; Future[Assertion]) = {
 *     val actor = new StringActor
 *     complete {
 *       actor ! Append("ScalaTest is designed to ") // set up the fixture
 *       testCode(actor) // "loan" the fixture to the test code
 *     } lastly {
 *       actor ! Clear // ensure the fixture will be cleaned up
 *     }
 *   }
 *
 *   Feature("Simplicity") {
 *     // This test needs the actor fixture
 *     Scenario("User needs to read test code written by others") {
 *       withActor { actor =&gt;
 *         actor ! Append("encourage clear code!")
 *         val futureString = actor ? GetValue
 *         futureString map { s =&gt;
 *           assert(s === "ScalaTest is designed to encourage clear code!")
 *         }
 *       }
 *     }
 *     // This test needs the database fixture
 *     Scenario("User needs to understand what the tests are doing") {
 *       withDatabase { futureDb =&gt;
 *         futureDb map { db =&gt;
 *           db.append("be easy to reason about!")
 *           assert(db.toString === "ScalaTest is designed to be easy to reason about!")
 *         }
 *       }
 *     }
 *     // This test needs both the actor and the database
 *     Scenario("User needs to write tests") {
 *       withDatabase { futureDb =&gt;
 *         withActor { actor =&gt; // loan-fixture methods compose
 *           actor ! Append("be easy to remember how to write!")
 *           val futureString = actor ? GetValue
 *           val futurePair: Future[(Db, String)] =
 *             futureDb zip futureString
 *           futurePair map { case (db, s) =&gt;
 *             db.append("be easy to learn!")
 *             assert(db.toString === "ScalaTest is designed to be easy to learn!")
 *             assert(s === "ScalaTest is designed to be easy to remember how to write!")
 *           }
 *         }
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * As demonstrated by the last test, loan-fixture methods compose. Not only do loan-fixture methods allow you to
 * give each test the fixture it needs, they allow you to give a test multiple fixtures and clean everything up afterwards.
 * 
 *
 * Also demonstrated in this example is the technique of giving each test its own "fixture sandbox" to play in. When your fixtures
 * involve external side-effects, like creating databases, it is a good idea to give each database a unique name as is
 * done in this example. This keeps tests completely isolated, allowing you to run them in parallel if desired.
 * 
 *
 * <a name="withFixtureOneArgAsyncTest"></a>
 * ====Overriding `withFixture(OneArgTest)`====
 *
 * If all or most tests need the same fixture, you can avoid some of the boilerplate of the loan-fixture method approach by using a
 * `fixture.AsyncTestSuite` and overriding `withFixture(OneArgAsyncTest)`.
 * Each test in a `fixture.AsyncTestSuite` takes a fixture as a parameter, allowing you to pass the fixture into
 * the test. You must indicate the type of the fixture parameter by specifying `FixtureParam`, and implement a
 * `withFixture` method that takes a `OneArgAsyncTest`. This `withFixture` method is responsible for
 * invoking the one-arg async test function, so you can perform fixture set up before invoking and passing
 * the fixture into the test function, and ensure clean up is performed after the test completes.
 * 
 *
 * To enable the stacking of traits that define `withFixture(NoArgAsyncTest)`, it is a good idea to let
 * `withFixture(NoArgAsyncTest)` invoke the test function instead of invoking the test
 * function directly. To do so, you'll need to convert the `OneArgAsyncTest` to a `NoArgAsyncTest`. You can do that by passing
 * the fixture object to the `toNoArgAsyncTest` method of `OneArgAsyncTest`. In other words, instead of
 * writing &ldquo;`test(theFixture)`&rdquo;, you'd delegate responsibility for
 * invoking the test function to the `withFixture(NoArgAsyncTest)` method of the same instance by writing:
 * 
 *
 * {{{
 * withFixture(test.toNoArgAsyncTest(theFixture))
 * }}}
 *
 * Here's a complete example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.oneargasynctest
 *
 * import org.scalatest._
 * import scala.concurrent.Future
 * import scala.concurrent.ExecutionContext
 *
 * // Defining actor messages
 * sealed abstract class StringOp
 * case object Clear extends StringOp
 * case class Append(value: String) extends StringOp
 * case object GetValue
 *
 * class StringActor { // Simulating an actor
 *   private final val sb = new StringBuilder
 *   def !(op: StringOp): Unit =
 *     synchronized {
 *       op match {
 *         case Append(value) =&gt; sb.append(value)
 *         case Clear =&gt; sb.clear()
 *       }
 *     }
 *   def ?(get: GetValue.type)(implicit c: ExecutionContext): Future[String] =
 *     Future {
 *       synchronized { sb.toString }
 *     }
 * }
 *
 * class ExampleSpec extends fixture.AsyncFeatureSpec {
 *
 *   type FixtureParam = StringActor
 *
 *   def withFixture(test: OneArgAsyncTest): FutureOutcome = {
 *
 *     val actor = new StringActor
 *     complete {
 *       actor ! Append("ScalaTest is designed to ") // set up the fixture
 *       withFixture(test.toNoArgAsyncTest(actor))
 *     } lastly {
 *       actor ! Clear // ensure the fixture will be cleaned up
 *     }
 *   }
 *
 *   Feature("Simplicity") {
 *     Scenario("User needs to read test code written by others") { actor =&gt;
 *       actor ! Append("encourage clear code!")
 *       val futureString = actor ? GetValue
 *       futureString map { s =&gt;
 *         assert(s === "ScalaTest is designed to encourage clear code!")
 *       }
 *     }
 *
 *     Scenario("User needs to understand what the tests are doing") { actor =&gt;
 *       actor ! Append("be easy to reason about!")
 *       val futureString = actor ? GetValue
 *       futureString map { s =&gt;
 *         assert(s === "ScalaTest is designed to be easy to reason about!")
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * In this example, the tests required one fixture object, a `StringActor`. If your tests need multiple fixture objects, you can
 * simply define the `FixtureParam` type to be a tuple containing the objects or, alternatively, a case class containing
 * the objects.  For more information on the `withFixture(OneArgAsyncTest)` technique, see
 * the <a href="fixture/AsyncFeatureSpec.html">documentation for `fixture.AsyncFeatureSpec`</a>.
 * 
 *
 * <a name="beforeAndAfter"></a>
 * ====Mixing in `BeforeAndAfter`====
 *
 * In all the shared fixture examples shown so far, the activities of creating, setting up, and cleaning up the fixture objects have been
 * performed ''during'' the test.  This means that if an exception occurs during any of these activities, it will be reported as a test failure.
 * Sometimes, however, you may want setup to happen ''before'' the test starts, and cleanup ''after'' the test has completed, so that if an
 * exception occurs during setup or cleanup, the entire suite aborts and no more tests are attempted. The simplest way to accomplish this in ScalaTest is
 * to mix in trait <a href="BeforeAndAfter.html">`BeforeAndAfter`</a>.  With this trait you can denote a bit of code to run before each test
 * with `before` and/or after each test each test with `after`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.beforeandafter
 *
 * import org.scalatest.AsyncFeatureSpec
 * import org.scalatest.BeforeAndAfter
 * import scala.concurrent.Future
 * import scala.concurrent.ExecutionContext
 *
 * // Defining actor messages
 * sealed abstract class StringOp
 * case object Clear extends StringOp
 * case class Append(value: String) extends StringOp
 * case object GetValue
 *
 * class StringActor { // Simulating an actor
 *   private final val sb = new StringBuilder
 *   def !(op: StringOp): Unit =
 *     synchronized {
 *       op match {
 *         case Append(value) =&gt; sb.append(value)
 *         case Clear =&gt; sb.clear()
 *       }
 *     }
 *   def ?(get: GetValue.type)(implicit c: ExecutionContext): Future[String] =
 *     Future {
 *       synchronized { sb.toString }
 *     }
 * }
 *
 * class ExampleSpec extends AsyncFeatureSpec with BeforeAndAfter {
 *
 *   final val actor = new StringActor
 *
 *   before {
 *     actor ! Append("ScalaTest is designed to ") // set up the fixture
 *   }
 *
 *   after {
 *     actor ! Clear // clean up the fixture
 *   }
 *
 *   Feature("Simplicity") {
 *     Scenario("User needs to read test code written by others") {
 *       actor ! Append("encourage clear code!")
 *       val futureString = actor ? GetValue
 *       futureString map { s =&gt;
 *         assert(s == "ScalaTest is designed to encourage clear code!")
 *       }
 *     }
 *
 *     Scenario("User needs to understand what the tests are doing") {
 *       actor ! Append("be easy to reason about!")
 *       val futureString = actor ? GetValue
 *       futureString map { s =&gt;
 *         assert(s == "ScalaTest is designed to be easy to reason about!")
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * Note that the only way `before` and `after` code can communicate with test code is via some
 * side-effecting mechanism, commonly by reassigning instance `var`s or by changing the state of mutable
 * objects held from instance `val`s (as in this example). If using instance `var`s or
 * mutable objects held from instance `val`s you wouldn't be able to run tests in parallel in the same instance
 * of the test class (on the JVM, not Scala.js) unless you synchronized access to the shared, mutable state.
 * 
 *
 * Note that on the JVM, if you override ScalaTest's default
 * <a href="#asyncExecutionModel">''serial execution context''</a>, you will likely need to
 * worry about synchronizing access to shared mutable fixture state, because the execution
 * context may assign different threads to process
 * different `Future` transformations. Although access to mutable state along
 * the same linear chain of `Future` transformations need not be synchronized,
 * it can be difficult to spot cases where these constraints are violated. The best approach
 * is to use only immutable objects when transforming `Future`s. When that's not
 * practical, involve only thread-safe mutable objects, as is done in the above example.
 * On Scala.js, by contrast, you need not worry about thread synchronization, because
 * in effect only one thread exists.
 * 
 *
 * Although `BeforeAndAfter` provides a minimal-boilerplate way to execute code before and after tests, it isn't designed to enable stackable
 * traits, because the order of execution would be non-obvious.  If you want to factor out before and after code that is common to multiple test suites, you
 * should use trait `BeforeAndAfterEach` instead, as shown later in the next section,
 * <a href="#composingFixtures.html">composing fixtures by stacking traits</a>.
 * 
 *
 * <a name="composingFixtures"></a>==Composing fixtures by stacking traits==
 *
 * In larger projects, teams often end up with several different fixtures that test classes need in different combinations,
 * and possibly initialized (and cleaned up) in different orders. A good way to accomplish this in ScalaTest is to factor the individual
 * fixtures into traits that can be composed using the ''stackable trait'' pattern. This can be done, for example, by placing
 * `withFixture` methods in several traits, each of which call `super.withFixture`. Here's an example in
 * which the `StringBuilderActor` and `StringBufferActor` fixtures used in the previous examples have been
 * factored out into two ''stackable fixture traits'' named `Builder` and `Buffer`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.composingwithasyncfixture
 *
 * import org.scalatest._
 * import org.scalatest.SuiteMixin
 * import collection.mutable.ListBuffer
 * import scala.concurrent.Future
 * import scala.concurrent.ExecutionContext
 *
 * // Defining actor messages
 * sealed abstract class StringOp
 * case object Clear extends StringOp
 * case class Append(value: String) extends StringOp
 * case object GetValue
 *
 * class StringBuilderActor { // Simulating an actor
 *   private final val sb = new StringBuilder
 *   def !(op: StringOp): Unit =
 *     synchronized {
 *       op match {
 *         case Append(value) =&gt; sb.append(value)
 *         case Clear =&gt; sb.clear()
 *       }
 *     }
 *   def ?(get: GetValue.type)(implicit c: ExecutionContext): Future[String] =
 *     Future {
 *       synchronized { sb.toString }
 *     }
 * }
 *
 * class StringBufferActor {
 *   private final val buf = ListBuffer.empty[String]
 *   def !(op: StringOp): Unit =
 *     synchronized {
 *       op match {
 *         case Append(value) =&gt; buf += value
 *         case Clear =&gt; buf.clear()
 *       }
 *     }
 *   def ?(get: GetValue.type)(implicit c: ExecutionContext): Future[List[String]] =
 *     Future {
 *       synchronized { buf.toList }
 *     }
 * }
 *
 * trait Builder extends AsyncTestSuiteMixin { this: AsyncTestSuite =&gt;
 *
 *   final val builderActor = new StringBuilderActor
 *
 *   abstract override def withFixture(test: NoArgAsyncTest) = {
 *     builderActor ! Append("ScalaTest is designed to ")
 *     complete {
 *       super.withFixture(test) // To be stackable, must call super.withFixture
 *     } lastly {
 *       builderActor ! Clear
 *     }
 *   }
 * }
 *
 * trait Buffer extends AsyncTestSuiteMixin { this: AsyncTestSuite =&gt;
 *
 *   final val bufferActor = new StringBufferActor
 *
 *   abstract override def withFixture(test: NoArgAsyncTest) = {
 *     complete {
 *       super.withFixture(test) // To be stackable, must call super.withFixture
 *     } lastly {
 *       bufferActor ! Clear
 *     }
 *   }
 * }
 *
 * class ExampleSpec extends AsyncFeatureSpec with Builder with Buffer {
 *
 *   Feature("Simplicity") {
 *     Scenario("User needs to read test code written by others") {
 *       builderActor ! Append("encourage clear code!")
 *       val futureString = builderActor ? GetValue
 *       val futureList = bufferActor ? GetValue
 *       val futurePair: Future[(String, List[String])] = futureString zip futureList
 *       futurePair map { case (str, lst) =&gt;
 *         assert(str == "ScalaTest is designed to encourage clear code!")
 *         assert(lst.isEmpty)
 *         bufferActor ! Append("sweet")
 *         succeed
 *       }
 *     }
 *
 *     Scenario("User needs to understand what the tests are doing") {
 *       builderActor ! Append("be easy to reason about!")
 *       val futureString = builderActor ? GetValue
 *       val futureList = bufferActor ? GetValue
 *       val futurePair: Future[(String, List[String])] = futureString zip futureList
 *       futurePair map { case (str, lst) =&gt;
 *         assert(str == "ScalaTest is designed to be easy to reason about!")
 *         assert(lst.isEmpty)
 *         bufferActor ! Append("awesome")
 *         succeed
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * By mixing in both the `Builder` and `Buffer` traits, `ExampleSpec` gets both fixtures, which will be
 * initialized before each test and cleaned up after. The order the traits are mixed together determines the order of execution.
 * In this case, `Builder` is &ldquo;super&rdquo; to `Buffer`. If you wanted `Buffer` to be &ldquo;super&rdquo;
 * to `Builder`, you need only switch the order you mix them together, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class Example2Spec extends AsyncFeatureSpec with Buffer with Builder
 * }}}
 *
 * If you only need one fixture you mix in only that trait:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class Example3Spec extends AsyncFeatureSpec with Builder
 * }}}
 *
 * Another way to create stackable fixture traits is by extending the <a href="BeforeAndAfterEach.html">`BeforeAndAfterEach`</a>
 * and/or <a href="BeforeAndAfterAll.html">`BeforeAndAfterAll`</a> traits.
 * `BeforeAndAfterEach` has a `beforeEach` method that will be run before each test (like JUnit's `setUp`),
 * and an `afterEach` method that will be run after (like JUnit's `tearDown`).
 * Similarly, `BeforeAndAfterAll` has a `beforeAll` method that will be run before all tests,
 * and an `afterAll` method that will be run after all tests. Here's what the previously shown example would look like if it
 * were rewritten to use the `BeforeAndAfterEach` methods instead of `withFixture`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.composingbeforeandaftereach
 *
 * import org.scalatest._
 * import org.scalatest.BeforeAndAfterEach
 * import collection.mutable.ListBuffer
 * import scala.concurrent.Future
 * import scala.concurrent.ExecutionContext
 *
 * // Defining actor messages
 * sealed abstract class StringOp
 * case object Clear extends StringOp
 * case class Append(value: String) extends StringOp
 * case object GetValue
 *
 * class StringBuilderActor { // Simulating an actor
 *   private final val sb = new StringBuilder
 *   def !(op: StringOp): Unit =
 *     synchronized {
 *       op match {
 *         case Append(value) =&gt; sb.append(value)
 *         case Clear =&gt; sb.clear()
 *       }
 *     }
 *   def ?(get: GetValue.type)(implicit c: ExecutionContext): Future[String] =
 *     Future {
 *       synchronized { sb.toString }
 *     }
 * }
 *
 * class StringBufferActor {
 *   private final val buf = ListBuffer.empty[String]
 *   def !(op: StringOp): Unit =
 *     synchronized {
 *       op match {
 *         case Append(value) =&gt; buf += value
 *         case Clear =&gt; buf.clear()
 *       }
 *     }
 *   def ?(get: GetValue.type)(implicit c: ExecutionContext): Future[List[String]] =
 *     Future {
 *       synchronized { buf.toList }
 *     }
 * }
 *
 * trait Builder extends BeforeAndAfterEach { this: Suite =&gt;
 *
 *   final val builderActor = new StringBuilderActor
 *
 *   override def beforeEach() {
 *     builderActor ! Append("ScalaTest is designed to ")
 *     super.beforeEach() // To be stackable, must call super.beforeEach
 *   }
 *
 *   override def afterEach() {
 *     try super.afterEach() // To be stackable, must call super.afterEach
 *     finally builderActor ! Clear
 *   }
 * }
 *
 * trait Buffer extends BeforeAndAfterEach { this: Suite =&gt;
 *
 *   final val bufferActor = new StringBufferActor
 *
 *   override def afterEach() {
 *     try super.afterEach() // To be stackable, must call super.afterEach
 *     finally bufferActor ! Clear
 *   }
 * }
 *
 * class ExampleSpec extends AsyncFeatureSpec with Builder with Buffer {
 *
 *   Feature("Simplicity") {
 *
 *     Scenario("User needs to read test code written by others") {
 *       builderActor ! Append("encourage clear code!")
 *       val futureString = builderActor ? GetValue
 *       val futureList = bufferActor ? GetValue
 *       val futurePair: Future[(String, List[String])] = futureString zip futureList
 *       futurePair map { case (str, lst) =&gt;
 *         assert(str == "ScalaTest is designed to encourage clear code!")
 *         assert(lst.isEmpty)
 *         bufferActor ! Append("sweet")
 *         succeed
 *       }
 *     }
 *
 *     Scenario("User needs to understand what the tests are doing") {
 *       builderActor ! Append("be easy to reason about!")
 *       val futureString = builderActor ? GetValue
 *       val futureList = bufferActor ? GetValue
 *       val futurePair: Future[(String, List[String])] = futureString zip futureList
 *       futurePair map { case (str, lst) =&gt;
 *         assert(str == "ScalaTest is designed to be easy to reason about!")
 *         assert(lst.isEmpty)
 *         bufferActor ! Append("awesome")
 *         succeed
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * To get the same ordering as `withFixture`, place your `super.beforeEach` call at the end of each
 * `beforeEach` method, and the `super.afterEach` call at the beginning of each `afterEach`
 * method, as shown in the previous example. It is a good idea to invoke `super.afterEach` in a `try`
 * block and perform cleanup in a `finally` clause, as shown in the previous example, because this ensures the
 * cleanup code is performed even if `super.afterEach` throws an exception.
 * 
 *
 * The difference between stacking traits that extend `BeforeAndAfterEach` versus traits that implement `withFixture` is
 * that setup and cleanup code happens before and after the test in `BeforeAndAfterEach`, but at the beginning and
 * end of the test in `withFixture`. Thus if a `withFixture` method completes abruptly with an exception, it is
 * considered a failed test. By contrast, if any of the `beforeEach` or `afterEach` methods of `BeforeAndAfterEach`
 * complete abruptly, it is considered an aborted suite, which will result in a <a href="events/SuiteAborted.html">`SuiteAborted`</a> event.
 * 
 *
 * <a name="sharedTests"></a>==Shared tests==
 *
 * Sometimes you may want to run the same test code on different fixture objects. In other words, you may want to write tests that are "shared"
 * by different fixture objects.
 * To accomplish this in an `AsyncFeatureSpec`, you first place shared tests in
 * ''behavior functions''. These behavior functions will be
 * invoked during the construction phase of any `AsyncFeatureSpec` that uses them, so that the tests they contain will
 * be registered as tests in that `AsyncFeatureSpec`.
 * For example, given this `StackActor` class:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfeaturespec.sharedtests
 *
 * import scala.collection.mutable.ListBuffer
 * import scala.concurrent.Future
 * import scala.concurrent.ExecutionContext
 *
 * // Stack operations
 * case class Push[T](value: T)
 * sealed abstract class StackOp
 * case object Pop extends StackOp
 * case object Peek extends StackOp
 * case object Size extends StackOp
 *
 * // Stack info
 * case class StackInfo[T](top: Option[T], size: Int, max: Int) {
 *   require(size &gt;= 0, "size was less than zero")
 *   require(max &gt;= size, "max was less than size")
 *   val isFull: Boolean = size == max
 *   val isEmpty: Boolean = size == 0
 * }
 *
 * class StackActor[T](Max: Int, name: String) {
 *
 *   private final val buf = new ListBuffer[T]
 *
 *   def !(push: Push[T]): Unit =
 *     synchronized {
 *       if (buf.size != Max)
 *         buf.prepend(push.value)
 *       else
 *         throw new IllegalStateException("can't push onto a full stack")
 *     }
 *
 *   def ?(op: StackOp)(implicit c: ExecutionContext): Future[StackInfo[T]] =
 *     synchronized {
 *       op match {
 *         case Pop =&gt;
 *           Future {
 *             if (buf.size != 0)
 *               StackInfo(Some(buf.remove(0)), buf.size, Max)
 *             else
 *               throw new IllegalStateException("can't pop an empty stack")
 *           }
 *         case Peek =&gt;
 *           Future {
 *             if (buf.size != 0)
 *               StackInfo(Some(buf(0)), buf.size, Max)
 *             else
 *               throw new IllegalStateException("can't peek an empty stack")
 *           }
 *         case Size =&gt;
 *           Future { StackInfo(None, buf.size, Max) }
 *       }
 *     }
 *
 *   override def toString: String = name
 * }
 * }}}
 *
 * You may want to test the stack represented by the `StackActor` class in different states: empty, full, with one item, with one item less than capacity,
 * ''etc''. You may find you have several tests that make sense any time the stack is non-empty. Thus you'd ideally want to run
 * those same tests for three stack fixture objects: a full stack, a stack with a one item, and a stack with one item less than
 * capacity. With shared tests, you can factor these tests out into a behavior function, into which you pass the
 * stack fixture to use when running the tests. So in your `AsyncFeatureSpec` for `StackActor`, you'd invoke the
 * behavior function three times, passing in each of the three stack fixtures so that the shared tests are run for all three fixtures.
 * 
 *
 * You can define a behavior function that encapsulates these shared tests inside the `AsyncFeatureSpec` that uses them. If they are shared
 * between different `AsyncFeatureSpec`s, however, you could also define them in a separate trait that is mixed into
 * each `AsyncFeatureSpec` that uses them.
 * <a name="StackBehaviors">For</a> example, here the `nonEmptyStackActor` behavior function (in this case, a
 * behavior ''method'') is defined in a trait along with another
 * method containing shared tests for non-full stacks:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.AsyncFeatureSpec
 *
 * trait AsyncFeatureSpecStackBehaviors { this: AsyncFeatureSpec =&gt;
 *
 *   def nonEmptyStackActor(createNonEmptyStackActor: =&gt; StackActor[Int],
 *         lastItemAdded: Int, name: String): Unit = {
 *
 *     Scenario("Size is fired at non-empty stack actor: " + name) {
 *       val stackActor = createNonEmptyStackActor
 *       val futureStackInfo = stackActor ? Size
 *       futureStackInfo map { stackInfo =&gt;
 *         assert(!stackInfo.isEmpty)
 *       }
 *     }
 *
 *     Scenario("Peek is fired at non-empty stack actor: " + name) {
 *       val stackActor = createNonEmptyStackActor
 *       val futurePair: Future[(StackInfo[Int], StackInfo[Int])] =
 *         for {
 *           beforePeek &lt;- stackActor ? Size
 *           afterPeek &lt;- stackActor ? Peek
 *         } yield (beforePeek, afterPeek)
 *       futurePair map { case (beforePeek, afterPeek) =&gt;
 *         assert(afterPeek.top == Some(lastItemAdded))
 *         assert(afterPeek.size == beforePeek.size)
 *       }
 *     }
 *
 *     Scenario("Pop is fired at non-empty stack actor: " + name) {
 *       val stackActor = createNonEmptyStackActor
 *       val futurePair: Future[(StackInfo[Int], StackInfo[Int])] =
 *         for {
 *           beforePop &lt;- stackActor ? Size
 *           afterPop &lt;- stackActor ? Pop
 *         } yield (beforePop, afterPop)
 *       futurePair map { case (beforePop, afterPop) =&gt;
 *         assert(afterPop.top == Some(lastItemAdded))
 *         assert(afterPop.size == beforePop.size - 1)
 *       }
 *     }
 *   }
 *
 *   def nonFullStackActor(createNonFullStackActor: =&gt; StackActor[Int], name: String): Unit = {
 *
 *     Scenario("Size is fired at non-full stack actor: " + name) {
 *       val stackActor = createNonFullStackActor
 *       val futureStackInfo = stackActor ? Size
 *       futureStackInfo map { stackInfo =&gt;
 *         assert(!stackInfo.isFull)
 *       }
 *     }
 *
 *     Scenario("Push is fired at non-full stack actor: " + name) {
 *       val stackActor = createNonFullStackActor
 *       val futurePair: Future[(StackInfo[Int], StackInfo[Int])] =
 *         for {
 *           beforePush &lt;- stackActor ? Size
 *           afterPush &lt;- { stackActor ! Push(7); stackActor ? Peek }
 *         } yield (beforePush, afterPush)
 *       futurePair map { case (beforePush, afterPush) =&gt;
 *         assert(afterPush.size == beforePush.size + 1)
 *         assert(afterPush.top == Some(7))
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * Given these behavior functions, you could invoke them directly, but `AsyncFeatureSpec` offers a DSL for the purpose,
 * which looks like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * ScenariosFor(nonEmptyStackActor(almostEmptyStackActor, LastValuePushed, almostEmptyStackActorName))
 * ScenariosFor(nonFullStackActor(almostEmptyStackActor, almostEmptyStackActorName))
 * }}}
 *
 * Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class StackSpec extends AsyncFeatureSpec with AsyncFeatureSpecStackBehaviors {
 *
 *   val Max = 10
 *   val LastValuePushed = Max - 1
 *
 *   // Stack fixture creation methods
 *   val emptyStackActorName = "empty stack actor"
 *   def emptyStackActor = new StackActor[Int](Max, emptyStackActorName )
 *
 *   val fullStackActorName = "full stack actor"
 *   def fullStackActor = {
 *     val stackActor = new StackActor[Int](Max, fullStackActorName )
 *     for (i &lt;- 0 until Max)
 *       stackActor ! Push(i)
 *     stackActor
 *   }
 *
 *   val almostEmptyStackActorName = "almost empty stack actor"
 *   def almostEmptyStackActor = {
 *     val stackActor = new StackActor[Int](Max, almostEmptyStackActorName )
 *     stackActor ! Push(LastValuePushed)
 *     stackActor
 *   }
 *
 *   val almostFullStackActorName = "almost full stack actor"
 *   def almostFullStackActor = {
 *     val stackActor = new StackActor[Int](Max, almostFullStackActorName)
 *     for (i &lt;- 1 to LastValuePushed)
 *       stackActor ! Push(i)
 *     stackActor
 *   }
 *
 *   Feature("A Stack is pushed and popped") {
 *
 *     Scenario("Size is fired at empty stack actor") {
 *       val stackActor = emptyStackActor
 *       val futureStackInfo = stackActor ? Size
 *       futureStackInfo map { stackInfo =&gt;
 *         assert(stackInfo.isEmpty)
 *       }
 *     }
 *
 *     Scenario("Peek is fired at empty stack actor") {
 *       recoverToSucceededIf[IllegalStateException] {
 *         emptyStackActor ? Peek
 *       }
 *     }
 *
 *     Scenario("Pop is fired at empty stack actor") {
 *       recoverToSucceededIf[IllegalStateException] {
 *         emptyStackActor ? Pop
 *       }
 *     }
 *
 *     ScenariosFor(nonEmptyStackActor(almostEmptyStackActor, LastValuePushed, almostEmptyStackActorName))
 *     ScenariosFor(nonFullStackActor(almostEmptyStackActor, almostEmptyStackActorName))
 *
 *     ScenariosFor(nonEmptyStackActor(almostFullStackActor, LastValuePushed, almostFullStackActorName))
 *     ScenariosFor(nonFullStackActor(almostFullStackActor, almostFullStackActorName))
 *
 *     Scenario("full is invoked on a full stack") {
 *       val stackActor = fullStackActor
 *       val futureStackInfo = stackActor ? Size
 *       futureStackInfo map { stackInfo =&gt;
 *         assert(stackInfo.isFull)
 *       }
 *     }
 *
 *     ScenariosFor(nonEmptyStackActor(fullStackActor, LastValuePushed, fullStackActorName))
 *
 *     Scenario("push is invoked on a full stack") {
 *       val stackActor = fullStackActor
 *       assertThrows[IllegalStateException] {
 *         stackActor ! Push(10)
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * If you load these classes into the Scala interpreter (with scalatest's JAR file on the class path), and execute it,
 * you'll see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new StackSpec)
 * <span class="stGreen">StackSpec:
 * Feature: A Stack actor
 * - Scenario: Size is fired at empty stack actor
 * - Scenario: Peek is fired at empty stack actor
 * - Scenario: Pop is fired at empty stack actor
 * - Scenario: Size is fired at non-empty stack actor: almost empty stack actor
 * - Scenario: Peek is fired at non-empty stack actor: almost empty stack actor
 * - Scenario: Pop is fired at non-empty stack actor: almost empty stack actor
 * - Scenario: Size is fired at non-full stack actor: almost empty stack actor
 * - Scenario: Push is fired at non-full stack actor: almost empty stack actor
 * - Scenario: Size is fired at non-empty stack actor: almost full stack actor
 * - Scenario: Peek is fired at non-empty stack actor: almost full stack actor
 * - Scenario: Pop is fired at non-empty stack actor: almost full stack actor
 * - Scenario: Size is fired at non-full stack actor: almost full stack actor
 * - Scenario: Push is fired at non-full stack actor: almost full stack actor
 * - Scenario: Size is fired at full stack actor
 * - Scenario: Size is fired at non-empty stack actor: full stack actor
 * - Scenario: Peek is fired at non-empty stack actor: full stack actor
 * - Scenario: Pop is fired at non-empty stack actor: full stack actor
 * - Scenario: Push is fired at full stack actor
</span>
 * }}}
 *
 * One thing to keep in mind when using shared tests is that in ScalaTest, each test in a suite must have a unique name.
 * If you register the same tests repeatedly in the same suite, one problem you may encounter is an exception at runtime
 * complaining that multiple tests are being registered with the same test name.
 * Although in an `AsyncFeatureSpec`, the `feature` clause is a nesting construct analogous to
 * `AsyncFunSpec`'s `describe` clause, you many sometimes need to do a bit of
 * extra work to ensure that the test names are unique. If a duplicate test name problem shows up in an
 * `AsyncFeatureSpec`, you'll need to pass in a prefix or suffix string to add to each test name. You can call
 * `toString` on the shared fixture object, or pass this string
 * the same way you pass any other data needed by the shared tests.
 * This is the approach taken by the previous `AsyncFeatureSpecStackBehaviors` example.
 * 
 *
 * Given this `AsyncFeatureSpecStackBehaviors` trait, calling it with the `almostEmptyStackActor` fixture, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * ScenariosFor(nonEmptyStackActor(almostEmptyStackActor, LastValuePushed, almostEmptyStackActorName))
 * }}}
 *
 * yields test names:
 * 
 *
 * <ul>
 * <li>`Size is fired at non-empty stack actor: almost empty stack actor`</li>
 * <li>`Peek is fired at non-empty stack actor: almost empty stack actor`</li>
 * <li>`Pop is fired at non-empty stack actor: almost empty stack actor`</li>
 * </ul>
 *
 * Whereas calling it with the `almostFullStackActor` fixture, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * ScenariosFor(nonEmptyStack(almostFullStackActor, lastValuePushed, almostFullStackActorName))
 * }}}
 *
 * yields different test names:
 * 
 *
 * <ul>
 * <li>`Size is fired at non-empty stack actor: almost full stack actor`</li>
 * <li>`Peek is fired at non-empty stack actor: almost full stack actor`</li>
 * <li>`Pop is fired at non-empty stack actor: almost full stack actor`</li>
 * </ul>
 */
abstract class AsyncFeatureSpec extends AsyncFeatureSpecLike {

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
