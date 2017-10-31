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
package org.scalatest.time

import org.scalatest.Resources

/**
 * Defines a family of singleton objects representing units of time.
 *
 * The singleton objects that extend this abstract class may be passed
 * to the constructor of <a href="Span.html">`Span`</a> to specify
 * units of time. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(1, Second)
 * }}}
 */
sealed abstract class Units extends Product with Serializable {
  private[scalatest] def singularMessageFun(lengthString: String): String
  private[scalatest] def pluralMessageFun(lengthString: String): String
}

/**
 * Indicates units for a single nanosecond.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify nanosecond units of time, so long as the value passed to `Span` is 1. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(1, Nanosecond)
 * }}}
 */
case object Nanosecond extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularNanosecondUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralNanosecondUnits(lengthString)
}

/**
 * Indicates nanosecond units.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify nanosecond units of time. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(10, Nanoseconds)
 * }}}
 */
case object Nanoseconds extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularNanosecondUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralNanosecondUnits(lengthString)
}

/**
 * Indicates units for a single microsecond.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify microsecond units of time, so long as the value passed to `Span` is 1. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(1, Microsecond)
 * }}}
 */
case object Microsecond extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularMicrosecondUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralMicrosecondUnits(lengthString)
}

/**
 * Indicates microsecond units.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify microsecond units of time. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(10, Microseconds)
 * }}}
 */
case object Microseconds extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularMicrosecondUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralMicrosecondUnits(lengthString)
}

/**
 * Indicates units for a single millisecond.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify millisecond units of time, so long as the value passed to `Span` is 1. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(1, Millisecond)
 * }}}
 */
case object Millisecond extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularMillisecondUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralMillisecondUnits(lengthString)
}

/**
 * Indicates millisecond units.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify millisecond units of time. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(10, Milliseconds)
 * }}}
 */
case object Milliseconds extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularMillisecondUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralMillisecondUnits(lengthString)
}
/**
 * Indicates millisecond units (shorthand form).
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify millisecond units of time. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(10, Millis)
 * }}}
 *
 * ''Note: `Millis` is merely a shorthand for <a href="Milliseconds$.html">`Milliseconds`</a>.
 * When passed to `Span`, `Millis` means exactly the same thing as
 * `Milliseconds`.''
 * 
 */
case object Millis extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularMillisecondUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralMillisecondUnits(lengthString)
}

/**
 * Indicates units for a single second.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify second units of time, so long as the value passed to `Span` is 1. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(1, Second)
 * }}}
 */
case object Second extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularSecondUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralSecondUnits(lengthString)
}

/**
 * Indicates second units.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify second units of time. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(10, Seconds)
 * }}}
 */
case object Seconds extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularSecondUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralSecondUnits(lengthString)
}

/**
 * Indicates units for a single minute.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify minute units of time, so long as the value passed to `Span` is 1. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(1, Minute)
 * }}}
 */
case object Minute extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularMinuteUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralMinuteUnits(lengthString)
}

/**
 * Indicates minute units.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify minute units of time. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(10, Minutes)
 * }}}
 */
case object Minutes extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularMinuteUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralMinuteUnits(lengthString)
}

/**
 * Indicates units for a single hour.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify hour units of time, so long as the value passed to `Span` is 1. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(1, Hour)
 * }}}
 */
case object Hour extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularHourUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralHourUnits(lengthString)
}

/**
 * Indicates hour units.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify hour units of time. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(10, Hours)
 * }}}
 */
case object Hours extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularHourUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralHourUnits(lengthString)
}

/**
 * Indicates units for a single day.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify day units of time, so long as the value passed to `Span` is 1. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(1, Day)
 * }}}
 */
case object Day extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularDayUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralDayUnits(lengthString)
}

/**
 * Indicates day units.
 *
 * This singleton object may be passed to the constructor of <a href="Span.html">`Span`</a> to
 * specify day units of time. For example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * Span(10, Days)
 * }}}
 */
case object Days extends Units {
  private[scalatest] def singularMessageFun(lengthString: String): String = Resources.singularDayUnits(lengthString)
  private[scalatest] def pluralMessageFun(lengthString: String): String = Resources.pluralDayUnits(lengthString)
}

