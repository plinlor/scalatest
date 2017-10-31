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

import org.scalactic._
import Suite.autoTagClassAnnotations

/**
 * Implementation trait for class `PropSpec`, which represents
 * a suite of property-based tests.
 * 
 * <a href="PropSpec.html">`PropSpec`</a> is a class, not a trait,
 * to minimize compile time given there is a slight compiler overhead to
 * mixing in traits compared to extending classes. If you need to mix the
 * behavior of `PropSpec` into some other class, you can use this
 * trait instead, because class `PropSpec` does nothing more than
 * extend this trait and add a nice `toString` implementation.
 * 
 *
 * See the documentation of the class for a <a href="PropSpec.html">detailed
 * overview of `PropSpec`</a>.
 * 
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.PropSpecFinder"))
//SCALATESTJS-ONLY @scala.scalajs.js.annotation.JSExportDescendentClasses(ignoreInvalidDescendants = true)
trait PropSpecLike extends TestSuite with TestRegistration with Informing with Notifying with Alerting with Documenting { thisSuite =>

  private final val engine = new Engine(Resources.concurrentPropSpecMod, "PropSpec")
  import engine._

  /**
   * Returns an `Informer` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked from inside a scope,
   * it will forward the information to the current reporter immediately.  If invoked from inside a test function,
   * it will record the information and forward it to the current reporter only after the test completed, as `recordedEvents`
   * of the test completed event, such as `TestSucceeded`. If invoked at any other time, it will print to the standard output.
   * This method can be called safely by any thread.
   */
  protected def info: Informer = atomicInformer.get

  /**
   * Returns a `Notifier` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `PropSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an `Alerter` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `PropSpec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def alert: Alerter = atomicAlerter.get

  /**
   * Returns a `Documenter` that during test execution will forward strings passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked from inside a scope,
   * it will forward the information to the current reporter immediately.  If invoked from inside a test function,
   * it will record the information and forward it to the current reporter only after the test completed, as `recordedEvents`
   * of the test completed event, such as `TestSucceeded`. If invoked at any other time, it will print to the standard output.
   * This method can be called safely by any thread.
   */
  protected def markup: Documenter = atomicDocumenter.get

  final def registerTest(testText: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -1
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    engine.registerTest(testText, Transformer(testFun _), Resources.testCannotBeNestedInsideAnotherTest, "PropSpecLike.scala", "registerTest", 4, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
  }

  final def registerIgnoredTest(testText: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepthAdjustment = -4
    engine.registerIgnoredTest(testText, Transformer(testFun _), Resources.testCannotBeNestedInsideAnotherTest, "PropSpecLike.scala", "registerIgnoredTest", 4, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

  /**
   * Register a property-based test with the specified name, optional tags, and function value that takes no arguments.
   * This method will register the test for later execution via an invocation of one of the `run`
   * methods. The passed test name must not have been registered previously on
   * this `PropSpec` instance.
   *
   * @param testName the name of the property
   * @param testTags the optional list of tags for this property
   * @param testFun the property function
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws NotAllowedException if `testName` had been registered previously
   * @throws NullArgumentException if `testName` or any passed test tag is `null`
   */
  protected def property(testName: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -2
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -6
    engine.registerTest(testName, Transformer(testFun _), Resources.propertyCannotAppearInsideAnotherProperty, "PropSpecLike.scala", "property", stackDepth, stackDepthAdjustment, None, None, Some(pos), None, testTags: _*)
  }

  /**
   * Register a property-based test to ignore, which has the specified name, optional tags, and function value that takes no arguments.
   * This method will register the test for later ignoring via an invocation of one of the `run`
   * methods. This method exists to make it easy to ignore an existing test by changing the call to `test`
   * to `ignore` without deleting or commenting out the actual test code. The test will not be run, but a
   * report will be sent that indicates the test was ignored. The passed test name must not have been registered previously on
   * this `PropSpec` instance.
   *
   * @param testName the name of the test
   * @param testTags the optional list of tags for this test
   * @param testFun the test function
   * @throws TestRegistrationClosedException if invoked after `run` has been invoked on this suite
   * @throws DuplicateTestNameException if a test with the same name has been registered previously
   * @throws NotAllowedException if `testName` had been registered previously
   */
  protected def ignore(testName: String, testTags: Tag*)(testFun: => Any /* Assertion */)(implicit pos: source.Position): Unit = {
    // SKIP-SCALATESTJS-START
    val stackDepth = 4
    val stackDepthAdjustment = -3
    // SKIP-SCALATESTJS-END
    //SCALATESTJS-ONLY val stackDepth = 6
    //SCALATESTJS-ONLY val stackDepthAdjustment = -7
    engine.registerIgnoredTest(testName, Transformer(testFun _), Resources.ignoreCannotAppearInsideAProperty, "PropSpecLike.scala", "ignore", stackDepth, stackDepthAdjustment, None, Some(pos), testTags: _*)
  }

  /**
  * An immutable `Set` of test names. If this `PropSpec` contains no tests, this method returns an empty `Set`.
  *
  * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's iterator will
  * return those names in the order in which the tests were registered.
  * 
  */
  override def testNames: Set[String] = {
    InsertionOrderSet(atomic.get.testNamesList)
  }

  /**
   * Run a test. This trait's implementation runs the test registered with the name specified by `testName`.
   *
   * @param testName the name of one test to run.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
   *
   * @throws IllegalArgumentException if `testName` is defined but a test with that name does not exist on this `PropSpec`
   * @throws NullArgumentException if any of `testName`, `reporter`, `stopper`, or `configMap`
   *     is `null`.
   */
  protected override def runTest(testName: String, args: Args): Status = {

    def invokeWithFixture(theTest: TestLeaf): Outcome = {
      val theConfigMap = args.configMap
      val testData = testDataFor(testName, theConfigMap)
      withFixture(
        new NoArgTest {
          val name = testData.name
          def apply(): Outcome = { theTest.testFun() }
          val configMap = testData.configMap
          val scopes = testData.scopes
          val text = testData.text
          val tags = testData.tags
          val pos = testData.pos
        }
      )
    }

    runTestImpl(thisSuite, testName, args, true, invokeWithFixture)
  }

  /**
   * A `Map` whose keys are `String` names of tagged tests and whose associated values are
   * the `Set` of tags for the test. If this `PropSpec` contains no tags, this method returns an empty `Map`.
   *
   * This trait's implementation returns tags that were passed as strings contained in `Tag` objects passed to 
   * methods `property` and `ignore`. 
   * 
   * 
   * In addition, this trait's implementation will also auto-tag tests with class level annotations.  
   * For example, if you annotate `@Ignore` at the class level, all test methods in the class will be auto-annotated with
   * `org.scalatest.Ignore`.
   * 
   */
  override def tags: Map[String, Set[String]] = autoTagClassAnnotations(atomic.get.tagsMap, this)

  /**
   * Run zero to many of this `PropSpec`'s tests.
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
   *
   * @throws NullArgumentException if any of the passed parameters is `null`.
   * @throws IllegalArgumentException if `testName` is defined, but no test with the specified test name
   *     exists in this `Suite`
   */
  protected override def runTests(testName: Option[String], args: Args): Status = {
    runTestsImpl(thisSuite, testName, args, info, true, runTest)
  }

  override def run(testName: Option[String], args: Args): Status = {
    runImpl(thisSuite, testName, args, super.run)
  }

  /**
   * Registers shared tests.
   *
   * This method enables the following syntax for shared tests in a `PropSpec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * propertiesFor(nonEmptyStack(lastValuePushed))
   * }}}
   *
   * This method just provides syntax sugar intended to make the intent of the code clearer.
   * Because the parameter passed to it is
   * type `Unit`, the expression will be evaluated before being passed, which
   * is sufficient to register the shared tests. For examples of shared tests, see the
   * <a href="#sharedTests">Shared tests section</a> in the main documentation for this trait.
   * 
   */
  protected def propertiesFor(unit: Unit): Unit = {}
  
  /**
   * Suite style name.
   */
  final override val styleName: String = "org.scalatest.PropSpec"
  
  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = createTestDataFor(testName, theConfigMap, this)
}
