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

import org.scalactic._

/**
 * A bundle of information about the current test.
 *
 * A <code>TestData</code> object is passed to the <code>withFixture</code> methods of traits <code>Suite</code> and <code>fixture.Suite</code>
 * (both <a href="Suite$NoArgTest.html"><code>NoArgTest</code></a> and <a href="fixture/Suite$OneArgTest.html"><code>OneArgTest</code></a>
 * extend <code>TestData</code>) and to the <code>beforeEach</code> and <code>afterEach</code>
 * methods of trait <a href="BeforeAndAfterEach.html"><code>BeforeAndAfterEach</code></a>. This enables fixtures and tests to make use
 * of the test name and configuration objects in the config map.
 * 
 *
 * In ScalaTest's event model, a test may be surrounded by &ldquo;scopes.&rdquo; Each test and scope is associated with string of text.
 * A test's name is a concatenation of the text of any surrounding scopes followed by the text provided with the test
 * itself, after each text element has been trimmed and one space inserted between each component. Here's an example:
 * 
 *
 * <pre class="stHighlight">
 * package org.scalatest.examples.freespec
 * 
 * import org.scalatest.FreeSpec
 * 
 * class SetSpec extends FreeSpec {
 * 
 *   "A Set" - {
 *     "when empty" - {
 *       "should have size 0" in {
 *         assert(Set.empty.size === 0)
 *       }
 *       
 *       "should produce NoSuchElementException when head is invoked" in {
 *         assertThrows[NoSuchElementException] {
 *           Set.empty.head
 *         }
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * The above <code>FreeSpec</code> contains two tests, both nested inside the same two scopes. The outermost scope names
 * the subject, <code>A Set</code>. The nested scope qualifies the subject with <code>when empty</code>. Inside that
 * scope are the two tests. The text of the tests are:
 *
 * <ul>
 * <li><code>should have size 0</code></li>
 * <li><code>should produce NoSuchElementException when head is invoked</code></li>
 * </ul>
 *
 * Therefore, the names of these two tests are:
 * 
 *
 * <ul>
 * <li><code>A Stack when empty should have size 0</code></li>
 * <li><code>A Stack when empty should produce NoSuchElementException when head is invoked</code></li>
 * </ul>
 *
 * The <code>TestData</code> instance for the first test would contain:
 * 
 *
 * <ul>
 * <li><code>name</code>: <code>"A Stack when empty should have size 0"</code></li>
 * <li><code>scopes</code>: <code>collection.immutable.IndexedSeq("A Stack", "when empty")</code></li>
 * <li><code>text</code>: <code>"should have size 0"</code></li>
 * </ul>
 *
 * @author Bill Venners
 * @author Chua Chee Seng
 */
trait TestData {

  /**
   * A <code>ConfigMap</code> containing objects that can be used
   * to configure the fixture and test.
   */
  val configMap: ConfigMap 

  /**
   * The name of this test.
   *
   * See the main documentation for this trait for an explanation of the difference between <code>name</code>, <code>text</code>,
   * and <code>scopes</code>.
   * 
   */
  val name: String

  /**
   * An immutable <code>IndexedSeq</code> containing the text for any "scopes" enclosing this test, in order
   * from outermost to innermost scope.
   *
   * See the main documentation for this trait for an explanation of the difference between <code>name</code>, <code>text</code>,
   * and <code>scopes</code>. If a test has no surrounding scopes, this field will contain an empty <code>IndexedSeq</code>.
   * 
   */
  val scopes: collection.immutable.IndexedSeq[String]

  /**
   * The "text" for this test.
   *
   * See the main documentation for this trait for an explanation of the difference between <code>name</code>, <code>text</code>,
   * and <code>scopes</code>. If a test has no surrounding scopes, this field will contain the same string as <code>name</code>.
   * 
   */
  val text: String
  
  /**
   * Tag names for this test.
   */
  val tags: Set[String]

  val pos: Option[source.Position]
}

