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

import Suite.getSimpleNameOfAnObjectsClass

// Scala version o fwhat I'm thinking JSuite might have in Java
private[scalatest] trait JSuite { thisSuite =>

  /**
   * Runs this suite of tests.
   *
   * @param testName an optional name of one test to execute. If `None`, all relevant tests should be executed.
   *                 I.e., `None` acts like a wildcard that means execute all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   *
   * @throws NullArgumentException if any passed parameter is `null`.
   */
  def runJSuite(testName: Option[String], args: Args): Status // Will be testName: nullable String, args: JArgs

  /**
  * A `Set` of test names. If this `Suite` contains no tests, this method returns an empty `Set`.
  *
  * Although subclass and subtrait implementations of this method may return a `Set` whose iterator produces `String`
  * test names in a well-defined order, the contract of this method does not required a defined order. Subclasses are free to
  * implement this method and return test names in either a defined or undefined order.
  * 
  */
  def getTestNames(): java.util.Set[String]

  /**
   * A `Map` whose keys are `String` tag names with which tests in this `Suite` are marked, and
   * whose values are the `Set` of test names marked with each tag.  If this `Suite` contains no tags, this
   * method returns an empty `Map`.
   *
   * Subclasses may implement this method to define and/or discover tags in a custom manner, but overriding method implementations
   * should never return an empty `Set` as a value. If a tag has no tests, its name should not appear as a key in the
   * returned `Map`.
   * 
   */
  def getTags(): java.util.Map[String, java.util.Set[String]]

  /**
   * The total number of tests that are expected to run when this `Suite`'s `run` method is invoked.
   *
   * @param filter a `Filter` with which to filter tests to count based on their tags
   */
  def getExpectedTestCount(filter: Filter): Int // Will be JFilter
  
  /**
   * The fully qualified name of the class that can be used to rerun this suite.
   */
  def getRerunner: Option[String] // Will return a nullable String
  
  /**
   * This suite's style name.
   *
   * This lifecycle method provides a string that is used to determine whether this suite object's
   * style is one of the <a href="tools/Runner$.html#specifyingChosenStyles">chosen styles</a> for
   * the project.
   * 
   */
  def getStyleName(): String // Actually, not sure we need this over in the JSuite area Maybe this will just not exist over there.
  // But it might make it simpler to be consistent.

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
  def getSuiteName(): String = getSimpleNameOfAnObjectsClass(thisSuite)

  /**
   * A string ID for this `Suite` that is intended to be unique among all suites reported during a run.
   *
   * This trait's
   * implementation of this method returns the fully qualified name of this object's class. 
   * Each suite reported during a run will commonly be an instance of a different `Suite` class,
   * and in such cases, this default implementation of this method will suffice. However, in special cases
   * you may need to override this method to ensure it is unique for each reported suite. For example, if you write
   * a `Suite` subclass that reads in a file whose name is passed to its constructor and dynamically
   * creates a suite of tests based on the information in that file, you will likely need to override this method
   * in your `Suite` subclass, perhaps by appending the pathname of the file to the fully qualified class name. 
   * That way if you run a suite of tests based on a directory full of these files, you'll have unique suite IDs for
   * each reported suite.
   * 
   *
   * The suite ID is ''intended'' to be unique, because ScalaTest does not enforce that it is unique. If it is not
   * unique, then you may not be able to uniquely identify a particular test of a particular suite. This ability is used,
   * for example, to dynamically tag tests as having failed in the previous run when rerunning only failed tests.
   * 
   *
   * @return this `Suite` object's ID.
   */
  def getSuiteId(): String = thisSuite.getClass.getName
}

