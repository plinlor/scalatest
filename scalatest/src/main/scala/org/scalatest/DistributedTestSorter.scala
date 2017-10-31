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

import events.Event

/**
 * A sorter for the events of a suite's distributed tests.
 *
 * This trait is used, for example, by <a href="ParallelTestExecution.html">`ParallelTestExecution`</a> to sort the
 * events of tests back into sequential order, with a timeout if an event takes too long.
 * 
 */
trait DistributedTestSorter {

  /**
   * Indicates a test with the specified name is about to be distributed.
   *
   * For example, trait `ParallelTestExecution` invokes this method prior to
   * passing a suite that will execute the specified test to the `Distributor`.
   * Even though the tests are run in parallel, the events for the tests will be reported
   * in the order this method is invoked.
   * 
   *
   * @throws IllegalArgumentException if the specified test name has already
   *     completed (was already passed to `distributingTest`), but its events
   *     have not yet been fully reported.
   * @throws NullArgumentException if `testName` is null.
   *
   * @param testName the name of the test that has completed
   */
  def distributingTest(testName: String)

  /**
   * Report an event for a distributed test.
   *
   * @param testName the name of the distributed test that produced this event
   * @param event the event to report
   * @throws NullArgumentException if either `testName` or `event` is null.
   */
  def apply(testName: String, event: Event)

  /**
   * Indicates the events for the distributed test with the specified name have all been fired.
   *
   * @throws IllegalArgumentException if the specified test name was never distributed
   *     (''i.e.'', was never passed to `distributingTest`), or has already
   *     completed (was already passed to `completedTest`.
   * @throws NullArgumentException if `testName` is null.
   *
   * @param testName the name of the test that has completed
   */
  def completedTest(testName: String)
}
