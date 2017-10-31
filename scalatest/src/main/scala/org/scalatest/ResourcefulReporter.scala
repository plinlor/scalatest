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
 * Subtrait of `Reporter` that contains a `dispose` method for
 * releasing any finite, non-memory resources, such as file handles, held by the
 * `Reporter`. <a href="tools/Runner$.html">`Runner`</a> will invoke `dispose` on
 * any `ResourcefulReporter` when it no longer needs the `Reporter`.
 */
trait ResourcefulReporter extends Reporter {

  /**
   * Release any finite, non-memory resources, such as file handles, held by this
   * `Reporter`. Clients should call this method when they no longer need
   * the `Reporter`, before releasing the last reference to the `Reporter`.
   * After this method is invoked, the `Reporter` may be defunct, and therefore not
   * usable anymore. If the `Reporter` holds no resources, it may do nothing when
   * this method is invoked, however, in that case, it probably won't implement `ResourcefulReporter`.
   */
  def dispose()
}
