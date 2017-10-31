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
package org.scalatest.prop

import org.scalacheck.Test.Parameters
import org.scalactic.anyvals.{PosZInt, PosZDouble, PosInt}
import org.scalacheck.Test.TestCallback


/**
 * Trait providing methods and classes used to configure property checks provided by the
 * the `forAll` methods of trait `GeneratorDrivenPropertyChecks` (for ScalaTest-style
 * property checks) and the `check` methods of trait `Checkers` (for ScalaCheck-style property checks).
 *
 * @author Bill Venners
 */
trait Configuration {

  @deprecated("Use PropertyCheckConfiguration directly instead.")
  trait PropertyCheckConfigurable {
    private [prop] def asPropertyCheckConfiguration: PropertyCheckConfiguration
   }

  object PropertyCheckConfiguration {
    private[scalatest] def calculateMaxDiscardedFactor(minSuccessful: Int, maxDiscarded: Int): Double =
      ((maxDiscarded + 1): Double) / (minSuccessful: Double)
    private[scalatest] def calculateMaxDiscarded(maxDiscardedRatio: Double, minSuccessful: Int): Double =
      (maxDiscardedRatio * minSuccessful) - 1
  }

  case class PropertyCheckConfiguration(minSuccessful: PosInt = PosInt(10),
                                        maxDiscardedFactor: PosZDouble = PosZDouble(5.0),
                                        minSize: PosZInt = PosZInt(0),
                                        sizeRange: PosZInt = PosZInt(100),
                                        workers: PosInt = PosInt(1)) extends PropertyCheckConfigurable {
    @deprecated("Transitional value to ensure upgrade compatibility when mixing PropertyCheckConfig and minSuccessful parameters.  Remove with PropertyCheckConfig class")
    private [scalatest] val legacyMaxDiscarded: Option[Int] = None
    @deprecated("Transitional value to ensure upgrade compatibility when mixing PropertyCheckConfig and minSize parameters.  Remove with PropertyCheckConfig class")
    private [scalatest] val legacyMaxSize: Option[Int] = None
    private [prop] def asPropertyCheckConfiguration = this
  }

  /**
   * Configuration object for property checks.
   *
   * The default values for the parameters are:
   * 
   *
   * <table style="border-collapse: collapse; border: 1px solid black">
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * minSuccessful
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * 100
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * maxDiscarded
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * 500
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * minSize
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * 0
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * maxSize
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * 100
   * </td>
   * </tr>
   * <tr>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * workers
   * </td>
   * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
   * 1
   * </td>
   * </tr>
   * </table>
   *
   * @param minSuccessful the minimum number of successful property evaluations required for the property to pass.
   * @param maxDiscarded the maximum number of discarded property evaluations allowed during a property check
   * @param minSize the minimum size parameter to provide to ScalaCheck, which it will use when generating objects for which size matters (such as strings or lists).
   * @param maxSize the maximum size parameter to provide to ScalaCheck, which it will use when generating objects for which size matters (such as strings or lists).
   * @param workers specifies the number of worker threads to use during property evaluation
   * @throws IllegalArgumentException if the specified `minSuccessful` value is less than or equal to zero,
   *   the specified `maxDiscarded` value is less than zero,
   *   the specified `minSize` value is less than zero,
   *   the specified `maxSize` value is less than zero,
   *   the specified `minSize` is greater than the specified or default value of `maxSize`, or
   *   the specified `workers` value is less than or equal to zero.
   *
   * @author Bill Venners
   */
  @deprecated("Use PropertyCheckConfiguration instead")
  case class PropertyCheckConfig(
    minSuccessful: Int = 10,
    maxDiscarded: Int = 500,
    minSize: Int = 0,
    maxSize: Int = 100,
    workers: Int = 1
  ) extends PropertyCheckConfigurable {
    require(minSuccessful > 0, "minSuccessful had value " + minSuccessful + ", but must be greater than zero")
    require(maxDiscarded >= 0, "maxDiscarded had value " + maxDiscarded + ", but must be greater than or equal to zero")
    require(minSize >= 0, "minSize had value " + minSize + ", but must be greater than or equal to zero")
    require(maxSize >= 0, "maxSize had value " + maxSize + ", but must be greater than or equal to zero")
    require(minSize <= maxSize, "minSize had value " + minSize + ", which must be less than or equal to maxSize, which had value " + maxSize)
    require(workers > 0, "workers had value " + workers + ", but must be greater than zero")
    private [prop] def asPropertyCheckConfiguration = this
  }

