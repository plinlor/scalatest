/*
 * Copyright 2001-2014 Artima, Inc.
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
 * A sister class to `org.scalatest.AsyncFunSuite` that can pass a fixture object into its tests.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * Use class `fixture.AsyncFunSuite` in situations for which <a href="../AsyncFunSuite.html">`AsyncFunSuite`</a>
 * would be a good choice, when all or most tests need the same fixture objects
 * that must be cleaned up afterwards. ''Note: `fixture.AsyncFunSuite` is intended for use in special situations, with class `AsyncFunSuite` used for general needs. For
 * more insight into where `fixture.AsyncFunSuite` fits in the big picture, see the <a href="../AsyncFunSuite.html#withFixtureOneArgAsyncTest">`withFixture(OneArgAsyncTest)`</a> subsection of the <a href="../AsyncFunSuite.html#sharedFixtures">Shared fixtures</a> section in the documentation for class `AsyncFunSuite`.''
 * </td></tr></table>
 * 
 * Class `fixture.AsyncFunSuite` behaves similarly to class `org.scalatest.AsyncFunSuite`, except that tests may have a
 * fixture parameter. The type of the
 * fixture parameter is defined by the abstract `FixtureParam` type, which is a member of this class.
 * This class also contains an abstract `withFixture` method. This `withFixture` method
 * takes a `OneArgAsyncTest`, which is a nested trait defined as a member of this class.
 * `OneArgAsyncTest` has an `apply` method that takes a `FixtureParam`.
 * This `apply` method is responsible for running a test.
 * This class's `runTest` method delegates the actual running of each test to `withFixture(OneArgAsyncTest)`, passing
 * in the test code to run via the `OneArgAsyncTest` argument. The `withFixture(OneArgAsyncTest)` method (abstract in this class) is responsible
 * for creating the fixture argument and passing it to the test function.
 * 
 * 
 * Subclasses of this class must, therefore, do three things differently from a plain old `org.scalatest.AsyncFunSuite`:
 * 
 * 
 * <ol>
 * <li>define the type of the fixture parameter by specifying type `FixtureParam`</li>
 * <li>define the `withFixture(OneArgAsyncTest)` method</li>
 * <li>write tests that take a fixture parameter</li>
 * <li>(You can also define tests that don't take a fixture parameter.)</li>
 * </ol>
 *
 * If the fixture you want to pass into your tests consists of multiple objects, you will need to combine
 * them into one object to use this class. One good approach to passing multiple fixture objects is
 * to encapsulate them in a case class. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * case class FixtureParam(file: File, writer: FileWriter)
 * }}}
 *
 * To enable the stacking of traits that define `withFixture(NoArgAsyncTest)`, it is a good idea to let
 * `withFixture(NoArgAsyncTest)` invoke the test function instead of invoking the test
 * function directly. To do so, you'll need to convert the `OneArgAsyncTest` to a `NoArgAsyncTest`. You can do that by passing
 * the fixture object to the `toNoArgAsyncTest` method of `OneArgAsyncTest`. In other words, instead of
 * writing &ldquo;`test(theFixture)`&rdquo;, you'd delegate responsibility for
 * invoking the test function to the `withFixture(NoArgAsyncTest)` method of the same instance by writing:
 * 
 *
 * {{{
 * withFixture(test.toNoArgAsyncTest(theFixture))
 * }}}
 *
 * Here's a complete example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.asyncfunsuite.oneargasynctest
 * 
 * import org.scalatest._
 * import java.io._
 * import scala.concurrent.Future
 * import scala.concurrent.ExecutionContext
 * 
 * // Defining actor messages
 * sealed abstract class StringOp
 * case object Clear extends StringOp
 * case class Append(value: String) extends StringOp
 * case object GetValue
 * 
 * class StringActor { // Simulating an actor
 *   private final val sb = new StringBuilder
 *   def !(op: StringOp): Unit =
 *     synchronized {
 *       op match {
 *         case Append(value) =&gt; sb.append(value)
 *         case Clear =&gt; sb.clear()
 *       }
 *     }
 *   def ?(get: GetValue.type)(implicit c: ExecutionContext): Future[String] =
 *     Future {
 *       synchronized { sb.toString }
 *     }
 * }
 * 
 * class ExampleSuite extends fixture.AsyncFunSuite {
 * 
 *   type FixtureParam = StringActor
 * 
 *   def withFixture(test: OneArgAsyncTest): FutureOutcome = {
 * 
 *     val actor = new StringActor
 *     complete {
 *       actor ! Append("ScalaTest is ") // set up the fixture
 *       withFixture(test.toNoArgAsyncTest(actor))
 *     } lastly {
 *       actor ! Clear // ensure the fixture will be cleaned up
 *     }
 *   }
 * 
 *   test("Testing should be easy") { actor =&gt;
 *     actor ! Append("easy!")
 *     val futureString = actor ? GetValue
 *     futureString map { s =&gt;
 *       assert(s === "ScalaTest is easy!")
 *     }
 *   }
 * 
 *   test("Testing should be fun") { actor =&gt;
 *     actor ! Append("fun!")
 *     val futureString = actor ? GetValue
 *     futureString map { s =&gt;
 *       assert(s === "ScalaTest is fun!")
 *     }
 *   }
 * }
 * }}}
 *
 * If a test fails, the future returned by the `OneArgAsyncTest` function will result in
 * an [[org.scalatest.Failed org.scalatest.Failed]] wrapping the exception describing
 * the failure. To ensure clean up happens even if a test fails, you should invoke the test function and do the cleanup using
 * `complete`-`lastly`, as shown in the previous example. The `complete`-`lastly` syntax, defined in `CompleteLastly`, which is extended by `AsyncTestSuite`, ensures
 * the second, cleanup block of code is executed, whether the the first block throws an exception or returns a future. If it returns a
 * future, the cleanup will be executed when the future completes.
 * 
 *
 * <a name="sharingFixturesAcrossClasses"></a>==Sharing fixtures across classes==
 *
 * If multiple test classes need the same fixture, you can define the `FixtureParam` and `withFixture(OneArgAsyncTest)`
 * implementations in a trait, then mix that trait into the test classes that need it. For example, if your application requires a database and your
 * integration tests use that database, you will likely have many test classes that need a database fixture. You can create a "database fixture" trait
 * that creates a database with a unique name, passes the connector into the test, then removes the database once the test completes. This is shown in
 * the following example:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.fixture.asyncfunsuite.sharing
 *
 * import java.util.concurrent.ConcurrentHashMap
 * import org.scalatest._
 * import DbServer._
 * import java.util.UUID.randomUUID
 * import scala.concurrent.Future
 *
 * object DbServer { // Simulating a database server
 *   type Db = StringBuffer
 *   private val databases = new ConcurrentHashMap[String, Db]
 *   def createDb(name: String): Db = {
 *     val db = new StringBuffer
 *     databases.put(name, db)
 *     db
 *   }
 *   def removeDb(name: String) {
 *     databases.remove(name)
 *   }
 * }
 *
 * trait DbFixture { this: fixture.AsyncTestSuite =&gt;
 *
 *   type FixtureParam = Db
 *
 *   // Allow clients to populate the database after
 *   // it is created
 *   def populateDb(db: Db) {}
 *
 *   def withFixture(test: OneArgAsyncTest): FutureOutcome = {
 *     val dbName = randomUUID.toString
 *     val db = createDb(dbName) // create the fixture
 *     complete {
 *       populateDb(db) // setup the fixture
 *       withFixture(test.toNoArgAsyncTest(db)) // "loan" the fixture to the test
 *     } lastly {
 *       removeDb(dbName) // ensure the fixture will be cleaned up
 *     }
 *   }
 * }
 *
 * class ExampleSuite extends fixture.AsyncFunSuite with DbFixture {
 *
 *   override def populateDb(db: Db) { // setup the fixture
 *     db.append("ScalaTest is ")
 *   }
 *
 *   test("testing should be easy") { db =&gt;
 *     Future {
 *       db.append("easy!")
 *       assert(db.toString === "ScalaTest is easy!")
 *     }
 *   }
 *
 *   test("testing should be fun") { db =&gt;
 *     Future {
 *       db.append("fun!")
 *       assert(db.toString === "ScalaTest is fun!")
 *     }
 *   }
 *
 *   // This test doesn't need a Db
 *   test("test code should be clear") { () =&gt;
 *     Future {
 *       val buf = new StringBuffer
 *       buf.append("ScalaTest code is ")
 *       buf.append("clear!")
 *       assert(buf.toString === "ScalaTest code is clear!")
 *     }
 *   }
 * }
 * }}}
 *
 * Often when you create fixtures in a trait like `DbFixture`, you'll still need to enable individual test classes
 * to "setup" a newly created fixture before it gets passed into the tests. A good way to accomplish this is to pass the newly
 * created fixture into a setup method, like `populateDb` in the previous example, before passing it to the test
 * function. Classes that need to perform such setup can override the method, as does `ExampleSuite`.
 * 
 *
 * If a test doesn't need the fixture, you can indicate that by providing a no-arg instead of a one-arg function, as is done in the
 * third test in the previous example, &ldquo;`test code should be clear`&rdquo;. In other words, instead of starting your function literal
 * with something like &ldquo;`db =&gt;`&rdquo;, you'd start it with &ldquo;`() =&gt;`&rdquo;. For such tests, `runTest`
 * will not invoke `withFixture(OneArgAsyncTest)`. It will instead directly invoke `withFixture(NoArgAsyncTest)`.
 * 
 *
 *
 * Both examples shown above demonstrate the technique of giving each test its own "fixture sandbox" to play in. When your fixtures
 * involve external side-effects, like creating files or databases, it is a good idea to give each file or database a unique name as is
 * done in these examples. This keeps tests completely isolated, allowing you to run them in parallel if desired. You could mix
 * <a href="../ParallelTestExecution.html">`ParallelTestExecution`</a> into either of these `ExampleSuite` classes, and the tests would run in parallel just fine.
 * 
 *
 * @author Bill Venners
 */
abstract class AsyncFunSuite extends AsyncFunSuiteLike {

  /**
   * Returns a user friendly string for this suite, composed of the
   * simple name of the class (possibly simplified further by removing dollar signs if added by the Scala interpeter) and, if this suite
   * contains nested suites, the result of invoking `toString` on each
   * of the nested suites, separated by commas and surrounded by parentheses.
   *
   * @return a user-friendly string for this suite
   */
  override def toString: String = org.scalatest.Suite.suiteToString(None, this)
}
