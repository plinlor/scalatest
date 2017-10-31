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
package org.scalatest.events


import java.util.Arrays

/**
 * Class used to specify a sequential order for events reported during a test run, so they
 * can be arranged in that order in a report even if the events were fired in some other order
 * during concurrent or distributed execution.
 *
 * An `Ordinal` is an immutable object holding a ''run stamp'' and a sequence
 * of ''stamps''.
 * The run stamp is an integer that identifies a particular run. All events
 * reported during the same run should share the same run stamp. By contrast, each
 * event reported during a particular run should have a different stamp sequence.
 * One use case for the run stamp is that the initial run from ScalaTest's GUI
 * will have run stamp 0. Subsequent reruns will have run stamps 1,
 * 2, 3, ''etc.'', so that reports in the GUI can simply be sorted in "ordinal" order. Another
 * use case is a set of servers used to run multiple tests simultaneously in a distributed
 * fashion. The run stamp can be used to identify the run to which an event belongs.
 * 
 *
 * The stamp sequence is designed to allow a sequential order of events to be specified during
 * concurrent execution of ScalaTest suites. ScalaTest's model for concurrent execution is that
 * the suites that make up a run may be executed concurrently, but the tests within a single suite
 * will be executed sequentially. In addition to tests, suites may contain nested suites. The default implementation
 * of `execute` in class <a href="../Suite.html">`Suite`</a> will first invoke `runNestedSuites` and
 * then `runTests`. If no <a href="../Distributor.html">`Distributor`</a> is passed to `execute`, the
 * `runNestedSuites` method will execute the nested suites sequentially via the same thread
 * that invoked `runNestedSuites`. As a result, suites will by default executed in depth first order
 * when executed sequentially. If a `Distributor` is passed to `execute`, the
 * `runNestedSuites` method will simply put its nested suites into the `Distributor`
 * and return. Some other threads or processes must then execute those nested suites. Given the default
 * implementations of `execute` and `runNestedSuites` described here, the `Ordinal`
 * will allow the events from a concurrent run to be sorted in the same depth-first order that the events
 * from a corresponding sequential run would arrive.
 * 
 *
 * Each event reported during a run should be given a unique `Ordinal`. An `Ordinal` is required
 * by all <a href="Event.html">`Event`</a> subclasses, instances of which are used to send information to the `report`
 * function passed to a `Suite`'s `execute` method. The first `Ordinal` for a run
 * can be produced by passing a run stamp to `Ordinal`'s lone public constructor:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * val firstOrdinal = new Ordinal(99)
 * }}}
 *
 * The run stamp can be any integer. The `Ordinal` created in this way can be passed along with the first
 * reported event of the run, such as a <a href="RunStarting.html">`RunStarting`</a> event. Thereafter, new `Ordinal`s for the same run
 * can be obtained by calling either `next` or `nextNewOldPair` on the previously obtained `Ordinal`.
 * In other words, given an `Ordinal`, you can obtain the next `Ordinal` by invoking one of these two
 * "next" methods on the `Ordinal` you have in hand. Before executing a new `Suite`, the `nextNewOldPair`
 * method should be invoked. This will return two new `Ordinal`s, one for the new `Suite` about to be executed, and
 * one for the currently executing entity (either a `Suite` or some sort of test runner). At any other time, the next `Ordinal`
 * can be obtained by simply invoking `next` on the current `Ordinal`.
 * 
 *
 * You can convert an `Ordinal` to a `List` by invoking `toList` on it. The resulting `List` will contain
 * the run stamp as its first element, and the contents of its stamps sequence as the subsequent elements. The stamps
 * sequence will initially be composed of a single element with the value 0. Thus, `toList` invoked on the `firstOrdinal` shown above will 
 * result in:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * firstOrdinal.toList // results in: List(99, 0)
 * }}}
 *
 * Each time `next` is invoked, the rightmost integer returned by `toList` will increment: 
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * val secondOrdinal = firstOrdinal.next
 * secondOrdinal.toList // results in: List(99, 1)
 * 
 * val thirdOrdinal = secondOrdinal.next
 * thirdOrdinal.toList  // result is : List(99, 2)
 * }}}
 *
 * When `nextNewOldPair` is invoked the result will be a tuple whose first element is the first `Ordinal` for
 * the new `Suite` about to be executed (for example, a nested `Suite` of the currently executing `Suite`). The
 * second element is the next `Ordinal` for the currently executing `Suite` or other entity:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val (nextForNewSuite, nextForThisRunner) = thirdOrdinal.nextNewOldPair
 * nextForNewSuite.toList   // results in: (99, 2, 0)
 * nextForThisRunner.toList // results in: (99, 3)
 * }}}
 *
 * The `toList` method of the `Ordinal` for the new suite starts with the same sequence of elements as the `Ordinal` from which it was
 * created, but has one more element, a 0, appended at the end. Subsequent invocations of `next` on this series of `Ordinal`s will
 * increment that last element:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val newSuiteOrdinal2 = nextForNewSuite.next
 * newSuiteOrdinal2.toList // results in: List(99, 2, 1)
 * 
 * val newSuiteOrdinal3 = newSuiteOrdinal2.next
 * newSuiteOrdinal3.toList  // result is : List(99, 2, 2)
 * }}}
 *
 * This behavior allows events fired by `Suite` running concurrently to be reordered in a pre-determined sequence after all the events
 * have been reported. The ordering of two `Ordinal`s can be determined by first comparing the first element of the `List`s obtained
 * by invoking `toList` on both `Ordinal`s. These values represent the `runStamp`. If one run stamp is a lower number than
 * the other, that `Ordinal` comes first. For example, an `Ordinal` with a run stamp of 98 is ordered before an `Ordinal` with
 * a run stamp of 99. If the run stamps are equal, the next number in the list is inspected. As with the run stamps, an  `Ordinal` with a lower
 * number is ordered before an `Ordinal` with a higher number. If two corresponding elements are equal, the next pair of elements will be inspected.
 * This will continue no down the length of the `List`s until a position is found where the element values are not equal, or the end of one or both of
 * the `List`s are reached. If the two `List`s are identical all the way to the end, and both `List`s have the same lengths, 
 * then the `Ordinal`s are equal. (Equal `Ordinal`s will not happen if correctly used by creating a new `Ordinal` for
 * each fired event and each new `Suite`.). If the two `List`s are identical all the way to the end of one, but the other `List`
 * is longer (has more elements), then the shorter list is ordered before the longer one.
 * 
 *
 * As an example, here are some `Ordinal` `List` forms in order:
 * 
 *
 * {{{
 * List(99, 0)
 * List(99, 1)
 * List(99, 2)
 * List(99, 2, 0)
 * List(99, 2, 1)
 * List(99, 2, 2)
 * List(99, 2, 2, 0)
 * List(99, 2, 2, 1)
 * List(99, 2, 2, 2)
 * List(99, 2, 3)
 * List(99, 2, 4)
 * List(99, 2, 4, 0)
 * List(99, 2, 4, 1)
 * List(99, 2, 4, 2)
 * List(99, 3)
 * List(99, 4)
 * List(99, 4, 0)
 * List(99, 4, 1)
 * List(99, 5)
 * }}}
 *
 * @param runStamp A number that identifies a particular run
 *
 * @author Bill Venners
 */
