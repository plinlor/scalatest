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

import org.scalatest._
import time.Span
import PatienceConfiguration._
import org.scalactic.Requirements._

/**
 * Trait providing methods and classes used to configure timeouts and, where relevant, the interval
 * between retries.
 *
 * This trait is called `PatienceConfiguration` because it allows configuration of two
 * values related to patience: The timeout specifies how much time asynchronous operations will be given
 * to succeed before giving up. The interval specifies how much time to wait between checks to determine
 * success when polling.
 * 
 *
 * The default values for timeout and interval provided by trait `PatienceConfiguration` are tuned for unit testing,
 * where running tests as fast as
 * possible is a high priority and subsystems requiring asynchronous operations are therefore often replaced
 * by mocks. This table shows the default values:
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
 * Values more appropriate to integration testing, where asynchronous operations tend to take longer because the tests are run
 * against the actual subsytems (not mocks), can be obtained by mixing in trait <a href="IntegrationPatience.html">`IntegrationPatience`</a>.
 * 
 *
 * The default values of both timeout and interval are passed to the `scaled` method, inherited
 * from `ScaledTimeSpans`, so that the defaults can be scaled up
 * or down together with other scaled time spans. See the documentation for trait <a href="ScaledTimeSpans.html">`ScaledTimeSpans`</a>
 * for more information.
 * 
 *
 * Timeouts are used by the `eventually` methods of trait
 * <a href="Eventually.html">`Eventually`</a> and the `await` method of class
 * `Waiter`, a member of trait
 * <a href="Waiters.html">`Waiters`</a>. Intervals are used by
 * the `eventually` methods.
 * 
 *
 * @author Bill Venners
 */
trait PatienceConfiguration extends AbstractPatienceConfiguration {

  private val defaultPatienceConfig = PatienceConfig()

  /**
   * Implicit `PatienceConfig` value providing default configuration values.
   *
   * To change the default configuration, override or hide this `def` with another implicit
   * `PatienceConfig` containing your desired default configuration values.
   * 
   */
  implicit def patienceConfig: PatienceConfig = defaultPatienceConfig

  /**
   * Returns a `Timeout` configuration parameter containing the passed value, which
   * specifies the maximum amount to wait for an asynchronous operation to complete.
   */
  def timeout(value: Span) = Timeout(value)

  /**
   * Returns an `Interval` configuration parameter containing the passed value, which
   * specifies the amount of time to sleep after a retry.
   */
  def interval(value: Span) = Interval(value)    // TODO: Throw NPE
}

object PatienceConfiguration {

  /**
   * Abstract class defining a family of configuration parameters for traits `Eventually` and `Waiters`.
   * 
   * The subclasses of this abstract class are used to pass configuration information to
   * the `eventually` methods of trait `Eventually` and the `await` methods of trait `Waiters`.
   * 
   *
   * @author Bill Venners
   * @author Chua Chee Seng
   */
  sealed abstract class PatienceConfigParam extends Product with Serializable

  /**
   * A `PatienceConfigParam` that specifies the maximum amount of time to wait for an asynchronous operation to
   * complete. 
   *
   * @param value the maximum amount of time to retry before giving up and throwing
   *   `TestFailedException`.
   * @throws NullArgumentException if passed `value` is `null`
   *
   * @author Bill Venners
   */
  final case class Timeout(value: Span) extends PatienceConfigParam {
    requireNonNull(value)
  }

  /**
   * A `PatienceConfigParam` that specifies the amount of time to sleep after
   * a retry.
   *
   * @param value the amount of time to sleep between each attempt
   * @throws NullArgumentException if passed `value` is `null`
   *
   * @author Bill Venners
   */
  final case class Interval(value: Span) extends PatienceConfigParam {
    requireNonNull(value)
  }
}
