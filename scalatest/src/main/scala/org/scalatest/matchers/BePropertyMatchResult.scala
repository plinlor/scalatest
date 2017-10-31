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
package org.scalatest.matchers

/**
 * The result of a `Boolean` property match operation, such as one performed by a
 * <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>,
 * which contains one field that indicates whether the match succeeded (''i.e.'', the `Boolean`
 * property was `true`) and one field that provides
 * the name of the property.
 *
 * For an example of a `BePropertyMatchResult` in action, see the documentation for
 * <a href="BePropertyMatcher.html">`BePropertyMatcher`</a>.
 * 
 *
 * @param matches indicates whether or not the matcher matched (if the `Boolean` property was true, it was a match)
 * @param propertyName the name of the `Boolean` property that was matched against
 *
 * @author Bill Venners
 */
final case class BePropertyMatchResult(
  val matches: Boolean, // true if the Boolean property was true
  val propertyName: String
)

/**
 * Companion object for the `BePropertyMatchResult` case class.
 *
 * @author Bill Venners
 */
object BePropertyMatchResult
