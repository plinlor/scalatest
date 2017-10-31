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
 * Supertrait for `Length` typeclasses.
 *
 * Trait `Length` is a typeclass trait for objects that can be queried for length.
 * Objects of type T for which an implicit `Length[T]` is available can be used
 * with the `should have length` syntax.
 * In other words, this trait enables you to use the length checking
 * syntax with arbitrary objects. As an example, the following `Bridge` class:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; import enablers.Length
 * import enablers.Length
 *
 * scala&gt; import Matchers._
 * import Matchers._
 *
 * scala&gt; case class Bridge(span: Int)
 * defined class Bridge
 * }}}
 *
 * Out of the box you can't use the `should have length` syntax with `Bridge`,
 * because ScalaTest doesn't know that a bridge's span means its length:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; val bridge = new Bridge(2000)
 * bridge: Bridge = Bridge(2000)
 *
 * scala&gt; bridge should have length 2000
 * &lt;console&gt;:34: error: could not find implicit value for
 *     parameter len: org.scalatest.enablers.Length[Bridge]
 *       bridge should have length 2000
 *                          ^
 * }}}
 *
 * You can teach this to ScalaTest, however, by defining an implicit `Length[Bridge]`.
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; implicit val lengthOfBridge: Length[Bridge] =
 *      |   new Length[Bridge] {
 *      |     def lengthOf(b: Bridge): Long = b.span
 *      |   }
 * lengthOfBridge: org.scalatest.enablers.Length[Bridge] = $anon$1@3fa27a4a
 * }}}
 *
 * With the implicit `Length[Bridge]` in scope, you can now use ScalaTest's `should have length`
 * syntax with `Bridge` instances:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; bridge should have length 2000
 * res4: org.scalatest.Assertion = Succeeded
 * 
 * scala&gt; bridge should have length 2001
 * org.scalatest.exceptions.TestFailedException: Bridge(2000) had length 2000 instead of expected length 2001
 *   at org.scalatest.MatchersHelper$.newTestFailedException(MatchersHelper.scala:148)
 *   at org.scalatest.MatchersHelper$.indicateFailure(MatchersHelper.scala:366)
 *   at org.scalatest.Matchers$ResultOfHaveWordForExtent.length(Matchers.scala:2720)
 *   ... 43 elided
 * }}}
 *
 * @author Bill Venners
 */
trait Length[T] {

  /**
   * Returns the length of the passed object.
   *
   * @param obj the object whose length to return
   * @return the length of the passed object
   */
  def lengthOf(obj: T): Long
}

/**
 * Companion object for `Length` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`scala.collection.GenSeq`</li>
 * <li>`String`</li>
 * <li>`Array`</li>
 * <li>`java.util.Collection`</li>
 * <li>arbitary object with a `length()` method that returns `Int`</li>
 * <li>arbitary object with a parameterless `length` method that returns `Int`</li>
 * <li>arbitary object with a `getLength()` method that returns `Int`</li>
 * <li>arbitary object with a parameterless `getLength` method that returns `Int`</li>
 * <li>arbitary object with a `length()` method that returns `Long`</li>
 * <li>arbitary object with a parameterless `length` method that returns `Long`</li>
 * <li>arbitary object with a `getLength()` method that returns `Long`</li>
 * <li>arbitary object with a parameterless `getLength` method that returns `Long`</li>
 * </ul>
 */
object Length {

  /**
   * Enable `Length` implementation for `java.util.List`
   *
   * @tparam JLIST any subtype of `java.util.List`
   * @return `Length[JLIST]` that supports `java.util.List` in `have length` syntax
   */
  implicit def lengthOfJavaList[JLIST <: java.util.List[_]]: Length[JLIST] = 
    new Length[JLIST] {
      def lengthOf(javaList: JLIST): Long = javaList.size
    }

  /**
   * Enable `Length` implementation for `scala.collection.GenSeq`
   *
   * @tparam SEQ any subtype of `scala.collection.GenSeq`
   * @return `Length[SEQ]` that supports `scala.collection.GenSeq` in `have length` syntax
   */
  implicit def lengthOfGenSeq[SEQ <: scala.collection.GenSeq[_]]: Length[SEQ] = 
    new Length[SEQ] {
      def lengthOf(seq: SEQ): Long = seq.length
    }

