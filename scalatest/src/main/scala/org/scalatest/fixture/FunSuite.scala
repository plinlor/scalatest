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

import org.scalatest._

/**
 * A sister class to `org.scalatest.FunSuite` that can pass a fixture object into its tests.
 *
 * <table><tr><td class="usage">
 * '''Recommended Usage''':
 * Use class `fixture.FunSuite` in situations for which <a href="../FunSuite.html">`FunSuite`</a>
 * would be a good choice, when all or most tests need the same fixture objects
 * that must be cleaned up afterwards. ''Note: `fixture.FunSuite` is intended for use in special situations, with class `FunSuite` used for general needs. For
 * more insight into where `fixture.FunSuite` fits in the big picture, see the <a href="../FunSuite.html#withFixtureOneArgTest">`withFixture(OneArgTest)`</a> subsection of the <a href="../FunSuite.html#sharedFixtures">Shared fixtures</a> section in the documentation for class `FunSuite`.''
 * </td></tr></table>
 * 
 * Class `fixture.FunSuite` behaves similarly to class `org.scalatest.FunSuite`, except that tests may have a
 * fixture parameter. The type of the
 * fixture parameter is defined by the abstract `FixtureParam` type, which is a member of this class.
 * This class also contains an abstract `withFixture` method. This `withFixture` method
 * takes a `OneArgTest`, which is a nested trait defined as a member of this class.
 * `OneArgTest` has an `apply` method that takes a `FixtureParam`.
 * This `apply` method is responsible for running a test.
 * This class's `runTest` method delegates the actual running of each test to `withFixture(OneArgTest)`, passing
 * in the test code to run via the `OneArgTest` argument. The `withFixture(OneArgTest)` method (abstract in this class) is responsible
 * for creating the fixture argument and passing it to the test function.
 * 
 * 
 * Subclasses of this class must, therefore, do three things differently from a plain old `org.scalatest.FunSuite`:
 * 
 * 
 * <ol>
 * <li>define the type of the fixture parameter by specifying type `FixtureParam`</li>
 * <li>define the `withFixture(OneArgTest)` method</li>
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
 * To enable the stacking of traits that define `withFixture(NoArgTest)`, it is a good idea to let
 * `withFixture(NoArgTest)` invoke the test function instead of invoking the test
 * function directly. To do so, you'll need to convert the `OneArgTest` to a `NoArgTest`. You can do that by passing
 * the fixture object to the `toNoArgTest` method of `OneArgTest`. In other words, instead of
 * writing &ldquo;`test(theFixture)`&rdquo;, you'd delegate responsibility for
 * invoking the test function to the `withFixture(NoArgTest)` method of the same instance by writing:
 * 
 *
 * {{{
 * withFixture(test.toNoArgTest(theFixture))
 * }}}
 *
 * Here's a complete example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.funsuite.oneargtest
 * 
 * import org.scalatest.fixture
 * import java.io._
 * 
 * class ExampleSuite extends fixture.FunSuite {
 * 
 *   case class FixtureParam(file: File, writer: FileWriter)
 * 
 *   def withFixture(test: OneArgTest) = {
 * 
 *     // create the fixture
 *     val file = File.createTempFile("hello", "world")
 *     val writer = new FileWriter(file)
 *     val theFixture = FixtureParam(file, writer)
 * 
 *     try {
 *       writer.write("ScalaTest is ") // set up the fixture
 *       withFixture(test.toNoArgTest(theFixture)) // "loan" the fixture to the test
 *     }
 *     finally writer.close() // clean up the fixture
 *   }
 * 
 *   test("testing should be easy") { f =&gt;
 *     f.writer.write("easy!")
 *     f.writer.flush()
 *     assert(f.file.length === 18)
 *   }
 * 
 *   test("testing should be fun") { f =&gt;
 *     f.writer.write("fun!")
 *     f.writer.flush()
 *     assert(f.file.length === 17)
 *   }
 * }
 * }}}
 *
 * If a test fails, the `OneArgTest` function will result in a [[org.scalatest.Failed Failed]] wrapping the exception describing the failure.
 * To ensure clean up happens even if a test fails, you should invoke the test function from inside a `try` block and do the cleanup in a
 * `finally` clause, as shown in the previous example.
 * 
 *
 * <a name="sharingFixturesAcrossClasses"></a>==Sharing fixtures across classes==
 *
 * If multiple test classes need the same fixture, you can define the `FixtureParam` and `withFixture(OneArgTest)` implementations
 * in a trait, then mix that trait into the test classes that need it. For example, if your application requires a database and your integration tests
 * use that database, you will likely have many test classes that need a database fixture. You can create a "database fixture" trait that creates a
 * database with a unique name, passes the connector into the test, then removes the database once the test completes. This is shown in the following example:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * package org.scalatest.examples.fixture.funsuite.sharing
 * 
 * import java.util.concurrent.ConcurrentHashMap
 * import org.scalatest.fixture
 * import DbServer._
 * import java.util.UUID.randomUUID
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
 * trait DbFixture { this: fixture.Suite =&gt;
 * 
 *   type FixtureParam = Db
 * 
 *   // Allow clients to populate the database after
 *   // it is created
 *   def populateDb(db: Db) {}
 * 
 *   def withFixture(test: OneArgTest) = {
 *     val dbName = randomUUID.toString
 *     val db = createDb(dbName) // create the fixture
 *     try {
 *       populateDb(db) // setup the fixture
 *       withFixture(test.toNoArgTest(db)) // "loan" the fixture to the test
 *     }
 *     finally removeDb(dbName) // clean up the fixture
 *   }
 * }
 * 
 * class ExampleSuite extends fixture.FunSuite with DbFixture {
 * 
 *   override def populateDb(db: Db) { // setup the fixture
 *     db.append("ScalaTest is ")
 *   }
 * 
 *   test("testing should be easy") { db =&gt;
 *     db.append("easy!")
 *     assert(db.toString === "ScalaTest is easy!")
 *   }
 * 
 *   test("testing should be fun") { db =&gt;
 *     db.append("fun!")
 *     assert(db.toString === "ScalaTest is fun!")
 *   }
 * 
 *   // This test doesn't need a Db
 *   test("test code should be clear") { () =&gt;
 *     val buf = new StringBuffer
 *     buf.append("ScalaTest code is ")
 *     buf.append("clear!")
 *     assert(buf.toString === "ScalaTest code is clear!")
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
 * will not invoke `withFixture(OneArgTest)`. It will instead directly invoke `withFixture(NoArgTest)`.
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
@Finders(Array("org.scalatest.finders.FunSuiteFinder"))
abstract class FunSuite extends FunSuiteLike {

  /**
   * Returns a user friendly string for this suite, composed of the
   * simple name of the class (possibly simplified further by removing dollar signs if added by the Scala interpeter) and, if this suite
   * contains nested suites, the result of invoking `toString` on each
   * of the nested suites, separated by commas and surrounded by parentheses.
   *
   * @return a user-friendly string for this suite
   */
  override def toString: String = Suite.suiteToString(None, this)
}
