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
 * Class that supports shared test registration via instances referenced from the `behave` field of `FunSpec`s,
 * `FlatSpec`s, and `WordSpec`s as well as instance of their sister traits, 
 * `fixture.FunSpec`, `fixture.FlatSpec`, and `fixture.WordSpec`.
 *
 * This class, via the `behave` field, enables syntax such as the following in `FunSpec`s, `FlatSpec`s,
 * `fixture.FunSpec`s, and `fixture.FlatSpec`s:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * it should behave like nonFullStack(stackWithOneItem)
 *           ^
 * }}}
 *
 * It also enables syntax such as the following syntax in `WordSpec`s and `fixture.WordSpec`s:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * behave like nonEmptyStack(lastValuePushed)
 * ^
 * }}}
 *
 * For more information and examples of the use of <cod>behave`, see the Shared tests section
 * in the main documentation for trait <a href="../FunSpec.html#SharedTests">`FunSpec`</a>,
 * <a href="../FlatSpec.html#SharedTests">`FlatSpec`</a>, or <a href="../WordSpec.html#SharedTests">`WordSpec`</a>.
 * 
 */
final class BehaveWord {

  /**
   * Supports the registration of shared tests.
   *
   * This method enables syntax such as the following in `FunSpec`s, `FlatSpec`s,
   * `fixture.FunSpec`s, and `fixture.FlatSpec`s:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * it should behave like nonFullStack(stackWithOneItem)
   *                  ^
   * }}}
   *
   * It also enables syntax such as the following syntax in `WordSpec`s and `fixture.WordSpec`s:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * behave like nonEmptyStack(lastValuePushed)
   * ^
   * }}}
   *
   * This method just provides syntax sugar intended to make the intent of the code clearer.
   * Because the parameter passed to it is
   * type `Unit`, the expression will be evaluated before being passed, which
   * is sufficient to register the shared tests.
   * For more information and examples of the use of <cod>behave`, see the Shared tests section
   * in the main documentation for trait <a href="../FunSpec.html#SharedTests">`FunSpec`</a>,
   * <a href="../FlatSpec.html#SharedTests">`FlatSpec`</a>, or <a href="../WordSpec.html#SharedTests">`WordSpec`</a>.
   * 
   */
  def like(unit: Unit): Unit = ()
  
  /**
   * Overrides toString to return "behave"
   */
  override def toString: String = "behave"
}
