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
package org.scalatest.concurrent

import org.scalatest._
import PatienceConfiguration._
import PimpedThreadGroup._
import org.scalactic.source
import _root_.java.util.concurrent.atomic.AtomicReference
import _root_.java.util.concurrent.{CopyOnWriteArrayList, ArrayBlockingQueue}
import org.scalatest.exceptions.NotAllowedException
import time.{Seconds, Millis, Span}

/**
 * Trait whose `Conductor` member facilitates the testing of classes, traits, and libraries designed
 * to be used by multiple threads concurrently.
 *
 * A `Conductor` conducts a multi-threaded scenario by maintaining
 * a clock of "beats." Beats are numbered starting with 0. You can ask a
 * `Conductor` to run threads that interact with the class, trait,
 * or library (the ''subject'')
 * you want to test. A thread can call the `Conductor`'s
 * `waitForBeat` method, which will cause the thread to block
 * until that beat has been reached. The `Conductor` will advance
 * the beat only when all threads participating in the test are blocked. By
 * tying the timing of thread activities to specific beats, you can write
 * tests for concurrent systems that have deterministic interleavings of
 * threads.
 * 
 *
 * A `Conductor` object has a three-phase lifecycle. It begins its life
 * in the ''setup'' phase. During this phase, you can start threads by
 * invoking the `thread` method on the `Conductor`.
 * When `conduct` is invoked on a `Conductor`, it enters
 * the ''conducting'' phase. During this phase it conducts the one multi-threaded
 * scenario it was designed to conduct. After all participating threads have exited, either by
 * returning normally or throwing an exception, the `conduct` method
 * will complete, either by returning normally or throwing an exception. As soon as
 * the `conduct` method completes, the `Conductor`
 * enters its ''defunct'' phase. Once the `Conductor` has conducted
 * a multi-threaded scenario, it is defunct and can't be reused. To run the same test again,
 * you'll need to create a new instance of `Conductor`.
 * 
 *
 * Here's an example of the use of `Conductor` to test the `ArrayBlockingQueue`
 * class from `java.util.concurrent`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * import org.scalatest.fixture.FunSuite
 * import org.scalatest.matchers.Matchers
 * import java.util.concurrent.ArrayBlockingQueue
 * import org.scalatest.concurrent.Conductors
 *
 * class ArrayBlockingQueueSuite extends FunSuite with Matchers with Conductors {
 *
 *   test("calling put on a full queue blocks the producer thread") {
 *
 *     val conductor = new Conductor
 *     import conductor._
 *
 *     val buf = new ArrayBlockingQueue[Int](1)
 *
 *     thread("producer") {
 *       buf put 42
 *       buf put 17
 *       beat should be (1)
 *     }
 *
 *     thread("consumer") {
 *       waitForBeat(1)
 *       buf.take should be (42)
 *       buf.take should be (17)
 *     }
 *
 *     whenFinished {
 *       buf should be ('empty)
 *     }
 *   }
 * }
 * }}}
 *
 * When the test shown is run, it will create one thread named ''producer'' and another named
 * ''consumer''. The producer thread will eventually execute the code passed as a by-name
 * parameter to `thread("producer")`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * buf put 42
 * buf put 17
 * beat should be (1)
 * }}}
 *
 * Similarly, the consumer thread will eventually execute the code passed as a by-name parameter
 * to `thread("consumer")`:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * waitForBeat(1)
 * buf.take should be (42)
 * buf.take should be (17)
 * }}}
 *
 * The `thread` creates the threads and starts them, but they will not immediately
 * execute the by-name parameter passed to them. They will first block, waiting for the `Conductor`
 * to give them a green light to proceed.
 * 
 *
 * The next call in the test is `whenFinished`. This method will first call `conduct` on
 * the `Conductor`, which will wait until all threads that were created (in this case, producer and consumer) are
 * at the "starting line", ''i.e.'', they have all started and are blocked, waiting on the green light.
 * The `conduct` method will then give these threads the green light and they will
 * all start executing their blocks concurrently.
 * 
 *
 * When the threads are given the green light, the beat is 0. The first thing the producer thread does is put 42 in
 * into the queue. As the queue is empty at this point, this succeeds. The producer thread next attempts to put a 17
 * into the queue, but because the queue has size 1, this can't succeed until the consumer thread has read the 42
 * from the queue. This hasn't happened yet, so producer blocks. Meanwhile, the consumer thread's first act is to
 * call `waitForBeat(1)`. Because the beat starts out at 0, this call will block the consumer thread.
 * As a result, once the producer thread has executed `buf put 17` and the consumer thread has executed
 * `waitForBeat(1)`, both threads will be blocked.
 * 
 *
 * The `Conductor` maintains a clock that wakes up periodically and checks to see if all threads
 * participating in the multi-threaded scenario (in this case, producer and consumer) are blocked. If so, it
 * increments the beat. Thus sometime later the beat will be incremented, from 0 to 1. Because consumer was
 * waiting for beat 1, it will wake up (''i.e.'', the `waitForBeat(1)` call will return) and
 * execute the next line of code in its block, `buf.take should be (42)`. This will succeed, because
 * the producer thread had previously (during beat 0) put 42 into the queue. This act will also make
 * producer runnable again, because it was blocked on the second `put`, which was waiting for another
 * thread to read that 42.
 * 
 *
 * Now both threads are unblocked and able to execute their next statement. The order is
 * non-deterministic, and can even be simultaneous if running on multiple cores. If the `consumer` thread
 * happens to execute `buf.take should be (17)` first, it will block (`buf.take` will not return), because the queue is
 * at that point empty. At some point later, the producer thread will execute `buf put 17`, which will
 * unblock the consumer thread. Again both threads will be runnable and the order non-deterministic and
 * possibly simulataneous. The producer thread may charge ahead and run its next statement, `beat should be (1)`.
 * This will succeed because the beat is indeed 1 at this point. As this is the last statement in the producer's block,
 * the producer thread will exit normally (it won't throw an exception). At some point later the consumer thread will
 * be allowed to complete its last statement, the `buf.take` call will return 17. The consumer thread will
 * execute `17 should be (17)`. This will succeed and as this was the last statement in its block, the consumer will return
 * normally.
 * 
 *
 * If either the producer or consumer thread had completed abruptbly with an exception, the `conduct` method
 * (which was called by `whenFinished`) would have completed abruptly with an exception to indicate the test
 * failed. However, since both threads returned normally, `conduct` will return. Because `conduct` doesn't
 * throw an exception, `whenFinished` will execute the block of code passed as a by-name parameter to it: `buf should be ('empty)`.
 * This will succeed, because the queue is indeed empty at this point. The `whenFinished` method will then return, and
 * because the `whenFinished` call was the last statement in the test and it didn't throw an exception, the test completes successfully.
 * 
 *
 * This test tests `ArrayBlockingQueue`, to make sure it works as expected. If there were a bug in `ArrayBlockingQueue`
 * such as a `put` called on a full queue didn't block, but instead overwrote the previous value, this test would detect
 * it. However, if there were a bug in `ArrayBlockingQueue` such that a call to `take` called on an empty queue
 * never blocked and always returned 0, this test might not detect it. The reason is that whether the consumer thread will ever call
 * `take` on an empty queue during this test is non-deterministic. It depends on how the threads get scheduled during beat 1.
 * What is deterministic in this test, because the consumer thread blocks during beat 0, is that the producer thread will definitely
 * attempt to write to a full queue. To make sure the other scenario is tested, you'd need a different test:
 * 
 *
 * {{{  <!-- class="stHighlight" -->
 * test("calling take on an empty queue blocks the consumer thread") {
 *
 *   val conductor = new Conductor
 *   import conductor._
 *
 *   val buf = new ArrayBlockingQueue[Int](1)
 *
 *   thread("producer") {
 *     waitForBeat(1)
 *     buf put 42
 *     buf put 17
 *   }
 *
 *   thread("consumer") {
 *     buf.take should be (42)
 *     buf.take should be (17)
 *     beat should be (1)
 *   }
 *
 *   whenFinished {
 *     buf should be ('empty)
 *   }
 * }
 * }}}
 *
 * In this test, the producer thread will block, waiting for beat 1. The consumer thread will invoke `buf.take`
 * as its first act. This will block, because the queue is empty. Because both threads are blocked, the `Conductor`
 * will at some point later increment the beat to 1. This will awaken the producer thread. It will return from its
 * `waitForBeat(1)` call and execute `buf put 42`. This will unblock the consumer thread, which will
 * take the 42, and so on.
 * 
 *
 * The problem that `Conductor` is designed to address is the difficulty, caused by the non-deterministic nature
 * of thread scheduling, of testing classes, traits, and libraries that are intended to be used by multiple threads.
 * If you just create a test in which one thread reads from an `ArrayBlockingQueue` and
 * another writes to it, you can't be sure that you have tested all possible interleavings of threads, no matter
 * how many times you run the test. The purpose of `Conductor`
 * is to enable you to write tests with deterministic interleavings of threads. If you write one test for each possible
 * interleaving of threads, then you can be sure you have all the scenarios tested. The two tests shown here, for example,
 * ensure that both the scenario in which a producer thread tries to write to a full queue and the scenario in which a
 * consumer thread tries to take from an empty queue are tested.
 * 
 *
 * Class `Conductor` was inspired by the
 * <a href="http://www.cs.umd.edu/projects/PL/multithreadedtc/">MultithreadedTC project</a>,
 * created by Bill Pugh and Nat Ayewah of the University of Maryland.
 * 
 *
 * Although useful, bear in mind that a `Conductor`'s results are not guaranteed to be
 * accurate 100% of the time. The reason is that it uses `java.lang.Thread`'s `getState` method to
 * decide when to advance the beat. This use goes against the advice given in the Javadoc documentation for
 * `getState`, which says, "This method is designed for use in monitoring of the system state, not for
 * synchronization." In short, sometimes the return value of `getState` occasionally may be inacurrate,
 * which in turn means that sometimes a `Conductor` could decide to advance the beat too early. In practice,
 * `Conductor` has proven to be very helpful when developing thread safe classes. It is also useful in
 * for regression tests, but you may have to tolerate occasional false negatives.
 * 
 *
 * @author Josh Cough
 * @author Bill Venners
 */
