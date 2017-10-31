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

import org.scalactic.{Equality, NormalizingEquality, Every}
import scala.collection.{GenTraversableOnce, GenTraversable}


/**
 * Supertrait for typeclasses that enable certain `contain` matcher syntax for containers.
 *
 * A `Containing[C]` provides access to the "containing nature" of type `C` in such
 * a way that relevant `contain` matcher syntax can be used with type `C`. A `C`
 * can be any type of "container," a type that in some way can contains one or more other objects. ScalaTest provides
 * implicit implementations for several types. You can enable the `contain` matcher syntax on your own
 * type `U` by defining an `Containing[U]` for the type and making it available implicitly.
 * 
 * ScalaTest provides implicit `Containing` instances for `scala.collection.GenTraversable`,
 * `java.util.Collection`, `java.util.Map`, `String`, `Array`, 
 * and `scala.Option` in the `Containing` companion object.
 * 
 *
 * <a name="containingVersusAggregating"></a>
 * ==`Containing` versus `Aggregating`==
 * 
 * The difference between `Containing` and <a href="Aggregating.html">`Aggregating`</a> is that
 * `Containing` enables `contain` matcher syntax that makes sense for "box" types that can
 * contain at most one value (for example, `scala.Option`),
 * whereas `Aggregating` enables `contain` matcher syntax for full-blown collections and other 
 * aggregations of potentially more than one object. For example, it makes sense to make assertions like these, which 
 * are enabled by `Containing`, for `scala.Option`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * val option: Option[Int] = Some(7)
 * option should contain (7)
 * option should contain oneOf (6, 7, 8)
 * option should contain noneOf (3, 4, 5)
 * }}}
 *
 * However, given a `scala.Option` can only ever contain at most one object, it doesn't make
 * sense to make assertions like the following, which are enabled via `Aggregation`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * // Could never succeed, so does not compile
 * option should contain allOf (6, 7, 8)
 * }}}
 * 
 * The above assertion could never succceed, because an option cannot contain more than
 * one value. By default the above statement does not compile, because `contain` `allOf`
 * is enabled by `Aggregating`, and ScalaTest provides no implicit `Aggregating` instance
 * for type `scala.Option`.
 * 
 */
trait Containing[-C] {

  /**
   * Implements `contain` `&lt;value&gt;` syntax for containers of type `C`.
   *
   * @param container a container about which an assertion is being made
   * @param element an element that should be contained in the passed container
   * @return true if the passed container contains the passed element
   */
  def contains(container: C, element: Any): Boolean

  /**
   * Implements `contain` `oneOf` syntax for containers of type `C`.
   *
   * @param container a container about which an assertion is being made
   * @param elements elements exactly one (''i.e.'', one and only one) of which should be contained in the passed container
   * @return true if the passed container contains exactly one of the passed elements
   */
  def containsOneOf(container: C, elements: scala.collection.Seq[Any]): Boolean

  /**
   * Implements `contain` `noneOf` syntax for containers of type `C`.
   *
   * @param container a container about which an assertion is being made
   * @param elements elements none of which should be contained in the passed container
   * @return true if the passed container contains none of the passed elements
   */
  def containsNoneOf(container: C, elements: scala.collection.Seq[Any]): Boolean
}

/*
  @tailrec
  def containsOneOf[T](left: T, rightItr: Iterator[Any])(implicit holder: Containing[T]): Boolean = {
    if (rightItr.hasNext) {
      val nextRight = rightItr.next
      if (holder.contains(left, nextRight)) // Found one of right in left, can succeed early
        true
      else
        containsOneOf(left, rightItr)
    }
    else // No more elements in right, left does not contain one of right.
      false
  }
*/

/**
 * Companion object for `Containing` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`scala.collection.GenTraversable`</li>
 * <li>`String`</li>
 * <li>`Array`</li>
 * <li>`scala.Option`</li>
 * <li>`java.util.Collection`</li>
 * <li>`java.util.Map`</li>
 * </ul>
 */
object Containing {
  
  private def tryEquality[T](left: Any, right: Any, equality: Equality[T]): Boolean = 
    try equality.areEqual(left.asInstanceOf[T], right)
      catch {
        case cce: ClassCastException => false
    }
  
