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

import scala.xml.{NodeSeq}
import org.scalactic.{Equality, Uniformity}

/**
 * Trait providing an implicit <a href="../scalactic/Equality.html">`Equality[T]`</a> for subtypes 
 * of `scala.xml.NodeSeq` that before testing for equality, will normalize left and right sides 
 * by removing empty XML text nodes and trimming non-empty text nodes.
 *
 * Here's an example of some unnormalized XML:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * &lt;summer&gt;
 *   &lt;day&gt;&lt;/day&gt;
 *   &lt;night&gt;
 *     with lots of stars
 *   &lt;/night&gt;
 * &lt;/summer&gt;
 * }}}
 *
 * Prior to testing it for equality, the implicit `Equality[T]` provided by this trait would transform
 * the above XML to:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * &lt;summer&gt;&lt;day&gt;&lt;/day&gt;&lt;night&gt;with lots of stars&lt;/night&gt;&lt;/summer&gt;
 * }}}
 *
 * The goal of this trait is to provide an implicit `Equality` for XML that makes it easier to write tests involving XML.
 * White space is significant in XML, and is taken into account by the default equality for XML, accessed
 * by invoking the `==` method on an XML `NodeSeq`. Here's an example:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; val xmlElem = &lt;summer&gt;&lt;day&gt;&lt;/day&gt;&lt;night&gt;with lots of stars&lt;/night&gt;&lt;/summer&gt;
 * xmlElem: scala.xml.Elem = &lt;summer&gt;&lt;day&gt;&lt;/day&gt;&lt;night&gt;with lots of stars&lt;/night&gt;&lt;/summer&gt;
 *
 * scala&gt; import org.scalatest.Assertions._
 * import org.scalatest.Assertions._
 *
 * scala&gt; assert(xmlElem === &lt;summer&gt;
 *      |   &lt;day&gt;&lt;/day&gt;
 *      |     &lt;night&gt;
 *      |       with lots of stars
 *      |     &lt;/night&gt;
 *      |   &lt;/summer&gt;)
 * org.scalatest.exceptions.TestFailedException: &lt;summer&gt;&lt;day&gt;&lt;/day&gt;&lt;night&gt;with lots of stars&lt;/night&gt;&lt;/summer&gt; did not equal &lt;summer&gt;
 *   &lt;day&gt;&lt;/day&gt;
 *     &lt;night&gt;
 *       with lots of stars
 *     &lt;/night&gt;
 *   &lt;/summer&gt;
 *   at org.scalatest.Assertions$class.newAssertionFailedException(Assertions.scala:500)
 *   at org.scalatest.Assertions$.newAssertionFailedException(Assertions.scala:1538)
 *   at org.scalatest.Assertions$AssertionsHelper.macroAssert(Assertions.scala:466)
 *   ... 53 elided
 * }}}
 *
 * The above assertion fails because of whitespace differences in the XML.
 * When such whitespace differences are unimportant to the actual application, it can make it
 * easier to write readable test code if you can compare XML for equality without taking
 * into account empty text nodes, or leading and trailing whitespace in nonempty text nodes.
 * This trait provides an `Equality[T]`
 * instance that does just that:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest.StreamlinedXmlEquality._
 * import org.scalatest.StreamlinedXmlEquality._
 *
 * scala&gt; assert(xmlElem === &lt;summer&gt;
 *      |   &lt;day&gt;&lt;/day&gt;
 *      |   &lt;night&gt;
 *      |     with lots of stars
 *      |   &lt;/night&gt;
 *      | &lt;/summer&gt;)
 * }}}
 *
 * @author Bill Venners
 */
trait StreamlinedXmlEquality {

  /**
   * Provides an implicit <a href="../scalactic/Equality.html">`Equality[T]`</a>
   * instance for any subtype of `scala.xml.NodeSeq` that, prior for testing
   * for equality, will normalize the XML by removing empty text nodes and trimming
   * non-empty text nodes.
   *
   * See the main documentation for this trait for more details and examples.
   * 
   *
   * @return an `Equality[T]` instance that normalizes XML before testing for equality
   */
  implicit def streamlinedXmlEquality[T <: NodeSeq]: Equality[T] = {
    new Equality[T] {
      val xu: Uniformity[T] = StreamlinedXml.streamlined[T]
      def areEqual(a: T, b: Any): Boolean = {
        xu.normalized(a) == xu.normalizedOrSame(b)
      }
    }
  }
}

/**
 * Companion object that facilitates the importing of `StreamlinedXmlEquality` members as 
 * an alternative to mixing it the trait. One use case is to import `StreamlinedXmlEquality` members so you can use
 * them in the Scala interpreter.
 *
 * @author Bill Venners
 */
object StreamlinedXmlEquality extends StreamlinedXmlEquality