trait Conductors extends PatienceConfiguration {

  /**
   * Class that facilitates the testing of classes, traits, and libraries designed
   * to be used by multiple threads concurrently.
   *
   * A `Conductor` conducts a multi-threaded scenario by maintaining
   * a clock of "beats." Beats are numbered starting with 0. You can ask a
   * `Conductor` to run threads that interact with the class, trait,
   * or library (the ''subject'')
   * you want to test. A thread can call the `Conductor`'s
   * `waitForBeat` method, which will cause the thread to block
   * until that beat has been reached. The `Conductor` will advance
   * the beat only when all threads participating in the test are blocked. By
   * tying the timing of thread activities to specific beats, you can write
   * tests for concurrent systems that have deterministic interleavings of
   * threads.
   * 
   *
   * A `Conductor` object has a three-phase lifecycle. It begins its life
   * in the ''setup'' phase. During this phase, you can start threads by
   * invoking the `thread` method on the `Conductor`.
   * When `conduct` is invoked on a `Conductor`, it enters
   * the ''conducting'' phase. During this phase it conducts the one multi-threaded
   * scenario it was designed to conduct. After all participating threads have exited, either by
   * returning normally or throwing an exception, the `conduct` method
   * will complete, either by returning normally or throwing an exception. As soon as
   * the `conduct` method completes, the `Conductor`
   * enters its ''defunct'' phase. Once the `Conductor` has conducted
   * a multi-threaded scenario, it is defunct and can't be reused. To run the same test again,
   * you'll need to create a new instance of `Conductor`.
   * 
   *
   * Here's an example of the use of `Conductor` to test the `ArrayBlockingQueue`
   * class from `java.util.concurrent`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * import org.scalatest.fixture.FunSuite
   * import org.scalatest.matchers.Matchers
   * import java.util.concurrent.ArrayBlockingQueue
   * import org.scalatest.concurrent.Conductors
   *
   * class ArrayBlockingQueueSuite extends FunSuite with Matchers with Conductors {
   *
   *   test("calling put on a full queue blocks the producer thread") {
   *
   *     val conductor = new Conductor
   *     import conductor._
   *
   *     val buf = new ArrayBlockingQueue[Int](1)
   *
   *     thread("producer") {
   *       buf put 42
   *       buf put 17
   *       beat should be (1)
   *     }
   *
   *     thread("consumer") {
   *       waitForBeat(1)
   *       buf.take should be (42)
   *       buf.take should be (17)
   *     }
   *
   *     whenFinished {
   *       buf should be ('empty)
   *     }
   *   }
   * }
   * }}}
   *
   * When the test shown is run, it will create one thread named ''producer'' and another named
   * ''consumer''. The producer thread will eventually execute the code passed as a by-name
   * parameter to `thread("producer")`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * buf put 42
   * buf put 17
   * beat should be (1)
   * }}}
   *
   * Similarly, the consumer thread will eventually execute the code passed as a by-name parameter
   * to `thread("consumer")`:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * waitForBeat(1)
   * buf.take should be (42)
   * buf.take should be (17)
   * }}}
   *
   * The `thread` calls create the threads and starts them, but they will not immediately
   * execute the by-name parameter passed to them. They will first block, waiting for the `Conductor`
   * to give them a green light to proceed.
   * 
   *
   * The next call in the test is `whenFinished`. This method will first call `conduct` on
   * the `Conductor`, which will wait until all threads that were created (in this case, producer and consumer) are
   * at the "starting line", ''i.e.'', they have all started and are blocked, waiting on the green light.
   * The `conduct` method will then give these threads the green light and they will
   * all start executing their blocks concurrently.
   * 
   *
   * When the threads are given the green light, the beat is 0. The first thing the producer thread does is put 42 in
   * into the queue. As the queue is empty at this point, this succeeds. The producer thread next attempts to put a 17
   * into the queue, but because the queue has size 1, this can't succeed until the consumer thread has read the 42
   * from the queue. This hasn't happened yet, so producer blocks. Meanwhile, the consumer thread's first act is to
   * call `waitForBeat(1)`. Because the beat starts out at 0, this call will block the consumer thread.
   * As a result, once the producer thread has executed `buf put 17` and the consumer thread has executed
   * `waitForBeat(1)`, both threads will be blocked.
   * 
   *
   * The `Conductor` maintains a clock that wakes up periodically and checks to see if all threads
   * participating in the multi-threaded scenario (in this case, producer and consumer) are blocked. If so, it
   * increments the beat. Thus sometime later the beat will be incremented, from 0 to 1. Because consumer was
   * waiting for beat 1, it will wake up (''i.e.'', the `waitForBeat(1)` call will return) and
   * execute the next line of code in its block, `buf.take should be (42)`. This will succeed, because
   * the producer thread had previously (during beat 0) put 42 into the queue. This act will also make
   * producer runnable again, because it was blocked on the second `put`, which was waiting for another
   * thread to read that 42.
   * 
   *
   * Now both threads are unblocked and able to execute their next statement. The order is
   * non-deterministic, and can even be simultaneous if running on multiple cores. If the `consumer` thread
   * happens to execute `buf.take should be (17)` first, it will block (`buf.take` will not return), because the queue is
   * at that point empty. At some point later, the producer thread will execute `buf put 17`, which will
   * unblock the consumer thread. Again both threads will be runnable and the order non-deterministic and
   * possibly simulataneous. The producer thread may charge ahead and run its next statement, `beat should be (1)`.
   * This will succeed because the beat is indeed 1 at this point. As this is the last statement in the producer's block,
   * the producer thread will exit normally (it won't throw an exception). At some point later the consumer thread will
   * be allowed to complete its last statement, the `buf.take` call will return 17. The consumer thread will
   * execute `17 should be (17)`. This will succeed and as this was the last statement in its block, the consumer will return
   * normally.
   * 
   *
   * If either the producer or consumer thread had completed abruptbly with an exception, the `conduct` method
   * (which was called by `whenFinished`) would have completed abruptly with an exception to indicate the test
   * failed. However, since both threads returned normally, `conduct` will return. Because `conduct` doesn't
   * throw an exception, `whenFinished` will execute the block of code passed as a by-name parameter to it: `buf should be ('empty)`.
   * This will succeed, because the queue is indeed empty at this point. The `whenFinished` method will then return, and
   * because the `whenFinished` call was the last statement in the test and it didn't throw an exception, the test completes successfully.
   * 
   *
   * This test tests `ArrayBlockingQueue`, to make sure it works as expected. If there were a bug in `ArrayBlockingQueue`
   * such as a `put` called on a full queue didn't block, but instead overwrote the previous value, this test would detect
   * it. However, if there were a bug in `ArrayBlockingQueue` such that a call to `take` called on an empty queue
   * never blocked and always returned 0, this test might not detect it. The reason is that whether the consumer thread will ever call
   * `take` on an empty queue during this test is non-deterministic. It depends on how the threads get scheduled during beat 1.
   * What is deterministic in this test, because the consumer thread blocks during beat 0, is that the producer thread will definitely
   * attempt to write to a full queue. To make sure the other scenario is tested, you'd need a different test:
   * 
   *
   * {{{  <!-- class="stHighlight" -->
   * test("calling take on an empty queue blocks the consumer thread") {
   *
   *   val conductor = new Conductor
   *   import conductor._
   *
   *   val buf = new ArrayBlockingQueue[Int](1)
   *
   *   thread("producer") {
   *     waitForBeat(1)
   *     buf put 42
   *     buf put 17
   *   }
   *
   *   thread("consumer") {
   *     buf.take should be (42)
   *     buf.take should be (17)
   *     beat should be (1)
   *   }
   *
   *   whenFinished {
   *     buf should be ('empty)
   *   }
   * }
   * }}}
   *
   * In this test, the producer thread will block, waiting for beat 1. The consumer thread will invoke `buf.take`
   * as its first act. This will block, because the queue is empty. Because both threads are blocked, the `Conductor`
   * will at some point later increment the beat to 1. This will awaken the producer thread. It will return from its
   * `waitForBeat(1)` call and execute `buf put 42`. This will unblock the consumer thread, which will
   * take the 42, and so on.
   * 
   *
   * The problem that `Conductor` is designed to address is the difficulty, caused by the non-deterministic nature
   * of thread scheduling, of testing classes, traits, and libraries that are intended to be used by multiple threads.
   * If you just create a test in which one thread reads from an `ArrayBlockingQueue` and
   * another writes to it, you can't be sure that you have tested all possible interleavings of threads, no matter
   * how many times you run the test. The purpose of `Conductor`
   * is to enable you to write tests with deterministic interleavings of threads. If you write one test for each possible
   * interleaving of threads, then you can be sure you have all the scenarios tested. The two tests shown here, for example,
   * ensure that both the scenario in which a producer thread tries to write to a full queue and the scenario in which a
   * consumer thread tries to take from an empty queue are tested.
   * 
   *
   * Class `Conductor` was inspired by the
   * <a href="http://www.cs.umd.edu/projects/PL/multithreadedtc/">MultithreadedTC project</a>,
   * created by Bill Pugh and Nat Ayewah of the University of Maryland.
   * 
   *
   * Although useful, bear in mind that a `Conductor`'s results are not guaranteed to be
   * accurate 100% of the time. The reason is that it uses `java.lang.Thread`'s `getState` method to
   * decide when to advance the beat. This use goes against the advice given in the Javadoc documentation for
   * `getState`, which says, "This method is designed for use in monitoring of the system state, not for
   * synchronization." In short, sometimes the return value of `getState` occasionally may be inacurrate,
   * which in turn means that sometimes a `Conductor` could decide to advance the beat too early. In practice,
   * `Conductor` has proven to be very helpful when developing thread safe classes. It is also useful in
   * for regression tests, but you may have to tolerate occasional false negatives.
   * 
   *
   * @author Josh Cough
   * @author Bill Venners
   */
  final class Conductor {

