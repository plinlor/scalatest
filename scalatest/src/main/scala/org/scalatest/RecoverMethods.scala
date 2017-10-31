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

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import scala.reflect.ClassTag
import org.scalactic.source
import org.scalatest.exceptions.StackDepthException._

import org.scalactic.source

/**
 * Offers two methods for transforming futures when exceptions are expected.
 *
 * This trait offers two methods for testing for expected exceptions in the context of
 * futures: `recoverToSucceededIf` and `recoverToExceptionIf`.
 * Because this trait is mixed into trait `AsyncTestSuite`, both of its methods are
 * available by default in any async-style suite.
 * 
 *
 * If you just want to ensure that a future fails with a particular exception type, and do
 * not need to inspect the exception further, use `recoverToSucceededIf`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * recoverToSucceededIf[IllegalStateException] { // Result type: Future[Assertion]
 *   emptyStackActor ? Peek
 * }
 * }}}
 *
 * The `recoverToSucceededIf` method performs a job similar to
 * <a href="Assertions.html#expectedExceptions">`assertThrows`</a>, except
 * in the context of a future. It transforms a `Future` of any type into a
 * `Future[Assertion]` that succeeds only if the original future fails with the specified
 * exception. Here's an example in the REPL:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; import org.scalatest.RecoverMethods._
 * import org.scalatest.RecoverMethods._
 * 
 * scala&gt; import scala.concurrent.Future
 * import scala.concurrent.Future
 * 
 * scala&gt; import scala.concurrent.ExecutionContext.Implicits.global
 * import scala.concurrent.ExecutionContext.Implicits.global
 * 
 * scala&gt; recoverToSucceededIf[IllegalStateException] {
 *      |   Future { throw new IllegalStateException }
 *      | }
 * res0: scala.concurrent.Future[org.scalatest.Assertion] = ...
 * 
 * scala&gt; res0.value
 * res1: Option[scala.util.Try[org.scalatest.Assertion]] = Some(Success(Succeeded))
 * }}}
 * 
 * Otherwise it fails with an error message similar to those given by `assertThrows`:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; recoverToSucceededIf[IllegalStateException] {
 *      |   Future { throw new RuntimeException }
 *      | }
 * res2: scala.concurrent.Future[org.scalatest.Assertion] = ...
 * 
 * scala&gt; res2.value
 * res3: Option[scala.util.Try[org.scalatest.Assertion]] =
 *     Some(Failure(org.scalatest.exceptions.TestFailedException: Expected exception
 *       java.lang.IllegalStateException to be thrown, but java.lang.RuntimeException
 *       was thrown))
 * 
 * scala&gt; recoverToSucceededIf[IllegalStateException] {
 *      |   Future { 42 }
 *      | }
 * res4: scala.concurrent.Future[org.scalatest.Assertion] = ...
 * 
 * scala&gt; res4.value
 * res5: Option[scala.util.Try[org.scalatest.Assertion]] =
 *     Some(Failure(org.scalatest.exceptions.TestFailedException: Expected exception
 *       java.lang.IllegalStateException to be thrown, but no exception was thrown))
 * }}}
 *
 * The `recoverToExceptionIf` method differs from the `recoverToSucceededIf` in
 * its behavior when the assertion succeeds: `recoverToSucceededIf` yields a `Future[Assertion]`,
 * whereas `recoverToExceptionIf` yields a `Future[T]`, where `T` is the
 * expected exception type.
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * recoverToExceptionIf[IllegalStateException] { // Result type: Future[IllegalStateException]
 *   emptyStackActor ? Peek
 * }
 * }}}
 *
 * In other words, `recoverToExpectionIf` is to
 * <a href="Assertions.html#expectedExceptions">`intercept`</a> as
 * `recovertToSucceededIf` is to `assertThrows`. The first one allows you to perform further
 * assertions on the expected exception. The second one gives you a result type that will satisfy the type checker
 * at the end of the test body. Here's an example showing `recoverToExceptionIf` in the REPL:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * scala&gt; val futureEx =
 *      |   recoverToExceptionIf[IllegalStateException] {
 *      |     Future { throw new IllegalStateException("hello") }
 *      |   }
 * futureEx: scala.concurrent.Future[IllegalStateException] = ...
 * 
 * scala&gt; futureEx.value
 * res6: Option[scala.util.Try[IllegalStateException]] =
 *     Some(Success(java.lang.IllegalStateException: hello))
 * 
 * scala&gt; futureEx map { ex =&gt; assert(ex.getMessage == "world") }
 * res7: scala.concurrent.Future[org.scalatest.Assertion] = ...
 * 
 * scala&gt; res7.value
 * res8: Option[scala.util.Try[org.scalatest.Assertion]] =
 *     Some(Failure(org.scalatest.exceptions.TestFailedException: "[hello]" did not equal "[world]"))
 * }}}
 *
 * @author Bill Venners
 */