  import scala.language.implicitConversions

  /**
   * Implicitly converts `PropertyCheckConfig`s to `PropertyCheckConfiguration`,
   * which enables a smoother upgrade path.
   */
  implicit def PropertyCheckConfig2PropertyCheckConfiguration(p: PropertyCheckConfig): PropertyCheckConfiguration = {
    val maxDiscardedFactor = PropertyCheckConfiguration.calculateMaxDiscardedFactor(p.minSuccessful, p.maxDiscarded)
      new PropertyCheckConfiguration(
        minSuccessful = PosInt.from(p.minSuccessful).get,
        maxDiscardedFactor = PosZDouble.from(maxDiscardedFactor).get,
        minSize = PosZInt.from(p.minSize).get,
        sizeRange = PosZInt.from(p.maxSize - p.minSize).get,
        workers = PosInt.from(p.workers).get) {
        override private [scalatest]  val legacyMaxDiscarded = Some(p.maxDiscarded)
        override private [scalatest]  val legacyMaxSize      = Some(p.maxSize)
      }
  }

  /**
   * Abstract class defining a family of configuration parameters for property checks.
   * 
   * The subclasses of this abstract class are used to pass configuration information to
   * the `forAll` methods of traits `PropertyChecks` (for ScalaTest-style
   * property checks) and `Checkers`(for ScalaCheck-style property checks).
   * 
   *
   * @author Bill Venners
   */
  sealed abstract class PropertyCheckConfigParam extends Product with Serializable
  
  /**
   * A `PropertyCheckConfigParam` that specifies the minimum number of successful
   * property evaluations required for the property to pass.
   *
   * @author Bill Venners
   */
  case class MinSuccessful(value: PosInt) extends PropertyCheckConfigParam

  /**
   * A `PropertyCheckConfigParam` that specifies the maximum number of discarded
   * property evaluations allowed during property evaluation.
   *
   * In `GeneratorDrivenPropertyChecks`, a property evaluation is discarded if it throws
   * `DiscardedEvaluationException`, which is produce by `whenever` clause that
   * evaluates to false. For example, consider this ScalaTest property check:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * // forAll defined in `GeneratorDrivenPropertyChecks`
   * forAll { (n: Int) => 
   *   whenever (n > 0) {
   *     doubleIt(n) should equal (n * 2)
   *   }
   * }
   *
   * }}}
   *
   * In the above code, whenever a non-positive `n` is passed, the property function will complete abruptly
   * with `DiscardedEvaluationException`.
   * 
   *
   * Similarly, in `Checkers`, a property evaluation is discarded if the expression to the left
   * of ScalaCheck's `==>` operator is false. Here's an example:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * // forAll defined in `Checkers`
   * forAll { (n: Int) => 
   *   (n > 0) ==> doubleIt(n) == (n * 2)
   * }
   *
   * }}}
   *
   * For either kind of property check, `MaxDiscarded` indicates the maximum number of discarded 
   * evaluations that will be allowed. As soon as one past this number of evaluations indicates it needs to be discarded,
   * the property check will fail.
   * 
   *
   * @throws IllegalArgumentException if specified `value` is less than zero.
   *
   * @author Bill Venners
   */
  @deprecated case class MaxDiscarded(value: Int) extends PropertyCheckConfigParam {
    require(value >= 0)
  }

  case class MaxDiscardedFactor(value: PosZDouble) extends PropertyCheckConfigParam
  
  /**
   * A `PropertyCheckConfigParam` that specifies the minimum size parameter to
   * provide to ScalaCheck, which it will use when generating objects for which size matters (such as
   * strings or lists).
   *
   * @throws IllegalArgumentException if specified `value` is less than zero.
   *
   * @author Bill Venners
   */
  case class MinSize(value: PosZInt) extends PropertyCheckConfigParam
  
  /**
   * A `PropertyCheckConfigParam` that specifies the maximum size parameter to
   * provide to ScalaCheck, which it will use when generating objects for which size matters (such as
   * strings or lists).
   *
   * Note that the maximum size should be greater than or equal to the minimum size. This requirement is
   * enforced by the `PropertyCheckConfig` constructor and the `forAll` methods of
   * traits `PropertyChecks` and `Checkers`. In other words, it is enforced at the point
   * both a maximum and minimum size are provided together.
   * 
   * 
   * @throws IllegalArgumentException if specified `value` is less than zero.
   *
   * @author Bill Venners
   */
  @deprecated("use SizeRange instead")
  case class MaxSize(value: Int) extends PropertyCheckConfigParam {
    require(value >= 0)
  }