    /**
     * The metronome used to coordinate between threads.
     * This clock is advanced by the clock thread.
     * The clock will not advance if it is frozen.
     */
    private final val clock = new Clock

    /////////////////////// thread management start //////////////////////////////

    // place all threads in a new thread group
    private final val threadGroup = new ThreadGroup("Orchestra")

    // all the threads in this test
    // This need not be volatile, because it is initialized with one object and
    // that stays forever. Because it is final, it
    private final val threads = new CopyOnWriteArrayList[Thread]()

    // Used to keep track of what names have been created so far, so that
    // it can be enforced that the names are unique.
    private final val threadNames = new CopyOnWriteArrayList[String]()

    // the main test thread
    private final val mainThread = Thread.currentThread

    /**
     * Creates a new thread that will execute the specified function.
     *
     * The name of the thread will be of the form Conductor-Thread-N, where N is some integer.
     * 
     *
     * This method may be safely called by any thread.
     * 
     *
     * @param fun the function to be executed by the newly created thread
     * @return the newly created thread
     */
    def thread(fun: => Any): Thread = threadNamed("Conductor-Thread-" + threads.size) { fun }

    /**
     * '''The overloaded thread method that takes a String name has been deprecated and will be removed in a future version of ScalaTest. Please use threadNamed instead.'''
     */
    @deprecated("The overloaded thread method that takes a String name has been deprecated and will be removed in a future version of ScalaTest. Please use threadNamed instead.")
    def thread(name: String)(fun: => Any)(implicit pos: source.Position): Thread = {
      threadNamed(name)(fun)
    }

