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
package org.scalatest.concurrent

import org.scalactic._
import java.util.concurrent.{TimeUnit, Future => FutureOfJava}
import org.scalatest.Resources
import org.scalatest.Suite.anExceptionThatShouldCauseAnAbort
import org.scalatest.exceptions.StackDepthException
import org.scalatest.exceptions.TestCanceledException
import org.scalatest.exceptions.{TestPendingException, TestFailedException, TimeoutField}
import org.scalatest.time.Span

/**
 * Provides an implicit conversion from `java.util.concurrent.Future[T]` to
 * <a href="Futures$FutureConcept.html">`FutureConcept[T]`</a>.
 *
 * This trait enables you to invoke the methods defined on `FutureConcept` on a Java `Future`, as well as to pass a Java future
 * to the `whenReady` methods of supertrait `Futures`.
 * See the documentation for supertrait <a href="Futures.html">`Futures`</a> for the details on the syntax this trait provides
 * for testing with Java futures.
 * 
 * 
 * @author Bill Venners
 */
trait JavaFutures extends Futures {

  import scala.language.implicitConversions

  /**
   * Implicitly converts a `java.util.concurrent.Future[T]` to
   * `FutureConcept[T]`, allowing you to invoke the methods
   * defined on `FutureConcept` on a Java `Future`, as well as to pass a Java future
   * to the `whenReady` methods of supertrait <a href="Futures.html">`Futures`</a>.
   *
   * See the documentation for supertrait <a href="Futures.html">`Futures`</a> for the details on the syntax this trait provides
   * for testing with Java futures.
   * 
   *
   * <p>If the `get` method of the underlying Java future throws `java.util.concurrent.ExecutionException`, and this
   * exception contains a non-`null` cause, that cause will be included in the `TestFailedException` as its cause. The `ExecutionException`
   * will be be included as the `TestFailedException`'s cause only if the `ExecutionException`'s cause is `null`.
   * 
   *
   * The `isExpired` method of the returned `FutureConcept` will always return `false`, because
   * the underlying type, `java.util.concurrent.Future`, does not support the notion of a timeout. The `isCanceled`
   * method of the returned `FutureConcept` will return the result of invoking `isCancelled` on the underlying
   * `java.util.concurrent.Future`.
   * 
   *
   * @param javaFuture a `java.util.concurrent.Future[T]` to convert
   * @return a `FutureConcept[T]` wrapping the passed `java.util.concurrent.Future[T]`
   */
  implicit def convertJavaFuture[T](javaFuture: FutureOfJava[T]): FutureConcept[T] =
    new FutureConcept[T] {
      def eitherValue: Option[Either[Throwable, T]] =
        if (javaFuture.isDone())
          Some(Right(javaFuture.get))
        else
          None
      def isExpired: Boolean = false // Java Futures don't support the notion of a timeout
      def isCanceled: Boolean = javaFuture.isCancelled // Two ll's in Canceled. The verbosity of Java strikes again!
      // TODO: Catch TimeoutException and wrap that in a TFE with ScalaTest's TimeoutException I think.
      // def awaitAtMost(span: Span): T = javaFuture.get(span.totalNanos, TimeUnit.NANOSECONDS)
      override private[concurrent] def futureValueImpl(pos: source.Position)(implicit config: PatienceConfig): T = {
        /*val adjustment =
          if (methodName == "whenReady")
            3
          else
            0*/

        if (javaFuture.isCanceled)
          throw new TestFailedException(
            (_: StackDepthException) => Some(Resources.futureWasCanceled),
            None,
            pos
          )
        try {
          javaFuture.get(config.timeout.totalNanos, TimeUnit.NANOSECONDS)
        }
        catch {
          case e: java.util.concurrent.TimeoutException =>
            throw new TestFailedException(
              (_: StackDepthException) => Some(Resources.wasNeverReady(1, config.interval.prettyString)),
              None,
              pos
            ) with TimeoutField {
              val timeout: Span = config.timeout
            }
          case e: java.util.concurrent.ExecutionException =>
            val cause = e.getCause
            val exToReport = if (cause == null) e else cause 
            if (anExceptionThatShouldCauseAnAbort(exToReport) || exToReport.isInstanceOf[TestPendingException] || exToReport.isInstanceOf[TestCanceledException]) {
              throw exToReport
            }
            throw new TestFailedException(
              (_: StackDepthException) => Some {
                if (exToReport.getMessage == null)
                  Resources.futureReturnedAnException(exToReport.getClass.getName)
                else
                  Resources.futureReturnedAnExceptionWithMessage(exToReport.getClass.getName, exToReport.getMessage)
              },
              Some(exToReport),
              pos
            )
        }
      }
    }
}
