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
package org.scalatest.selenium

import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.safari.SafariDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import java.util.concurrent.TimeUnit
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.Clock
import org.openqa.selenium.support.ui.Sleeper
import org.openqa.selenium.support.ui.ExpectedCondition
import scala.collection.mutable.Buffer
import scala.collection.JavaConverters._
import org.openqa.selenium.Cookie
import java.util.Date
import org.scalatest.time.Span
import org.scalatest.time.Milliseconds
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.OutputType
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import org.openqa.selenium.Alert
import org.openqa.selenium.support.ui.Select
import org.scalatest.exceptions.TestFailedException
import org.scalatest.exceptions.StackDepthException
import org.openqa.selenium.JavascriptExecutor
import org.scalatest.ScreenshotCapturer
import org.scalatest.time.Nanosecond
import org.scalatest.Resources
import org.scalatest.exceptions.StackDepthExceptionHelper.getStackDepth
import org.scalactic.source

/**
 * Trait that provides a domain specific language (DSL) for writing browser-based tests using <a href="http://seleniumhq.org">Selenium</a>.  
 *
 * To use ScalaTest's Selenium DSL, mix trait `WebBrowser` into your test class. This trait provides the DSL in its
 * entirety except for one missing piece: an implicit `org.openqa.selenium.WebDriver`. One way to provide the missing
 * implicit driver is to declare one as a member of your test class, like this:
 * 
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest._
 * import selenium._
 * import org.openqa.selenium._
 * import htmlunit._
 *
 * class BlogSpec extends FlatSpec with Matchers with WebBrowser {
 *
 *   implicit val webDriver: WebDriver = new HtmlUnitDriver
 * 
 *   val host = "http://localhost:9000/"
 *
 *   "The blog app home page" should "have the correct title" in {
 *     go to (host + "index.html")
 *     pageTitle should be ("Awesome Blog")
 *   }
 * }
 * }}}
 * 
 * For convenience, however, ScalaTest provides a `WebBrowser` subtrait containing an implicit `WebDriver` for each
 * driver provided by Selenium. 
 * Thus a simpler way to use the `HtmlUnit` driver, for example, is to extend
 * ScalaTest's <a href="HtmlUnit.html">`HtmlUnit`</a> trait, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest._
 * import selenium._
 *
 * class BlogSpec extends FlatSpec with Matchers with HtmlUnit {
 *
 *   val host = "http://localhost:9000/"
 *
 *   "The blog app home page" should "have the correct title" in {
 *     go to (host + "index.html")
 *     pageTitle should be ("Awesome Blog")
 *   }
 * }
 * }}}
 * 
 * The web driver traits provided by ScalaTest are:
 * 
 * 
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Driver'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''`WebBrowser` subtrait'''</th></tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * Google Chrome
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="Chrome.html">`Chrome`</a>
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * Mozilla Firefox
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="Firefox.html">`Firefox`</a>
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * HtmlUnit
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="HtmlUnit.html">`HtmlUnit`</a>
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * Microsoft Internet Explorer
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="InternetExplorer.html">`InternetExplorer`</a>
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * Apple Safari
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 * <a href="Safari.html">`Safari`</a>
 * </td>
 * </tr>
 * </table>
 *
 * ==Navigation==
 *
 * You can ask the browser to retrieve a page (go to a URL) like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * go to "http://www.artima.com"
 * }}}
 * 
 * Note: If you are using the ''page object pattern'', you can also go to a page using the `Page` instance, as
 * illustrated in the section on <a href="#pageObjects">page objects</a> below.
 * 
 *
 * Once you have retrieved a page, you can fill in and submit forms, query for the values of page elements, and make assertions.  
 * In the following example, selenium will go to `http://www.google.com`, fill in the text box with
 * `Cheese!`, press the submit button, and wait for result returned from an AJAX call:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * go to "http://www.google.com"
 * click on "q"
 * enter("Cheese!")
 * submit()
 * // Google's search is rendered dynamically with JavaScript.
 * eventually { pageTitle should be ("Cheese! - Google Search") }
 * }}}
 * 
 * In the above example, the `"q"` used in &ldquo;`click on "q"`&rdquo; 
 * can be either the id or name of an element. ScalaTest's Selenium DSL will try to lookup by id first. If it cannot find 
 * any element with an id equal to `&quot;q&quot;`, it will then try lookup by name `&quot;q&quot;`.
 * 
 * 
 * Alternatively, you can be more specific:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * click on id("q")   // to lookup by id "q" 
 * click on name("q") // to lookup by name "q" 
 * }}}
 * 
 * In addition to `id` and `name`, you can use the following approaches to lookup elements, just as you can do with
 * Selenium's `org.openqa.selenium.By` class:
 * 
 * 
 * <ul>
 *   <li>`xpath`</li>
 *   <li>`className`</li>
 *   <li>`cssSelector`</li>
 *   <li>`linkText`</li>
 *   <li>`partialLinkText`</li>
 *   <li>`tagName`</li>
 * </ul>
 * 
 * For example, you can select by link text with:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * click on linkText("click here!")
 * }}}
 * 
 * If an element is not found via any form of lookup, evaluation will complete abruptly with a `TestFailedException`.
 *
 * ==Getting and setting input element values==
 * 
 * ScalaTest's Selenium DSL provides a clear, simple syntax for accessing and updating the values of input elements such as
 * text fields, radio buttons, checkboxes, selection lists, and the input types introduced in HTML5. If a requested element is not found, or if it is found but is
 * not of the requested type, an exception will immediately result causing the test to fail.
 *
 * The most common way to access field value is through the `value` property, which is supported by the following
 * input types:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr>
 *   <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 *     '''Tag Name'''
 *   </th>
 *   <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 *     '''Input Type'''
 *   </th>
 *   <th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">
 *     '''Lookup Method'''
 *   </th>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `text`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `textField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `textarea`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `-`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `textArea`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `password`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `pwdField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `email`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `emailField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `color`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `colorField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `date`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `dateField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `datetime`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `dateTimeField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `datetime-local`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `dateTimeLocalField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `month`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `monthField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `number`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `numberField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `range`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `rangeField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `search`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `searchField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `tel`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `telField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `time`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `timeField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `url`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `urlField`
 *   </td>
 * </tr>
 * <tr>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `input`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `week`
 *   </td>
 *   <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">
 *     `weekField`
 *   </td>
 * </tr>
 * </table>
 *
 * You can change a input field's value by assigning it via the `=` operator, like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * textField("q").value = "Cheese!"
 * }}}
 * 
 * And you can access a input field's value by simply invoking `value` on it:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * textField("q").value should be ("Cheese!")
 * }}}
 * 
 * If the text field is empty, `value` will return an empty string (`""`).
 * 
 * 
 * You can use the same syntax with other type of input fields by replacing `textField` with `Lookup Method` listed in table above,
 * for example to use text area:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * textArea("body").value = "I saw something cool today!"
 * textArea("body").value should be ("I saw something cool today!")
 * }}}
 * 
 * or with a password field:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * pwdField("secret").value = "Don't tell anybody!"
 * pwdField("secret").value should be ("Don't tell anybody!")
 * }}}
 *
 * ===Alternate Way for Data Entry===
 * 
 * An alternate way to enter data into a input fields is to use `enter` or `pressKeys`.
 * Although both of `enter` and `pressKeys` send characters to the active element, `pressKeys` can be used on any kind of
 * element, whereas `enter` can only be used on text entry fields, which include:
 * 
 *
 * <ul>
 *   <li>`textField`</li>
 *   <li>`textArea`</li>
 *   <li>`pwdField`</li>
 *   <li>`emailField`</li>
 *   <li>`searchField`</li>
 *   <li>`telField`</li>
 *   <li>`urlField`</li>
 * </ul>
 *
 * Another difference is that `enter` will clear the text field or area before sending the characters,
 * effectively replacing any currently existing text with the new text passed to `enter`. By contrast,
 * `pressKeys` does not do any clearing&#8212;it just appends more characters to any existing text.
 * You can backup with `pressKeys`, however, by sending explicit backspace characters, `"&#92;u0008"`.
 * 
 * 
 * To use these commands, you must first click on the input field you are interested in
 * to give it the focus. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * click on "q"
 * enter("Cheese!")
 * }}}
 * 
 * Here's a (contrived) example of using `pressKeys` with backspace to fix a typo:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * click on "q"              // q is the name or id of a text field or text area
 * enter("Cheesey!")         // Oops, meant to say Cheese!
 * pressKeys("&#92;u0008&#92;u0008") // Send two backspaces; now the value is Cheese
 * pressKeys("!")            // Send the missing exclamation point; now the value is Cheese!
 * }}}
 * 
 * ===Radio buttons===
 * 
 * Radio buttons work together in groups. For example, you could have a group of radio buttons, like this:
 * 
 * 
 * {{{
 * &lt;input type="radio" id="opt1" name="group1" value="Option 1"&gt; Option 1&lt;/input&gt;
 * &lt;input type="radio" id="opt2" name="group1" value="Option 2"&gt; Option 2&lt;/input&gt;
 * &lt;input type="radio" id="opt3" name="group1" value="Option 3"&gt; Option 3&lt;/input&gt;
 * }}}
 * 
 * You can select an option in either of two ways:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * radioButtonGroup("group1").value = "Option 2"
 * radioButtonGroup("group1").selection = Some("Option 2")
 * }}}
 *
 * Likewise, you can read the currently selected value of a group of radio buttons in two ways:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * radioButtonGroup("group1").value should be ("Option 2")
 * radioButtonGroup("group1").selection should be (Some("Option 2"))
 * }}}
 * 
 * If the radio button has no selection at all, `selection` will return `None` whereas `value`
 * will throw a `TestFailedException`. By using `value`, you are indicating you expect a selection, and if there
 * isn't a selection that should result in a failed test.
 * 
 * 
 * If you would like to work with `RadioButton` element directly, you can select it by calling `radioButton`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * click on radioButton("opt1")
 * }}}
 * 
 * you can check if an option is selected by calling `isSelected`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * radioButton("opt1").isSelected should be (true)
 * }}}
 * 
 * to get the value of radio button, you can call `value`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * radioButton("opt1").value should be ("Option 1")
 * }}}
 * 
 * ===Checkboxes===
 * 
 * A checkbox in one of two states: selected or cleared. Here's how you select a checkbox:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * checkbox("cbx1").select()
 * }}}
 * 
 * And here's how you'd clear one:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * checkbox("cbx1").clear()
 * }}}
 * 
 * You can access the current state of a checkbox with `isSelected`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * checkbox("cbx1").isSelected should be (true)
 * }}}
 * 
 * ===Single-selection dropdown lists===
 * 
 * Given the following single-selection dropdown list:
 * 
 * 
 * {{{
 * &lt;select id="select1"&gt;
 *  &lt;option value="option1"&gt;Option 1&lt;/option&gt;
 *  &lt;option value="option2"&gt;Option 2&lt;/option&gt;
 *  &lt;option value="option3"&gt;Option 3&lt;/option&gt;
 * &lt;/select&gt;
 * }}}
 * 
 * You could select `Option 2` in either of two ways:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * singleSel("select1").value = "option2"
 * singleSel("select1").selection = Some("option2")
 * }}}
 * 
 * To clear the selection, either invoke `clear` or set `selection` to `None`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * singleSel("select1").clear()
 * singleSel("select1").selection = None
 * }}}
 * 
 * You can read the currently selected value of a single-selection list in the same manner as radio buttons:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * singleSel("select1").value should be ("option2")
 * singleSel("select1").selection should be (Some("option2"))
 * }}}
 * 
 * If the single-selection list has no selection at all, `selection` will return `None` whereas `value`
 * will throw a `TestFailedException`. By using `value`, you are indicating you expect a selection, and if there
 * isn't a selection that should result in a failed test.
 * 
 * 
 * ===Multiple-selection lists===
 * 
 * Given the following multiple-selection list:
 * 
 * 
 * {{{
 * &lt;select name="select2" multiple="multiple"&gt;
 *  &lt;option value="option4"&gt;Option 4&lt;/option&gt;
 *  &lt;option value="option5"&gt;Option 5&lt;/option&gt;
 *  &lt;option value="option6"&gt;Option 6&lt;/option&gt;
 * &lt;/select&gt;
 * }}}
 * 
 * You could select `Option 5` and `Option 6` like this:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * multiSel("select2").values = Seq("option5", "option6")
 * }}}
 * 
 * The previous command would essentially clear all selections first, then select `Option 5` and `Option 6`.
 * If instead you want to ''not'' clear any existing selection, just additionally select `Option 5` and `Option 6`,
 * you can use the `+=` operator, like this.
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * multiSel("select2").values += "option5"
 * multiSel("select2").values += "option6"
 * }}}
 * 
 * To clear a specific option, pass its name to `clear`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * multiSel("select2").clear("option5")
 * }}}
 * 
 * To clear all selections, call `clearAll`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * multiSel("select2").clearAll()
 * }}}
 * 
 * You can access the current selections with `values`, which returns an immutable `IndexedSeq[String]`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * multiSel("select2").values should have size 2
 * multiSel("select2").values(0) should be ("option5")
 * multiSel("select2").values(1) should be ("option6")
 * }}}
 * 
 * ===Clicking and submitting===
 * 
 * You can click on any element with &ldquo;`click on`&rdquo; as shown previously:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * click on "aButton"
 * click on name("aTextField")
 * }}}
 * 
 * If the requested element is not found, `click on` will throw an exception, failing the test.
 * 
 * 
 * Clicking on a input element will give it the focus. If current focus is in on an input element within a form, you can submit the form by 
 * calling `submit`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * submit()
 * }}}
 * 
 * ==Switching==
 * 
 * You can switch to a popup alert bo using the following code:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * switch to alertBox
 * }}}
 * 
 * to switch to a frame, you could:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * switch to frame(0) // switch by index
 * switch to frame("name") // switch by name
 * }}}
 * 
 * If you have reference to a window handle (can be obtained from calling windowHandle/windowHandles), you can switch to a particular 
 * window by:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * switch to window(windowHandle)
 * }}}
 * 
 * You can also switch to active element and default content:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * switch to activeElement
 * switch to defaultContent
 * }}}
 * 
 * ==Navigation history==
 * 
 * In real web browser, you can press the 'Back' button to go back to previous page.  To emulate that action in your test, you can call `goBack`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * goBack()
 * }}}
 * 
 * To emulate the 'Forward' button, you can call:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * goForward()
 * }}}
 * 
 * And to refresh or reload the current page, you can call:
 * 
 * {{{  <!-- class="stHighlight" -->
 * reloadPage()
 * }}}
 * 
 * ==Cookies!==
 * 
 * <p>To create a new cookie, you'll say:
 * 
 * {{{  <!-- class="stHighlight" -->
 * add cookie ("cookie_name", "cookie_value")
 * }}}
 * 
 * to read a cookie value, you do:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * cookie("cookie_name").value should be ("cookie_value") // If value is undefined, throws TFE right then and there. Never returns null.
 * }}}
 * 
 * In addition to the common use of name-value cookie, you can pass these extra fields when creating the cookie, available ways are:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * cookie(name: String, value: String)
 * cookie(name: String, value: String, path: String)
 * cookie(name: String, value: String, path: String, expiry: Date)
 * cookie(name: String, value: String, path: String, expiry: Date, domain: String)
 * cookie(name: String, value: String, path: String, expiry: Date, domain: String, secure: Boolean)
 * }}}
 * 
 * and to read those extra fields:
 * 
 * {{{  <!-- class="stHighlight" -->
 * cookie("cookie_name").value   // Read cookie's value
 * cookie("cookie_name").path    // Read cookie's path
 * cookie("cookie_name").expiry  // Read cookie's expiry
 * cookie("cookie_name").domain  // Read cookie's domain
 * cookie("cookie_name").isSecure  // Read cookie's isSecure flag
 * }}}
 * 
 * In order to delete a cookie, you could use the following code: 
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * delete cookie "cookie_name"
 * }}}
 * 
 * or to delete all cookies in the same domain:-
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * delete all cookies
 * }}}
 * 
 * To get the underlying Selenium cookie, you can use `underlying`:
 * 
 * {{{  <!-- class="stHighlight" -->
 * cookie("cookie_name").underlying.validate()  // call the validate() method on underlying Selenium cookie
 * }}}
 * 
 * ==Other useful element properties==
 * 
 * All element types (`textField`, `textArea`, `radioButton`, `checkbox`, `singleSel`, `multiSel`) 
 * support the following useful properties:
 * 
 * 
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Method'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''Description'''</th></tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `location`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * The XY location of the top-left corner of this `Element`.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `size`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * The width/height size of this `Element`.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `isDisplayed`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * Indicates whether this `Element` is displayed.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `isEnabled`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * Indicates whether this `Element` is enabled.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `isSelected`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * Indicates whether this `Element` is selected.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `tagName`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * The tag name of this element.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `underlying`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * The underlying `WebElement` wrapped by this `Element`.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `attribute(name: String)`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * The attribute value of the given attribute name of this element, wrapped in a `Some`, or `None` if no
 * such attribute exists on this `Element`.
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `text`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * Returns the visible (''i.e.'', not hidden by CSS) text of this element, including sub-elements, without any leading or trailing whitespace.
 * </td>
 * </tr>
 * </table>
 * 
 * ==Implicit wait==
 * 
 * To set Selenium's implicit wait timeout, you can call the `implicitlyWait` method:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * implicitlyWait(Span(10, Seconds))
 * }}}
 * 
 * Invoking this method sets the amount of time the driver will wait when searching for an element that is not immediately present. For
 * more information, see the documentation for method `implicitlyWait`.
 * 
 *
 * ==Page source and current URL==
 * 
 * It is possible to get the html source of currently loaded page, using:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * pageSource
 * }}}
 * 
 * and if needed, get the current URL of currently loaded page:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * currentUrl
 * }}}
 * 
 * ==Screen capture==
 * 
 * You can capture screen using the following code:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * val file = capture
 * }}}
 * 
 * By default, the captured image file will be saved in temporary folder (returned by java.io.tmpdir property), with random file name 
 * ends with .png extension.  You can specify a fixed file name:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * capture to "MyScreenShot.png"
 * }}}
 * 
 * or
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * capture to "MyScreenShot"
 * }}}
 * 
 * Both will result in a same file name `MyScreenShot.png`.
 * 
 * 
 * You can also change the target folder screenshot file is written to, by saying:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * setCaptureDir("/home/your_name/screenshots")
 * }}}
 * 
 * If you want to capture a screenshot when something goes wrong (e.g. test failed), you can use `withScreenshot`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * withScreenshot {
 *   assert("Gold" == "Silver", "Expected gold, but got silver")
 * }
 * }}}
 * 
 * In case the test code fails, you'll see the screenshot location appended to the error message, for example:
 * 
 * 
 * {{{
 * Expected gold but got silver; screenshot capture in /tmp/AbCdEfGhIj.png
 * }}}
 * 
 * <a name="pageObjects"></a>
 * ==Using the page object pattern==
 *
 * If you use the page object pattern, mixing trait `Page` into your page classes will allow you to use the `go to` 
 * syntax with your page objects. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * class HomePage extends Page {
 *   val url = "http://localhost:9000/index.html"
 * }
 *
 * val homePage = new HomePage
 * go to homePage
 * }}}
 *
 * ==Executing JavaScript==
 *
 * To execute arbitrary JavaScript, for example, to test some JavaScript functions on your page, pass it to `executeScript`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * go to (host + "index.html")
 * val result1 = executeScript("return document.title;")
 * result1 should be ("Test Title")
 * val result2 = executeScript("return 'Hello ' + arguments[0]", "ScalaTest")
 * result2 should be ("Hello ScalaTest")
 * }}}
 *
 * To execute an asynchronous bit of JavaScript, pass it to `executeAsyncScript`. You can set the script timeout with `setScriptTimeout`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val script = """
 *   var callback = arguments[arguments.length - 1];
 *   window.setTimeout(function() {callback('Hello ScalaTest')}, 500);
 * """
 * setScriptTimeout(1 second)
 * val result = executeAsyncScript(script)
 * result should be ("Hello ScalaTest")
 * }}}
 *
 * ==Querying for elements==
 *
 * You can query for arbitrary elements via `find` and `findAll`. The `find` method returns the first matching element, wrapped in a `Some`,
 * or `None` if no element is found. The `findAll` method returns an immutable `IndexedSeq` of all matching elements. If no elements match the query, `findAll`
 * returns an empty `IndexedSeq`. These methods allow you to perform rich queries using `for` expressions. Here are some examples:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * val ele: Option[Element] = find("q")
 *
 * val eles: colection.immutable.IndexedSeq[Element] = findAll(className("small"))
 * for (e <- eles; if e.tagName != "input")
 *   e should be ('displayed)
 * val textFields = eles filter { tf.isInstanceOf[TextField] }
 * }}}
 *
 * ==Cleaning up==
 * 
 * To close the current browser window, and exit the driver if the current window was the only one remaining, use `close`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * close()
 * }}}
 * 
 * To close all windows, and exit the driver, use `quit`:
 * 
 * 
 * {{{  <!-- class="stHighlight" -->
 * quit()
 * }}}
 * 
 * <a name="alternateForms"></a>
 * ==Alternate forms==
 * 
 * Although statements like &ldquo;`delete all cookies`&rdquo; fit well with matcher statements
 * like &ldquo;`title should be ("Cheese!")`&rdquo;, they do not fit as well
 * with the simple method call form of assertions. If you prefer, you can avoid operator notation
 * and instead use alternatives that take the form of plain-old method calls. Here's an example:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * goTo("http://www.google.com")
 * clickOn("q")
 * textField("q").value = "Cheese!"
 * submit()
 * // Google's search is rendered dynamically with JavaScript.
 * eventually(assert(pageTitle === "Cheese! - Google Search"))
 * }}}
 * 
 * Here's a table showing the complete list of alternatives:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''operator notation'''</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">'''method call'''</th></tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `go to (host + "index.html")`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `goTo(host + "index.html")`
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `click on "aButton"`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `clickOn("aButton")`
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `switch to activeElement`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `switchTo(activeElement)`
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `add cookie ("cookie_name", "cookie_value")`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `addCookie("cookie_name", "cookie_value")`
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `delete cookie "cookie_name"`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `deleteCookie("cookie_name")`
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `delete all cookies`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `deleteAllCookies()`
 * </td>
 * </tr>
 * <tr>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `capture to "MyScreenShot"`
 * </td>
 * <td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: left">
 * `captureTo("MyScreenShot")`
 * </td>
 * </tr>
 * </table>
 * 
 * @author Chua Chee Seng
 * @author Bill Venners
 */