    /**
     * Creates a new thread with the specified name that will execute the specified function.
     *
     * This method may be safely called by any thread.
     * 
     *
     * @param name the name of the newly created thread
     * @param fun the function to be executed by the newly created thread
     * @return the newly created thread
     */
    def threadNamed(name: String)(fun: => Any)(implicit pos: source.Position): Thread = {
      currentState.get match {
        case TestFinished =>
          throw new NotAllowedException(Resources.threadCalledAfterConductingHasCompleted, pos)
        case _ =>
          if (threadNames contains name)
            throw new NotAllowedException(Resources.cantRegisterThreadsWithSameName(name), pos)
          val t = TestThread(name, fun _)
          threads add t
          threadNames add name
          t.start()
          t
      }
    }

    // The reason that the thread is started immediately, is so that nested threads
    // will start immediately, without requiring the user to explicitly start() them.
    // Also, so that the thread method can return a Thread object.

    /*
    * A test thread runs the given function.
    * It only does so after it is given permission to do so by the main thread.
    * The main thread grants permission after it receives notication that
    * all test threads are ready to go.
    */
    private case class TestThread(name: String, f: () => Unit) extends Thread(threadGroup, name) {

      // Indicate a TestThread has been created that has not yet started running
      testThreadsStartingCounter.increment()

      override def run(): Unit = {
        try {
          // Indicate to the TestThreadsStartingCounter that one more thread is ready to go
          testThreadsStartingCounter.decrement()

          // wait for the main thread to say its ok to go.
          greenLightForTestThreads.await

          // go
          f()
        } catch {
          case t: Throwable =>
            if (firstExceptionThrown.isEmpty) {
              // The mainThread is likely joined to some test thread, so it needs to be awakened. If it
              // is joined to this thread, it will wake up shortly because this thread is about to die
              // by returning. If it is joined to a different thread, then it needs to be interrupted,
              // but this thread can't interrupt it, because then there's a race condition if it is
              // actually joined to this thread, between join returning because this thread returns
              // or join throwing an InterruptedException. So here just offer the throwable to
              // the firstExceptionThrown queue and return. Only the first will be accepted by the queue.
              // ThreadDeath exceptions that arise from being stopped will not go in because the queue
              // is already full. The clock thread checks the firestExceptionThrown queue each cycle, and
              // if it finds it is non-empty, it stops any live thread.
              firstExceptionThrown offer t
            }
        }
      }
    }

