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

import org.scalatest.concurrent.SleepHelper
import org.scalatest.concurrent.SleepHelper
import org.scalatest.concurrent.SleepHelper
import time.Span

/**
 * Provides methods that can be used in `withFixture` implementations to retry tests in various scenarios.
 *
 * Trait `Retries` is intended to help you deal with &ldquo;flickers&rdquo;&mdash;tests that usually pass, but
 * occasionally fail. The best way to deal with such tests is to fix them so they always pass. Sometimes, however, this is
 * not practical. In such cases, flickers can waste your time by forcing you to investigate test failures that turn
 * out to be flickers. Or worse, like the boy who cried wolf, the flickers may train you an your colleagues to not pay attention
 * to failures such that you don't notice real problems, at least not in a timely manner.
 * 
 *
 * Trait `Retries` offers methods that will retry a failed and/or canceled test once, on the same thread,
 * with or without a delay. These methods take a block that results in <a href="Outcome.html">`Outcome`</a>,
 * and are intended to be used in `withFixture` methods. You should be very selective about which tests you
 * retry, retrying those for which you have good evidence to conclude they are flickers. Thus it is recommended you
 * only retry tests that are tagged with `Retryable`, and only tag tests as such once they have flickered
 * consistently for a while, and only after you invested a reasonable effort into fixing them properly.
 * 
 *
 * Here's an example showing how you might use `Retries`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.tagobjects.retryable
 * 
 * import org.scalatest._
 * import tagobjects.Retryable
 * 
 * class SetSpec extends FlatSpec with Retries {
 * 
 *   override def withFixture(test: NoArgTest) = {
 *     if (isRetryable(test))
 *       withRetry { super.withFixture(test) }
 *     else
 *       super.withFixture(test)
 *   }
 *
 *   "An empty Set" should "have size 0" taggedAs(Retryable) in {
 *     assert(Set.empty.size === 0)
 *   }
 * }
 * }}}
 */
trait Retries {

  /**
   * Retries the given block immediately (with no delay) if the <a href="Outcome.html">`Outcome`</a> of executing
   * the block is either <a href="Failed.html">`Failed`</a> or <a href="Canceled.html">`Canceled`</a>.
   *
   * The behavior of this method is defined in the table below. The first two rows show the main "retry" behavior: if
   * executing the block initially fails, and on retry it succeeds, the result is `Canceled`. The purpose of this is
   * to deal with "flickering" tests by downgrading a failure that succeeds on retry to a cancelation.
   * Or, if executing the block initially results in `Canceled`, and on retry it succeeds, the result
   * is `Succeeded`. The purpose of this is to deal with tests that intermittently cancel by ignoring a cancelation that
   * succeeds on retry.
   * 
   *
   * In the table below, if the &ldquo;Retry `Outcome`&rdquo; has just a dash, the block is not retried.
   * Otherwise, the block is retried on the same thread, with no delay.
   * 
   *
   * <table style="border-collapse: collapse; border: 1px solid black">
   * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''First `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Retry `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Result'''</th></tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled` (the `Succeeded` and `Failed` are
   *     discarded; the exception from the `Failed` is the cause of the exception in the `Canceled`)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the first `Failed` (the second `Failed` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Pending` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the first `Canceled` (the second `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Canceled` (the `Pending` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * </table>
   * 
   * @param blk the block to execute and potentially retry
   */
  def withRetry(blk: => Outcome): Outcome = withRetry(Span.Zero)(blk)

