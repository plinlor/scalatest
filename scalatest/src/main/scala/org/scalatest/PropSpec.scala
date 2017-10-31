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
 * A suite of property-based tests.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * Class `PropSpec` is a good fit for teams that want to write tests exclusively in terms of property checks, and is also a good choice
 * for writing the occasional <a href="#testMatrix">test matrix</a> when a different style trait is chosen as the main unit testing style.
 * </td></tr></table>
 * 
 * Here's an example `PropSpec`:
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.propspec
 * 
 * import org.scalatest._
 * import prop._
 * import scala.collection.immutable._
 * 
 * class SetSpec extends PropSpec with TableDrivenPropertyChecks with Matchers {
 * 
 *   val examples =
 *     Table(
 *       "set",
 *       BitSet.empty,
 *       HashSet.empty[Int],
 *       TreeSet.empty[Int]
 *     )
 *   
 *   property("an empty Set should have size 0") {
 *     forAll(examples) { set =&gt;
 *       set.size should be (0)
 *     }
 *   }
 * 
 *   property("invoking head on an empty set should produce NoSuchElementException") {
 *     forAll(examples) { set =&gt;
 *       a [NoSuchElementException] should be thrownBy { set.head }
 *     }
 *   }
 * }
 * }}}
 *
 * You can run a `PropSpec` by invoking `execute` on it.
 * This method, which prints test results to the standard output, is intended to serve as a
 * convenient way to run tests from within the Scala interpreter. For example,
 * to run `SetSpec` from within the Scala interpreter, you could write:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * }}}
 *
 * And you would see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">SetSpec:
 * - an empty Set should have size 0
 * - invoking head on an empty Set should produce NoSuchElementException</span>
 * }}}
 *
 * Or, to run just the &ldquo;`an empty Set should have size 0`&rdquo; method, you could pass that test's name, or any unique substring of the
 * name, such as `"size 0"` or even just `"0"`. Here's an example:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec, "size 0")
 * <span class="stGreen">SetSpec:
 * - an empty Set should have size 0</span>
 * }}}
 *
 * You can also pass to `execute` a <a href="ConfigMap.html">''config map''</a> of key-value
 * pairs, which will be passed down into suites and tests, as well as other parameters that configure the run itself.
 * For more information on running in the Scala interpreter, see the documentation for `execute` (below) and the
 * <a href="Shell.html">ScalaTest shell</a>.
 * 
 *
 * The `execute` method invokes a `run` method that takes two
 * parameters. This `run` method, which actually executes the suite, will usually be invoked by a test runner, such
 * as <a href="run$.html">`run`</a>, <a href="tools/Runner$.html">`tools.Runner`</a>, a build tool, or an IDE.
 * 
 *
 * &ldquo;`property`&rdquo; is a method, defined in `PropSpec`, which will be invoked
 * by the primary constructor of `SetSpec`. You specify the name of the test as
 * a string between the parentheses, and the test code itself between curly braces.
 * The test code is a function passed as a by-name parameter to `property`, which registers
 * it for later execution.
 * 
 *
 * A `PropSpec`'s lifecycle has two phases: the ''registration'' phase and the
 * ''ready'' phase. It starts in registration phase and enters ready phase the first time
 * `run` is called on it. It then remains in ready phase for the remainder of its lifetime.
 * 
 *
 * Tests can only be registered with the `property` method while the `PropSpec` is
 * in its registration phase. Any attempt to register a test after the `PropSpec` has
 * entered its ready phase, ''i.e.'', after `run` has been invoked on the `PropSpec`,
 * will be met with a thrown <a href="exceptions/TestRegistrationClosedException.html">`TestRegistrationClosedException`</a>. The recommended style
 * of using `PropSpec` is to register tests during object construction as is done in all
 * the examples shown here. If you keep to the recommended style, you should never see a
 * `TestRegistrationClosedException`.
 * 
 *
 * ==Ignored tests==
 *
 * To support the common use case of temporarily disabling a test, with the
 * good intention of resurrecting the test at a later time, `PropSpec` provides registration
 * methods that start with `ignore` instead of `property`. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.suite.ignore
 * 
 * import org.scalatest._
 * import prop._
 * import scala.collection.immutable._
 * 
 * class SetSpec extends PropSpec with TableDrivenPropertyChecks with Matchers {
 * 
 *   val examples =
 *     Table(
 *       "set",
 *       BitSet.empty,
 *       HashSet.empty[Int],
 *       TreeSet.empty[Int]
 *     )
 * 
 *   ignore("an empty Set should have size 0") {
 *     forAll(examples) { set =>
 *       set.size should be (0)
 *     }
 *   }
 * 
 *   property("invoking head on an empty set should produce NoSuchElementException") {
 *     forAll(examples) { set =>
 *       a [NoSuchElementException] should be thrownBy { set.head }
 *     }
 *   }
 * }
 * }}}
 *
 * If you run this version of `SetSuite` with:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * }}}
 *
 * It will run only the second test and report that the first test was ignored:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">SetSuite:</span>
 * <span class="stYellow">- an empty Set should have size 0 !!! IGNORED !!!</span>
 * <span class="stGreen">- invoking head on an empty Set should produce NoSuchElementException</span>
 * }}}
 *
 * <a name="informers"></a>==Informers==
 *
 * One of the parameters to `PropSpec`'s `run` method is a <a href="Reporter.html">`Reporter`</a>, which
 * will collect and report information about the running suite of tests.
 * Information about suites and tests that were run, whether tests succeeded or failed, 
 * and tests that were ignored will be passed to the `Reporter` as the suite runs.
 * Most often the reporting done by default by `PropSpec`'s methods will be sufficient, but
 * occasionally you may wish to provide custom information to the `Reporter` from a test.
 * For this purpose, an <a href="Informer.html">`Informer`</a> that will forward information
 * to the current `Reporter` is provided via the `info` parameterless method.
 * You can pass the extra information to the `Informer` via its `apply` method.
 * The `Informer` will then pass the information to the `Reporter` via an <a href="events/InfoProvided.html">`InfoProvided`</a> event.
 * Here's an example that shows both a direct use as well as an indirect use through the methods
 * of <a href="GivenWhenThen.html">`GivenWhenThen`</a>:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.propspec.info
 * 
 * import org.scalatest._
 * import prop._
 * import collection.mutable
 * 
 * class SetSpec extends PropSpec with TableDrivenPropertyChecks with GivenWhenThen {
 * 
 *   val examples =
 *     Table(
 *       "set",
 *       mutable.BitSet.empty,
 *       mutable.HashSet.empty[Int],
 *       mutable.LinkedHashSet.empty[Int]
 *     )
 * 
 *   property("an element can be added to an empty mutable Set") {
 * 
 *     forAll(examples) { set =&gt;
 * 
 *       info("----------------")
 * 
 *       Given("an empty mutable " + set.getClass.getSimpleName)
 *       assert(set.isEmpty)
 * 
 *       When("an element is added")
 *       set += 99
 * 
 *       Then("the Set should have size 1")
 *       assert(set.size === 1)
 * 
 *       And("the Set should contain the added element")
 *       assert(set.contains(99))
 *     }
 *   }
 * }
 * }}}
 *
 *
 * If you run this `PropSpec` from the interpreter, you will see the following output:
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * <span class="stGreen">SetSpec:
 * - an element can be added to an empty mutable Set
 *   + ---------------- 
 *   + Given an empty mutable BitSet 
 *   + When an element is added 
 *   + Then the Set should have size 1 
 *   + And the Set should contain the added element 
 *   + ---------------- 
 *   + Given an empty mutable HashSet 
 *   + When an element is added 
 *   + Then the Set should have size 1 
 *   + And the Set should contain the added element 
 *   + ---------------- 
 *   + Given an empty mutable LinkedHashSet 
 *   + When an element is added 
 *   + Then the Set should have size 1 
 *   + And the Set should contain the added element</span>
 * }}}
 *
 * <a name="documenters"></a>==Documenters==
 *
 * `PropSpec` also provides a `markup` method that returns a <a href="Documenter.html">`Documenter`</a>, which allows you to send
 * to the `Reporter` text formatted in <a href="http://daringfireball.net/projects/markdown/" target="_blank">Markdown syntax</a>.
 * You can pass the extra information to the `Documenter` via its `apply` method.
 * The `Documenter` will then pass the information to the `Reporter` via an <a href="events/MarkupProvided.html">`MarkupProvided`</a> event.
 * 
 *
 * Here's an example `PropSpec` that uses `markup`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.propspec.markup
 *
 * import org.scalatest._
 * import prop._
 * import collection.mutable
 *
 * class SetSpec extends PropSpec with TableDrivenPropertyChecks with GivenWhenThen {
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
 *
 * If you wish that methods like `take`,
 * `drop`, `filter` return the same kind of set,
 * you should also override:
 *
 *     def empty: This
 *
 * It is also good idea to override methods `foreach` and
 * `size` for efficiency.
 *
 *   """ }
 *
 *   val examples =
 *     Table(
 *       "set",
 *       mutable.BitSet.empty,
 *       mutable.HashSet.empty[Int],
 *       mutable.LinkedHashSet.empty[Int]
 *     )
 *
 *   property("an element can be added to an empty mutable Set") {
 *
 *     forAll(examples) { set =>
 *
 *       info("----------------")
 *
 *       Given("an empty mutable " + set.getClass.getSimpleName)
 *       assert(set.isEmpty)
 *
 *       When("an element is added")
 *       set += 99
 *
 *       Then("the Set should have size 1")
 *       assert(set.size === 1)
 *
 *       And("the Set should contain the added element")
 *       assert(set.contains(99))
 *     }
 *
 *     markup("This test finished with a **bold** statement!")
 *   }
 * }
 * }}}
 *
 * Although all of ScalaTest's built-in reporters will display the markup text in some form,
 * the HTML reporter will format the markup information into HTML. Thus, the main purpose of `markup` is to
 * add nicely formatted text to HTML reports. Here's what the above `SetSpec` would look like in the HTML reporter:
 * 
 *
 * <img class="stScreenShot" src="../../lib/propSpec.gif">
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
 * package org.scalatest.examples.propspec.note
 *
 * import org.scalatest._
 * import prop._
 * import collection.mutable
 *
 * class SetSpec extends PropSpec with TableDrivenPropertyChecks {
 *
 *   val examples =
 *     Table(
 *       "set",
 *       mutable.BitSet.empty,
 *       mutable.HashSet.empty[Int],
 *       mutable.LinkedHashSet.empty[Int]
 *     )
 *
 *   property("an element can be added to an empty mutable Set") {
 *
 *     info("info is recorded")
 *     markup("markup is *also* recorded")
 *     note("notes are sent immediately")
 *     alert("alerts are also sent immediately")
 *
 *     forAll(examples) { set =>
 *
 *       assert(set.isEmpty)
 *       set += 99
 *       assert(set.size === 1)
 *       assert(set.contains(99))
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
 *   + notes are sent immediately</span>
 *   <span class="stYellow">+ alerts are also sent immediately</span>
 * <span class="stGreen">- an element can be added to an empty mutable Set
 *   + info is recorded
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
 * bit of behavior required by the system being tested. The test can also include some code that
 * sends more information about the behavior to the reporter when the tests run. At the end of the test,
 * it can call method `pending`, which will cause it to complete abruptly with <a href="exceptions/TestPendingException.html">`TestPendingException`</a>.
 * 
 *
 * Because tests in ScalaTest can be designated as pending with `TestPendingException`, both the test name and any information
 * sent to the reporter when running the test can appear in the report of a test run. 
 * (The code of a pending test is executed just like any other test.) However, because the test completes abruptly
 * with `TestPendingException`, the test will be reported as pending, to indicate
 * the actual test, and possibly the functionality, has not yet been implemented.
 * 
 *
 * You can mark tests pending in `PropSpec` like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest._
 * import prop._
 * import scala.collection.immutable._
 * 
 * class SetSpec extends PropSpec with TableDrivenPropertyChecks with Matchers {
 * 
 *   val examples =
 *     Table(
 *       "set",
 *       BitSet.empty,
 *       HashSet.empty[Int],
 *       TreeSet.empty[Int]
 *     )
 * 
 *   property("an empty Set should have size 0") (pending)
 * 
 *   property("invoking head on an empty set should produce NoSuchElementException") {
 *     forAll(examples) { set =&gt;
 *       a [NoSuchElementException] should be thrownBy { set.head }
 *     }
 *   }
 * }
 * }}}
 *
 * (Note: "`(pending)`" is the body of the test. Thus the test contains just one statement, an invocation
 * of the `pending` method, which throws `TestPendingException`.)
 * If you run this version of `SetSuite` with:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSuite)
 * }}}
 *
 * It will run both tests, but report that first test is pending. You'll see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">SetSuite:</span>
 * <span class="stYellow">- An empty Set should have size 0 (pending)</span>
 * <span class="stGreen">- Invoking head on an empty Set should produce NoSuchElementException</span>
 * }}}
 * 
 * One difference between an ignored test and a pending one is that an ignored test is intended to be used during a
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
 * the body of pending tests are executed up until they throw `TestPendingException`. The reason for this difference
 * is that it enables your unfinished test to send `InfoProvided` messages to the reporter before it completes
 * abruptly with `TestPendingException`, as shown in the previous example on `Informer`s
 * that used the `GivenWhenThen` trait.
 * 
 *
 * <a name="taggingTests"></a>==Tagging tests==
 *
 * A `PropSpec`'s tests may be classified into groups by ''tagging'' them with string names.
 * As with any suite, when executing a `PropSpec`, groups of tests can
 * optionally be included and/or excluded. To tag a `PropSpec`'s tests,
 * you pass objects that extend class `org.scalatest.Tag` to methods
 * that register tests. Class `Tag` takes one parameter, a string name.  If you have
 * created tag annotation interfaces as described in the <a href="Tag.html">`Tag` documentation</a>, then you
 * will probably want to use tag names on your test functions that match. To do so, simply 
 * pass the fully qualified names of the tag interfaces to the `Tag` constructor. For example, if you've
 * defined a tag annotation interface with fully qualified names,
 * `com.mycompany.tags.DbTest`, then you could
 * create a matching tag for `PropSpec`s like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.propspec.tagging
 *
 * import org.scalatest.Tag
 *
 * object DbTest extends Tag("com.mycompany.tags.DbTest")
 * }}}
 *
 * Given these definitions, you could place `PropSpec` tests into groups with tags like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest._
 * import prop._
 * import tagobjects.Slow
 * import scala.collection.immutable._
 * 
 * class SetSpec extends PropSpec with TableDrivenPropertyChecks with Matchers {
 * 
 *   val examples =
 *     Table(
 *       "set",
 *       BitSet.empty,
 *       HashSet.empty[Int],
 *       TreeSet.empty[Int]
 *     )
 * 
 *   property("an empty Set should have size 0", Slow) {
 *     forAll(examples) { set =&gt;
 *       set.size should be (0)
 *     }
 *   }
 * 
 *   property("invoking head on an empty set should produce NoSuchElementException",
 *       Slow, DbTest) {
 * 
 *     forAll(examples) { set =&gt;
 *       a [NoSuchElementException] should be thrownBy { set.head }
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
 * ScalaTest recommends three techniques to eliminate such code duplication:
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
 * mutable state across tests will make your test code easier to reason about and more amenable for parallel
 * test execution.
 *
 * The techniques in `PropSpec` are identical to those in `FunSuite`, but with &ldquo;`test`&rdquo;
 * replaced by &ldquo;`property`&rdquo;. The following table summarizes the options with a link to the relevant
 * documentation for trait `FunSuite`:
 * 
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
 *     <a href="FunSuite.html#getFixtureMethods">get-fixture methods</a>
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     The ''extract method'' refactor helps you create a fresh instances of mutable fixture objects in each test
 *     that needs them, but doesn't help you clean them up when you're done.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="FunSuite.html#fixtureContextObjects">fixture-context objects</a>
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     By placing fixture methods and fields into traits, you can easily give each test just the newly created
 *     fixtures it needs by mixing together traits.  Use this technique when you need ''different combinations
 *     of mutable fixture objects in different tests'', and don't need to clean up after.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="FunSuite.html#loanFixtureMethods">loan-fixture methods</a>
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
 *     <a href="FunSuite.html#withFixtureNoArgTest">
 *       `withFixture(NoArgTest)`</a>
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
 *  <dd style="display: list-item; list-style-type: disc; margin-left: 1.2em;">You have objects to pass into tests (override `withFixture(''One''ArgTest)` instead)</dd>
 *  </dl>
 *  </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="FunSuite.html#withFixtureOneArgTest">
 *       `withFixture(OneArgTest)`
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
 *     <a href="FunSuite.html#beforeAndAfter">`BeforeAndAfter`</a>
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     Use this boilerplate-buster when you need to perform the same side-effects before and/or after tests, rather than at the beginning or end of tests.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 *     <a href="FunSuite.html#composingFixtures">`BeforeAndAfterEach`</a>
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 *     Use when you want to ''stack traits'' that perform the same side-effects before and/or after tests, rather than at the beginning or end of tests.
 *   </td>
 * </tr>
 *
 * </table>
 *
 * <a name="testMatrix"></a>
 * ====Using `PropSpec` to implement a test matrix====
 *
 * Using fixture-context objects in a `PropSpec` is a good way to implement a test matrix.
 * What is the matrix? A test matrix is a series of tests that you need to run on a series of subjects. For example, The Scala API contains
 * many implementations of trait `Set`. Every implementation must obey the contract of `Set`. 
 * One property of any `Set` is that an empty `Set` should have size 0, another is that
 * invoking head on an empty `Set` should give you a `NoSuchElementException`, and so on. Already you have a matrix,
 * where rows are the properties and the columns are the set implementations:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">&nbsp;</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">`BitSet`</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">`HashSet`</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">`TreeSet`</th></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">An empty Set should have size 0</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><span class="stGreen">pass</span></td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><span class="stGreen">pass</span></td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><span class="stGreen">pass</span></td></td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">Invoking head on an empty set should produce NoSuchElementException</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><span class="stGreen">pass</span></td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><span class="stGreen">pass</span></td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><span class="stGreen">pass</span></td></td></tr>
 * </table>
 *
 * One way to implement this test matrix is to define a trait to represent the columns (in this case, `BitSet`, `HashSet`,
 * and `TreeSet`) as elements in a single-dimensional `Table`. Each element in the `Table` represents
 * one `Set` implementation. Because different properties may require different fixture instances for those implementations, you
 * can define a trait to hold the examples, like this:
 *
 * {{{  <!-- class="stHighlight" -->
 * trait SetExamples extends Tables {
 *
 *   def examples = Table("set", bitSet, hashSet, treeSet)
 * 
 *   def bitSet: BitSet
 *   def hashSet: HashSet[Int]
 *   def treeSet: TreeSet[Int]
 * }
 * }}}
 *
 * Given this trait, you could provide empty sets in one implementation of `SetExamples`, and non-empty sets in another.
 * Here's how you might provide empty set examples:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class EmptySetExamples extends SetExamples {
 *   def bitSet = BitSet.empty
 *   def hashSet = HashSet.empty[Int]
 *   def treeSet = TreeSet.empty[Int]
 * }
 * }}}
 * 
 * And here's how you might provide set examples with one item each:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class SetWithOneItemExamples extends SetExamples {
 *   def bitSet = BitSet(1)
 *   def hashSet = HashSet(1)
 *   def treeSet = TreeSet(1)
 * }
 * }}}
 * 
 * Armed with these example classes, you can define checks of properties that require
 * empty or non-empty set fixtures by using instances of these classes as fixture-context
 * objects. In other words, the columns of the test matrix are implemented as elements of
 * a one-dimensional table of fixtures, the rows are implemented as `property`
 * clauses of a `PropSpec`.
 * 
 *
 * Here's a complete example that checks the two properties mentioned previously:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.propspec.matrix
 * 
 * import org.scalatest._
 * import org.scalatest.prop._
 * import scala.collection.immutable._
 * 
 * trait SetExamples extends Tables {
 *
 *   def examples = Table("set", bitSet, hashSet, treeSet)
 * 
 *   def bitSet: BitSet
 *   def hashSet: HashSet[Int]
 *   def treeSet: TreeSet[Int]
 * }
 * 
 * class EmptySetExamples extends SetExamples {
 *   def bitSet = BitSet.empty
 *   def hashSet = HashSet.empty[Int]
 *   def treeSet = TreeSet.empty[Int]
 * }
 * 
 * class SetSpec extends PropSpec with TableDrivenPropertyChecks with Matchers {
 * 
 *   property("an empty Set should have size 0") {
 *     new EmptySetExamples {
 *       forAll(examples) { set =&gt;
 *         set.size should be (0)
 *       }
 *     }
 *   }
 * 
 *   property("invoking head on an empty set should produce NoSuchElementException") {
 *     new EmptySetExamples {
 *       forAll(examples) { set =&gt;
 *         a [NoSuchElementException] should be thrownBy { set.head }
 *       }
 *     }
 *   }
 * }
 * }}}
 * 
 * One benefit of this approach is that the compiler will help you when you need to add either a new row
 * or column to the matrix. In either case, you'll need to ensure all cells are checked to get your code to compile.
 * 
 *
 * <a name="sharedTests"></a>==Shared tests==
 *
 * Sometimes you may want to run the same test code on different fixture objects. That is to say, you may want to write tests that are "shared"
 * by different fixture objects.
 * You accomplish this in a `PropSpec` in the same way you would do it in a `FunSuite`, except instead of `test`
 * you say `property`, and instead of `testsFor` you say `propertiesFor`. 
 * For more information, see the <a href="FunSuite.html#sharedTests">Shared tests</a> section of `FunSuite`'s
 * documentation.
 * 
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.PropSpecFinder"))
class PropSpec extends PropSpecLike {

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
