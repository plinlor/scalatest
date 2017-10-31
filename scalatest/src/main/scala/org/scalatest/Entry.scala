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

/**
 * A case class implementation of `java.util.Map.Entry` to make it easier to
 * test Java `Map`s with ScalaTest <a href="Matchers.html">Matchers</a>.
 *
 * In Java, `java.util.Map` is not a subtype of `java.util.Collection`, and does not
 * actually define an element type. You can ask a Java `Map` for an &ldquo;entry set&rdquo;
 * via the `entrySet` method, which will return the `Map`'s key/value pairs
 * wrapped in a set of `java.util.Map.Entry`, but a `Map` is not actually
 * a collection of `Entry`. To make Java `Map`s easier to work with, however,
 * ScalaTest matchers allows you to treat a Java `Map` as a collection of `Entry`,
 * and defines this convenience implementation of `java.util.Map.Entry`.
 * Here's how you use it:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * javaMap should contain (Entry(2, 3))
 * javaMap should contain oneOf (Entry(2, 3), Entry(3, 4))
 * }}}
 *
 * @param key the key of this entry
 * @param value the value of this entry
 */
// SKIP-SCALATESTJS-START
case class Entry[K, V](key: K, value: V) extends java.util.Map.Entry[K, V] {
// SKIP-SCALATESTJS-END
//SCALATESTJS-ONLY case class Entry[K, V](key: K, value: V) {

  /**
   * Returns the key corresponding to this `Entry`.
   *
   * @return the key corresponding to this entry
   */
  def getKey: K = key

  /**
   * Returns the value corresponding to this entry.
   *
   * @return the value corresponding to this entry 
   */
  def getValue: V = value

  /**
   * Throws `UnsupportedOperationException`.
   *
   * @throws UnsupportedOperationException unconditionally
   */
  def setValue(v: V): V = throw new UnsupportedOperationException

  /**
   * Compares the specified object with this entry for equality.
   *
   * @param other the object to be compared for equality with this map entry 
   * @return true if the specified object is equal to this map entry
   */
  override def equals(other: Any): Boolean = {
    other match {
      case that: java.util.Map.Entry[_, _] => 
         (if (key == null) that.getKey == null else key == that.getKey) &&
         (if (value == null) that.getValue == null else value == that.getValue)
      case _ => false
    }
  }

  /**
   * Returns the hash code value for this map entry.
   *
   * @return the hash code value for this map entry
   */
  override def hashCode: Int = {
    (if (key == null) 0 else key.hashCode) ^
    (if (value == null) 0 else value.hashCode)
  }

  /**
   * Returns a `String` representation of this `Entry` consisting of
   * concatenating the result of invoking `toString` on the `key`,
   * an equals sign, and the result of invoking `toString` on the `value`.
   *
   * @return a `String` already!
   */
  override def toString: String = key.toString + "=" + value.toString
}

