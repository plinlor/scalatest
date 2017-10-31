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

import org.scalatest.events.Event

/**
 * Trait whose instances collect the results of a running
 * suite of tests and presents those results in some way to the user. Instances of this trait can
 * be called "report functions" or "reporters."
 *
 * Reporters receive test results via fifteen events.
 * Each event is fired to pass a particular kind of information to
 * the reporter. The events are:
 * 
 *
 * <ul>
 * <li><a href="events/DiscoveryStarting.html">`DiscoveryStarting`</a></li>
 * <li><a href="events/DiscoveryCompleted.html">`DiscoveryCompleted`</a></li>
 * <li><a href="events/RunStarting.html">`RunStarting`</a></li>
 * <li><a href="events/RunStopped.html">`RunStopped`</a></li>
 * <li><a href="events/RunAborted.html">`RunAborted`</a></li>
 * <li><a href="events/RunCompleted.html">`RunCompleted`</a></li>
 * <li><a href="events/ScopeOpened.html">`ScopeOpened`</a></li>
 * <li><a href="events/ScopeClosed.html">`ScopeClosed`</a></li>
 * <li><a href="events/ScopePending.html">`ScopePending`</a></li>
 * <li><a href="events/TestStarting.html">`TestStarting`</a></li>
 * <li><a href="events/TestSucceeded.html">`TestSucceeded`</a></li>
 * <li><a href="events/TestFailed.html">`TestFailed`</a></li>
 * <li><a href="events/TestCanceled.html">`TestCanceled`</a></li>
 * <li><a href="events/TestIgnored.html">`TestIgnored`</a></li>
 * <li><a href="events/TestPending.html">`TestPending`</a></li>
 * <li><a href="events/SuiteStarting.html">`SuiteStarting`</a></li>
 * <li><a href="events/SuiteCompleted.html">`SuiteCompleted`</a></li>
 * <li><a href="events/SuiteAborted.html">`SuiteAborted`</a></li>
 * <li><a href="events/InfoProvided.html">`InfoProvided`</a></li>
 * <li><a href="events/MarkupProvided.html">`MarkupProvided`</a></li>
 * <li><a href="events/AlertProvided.html">`AlertProvided`</a></li>
 * <li><a href="events/NoteProvided.html">`NoteProvided`</a></li>
 * </ul>
 *
 * Reporters may be implemented such that they only present some of the reported events to the user. For example, you could
 * define a reporter class that does nothing in response to `SuiteStarting` events.
 * Such a class would always ignore `SuiteStarting` events.
 * 
 *
 * The term ''test'' as used in the `TestStarting`, `TestSucceeded`,
 * and `TestFailed` event names
 * is defined abstractly to enable a wide range of test implementations.
 * ScalaTest's style traits (subclasse of trait <a href="Suite.html">`Suite`</a>) fire
 * `TestStarting` to indicate they are about to invoke one
 * of their tests, `TestSucceeded` to indicate a test returned normally,
 * and `TestFailed` to indicate a test completed abruptly with an exception.
 * Although the execution of a `Suite` subclass's tests will likely be a common event
 * reported via the
 * `TestStarting`, `TestSucceeded`, and `TestFailed` events, because
 * of the abstract definition of &ldquo;test&rdquo; used by the
 * the event classes, these events are not limited to this use. Information about any conceptual test
 * may be reported via the `TestStarting`, `TestSucceeded`, and
 * `TestFailed` events.
 *
 * Likewise, the term ''suite'' as used in the `SuiteStarting`, `SuiteAborted`,
 * and `SuiteCompleted` event names
 * is defined abstractly to enable a wide range of suite implementations.
 * Object <a href="tools/Runner$.html">`Runner`</a> fires `SuiteStarting` to indicate it is about to invoke
 * `run` on a
 * `Suite`, `SuiteCompleted` to indicate a `Suite`'s
 * `run` method returned normally,
 * and `SuiteAborted` to indicate a `Suite`'s `run`
 * method completed abruptly with an exception.
 * Similarly, class `Suite` fires `SuiteStarting` to indicate it is about to invoke
 * `run` on a
 * nested `Suite`, `SuiteCompleted` to indicate a nested `Suite`'s
 * `run` method returned normally,
 * and `SuiteAborted` to indicate a nested `Suite`'s `run`
 * method completed abruptly with an exception.
 * Although the execution of a `Suite`'s `run` method will likely be a
 * common event reported via the
 * `SuiteStarting`, `SuiteAborted`, and `SuiteCompleted` events, because
 * of the abstract definition of "suite" used by the
 * event classes, these events are not limited to this use. Information about any conceptual suite
 * may be reported via the `SuiteStarting`, `SuiteAborted`, and
 * `SuiteCompleted` events.
 *
 * ==Extensibility==
 *
 * You can create classes that extend `Reporter` to report test results in custom ways, and to
 * report custom information passed as an event "payload."
 * `Reporter` classes can handle events in any manner, including doing nothing.
 * 
 *
 * @author Bill Venners
 */