    /**
     * A BlockingQueue containing the first exception that occured
     * in test threads, or that was thrown by the clock thread.
     */
    private val firstExceptionThrown = new ArrayBlockingQueue[Throwable](1)

    // Won't write one that takes clockPeriod and timeout for 1.0. For now people
    // can just call conduct(a, b) directly followed by the code they want to run
    // afterwards. See if anyone asks for a whenFinished(a, b) {}
    /**
     * Invokes `conduct` and after `conduct` method returns,
     * if `conduct` returns normally (''i.e.'', without throwing
     * an exception), invokes the passed function.
     *
     * If `conduct` completes abruptly with an exception, this method
     * will complete abruptly with the same exception and not execute the passed
     * function.
     * 
     *
     * This method must be called by the thread that instantiated this `Conductor`,
     * and that same thread will invoke `conduct` and, if it returns noramlly, execute
     * the passed function.
     * 
     *
     * Because `whenFinished` invokes `conduct`, it can only be invoked
     * once on a `Conductor` instance. As a result, if you need to pass a block of
     * code to `whenFinished` it should be the last statement of your test. If you
     * don't have a block of code that needs to be run once all the threads have finished
     * successfully, then you can simply invoke `conduct` and never invoke
     * `whenFinished`.
     * 
     *
     * @param fun the function to execute after `conduct` call returns
     * @throws NotAllowedException if the calling thread is not the thread that
     *   instantiated this `Conductor`, or if `conduct` has already
     *    been invoked on this conductor.
     */
    def whenFinished(fun: => Assertion)(implicit pos: source.Position): Assertion = {

      if (Thread.currentThread != mainThread)
        throw new NotAllowedException(Resources.whenFinishedCanOnlyBeCalledByMainThread, pos)

      if (conductingHasBegun)
        throw new NotAllowedException(Resources.cannotInvokeWhenFinishedAfterConduct, pos)

      conduct()

      fun
    }

    /**
     * Blocks the current thread until the thread beat reaches the
     * specified value, at which point the current thread will be unblocked.
     *
     * @param beat the tick value to wait for
     * @throws NotAllowedException if the a `beat` less than or equal to zero is passed
     */
    def waitForBeat(beat: Int)(implicit pos: source.Position): Succeeded.type = {
      if (beat == 0)
        throw new NotAllowedException(Resources.cannotWaitForBeatZero, pos)
      if (beat < 0)
        throw new NotAllowedException(Resources.cannotWaitForNegativeBeat, pos)
      clock waitForBeat beat
    }

    /**
     * The current value of the thread clock.
     *
     * @return the current beat value
     */
    def beat: Int = clock.currentBeat

    /**
     * Executes the passed function with the `Conductor` ''frozen'' so that it
     * won't advance the clock.
     *
     * While the `Conductor` is frozen, the beat will not advance. Once the
     * passed function has completed executing, the `Conductor` will be unfrozen
     * so that the beat will advance when all threads are blocked, as normal.
     * 
     *
     * @param fun the function to execute while the `Conductor` is frozen.
     */
    def withConductorFrozen[T](fun: => T): T = { clock.withClockFrozen(fun) }

