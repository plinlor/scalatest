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

import Filter.IgnoreTag
import org.scalactic.Requirements._

/**
 * Filter whose `apply` method determines which of the passed tests to run and ignore based on tags to include and exclude passed as
 * as class parameters.
 *
 * This class handles the `org.scalatest.Ignore` tag specially, in that its `apply` method indicates which
 * tests should be ignored based on whether they are tagged with `org.scalatest.Ignore`. If
 * `"org.scalatest.Ignore"` is not passed in the `tagsToExclude` set, it will be implicitly added. However, if the 
 * `tagsToInclude` option is defined, and the contained set does not include `"org.scalatest.Ignore"`, then only those tests
 * that are both tagged with `org.scalatest.Ignore` and at least one of the tags in the `tagsToInclude` set
 * will be included in the result of `apply` and marked as ignored (so long as the test is not also
 * marked with a tag other than `org.scalatest.Ignore` that is a member of the `tagsToExclude`
 * set. For example, if `SlowAsMolasses` is a member of the `tagsToInclude` set and a
 * test is tagged with both `org.scalatest.Ignore` and `SlowAsMolasses`, and
 * `SlowAsMolasses` appears in the `tagsToExclude` set, the
 * `SlowAsMolasses` tag will "overpower" the `org.scalatest.Ignore` tag, and the
 * test will be filtered out entirely rather than being ignored.
 * 
 *
 * @param tagsToInclude an optional `Set` of `String` tag names to include (''i.e.'', not filter out) when filtering tests
 * @param tagsToExclude a `Set` of `String` tag names to exclude (''i.e.'', filter out) when filtering tests
 * @param excludeNestedSuites a `Boolean` to indicate whether to run nested suites
 * @param dynaTags dynamic tags for the filter
 *
 * @throws NullArgumentException if either `tagsToInclude` or `tagsToExclude` are null
 * @throws IllegalArgumentException if `tagsToInclude` is defined, but contains an empty set
 */
final class Filter private (val tagsToInclude: Option[Set[String]], val tagsToExclude: Set[String], val excludeNestedSuites: Boolean, val dynaTags: DynaTags) extends Serializable {

  requireNonNull(tagsToInclude, tagsToExclude, dynaTags)

  tagsToInclude match {
    case Some(tagsToInclude) =>
      if (tagsToInclude.isEmpty)
        throw new IllegalArgumentException("tagsToInclude was defined, but contained an empty set")
    case None =>
  }

  private def includedTestNames(testNamesAsList: List[String], tags: Map[String, Set[String]]): List[String] = 
    tagsToInclude match {
      case None => testNamesAsList
      case Some(tagsToInclude) =>
        for {
          testName <- testNamesAsList
          if tags contains testName
          intersection = tagsToInclude intersect tags(testName)
          if intersection.size > 0
        } yield testName
    }

  private def verifyPreconditionsForMethods(testNames: Set[String], tags: Map[String, Set[String]]): Unit = {
    val testWithEmptyTagSet = tags.find(tuple => tuple._2.isEmpty)
    testWithEmptyTagSet match {
      case Some((testName, _)) => throw new IllegalArgumentException(testName + " was associated with an empty set in the map passsed as tags")
      case None =>
    }
  }
  
  private def mergeTestTags(testTagsList: List[Map[String, Set[String]]]): Map[String, Set[String]] = {
    val mergedTags = scala.collection.mutable.Map[String, Set[String]]() ++ testTagsList.head
    for (testTags <- testTagsList.tail) {
      for ((testName, tagSet) <- testTags) {
        val existingTagSetOpt = mergedTags.get(testName)
        existingTagSetOpt match {
          case Some(existingTagSet) =>
            mergedTags(testName) = existingTagSet ++ tagSet
          case None => 
            mergedTags += ((testName, tagSet))
        }
      }
    }
    mergedTags.toMap
  }
  
  private[scalatest] def mergeTestDynamicTags(tags: Map[String, Set[String]], suiteId: String, testNames: Set[String]): Map[String, Set[String]] = {
    val dynaTestTags = 
      if (dynaTags.testTags.isDefinedAt(suiteId))
        dynaTags.testTags(suiteId)
      else
        Map.empty[String, Set[String]]
    
    val dynaSuiteTags = 
      if (dynaTags.suiteTags.isDefinedAt(suiteId)) {
        val suiteTags = dynaTags.suiteTags(suiteId)
        Map() ++ testNames.map(tn => (tn, suiteTags))
      }
      else
        Map.empty[String, Set[String]]
     
    mergeTestTags(List(tags, dynaTestTags, dynaSuiteTags))
  }

