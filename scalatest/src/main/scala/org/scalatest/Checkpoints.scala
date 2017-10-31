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

import org.scalatest.exceptions.StackDepthException

import org.scalatest.exceptions._
import org.scalactic._

/**
 * Trait providing class `Checkpoint`, which enables multiple assertions
 * to be performed within a test, with any failures accumulated and reported
 * together at the end of the test.
 *
 * Because ScalaTest uses exceptions to signal failed assertions, normally execution
 * of a test will stop as soon as the first failed assertion is encountered. Trait
 * `Checkpoints` provides an option when you want to continue executing
 * the remainder of the test body, or part of it, even if an assertion has already failed in that test.
 * 
 * To use a `Checkpoint` (once you've mixed in or imported the members of trait
 * `Checkpoints`), you first need to create one, like this:
 * 
 *
 * {{{
 * val cp = new Checkpoint
 * }}}
 *
 * Then give the `Checkpoint` assertions to execute by passing them (via a by-name parameter)
 * to its `apply` method, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val (x, y) = (1, 2)
 * cp { x should be &lt; 0 }
 * cp { y should be &gt; 9 }
 * }}}
 *
 * Both of the above assertions will fail, but it won't be reported yet. The `Checkpoint` will execute them
 * right away, each time its `apply` method is invoked. But it will catch the `TestFailedExceptions` and
 * save them, only reporting them later when `reportAll` is invoked. Thus, at the end of the test, you must call
 * `reportAll`, like this:
 * 
 *
 * {{{
 * cp.reportAll()
 * }}}
 * 
 * This `reportAll` invocation will complete abruptly with a `TestFailedException` whose message
 * includes the message, source file, and line number of each of the checkpointed assertions that previously failed. For example:
 * 
 *
 * {{{
 * 1 was not less than 0 (in Checkpoint) at ExampleSpec.scala:12
 * 2 was not greater than 9 (in Checkpoint) at ExampleSpec.scala:13
 * }}}
 *
 * Make sure you invoke `reportAll` before the test completes, otherwise any failures that were detected by the
 * `Checkpoint` will not be reported.
 * 
 *
 * Note that a `Checkpoint` will catch and record for later reporting (via `reportAll`) exceptions that mix in `StackDepth`
 * except for `TestCanceledException`, `TestRegistrationClosedException`, `NotAllowedException`,
 * and `DuplicateTestNameException`. If a block of code passed to a `Checkpoint`'s `apply` method completes
 * abruptly with any of the `StackDepth` exceptions in the previous list, or any non-`StackDepth` exception, that invocation
 * of the `apply` method will complete abruptly with the same exception immediately. Unless you put `reportAll` in a finally
 * clause and handle this case, such an unexpected exception will cause you to lose any information about assertions that failed earlier in the test and were
 * recorded by the `Checkpoint`.
 * 
 *
 * @author Bill Venners
 * @author George Berger
 */
trait Checkpoints {

  /**
   * Class that allows multiple assertions to be performed within a test, with any
   * failures accumulated and reported together at the end of the test.
   * 
   * See the main documentation for trait `Checkpoints` for more information and an example.
   * 
   */
  class Checkpoint {
    private final val failures: ConcurrentLinkedQueue[Throwable with StackDepth] =
      new ConcurrentLinkedQueue

    //
    // Returns a string containing the file name and line number where
    // the test failure occurred, e.g. "HelloSuite.scala:18".
    //
    private def getFailLine(t: Throwable with StackDepth): String =
      t.failedCodeFileNameAndLineNumberString match {
        case Some(failLine) => failLine
        case None => "unknown line number"
      }

    /**
     * Executes the passed block of code and catches and records for later reporting (via `reportAll`) any exceptions that mix in `StackDepth`
     * except for `TestCanceledException`, `TestRegistrationClosedException `, `NotAllowedException `,
     * and `DuplicateTestNameException `.
     * 
     * If the block of code completes abruptly with any of the `StackDepth` exceptions in the
     * previous list, or any non-`StackDepth` exception, that invocation of this `apply` method will complete abruptly
     * with the same exception.
     * 
     *
     * @param f the block of code, likely containing one or more assertions, to execute
     */
    def apply(f: => Unit): Unit = {
      try {
        f
      }
      catch {
        case e: TestCanceledException => throw e
        case e: TestRegistrationClosedException => throw e
        case e: NotAllowedException => throw e
        case e: DuplicateTestNameException => throw e
        case e: StackDepth  => failures.add(e)
        case e: Throwable => throw e
      }
    }

    /**
     * If any failures were caught by checkpoints, throws a `TestFailedException`
     * whose detail message lists the failure messages and line numbers from each of the
     * failed checkpoints.
     */
    def reportAll()(implicit pos: source.Position): Unit = {
      // SKIP-SCALATESTJS-START
      val stackDepth = 1
      // SKIP-SCALATESTJS-END
      //SCALATESTJS-ONLY val stackDepth = 10
      if (!failures.isEmpty) {
        val failMessages =
          for (failure <- failures.asScala)
          yield failure.getMessage + " " + Resources.atCheckpointAt + " " + getFailLine(failure)
        throw new TestFailedException((sde: StackDepthException) => Some(failMessages.mkString("\n")), None, pos)
      }
    }
  }
}

/**
 * Companion object that facilitates the importing the members of trait `Checkpoints` as 
 * an alternative to mixing it in. One use case is to import `Checkpoints` so you can use
 * it in the Scala interpreter.
 *
 * @author Bill Venners
 */
object Checkpoints extends Checkpoints