  /**
   * Retries the given block with a given delay if the <a href="Outcome.html">`Outcome`</a> of executing
   * the block is either <a href="Failed.html">`Failed`</a> or <a href="Canceled.html">`Canceled`</a>.
   *
   * The behavior of this method is defined in the table below. The first two rows show the main "retry" behavior: if
   * executing the block initially fails, and on retry it succeeds, the result is `Canceled`. The purpose of this is
   * to deal with "flickering" tests by downgrading a failure that succeeds on retry
   * to a cancelation.
   * Or, if executing the block initially results in `Canceled`, and on retry it succeeds, the result
   * is `Succeeded`. The purpose of this is to deal with tests that intermittently cancel by ignoring a cancelation that
   * succeeds on retry.
   * 
   *
   * In the table below, if the &ldquo;Retry `Outcome`&rdquo; has just a dash, the block is not retried.
   * Otherwise, the block is retried on the same thread, after sleeping the given delay.
   * 
   *
   * <table style="border-collapse: collapse; border: 1px solid black">
   * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''First `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Retry `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Result'''</th></tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled` (the `Succeeded` and `Failed` are
   *     discarded; the exception from the `Failed` is the cause of the exception in the `Canceled`)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the first `Failed` (the second `Failed` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Pending` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the first `Canceled` (the second `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Canceled` (the `Pending` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * </table>
   * 
   * @param delay the amount of time to sleep before retrying
   * @param blk the block to execute and potentially retry
   */
  def withRetry(delay: Span)(blk: => Outcome): Outcome = {
    val firstOutcome = blk
    firstOutcome match {
      case Failed(ex) =>
        blk match {
          case Succeeded => Canceled(Resources.testFlickered, ex)
          case other => firstOutcome
        }
      case Canceled(ex) =>
        blk match {
          case Succeeded => Succeeded
          case failed: Failed => failed // Never hide a failure.
          case other => firstOutcome
        }
      case other => other
    }
  }

  /**
   * Retries the given block immediately (with no delay) if the <a href="Outcome.html">`Outcome`</a> of executing
   * the block is <a href="Failed.html">`Failed`</a>.
   *
   * The behavior of this method is defined in the table below. The first row shows the main "retry" behavior: if
   * executing the block initially fails, and on retry it succeeds, the result is `Canceled`. The purpose of this is
   * to deal with "flickering" tests by downgrading a failure that succeeds on retry
   * to a cancelation.
   * 
   *
   * In the table below, if the &ldquo;Retry `Outcome`&rdquo; has just a dash, the block is not retried.
   * Otherwise, the block is retried on the same thread, with no delay.
   * 
   *
   * <table style="border-collapse: collapse; border: 1px solid black">
   * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''First `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Retry `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Result'''</th></tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled` (the `Succeeded` and `Failed` are
   *     discarded; the exception from the `Failed` is the cause of the exception in the `Canceled`)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Canceled` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the first `Failed` (the second `Failed` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Pending` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * </table>
   * 
   * @param blk the block to execute and potentially retry
   */
  def withRetryOnFailure(blk: => Outcome): Outcome = withRetryOnFailure(Span.Zero)(blk)

  /**
   * Retries the given block immediately with the given delay if the <a href="Outcome.html">`Outcome`</a> of executing
   * the block is <a href="Failed.html">`Failed`</a>.
   *
   * The behavior of this method is defined in the table below. The first row shows the main "retry" behavior: if
   * executing the block initially fails, and on retry it succeeds, the result is `Canceled`. The purpose of this is
   * to deal with "flickering" tests by downgrading a failure that succeeds on retry
   * to a cancelation.
   * 
   *
   * In the table below, if the &ldquo;Retry `Outcome`&rdquo; has just a dash, the block is not retried.
   * Otherwise, the block is retried on the same thread, after the given delay.
   * 
   *
   * <table style="border-collapse: collapse; border: 1px solid black">
   * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''First `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Retry `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Result'''</th></tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled` (the `Succeeded` and `Failed` are
   *     discarded; the exception from the `Failed` is the cause of the exception in the `Canceled`)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Canceled` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the first `Failed` (the second `Failed` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Pending` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * </table>
   * 
   * @param delay the amount of time to sleep before retrying
   * @param blk the block to execute and potentially retry
   */
  def withRetryOnFailure(delay: Span)(blk: => Outcome): Outcome = {
    val firstOutcome = blk
    firstOutcome match {
      case Failed(ex) =>
        if (delay != Span.Zero)
          SleepHelper.sleep(delay.millisPart)
        blk match {
          case Succeeded => Canceled(Resources.testFlickered, ex)
          case other => firstOutcome
        }
      case other => other
    }
  }

