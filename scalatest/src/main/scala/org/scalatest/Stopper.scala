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
 * Trait whose instances can accept a stop request and indicate whether a stop has already been requested.
 *
 * This is passed in
 * to the `run` method of <a href="Suite.html">`Suite`</a>, so that running suites of tests can be
 * requested to stop early.
 * 
 *
 * @author Bill Venners
 */
trait Stopper {

  /**
   * Indicates whether a stop has been requested.
   *
   * Call this method
   * to determine whether a running test should stop. The `run` method of any `Suite`, or
   * code invoked by `run`, should periodically check the
   * stop requested function. If `true`,
   * the `run` method should interrupt its work and simply return.
   * 
   *
   * @return true if a stop has been requested
   */
  def stopRequested: Boolean

  /**
   * Request that the current run stop.
   *
   * Invoking this method is like pulling the stop-request chord in a streetcar. It requests a stop, but in no
   * way forces a stop. The running suite of tests decides when and how (and if) to respond to a stop request.
   * ScalaTest's style traits periodically check the `stopRequested` method of the passed `Stopper`,
   * and if a stop has been requested, terminates gracefully.
   * 
   */
  def requestStop()
}

/**
 * Companion object to Stopper that holds a factory method that produces a new `Stopper` whose
 * `stopRequested` method returns false until after its `requestStop` has been
 * invoked.
 */
object Stopper {

  private class DefaultStopper extends Stopper {
    @volatile private var stopWasRequested = false
    def stopRequested: Boolean = stopWasRequested
    def requestStop(): Unit = {
      stopWasRequested = true
    }
  }

  /**
   * Factory method that produces a new `Stopper` whose
   * `stopRequested` method returns false until after its `requestStop` has been
   * invoked.
   *
   * The `Stopper` returned by this method can be safely used by multiple threads concurrently.
   * 
   *
   * @return a new default stopper
   */
  def default: Stopper = new DefaultStopper
}
