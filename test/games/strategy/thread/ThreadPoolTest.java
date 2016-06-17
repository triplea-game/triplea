package games.strategy.thread;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

public class ThreadPoolTest {

  @Test
  public void testRunOneTask() {
    final ThreadPool pool = new ThreadPool(50);
    final Task task = new Task();
    pool.runTask(task);
    pool.waitForAll();
    assertTrue(task.isDone());
  }

  @Test
  public void testSingleThread() {
    final ThreadPool pool = new ThreadPool(1);
    final Collection<Task> tasks = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      final Task task = new Task();
      tasks.add(task);
      pool.runTask(task);
    }
    pool.waitForAll();
    for (Task runnable : tasks) {
      assertTrue(runnable.isDone());
    }
    pool.shutDown();
  }

  @Test
  public void testSimple() {
    final ThreadPool pool = new ThreadPool(5);
    final Collection<Task> tasks = new ArrayList<>();
    for (int i = 0; i < 3000; i++) {
      final Task task = new Task();
      tasks.add(task);
      pool.runTask(task);
    }

    pool.waitForAll();
    for(Task task1 : tasks){
      assertTrue(task1.isDone());
    }
    pool.shutDown();
  }

  @Test
  public void testBlocked() {
    final Collection<Thread> threads = new ArrayList<>();
    for (int j = 0; j < 50; j++) {
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          threadTestBlock();
        }
      };
      final Thread t = new Thread(r);
      threads.add(t);
      t.start();
    }
    for(Thread thread : threads){
      try {
        thread.join();
      } catch (InterruptedException e) {
        // ignore interrupted exception
      }
    }
  }

  private static void threadTestBlock() {
    final ThreadPool pool = new ThreadPool(2);
    final ArrayList<BlockedTask> blockedTasks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final BlockedTask task = new BlockedTask();
      blockedTasks.add(task);
      pool.runTask(task);
    }
    pool.waitForAll();
    for (final BlockedTask task : blockedTasks) {
      assertTrue(task.isDone());
    }
    pool.shutDown();
  }
}


class Task implements Runnable {
  private boolean done = false;

  public synchronized boolean isDone() {
    return done;
  }

  @Override
  public void run() {
    try {
      Thread.sleep(0, 1);
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
    done = true;
  }
}


class BlockedTask extends Task {
  @Override
  public void run() {
    synchronized (this) {
      try {
        wait(10);
      } catch (final InterruptedException ie) {
      }
      super.run();
    }
  }
}
