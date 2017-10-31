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
 * Wrapper `Suite` that passes an instance of the config map to the constructor of the
 * wrapped `Suite` when `run` is invoked.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * Trait `ConfigMapWrapperSuite` is primarily intended to be used with the <a href="path/package.html">"path" traits</a>, which can't
 * use the usual approaches to accessing the config map because of the eager manner in which they run tests.''
 * </td></tr></table>
 * 
 * Each time `run` is invoked on an instance of `ConfigMapWrapperSuite`, this
 * suite will create a new instance of the suite to wrap, passing to the constructor the config map passed to
 * `run`. This way, if the same `ConfigMapWrapperSuite` instance is run multiple
 * times, each time with a different config map, an instance of the wrapped suite will be created
 * for each config map. In addition to being passed to the wrapped suite's constructor, the config map passed
 * to the `ConfigMapWrapperSuite`'s `run` method will also be passed to the `run`
 * method of the newly created wrapped suite instance.
 * 
 *
 * The config map is accessible inside a `Suite` in many ways. It is passed to `run`,
 * `runNestedSuites`, `runTests`, and `runTest`. It is also passed to
 * `withFixture`, accessible via a method on <a href="Suite$NoArgTest.html">`NoArgTest`</a> and
 * <a href="fixture/Suite$OneArgTest.html">`OneArgTest`</a>.
 * It is passed to an overloaded forms of the `beforeEach` and `afterEach` methods of trait
 * <a href="BeforeAndAfterEach.html">`BeforeAndAfterEach`</a>, as well as overloaded forms of the `beforeAll` and
 * `afterAll` methods of trait <a href="BeforeAndAfterAll.html">`BeforeAndAfterAll`</a>. Tests themselves can have information
 * taken from the config map, or the entire config map, through various means. The config map may be passed into
 * the test via a <a href="fixture/ConfigMapFixture.html">`ConfigMapFixture`</a>, for example. Class `ConfigMapWrapperSuite`
 * represents one more way to get at the config map inside a suite of test: `ConfigMapWrapperSuite` will
 * pass the config map to the constructor of your suite class, bringing it easily into scope for tests and
 * helper methods alike.
 * 
 *
 * Having the config map passed to the suite constructor might be more convenient in some cases, but in the case
 * of the <a href="path/package.html">`org.scalatest.path`</a> traits, it is necessary if a test needs
 * information from a config map. The reason is that in a path trait, the test code is executed eagerly,
 * ''before `run` is invoked''. The results of the tests are registered when the tests are executed, and those
 * results are merely ''reported'' once `run` is invoked. Thus by the time `run` has been invoked, it
 * is too late to get the config map to the tests, which have already been executed. Using a `ConfigMapWrapperSuite` solves that problem.
 * By passing the config map to the constructor, it is available early enough for the running tests to use it.
 * Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest._
 *
 * @WrapWith(classOf[ConfigMapWrapperSuite])
 * class ExampleSpec(configMap: ConfigMap) extends path.FunSpec {
 *
 *   describe("A widget database") {
 *     it("should contain consistent values") {
 *       val dbName = configMap("WidgetDbName") // Can access config map
 *       // ...
 *     }
 *   }
 * }
 * }}}
 *
 * @author Bill Venners
 */
final class ConfigMapWrapperSuite(clazz: Class[_ <: Suite]) extends Suite {

  private lazy val wrappedSuite = {
    val constructor = clazz.getConstructor(classOf[Map[_, _]])
    constructor.newInstance(Map.empty)
  }

  override def suiteId = clazz.getName

  /**
   * Returns the result obtained from invoking `expectedTestCount` on an instance of the wrapped
   * suite, constructed by passing an empty config map to its constructor, passing into the wrapped suite's
   * `expectedTestCount` method the specified `Filter`.
   *
   * @param filter the `Filter` to pass to the wrapped suite's `expectedTestCount` method
   * @return the result of invoking `expectedTestCount` on an instance of wrapped suite
   */
  override def expectedTestCount(filter: Filter): Int = wrappedSuite.expectedTestCount(filter)

  /**
   * Returns the result obtained from invoking `testNames` on an instance of the wrapped
   * suite, constructed by passing an empty config map to its constructor.
   *
   * @return the result of invoking `testNames` on an instance of wrapped suite
   */
  override def testNames: Set[String] = wrappedSuite.testNames

  /**
   * Returns the result obtained from invoking `nestedSuites` on an instance of the wrapped
   * suite, constructed by passing an empty config map to its constructor.
   *
   * @return the result of invoking `nestedSuites` on an instance of wrapped suite
   */
  override def nestedSuites: collection.immutable.IndexedSeq[Suite] = wrappedSuite.nestedSuites

  /**
   * Returns the result obtained from invoking `tags` on an instance of the wrapped
   * suite, constructed by passing an empty config map to its constructor.
   *
   * @return the result of invoking `testNames` on an instance of wrapped suite
   */
  override def tags: Map[String, Set[String]] = wrappedSuite.tags

  /**
   * Constructs a new instance of the suite to wrap, whose `Class` is passed to this
   * suite's constructor, passing in the specified config map, and invokes `run` on
   * that new instance, passing in the same arguments passed to this method.
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests and nested suites started by this method have completed, and whether or not a failure occurred.
   *
   * @throws NullArgumentException if any passed parameter is `null`.
   * @throws IllegalArgumentException if `testName` is defined, but no test with the specified test name
   *     exists in the `Suite`
   */
  override def run(testName: Option[String], args: Args): Status = {
    val constructor = clazz.getConstructor(classOf[Map[_, _]])
    val suite = constructor.newInstance(args.configMap)
    suite.run(testName, args)
  }
}
  // TODO: Check the testName throwing an IAE behavior
