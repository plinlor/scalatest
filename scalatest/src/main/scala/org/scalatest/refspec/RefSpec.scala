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
package org.scalatest.refspec

import java.lang.reflect.{Method, Modifier, InvocationTargetException}
import org.scalatest.{Suite, Finders, Resources}
import RefSpec.equalIfRequiredCompactify
import RefSpec.isTestMethod

/**
 * Facilitates a &ldquo;behavior-driven&rdquo; style of development (BDD), in which tests
 * are methods, optionally nested inside singleton objects defining textual scopes.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * Class `RefSpec` allows you to define tests as methods, which saves one function literal per test compared to style classes that represent tests as functions.
 * Fewer function literals translates into faster compile times and fewer generated class files, which can help minimize build times.
 * As a result, using `RefSpec` can be a good choice in large projects where build times are a concern as well as when generating large numbers of
 * tests programatically via static code generators.
 * </td></tr></table>
 * 
 * Here's an example `RefSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec
 * 
 * import org.scalatest.RefSpec
 * 
 * class SetSpec extends RefSpec {
 * 
 *   object &#96;A Set&#96; {
 *     object &#96;when empty&#96; {
 *       def &#96;should have size 0&#96; {
 *         assert(Set.empty.size === 0)
 *       }
 *     
 *       def &#96;should produce NoSuchElementException when head is invoked&#96; {
 *         assertThrows[NoSuchElementException] {
 *           Set.empty.head
 *         }
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * A `RefSpec` can contain ''scopes'' and tests. You define a scope
 * with a nested singleton object, and a test with a method. The names of both ''scope objects'' and ''test methods''
 * must be expressed in back ticks and contain at least one space character.
 *
 * A space placed in backticks is encoded by the Scala compiler as `$u0020`, as
 * illustrated here:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; def &#96;an example&#96; = ()
 * an$u0020example: Unit
 * }}}
 * 
 * `RefSpec` uses reflection to discover scope objects and test methods.
 * During discovery, `RefSpec` will consider any nested singleton object whose name
 * includes `$u0020` a scope object, and any method whose name includes `$u0020` a test method.
 * It will ignore any singleton objects or methods that do not include a `$u0020` character. Thus, `RefSpec` would
 * not consider the following singleton object a scope object:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * object &#96;Set&#96; { // Not discovered, because no space character
 * }
 * }}}
 *
 * You can make such a scope discoverable by placing a space at the end, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * object &#96;Set &#96; { // Discovered, because of the trailing space character
 * }
 * }}}
 *
 * Rather than performing this discovery during construction, when instance variables used by scope objects may as yet be uninitialized,
 * `RefSpec` performs discovery lazily, the first time a method needing the results of discovery is invoked.
 * For example, methods `run`, `runTests`, `tags`, `expectedTestCount`,
 * `runTest`, and `testNames` all ensure that scopes and tests have already been discovered prior to doing anything
 * else. Discovery is performed, and the results recorded, only once for each `RefSpec` instance.
 * 
 *
 * A scope names, or gives more information about, the ''subject'' (class or other entity) you are specifying
 * and testing. In the previous example, `&#96;A Set&#96;`
 * is the subject under specification and test. With each test name you provide a string (the ''test text'') that specifies
 * one bit of behavior of the subject, and a block of code (the body of the test method) that verifies that behavior.
 * 
 *
 * When you execute a `RefSpec`, it will send <a href="../events/Formatter.html">`Formatter`</a>s in the events it sends to the
 * <a href="../Reporter.html">`Reporter`</a>. ScalaTest's built-in reporters will report these events in such a way
 * that the output is easy to read as an informal specification of the ''subject'' being tested.
 * For example, were you to run `SetSpec` from within the Scala interpreter:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * }}}
 *
 * You would see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">A Set</span>
 * <span class="stGreen">  when empty</span>
 * <span class="stGreen">  - should have size 0</span>
 * <span class="stGreen">  - should produce NoSuchElementException when head is invoked</span>
 * }}}
 *
 * Or, to run just the test named `A Set when empty should have size 0`, you could pass that test's name, or any unique substring of the
 * name, such as `"size 0"` or even just `"0"`. Here's an example:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSuite, "size 0")
 * <span class="stGreen">A Set</span>
 * <span class="stGreen">  when empty</span>
 * <span class="stGreen">  - should have size 0</span>
 * }}}
 *
 * You can also pass to `execute` a <a href="../ConfigMap.html">''config map''</a> of key-value
 * pairs, which will be passed down into suites and tests, as well as other parameters that configure the run itself.
 * For more information on running in the Scala interpreter, see the documentation for the
 * <a href="../Shell.html">ScalaTest shell</a>.
 * 
 *
 * The `execute` method invokes a `run` method that takes two
 * parameters. This `run` method, which actually executes the suite, will usually be invoked by a test runner, such
 * as <a href="../run$.html">`run`</a>, <a href="../tools/Runner$.html">`tools.Runner`</a>, a build tool, or an IDE.
 * 
 *
 * The test methods shown in this example are parameterless. This is recommended even for test methods with obvious side effects. In production code
 * you would normally declare no-arg, side-effecting methods as ''empty-paren'' methods, and call them with
 * empty parentheses, to make it more obvious to readers of the code that they have a side effect. Whether or not a test method has
 * a side effect, however, is a less important distinction than it is for methods in production code. Moreover, test methods are not
 * normally invoked directly by client code, but rather through reflection by running the `Suite` that contains them, so a
 * lack of parentheses on an invocation of a side-effecting test method would not normally appear in any client code. Given the empty
 * parentheses do not add much value in the test methods case, the recommended style is to simply always leave them off.
 * 
 *
 * ''Note: The approach of using backticks around test method names to make it easier to write descriptive test names was
 * inspired by the <a href="http://github.com/SimpleFinance/simplespec" target="_blank">`SimpleSpec`</a> test framework, originally created by Coda Hale.''
 * 
 *
 * <a name="ignoredTests"></a>==Ignored tests==
 *
 * To support the common use case of temporarily disabling a test in a `RefSpec`, with the
 * good intention of resurrecting the test at a later time, you can annotate the test method with `@Ignore`.
 * For example, to temporarily disable the test method with the name `&#96;should have size zero"`, just annotate
 * it with `@Ignore`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.ignore
 * 
 * import org.scalatest._
 * 
 * class SetSpec extends RefSpec {
 *   
 *   object &#96;A Set&#96; {
 *     object &#96;when empty&#96; {
 *       @Ignore def &#96;should have size 0&#96; {
 *         assert(Set.empty.size === 0)
 *       }
 *       
 *       def &#96;should produce NoSuchElementException when head is invoked&#96; {
 *         assertThrows[NoSuchElementException] {
 *           Set.empty.head
 *         }
 *       }
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
 * It will run only the second test and report that the first test was ignored:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">A Set</span>
 * <span class="stGreen">  when empty</span>
 * <span class="stYellow">  - should have size 0 !!! IGNORED !!!</span>
 * <span class="stGreen">  - should produce NoSuchElementException when head is invoked</span>
 * }}}
 *
 * If you wish to temporarily ignore an entire suite of tests, you can annotate the test class with `@Ignore`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.ignoreall
 * 
 * import org.scalatest._
 *
 * @Ignore
 * class SetSpec extends RefSpec {
 *   
 *   object &#96;A Set&#96; {
 *     object &#96;when empty&#96; {
 *       def &#96;should have size 0&#96; {
 *         assert(Set.empty.size === 0)
 *       }
 *       
 *       def &#96;should produce NoSuchElementException when head is invoked&#96; {
 *         assertThrows[NoSuchElementException] {
 *           Set.empty.head
 *         }
 *       }
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
 * A Set
 *   when empty</span>
 * <span class="stYellow">  - should have size 0 !!! IGNORED !!!</span>
 * <span class="stYellow">  - should produce NoSuchElementException when head is invoked !!! IGNORED !!!</span>
 * }}}
 *
 * Note that marking a test class as ignored won't prevent it from being discovered by ScalaTest. Ignored classes
 * will be discovered and run, and all their tests will be reported as ignored. This is intended to keep the ignored
 * class visible, to encourage the developers to eventually fix and &ldquo;un-ignore&rdquo; it. If you want to
 * prevent a class from being discovered at all, use the <a href="../DoNotDiscover.html">`DoNotDiscover`</a> annotation instead.
 * 
 *
 *
 * <a name="informers"></a>==Informers==
 *
 * One of the objects to `RefSpec`'s `run` method is a `Reporter`, which
 * will collect and report information about the running suite of tests.
 * Information about suites and tests that were run, whether tests succeeded or failed, 
 * and tests that were ignored will be passed to the `Reporter` as the suite runs.
 * Most often the reporting done by default by `RefSpec`'s methods will be sufficient, but
 * occasionally you may wish to provide custom information to the `Reporter` from a test.
 * For this purpose, an <a href="../Informer.html">`Informer`</a> that will forward information to the current `Reporter`
 * is provided via the `info` parameterless method.
 * You can pass the extra information to the `Informer` via one of its `apply` methods.
 * The `Informer` will then pass the information to the `Reporter` via an <a href="../events/InfoProvided.html">`InfoProvided`</a> event.
 * Here's an example in which the `Informer` returned by `info` is used implicitly by the
 * `Given`, `When`, and `Then` methods of trait <a href="../GivenWhenThen.html">`GivenWhenThen`</a>:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.info
 * 
 * import collection.mutable
 * import org.scalatest._
 * 
 * class SetSpec extends RefSpec with GivenWhenThen {
 *   
 *   object &#96;A mutable Set&#96; {
 *     def &#96;should allow an element to be added&#96; {
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
 *       info("That's all folks!")
 *     }
 *   }
 * }
 * }}}
 *
 * If you run this `RefSpec` from the interpreter, you will see the following output:
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * <span class="stGreen">A mutable Set
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
 * `RefSpec` also provides a `markup` method that returns a <a href="../Documenter.html">`Documenter`</a>, which allows you to send
 * to the `Reporter` text formatted in <a href="http://daringfireball.net/projects/markdown/" target="_blank">Markdown syntax</a>.
 * You can pass the extra information to the `Documenter` via its `apply` method.
 * The `Documenter` will then pass the information to the `Reporter` via an <a href="../events/MarkupProvided.html">`MarkupProvided`</a> event.
 * 
 *
 * Here's an example `RefSpec` that uses `markup`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.markup
 *
 * import collection.mutable
 * import org.scalatest._
 *
 * class SetSpec extends RefSpec with GivenWhenThen {
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
 *   object `A mutable Set` {
 *     def `should allow an element to be added` {
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
 * <img class="stScreenShot" src="../../../lib/spec.gif">
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
 * To get immediate (''i.e.'', non-recorded) notifications from tests, you can use `note` (a <a href="../Notifier.html">`Notifier`</a>) and `alert`
 * (an <a href="../Alerter.html">`Alerter`</a>). Here's an example showing the differences:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.note
 *
 * import collection.mutable
 * import org.scalatest._
 *
 * class SetSpec extends RefSpec {
 *
 *   object `A mutable Set` {
 *     def `should allow an element to be added` {
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
 * A mutable Set
 *   + notes are sent immediately</span>
 *   <span class="stYellow">+ alerts are also sent immediately</span>
 * <span class="stGreen">- should allow an element to be added
 *   + info is recorded
 *   + markup is *also* recorded</span>
 * }}}
 *
 * Another example is <a href="../tools/Runner$.html#slowpokeNotifications">slowpoke notifications</a>.
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
 * it can call method `pending`, which will cause it to complete abruptly with <a href="../exceptions/TestPendingException.html">`TestPendingException`</a>.
 * 
 *
 * Because tests in ScalaTest can be designated as pending with `TestPendingException`, both the test name and any information
 * sent to the reporter when running the test can appear in the report of a test run. 
 * (The code of a pending test is executed just like any other test.) However, because the test completes abruptly
 * with `TestPendingException`, the test will be reported as pending, to indicate
 * the actual test, and possibly the functionality, has not yet been implemented.
 * 
 *
 * You can mark a test as pending in `RefSpec` by using "`{ pending }`" as the body of the test method,
 * like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.pending
 * 
 * import org.scalatest._
 * 
 * class SetSpec extends RefSpec {
 * 
 *   object &#96;A Set&#96; {
 *     object &#96;when empty&#96; {
 *       def &#96;should have size 0&#96; { pending }
 *       
 *       def &#96;should produce NoSuchElementException when head is invoked&#96; {
 *         assertThrows[NoSuchElementException] {
 *           Set.empty.head
 *         }
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * (Note: &ldquo;`pending`&rdquo; is the body of the test. Thus the test contains just one statement, an invocation
 * of the `pending` method, which throws `TestPendingException`.)
 * If you run this version of `SetSpec` with:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * }}}
 *
 * It will run both tests, but report that test "`should have size 0`" is pending. You'll see:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stGreen">A Set</span>
 * <span class="stGreen">  when empty</span>
 * <span class="stYellow">  - should have size 0 (pending)</span>
 * <span class="stGreen">  - should produce NoSuchElementException when head is invoked</span>
 * }}}
 * 
 * <a name="taggingTests"></a>==Tagging tests==
 *
 * A `RefSpec`'s tests may be classified into groups by ''tagging'' them with string names. When executing
 * a `RefSpec`, groups of tests can optionally be included and/or excluded. In this
 * trait's implementation, tags are indicated by annotations attached to the test method. To
 * create a new tag type to use in `RefSpec`s, simply define a new Java annotation that itself is annotated with
 * the `org.scalatest.TagAnnotation` annotation.
 * (Currently, for annotations to be
 * visible in Scala programs via Java reflection, the annotations themselves must be written in Java.) For example,
 * to create tags named `SlowTest` and `DbTest`, you would
 * write in Java:
 * 
 *
 * {{{
 * package org.scalatest.examples.spec.tagging;
 * import java.lang.annotation.*; 
 * import org.scalatest.TagAnnotation;
 * 
 * @TagAnnotation
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target({ElementType.METHOD, ElementType.TYPE})
 * public @interface SlowTest {}
 * 
 * @TagAnnotation
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target({ElementType.METHOD, ElementType.TYPE})
 * public @interface DbTest {}
 * }}}
 *
 * Given these annotations, you could tag `RefSpec` tests like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.tagging
 * 
 * import org.scalatest.RefSpec
 * 
 * class SetSpec extends RefSpec {
 * 
 *   object &#96;A Set&#96; {
 *     object &#96;when empty&#96; {

 *       @SlowTest
 *       def &#96;should have size 0&#96; {
 *         assert(Set.empty.size === 0)
 *       }
 *       
 *       @SlowTest @DbTest
 *       def &#96;should produce NoSuchElementException when head is invoked&#96; {
 *         assertThrows[NoSuchElementException] {
 *           Set.empty.head
 *         }
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * The `run` method takes a <a href="../Filter.html">`Filter`</a>, whose constructor takes an optional
 * `Set[String]` called `tagsToInclude` and a `Set[String]` called
 * `tagsToExclude`. If `tagsToInclude` is `None`, all tests will be run
 * except those those with tags listed in the
 * `tagsToExclude` `Set`. If `tagsToInclude` is defined, only tests
 * with tags mentioned in the `tagsToInclude` set, and not mentioned in `tagsToExclude`,
 * will be run.
 * 
 *
 * A tag annotation also allows you to tag all the tests of a `RefSpec` in
 * one stroke by annotating the class.  For more information and examples, see the
 * <a href="../Tag.html">documentation for class `Tag`</a>.
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
 * package org.scalatest.examples.spec.getfixture
 * 
 * import org.scalatest.RefSpec
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends RefSpec {
 * 
 *   class Fixture {
 *     val builder = new StringBuilder("ScalaTest is ")
 *     val buffer = new ListBuffer[String]
 *   }
 *   
 *   def fixture = new Fixture
 *   
 *   object &#96;Testing &#96; {
 *     def &#96;should be easy&#96; {
 *       val f = fixture
 *       f.builder.append("easy!")
 *       assert(f.builder.toString === "ScalaTest is easy!")
 *       assert(f.buffer.isEmpty)
 *       f.buffer += "sweet"
 *     }
 *   
 *     def &#96;should be fun&#96; {
 *       val f = fixture
 *       f.builder.append("fun!")
 *       assert(f.builder.toString === "ScalaTest is fun!")
 *       assert(f.buffer.isEmpty)
 *     }
 *   }
 * }
 * }}}
 *
 * The &ldquo;`f.`&rdquo; in front of each use of a fixture object provides a visual indication of which objects 
 * are part of the fixture, but if you prefer, you can import the the members with &ldquo;`import f._`&rdquo; and use the names directly.
 * 
 *
 * If you need to configure fixture objects differently in different tests, you can pass configuration into the get-fixture method. For example, you could pass
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
 * package org.scalatest.examples.spec.fixturecontext
 * 
 * import collection.mutable.ListBuffer
 * import org.scalatest.RefSpec
 * 
 * class ExampleSpec extends RefSpec {
 * 
 *   trait Builder {
 *     val builder = new StringBuilder("ScalaTest is ")
 *   }
 * 
 *   trait Buffer {
 *     val buffer = ListBuffer("ScalaTest", "is")
 *   }
 * 
 *   object &#96;Testing &#96; {
 *     // This test needs the StringBuilder fixture
 *     def &#96;should be productive&#96; {
 *       new Builder {
 *         builder.append("productive!")
 *         assert(builder.toString === "ScalaTest is productive!")
 *       }
 *     }
 *   }
 * 
 *   object &#96;Test code&#96; {
 *     // This test needs the ListBuffer[String] fixture
 *     def &#96;should be readable&#96; {
 *       new Buffer {
 *         buffer += ("readable!")
 *         assert(buffer === List("ScalaTest", "is", "readable!"))
 *       }
 *     }
 * 
 *     // This test needs both the StringBuilder and ListBuffer
 *     def &#96;should be clear and concise&#96; {
 *       new Builder with Buffer {
 *         builder.append("clear!")
 *         buffer += ("concise!")
 *         assert(builder.toString === "ScalaTest is clear!")
 *         assert(buffer === List("ScalaTest", "is", "concise!"))
 *       }
 *     }
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
 * lifecycle methods defined in trait <a href="../Suite.html#lifecycle-methods">`Suite`</a>.
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
 * of `withFixture`, and let it invoke the test function rather than invoking the test function directly. In other words, instead of writing
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
 * and send that information to the reporter:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.noargtest
 * 
 * import java.io.File
 * import org.scalatest._
 * 
 * class ExampleSpec extends RefSpec {
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
 *   object &#96;This test&#96; {
 *     def &#96;should succeed&#96; {
 *       assert(1 + 1 === 2)
 *     }
 * 
 *     def &#96;should fail&#96; {
 *       assert(1 + 1 === 3)
 *     }
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
 * This test</span>
 * <span class="stRed">- should fail *** FAILED ***
 *   2 did not equal 3 (<console>:33)
 *   + Dir snapshot: hello.txt, world.txt </span>
 * - should succeed
 * }}}
 *
 * Note that the <a href="../Suite$NoArgTest.html">`NoArgTest`</a> passed to `withFixture`, in addition to
 * an `apply` method that executes the test, also includes the test name and the <a href="../ConfigMap.html">config
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
 * package org.scalatest.examples.spec.loanfixture
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
 * import org.scalatest.RefSpec
 * import DbServer._
 * import java.util.UUID.randomUUID
 * import java.io._
 * 
 * class ExampleSpec extends RefSpec {
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
 *   object &#96;Testing &#96; {
 *     // This test needs the file fixture
 *     def &#96;should be productive&#96; {
 *       withFile { (file, writer) =&gt;
 *         writer.write("productive!")
 *         writer.flush()
 *         assert(file.length === 24)
 *       }
 *     }
 *   }
 *   
 *   object &#96;Test code&#96; {
 *     // This test needs the database fixture
 *     def &#96;should be readable&#96; {
 *       withDatabase { db =&gt;
 *         db.append("readable!")
 *         assert(db.toString === "ScalaTest is readable!")
 *       }
 *     }
 * 
 *     // This test needs both the file and the database
 *     def &#96;should be clear and concise&#96; {
 *       withDatabase { db =&gt;
 *        withFile { (file, writer) =&gt; // loan-fixture methods compose
 *           db.append("clear!")
 *           writer.write("concise!")
 *           writer.flush()
 *           assert(db.toString === "ScalaTest is clear!")
 *           assert(file.length === 21)
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
 * involve external side-effects, like creating files or databases, it is a good idea to give each file or database a unique name as is
 * done in this example. This keeps tests completely isolated, allowing you to run them in parallel if desired.
 * 
 *
 * <a name="withFixtureOneArgTest"></a>
 * ====Overriding `withFixture(OneArgTest)`====
 *
 * `fixture.Spec` is deprecated, please use `fixture.FunSpec` instead.
 *
 * <a name="beforeAndAfter"></a>
 * ====Mixing in `BeforeAndAfter`====
 *
 * In all the shared fixture examples shown so far, the activities of creating, setting up, and cleaning up the fixture objects have been
 * performed ''during'' the test.  This means that if an exception occurs during any of these activities, it will be reported as a test failure.
 * Sometimes, however, you may want setup to happen ''before'' the test starts, and cleanup ''after'' the test has completed, so that if an
 * exception occurs during setup or cleanup, the entire suite aborts and no more tests are attempted. The simplest way to accomplish this in ScalaTest is
 * to mix in trait <a href="../BeforeAndAfter.html">`BeforeAndAfter`</a>.  With this trait you can denote a bit of code to run before each test
 * with `before` and/or after each test each test with `after`, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.beforeandafter
 * 
 * import org.scalatest.RefSpec
 * import org.scalatest.BeforeAndAfter
 * import collection.mutable.ListBuffer
 * 
 * class ExampleSpec extends RefSpec with BeforeAndAfter {
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
 *   object &#96;Testing &#96; {
 *     def &#96;should be easy&#96; {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     def &#96;should be fun&#96; {
 *       builder.append("fun!")
 *       assert(builder.toString === "ScalaTest is fun!")
 *       assert(buffer.isEmpty)
 *     }
 *   }
 * }
 * }}}
 *
 * Note that the only way `before` and `after` code can communicate with test code is via some side-effecting mechanism, commonly by
 * reassigning instance `var`s or by changing the state of mutable objects held from instance `val`s (as in this example). If using
 * instance `var`s or mutable objects held from instance `val`s you wouldn't be able to run tests in parallel in the same instance
 * of the test class unless you synchronized access to the shared, mutable state. This is why ScalaTest's `ParallelTestExecution` trait extends
 * <a href="../OneInstancePerTest.html">`OneInstancePerTest`</a>. By running each test in its own instance of the class, each test has its own copy of the instance variables, so you
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
 * package org.scalatest.examples.spec.composingwithfixture
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
 * class ExampleSpec extends RefSpec with Builder with Buffer {
 * 
 *   object &#96;Testing &#96; {
 *     def &#96;should be easy&#96; {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     def &#96;should be fun&#96; {
 *       builder.append("fun!")
 *       assert(builder.toString === "ScalaTest is fun!")
 *       assert(buffer.isEmpty)
 *       buffer += "clear"
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
 * class Example2Spec extends RefSpec with Buffer with Builder
 * }}}
 *
 * And if you only need one fixture you mix in only that trait:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class Example3Spec extends RefSpec with Builder
 * }}}
 *
 * Another way to create stackable fixture traits is by extending the <a href="../BeforeAndAfterEach.html">`BeforeAndAfterEach`</a>
 * and/or <a href="../BeforeAndAfterAll.html">`BeforeAndAfterAll`</a> traits.
 * `BeforeAndAfterEach` has a `beforeEach` method that will be run before each test (like JUnit's `setUp`),
 * and an `afterEach` method that will be run after (like JUnit's `tearDown`).
 * Similarly, `BeforeAndAfterAll` has a `beforeAll` method that will be run before all tests,
 * and an `afterAll` method that will be run after all tests. Here's what the previously shown example would look like if it
 * were rewritten to use the `BeforeAndAfterEach` methods instead of `withFixture`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.spec.composingbeforeandaftereach
 * 
 * import org.scalatest._
 * import org.scalatest.BeforeAndAfterEach
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
 * class ExampleSpec extends RefSpec with Builder with Buffer {
 * 
 *   object &#96;Testing &#96; {
 *     def &#96;should be easy&#96; {
 *       builder.append("easy!")
 *       assert(builder.toString === "ScalaTest is easy!")
 *       assert(buffer.isEmpty)
 *       buffer += "sweet"
 *     }
 * 
 *     def &#96;should be fun&#96; {
 *       builder.append("fun!")
 *       assert(builder.toString === "ScalaTest is fun!")
 *       assert(buffer.isEmpty)
 *       buffer += "clear"
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
 * complete abruptly, it is considered an aborted suite, which will result in a <a href="../events/SuiteAborted.html">`SuiteAborted`</a> event.
 * 
 * 
 * <a name="sharedTests"></a>==Shared tests==
 *
 * Because `RefSpec` represents tests as methods, you cannot share or otherwise dynamically generate tests. Instead, use static code generation
 * if you want to generate tests in a `RefSpec`. In other words, write a program that statically generates the entire source file of
 * a `RefSpec` subclass.
 * 
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.SpecFinder"))
class RefSpec extends RefSpecLike {

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

private[scalatest] object RefSpec {

  def isTestMethod(m: Method): Boolean = {
    
    val isInstanceMethod = !Modifier.isStatic(m.getModifiers())

    val hasNoParams = m.getParameterTypes.isEmpty

    // name must have at least one encoded space: "$u0220"
    val includesEncodedSpace = m.getName.indexOf("$u0020") >= 0
    
    val isOuterMethod = m.getName.endsWith("$$outer")
    
    val isNestedMethod = m.getName.matches(".+\\$\\$.+\\$[1-9]+")

    //val isOuterMethod = m.getName.endsWith("$$$outer")
    // def maybe(b: Boolean) = if (b) "" else "!"
    // println("m.getName: " + m.getName + ": " + maybe(isInstanceMethod) + "isInstanceMethod, " + maybe(hasNoParams) + "hasNoParams, " + maybe(includesEncodedSpace) + "includesEncodedSpace")
    isInstanceMethod && hasNoParams && includesEncodedSpace && !isOuterMethod && !isNestedMethod
  }
  
  import java.security.MessageDigest
  import scala.io.Codec
  
  // The following compactify code is written based on scala compiler source code at:-
  // https://github.com/scala/scala/blob/master/src/reflect/scala/reflect/internal/StdNames.scala#L47
  
  private val compactifiedMarker = "$$$$"
  
  def equalIfRequiredCompactify(value: String, compactified: String): Boolean = {
    if (compactified.matches(".+\\$\\$\\$\\$.+\\$\\$\\$\\$.+")) {
      val firstDolarIdx = compactified.indexOf("$$$$")
      val lastDolarIdx = compactified.lastIndexOf("$$$$")
      val prefix = compactified.substring(0, firstDolarIdx)
      val suffix = compactified.substring(lastDolarIdx + 4)
      val lastIndexOfDot = value.lastIndexOf(".")
      val toHash = 
        if (lastIndexOfDot >= 0) 
          value.substring(0, value.length - 1).substring(value.lastIndexOf(".") + 1)
        else
          value
          
      val bytes = Codec.toUTF8(toHash.toArray)
      val md5 = MessageDigest.getInstance("MD5")
      md5.update(bytes)
      val md5chars = (md5.digest() map (b => (b & 0xFF).toHexString)).mkString
      (prefix + compactifiedMarker + md5chars + compactifiedMarker + suffix) == compactified
    }
    else
      value == compactified
  }
}

