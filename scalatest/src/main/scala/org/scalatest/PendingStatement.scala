/*
 * Copyright 2001-2015 Artima, Inc.
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

/**
 * Trait mixed into the result type of the `pending` statement of trait `Assertions`, which always throws `TestPendingException`. 
 *
 * This type is used primarily to facilitate the `is (pending)` syntax of
 * traits <a href="FlatSpec.html">`FlatSpec`</a>, <a href="WordSpec.html">`WordSpec`</a>, and
 * <a href="FlatSpec.html">`FLatSpec`</a> as well the
 * `is (pending)` or `(pending)` syntax of sibling traits
 * in the `org.scalatest.fixture` package. Because the `pending`
 * method in `Assertions` always completes abruptly with an exception, its
 * type would be inferred to be `Nothing`, which is a relatively common
 * type. To make sure syntax like `is (pending)` only works with
 * method `pending`, it is helpful to have a specially named
 * "`Nothing`" type.
 * 
 */
sealed trait PendingStatement

