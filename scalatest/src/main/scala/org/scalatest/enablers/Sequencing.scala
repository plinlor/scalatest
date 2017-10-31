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
import org.scalactic.{Equality, Every}
import scala.collection.GenTraversable
import org.scalatest.words.ArrayWrapper
import scala.annotation.tailrec

/**
 * Typeclass that enables for sequencing certain `contain` syntax in the ScalaTest matchers DSL.
 *
 * An `Sequencing[A]` provides access to the "sequenching nature" of type `A` in such
 * a way that relevant `contain` matcher syntax can be used with type `A`. An `A`
 * can be any type of ''sequencing''&#8212;an object that in some way brings together other objects in order.
 * ScalaTest provides implicit implementations for several types out of the box in the
 * <a href="Sequencing$.html">`Sequencing` companion object</a>:
 * 
 *
 * <ul>
 * <li>`scala.collection.GenSeq`</li>
 * <li>`scala.collection.SortedSet`</li>
 * <li>`scala.collection.SortedMap`</li>
 * <li>`Array`</li>
 * <li>`java.util.List`</li>
 * <li>`java.util.SortedSet`</li>
 * <li>`java.util.SortedMap`</li>
 * <li>`String`</li>
 * </ul>
 *
 * The `contain` syntax enabled by this trait is:
 *
 * <ul>
 * <li>`result should contain inOrder (1, 2, 3)`</li>
 * <li>`result should contain inOrderOnly (1, 2, 3)`</li>
 * <li>`result should contain theSameElementsInOrderAs List(1, 2, 3)`</li>
 * </ul>
 *
 * You can enable the `contain` matcher syntax enabled by `Sequencing` on your own
 * type `U` by defining an `Sequencing[U]` for the type and making it available implicitly.
 * 
 */
trait Sequencing[-S] {

  /**
   * Implements `contain` `inOrder` syntax for sequences of type `S`.
   *
   * @param sequence an sequence about which an assertion is being made
   * @param eles elements all of which should be contained, in order of appearance in `eles`, in the passed sequence
   * @return true if the passed sequence contains all of the passed elements in (iteration) order
   */
  def containsInOrder(sequence: S, eles: Seq[Any]): Boolean

  /**
   * Implements `contain` `inOrderOnly` syntax for sequences of type `S`.
   *
   * @param sequence an sequence about which an assertion is being made
   * @param eles the only elements that should be contained, in order of appearence in `eles`, in the passed sequence
   * @return true if the passed sequence contains only the passed elements in (iteration) order
   */
  def containsInOrderOnly(sequence: S, eles: Seq[Any]): Boolean

  /**
   * Implements `contain` `theSameElementsInOrderAs` syntax for sequences of type `S`.
   *
   * @param leftSequence an sequence about which an assertion is being made
   * @param rightSequence an sequence that should contain the same elements, in (iterated) order as the passed `leftSequence`
   * @return true if the passed `leftSequence` contains the same elements, in (iterated) order, as the passed `rightSequence`
   */
  def containsTheSameElementsInOrderAs(leftSequence: S, rightSequence: GenTraversable[Any]): Boolean
}

/**
 * Companion object for `Sequencing` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`scala.collection.GenSeq`</li>
 * <li>`scala.collection.SortedSet`</li>
 * <li>`scala.collection.SortedMap`</li>
 * <li>`Array`</li>
 * <li>`java.util.List`</li>
 * <li>`java.util.SortedSet`</li>
 * <li>`java.util.SortedMap`</li>
 * <li>`String`</li>
 * </ul>
 */
object Sequencing {
  
  private def checkTheSameElementsInOrderAs[T](left: GenTraversable[T], right: GenTraversable[Any], equality: Equality[T]): Boolean = {
    @tailrec
    def checkEqual(left: Iterator[T], right: Iterator[Any]): Boolean = {
      if (left.hasNext && right.hasNext) {
        val nextLeft = left.next
        val nextRight = right.next
        if (!equality.areEqual(nextLeft, nextRight))
          false
        else
          checkEqual(left, right)
      }
      else
        left.isEmpty && right.isEmpty
    }
    checkEqual(left.toIterator, right.toIterator)
  }

