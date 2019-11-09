package org.triplea.thread;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.java.Interruptibles;

public class ThreadPoolTest {

  @Test
  void testThrowsExceptionOnInvalidArgument() {
    assertThrows(IllegalArgumentException.class, () -> new ThreadPool(0));
    assertThrows(IllegalArgumentException.class, () -> new ThreadPool(-1));
    assertThrows(IllegalArgumentException.class, () -> new ThreadPool(Integer.MIN_VALUE));
  }

  @Test
  void testRunOneTask() {
    final ThreadPool pool = new ThreadPool(50);
    final Task task = new Task();
    pool.submit(task);
    pool.waitForAll();
    assertTrue(task.isDone());
  }

  @Test
  void testSingleThread() {
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
  void testSimple() {
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
  void testBlocked() {
    final Collection<Thread> threads = new ArrayList<>();
    for (int j = 0; j < 50; j++) {
      final Thread t = new Thread(ThreadPoolTest::threadTestBlock);
      threads.add(t);
      t.start();
    }
    threads.forEach(Interruptibles::join);
  }

  private static void threadTestBlock() {
    final ThreadPool pool = new ThreadPool(2);
    final List<BlockedTask> blockedTasks = new ArrayList<>();
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
    private volatile boolean done = false;

    boolean isDone() {
      return done;
    }

    @Override
    public void run() {
      Interruptibles.sleep(0L, 1);
      done = true;
    }
  }

  private static class BlockedTask extends Task {
    @Override
    public void run() {
      synchronized (this) {
        Interruptibles.sleep(10L);
        super.run();
      }
    }
  }
}
