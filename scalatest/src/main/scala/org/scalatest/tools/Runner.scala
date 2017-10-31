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
package org.scalatest.tools

import org.scalatest._
import java.net.URL
import java.net.MalformedURLException
import java.net.URLClassLoader
import java.io.File
import java.io.IOException
import javax.swing.SwingUtilities
import java.util.concurrent.ArrayBlockingQueue
import java.util.regex.Pattern
import java.util.concurrent.Semaphore
import org.scalatest.events._
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory
import SuiteDiscoveryHelper._
import org.scalatest.time.Span
import org.scalatest.time.Seconds
import org.scalatest.time.Millis
import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.junit.JUnitWrapperSuite
import org.scalatest.testng.TestNGWrapperSuite
import Suite.{mergeMap, CHOSEN_STYLES, SELECTED_TAG, testSortingReporterTimeout}
import ArgsParser._
import org.scalactic.Requirements._

/*
Command line args:

a - archive count for dashboard reporter     --archive
A - select previously failed and/or canceled tests to rerun --again
b - run a testNG test (b for Beust)          --testng
B - 
c - parallel execution (--parallel)          --parallel (deprecated, will be custom reporter)
C - custom reporter (temporarily)
d - dashboard reporter                       --dashboard
D - config map pair                          -D
e - standard error reporter (--stderr-reporter)    --stderr
E -
f - file reporter                            --file
F - Span scale factor
g - graphical reporter                       --graphical
G -
h - HTML Reporter                            --html
H -
i - this one is used for the Suite ID        --suiteId
I -
j - currently JUnit directly (can drop and use WrapWith)   --junit
J - 
k - socket reporter XML
K - socket reporter binary
l - tags to exclude                          --exclude
L -
m - members only path                        --members
M - memory reporter                          --memory (records failed tests in a file, so they can be rerun with A)
n - tags to include                          --include
N -
o - standard out reporter
O -
p - space-separated runpath (currently deprecated, will be parallel execution)
P - parallel execution (temporarily)
q - Suffix? -q Spec will only look at class files whose name ends in Spec
Q - equalivalent to -q Suite -q Spec
r - custom reporter (currently deprecated, will be runpath)
R - space-separated runpath (temporarily)
s - suite class name (to become a glob)
S -
t - test name
T - sorting timeout                        --sorting-timeout
u - JUnit XML reporter
U -
v - ScalaTest version number (also -version and --version)
V -
w - wildcard path
W - slowpoke detector (with two integral args, for timeout (delay) and interval (period), in seconds)
x - save for ScalaTest native XML
X -
y - sets org.scalatest.chosenstyle -y FunSpec or -y "FunSpec FunSuite"
-Y for after -h to set a custom style sheet
z - test name wildcard
Z -

StringReporter configuration params:
A - drop AlertProvided events
B - drop NoteProvided events
C - drop TestSucceeded events
D - show all durations
E - drop TestPending events
F - show full stack traces
*G - reminder with full stack traces
H - drop SuiteStarting events
*I - Reminder without stack traces
J
*K - exclude TestCanceled events from reminder
L - drop SuiteCompleted events
M - drop MarkupProvided events
N - drop TestStarting events
O - drop InfoProvided events
P - drop ScopeOpened events
Q - drop ScopeClosed events
R - drop ScopePending events 
S - show short stack traces
*T - reminder with short stack traces
U - unformatted mode
V
W - without color
X - drop TestIgnored events
Z
*/

private[tools] case class SuiteConfig(suite: Suite, dynaTags: DynaTags, requireSelectedTag: Boolean, excludeNestedSuites: Boolean)
private[scalatest] case class ConcurrentConfig(numThreads: Int, enableSuiteSortingReporter: Boolean)
private[tools] case class SlowpokeConfig(delayInMillis: Long, periodInMillis: Long)

