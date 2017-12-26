package games.strategy.thread;

import java.util.ArrayDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import games.strategy.debug.ClientLogger;

/**
 * An ExecutorService backed thread pool.
 */
public class ThreadPool {
  private final ExecutorService executorService;
  private final ArrayDeque<Future<?>> futuresStack = new ArrayDeque<>();

  /**
   * Creates a new instance of ThreadPool max is the maximum number of threads the pool can have. The pool may have
   * fewer threads at any given time.
   */
  public ThreadPool(final int max) {
    executorService = Executors.newFixedThreadPool(max);
  }


  /**
   * Run the given task.
   */
  public void runTask(final Runnable task) {
    futuresStack.push(executorService.submit(task));
  }


  /**
   * Returns when all tasks run through the runTask method have finished.
   */
  public void waitForAll() {
    while (!futuresStack.isEmpty()) {
      try {
        futuresStack.pop().get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        ClientLogger.logError(e);
      }
    }
  }

  /**
   * Shutdown the thread pool. Currently running tasks will finish, but new tasks will not start.
   * All threads will shutdown after finishing any tasks they may be currently running.
   * A call to shutDown() followed by waitForAll() will ensure that no threads are running.
   */
  public void shutDown() {
    executorService.shutdown();
  }

}
