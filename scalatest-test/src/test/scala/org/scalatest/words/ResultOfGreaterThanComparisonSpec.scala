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
package org.scalatest.words

import org.scalatest._
import Matchers._

class ResultOfGreaterThanComparisonSpec extends FunSpec {
  
  describe("ResultOfGreaterThanComparison ") {
    
    it("should have pretty toString") {
      val result = new ResultOfGreaterThanComparison(8)
      result.toString should be ("> (8)")
    }
  }
  
}