  private def checkInOrderOnly[T](left: GenTraversable[T], right: GenTraversable[Any], equality: Equality[T]): Boolean = {
  
    @tailrec
    def checkEqual(left: T, right: Any, leftItr: Iterator[T], rightItr: Iterator[Any]): Boolean = {
      if (equality.areEqual(left, right)) { // The first time in, left must equal right
        // Now need to iterate through the left while it is equal to the right
        @tailrec
        def checkNextLeftAgainstCurrentRight(): Option[T] = { // Returns first left that doesn't match the current right, or None, if all remaining lefts matched current right
          if (leftItr.hasNext) {
            val nextLeft = leftItr.next
            if (equality.areEqual(nextLeft, right))
              checkNextLeftAgainstCurrentRight()
            else
              Some(nextLeft)
          }
          else None // No more lefts
        }
        val nextLeftOption = checkNextLeftAgainstCurrentRight()
        nextLeftOption match {
          case Some(nextLeft) => 
            if (rightItr.hasNext) {
              checkEqual(nextLeft, rightItr.next, leftItr, rightItr)
            }
            else false
          case None => !rightItr.hasNext // No more lefts remaining, so we're good so long as no more rights remaining either.
        }
      }
      else false
    }

    val leftItr: Iterator[T] = left.toIterator
    val rightItr: Iterator[Any] = right.toIterator
    if (leftItr.hasNext && rightItr.hasNext)
      checkEqual(leftItr.next, rightItr.next, leftItr, rightItr)
    else left.isEmpty && right.isEmpty
  }
  
  private def checkInOrder[T](left: GenTraversable[T], right: GenTraversable[Any], equality: Equality[T]): Boolean = {
    @tailrec
    def lastIndexOf(itr: Iterator[T], element: Any, idx: Option[Int], i: Int): Option[Int] = {
      if (itr.hasNext) {
        val next = itr.next
        if (equality.areEqual(next, element))
          lastIndexOf(itr, element, Some(i), i + 1)
        else
          lastIndexOf(itr, element, idx, i + 1)
      }
      else
        idx
    }
  
    @tailrec
    def checkEqual(left: GenTraversable[T], rightItr: Iterator[Any]): Boolean = {
      if (rightItr.hasNext) {
        val nextRight = rightItr.next
        lastIndexOf(left.toIterator, nextRight, None, 0) match {
          case Some(idx) => 
            checkEqual(left.drop(idx).tail, rightItr)
          case None => 
            false // Element not found, let's fail early
        }
      }
      else // No more element in right, left contains all of right.
        true
    }
    checkEqual(left, right.toIterator)
  }

  import scala.language.higherKinds

