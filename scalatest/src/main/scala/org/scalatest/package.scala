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
package org

/**
 * ScalaTest's main traits, classes, and other members, including members supporting ScalaTest's DSL for the Scala interpreter.
 */
package object scalatest {

   // SKIP-SCALATESTJS-START
  private val defaultShell = ShellImpl()

  /**
   * Returns a copy of this `Shell` with `colorPassed` configuration parameter set to `true`.
   */
  lazy val color: Shell = defaultShell.color

  /**
   * Returns a copy of this `Shell` with `durationsPassed` configuration parameter set to `true`.
   */
  lazy val durations: Shell = defaultShell.durations

  /**
   * Returns a copy of this `Shell` with `shortStacksPassed` configuration parameter set to `true`.
   */
  lazy val shortstacks: Shell = defaultShell.shortstacks

  /**
   * Returns a copy of this `Shell` with `fullStacksPassed` configuration parameter set to `true`.
   */
  lazy val fullstacks: Shell = defaultShell.fullstacks

  /**
   * Returns a copy of this `Shell` with `statsPassed` configuration parameter set to `true`.
   */
  lazy val stats: Shell = defaultShell.stats

  /**
   * Returns a copy of this `Shell` with `colorPassed` configuration parameter set to `false`.
   */
  lazy val nocolor: Shell = defaultShell.nocolor

  /**
   * Returns a copy of this `Shell` with `durationsPassed` configuration parameter set to `false`.
   */
  lazy val nodurations: Shell = defaultShell.nodurations

  /**
   * Returns a copy of this `Shell` with `shortStacksPassed` configuration parameter set to `false`.
   */
  lazy val nostacks: Shell = defaultShell.nostacks

  /**
   * Returns a copy of this `Shell` with `statsPassed` configuration parameter set to `false`.
   */
  lazy val nostats: Shell = defaultShell.nostats
  // SKIP-SCALATESTJS-END

  /**
   * The version number of ScalaTest.
   *
   * @return the ScalaTest version number.
   */
  val ScalaTestVersion: String = ScalaTestVersions.ScalaTestVersion

  @deprecated("Please use PendingStatement instead")
  type PendingNothing = PendingStatement

  private[scalatest] type Expectation = Fact

  /* 
   * Marker trait that serves as the result type of <code>assert</code>, <code>assume</code>, and <code>pending</code> methods of
   * trait <code>Assertions</code>, which return its only instance, the <code>Succeeded</code> singleton, or throw
   * an exception that indicates a failed, canceled, or pending test.
   */
  type Assertion = compatible.Assertion

  // SKIP-SCALATESTJS-START
  /**
   * '''The name `org.scalatest.SpecLike` has been deprecated and will be removed in a future version of ScalaTest. Please use
   * its new name, `org.scalatest.refspec.RefSpecLike`, instead.'''
   *
   * Because this style uses reflection at runtime to discover scopes and tests, it can only be supported on the JVM, not Scala.js.
   * Thus in ScalaTest 3.0.0, class `org.scalatest.SpecLike` was moved to the `org.scalatest.refspec` package and renamed
   * `RefSpecLike`, with the intention of later moving it to a separate module available only on the JVM.
   * 
   */
  @deprecated("Please use org.scalatest.refspec.RefSpecLike instead")
  type SpecLike = refspec.RefSpecLike

  /**
   * '''The name `org.scalatest.Spec` has been deprecated and will be removed in a future version of ScalaTest. Please use
   * its new name, `org.scalatest.refspec.RefSpec`, instead.'''
   *
   * Because this style uses reflection at runtime to discover scopes and tests, it can only be supported on the JVM, not Scala.js.
   * Thus in ScalaTest 3.0.0, class `org.scalatest.Spec` was moved to the `org.scalatest.refspec` package and renamed
   * `RefSpec`, with the intention of later moving it to a separate module available only on the JVM.
   * 
   */
  @deprecated("Please use org.scalatest.refspec.RefSpec instead")
  type Spec = refspec.RefSpec
  // SKIP-SCALATESTJS-END
}
