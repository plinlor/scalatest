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
 * Trait that contains the `outcomeOf` method, which executes a passed code block and
 * transforms the outcome into an <a href="Outcome.html">`Outcome`</a>, using the
 * same mechanism used by ScalaTest to produce an `Outcome` when executing
 * a test.
 *
 * For an example of `outcomeOf` in action, see the documentation for
 * class <a href="prop/TableFor2.html">`TableFor2`</a>.
 * 
 *
 * @author Bill Venners
 */
trait OutcomeOf {

  /**
   * Executes the supplied code (a by-name parameter) and returns an `Outcome`.
   *
   * Because `Error`s are used to denote serious errors, ScalaTest does not always treat a test that completes abruptly with
   * an `Error` as a test failure, but sometimes as
   * an indication that serious problems have arisen that should cause the run to abort, and the `outcomeOf` method exhibits
   * the same behavior. For example, if a test completes abruptly
   * with an `OutOfMemoryError`, it will not be reported as a test failure, but will instead cause the run to abort.
   * Because not everyone uses `Error`s only to represent serious problems, however, ScalaTest only behaves this way
   * for the following exception types (and their subclasses):
   * 
   *
   * <ul>
   * <li>`java.lang.annotation.AnnotationFormatError`</li>
   * <li>`java.awt.AWTError`</li>
   * <li>`java.nio.charset.CoderMalfunctionError`</li>
   * <li>`javax.xml.parsers.FactoryConfigurationError`</li>
   * <li>`java.lang.LinkageError`</li>
   * <li>`java.lang.ThreadDeath`</li>
   * <li>`javax.xml.transform.TransformerFactoryConfigurationError`</li>
   * <li>`java.lang.VirtualMachineError`</li>
   * </ul>
   *
   * The previous list includes all `Error`s that exist as part of Java 1.5 API, excluding `java.lang.AssertionError`.
   * If the code supplied to `outcomeOf` completes abruptly in one of the errors in the previous list, `outcomeOf`
   * will not return an `Outcome`, but rather will complete abruptly with the same exception.
   * will wrap any other exception thrown by the supplied code in a `Some` and return it.
   * 
   *
   * The `outcomeOf` method (and ScalaTest in general) does treat a thrown `AssertionError` as an indication of a test failure and therefore
   * returns a `Failed` wrapping the `AssertionError`. In addition, any other `Error` that is not an instance of a
   * type mentioned in the previous list will be caught by the `outcomeOf` and transformed as follows:
   *
   * <ul>
   * <li><a href="exceptions/TestPendingException.html">`TestPendingException`</a></li>: <a href="Pending$.html">`Pending`</a>
   * <li><a href="exceptions/TestCanceledException.html">`TestCanceledException`</a></li>: <a href="Canceled.html">`Canceled`</a>
   * <li>otherwise: <a href="Failed.html">`Failed`</a>
   * </ul>
   * 
   *
   * If the code block completes normally (''i.e.'', it doesn't throw any exception), `outcomeOf` results in <a href="Succeeded$.html">`Succeeded`</a>.
   * 
   *
   * @param f a block of code to execute
   * @return an `Outcome` representing the outcome of executing the block of code
   */
  def outcomeOf(f: => Any): Outcome = {
    try {                                         
      f                                           
      Succeeded
    }                                             
    catch {                                       
      case ex: exceptions.TestCanceledException => Canceled(ex)                           
      case _: exceptions.TestPendingException => Pending
      case tfe: exceptions.TestFailedException => Failed(tfe)
      case ex: Throwable if !Suite.anExceptionThatShouldCauseAnAbort(ex) => Failed(ex)                           
    }
  }
}

/**
 * Companion object that facilitates the importing of `OutcomeOf`'s method as 
 * an alternative to mixing it in. One use case is to import `OutcomeOf`'s method so you can use
 * it in the Scala interpreter.
 *
 * @author Bill Venners
 */
object OutcomeOf extends OutcomeOf

