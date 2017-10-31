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
 * Facilitates a &ldquo;behavior-driven&rdquo; style of development (BDD), in which tests
 * are combined with text that specifies the behavior the tests verify.
 * 
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * Class `FlatSpec` is a good first step for teams wishing to move from xUnit to BDD, because its structure is flat like xUnit, so simple and familiar, 
 * but the test names must be written in a specification style: &ldquo;X should Y,&rdquo; &ldquo;A must B,&rdquo; ''etc.  ''
 * </td></tr></table>
 * 
 * Trait `FlatSpec` is so named because
 * your specification text and tests line up flat against the left-side indentation level, with no nesting needed.
 * Here's an example `FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec
 * 
 * import org.scalatest.FlatSpec
 * 
 * class SetSpec extends FlatSpec {
 * 
 *   behavior of "An empty Set"
 *   
 *   it should "have size 0" in {
 *     assert(Set.empty.size === 0)
 *   }
 *     
 *   it should "produce NoSuchElementException when head is invoked" in {
 *     assertThrows[NoSuchElementException] {
 *       Set.empty.head
 *     }
 *   }
 * }
 * }}}
 *
 * Note: you can use `must` or `can` as well as `should` in a `FlatSpec`. For example, instead of
 * `it should "have`..., you could write `it must "have`... or `it can "have`....
 * 
 *
 * Instead of using a `behavior of` clause, you can alternatively use a shorthand syntax in which you replace
 * the first `it` with the subject string, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec
 * 
 * import org.scalatest.FlatSpec
 * 
 * class SetSpec extends FlatSpec {
 *   
 *   "An empty Set" should "have size 0" in {
 *     assert(Set.empty.size === 0)
 *   }
 *     
 *   it should "produce NoSuchElementException when head is invoked" in {
 *     assertThrows[NoSuchElementException] {
 *       Set.empty.head
 *     }
 *   }
 * }
 * }}}
 *
 * Running either of the two previous versions of `SetSpec` in the Scala interpreter would yield:
 * 
 * 
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">An empty Set
 * - should have size 0
 * - should produce NoSuchElementException when head is invoked</span>
 * }}}
 *
 * In a `FlatSpec` you write a one (or more) sentence specification for each bit of behavior you wish to
 * specify and test. Each specification sentence has a
 * "subject," which is sometimes called the ''system under test'' (or SUT). The 
 * subject is the entity being specified and tested and also serves as the subject of the sentences you write for each test.
 * Often you will want to write multiple tests for the same subject. In a `FlatSpec`, you name the subject once,
 * with a `behavior of` clause or its shorthand, then write tests for that subject with `it should`/`must`/`can "do something"` phrases.
 * Each `it` refers to the most recently declared subject. For example, the four tests shown in this snippet are all testing
 * a stack that contains one item:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * behavior of "A Stack (with one item)"
 *
 * it should "be non-empty" in {}
 *
 * it should "return the top item on peek" in {}
 *
 * it should "not remove the top item on peek" in {}
 *
 * it should "remove the top item on pop" in {}
 * }}}
 * 
 * The same is true if the tests are written using the shorthand notation:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (with one item)" should "be non-empty" in {}
 *
 * it should "return the top item on peek" in {}
 *
 * it should "not remove the top item on peek" in {}
 *
 * it should "remove the top item on pop" in {}
 * }}}
 * 
 * In a `FlatSpec`, therefore, to figure out what "`it`" means, you just scan vertically until you find the most
 * recent use of `behavior of` or the shorthand notation.
 * 
 *
 * Because sometimes the subject could be plural, you can alternatively use `they` instead of `it`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "The combinators" should "be easy to learn" in {}
 *
 * they should "be efficient" in {}
 *
 * they should "do something cool" in {}
 * }}}
 * 
 * A `FlatSpec`'s lifecycle has two phases: the ''registration'' phase and the
 * ''ready'' phase. It starts in registration phase and enters ready phase the first time
 * `run` is called on it. It then remains in ready phase for the remainder of its lifetime.
 * 
 *
 * Tests can only be registered while the `FlatSpec` is
 * in its registration phase. Any attempt to register a test after the `FlatSpec` has
 * entered its ready phase, ''i.e.'', after `run` has been invoked on the `FlatSpec`,
 * will be met with a thrown <a href="exceptions/TestRegistrationClosedException.html">`TestRegistrationClosedException`</a>. The recommended style
 * of using `FlatSpec` is to register tests during object construction as is done in all
 * the examples shown here. If you keep to the recommended style, you should never see a
 * `TestRegistrationClosedException`.
 * 
 *
 * <a name="ignoredTests"></a>==Ignored tests==
 *
 * To support the common use case of temporarily disabling a test, with the
 * good intention of resurrecting the test at a later time, `FlatSpec` provides a method
 * `ignore` that can be used instead of `it` or `they` to register a test. For example, to temporarily
 * disable the test with the name `"An empty Set should produce NoSuchElementException when head is invoked"`, just
 * change &ldquo;`it`&rdquo; into &#8220;`ignore`,&#8221; like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.ignore
 * 
 * import org.scalatest.FlatSpec
 * 
 * class SetSpec extends FlatSpec {
 *   
 *   "An empty Set" should "have size 0" in {
 *     assert(Set.empty.size === 0)
 *   }
 *     
 *   ignore should "produce NoSuchElementException when head is invoked" in {
 *     assertThrows[NoSuchElementException] {
 *       Set.empty.head
 *     }
 *   }
 * }
 * }}}
 *
 * If you run this version of `SetSpec` with:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * }}}
 *
 * It will run only the first test and report that the second test was ignored:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">An empty Set</span>
 * <span class="stGreen">- should have size 0</span>
 * <span class="stYellow">- should produce NoSuchElementException when head is invoked !!! IGNORED !!!</span>
 * }}}
 *
 * When using shorthand notation, you won't have an `it` to change into `ignore` for
 * the first test of each new subject. To ignore such tests, you must instead change `in` to `ignore`.
 * For example, to temporarily disable the test with the name `"An empty Set should have size 0"`,
 * change &ldquo;`in`&rdquo; into &#8220;`ignore`&#8221; like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.ignoreafter
 * 
 * import org.scalatest.FlatSpec
 * 
 * class SetSpec extends FlatSpec {
 *   
 *   "An empty Set" should "have size 0" ignore {
 *     assert(Set.empty.size === 0)
 *   }
 *     
 *   it should "produce NoSuchElementException when head is invoked" in {
 *     assertThrows[NoSuchElementException] {
 *       Set.empty.head
 *     }
 *   }
 * }
 * }}}
 *
 * If you run this version of `StackSpec` with:
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
 * <span class="stGreen">An empty Set</span>
 * <span class="stYellow">- should have size 0 !!! IGNORED !!!</span>
 * <span class="stGreen">- should produce NoSuchElementException when head is invoked</span>
 * }}}
 *
 * If you wish to temporarily ignore an entire suite of tests, you can (on the JVM, not Scala.js) annotate the test class with `@Ignore`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.ignoreall
 * 
 * import org.scalatest._
 * 
 * @Ignore
 * class SetSpec extends FlatSpec {
 * 
 *   "An empty Set" should "have size 0" in {
 *     assert(Set.empty.size === 0)
 *   }
 * 
 *   it should "produce NoSuchElementException when head is invoked" in {
 *     assertThrows[NoSuchElementException] {
 *       Set.empty.head
 *     }
 *   }
 * }
 * }}}
 *
 * When you mark a test class with a tag annotation, ScalaTest will mark each test defined in that class with that tag.
 * Thus, marking the `SetSpec` in the above example with the `@Ignore` tag annotation means that both tests
 * in the class will be ignored. If you run the above `SetSpec` in the Scala interpreter, you'll see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * <span class="stGreen">SetSpec:
 * An empty Set</span>
 * <span class="stYellow">- should have size 0 !!! IGNORED !!!
 * - should produce NoSuchElementException when head is invoked !!! IGNORED !!!</span>
 * }}}
 *
 * Note that marking a test class as ignored won't prevent it from being discovered by ScalaTest. Ignored classes
 * will be discovered and run, and all their tests will be reported as ignored. This is intended to keep the ignored
 * class visible, to encourage the developers to eventually fix and &ldquo;un-ignore&rdquo; it. If you want to
 * prevent a class from being discovered at all (on the JVM, not Scala.js), use the <a href="DoNotDiscover.html">`DoNotDiscover`</a> annotation instead.
 * 
 *
 * <a name="informers"></a>==Informers==
 *
 * One of the parameters to `FlatSpec`'s `run` method is a <a href="Reporter.html">`Reporter`</a>, which
 * will collect and report information about the running suite of tests.
 * Information about suites and tests that were run, whether tests succeeded or failed, 
 * and tests that were ignored will be passed to the `Reporter` as the suite runs.
 * Most often the reporting done by default by `FlatSpec`'s methods will be sufficient, but
 * occasionally you may wish to provide custom information to the `Reporter` from a test.
 * For this purpose, an <a href="Informer.html">`Informer`</a> that will forward information to the current `Reporter`
 * is provided via the `info` parameterless method.
 * You can pass the extra information to the `Informer` via its `apply` method.
 * The `Informer` will then pass the information to the `Reporter` via an <a href="events/InfoProvided.html">`InfoProvided`</a> event.
 * 
 * 
 * One use case for the `Informer` is to pass more information about a specification to the reporter. For example,
 * the <a href="GivenWhenThen.html">`GivenWhenThen`</a> trait provides methods that use the implicit `info` provided by `FlatSpec`
 * to pass such information to the reporter.  Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.info
 * 
 * import collection.mutable
 * import org.scalatest._
 * 
 * class SetSpec extends FlatSpec with GivenWhenThen {
 *   
 *   "A mutable Set" should "allow an element to be added" in {
 *     Given("an empty mutable Set")
 *     val set = mutable.Set.empty[String]
 * 
 *     When("an element is added")
 *     set += "clarity"
 * 
 *     Then("the Set should have size 1")
 *     assert(set.size === 1)
 * 
 *     And("the Set should contain the added element")
 *     assert(set.contains("clarity"))
 * 
 *     info("That's all folks!")
 *   }
 * }
 * }}}
 *
 * If you run this `FlatSpec` from the interpreter, you will see the following output:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * <span class="stGreen">SetSpec:
 * A mutable Set
 * - should allow an element to be added
 *   + Given an empty mutable Set 
 *   + When an element is added 
 *   + Then the Set should have size 1 
 *   + And the Set should contain the added element 
 *   + That's all folks! </span>
 * }}}
 *
 * <a name="documenters"></a>==Documenters==
 *
 * `FlatSpec` also provides a `markup` method that returns a <a href="Documenter.html">`Documenter`</a>, which allows you to send
 * to the `Reporter` text formatted in <a href="http://daringfireball.net/projects/markdown/" target="_blank">Markdown syntax</a>.
 * You can pass the extra information to the `Documenter` via its `apply` method.
 * The `Documenter` will then pass the information to the `Reporter` via an <a href="events/MarkupProvided.html">`MarkupProvided`</a> event.
 * 
 *
 * Here's an example `FlatSpec` that uses `markup`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.markup
 *
 * import collection.mutable
 * import org.scalatest._
 *
 * class SetSpec extends FlatSpec with GivenWhenThen {
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
 *   "A mutable Set" should "allow an element to be added" in {
 *     Given("an empty mutable Set")
 *     val set = mutable.Set.empty[String]
 *
 *     When("an element is added")
 *     set += "clarity"
 *
 *     Then("the Set should have size 1")
 *     assert(set.size === 1)
 *
 *     And("the Set should contain the added element")
 *     assert(set.contains("clarity"))
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
 * <img class="stScreenShot" src="../../lib/flatSpec.gif">
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
 * package org.scalatest.examples.flatspec.note
 *
 * import collection.mutable
 * import org.scalatest._
 *
 * class SetSpec extends FlatSpec {
 *
 *   "A mutable Set" should "allow an element to be added" in {
 *
 *     info("info is recorded")
 *     markup("markup is *also* recorded")
 *     note("notes are sent immediately")
 *     alert("alerts are also sent immediately")
 *
 *     val set = mutable.Set.empty[String]
 *     set += "clarity"
 *     assert(set.size === 1)
 *     assert(set.contains("clarity"))
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
 * A mutable Set
 *   + notes are sent immediately</span>
 *   <span class="stYellow">+ alerts are also sent immediately</span>
 * <span class="stGreen">- should allow an element to be added
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
 * sent to the reporter when running the test can appear in the report of a test run. (In other words,
 * the code of a pending test is executed just like any other test.) However, because the test completes abruptly
 * with `TestPendingException`, the test will be reported as pending, to indicate
 * the actual test, and possibly the functionality it is intended to test, has not yet been implemented.
 * You can mark tests as pending in `FlatSpec` like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.pending
 * 
 * import org.scalatest._
 * 
 * class SetSpec extends FlatSpec {
 * 
 *   "An empty Set" should "have size 0" in (pending)
 *     
 *   it should "produce NoSuchElementException when head is invoked" in {
 *     assertThrows[NoSuchElementException] {
 *       Set.empty.head
 *     }
 *   }
 * }
 * }}}
 *
 * If you run this version of `FlatSpec` with:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * }}}
 *
 * It will run both tests but report that `An empty Set should have size 0` is pending. You'll see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">An empty Set</span>
 * <span class="stYellow">- should have size 0 (pending)</span>
 * <span class="stGreen">- should produce NoSuchElementException when head is invoked</span>
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
 * that used the `GivenWhenThen` trait. For example, the following snippet in a `FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 *  "The Scala language" must "add correctly" in { 
 *     Given("two integers")
 *     When("they are added")
 *     Then("the result is the sum of the two numbers")
 *     pending
 *   }
 *   // ...
 * }}}
 *
 * Would yield the following output when run in the interpreter:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">The Scala language</span>
 * <span class="stYellow">- must add correctly (pending)
 *   + Given two integers 
 *   + When they are added 
 *   + Then the result is the sum of the two numbers</span> 
 * }}}
 *
 * <a name="taggingTests"></a>==Tagging tests==
 *
 * A `FlatSpec`'s tests may be classified into groups by ''tagging'' them with string names.
 * As with any suite, when executing a `FlatSpec`, groups of tests can
 * optionally be included and/or excluded. To tag a `FlatSpec`'s tests,
 * you pass objects that extend class `org.scalatest.Tag` to methods
 * that register tests. Class `Tag` takes one parameter, a string name.  If you have
 * created tag annotation interfaces as described in the <a href="Tag.html">`Tag` documentation</a>, then you
 * will probably want to use tag names on your test functions that match. To do so, simply 
 * pass the fully qualified names of the tag interfaces to the `Tag` constructor. For example, if you've
 * defined a tag annotation interface with fully qualified name, 
 * `com.mycompany.tags.DbTest`, then you could
 * create a matching tag for `FlatSpec`s like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.tagging
 * 
 * import org.scalatest.Tag
 * 
 * object DbTest extends Tag("com.mycompany.tags.DbTest")
 * }}}
 *
 * Given these definitions, you could place `FlatSpec` tests into groups with tags like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.FlatSpec
 * import org.scalatest.tagobjects.Slow
 * 
 * class SetSpec extends FlatSpec {
 * 
 *   behavior of "An empty Set"
 *   
 *   it should "have size 0" taggedAs(Slow) in {
 *     assert(Set.empty.size === 0)
 *   }
 *     
 *   it should "produce NoSuchElementException when head is invoked" taggedAs(Slow, DbTest) in {
 *     assertThrows[NoSuchElementException] {
 *       Set.empty.head
 *     }
 *   }
 * }
 * }}}
 *
 * This code marks both tests with the `org.scalatest.tags.Slow` tag, 
 * and the second test with the `com.mycompany.tags.DbTest` tag.
 * 
 *
 * The `run` method takes a <a href="Filter.html">`Filter`</a>, whose constructor takes an optional
 * `Set[String]` called `tagsToInclude` and a `Set[String]` called
 * `tagsToExclude`. If `tagsToInclude` is `None`, all tests will be run
 * except those those belonging to tags listed in the
 * `tagsToExclude` `Set`. If `tagsToInclude` is defined, only tests
 * belonging to tags mentioned in the `tagsToInclude` set, and not mentioned in `tagsToExclude`,
 * will be run.
 * 
 *
 * It is recommended, though not required, that you create a corresponding tag annotation when you
 * create a `Tag` object. A tag annotation (on the JVM, not Scala.js) allows you to tag all the tests of a `FlatSpec` in
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
 * test execution.<p>The following sections
 * describe these techniques, including explaining the recommended usage
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
 *     <a href="#fixtureContextObjects">fixture-context objects</a>
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
 *     <a href="#withFixtureNoArgTest">
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
 *     <a href="#withFixtureOneArgTest">
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
 * package org.scalatest.examples.flatspec.getfixture
 * 
 * import org.scalatest.FlatSpec
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends FlatSpec {
 * 
 *   class Fixture {
 *     val builder = new StringBuilder("ScalaTest is ")
 *     val buffer = new ListBuffer[String]
 *   }
 *
 *   def fixture = new Fixture
 *   
 *   "Testing" should "be easy" in {
 *     val f = fixture
 *     f.builder.append("easy!")
 *     assert(f.builder.toString === "ScalaTest is easy!")
 *     assert(f.buffer.isEmpty)
 *     f.buffer += "sweet"
 *   }
 *   
 *   it should "be fun" in {
 *     val f = fixture
 *     f.builder.append("fun!")
 *     assert(f.builder.toString === "ScalaTest is fun!")
 *     assert(f.buffer.isEmpty)
 *   }
 * }
 * }}}
 *
 * The &ldquo;`f.`&rdquo; in front of each use of a fixture object provides a visual indication of which objects 
 * are part of the fixture, but if you prefer, you can import the the members with &ldquo;`import f._`&rdquo; and use the names directly.
 * 
 *
 * If you need to configure fixture objects differently in different tests, you can pass configuration into the get-fixture method. For example, if you could pass
 * in an initial value for a mutable fixture object as a parameter to the get-fixture method.
 * 
 *
 * <a name="fixtureContextObjects"></a>
 * ====Instantiating fixture-context objects ====
 *
 * An alternate technique that is especially useful when different tests need different combinations of fixture objects is to define the fixture objects as instance variables
 * of ''fixture-context objects'' whose instantiation forms the body of tests. Like get-fixture methods, fixture-context objects are only
 * appropriate if you don't need to clean up the fixtures after using them.
 * 
 *
 * To use this technique, you define instance variables intialized with fixture objects in traits and/or classes, then in each test instantiate an object that
 * contains just the fixture objects needed by the test. Traits allow you to mix together just the fixture objects needed by each test, whereas classes
 * allow you to pass data in via a constructor to configure the fixture objects. Here's an example in which fixture objects are partitioned into two traits
 * and each test just mixes together the traits it needs:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.fixturecontext
 * 
 * import collection.mutable.ListBuffer
 * import org.scalatest.FlatSpec
 * 
 * class ExampleSpec extends FlatSpec {
 * 
 *   trait Builder {
 *     val builder = new StringBuilder("ScalaTest is ")
 *   }
 * 
 *   trait Buffer {
 *     val buffer = ListBuffer("ScalaTest", "is")
 *   }
 * 
 *   // This test needs the StringBuilder fixture
 *   "Testing" should "be productive" in new Builder {
 *     builder.append("productive!")
 *     assert(builder.toString === "ScalaTest is productive!")
 *   }
 * 
 *   // This test needs the ListBuffer[String] fixture
 *   "Test code" should "be readable" in new Buffer {
 *     buffer += ("readable!")
 *     assert(buffer === List("ScalaTest", "is", "readable!"))
 *   }
 * 
 *   // This test needs both the StringBuilder and ListBuffer
 *   it should "be clear and concise" in new Builder with Buffer {
 *     builder.append("clear!")
 *     buffer += ("concise!")
 *     assert(builder.toString === "ScalaTest is clear!")
 *     assert(buffer === List("ScalaTest", "is", "concise!"))
 *   }
 * }
 * }}}
 *
 * <a name="withFixtureNoArgTest"></a>
 * ====Overriding `withFixture(NoArgTest)`====
 *
 * Although the get-fixture method and fixture-context object approaches take care of setting up a fixture at the beginning of each
 * test, they don't address the problem of cleaning up a fixture at the end of the test. If you just need to perform a side-effect at the beginning or end of
 * a test, and don't need to actually pass any fixture objects into the test, you can override `withFixture(NoArgTest)`, one of ScalaTest's
 * lifecycle methods defined in trait <a href="Suite.html">`Suite`</a>.
 * 
 *
 * Trait `Suite`'s implementation of `runTest` passes a no-arg test function to `withFixture(NoArgTest)`. It is `withFixture`'s
 * responsibility to invoke that test function. `Suite`'s implementation of `withFixture` simply
 * invokes the function, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Default implementation in trait Suite
 * protected def withFixture(test: NoArgTest) = {
 *   test()
 * }
 * }}}
 *
 * You can, therefore, override `withFixture` to perform setup before and/or cleanup after invoking the test function. If
 * you have cleanup to perform, you should invoke the test function inside a `try` block and perform the cleanup in
 * a `finally` clause, in case an exception propagates back through `withFixture`. (If a test fails because of an exception,
 * the test function invoked by withFixture will result in a [[org.scalatest.Failed `Failed`]] wrapping the exception. Nevertheless,
 * best practice is to perform cleanup in a finally clause just in case an exception occurs.)
 * 
 *
 * The `withFixture` method is designed to be stacked, and to enable this, you should always call the `super` implementation
 * of `withFixture`, and let it invoke the test function rather than invoking the test function directly. That is to say, instead of writing
 * &ldquo;`test()`&rdquo;, you should write &ldquo;`super.withFixture(test)`&rdquo;, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Your implementation
 * override def withFixture(test: NoArgTest) = {
 *   // Perform setup
 *   try super.withFixture(test) // Invoke the test function
 *   finally {
 *     // Perform cleanup
 *   }
 * }
 * }}}
 *
 * Here's an example in which `withFixture(NoArgTest)` is used to take a snapshot of the working directory if a test fails, and 
 * send that information to the reporter:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.noargtest
 * 
 * import java.io.File
 * import org.scalatest._
 * 
 * class ExampleSpec extends FlatSpec {
 * 
 *   override def withFixture(test: NoArgTest) = {
 * 
 *     super.withFixture(test) match {
 *       case failed: Failed =&gt;
 *         val currDir = new File(".")
 *         val fileNames = currDir.list()
 *         info("Dir snapshot: " + fileNames.mkString(", "))
 *         failed
 *       case other =&gt; other
 *     }
 *   }
 * 
 *   "This test" should "succeed" in {
 *     assert(1 + 1 === 2)
 *   }
 * 
 *   it should "fail" in {
 *     assert(1 + 1 === 3)
 *   }
 * }
 * }}}
 *
 * Running this version of `ExampleSuite` in the interpreter in a directory with two files, `hello.txt` and `world.txt`
 * would give the following output:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new ExampleSuite)
 * <span class="stGreen">ExampleSuite:
 * This test
 * - should succeed</span>
 * <span class="stRed">- should fail *** FAILED ***
 *   2 did not equal 3 (<console>:33)
 *   + Dir snapshot: hello.txt, world.txt </span>
 * }}}
 *
 * Note that the <a href="Suite$NoArgTest.html">`NoArgTest`</a> passed to `withFixture`, in addition to
 * an `apply` method that executes the test, also includes [[org.scalatest.TestData `TestData`]] such as the test name and the <a href="ConfigMap.html">config
 * map</a> passed to `runTest`. Thus you can also use the test name and configuration objects in your `withFixture`
 * implementation.
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
 * package org.scalatest.examples.flatspec.loanfixture
 * 
 * import java.util.concurrent.ConcurrentHashMap
 * 
 * object DbServer { // Simulating a database server
 *   type Db = StringBuffer
 *   private val databases = new ConcurrentHashMap[String, Db]
 *   def createDb(name: String): Db = {
 *     val db = new StringBuffer
 *     databases.put(name, db)
 *     db
 *   }
 *   def removeDb(name: String) {
 *     databases.remove(name)
 *   }
 * }
 * 
 * import org.scalatest.FlatSpec
 * import DbServer._
 * import java.util.UUID.randomUUID
 * import java.io._
 * 
 * class ExampleSpec extends FlatSpec {
 * 
 *   def withDatabase(testCode: Db =&gt; Any) {
 *     val dbName = randomUUID.toString
 *     val db = createDb(dbName) // create the fixture
 *     try {
 *       db.append("ScalaTest is ") // perform setup
 *       testCode(db) // "loan" the fixture to the test
 *     }
 *     finally removeDb(dbName) // clean up the fixture
 *   }
 * 
 *   def withFile(testCode: (File, FileWriter) =&gt; Any) {
 *     val file = File.createTempFile("hello", "world") // create the fixture
 *     val writer = new FileWriter(file)
 *     try {
 *       writer.write("ScalaTest is ") // set up the fixture
 *       testCode(file, writer) // "loan" the fixture to the test
 *     }
 *     finally writer.close() // clean up the fixture
 *   }
 * 
 *   // This test needs the file fixture
 *   "Testing" should "be productive" in withFile { (file, writer) =&gt;
 *     writer.write("productive!")
 *     writer.flush()
 *     assert(file.length === 24)
 *   }
 *   
 *   // This test needs the database fixture
 *   "Test code" should "be readable" in withDatabase { db =&gt;
 *     db.append("readable!")
 *     assert(db.toString === "ScalaTest is readable!")
 *   }
 * 
 *   // This test needs both the file and the database
 *   it should "be clear and concise" in withDatabase { db =&gt;
 *     withFile { (file, writer) =&gt; // loan-fixture methods compose
 *       db.append("clear!")
 *       writer.write("concise!")
 *       writer.flush()
 *       assert(db.toString === "ScalaTest is clear!")
 *       assert(file.length === 21)
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
 * involve external side-effects, like creating files or databases, it is a good idea to give each file or database a unique name as is
 * done in this example. This keeps tests completely isolated, allowing you to run them in parallel if desired.
 * 
 *
 * <a name="withFixtureOneArgTest"></a>
 * ====Overriding `withFixture(OneArgTest)`====
 *
 * If all or most tests need the same fixture, you can avoid some of the boilerplate of the loan-fixture method approach by using a `fixture.FlatSpec`
 * and overriding `withFixture(OneArgTest)`.
 * Each test in a `fixture.FlatSpec` takes a fixture as a parameter, allowing you to pass the fixture into
 * the test. You must indicate the type of the fixture parameter by specifying `FixtureParam`, and implement a
 * `withFixture` method that takes a `OneArgTest`. This `withFixture` method is responsible for
 * invoking the one-arg test function, so you can perform fixture set up before, and clean up after, invoking and passing
 * the fixture into the test function.
 * 
 *
 * To enable the stacking of traits that define `withFixture(NoArgTest)`, it is a good idea to let
 * `withFixture(NoArgTest)` invoke the test function instead of invoking the test
 * function directly. To do so, you'll need to convert the `OneArgTest` to a `NoArgTest`. You can do that by passing
 * the fixture object to the `toNoArgTest` method of `OneArgTest`. In other words, instead of
 * writing &ldquo;`test(theFixture)`&rdquo;, you'd delegate responsibility for
 * invoking the test function to the `withFixture(NoArgTest)` method of the same instance by writing:
 * 
 *
 * {{{
 * withFixture(test.toNoArgTest(theFixture))
 * }}}
 *
 * Here's a complete example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.oneargtest
 * 
 * import org.scalatest.fixture
 * import java.io._
 * 
 * class ExampleSpec extends fixture.FlatSpec {
 * 
 *   case class FixtureParam(file: File, writer: FileWriter)
 * 
 *   def withFixture(test: OneArgTest) = {
 *     val file = File.createTempFile("hello", "world") // create the fixture
 *     val writer = new FileWriter(file)
 *     val theFixture = FixtureParam(file, writer)
 * 
 *     try {
 *       writer.write("ScalaTest is ") // set up the fixture
 *       withFixture(test.toNoArgTest(theFixture)) // "loan" the fixture to the test
 *     }
 *     finally writer.close() // clean up the fixture
 *   }
 * 
 *   "Testing" should "be easy" in { f =&gt;
 *     f.writer.write("easy!")
 *     f.writer.flush()
 *     assert(f.file.length === 18)
 *   }
 * 
 *   it should "be fun" in { f =&gt;
 *     f.writer.write("fun!")
 *     f.writer.flush()
 *     assert(f.file.length === 17)
 *   }
 * }
 * }}}
 *
 * In this example, the tests actually required two fixture objects, a `File` and a `FileWriter`. In such situations you can
 * simply define the `FixtureParam` type to be a tuple containing the objects, or as is done in this example, a case class containing
 * the objects.  For more information on the `withFixture(OneArgTest)` technique, see the <a href="fixture/FlatSpec.html">documentation for `fixture.FlatSpec`</a>.
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
 * package org.scalatest.examples.flatspec.beforeandafter
 * 
 * import org.scalatest._
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends FlatSpec with BeforeAndAfter {
 * 
 *   val builder = new StringBuilder
 *   val buffer = new ListBuffer[String]
 * 
 *   before {
 *     builder.append("ScalaTest is ")
 *   }
 * 
 *   after {
 *     builder.clear()
 *     buffer.clear()
 *   }
 * 
 *   "Testing" should "be easy" in {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   it should "be fun" in {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *   }
 * }
 * }}}
 *
 * Note that the only way `before` and `after` code can communicate with test code is via some side-effecting mechanism, commonly by
 * reassigning instance `var`s or by changing the state of mutable objects held from instance `val`s (as in this example). If using
 * instance `var`s or mutable objects held from instance `val`s you wouldn't be able to run tests in parallel in the same instance
 * of the test class (on the JVM, not Scala.js) unless you synchronized access to the shared, mutable state. This is why ScalaTest's `ParallelTestExecution` trait extends
 * <a href="OneInstancePerTest.html">`OneInstancePerTest`</a>. By running each test in its own instance of the class, each test has its own copy of the instance variables, so you
 * don't need to synchronize. If you mixed `ParallelTestExecution` into the `ExampleSuite` above, the tests would run in parallel just fine
 * without any synchronization needed on the mutable `StringBuilder` and `ListBuffer[String]` objects.
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
 * which the `StringBuilder` and `ListBuffer[String]` fixtures used in the previous examples have been
 * factored out into two ''stackable fixture traits'' named `Builder` and `Buffer`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.composingwithfixture
 * 
 * import org.scalatest._
 * import collection.mutable.ListBuffer
 * 
 * trait Builder extends TestSuiteMixin { this: TestSuite =&gt;
 * 
 *   val builder = new StringBuilder
 * 
 *   abstract override def withFixture(test: NoArgTest) = {
 *     builder.append("ScalaTest is ")
 *     try super.withFixture(test) // To be stackable, must call super.withFixture
 *     finally builder.clear()
 *   }
 * }
 * 
 * trait Buffer extends TestSuiteMixin { this: TestSuite =&gt;
 * 
 *   val buffer = new ListBuffer[String]
 * 
 *   abstract override def withFixture(test: NoArgTest) = {
 *     try super.withFixture(test) // To be stackable, must call super.withFixture
 *     finally buffer.clear()
 *   }
 * }
 * 
 * class ExampleSpec extends FlatSpec with Builder with Buffer {
 * 
 *   "Testing" should "be easy" in {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   it should "be fun" in {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *     buffer += "clear"
 *   }
 * }
 * }}}
 *
 * By mixing in both the `Builder` and `Buffer` traits, `ExampleSuite` gets both fixtures, which will be
 * initialized before each test and cleaned up after. The order the traits are mixed together determines the order of execution.
 * In this case, `Builder` is &ldquo;super&rdquo; to `Buffer`. If you wanted `Buffer` to be &ldquo;super&rdquo;
 * to `Builder`, you need only switch the order you mix them together, like this: 
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class Example2Spec extends FlatSpec with Buffer with Builder
 * }}}
 *
 * And if you only need one fixture you mix in only that trait:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class Example3Spec extends FlatSpec with Builder
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
 * package org.scalatest.examples.flatspec.composingbeforeandaftereach
 * 
 * import org.scalatest._
 * import collection.mutable.ListBuffer
 * 
 * trait Builder extends BeforeAndAfterEach { this: Suite =&gt;
 * 
 *   val builder = new StringBuilder
 * 
 *   override def beforeEach() {
 *     builder.append("ScalaTest is ")
 *     super.beforeEach() // To be stackable, must call super.beforeEach
 *   }
 * 
 *   override def afterEach() {
 *     try super.afterEach() // To be stackable, must call super.afterEach
 *     finally builder.clear()
 *   }
 * }
 * 
 * trait Buffer extends BeforeAndAfterEach { this: Suite =&gt;
 * 
 *   val buffer = new ListBuffer[String]
 * 
 *   override def afterEach() {
 *     try super.afterEach() // To be stackable, must call super.afterEach
 *     finally buffer.clear()
 *   }
 * }
 * 
 * class ExampleSpec extends FlatSpec with Builder with Buffer {
 * 
 *   "Testing" should "be easy" in {
 *     builder.append("easy!")
 *     assert(builder.toString === "ScalaTest is easy!")
 *     assert(buffer.isEmpty)
 *     buffer += "sweet"
 *   }
 * 
 *   it should "be fun" in {
 *     builder.append("fun!")
 *     assert(builder.toString === "ScalaTest is fun!")
 *     assert(buffer.isEmpty)
 *     buffer += "clear"
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
 * by different fixture objects.  To accomplish this in a `FlatSpec`, you first place shared tests in ''behavior functions''.
 * These behavior functions will be invoked during the construction phase of any `FlatSpec` that uses them, so that the tests they
 * contain will be registered as tests in that `FlatSpec`.  For example, given this stack class:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import scala.collection.mutable.ListBuffer
 * 
 * class Stack[T] {
 *
 *   val MAX = 10
 *   private val buf = new ListBuffer[T]
 *
 *   def push(o: T) {
 *     if (!full)
 *       buf.prepend(o)
 *     else
 *       throw new IllegalStateException("can't push onto a full stack")
 *   }
 *
 *   def pop(): T = {
 *     if (!empty)
 *       buf.remove(0)
 *     else
 *       throw new IllegalStateException("can't pop an empty stack")
 *   }
 *
 *   def peek: T = {
 *     if (!empty)
 *       buf(0)
 *     else
 *       throw new IllegalStateException("can't pop an empty stack")
 *   }
 *
 *   def full: Boolean = buf.size == MAX
 *   def empty: Boolean = buf.size == 0
 *   def size = buf.size
 *
 *   override def toString = buf.mkString("Stack(", ", ", ")")
 * }
 * }}}
 *
 * You may want to test the `Stack` class in different states: empty, full, with one item, with one item less than capacity,
 * ''etc''. You may find you have several tests that make sense any time the stack is non-empty. Thus you'd ideally want to run
 * those same tests for three stack fixture objects: a full stack, a stack with a one item, and a stack with one item less than
 * capacity. With shared tests, you can factor these tests out into a behavior function, into which you pass the
 * stack fixture to use when running the tests. So in your `FlatSpec` for stack, you'd invoke the
 * behavior function three times, passing in each of the three stack fixtures so that the shared tests are run for all three fixtures. You
 * can define a behavior function that encapsulates these shared tests inside the `FlatSpec` that uses them. If they are shared
 * between different `FlatSpec`s, however, you could also define them in a separate trait that is mixed into each `FlatSpec`
 * that uses them.
 * 
 *
 * <a name="StackBehaviors">For</a> example, here the `nonEmptyStack` behavior function (in this case, a behavior ''method'') is
 * defined in a trait along with another method containing shared tests for non-full stacks:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * trait StackBehaviors { this: FlatSpec =&gt;
 * 
 *   def nonEmptyStack(newStack: =&gt; Stack[Int], lastItemAdded: Int) {
 *
 *     it should "be non-empty" in {
 *       assert(!newStack.empty)
 *     }
 *
 *     it should "return the top item on peek" in {
 *       assert(newStack.peek === lastItemAdded)
 *     }
 *
 *     it should "not remove the top item on peek" in {
 *       val stack = newStack
 *       val size = stack.size
 *       assert(stack.peek === lastItemAdded)
 *       assert(stack.size === size)
 *     }
 *
 *     it should "remove the top item on pop" in {
 *       val stack = newStack
 *       val size = stack.size
 *       assert(stack.pop === lastItemAdded)
 *       assert(stack.size === size - 1)
 *     }
 *   }
 *
 *   def nonFullStack(newStack: =&gt; Stack[Int]) {
 *
 *     it should "not be full" in {
 *       assert(!newStack.full)
 *     }
 *
 *     it should "add to the top on push" in {
 *       val stack = newStack
 *       val size = stack.size
 *       stack.push(7)
 *       assert(stack.size === size + 1)
 *       assert(stack.peek === 7)
 *     }
 *   }
 * }
 * }}}
 *
 *
 * Given these behavior functions, you could invoke them directly, but `FlatSpec` offers a DSL for the purpose,
 * which looks like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * it should behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
 * it should behave like nonFullStack(stackWithOneItem)
 * }}}
 *
 * If you prefer to use an imperative style to change fixtures, for example by mixing in `BeforeAndAfterEach` and
 * reassigning a `stack` `var` in `beforeEach`, you could write your behavior functions
 * in the context of that `var`, which means you wouldn't need to pass in the stack fixture because it would be
 * in scope already inside the behavior function. In that case, your code would look like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * it should behave like nonEmptyStack // assuming lastValuePushed is also in scope inside nonEmptyStack
 * it should behave like nonFullStack
 * }}}
 *
 * The recommended style, however, is the functional, pass-all-the-needed-values-in style. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class SharedTestExampleSpec extends FlatSpec with StackBehaviors {
 * 
 *   // Stack fixture creation methods
 *   def emptyStack = new Stack[Int]
 * 
 *   def fullStack = {
 *     val stack = new Stack[Int]
 *     for (i <- 0 until stack.MAX)
 *       stack.push(i)
 *     stack
 *   }
 * 
 *   def stackWithOneItem = {
 *     val stack = new Stack[Int]
 *     stack.push(9)
 *     stack
 *   }
 * 
 *   def stackWithOneItemLessThanCapacity = {
 *     val stack = new Stack[Int]
 *     for (i <- 1 to 9)
 *       stack.push(i)
 *     stack
 *   }
 * 
 *   val lastValuePushed = 9
 * 
 *   "A Stack (when empty)" should "be empty" in {
 *     assert(emptyStack.empty)
 *   }
 * 
 *   it should "complain on peek" in {
 *     assertThrows[IllegalStateException] {
 *       emptyStack.peek
 *     }
 *   }
 *
 *   it should "complain on pop" in {
 *     assertThrows[IllegalStateException] {
 *       emptyStack.pop
 *     }
 *   }
 * 
 *   "A Stack (with one item)" should behave like nonEmptyStack(stackWithOneItem, lastValuePushed)
 *
 *   it should behave like nonFullStack(stackWithOneItem)
 *     
 *   "A Stack (with one item less than capacity)" should behave like nonEmptyStack(stackWithOneItemLessThanCapacity, lastValuePushed)
 *
 *   it should behave like nonFullStack(stackWithOneItemLessThanCapacity)
 * 
 *   "A Stack (full)" should "be full" in {
 *     assert(fullStack.full)
 *   }
 * 
 *   it should behave like nonEmptyStack(fullStack, lastValuePushed)
 * 
 *   it should "complain on a push" in {
 *     assertThrows[IllegalStateException] {
 *       fullStack.push(10)
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
 * scala&gt; org.scalatest.run(new SharedTestExampleSpec)
 * <span class="stGreen">A Stack (when empty)
 * - should be empty
 * - should complain on peek
 * - should complain on pop
 * A Stack (with one item) 
 * - should be non-empty
 * - should return the top item on peek
 * - should not remove the top item on peek
 * - should remove the top item on pop
 * - should not be full
 * - should add to the top on push
 * A Stack (with one item less than capacity) 
 * - should be non-empty
 * - should return the top item on peek
 * - should not remove the top item on peek
 * - should remove the top item on pop
 * - should not be full
 * - should add to the top on push
 * A Stack (full) 
 * - should be full
 * - should be non-empty
 * - should return the top item on peek
 * - should not remove the top item on peek
 * - should remove the top item on pop
 * - should complain on a push</span>
 * }}}
 * 
 * One thing to keep in mind when using shared tests is that in ScalaTest, each test in a suite must have a unique name.
 * If you register the same tests repeatedly in the same suite, one problem you may encounter is an exception at runtime
 * complaining that multiple tests are being registered with the same test name. A good way to solve this problem in a `FlatSpec` is to make sure
 * each invocation of a behavior function is in the context of a different set of `when`, ''verb'' (`should`,
 * `must`, or `can`), and `that` clauses,
 * which will prepend a string to each test name.
 * For example, the following code in a `FlatSpec` would register a test with the name `"A Stack (when empty) should be empty"`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 *   behavior of "A Stack (when empty)"
 *       
 *   it should "be empty" in {
 *     assert(emptyStack.empty)
 *   }
 *   // ...
 * }}}
 *
 * Or, using the shorthand notation:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 *   "A Stack" when {
 *     "empty" should {
 *       "be empty" in {
 *         assert(emptyStack.empty)
 *       }
 *     }
 *   }
 *   // ...
 * }}}
 *
 * If the `"should be empty"` test was factored out into a behavior function, it could be called repeatedly so long
 * as each invocation of the behavior function is in the context of a different combination
 * of `when`, ''verb'', and `that` clauses.
 * 
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.FlatSpecFinder"))
class FlatSpec extends FlatSpecLike {

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