  /**
   * Enable `Length` implementation for `Array`
   *
   * @tparam E the type of the element in the `Array`
   * @return `Length[Array[E]]` that supports `Array` in `have length` syntax
   */
  implicit def lengthOfArray[E]: Length[Array[E]] = 
    new Length[Array[E]] {
      def lengthOf(arr: Array[E]): Long = arr.length
    }

  /**
   * Enable `Length` implementation for `String`
   *
   * @return `Length[String]` that supports `String` in `have length` syntax
   */
  implicit val lengthOfString: Length[String] = 
    new Length[String] {
      def lengthOf(str: String): Long = str.length
    }

  import scala.language.reflectiveCalls

  /**
   * Enable `Length` implementation for arbitary object with `length()` method that returns `Int`.
   *
   * @tparam T any type with `length()` method that returns `Int`
   * @return `Length[T]` that supports `T` in `have length` syntax
   */
  implicit def lengthOfAnyRefWithLengthMethodForInt[T <: AnyRef { def length(): Int}]: Length[T] = 
    new Length[T] {
      def lengthOf(obj: T): Long = obj.length
    }

  /**
   * Enable `Length` implementation for arbitary object with parameterless `length` method that returns `Int`.
   *
   * @tparam T any type with parameterless `length` method that returns `Int`
   * @return `Length[T]` that supports `T` in `have length` syntax
   */
  implicit def lengthOfAnyRefWithParameterlessLengthMethodForInt[T <: AnyRef { def length: Int}]: Length[T] = 
    new Length[T] {
      def lengthOf(obj: T): Long = obj.length
    }

  /**
   * Enable `Length` implementation for arbitary object with `getLength()` method that returns `Int`.
   *
   * @tparam T any type with `getLength()` method that returns `Int`
   * @return `Length[T]` that supports `T` in `have length` syntax
   */
  implicit def lengthOfAnyRefWithGetLengthMethodForInt[T <: AnyRef { def getLength(): Int}]: Length[T] = 
    new Length[T] {
      def lengthOf(obj: T): Long = obj.getLength
    }

  /**
   * Enable `Length` implementation for arbitary object with parameterless `getLength` method that returns `Int`.
   *
   * @tparam T any type with parameterless `getLength` method that returns `Int`
   * @return `Length[T]` that supports `T` in `have length` syntax
   */
  implicit def lengthOfAnyRefWithParameterlessGetLengthMethodForInt[T <: AnyRef { def getLength: Int}]: Length[T] = 
    new Length[T] {
      def lengthOf(obj: T): Long = obj.getLength
    }

  /**
   * Enable `Length` implementation for arbitary object with `length()` method that returns `Long`.
   *
   * @tparam T any type with `length()` method that returns `Long`
   * @return `Length[T]` that supports `T` in `have length` syntax
   */
  implicit def lengthOfAnyRefWithLengthMethodForLong[T <: AnyRef { def length(): Long}]: Length[T] = 
    new Length[T] {
      def lengthOf(obj: T): Long = obj.length
    }

  /**
   * Enable `Length` implementation for arbitary object with parameterless `length` method that returns `Long`.
   *
   * @tparam T any type with parameterless `length` method that returns `Long`
   * @return `Length[T]` that supports `T` in `have length` syntax
   */
  implicit def lengthOfAnyRefWithParameterlessLengthMethodForLong[T <: AnyRef { def length: Long}]: Length[T] = 
    new Length[T] {
      def lengthOf(obj: T): Long = obj.length
    }

  /**
   * Enable `Length` implementation for arbitary object with `getLength()` method that returns `Long`.
   *
   * @tparam T any type with `getLength()` method that returns `Long`
   * @return `Length[T]` that supports `T` in `have length` syntax
   */
  implicit def lengthOfAnyRefWithGetLengthMethodForLong[T <: AnyRef { def getLength(): Long}]: Length[T] = 
    new Length[T] {
      def lengthOf(obj: T): Long = obj.getLength
    }

  /**
   * Enable `Length` implementation for arbitary object with parameterless `getLength` method that returns `Long`.
   *
   * @tparam T any type with parameterless `getLength` method that returns `Long`
   * @return `Length[T]` that supports `T` in `have length` syntax
   */
  implicit def lengthOfAnyRefWithParameterlessGetLengthMethodForLong[T <: AnyRef { def getLength: Long}]: Length[T] = 
    new Length[T] {
      def lengthOf(obj: T): Long = obj.getLength
    }
}

