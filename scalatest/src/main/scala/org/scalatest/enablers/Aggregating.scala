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
import org.scalatest.FailureMessages
import org.scalatest.words.ArrayWrapper
import scala.annotation.tailrec

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
trait Aggregating[-A] {

// TODO: Write tests that a NotAllowedException is thrown when no elements are passed, maybe if only one element is passed, and 
// likely if an object is repeated in the list.
  /**
   * Implements `contain` `atLeastOneOf` syntax for aggregations of type `A`.
   *
   * @param aggregation an aggregation about which an assertion is being made
   * @param eles elements at least one of which should be contained in the passed aggregation
   * @return true if the passed aggregation contains at least one of the passed elements
   */
  def containsAtLeastOneOf(aggregation: A, eles: Seq[Any]): Boolean

  /**
   * Implements `contain` `theSameElementsAs` syntax for aggregations of type `A`.
   *
   * @param leftAggregation an aggregation about which an assertion is being made
   * @param rightAggregation an aggregation that should contain the same elements as the passed `leftAggregation`
   * @return true if the passed `leftAggregation` contains the same elements as the passed `rightAggregation`
   */
  def containsTheSameElementsAs(leftAggregation: A, rightAggregation: GenTraversable[Any]): Boolean

  /**
   * Implements `contain` `only` syntax for aggregations of type `A`.
   *
   * @param aggregation an aggregation about which an assertion is being made
   * @param eles the only elements that should be contained in the passed aggregation
   * @return true if the passed aggregation contains only the passed elements
   */
  def containsOnly(aggregation: A, eles: Seq[Any]): Boolean

  /**
   * Implements `contain` `allOf` syntax for aggregations of type `A`.
   *
   * @param aggregation an aggregation about which an assertion is being made
   * @param eles elements all of which should be contained in the passed aggregation
   * @return true if the passed aggregation contains all of the passed elements
   */
  def containsAllOf(aggregation: A, eles: Seq[Any]): Boolean

  /**
   * Implements `contain` `atMostOneOf` syntax for aggregations of type `A`.
   *
   * @param aggregation an aggregation about which an assertion is being made
   * @param eles elements at most one of which should be contained in the passed aggregation
   * @return true if the passed aggregation contains at most one of the passed elements
   */
  def containsAtMostOneOf(aggregation: A, eles: Seq[Any]): Boolean
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
object Aggregating {

  // TODO: Throwing exceptions is slow. Just do a pattern match and test the type before trying to cast it.
  private[scalatest] def tryEquality[T](left: Any, right: Any, equality: Equality[T]): Boolean = 
    try equality.areEqual(left.asInstanceOf[T], right)
      catch {
        case cce: ClassCastException => false
    }
  
  private[scalatest] def checkTheSameElementsAs[T](left: GenTraversable[T], right: GenTraversable[Any], equality: Equality[T]): Boolean = {
    case class ElementCount(element: Any, leftCount: Int, rightCount: Int)
    object ZipNoMatch
    
    def leftNewCount(next: Any, count: IndexedSeq[ElementCount]): IndexedSeq[ElementCount] = {
      val idx = count.indexWhere(ec => tryEquality(next, ec.element, equality))
      if (idx >= 0) {
        val currentElementCount = count(idx)
        count.updated(idx, ElementCount(currentElementCount.element, currentElementCount.leftCount + 1, currentElementCount.rightCount))
      }
      else
        count :+ ElementCount(next, 1, 0)
    }
    
    def rightNewCount(next: Any, count: IndexedSeq[ElementCount]): IndexedSeq[ElementCount] = {
      val idx = count.indexWhere(ec => tryEquality(next, ec.element, equality))
      if (idx >= 0) {
        val currentElementCount = count(idx)
        count.updated(idx, ElementCount(currentElementCount.element, currentElementCount.leftCount, currentElementCount.rightCount + 1))
      }
      else
        count :+ ElementCount(next, 0, 1)
    }
    
    val counts = right.toIterable.zipAll(left.toIterable, ZipNoMatch, ZipNoMatch).aggregate(IndexedSeq.empty[ElementCount])( 
      { case (count, (nextLeft, nextRight)) => 
          if (nextLeft == ZipNoMatch || nextRight == ZipNoMatch)
            return false  // size not match, can fail early
          rightNewCount(nextRight, leftNewCount(nextLeft, count))
      }, 
      { case (count1, count2) =>
          count2.foldLeft(count1) { case (count, next) => 
            val idx = count.indexWhere(ec => tryEquality(next.element, ec.element, equality))
            if (idx >= 0) {
              val currentElementCount = count(idx)
              count.updated(idx, ElementCount(currentElementCount.element, currentElementCount.leftCount + next.leftCount, currentElementCount.rightCount + next.rightCount))
            }
            else
              count :+ next
          }
      }
    )
    
    !counts.exists(e => e.leftCount != e.rightCount)
  }
  
