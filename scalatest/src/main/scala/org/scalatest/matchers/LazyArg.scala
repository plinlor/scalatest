
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
package org.scalatest.matchers

/**
 * Transforms a given object's `toString` with a given function lazily.
 *
 * This class is intended to be used with the `mapResult` method of
 * <a href="MatcherProducers.html">`MatcherProducers`</a>, which you can use to 
 * modify error messages when composing matchers. This class exists to enable those error messages
 * to be modified ''lazily'', so that `toString` is invoked on the given
 * `arg`, and its result transformed by the given function `f`, ''only when and if''
 * the `toString` method is invoked on the `LazyArg`. As a performance optimization, ScalaTest's
 * <a href="MatchResult.html">`MatchResult`</a> avoids invoking `toString` on objects
 * until and unless an error message is actually needed, to minimize unecessary creation and concatenation
 * of strings. The `LazyArg` class enables this same performance optimization when composing
 * matchers.
 * 
 *
 * The other design goal of `LazyArg` is to make the internal `arg` available for inspection
 * in an IDE. In a future version of ScalaTest, the `args` of `MatchResult` that were used
 * to create the error message will be included in the `TestFailedException`, so they can be inspected
 * in IDEs. This is why the `arg` field of `LazyArg` is public.
 * 
 *
 * For an example of using `LazyArg`, see the <a href="Matcher.html#composingMatchers">Composing matchers</a>
 * section in the main documentation for trait `Matcher`.
 * 
 *
 * @param arg the argument
 * @param f a function that given the `arg` will produce a `String`
 */
final case class LazyArg(val arg: Any)(f: Any => String) {
 
  /**
   * Returns the result of invoking the function `f`, passed to the `LazyArg`
   * constructor, on field `arg`.
   */
  override def toString = f(arg.toString)
}

