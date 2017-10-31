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
package org.scalatest.tagobjects

import org.scalatest.Tag

/**
 * Tag object that indicates a test is disk-intensive (''i.e.'', consumes a lot of disk-IO bandwidth when it runs).
 *
 * The corresponding tag annotation for this tag object is `org.scalatest.tags.Disk`.
 * This tag object can be used to tag test functions (in style traits other than `Spec`, in which tests are methods not functions) as being disk-intensive.
 * See the "tagging tests" section in the documentation for your chosen styles to see the syntax. Here's an example for `FlatSpec`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.tagobjects.disk
 * 
 * import org.scalatest._
 * import tagobjects.Disk
 * 
 * class SetSpec extends FlatSpec {
 * 
 *   "An empty Set" should "have size 0" taggedAs(Disk) in {
 *     assert(Set.empty.size === 0)
 *   }
 * }
 * }}}
 */
object Disk extends Tag("org.scalatest.tags.Disk") 
