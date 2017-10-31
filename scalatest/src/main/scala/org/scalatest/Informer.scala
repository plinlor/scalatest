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

import org.scalactic.source

/**
 * Trait to which custom information about a running suite of tests can be reported.
 * 
 * An `Informer` is essentially
 * used to wrap a `Reporter` and provide easy ways to send custom information
 * to that `Reporter` via an `InfoProvided` event.
 * `Informer` contains an `apply` method that takes a string and
 * an optional payload object of type `Any`.
 * The `Informer` will forward the passed `message` string to the
 * <a href="Reporter.html">`Reporter`</a> as the `message` parameter, and the optional
 * payload object as the `payload` parameter, of an <a href="InfoProvided.html">`InfoProvided`</a> event.
 * 
 *
 * Here's an example in which the `Informer` is used both directly via `info`
 * method of trait <a href="FlatSpec.html">`FlatSpec`</a> and indirectly via the methods of
 * trait <a href="GivenWhenThen.html">`GivenWhenThen`</a>:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.flatspec.info
 * 
 * import collection.mutable
 * import org.scalatest._
 * 
 * class SetSpec extends FlatSpec with GivenWhenThen {
 *   
 *   "A mutable Set" should "allow an element to be added" in {
 *     given("an empty mutable Set")
 *     val set = mutable.Set.empty[String]
 * 
 *     when("an element is added")
 *     set += "clarity"
 * 
 *     then("the Set should have size 1")
 *     assert(set.size === 1)
 * 
 *     and("the Set should contain the added element")
 *     assert(set.contains("clarity"))
 * 
 *     info("That's all folks!")
 *   }
 * }
 * }}}
 *
 * If you run this `SetSpec` from the interpreter, you will see the following output:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; org.scalatest.run(new SetSpec)
 * <span class="stGreen">A mutable Set
 * - should allow an element to be added
 *   + Given an empty mutable Set 
 *   + When an element is added 
 *   + Then the Set should have size 1 
 *   + And the Set should contain the added element 
 *   + That's all folks! </span>
 * }}}
 *
 * @author Bill Venners
 */
trait Informer {
       // TODO: Make sure all the informer implementations check for null
  /**
   * Provide information and optionally, a payload, to the `Reporter` via an
   * `InfoProvided` event.
   *
   * @param message a string that will be forwarded to the wrapped `Reporter`
   *   via an `InfoProvided` event.
   * @param payload an optional object which will be forwarded to the wrapped `Reporter`
   *   as a payload via an `InfoProvided` event.
   *
   * @throws NullArgumentException if `message` or `payload` reference is `null`
   */
  def apply(message: String, payload: Option[Any] = None)(implicit pos: source.Position): Unit
}
