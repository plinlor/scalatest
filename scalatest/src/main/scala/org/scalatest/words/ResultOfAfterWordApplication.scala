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


/**
 * Class that supports the use of ''after words'' in `WordSpec`
 * and `fixture.WordSpec`.
 *
 * A `ResultOfAfterWordApplication`, which encapsulates the text of the after word
 * and a block,
 * is accepted by `when`, `should`, `must`, `can`, and `that`
 * methods.  For more information, see the
 * <a href="../WordSpec.html#AfterWords">main documentation`</a> for trait `WordSpec`.
 * 
 */
final class ResultOfAfterWordApplication(val text: String, val f: () => Unit) {
  /**
   * Overrides toString to return the passed in text.
   */
  override def toString: String = text
}