    /**
     * Indicates whether the conductor has been frozen.
     *
     * Note: The only way a thread
     * can freeze the conductor is by calling `withConductorFrozen`.
     * 
     */
    def isConductorFrozen: Boolean = clock.isFrozen

    private val testThreadsStartingCounter = new TestThreadsStartingCounter

    /**
     * Keeps the test threads from executing their bodies until the main thread
     * allows them to.
     */
    private val greenLightForTestThreads = new CountDownLatch(1)

    /**
     * Conducts a multi-threaded test using the configured maximum allowed time between beats
     * (the `timeout`) and the configured time to sleep between checks (the `interval`).
     *
     * @param config the `PatienceConfig` object containing the `timeout` and
     *          `interval` parameters used to configure the multi-threaded test
     */
    def conduct()(implicit config: PatienceConfig, pos: source.Position): Assertion = {
      conductImpl(config.timeout, config.interval, pos)
    }

    /**
     * Conducts a multi-threaded test using the configured maximum allowed time between beats
     * (the `timeout`) and the configured time to sleep between checks (the `interval`).
     *
     * The maximum amount of time allowed between successive beats is configured by the value contained in the passed
     * `timeout` parameter.
     * The interval to sleep between successive checks for progress is configured by the value contained in the passed
     * `interval` parameter.
     * 
     *
     * @param timeout the `Timeout` configuration parameter
     * @param interval the `Interval` configuration parameter
     */
    def conduct(timeout: Timeout, interval: Interval)(implicit pos: source.Position): Assertion = {
      conductImpl(timeout.value, interval. value, pos)
    }

    /**
     * Conducts a multi-threaded test using the configured maximum allowed time between beats
     * (the `timeout`) and the configured time to sleep between checks (the `interval`).
     *
     * The maximum amount of time allowed between successive beats is configured by the value contained in the passed
     * `timeout` parameter.
     * The interval to sleep between successive checks for progress is configured by  by the `interval` field of
     * the `PatienceConfig` passed implicitly as the last parameter.
     * 
     *
     * @param timeout the `Timeout` configuration parameter
     * @param config the `PatienceConfig` object containing the (unused) `timeout` and
     *          (used) `interval` parameters
     */
    def conduct(timeout: Timeout)(implicit config: PatienceConfig, pos: source.Position): Assertion = {
      conductImpl(timeout.value, config.interval, pos)
    }

    /**
     * Conducts a multi-threaded test using the configured maximum allowed time between beats
     * (the `timeout`) and the configured time to sleep between checks (the `interval`).
     *
     * The maximum amount of time allowed between successive beats is configured by the `timeout` field of
     * the `PatienceConfig` passed implicitly as the last parameter.
     * The interval to sleep between successive checks for progress is configured by the value contained in the passed
     * `interval` parameter.
     * 
     *
     * @param interval the `Interval` configuration parameter
     * @param config the `PatienceConfig` object containing the (used) `timeout` and
     *          (unused) `interval` parameters
     */
    def conduct(interval: Interval)(implicit config: PatienceConfig, pos: source.Position): Assertion = {
      conductImpl(config.timeout, interval.value, pos)
    }

    private val currentState: AtomicReference[ConductorState] = new AtomicReference(Setup)

    /**
     * Indicates whether either of the two overloaded `conduct` methods
     * have been invoked.
     *
     * This method returns true if either `conduct` method has been invoked. The
     * `conduct` method may have returned or not. (In other words, a `true`
     * result from this method does not mean the `conduct` method has returned,
     * just that it was already been invoked and,therefore, the multi-threaded scenario it
     * conducts has definitely begun.)
     * 
     */
    def conductingHasBegun: Boolean = currentState.get.testWasStarted

    private def conductImpl(timeout: Span, clockInterval: Span, pos: source.Position): Assertion = {

      // if the test was started already, explode
      // otherwise, change state to TestStarted
      if (conductingHasBegun)
        throw new NotAllowedException(Resources.cannotCallConductTwice, pos)
      else
        currentState set TestStarted

      // wait until all threads are definitely ready to go
      if (threads.size > 0)
        testThreadsStartingCounter.waitUntilAllTestThreadsHaveStarted()

      // release the latch, allowing all threads to start
      // wait for all the test threads to start before starting the clock
      greenLightForTestThreads.countDown()

      // start the clock thread
      val clockThread = ClockThread(timeout, clockInterval)
      clockThread.start()

      // wait until all threads have ended
      waitForThreads

      // change state to test finished
      currentState set TestFinished

      if (!firstExceptionThrown.isEmpty)
        throw firstExceptionThrown.peek
      else Succeeded
    }

    /**
     * Wait for all of the test case threads to complete, or for one
     * of the threads to throw an exception, or for the clock thread to
     * interrupt this (main) thread of execution. When the clock thread
     * or other threads fail, the error is placed in the shared error array
     * and thrown by this method.
     *
     * @param threads List of all the test case threads and the clock thread
     */
    // Explain how we understand it works: if the thread that's been joined already dies with an exception
    // that will go into errors, and this thread that called join will return. If the thread that's been joined returns and doesn't
    // die, that means all went well, and join will return and it can loop to the next one.
    // There should be no race condition between the last thread being waited on by join, it dies, join
    // returns, and after that the error gets into the errors. Because if you look in run() in the
    // thread inside createTestThread, the signaling error happens in a catch Throwable block before the thread
    // returns.
    private def waitForThreads: Succeeded.type = {
      var interrupted = false
      while(!interrupted && threadGroup.areAnyThreadsAlive) {
        threadGroup.getThreads.foreach { t =>
          if (!interrupted && t.isAlive && firstExceptionThrown.isEmpty)
            try {
              t.join()
            }
            catch {
              // main thread will be interrupted if a timeout occurs, deadlock is suspected,
              // or a test thread completes abruptly with an exception. Just loop here, because
              // firstExceptionThrown should be non-empty after InterruptedException is caught, and
              // if not, then I don't know how it got interrupted, but just keep looping.
              case e: InterruptedException =>
                interrupted = true
            }
        }
      }
      Succeeded
    }



