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
package org.scalatest.jmock

import org.jmock.Mockery
import org.jmock.lib.legacy.ClassImposteriser
import scala.reflect.ClassTag

/**
 * Class that wraps and manages the lifecycle of a single `org.jmock.Mockery` context object,
 * provides some basic syntax sugar for using <a href="http://www.jmock.org/" target="_blank">JMock</a>
 * in Scala.
 *
 * Using the JMock API directly, you first need a `Mockery` context object:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val context = new Mockery
 * }}}
 *
 * `JMockCycle` uses jMock's `ClassImposterizer` to support mocking of classes, so the following line
 * would also be needed if you wanted that functionality as well:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * context.setImposteriser(ClassImposteriser.INSTANCE)
 * }}}
 *
 * When using this class, you would instead create an instance of this class (which will create and
 * wrap a `Mockery` object) and import its members, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val cycle = new JMockCycle
 * import cycle._
 * }}}
 *
 * Using the JMock API directly, you would create a mock object like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val mockCollaborator = context.mock(classOf[Collaborator])
 * }}}
 *
 * Having imported the members of an instance of this class, you can shorten that to:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val mockCollaborator = mock[Collaborator]
 * }}}
 *
 * After creating mocks, you set expectations on them, using syntax like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * context.checking(
 *   new Expectations() {
 *     oneOf (mockCollaborator).documentAdded("Document")
 *     exactly(3).of (mockCollaborator).documentChanged("Document")
 *    }
 *  )
 * }}}
 *
 * Having imported the members of an instance of this class, you can shorten this step to:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * expecting { e => import e._
 *   oneOf (mockCollaborator).documentAdded("Document")
 *   exactly(3).of (mockCollaborator).documentChanged("Document")
 * }
 * }}}
 *
 * The `expecting` method will create a new `Expectations` object, pass it into
 * the function you provide, which sets the expectations. After the function returns, the `expecting`
 * method will pass the `Expectations` object to the `checking`
 * method of its internal `Mockery` context.
 * 
 *
 * The `expecting` method passes an instance of class
 * `org.scalatest.mock.JMockExpectations` to the function you pass into
 * `expectations`. `JMockExpectations` extends `org.jmock.Expectations` and
 * adds several overloaded `withArg` methods. These `withArg` methods simply
 * invoke corresponding `with` methods on themselves. Because `with` is
 * a keyword in Scala, to invoke these directly you must surround them in back ticks, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * oneOf (mockCollaborator).documentAdded(`with`("Document"))
 * }}}
 *
 * By importing the members of the passed `JMockExpectations` object, you can
 * instead call `withArg` with no back ticks needed:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * oneOf (mockCollaborator).documentAdded(withArg("Document"))
 * }}}
 *
 * Once you've set expectations on the mock objects, when using the JMock API directly, you use the mock, then invoke
 * `assertIsSatisfied` on the `Mockery` context to make sure the mock
 * was used in accordance with the expectations you set on it. Here's how that looks:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * classUnderTest.addDocument("Document", new Array[Byte](0))
 * classUnderTest.addDocument("Document", new Array[Byte](0))
 * classUnderTest.addDocument("Document", new Array[Byte](0))
 * classUnderTest.addDocument("Document", new Array[Byte](0))
 * context.assertIsSatisfied()
 * }}}
 *
 * This class enables you to use the following, more declarative syntax instead:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * whenExecuting {
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 * }
 * }}}
 *
 * The `whenExecuting` method will execute the passed function, then
 * invoke `assertIsSatisfied` on its internal `Mockery`
 * context object.
 * 
 *
 * To summarize, here's what a typical test using `JMockCycle` looks like:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val cycle = new JMockCycle
 * import cycle._
 *
 * val mockCollaborator = mock[Collaborator]
 *
 * expecting { e => import e._
 *   oneOf (mockCollaborator).documentAdded("Document")
 *   exactly(3).of (mockCollaborator).documentChanged("Document")
 * }
 *
 * whenExecuting {
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 * }
 * }}}
 *
 * ScalaTest also provides a <a href="JMockCycleFixture.html">`JMockCycleFixture`</a> trait, which
 * will pass a new `JMockCycle` into each test that needs one.
 * 
 *
 * @author Bill Venners
 */
final class JMockCycle {

  private val context = new Mockery
  context.setImposteriser(ClassImposteriser.INSTANCE)

  /**
   * Invokes the `mock` method on this `JMockCycle`'s internal
   * `Mockery` context object, passing in a class instance for the
   * specified type parameter.
   *
   * Using the JMock API directly, you create a mock with:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = context.mock(classOf[Collaborator])
   * }}}
   *
   * Having imported the members of an instance of this class, you can shorten that to:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = mock[Collaborator]
   * }}}
   */
  def mock[T <: AnyRef](implicit classTag: ClassTag[T]): T = {
    context.mock(classTag.runtimeClass.asInstanceOf[Class[T]])
  }

  /**
   * Sets expectations on mock objects.
   *
   * After creating mocks, you set expectations on them, using syntax like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * context.checking(
   *   new Expectations() {
   *     oneOf (mockCollaborator).documentAdded("Document")
   *     exactly(3).of (mockCollaborator).documentChanged("Document")
   *    }
   *  )
   * }}}
   *
   * Having imported the members of an instance of this class, you can shorten this step to:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * expecting { e => import e._
   *   oneOf (mockCollaborator).documentAdded("Document")
   *   exactly(3).of (mockCollaborator).documentChanged("Document")
   * }
   * }}}
   *
   * The `expecting` method will create a new `Expectations` object, pass it into
   * the function you provide, which sets the expectations. After the function returns, the `expecting`
   * method will pass the `Expectations` object to the `checking`
   * method of its internal `Mockery` context.
   * 
   *
   * This method passes an instance of class `org.scalatest.mock.JMockExpectations` to the
   * passed function. `JMockExpectations` extends `org.jmock.Expectations` and
   * adds several overloaded `withArg` methods. These `withArg` methods simply
   * invoke corresponding `with` methods on themselves. Because `with` is
   * a keyword in Scala, to invoke these directly you must surround them in back ticks, like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * oneOf (mockCollaborator).documentAdded(`with`("Document"))
   * }}}
   *
   * By importing the members of the passed `JMockExpectations` object, you can
   * instead call `withArg` with no back ticks needed:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * oneOf (mockCollaborator).documentAdded(withArg("Document"))
   * }}}
   *
   * @param fun a function that sets expectations on the passed `JMockExpectations`
   *    object
   */
  def expecting(fun: JMockExpectations => Unit): Unit = {
    val e = new JMockExpectations
    fun(e)
    context.checking(e)
  }

  /**
   * Executes code using mocks with expectations set.
   * 
   * Once you've set expectations on the mock objects, when using the JMock API directly, you use the mock, then invoke
   * `assertIsSatisfied` on the `Mockery` context to make sure the mock
   * was used in accordance with the expectations you set on it. Here's how that looks:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * context.assertIsSatisfied()
   * }}}
   *
   * This class enables you to use the following, more declarative syntax instead:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * whenExecuting {
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   * }
   * }}}
   *
   * The `whenExecuting` method will execute the passed function, then
   * invoke `assertIsSatisfied` on its internal `Mockery`
   * context object.
   * 
   *
   * @param fun the code to execute under previously set expectations
   * @throws org.mock.ExpectationError if an expectation is not met
   */
  def whenExecuting(fun: => Unit): Unit = {
    fun
    context.assertIsSatisfied()
  }
}
