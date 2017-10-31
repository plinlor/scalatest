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
 * Supertrait for `Size` typeclasses.
 *
 * Trait `Size` is a typeclass trait for objects that can be queried for size.
 * Objects of type T for which an implicit `Size[T]` is available can be used
 * with the `should have size` syntax.
 * In other words, this trait enables you to use the size checking
 * syntax with arbitrary objects. As an example, the following `Bridge` class:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; import enablers.Size
 * import enablers.Size
 *
 * scala&gt; import Matchers._
 * import Matchers._
 *
 * scala&gt; case class Bridge(span: Int)
 * defined class Bridge
 * }}}
 *
 * Out of the box you can't use the `should have size` syntax with `Bridge`,
 * because ScalaTest doesn't know that a bridge's span means its size:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; val bridge = new Bridge(2000)
 * bridge: Bridge = Bridge(2000)
 *
 * scala&gt; bridge should have size 2000
 * &lt;console&gt;:34: error: could not find implicit value for
 *     parameter sz: org.scalatest.enablers.Size[Bridge]
 *       bridge should have size 2000
 *                          ^
 * }}}
 *
 * You can teach this to ScalaTest, however, by defining an implicit `Size[Bridge]`.
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; implicit val sizeOfBridge: Size[Bridge] =
 *      |   new Size[Bridge] {
 *      |     def sizeOf(b: Bridge): Long = b.span
 *      |   }
 * sizeOfBridge: org.scalatest.enablers.Size[Bridge] = $anon$1@3fa27a4a
 * }}}
 *
 * With the implicit `Size[Bridge]` in scope, you can now use ScalaTest's `should have size`
 * syntax with `Bridge` instances:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; bridge should have size 2000
 * res4: org.scalatest.Assertion = Succeeded
 * 
 * scala&gt; bridge should have size 2001
 * org.scalatest.exceptions.TestFailedException: Bridge(2000) had size 2000 instead of expected size 2001
 *   at org.scalatest.MatchersHelper$.newTestFailedException(MatchersHelper.scala:148)
 *   at org.scalatest.MatchersHelper$.indicateFailure(MatchersHelper.scala:366)
 *   at org.scalatest.Matchers$ResultOfHaveWordForExtent.size(Matchers.scala:2720)
 *   ... 43 elided
 * }}}
 *
 * @author Bill Venners
 */
trait Size[T] {

  /**
   * Returns the size of the passed object.
   *
   * @param obj the object whose size to return
   * @return the size of the passed object
   */
  def sizeOf(obj: T): Long
}

/**
 * Companion object for `Size` that provides implicit implementations for the following types:
 *
 * <ul>
 * <li>`scala.collection.GenTraversable`</li>
 * <li>`String`</li>
 * <li>`Array`</li>
 * <li>`java.util.Collection`</li>
 * <li>`java.util.Map`</li>
 * <li>arbitary object with a `size()` method that returns `Int`</li>
 * <li>arbitary object with a parameterless `size` method that returns `Int`</li>
 * <li>arbitary object with a `getSize()` method that returns `Int`</li>
 * <li>arbitary object with a parameterless `getSize` method that returns `Int`</li>
 * <li>arbitary object with a `size()` method that returns `Long`</li>
 * <li>arbitary object with a parameterless `size` method that returns `Long`</li>
 * <li>arbitary object with a `getSize()` method that returns `Long`</li>
 * <li>arbitary object with a parameterless `getSize` method that returns `Long`</li>
 * </ul>
 */
object Size {

  /**
   * Enable `Size` implementation for `java.util.Collection`
   *
   * @tparam JCOL any subtype of `java.util.Collection`
   * @return `Size[JCOL]` that supports `java.util.Collection` in `have size` syntax
   */
  implicit def sizeOfJavaCollection[JCOL <: java.util.Collection[_]]: Size[JCOL] = 
    new Size[JCOL] {
      def sizeOf(javaColl: JCOL): Long = javaColl.size
    }

  /**
   * Enable `Size` implementation for `java.util.Map`
   *
   * @tparam JMAP any subtype of `java.util.Map`
   * @return `Size[JMAP]` that supports `java.util.Map` in `have size` syntax
   */
  implicit def sizeOfJavaMap[JMAP <: java.util.Map[_, _]]: Size[JMAP] = 
    new Size[JMAP] {
      def sizeOf(javaMap: JMAP): Long = javaMap.size
    }

  /**
   * Enable `Size` implementation for `scala.collection.GenTraversable`
   *
   * @tparam TRAV any subtype of `scala.collection.GenTraversable`
   * @return `Size[TRAV]` that supports `scala.collection.GenTraversable` in `have size` syntax
   */
  implicit def sizeOfGenTraversable[TRAV <: scala.collection.GenTraversable[_]]: Size[TRAV] = 
    new Size[TRAV] {
      def sizeOf(trav: TRAV): Long = trav.size
    }