/**
 * Application that runs a suite of tests.
 *
 * Note: this application offers the full range of ScalaTest features via command line arguments described below. If you just want
 * to run a suite of tests from the command line and see results on the standard output, you may prefer to use <a href="../run$.html">ScalaTest's simple runner</a>.
 * 
 *
 * The basic form of a `Runner` invocation is:
 * 
 *
 * {{{ class="stExamples">
 * scala [-cp scalatest-&lt;version&gt;.jar:...] org.scalatest.tools.Runner [arguments]
 * }}}
 *
 * The arguments `Runner` accepts are described in the following table:
 * 
 *
 * <table style="border-collapse: collapse; border: 1px solid black">
 * <tr><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">argument</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">description</th><th style="background-color: #CCCCCC; border-width: 1px; padding: 3px; text-align: center; border: 1px solid black">example</th></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-D''key''=''value''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">defines a key/value pair for the <a href="#configMapSection">''config map''</a></td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-DmaxConnections=100`</a></td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-R ''&lt;runpath elements&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">the <a href="#specifyingARunpath">specifies the ''runpath''</a> from which tests classes will be<br/>discovered and loaded (Note: only one `-R` allowed)</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">''Unix'': `-R target/classes:target/generated/classes`<br/>''Windows'': `-R target\classes;target\generated\classes`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-n ''&lt;tag name&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><a href="#specifyingTagsToIncludeAndExclude">specifies a tag to include</a> (Note: only one tag name allowed per `-n`)</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-n UnitTests -n FastTests`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-l ''&lt;tag name&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><a href="#specifyingTagsToIncludeAndExclude">specifies a tag to exclude</a> (Note: only one tag name allowed per `-l`)</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-l SlowTests -l PerfTests`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-P''[S][integer thread count]''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><a href="#executingSuitesInParallel">specifies a parallel run</a>, with optional suite sorting and thread count<br/>(Note: only one `-P` allowed)</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-P`, `-PS`, `-PS 8`, ''or'' `-P8`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-s ''&lt;suite class name&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">specifies a <a href="#executingSuites">suite class</a> to run</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-s com.company.project.StackSpec`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-m ''&lt;members-only package&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">requests that suites that are <a href="#membersOnlyWildcard">direct members of the specified package</a><br/> be discovered and run</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-m com.company.project`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-w ''&lt;wildcard package&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">requests that suites that are <a href="#membersOnlyWildcard">members of the specified package or its subpackages</a><br/>be discovered and run</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-w com.company.project`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-q ''&lt;suffixes&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">specify <a href="#specifyingSuffixesToDiscover">suffixes to discover</a></td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-q Spec -q Suite`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-Q`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">discover only classes whose names end with `Spec` or `Suite`<br/>(or other suffixes specified by `-q`)</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-Q`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-j ''&lt;JUnit class name&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">instantiate and run a <a href="#specifyingJUnitTests">JUnit test class</a></td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-j StackTestClass`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-b ''&lt;TestNG XML file&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">run <a href="#specifyingTestNGXML">TestNG tests</a> using the specified TestNG XML file</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-b testng.xml`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-F ''&lt;span scale factor&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">a factor by which to <a href="#scalingTimeSpans">scale time spans</a><br/>(Note: only one `-F` is allowed)</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-F 10` ''or'' `-F 2.5`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-T ''&lt;sorting timeout&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">specifies a integer timeout (in seconds) for sorting the events of<br/>parallel runs back into sequential order</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-T 5`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-y ''&lt;chosen styles&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">specifies <a href="#specifyingChosenStyles">chosen styles</a> for your project</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-y org.scalatest.FlatSpec`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-i ''&lt;suite ID&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">specifies a <a href="#selectingSuitesAndTests">suite to run by ID</a> (Note: must follow `-s`, <br/>and is intended to be used primarily by tools such as IDEs.)</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-i com.company.project.FileSpec-file1.txt`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-t ''&lt;test name&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><a href="#selectingSuitesAndTests">select the test</a> with the specified name</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-t "An empty Stack should complain when popped"`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-z ''&lt;test name substring&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center"><a href="#selectingSuitesAndTests">select tests</a> whose names include the specified substring</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-z "popped"`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-g''[NCXEHLOPQMD]''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">select the graphical reporter</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-g`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-f''[NCXEHLOPQMDWSFU] &lt;filename&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">select the file reporter</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-f output.txt`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-u ''&lt;directory name&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">select the JUnit XML reporter</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-u target/junitxmldir`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-h ''&lt;directory name&gt;'' [-Y ''&lt;css file name&gt;'']`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">select the HTML reporter, optionally including the specified CSS file</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-h target/htmldir -Y src/main/html/customStyles.css`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-v`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">print the ScalaTest version</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-v` ''or, also'' `-version`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-o''[NCXEHLOPQMDWSFU]''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">select the standard output reporter</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-o`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-e''[NCXEHLOPQMDWSFU]''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">select the standard error reporter</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-e`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-C''[NCXEHLOPQMD] &lt;reporter class&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">select a custom reporter</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-C com.company.project.BarReporter`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-M ''&lt;file name&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">memorize failed and canceled tests in a file, so they can be rerun with -A (again)</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-M rerun.txt`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-A ''&lt;file name&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">used in conjunction with -M (momento) to select previously failed<br/>and canceled tests to rerun again</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-A rerun.txt`</td></tr>
 * <tr><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-W ''&lt;delay&gt;'' ''&lt;period&gt;''`</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">requests <a href="#slowpokeNotifications">notifications of ''slowpoke'' tests</a>, tests that have been running<br/>longer than ''delay'' seconds, every ''period'' seconds.</td><td style="border-width: 1px; padding: 3px; border: 1px solid black; text-align: center">`-W 60 60`</td></tr>
 * </table>
 *
 * The simplest way to start `Runner` is to specify the directory containing your compiled tests as the sole element of the runpath, for example:
 * 
 *
 * {{{ class="stExamples">scala -classpath scalatest-&lt;version&gt;.jar org.scalatest.tools.Runner -R compiled_tests</pre>
 *
 * Given the previous command, `Runner` will discover and execute all `Suite`s in the `compiled_tests` directory and its subdirectories,
 * and show results in graphical user interface (GUI).
 * 
 *
 * <a name="executingSuites"></a>
 * ==Executing suites==
 *
 * Each `-s` argument must be followed by one and only one fully qualified class name. The class must either extend `Suite` and
 * have a public, no-arg constructor, or be annotated by a valid `WrapWith` annotation.
 * 
 *
 * <a name="configMapSection"></a>
 * ==Specifying the config map==
 *
 * A ''config map'' contains pairs consisting of a string key and a value that may be of any type. (Keys that start with
 * &quot;org.scalatest.&quot; are reserved for ScalaTest. Configuration values that are themselves strings may be specified on the
 * `Runner` command line.
 * Each configuration pair is denoted with a "-D", followed immediately by the key string, an &quot;=&quot;, and the value string.
 * For example:
 * 
 *
 * {{{ class="stExamples">-Ddbname=testdb -Dserver=192.168.1.188</pre>
 *
 * <a name="specifyingARunpath"></a>
 * ==Specifying a runpath==
 *
 * A runpath is the list of filenames, directory paths, and/or URLs that `Runner`
 * uses to load classes for the running test. If runpath is specified, `Runner` creates
 * a custom class loader to load classes available on the runpath.
 * The graphical user interface reloads the test classes anew for each run
 * by creating and using a new instance of the custom class loader for each run.
 * The classes that comprise the test may also be made available on
 * the classpath, in which case no runpath need be specified.
 * 
 *
 * The runpath is specified with the <b>-R</b> option. The <b>-R</b> must be followed by a space,
 * a double quote (`"`), a white-space-separated list of
 * paths and URLs, and a double quote. If specifying only one element in the runpath, you can leave off
 * the double quotes, which only serve to combine a white-space separated list of strings into one
 * command line argument. If you have path elements that themselves have a space in them, you must
 * place a backslash (\) in front of the space. Here's an example:
 * 
 *
 * {{{ class="stExamples">-R "serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar target/class\ files"</pre>
 *
 * <a name="specifyingReporters"></a>
 * ==Specifying reporters==
 *
 * Reporters can be specified  on the command line in any of the following
 * ways:
 * 
 *
 * <ul>
 * <li> `<b>-g[configs...]</b>` - causes display of a graphical user interface that allows
 *    tests to be run and results to be investigated</li>
 * <li> `<b>-f[configs...] &lt;filename&gt;</b>` - causes test results to be written to
 *     the named file</li>
 * <li> `<b>-u &lt;directory&gt;</b>` - causes test results to be written to
 *      junit-style xml files in the named directory</li>
 * <li> `<b>-h &lt;directory&gt; [-Y &lt;CSS file&gt;]</b>` - causes test results to be written to
 *      HTML files in the named directory, optionally included the specified CSS file</li>
 * <li> `<b>-a &lt;number of files to archive&gt;</b>` - causes specified number of old
 *      summary and durations files to be archived (in summaries/ and durations/ subdirectories)
 *      for dashboard reporter (default is two)</li>
 * <li> `<b>-o[configs...]</b>` - causes test results to be written to
 *     the standard output</li>
 * <li> `<b>-e[configs...]</b>` - causes test results to be written to
 *     the standard error</li>
 * <li> `<b>-k &lt;host&gt; &lt;port&gt;</b>` - causes test results to be written to
 *      socket in the named host and port number, using XML format</li>
 * <li> `<b>-K &lt;host&gt; &lt;port&gt;</b>` - causes test results to be written to
 *      socket in the named host and port number, using Java object binary format</li>
 * <li> `<b>-C[configs...] &lt;reporterclass&gt;</b>` - causes test results to be reported to
 *     an instance of the specified fully qualified `Reporter` class name</li>
 * </ul>
 *
 * The `<b>[configs...]</b>` parameter, which is used to configure reporters, is described in the next section.
 * 
 *
 * The `<b>-C</b>` option causes the reporter specified in
 * `<b>&lt;reporterclass&gt;</b>` to be
 * instantiated.
 * Each reporter class specified with a <b>-C</b> option must be public, implement
 * `org.scalatest.Reporter`, and have a public no-arg constructor.
 * Reporter classes must be specified with fully qualified names. 
 * The specified reporter classes may be
 * deployed on the classpath. If a runpath is specified with the
 * `-R` option, the specified reporter classes may also be loaded from the runpath.
 * All specified reporter classes will be loaded and instantiated via their no-arg constructor.
 * 
 *
 * For example, to run a suite named `MySuite` from the `mydir` directory
 * using two reporters, the graphical reporter and a file reporter
 * writing to a file named `"test.out"`, you would type:
 * 
 *
 * {{{ class="stExamples">java -jar scalatest.jar -R mydir <b>-g -f test.out</b> -s MySuite</pre>
 *
 * The `<b>-g</b>`, `<b>-o</b>`, or `<b>-e</b>` options can
 * appear at most once each in any single command line.
 * Multiple appearances of `<b>-f</b>` and `<b>-C</b>` result in multiple reporters
 * unless the specified `<b>&lt;filename&gt;</b>` or `<b>&lt;reporterclass&gt;</b>` is
 * repeated. If any of `<b>-g</b>`, `<b>-o</b>`, `<b>-e</b>`,
 * `<b>&lt;filename&gt;</b>` or `<b>&lt;reporterclass&gt;</b>` are repeated on
 * the command line, the `Runner` will print an error message and not run the tests.
 * 
 *
 * `Runner` adds the reporters specified on the command line to a ''dispatch reporter'',
 * which will dispatch each method invocation to each contained reporter. `Runner` will pass
 * the dispatch reporter to executed suites. As a result, every
 * specified reporter will receive every report generated by the running suite of tests.
 * If no reporters are specified, a graphical
 * runner will be displayed that provides a graphical report of
 * executed suites.
 * 
 *
 * <a name="configuringReporters"></a>
 * ==Configuring reporters==
 *
 * Each reporter option on the command line can include configuration characters. Configuration characters
 * are specified immediately following the `<b>-g</b>`, `<b>-o</b>`,
 * `<b>-e</b>`, `<b>-f</b>`, or `<b>-C</b>`. The following configuration
 * characters, which cause reports to be dropped, are valid for any reporter:
 * 
 *
 * <ul>
 * <li> `<b>N</b>` - drop `TestStarting` events</li>
 * <li> `<b>C</b>` - drop `TestSucceeded` events</li>
 * <li> `<b>X</b>` - drop `TestIgnored` events</li>
 * <li> `<b>E</b>` - drop `TestPending` events</li>
 * <li> `<b>H</b>` - drop `SuiteStarting` events</li>
 * <li> `<b>L</b>` - drop `SuiteCompleted` events</li>
 * <li> `<b>O</b>` - drop `InfoProvided` events</li>
 * <li> `<b>P</b>` - drop `ScopeOpened` events</li>
 * <li> `<b>Q</b>` - drop `ScopeClosed` events</li>
 * <li> `<b>R</b>` - drop `ScopePending` events</li>
 * <li> `<b>M</b>` - drop `MarkupProvided` events</li>
 * </ul>
 *
 * A dropped event will not be delivered to the reporter at all. So the reporter will not know about it and therefore not
 * present information about the event in its report. For example, if you specify `-oN`, the standard output reporter
 * will never receive any `TestStarting` events and will therefore never report them. The purpose of these
 * configuration parameters is to allow users to selectively remove events they find add clutter to the report without
 * providing essential information.
 * 
 *
 * The following three reporter configuration parameters may additionally be used on standard output (-o), standard error (-e),
 * and file (-f) reporters: 
 * 
 *
 * <ul>
 * <li> `<b>W</b>` - without color</li>
 * <li> `<b>D</b>` - show all durations</li>
 * <li> `<b>S</b>` - show short stack traces</li>
 * <li> `<b>F</b>` - show full stack traces</li>
 * <li> `<b>U</b>` - unformatted mode</li>
 * <li> `<b>I</b>` - show reminder of failed and canceled tests without stack traces</li>
 * <li> `<b>T</b>` - show reminder of failed and canceled tests with short stack traces</li>
 * <li> `<b>G</b>` - show reminder of failed and canceled tests with full stack traces</li>
 * <li> `<b>K</b>` - exclude `TestCanceled` events from reminder</li>
 * </ul>
 *
 * If you specify a W, D, S, F, U, R, T, G, or K for any reporter other than standard output, standard error, or file reporters, `Runner`
 * will complain with an error message and not perform the run.
 * 
 *
 * Configuring a standard output, error, or file reporter with `D` will cause that reporter to
 * print a duration for each test and suite.  When running in the default mode, a duration will only be printed for
 * the entire run.
 * 
 *
 * Configuring a standard output, error, or file reporter with `F` will cause that reporter to print full stack traces for all exceptions,
 * including `TestFailedExceptions`. Every `TestFailedException` contains a stack depth of the
 * line of test code that failed so that users won't need to search through a stack trace to find it. When running in the default,
 * mode, these reporters will only show full stack traces when other exceptions are thrown, such as an exception thrown
 * by production code. When a `TestFailedException` is thrown in default mode, only the source filename and
 * line number of the line of test code that caused the test to fail are printed along with the error message, not the full stack
 * trace. 
 * 
 *
 * The 'U' unformatted configuration removes some formatting from the output and adds verbosity.
 * The purpose of unformatted (or, "ugly") mode is to facilitate debugging of parallel runs. If you have
 * tests that fail or hang during parallel runs, but succeed when run sequentially, unformatted mode can help.
 * In unformatted mode, you can see exactly what is happening when it is happening. Rather than attempting to make the output
 * look as pretty and human-readable as possible, unformatted mode will just print out verbose information about each event
 * as it arrives, helping you track down the problem
 * you are trying to debug.
 * 
 *
 * By default, a standard output, error, or file reporter inserts ansi escape codes into the output printed to change and later reset
 * terminal colors. Information printed as a result of run starting, completed, and stopped events
 * is printed in cyan. Information printed as a result of ignored or pending test events is shown in yellow. Information printed
 * as a result of test failed, suite aborted, or run aborted events is printed in red. All other information is printed in green.
 * The purpose of these colors is to facilitate speedy reading of the output, especially the finding of failed tests, which can
 * get lost in a sea of passing tests. Configuring a standard output, error, or file reporter into without-color mode (`W`) will
 * turn off this behavior. No ansi codes will be inserted.
 * 
 *
 * The `R`, `T`, and `G` options enable "reminders" of failed and, optionally, canceled tests to be printed
 * at the end of the summary. This minimizes or eliminates the need to search and scroll backwards to find out what tests failed or were canceled.
 * For large test suites, the actual failure message could have scrolled off the top of the buffer, making it otherwise impossible
 * to see what failed. You can configure the detail level of the stack trace for regular reports of failed and canceled tests independently
 * from that of reminders. To set the detail level for regular reports, use `S` for short stack traces, `F` for
 * full stack traces, or nothing for the default of no stack trace. To set the detail level for reminder reports, use `T` for
 * reminders with short stack traces, `G` for reminders with full stack traces in reminders, or `R` for reminders
 * with no stack traces. If you wish to exclude reminders of canceled tests, ''i.e.'', only see reminders of failed tests, specify
 * `K` along with one of `R`, `T`, or `G`, as in `"-oRK"`.
 * 
 *
 * For example, to run a suite using two reporters, the graphical reporter configured to present every reported event
 * and a standard error reporter configured to present everything but test starting, test succeeded, test ignored, test
 * pending, suite starting, suite completed, and info provided events, you would type:
 * 
 *
 * `scala -classpath scalatest-&lt;version&gt;.jar -R mydir '''-g -eNDXEHLO''' -s MySuite`
 * 
 *
 * Note that no white space is allowed between the reporter option and the initial configuration
 * parameters. So `"-e NDXEHLO"` will not work,
 * `"-eNDXEHLO"` will work.
 * 
 *
 * <a name="specifyingTagsToIncludeAndExclude"></a>
 * ==Specifying tags to include and exclude==
 *
 * You can specify tag names of tests to include or exclude from a run. To specify tags to include,
 * use `-n` followed by a white-space-separated list of tag names to include, surrounded by
 * double quotes. (The double quotes are not needed if specifying just one tag.)  Similarly, to specify tags
 * to exclude, use `-l` followed by a white-space-separated
 * list of tag names to exclude, surrounded by double quotes. (As before, the double quotes are not needed
 * if specifying just one tag.) If tags to include is not specified, then all tests
 * except those mentioned in the tags to exclude (and in the `org.scalatest.Ignore` tag), will be executed.
 * (In other words, the absence of a `-n` option is like a wildcard, indicating all tests be included.)
 * If tags to include is specified, then only those tests whose tags are mentioned in the argument following `-n`
 * and not mentioned in the tags to exclude, will be executed. For more information on test tags, see
 * the <a href="../Suite.html">documentation for `Suite`</a>. Here are some examples:
 * 
 *
 * <ul>
 * <li>`-n CheckinTests`</li>
 * <li>`-n FunctionalTests -l org.scalatest.tags.Slow`</li>
 * <li>`-n "CheckinTests FunctionalTests" -l "org.scalatest.tags.Slow org.scalatest.tags.Network"`</li>
 * </ul>
 * 
 *
 * <a name="specifyingSuffixesToDiscover"></a>
 * ==Specifying suffixes to discover==
 *
 * You can specify suffixes of `Suite` names to discover. To specify suffixes to discover,
 * use `-q` followed by a vertical-bar-separated list of suffixes to discover, surrounded by
 * double quotes. (The double quotes are not needed if specifying just one suffix.)  Or you can specify
 * them individually using multiple -q's.
 * If suffixes to discover is not specified, then all suffixes are considered.
 * If suffixes is specified, then only those Suites whose class names end in one of the specified suffixes
 * will be considered during discovery. Here are some examples:
 * 
 *
 * <ul>
 * <li>`-q Spec`</li>
 * <li>`-q "Spec|Suite"`</li>
 * <li>`-q Spec -q Suite`</li>
 * </ul>
 * 
 *
 * Option -Q can be used to specify a default set of suffixes "Spec|Suite". If you specify both -Q and -q, you'll get Spec
 * and Suite in addition to the other suffix or suffixes you specify with -q.
 * 
 *
 * Specifying suffixes can speed up the discovery process because class files with names not ending the specified suffixes
 * can be immediately disqualified, without needing to load and inspect them to see if they either extend `Suite`
 * and declare a public, no-arg constructor, or are annotated with `WrapWith`. 
 * 
 *
 * <a name="executingSuitesInParallel"></a>
 * ==Executing `Suite`s in parallel==
 *
 * With the proliferation of multi-core architectures, and the often parallelizable nature of tests, it is useful to be able to run
 * tests in parallel. If you include `-P` on the command line, `Runner` will pass a `Distributor` to
 * the `Suite`s you specify with `-s`. `Runner` will set up a thread pool to execute any `Suite`s
 * passed to the `Distributor`'s `put` method in parallel. Trait `Suite`'s implementation of
 * `runNestedSuites` will place any nested `Suite`s into this `Distributor`. Thus, if you have a `Suite`
 * of tests that must be executed sequentially, you should override `runNestedSuites` as described in the <a href="../Distributor.html">documentation for `Distributor`</a>.
 * 
 *
 * The `-P` option may optionally be appended with a number (e.g.
 * "`-P10`" -- no intervening space) to specify the number of
 * threads to be created in the thread pool.  If no number (or 0) is
 * specified, the number of threads will be decided based on the number of
 * processors available.
 * 
 *
 * <a name="specifyingSuites"></a>
 * ==Specifying `Suite`s==
 *
 * Suites are specified on the command line with a <b>-s</b> followed by the fully qualified
 * name of a `Suite` subclass, as in:
 * 
 *
 * {{{ class="stExamples">-s com.artima.serviceuitest.ServiceUITestkit</pre>
 *
 * Each specified suite class must be public, a subclass of
 * `org.scalatest.Suite`, and contain a public no-arg constructor.
 * `Suite` classes must be specified with fully qualified names. 
 * The specified `Suite` classes may be
 * loaded from the classpath. If a runpath is specified with the
 * `-R` option, specified `Suite` classes may also be loaded from the runpath.
 * All specified `Suite` classes will be loaded and instantiated via their no-arg constructor.
 * 
 *
 * The runner will invoke `execute` on each instantiated `org.scalatest.Suite`,
 * passing in the dispatch reporter to each `execute` method.
 * 
 *
 * `Runner` is intended to be used from the command line. It is included in `org.scalatest`
 * package as a convenience for the user. If this package is incorporated into tools, such as IDEs, which take
 * over the role of runner, object `org.scalatest.tools.Runner` may be excluded from that implementation of the package.
 * All other public types declared in package `org.scalatest.tools.Runner` should be included in any such usage, however,
 * so client software can count on them being available.
 * 
 *
 * <a name="membersOnlyWildcard"></a>
 * ==Specifying "members-only" and "wildcard" `Suite` paths==
 *
 * If you specify `Suite` path names with `-m` or `-w`, `Runner` will automatically
 * discover and execute accessible `Suite`s in the runpath that are either a member of (in the case of `-m`)
 * or enclosed by (in the case of `-w`) the specified path. As used in this context, a ''path'' is a portion of a fully qualified name.
 * For example, the fully qualifed name `com.example.webapp.MySuite` contains paths `com`, `com.example`, and `com.example.webapp`.
 * The fully qualifed name `com.example.webapp.MyObject.NestedSuite` contains paths `com`, `com.example`,
 * `com.example.webapp`, and `com.example.webapp.MyObject`.
 * An ''accessible `Suite`'' is a public class that extends `org.scalatest.Suite`
 * and defines a public no-arg constructor. Note that `Suite`s defined inside classes and traits do not have no-arg constructors,
 * and therefore won't be discovered. `Suite`s defined inside singleton objects, however, do get a no-arg constructor by default, thus
 * they can be discovered.
 * 
 *
 * For example, if you specify `-m com.example.webapp`
 * on the command line, and you've placed `com.example.webapp.RedSuite` and `com.example.webapp.BlueSuite`
 * on the runpath, then `Runner` will instantiate and execute both of those `Suite`s. The difference
 * between `-m` and `-w` is that for `-m`, only `Suite`s that are direct members of the named path
 * will be discovered. For `-w`, any `Suite`s whose fully qualified
 * name begins with the specified path will be discovered. Thus, if `com.example.webapp.controllers.GreenSuite`
 * exists on the runpath, invoking `Runner` with `-w com.example.webapp` will cause `GreenSuite`
 * to be discovered, because its fully qualifed name begins with `"com.example.webapp"`. But if you invoke `Runner`
 * with `-m com.example.webapp`, `GreenSuite` will ''not'' be discovered because it is directly
 * a member of `com.example.webapp.controllers`, not `com.example.webapp`.
 * 
 *
 * If you specify no `-s`, `-m`, or `-w` arguments on the command line to `Runner`, it will discover and execute all accessible `Suite`s
 * in the runpath.
 * 
 *
 * <a name="specifyingChosenStyles"></a>
 * ==Specifying chosen styles==
 *
 * You can optionally specify chosen styles for a ScalaTest run. ScalaTest supports different styles of
 * testing so that different teams can use the style or styles that best suits their situation and culture. But
 * in any one project, it is recommended you decide on one main style for unit testing, and
 * consistently use only that style for unit testing throughout the project. If you also have integration
 * tests in your project, you may wish to pick a different style for them than you are using for unit testing.
 * You may want to allow certain styles to be used in special testing situations on a project, but in general,
 * it is best to minimize the styles used in any given project to a few, or one.
 * 
 *
 * To facilitate the communication and enforcement of a team's style choices for a project, you can
 * specify the chosen styles in your project build. If chosen styles is defined, ScalaTest style traits that are
 * not among the chosen list will abort with a message complaining that the style trait is not one of the
 * chosen styles. The style name for each ScalaTest style trait is its fully qualified name. For example,
 * to specify that `org.scalatest.FunSpec` as your chosen style you'd pass this to
 * `Runner`:
 * 
 *
 * {{{ class="stExamples">-y org.scalatest.FunSpec</pre>
 *
 * If you wanted `org.scalatest.FunSpec` as your main unit testing style, but also wanted to
 * allow `PropSpec` for test matrixes and `FeatureSpec` for
 * integration tests, you would write:
 * 
 *
 * {{{ class="stExamples">-y org.scalatest.FunSpec -y org.scalatest.PropSpec -y org.scalatest.FeatureSpec</pre>
 *
 * To select `org.scalatest.FlatSpec` as your main unit testing style, but allow
 * `org.scalatest.fixture.FlatSpec` for multi-threaded unit tests, you'd write:
 * 
 *
 * {{{ class="stExamples">-y org.scalatest.FlatSpec -y org.scalatest.fixture.FlatSpec</pre>
 *
 * The style name for a suite is obtained by invoking its `styleName` method. Custom style
 * traits can override this method so that a custom style can participate in the chosen styles list.
 * 
 *
 * Because ScalaTest is so customizable, a determined programmer could circumvent
 * the chosen styles check, but in practice `-y` should be persuasive enough tool
 * to keep most team members in line.
 * 
 *
 * <a name="selectingSuitesAndTests"></a>
 * ==Selecting suites and tests==
 *
 * `Runner` accepts three arguments that facilitate selecting suites and tests: `-i`, `-t`, and `-z`.
 * The `-i` option enables a suite to be selected by suite ID. This argument is intended to allow tools such as IDEs or build tools to
 * rerun specific tests or suites from information included in the results of a previous run.  A `-i` must follow a `-s`
 * that specifies a class with a public, no-arg constructor. The `-i` parameter can be used, for example, to rerun a nested suite that
 * declares no zero-arg constructor, which was created by containing suite that does declare a no-arg constructor. In this case, `-s` would be
 * used to specify the class ScalaTest can instantiate directly, the containing suite that has a public, no-arg constructor, and `-i` would be
 * used to select the desired nested suite. One important use case for `-i` is to enable such a nested suite that aborted during the previous run
 * to be rerun. <!-- TODO: Need to point them to more info, maybe in SuiteMixin's rerunner method description? -->
 * 
 *
 * The `-t` argument allows a test to be selected by its (complete) test name. Like `-i`, the `-t` argument is primarily intented
 * to be used by tools such as IDEs or build tools, to rerun selected tests based on information obtained from the results of a previous run.
 * For example, `-t` could be used to rerun a test that failed in the previous run.
 * The `-t` argument can be used directly by users, but because descriptive test names are usually rather long, the `-z` argument (described next), will
 * usually be a more practical choice for users. If a `-t` follows either `-s` or `-i`, then it only applies to the suite
 * identified.  If it is specified independent of a `-s` or `-i`, then discovery is performed to find all Suites containing the test name.
 * 
 *
 * The `-z` option allows tests to be selected by a simplified wildcard: any test whose name includes the substring specified after `-z`
 * will be selected. For example, `-z popped` would select tests named `"An empty stack should complain when popped"` and `"A non-empty stack
 * should return the last-pushed value when popped`, but not `"An empty stack should be empty"`. In short, `-z popped` would select any
 * tests whose name includes the substring `"popped"`, and not select any tests whose names don't include `"popped"`. This simplified
 * approach to test name wildcards, which was suggested by Mathias Doenitz, works around the difficulty of finding an actual wildcard character that will work
 * reliably on different operating systems.  Like `-t`, if `-z` follows `-s` or `-i`, then it only applies to the Suite specified.  Otherwise discovery is performed to find all Suites containing test names that include the substring.
 * 
 *
 * <a name="scalingTimeSpans"></a>
 * ==Specifying a span scale factor==
 *
 * If you specify a integer or floating point ''span scale factor'' with `-F`, trait <a href="../concurrent/ScaledTimeSpans.html">`ScaledTimeSpans`</a>
 * trait will  return the specified value from its implementation of `spanScaleFactor`. This allows you to tune the "patience" of a run (how long to wait
 * for asynchronous operations) from the command line. For more information, see the documentation for trait <a href="../concurrent/ScaledTimeSpans.html">`ScaledTimeSpans`</a>.
 * 
 *
 * <a name="specifyingTestNGXML"></a>
 * ==Specifying TestNG XML config file paths==
 *
 * If you specify one or more file paths with `-b` (b for Beust, the last name of TestNG's creator), `Runner` will create a `org.scalatest.testng.TestNGWrapperSuite`,
 * passing in a `List` of the specified paths. When executed, the `TestNGWrapperSuite` will create one `TestNG` instance
 * and pass each specified file path to it for running. If you include `-b` arguments, you must include TestNG's jar file on the class path or runpath.
 * The `-b` argument will enable you to run existing `TestNG` tests, including tests written in Java, as part of a ScalaTest run.
 * You need not use `-b` to run suites written in Scala that extend `TestNGSuite`. You can simply run such suites with 
 * `-s`, `-m`, or `-w` parameters.
 * 
 *
 * <a name="specifyingJUnitTests"></a>
 * ==Specifying JUnit tests==
 *
 * JUnit tests, including ones written in Java, may be run by specifying
 * `-j classname`, where the classname is a valid JUnit class
 * such as a TestCase, TestSuite, or a class implementing a static suite()
 * method returning a TestSuite. 
 * To use this option you must include a JUnit jar file on your classpath.
 * 
 *
 * <a name="memorizingAndRerunning"> </a>
 * ==Memorizing and rerunning failed and canceled tests==
 *
 * You can memorize failed and canceled tests using `-M`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * -M failed-canceled.txt
 * }}}
 *
 * All failed and canceled tests will be memorized in `failed-canceled.txt`, to rerun them again, you use `-A`:
 *
 * {{{  <!-- class="stHighlight" -->
 * -A failed-canceled.txt
 * }}}
 *
 * <a name="slowpokeNotifications"> </a>
 * ==Slowpoke notifications==
 *
 * You can request to recieve periodic notifications of ''slowpokes'', tests that have been running longer than a given amount of time, specified in
 * seconds by the first integer after `-W`, the ''delay''.
 * You specify the period between slowpoke notifications in seconds with the second integer after `-W`, the ''period''. Thus to receive
 * notifications very minute of tests that have been running longer than two minutes, you'd use:
 * 
 * 
 * {{{ class="stGray">
 * `-W 120 60`
 * }}}
 *
 * Slowpoke notifications will be sent via <a href="../events/AlertProvided.html">`AlertProvided`</a> events. The standard out reporter, for example, 
 * will report such notifications like:
 * 
 *
 * {{{  <!-- class="stREPL" -->
 * <span class="stYellow">*** Test still running after 2 minutes, 13 seconds: suite name: ExampleSpec, test name: An egg timer should take 10 minutes.</span>
 * }}}
 *
 * @author Bill Venners
 * @author George Berger
 * @author Josh Cough
 * @author Chee Seng
 */
