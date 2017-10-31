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

import org.scalactic.Every
import scala.collection.GenTraversable
import org.scalatest.FailureMessages
import scala.annotation.tailrec
import scala.language.higherKinds

/**
 * Supertrait for typeclasses that enable <a href="../LoneElement.html">`loneElement`</a> and <a href="../Inspectors.html">inspectors</a> syntax
 * for collections.
 *
 * A `Collecting[E, C]` provides access to the "collecting nature" of type `C` in such
 * a way that `loneElement` syntax can be used with type `C`. A `C`
 * can be any type of "collecting", a type that in some way collects or brings together elements of type `E`.
 * ScalaTest provides implicit implementations for several types. You can enable the `contain` matcher syntax
 * on your own type `U` by defining an `Collecting[E, U]` for the type and making it available implicitly.
 * 
 * 
 * ScalaTest provides implicit `Collecting` instances for `scala.collection.GenTraversable`,
 * `Array`, `java.util.Collection` and `java.util.Map` in the
 * `Collecting` companion object.
 * 
 */
trait Collecting[E, C] {

  /**
   * Implements the `loneElement` syntax of trait `LoneElement`.
   *
   * Returns the lone element contained in a collection, wrapped in a `Some`, or `None`  
   * if the collection contains either no elements or more than one element.
   * 
   *
   * @param collection a collection about which an assertion is being made
   * @return `Some[E]` if the collection contains one and only one element, `None` otherwise.
   */
  def loneElementOf(collection: C): Option[E]

  /**
   * Returns the size of the passed `collection`.
   *
   * @param collection a `collection` to check the size of
   * @return the size of the passed `collection`
   */
  def sizeOf(collection: C): Int

  /**
   * Returns a `GenTraversable[E]` containing the same elements (in the same
   * order, if the original collection had a defined order), as the passed `collection` .
   *
   * @param collection a `collection` to check the size of
   * @return a `GenTraversable[E]` containing the same elements as the passed `collection`
   */
  def genTraversableFrom(collection: C): GenTraversable[E]
}

/**
 * Companion object for `Collecting` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`scala.collection.GenTraversable`</li>
 * <li>`Array`</li>
 * <li>`java.util.Collection`</li>
 * <li>`java.util.Map`</li>
 * </ul>
 */
object Collecting {

  /**
   * Implicit to support `Collecting` nature of `GenTraversable`.
   *
   * @tparam E the type of the element in the `GenTraversable`
   * @tparam TRAV any subtype of `GenTraversable`
   * @return `Collecting[E, TRAV[E]]` that supports `GenTraversable` in `loneElement` syntax
   */
  implicit def collectingNatureOfGenTraversable[E, TRAV[e] <: scala.collection.GenTraversable[e]]: Collecting[E, TRAV[E]] = 
    new Collecting[E, TRAV[E]] {
      def loneElementOf(trav: TRAV[E]): Option[E] = {
        if (trav.size == 1) Some(trav.head) else None
      }
      def sizeOf(trav: TRAV[E]): Int = trav.size
      def genTraversableFrom(collection: TRAV[E]): GenTraversable[E] = collection
    }

  /**
   * Implicit to support `Collecting` nature of `Array`.
   *
   * @tparam E the type of the element in the `Array`
   * @return `Collecting[E, Array[E]]` that supports `Array` in `loneElement` syntax
   */
  implicit def collectingNatureOfArray[E]: Collecting[E, Array[E]] = 
    new Collecting[E, Array[E]] {
      def loneElementOf(array: Array[E]): Option[E] = {
        if (array.size == 1) Some(array.head) else None
      }
      def sizeOf(array: Array[E]): Int = array.length
      def genTraversableFrom(collection: Array[E]): GenTraversable[E] = collection
    }

  /**
   * Implicit to support `Collecting` nature of `String`.
   *
   * @return `Collecting[Char, String]` that supports `String` in `loneElement` syntax
   */
  implicit def collectingNatureOfString: Collecting[Char, String] = 
    new Collecting[Char, String] {
      def loneElementOf(string: String): Option[Char] = {
        if (string.size == 1) Some(string.head) else None
      }
      def sizeOf(string: String): Int = string.length
      def genTraversableFrom(collection: String): GenTraversable[Char] = collection.toVector
    }

  /**
   * Implicit to support `Collecting` nature of `java.util.Collection`.
   *
   * @tparam E the type of the element in the `java.util.Collection`
   * @tparam JCOL any subtype of `java.util.Collection`
   * @return `Collecting[E, JCOL[E]]` that supports `java.util.Collection` in `loneElement` syntax
   */
  implicit def collectingNatureOfJavaCollection[E, JCOL[e] <: java.util.Collection[e]]: Collecting[E, JCOL[E]] = 
    new Collecting[E, JCOL[E]] {
      def loneElementOf(coll: JCOL[E]): Option[E] = {
        if (coll.size == 1) Some(coll.iterator.next) else None
      }
      def sizeOf(coll: JCOL[E]): Int = coll.size
      def genTraversableFrom(collection: JCOL[E]): GenTraversable[E] = {
        import scala.collection.JavaConverters._
        /*
        This is what asScala does, to make sure it keeps the order of Lists
        scala.collection.mutable.Buffer <=> java.util.List
        scala.collection.mutable.Set <=> java.util.Set
        */
        collection match {
          case jList: java.util.List[E @unchecked] => jList.asScala
          case jSet: java.util.Set[E @unchecked] => jSet.asScala
          case _ => collection.asScala
        }
      }
    }

  // Wrap the extracted entry in an org.scalatest.Entry so people can call key and value methods instead of getKey and getValue
  /**
   * Implicit to support `Collecting` nature of `java.util.Map`.
   *
   * @tparam K the type of the key in the `java.util.Map`
   * @tparam V the type of the value in the `java.util.Map`
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `Collecting[org.scalatest.Entry[K, V], JMAP[K, V]]` that supports `java.util.Map` in `loneElement` syntax
   */
  implicit def collectingNatureOfJavaMap[K, V, JMAP[k, v] <: java.util.Map[k, v]]: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]] = 
    new Collecting[org.scalatest.Entry[K, V], JMAP[K, V]] {
      def loneElementOf(jmap: JMAP[K, V]): Option[org.scalatest.Entry[K, V]] = {
        if (jmap.size == 1) {
          val loneEntry = jmap.entrySet.iterator.next
          Some(org.scalatest.Entry(loneEntry.getKey, loneEntry.getValue))
        } else None
      }
      def sizeOf(jmap: JMAP[K, V]): Int = jmap.size
        /*
        Original order needs to be preserved
        */
      def genTraversableFrom(collection: JMAP[K, V]): scala.collection.GenTraversable[org.scalatest.Entry[K, V]] = {
        import scala.collection.JavaConverters._
        collection.entrySet.iterator.asScala.map(entry => org.scalatest.Entry(entry.getKey, entry.getValue)).toList
      }
    }

  /**
   * Implicit to support `Collecting` nature of `Every`.
   *
   * @tparam E the type of the element in the `Every`
   * @tparam EVERY any subtype of `Every`
   * @return `Collecting[EVERY[E]]` that supports `Every` in `loneElement` syntax
   */
  implicit def collectingNatureOfEvery[E, EVERY[e] <: Every[e]]: Collecting[E, EVERY[E]] =
    new Collecting[E, EVERY[E]] {
      def loneElementOf(every: EVERY[E]): Option[E] =
        if (every.size == 1) Some(every.head) else None
      def sizeOf(every: EVERY[E]): Int = every.size
      def genTraversableFrom(collection: EVERY[E]): GenTraversable[E] = collection.toVector
    }

}
