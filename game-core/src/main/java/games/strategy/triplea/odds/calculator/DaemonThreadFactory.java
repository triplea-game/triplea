package games.strategy.triplea.odds.calculator;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Borrowed from Executors$DefaultThreadFactory, but allows for custom name and daemon.
 */
class DaemonThreadFactory implements ThreadFactory {
  private static final AtomicInteger poolNumber = new AtomicInteger(1);
  private final ThreadGroup group;
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String namePrefix;

  DaemonThreadFactory(final String name) {
    group = Thread.currentThread().getThreadGroup();
    namePrefix = name + ": pool-" + poolNumber.getAndIncrement() + "-thread-";
  }

  @Override
  public Thread newThread(final Runnable r) {
    final Thread thread = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
    thread.setDaemon(true);
    thread.setPriority(Thread.NORM_PRIORITY);
    return thread;
  }
}