  private[scalatest] def checkOnly[T](left: GenTraversable[T], right: GenTraversable[Any], equality: Equality[T]): Boolean =
    left.forall(l => right.find(r => tryEquality(l, r, equality)).isDefined) &&
    right.forall(r => left.find(l => tryEquality(l, r, equality)).isDefined)
  
  private[scalatest] def checkAllOf[T](left: GenTraversable[T], right: GenTraversable[Any], equality: Equality[T]): Boolean = {
    @tailrec
    def checkEqual(left: GenTraversable[T], rightItr: Iterator[Any]): Boolean = {
      if (rightItr.hasNext) {
        val nextRight = rightItr.next
        if (left.exists(t => equality.areEqual(t, nextRight))) 
          checkEqual(left, rightItr)
        else
          false // Element not found, let's fail early
      }
      else // No more element in right, left contains all of right.
        true
    }
    checkEqual(left, right.toIterator)
  }
  
  private[scalatest] def checkAtMostOneOf[T](left: GenTraversable[T], right: GenTraversable[Any], equality: Equality[T]): Boolean = {
    
    def countElements: Int = 
      right.aggregate(0)(
        { case (count, nextRight) => 
            if (left.exists(l => equality.areEqual(l, nextRight))) {
              val newCount = count + 1
              if (newCount > 1)
                return newCount
              else
                newCount
            }
            else
              count
        }, 
        { case (count1, count2) => count1 + count2 }
      )
    val count = countElements
    count <= 1      
  }

  import scala.language.higherKinds

