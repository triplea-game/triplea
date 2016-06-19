package games.strategy.thread;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Test;

import games.strategy.thread.LockUtil.ErrorReporter;

public class LockUtilTest {
  private static final LockUtil S_LOCKUTIL = new LockUtil();
  private final TestErrorReporter m_reporter = new TestErrorReporter();

  @Before
  public void setUp() {
    S_LOCKUTIL.setErrorReporter(m_reporter);
  }

  @Test
  public void testEmpty() {
    assertFalse(S_LOCKUTIL.isLockHeld(new ReentrantLock()));
  }

  @Test
  public void testMultipleLocks() {
    final List<Lock> locks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      locks.add(new ReentrantLock());
    }
    for (final Lock l : locks) {
      S_LOCKUTIL.acquireLock(l);
      assertTrue(S_LOCKUTIL.isLockHeld(l));
    }
    for (final Lock l : locks) {
      S_LOCKUTIL.releaseLock(l);
      assertFalse(S_LOCKUTIL.isLockHeld(l));
    }
    assertFalse(m_reporter.errorOccured());
    // repeat the sequence, make sure no errors
    for (final Lock l : locks) {
      S_LOCKUTIL.acquireLock(l);
    }
    assertFalse(m_reporter.errorOccured());
  }

  @Test
  public void testFail() {
    final Lock l1 = new ReentrantLock();
    final Lock l2 = new ReentrantLock();
    // acquire in the correct order
    S_LOCKUTIL.acquireLock(l1);
    S_LOCKUTIL.acquireLock(l2);
    // release
    S_LOCKUTIL.releaseLock(l2);
    S_LOCKUTIL.releaseLock(l1);
    assertFalse(m_reporter.errorOccured());
    // acquire locks in the wrong order
    S_LOCKUTIL.acquireLock(l2);
    S_LOCKUTIL.acquireLock(l1);
    assertTrue(m_reporter.errorOccured());
  }

  @Test
  public void testAcquireTwice() {
    final ReentrantLock l1 = new ReentrantLock();
    S_LOCKUTIL.acquireLock(l1);
    S_LOCKUTIL.acquireLock(l1);
    S_LOCKUTIL.releaseLock(l1);
    S_LOCKUTIL.releaseLock(l1);
    assertTrue(l1.getHoldCount() == 0);
    assertFalse(S_LOCKUTIL.isLockHeld(l1));
  }
}


class TestErrorReporter extends ErrorReporter {
  private boolean m_errorOccured = false;

  @Override
  public void reportError(final Lock from, final Lock to) {
    m_errorOccured = true;
  }

  public boolean errorOccured() {
    return m_errorOccured;
  }
}