object Runner {

  private val RUNNER_JFRAME_START_X: Int = 150
  private val RUNNER_JFRAME_START_Y: Int = 100
  
  @volatile private[scalatest] var spanScaleFactor: Double = 1.0

  private final val DefaultNumFilesToArchive = 2
  
  //                     TO
  // We always include a PassFailReporter on runs in order to determine
  // whether or not all tests passed.
  //
  // The thread that calls Runner.run() will either start a GUI, if a graphic
  // reporter was requested, or just run the tests itself. If a GUI is started,
  // an event handler thread will get going, and it will start a RunnerThread,
  // which will actually do the running. The GUI can repeatedly start RunnerThreads
  // and RerunnerThreads, until the GUI is closed. If -P is specified, that means
  // the tests should be run concurrently, which in turn means a Distributor will
  // be passed to the execute method of the Suites, which will in turn populate
  // it with its nested suites instead of executing them directly in the same
  // thread. The Distributor works in conjunction with a pool of threads that
  // will take suites out of the distributor queue and execute them. The DispatchReporter
  // will serialize all reports via an actor, which because that actor uses receive
  // not react, will have its own thread. So the DispatchReporter actor's thread will
  // be the one that actually invokes TestFailed, RunAborted, etc., on this PassFailReporter.
  // The thread that invoked Runner.run(), will be the one that calls allTestsPassed.
  //
  // The thread that invoked Runner.run() will be the one to instantiate the PassFailReporter
  // and in its primary constructor acquire the single semaphore permit. This permit will
  // only be released by the DispatchReporter's actor thread when a runAborted, runStopped,
  // or runCompleted is invoked. allTestsPassed will block until it can reacquire the lone
  // semaphore permit. Thus, a PassFailReporter can just be used for one run, then it is
  // spent. A new PassFailReporter is therefore created each time the Runner.run() method is invoked.
  //
  private class PassFailReporter extends Reporter {

