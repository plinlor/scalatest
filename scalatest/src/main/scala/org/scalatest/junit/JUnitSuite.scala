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
package org.scalatest.junit;




/**
 * A suite of tests that can be run with either JUnit or ScalaTest. This class allows you to write JUnit 4 tests
 * with ScalaTest's more concise assertion syntax as well as JUnit's assertions (`assertEquals`, etc.).
 * You create tests by defining methods that are annotated with `Test`, and can create fixtures with
 * methods annotated with `Before` and `After`. For example:
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.junit.JUnitSuite
 * import scala.collection.mutable.ListBuffer
 * import _root_.org.junit.Test
 * import _root_.org.junit.Before
 *
 * class TwoSuite extends JUnitSuite {
 *
 *   var sb: StringBuilder = _
 *   var lb: ListBuffer[String] = _
 *
 *   @Before def initialize() {
 *     sb = new StringBuilder("ScalaTest is ")
 *     lb = new ListBuffer[String]
 *   }
 *
 *   @Test def verifyEasy() {
 *     sb.append("easy!")
 *     assert(sb.toString === "ScalaTest is easy!")
 *     assert(lb.isEmpty)
 *     lb += "sweet"
 *   }
 *
 *   @Test def verifyFun() {
 *     sb.append("fun!")
 *     assert(sb.toString === "ScalaTest is fun!")
 *     assert(lb.isEmpty)
 *   }
 * }
 * }}}
 *
 * To execute `JUnitSuite`s with ScalaTest's `Runner`, you must include JUnit's jar file on the class path or runpath.
 * This version of `JUnitSuite` was tested with JUnit version 4.10.
 * 
 *
 * Instances of this class are not thread safe.
 * 
 *
 * @author Bill Venners
 * @author Daniel Watson
 * @author Joel Neely
 */
class JUnitSuite extends JUnitSuiteLike