  private[scalatest] def checkOneOf[T](left: GenTraversableOnce[T], right: GenTraversable[Any], equality: Equality[T]): Set[Any] = {
    // aggregate version is more verbose, but it allows parallel execution.
    right.aggregate(Set.empty[Any])( 
      { case (fs, r) => 
          if (left.exists(t => equality.areEqual(t, r))) {
            // r is in the left
            if (fs.size != 0) // This .size should be safe, it won't go > 1
              return fs + r // fail early by returning early, hmm..  not so 'functional'??
            else
              fs + r
          }
          else 
            fs // r is not in the left
      }, 
      { case (fs1, fs2) => 
        val fs = fs1 + fs2
        if (fs.size > 1)
          return fs // fail early by returning early
        else
          fs
      }
    )
  }
  
  private[scalatest] def checkNoneOf[T](left: GenTraversableOnce[T], right: GenTraversable[Any], equality: Equality[T]): Option[Any] = {
    right.aggregate(None)( 
      { case (f, r) => 
          if (left.exists(t => equality.areEqual(t, r))) 
            return Some(r) // r is in the left, fail early by returning.
          else 
            None // r is not in the left
      }, 
      { case (f1, f2) => None }
    )
  }

  import scala.language.higherKinds

  /**
   * Implicit to support `Containing` nature of `java.util.Collection`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `java.util.Collection`
   * @tparam E the type of the element in the `java.util.Collection`
   * @tparam JCOL any subtype of `java.util.Collection`
   * @return `Containing[JCOL[E]]` that supports `java.util.Collection` in relevant `contain` syntax
   */
  implicit def containingNatureOfJavaCollection[E, JCOL[e] <: java.util.Collection[e]](implicit equality: Equality[E]): Containing[JCOL[E]] = 
    new Containing[JCOL[E]] {
      def contains(javaColl: JCOL[E], ele: Any): Boolean = {
        val it: java.util.Iterator[E] = javaColl.iterator
        var found = false
        while (!found && it.hasNext) {
          found = equality.areEqual(it.next , ele)
        }
        found
      }
      import scala.collection.JavaConverters._
      def containsOneOf(javaColl: JCOL[E], elements: scala.collection.Seq[Any]): Boolean = {
        
        val foundSet = checkOneOf[E](javaColl.asScala, elements, equality)
        foundSet.size == 1
      }
      def containsNoneOf(javaColl: JCOL[E], elements: scala.collection.Seq[Any]): Boolean = {
        val found = checkNoneOf[E](javaColl.asScala, elements, equality)
        !found.isDefined
      }
    }

