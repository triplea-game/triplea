package games.strategy.thread;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import com.google.common.annotations.VisibleForTesting;

/**
 * Utility class for ensuring that locks are acquired in a consistent order.
 * <p>
 * Simply use this class and call acquireLock(aLock) releaseLock(aLock) instead of lock.lock(), lock.release(). If locks
 * are acquired in an
 * inconsistent order, an error message will be printed.
 * <p>
 * This class is not terribly good for multithreading as it locks globally on all calls, but that is ok, as this code is
 * meant more for when
 * you are considering your ambitious multi-threaded code a mistake, and you are trying to limit the damage.
 * <p>
 */
public enum LockUtil {
  INSTANCE;

  // the locks the current thread has
  // because locks can be re-entrant, store this as a count
  private final ThreadLocal<Map<Lock, Integer>> locksHeld = ThreadLocal.withInitial(() -> new HashMap<>());

  // a map of all the locks ever held when a lock was acquired
  // store weak references to everything so that locks don't linger here forever
  private final Map<Lock, Set<WeakLockRef>> locksHeldWhenAcquired = new WeakHashMap<>();
  private final Object mutex = new Object();

  private final AtomicReference<ErrorReporter> errorReporterRef = new AtomicReference<>(new DefaultErrorReporter());

  public void acquireLock(final Lock aLock) {
    // we already have the lock, increase the count
    if (isLockHeld(aLock)) {
      final int current = locksHeld.get().get(aLock);
      locksHeld.get().put(aLock, current + 1);
    }
    // we don't have it
    else {
      synchronized (mutex) {
        // all the locks currently held must be acquired before a lock
        if (!locksHeldWhenAcquired.containsKey(aLock)) {
          locksHeldWhenAcquired.put(aLock, new HashSet<>());
        }
        for (final Lock l : locksHeld.get().keySet()) {
          locksHeldWhenAcquired.get(aLock).add(new WeakLockRef(l));
        }
        // we are lock a, check to
        // see if any lock we hold (b)
        // has ever been acquired before a
        for (final Lock l : locksHeld.get().keySet()) {
          final Set<WeakLockRef> held = locksHeldWhenAcquired.get(l);
          // clear out of date locks
          final Iterator<WeakLockRef> iter = held.iterator();
          while (iter.hasNext()) {
            if (iter.next().get() == null) {
              iter.remove();
            }
          }
          if (held.contains(new WeakLockRef(aLock))) {
            errorReporterRef.get().reportError(aLock, l);
          }
        }
      }
      locksHeld.get().put(aLock, 1);
    }

    aLock.lock();
  }

  public void releaseLock(final Lock aLock) {
    int count = locksHeld.get().get(aLock);
    count--;
    if (count == 0) {
      locksHeld.get().remove(aLock);
    } else {
      locksHeld.get().put(aLock, count);
    }

    aLock.unlock();
  }

  public boolean isLockHeld(final Lock aLock) {
    return locksHeld.get().containsKey(aLock);
  }

  @VisibleForTesting
  ErrorReporter setErrorReporter(final ErrorReporter errorReporter) {
    return errorReporterRef.getAndSet(errorReporter);
  }

  @VisibleForTesting
  interface ErrorReporter {
    void reportError(Lock from, Lock to);
  }

  private static final class DefaultErrorReporter implements ErrorReporter {
    @Override
    public void reportError(final Lock from, final Lock to) {
      System.err.println("Invalid lock ordering at, from:" + from + " to:" + to + " stack trace:" + getStackTrace());
    }

    private static String getStackTrace() {
      final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      final StringBuilder builder = new StringBuilder();
      for (final StackTraceElement e : trace) {
        builder.append(e.toString());
        builder.append("\n");
      }
      return builder.toString();
    }
  }

  private static final class WeakLockRef extends WeakReference<Lock> {
    // cache the hash code to make sure it doesn't change if our reference
    // has been cleared
    private final int hashCode;

    public WeakLockRef(final Lock referent) {
      super(referent);
      hashCode = referent.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof WeakLockRef) {
        final WeakLockRef other = (WeakLockRef) o;
        return other.get() == this.get();
      }
      return false;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }
}
