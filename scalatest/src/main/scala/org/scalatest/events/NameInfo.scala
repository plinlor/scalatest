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


/**
 * Class that holds information about names for the ''information events'' <a href="InfoProvided.html">`InfoProvided`</a>, <a href="MarkupProvided.html">`MarkupProvided`</a>,
 * <a href="ScopeOpened.html">`ScopeOpened`</a>, <a href="ScopeClosed.html">`ScopeClosed`</a>, <a href="ScopePending.html">`ScopePending`</a>, <a href="AlertProvided.html">`AlertProvided`</a> and <a href="NoteProvided.html">`NoteProvided`</a>.
 *
 * An information event may be fired from anywhere. In this respect these events are different
 * from the other events, for which it is defined whether they are fired in the context of a suite or test.
 * If fired in the context of a test, an information event event should include a `NameInfo` in which
 * `testName` is defined. If fired in the context of a suite, but not a test, the `InfoProvided` event
 * should include a `NameInfo` in which `testName` is ''not'' defined. If fired within the context
 * of neither a suite nor a test, the `nameInfo` of the `InfoProvided` event (an `Option[NameInfo]`) should be `None`.
 * 
 *
 * If either `suiteClassName` or `testName` is defined, then `suiteName` and `suiteId` must be defined.
 * The suite class name parameter is optional even if a suite name is provided by passing a `Some` as `suiteName`,
 * because suites in ScalaTest are an abstraction that
 * need not necessarily correspond to one class. Nevertheless, in most cases each suite will correspond
 * to a class, and when it does, the fully qualified name of that class should be reported by passing a
 * `Some` for `suiteClassName`. One use for this bit of information is JUnit integration,
 * because the "name" provided to a JUnit `org.junit.runner.Description` appears to usually include
 * a fully qualified class name by convention.
 * 
 *
 * @param suiteName an optional name of the suite about which an information event was fired
 * @param suiteId an optional string ID for the suite about which an information event was fired, intended to be unique across all suites in a run
 * @param suiteClassName an optional fully qualifed `Suite` class name about which the information was provided
 * @param testName an optional test name information
 *
 * @author Bill Venners
 */
final case class NameInfo(suiteName: String, suiteId: String, suiteClassName: Option[String], testName: Option[String])

