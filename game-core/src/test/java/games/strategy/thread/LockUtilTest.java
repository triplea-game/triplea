package games.strategy.thread;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class LockUtilTest {
  private final LockUtil lockUtil = LockUtil.INSTANCE;

  @Mock private LockUtil.ErrorReporter errorReporter;

  private LockUtil.ErrorReporter oldErrorReporter;

  @BeforeEach
  void setUp() {
    oldErrorReporter = lockUtil.setErrorReporter(errorReporter);
  }

  @AfterEach
  void tearDown() {
    lockUtil.setErrorReporter(oldErrorReporter);
  }

  @Test
  void testEmpty() {
    assertFalse(lockUtil.isLockHeld(new ReentrantLock()));
  }

  @Test
  void testMultipleLocks() {
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
  void testFail() {
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
  void testAcquireTwice() {
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