final class Ordinal private (val runStamp: Int, private val stamps: Array[Int]) extends Ordered[Ordinal] with java.io.Serializable {

  /**
   * Construct a the first `Ordinal` for a run.
   *
   * @param runStamp a number that identifies a particular run
   */
  def this(runStamp: Int) = this(runStamp, Array(0))

  /**
   * Construct the next `Ordinal` for the current suite or other entity, such as a runner.
   */
  def next: Ordinal = {
    val newArray = new Array[Int](stamps.length) // Can't seem to clone
    val zipped = stamps.zipWithIndex
    for ((num, idx) <- zipped)
      newArray(idx) = num
    newArray(stamps.length - 1) += 1
    new Ordinal(runStamp, newArray)
  }

  /**
   * Construct two new `Ordinal`s, one for a new `Suite` about to be executed and
   * one for the current `Suite` or other entity, such as a runner. The `Ordinal`
   * for the new `Suite` is the first (`_1`) element in the tuple:
   *
   * {{{  <!-- class="stHighlight" -->
   * val (nextOrdinalForNewSuite, nextOrdinalForThisSuite) currentOrdinal.nextNewOldPair
   * }}}
   *
   * The reason the next `Ordinal` for the new `Suite` is first is because it will
   * be ordered ''before'' the next `Ordinal` for the current `Suite` (or other
   * entity such as a runner). In fact, any event reported within the context of the new `Suite` or
   * its nested `Suite`s will be ordered before the next `Ordinal` for the current `Suite`.
   * 
   *
   * @return a tuple whose first element is the first `Ordinal` for the new `Suite` and whose
   *          second element is the next `Ordinal` for the current `Suite` or other entity, such
   *          as a runner.
   */
  def nextNewOldPair: (Ordinal, Ordinal) = {
    val newArrayForNewSuite = new Array[Int](stamps.length + 1)
    val newArrayForOldSuite = new Array[Int](stamps.length)
    val zipped = stamps.zipWithIndex
    for ((num, idx) <- zipped) {
      newArrayForNewSuite(idx) = num
      newArrayForOldSuite(idx) = num
    }
    newArrayForOldSuite(stamps.length - 1) += 1
    (new Ordinal(runStamp, newArrayForNewSuite), new Ordinal(runStamp, newArrayForOldSuite))
  }

