package games.strategy.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

public class ThreadPoolTest extends TestCase { 
  /** Creates a new instance of ThreadPoolTest */
  public ThreadPoolTest(final String s) {
    super(s);
  }

  public void testRunOneTask() {
    final ThreadPool pool = new ThreadPool(50);
    final Task task = new Task();
    pool.runTask(task);
    pool.waitForAll();
    assertTrue(task.isDone());
  }

  public void testSingleThread() {
    final ThreadPool pool = new ThreadPool(1);
    final Collection<Runnable> tasks = new ArrayList<Runnable>();
    for (int i = 0; i < 30; i++) {
      final Runnable task = new Task();
      tasks.add(task);
      pool.runTask(task);
    }
    pool.waitForAll();
    final Iterator<Runnable> iter = tasks.iterator();
    while (iter.hasNext()) {
      assertTrue(((Task) iter.next()).isDone());
    }
    pool.shutDown();
  }

  public void testSimple() {
    final ThreadPool pool = new ThreadPool(5);
    final Collection<Task> tasks = new ArrayList<Task>();
    for (int i = 0; i < 3000; i++) {
      final Task task = new Task();
      tasks.add(task);
      pool.runTask(task);
    }

    pool.waitForAll();
    final Iterator<Task> iter = tasks.iterator();
    while (iter.hasNext()) {
      assertTrue(iter.next().isDone());
    }
    pool.shutDown();
  }

  public void testBlocked() {
    final Collection<Thread> threads = new ArrayList<Thread>();
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
    final Iterator<Thread> iter = threads.iterator();
    while (iter.hasNext()) {
      try {
        iter.next().join();
      } catch (final InterruptedException ex) {
        ex.printStackTrace();
      }
    }
  }

  private static void threadTestBlock() {
    final ThreadPool pool = new ThreadPool(2);
    final ArrayList<BlockedTask> blockedTasks = new ArrayList<BlockedTask>();
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
