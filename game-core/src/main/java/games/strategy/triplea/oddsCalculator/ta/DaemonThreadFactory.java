package games.strategy.triplea.oddsCalculator.ta;

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
  private final boolean daemon;

  DaemonThreadFactory(final boolean isDaemon, final String name) {
    daemon = isDaemon;
    final SecurityManager s = System.getSecurityManager();
    group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    namePrefix = name + ": pool-" + poolNumber.getAndIncrement() + "-thread-";
  }

  @Override
  public Thread newThread(final Runnable r) {
    final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
    t.setDaemon(daemon);
    if (t.getPriority() != Thread.NORM_PRIORITY) {
      t.setPriority(Thread.NORM_PRIORITY);
    }
    return t;
  }
}