    /**
     * A Clock manages the current beat in a Conductor.
     * Several duties stem from that responsibility.
     *
     * The clock will:
     *
     * <ol>
     * <li>Block a thread until the tick has reached a particular time.</li>
     * <li>Report the current time</li>
     * <li>Run operations with the clock frozen.</li>
     * </ol>
     */
    private class Clock {

      import java.util.concurrent.locks.ReentrantReadWriteLock
      import PimpedReadWriteLock._

      // clock starts at time 0
      private var currentTime = 0

      // methods in Clock that access or modify the private instance vars of this
      // Clock are synchronized on the object referenced from lock
      private val lock = new AnyRef

      /**
       * Read locks are acquired when clock is frozen and must be
       * released before the clock can advance in a advance(). (In a
       * ReentrantReadWriteLock, multiple threads can hold the read lock (and these
       * threads might read the value of currentTime (the currentBeat method), or just execute a
       * function with the clock frozen (the withClockFrozen method). The write lock
       * of a ReentrantReadWriteLock is exclusive, so only one can hold it, and it
       * can't be held if there are a thread or threads holding the read lock. This
       * is why the clock can't advance during a withClockFrozen, because the read
       * lock is grabbed before the function is executed in withClockFrozen, thus
       * advance will not be able to acquire the write lock to update currentTime
       * until after withClockFrozen has released the read lock (and there are no other
       * threads holding a read lock or the write lock).
       */
      private val rwLock = new ReentrantReadWriteLock

      private var highestBeatBeingWaitedOn = 0

      /**
       * Advance the current beat. In order to do so, the clock will wait
       * until it has become unfrozen.
       *
       * All threads waiting for the clock to advance (they would have been put in the lock
       * object's wait set by invoking the waitForBeat method) will be notified after the advance.
       *
       * Only the clock thread should be calling this.
       *
       * If the clock has been frozen by a thread, then that thread will own the readLock. Write
       * lock can only be acquired when there are no readers, so ticks won't progress while someone
       * has the clock frozen. Other methods also grab the read lock, like time (which gets
       * the current beat.)
       */
      def advance(): Succeeded.type = {
        lock.synchronized {
          rwLock.write {
            currentTime += 1
          }
          lock.notifyAll()
        }
        Succeeded
      }

      /**
       * The current beat.
       */
      def currentBeat: Int =
        lock.synchronized {
          rwLock read currentTime
        }

      /**
       * When wait for beat is called, the current thread will block until
       * the given beat is reached by the clock.
       */
      def waitForBeat(beat: Int): Succeeded.type = {
        lock.synchronized {
          if (beat > highestBeatBeingWaitedOn)
            highestBeatBeingWaitedOn = beat
          while (currentBeat < beat) {
            try {
              lock.wait()
            } catch {     // TODO: this is probably fine, but check JCIP about InterEx again
              case e: InterruptedException => throw new AssertionError(e)
            }         // Actually I"m not sure. Maybe should reset the interupted status
          }
        }
        Succeeded
      }

      // The reason there's no race condition between calling currentBeat in the while and calling
      // lock.wait() later (between that) and some other thread incrementing the beat and doing
      // a notify that this thread would miss (which it would want to know about if that's the
      // new time that it's waiting for) is because both this and the currentBeat method are synchronized
      // on the lock.

      /**
       * Returns true if any thread is waiting for a beat in the future (greater than the current beat)
       */
      def isAnyThreadWaitingForABeat: Boolean = {
        lock.synchronized { highestBeatBeingWaitedOn > currentTime }
      }

      /**
       * When the clock is frozen, it will not advance even when all threads
       * are blocked. Use this to block the current thread with a time limit,
       * but prevent the clock from advancing due to a waitForBeat(Int) in
       * another thread.
       */
      def withClockFrozen[T](fun: => T): T = rwLock read fun

      /**
       * Check if the clock has been frozen by any threads.
       */
      def isFrozen: Boolean = rwLock.getReadLockCount > 0
    }

