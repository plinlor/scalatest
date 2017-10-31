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
package org.scalatest.easymock

import org.easymock.IExpectationSetters
import scala.reflect.ClassTag
import org.easymock.EasyMock
import org.easymock.EasyMock.{expect => easyMockExpect, expectLastCall}

/**
 * Trait that provides some basic syntax sugar for <a href="http://easymock.org/" target="_blank">EasyMock</a>.
 *
 * Using the EasyMock API directly, you create a mock with:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val mockCollaborator = createMock(classOf[Collaborator])
 * }}}
 *
 * With this trait, you can shorten that to:
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
 * mockCollaborator.documentAdded("Document")
 * mockCollaborator.documentChanged("Document")
 * expectLastCall().times(3)
 * }}}
 *
 * If you wish to highlight which statements are setting expectations on the mock (versus
 * which ones are actually using the mock), you can place them in an `expecting`
 * clause, provided by this trait, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * expecting {
 *   mockCollaborator.documentAdded("Document")
 *   mockCollaborator.documentChanged("Document")
 *   lastCall.times(3)
 * }
 * }}}
 *
 * Using an `expecting` clause is optional, because it does nothing but visually indicate
 * which statements are setting expectations on mocks. (Note: this trait also provides the `lastCall`
 * method, which just calls `expectLastCall`.)
 * 
 *
 * Once you've set expectations on the mock objects, you must invoke `replay` on
 * the mocks to indicate you are done setting expectations, and will start using the mock.
 * After using the mock, you must invoke `verify` to check to make sure the mock
 * was used in accordance with the expectations you set on it. Here's how that looks when you
 * use the EasyMock API directly:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * replay(mockCollaborator)
 * classUnderTest.addDocument("Document", new Array[Byte](0))
 * classUnderTest.addDocument("Document", new Array[Byte](0))
 * classUnderTest.addDocument("Document", new Array[Byte](0))
 * classUnderTest.addDocument("Document", new Array[Byte](0))
 * verify(mockCollaborator)
 * }}}
 *
 * This trait enables you to use the following, more declarative syntax instead:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * whenExecuting(mockCollaborator) {
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 * }
 * }}}
 *
 * The `whenExecuting` method will pass the `mockCollaborator` to
 * `replay`, execute the passed function (your code that uses the mock), and
 * call `verify`, passing in the `mockCollaborator`. If you want to
 * use multiple mocks, you can pass multiple mocks to `whenExecuting`.
 * 
 *
 * To summarize, here's what a typical test using `EasyMockSugar` looks like:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val mockCollaborator = mock[Collaborator]
 *
 * expecting {
 *   mockCollaborator.documentAdded("Document")
 *   mockCollaborator.documentChanged("Document")
 *   lastCall.times(3)
 * }
 *
 * whenExecuting(mockCollaborator) {
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 *   classUnderTest.addDocument("Document", new Array[Byte](0))
 * }
 * }}}
 *
 * An alternative approach is to place your mock objects in a `MockObjects` holder object referenced
 * from an implicit `val`, then use the overloaded variant of `whenExecuting` that
 * takes an implicit `MockObjects` parameter. Here's how that would look:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * implicit val mocks = MockObjects(mock[Collaborator])
 *
 * expecting {
 *   mockCollaborator.documentAdded("Document")
 *   mockCollaborator.documentChanged("Document")
 *   lastCall.times(3)
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
 * Note: As of ScalaTest 1.3, this trait supports EasyMock 3, with no dependencies on EasyMock class extension.
 * 
 *
 * @author Bill Venners
 * @author George Berger
 */
trait EasyMockSugar {

  import scala.language.implicitConversions

  /**
   * Implicit conversion that invokes the `expect` method on the `EasyMock` companion object (''i.e.'', the
   * static `expect` method in Java class `org.easymock.EasyMock`).
   *
   * In a ScalaTest `Suite`, the `expect` method defined in `Assertions`, and inherited by `Suite`,
   * interferes with the `expect` method if imported from `EasyMock`. You can invoke it by qualifying it, ''i.e.'',
   * `EasyMock.expect`, or by changing its name on import, like this:
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.easymock.EasyMock.{expect => easyMockExpect, _}
   * }}}
   *
   * But if you mix in this trait, you can just invoke `call` instead.
   * 
   *
   * You can use this method, for example, to chain expectations like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * expecting {
   *   call(mock.getName).andReturn("Ben Franklin")
   * }
   * }}}
   *
   * Note: the name of this methods is `call`, not `expectCall` because
   * "expect" appears in the surrounding `expecting` clause provided by this trait.
   * 
   *
   * Moreover, because this method is marked `implicit`, you will usually be able to simply
   * leave it off. So long as the result of the method call you are expecting doesn't have
   * a method that satisfies the subsequent invocation (such as `andReturn` in this
   * example), the Scala compiler will invoke `call` for you
   * implicitly. Here's how that looks:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * expecting {
   *   mock.getName.andReturn("Ben Franklin")
   * }
   * }}}
   *
   * @param value - the result of invoking a method on mock prior to invoking `replay`.
   */
  implicit def call[T](value: T): IExpectationSetters[T] = easyMockExpect(value)