    @volatile private var failedAbortedOrStopped = false
    private val runDoneSemaphore = new Semaphore(1)
    runDoneSemaphore.acquire()

    override def apply(event: Event): Unit = {
      event match {
        case _: TestFailed =>
          failedAbortedOrStopped = true

        case _: RunAborted =>
          failedAbortedOrStopped = true
          runDoneSemaphore.release()

        case _: SuiteAborted =>
          failedAbortedOrStopped = true

        case _: RunStopped =>
          failedAbortedOrStopped = true
          runDoneSemaphore.release() 

        case _: RunCompleted =>
          runDoneSemaphore.release()

        case _ =>
      }
    }

    def allTestsPassed = {
      runDoneSemaphore.acquire()
      !failedAbortedOrStopped
    }
  }

  // TODO: I don't think I'm enforcing that properties can't start with "org.scalatest"
  // TODO: I don't think I'm handling rejecting multiple -f/-C with the same arg. -f fred.txt -f fred.txt should
  // fail, as should -C MyReporter -C MyReporter. I'm failing on -o -o, -g -g, and -e -e, but the error messages
  // could indeed be nicer.
  /**
   * Runs a suite of tests, with optional GUI. See the main documentation for this singleton object for the details.
   */
  def main(args: Array[String]): Unit = {
    // println("FOR DEBUGGING, THESE ARE THE ARGS PASSED TO main(): " + args.mkString(" "))
    Thread.currentThread.setName("ScalaTest-main")
    val result = 
      if (args.contains("-v") || args.contains("--version")) {
        val version = org.scalatest.ScalaTestVersions.ScalaTestVersion
        val scalaVersion = org.scalatest.ScalaTestVersions.BuiltForScalaVersion
        println("ScalaTest " + version + " (Built for Scala " + scalaVersion + ")")
        runOptionallyWithPassFailReporter(args.filter(arg => arg != "-v" && arg != "--version"), true)
      }
      else
        runOptionallyWithPassFailReporter(args, true)

    if (result)
      System.exit(0)
    else
      System.exit(1)
  }