  /**
   * A `PropertyCheckConfigParam` that (with minSize) specifies the maximum size parameter to
   * provide to ScalaCheck, which it will use when generating objects for which size matters (such as
   * strings or lists).
   *
   * Note that the size range is added to minSize in order to calculate the maximum size passed to ScalaCheck.
   * Using a range allows compile-time checking of a non-negative number being specified.
   * 
   *
   * @author Bill Venners
   */
  case class SizeRange(value: PosZInt) extends PropertyCheckConfigParam
  
  /**
   * A `PropertyCheckConfigParam` that specifies the number of worker threads
   * to use when evaluating a property.
   *
   * @throws IllegalArgumentException if specified `value` is less than or equal to zero.
   *
   * @author Bill Venners
   */
  case class Workers(value: PosInt) extends PropertyCheckConfigParam
  
  /**
   * Returns a `MinSuccessful` property check configuration parameter containing the passed value, which specifies the minimum number of successful
   * property evaluations required for the property to pass.
   *
   */
  def minSuccessful(value: PosInt): MinSuccessful = new MinSuccessful(value)

  /**
   * Returns a `MaxDiscarded` property check configuration parameter containing the passed value, which specifies the maximum number of discarded
   * property evaluations allowed during property evaluation.
   *
   * @throws IllegalArgumentException if specified `value` is less than zero.
   */
  @deprecated("use maxDiscardedFactor instead")
  def maxDiscarded(value: Int): MaxDiscarded = new MaxDiscarded(value)

  /**
   * Returns a `MaxDiscardedFactor` property check configuration parameter containing the passed value, which specifies the factor of discarded
   * property evaluations allowed during property evaluation.
   *
   */
  def maxDiscardedFactor(value: PosZDouble): MaxDiscardedFactor = MaxDiscardedFactor(value)

  /**
   * Returns a `MinSize` property check configuration parameter containing the passed value, which specifies the minimum size parameter to
   * provide to ScalaCheck, which it will use when generating objects for which size matters (such as
   * strings or lists).
   *
   */
  def minSize(value: PosZInt): MinSize = new MinSize(value)

  /**
   * Returns a `MaxSize` property check configuration parameter containing the passed value, which specifies the maximum size parameter to
   * provide to ScalaCheck, which it will use when generating objects for which size matters (such as
   * strings or lists).
   *
   * Note that the maximum size should be greater than or equal to the minimum size. This requirement is
   * enforced by the `PropertyCheckConfig` constructor and the `forAll` methods of
   * traits `PropertyChecks` and `Checkers`. In other words, it is enforced at the point
   * both a maximum and minimum size are provided together.
   * 
   * 
   * @throws IllegalArgumentException if specified `value` is less than zero.
   */
  @deprecated("use SizeRange instead") def maxSize(value: Int): MaxSize = new MaxSize(value)

  /**
   * Returns a `SizeRange` property check configuration parameter containing the passed value, that (with minSize) specifies the maximum size parameter to
   * provide to ScalaCheck, which it will use when generating objects for which size matters (such as
   * strings or lists).
   *
   * Note that the size range is added to minSize in order to calculate the maximum size passed to ScalaCheck.
   * Using a range allows compile-time checking of a non-negative number being specified.
   * 
   *
   * @author Bill Venners
   */
  def sizeRange(value: PosZInt): SizeRange = SizeRange(value)

  /**
   * Returns a `Workers` property check configuration parameter containing the passed value, which specifies the number of worker threads
   * to use when evaluating a property.
   *
   */
  def workers(value: PosInt): Workers = new Workers(value)

