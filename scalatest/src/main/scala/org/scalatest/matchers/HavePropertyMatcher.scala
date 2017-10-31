/* * Copyright 2001-2013 Artima, Inc.
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

// T is the type of the object that has a property to verify with an instance of this trait, P is the type of that particular property
// Since I should be able to pass 
/**
 * Trait extended by matcher objects, which may appear after the word `have`, that can match against a 
 * property of the type specified by the `HavePropertyMatcher`'s second type parameter `P`.
 * `HavePropertyMatcher`'s first type parameter, `T`, specifies the type that declares the property. The match will succeed if and
 * only if the value of the property equals the specified value.
 * The object containing the property
 * is passed to the `HavePropertyMatcher`'s
 * `apply` method. The result is a `HavePropertyMatchResult[P]`.
 * A `HavePropertyMatcher` is, therefore, a function from the specified type, `T`, to
 * a `HavePropertyMatchResult[P]`.
 *
 * Although `HavePropertyMatcher`
 * and `Matcher` represent similar concepts, they have no inheritance relationship
 * because `Matcher` is intended for use right after `should` or `must`
 * whereas `HavePropertyMatcher` is intended for use right after `have`.
 * 
 *
 * A `HavePropertyMatcher` essentially allows you to write statically typed
 * property assertions similar to the dynamic ones that use symbols:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * book should have ('title ("Moby Dick")) // dynamic: uses reflection
 * book should have (title ("Moby Dick"))  // type safe: only works on Books; no reflection used
 * }}}
 *
 * One good way to organize custom matchers is to place them inside one or more traits that
 * you can then mix into the suites or specs that need them. Here's an example that
 * includes two methods that produce `HavePropertyMatcher`s:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * case class Book(val title: String, val author: String)
 *
 * trait CustomMatchers {
 * 
 *   def title(expectedValue: String) =
 *     new HavePropertyMatcher[Book, String] {
 *       def apply(book: Book) =
 *         HavePropertyMatchResult(
 *           book.title == expectedValue,
 *           "title",
 *           expectedValue,
 *           book.title
 *         )
 *     }
 *
 *   def author(expectedValue: String) = 
 *     new HavePropertyMatcher[Book, String] {
 *       def apply(book: Book) =
 *         HavePropertyMatchResult(
 *           book.author == expectedValue,
 *           "author",
 *           expectedValue,
 *           book.author
 *         )
 *     }
 * }
 * }}}
 * 
 * Each time the `title` method is called, it returns a new `HavePropertyMatcher[Book, String]` that
 * can be used to match against the `title` property of the `Book` passed to its `apply`
 * method. Because the type parameter of these two `HavePropertyMatcher`s is `Book`, they 
 * can only be used with instances of that type. (The compiler will enforce this.) The match will succeed if the
 * `title` property equals the value passed as `expectedValue`.
 * If the match succeeds, the `matches` field of the returned `HavePropertyMatchResult` will be `true`.
 * The second field, `propertyName`, is simply the string name of the property.
 * The third and fourth fields, `expectedValue` and `actualValue` indicate the expected and actual
 * values, respectively, for the property.
 * Here's an example that uses these `HavePropertyMatchers`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class ExampleSpec extends RefSpec with Matchers with CustomMatchers {
 * 
 *   describe("A book") {
 * 
 *     it("should have the correct title and author") {
 * 
 *       val book = Book("Moby Dick", "Melville")
 * 
 *       book should have (
 *         title ("Moby Dick"),
 *         author ("Melville")
 *       )
 *     }
 *   }
 * }
 * }}}
 *
 * These matches should succeed, but if for example the first property, `title ("Moby Dick")`, were to fail, you would get an error message like:
 * 
 *
 * {{{ class="stExamples">
 * The title property had value "A Tale of Two Cities", instead of its expected value "Moby Dick",
 * on object Book(A Tale of Two Cities,Dickens)
 * }}}
 *
 * For more information on `HavePropertyMatchResult` and the meaning of its fields, please
 * see the documentation for <a href="HavePropertyMatchResult.html">`HavePropertyMatchResult`</a>. To understand why `HavePropertyMatcher`
 * is contravariant in its type parameter, see the section entitled "Matcher's variance" in the
 * documentation for <a href="../Matcher.html">`Matcher`</a>.
 * 
 *
 * @author Bill Venners
*/
trait HavePropertyMatcher[-T, P] extends Function1[T, HavePropertyMatchResult[P]] {

  thisHavePropertyMatcher =>

  /**
   * Check to see if a property on the specified object, `objectWithProperty`, matches its
   * expected value, and report the result in
   * the returned `HavePropertyMatchResult`. The `objectWithProperty` is
   * usually the value to the left of a `should` or `must` invocation. For example, `book`
   * would be passed as the `objectWithProperty` in:
   *
   * {{{  <!-- class="stHighlight" -->
   * book should have (title ("Moby Dick"))
   * }}}
   *
   * @param objectWithProperty the object with the property against which to match
   * @return the `HavePropertyMatchResult` that represents the result of the match
   */
  def apply(objectWithProperty: T): HavePropertyMatchResult[P]

  /**
   * Compose this `HavePropertyMatcher` with the passed function, returning a new `HavePropertyMatcher`.
   *
   * This method overrides `compose` on `Function1` to
   * return a more specific function type of `HavePropertyMatcher`.
   * 
   */
  override def compose[U](g: U => T): HavePropertyMatcher[U, P] =
    new HavePropertyMatcher[U, P] {
      def apply(u: U) = thisHavePropertyMatcher.apply(g(u))
    }
}

/**
 * Companion object for trait `HavePropertyMatcher` that provides a
 * factory method that creates a `HavePropertyMatcher[T]` from a
 * passed function of type `(T =&gt; HavePropertyMatchResult)`.
 *
 * @author Bill Venners
 */
object HavePropertyMatcher {

  /**
   * Factory method that creates a `HavePropertyMatcher[T]` from a
   * passed function of type `(T =&gt; HavePropertyMatchResult)`.
   *
   * This allows you to create a `HavePropertyMatcher` in a slightly
   * more concise way, for example:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   *  case class Person(name: String)
   *  def name(expectedName: String) = {
   *    HavePropertyMatcher { 
   *      (person: Person) =&gt; HavePropertyMatchResult(
   *        person.name == expectedName,
   *        "name",
   *        expectedName,
   *        person.name
   *      ) 
   *    } 
   * }}}
   *
   * @author Bill Venners
   */
  def apply[T, P](fun: T => HavePropertyMatchResult[P])(implicit evT: ClassTag[T], evP: ClassTag[P]): HavePropertyMatcher[T, P] =
    new HavePropertyMatcher[T, P] {
      def apply(left: T) = fun(left)
      override def toString: String = "HavePropertyMatcher[" + evT.runtimeClass.getName + ", " + evP.runtimeClass.getName + "](" + evT.runtimeClass.getName + " => HavePropertyMatchResult[" + evP.runtimeClass.getName + "])"
    }
}
