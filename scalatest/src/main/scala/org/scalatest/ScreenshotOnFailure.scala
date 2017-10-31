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
 * Stackable trait that when mixed into a `Suite with ScreenshotCapturer` ensures a screenshot
 * is captured for every failed test.
 *
 * The screenshot is file is placed in a directory whose name is defined by `screenshotDir`. By
 * default, `screenshotDir` returns the value of the `java.io.tmpdir` system property.
 * To change this, override `screenshotDir`.
 * 
 */
private[scalatest] trait ScreenshotOnFailure extends TestSuiteMixin { this: TestSuite with ScreenshotCapturer => 
  
  /**
   * The name of the directory into which screenshots will be captured.
   *
   * By default, this method returns the value of the `java.io.tmpdir` system property.
   * To change this, override `screenshotDir`.
   * 
   */
  val screenshotDir: String = System.getProperty("java.io.tmpdir")
  
  /**
   * Delegates to `super.withFixture` to execute the passed `NoArgTest`, and if the test fails,
   * captures a screenshot to the directory name defined by `screenshotDir`.
   *
   * This method captures screenshots by invoking `captureScreenshot`, defined in trait `ScreenshotCapturer`.
   * If `captureScreenshot` completes abruptly with an exception, information about that exception, including the full
   * stack trace, is printed to the standard error stream, and the original exception that indicated a failed test is rethrown.
   * 
   */
  abstract override def withFixture(test: NoArgTest): Outcome = {
    super.withFixture(test) match {
      case failed: Failed =>
        try captureScreenshot(screenshotDir)
        catch {
          case innerE: Throwable =>
            Console.err.println("Unable to capture screenshot to " + screenshotDir)
            innerE.printStackTrace(Console.err)
        }
        failed
      case other => other
    }
  }
}