  import scala.language.implicitConversions

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Containing` of type `JCOL[E]`, where `JCOL` is a subtype of `java.util.Collection`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * val javaList = new java.util.ArrayList[String]()
   * javaList.add("hi")
   * (javaList should contain oneOf ("HI")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Containing[java.util.ArrayList[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `java.util.Collection`
   * @tparam JCOL subtype of `java.util.Collection`
   * @return `Containing` of type `JCOL[E]`
   */
  implicit def convertEqualityToJavaCollectionContaining[E, JCOL[e] <: java.util.Collection[e]](equality: Equality[E]): Containing[JCOL[E]] = 
    containingNatureOfJavaCollection(equality)

  /**
   * Implicit to support `Containing` nature of `GenTraversable`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `GenTraversable`
   * @tparam E the type of the element in the `GenTraversable`
   * @tparam TRAV any subtype of `GenTraversable`
   * @return `Containing[TRAV[E]]` that supports `GenTraversable` in relevant `contain` syntax
   */
  implicit def containingNatureOfGenTraversable[E, TRAV[e] <: scala.collection.GenTraversable[e]](implicit equality: Equality[E]): Containing[TRAV[E]] = 
    new Containing[TRAV[E]] {
      def contains(trav: TRAV[E], ele: Any): Boolean = {
        equality match {
          case normEq: NormalizingEquality[_] => 
            val normRight = normEq.normalizedOrSame(ele)
            trav.exists((e: E) => normEq.afterNormalizationEquality.areEqual(normEq.normalized(e), normRight))
          case _ => trav.exists((e: E) => equality.areEqual(e, ele))
        }
      }
      def containsOneOf(trav: TRAV[E], elements: scala.collection.Seq[Any]): Boolean = {
        val foundSet = checkOneOf[E](trav, elements, equality)
        foundSet.size == 1
      }
      def containsNoneOf(trav: TRAV[E], elements: scala.collection.Seq[Any]): Boolean = {
        val found = checkNoneOf[E](trav, elements, equality)
        !found.isDefined
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Containing` of type `TRAV[E]`, where `TRAV` is a subtype of `GenTraversable`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (List("hi") should contain oneOf ("HI")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Containing[List[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `GenTraversable`
   * @tparam TRAV subtype of `GenTraversable`
   * @return `Containing` of type `TRAV[E]`
   */
  implicit def convertEqualityToGenTraversableContaining[E, TRAV[e] <: scala.collection.GenTraversable[e]](equality: Equality[E]): Containing[TRAV[E]] = 
    containingNatureOfGenTraversable(equality)

  // OPT so that it will work with Some also, but it doesn't work with None
  /**
   * Implicit to support `Containing` nature of `scala.Option`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `Option`
   * @tparam E the type of the element in the `scala.Option`
   * @tparam OPT any subtype of `scala.Option`
   * @return `Containing[OPT[E]]` that supports `scala.Option` in relevant `contain` syntax
   */
  implicit def containingNatureOfOption[E, OPT[e] <: Option[e]](implicit equality: Equality[E]): Containing[OPT[E]] = 
    new Containing[OPT[E]] {
      def contains(opt: OPT[E], ele: Any): Boolean = {
        opt.exists((e: E) => equality.areEqual(e, ele))
      }
      def containsOneOf(opt: OPT[E], elements: scala.collection.Seq[Any]): Boolean = {
        val foundSet = checkOneOf[E](opt, elements, equality)
        foundSet.size == 1
      }
      def containsNoneOf(opt: OPT[E], elements: scala.collection.Seq[Any]): Boolean = {
        val found = checkNoneOf[E](opt, elements, equality)
        !found.isDefined
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Containing` of type `OPT[E]`, where `OPT` is a subtype of `scala.Option`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (Some("hi") should contain oneOf ("HI")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Containing[Some[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `scala.Option`
   * @tparam OPT subtype of `scala.Option`
   * @return `Containing` of type `OPT[E]`
   */
  implicit def convertEqualityToOptionContaining[E, OPT[e] <: Option[e]](equality: Equality[E]): Containing[OPT[E]] = 
    containingNatureOfOption(equality)

  /**
   * Implicit to support `Containing` nature of `Array`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `Array`
   * @tparam E the type of the element in the `Array`
   * @return `Containing[Array[E]]` that supports `Array` in relevant `contain` syntax
   */
  implicit def containingNatureOfArray[E](implicit equality: Equality[E]): Containing[Array[E]] = 
    new Containing[Array[E]] {
      def contains(arr: Array[E], ele: Any): Boolean =
        arr.exists((e: E) => equality.areEqual(e, ele))
      def containsOneOf(arr: Array[E], elements: scala.collection.Seq[Any]): Boolean = {
        val foundSet = checkOneOf[E](arr, elements, equality)
        foundSet.size == 1
      }
      def containsNoneOf(arr: Array[E], elements: scala.collection.Seq[Any]): Boolean = {
        val found = checkNoneOf[E](arr, elements, equality)
        !found.isDefined
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Containing` of type `Array[E]`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (Array("hi") should contain oneOf ("HI")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Containing[Array[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `Array`
   * @return `Containing` of type `Array[E]`
   */
  implicit def convertEqualityToArrayContaining[E](equality: Equality[E]): Containing[Array[E]] = 
    containingNatureOfArray(equality)

  /**
   * Implicit to support `Containing` nature of `String`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of `Char` in the `String`
   * @return `Containing[String]` that supports `String` in relevant `contain` syntax
   */
  implicit def containingNatureOfString(implicit equality: Equality[Char]): Containing[String] = 
    new Containing[String] {
      def contains(str: String, ele: Any): Boolean =
        str.exists((e: Char) => equality.areEqual(e, ele))
      def containsOneOf(str: String, elements: scala.collection.Seq[Any]): Boolean = {
        val foundSet = checkOneOf[Char](str, elements, equality)
        foundSet.size == 1
      }
      def containsNoneOf(str: String, elements: scala.collection.Seq[Any]): Boolean = {
        val found = checkNoneOf[Char](str, elements, equality)
        !found.isDefined
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `Char`
   * into `Containing` of type `String`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * // lowerCased needs to be implemented as Normalization[Char]
   * ("hi hello" should contain oneOf ('E')) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[Char]`</a>
   * and this implicit conversion will convert it into `Containing[String]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `Char`
   * @return `Containing` of type `String`
   */
  implicit def convertEqualityToStringContaining(equality: Equality[Char]): Containing[String] = 
    containingNatureOfString(equality)

  /**
   * Implicit to support `Containing` nature of `java.util.Map`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of entry in the `java.util.Map`
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `Containing[JMAP[K, V]]` that supports `java.util.Map` in relevant `contain` syntax
   */
  implicit def containingNatureOfJavaMap[K, V, JMAP[k, v] <: java.util.Map[k, v]](implicit equality: Equality[java.util.Map.Entry[K, V]]): Containing[JMAP[K, V]] = 
    new Containing[JMAP[K, V]] {
      import scala.collection.JavaConverters._
      def contains(map: JMAP[K, V], ele: Any): Boolean = {
        map.entrySet.asScala.exists((e: java.util.Map.Entry[K, V]) => equality.areEqual(e, ele))
      }
      def containsOneOf(map: JMAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        val foundSet = checkOneOf[java.util.Map.Entry[K, V]](map.entrySet.asScala, elements, equality)
        foundSet.size == 1
      }
      def containsNoneOf(map: JMAP[K, V], elements: scala.collection.Seq[Any]): Boolean = {
        val found = checkNoneOf[java.util.Map.Entry[K, V]](map.entrySet.asScala, elements, equality)
        !found.isDefined
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `java.util.Map.Entry[K, V]`
   * into `Containing` of type `JMAP[K, V]`, where `JMAP` is a subtype of `java.util.Map`.
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
   * and this implicit conversion will convert it into `Containing[java.util.HashMap[Int, String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `java.util.Map.Entry[K, V]`
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `Containing` of type `JMAP[K, V]`
   */
  implicit def convertEqualityToJavaMapContaining[K, V, JMAP[k, v] <: java.util.Map[k, v]](equality: Equality[java.util.Map.Entry[K, V]]): Containing[JMAP[K, V]] = 
    containingNatureOfJavaMap(equality)

  /**
   * Implicit to support `Containing` nature of `Every`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> type class that is used to check equality of element in the `Every`
   * @tparam E the type of the element in the `Every`
   * @return `Containing[Every[E]]` that supports `Every` in relevant `contain` syntax
   */
  implicit def containingNatureOfEvery[E](implicit equality: Equality[E]): Containing[Every[E]] =
    new Containing[Every[E]] {
      def contains(every: Every[E], ele: Any): Boolean =
        equality match {
          case normEq: NormalizingEquality[_] =>
            val normRight = normEq.normalizedOrSame(ele)
            every.exists((e: E) => normEq.afterNormalizationEquality.areEqual(normEq.normalized(e), normRight))
          case _ => every.exists((e: E) => equality.areEqual(e, ele))
        }
      def containsOneOf(every: Every[E], elements: scala.collection.Seq[Any]): Boolean = {
        val foundSet = checkOneOf[E](every, elements, equality)
        foundSet.size == 1
      }
      def containsNoneOf(every: Every[E], elements: scala.collection.Seq[Any]): Boolean = {
        val found = checkNoneOf[E](every, elements, equality)
        !found.isDefined
      }
    }

  /**
   * Implicit conversion that converts an <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * into `Containing` of type `Every[E]`.
   * This is required to support the explicit <a href="../../scalactic/Equality.html">`Equality`</a> syntax, for example:
   *
   * {{{  <!-- class="stHighlight" -->
   * (Every("hi", "he", "ho") should contain oneOf ("HI")) (after being lowerCased)
   * }}}
   *
   * `(after being lowerCased)` will returns an <a href="../../scalactic/Equality.html">`Equality[String]`</a>
   * and this implicit conversion will convert it into `Containing[Every[String]]`.
   *
   * @param equality <a href="../../scalactic/Equality.html">`Equality`</a> of type `E`
   * @tparam E type of elements in the `Every`
   * @return `Containing` of type `Every[E]`
   */
  implicit def convertEqualityToEveryContaining[E](equality: Equality[E]): Containing[Every[E]] =
    containingNatureOfEvery(equality)
}