  /**
   * Retries the given block immediately (with no delay) if the <a href="Outcome.html">`Outcome`</a> of executing
   * the block is <a href="Canceled.html">`Canceled`</a>.
   *
   * The behavior of this method is defined in the table below. The first row shows the main "retry" behavior: if
   * executing the block initially results in `Canceled`, and on retry it succeeds, the result is `Succeeded`.
   * The purpose of this is to deal with tests that intermittently cancel by ignoring a cancelation that
   * succeeds on retry.
   * 
   *
   * In the table below, if the &ldquo;Retry `Outcome`&rdquo; has just a dash, the block is not retried.
   * Otherwise, the block is retried on the same thread, with no delay.
   * 
   *
   * <table style="border-collapse: collapse; border: 1px solid black">
   * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''First `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Retry `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Result'''</th></tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Succeeded` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the first `Canceled` (the second `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Canceled` (the `Pending` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * </table>
   * 
   * @param blk the block to execute and potentially retry
   */
  def withRetryOnCancel(blk: => Outcome): Outcome = withRetryOnCancel(Span.Zero)(blk)

  /**
   * Retries the given block after the given delay if the <a href="Outcome.html">`Outcome`</a> of executing
   * the block is <a href="Canceled.html">`Canceled`</a>.
   *
   * The behavior of this method is defined in the table below. The first row shows the main "retry" behavior: if
   * executing the block initially results in `Canceled`, and on retry it succeeds, the result is `Succeeded`.
   * The purpose of this is to deal with tests that intermittently cancel by ignoring a cancelation that
   * succeeds on retry.
   * 
   *
   * In the table below, if the &ldquo;Retry `Outcome`&rdquo; has just a dash, the block is not retried.
   * Otherwise, the block is retried on the same thread, after the given delay.
   * 
   *
   * <table style="border-collapse: collapse; border: 1px solid black">
   * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''First `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Retry `Outcome`'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Result'''</th></tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Succeeded` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Succeeded` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * &mdash;
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed` (no retry)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the first `Canceled` (the second `Canceled` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Pending`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Canceled` (the `Pending` is discarded)
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Canceled`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * `Failed`
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * the `Failed` (the `Canceled` is discarded)
   * </td>
   * </tr>
   * </table>
   * 
   * @param delay the amount of time to sleep before retrying
   * @param blk the block to execute and potentially retry
   */
  def withRetryOnCancel(delay: Span)(blk: => Outcome): Outcome = {
    val firstOutcome = blk
    firstOutcome match {
      case Canceled(ex) =>
        if (delay != Span.Zero)
          SleepHelper.sleep(delay.millisPart)
        SleepHelper.sleep(delay.millisPart)
        blk match {
          case Succeeded => Succeeded
          case failed: Failed => failed // Never hide a failure.
          case other => firstOutcome
        }
      case other => other
    }
  }

  /**
   * Indicates whether the test described by the given `TestData` includes the
   * tag `org.scalatest.tags.Retryable`.
   *
   * This method provides an easy way to selectively retry just tests that are flickering. You can
   * annotated such problematic tests with `Retryable`, and just retry those. Here's
   * what it might look like:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * override def withFixture(test: NoArgTest) = {
   *   if (isRetryable(test))
   *     withRetry { super.withFixture(test) }
   *   else
   *     super.withFixture(test)
   * }
   * }}}
   *
   */
  def isRetryable(testData: TestData): Boolean = testData.tags.exists(_ == "org.scalatest.tags.Retryable")
}

/**
 * Companion object to trait `Retries` that enables its members to be imported as an 
 * alternative to mixing them in.
 */
object Retries extends Retries