  /**
   * Invokes the `expectLastCall` method on the `EasyMock` companion object (''i.e.'', the
   * static `expect` method in Java class `org.easymock.EasyMock`).
   *
   * This method is provided simply to allow you to avoid repeating "expect" inside an
   * `expecting` clause. Here's an example that uses the `expectLastCall` directly
   * to express the expectation that the `getName` method will be invoked three times
   * on a mock, each time returning `"Ben Franklin"`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * expecting {
   *   mock.getName.andReturn("Ben Franklin")
   *   expectLastCall.times(3)
   * }
   * }}}
   *
   * Using this method, you can compress this to:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * expecting {
   *   mock.getName.andReturn("Ben Franklin")
   *   lastCall.times(3)
   * }
   * }}}
   */
  def lastCall[T]: IExpectationSetters[T] = expectLastCall()

  /**
   * Invokes the `createMock` method on the `EasyMock` companion object (''i.e.'', the
   * static `createMock` method in Java class `org.easymock.classextension.EasyMock`).
   *
   * Using the EasyMock API directly, you create a mock with:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = createMock(classOf[Collaborator])
   * }}}
   *
   * Using this method, you can shorten that to:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = mock[Collaborator]
   * }}}
   */
  def mock[T <: AnyRef](implicit classTag: ClassTag[T]): T = {
    EasyMock.createMock(classTag.runtimeClass.asInstanceOf[Class[T]])
  }

  /**
   * Invokes the `createStrictMock` method on the `EasyMock` companion object (''i.e.'', the
   * static `createStrictMock` method in Java class `org.easymock.classextension.EasyMock`).
   *
   * Using the EasyMock API directly, you create a strict mock with:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = createStrictMock(classOf[Collaborator])
   * }}}
   *
   * Using this trait, you can shorten that to:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = strictMock[Collaborator]
   * }}}
   */
  def strictMock[T <: AnyRef](implicit classTag: ClassTag[T]): T = {
    EasyMock.createStrictMock(classTag.runtimeClass.asInstanceOf[Class[T]])
  }

  /**
   * Invokes the `createNiceMock` method on the `EasyMock` companion object (''i.e.'', the
   * static `createNiceMock` method in Java class `org.easymock.classextension.EasyMock`).
   *
   * Using the EasyMock API directly, you create a nice mock with:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = createNiceMock(classOf[Collaborator])
   * }}}
   *
   * Using this trait, you can shorten that to:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = niceMock[Collaborator]
   * }}}
   */
  def niceMock[T <: AnyRef](implicit classTag: ClassTag[T]): T = {
    EasyMock.createNiceMock(classTag.runtimeClass.asInstanceOf[Class[T]])
  }

  /**
   * Provides a visual clue to readers of the code that a set of statements are expectations being
   * set on mocks.
   *
   * Using the EasyMock API directly, you set expectations on a mock object with syntax like:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * mockCollaborator.documentAdded("Document")
   * mockCollaborator.documentChanged("Document")
   * expectLastCall().times(3)
   * }}}
   *
   * This `expecting` method can make it more obvious which portion of your test code
   * is devoted to setting expectations on mock objects. For example:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * expecting {
   *   mockCollaborator.documentAdded("Document")
   *   mockCollaborator.documentChanged("Document")
   *   lastCall.times(3)
   * }
   * }}}
   *
   * Using an `expecting` clause is optional, because it does nothing besides visually indicate
   * which statements are setting expectations on mocks. Note: this trait also provides the `lastCall`
   * method, which just calls `expectLastCall`. This allows you to avoid writing "expect" twice.
   * Also, the reason `expecting` doesn't take a by-name parameter, execute that, then call
   * `replay` is because you would then need to pass your mock object or objects into
   * `expecting`. Since you already need to pass the mocks into `whenExecuting` so
   * that `verify` can be invoked on them, it yields more concise client code to have
   * `whenExecuting` invoke `replay` on the mocks first rather than having
   * `expecting` invoke `replay` last.
   * 
   */
  def expecting(unused: Any): Unit = ()