  /**
   * Implicit to support `Sequencing` nature of `scala.collection.GenSeq`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `scala.collection.GenSeq`
   * @tparam E the type of the element in the `scala.collection.GenSeq`
   * @tparam SEQ any subtype of `scala.collection.GenSeq`
   * @return `Sequencing[SEQ[E]]` that supports `scala.collection.GenSeq` in relevant `contain` syntax
   */
  implicit def sequencingNatureOfGenSeq[E, SEQ[e] <: scala.collection.GenSeq[e]](implicit equality: Equality[E]): Sequencing[SEQ[E]] =
    new Sequencing[SEQ[E]] {

      def containsInOrder(seq: SEQ[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrder(seq, elements, equality)
      }

      def containsInOrderOnly(seq: SEQ[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrderOnly[E](seq, elements, equality)
      }

// TODO: Make elements a Sequencing
      def containsTheSameElementsInOrderAs(seq: SEQ[E], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsInOrderAs[E](seq, elements, equality)
      }
    }

  import scala.language.implicitConversions

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Sequencing` of type `SEQ[E]`, where `SEQ` is a subtype of `scala.collection.GenSeq`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (List("hi", "he") should contain inOrderOnly ("HI", "HE")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Sequencing[List[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `scala.collection.GenSeq`
   * @tparam SEQ subtype of `scala.collection.GenSeq`
   * @return `Sequencing` of type `SEQ[E]`
   */
  implicit def convertEqualityToGenSeqSequencing[E, SEQ[e] <: scala.collection.GenSeq[e]](equality: Equality[E]): Sequencing[SEQ[E]] = 
    sequencingNatureOfGenSeq(equality)

  /**
   * Implicit to support `Sequencing` nature of `scala.collection.SortedSet`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `scala.collection.SortedSet`
   * @tparam E the type of the element in the `scala.collection.SortedSet`
   * @tparam SET any subtype of `scala.collection.SortedSet`
   * @return `Sequencing[SET[E]]` that supports `scala.collection.SortedSet` in relevant `contain` syntax
   */
  implicit def sequencingNatureOfSortedSet[E, SET[e] <: scala.collection.SortedSet[e]](implicit equality: Equality[E]): Sequencing[SET[E]] =
    new Sequencing[SET[E]] {

      def containsInOrder(set: SET[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrder(set, elements, equality)
      }

      def containsInOrderOnly(set: SET[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrderOnly[E](set, elements, equality)
      }

      def containsTheSameElementsInOrderAs(set: SET[E], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsInOrderAs[E](set, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Sequencing` of type `SET[E]`, where `SET` is a subtype of `scala.collection.SortedSet`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (SortedSet("hi", "he") should contain inOrderOnly ("HI", "HE")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Sequencing[SortedSet[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `scala.collection.SortedSet`
   * @tparam SET subtype of `scala.collection.SortedSet`
   * @return `Sequencing` of type `SET[E]`
   */
  implicit def convertEqualityToSortedSetSequencing[E, SET[e] <: scala.collection.SortedSet[e]](equality: Equality[E]): Sequencing[SET[E]] = 
    sequencingNatureOfSortedSet(equality)

  /**
   * Implicit to support `Sequencing` nature of `scala.collection.SortedMap`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `scala.collection.SortedMap`
   * @tparam K the type of the key in the `scala.collection.SortedMap`
   * @tparam V the type of the value in the `scala.collection.SortedMap`
   * @tparam MAP any subtype of `scala.collection.SortedMap`
   * @return `Sequencing[MAP[K, V]]` that supports `scala.collection.SortedMap` in relevant `contain` syntax
   */
  implicit def sequencingNatureOfSortedMap[K, V, MAP[k, v] <: scala.collection.SortedMap[k, v]](implicit equality: Equality[(K, V)]): Sequencing[MAP[K, V]] =
    new Sequencing[MAP[K, V]] {

      def containsInOrder(map: MAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrder(map, elements, equality)
      }

      def containsInOrderOnly(map: MAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrderOnly(map, elements, equality)
      }

      def containsTheSameElementsInOrderAs(map: MAP[K, V], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsInOrderAs(map, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `(K, V)`
   * into `Sequencing` of type `MAP[K, V]`, where `MAP` is a subtype of `scala.collection.SortedMap`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * // lowerCased needs to be implemented as Normalization[(K, V)]
   * (SortedMap("hi" -> "hi", "he" -> "he") should contain inOrderOnly ("HI" -> "HI", "HE" -> "HE")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Sequencing[SortedMap[String, String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `(K, V)`
   * @tparam K the type of the key in the `scala.collection.SortedMap`
   * @tparam V the type of the value in the `scala.collection.SortedMap`
   * @tparam MAP subtype of `scala.collection.SortedMap`
   * @return `Sequencing` of type `MAP[K, V]`
   */
  implicit def convertEqualityToSortedMapSequencing[K, V, MAP[k, v] <: scala.collection.SortedMap[k, v]](equality: Equality[(K, V)]): Sequencing[MAP[K, V]] = 
    sequencingNatureOfSortedMap(equality)

  /**
   * Implicit to support `Sequencing` nature of `Array`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `Array`
   * @tparam E the type of the element in the `Array`
   * @return `Sequencing[Array[E]]` that supports `Array` in relevant `contain` syntax
   */
  implicit def sequencingNatureOfArray[E](implicit equality: Equality[E]): Sequencing[Array[E]] = 
    new Sequencing[Array[E]] {

      def containsInOrder(array: Array[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrder(new ArrayWrapper(array), elements, equality)
      }

      def containsInOrderOnly(array: Array[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrderOnly(new ArrayWrapper(array), elements, equality)
      }

      def containsTheSameElementsInOrderAs(array: Array[E], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsInOrderAs[E](new ArrayWrapper(array), elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Sequencing` of type `Array[E]`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (Array("hi", "he") should contain inOrderOnly ("HI", "HE")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Sequencing[Array[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `Array`
   * @return `Sequencing` of type `Array[E]`
   */
  implicit def convertEqualityToArraySequencing[E](equality: Equality[E]): Sequencing[Array[E]] = 
    sequencingNatureOfArray(equality)

  /**
   * Implicit to support `Sequencing` nature of `java.util.List`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `java.util.List`
   * @tparam E the type of the element in the `java.util.List`
   * @tparam JLIST any subtype of `java.util.List`
   * @return `Sequencing[JLIST[E]]` that supports `java.util.List` in relevant `contain` syntax
   */
  implicit def sequencingNatureOfJavaList[E, JLIST[e] <: java.util.List[e]](implicit equality: Equality[E]): Sequencing[JLIST[E]] = 
    new Sequencing[JLIST[E]] {

      def containsInOrder(col: JLIST[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrder(col.asScala, elements, equality)
      }

      def containsInOrderOnly(col: JLIST[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrderOnly(col.asScala, elements, equality)
      }

      def containsTheSameElementsInOrderAs(col: JLIST[E], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsInOrderAs(col.asScala, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Sequencing` of type `JLIST[E]`, where `JLIST` is a subtype of `java.util.List`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * val javaList = new java.util.ArrayList[String]()
   * javaList.add("hi", "he")
   * (javaList should contain ("HI", "HE")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Sequencing[java.util.ArrayList[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `java.util.List`
   * @tparam JLIST subtype of `java.util.List`
   * @return `Sequencing` of type `JLIST[E]`
   */
  implicit def convertEqualityToJavaListSequencing[E, JLIST[e] <: java.util.List[e]](equality: Equality[E]): Sequencing[JLIST[E]] = 
    sequencingNatureOfJavaList(equality)

  /**
   * Implicit to support `Sequencing` nature of `java.util.SortedSet`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `java.util.SortedSet`
   * @tparam E the type of the element in the `java.util.SortedSet`
   * @tparam JSET any subtype of `java.util.SortedSet`
   * @return `Sequencing[JSET[E]]` that supports `java.util.SortedSet` in relevant `contain` syntax
   */
  implicit def sequencingNatureOfJavaSortedSet[E, JSET[e] <: java.util.SortedSet[e]](implicit equality: Equality[E]): Sequencing[JSET[E]] =
    new Sequencing[JSET[E]] {

      def containsInOrder(set: JSET[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrder(set.iterator.asScala.toVector, elements, equality)
      }

      def containsInOrderOnly(set: JSET[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrderOnly[E](set.iterator.asScala.toVector, elements, equality)
      }

      def containsTheSameElementsInOrderAs(set: JSET[E], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsInOrderAs[E](set.iterator.asScala.toVector, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Sequencing` of type `JSET[E]`, where `JSET` is a subtype of `java.util.SortedSet`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * val javaSet = new java.util.TreeSet[String]()
   * javaSet.add("hi", "he")
   * (javaSet should contain inOrderOnly ("HI", "HE")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Sequencing[java.util.TreeSet[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `java.util.List`
   * @tparam JSET subtype of `java.util.List`
   * @return `Sequencing` of type `JLIST[E]`
   */
  implicit def convertEqualityToJavaSortedSetSequencing[E, JSET[e] <: java.util.SortedSet[e]](equality: Equality[E]): Sequencing[JSET[E]] = 
    sequencingNatureOfJavaSortedSet(equality)

  /**
   * Implicit to support `Sequencing` nature of `java.util.SortedMap`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of entry in the `java.util.SortedMap`
   * @tparam K the type of the key in the `java.util.SortedMap`
   * @tparam V the type of the value in the `java.util.SortedMap`
   * @tparam JMAP any subtype of `java.util.SortedMap`
   * @return `Sequencing[JMAP[K, V]]` that supports `java.util.SortedMap` in relevant `contain` syntax
   */
  implicit def sequencingNatureOfJavaSortedMap[K, V, JMAP[k, v] <: java.util.SortedMap[k, v]](implicit equality: Equality[java.util.Map.Entry[K, V]]): Sequencing[JMAP[K, V]] =
    new Sequencing[JMAP[K, V]] {

      def containsInOrder(map: JMAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrder(map.entrySet.iterator.asScala.toVector, elements, equality)
      }

      def containsInOrderOnly(map: JMAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrderOnly(map.entrySet.iterator.asScala.toVector, elements, equality)
      }

      def containsTheSameElementsInOrderAs(map: JMAP[K, V], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsInOrderAs(map.entrySet.iterator.asScala.toVector, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `java.util.Map.Entry[K, V]`
   * into `Sequencing` of type `JMAP[K, V]`, where `JMAP` is a subtype of `java.util.SortedMap`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * val javaMap = new java.util.TreeMap[Int, String]()
   * javaMap.put(1, "one")
   * // lowerCased needs to be implemented as Normalization[java.util.Map.Entry[K, V]]
   * (javaMap should contain inOrderOnly (Entry(1, "ONE"))) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`java.util.Map.Entry[Int, String]`</a>
   * and this implicit conversion will convert it into `Aggregating[java.util.TreeMap[Int, String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `java.util.Map.Entry[K, V]`
   * @tparam K the type of the key in the `java.util.SortedMap`
   * @tparam V the type of the value in the `java.util.SortedMap`
   * @tparam JMAP subtype of `java.util.SortedMap`
   * @return `Sequencing` of type `JMAP[K, V]`
   */
  implicit def convertEqualityToJavaSortedMapSequencing[K, V, JMAP[k, v] <: java.util.SortedMap[k, v]](equality: Equality[java.util.Map.Entry[K, V]]): Sequencing[JMAP[K, V]] = 
    sequencingNatureOfJavaSortedMap(equality)

  /**
   * Implicit to support `Sequencing` nature of `String`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of `Char` in the `String`
   * @return `Sequencing[String]` that supports `String` in relevant `contain` syntax
   */
  implicit def sequencingNatureOfString(implicit equality: Equality[Char]): Sequencing[String] = 
    new Sequencing[String] {

      def containsInOrder(s: String, elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrder(s, elements, equality)
      }

      def containsInOrderOnly(s: String, elements: scala.collection.Seq[Any]): Boolean = {
        checkInOrderOnly(s, elements, equality)
      }

      def containsTheSameElementsInOrderAs(s: String, elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsInOrderAs(s, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `Char`
   * into `Sequencing` of type `String`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * // lowerCased needs to be implemented as Normalization[Char]
   * ("hi hello" should contain inOrderOnly ('E')) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[Char]`</a>
   * and this implicit conversion will convert it into `Sequencing[String]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `Char`
   * @return `Sequencing` of type `String`
   */
  implicit def convertEqualityToStringSequencing(equality: Equality[Char]): Sequencing[String] = 
    sequencingNatureOfString(equality)

  /**
   * Implicit to support `Sequencing` nature of `Every`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `Every`
   * @tparam E the type of the element in the `Every`
   * @return `Sequencing[Every[E]]` that supports `Every` in relevant `contain` syntax
   */
  implicit def sequencingNatureOfEvery[E](implicit equality: Equality[E]): Sequencing[Every[E]] =
    new Sequencing[Every[E]] {

      def containsInOrder(every: Every[E], elements: scala.collection.Seq[Any]): Boolean =
        checkInOrder(every, elements, equality)

      def containsInOrderOnly(every: Every[E], elements: scala.collection.Seq[Any]): Boolean =
        checkInOrderOnly(every, elements, equality)

      def containsTheSameElementsInOrderAs(every: Every[E], elements: GenTraversable[Any]): Boolean =
        checkTheSameElementsInOrderAs[E](every, elements, equality)
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Sequencing` of type `Every[E]`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (Every("hi", "he") should contain inOrderOnly ("HI", "HE")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Sequencing[Every[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `Every`
   * @return `Sequencing` of type `Every[E]`
   */
  implicit def convertEqualityToEverySequencing[E](equality: Equality[E]): Sequencing[Every[E]] =
    sequencingNatureOfEvery(equality)
    
}
