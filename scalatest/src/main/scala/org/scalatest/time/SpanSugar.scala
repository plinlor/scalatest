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
package org.scalatest.time

/**
 * Trait providing four implicit conversions that allow you to specify spans of time
 * by invoking "units" methods such as `millis`, `seconds`, and `minutes`
 * on `Int`, `Long`, `Float`, and `Double`.
 * 
 * This trait enables you to specify a span of time in a clear, boilerplate-free way when you
 * need to provide an instance of <a href="Span.html">`Span`</a>. This
 * can be used, for example, with the `failAfter` method of trait
 * <a href="../concurrent/Timeouts.html">`Timeouts`</a> or the `timeLimit` field of trait
 * <a href="../concurrent/TimeLimitedTests.html">`TimeLimitedTests`</a>. It can also be used to specify
 * timeouts when using traits <a href="../concurrent/Eventually.html">`Eventually`</a>,
 * <a href="../concurrent/Futures.html">`Futures`</a>,
 * <a href="../concurrent/Waiter.html">`Waiter`</a>. Here are examples of each unit enabled by this trait:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''`Int`'''
 * </th>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''`Long`'''
 * </th>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''`Float`'''
 * </th>
 * <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 * '''`Double`'''
 * </th>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1 nanosecond
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1L nanosecond
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0F nanosecond
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0 nanosecond
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100 nanoseconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100L nanoseconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8F nanoseconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8 nanoseconds
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1 microsecond
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1L microsecond
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0F microsecond
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0 microsecond
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100 microseconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100L microseconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8F microseconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8 microseconds
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1 millisecond
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1L millisecond
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0F millisecond
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0 millisecond
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100 milliseconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100L milliseconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8F milliseconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8 milliseconds
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100 millis
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100L millis
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8F millis
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8 millis
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1 second
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1L second
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0F second
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0 second
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100 seconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100L seconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8F seconds
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8 seconds
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1 minute
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1L minute
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0F minute
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0 minute
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100 minutes
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100L minutes
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8F minutes
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8 minutes
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1 hour
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1L hour
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0F hour
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0 hour
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100 hours
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100L hours
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8F hours
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8 hours
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1 day
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1L day
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0F day
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 1.0 day
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100 days
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 100L days
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8F days
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: right">
 * 99.8 days
 * </td>
 * </tr>
 * </table>
 *
 * This trait is not the default way to specify `Span`s for two reasons. First, it adds
 * four implicits, which would give the compiler more work to do and may conflict with other implicits the
 * user has in scope. Instead, `Span` provides a clear, concise default way to specify time
 * spans that requires no implicits. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(1, Second)
 * }}}
 *
 * If you already have implicit conversions in scope that provide a similar syntax sugar for expression
 * time spans, you can use that by providing an implicit conversion from the result of those expressions
 * to `Span`. Note that because of implicit conversions in the `Span` companion object,
 * you can use a `scala.concurrent.duration.Duration` (including in its "sugary" form) where
 * a `Span` is needed, and vice versa.
 * 
 */
trait SpanSugar {

  implicit val postfixOps = language.postfixOps

  /**
   * Class containing methods that return a `Span` time value calculated from the
   * `Long` value passed to the `GrainOfTime` constructor.
   * 
   * @param value the value to be converted
   */
  class GrainOfTime(value: Long) {

    /**
     * A units method for one nanosecond.
     *
     * @return A `Span` representing the value passed to the constructor in nanoseconds
     */
    def nanosecond: Span = Span(value, Nanosecond)

    /**
     * A units method for nanoseconds.
     *
     * @return A `Span` representing the value passed to the constructor in nanoseconds
     */
    def nanoseconds: Span = Span(value, Nanoseconds)

    /**
     * A units method for one microsecond.
     *
     * @return A `Span` representing the value passed to the constructor in microseconds
     */
    def microsecond: Span = Span(value, Microsecond)

    /**
     * A units method for microseconds.
     *
     * @return A `Span` representing the value passed to the constructor in microseconds
     */
    def microseconds: Span = Span(value, Microseconds)

    /**
     * A units method for one millisecond. 
     * 
     * @return A `Span` representing the value passed to the constructor in milliseconds
     */
    def millisecond: Span = Span(value, Millisecond)
    
    /**
     * A units method for milliseconds. 
     * 
     * @return A `Span` representing the value passed to the constructor in milliseconds
     */
    def milliseconds: Span = Span(value, Milliseconds)

    /**
     * A shorter units method for milliseconds. 
     * 
     * @return A `Span` representing the value passed to the constructor in milliseconds
     */
    def millis: Span = Span(value, Millis)

    /**
     * A units method for one second. 
     * 
     * @return A `Span` representing the value passed to the constructor in seconds
     */
    def second: Span = Span(value, Second) 
    
    /**
     * A units method for seconds. 
     * 
     * @return A `Span` representing the value passed to the constructor in seconds
     */
    def seconds: Span = Span(value, Seconds)

    /**
     * A units method for one minute. 
     * 
     * @return A `Span` representing the value passed to the constructor in minutes
     */
    def minute: Span = Span(value, Minute)

    /**
     * A units method for minutes. 
     * 
     * @return A `Span` representing the value passed to the constructor in minutes
     */
    def minutes: Span = Span(value, Minutes)
    
    /**
     * A units method for one hour. 
     * 
     * @return A `Span` representing the value passed to the constructor in hours
     */
    def hour: Span = Span(value, Hour)