trait RecoverMethods {

  /**
   * Transforms a future of any type into a `Future[T]`, where `T` is a given
   * expected exception type, which succeeds if the given future
   * completes with a `Failure` containing the specified exception type.
   *
   * See the main documentation for this trait for more detail and examples.
   * 
   *
   * @param future A future of any type, which you expect to fail with an exception of the specified type T
   * @return a Future[T] containing on success the expected exception, or containing on failure
   *   a `TestFailedException`
   */
  def recoverToExceptionIf[T <: AnyRef](future: Future[Any])(implicit classTag: ClassTag[T], exCtx: ExecutionContext, pos: source.Position): Future[T] = {
    val clazz = classTag.runtimeClass
    future.failed.transform(
      ex =>
        if (!clazz.isAssignableFrom(ex.getClass)) {
          val message = Resources.wrongException(clazz.getName, ex.getClass.getName)
          throw newAssertionFailedExceptionForRecover(Some(message), Some(ex), pos)
        }
        else ex.asInstanceOf[T]
      ,
      ex => {
        val message = Resources.exceptionExpected(clazz.getName)
        throw newAssertionFailedExceptionForRecover(Some(message), None, pos)
      }
    )
  }

  /**
   * Transforms a future of any type into a `Future[Assertion]` that succeeds if the future
   * completes with a `Failure` containing the specified exception type.
   *
   * See the main documentation for this trait for more detail and examples.
   * 
   *
   * @param future A future of any type, which you expect to fail with an exception of the specified type T
   * @return a Future[Assertion] containing on success the `Succeeded` singleton, or containing on failure
   *   a `TestFailedException`
   */
  def recoverToSucceededIf[T <: AnyRef](future: Future[Any])(implicit classTag: ClassTag[T], exCtx: ExecutionContext, pos: source.Position): Future[Assertion] = {
    val clazz = classTag.runtimeClass
    future.failed.transform(
      rawEx => {
        val ex =
          rawEx match {
            case execEx: java.util.concurrent.ExecutionException => execEx.getCause
            case other => other
          }
        if (!clazz.isAssignableFrom(ex.getClass)) {
          val message = Resources.wrongException(clazz.getName, ex.getClass.getName)
          throw newAssertionFailedExceptionForRecover(Some(message), Some(ex), pos)
        }
        else Succeeded
      },
      ex => {
        val message = Resources.exceptionExpected(clazz.getName)
        throw newAssertionFailedExceptionForRecover(Some(message), None, pos)
      }
    )
  }

  private[scalatest] def newAssertionFailedExceptionForRecover(optionalMessage: Option[String], optionalCause: Option[Throwable], pos: source.Position): Throwable =
    new exceptions.TestFailedException(toExceptionFunction(optionalMessage), optionalCause, pos)
}

/**
 * Companion object that facilitates the importing of `RecoverMethods`'s method as 
 * an alternative to mixing it in. One use case is to import `RecoverMethods`'s method so you can use
 * it in the Scala interpreter.
 *
 * @author Bill Venners
 */
object RecoverMethods extends RecoverMethods

