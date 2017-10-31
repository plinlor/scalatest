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

import exceptions.TestCanceledException
import exceptions.StackDepthException

import reflect.ClassTag
import collection.immutable.MapLike
import org.scalactic.Equality
import org.scalactic.source
import enablers.Containing
import enablers.Aggregating
import enablers.KeyMapping
import enablers.ValueMapping
import org.scalatest.exceptions.StackDepthException

import scala.collection.GenTraversable

// TODO: Oops. Need to pass ConfigMap not Map[String, Any] in TestStarting.
/**
 * A map of configuration data.
 *
 * A `ConfigMap` can be populated from the <a href="tools/Runner$.html">`Runner`</a> command line via `-D` 
 * arguments. `Runner` passes it to many methods where you can use it to configure your
 * test runs. For example, `Runner` passed the `ConfigMap` to:
 * 
 * 
 * <ul>
 * <li>the `apply` method of <a href="Reporter.html">`Reporter`</a>s via `RunStarting` events</li>
 * <li>the `run` method of <a href="Suite.html">`Suite`</a>
 * <li>the `runNestedSuites` method of `Suite`
 * <li>the `runTests` method of `Suite`
 * <li>the `runTest` method of `Suite`
 * <li>the `withFixture(NoArgTest)` method of `Suite`
 * <li>the `withFixture(OneArgTest)` method of <a href="fixture/Suite.html">`fixture.Suite`</a>
 * <li>the `beforeEach(TestData)` method of <a href="BeforeAndAfterEachTestData.html">`BeforeAndAfterEachTestData`</a>
 * <li>the `afterEach(TestData)` method of `BeforeAndAfterEachTestData`
 * </ul>
 *
 * In addition to accessing the `ConfigMap` in overriden implementations of the above methods, you can also transform
 * and pass along a modified `ConfigMap`.
 * 
 *
 * A `ConfigMap` maps string keys to values of any type, ''i.e.'', it is a `Map[String, Any]`.
 * To get a configuration value in a variable of the actual type of that value, therefore, you'll need to perform an unsafe cast. If
 * this cast fails, you'll get an exception, which so long as the `ConfigMap` is used only in tests, will
 * result in either a failed or canceled test or aborted suite. To give such exceptions nice stack depths and error messages, and to
 * eliminate the need for using `asInstanceOf` in your test code, `ConfigMap` provides three
 * methods for accessing values at expected types.
 * 
 *
 * The `getRequired` method returns the value bound to a key cast to a specified type, or throws <a href="exceptions/TestCanceledException.html">`TestCanceledException`</a>
 * if either the key is not bound or is bound to an incompatible type. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val tempFileName: String = configMap.getRequired[String]("tempFileName")
 * }}}
 *
 * The `getOptional` method returns the value bound to a key cast to a specified type, wrapped in a `Some`,
 * returns `None` if the key is not bound, or throws `TestCanceledException` if the key exists but is
 * bound to an incompatible type. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val tempFileName: Option[String] = configMap.getOptional[String]("tempFileName")
 * }}}
 *
 * The `getWithDefault` method returns the value bound to a key cast to a specified type,
 * returns a specified default value if the key is not bound, or throws `TestCanceledException` if the key exists but is
 * either not bound or is bound to an incompatible type. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val tempFileName: String = configMap.getWithDefault[String]("tempFileName", "tmp.txt")
 * }}}
 *
 * @param underlying an immutable `Map` that holds the key/value pairs contained in this `ConfigMap`
 * 
 * @author Bill Venners
 */
class ConfigMap(underlying: Map[String, Any]) extends Map[String, Any] with MapLike[String, Any, ConfigMap] with java.io.Serializable {

  def get(key: String): Option[Any] = underlying.get(key)

  def iterator: Iterator[(String, Any)] = underlying.iterator

  def +[A >: Any](kv: (String, A)): ConfigMap = new ConfigMap(underlying + kv)