  /**
   * Filter test names based on their tags.
   *
   * Each tuple in the returned list contains a `String`
   * test name and a `Boolean` that indicates whether the test should be ignored. A test will be marked as ignored
   * if `org.scalatest.Ignore` is in its tags set, and either `tagsToInclude` is `None`, or
   * `tagsToInclude`'s value (a set) contains the test's name, unless another tag for that test besides `org.scalatest.Ignore`
   * is also included in `tagsToExclude`. For example, if a test is tagged with
   * both `org.scalatest.Ignore` and `SlowAsMolasses`, and `SlowAsMolasses`
   * appears in the `tagsToExclude` set, the `SlowAsMolasses` tag will
   * "overpower" the `org.scalatest.Ignore` tag, and this method will return
   * a list that does not include the test name.
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * for ((testName, ignoreTest) <- filter(testNames, tags))
   *   if (ignoreTest)
   *     // ignore the test
   *   else
   *     // execute the test
   * }}}
   *
   * @param testNames test names to be filtered
   * @param tags a map from test name to tags, containing only test names included in the `testNames` set, and
   *   only test names that have at least one tag
   *
   * @throws IllegalArgumentException if any set contained in the passed `tags` map is empty
   */
// I will make this private so I can keep using that darned deprecated implicit conversion.
// TODO: REMOVE THIS DEPRECATED ONCE TESTS PASS, AND AFTER DEPRECATION CYCLE OF THE Function2
// IMPLICIT, REMOVE THE WHOLE PRIVATE METHOD.
  @deprecated("Please use the apply method that takes a suiteId instead, the one with this signature: def apply(testNames: Set[String], testTags: Map[String, Set[String]], suiteId: String): List[(String, Boolean)]")
  private def apply(testNames: Set[String], tags: Map[String, Set[String]]): List[(String, Boolean)] = {

    verifyPreconditionsForMethods(testNames, tags)

    val testNamesAsList = testNames.toList // to preserve the order
    val filtered =
      for {
        testName <- includedTestNames(testNamesAsList, tags)
        if !tags.contains(testName) ||
                (tags(testName).contains(IgnoreTag) && (tags(testName) intersect (tagsToExclude + "org.scalatest.Ignore")).size == 1) ||
                (tags(testName) intersect tagsToExclude).size == 0
      } yield (testName, tags.contains(testName) && tags(testName).contains(IgnoreTag))

    filtered
  }
  
  def apply(testNames: Set[String], tags: Map[String, Set[String]], suiteId: String): List[(String, Boolean)] = {
    val testTags: Map[String, Set[String]] = mergeTestDynamicTags(tags, suiteId, testNames)
    verifyPreconditionsForMethods(testNames, testTags)

    val testNamesAsList = testNames.toList // to preserve the order
    val filtered =
      for {
        testName <- includedTestNames(testNamesAsList, testTags)
        if !testTags.contains(testName) ||
                (testTags(testName).contains(IgnoreTag) && (testTags(testName) intersect (tagsToExclude + "org.scalatest.Ignore")).size == 1) ||
                (testTags(testName) intersect tagsToExclude).size == 0
      } yield (testName, testTags.contains(testName) && testTags(testName).contains(IgnoreTag))

    filtered
  }

