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
import org.scalatest.exceptions._
import Spec._
import Suite._
import scala.reflect.NameTransformer._
import StackDepthExceptionHelper.posOrElseStackDepthFun
import java.lang.reflect.{Method, Modifier, InvocationTargetException}
import org.scalactic.{source, Prettifier}
import org.scalatest.Suite.{IgnoreTagName, autoTagClassAnnotations}
import org.scalatest.events.{TopOfClass, TopOfMethod}


/**
 * '''Trait `fixture.SpecLike` has been deprecated and will be removed in a future version of ScalaTest. Please use
 * `org.scalatest.fixture.FunSpec` instead.'''
 *
 * Because this style uses reflection at runtime to discover scopes and tests, it can only be supported on the JVM, not Scala.js.
 * Thus in ScalaTest 3.0.0, class `org.scalatest.SpecLike` was moved to the `org.scalatest.refspec` package and renamed
 * `RefSpecLike`, with the intention of later moving it to a separate module available only on the JVM. If the 
 * `org.scalatest.refspec._` package contained a `fixture` subpackage, then importing `org.scalatest.refspec._`
 * would import the name `fixture` as `org.scalatest.refspec.fixture`. This would likely be confusing for users,
 * who expect `fixture` to mean `org.scalatest.fixture`.
 * 
 *
 * As a result this class has been deprecated and will ''not''
 * be moved to package `org.scalatest.refspec`. Instead we recommend you rewrite any test classes that currently extend
 * `org.scalatest.fixture.SpecLike` to extend <a href="FunSpecLike.html">`org.scalatest.fixture.FunSpecLike`</a> instead,
 * replacing any scope `object`
 * with a `describe` clause, and any test method with an `it` clause.
 * 
 *
 * @author Bill Venners
 */
@Finders(Array("org.scalatest.finders.SpecFinder"))
@deprecated("fixture.SpecLike has been deprecated and will be removed in a future version of ScalaTest. Please use org.scalatest.fixture.FunSpecLike instead.")
trait SpecLike extends TestSuite with Informing with Notifying with Alerting with Documenting  { thisSuite => 

  private final val engine = new FixtureEngine[FixtureParam](Resources.concurrentSpecMod, "Spec")
  import engine._
  // Sychronized on thisSuite, only accessed from ensureScopesAndTestsRegistered
  private var scopesRegistered = false