  /**
   * Enable `Size` implementation for `Array`
   *
   * @tparam E the type of the element in the `Array`
   * @return `Size[Array[E]]` that supports `Array` in `have size` syntax
   */
  implicit def sizeOfArray[E]: Size[Array[E]] = 
    new Size[Array[E]] {
      def sizeOf(arr: Array[E]): Long = arr.length
    }

  /**
   * Enable `Size` implementation for `String`
   *
   * @return `Size[String]` that supports `String` in `have size` syntax
   */
  implicit val sizeOfString: Size[String] = 
    new Size[String] {
      def sizeOf(str: String): Long = str.length
    }

  import scala.language.reflectiveCalls

  /**
   * Enable `Size` implementation for arbitary object with `size()` method that returns `Int`.
   *
   * @tparam T any type with `size()` method that returns `Int`
   * @return `Size[T]` that supports `T` in `have size` syntax
   */
  implicit def sizeOfAnyRefWithSizeMethodForInt[T <: AnyRef { def size(): Int}]: Size[T] = 
    new Size[T] {
      def sizeOf(obj: T): Long = obj.size
    }

  /**
   * Enable `Size` implementation for arbitary object with parameterless `size` method that returns `Int`.
   *
   * @tparam T any type with parameterless `size` method that returns `Int`
   * @return `Size[T]` that supports `T` in `have size` syntax
   */
  implicit def sizeOfAnyRefWithParameterlessSizeMethodForInt[T <: AnyRef { def size: Int}]: Size[T] = 
    new Size[T] {
      def sizeOf(obj: T): Long = obj.size
    }

  /**
   * Enable `Size` implementation for arbitary object with `getSize()` method that returns `Int`.
   *
   * @tparam T any type with `getSize()` method that returns `Int`
   * @return `Size[T]` that supports `T` in `have size` syntax
   */
  implicit def sizeOfAnyRefWithGetSizeMethodForInt[T <: AnyRef { def getSize(): Int}]: Size[T] = 
    new Size[T] {
      def sizeOf(obj: T): Long = obj.getSize
    }

  /**
   * Enable `Size` implementation for arbitary object with parameterless `getSize` method that returns `Int`.
   *
   * @tparam T any type with parameterless `getSize` method that returns `Int`
   * @return `Size[T]` that supports `T` in `have size` syntax
   */
  implicit def sizeOfAnyRefWithParameterlessGetSizeMethodForInt[T <: AnyRef { def getSize: Int}]: Size[T] = 
    new Size[T] {
      def sizeOf(obj: T): Long = obj.getSize
    }

  /**
   * Enable `Size` implementation for arbitary object with `size()` method that returns `Long`.
   *
   * @tparam T any type with `size()` method that returns `Long`
   * @return `Size[T]` that supports `T` in `have size` syntax
   */
  implicit def sizeOfAnyRefWithSizeMethodForLong[T <: AnyRef { def size(): Long}]: Size[T] = 
    new Size[T] {
      def sizeOf(obj: T): Long = obj.size
    }

  /**
   * Enable `Size` implementation for arbitary object with parameterless `size` method that returns `Long`.
   *
   * @tparam T any type with parameterless `size` method that returns `Long`
   * @return `Size[T]` that supports `T` in `have size` syntax
   */
  implicit def sizeOfAnyRefWithParameterlessSizeMethodForLong[T <: AnyRef { def size: Long}]: Size[T] = 
    new Size[T] {
      def sizeOf(obj: T): Long = obj.size
    }

  /**
   * Enable `Size` implementation for arbitary object with `getSize()` method that returns `Long`.
   *
   * @tparam T any type with `getSize()` method that returns `Long`
   * @return `Size[T]` that supports `T` in `have size` syntax
   */
  implicit def sizeOfAnyRefWithGetSizeMethodForLong[T <: AnyRef { def getSize(): Long}]: Size[T] = 
    new Size[T] {
      def sizeOf(obj: T): Long = obj.getSize
    }

  /**
   * Enable `Size` implementation for arbitary object with `getSize` method that returns `Long`.
   *
   * @tparam T any type with `getSize` method that returns `Long`
   * @return `Size[T]` that supports `T` in `have size` syntax
   */
  implicit def sizeOfAnyRefWithParameterlessGetSizeMethodForLong[T <: AnyRef { def getSize: Long}]: Size[T] = 
    new Size[T] {
      def sizeOf(obj: T): Long = obj.getSize
    }
}