  /**
   * Invokes `replay` on the passed mock object or objects, executes the passed function, then invokes
   * `verify` on the passed mock object or objects.
   *
   * Once you've set expectations on some mock objects, you must invoke `replay` on
   * the mocks to indicate you are done setting expectations, and will start using the mocks.
   * After using the mocks, you must invoke `verify` to check to make sure the mocks
   * were used in accordance with the expectations you set on it. Here's how that looks when you
   * use the EasyMock API directly:
   * 
   *
   *
   * {{{  <!-- class="stHighlight" -->
   * replay(mock)
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * verify(mock)
   * }}}
   *
   * This method enables you to use the following, more declarative syntax instead:
   * 
   * 
   * {{{  <!-- class="stHighlight" -->
   * whenExecuting(mockCollaborator) {
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   * }
   * }}}
   *
   * If you are working with multiple mock objects at once, you simply pass
   * them all to `whenExecuting`, like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * whenExecuting(mock1, mock2, mock3) {
   *   // ...
   * }
   * }}}
   *
   * The `whenExecuting` method will first invoke `EasyMock.reply`
   * once for each mock you supplied, execute the passed function, then
   * invoke `EasyMock.verify` once for each mock you supplied. If an exception
   * is thrown by the passed function, `whenExecuting` will complete abruptly with
   * that same exception without executing verify on any of the mocks.
   * 
   *
   * @param mocks one or more mock objects to invoke `replay` before using and `verify` after using.
   * @throws IllegalArgumentException if no mocks are passed
   */
  def whenExecuting(mocks: AnyRef*)(fun: => Unit): Unit = {

    require(mocks.length > 0, "Must pass at least one mock to whenExecuting, but mocks.length was 0.") 

    for (m <- mocks)
      EasyMock.replay(m)

    fun

    // Don't put this in a try block, so that if fun throws an exception 
    // it propagates out immediately and shows up as the cause of the failed test
    for (m <- mocks)
      EasyMock.verify(m)
  }

  /**
   * Holder class for a collection of mocks that can be passed implicitly to one form of the
   * overloaded `whenExecuting` method.
   *
   * @param mocks one or more mock objects that you intend to pass to `whenExecuting`
   * @throws IllegalArgumentException if no mocks are passed
   */
  case class MockObjects(mocks: AnyRef*) {
    require(mocks.length > 0, "Must pass at least one mock to MockObjects constructor, but mocks.length was 0.") 
  }

  /**
   * Invokes `replay` on the mock object or objects passed via an implicit parameter,
   * executes the passed function, then invokes `verify` on the passed mock object or objects.
   *
   * Once you've set expectations on some mock objects, you must invoke `replay` on
   * the mocks to indicate you are done setting expectations, and will start using the mocks.
   * After using the mocks, you must invoke `verify` to check to make sure the mocks
   * were used in accordance with the expectations you set on it. Here's how that looks when you
   * use the EasyMock API directly:
   * 
   *
   *
   * {{{  <!-- class="stHighlight" -->
   * replay(mock)
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * classUnderTest.addDocument("Document", new Array[Byte](0))
   * verify(mock)
   * }}}
   *
   * This method enables you to use the following, more declarative syntax instead:
   * 
   * 
   * {{{  <!-- class="stHighlight" -->
   * implicit val mocks = MockObjects(mockCollaborator)
   *
   * whenExecuting {
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   *   classUnderTest.addDocument("Document", new Array[Byte](0))
   * }
   * }}}
   *
   * If you are working with multiple mock objects at once, you simply pass
   * them all to `MockObjects`, like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * implicit val mocks = MockObjects(mock1, mock2, mock3)
   * }}}
   *
   * The `whenExecuting` method will first invoke `EasyMock.reply`
   * once for each mock you supplied, execute the passed function, then
   * invoke `EasyMock.verify` once for each mock you supplied. If an exception
   * is thrown by the passed function, `whenExecuting` will complete abruptly with
   * that same exception without executing verify on any of the mocks.
   * 
   */
  def whenExecuting(fun: => Unit)(implicit mocks: MockObjects): Unit = {
    whenExecuting(mocks.mocks: _*)(fun)
  }
}

/**
 * Companion object that facilitates the importing of `EasyMockSugar` members as 
 * an alternative to mixing it in. One use case is to import `EasyMockSugar` members so you can use
 * them in the Scala interpreter.
 */
// TODO: Fill in an example
object EasyMockSugar extends EasyMockSugar