trait WebBrowser { 

  /**
   * A point containing an XY screen location.
   */
  case class Point(x: Int, y: Int)

  /**
   * A dimension containing the width and height of a screen element.
   */
  case class Dimension(width: Int, height: Int)

  /**
   * Wrapper class for a Selenium `WebElement`.
   *
   * This class provides idiomatic Scala access to the services of an underlying `WebElement`.
   * You can access the wrapped `WebElement` via the `underlying` method.
   * 
   */
  sealed trait Element {

    /**
     * The XY location of the top-left corner of this `Element`.
     *
     * This invokes `getLocation` on the underlying `WebElement`.
     * 
     *
     * @return the location of the top-left corner of this element on the page
     */
    def location: Point = Point(underlying.getLocation.getX, underlying.getLocation.getY)

    /**
     * The width/height size of this `Element`.
     *
     * This invokes `getSize` on the underlying `WebElement`.
     * 
     *
     * @return the size of the element on the page
     */
    def size: Dimension = Dimension(underlying.getSize.getWidth, underlying.getSize.getHeight)

    /**
     * Indicates whether this `Element` is displayed.
     *
     * This invokes `isDisplayed` on the underlying `WebElement`.
     * 
     *
     * @return `true` if the element is currently displayed
     */
    def isDisplayed: Boolean = underlying.isDisplayed

    /**
     * Indicates whether this `Element` is enabled.
     *
     * This invokes `isEnabled` on the underlying `WebElement`, which
     * will generally return `true` for everything but disabled input elements.
     * 
     *
     * @return `true` if the element is currently enabled
     */
    def isEnabled: Boolean = underlying.isEnabled

    /**
     * Indicates whether this `Element` is selected.
     *
     * This method, which invokes `isSelected` on the underlying `WebElement`,
     * is relevant only for input elements such as checkboxes, options in a single- or multiple-selection
     * list box, and radio buttons. For any other element it will simply return `false`.
     * 
     *
     * @return `true` if the element is currently selected or checked
     */
    def isSelected: Boolean = underlying.isSelected

    /**
     * The tag name of this element.
     *
     * This method invokes `getTagName` on the underlying `WebElement`.
     * Note it returns the name of the tag, not the value of the of the `name` attribute.
     * For example, it will return will return `"input"` for the element
     * `&lt;input name="city" /&gt;`, not `"city"`.
     * 
     *
     * @return the tag name of this element
     */
    def tagName: String = underlying.getTagName

    /**
     * The underlying `WebElement` wrapped by this `Element`
     */
    val underlying: WebElement
    
    /**
     * The attribute value of the given attribute name of this element, wrapped in a `Some`, or `None` if no
     * such attribute exists on this `Element`.
     *
     * This method invokes `getAttribute` on the underlying `WebElement`, passing in the
     * specified `name`.
     * 
     *
     * @return the attribute with the given name, wrapped in a `Some`, else `None`
     */
    def attribute(name: String): Option[String] = Option(underlying.getAttribute(name))

    /**
     * Returns the visible (''i.e.'', not hidden by CSS) text of this element, including sub-elements, without any leading or trailing whitespace.
     *
     * @return the visible text enclosed by this element, or an empty string, if the element encloses no visible text
     */
    def text: String = {
      val txt = underlying.getText
      if (txt != null) txt else "" // Just in case, I'm not sure if Selenium would ever return null here
    }

    /**
     * Returns the result of invoking `equals` on the underlying `Element`, passing
     * in the specified `other` object.
     *
     * @param other the object with which to compare for equality
     *
     * @return true if the passed object is equal to this one
     */
    override def equals(other: Any): Boolean = underlying == other

    /**
     * Returns the result of invoking `hashCode` on the underlying `Element`.
     *
     * @return a hash code for this object
     */
    override def hashCode: Int = underlying.hashCode

    /**
     * Returns the result of invoking `toString` on the underlying `Element`.
     *
     * @return a string representation of this object
     */
    override def toString: String = underlying.toString 
  }

  // fluentLinium has a doubleClick. Wonder how they are doing that?

  /**
   * Wrapper class for a Selenium `Cookie`.
   *
   * This class provides idiomatic Scala access to the services of an underlying `Cookie`.
   * You can access the wrapped `Cookie` via the `underlying` method.
   * 
   */
  final class WrappedCookie(val underlying: Cookie) {