    /**
     * A units method for hours. 
     * 
     * @return A `Span` representing the value passed to the constructor in hours
     */
    def hours: Span = Span(value, Hours)
    
    /**
     * A units method for one day. 
     * 
     * @return A `Span` representing the value passed to the constructor in days
     */
    def day: Span = Span(value, Day)

    /**
     * A units method for days. 
     * 
     * @return A `Span` representing the value passed to the constructor multiplied in days
     */
    def days: Span = Span(value, Days)
  }

  /**
   * Class containing methods that return a `Span` time value calculated from the
   * `Double` value passed to the `FloatingGrainOfTime` constructor.
   *
   * @param value the value to be converted
   */
  class FloatingGrainOfTime(value: Double) {

    /**
     * A units method for one nanosecond.
     *
     * @return A `Span` representing the value passed to the constructor in nanoseconds
     */
    def nanosecond: Span = Span(value, Nanosecond)

    /**
     * A units method for nanoseconds.
     *
     * @return A `Span` representing the value passed to the constructor in nanoseconds
     */
    def nanoseconds: Span = Span(value, Nanoseconds)

    /**
     * A units method for one microsecond.
     *
     * @return A `Span` representing the value passed to the constructor in microseconds
     */
    def microsecond: Span = Span(value, Microsecond)

    /**
     * A units method for microseconds.
     *
     * @return A `Span` representing the value passed to the constructor in microseconds
     */
    def microseconds: Span = Span(value, Microseconds)

    /**
     * A units method for one millisecond.
     *
     * @return A `Span` representing the value passed to the constructor in milliseconds
     */
    def millisecond: Span = Span(value, Millisecond)

    /**
     * A units method for milliseconds.
     *
     * @return A `Span` representing the value passed to the constructor in milliseconds
     */
    def milliseconds: Span = Span(value, Milliseconds)

    /**
     * A shorter units method for milliseconds.
     *
     * @return A `Span` representing the value passed to the constructor in milliseconds
     */
    def millis: Span = Span(value, Millis)

    /**
     * A units method for one second.
     *
     * @return A `Span` representing the value passed to the constructor in seconds
     */
    def second: Span = Span(value, Second)

    /**
     * A units method for seconds.
     *
     * @return A `Span` representing the value passed to the constructor in seconds
     */
    def seconds: Span = Span(value, Seconds)

    /**
     * A units method for one minute.
     *
     * @return A `Span` representing the value passed to the constructor in minutes
     */
    def minute: Span = Span(value, Minute)

    /**
     * A units method for minutes.
     *
     * @return A `Span` representing the value passed to the constructor in minutes
     */
    def minutes: Span = Span(value, Minutes)

    /**
     * A units method for one hour.
     *
     * @return A `Span` representing the value passed to the constructor in hours
     */
    def hour: Span = Span(value, Hour)

    /**
     * A units method for hours.
     *
     * @return A `Span` representing the value passed to the constructor in hours
     */
    def hours: Span = Span(value, Hours)

    /**
     * A units method for one day.
     *
     * @return A `Span` representing the value passed to the constructor in days
     */
    def day: Span = Span(value, Day)

    /**
     * A units method for days.
     *
     * @return A `Span` representing the value passed to the constructor multiplied in days
     */
    def days: Span = Span(value, Days)
  }

  import scala.language.implicitConversions

  /**
   * Implicit conversion that adds time units methods to `Int`s.
   * 
   * @param i: the `Int` to which to add time units methods
   * @return a `GrainOfTime` wrapping the passed `Int`
   */
  implicit def convertIntToGrainOfTime(i: Int): GrainOfTime = new GrainOfTime(i)
  
  /**
   * Implicit conversion that adds time units methods to `Long`s.
   * 
   * @param i: the `Long` to which to add time units methods
   * @return a `GrainOfTime` wrapping the passed `Long`
   */
  implicit def convertLongToGrainOfTime(i: Long): GrainOfTime = new GrainOfTime(i)


  /**
   * Implicit conversion that adds time units methods to `Float`s.
   *
   * @param f: the `Float` to which to add time units methods
   * @return a `FloatingGrainOfTime` wrapping the passed `Float`
   */
  implicit def convertFloatToGrainOfTime(f: Float): FloatingGrainOfTime = new FloatingGrainOfTime(f)

  /**
   * Implicit conversion that adds time units methods to `Double`s.
   *
   * @param d: the `Double` to which to add time units methods
   * @return a `FloatingGrainOfTime` wrapping the passed `Double`
   */
  implicit def convertDoubleToGrainOfTime(d: Double): FloatingGrainOfTime = new FloatingGrainOfTime(d)
}

/**
 * Companion object that facilitates the importing of `SpanSugar` members as 
 * an alternative to mixing it in. One use case is to import `SpanSugar` members so you can use
 * them in the Scala interpreter:
 *
 * {{{  <!-- class="stREPL" -->
 * $scala -classpath scalatest.jar
 * Welcome to Scala version 2.9.1.final (Java HotSpot(TM) 64-Bit Server VM, Java 1.6.0_29).
 * Type in expressions to have them evaluated.
 * Type :help for more information.
 *
 * scala&gt; import org.scalatest._
 * import org.scalatest._
 *
 * scala&gt; import concurrent.Eventually._
 * import org.scalatest.concurrent.Eventually._
 *
 * scala&gt; import time.SpanSugar._
 * import org.scalatest.time.SpanSugar._
 *
 * scala&gt; eventually(timeout(100 millis)) { 1 + 1 should equal (3) }
 * }}}
 */
object SpanSugar extends SpanSugar
