package games.strategy.thread;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import games.strategy.util.ThreadUtil;

public class ThreadPoolTest {

  @Test
  public void testThrowsExceptionOnInvalidArgument() {
    assertThrows(IllegalArgumentException.class, () -> new ThreadPool(0));
    assertThrows(IllegalArgumentException.class, () -> new ThreadPool(-1));
    assertThrows(IllegalArgumentException.class, () -> new ThreadPool(Integer.MIN_VALUE));
  }

  @Test
  public void testRunOneTask() {
    final ThreadPool pool = new ThreadPool(50);
    final Task task = new Task();
    pool.submit(task);
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
      pool.submit(task);
    }
    pool.waitForAll();
    for (final Task runnable : tasks) {
      assertTrue(runnable.isDone());
    }
    pool.shutdown();
  }

  @Test
  public void testSimple() {
    final ThreadPool pool = new ThreadPool(5);
    final Collection<Task> tasks = new ArrayList<>();
    for (int i = 0; i < 3000; i++) {
      final Task task = new Task();
      tasks.add(task);
      pool.submit(task);
    }

    pool.waitForAll();
    for (final Task task1 : tasks) {
      assertTrue(task1.isDone());
    }
    pool.shutdown();
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
    for (final Thread thread : threads) {
      try {
        thread.join();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static void threadTestBlock() {
    final ThreadPool pool = new ThreadPool(2);
    final ArrayList<BlockedTask> blockedTasks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final BlockedTask task = new BlockedTask();
      blockedTasks.add(task);
      pool.submit(task);
    }
    pool.waitForAll();
    for (final BlockedTask task : blockedTasks) {
      assertTrue(task.isDone());
    }
    pool.shutdown();
  }

  private static class Task implements Runnable {
    private boolean done = false;

    public synchronized boolean isDone() {
      return done;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(0, 1);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      done = true;
    }
  }

  private static class BlockedTask extends Task {
    @Override
    public void run() {
      synchronized (this) {
        ThreadUtil.sleep(10L);
        super.run();
      }
    }
  }
}
