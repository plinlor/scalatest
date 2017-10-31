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

import org.scalatest.time.{Millis, Seconds, Span}

/**
 * Stackable modification trait for <a href="PatienceConfiguration.html">`PatienceConfiguration`</a> that provides default timeout and interval
 * values appropriate for integration testing. 
 *
 * The default values for the parameters are:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Configuration Parameter'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Default Value'''</th></tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * `timeout`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * `scaled(15 seconds)`
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * `interval`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * `scaled(150 milliseconds)`
 * </td>
 * </tr>
 * </table>
 *
 * The default values of both timeout and interval are passed to the `scaled` method, inherited
 * from `ScaledTimeSpans`, so that the defaults can be scaled up
 * or down together with other scaled time spans. See the documentation for trait <a href="ScaledTimeSpans.html">`ScaledTimeSpans`</a>
 * for more information.
 * 
 *
 * Mix this trait into any class that uses `PatienceConfiguration` (such as classes that mix in <a href="Eventually.html">`Eventually`</a>
 * or <a href="Waiters.html">`Waiters`</a>) to get timeouts tuned towards integration testing, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class ExampleSpec extends FeatureSpec with Eventually with IntegrationPatience {
 *   // ...
 * }
 * }}}
 *
 * @author Bill Venners
 * @author Chua Chee Seng
 */
trait IntegrationPatience extends AbstractPatienceConfiguration { this: PatienceConfiguration =>

  private val defaultPatienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = scaled(Span(15, Seconds)),
      interval = scaled(Span(150, Millis))
    )

  /**
   * Implicit `PatienceConfig` value providing default configuration values suitable for integration testing.
   */
  implicit abstract override val patienceConfig: PatienceConfig = defaultPatienceConfig
}
