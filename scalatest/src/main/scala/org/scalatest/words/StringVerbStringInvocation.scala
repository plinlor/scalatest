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

import org.scalactic._

/**
 * Class that provides a role-specific type for an implicit conversion used to support
 * the registration of subject descriptions in `FlatSpec` and `FreeSpec` styles.
 *
 * For example, this class enables syntax such as the following in `WordSpec`
 * and `fixture.WordSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * "A Stack (when empty)" should { ...
 *                        ^
 * }}}
 *
 * This `should` method, which is provided in `ShouldVerb`, needs an implicit parameter
 * of type `StringVerbBlockRegistration`.
 *
 * @author Bill Venners
 */
abstract class StringVerbStringInvocation {

  /**
   * Registers a subject description in `WordSpec` and `fixture.WordSpec`.
   *
   * For example, this class enables syntax such as the following in `WordSpec`
   * and `fixture.WordSpec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "A Stack (when empty)" should { ...
   *                        ^
   * }}}
   *
   */
  def apply(subject: String, verb: String, predicate: String, pos: source.Position): ResultOfStringPassedToVerb
}

