package games.strategy.thread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class LockUtilTest {
  private final LockUtil lockUtil = LockUtil.INSTANCE;

  @Mock
  private LockUtil.ErrorReporter errorReporter;

  private LockUtil.ErrorReporter oldErrorReporter;

  @Before
  public void setUp() {
    oldErrorReporter = lockUtil.setErrorReporter(errorReporter);
  }

  @After
  public void tearDown() {
    lockUtil.setErrorReporter(oldErrorReporter);
  }

  @Test
  public void testEmpty() {
    assertFalse(lockUtil.isLockHeld(new ReentrantLock()));
  }

  @Test
  public void testMultipleLocks() {
    final List<Lock> locks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      locks.add(new ReentrantLock());
    }
    for (final Lock l : locks) {
      lockUtil.acquireLock(l);
      assertTrue(lockUtil.isLockHeld(l));
    }
    for (final Lock l : locks) {
      lockUtil.releaseLock(l);
      assertFalse(lockUtil.isLockHeld(l));
    }
    assertNoErrorOccurred();
    // repeat the sequence, make sure no errors
    for (final Lock l : locks) {
      lockUtil.acquireLock(l);
    }
    assertNoErrorOccurred();
  }

  @Test
  public void testFail() {
    final Lock l1 = new ReentrantLock();
    final Lock l2 = new ReentrantLock();
    // acquire in the correct order
    lockUtil.acquireLock(l1);
    lockUtil.acquireLock(l2);
    // release
    lockUtil.releaseLock(l2);
    lockUtil.releaseLock(l1);
    assertNoErrorOccurred();
    // acquire locks in the wrong order
    lockUtil.acquireLock(l2);
    lockUtil.acquireLock(l1);
    assertErrorOccurred();
  }

  @Test
  public void testAcquireTwice() {
    final ReentrantLock l1 = new ReentrantLock();
    lockUtil.acquireLock(l1);
    lockUtil.acquireLock(l1);
    lockUtil.releaseLock(l1);
    lockUtil.releaseLock(l1);
    assertEquals(0, l1.getHoldCount());
    assertFalse(lockUtil.isLockHeld(l1));
  }

  private void assertErrorOccurred() {
    verify(errorReporter).reportError(isA(Lock.class), isA(Lock.class));
  }

  private void assertNoErrorOccurred() {
    verify(errorReporter, never()).reportError(isA(Lock.class), isA(Lock.class));
  }
}