  /**
   * Filter one test name based on its tags.
   *
   * The returned tuple contains a `Boolean`
   * that indicates whether the test should be filtered, and if not, a `Boolean` that
   * indicates whether the test should be ignored. A test will be marked as ignored
   * if `org.scalatest.Ignore` is in its tags set, and either `tagsToInclude`
   * is `None`, or `tagsToInclude`'s value (a set) contains the passed
   * test name, unless another tag for that test besides `org.scalatest.Ignore`
   * is also included in `tagsToExclude`. For example, if a test is tagged with
   * both `org.scalatest.Ignore` and `SlowAsMolasses`, and `SlowAsMolasses`
   * appears in the `tagsToExclude` set, the `SlowAsMolasses` tag will
   * "overpower" the `org.scalatest.Ignore` tag, and this method will return
   * (true, false). 
   * 
   * 
   * {{{  <!-- class="stHighlight" -->
   * val (filterTest, ignoreTest) = filter(testName, tags)
   * if (!filterTest)
   *   if (ignoreTest)
   *     // ignore the test
   *   else
   *     // execute the test
   * }}}
   *
   * @param testName the test name to be filtered
   * @param tags a map from test name to tags, containing only test names that have at least one tag
   * @param suiteId the suite Id of the suite to filter
   *
   * @throws IllegalArgumentException if any set contained in the passed `tags` map is empty
   */
  def apply(testName: String, tags: Map[String, Set[String]], suiteId: String): (Boolean, Boolean) = {
    val testTags: Map[String, Set[String]] = mergeTestDynamicTags(tags, suiteId, Set(testName))
    val list = apply(Set(testName), testTags)
    if (list.isEmpty)
      (true, false)
    else
      (false, list.head._2)
  }

  /**
   * Returns the number of tests that should be run after the passed `testNames` and `tags` have been filtered
   * with the `tagsToInclude` and `tagsToExclude` class parameters.
   *
   * The result of this method may be smaller than the number of
   * elements in the list returned by `apply`, because the count returned by this method does not include ignored tests,
   * and the list returned by `apply` does include ignored tests.
   * 
   *
   * @param testNames test names to be filtered
   * @param tags a map from test name to tags, containing only test names included in the `testNames` set, and
   *   only test names that have at least one tag
   * @param suiteId the suite Id of the suite to filter
   *
   * @throws IllegalArgumentException if any set contained in the passed `tags` map is empty
   */
  def runnableTestCount(testNames: Set[String], testTags: Map[String, Set[String]], suiteId: String): Int = {
    val tags: Map[String, Set[String]] = mergeTestDynamicTags(testTags, suiteId, testNames)
    verifyPreconditionsForMethods(testNames, tags)

    val testNamesAsList = testNames.toList // to preserve the order
    val runnableTests = 
      for {
        testName <- includedTestNames(testNamesAsList, tags)
        if !tags.contains(testName) || (!tags(testName).contains(IgnoreTag) && (tags(testName) intersect tagsToExclude).size == 0)
      } yield testName

    runnableTests.size
  }
}

object Filter {
  private final val IgnoreTag = "org.scalatest.Ignore"

/**
 * Factory method for a `Filter` initialized with the passed `tagsToInclude`
 * and `tagsToExclude`.
 *
 * @param tagsToInclude an optional `Set` of `String` tag names to include (''i.e.'', not filter out) when filtering tests
 * @param tagsToExclude a `Set` of `String` tag names to exclude (''i.e.'', filter out) when filtering tests
 * @param excludeNestedSuites a `Boolean` to indicate whether to run nested suites
 * @param dynaTags dynamic tags for the filter
 *
 * @throws NullArgumentException if either `tagsToInclude` or `tagsToExclude` are null
 * @throws IllegalArgumentException if `tagsToInclude` is defined, but contains an empty set
 */
  def apply(tagsToInclude: Option[Set[String]] = None, tagsToExclude: Set[String] = Set(IgnoreTag), excludeNestedSuites: Boolean = false, dynaTags: DynaTags = DynaTags(Map.empty, Map.empty)) =
    new Filter(tagsToInclude, tagsToExclude, excludeNestedSuites, dynaTags)

  /**
   * Factory method for a default `Filter`, for which `tagsToInclude is `None`, 
   * `tagsToExclude` is `Set("org.scalatest.Ignore")`, and `excludeNestedSuites` is false.
   *
   * @return a default `Filter`
   */
  def default: Filter = apply()

  @deprecated("This implicit conversion was added in ScalaTest 3.0.0 because the inheritance relationship between Filter and Function2[Set[String], Map[String, Set[String]], List[(String, Boolean)]] was dropped. Please use the apply method that takes a suiteId instead, the one with this signature: def apply(testNames: Set[String], testTags: Map[String, Set[String]], suiteId: String): List[(String, Boolean)].")
  implicit def convertFilterToFunction2(filter: Filter): (Set[String], Map[String, Set[String]]) => List[(String, Boolean)] = (set, map) => filter.apply(set, map)
}
