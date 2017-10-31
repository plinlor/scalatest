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

/**
 * Class whose subclasses can be used to tag tests in style traits in which tests are defined as functions.
 *
 * ScalaTest has two ways to tag tests: annotations and instances of this `Tag` class.
 * To tag a test method or an entire test class, you use a ''tag annotation'', whereas to tag a test function,
 * you use a `Tag` object. Though not required, it is usually a good idea to define both an annotation
 * and a corresponding `Tag` object for each conceptual tag you want, so you can tag anything: test functions, test classes,
 * and test methods. The name of the conceptual tag is the fully qualified name of the annotation interface, so you must
 * pass this name to the `Tag` constructor.
 * 
 *
 * For example, imagine you want to tag integration tests that use the actual database, and are, therefore, generally slower. You could
 * create a tag annotation and object called `DbTest`. To give them both the same simple name, you can declare them in different packages.
 * The tag annotation must be written in Java, not Scala, because annotations written
 * in Scala are not accessible at runtime. Here's an example:
 * 
 *
 * {{{
 * package com.mycompany.myproject.testing.tags;
 *
 * import java.lang.annotation.*; 
 * import org.scalatest.TagAnnotation
 *
 * @TagAnnotation
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target({ElementType.METHOD, ElementType.TYPE})
 * public @interface DbTest {}
 * }}}
 *
 * Given this annotation's fully qualified name is `com.mycompany.myproject.testing.tags.DbTest` the corresponding `Tag`
 * object decaration must have that name passed to its constructor, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package com.mycompany.myproject.testing.tagobjects
 *
 * object DbTest extends Tag("com.mycompany.myproject.testing.tags.DbTest")
 * }}}
 *
 * Given these definitions, you could tag a test function as a `DbTest` in, for
 * example, a <a href="FlatSpec.html">`FlatSpec`</a> like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.FlatSpec
 * import com.mycompany.myproject.testing.tagobjects.DbTest
 *
 * class ExampleSpec extends FlatSpec {
 *
 *   "Integration tests" can "sometimes be slow" taggedAs(DbTest) in {
 *     Thread.sleep(1000)
 *   }
 * }
 * }}}
 *
 * You could tag a test method as a `DbTest` in, for
 * example, a <a href="Suite.html">`Suite`</a> like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.Suite
 * import com.mycompany.myproject.testing.tags.DbTest
 *
 * class ExampleSuite extends Suite {
 *
 *   @DbTest
 *   def &#96;integration tests can sometimes be slow&#96; {
 *     Thread.sleep(1000)
 *   }
 * }
 * }}}
 *
 * And you could tag all the tests in an entire test class by annotating the class, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.FlatSpec
 * import com.mycompany.myproject.testing.tags.DbTest
 *
 * @DBTest
 * class ExampleSpec extends FlatSpec {
 *
 *   "Integration tests" can "sometimes be slow" in {
 *     Thread.sleep(1000)
 *   }
 *
 *   they should "likely sometimes be excluded " in {
 *     Thread.sleep(1000)
 *   }
 * }
 * }}}
 *
 * In the previous example, both tests will be tagged as `DBTest`s even though the
 * tests are not tagged as such individually. 
 * 
 *
 * When you run ScalaTest and want to either include or exclude `DbTest`s, you'd give the fully qualified
 * name of the tag annotation (which is also the name passed to the corresponding `Tag` constructor) to <a href="tools/Runner$.html">`Runner`</a>. For
 * example, here's how you'd exclude `DbTest`s on the `Runner` command line:
 * 
 *
 * {{{
 * -l com.mycompany.myproject.testing.tags.DbTest
 * }}}
 *
 * For examples of tagging in other style traits, see the "Tagging tests" section in the documentation for the trait:
 * 
 *
 * <ul>
 * <li><a href="FeatureSpec.html#taggingTests">Tagging `FeatureSpec` tests</a></li>
 * <li><a href="FlatSpec.html#taggingTests">Tagging `FlatSpec` tests</a></li>
 * <li><a href="FreeSpec.html#taggingTests">Tagging `FreeSpec` tests</a></li>
 * <li><a href="FunSpec.html#taggingTests">Tagging `FunSpec` tests</a></li>
 * <li><a href="FunSuite.html#taggingTests">Tagging `FunSuite` tests</a></li>
 * <li><a href="PropSpec.html#taggingTests">Tagging `PropSpec` tests</a></li>
 * <li><a href="Spec.html#taggingTests">Tagging `Spec` tests</a></li>
 * <li><a href="WordSpec.html#taggingTests">Tagging `WordSpec` tests</a></li>
 * </ul>
 *
 * @author Bill Venners
 * @author George Berger
 */
class Tag(val name: String)

/**
 * Companion object for `Tag`, which offers a factory method.
 *
 * @author George Berger
 * @author Bill Venners
 */
object Tag {

  /**
   * Factory method for creating new `Tag` objects.
   */
  def apply(name: String): Tag = {
    new Tag(name)
  }
}

