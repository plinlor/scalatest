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
package org.scalatest.fixture


/**
 * A function that takes no parameters (''i.e.'', a `Function0` or "no-arg" function) and results in `Unit`, which when
 * invoked executes the body of the constructor of the class into which this trait is mixed.
 *
 * This trait extends `DelayedInit` and defines a `delayedInit` method that
 * saves the body of the constructor (passed to `delayedInit`) for later execution when `apply` is invoked.
 * 
 *
 * This trait is somewhat magical and therefore may be challenging for your collegues to understand, so please use it as a last resort only when the
 * simpler options described in the "<a href="../FlatSpec.html#sharedFixtures">shared fixtures</a>" section of your chosen style trait won't do
 * the job. `NoArg` is
 * intended to address a specific use case that will likely be rare, and is unlikely to be useful outside of its intended use case, but
 * it is quite handy for its intended use case (described in the next paragraph).
 * One potential gotcha, for example, is that a subclass's constructor body could in theory be executed multiple times by simply invoking `apply` multiple
 * times. In the intended use case for this trait, however, the body will be executed only once.
 * 
 *
 * The intended use case for this method is (relatively rare) situations in which you want to extend a different instance of the same class
 * for each test, with the body of the test inheriting the members of that class, and with code executed before and/or after
 * the body of the test.
 * 
 *
 * For example, Akka's `TestKit` class takes an `ActorSystem`,
 * which must have a unique name. To run a suite of tests in parallel, each test must get its own `ActorSystem`, to
 * ensure the tests run in isolation. At the end of each test, the `ActorSystem` must be shutdown. With `NoArg`,
 * you can achieve this by first defining a class that extends `TestKit` and mixes in `NoArg`.
 * Here's an example taken with permission from the book <a href="http://www.artima.com/shop/akka_concurrency">''Akka Concurrency''</a>, by Derek Wyatt:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import akka.actor.ActorSystem
 * import akka.testkit.{TestKit, ImplicitSender}
 * import java.util.concurrent.atomic.AtomicInteger
 * import org.scalatest.fixture.NoArg
 *
 * object ActorSys {
 *   val uniqueId = new AtomicInteger(0)
 * }
 *
 * class ActorSys(name: String) extends
 *         TestKit(ActorSystem(name))
 *         with ImplicitSender
 *         with NoArg {
 *
 *   def this() = this(
 *     "TestSystem%05d".format(
 *        ActorSys.uniqueId.getAndIncrement()))
 *
 *   def shutdown(): Unit = system.shutdown()
 *
 *   override def apply() {
 *     try super.apply()
 *     finally shutdown()
 *   }
 * }
 * }}}
 *
 * Given this implementation of `ActorSys`, which will invoke `shutdown` after the constructor code
 * is executed, you can run each test in a suite in a subclass of `TestKit`, giving each test's `TestKit`
 * an `ActorSystem` with a unique name, allowing you to safely run those tests in parallel. Here's an example
 * from ''Akka Concurrency'':
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class MyActorSpec extends fixture.WordSpec
 *         with Matchers
 *         with UnitFixture
 *         with ParallelTestExecution {
 * 
 *   def makeActor(): ActorRef =
 *     system.actorOf(Props[MyActor], "MyActor")
 * 
 *   "My Actor" should {
 *     "throw when made with the wrong name" in new ActorSys {
 *       an [Exception] should be thrownBy {
 *         // use a generated name
 *         val a = system.actorOf(Props[MyActor])
 *       }
 *     }
 *     "construct without exception" in new ActorSys {
 *       val a = makeActor()
 *       // The throw will cause the test to fail
 *     }
 *     "respond with a Pong to a Ping" in new ActorSys {
 *       val a = makeActor()
 *       a ! Ping
 *       expectMsg(Pong)
 *     }
 *   }
 * }
 * }}}
 *
 * <a href="UnitFixture.html">`UnitFixture`</a> is used in this example, because in this case, the `fixture.WordSpec` feature enabling tests to be defined as
 * functions from fixture objects of type `FixtureParam` to `Unit` is not being used. Rather, only the secondary feature that enables
 * tests to be defined as functions from ''no parameters'' to `Unit` is being used. This secondary feature is described in the second-to-last
 * paragraph on the main Scaladoc documentation of <a href="WordSpec.html">`fixture.WordSpec`</a>, which says:
 * 
 *
 * <blockquote>
 * If a test doesn't need the fixture, you can indicate that by providing a no-arg instead of a one-arg function, ...
 * In other words, instead of starting your function literal
 * with something like &ldquo;`db =&gt;`&rdquo;, you'd start it with &ldquo;`() =&gt;`&rdquo;. For such tests, `runTest`
 * will not invoke `withFixture(OneArgTest)`. It will instead directly invoke `withFixture(NoArgTest)`.
 * </blockquote>
 *
 * Since `FixtureParam` is unused in this use case, it could 
 * be anything. Making it `Unit` will hopefully help readers more easily recognize that it is not being used.
 * 
 *
 * Note: As of Scala 2.11, `DelayedInit` (which is used by `NoArg`) has been deprecated, to indicate it is buggy and should be avoided
 * if possible. Those in charge of the Scala compiler and standard library have promised that `DelayedInit` will not be removed from Scala
 * unless an alternate way to achieve the same goal is provided. Thus it ''should'' be safe to use `NoArg`, but if you'd rather
 * not you can achieve the same effect with a bit more boilerplate by extending (`() =&gt; Unit`) instead of `NoArg` and placing
 * your code in an explicit `body` method. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import akka.actor.ActorSystem
 * import akka.testkit.{TestKit, ImplicitSender}
 * import java.util.concurrent.atomic.AtomicInteger
 * import org.scalatest.fixture.NoArg
 *
 * object ActorSys {
 *   val uniqueId = new AtomicInteger(0)
 * }
 *
 * class ActorSys(name: String) extends
 *         TestKit(ActorSystem(name))
 *         with ImplicitSender
 *         with (() =&gt; Unit) {
 *
 *   def this() = this(
 *     "TestSystem%05d".format(
 *        ActorSys.uniqueId.getAndIncrement()))
 *
 *   def shutdown(): Unit = system.shutdown()
 *   def body(): Unit
 *
 *   override def apply() = {
 *     try body()
 *     finally shutdown()
 *   }
 * }
 * }}}
 *
 * Using this version of `ActorSys` will require an explicit
 * `body` method in the tests:
 *
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class MyActorSpec extends fixture.WordSpec
 *         with Matchers
 *         with UnitFixture
 *         with ParallelTestExecution {
 * 
 *   def makeActor(): ActorRef =
 *     system.actorOf(Props[MyActor], "MyActor")
 * 
 *   "My Actor" should {
 *     "throw when made with the wrong name" in new ActorSys {
 *       def body() = 
 *         an [Exception] should be thrownBy {
 *           // use a generated name
 *           val a = system.actorOf(Props[MyActor])
 *         }
 *     }
 *     "construct without exception" in new ActorSys {
 *       def body() = {
 *         val a = makeActor()
 *         // The throw will cause the test to fail
 *       }
 *     }
 *     "respond with a Pong to a Ping" in new ActorSys {
 *       def body() = {
 *         val a = makeActor()
 *         a ! Ping
 *         expectMsg(Pong)
 *       }
 *     }
 *   }
 * }
 * }}}
 *
 */
trait NoArg extends DelayedInit with (() => Unit) {

  private var theBody: () => Unit = _

  /**
   * Saves the body of the constructor, passed as `body`, for later execution by `apply`.
   */
  final def delayedInit(body: => Unit): Unit = {
    synchronized { theBody = (() => body) }
  }

  /**
   * Executes the body of the constructor that was passed to `delayedInit`.
   */
  def apply(): Unit = synchronized { if (theBody != null) theBody() }

  /**
   * This method exists to cause a compile-time type error if someone accidentally 
   * tries to mix this trait into a `Suite`.
   *
   * This trait is intended to be mixed
   * into classes that are constructed within the body (or as the body) of tests, not mixed into `Suite`s themselves. For an example,
   * the the main Scaladoc comment for this trait.
   * 
   */
  final val styleName: Int = 0 // So can't mix into Suite
}

