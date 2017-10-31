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
package org.scalatest

import org.scalactic._
import enablers.Collecting
import exceptions.StackDepthException

/**
 * Trait that provides an implicit conversion that adds to collection types a `loneElement` method, which
 * will return the value of the lone element if the collection does
 * indeed contain one and only one element, or throw <a href="TestFailedException.html">`TestFailedException`</a> if not.
 *
 * This construct allows you to express in one statement that a collection should contain one and only one element
 * and that the element value should meet some expectation. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * set.loneElement should be &gt; 9
 * }}}
 *
 * Or, using an assertion instead of a matcher expression:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * assert(set.loneElement &gt; 9)
 * }}}
 *
 * The `loneElement` syntax can be used with any collection type `C` for which an
 * implicit <a href="enablers/Collecting.html">`Collecting[C]`</a> is available. ScalaTest provides
 * implicit `Collecting` instances for `scala.collection.GenTraversable`, `Array`,
 * and `java.util.Collection`. You can enable the `loneElement`
 * syntax on other collection types by defining an implicit `Collecting` instances for those types.
 * 
 *
 * If you want to use `loneElement` with a `java.util.Map`, first transform it to a
 * set of entries with `entrySet`, and if helpful, use ScalaTest's <a href="Entry.html">`Entry`</a> class:
 * 
 * 
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; import LoneElement._
 * import LoneElement._
 *
 * scala&gt; import Matchers._
 * import Matchers._
 *
 * scala&gt; val jmap = new java.util.HashMap[String, Int]
 * jmap: java.util.HashMap[String,Int] = {}
 *
 * scala&gt; jmap.put("one", 1)
 * res0: Int = 0
 *
 * scala&gt; jmap.entrySet.loneElement should be (Entry("one", 1))
 * }}}
 *
 * @author Bill Venners
 */
trait LoneElement {

  import scala.language.higherKinds

  /**
   * Wrapper class that adds a `loneElement` method to any collection type `C` for which 
   * an implicit `Collecting[C]` is available.
   *
   * Through the implicit conversion provided by trait `LoneElement`, this class allows you to make statements like:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * trav.loneElement should be &gt; 9
   * }}}
   *
   * @tparam E the element type of the collection on which to add the `loneElement` method
   * @tparam CTC the "collection type constructor" for the collection on which to add the `loneElement` method
   * @param collection a collection to wrap in a `LoneElementCollectionWrapper`, which provides the `loneElement` method.
   * @param collecting a typeclass that enables the `loneElement` syntax
   */
  final class LoneElementCollectionWrapper[E, CTC[_]](collection: CTC[E], collecting: Collecting[E, CTC[E]], prettifier: Prettifier, pos: source.Position) {

    /**
     * Returns the value contained in the wrapped collection, if it contains one and only one element, else throws `TestFailedException` with
     * a detail message describing the problem.
     *
     * This method enables syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * trav.loneElement should be &gt; 9
     *      ^
     * }}}
     */
    def loneElement: E = {
      collecting.loneElementOf(collection) match {
        case Some(ele) => ele
        case None =>
          throw new exceptions.TestFailedException(
            (_: StackDepthException) => Some(FailureMessages.notLoneElement(prettifier,
                 collection,
                 collecting.sizeOf(collection))),
            None,
            pos
          )
      }
    }
  }

  import scala.language.implicitConversions

  /**
   * Implicit conversion that adds a `loneElement` method to any collection type `C` for which an
   * implicit `Collecting[C]` is available.
   *
   * @tparam E the element type of the collection on which to add the `loneElement` method
   * @tparam CTC the "collection type constructor" for the collection on which to add the `loneElement` method
   * @param collection the collection on which to add the `loneElement` method
   * @param collecting a typeclass that enables the `loneElement` syntax
   */
  implicit def convertToCollectionLoneElementWrapper[E, CTC[_]](collection: CTC[E])(implicit collecting: Collecting[E, CTC[E]], prettifier: Prettifier, pos: source.Position): LoneElementCollectionWrapper[E, CTC] = new LoneElementCollectionWrapper[E, CTC](collection, collecting, prettifier, pos)

  /**
   * Wrapper class that adds a `loneElement` method to Java Map for which
   * an implicit `Collecting[org.scalatest.Entry, java.util.Map]` is available.
   *
   * Through the implicit conversion provided by trait `LoneElement`, this class allows you to make statements like:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * jmap.loneElement.getKey should be &gt; 9
   * }}}
   *
   * @tparam K the element type of the Java Map key on which to add the `loneElement` method
   * @tparam V the element type of the Java Map value on which to add the `loneElement` method
   * @tparam JMAP the "Java Map type constructor" for the collection on which to add the `loneElement` method
   * @param collecting a typeclass that enables the `loneElement` syntax
   */
  final class LoneElementJavaMapWrapper[K, V, JMAP[_, _] <: java.util.Map[_, _]](jmap: JMAP[K, V], collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], prettifier: Prettifier, pos: source.Position) {

    def loneElement: org.scalatest.Entry[K, V] = {
      collecting.loneElementOf(jmap) match {
        case Some(ele) => ele
        case None =>
          throw new exceptions.TestFailedException(
            (_: StackDepthException) => Some(FailureMessages.notLoneElement(prettifier,
                 jmap,
                 collecting.sizeOf(jmap))), 
            None,
            pos
          )
      }
    }
  }

  // Needed for Java Map to work, any better solution?
  implicit def convertJavaMapToCollectionLoneElementWrapper[K, V, JMAP[_, _] <: java.util.Map[_, _]](jmap: JMAP[K, V])(implicit collecting: Collecting[org.scalatest.Entry[K, V], JMAP[K, V]], prettifier: Prettifier, pos: source.Position): LoneElementJavaMapWrapper[K, V, JMAP] = {
    new LoneElementJavaMapWrapper[K, V, JMAP](jmap, collecting, prettifier, pos)
  }

  /**
   * Wrapper class that adds a `loneElement` method to `String` for which an
   * implicit `Collecting[C]` is available.
   *
   * Through the implicit conversion provided by trait `LoneElement`, this class allows you to make statements like:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * "9".loneElement should be ('9')
   * }}}
   *
   * @param s the `String` to wrap
   * @param collecting a typeclass that enables the `loneElement` syntax
   */
  final class LoneElementStringWrapper(s: String, prettifier: Prettifier, pos: source.Position) {

    def loneElement: Char = {
      if (s.length == 1)
        s.charAt(0)
      else
        throw new exceptions.TestFailedException(
          (_: StackDepthException) => Some(FailureMessages.notLoneElement(prettifier,
            s,
            s.length)),
          None,
          pos
        )
    }
  }

  /**
   * Implicit conversion that adds a `loneElement` method to String for which an
   * implicit `Collecting[C]` is available.
   *
   * @param s the `String` to wrap
   * @param collecting a typeclass that enables the `loneElement` syntax
   */
  implicit def convertToStringLoneElementWrapper(s: String)(implicit prettifier: Prettifier, pos: source.Position): LoneElementStringWrapper =
    new LoneElementStringWrapper(s, prettifier, pos)
}

/**
 * Companion object that facilitates the importing of `LoneElement` members as 
 * an alternative to mixing it in. One use case is to import `LoneElement`'s members so you can use
 * `loneElement` in the Scala interpreter.
 */
object LoneElement extends LoneElement

