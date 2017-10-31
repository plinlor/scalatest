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

import scala.reflect.ClassTag

// T is the type of the object that has a Boolean property to verify with an instance of this trait
// This is not a subtype of BeMatcher, because BeMatcher only works after "be", but 
// BePropertyMatcher will work after "be", "be a", or "be an"
/**
 * Trait extended by matcher objects, which may appear after the word `be`, that can match against a `Boolean`
 * property. The match will succeed if and only if the `Boolean` property equals `true`.
 * The object containing the property, which must be of the type specified by the `BePropertyMatcher`'s type
 * parameter `T`, is passed to the `BePropertyMatcher`'s
 * `apply` method. The result is a `BePropertyMatchResult`.
 * A `BePropertyMatcher` is, therefore, a function from the specified type, `T`, to
 * a `BePropertyMatchResult`.
 *
 * Although `BePropertyMatcher`
 * and `Matcher` represent similar concepts, they have no inheritance relationship
 * because `Matcher` is intended for use right after `should` or `must`
 * whereas `BePropertyMatcher` is intended for use right after `be`.
 * 
 *
 * A `BePropertyMatcher` essentially allows you to write statically typed `Boolean`
 * property assertions similar to the dynamic ones that use symbols:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * tempFile should be a ('file) // dynamic: uses reflection
 * tempFile should be a (file)  // type safe: only works on Files; no reflection used
 * }}}
 *
 * One good way to organize custom matchers is to place them inside one or more traits that
 * you can then mix into the suites or specs that need them. Here's an example that
 * includes two `BePropertyMatcher`s:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * trait CustomMatchers {
 * 
 *   class FileBePropertyMatcher extends BePropertyMatcher[java.io.File] {
 *     def apply(left: java.io.File) = BePropertyMatchResult(left.isFile, "file")
 *   }
 * 
 *   class DirectoryBePropertyMatcher extends BePropertyMatcher[java.io.File] {
 *     def apply(left: java.io.File) = BePropertyMatchResult(left.isDirectory, "directory")
 *   }
 * 
 *   val file = new FileBePropertyMatcher
 *   val directory = new DirectoryBePropertyMatcher
 * }
 * }}}
 * 
 * Because the type parameter of these two `BePropertyMatcher`s is `java.io.File`, they 
 * can only be used with instances of that type. (The compiler will enforce this.) All they do is create a
 * `BePropertyMatchResult` whose `matches` field is `true` if and only if the `Boolean` property
 * is `true`. The second field, `propertyName`, is simply the string name of the property.
 * The `file` and `directory` `val`s create variables that can be used in
 * matcher expressions that test whether a `java.io.File` is a file or a directory. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class ExampleSpec extends RefSpec with Matchers with CustomMatchers {
 * 
 *   describe("A temp file") {
 * 
 *     it("should be a file, not a directory") {
 * 
 *       val tempFile = java.io.File.createTempFile("delete", "me")
 * 
 *       try {
 *         tempFile should be a (file)
 *         tempFile should not be a (directory)
 *       }
 *       finally {
 *         tempFile.delete()
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 * These matches should succeed, but if for example the first match, `tempFile should be a (file)`, were to fail, you would get an error message like:
 * 
 *
 * {{{ class="stExamples">
 * /tmp/delme1234me was not a file
 * }}}
 *
 * For more information on `BePropertyMatchResult` and the meaning of its fields, please
 * see the documentation for <a href="BePropertyMatchResult.html">`BePropertyMatchResult`</a>. To understand why `BePropertyMatcher`
 * is contravariant in its type parameter, see the section entitled "Matcher's variance" in the
 * documentation for <a href="../Matcher.html">`Matcher`</a>.
 * 
 *
 * @author Bill Venners
*/
trait BePropertyMatcher[-T] extends Function1[T, BePropertyMatchResult] {

  thisBePropertyMatcher => 

  /**
   * Check to see if a `Boolean` property on the specified object, `objectWithProperty`, matches its
   * expected value, and report the result in
   * the returned `BePropertyMatchResult`. The `objectWithProperty` is
   * usually the value to the left of a `should` or `must` invocation. For example, `tempFile`
   * would be passed as the `objectWithProperty` in:
   *
   * {{{  <!-- class="stHighlight" -->
   * tempFile should be a (file)
   * }}}
   *
   * @param objectWithProperty the object with the `Boolean` property against which to match
   * @return the `BePropertyMatchResult` that represents the result of the match
   */
  def apply(objectWithProperty: T): BePropertyMatchResult

  /**
   * Compose this `BePropertyMatcher` with the passed function, returning a new `BePropertyMatcher`.
   *
   * This method overrides `compose` on `Function1` to
   * return a more specific function type of `BePropertyMatcher`.
   * 
   */
  override def compose[U](g: U => T): BePropertyMatcher[U] =
    new BePropertyMatcher[U] {
      def apply(u: U) = thisBePropertyMatcher.apply(g(u))
    }
}

/**
 * Companion object for trait `BePropertyMatcher` that provides a
 * factory method that creates a `BePropertyMatcher[T]` from a
 * passed function of type `(T => BePropertyMatchResult)`.
 *
 * @author Bill Venners
 */
object BePropertyMatcher {

  /**
   * Factory method that creates a `BePropertyMatcher[T]` from a
   * passed function of type `(T => BePropertyMatchResult)`.
   *
   * @author Bill Venners
   */
  def apply[T](fun: T => BePropertyMatchResult)(implicit ev: ClassTag[T]): BePropertyMatcher[T] =
    new BePropertyMatcher[T] {
      def apply(left: T) = fun(left)
      override def toString: String = "BePropertyMatcher[" + ev.runtimeClass.getName + "](" + ev.runtimeClass.getName + " => BePropertyMatchResult)"
    }
}
