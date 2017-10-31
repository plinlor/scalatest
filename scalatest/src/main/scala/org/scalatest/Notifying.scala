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
 * Trait that contains the `note` method, which can be used to send a status notification to the reporter.
 *
 * The difference between `note` and the `info` method of <a href="Informer.html">`Informer`</a> is that
 * `info` messages provided during a test are recorded and sent as part of test completion event, whereas
 * `note` messages are sent right away as <a href="events/NoteProvided.html">`NoteProvided`</a> messages. For long-running tests,
 * `note` allows you to send "status notifications" to the reporter right away, so users can track the
 * progress of the long-running tests. By contrast, `info` messages will only be seen by the user after the
 * test has completed, and are more geared towards specification (such as <a href="GivenWhenThen.html">Given/When/Then</a> messages) than notification.
 * 
 *
 * The difference between `note` and the `alert` method of <a href="Alerting.html">`Alerting`</a> is
 * that `alert` is intended to be used
 * for warnings or notifications of potential problems, whereas `note` is just for status notifications.
 * In string reporters for which ANSI color is enabled, `note` notifications are shown in green and `alert` notifications
 * in yellow.
 * 
 */
trait Notifying {

  /**
   * Returns an `Notifier` that can send a status notification via an `NoteProvided` event to the reporter.
   */
  protected def note: Notifier
}
