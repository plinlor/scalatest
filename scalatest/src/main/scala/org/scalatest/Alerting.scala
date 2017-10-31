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
 * Trait that contains the `alert` method, which can be used to send an alert to the reporter.
 *
 * One difference between `alert` and the `info` method of `Informer` is that
 * `info` messages provided during a test are recorded and sent as part of test completion event, whereas
 * `alert` messages are sent right away as `AlertProvided` messages. For long-running tests,
 * `alert` allows you to send "alert notifications" to the reporter right away, so users can be made aware
 * of potential problems being experienced by long-running tests. By contrast, `info` messages will only be seen by the user after the
 * test has completed, and are more geared towards specification (such as Given/When/Then messages) than notification.
 * 
 *
 * The difference between `alert` and the `update` method of <a href="Updating.html">`Updating`</a> is
 * that `alert` is intended to be used
 * for warnings or notifications of potential problems, whereas `update` is just for status updates.
 * In string reporters for which ANSI color is enabled, `update` notifications are shown in green and `alert` notifications
 * in yellow.
 * 
 */
trait Alerting {

  /**
   * Returns an `Alerter` that can send an alert message via an `AlertProvided` event to the reporter.
   */
  protected def alert: Alerter
}