  private def ensureScopesAndTestsRegistered(): Unit = {

    thisSuite.synchronized {
      if (!scopesRegistered) {
        scopesRegistered = true
        def getMethod(o: AnyRef, testName: String) = { 
          val methodName = encode(simpleNameForTest(testName))
          val candidateMethods = o.getClass.getMethods.filter(_.getName == methodName)
          if (candidateMethods.size == 0)
            throw new IllegalArgumentException(Resources.testNotFound(testName))
          candidateMethods(0)
        }
        
        def getMethodTags(o: AnyRef, methodName: String) =
          for {
            a <- getMethod(o, methodName).getDeclaredAnnotations
            annotationClass = a.annotationType
            if annotationClass.isAnnotationPresent(classOf[TagAnnotation])
          } yield annotationClass.getName
          
        def getScopeClassName(o: AnyRef): String = {
          val className = o.getClass.getName
          if (className.endsWith("$"))
            className 
          else
            className + "$"
        }
          
        def isScopeMethod(o: AnyRef, m: Method): Boolean = {
          val scopeMethodName = getScopeClassName(o)+ m.getName + "$"
          
          val returnTypeName = m.getReturnType.getName
          
          equalIfRequiredCompactify(scopeMethodName, returnTypeName)
        }
        
        def getScopeDesc(m: Method): String = {
          val objName = m.getReturnType.getName
          val objClassName = decode(objName.substring(0, objName.length - 1))
          objClassName.substring(objClassName.lastIndexOf("$") + 1)
        }
        
        val testTags = tags
        object MethodNameEncodedOrdering extends Ordering[Method] {
          def compare(x: Method, y: Method): Int = {
            decode(x.getName) compareTo decode(y.getName)
          }
        }
        
        def register(o: AnyRef): Unit = {
          val testMethods = o.getClass.getMethods.filter(isTestMethod(_)).sorted(MethodNameEncodedOrdering)
          
// TODO: Detect duplicate test names, one with fixture param and one without.
          testMethods.foreach { m =>
            val scope = isScopeMethod(o, m)
            if (scope) {
              val scopeDesc = getScopeDesc(m)
              def scopeFun: Unit = {
                try {
                  val scopeObj = m.invoke(o)
                  register(scopeObj)
                }
                catch {
                  case ite: InvocationTargetException if ite.getTargetException != null =>
                    throw ite.getTargetException
                }
              }
              val scopeLocation = TopOfClass(m.getReturnType.getName)
              try {
                registerNestedBranch(scopeDesc, None, scopeFun, Resources.registrationAlreadyClosed, sourceFileName, "ensureScopesAndTestsRegistered", 2, 0, Some(scopeLocation), None)
              }
              catch {
                case e: TestFailedException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideDefNotObject, Some(e), posOrElseStackDepthFun(e.position, (_: StackDepthException) => 8))
                case e: TestCanceledException => throw new NotAllowedException(FailureMessages.assertionShouldBePutInsideDefNotObject, Some(e), posOrElseStackDepthFun(e.position, (_: StackDepthException) => 8))
                case dtne: DuplicateTestNameException => throw dtne
                case other: Throwable if (!Suite.anExceptionThatShouldCauseAnAbort(other)) =>
                  if (ScalaTestVersions.BuiltForScalaVersion == "2.12" || ScalaTestVersions.BuiltForScalaVersion == "2.13")
                    throw new NotAllowedException(FailureMessages.exceptionWasThrownInObject(Prettifier.default, UnquotedString(other.getClass.getName), UnquotedString(scopeDesc)), Some(other), Right((_: StackDepthException) => 9))
                  else
                    throw new NotAllowedException(FailureMessages.exceptionWasThrownInObject(Prettifier.default, UnquotedString(other.getClass.getName), UnquotedString(scopeDesc)), Some(other), Right((_: StackDepthException) => 8))
                case other: Throwable => throw other
              }
            }
            else {
              val methodName = m.getName
              val testName = 
                // if (m.getParameterTypes.length == 0)
                  decode(methodName)
                // else
                  // decode(methodName) + FixtureInParens
              val methodTags = getMethodTags(o, testName)
              val testFun: FixtureParam => Unit = (fixture: FixtureParam) => { 
                val anyRefFixture: AnyRef = fixture.asInstanceOf[AnyRef] // TODO zap this cast
                val argsArray: Array[Object] = 
                  if (m.getParameterTypes.length == 0)
                    Array.empty
                  else
                    Array(anyRefFixture)  
                try m.invoke(o, argsArray: _*)
                catch {
                  case ite: InvocationTargetException => 
                    throw ite.getTargetException
                }
              }
          
              val testLocation = TopOfMethod(getScopeClassName(o), m.toGenericString)
              val isIgnore = testTags.get(methodName) match {
                case Some(tagSet) => tagSet.contains(IgnoreTagName) || methodTags.contains(IgnoreTagName)
                case None => methodTags.contains(IgnoreTagName)
              }

              val registerTestStackDepth =
                if (ScalaTestVersions.BuiltForScalaVersion == "2.12" || ScalaTestVersions.BuiltForScalaVersion == "2.13")
                  3
                else
                  2

              if (isIgnore)
                registerIgnoredTest(testName, Transformer(testFun), Resources.registrationAlreadyClosed, sourceFileName, "ensureScopesAndTestsRegistered", 3, 0, Some(testLocation), None, methodTags.map(new Tag(_)): _*)
              else
                registerTest(testName, Transformer(testFun), Resources.registrationAlreadyClosed, sourceFileName, "ensureScopesAndTestsRegistered", registerTestStackDepth, 1, None, Some(testLocation), None, None, methodTags.map(new Tag(_)): _*)
            }
          }
        }
     
        register(thisSuite)
      }
    }
  }

  // TODO: Probably make this private final val sourceFileName in a singleton object so it gets compiled in rather than carried around in each instance
  private[scalatest] val sourceFileName = "SpecLike.scala"

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
   * `Spec` is being executed, such as from inside a test function, it will forward the information to
   * the current reporter immediately. If invoked at any other time, it will
   * print to the standard output. This method can be called safely by any thread.
   */
  protected def note: Notifier = atomicNotifier.get

  /**
   * Returns an `Alerter` that during test execution will forward strings (and other objects) passed to its
   * `apply` method to the current reporter. If invoked in a constructor, it
   * will register the passed string for forwarding later during test execution. If invoked while this
   * `Spec` is being executed, such as from inside a test function, it will forward the information to
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
  
  /**
   * An immutable `Set` of test names. If this `Spec` contains no tests, this method returns an
   * empty `Set`.
   *
   * This trait's implementation of this method will return a set that contains the names of all registered tests. The set's
   * iterator will return those names in the order in which the tests were registered. Each test's name is composed
   * of the concatenation of the name of each surrounding ''scope object'', in order from outside in, and the name of the
   * test method itself, with all components separated by a space. For example, consider this `Spec`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.refspec.RefSpec
   *
   * class StackSpec extends RefSpec {
   *   object &#96;A Stack&#96; {
   *     object &#96;(when not empty)&#96; {
   *       def &#96;must allow me to pop&#96; {}
   *     }
   *     object &#96;(when not full)&#96; {
   *       def &#96;must allow me to push&#96; {}
   *     }
   *   }
   * }
   * }}}
   *
   * Invoking `testNames` on this `Spec` will yield a set that contains the following
   * two test name strings:
   * 
   *
   * {{{ class="stExamples">
   * "A Stack (when not empty) must allow me to pop"
   * "A Stack (when not full) must allow me to push"
   * }}}
   *
   * This trait's implementation of this method will first ensure that the discovery of scope objects and test methods
   * has been performed.
   * 
   *
   * @return the `Set` of test names
   */
  override def testNames: Set[String] = {
    ensureScopesAndTestsRegistered()
    InsertionOrderSet(atomic.get.testNamesList)
  }
  
  /**
   * Run a test. This trait's implementation runs the test registered with the name specified by
   * `testName`. Each test's name is a concatenation of the text of all ''scope objects'' surrounding a test,
   * from outside in, and the test method's name, with one space placed between each item. (See the documentation
   * for `testNames` for an example.)
   *
   * This trait's implementation of this method will first ensure that the discovery of scope objects and test methods
   * has been performed.
   * 
   *
   * @param testName the name of one test to execute.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when the test started by this method has completed, and whether or not it failed .
   * @throws NullArgumentException if `testName` or `args` is `null`.
   */
  protected override def runTest(testName: String, args: Args): Status = {

    ensureScopesAndTestsRegistered()

    def invokeWithFixture(theTest: TestLeaf): Outcome = {
      val theConfigMap = args.configMap
      val testData = testDataFor(testName, theConfigMap)
      withFixture(
        new OneArgTest {
          val name = testData.name
          def apply(fixture: FixtureParam): Outcome = { theTest.testFun(fixture) }
          val configMap = testData.configMap
          val scopes = testData.scopes
          val text = testData.text
          val tags = testData.tags
          val pos = testData.pos
        }
        //new TestFunAndConfigMap(testName, theTest.testFun, theConfigMap)
      )
    }

    runTestImpl(thisSuite, testName, args, true, invokeWithFixture)
  }
  
  /**
   * The total number of tests that are expected to run when this `Spec`'s `run` method is invoked.
   *
   * This trait's implementation of this method returns the sum of:
   * 
   *
   * <ul>
   * <li>the size of the `testNames` `List`, minus the number of tests marked as ignored and
   * any tests that are exluded by the passed `Filter`</li>
   * <li>the sum of the values obtained by invoking
   *     `expectedTestCount` on every nested `Suite` contained in
   *     `nestedSuites`</li>
   * </ul>
   *
   * This trait's implementation of this method will first ensure that the discovery of scope objects and test methods
   * has been performed.
   * 
   *
   * @param filter a `Filter` with which to filter tests to count based on their tags
   * @return the expected number test count
   */
  final override def expectedTestCount(filter: Filter): Int = {
    ensureScopesAndTestsRegistered()
    super.expectedTestCount(filter)
  }

  /**
   * A `Map` whose keys are `String` tag names to which tests in this `Spec` belong, and values
   * the `Set` of test names that belong to each tag. If this `Spec` contains no tags, this method returns an empty `Map`.
   *
   * This trait's implementation returns tags that were passed as strings contained in `Tag` objects passed to 
   * methods `test` and `ignore`. 
   * 
   * 
   * In addition, this trait's implementation will also auto-tag tests with class level annotations.  
   * For example, if you annotate @Ignore at the class level, all test methods in the class will be auto-annotated with @Ignore.
   * 
   *
   * This trait's implementation of this method will first ensure that the discovery of scope objects and test methods
   * has been performed.
   * 
   */
  override def tags: Map[String, Set[String]] = {
    ensureScopesAndTestsRegistered()
    autoTagClassAnnotations(atomic.get.tagsMap, this)
  }

  /**
   * Run zero to many of this `Spec`'s tests.
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Spec`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests started by this method have completed, and whether or not a failure occurred.
   * @throws NullArgumentException if any of the passed parameters is `null`.
   * @throws IllegalArgumentException if `testName` is defined, but no test with the specified test name
   *     exists in this `Spec`
   */
  protected override def runTests(testName: Option[String], args: Args): Status = {
    ensureScopesAndTestsRegistered()
    runTestsImpl(thisSuite, testName, args, info, true, runTest)
  }

  /**
   * Runs this `fixture.Spec`.
   *
   * <p>If `testName` is `None`, this trait's implementation of this method
   * calls these two methods on this object in this order:
   *
   * <ol>
   * <li>`runNestedSuites(report, stopper, tagsToInclude, tagsToExclude, configMap, distributor)`</li>
   * <li>`runTests(testName, report, stopper, tagsToInclude, tagsToExclude, configMap)`</li>
   * </ol>
   *
   * If `testName` is defined, then this trait's implementation of this method
   * calls `runTests`, but does not call `runNestedSuites`. This behavior
   * is part of the contract of this method. Subclasses that override `run` must take
   * care not to call `runNestedSuites` if `testName` is defined. (The
   * `OneInstancePerTest` trait depends on this behavior, for example.)
   * 
   *
   * This trait's implementation of this method will first ensure that the discovery of scope objects and test methods
   * has been performed.
   * 
   *
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `fixture.SpecLike`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests and nested suites started by this method have completed, and whether or not a failure occurred.
   * @throws NullArgumentException if any passed parameter is `null`.
   * @throws IllegalArgumentException if `testName` is defined, but no test with the specified test name
   *     exists in this `Suite`
   */
  override def run(testName: Option[String], args: Args): Status = {
    ensureScopesAndTestsRegistered()
    runImpl(thisSuite, testName, args, super.run)
  }
  
  /**
   * Suite style name.
   *
   * @return `org.scalatest.fixture.Spec`
   */
  final override val styleName: String = "org.scalatest.fixture.Spec"

  override def testDataFor(testName: String, theConfigMap: ConfigMap = ConfigMap.empty): TestData = createTestDataFor(testName, theConfigMap, this)
}
