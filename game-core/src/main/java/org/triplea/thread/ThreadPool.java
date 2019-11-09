package org.triplea.thread;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import lombok.extern.java.Log;

/** An ExecutorService backed thread pool. */
@Log
public class ThreadPool {
  private final ExecutorService executorService;
  private final Queue<Future<?>> futureQueue = new ArrayDeque<>();

  /**
   * Creates a thread pool that reuses a fixed number of threads operating off a shared unbounded
   * queue. At any point, at most {@code max} threads will be active processing tasks. If additional
   * tasks are submitted when all threads are active, they will wait in the queue until a thread is
   * available. If any thread terminates due to a failure during execution prior to shutdown, a new
   * one will take its place if needed to execute subsequent tasks. The threads in the pool will
   * exist until it is explicitly {@link ThreadPool#shutdown shutdown}.
   *
   * @param max the number of threads in the pool
   * @throws IllegalArgumentException if {@code max <= 0}
   */
  public ThreadPool(final int max) {
    executorService = Executors.newFixedThreadPool(max);
  }

  /** Run the given task. */
  public void submit(final Runnable task) {
    futureQueue.add(executorService.submit(task));
  }

  /** Returns when all tasks run through the runTask method have finished. */
  public void waitForAll() {
    while (!futureQueue.isEmpty()) {
      try {
        futureQueue.remove().get();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (final ExecutionException e) {
        final Throwable exceptionToLog = e.getCause();
        /*
         * An {@code ExecutionException} contains no useful information; it's simply an adapter
         * to tunnel exceptions thrown by tasks through the {@code Executor} API. We only log the
         * cause to reduce the number of stack trace frames visible to the user.
         */
        log.log(
            Level.SEVERE,
            "Threading execution exception: " + exceptionToLog.getMessage(),
            exceptionToLog);
      }
    }
  }

  /**
   * Shutdown the thread pool. Currently running tasks will finish, but new tasks will not start.
   * All threads will shutdown after finishing any tasks they may be currently running. A call to
   * shutdown() followed by waitForAll() will ensure that no threads are running.
   */
  public void shutdown() {
    executorService.shutdown();
  }
}
