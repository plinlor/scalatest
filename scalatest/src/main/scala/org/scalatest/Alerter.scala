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

import org.scalactic.source

/**
 * Trait providing an `apply` method to which alert messages about a running suite of tests can be reported.
 * 
 * An `Alerter` is essentially
 * used to wrap a `Reporter` and provide easy ways to send alert messages
 * to that `Reporter` via an `AlertProvided` event.
 * `Alerter` contains an `apply` method that takes a string and
 * an optional payload object of type `Any`.
 * The `Alerter` will forward the passed alert `message` string to the
 * <a href="Reporter.html">`Reporter`</a> as the `message` parameter, and the optional
 * payload object as the `payload` parameter, of an <a href="AlertProvided.html">`AlertProvided`</a> event.
 * 
 *
 * For insight into the differences between `Alerter`, `Notifier`, and `Informer`, see the
 * main documentation for trait <a href="Alerting.html">`Alerting`</a>.
 * 
 */
trait Alerter {

  /**
   * Send an alert message via an `AlertProvided` event to the reporter.
   */
  def apply(message: String, payload: Option[Any] = None)(implicit pos: source.Position): Unit
}
