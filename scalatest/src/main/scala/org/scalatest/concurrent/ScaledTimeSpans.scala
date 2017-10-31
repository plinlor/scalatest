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

import org.scalatest.time.Span
// SKIP-SCALATESTJS-START
import org.scalatest.tools.Runner
// SKIP-SCALATESTJS-END

/*
I have checked in the PoC at the following branch:-

https://scalatest.googlecode.com/svn/branches/akka-span-factor 

The overriding is pretty straight forward (though it did take some time for me to read through akka's doc to figure out how to get the akka.test.timefactor correctly):-

import org.scalatest.concurrent.Eventually
import akka.actor.ActorSystem
import akka.testkit.TestKitExtension

object AkkaEventually extends Eventually {
  override def spanScaleFactor: Double = TestKitExtension.get(ActorSystem()).TestTimeFactor
}

and the TestTimeFactor is defined in application.conf:-

akka {
  test {
    timefactor = 2.0
  }
}

I have checked in AkkaEventuallySpec that checks the behavior, you can open and run the project using eclipse, with dependency to jars (couldn't find akka maven configuration in their download page) I attached and scalatest 1.8 RC2 that built from to-release-as-1.8 branch.

Hope this helps.

Thanks!
*/
/*
import org.scalatest.concurrent.SpanScaleFactor
import akka.actor.ActorSystem
import akka.testkit.TestKitExtension

trait AkkaSpanScaleFactor extends SpanScaleFactor {
  override def spanScaleFactor: Double = TestKitExtension.get(ActorSystem()).TestTimeFactor
}

and the TestTimeFactor is defined in application.conf:-

akka {
  test {
    timefactor = 2.0
  }
}

class MySpec extends FunSpec with Eventually with AkkaSpanScaleFactor {
  // ..
}
*/
/**
 * Trait providing a `scaled` method that can be used to scale time
 * `Span`s used during the testing of asynchronous operations.
 *
 * The `scaled` method allows tests of asynchronous operations to be tuned
 * according to need.  For example, `Span`s can be scaled larger when running
 * tests on slower continuous integration servers or smaller when running on faster
 * development machines.
 * 
 *
 * The `Double` factor by which to scale the `Span`s passed to
 * `scaled` is obtained from the `spanScaleFactor` method, also declared
 * in this trait. By default this method returns 1.0, but can be configured to return
 * a different value by passing a `-F` argument to <a href="../tools/Runner$.html">`Runner`</a> (or
 * an equivalent mechanism in an ant, sbt, or Maven build file).
 * 
 *
 * The default timeouts and intervals defined for traits <a href="Eventually.html">`Eventually`</a> and
 * <a href="Waiters.html">`Waiters`</a> invoke `scaled`, so those defaults
 * will be scaled automatically. Other than such defaults, however, to get a `Span`
 * to scale you'll need to explicitly pass it to `scaled`.
 * For example, here's how you would scale a `Span` you supply to 
 * the `failAfter` method from trait `Timeouts`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * failAfter(scaled(150 millis)) {
 *   // ...
 * }
 * }}}
 *
 * The reason `Span`s are not scaled automatically in the general case is
 * to make code obvious. If a reader sees `failAfter(1 second)`, it will
 * mean exactly that: fail after one second. And if a `Span` will be scaled,
 * the reader will clearly see that as well: `failAfter(scaled(1 second))`.
 * 
 *
 * ==Overriding `spanScaleFactor`==
 * 
 * 
 * You can override the `spanScaleFactor` method to configure the factor by a
 * different means. For example, to configure the factor from Akka
 * TestKit's test time factor you might create a trait like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.concurrent.ScaledTimeSpans
 * import akka.actor.ActorSystem
 * import akka.testkit.TestKitExtension
 *
 * trait AkkaSpanScaleFactor extends ScaledTimeSpans {
 *   override def spanScaleFactor: Double =
 *       TestKitExtension.get(ActorSystem()).TestTimeFactor
 * }
 * }}}
 *
 * This trait overrides `spanScaleFactor` so that it takes its
 * scale factor from Akka's `application.conf` file.
 * You could then scale `Span`s tenfold in Akka's configuration file
 * like this:
 * 
 *
 * {{{
 * akka {
 *   test {
 *     timefactor = 10.0
 *   }
 * }
 * }}}
 *
 * Armed with this trait and configuration file, you can simply mix trait
 * `AkkaSpanScaleFactor` into any test class whose `Span`s
 * you want to scale, like this:
 * 
 * {{{  <!-- class="stHighlight" -->
 * class MySpec extends FunSpec with Eventually with AkkaSpanScaleFactor {
 *   // ..
 * }
 * }}}
 *
 * @author Bill Venners
 */
trait ScaledTimeSpans {

// TODO: Verify the example works. I just now changed concurrent.SpanScaleFactor into ScaledTimeSpans, because we
// don't actually have a SpanScaleFactor. It was an intermediate trait that was never released final. I'd rather have
// an example that may work than one I know does not work, but better yet is one I know actually does work!
  /**
   * Scales the passed `Span` by the `Double` factor returned
   * by `spanScaleFactor`.
   *
   * The `Span` is scaled by invoking its `scaledBy` method,
   * thus this method has the same behavior:
   * The value returned by `spanScaleFactor` can be any positive number or zero,
   * including a fractional number. A number greater than one will scale the `Span`
   * up to a larger value. A fractional number will scale it down to a smaller value. A
   * factor of 1.0 will cause the exact same `Span` to be returned. A
   * factor of zero will cause `Span.ZeroLength` to be returned.
   * If overflow occurs, `Span.Max` will be returned. If underflow occurs,
   * `Span.ZeroLength` will be returned.
   * 
   *
   * @throws IllegalArgumentException if the value returned from `spanScaleFactor`
   *           is less than zero
   */
  final def scaled(span: Span): Span = span scaledBy spanScaleFactor

  /**
   * The factor by which the `scaled` method will scale `Span`s.
   *
   * The default implementation of this method will return the ''span scale factor'' that 
   * was specified for the run, or 1.0 if no factor was specified. For example, you can specify a span scale factor when invoking ScalaTest
   * via the command line by passing a <a href="../tools/Runner$.html#scalingTimeSpans">`-F` argument</a> to <a href="../tools/Runner$.html">`Runner`</a>.
   * 
   */
  def spanScaleFactor: Double =
    // SKIP-SCALATESTJS-START
    Runner.spanScaleFactor
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY 1.0
}

