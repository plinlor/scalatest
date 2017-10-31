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

import org.scalactic.Requirements._

/**
 * Trait that causes the nested suites of any suite it is mixed into to be run sequentially even if
 * a `Distributor` is passed to `runNestedSuites`. This trait overrides the 
 * `runNestedSuites` method and fowards every parameter passed to it to a superclass invocation
 * of `runNestedSuites`, except it always passes `None` for the `Distributor`.
 * Mix in this trait into any suite whose nested suites need to be run sequentially even with the rest of the
 * run is being executed concurrently.
 */
trait SequentialNestedSuiteExecution extends SuiteMixin { this: Suite =>

  /**
   * This trait's implementation of `runNestedSuites`s invokes `runNestedSuites` on `super`,
   * passing in `None` for the `Distributor`.
   *
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all nested suites started by this method have completed, and whether or not a failure occurred.
   *
   * @throws NullArgumentException if any passed parameter is `null`.
   */
  abstract override protected def runNestedSuites(args: Args): Status = {
    requireNonNull(args)

    super.runNestedSuites(args.copy(distributor = None))
  }
}
