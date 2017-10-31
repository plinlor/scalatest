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

import org.scalatest.events.Ordinal

// Note: The reason Tracker is mutable is that methods would have to pass back, and that's hard because exceptions can
// also be thrown. So this mutable object is how methods invoked "returns" updates to the current ordinal whether those
// methods return normally or complete abruptly with an exception. Also, sometimes with closures capturing free variables,
// those free variables may want to grab an ordinal in the context of a callee even after the callee has already called
// some other method. So in other words the calling method may need to know the "current ordinal" even before the method
// it calls has completed in any manner, i.e., while it is running. (The example is the info stuff in FunSuite, which sets
// up an info that's useful during a run, then calls super.run(...).

/**
 * Class that tracks the progress of a series of `Ordinal`s produced by invoking
 * `next` and `nextNewOldPair` on the current `Ordinal`.
 *
 * Instances of this class are thread safe. Multiple threads can invoke `nextOrdinal`
 * and `nextTracker` concurrently. This facilitates multi-threaded tests that send
 * `infoProvided` reports concurrently. When using a `Dispatcher` to execute
 * suites in parallel, the intention is that each `Tracker` will only be used by one
 * thread. For example, if the optional `Dispatcher`  passed to `Suite`'s implementation
 * of <a href="Suite.html#lifecycle-methods"`runNestedSuites`</a> is defined, that method will obtain a new `Tracker` by invoking
 * `nextTracker` for each nested suite it passes to the `Dispatcher`.
 * 
 *
 * @param firstOrdinal the first `Ordinal` in the series of `Ordinal`s
 *        tracked by this `Tracker`, which will be used to initialize this `Tracker`'s
 *        current `Ordinal`.
 *
 * @author Bill Venners
 */
final class Tracker(firstOrdinal: Ordinal = new Ordinal(0)) {

  private var currentOrdinal = firstOrdinal

  /**
   * Returns the next `Ordinal` in the series tracked by this `Tracker`.
   *
   * This method saves the current `Ordinal` in a local variable, reassigns the current `Ordinal`
   * with the value returned by invoking `nextOrdinal` on the saved `Ordinal`, then
   * returns the saved `Ordinal`. As a result, if this method is invoked immediately after construction,
   * this method will return the `Ordinal` passed as `firstOrdinal`.
   * 
   *
   * @return the next `Ordinal` in the series
   */
  def nextOrdinal(): Ordinal = {
    synchronized {
      val ordinalToReturn = currentOrdinal
      currentOrdinal = currentOrdinal.next
      ordinalToReturn
    }
  }

  /**
   * Returns a `Tracker` initialized with the first element in the tuple returned by invoking
   * `nextNewOldPair` on the current `Ordinal`, and reassigns the current `Ordinal`
   * with the second element that was returned by the `nextNewOldPair` invocation.
   *
   * The `Ordinal` series of the returned `Tracker` will be placed after all the
   * `Ordinal`s previously returned by invoking `nextOrdinal` on this `Tracker` and
   * before all the `Ordinal`s subsequently returned by invoking `nextOrdinal` on
   * this `Tracker` in the future. This method is intended to be used when executing nested suites
   * in parallel. Each nested suite passed to the `Distributor` will get its own `Tracker`
   * obtained by invoking `nextTracker` on the current thread's `Tracker`.
   * 
   *
   * @return the next `Tracker` in this series
   */
  def nextTracker(): Tracker = {
    synchronized {
      val (nextForNewThread, nextForThisThread) = currentOrdinal.nextNewOldPair
      currentOrdinal = nextForThisThread
      new Tracker(nextForNewThread)
    }
  }
}

object Tracker {
  private val defaultTracker = new Tracker()
  def default: Tracker = defaultTracker
}