    /**
     * The domain to which this cookie is visible.
     *
     * This invokes `getDomain` on the underlying `Cookie`.
     * 
     *
     * @return the domain of this cookie
     */
    def domain: String = underlying.getDomain 

    /**
     * The expire date of this cookie.
     *
     * This invokes `getExpiry` on the underlying `Cookie`.
     * 
     *
     * @return the expire date of this cookie
     */
    def expiry: Option[Date] = Option(underlying.getExpiry)

    /**
     * The name of this cookie.
     *
     * This invokes `getName` on the underlying `Cookie`.
     * 
     *
     * @return the name of this cookie
     */
    def name: String = underlying.getName

    /**
     * The path of this cookie.
     *
     * This invokes `getPath` on the underlying `Cookie`.
     * 
     *
     * @return the path of this cookie
     */
    def path: String = underlying.getPath 

    /**
     * The value of this cookie.
     *
     * This invokes `getValue` on the underlying `Cookie`.
     * 
     *
     * @return the value of this cookie
     */
    def value: String = underlying.getValue

    /**
     * Indicates whether the cookie requires a secure connection.
     *
     * This invokes `isSecure` on the underlying `Cookie`.
     * 
     *
     * @return true if this cookie requires a secure connection.
     */
    def secure: Boolean = underlying.isSecure

    /**
     * Returns the result of invoking `equals` on the underlying `Cookie`, passing
     * in the specified `other` object.
     *
     * Two Selenium `Cookie`s are considered equal if their name and values are equal.
     * 
     *
     * @param other the object with which to compare for equality
     *
     * @return true if the passed object is equal to this one
     */
    override def equals(other: Any): Boolean = underlying == other

    /**
     * Returns the result of invoking `hashCode` on the underlying `Cookie`.
     *
     * @return a hash code for this object
     */
    override def hashCode: Int = underlying.hashCode

    /**
     * Returns the result of invoking `toString` on the underlying `Cookie`.
     *
     * @return a string representation of this object
     */
    override def toString: String = underlying.toString 
  }

  /**
   * This class is part of the ScalaTest's Selenium DSL. Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a>
   * for an overview of the Selenium DSL.
   */
  class CookiesNoun

  /**
   * This field supports cookie deletion in ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This field enables the following syntax:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * delete all cookies
   *            ^
   * }}}
   */
  val cookies = new CookiesNoun
  
  /**
   * This sealed abstract class supports switching in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * One subclass of `SwitchTarget` exists for each kind of target that
   * can be switched to: active element, alert box, default content, frame (indentified by index,
   * name or id, or enclosed element), and window.
   * 
   */
  sealed abstract class SwitchTarget[T] {

    /**
     * Abstract method implemented by subclasses that represent "targets" to which the user can switch.
     *
     * @param driver the `WebDriver` with which to perform the switch
     */
    def switch(driver: WebDriver)(implicit position: source.Position): T
  }

  /**
   * This class supports switching to the currently active element in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to activeElement
   *           ^
   * }}}
   */
  final class ActiveElementTarget extends SwitchTarget[Element] {

    /**
     * Switches the driver to the currently active element.
     *
     * @param driver the `WebDriver` with which to perform the switch
     */
    def switch(driver: WebDriver)(implicit position: source.Position): Element = {
      createTypedElement(driver.switchTo.activeElement, position)
    }
  }

  /**
   * This class supports switching to the alert box in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to alertBox
   *           ^
   * }}}
   */
  final class AlertTarget extends SwitchTarget[Alert] {

    /**
     * Switches the driver to the currently active alert box.
     *
     * @param driver the `WebDriver` with which to perform the switch
     */
    def switch(driver: WebDriver)(implicit position: source.Position): Alert = {
      driver.switchTo.alert
    }
  }

  /**
   * This class supports switching to the default content in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to defaultContent
   *           ^
   * }}}
   */
  final class DefaultContentTarget extends SwitchTarget[WebDriver] {

    /**
     * Switches the driver to the default content
     *
     * @param driver the `WebDriver` with which to perform the switch
     */
    def switch(driver: WebDriver)(implicit position: source.Position): WebDriver = {
      driver.switchTo.defaultContent
    }
  }
 
  /**
   * This class supports switching to a frame by index in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to frame(0)
   *           ^
   * }}}
   */
  final class FrameIndexTarget(index: Int) extends SwitchTarget[WebDriver] {

    /**
     * Switches the driver to the frame at the index that was passed to the constructor.
     *
     * @param driver the `WebDriver` with which to perform the switch
     */
    def switch(driver: WebDriver)(implicit pos: source.Position): WebDriver =
      try {
        driver.switchTo.frame(index)
      }
      catch {
        case e: org.openqa.selenium.NoSuchFrameException => 
          throw new TestFailedException(
                     (_: StackDepthException) => Some("Frame at index '" + index + "' not found."),
                     None,
                     pos
                   )
      }
  }

  /**
   * This class supports switching to a frame by name or ID in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to frame("name")
   *           ^
   * }}}
   */
  final class FrameNameOrIdTarget(nameOrId: String) extends SwitchTarget[WebDriver] {

    /**
     * Switches the driver to the frame with the name or ID that was passed to the constructor.
     *
     * @param driver the `WebDriver` with which to perform the switch
     */
    def switch(driver: WebDriver)(implicit pos: source.Position): WebDriver =
      try {
        driver.switchTo.frame(nameOrId)
      }
      catch {
        case e: org.openqa.selenium.NoSuchFrameException => 
          throw new TestFailedException(
                     (_: StackDepthException) => Some("Frame with name or ID '" + nameOrId + "' not found."),
                     None,
                     pos
                   )
      }
  }

  /**
   * This class supports switching to a frame by web element in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   */
  final class FrameWebElementTarget(webElement: WebElement) extends SwitchTarget[WebDriver] {

    /**
     * Switches the driver to the frame containing the `WebElement` that was passed to the constructor.
     *
     * @param driver the `WebDriver` with which to perform the switch
     */
    def switch(driver: WebDriver)(implicit pos: source.Position): WebDriver =
      try {
        driver.switchTo.frame(webElement)
      }
      catch {
        case e: org.openqa.selenium.NoSuchFrameException => 
          throw new TestFailedException(
                     (_: StackDepthException) => Some("Frame element '" + webElement + "' not found."),
                     None,
                     pos
                   )
      }
  }
  
  /**
   * This class supports switching to a frame by element in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   */
  final class FrameElementTarget(element: Element) extends SwitchTarget[WebDriver] {

    /**
     * Switches the driver to the frame containing the `Element` that was passed to the constructor.
     *
     * @param driver the `WebDriver` with which to perform the switch
     */
    def switch(driver: WebDriver)(implicit pos: source.Position): WebDriver =
      try {
        driver.switchTo.frame(element.underlying)
      }
      catch {
        case e: org.openqa.selenium.NoSuchFrameException => 
          throw new TestFailedException(
                     (_: StackDepthException) => Some("Frame element '" + element + "' not found."),
                     None,
                     pos
                   )
      }
  }

  /**
   * This class supports switching to a window by name or handle in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to window(windowHandle)
   *           ^
   * }}}
   */
  final class WindowTarget(nameOrHandle: String) extends SwitchTarget[WebDriver] {

    /**
     * Switches the driver to the window with the name or ID that was passed to the constructor.
     *
     * @param driver the `WebDriver` with which to perform the switch
     */
    def switch(driver: WebDriver)(implicit pos: source.Position): WebDriver =
      try {
        driver.switchTo.window(nameOrHandle)
      }
      catch {
        case e: org.openqa.selenium.NoSuchWindowException => 
          throw new TestFailedException(
                     (_: StackDepthException) => Some("Window with nameOrHandle '" + nameOrHandle + "' not found."),
                     None,
                     pos
                   )
      }
  }
  
  private def isInputField(webElement: WebElement, name: String): Boolean = {
    val elementTypeRaw = webElement.getAttribute("type")
    val elementType =
      if (elementTypeRaw == null)
        "text"
      else
        elementTypeRaw
    webElement.getTagName.toLowerCase == "input" && elementType.toLowerCase == name
  }
      
  private def isTextField(webElement: WebElement): Boolean = isInputField(webElement, "text")
  private def isPasswordField(webElement: WebElement): Boolean = isInputField(webElement, "password")
  private def isCheckBox(webElement: WebElement): Boolean = isInputField(webElement, "checkbox")
  private def isRadioButton(webElement: WebElement): Boolean = isInputField(webElement, "radio")
  private def isEmailField(webElement: WebElement): Boolean = isInputField(webElement, "email") || isInputField(webElement, "text")
  private def isColorField(webElement: WebElement): Boolean = isInputField(webElement, "color") || isInputField(webElement, "text")
  private def isDateField(webElement: WebElement): Boolean = isInputField(webElement, "date") || isInputField(webElement, "text")
  private def isDateTimeField(webElement: WebElement): Boolean = isInputField(webElement, "datetime") || isInputField(webElement, "text")
  private def isDateTimeLocalField(webElement: WebElement): Boolean = isInputField(webElement, "datetime-local") || isInputField(webElement, "text")
  private def isMonthField(webElement: WebElement): Boolean = isInputField(webElement, "month") || isInputField(webElement, "text")
  private def isNumberField(webElement: WebElement): Boolean = isInputField(webElement, "number") || isInputField(webElement, "text")
  private def isRangeField(webElement: WebElement): Boolean = isInputField(webElement, "range") || isInputField(webElement, "text")
  private def isSearchField(webElement: WebElement): Boolean = isInputField(webElement, "search") || isInputField(webElement, "text")
  private def isTelField(webElement: WebElement): Boolean = isInputField(webElement, "tel") || isInputField(webElement, "text")
  private def isTimeField(webElement: WebElement): Boolean = isInputField(webElement, "time") || isInputField(webElement, "text")
  private def isUrlField(webElement: WebElement): Boolean = isInputField(webElement, "url") || isInputField(webElement, "text")
  private def isWeekField(webElement: WebElement): Boolean = isInputField(webElement, "week") || isInputField(webElement, "text")

