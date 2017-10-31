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

import org.scalatest.time.{Span, Millis}


/**
 * Trait that defines an abstract `patienceConfig` method that is implemented in <a href="PatienceConfiguration.html">`PatienceConfiguration`</a> and can
 * be overriden in stackable modification traits such as <a href="IntegrationPatience.html">`IntegrationPatience`</a>.
 *
 * The main purpose of `AbstractPatienceConfiguration` is to differentiate core `PatienceConfiguration`
 * traits, such as <a href="Eventually.html">`Eventually`</a> and <a href="Waiters.html">`Waiters`</a>, from stackable
 * modification traits for `PatienceConfiguration`s such as `IntegrationPatience`.
 * Because these stackable traits extend `AbstractPatienceConfiguration` 
 * instead of <a href="../Suite.html">`Suite`</a>, you can't simply mix in a stackable trait:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class ExampleSpec extends FunSpec with IntegrationPatience // Won't compile
 * }}}
 *
 * The previous code is undesirable because `IntegrationPatience` would have no affect on the class. Instead, you need to mix
 * in a core `PatienceConfiguration` trait and mix the stackable `IntegrationPatience` trait
 * into that, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class ExampleSpec extends FunSpec with Eventually with IntegrationPatience // Compiles fine
 * }}}
 *
 * The previous code is better because `IntegrationPatience` does have an effect: it modifies the behavior
 * of `Eventually`.
 * 
 *
 * @author Bill Venners
 */
trait AbstractPatienceConfiguration extends ScaledTimeSpans {

  /**
   * Configuration object for asynchronous constructs, such as those provided by traits <a href="Eventually.html">`Eventually`</a> and
   * <a href="Waiters.html">`Waiters`</a>.
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
   * `scaled(150 milliseconds)`
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `interval`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `scaled(15 milliseconds)`
   * </td>
   * </tr>
   * </table>
   *
   * @param timeout the maximum amount of time to wait for an asynchronous operation to complete before giving up and throwing
   *   `TestFailedException`.
   * @param interval the amount of time to sleep between each check of the status of an asynchronous operation when polling
   *
   * @author Bill Venners
   * @author Chua Chee Seng
   */
  final case class PatienceConfig(timeout: Span = scaled(Span(150, Millis)), interval: Span = scaled(Span(15, Millis)))

  /**
   * Returns a `PatienceConfig` value providing default configuration values if implemented and made implicit in subtraits.
   */
  def patienceConfig: PatienceConfig
}