  /**
   * Runs a suite of tests, with optional GUI. See the main documentation for this singleton object for the details.
   * The difference between this method and `main` is simply that this method will block until the run
   * has completed, aborted, or been stopped, and return `true` if all tests executed and passed. In other
   * words, if any test fails, or if any suite aborts, or if the run aborts or is stopped, this method will
   * return `false`. This value is used, for example, by the ScalaTest ant task to determine whether
   * to continue the build if `haltOnFailure` is set to `true`.
   *
   * @return true if all tests were executed and passed.
   */
  def run(args: Array[String]): Boolean = {
    // println("FOR DEBUGGING, THESE ARE THE ARGS PASSED TO run(): " + args.mkString(" "))
    val originalThreadName = Thread.currentThread.getName
    try {
      Thread.currentThread.setName("ScalaTest-run")
      runOptionallyWithPassFailReporter(args, true)
    }
    finally Thread.currentThread.setName(originalThreadName)
  }

  private def runOptionallyWithPassFailReporter(args: Array[String], runWithPassFailReporter: Boolean): Boolean = {

    checkArgsForValidity(args) match {
      case Some(s) => {
        println(s)
        System.exit(1) // TODO: Shouldn't this be returning false?
      }
      case None =>
    }

    val ParsedArgs(
      runpathArgs,
      reporterArgs,
      suiteArgs,
      againArgs,
      junitArgs,
      propertiesArgs,
      tagsToIncludeArgs,
      tagsToExcludeArgs,
      concurrentArgs,
      membersOnlyArgs,
      wildcardArgs,
      testNGArgs,
      suffixes, 
      chosenStyles, 
      spanScaleFactors, 
      testSortingReporterTimeouts,
      slowpokeArgs
    ) = parseArgs(args)

    val fullReporterConfigurations: ReporterConfigurations =
      if (reporterArgs.isEmpty)
        // If no reporters specified, just give them a graphic reporter
        new ReporterConfigurations(Some(GraphicReporterConfiguration(Set())), Nil, Nil, Nil, /*Nil, Nil, */None, None, Nil, Nil, Nil, Nil)
      else
        parseReporterArgsIntoConfigurations(reporterArgs)

    val (suitesList: List[SuiteParam], testSpecs: List[TestSpec]) =
      parseSuiteArgs(suiteArgs)
    val agains: List[String] = parseAgainArgs(againArgs)
    val junitsList: List[String] = parseSuiteArgsIntoNameStrings(junitArgs, "-j")
    val runpathList: List[String] = parseRunpathArgIntoList(runpathArgs)
    val propertiesMap: ConfigMap = parsePropertiesArgsIntoMap(propertiesArgs)
    val tagsToInclude: Set[String] = parseCompoundArgIntoSet(tagsToIncludeArgs, "-n")
    val tagsToExclude: Set[String] = parseCompoundArgIntoSet(tagsToExcludeArgs, "-l")
    val concurrent: Boolean = !concurrentArgs.isEmpty
    val concurrentConfig: ConcurrentConfig = parseConcurrentConfig(concurrentArgs)
    val membersOnlyList: List[String] = parseSuiteArgsIntoNameStrings(membersOnlyArgs, "-m")
    val wildcardList: List[String] = parseSuiteArgsIntoNameStrings(wildcardArgs, "-w")
    val testNGList: List[String] = parseSuiteArgsIntoNameStrings(testNGArgs, "-b")
    val chosenStyleSet: Set[String] = parseChosenStylesIntoChosenStyleSet(chosenStyles, "-y")
    val slowpokeConfig: Option[SlowpokeConfig] = parseSlowpokeConfig(slowpokeArgs)
    spanScaleFactor = parseDoubleArgument(spanScaleFactors, "-F", 1.0)
    testSortingReporterTimeout = Span(parseDoubleArgument(testSortingReporterTimeouts, "-T", 2.0), Seconds)

    // If there's a graphic reporter, we need to leave it out of
    // reporterSpecs, because we want to pass all reporterSpecs except
    // the graphic reporter's to the RunnerJFrame (because RunnerJFrame *is*
    // the graphic reporter).
    val reporterConfigs: ReporterConfigurations =
      fullReporterConfigurations.graphicReporterConfiguration match {
        case None => fullReporterConfigurations
        case Some(grs) => {
          new ReporterConfigurations(
            None,
            fullReporterConfigurations.fileReporterConfigurationList,
            fullReporterConfigurations.memoryReporterConfigurationList,
            fullReporterConfigurations.junitXmlReporterConfigurationList,
            //fullReporterConfigurations.dashboardReporterConfigurationList,
            //fullReporterConfigurations.xmlReporterConfigurationList,
            fullReporterConfigurations.standardOutReporterConfiguration,
            fullReporterConfigurations.standardErrReporterConfiguration,
            fullReporterConfigurations.htmlReporterConfigurationList,
            fullReporterConfigurations.customReporterConfigurationList, 
            fullReporterConfigurations.xmlSocketReporterConfigurationList, 
            fullReporterConfigurations.socketReporterConfigurationList
          )
        }
      }

    val passFailReporter = if (runWithPassFailReporter) Some(new PassFailReporter) else None

    if (propertiesMap.isDefinedAt(CHOSEN_STYLES))
      throw new IllegalArgumentException("Property name '" + CHOSEN_STYLES + "' is used by ScalaTest, please choose other property name.")
    val configMap: ConfigMap = 
      if (chosenStyleSet.isEmpty)
        propertiesMap
      else
        propertiesMap + (CHOSEN_STYLES -> chosenStyleSet)

    val (detectSlowpokes: Boolean, slowpokeDetectionDelay: Long, slowpokeDetectionPeriod: Long) =
      slowpokeConfig match {
        case Some(SlowpokeConfig(delayInMillis, periodInMillis)) => (true, delayInMillis, periodInMillis)
        case _ => (false, 60000L, 60000L)
      }
    fullReporterConfigurations.graphicReporterConfiguration match {
      case Some(GraphicReporterConfiguration(configSet)) => {
        val graphicEventsToPresent: Set[EventToPresent] = EventToPresent.allEventsToPresent filter
          (if (configSet.contains(FilterTestStarting)) {_ != PresentTestStarting} else etp => true) filter
          (if (configSet.contains(FilterTestSucceeded)) {_ != PresentTestSucceeded} else etp => true) filter
          (if (configSet.contains(FilterTestIgnored)) {_ != PresentTestIgnored} else etp => true) filter
          (if (configSet.contains(FilterTestPending)) {_ != PresentTestPending} else etp => true) filter
          (if (configSet.contains(FilterScopeOpened)) {_ != PresentScopeOpened} else etp => true) filter
          (if (configSet.contains(FilterScopeClosed)) {_ != PresentScopeClosed} else etp => true) filter
          (if (configSet.contains(FilterScopePending)) {_ != PresentScopePending} else etp => true) filter
          (if (configSet.contains(FilterSuiteStarting)) {_ != PresentSuiteStarting} else etp => true) filter
          (if (configSet.contains(FilterSuiteCompleted)) {_ != PresentSuiteCompleted} else etp => true) filter
          (if (configSet.contains(FilterInfoProvided)) {_ != PresentInfoProvided} else etp => true) filter 
          (if (configSet.contains(FilterMarkupProvided)) {_ != PresentMarkupProvided} else etp => true)

        val abq = new ArrayBlockingQueue[RunnerJFrame](1)
        usingEventDispatchThread {
          val rjf = new RunnerJFrame(
            graphicEventsToPresent,
            reporterConfigs,
            suitesList,
            agains,
            testSpecs,
            junitsList,
            runpathList,
            tagsToInclude,
            tagsToExclude,
            configMap,
            concurrent,
            membersOnlyList,
            wildcardList,
            testNGList,
            passFailReporter,
            concurrentConfig,
            suffixes,
            chosenStyleSet,
            detectSlowpokes,
            slowpokeDetectionDelay,
            slowpokeDetectionPeriod
          )
          rjf.setLocation(RUNNER_JFRAME_START_X, RUNNER_JFRAME_START_Y)
          rjf.setVisible(true)
          rjf.prepUIForRunning()
          rjf.runFromGUI()
          abq.put(rjf)
        }
        // To get the Ant task to work, the main thread needs to block until
        // The GUI window exits.
        val rjf = abq.take()
        rjf.blockUntilWindowClosed()
      }
      case None => { // Run the test without a GUI
        withClassLoaderAndDispatchReporter(
          runpathList,
          reporterConfigs,
          None,
          passFailReporter,
          detectSlowpokes,
          slowpokeDetectionDelay,
          slowpokeDetectionPeriod
       ) { (loader, dispatchReporter) =>
          doRunRunRunDaDoRunRun(
            dispatchReporter,
            suitesList,
            agains,
            testSpecs,
            junitsList,
            Stopper.default,
            tagsToInclude,
            tagsToExclude,
            configMap,
            concurrent,
            membersOnlyList,
            wildcardList,
            testNGList,
            runpathList,
            loader,
            new RunDoneListener {},
            1,
            concurrentConfig,
            suffixes,
            chosenStyleSet
          )
        }
      }
    }
    
    passFailReporter match {
      case Some(pfr) => pfr.allTestsPassed
      case None => false
    }
  }

