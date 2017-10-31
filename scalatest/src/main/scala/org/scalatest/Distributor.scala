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
 * Trait whose instances facilitate parallel execution of `Suite`s.
 * An optional `Distributor` is passed to the `run` method of <a href="Suite.html">`Suite`</a>. If a
 * `Distributor` is indeed passed, trait `Suite`'s implementation of `run` will
 * populate that `Distributor` with its nested `Suite`s (by passing them to the `Distributor`'s
 * `apply` method) rather than executing the nested `Suite`s directly. It is then up to another thread or process
 * to execute those `Suite`s.
 *
 * If you have a set of nested `Suite`s that must be executed sequentially, you can mix in trait
 * <a href="SequentialNestedSuiteExecution.html">`SequentialNestedSuiteExecution`</a>, which overrides `runNestedSuites` and
 * calls `super`'s `runNestedSuites` implementation, passing in `None` for the
 * `Distributor`.
 * 
 * 
 * Implementations of this trait must be thread safe.
 * 
 *
 * @author Bill Venners
 */
trait Distributor {
  
  /**
   * Puts a `Suite` into the `Distributor`.
   *
   * The `Distributor` can decide which, if any, of the passed `Args</code
   * to pass to the `Suite`'s apply method. For example, a `Distributor`
   * may pass itself wrapped in a `Some` in the `Args` it passes to the `Suite`'s `run`
   * method instead of the `args.distributor` value.
   * 
   *
   * @param suite the `Suite` to put into the `Distributor`.
   * @param args a `Args` containing objects that may be passed to the `Suite`'s
   *             `run` method via a `Args` instance.
   *
   * @throws NullArgumentException if either `suite` or `tracker` is `null`.
   */
  def apply(suite: Suite, args: Args): Status
}

