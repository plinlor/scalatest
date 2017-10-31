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
 * The result of a property match operation such as one performed by a
 * <a href="HavePropertyMatcher.html">`HavePropertyMatcher`</a>,
 * which contains one field that indicates whether the match succeeded (''i.e.'', the 
 * property had its expected value), one field that provides
 * the name of the property, and two fields giving the expected and actual values.
 * `HavePropertyMatchResult`'s type parameter, `P`, specifies the type of the property.
 *
 * For an example of a `HavePropertyMatchResult` in action, see the documentation for
 * <a href="HavePropertyMatcher.html">`HavePropertyMatcher`</a>.
 * 
 *
 * @param matches indicates whether or not the matcher matched (if the property had its expected value, it was a match)
 * @param propertyName the name of the property (of type `P`) that was matched against
 * @param expectedValue the expected value of the property
 * @param actualValue the actual value of the property
 *
 * @author Bill Venners
 */
final case class HavePropertyMatchResult[P](
  val matches: Boolean,
  val propertyName: String,
  val expectedValue: P,
  val actualValue: P
)

/**
 * Companion object for the `HavePropertyMatchResult` case class.
 *
 * @author Bill Venners
 */
object HavePropertyMatchResult