  // For debugging.
/*
  private[scalatest] def printOpts(opt: EventToPresent.Set32) {
    if (opt.contains(EventToPresent.PresentRunStarting))
      println("PresentRunStarting")
    if (opt.contains(EventToPresent.PresentTestStarting))
      println("PresentTestStarting")
    if (opt.contains(EventToPresent.PresentTestSucceeded))
      println("PresentTestSucceeded")
    if (opt.contains(EventToPresent.PresentTestFailed))
      println("PresentTestFailed")
    if (opt.contains(EventToPresent.PresentTestIgnored))
      println("PresentTestIgnored")
    if (opt.contains(EventToPresent.PresentSuiteStarting))
      println("PresentSuiteStarting")
    if (opt.contains(EventToPresent.PresentSuiteCompleted))
      println("PresentSuiteCompleted")
    if (opt.contains(EventToPresent.PresentSuiteAborted))
      println("PresentSuiteAborted")
    if (opt.contains(EventToPresent.PresentInfoProvided))
      println("PresentInfoProvided")
    if (opt.contains(EventToPresent.PresentRunStopped))
      println("PresentRunStopped")
    if (opt.contains(EventToPresent.PresentRunCompleted))
      println("PresentRunCompleted")
    if (opt.contains(EventToPresent.PresentRunAborted))
      println("PresentRunAborted")
  }
*/
  
  // We number our named threads so that people can keep track
  // of it as it goes through different suites. But in case the
  // multiple doRunRunRunDaDoRunRun's get called, we want to
  // use different numbers. So this is a "global" count in Runner.
  private val atomicThreadCounter = new AtomicInteger