trait Reporter {

  /**
   * Invoked to report an event that subclasses may wish to report in some way to the user.
   *
   * @param event the event being reported
   */
  def apply(event: Event)
}

private[scalatest] object Reporter {

  private[scalatest] def indentStackTrace(stackTrace: String, level: Int): String = {
    val indentation = if (level > 0) "  " * level else ""
    val withTabsZapped = stackTrace.replaceAll("\t", "  ")
    val withInitialIndent = indentation + withTabsZapped
    withInitialIndent.replaceAll("\n", "\n" + indentation) // I wonder if I need to worry about alternate line endings. Probably.
  }

  // In the unlikely event that a message is blank, use the throwable's detail message
  private[scalatest] def messageOrThrowablesDetailMessage(message: String, throwable: Option[Throwable]): String = {
    val trimmedMessage = message.trim
    if (!trimmedMessage.isEmpty)
      trimmedMessage
    else
      throwable match {
        case Some(t) => t.getMessage.trim
        case None => ""
      }
  }

  // TODO: Not a real problem, but if a DispatchReporter ever got itself in
  // its list of reporters, this would end up being an infinite loop. But
  // That first part, a DispatchReporter getting itself in there would be the real
  // bug.
  def propagateDispose(reporter: Reporter): Unit = {
    reporter match {
      // SKIP-SCALATESTJS-START
      case dispatchReporter: DispatchReporter => dispatchReporter.dispatchDisposeAndWaitUntilDone()
      // SKIP-SCALATESTJS-END
      case resourcefulReporter: ResourcefulReporter => resourcefulReporter.dispose()
      case _ =>
    }
  }
}

  /*
      case RunStarting(ordinal, testCount, formatter, payload, threadName, timeStamp) => runStarting(testCount)

      case TestStarting(ordinal, suiteName, suiteClassName, testName, formatter, rerunnable, payload, threadName, timeStamp) =>

      case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) => 

      case TestFailed(ordinal, message, suiteName, suiteClassName, testName, throwable, duration, formatter, rerunnable, payload, threadName, timeStamp) => 

      case TestIgnored(ordinal, suiteName, suiteClassName, testName, formatter, payload, threadName, timeStamp) => 

      case TestPending(ordinal, suiteName, suiteClassName, testName, formatter, payload, threadName, timeStamp) => 

      case SuiteStarting(ordinal, suiteName, suiteClassName, formatter, rerunnable, payload, threadName, timeStamp) =>

      case SuiteCompleted(ordinal, suiteName, suiteClassName, duration, formatter, rerunnable, payload, threadName, timeStamp) => 

      case SuiteAborted(ordinal, message, suiteName, suiteClassName, throwable, duration, formatter, rerunnable, payload, threadName, timeStamp) => 

      case InfoProvided(ordinal, message, nameInfo, throwable, formatter, payload, threadName, timeStamp) => {

      case RunStopped(ordinal, duration, summary, formatter, payload, threadName, timeStamp) => runStopped()

      case RunAborted(ordinal, message, throwable, duration, summary, formatter, payload, threadName, timeStamp) => 

      case RunCompleted(ordinal, duration, summary, formatter, payload, threadName, timeStamp) => runCompleted()
*/

