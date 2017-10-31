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
package org.scalatest.mockito

import org.mockito.Mockito.{mock => mockitoMock}
import reflect.ClassTag
import org.mockito.stubbing.Answer
import org.mockito.MockSettings

/**
 * Trait that provides some basic syntax sugar for <a href="http://mockito.org/" target="_blank">Mockito</a>.
 *
 * Using the Mockito API directly, you create a mock with:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val mockCollaborator = mock(classOf[Collaborator])
 * }}}
 *
 * Using this trait, you can shorten that to:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val mockCollaborator = mock[Collaborator]
 * }}}
 *
 * This trait also provides shorthands for the three other (non-deprecated) overloaded `mock` methods,
 * which allow you to pass in a default answer, a name, or settings.
 * 
 *
 * @author Bill Venners
 * @author Chua Chee Seng
 */
trait MockitoSugar {

  /**
   * Invokes the `mock(classToMock: Class[T])` method on the `Mockito` companion object (''i.e.'', the
   * static `mock(java.lang.Class<T> classToMock)` method in Java class `org.mockito.Mockito`).
   *
   * Using the Mockito API directly, you create a mock with:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = mock(classOf[Collaborator])
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
    mockitoMock(classTag.runtimeClass.asInstanceOf[Class[T]])
  }
  
  /**
   * Invokes the `mock(classToMock: Class[T], defaultAnswer: Answer[_])` method on the `Mockito` companion object (''i.e.'', the
   * static `mock(java.lang.Class<T> classToMock, org.mockito.stubbing.Answer defaultAnswer)` method in Java class `org.mockito.Mockito`).
   *
   * Using the Mockito API directly, you create a mock with:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = mock(classOf[Collaborator], defaultAnswer)
   * }}}
   *
   * Using this method, you can shorten that to:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = mock[Collaborator](defaultAnswer)
   * }}}
   */
  def mock[T <: AnyRef](defaultAnswer: Answer[_])(implicit classTag: ClassTag[T]): T = {
    mockitoMock(classTag.runtimeClass.asInstanceOf[Class[T]], defaultAnswer)
  }
  
  /**
   * Invokes the `mock(classToMock: Class[T], mockSettings: MockSettings)` method on the `Mockito` companion object (''i.e.'', the
   * static `mock(java.lang.Class<T> classToMock, org.mockito.MockSettings mockSettings)` method in Java class `org.mockito.Mockito`).
   *
   * Using the Mockito API directly, you create a mock with:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = mock(classOf[Collaborator], mockSettings)
   * }}}
   *
   * Using this method, you can shorten that to:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = mock[Collaborator](mockSettings)
   * }}}
   */
  def mock[T <: AnyRef](mockSettings: MockSettings)(implicit classTag: ClassTag[T]): T = {
    mockitoMock(classTag.runtimeClass.asInstanceOf[Class[T]], mockSettings)
  }
  
  /**
   * Invokes the `mock(classToMock: Class[T], name: String)` method on the `Mockito` companion object (''i.e.'', the
   * static `mock(java.lang.Class<T> classToMock, java.lang.String name)` method in Java class `org.mockito.Mockito`).
   *
   * Using the Mockito API directly, you create a mock with:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = mock(classOf[Collaborator], name)
   * }}}
   *
   * Using this method, you can shorten that to:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val mockCollaborator = mock[Collaborator](name)
   * }}}
   */
  def mock[T <: AnyRef](name: String)(implicit classTag: ClassTag[T]): T = {
    mockitoMock(classTag.runtimeClass.asInstanceOf[Class[T]], name)
  }
}

/**
 * Companion object that facilitates the importing of `MockitoSugar` members as 
 * an alternative to mixing it in. One use case is to import `MockitoSugar` members so you can use
 * them in the Scala interpreter.
 */
// TODO: Fill in an example
object MockitoSugar extends MockitoSugar

