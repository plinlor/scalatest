/*
 * Copyright 2001-2014 Artima, Inc.
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

import scala.xml.{Text, Node, Elem, NodeSeq}
import org.scalactic.Uniformity

/**
 * Trait providing a `streamlined` method that returns a <a href="../scalactic/Uniformity.html">`Uniformity[T]`</a>
 * instance for any subtype of `scala.xml.NodeSeq` that will normalize the XML by removing empty text nodes and trimming
 * non-empty text nodes.
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
 * The `Uniformity` returned by this trait's `streamlined` method would transform
 * the above XML to:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * &lt;summer&gt;&lt;day&gt;&lt;/day&gt;&lt;night&gt;with lots of stars&lt;/night&gt;&lt;/summer&gt;
 * }}}
 *
 * The `streamlined` method can be used with the <a href="../scalactic/Explicitly.html">`Explicitly`</a> DSL, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * xmlElem should equal (
 *   &lt;summer&gt;
 *     &lt;day&gt;&lt;/day&gt;
 *     &lt;night&gt;
 *       with lots of stars
 *     &lt;/night&gt;
 *   &lt;/summer&gt;
 * ) (after being streamlined[Elem])
 * }}}
 *
 * The goal of this trait is to provide a normalization for XML that makes it easier to test XML objects for equality.
 * White space is significant in XML, and is taken into account by the default equality for XML, accessed
 * by invoking the `==` method on an XML `NodeSeq`. Here's an example:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; val xmlElem = &lt;summer&gt;&lt;day&gt;&lt;/day&gt;&lt;night&gt;with lots of stars&lt;/night&gt;&lt;/summer&gt;
 * xmlElem: scala.xml.Elem = &lt;summer&gt;&lt;day&gt;&lt;/day&gt;&lt;night&gt;with lots of stars&lt;/night&gt;&lt;/summer&gt;
 *
 * scala&gt; xmlElem == &lt;summer&gt;
 *      |   &lt;day&gt;&lt;/day&gt;
 *      |   &lt;night&gt;
 *      |     with lots of stars
 *      |   &lt;/night&gt;
 *      | &lt;/summer&gt;
 * res1: Boolean = false
 * }}}
 *
 * The above equality comparison produces false because of whitespace differences in the XML.
 * When such whitespace differences are unimportant to the actual application, it can make it
 * easier to write readable test code if you can compare XML for equality without taking
 * into account empty text nodes, or leading and trailing whitespace in nonempty text nodes.
 * The `streamlined` method of this trait provides a `Uniformity`
 * instance that does just that:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalactic._
 * import org.scalactic._
 *
 * scala&gt; import Explicitly._
 * import Explicitly._
 *
 * scala&gt; import TripleEquals._
 * import TripleEquals._
 *
 * scala&gt; import org.scalatest.StreamlinedXml._
 * import StreamlinedXml._
 *
 * scala&gt; import scala.xml.Elem
 * import scala.xml.Elem * 
 * 
 * scala&gt; (xmlElem === &lt;summer&gt;
 *      |   &lt;day&gt;&lt;/day&gt;
 *      |   &lt;night&gt;
 *      |     with lots of stars
 *      |   &lt;/night&gt;
 *      | &lt;/summer&gt;) (after being streamlined[Elem])
 * res9: Boolean = true
 * }}}
 *
 * @author Bill Venners
 */
trait StreamlinedXml {

  /**
   * Provides a <a href="../scalactic/Uniformity.html">`Uniformity[T]`</a>
   * instance for any subtype of `scala.xml.NodeSeq` that will normalize the XML by removing empty text nodes and trimming
   * non-empty text nodes.
   *
   * The purpose of this `Uniformity` is to make it easier to write readable test
   * code that compares XML for equality. See the main documentation for this trait for more
   * details and examples.
   * 
   *
   * @return a `Uniformity[T]` instance that normalizes XML for testing
   */
  def streamlined[T <: NodeSeq]: Uniformity[T] = {

    def trimTextZappingEmpty(node: Node): Seq[Node] =
      node match {
        case Text(text) if (text.trim.isEmpty) => Nil
        case Text(text) => List(Text(text.trim))
        case Elem(pre, lab, md, scp, children @ _*) =>
          Elem(pre, lab, md, scp, false, (children.flatMap(trimTextZappingEmpty)):_*)
        case _ => List(node)
      }

    new Uniformity[T] {
      def normalized(nodeSeq: T): T =
        nodeSeq match {
          case Elem(pre, lab, md, scp, children @ _*) =>
            val mergedTextNodes = // Merge adjacent text nodes
              children.foldLeft(Nil: List[Node]) { (acc, ele) =>
                ele match {
                  case eleTxt: Text =>
                    acc.headOption match {
                      case Some(accTxt: Text) =>
                        Text(accTxt.text + eleTxt.text) :: acc.tail
                      case _ => ele :: acc
                    }
                  case _ => ele :: acc
                }
              }
            Elem(pre, lab, md, scp, false, (mergedTextNodes.flatMap(trimTextZappingEmpty)):_*).asInstanceOf[T]
          case _ => nodeSeq
        }

      /**
       * Returns true if the passed `Any` is a `Elem`.
       *
       * @return true if the passed `Any` is a `Elem`.
       */
      final def normalizedCanHandle(b: Any): Boolean = b.isInstanceOf[NodeSeq]
    
      /**
       * Normalizes the passed object if it is a `Elem`.
       *
       * This method returns either:
       * 
       *
       * <ul>
       * <li>if the passed object is a `Elem`, the result of passing that string to `normalized`</li>
       * <li>else, the same exact object that was passed
       * 
       *
       * @return a normalized form of any passed `Elem`, or the same object if not a `Elem`.
       */
      final def normalizedOrSame(b: Any): Any =
        b match {
          case s: NodeSeq => StreamlinedXml.streamlined[NodeSeq].normalized(s)
          case _ => b
       }
    }
  }
}

/**
 * Companion object that facilitates the importing of `StreamlinedXml` members as 
 * an alternative to mixing it the trait. One use case is to import `StreamlinedXml` members so you can use
 * them in the Scala interpreter.
 *
 * @author Bill Venners
 */
object StreamlinedXml extends StreamlinedXml