  private def isTextArea(webElement: WebElement): Boolean = 
    webElement.getTagName.toLowerCase == "textarea"
  
  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * textField("q").value should be ("Cheese!")
   * }}}
   *
   * @param underlying the `WebElement` representing a text field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a text field
   */
  final class TextField(val underlying: WebElement)(implicit pos: source.Position) extends Element {
    
    if(!isTextField(underlying))
      throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not text field."),
                     None,
                     pos
                   )
    
    /**
     * Gets this text field's value.
     *
     * This method invokes `getAttribute("value")` on the underlying `WebElement`.
     * 
     *
     * @return the text field's value
     */
    def value: String = underlying.getAttribute("value")  
    
    /**
     * Sets this text field's value.
     *
     * @param value the new value
     */
    def value_=(value: String): Unit = {
      underlying.clear()
      underlying.sendKeys(value)
    }

    /**
     * Clears this text field.
     */
    def clear(): Unit = { underlying.clear() }
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * textArea("q").value should be ("Cheese!")
   * }}}
   *
   * @param underlying the `WebElement` representing a text area
   * @throws TestFailedExeption if the passed `WebElement` does not represent a text area
   */
  final class TextArea(val underlying: WebElement)(implicit pos: source.Position) extends Element {
    if(!isTextArea(underlying))
      throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not text area."),
                     None,
                     pos
                   )
    
    /**
     * Gets this text area's value.
     *
     * This method invokes `getAttribute("value")` on the underlying `WebElement`.
     * 
     *
     * @return the text area's value
     */
    def value: String = underlying.getAttribute("value")

    /**
     * Sets this text area's value.
     *
     * @param value the new value
     */
    def value_=(value: String): Unit = {
      underlying.clear()
      underlying.sendKeys(value)
    }

    /**
     * Clears this text area.
     */
    def clear(): Unit = { underlying.clear() }
  }
  
  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * pwdField("q").value should be ("Cheese!")
   * }}}
   *
   * @param underlying the `WebElement` representing a password field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a password field
   */
  final class PasswordField(val underlying: WebElement)(implicit pos: source.Position) extends Element {
    
    if(!isPasswordField(underlying))
      throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not password field."),
                     None,
                     pos
                   )
    
    /**
     * Gets this password field's value.
     *
     * This method invokes `getAttribute("value")` on the underlying `WebElement`.
     * 
     *
     * @return the password field's value
     */
    def value: String = underlying.getAttribute("value")  
    
    /**
     * Sets this password field's value.
     *
     * @param value the new value
     */
    def value_=(value: String): Unit = {
      underlying.clear()
      underlying.sendKeys(value)
    }

    /**
     * Clears this text field.
     */
    def clear(): Unit = { underlying.clear() }
  }
  
  trait ValueElement extends Element {
    val underlying: WebElement

    def checkCorrectType(isA: (WebElement) => Boolean, typeDescription: String)(implicit pos: source.Position): Unit = {
      if(!isA(underlying))
        throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not " + typeDescription + " field."),
                     None,
                     pos
                   )
    }

    /**
     * Gets this field's value.
     *
     * This method invokes `getAttribute("value")` on the underlying `WebElement`.
     * 
     *
     * @return the field's value
     */
    def value: String = underlying.getAttribute("value")  
    
    /**
     * Sets this field's value.
     *
     * @param value the new value
     */
    def value_=(value: String)(implicit driver: WebDriver): Unit = {
      underlying.clear()

      driver match {
        case executor: JavascriptExecutor => executor.executeScript("arguments[0].setAttribute(arguments[1], arguments[2]);", underlying, "value", value)
        case _ => underlying.sendKeys(value)
      }
      //underlying.setAttribute("value", value)
      //underlying.sendKeys(value)
    }

    /**
     * Clears this field.
     */
    def clear(): Unit = { underlying.clear() }
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * emailField("q").value should be ("foo@bar.com")
   * }}}
   *
   * @param underlying the `WebElement` representing a email field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a email field
   */
  final class EmailField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isEmailField, "email")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * colorField("q").value should be ("Cheese!")
   * }}}
   *
   * @param underlying the `WebElement` representing a color field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a color field
   */
  final class ColorField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isColorField, "color")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * dateField("q").value should be ("2003-03-01")
   * }}}
   *
   * @param underlying the `WebElement` representing a date field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a date field
   */
  final class DateField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isDateField, "date")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * dateTimeField("q").value should be ("2003-03-01T12:13:14")
   * }}}
   *
   * @param underlying the `WebElement` representing a datetime field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a datetime field
   */
  final class DateTimeField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isDateTimeField, "datetime")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * dateTimeLocalField("q").value should be ("2003-03-01T12:13:14")
   * }}}
   *
   * @param underlying the `WebElement` representing a datetime-local field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a datetime-local field
   */
  final class DateTimeLocalField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isDateTimeLocalField, "datetime-local")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * monthField("q").value should be ("2003-04")
   * }}}
   *
   * @param underlying the `WebElement` representing a month field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a month field
   */
  final class MonthField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isMonthField, "month")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * numberField("q").value should be ("1.3")
   * }}}
   *
   * @param underlying the `WebElement` representing a number field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a number field
   */
  final class NumberField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isNumberField, "number")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * rangeField("q").value should be ("1.3")
   * }}}
   *
   * @param underlying the `WebElement` representing a range field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a range field
   */
  final class RangeField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isRangeField, "range")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * searchField("q").value should be ("google")
   * }}}
   *
   * @param underlying the `WebElement` representing a search field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a search field
   */
  final class SearchField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isSearchField, "search")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * telField("q").value should be ("911-911-9191")
   * }}}
   *
   * @param underlying the `WebElement` representing a tel field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a tel field
   */
  final class TelField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isTelField, "tel")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * timeField("q").value should be ("12:13:14")
   * }}}
   *
   * @param underlying the `WebElement` representing a time field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a time field
   */
  final class TimeField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isTimeField, "time")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * urlField("q").value should be ("http://google.com")
   * }}}
   *
   * @param underlying the `WebElement` representing a url field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a url field
   */
  final class UrlField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isUrlField, "url")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * weekField("q").value should be ("1996-W16")
   * }}}
   *
   * @param underlying the `WebElement` representing a week field
   * @throws TestFailedExeption if the passed `WebElement` does not represent a week field
   */
  final class WeekField(val underlying: WebElement)(implicit pos: source.Position) extends Element with ValueElement {
    checkCorrectType(isWeekField, "week")(pos)
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * radioButton(id("opt1")).value should be ("Option 1!")
   * }}}
   *
   * @param underlying the `WebElement` representing a text area
   * @throws TestFailedExeption if the passed `WebElement` does not represent a text area
   */
  final class RadioButton(val underlying: WebElement)(implicit pos: source.Position) extends Element {
    if(!isRadioButton(underlying))
      throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not radio button."),
                     None,
                     pos
                   )
    /**
     * Gets this radio button's value.
     *
     * Invokes `getAttribute("value")` on the underlying `WebElement`.
     * 
     *
     * @return the radio button's value
     */
    def value: String = underlying.getAttribute("value")
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * radioButtonGroup("group1").value should be ("Option 2")
   * }}}
   *
   * @throws TestFailedExeption if no radio button with the passed `groupName` are found
   */
  final class RadioButtonGroup(groupName: String, driver: WebDriver)(implicit pos: source.Position) {

    private def groupElements = driver.findElements(By.name(groupName)).asScala.toList.filter(isRadioButton(_))

    if (groupElements.length == 0)
      throw new TestFailedException(
                     (_: StackDepthException) => Some("No radio buttons with group name '" + groupName + "' was found."),
                     None,
                     pos
                   )

    /**
     * Returns the value of this group's selected radio button, or throws `TestFailedException` if no
     * radio button in this group is selected.
     *
     * @return the value of this group's selected radio button
     * @throws TestFailedExeption if no radio button in this group is selected
     */
    def value(implicit pos: source.Position): String = selection match {
      case Some(v) => v
      case None => 
        throw new TestFailedException(
                     (_: StackDepthException) => Some("The radio button group on which value was invoked contained no selected radio button."),
                     None,
                     pos
                   )
    }

    /**
     * Returns the value of this group's selected radio button, wrapped in a `Some`, or `None`, if no
     * radio button in this group is selected.
     *
     * @return the value of this group's selected radio button, wrapped in a `Some`, else `None`
     */
    def selection: Option[String] = {
      groupElements.find(_.isSelected) match {
        case Some(radio) => 
          Some(radio.getAttribute("value"))
        case None =>
          None
      }
    }

    /**
     * Selects the radio button with the passed value.
     *
     * @param the value of the radio button to select
     * @throws TestFailedExeption if the passed string is not the value of any radio button in this group
     */
    def value_=(value: String)(implicit pos: source.Position): Unit = {
      groupElements.find(_.getAttribute("value") == value) match {
        case Some(radio) => 
          radio.click()
        case None => 
          throw new TestFailedException(
                     (_: StackDepthException) => Some("Radio button value '" + value + "' not found for group '" + groupName + "'."),
                     None,
                     pos
                   )
      }
    }
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * checkbox("cbx1").select()
   * }}}
   *
   * @param underlying the `WebElement` representing a checkbox
   * @throws TestFailedExeption if the passed `WebElement` does not represent a checkbox
   */
  final class Checkbox(val underlying: WebElement)(implicit pos: source.Position) extends Element {
    if(!isCheckBox(underlying))
      throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not check box."),
                     None,
                     pos
                   )
    
    /**
     * Selects this checkbox.
     */
    def select(): Unit = {
      if (!underlying.isSelected)
        underlying.click()
    }

    /**
     * Clears this checkbox
     */
    def clear(): Unit = {
      if (underlying.isSelected())
        underlying.click()
    }

    /**
     * Gets this checkbox's value.
     *
     * This method invokes `getAttribute("value")` on the underlying `WebElement`.
     * 
     *
     * @return the checkbox's value
     */
    def value: String = underlying.getAttribute("value")
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * multiSel("select2").values += "option5"
   *                            ^
   * }}}
   *
   * Instances of this class are returned from the `values` method of `MultiSel`.
   * `MultiSelOptionSeq` is an immutable `IndexedSeq[String]` that wraps an underlying immutable `IndexedSeq[String]` and adds two
   * methods, `+` and `-`, to facilitate the `+=` syntax for setting additional options
   * of the `MultiSel`. The Scala compiler will rewrite:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * multiSel("select2").values += "option5"
   * }}}
   *
   * To:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * multiSel("select2").values = multiSel("select2").values + "option5"
   * }}}
   *
   * Thus, first a new `MultiSelOptionSeq` is created by invoking the `+` method on the `MultiSelOptionSeq`
   * returned by `values`, and that result is passed to the `values_=` method.
   * 
   *
   * For symmetry, this class also offers a `-` method, which can be used to deselect an option, like this:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * multiSel("select2").values -= "option5"
   *                            ^
   * }}}
   *
   */
  class MultiSelOptionSeq(underlying: collection.immutable.IndexedSeq[String]) extends collection.immutable.IndexedSeq[String] {

    /**
     * Selects an element by its index in the sequence.
     *
     * This method invokes `apply` on the underlying immutable `IndexedSeq[String]`, passing in `idx`, and returns the result.
     * 
     *
     * @param idx the index to select
     * @return the element of this sequence at index `idx`, where 0 indicates the first element
     */
    def apply(idx: Int): String = underlying.apply(idx)

    /**
     * The length of this sequence.
     *
     * This method invokes `length` on the underlying immutable `IndexedSeq[String]` and returns the result.
     * 
     *
     * @return the number of elements in this sequence
     */
    def length: Int = underlying.length

    /**
     * Appends a string element to this sequence, if it doesn't already exist in the sequence.
     *
     * If the string element already exists in this sequence, this method returns itself. If not,
     * this method returns a new `MultiSelOptionSeq` with the passed value appended to the
     * end of the original `MultiSelOptionSeq`.
     * 
     *
     * @param the string element to append to this sequence
     * @return a `MultiSelOptionSeq` that contains the passed string value
     */
    def +(value: String): MultiSelOptionSeq = {
      if (!underlying.contains(value))
        new MultiSelOptionSeq(underlying :+ value)
      else
        this
    }

    /**
     * Removes a string element to this sequence, if it already exists in the sequence.
     *
     * If the string element does not already exist in this sequence, this method returns itself. If the element
     * is contained in this sequence, this method returns a new `MultiSelOptionSeq` with the passed value
     * removed from the the original `MultiSelOptionSeq`, leaving any other elements in the same order.
     * 
     *
     * @param the string element to append to this sequence
     * @return a `MultiSelOptionSeq` that contains the passed string value
     */
    def -(value: String): MultiSelOptionSeq = {
      if (underlying.contains(value))
        new MultiSelOptionSeq(underlying.filter(_ != value))
      else
        this
    }
  }

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * singleSel.clear()
   * }}}
   *
   * @param underlying a `WebElement` representing a single selection list
   * @throws TestFailedExeption if the passed `WebElement` does not represent a single selection list
   */
  class SingleSel(val underlying: WebElement)(implicit pos: source.Position) extends Element {
    if(underlying.getTagName.toLowerCase != "select")
      throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not select."),
                     None,
                     pos
                   )
    private val select = new Select(underlying)
    if (select.isMultiple)
      throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not a single-selection list."),
                     None,
                     pos
                   )
    
    /**
     * Returns the value of this single selection list, wrapped in a `Some`, or `None`, if this single
     * selection list has no currently selected value.
     *
     * @return the value of this single selection list, wrapped in a `Some`, else `None`
     */
    def selection = {
      val first = select.getFirstSelectedOption
      if (first == null)
        None
      else
        Some(first.getAttribute("value"))
    }
    
    /**
     * Gets this single selection list's selected value, or throws `TestFailedException` if no value is currently selected.
     *
     * @return the single selection list's value
     * @throws TestFailedException if the single selection list has no selected value
     */
    def value(implicit pos: source.Position): String = selection match {
      case Some(v) => v
      case None => 
        throw new TestFailedException(
                     (_: StackDepthException) => Some("The single selection list on which value was invoked had no selection."),
                     None,
                     pos
                   )
    }
    
    /**
     * Sets this single selection list's value to the passed value.
     *
     * @param value the new value
     * @throws TestFailedException if the passed value does not match not one of the single selection list's values
     */
    def value_=(value : String)(implicit pos: source.Position): Unit = {
      try {
        select.selectByValue(value)
      }
      catch {
        case e: org.openqa.selenium.NoSuchElementException => 
          throw new TestFailedException(
                     (_: StackDepthException) => Some(e.getMessage),
                     Some(e),
                     pos
                   )
      }
    }
  }

  

  /**
   * This class is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * multiSel("select2").clear("option5")
   * }}}
   *
   * @param underlying a `WebElement` representing a multiple selection list
   * @throws TestFailedExeption if the passed `WebElement` does not represent a multiple selection list
   */
  class MultiSel(val underlying: WebElement)(implicit pos: source.Position) extends Element {
    if(underlying.getTagName.toLowerCase != "select")
      throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not select."),
                     None,
                     pos
                   )
    private val select = new Select(underlying)
    if (!select.isMultiple)
      throw new TestFailedException(
                     (_: StackDepthException) => Some("Element " + underlying + " is not a multi-selection list."),
                     None,
                     pos
                   )

    /**
     * Clears the passed value in this multiple selection list.
     *
     * @param value the value to clear
     */
    def clear(value: String): Unit = {
      select.deselectByValue(value)
    }
  
    /**
     * Gets all selected values of this multiple selection list.
     *
     * If the multiple selection list has no selections, ths method will
     * return an empty `IndexedSeq`.
     * 
     *
     * @return An `IndexedSeq` containing the currently selected values
     */
    def values: MultiSelOptionSeq = {
      val elementSeq = Vector.empty ++ select.getAllSelectedOptions.asScala
      new MultiSelOptionSeq(elementSeq.map(_.getAttribute("value")))
    }

    /**
     * Clears any existing selections then sets all values contained in the passed `collection.Seq[String]`.
     *
     * In other words, the `values_=` method ''replaces'' the current selections, if any, with
     * new selections defined by the passed `Seq[String]`.
     * 
     *
     * @param values a `Seq` of string values to select
     * @throws TestFailedException if a value contained in the passed `Seq[String]` is not
     *         among this multiple selection list's values.
     */
    def values_=(values: collection.Seq[String])(implicit pos: source.Position): Unit = {
      try {
        clearAll()
        values.foreach(select.selectByValue(_))
      }
      catch {
        case e: org.openqa.selenium.NoSuchElementException => 
          throw new TestFailedException(
                     (_: StackDepthException) => Some(e.getMessage),
                     Some(e),
                     pos
                   )
      }
    }
    
    /**
     * Clears all selected values in this multiple selection list.
     *
     * @param value the value to clear
     */
    def clearAll(): Unit = {
      select.deselectAll()
    }
  }
  
  /**
   * This object is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This object enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * go to "http://www.artima.com"
   * ^
   * }}}
   */
  object go {

    /**
     * Sends the browser to the passed URL.
     *
     * This method enables syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * go to "http://www.artima.com"
     *    ^
     * }}}
     *
     * @param url the URL to which to send the browser
     * @param driver the `WebDriver` with which to drive the browser
     */
    def to(url: String)(implicit driver: WebDriver): Unit = {
      driver.get(url)
    }

    /**
     * Sends the browser to the URL contained in the passed `Page` object.
     *
     * This method enables syntax such as the following:
     * 
     *
     * {{{  <!-- class="stHighlight" -->
     * go to homePage
     *    ^
     * }}}
     *
     * @param page the `Page` object containing the URL to which to send the browser
     * @param driver the `WebDriver` with which to drive the browser
     */
    def to(page: Page)(implicit driver: WebDriver): Unit = {
      driver.get(page.url)
    }
  }
  
  /**
   * Sends the browser to the passed URL.
   *
   * Here's an example:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * goTo("http://www.artima.com")
   * }}}
   *
   * @param url the URL to which to send the browser
   * @param driver the `WebDriver` with which to drive the browser
   */
  def goTo(url: String)(implicit driver: WebDriver): Unit = {
    go to url
  }
  
  /**
   * Sends the browser to the URL contained in the passed `Page` object.
   *
   * Here's an example:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * goTo(homePage)
   * }}}
   *
   * @param page the `Page` object containing the URL to which to send the browser
   * @param driver the `WebDriver` with which to drive the browser
   */
  def goTo(page: Page)(implicit driver: WebDriver): Unit = {
    go to page
  }
  
  /**
   * Closes the current browser window, and exits the driver if the current window was the only one remaining.
   *
   * @param driver the `WebDriver` with which to drive the browser
   */
  def close()(implicit driver: WebDriver): Unit = {
    driver.close()
  }
  
  /**
   * Returns the title of the current page, or the empty string if the current page has no title.
   *
   * @param driver the `WebDriver` with which to drive the browser
   * @return the current page's title, or the empty string if the current page has no title
   */
  def pageTitle(implicit driver: WebDriver): String = {
    val t = driver.getTitle
    if (t != null) t else ""
  }
  
  /**
   * Returns the source of the current page.
   *
   * This method invokes `getPageSource` on the passed `WebDriver` and returns the result.
   * 
   *
   * @param driver the `WebDriver` with which to drive the browser
   * @return the source of the current page
   */
  def pageSource(implicit driver: WebDriver): String = driver.getPageSource
  
  /**
   * Returns the URL of the current page.
   *
   * This method invokes `getCurrentUrl` on the passed `WebDriver` and returns the result.
   * 
   *
   * @param driver the `WebDriver` with which to drive the browser
   * @return the URL of the current page
   */
  def currentUrl(implicit driver: WebDriver): String = driver.getCurrentUrl
  
  /**
   * This trait is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * Subclasses of this trait define different ways of querying for elements, enabling
   * syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on id("q")
   *          ^
   * }}}
   */
  sealed trait Query extends Product with Serializable {

    /**
     * The Selenium `By` for this query.
     */
    val by: By

    /**
     * The query string for this query.
     *
     * For example, the query string for `id("q")` is `"q"`.
     * 
     */
    val queryString: String

    /**
     * Returns the first `Element` selected by this query, or throws `TestFailedException`
     * if no `Element` is selected.
     *
     * The class of the `Element` returned will be a subtype of `Element` if appropriate.
     * For example, if this query selects a text field, the class of the returned `Element` will
     * be `TextField`.
     * 
     *
     * @param driver the `WebDriver` with which to drive the browser
     * @return the `Element` selected by this query
     * @throws TestFailedException if nothing is selected by this query
     */
    def element(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Element = {
      try {
        createTypedElement(driver.findElement(by), pos)
      }
      catch {
        case e: org.openqa.selenium.NoSuchElementException =>
          // the following is avoid the suite instance to be bound/dragged into the messageFun, which can cause serialization problem.
          val queryStringValue = queryString
          throw new TestFailedException(
                     (_: StackDepthException) => Some("Element '" + queryStringValue + "' not found."),
                     Some(e),
                     pos
                   )
      }
    }
    
    /**
     * Returns the first `Element` selected by this query, wrapped in a `Some`, or `None`
     * if no `Element` is selected.
     *
     * The class of the `Element` returned will be a subtype of `Element` if appropriate.
     * For example, if this query selects a text field, the class of the returned `Element` will
     * be `TextField`.
     * 
     *
     * @param driver the `WebDriver` with which to drive the browser
     * @return the `Element` selected by this query, wrapped in a `Some`, or `None` if
     *   no `Element` is selected
     */
    def findElement(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Option[Element] =
      try {
        Some(createTypedElement(driver.findElement(by), pos))
      }
      catch {
        case e: org.openqa.selenium.NoSuchElementException => None
      }

    /**
     * Returns an `Iterator` over all `Element`s selected by this query.
     *
     * The class of the `Element`s produced by the returned `Iterator` will be a
     * subtypes of `Element` if appropriate.  For example, if an `Element`representing
     * a text field is returned by the `Iterator`, the class of the returned `Element` will
     * be `TextField`.
     * 
     *
     * If no `Elements` are selected by this query, this method will return an empty `Iterator` will be returned.
     *
     * @param driver the `WebDriver` with which to drive the browser
     * @return the `Iterator` over all `Element`s selected by this query
     */
    def findAllElements(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Iterator[Element] = driver.findElements(by).asScala.toIterator.map { e => createTypedElement(e, pos) }
    
    /**
     * Returns the first `WebElement` selected by this query, or throws `TestFailedException`
     * if no `WebElement` is selected.
     *
     * @param driver the `WebDriver` with which to drive the browser
     * @return the `WebElement` selected by this query
     * @throws TestFailedException if nothing is selected by this query
     */
    def webElement(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): WebElement = {
      try {
        driver.findElement(by)
      }
      catch {
        case e: org.openqa.selenium.NoSuchElementException =>
          // the following is avoid the suite instance to be bound/dragged into the messageFun, which can cause serialization problem.
          val queryStringValue = queryString
          throw new TestFailedException(
                     (_: StackDepthException) => Some("WebElement '" + queryStringValue + "' not found."),
                     Some(e),
                     pos
                   )
      }
    }
  }

  /**
   * An ID query.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on id("q")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  case class IdQuery(queryString: String) extends Query { val by = By.id(queryString)}

  /**
   * A name query.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on name("q")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  case class NameQuery(queryString: String) extends Query { val by = By.name(queryString) }

  /**
   * An XPath query.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on xpath("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  case class XPathQuery(queryString: String) extends Query { val by = By.xpath(queryString) }

// TODO: Are these case classes just to get at the val?
  /**
   * A class name query.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on className("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  case class ClassNameQuery(queryString: String) extends Query { val by = By.className(queryString) }

  /**
   * A CSS selector query.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on cssSelector("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  case class CssSelectorQuery(queryString: String) extends Query { val by = By.cssSelector(queryString) }

  /**
   * A link text query.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on linkText("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  case class LinkTextQuery(queryString: String) extends Query { val by = By.linkText(queryString) }

  /**
   * A partial link text query.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on partialLinkText("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  case class PartialLinkTextQuery(queryString: String) extends Query { val by = By.partialLinkText(queryString) }

  /**
   * A tag name query.
   *
   * This class enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on tagName("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  case class TagNameQuery(queryString: String) extends Query { val by = By.tagName(queryString) }
  
  /**
   * Returns an ID query.
   *
   * This method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on id("q")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  def id(elementId: String): IdQuery = new IdQuery(elementId)

  /**
   * Returns a name query.
   *
   * This method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on name("q")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  def name(elementName: String): NameQuery = new NameQuery(elementName)

  /**
   * Returns an XPath query.
   *
   * This method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on xpath("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  def xpath(xpath: String): XPathQuery = new XPathQuery(xpath)

  /**
   * Returns a class name query.
   *
   * This method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on className("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  def className(className: String): ClassNameQuery = new ClassNameQuery(className)

  /**
   * Returns a CSS selector query.
   *
   * This method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on cssSelector("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  def cssSelector(cssSelector: String): CssSelectorQuery = new CssSelectorQuery(cssSelector)

  /**
   * Returns a link text query.
   *
   * This method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on linkText("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  def linkText(linkText: String): LinkTextQuery = new LinkTextQuery(linkText)

  /**
   * Returns a partial link text query.
   *
   * This method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on partialLinkText("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  def partialLinkText(partialLinkText: String): PartialLinkTextQuery = new PartialLinkTextQuery(partialLinkText)

  /**
   * Returns a tag name query.
   *
   * This method enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on tagName("???")
   *          ^
   * }}}
   *
   * @param queryString the query string for this query.
   */
  def tagName(tagName: String): TagNameQuery = new TagNameQuery(tagName)

  private def createTypedElement(element: WebElement, pos: source.Position = implicitly[source.Position]): Element = {
    if (isTextField(element))
      new TextField(element)(pos)
    else if (isTextArea(element))
      new TextArea(element)(pos)
    else if (isPasswordField(element))
      new PasswordField(element)(pos)
    else if (isEmailField(element))
      new EmailField(element)(pos)
    else if (isColorField(element))
      new ColorField(element)(pos)
    else if (isDateField(element))
      new DateField(element)(pos)
    else if (isDateTimeField(element))
      new DateTimeField(element)(pos)
    else if (isDateTimeLocalField(element))
      new DateTimeLocalField(element)(pos)
    else if (isMonthField(element))
      new MonthField(element)(pos)
    else if (isNumberField(element))
      new NumberField(element)(pos)
    else if (isRangeField(element))
      new RangeField(element)(pos)
    else if (isSearchField(element))
      new SearchField(element)(pos)
    else if (isTelField(element))
      new TelField(element)(pos)
    else if (isTimeField(element))
      new TimeField(element)(pos)
    else if (isUrlField(element))
      new UrlField(element)(pos)
    else if (isWeekField(element))
      new WeekField(element)(pos)
    else if (isCheckBox(element))
      new Checkbox(element)(pos)
    else if (isRadioButton(element))
      new RadioButton(element)(pos)
    else if (element.getTagName.toLowerCase == "select") {
      val select = new Select(element)
      if (select.isMultiple)
        new MultiSel(element)(pos)
      else
        new SingleSel(element)(pos)
    }
    else
      new Element() { val underlying = element }
  }
  
// XXX
  /**
   * Finds and returns the first element selected by the specified `Query`, wrapped
   * in a `Some`, or `None` if no element is selected.
   *
   * The class of the `Element` returned will be a subtype of `Element` if appropriate.
   * For example, if the query selects a text field, the class of the returned `Element` will
   * be `TextField`.
   * 
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @return the `Element` selected by this query, wrapped in a `Some`, or `None` if
   *   no `Element` is selected
   */
  def find(query: Query)(implicit driver: WebDriver): Option[Element] = query.findElement

  /**
   * Finds and returns the first element selected by the specified string ID or name, wrapped
   * in a `Some`, or `None` if no element is selected. YYY
   *
   * This method will try to lookup by id first. If it cannot find 
   * any element with an id equal to the specified `queryString`, it will then try lookup by name.
   * 
   *
   * The class of the `Element` returned will be a subtype of `Element` if appropriate.
   * For example, if the query selects a text field, the class of the returned `Element` will
   * be `TextField`.
   * 
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @return the `Element` selected by this query, wrapped in a `Some`, or `None` if
   *   no `Element` is selected
   */
  def find(queryString: String)(implicit driver: WebDriver): Option[Element] = 
    new IdQuery(queryString).findElement match {
      case Some(element) => Some(element)
      case None => new NameQuery(queryString).findElement match {
        case Some(element) => Some(element)
        case None => None
      }
    }

  /**
   * Returns an `Iterator` over all `Element`s selected by this query.
   *
   * The class of the `Element`s produced by the returned `Iterator` will be a
   * subtypes of `Element` if appropriate.  For example, if an `Element`representing
   * a text field is returned by the `Iterator`, the class of the returned `Element` will
   * be `TextField`.
   * 
   *
   * If no `Elements` are selected by this query, this method will return an empty `Iterator` will be returned.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @return the `Iterator` over all `Element`s selected by this query
   */
  def findAll(query: Query)(implicit driver: WebDriver): Iterator[Element] = query.findAllElements

  /**
   * Returns an `Iterator` over all `Element`s selected by the specified string ID or name 
   *
   * This method will try to lookup by id first. If it cannot find 
   * any element with an id equal to the specified `queryString`, it will then try lookup by name.
   * 
   *
   * The class of the `Element` returned will be a subtype of `Element` if appropriate.
   * For example, if the query selects a text field, the class of the returned `Element` will
   * be `TextField`.
   * 
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @return the `Iterator` over all `Element`s selected by this query
   */
  def findAll(queryString: String)(implicit driver: WebDriver): Iterator[Element] = {
    val byIdItr = new IdQuery(queryString).findAllElements
    if (byIdItr.hasNext)
      byIdItr
    else 
      new NameQuery(queryString).findAllElements
  }
  
  private def tryQueries[T](queryString: String)(f: Query => T)(implicit driver: WebDriver): T = {
    try {
      f(IdQuery(queryString))
    }
    catch {
      case _: Throwable => f(NameQuery(queryString))
    }
  }
  
  /**
   * Finds and returns the first `TextField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `TextField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `TextField`
   * @return the `TextField` selected by this query
   */
  def textField(query: Query)(implicit driver: WebDriver, pos: source.Position): TextField = new TextField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `TextField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `TextField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `TextField`
   * @return the `TextField` selected by this query
   */
  def textField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): TextField =
    tryQueries(queryString)(q => new TextField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `TextArea` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `TextArea`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `TextArea`
   * @return the `TextArea` selected by this query
   */
  def textArea(query: Query)(implicit driver: WebDriver, pos: source.Position) = new TextArea(query.webElement)(pos)
  
  /**
   * Finds and returns the first `TextArea` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `TextArea`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `TextArea`
   * @return the `TextArea` selected by this query
   */
  def textArea(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): TextArea =
    tryQueries(queryString)(q => new TextArea(q.webElement)(pos))
    
  /**
   * Finds and returns the first `PasswordField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `PasswordField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `PasswordField`
   * @return the `PasswordField` selected by this query
   */
  def pwdField(query: Query)(implicit driver: WebDriver, pos: source.Position): PasswordField = new PasswordField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `PasswordField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `PasswordField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `PasswordField`
   * @return the `PasswordField` selected by this query
   */
  def pwdField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): PasswordField =
    tryQueries(queryString)(q => new PasswordField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `EmailField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `EmailField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `EmailField`
   * @return the `EmailField` selected by this query
   */
  def emailField(query: Query)(implicit driver: WebDriver, pos: source.Position): EmailField = new EmailField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `EmailField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `EmailField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `EmailField`
   * @return the `EmailField` selected by this query
   */
  def emailField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): EmailField =
    tryQueries(queryString)(q => new EmailField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `ColorField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `ColorField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `ColorField`
   * @return the `ColorField` selected by this query
   */
  def colorField(query: Query)(implicit driver: WebDriver, pos: source.Position): ColorField = new ColorField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `ColorField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `ColorField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `ColorField`
   * @return the `ColorField` selected by this query
   */
  def colorField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): ColorField =
    tryQueries(queryString)(q => new ColorField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `DateField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `DateField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `DateField`
   * @return the `DateField` selected by this query
   */
  def dateField(query: Query)(implicit driver: WebDriver, pos: source.Position): DateField = new DateField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `DateField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `DateField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `DateField`
   * @return the `DateField` selected by this query
   */
  def dateField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): DateField =
    tryQueries(queryString)(q => new DateField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `DateTimeField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `DateTimeField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `DateTimeField`
   * @return the `DateTimeField` selected by this query
   */
  def dateTimeField(query: Query)(implicit driver: WebDriver, pos: source.Position): DateTimeField = new DateTimeField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `DateTimeField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `DateTimeField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `DateTimeField`
   * @return the `DateTimeField` selected by this query
   */
  def dateTimeField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): DateTimeField =
    tryQueries(queryString)(q => new DateTimeField(q.webElement)(pos))

  /**
   * Finds and returns the first `DateTimeLocalField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `DateTimeLocalField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `DateTimeLocalField`
   * @return the `DateTimeLocalField` selected by this query
   */
  def dateTimeLocalField(query: Query)(implicit driver: WebDriver, pos: source.Position): DateTimeLocalField = new DateTimeLocalField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `DateTimeLocalField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `DateTimeLocalField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `DateTimeLocalField`
   * @return the `DateTimeLocalField` selected by this query
   */
  def dateTimeLocalField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): DateTimeLocalField =
    tryQueries(queryString)(q => new DateTimeLocalField(q.webElement)(pos))

  /**
   * Finds and returns the first `MonthField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `MonthField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `MonthField`
   * @return the `MonthField` selected by this query
   */
  def monthField(query: Query)(implicit driver: WebDriver, pos: source.Position): MonthField = new MonthField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `MonthField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `MonthField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `MonthField`
   * @return the `MonthField` selected by this query
   */
  def monthField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): MonthField =
    tryQueries(queryString)(q => new MonthField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `NumberField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `NumberField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `NumberField`
   * @return the `NumberField` selected by this query
   */
  def numberField(query: Query)(implicit driver: WebDriver, pos: source.Position): NumberField = new NumberField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `NumberField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `NumberField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `NumberField`
   * @return the `NumberField` selected by this query
   */
  def numberField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): NumberField =
    tryQueries(queryString)(q => new NumberField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `RangeField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `RangeField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `RangeField`
   * @return the `RangeField` selected by this query
   */
  def rangeField(query: Query)(implicit driver: WebDriver, pos: source.Position): RangeField = new RangeField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `RangeField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `RangeField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `RangeField`
   * @return the `RangeField` selected by this query
   */
  def rangeField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): RangeField =
    tryQueries(queryString)(q => new RangeField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `SearchField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `SearchField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `SearchField`
   * @return the `SearchField` selected by this query
   */
  def searchField(query: Query)(implicit driver: WebDriver, pos: source.Position): SearchField = new SearchField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `SearchField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `SearchField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `SearchField`
   * @return the `SearchField` selected by this query
   */
  def searchField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): SearchField =
    tryQueries(queryString)(q => new SearchField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `TelField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `TelField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `TelField`
   * @return the `TelField` selected by this query
   */
  def telField(query: Query)(implicit driver: WebDriver, pos: source.Position): TelField = new TelField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `TelField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `TelField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `TelField`
   * @return the `TelField` selected by this query
   */
  def telField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): TelField =
    tryQueries(queryString)(q => new TelField(q.webElement)(pos))
  
  /**
   * Finds and returns the first `TimeField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `TimeField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `TimeField`
   * @return the `TimeField` selected by this query
   */
  def timeField(query: Query)(implicit driver: WebDriver, pos: source.Position): TimeField = new TimeField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `TimeField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `TimeField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `TimeField`
   * @return the `TimeField` selected by this query
   */
  def timeField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): TimeField =
    tryQueries(queryString)(q => new TimeField(q.webElement)(pos))

  /**
   * Finds and returns the first `UrlField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `UrlField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `UrlField`
   * @return the `UrlField` selected by this query
   */
  def urlField(query: Query)(implicit driver: WebDriver, pos: source.Position): UrlField = new UrlField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `UrlField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `UrlField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `UrlField`
   * @return the `UrlField` selected by this query
   */
  def urlField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): UrlField =
    tryQueries(queryString)(q => new UrlField(q.webElement)(pos))

  /**
   * Finds and returns the first `WeekField` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `WeekField`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `WeekField`
   * @return the `WeekField` selected by this query
   */
  def weekField(query: Query)(implicit driver: WebDriver, pos: source.Position): WeekField = new WeekField(query.webElement)(pos)
  
  /**
   * Finds and returns the first `WeekField` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `WeekField`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `WeekField`
   * @return the `WeekField` selected by this query
   */
  def weekField(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): WeekField =
    tryQueries(queryString)(q => new WeekField(q.webElement)(pos))

  /**
   * Finds and returns `RadioButtonGroup` selected by the specified group name, throws `TestFailedException` if 
   * no element with the specified group name is found, or found any element with the specified group name but not a `RadioButton`
   * 
   * @param groupName the group name with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if no element with the specified group name is found, or found any element with the specified group name but not a `RadioButton`
   * @return the `RadioButtonGroup` selected by this query
   */
  def radioButtonGroup(groupName: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]) = new RadioButtonGroup(groupName, driver)(pos)
  
  /**
   * Finds and returns the first `RadioButton` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `RadioButton`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `RadioButton`
   * @return the `RadioButton` selected by this query
   */
  def radioButton(query: Query)(implicit driver: WebDriver, pos: source.Position) = new RadioButton(query.webElement)(pos)
  
  /**
   * Finds and returns the first `RadioButton` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `RadioButton`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `RadioButton`
   * @return the `RadioButton` selected by this query
   */
  def radioButton(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): RadioButton =
    tryQueries(queryString)(q => new RadioButton(q.webElement)(pos))
  
  /**
   * Finds and returns the first `Checkbox` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `Checkbox`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `Checkbox`
   * @return the `Checkbox` selected by this query
   */
  def checkbox(query: Query)(implicit driver: WebDriver, pos: source.Position) = new Checkbox(query.webElement)(pos)
  
  /**
   * Finds and returns the first `Checkbox` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `Checkbox`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `Checkbox`
   * @return the `Checkbox` selected by this query
   */
  def checkbox(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Checkbox =
    tryQueries(queryString)(q => new Checkbox(q.webElement)(pos))
  
  /**
   * Finds and returns the first `SingleSel` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `SingleSel`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `SingleSel`
   * @return the `SingleSel` selected by this query
   */
  def singleSel(query: Query)(implicit driver: WebDriver, pos: source.Position) = new SingleSel(query.webElement)(pos)
  
  /**
   * Finds and returns the first `SingleSel` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `SingleSel`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `SingleSel`
   * @return the `SingleSel` selected by this query
   */
  def singleSel(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): SingleSel =
    tryQueries(queryString)(q => new SingleSel(q.webElement)(pos))
  
  /**
   * Finds and returns the first `MultiSel` selected by the specified `Query`, throws `TestFailedException` 
   * if element not found or the found element is not a `MultiSel`.
   *
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `MultiSel`
   * @return the `MultiSel` selected by this query
   */
  def multiSel(query: Query)(implicit driver: WebDriver, pos: source.Position) = new MultiSel(query.webElement)(pos)
  
  /**
   * Finds and returns the first `MultiSel` selected by the specified string ID or name, throws `TestFailedException` 
   * if element not found or the found element is not a `MultiSel`.
   *
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if element not found or found element is not a `MultiSel`
   * @return the `MultiSel` selected by this query
   */
  def multiSel(queryString: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): MultiSel =
    tryQueries(queryString)(q => new MultiSel(q.webElement)(pos))
    
  /**
   * This object is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This object enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * click on "aButton"
   * ^
   * }}}
   */
  object click {
    /**
     * Click on the specified `WebElement`
     * 
     * @param element the `WebElement` to click on
     */
    def on(element: WebElement): Unit = {
      element.click()
    }
    
    /**
     * Click on the first `Element` selected by the specified `Query`
     * 
     * @param query the `Query` with which to search
     * @param driver the `WebDriver` with which to drive the browser
     */
    def on(query: Query)(implicit driver: WebDriver): Unit = {
      query.webElement.click()
    }
  
    /**
     * Click on the first `Element` selected by the specified string ID or name
     * 
     * @param queryString the string with which to search, first by ID then by name
     * @param driver the `WebDriver` with which to drive the browser
     */
    def on(queryString: String)(implicit driver: WebDriver): Unit = {
      // stack depth is not correct if just call the button("...") directly.
      val target = tryQueries(queryString)(q => q.webElement)
      on(target)
    }
    
    /**
     * Click on the specified `Element`
     * 
     * @param element the `Element` to click on
     */
    def on(element: Element): Unit = {
      element.underlying.click()
    }
  }
  
  /**
   * Click on the specified `WebElement`
   * 
   * @param element the `WebElement` to click on
   */
  def clickOn(element: WebElement): Unit = {
    click on element
  }
  
  /**
   * Click on the first `Element` selected by the specified `Query`
   * 
   * @param query the `Query` with which to search
   * @param driver the `WebDriver` with which to drive the browser
   */
  def clickOn(query: Query)(implicit driver: WebDriver): Unit = {
    click on query
  }
  
  /**
   * Click on the first `Element` selected by the specified string ID or name
   * 
   * @param queryString the string with which to search, first by ID then by name
   * @param driver the `WebDriver` with which to drive the browser
   */
  def clickOn(queryString: String)(implicit driver: WebDriver): Unit = {
    click on queryString
  }
  
  /**
   * Click on the specified `Element`
   * 
   * @param element the `Element` to click on
   */
  def clickOn(element: Element): Unit = {
    click on element
  }
  
  /**
   * Submit the form where current active element belongs to, and throws TestFailedException if current active element is not 
   * in a form or underlying WebDriver encounters problem when submitting the form.  If this causes the current page to change, 
   * this call will block until the new page is loaded.
   * 
   * @param driver the `WebDriver` with which to drive the browser
   * @throws TestFailedException if current active element is not in a form or underlying WebDriver encounters problem when submitting the form.
   */
  def submit()(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Unit = {
    try {
      (switch to activeElement).underlying.submit()
    }
    catch {
      case e: org.openqa.selenium.NoSuchElementException => 
        throw new TestFailedException(
                     (_: StackDepthException) => Some("Current element is not a form element."),
                     Some(e),
                     pos
                   )
      case e: Throwable => 
        // Could happens as bug in different WebDriver, like NullPointerException in HtmlUnitDriver when element is not a form element.
        // Anyway, we'll just wrap them as TestFailedException
        throw new TestFailedException(
                     (_: StackDepthException) => Some("WebDriver encountered problem to submit(): " + e.getMessage),
                     Some(e),
                     pos
                   )
    }
  }
  
  /**
   * Sets the amount of time the driver should wait when searching for an element that is not immediately present.
   *
   * When searching for requested elements, Selenium will poll the page until the requested element (or at least one of multiple requested
   * elements) is found or this "implicit wait" timeout has expired.
   * If the timeout expires, Selenium will throw `NoSuchElementException`, which ScalaTest's Selenium DSL will wrap in a `TestFailedException`.
   * 
   *
   * You can alternatively set this timeout to zero and use ScalaTest's `eventually` construct.
   * 
   *
   * This method invokes `manage.timeouts.implicitlyWait` on the passed `WebDriver`. See the documentation of Selenium's
   * `WebDriver#Timeouts` interface for more information.
   * 
   *
   * @param timeout the time span to implicitly wait
   * @param driver the `WebDriver` on which to set the implicit wait
   */
  def implicitlyWait(timeout: Span)(implicit driver: WebDriver): Unit = {
    driver.manage.timeouts.implicitlyWait(timeout.totalNanos, TimeUnit.NANOSECONDS)
  }

  /**
   * Close all windows, and exit the driver.
   * 
   * @param driver the `WebDriver` on which to quit. 
   */
  def quit()(implicit driver: WebDriver): Unit = {
    driver.quit()
  }
  
  /**
   * Get an opaque handle to current active window that uniquely identifies it within the implicit driver instance.
   * 
   * @param driver the `WebDriver` with which to drive the browser
   */
  def windowHandle(implicit driver: WebDriver): String = driver.getWindowHandle
  
  /**
   * Get a set of window handles which can be used to iterate over all open windows
   * 
   * @param driver the `WebDriver` with which to drive the browser
   */
  def windowHandles(implicit driver: WebDriver): Set[String] = driver.getWindowHandles.asScala.toSet
  
  /**
   * This object is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This object enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * switch to alertBox
   * ^
   * }}}
   */
  object switch {
    /**
     * Switch to the specified `SwitchTarget`
     * 
     * @param target the `SwitchTarget` to switch to
     * @param driver the `WebDriver` with which to drive the browser
     * @return instance of specified `SwitchTarget`'s type parameter
     */
    def to[T](target: SwitchTarget[T])(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): T = {
      target.switch(driver)(pos)
    }
  }
  
  /**
   * This value supports switching to the currently active element in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to activeElement
   *           ^
   * }}}
   */
  val activeElement = new ActiveElementTarget()
  
  /**
   * This value supports switching to the alert box in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to alertBox
   *           ^
   * }}}
   */
  val alertBox = new AlertTarget()
  
  /**
   * This value supports switching to the default content in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to defaultContent
   *           ^
   * }}}
   */
  val defaultContent = new DefaultContentTarget()
  
  /**
   * This method supports switching to a frame by index in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to frame(0)
   *           ^
   * }}}
   * 
   * @param index the index of frame to switch to
   * @return a FrameIndexTarget instance
   */
  def frame(index: Int) = new FrameIndexTarget(index)
  
  /**
   * This method supports switching to a frame by name or ID in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to frame("name")
   *           ^
   * }}}
   * 
   * @param nameOrId name or ID of the frame to switch to
   * @return a FrameNameOrIdTarget instance
   */
  def frame(nameOrId: String) = new FrameNameOrIdTarget(nameOrId)
  
  /**
   * This method supports switching to a frame by web element in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   * 
   * @param element `WebElement` which is contained in the frame to switch to
   * @return a FrameWebElementTarget instance
   */
  def frame(element: WebElement) = new FrameWebElementTarget(element)
  
  /**
   * This method supports switching to a frame by element in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   * 
   * @param element `Element` which is contained in the frame to switch to
   * @return a FrameElementTarget instance
   */
  def frame(element: Element) = new FrameElementTarget(element)
  
  /**
   * This method supports switching to a frame by `Query` in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   * 
   * @param query `Query` used to select `WebElement` which is contained in the frame to switch to 
   * @return a FrameWebElementTarget instance
   */
  def frame(query: Query)(implicit driver: WebDriver) = new FrameWebElementTarget(query.webElement)
  
  /**
   * This class supports switching to a window by name or handle in ScalaTest's Selenium DSL.
   * Please see the documentation for <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This class is enables the following syntax:
   * 
   *
   * {{{
   * switch to window(windowHandle)
   *           ^
   * }}}
   * 
   * @param nameOrHandle name or window handle of the window to switch to
   * @return a WindowTarget instance
   */
  def window(nameOrHandle: String) = new WindowTarget(nameOrHandle)
  
  /**
   * Switch to the specified `SwitchTarget`
   * 
   * @param target the `SwitchTarget` to switch to
   * @param driver the `WebDriver` with which to drive the browser
   * @return instance of specified `SwitchTarget`'s type parameter
   */
  def switchTo[T](target: SwitchTarget[T])(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): T = switch to target
  
  /**
   * Go back to previous page.
   * 
   * @param driver the `WebDriver` with which to drive the browser
   */
  def goBack()(implicit driver: WebDriver): Unit = {
    driver.navigate.back()
  }
  
  /**
   * Go forward to next page.
   * 
   * @param driver the `WebDriver` with which to drive the browser
   */
  def goForward()(implicit driver: WebDriver): Unit = {
    driver.navigate.forward()
  }
  
  /**
   * Reload the current page.
   * 
   * @param driver the `WebDriver` with which to drive the browser
   */
  def reloadPage()(implicit driver: WebDriver): Unit = {
    driver.navigate.refresh()
  }
  
  /**
   * This object is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This object enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * add cookie("aName", "aValue") 
   * ^
   * }}}
   */
  object add {
    private def addCookie(cookie: Cookie)(implicit driver: WebDriver): Unit = {
      driver.manage.addCookie(cookie)
    }
    
    // Default values determined from http://code.google.com/p/selenium/source/browse/trunk/java/client/src/org/openqa/selenium/Cookie.java
    /**
     * Add cookie in the web browser.  If the cookie's domain name is left blank (default), it is assumed that the cookie is meant for the domain of the current document.
     * 
     * @param name cookie's name
     * @param value cookie's value
     * @param path cookie's path
     * @param expiry cookie's expiry data
     * @param domain cookie's domain name
     * @param secure whether this cookie is secured.
     * @param driver the `WebDriver` with which to drive the browser 
     */
    def cookie(name: String, value: String, path: String = "/", expiry: Date = null, domain: String = null, secure: Boolean = false)(implicit driver: WebDriver): Unit = { 
      addCookie(new Cookie(name, value, domain, path, expiry, secure))
    }
  }
  
  /**
   * Get a saved cookie from web browser, throws TestFailedException if the cookie does not exist.
   * 
   * @param name cookie's name
   * @return a WrappedCookie instance
   */
  def cookie(name: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): WrappedCookie = {
    getCookie(name)(driver, pos)
  }
  
  private def getCookie(name: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): WrappedCookie = {
    driver.manage.getCookies.asScala.toList.find(_.getName == name) match {
      case Some(cookie) => 
        new WrappedCookie(cookie)
      case None =>
        throw new TestFailedException(
                     (_: StackDepthException) => Some("Cookie '" + name + "' not found."),
                     None,
                     pos
                   )
    }
  }
  
  /**
   * This object is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This object enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * delete cookie "aName" 
   * ^
   * 
   * delete all cookies
   * ^
   * }}}
   */
  object delete {
    private def deleteCookie(name: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Unit = {
      val cookie = getCookie(name)
      if (cookie == null) 
        throw new TestFailedException(
                     (_: StackDepthException) => Some("Cookie '" + name + "' not found."),
                     None,
                     pos
                   )
      driver.manage.deleteCookie(cookie.underlying)
    }
    
    /**
     * Delete cookie with the specified name from web browser, throws TestFailedException if the specified cookie does not exists.
     * 
     * @param name cookie's name
     * @param driver the `WebDriver` with which to drive the browser
     */
    def cookie(name: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Unit = {
      deleteCookie(name)
    }
    
    /**
     * Delete all cookies in the current domain from web browser.
     * 
     * @param driver the `WebDriver` with which to drive the browser
     */
    def all(cookies: CookiesNoun)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Unit = {
      driver.manage.deleteAllCookies()
    }
  }

  /**
     * Add cookie in the web browser.  If the cookie's domain name is left blank (default), it is assumed that the cookie is meant for the domain of the current document.
     * 
     * @param name cookie's name
     * @param value cookie's value
     * @param path cookie's path
     * @param expiry cookie's expiry data
     * @param domain cookie's domain name
     * @param secure whether this cookie is secured.
     * @param driver the `WebDriver` with which to drive the browser 
     */
  def addCookie(name: String, value: String, path: String = "/", expiry: Date = null, domain: String = null, secure: Boolean = false)(implicit driver: WebDriver): Unit = {
    add cookie (name, value, path, expiry, domain, secure)
  }
  
  /**
   * Delete cookie with the specified name from web browser, throws TestFailedException if the specified cookie does not exists.
   * 
   * @param name cookie's name
   * @param driver the `WebDriver` with which to drive the browser
   */
  def deleteCookie(name: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Unit = {
    delete cookie name
  }
  
  /**
   * Delete all cookies in the current domain from web browser.
   * 
   * @param driver the `WebDriver` with which to drive the browser
   */
  def deleteAllCookies()(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Unit = {
    delete all cookies
  }
  
  /**
   * Check if screenshot is supported
   * 
   * @param driver the `WebDriver` with which to drive the browser
   * @return true if screenshot is supported, false otherwise
   */
  def isScreenshotSupported(implicit driver: WebDriver): Boolean = driver.isInstanceOf[TakesScreenshot]
  
  /**
   * This object is part of ScalaTest's Selenium DSL. Please see the documentation for
   * <a href="WebBrowser.html">`WebBrowser`</a> for an overview of the Selenium DSL.
   *
   * This object enables syntax such as the following:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * capture
   * ^
   * 
   * capture to "MyScreenshot.png" 
   * ^
   * }}}
   */
  object capture {
    
    /**
     * Capture screenshot and save it as the specified name (if file name does not end with .png, it will be extended automatically) in capture directory, 
     * which by default is system property's java.io.tmpdir.  You can change capture directory by calling `setCaptureDir`
     * 
     * @param fileName screenshot file name, if does not end with .png, it will be extended automatically
     */
    def to(fileName: String)(implicit driver: WebDriver): Unit = {
      driver match {
        case takesScreenshot: TakesScreenshot => 
          val tmpFile = takesScreenshot.getScreenshotAs(OutputType.FILE)
          val outFile = new File(targetDir, if (fileName.toLowerCase.endsWith(".png")) fileName else fileName + ".png")
          new FileOutputStream(outFile).getChannel.transferFrom(
            new FileInputStream(tmpFile).getChannel, 0, Long.MaxValue)
        case _ =>
          throw new UnsupportedOperationException("Screen capture is not support by " + driver.getClass.getName)
      }
    }

    /**
      * Capture screenshot and save it as the specified directory.
      *
      * @param dirName directory name to save screenshot.
      */
    def toDir(dirName: String)(implicit driver: WebDriver): Unit = {
      driver match {
        case takesScreenshot: TakesScreenshot =>
          val tmpFile = takesScreenshot.getScreenshotAs(OutputType.FILE)
          val dir = new File(dirName)
          if (!dir.exists)
            dir.mkdirs()
          val outFile = new File(dir, if (tmpFile.getName.toLowerCase.endsWith(".png")) "ScalaTest-" + tmpFile.getName else "ScalaTest-" + tmpFile.getName + ".png")
          new FileOutputStream(outFile).getChannel.transferFrom(
            new FileInputStream(tmpFile).getChannel, 0, Long.MaxValue)
        case _ =>
          throw new UnsupportedOperationException("Screen capture is not support by " + driver.getClass.getName)
      }
    }
    
    /**
     * Capture screenshot and save it in capture directory, which by default is system property's java.io.tmpdir.  
     * You can change capture directory by calling `setCaptureDir`
     */
    def apply()(implicit driver: WebDriver): File = {
      driver match {
        case takesScreenshot: TakesScreenshot => 
          val tmpFile = takesScreenshot.getScreenshotAs(OutputType.FILE)
          val fileName = tmpFile.getName
          val outFile = new File(targetDir, if (fileName.toLowerCase.endsWith(".png")) fileName else fileName + ".png")
          new FileOutputStream(outFile).getChannel.transferFrom(
            new FileInputStream(tmpFile).getChannel, 0, Long.MaxValue)
          outFile
        case _ =>
          throw new UnsupportedOperationException("Screen capture is not support by " + driver.getClass.getName)
      }
    }
  }
  
  /**
   * Capture screenshot and save it as the specified name (if file name does not end with .png, it will be extended automatically) in capture directory, 
   * which by default is system property's java.io.tmpdir.  You can change capture directory by calling `setCaptureDir`
   * 
   * @param fileName screenshot file name, if does not end with .png, it will be extended automatically
   */
  def captureTo(fileName: String)(implicit driver: WebDriver): Unit = {
    capture to fileName
  }
  
  // Can get by with volatile, because the setting doesn't depend on the getting
  @volatile private var targetDir = new File(System.getProperty("java.io.tmpdir"))
    
  /**
   * Set capture directory.
   * 
   * @param targetDirPath the path of capture directory
   */
  def setCaptureDir(targetDirPath: String): Unit = {
      targetDir = 
        if (targetDirPath.endsWith(File.separator))
          new File(targetDirPath)
        else
          new File(targetDirPath + File.separator)
      if (!targetDir.exists)
        targetDir.mkdirs()
  }
  
  /**
   * Execute the given function, if `ModifiableMessage` exception is thrown from the given function, 
   * a screenshot will be captured automatically into capture directory, which by default is system property's java.io.tmpdir.  
   * You can change capture directory by calling `setCaptureDir`
   * 
   * @param fun function to execute
   * @return the value returned by `fun`
   */
  def withScreenshot[T](fun: => T)(implicit driver: WebDriver): T = {
    try {
      fun
    }
    catch {
      case e: org.scalatest.exceptions.ModifiableMessage[_] =>
        throw e.modifyMessage{ (currentMessage: Option[String]) => 
          val captureFile: File = capture.apply()
          currentMessage match {
            case Some(currentMsg) => 
              Some(currentMsg + "; screenshot captured in " + captureFile.getAbsolutePath)
            case None => 
              Some("screenshot captured in " + captureFile.getAbsolutePath)
          }
        }
    }
  }
  
  /**
   * Executes JavaScript in the context of the currently selected frame or window.  The script fragment provided will be executed as the body of an anonymous function. 
   * 
   * Within the script, you can use `document` to refer to the current document. Local variables will not be available once the script has finished executing, but global variables will.
   * 
   * 
   * To return a value (e.g. if the script contains a return statement), then the following steps will be taken:
   * 
   * 
   * <ol>
   *   <li>For an HTML element, this method returns a WebElement</li>
   *   <li>For a decimal, a Double is returned</li>
   *   <li>For a non-decimal number, a Long is returned</li>
   *   <li>For a boolean, a Boolean is returned</li>
   *   <li>For all other cases, a String is returned</li>
   *   <li>For an array, return a List<Object> with each object following the rules above. We support nested lists</li>
   *   <li>Unless the value is null or there is no return value, in which null is returned</li>
   * </ol>
   *
   * Script arguments must be a number, boolean, String, WebElement, or a List of any combination of these. An exception will
   * be thrown if the arguments do not meet these criteria. The arguments will be made available to the JavaScript via the "arguments" variable.
   * (Note that although this behavior is specified by <a href="http://selenium.googlecode.com/git/docs/api/java/org/openqa/selenium/JavascriptExecutor.html">Selenium's JavascriptExecutor Javadoc</a>,
   * it may still be possible for the underlying `JavascriptExecutor` implementation to return an objects of other types.
   * For example, `HtmlUnit` has been observed to return a `java.util.Map` for a Javascript object.)
   * 
   * 
   * @param script the JavaScript to execute
   * @param args the arguments to the script, may be empty
   * @return One of Boolean, Long, String, List or WebElement. Or null (following <a href="http://selenium.googlecode.com/git/docs/api/java/org/openqa/selenium/JavascriptExecutor.html">Selenium's JavascriptExecutor Javadoc</a>)
   */
  def executeScript[T](script: String, args: AnyRef*)(implicit driver: WebDriver): AnyRef =
    driver match {
      case executor: JavascriptExecutor => executor.executeScript(script, args.toArray : _*)
      case _ => throw new UnsupportedOperationException("Web driver " + driver.getClass.getName + " does not support javascript execution.")
    }
  
  /**
   * Executes an asynchronous piece of JavaScript in the context of the currently selected frame or window.  Unlike executing synchronous JavaScript, 
   * scripts executed with this method must explicitly signal they are finished by invoking the provided callback. This callback is always injected into 
   * the executed function as the last argument.
   * 
   * The first argument passed to the callback function will be used as the script's result. This value will be handled as follows: 
   * 
   * 
   * <ol> 
   *   <li>For an HTML element, this method returns a WebElement</li>
   *   <li>For a number, a Long is returned</li>
   *   <li>For a boolean, a Boolean is returned</li>
   *   <li>For all other cases, a String is returned</li>
   *   <li>For an array, return a List<Object> with each object following the rules above. We support nested lists</li>
   *   <li>Unless the value is null or there is no return value, in which null is returned</li>
   * </ol>
   * 
   * Script arguments must be a number, boolean, String, WebElement, or a List of any combination of these. An exception will 
   * be thrown if the arguments do not meet these criteria. The arguments will be made available to the JavaScript via the "arguments" variable.
   * (Note that although this behavior is specified by <a href="http://selenium.googlecode.com/git/docs/api/java/org/openqa/selenium/JavascriptExecutor.html">Selenium's JavascriptExecutor Javadoc</a>,
   * it may still be possible for the underlying `JavascriptExecutor` implementation to return an objects of other types.
   * For example, `HtmlUnit` has been observed to return a `java.util.Map` for a Javascript object.)
   * 
   * 
   * @param script the JavaScript to execute
   * @param args the arguments to the script, may be empty
   * @return One of Boolean, Long, String, List, WebElement, or null (following <a href="http://selenium.googlecode.com/git/docs/api/java/org/openqa/selenium/JavascriptExecutor.html">Selenium's JavascriptExecutor Javadoc</a>)
   */
  def executeAsyncScript(script: String, args: AnyRef*)(implicit driver: WebDriver): AnyRef =
    driver match {
      case executor: JavascriptExecutor => executor.executeAsyncScript(script, args.toArray : _*)
      case _ => throw new UnsupportedOperationException("Web driver " + driver.getClass.getName + " does not support javascript execution.")
    }
  
  /**
   * Sets the amount of time to wait for an asynchronous script to finish execution before throwing an exception.
   * 
   * @param timeout the amount of time to wait for an asynchronous script to finish execution before throwing exception
   */
  def setScriptTimeout(timeout: Span)(implicit driver: WebDriver): Unit = {
    driver.manage().timeouts().setScriptTimeout(timeout.totalNanos, TimeUnit.NANOSECONDS);
  }

  // Clears the text field or area, then presses the passed keys
  /**
   * Clears the current active `TextField` or `TextArea`, and presses the passed keys.  
   * Throws `TestFailedException` if current active is not `TextField` or `TextArea`.
   * 
   * @param value keys to press in current active `TextField` or `TextArea`
   */
  def enter(value: String)(implicit driver: WebDriver, pos: source.Position = implicitly[source.Position]): Unit = {
    val ae = switch to activeElement
    ae match {
      case tf: TextField => tf.value = value
      case ta: TextArea => ta.value = value
      case pf: PasswordField => pf.value = value
      case pf: EmailField => pf.value = value
      case pf: SearchField => pf.value = value
      case pf: TelField => pf.value = value
      case pf: UrlField => pf.value = value
      case _ => 
        throw new TestFailedException(
                     (_: StackDepthException) => Some("Currently selected element is neither a text field, text area, password field, email field, search field, tel field or url field"),
                     None,
                     pos
                   )
    }
  }

  /**
   * Press the passed keys to current active element.
   * 
   * @param value keys to press in current active element
   */
  def pressKeys(value: String)(implicit driver: WebDriver): Unit = {
    val ae: WebElement = driver.switchTo.activeElement
    ae.sendKeys(value)
  }
}

/**
 * Companion object that facilitates the importing of `WebBrowser` members as 
 * an alternative to mixing it in. One use case is to import `WebBrowser` members so you can use
 * them in the Scala interpreter.
 */
object WebBrowser extends WebBrowser

/**
 * Trait declaring a `webDriver` field that enables tests to be abstracted across different kinds of `WebDriver`s.
 *
 * This trait enables you to place tests that you want to run in multiple browsers in a trait with a self type of
 * `WebBrowser with Driver`, like this:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * trait MyBrowserTests {
 *   this: WebBrowser with Driver =&gt;
 *   // Your browser tests
 * }
 * }}}
 *
 * Then you can create concrete subclasses for each actual browser you want to run those tests in:
 *
 * {{{  <!-- class="stHighlight" -->
 * class MyBrowserTestsWithChrome extends MyBrowserTests with Chrome
 * class MyBrowserTestsWithSafari extends MyBrowserTests with Safari
 * class MyBrowserTestsWithInternetExplorer extends MyBrowserTests with InternetExplorer
 * class MyBrowserTestsWithFirefox extends MyBrowserTests with Firefox
 * }}}
 */
trait Driver { this: WebBrowser =>

  /**
   * An implicit `WebDriver`.
   *
   * This abstract field is implemented by subtraits `HtmlUnit`, `FireFox`, `Safari`, `Chrome`,
   * and `InternetExplorer`.
   * 
   */
  implicit val webDriver: WebDriver
}

/**
 * `WebBrowser` subtrait that defines an implicit `WebDriver` for HTMLUnit (an `org.openqa.selenium.htmlunit.HtmlUnitDriver`), with JavaScript
 * enabled by default.
 *
 * Note: You can disable JavaScript with:
 * 
 *
 * {{{
 * webDriver.setJavascriptEnabled(false)
 * }}}
 */
trait HtmlUnit extends WebBrowser with Driver with ScreenshotCapturer {

  /**
   * `WebBrowser` subtrait that defines an implicit `WebDriver` for HTMLUnit (an `org.openqa.selenium.htmlunit.HtmlUnitDriver`), with JavaScript
   * enabled by default.
   *
   * Note: You can disable JavaScript with:
   * 
   *
   * {{{
   * webDriver.setJavascriptEnabled(false)
   * }}}
   */
  implicit val webDriver = new HtmlUnitDriver()

  webDriver.setJavascriptEnabled(true)

  /**
   * Captures a screenshot and saves it as a file in the specified directory.
   */
  def captureScreenshot(directory: String): Unit = {
    capture toDir directory
  }
}

/**
 * Companion object that facilitates the importing of `HtmlUnit` members as 
 * an alternative to mixing it in. One use case is to import `HtmlUnit` members so you can use
 * them in the Scala interpreter.
 */
object HtmlUnit extends HtmlUnit

/**
 * `WebBrowser` subtrait that defines an implicit `WebDriver` for Firefox (an `org.openqa.selenium.firefox.FirefoxDriver`).
 *
 * The `FirefoxDriver` uses the `FirefoxProfile` defined as `firefoxProfile`. By default this is just a `new FirefoxProfile`.
 * You can mutate this object to modify the profile, or override `firefoxProfile`.
 * 
 */
trait Firefox extends WebBrowser with Driver with ScreenshotCapturer {

  /**
   * The `FirefoxProfile` passed to the constructor of the `FirefoxDriver` returned by `webDriver`.
   *
   * The `FirefoxDriver` uses the `FirefoxProfile` defined as `firefoxProfile`. By default this is just a `new FirefoxProfile`.
   * You can mutate this object to modify the profile, or override `firefoxProfile`.
   * 
   */
  val firefoxProfile = new FirefoxProfile()

  /**
   * `WebBrowser` subtrait that defines an implicit `WebDriver` for Firefox (an `org.openqa.selenium.firefox.FirefoxDriver`), with a default
   * Firefox profile.
   *
   * The `FirefoxDriver` uses the `FirefoxProfile` defined as `firefoxProfile`. By default this is just a `new FirefoxProfile`.
   * You can mutate this object to modify the profile, or override `firefoxProfile`.
   * 
   */
  implicit val webDriver: WebDriver = new FirefoxDriver(firefoxProfile)

  /**
   * Captures a screenshot and saves it as a file in the specified directory.
   */
  def captureScreenshot(directory: String): Unit = {
    capture toDir directory
  }
}

/**
 * Companion object that facilitates the importing of `Firefox` members as 
 * an alternative to mixing it in. One use case is to import `Firefox` members so you can use
 * them in the Scala interpreter.
 */
object Firefox extends Firefox

/**
 * `WebBrowser` subtrait that defines an implicit `WebDriver` for Safari (an `org.openqa.selenium.safari.SafariDriver`).
 */
trait Safari extends WebBrowser with Driver with ScreenshotCapturer {
  /**
   * `WebBrowser` subtrait that defines an implicit `WebDriver` for Safari (an `org.openqa.selenium.safari.SafariDriver`).
   */
  implicit val webDriver = new SafariDriver()

  /**
   * Captures a screenshot and saves it as a file in the specified directory.
   */
  def captureScreenshot(directory: String): Unit = {
    capture toDir directory
  }
}

/**
 * Companion object that facilitates the importing of `Safari` members as 
 * an alternative to mixing it in. One use case is to import `Safari` members so you can use
 * them in the Scala interpreter.
 */
object Safari extends Safari

/**
 * `WebBrowser` subtrait that defines an implicit `WebDriver` for Chrome (an `org.openqa.selenium.chrome.ChromeDriver`).
 */
trait Chrome extends WebBrowser with Driver with ScreenshotCapturer {
  /**
   * `WebBrowser` subtrait that defines an implicit `WebDriver` for Chrome (an `org.openqa.selenium.chrome.ChromeDriver`).
   */
  implicit val webDriver = new ChromeDriver()

  /**
   * Captures a screenshot and saves it as a file in the specified directory.
   */
  def captureScreenshot(directory: String): Unit = {
    capture toDir directory
  }
}

/**
 * Companion object that facilitates the importing of `Chrome` members as 
 * an alternative to mixing it in. One use case is to import `Chrome` members so you can use
 * them in the Scala interpreter.
 */
object Chrome extends Chrome

/**
 * `WebBrowser` subtrait that defines an implicit `WebDriver` for Internet Explorer (an `org.openqa.selenium.ie.InternetExplorerDriver`).
 */
trait InternetExplorer extends WebBrowser with Driver with ScreenshotCapturer {
  /**
   * `WebBrowser` subtrait that defines an implicit `WebDriver` for Internet Explorer (an `org.openqa.selenium.ie.InternetExplorerDriver`).
   */
  implicit val webDriver = new InternetExplorerDriver()

  /**
   * Captures a screenshot and saves it as a file in the specified directory.
   */
  def captureScreenshot(directory: String): Unit = {
    capture toDir directory
  }
}

/**
 * Companion object that facilitates the importing of `InternetExplorer` members as 
 * an alternative to mixing it in. One use case is to import `InternetExplorer` members so you can use
 * them in the Scala interpreter.
 */
object InternetExplorer extends InternetExplorer

/*
 * <p>
 * If you mix in <a href="../ScreenshotOnFailure.html"><code>ScreenshotOnFailure</code></a>, ScalaTest will capture a screenshot and store it to either the system temp directory
 * or a directory you choose, and send the filename to the report, associated with the failed test. The <code>ScreenshotOnFailure</code> trait requires that it be
 * mixed into a <a href="../ScreenshotCapturer.html"><code>ScreenshotCapturer</code></a>, which trait <code>WebBrowser</code> does not extend. To satisfy this
 * requirement, you can extend one of <code>WebBrowser</code>'s subtraits, such as:
 * </p>
 * 
 * <pre class="stHighlight">
 * class WebAppSpec extends Firefox with ScreenshotOnFailure {
 *   // ...
 * }
 * </pre>
 *
*/
