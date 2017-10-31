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

import scala.collection.JavaConverters._
import Aggregating.tryEquality
import org.scalactic.Equality
import org.scalatest.FailureMessages
import scala.annotation.tailrec
import scala.collection.GenTraversable

/**
 * Supertrait for typeclasses that enable the `be` `sorted` matcher syntax.
 *
 * A `Sortable[S]` provides access to the "sortable nature" of type `S` in such
 * a way that `be` `sorted` matcher syntax can be used with type `S`. An `S`
 * can be any type for which the concept of being sorted makes sense, such as sequences. ScalaTest provides
 * implicit implementations for several types. You can enable the `be` `sorted` matcher syntax on your own
 * type `U` by defining a `Sortable[U]` for the type and making it available implicitly.
 * 
 * ScalaTest provides an implicit `Sortable` instance for types out of the box
 * in the <a href="Sortable$.html">`Sortable` companion object</a>:
 * 
 *
 * <ul>
 * <li>`scala.collection.GenSeq`</li>
 * <li>`Array`</li>
 * <li>`java.util.List`</li>
 * </ul>
 *
 */
trait Sortable[-S] {

  /**
   * Determines whether the passed sequence is sorted, ''i.e.'', the elements of the passed sequence are in sorted order.
   *
   * @param sequence the sequence to check whether it is sorted
   * @return `true` if passed `sequence` is sorted, `false` otherwise.
   */
  def isSorted(sequence: S): Boolean
}

/**
 * Companion object for `Sortable` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`scala.collection.GenSeq`</li>
 * <li>`Array`</li>
 * <li>`java.util.List`</li>
 * </ul>
 */
object Sortable {

  import scala.language.higherKinds

// Sliding doesn't exist on GenSeq, and this is inherently sequential, so make them say .seq if they have a parallel Seq
// Actually on second thought, I think just do a .seq on it.
  /**
   * Enable `Sortable` implementation for `scala.collection.GenSeq`
   *
   * @param ordering `scala.math.Ordering`</a> of type `E`
   * @tparam E type of elements in the `scala.collection.GenSeq`
   * @tparam SEQ any subtype of `scala.collection.GenSeq`
   * @return `Sortable[SEQ[E]]` that supports `scala.collection.GenSeq` in `be` `sortable` syntax
   */
  implicit def sortableNatureOfSeq[E, SEQ[e] <: scala.collection.GenSeq[e]](implicit ordering: Ordering[E]): Sortable[SEQ[E]] =
    new Sortable[SEQ[E]] {
      def isSorted(o: SEQ[E]): Boolean =
        if (o.size > 1)
          o.seq.sliding(2).forall { duo => ordering.lteq(duo(0), duo(1)) }
        else
          true
    }

  /**
   * Enable `Sortable` implementation for `Array`
   *
   * @param ordering `scala.math.Ordering`</a> of type `E`
   * @tparam E type of elements in the `Array`
   * @return `Sortable[Array[E]]` that supports `Array` in `be` `sortable` syntax
   */
  implicit def sortableNatureOfArray[E](implicit ordering: Ordering[E]): Sortable[Array[E]] = 
    new Sortable[Array[E]] {
      def isSorted(o: Array[E]): Boolean =
        if (o.length > 1)
          o.sliding(2).forall { duo => ordering.lteq(duo(0), duo(1)) }
        else
          true
    }

  /**
   * Enable `Sortable` implementation for `String`
   *
   * @param ordering `scala.math.Ordering`</a> of type `Char`
   * @return `Sortable[String]` that supports `String` in `be` `sortable` syntax
   */
  implicit def sortableNatureOfString(implicit ordering: Ordering[Char]): Sortable[String] = 
    new Sortable[String] {
      def isSorted(o: String): Boolean =
        if (o.length > 1)
          o.sliding(2).forall { duo => ordering.lteq(duo(0), duo(1)) }
        else
          true
    }

  /**
   * Enable `Sortable` implementation for `java.util.List`
   *
   * @param ordering `scala.math.Ordering`</a> of type `E`
   * @tparam E type of elements in the `java.util.List`
   * @tparam JLIST any subtype of `java.util.List`
   * @return `Sortable[JLIST[E]]` that supports `java.util.List` in `be` `sortable` syntax
   */
  implicit def sortableNatureOfJavaList[E, JLIST[e] <: java.util.List[e]](implicit ordering: Ordering[E]): Sortable[JLIST[E]] = 
    new Sortable[JLIST[E]] {
      def isSorted(o: JLIST[E]): Boolean =
        if (o.size > 1)
          o.asScala.sliding(2).forall { duo => ordering.lteq(duo(0), duo(1)) }
        else
          true
    }
}