    /**
     * The clock thread is the manager of the multi-threaded scenario.
     * Periodically checks all the test threads and regulates them.
     * If all the threads are blocked and at least one is waiting for a beat,
     * the clock advances to the next beat and all waiting threads are notified.
     * If none of the threads are waiting for a tick or in timed waiting,
     * a deadlock is detected. The clock thread times out if a thread is in runnable
     * or all are blocked and one is in timed waiting for longer than the runLimit.
     *
     * Algorithm in detail:
     *
     * While there are threads alive
     *
     *    If there are threads RUNNING
     *
     *       If they have been running too long
     *
     *          stop the test with a timeout error
     *
     *    else if there are threads waiting for a beat
     *
     *       advance the clock
     *
     *    else if there are threads in TIMED_WAITING
     *
     *       increment the deadlock counter
     *
     *       if the deadlock counter has reached a threshold
     *
     *          stop the test due to potential deadlock
     *
     *    sleep for one clockInterval
     *
     *
     * @param mainThread The main test thread. This thread will be waiting
     * for all the test threads to finish. It will be interrupted if the
     * ClockThread detects a deadlock or timeout.
     *
     * @param clockInterval The period between checks for the clock
     *
     * @param timeout The max time limit allowed between successive ticks of the clock
     */
    private case class ClockThread(timeout: Span, clockInterval: Span) extends Thread("Conductor-Clock") {

      // When a test thread throws an exception, the main thread will stop all the other threads,
      // but won't stop the clock thread. This is because the clock thread will simply return after
      // all the other threads have died. Thus the clock thread could last beyond the end of the
      // application, if the clock period was set high. Thus by making the clock thread a daemon
      // thread, it won't keep the application up just because it is still asleep and hasn't noticed
      // yet that all the test threads are gone.
      this setDaemon true

      // bv made lastProgress and deadlockCount volatile because ClockThread is instantiated by
      // a different thread that invokes run, so the thread that invokes run may not see the initial
      // values correctly unless it is volatile (unless safely published). So just in case.
      // used in detecting timeouts
      @volatile private var lastProgress = System.nanoTime

      // used in detecting deadlocks
      @volatile private var deadlockCount = 0
      private val MaxDeadlockDetectionsBeforeDeadlock = 50

      /**
       * Runs the steps described in the main documentation for class `ClockThread`.
       */
      override def run(): Unit = {

        // While there are threads that are not NEW or TERMINATED. (A thread is
        // NEW after it has been instantiated, but run() hasn't been called yet.)
        // So this means there are threads that are RUNNABLE, BLOCKED, WAITING, or
        // TIMED_WAITING. (BLOCKED is waiting for a lock. WAITING is in the wait set.)
        while (threadGroup.areAnyThreadsAlive) {
          if (!firstExceptionThrown.isEmpty) {
            // If any exception has been thrown, stop any live test thread.
            threadGroup.getThreads.foreach { t =>
              if (t.isAlive)
                t.stop()
            }
          }
          // If any threads are in the RUNNABLE state, just check to see if there's been
          // no progress for more than the timeout amount of time. If RUNNABLE threads
          // exist, but the timeout limit has not been reached, then just go
          // back to sleep.
          else if (threadGroup.areAnyThreadsRunning) {
            if (runningTooLong) stopDueToTimeout()
          }
          // No RUNNABLE threads, so if any threads are waiting for a beat, advance
          // the beat.
          else if (clock.isAnyThreadWaitingForABeat) {
            clock.advance()
            deadlockCount = 0
            lastProgress = System.nanoTime
          }
          else if (!threadGroup.areAnyThreadsInTimedWaiting) {
            // At this point, no threads are RUNNABLE, None
            // are waiting for a beat, and none are in TimedWaiting.
            // If this persists for MaxDeadlockDetectionsBeforeDeadlock,
            // go ahead and abort.
            detectDeadlock()
          }
          Thread.sleep(clockInterval.millisPart, clockInterval.nanosPart)
        }
      }

      /**
       * Threads have been running too long (timeout) if
       * The number of seconds since the last progress are more
       * than the allowed maximum run time.
       */
      private def runningTooLong: Boolean = System.nanoTime - lastProgress > timeout.totalNanos

      /**
       * Stop the test due to a timeout.
       */
      private def stopDueToTimeout(): Unit = {
        val errorMessage = Resources.testTimedOut(timeout.prettyString)
        // The mainThread is likely joined to some test thread, so wake it up. It will look and
        // notice that the firstExceptionThrown is no longer empty, and will stop all live test threads,
        // then rethrow the first exception thrown.
        firstExceptionThrown offer new RuntimeException(errorMessage)
        mainThread.interrupt()
      }

      /**
       * Determine if there is a deadlock and if so, stop the test.
       */
      private def detectDeadlock(): Unit = {
        // Should never get to >= before ==, but just playing it safe
        if (deadlockCount >= MaxDeadlockDetectionsBeforeDeadlock) {
          val errorMessage = Resources.suspectedDeadlock(MaxDeadlockDetectionsBeforeDeadlock.toString, (clockInterval scaledBy MaxDeadlockDetectionsBeforeDeadlock).prettyString)
          firstExceptionThrown offer new RuntimeException(errorMessage)

          // The mainThread is likely joined to some test thread, so wake it up. It will look and
          // notice that the firstExceptionThrown is no longer empty, and will stop all live test threads,
          // then rethrow the rirst exception thrown.
          mainThread.interrupt()
        }
        else deadlockCount += 1
      }
    }

    /**
     * Base class for the possible states of the Conductor.
     */
    private sealed abstract class ConductorState(val testWasStarted: Boolean, val testIsFinished: Boolean) extends Product with Serializable

    /**
     * The initial state of the Conductor.
     * Any calls the thread{ ... } will result in started Threads that quickly block waiting for the
     * main thread to give it the green light.
     * Any call to conduct will start the test.
     */
    private case object Setup extends ConductorState(false, false)

    /**
     * The state of the Conductor while its running.
     * Any calls the thread{ ... } will result in running Threads.
     * Any further call to conduct will result in an exception.
     */
    private case object TestStarted extends ConductorState(true, false)

    /**
     * The state of the Conductor after all threads have finished,
     * and the whenFinished method has completed.
     * Any calls the thread{ ... } will result in an exception
     * Any call to conduct will result in an exception.
     */
    private case object TestFinished extends ConductorState(true, true)
  }
}