  private[scalatest] def doRunRunRunDaDoRunRun(
    dispatch: DispatchReporter,
    suitesList: List[SuiteParam],
    agains: List[String],
    testSpecs: List[TestSpec],
    junitsList: List[String],
    stopper: Stopper,
    tagsToIncludeSet: Set[String], 
    tagsToExcludeSet: Set[String], 
    configMap: ConfigMap,
    concurrent: Boolean,
    membersOnlyList: List[String],
    wildcardList: List[String],
    testNGList: List[String],
    runpath: List[String],
    loader: ClassLoader,
    doneListener: RunDoneListener,
    runStamp: Int,
    concurrentConfig: ConcurrentConfig,
    suffixes: Option[Pattern],
    chosenStyleSet: Set[String]
  ): Unit = {

    // TODO: add more, and to RunnerThread too
    requireNonNull(dispatch,
                   suitesList,
                   agains,
                   testSpecs,
                   junitsList,
                   stopper,
                   tagsToIncludeSet,
                   tagsToExcludeSet,
                   configMap,
                   membersOnlyList,
                   wildcardList,
                   testNGList,
                   runpath,
                   loader,
                   doneListener,
                   chosenStyleSet)

    val (globSuites, nonGlobSuites) = suitesList.partition(_.isGlob)

    var tracker = new Tracker(new Ordinal(runStamp))

    //
    // Generates SuiteConfigs for Suites found via discovery.
    //
    def genDiscoSuites: List[SuiteConfig] = {
      val emptyDynaTags = DynaTags(Map.empty[String, Set[String]], Map.empty[String, Map[String, Set[String]]])

      //
      // If user specified any -t or -z arguments independent of
      // a Suite name, or any members-only or wildcard args, or
      // suite args with glob characters, then we need to do
      // discovery to find the Suites associated with those
      // args.
      //
      val discoArgsArePresent =
        !membersOnlyList.isEmpty || !wildcardList.isEmpty ||
        !testSpecs.isEmpty || !globSuites.isEmpty

      //
      // If user specified any specific Suites to run, then we don't
      // have to do any discovery unless discoArgsArePresent.
      //
      val suiteArgsArePresent =
        !nonGlobSuites.isEmpty || !junitsList.isEmpty || !testNGList.isEmpty ||
        !agains.isEmpty

      if (suiteArgsArePresent && !discoArgsArePresent) {
        Nil // No DiscoverySuites in this case. Just run Suites
            // named with -s or -j or -b
      }
      else {
        val discoveryStartTime = System.currentTimeMillis
        dispatch(DiscoveryStarting(tracker.nextOrdinal(), configMap))

        val accessibleSuites: Set[String] =
          discoverSuiteNames(runpath, loader, suffixes)

        val discoSuites =
          if (!discoArgsArePresent && !suiteArgsArePresent) {
            // In this case, they didn't specify any -w, -m, -s,
            // -j or -b on the command line, so the default is to
            // run any accessible Suites discovered on the runpath
            List(SuiteConfig(new DiscoverySuite("", accessibleSuites, true, loader), emptyDynaTags, false, false))
          }
          else {
            val membersOnlyInstances =
              for (membersOnlyName <- membersOnlyList)
                yield SuiteConfig(new DiscoverySuite(membersOnlyName, accessibleSuites, false, loader), emptyDynaTags, false, false)
  
            val wildcardInstances =
              for (wildcardName <- wildcardList)
                yield SuiteConfig(new DiscoverySuite(wildcardName, accessibleSuites, true, loader), emptyDynaTags, false, false)
  
            val testSpecSuiteParams =
              SuiteDiscoveryHelper.discoverTests(
                testSpecs, accessibleSuites, loader)
  
            val testSpecInstances = 
              for (suiteParam <- testSpecSuiteParams)
                yield genSuiteConfig(suiteParam, loader)
  
            val deglobbedSuiteParams: List[SuiteParam] =
              deglobSuiteParams(globSuites, accessibleSuites)
  
            val globInstances = 
              for (suiteParam <- deglobbedSuiteParams)
                yield genSuiteConfig(suiteParam, loader)
  
            membersOnlyInstances ::: wildcardInstances ::: testSpecInstances ::: globInstances
          }

        val discoveryDuration = System.currentTimeMillis - discoveryStartTime
        dispatch(
          DiscoveryCompleted(tracker.nextOrdinal(), Some(discoveryDuration)))

        discoSuites
      }
    }

    // suites specified by name, either directly via -s or in a file via -A
    val againSuites = readMemoryFiles(agains, dispatch, tracker)
    val specificSuites = nonGlobSuites ::: againSuites

    val runStartTime = System.currentTimeMillis
    
    try {
      val loadProblemsExist =
        try {
          val unrunnableList = specificSuites.filter{ suiteParam => 
            val className = suiteParam.className
            loader.loadClass(className) // Check if the class exist, so if not we get the nice cannot load suite error message.
            !isAccessibleSuite(className, loader) && !isRunnable(className, loader)
          }
          if (!unrunnableList.isEmpty) {
            val names = for (suiteParam <- unrunnableList) yield " " + suiteParam.className
            dispatch(RunAborted(tracker.nextOrdinal(), Resources.nonSuite + names.mkString(", "), None))
            true
          }
          else {
            false
          }
        }
        catch {
          case e: ClassNotFoundException => {
            dispatch(RunAborted(tracker.nextOrdinal(), Resources.cannotLoadSuite(e.getMessage), Some(e)))
            true
          }
        }

      if (!loadProblemsExist) {
        try {
          val namedSuiteInstances: List[SuiteConfig] =
            for (suiteParam <- specificSuites)
              yield genSuiteConfig(suiteParam, loader)
          
          val emptyDynaTags = DynaTags(Map.empty[String, Set[String]], Map.empty[String, Map[String, Set[String]]])

          val junitSuiteInstances: List[SuiteConfig] =
            for (junitClassName <- junitsList)
              yield SuiteConfig(new JUnitWrapperSuite(junitClassName, loader), emptyDynaTags, false, true) // JUnit suite should exclude nested suites

          val testNGWrapperSuiteList: List[SuiteConfig] =
            if (!testNGList.isEmpty)
              List(SuiteConfig(new TestNGWrapperSuite(testNGList), emptyDynaTags, false, true)) // TestNG suite should exclude nested suites
            else
              Nil

          val discoSuiteInstances = genDiscoSuites

          val suiteInstances: List[SuiteConfig] = namedSuiteInstances ::: junitSuiteInstances ::: discoSuiteInstances ::: testNGWrapperSuiteList

          val testCountList =
            for (suiteConfig <- suiteInstances)
              yield { 
              val tagsToInclude = if (suiteConfig.requireSelectedTag) tagsToIncludeSet ++ Set(SELECTED_TAG) else tagsToIncludeSet
              val filter = Filter(if (tagsToInclude.isEmpty) None else Some(tagsToInclude), tagsToExcludeSet, suiteConfig.excludeNestedSuites, suiteConfig.dynaTags)
              suiteConfig.suite.expectedTestCount(filter)
            }
  
          def sumInts(list: List[Int]): Int =
            list match {
              case Nil => 0
              case x :: xs => x + sumInts(xs)
            }

          val expectedTestCount = sumInts(testCountList)

          dispatch(RunStarting(tracker.nextOrdinal(), expectedTestCount, configMap))
          
          if (concurrent) {

            // Because some tests may do IO, will create a pool of 2 times the number of processors reported
            // by the Runtime's availableProcessors method.
            val poolSize =
              if (concurrentConfig.numThreads > 0) concurrentConfig.numThreads
              else Runtime.getRuntime.availableProcessors * 2

            val distributedSuiteSorter = 
              if (concurrentConfig.enableSuiteSortingReporter)
                Some(new SuiteSortingReporter(dispatch, Span(testSortingReporterTimeout.millisPart + 1000, Millis), System.err))
              else
                None
              
            val concurrentDispatch = 
              distributedSuiteSorter match {
                case Some(dss) => dss
                case None => dispatch
              }
                
            val threadFactory =
              new ThreadFactory {
                val defaultThreadFactory = Executors.defaultThreadFactory
                def newThread(runnable: Runnable): Thread = {
                  val thread = defaultThreadFactory.newThread(runnable)
                  thread.setName("ScalaTest-" + atomicThreadCounter.incrementAndGet())
                  thread
                }
              }
            val execSvc: ExecutorService = Executors.newFixedThreadPool(poolSize, threadFactory)
            try {

              val distributor = new ConcurrentDistributor(Args(dispatch, stopper, Filter(if (tagsToIncludeSet.isEmpty) None else Some(tagsToIncludeSet), tagsToExcludeSet), configMap, None, tracker, chosenStyleSet), execSvc)
              if (System.getProperty("org.scalatest.tools.Runner.forever", "false") == "true") {

                while (true) {
                  val statuses = for (suiteConfig <- suiteInstances) yield {
                    val tagsToInclude = if (suiteConfig.requireSelectedTag) tagsToIncludeSet ++ Set(SELECTED_TAG) else tagsToIncludeSet
                    val filter = Filter(if (tagsToInclude.isEmpty) None else Some(tagsToInclude), tagsToExcludeSet, suiteConfig.excludeNestedSuites, suiteConfig.dynaTags)
                    val runArgs = Args(concurrentDispatch, stopper, filter, configMap, Some(distributor), tracker.nextTracker, chosenStyleSet, false, None, distributedSuiteSorter)
                    distributor.apply(suiteConfig.suite, runArgs)
                  }
                  distributor.waitUntilDone()
                  (new CompositeStatus(statuses.toSet)).waitUntilCompleted()
                }
              }
              else {
                val statuses = for (suiteConfig <- suiteInstances) yield {
                  val tagsToInclude = if (suiteConfig.requireSelectedTag) tagsToIncludeSet ++ Set(SELECTED_TAG) else tagsToIncludeSet
                  val filter = Filter(if (tagsToInclude.isEmpty) None else Some(tagsToInclude), tagsToExcludeSet, suiteConfig.excludeNestedSuites, suiteConfig.dynaTags)
                  val runArgs = Args(concurrentDispatch, stopper, filter, configMap, Some(distributor), tracker.nextTracker, chosenStyleSet, false, None, distributedSuiteSorter)
                  distributor.apply(suiteConfig.suite, runArgs)
                }
                distributor.waitUntilDone()
                (new CompositeStatus(statuses.toSet)).waitUntilCompleted()
              }
            }
            finally {
              execSvc.shutdown()
            }
          }
          else {
            for (suiteConfig <- suiteInstances) {
              val tagsToInclude = if (suiteConfig.requireSelectedTag) tagsToIncludeSet ++ Set(SELECTED_TAG) else tagsToIncludeSet
              val filter = Filter(if (tagsToInclude.isEmpty) None else Some(tagsToInclude), tagsToExcludeSet, suiteConfig.excludeNestedSuites, suiteConfig.dynaTags)
              val runArgs = Args(dispatch, stopper, filter, configMap, None, tracker, chosenStyleSet)
              val status = new ScalaTestStatefulStatus()
              val suiteRunner = new SuiteRunner(suiteConfig.suite, runArgs, status)
              suiteRunner.run()
              status.waitUntilCompleted()
            }
          }

          val duration = System.currentTimeMillis - runStartTime
          if (stopper.stopRequested) {
            dispatch(RunStopped(tracker.nextOrdinal(), Some(duration)))
          }
          else {
            dispatch(RunCompleted(tracker.nextOrdinal(), Some(duration)))
          }
        }
        catch {
          case e: InstantiationException =>
            dispatch(RunAborted(tracker.nextOrdinal(), Resources.cannotInstantiateSuite(e.getMessage), Some(e), Some(System.currentTimeMillis - runStartTime)))
          case e: IllegalAccessException =>
            dispatch(RunAborted(tracker.nextOrdinal(), Resources.cannotInstantiateSuite(e.getMessage), Some(e), Some(System.currentTimeMillis - runStartTime)))
          case e: NoClassDefFoundError =>
            dispatch(RunAborted(tracker.nextOrdinal(), Resources.cannotLoadClass(e.getMessage), Some(e), Some(System.currentTimeMillis - runStartTime)))
          case e: Throwable =>
            dispatch(RunAborted(tracker.nextOrdinal(), Resources.bigProblems(e), Some(e), Some(System.currentTimeMillis - runStartTime)))
        }
      }
    }
    finally {
      dispatch.dispatchDisposeAndWaitUntilDone()
      doneListener.done()
    }
  }