  /**
   * Implicit to support `Aggregating` nature of `GenTraversable`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `GenTraversable`
   * @tparam E the type of the element in the `GenTraversable`
   * @tparam TRAV any subtype of `GenTraversable`
   * @return `Aggregating[TRAV[E]]` that supports `GenTraversable` in relevant `contain` syntax
   */
  implicit def aggregatingNatureOfGenTraversable[E, TRAV[e] <: scala.collection.GenTraversable[e]](implicit equality: Equality[E]): Aggregating[TRAV[E]] = 
    new Aggregating[TRAV[E]] {
      def containsAtLeastOneOf(trav: TRAV[E], elements: scala.collection.Seq[Any]): Boolean = {
        trav.exists((e: E) => elements.exists((ele: Any) => equality.areEqual(e, ele)))
      }
      def containsTheSameElementsAs(trav: TRAV[E], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsAs[E](trav, elements, equality)
      }
      def containsOnly(trav: TRAV[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkOnly[E](trav, elements, equality)
      }
      def containsAllOf(trav: TRAV[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkAllOf(trav, elements, equality)
      }
      def containsAtMostOneOf(trav: TRAV[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkAtMostOneOf(trav, elements, equality)
      }
    }

  import scala.language.implicitConversions

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Aggregating` of type `TRAV[E]`, where `TRAV` is a subtype of `GenTraversable`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (List("hi") should contain ("HI")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Aggregating[List[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `GenTraversable`
   * @tparam TRAV subtype of `GenTraversable`
   * @return `Aggregating` of type `TRAV[E]`
   */
  implicit def convertEqualityToGenTraversableAggregating[E, TRAV[e] <: scala.collection.GenTraversable[e]](equality: Equality[E]): Aggregating[TRAV[E]] =
    aggregatingNatureOfGenTraversable(equality)

  /**
   * Implicit to support `Aggregating` nature of `Array`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `Array`
   * @tparam E the type of the element in the `Array`
   * @return `Aggregating[Array[E]]` that supports `Array` in relevant `contain` syntax
   */
  implicit def aggregatingNatureOfArray[E](implicit equality: Equality[E]): Aggregating[Array[E]] = 
    new Aggregating[Array[E]] {
      def containsAtLeastOneOf(array: Array[E], elements: scala.collection.Seq[Any]): Boolean = {
        new ArrayWrapper(array).exists((e: E) => elements.exists((ele: Any) => equality.areEqual(e, ele)))
      }
      def containsTheSameElementsAs(array: Array[E], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsAs[E](new ArrayWrapper(array), elements, equality)
      }
      def containsOnly(array: Array[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkOnly(new ArrayWrapper(array), elements, equality)
      }
      def containsAllOf(array: Array[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkAllOf(new ArrayWrapper(array), elements, equality)
      }
      def containsAtMostOneOf(array: Array[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkAtMostOneOf(new ArrayWrapper(array), elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Aggregating` of type `Array[E]`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (Array("hi") should contain ("HI")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Aggregating[Array[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `Array`
   * @return `Aggregating` of type `Array[E]`
   */
  implicit def convertEqualityToArrayAggregating[E](equality: Equality[E]): Aggregating[Array[E]] = 
    aggregatingNatureOfArray(equality)

  /**
   * Implicit to support `Aggregating` nature of `String`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of `Char` in the `String`
   * @return `Aggregating[String]` that supports `String` in relevant `contain` syntax
   */
  implicit def aggregatingNatureOfString(implicit equality: Equality[Char]): Aggregating[String] = 
    new Aggregating[String] {
      def containsAtLeastOneOf(s: String, elements: scala.collection.Seq[Any]): Boolean = {
        s.exists((e: Char) => elements.exists((ele: Any) => equality.areEqual(e, ele)))
      }
      def containsTheSameElementsAs(s: String, elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsAs(s, elements, equality)
      }
      def containsOnly(s: String, elements: scala.collection.Seq[Any]): Boolean = {
        checkOnly(s, elements, equality)
      }
      def containsAllOf(s: String, elements: scala.collection.Seq[Any]): Boolean = {
        checkAllOf(s, elements, equality)
      }
      def containsAtMostOneOf(s: String, elements: scala.collection.Seq[Any]): Boolean = {
        checkAtMostOneOf(s, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `Char`
   * into `Aggregating` of type `String`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * // lowerCased needs to be implemented as Normalization[Char]
   * ("hi hello" should contain ('E')) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[Char]`</a>
   * and this implicit conversion will convert it into `Aggregating[String]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `Char`
   * @return `Aggregating` of type `String`
   */
  implicit def convertEqualityToStringAggregating(equality: Equality[Char]): Aggregating[String] =
    aggregatingNatureOfString(equality)

  /**
   * Implicit to support `Aggregating` nature of `java.util.Collection`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `java.util.Collection`
   * @tparam E the type of the element in the `java.util.Collection`
   * @tparam JCOL any subtype of `java.util.Collection`
   * @return `Aggregating[JCOL[E]]` that supports `java.util.Collection` in relevant `contain` syntax
   */
  implicit def aggregatingNatureOfJavaCollection[E, JCOL[e] <: java.util.Collection[e]](implicit equality: Equality[E]): Aggregating[JCOL[E]] = 
    new Aggregating[JCOL[E]] {
      def containsAtLeastOneOf(col: JCOL[E], elements: scala.collection.Seq[Any]): Boolean = {
        col.asScala.exists((e: E) => elements.exists((ele: Any) => equality.areEqual(e, ele)))
      }
      def containsTheSameElementsAs(col: JCOL[E], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsAs(col.asScala, elements, equality)
      }
      def containsOnly(col: JCOL[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkOnly(col.asScala, elements, equality)
      }
      def containsAllOf(col: JCOL[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkAllOf(col.asScala, elements, equality)
      }
      def containsAtMostOneOf(col: JCOL[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkAtMostOneOf(col.asScala, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Aggregating` of type `JCOL[E]`, where `JCOL` is a subtype of `java.util.Collection`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * val javaList = new java.util.ArrayList[String]()
   * javaList.add("hi")
   * (javaList should contain ("HI")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Aggregating[java.util.ArrayList[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `java.util.Collection`
   * @tparam JCOL subtype of `java.util.Collection`
   * @return `Aggregating` of type `JCOL[E]`
   */
  implicit def convertEqualityToJavaCollectionAggregating[E, JCOL[e] <: java.util.Collection[e]](equality: Equality[E]): Aggregating[JCOL[E]] = 
    aggregatingNatureOfJavaCollection(equality)

  /**
   * Implicit to support `Aggregating` nature of `java.util.Map`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of entry in the `java.util.Map`
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `Aggregating[JMAP[K, V]]` that supports `java.util.Map` in relevant `contain` syntax
   */
  implicit def aggregatingNatureOfJavaMap[K, V, JMAP[k, v] <: java.util.Map[k, v]](implicit equality: Equality[java.util.Map.Entry[K, V]]): Aggregating[JMAP[K, V]] = 
    new Aggregating[JMAP[K, V]] {
    
      import scala.collection.JavaConverters._
      def containsAtLeastOneOf(map: JMAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        map.entrySet.asScala.exists((e: java.util.Map.Entry[K, V]) => elements.exists((ele: Any) => equality.areEqual(e, ele)))
      }
      def containsTheSameElementsAs(map: JMAP[K, V], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsAs(map.entrySet.asScala, elements, equality)
      }
      def containsOnly(map: JMAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        checkOnly(map.entrySet.asScala, elements, equality)
      }
      def containsAllOf(map: JMAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        checkAllOf(map.entrySet.asScala, elements, equality)
      }
      def containsAtMostOneOf(map: JMAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        checkAtMostOneOf(map.entrySet.asScala, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `java.util.Map.Entry[K, V]`
   * into `Aggregating` of type `JMAP[K, V]`, where `JMAP` is a subtype of `java.util.Map`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * val javaMap = new java.util.HashMap[Int, String]()
   * javaMap.put(1, "one")
   * // lowerCased needs to be implemented as Normalization[java.util.Map.Entry[K, V]]
   * (javaMap should contain (Entry(1, "ONE"))) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`java.util.Map.Entry[Int, String]`</a>
   * and this implicit conversion will convert it into `Aggregating[java.util.HashMap[Int, String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `java.util.Map.Entry[K, V]`
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `Aggregating` of type `JMAP[K, V]`
   */
  implicit def convertEqualityToJavaMapAggregating[K, V, JMAP[k, v] <: java.util.Map[k, v]](equality: Equality[java.util.Map.Entry[K, V]]): Aggregating[JMAP[K, V]] = 
    aggregatingNatureOfJavaMap(equality)

  /**
   * Implicit to support `Aggregating` nature of `Every`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `Every`
   * @tparam E the type of the element in the `Every`
   * @return `Aggregating[Every[E]]` that supports `Every` in relevant `contain` syntax
   */
  implicit def aggregatingNatureOfEvery[E](implicit equality: Equality[E]): Aggregating[Every[E]] =
    new Aggregating[Every[E]] {
      def containsAtLeastOneOf(every: Every[E], elements: scala.collection.Seq[Any]): Boolean = {
        every.exists((e: E) => elements.exists((ele: Any) => equality.areEqual(e, ele)))
      }
      def containsTheSameElementsAs(every: Every[E], elements: GenTraversable[Any]): Boolean = {
        checkTheSameElementsAs[E](every, elements, equality)
      }
      def containsOnly(every: Every[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkOnly(every, elements, equality)
      }
      def containsAllOf(every: Every[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkAllOf(every, elements, equality)
      }
      def containsAtMostOneOf(every: Every[E], elements: scala.collection.Seq[Any]): Boolean = {
        checkAtMostOneOf(every, elements, equality)
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Aggregating` of type `Every[E]`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (Every("hi") should contain ("HI")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Aggregating[Every[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `Every`
   * @return `Aggregating` of type `Every[E]`
   */
  implicit def convertEqualityToEveryAggregating[E](equality: Equality[E]): Aggregating[Every[E]] =
    aggregatingNatureOfEvery(equality)
}