  def -(key: String): ConfigMap = new ConfigMap(underlying - key)

  override def empty: ConfigMap = new ConfigMap(Map.empty[String, Any])

  /**
   * Returns the value bound to a key cast to a specified type, wrapped in a `Some`,
   * returns `None` if the key is not bound, or throws `TestCanceledException` if the key exists but is
   * bound to an incompatible type. Here's an example:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val tempFileName: Option[String] = configMap.getOptional[String]("tempFileName")
   * }}}
   *
   * @param key the key with which the desired value should be associated
   * @param classTag an implicit `ClassTag` specifying the expected type for the desired value
   */
  def getOptional[V](key: String)(implicit classTag: ClassTag[V]): Option[V] = {
    if (underlying.contains(key)) Some(getRequired[V](key))
    else None
  }

  /**
   * Returns the value bound to a key cast to the specified type `V`,
   * returns a specified default value if the key is not bound, or throws `TestCanceledException` if the key exists but is
   * if either the key is not bound or is bound to an incompatible type. Here's an example:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val tempFileName: String = configMap.getWithDefault[String]("tempFileName", "tmp.txt")
   * }}}
   *
   * @param key the key with which the desired value should be associated
   * @param default a default value to return if the key is not found
   * @param classTag an implicit `ClassTag` specifying the expected type for the desired value
   */
  def getWithDefault[V](key: String, default: => V)(implicit classTag: ClassTag[V]): V = {
    if (underlying.contains(key)) getRequired[V](key)
    else default
  }

  /**
   * Returns the value bound to a key cast to the specified type `V`, or throws `TestCanceledException`
   * if either the key is not bound or is bound to an incompatible type. Here's an example:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * val tempFileName: String = configMap.getRequired[String]("tempFileName")
   * }}}
   *
   * @param key the key with which the desired value should be associated
   * @param classTag an implicit `ClassTag` specifying the expected type for the desired value
   */
  def getRequired[V](key: String)(implicit classTag: ClassTag[V], pos: source.Position): V = {
    underlying.get(key) match {
      case Some(value) =>
        val expectedClass = classTag.runtimeClass
        val boxedExpectedClass =
          expectedClass match {
            case java.lang.Boolean.TYPE => classOf[java.lang.Boolean]
            case java.lang.Byte.TYPE => classOf[java.lang.Byte]
            case java.lang.Short.TYPE => classOf[java.lang.Short]
            case java.lang.Integer.TYPE => classOf[java.lang.Integer]
            case java.lang.Long.TYPE => classOf[java.lang.Long]
            case java.lang.Character.TYPE => classOf[java.lang.Character]
            case java.lang.Float.TYPE => classOf[java.lang.Float]
            case java.lang.Double.TYPE => classOf[java.lang.Double]
            case _ => expectedClass
          }
        val actualClass = value.asInstanceOf[AnyRef].getClass
        if (boxedExpectedClass.isAssignableFrom(actualClass))
          value.asInstanceOf[V]
        else
            throw new TestCanceledException((sde: StackDepthException) => Some(Resources.configMapEntryHadUnexpectedType(key, actualClass, expectedClass, value.asInstanceOf[AnyRef])), None, pos, None)
      case None => throw new TestCanceledException((sde: StackDepthException) => Some(Resources.configMapEntryNotFound(key)), None, pos, None)
    }
  }
}

/**
 * Companion object to class `ConfigMap` containing factory methods.
 *
 * @author Bill Venners
 */
object ConfigMap {

  /**
   * Constructs a `ConfigMap` containing the passed key/value pairs.
   *
   * @param pairs zero to many key/value pairs with which to initialize a new `ConfigMap`.
   */
  def apply(pairs: (String, Any)*): ConfigMap = new ConfigMap(Map(pairs: _*))

  /**
   * Constructs an empty `ConfigMap`.
   */
  def empty: ConfigMap = new ConfigMap(Map.empty)
}