  //
  // Creates a SuiteParam for each Suite found that matches one
  // of the patterns in a list of SuiteParams containing globs.
  //
  private[tools] def deglobSuiteParams(globsList: List[SuiteParam],
                                       accessibleSuites: Set[String]):
  List[SuiteParam] =
    for {
      suiteParam <- globsList
      name <- accessibleSuites
      if suiteParam.matches(name)
    }
    yield suiteParam.copy(className = name)

  //
  // Reads each specified file and generates a SuiteParam object
  // for each entry in file.  Files are of the format created when
  // -M option is specified to record failed/canceled/aborted
  // tests so they can be run again later.
  //
  private[tools] def readMemoryFiles(fileNames: List[String],
                                     reporter: Reporter,
                                     tracker: Tracker):
  List[SuiteParam] =
  {
    val mementos =
      for {
        fileName <- fileNames
        memento <- Memento.readFromFile(fileName)
      }
      yield memento

    val (unrerunnables, rerunnables) = mementos.partition(_.className == None)

    for (memento <- unrerunnables)
      reporter.apply(
        AlertProvided(
          tracker.nextOrdinal,
          Resources.cannotRerun(memento.eventName, memento.suiteId,
                    memento.testName),
          None))

    rerunnables.map(_.toSuiteParam)
  }

  private[tools] def genSuiteConfig(suiteParam: SuiteParam, loader: ClassLoader): SuiteConfig = {
    val suiteClassName = suiteParam.className
    val clazz = loader.loadClass(suiteClassName)
    val wrapWithAnnotation = clazz.getAnnotation(classOf[WrapWith])
    val suiteInstance = 
      if (wrapWithAnnotation == null) 
        clazz.newInstance.asInstanceOf[Suite]
      else {
        val suiteClazz = wrapWithAnnotation.value
        val constructorList = suiteClazz.getDeclaredConstructors()
        val constructor = constructorList.find { c => 
          val types = c.getParameterTypes
          types.length == 1 && types(0) == classOf[java.lang.Class[_]]
        }
        constructor.get.newInstance(clazz).asInstanceOf[Suite]
      }
    
    if (suiteParam.testNames.length == 0 && suiteParam.wildcardTestNames.length == 0 && suiteParam.nestedSuites.length == 0)
      SuiteConfig(suiteInstance, new DynaTags(Map.empty, Map.empty), false, false) // -s suiteClass, no dynamic tagging required.
    else {
      val nestedSuites = suiteParam.nestedSuites
      
      val (selectSuiteList, selectTestList) = nestedSuites.partition(ns => ns.testNames.length == 0 || ns.wildcardTestNames.length == 0)
      val suiteDynaTags: Map[String, Set[String]] = Map() ++ selectSuiteList.map(ns => (ns.suiteId -> Set(SELECTED_TAG)))
      
      val suiteExactTestDynaTags: Map[String, Map[String, Set[String]]] = 
        if (suiteParam.testNames.length > 0) 
          Map(suiteInstance.suiteId -> (Map() ++ suiteParam.testNames.map(tn => (tn -> Set(SELECTED_TAG)))))
        else 
          Map.empty
      
      val suiteWildcardTestDynaTags: Map[String, Map[String, Set[String]]] = 
        if (suiteParam.wildcardTestNames.length > 0) {
          val wildcardTestNames = suiteParam.wildcardTestNames
          val allTestNames = suiteInstance.testNames
          val wildcardTestTags = Map() ++ allTestNames.filter(tn => wildcardTestNames.find(wc => tn.contains(wc)).isDefined)
                                              .map(tn => (tn -> Set(SELECTED_TAG)))
          Map(suiteInstance.suiteId -> wildcardTestTags)
        }
        else
          Map.empty
          
      def getNestedSuiteSelectedTestNames(nestedSuite: NestedSuiteParam): Array[String] = {
        if (nestedSuite.wildcardTestNames.length == 0)
          nestedSuite.testNames
        else {
          val wildcardTestNames = nestedSuite.wildcardTestNames
          val allTestNames = suiteInstance.testNames
          nestedSuite.testNames ++ allTestNames.filter(tn => wildcardTestNames.find(wc => tn.contains(wc)).isDefined)
        }
      }
      
      val nestedSuitesTestDynaTags: Map[String, Map[String, Set[String]]] 
        = Map() ++ selectTestList.map(ns => (ns.suiteId -> (Map() ++ getNestedSuiteSelectedTestNames(ns).map(tn => (tn, Set(SELECTED_TAG))))))
        
      val testDynaTags = mergeMap[String, Map[String, Set[String]]](List(suiteExactTestDynaTags, suiteWildcardTestDynaTags, nestedSuitesTestDynaTags)) { (suiteTestMap1, suiteTestMap2) => 
                           mergeMap[String, Set[String]](List(suiteTestMap1, suiteTestMap2)) { (tagSet1, tagSet2) =>
                             tagSet1 ++ tagSet2
                           }
                         }
      // Only exclude nested suites when using -s XXX -t XXXX, or -s XXX -z XXX
      val excludeNestedSuites = suiteParam.testNames.length > 0 && nestedSuites.length == 0
      SuiteConfig(suiteInstance, new DynaTags(suiteDynaTags, testDynaTags), true, excludeNestedSuites)
    }
  }

  private[scalatest] def excludesWithIgnore(excludes: Set[String]) = excludes + "org.scalatest.Ignore"

  private[scalatest] def withClassLoaderAndDispatchReporter(
    runpathList: List[String],
    reporterSpecs: ReporterConfigurations,
    graphicReporter: Option[Reporter],
    passFailReporter: Option[Reporter],
    detectSlowpokes: Boolean,
    slowpokeDetectionDelay: Long,
    slowpokeDetectionPeriod: Long
  )(f: (ClassLoader, DispatchReporter) => Unit): Unit = {

    val loader: ClassLoader = getRunpathClassLoader(runpathList)
    try {
      Thread.currentThread.setContextClassLoader(loader)
      try {
        val dispatchReporter = ReporterFactory.getDispatchReporter(reporterSpecs, graphicReporter, passFailReporter, loader, None, detectSlowpokes, slowpokeDetectionDelay, slowpokeDetectionPeriod)
        try {
          f(loader, dispatchReporter)
        }
        finally {
          dispatchReporter.dispatchDisposeAndWaitUntilDone()
        }
      }
      catch {
        // getDispatchReporter may complete abruptly with an exception, if there is an problem trying to load
        // or instantiate a custom reporter class.
        case ex: Throwable => {
          System.err.println(Resources.bigProblemsMaybeCustomReporter)
          ex.printStackTrace(System.err)
        }
      }
    }
    finally {
      // eventually call close on the RunpathClassLoader
    }
  }

  private[scalatest] def getRunpathClassLoader(runpathList: List[String]): ClassLoader = {

    requireNonNull(runpathList)
    if (runpathList.isEmpty) {
      classOf[Suite].getClassLoader // Could this be null technically?
    }
    else {
      val urlsList: List[URL] =
        for (raw <- runpathList) yield {
          try {
            new URL(raw)
          }
          catch {
            case murle: MalformedURLException => {
  
              // Assume they tried to just pass in a file name
              val file: File = new File(raw)
  
              // file.toURL may throw MalformedURLException too, but for now
              // just let that propagate up.
              file.toURI.toURL // If a dir, comes back terminated by a slash
            }
          }
        }
  
      // Here is where the Jini preferred class loader stuff went.

      // Tell the URLConnections to not use caching, so that repeated runs and reruns actually work
      // on the latest binaries.
      for (url <- urlsList) {
        try {
          url.openConnection.setDefaultUseCaches(false)
        }
        catch {
          case e: IOException => // just ignore these
        }
      }

      new URLClassLoader(urlsList.toArray, classOf[Suite].getClassLoader)
    }
  }

  private[scalatest] def usingEventDispatchThread(f: => Unit): Unit = {
    SwingUtilities.invokeLater(
      new Runnable() {
        def run(): Unit = {
          f
        }
      }
    )
  }
}