  /**
   * Returns a `List[Int]` representation of this `Ordinal`. A set of `Ordinal`s will be ordered
   * in the same order as the set of `List[Int]`s that are returned by invoking this method on each of the `Ordinal`s.
   * The first element of the returned `List[Int]` is the `runStamp`.
   *
   * @return a `List[Int]` representation of this `Ordinal`.
   */
  def toList: List[Int] = runStamp :: stamps.toList

  /**
   * Compares this `Ordinal` with the passed `Ordinal` for order. If this object is "less than" (ordered before)
   * the passed object, `compare` will return a negative integer. If this class is "greater than" (ordered after)
   * the passed object, `compare` will return a positive integer. Otherwise, this `Ordinal` is equal to
   * the passed object, and `compare` will return 0.
   * 
   * @return a negative integer, 0, or positive integer indicating this `Ordinal` is less than, equal to, or greater than the passed `Ordinal`.
   */
  def compare(that: Ordinal) = {
    val runStampDiff = this.runStamp - that.runStamp
    if (runStampDiff == 0) {
      val shorterLength =
        if (this.stamps.length < that.stamps.length)
          this.stamps.length
        else
          that.stamps.length
      var i = 0
      var diff = 0
      while (diff == 0 && i < shorterLength) {
        diff = this.stamps(i) - that.stamps(i)
        i += 1
      }
      // If they were equal all the way to the shorterLength, the longest array
      // one is the greater ordinal. This is because the newSuite stuff happens
      // before the next thing that happens in the old suite.
      if (diff != 0) diff
      else this.stamps.length - that.stamps.length
    }
    else runStampDiff
  }

  /**
   * Indicates whether the passed object is equal to this one.
   *
   * @param the object with which to compare this one for equality
   * @return true if the passed object is equal to this one
   */
  override def equals(other: Any): Boolean =
    other match {
      case that: Ordinal =>
        runStamp == that.runStamp &&
        (stamps.deep == that.stamps.deep)
      case _ => false
    }

  /**
   * Returns a hash code value for this object.
   *
   * @return a hash code for this object
   */
  override def hashCode: Int =
    41 * (
      41 + runStamp
    ) + Arrays.hashCode(stamps)

  /**
   * Returns a string that includes the integers returned by `toList`.
   */
  override def toString: String = toList.mkString("Ordinal(", ", ", ")")
}
