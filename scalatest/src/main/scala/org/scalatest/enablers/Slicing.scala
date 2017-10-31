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
package org.scalatest.enablers



/**
 * Typeclass that enables for aggregations certain `contain` syntax in the ScalaTest matchers DSL.
 *
 * An `Aggregating[A]` provides access to the "aggregating nature" of type `A` in such
 * a way that relevant `contain` matcher syntax can be used with type `A`. An `A`
 * can be any type of ''aggregation''&#8212;an object that in some way aggregates or brings together other objects. ScalaTest provides
 * implicit implementations for several types out of the box in the
 * <a href="Aggregating$.html">`Aggregating` companion object</a>:
 * 
 * 
 * <ul>
 * <li>`scala.collection.GenTraversable`</li>
 * <li>`String`</li>
 * <li>`Array`</li>
 * <li>`java.util.Collection`</li>
 * <li>`java.util.Map`</li>
 * </ul>
 * 
 * The `contain` syntax enabled by this trait is:
 * 
 * <ul>
 * <li>`result` `should` `contain` `atLeastOneOf` `(1, 2, 3)`</li>
 * <li>`result` `should` `contain` `atMostOneOf` `(1, 2, 3)`</li>
 * <li>`result` `should` `contain` `only` `(1, 2, 3)`</li>
 * <li>`result` `should` `contain` `allOf` `(1, 2, 3)`</li>
 * <li>`result` `should` `contain` `theSameElementsAs` `(List(1, 2, 3))`</li>
 * </ul>
 * 
 * You can enable the `contain` matcher syntax enabled by `Aggregating` on your own
 * type `U` by defining an `Aggregating[U]` for the type and making it available implicitly.
 * 
 *
 * Note, for an explanation of the difference between `Containing` and `Aggregating`, both of which
 * enable `contain` matcher syntax, see the <a href="Containing.html#containingVersusAggregating">Containing
 * versus Aggregating</a> section of the main documentation for trait `Containing`.
 * 
 */
private[scalatest] trait Slicing[-A] {

// TODO: Write tests that a NotAllowedException is thrown when no elements are passed, maybe if only one element is passed, and 
// likely if an object is repeated in the list.
  /**
   * Implements `contain` `atLeastOneOf` syntax for aggregations of type `A`.
   *
   * @param aggregation an aggregation about which an assertion is being made
   * @param eles elements at least one of which should be contained in the passed aggregation
   * @return true if the passed aggregation contains at least one of the passed elements
   */
  def includes(sequence: A, subSequence: A): Boolean

  /**
   * Implements `contain` `theSameElementsAs` syntax for aggregations of type `A`.
   *
   * @param leftAggregation an aggregation about which an assertion is being made
   * @param rightAggregation an aggregation that should contain the same elements as the passed `leftAggregation`
   * @return true if the passed `leftAggregation` contains the same elements as the passed `rightAggregation`
   */
  def startsWith(sequence: A, prefix: A): Boolean

  /**
   * Implements `contain` `only` syntax for aggregations of type `A`.
   *
   * @param aggregation an aggregation about which an assertion is being made
   * @param eles the only elements that should be contained in the passed aggregation
   * @return true if the passed aggregation contains only the passed elements
   */
  def endsWith(sequence: A, suffix: A): Boolean
}

/**
 * Companion object for `Aggregating` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`scala.collection.GenTraversable`</li>
 * <li>`String`</li>
 * <li>`Array`</li>
 * <li>`java.util.Collection`</li>
 * <li>`java.util.Map`</li>
 * </ul>
 */
private[scalatest] object Slicing {

  /**
   * Implicit to support `Aggregating` nature of `String`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of `Char` in the `String`
   * @return `Aggregating[String]` that supports `String` in relevant `contain` syntax
   */
  implicit def slicingNatureOfString: Slicing[String] = 
    new Slicing[String] {
      def includes(string: String, subString: String): Boolean = string.indexOf(subString) >= 0
      def startsWith(string: String, prefix: String): Boolean = string.startsWith(prefix)
      def endsWith(string: String, suffix: String): Boolean = string.endsWith(suffix)
    }
}