  private[scalatest] def getScalaCheckParams(
    configParams: Seq[Configuration#PropertyCheckConfigParam],
    c: PropertyCheckConfigurable
  ): Parameters = {

    val config: PropertyCheckConfiguration = c.asPropertyCheckConfiguration
    var minSuccessful: Option[Int] = None
    var maxDiscarded: Option[Int] = None
    var maxDiscardedFactor: Option[Double] = None
    var pminSize: Option[Int] = None
    var psizeRange: Option[Int] = None
    var pmaxSize: Option[Int] = None
    var pworkers: Option[Int] = None

    var minSuccessfulTotalFound = 0
    var maxDiscardedTotalFound = 0
    var maxDiscardedFactorTotalFound = 0
    var minSizeTotalFound = 0
    var sizeRangeTotalFound = 0
    var maxSizeTotalFound = 0
    var workersTotalFound = 0

    for (configParam <- configParams) {
      configParam match {
        case param: MinSuccessful =>
          minSuccessful = Some(param.value)
          minSuccessfulTotalFound += 1
        case param: MaxDiscarded =>
          maxDiscarded = Some(param.value)
          maxDiscardedTotalFound += 1
        case param: MaxDiscardedFactor =>
          maxDiscardedFactor = Some(param.value)
          maxDiscardedFactorTotalFound += 1
        case param: MinSize =>
          pminSize = Some(param.value)
          minSizeTotalFound += 1
        case param: SizeRange =>
          psizeRange = Some(param.value)
          sizeRangeTotalFound += 1
        case param: MaxSize =>
          pmaxSize = Some(param.value)
          maxSizeTotalFound += 1
        case param: Workers =>
          pworkers = Some(param.value)
          workersTotalFound += 1
      }
    }

    if (minSuccessfulTotalFound > 1)
      throw new IllegalArgumentException("can pass at most one MinSuccessful config parameters, but " + minSuccessfulTotalFound + " were passed")
    val maxDiscardedAndFactorTotalFound = maxDiscardedTotalFound + maxDiscardedFactorTotalFound
    if (maxDiscardedAndFactorTotalFound > 1)
      throw new IllegalArgumentException("can pass at most one MaxDiscarded or MaxDiscardedFactor config parameters, but " + maxDiscardedAndFactorTotalFound + " were passed")
    if (minSizeTotalFound > 1)
      throw new IllegalArgumentException("can pass at most one MinSize config parameters, but " + minSizeTotalFound + " were passed")
    val maxSizeAndSizeRangeTotalFound = maxSizeTotalFound + sizeRangeTotalFound
    if (maxSizeAndSizeRangeTotalFound > 1)
      throw new IllegalArgumentException("can pass at most one SizeRange or MaxSize config parameters, but " + maxSizeAndSizeRangeTotalFound + " were passed")
    if (workersTotalFound > 1)
      throw new IllegalArgumentException("can pass at most one Workers config parameters, but " + workersTotalFound + " were passed")

    val minSuccessfulTests: Int = minSuccessful.getOrElse(config.minSuccessful)

    val minSize: Int = pminSize.getOrElse(config.minSize)

    val maxSize = {
      (psizeRange, pmaxSize, config.legacyMaxSize) match {
        case (None, None, Some(legacyMaxSize)) =>
          legacyMaxSize
        case (None, Some(maxSize), _) =>
          maxSize
        case _ =>
          psizeRange.getOrElse(config.sizeRange.value) + minSize
      }
    }

    val maxDiscardRatio: Float = {
      (maxDiscardedFactor, maxDiscarded, config.legacyMaxDiscarded, minSuccessful) match {
        case (None, None, Some(legacyMaxDiscarded), Some(specifiedMinSuccessful)) =>
          PropertyCheckConfiguration.calculateMaxDiscardedFactor(specifiedMinSuccessful, legacyMaxDiscarded).toFloat
        case (None, Some(md), _, _) =>
          if (md < 0) Parameters.default.maxDiscardRatio
          else PropertyCheckConfiguration.calculateMaxDiscardedFactor(minSuccessfulTests, md).toFloat
        case _ =>
          maxDiscardedFactor.getOrElse(config.maxDiscardedFactor.value).toFloat
      }
    }

    Parameters.default
      .withMinSuccessfulTests(minSuccessfulTests)
      .withMinSize(minSize)
      .withMaxSize(maxSize)
      .withWorkers(pworkers.getOrElse(config.workers))
      .withTestCallback(new TestCallback {})
      .withMaxDiscardRatio(maxDiscardRatio)
      .withCustomClassLoader(None)
  }

  /**
   * Implicit `PropertyCheckConfig` value providing default configuration values.
   */
  implicit val generatorDrivenConfig = PropertyCheckConfiguration()
}

/**
 * Companion object that facilitates the importing of `Configuration` members as
 * an alternative to mixing it in. One use case is to import `Configuration` members so you can use
 * them in the Scala interpreter.
 */
object Configuration extends Configuration
