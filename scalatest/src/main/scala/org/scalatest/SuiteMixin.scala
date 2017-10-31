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
 * Trait defining abstract "lifecycle" methods that are implemented in <a href="Suite.html#lifecycle-methods">`Suite`</a> and can
 * be overridden in stackable modification traits.
 *
 * The main purpose of `SuiteMixin` is to differentiate core `Suite`
 * style traits, such as <a href="Spec.html">`Spec`</a>, <a href="FunSuite.html">`FunSuite`</a>, and <a href="FunSpec.html">`FunSpec`</a> from stackable
 * modification traits for `Suite`s such as <a href="BeforeAndAfterEach.html">`BeforeAndAfterEach`</a>, <a href="OneInstancePerTest.html">`OneInstancePerTest`</a>,
 * and <a href="SequentialNestedSuiteExecution.html">`SequentialNestedSuiteExecution`</a>. Because these stackable traits extend `SuiteMixin`
 * instead of `Suite`, you can't define a suite by simply extending one of the stackable traits:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class MySuite extends BeforeAndAfterEach // Won't compile
 * }}}
 *
 * Instead, you need to extend a core `Suite` trait and mix the stackable `BeforeAndAfterEach` trait
 * into that, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class MySuite extends FunSuite with BeforeAndAfterEach // Compiles fine
 * }}}
 *
 * @author Bill Venners
 */
trait SuiteMixin { this: Suite =>

  /**
   * Runs this suite of tests.
   *
   * @param testName an optional name of one test to execute. If `None`, all relevant tests should be executed.
   *                 I.e., `None` acts like a wildcard that means execute all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests and nested suites started by this method have completed, and whether or not a failure occurred.
   *
   * @throws NullArgumentException if any passed parameter is `null`.
   */
  def run(testName: Option[String], args: Args): Status

  /**
   * Runs zero to many of this suite's nested suites.
   *
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all nested suites started by this method have completed, and whether or not a failure occurred.
   *
   * @throws NullArgumentException if `args` is `null`.
   */
  protected def runNestedSuites(args: Args): Status

  /**
   * Runs zero to many of this suite's tests.
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
   *
   * @throws NullArgumentException if either `testName` or `args` is `null`.
   */
  protected def runTests(testName: Option[String], args: Args): Status

  /**
   * Runs a test.
   *
   * @param testName the name of one test to execute.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
   *
   * @throws NullArgumentException if any of `testName` or `args` is `null`.
   */
  protected def runTest(
    testName: String,
    args: Args
  ): Status

  /**
   * A user-friendly suite name for this `Suite`.
   *
   * This trait's
   * implementation of this method returns the simple name of this object's class. This
   * trait's implementation of `runNestedSuites` calls this method to obtain a
   * name for `Report`s to pass to the `suiteStarting`, `suiteCompleted`,
   * and `suiteAborted` methods of the `Reporter`.
   * 
   *
   * @return this `Suite` object's suite name.
   */
  def suiteName: String

  /**
   * A string ID for this `Suite` that is intended to be unique among all suites reported during a run.
   *
   * The suite ID is ''intended'' to be unique, because ScalaTest does not enforce that it is unique. If it is not
   * unique, then you may not be able to uniquely identify a particular test of a particular suite. This ability is used,
   * for example, to dynamically tag tests as having failed in the previous run when rerunning only failed tests.
   * 
   *
   * @return this `Suite` object's ID.
   */
  def suiteId: String

  /**
   * Provides a `TestData` instance for the passed test name, given the passed config map.
   *
   * This method is used to obtain a `TestData` instance to pass to `withFixture(NoArgTest)`
   * and `withFixture(OneArgTest)` and the `beforeEach` and `afterEach` methods
   * of trait `BeforeAndAfterEach`.
   * 
   *
   * @param testName the name of the test for which to return a `TestData` instance
   * @param theConfigMap the config map to include in the returned `TestData`
   * @return a `TestData` instance for the specified test, which includes the specified config map
   */
  def testDataFor(testName: String, theConfigMap: ConfigMap): TestData

  /**
  * A `Set` of test names. If this `Suite` contains no tests, this method returns an empty `Set`.
  *
  * Although subclass and subtrait implementations of this method may return a `Set` whose iterator produces `String`
  * test names in a well-defined order, the contract of this method does not required a defined order. Subclasses are free to
  * implement this method and return test names in either a defined or undefined order.
  * 
  */
  def testNames: Set[String]

  /**
  * An immutable `IndexedSeq` of this `SuiteMixin` object's nested `Suite`s. If this `SuiteMixin` contains no nested `Suite`s,
  * this method returns an empty `IndexedSeq`.
  */
  def nestedSuites: collection.immutable.IndexedSeq[Suite]

  /**
   * A `Map` whose keys are `String` names of tagged tests and
   * whose associated values are the `Set` of tag names for the test. If a test has no associated tags, its name
   * does not appear as a key in the returned `Map`. If this `Suite` contains no tests with tags, this
   * method returns an empty `Map`.
   *
   * Subclasses may override this method to define and/or discover tags in a custom manner, but overriding method implementations
   * should never return an empty `Set` as a value. If a test has no tags, its name should not appear as a key in the
   * returned `Map`.
   * 
   */
  def tags: Map[String, Set[String]]

  /**
   * The total number of tests that are expected to run when this `Suite`'s `run` method is invoked.
   *
   * @param filter a `Filter` with which to filter tests to count based on their tags
   */
  def expectedTestCount(filter: Filter): Int
  
  /**
   * The fully qualified name of the class that can be used to rerun this suite.
   */
  def rerunner: Option[String]
  
  /**
   * This suite's style name.
   *
   * This lifecycle method provides a string that is used to determine whether this suite object's
   * style is one of the <a href="tools/Runner$.html#specifyingChosenStyles">chosen styles</a> for
   * the project.
   * 
   */
  val styleName: String
}

