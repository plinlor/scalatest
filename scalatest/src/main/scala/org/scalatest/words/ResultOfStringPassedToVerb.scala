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
package org.scalatest.words

import org.scalatest._

/**
 * Abstract class that supports test registration in `FlatSpec`
 * and `fixture.FlatSpec`.
 *
 * For example, this class enables syntax such as the following pending test registration
 * in `FlatSpec` and `fixture.FlatSpec`:
 * 
 *
 * {{{
 * "A Stack (when empty)" should "be empty" is (pending)
 *                                          ^
 * }}}
 *
 *
 * For example, this class enables syntax such as the following tagged test registration
 * in `FlatSpec` and `fixture.FlatSpec`:
 * 
 *
 * {{{
 * "A Stack (when empty)" should "be empty" taggedAs(SlowTet) in { ... }
 *                                          ^
 * }}}
 *
 * This class also indirectly enables syntax such as the following regular test registration
 * in `FlatSpec` and `fixture.FlatSpec`:
 * 
 *
 * {{{
 * "A Stack (when empty)" should "be empty" in { ... }
 *                                          ^
 * }}}
 *
 * However, this class does not declare any methods named `in`, because the
 * type passed to `in` differs in a `FlatSpec` and a `fixture.FlatSpec`.
 * A `fixture.FlatSpec` needs two `in` methods, one that takes a no-arg
 * test function and another that takes a one-arg test function (a test that takes a
 * `Fixture` as its parameter). By constrast, a `FlatSpec` needs
 * only one `in` method that takes a by-name parameter. As a result,
 * `FlatSpec` and `fixture.FlatSpec` each provide an implicit conversion
 * from `ResultOfStringPassedToVerb` to a type that provides the appropriate
 * `in` methods. 
 * 
 *
 * @author Bill Venners
 */
abstract class ResultOfStringPassedToVerb(val verb: String, val rest: String) {

  /**
   * Supports the registration of pending tests in a
   * `FlatSpec` and `fixture.FlatSpec`.
   *
   * This method supports syntax such as the following:
   * 
   *
   * {{{
   * "A Stack" must "pop values in last-in-first-out order" is (pending)
   *                                                        ^
   * }}}
   *
   * For examples of pending test registration, see the <a href="../FlatSpec.html#PendingTests">Pending tests section</a> in the main documentation
   * for trait `FlatSpec`.
   * 
   */
  def is(fun: => PendingStatement)

  /**
   * Supports the registration of tagged tests in `FlatSpec` and `fixture.FlatSpec`.
   *
   * This method supports syntax such as the following:
   * 
   *
   * {{{
   * "A Stack" must "pop values in last-in-first-out order" taggedAs(SlowTest) in { ... }
   *                                                        ^
   * }}}
   *
   * For examples of tagged test registration, see the <a href="../FlatSpec.html#TaggingTests">Tagging tests section</a> in the main documentation
   * for trait `FlatSpec`.
   * 
   */
  def taggedAs(firstTestTag: Tag, otherTestTags: Tag*): ResultOfTaggedAsInvocation
}
