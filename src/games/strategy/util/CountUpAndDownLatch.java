package games.strategy.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * This synchronization aid is very similar to {@link CountDownLatch},
 * except that you can increment the latch, and can also countDownOrWaitIfZero
 * (which will wait until the latch is positive again before counting down).
 * Implements AQS behind the scenes similar to CountDownLatch.
 * 
 * @author Mark Christopher Duncan (veqryn)
 */
public class CountUpAndDownLatch extends AbstractQueuedSynchronizer {
	private static final long serialVersionUID = -1656388212821764097L;
	
	private final Sync sync;
	private final Sync waitIfZeroSync;
	
	/**
	 * Constructs a {@code CountUpAndDownLatch} initialized with zero.
	 */
	public CountUpAndDownLatch() {
		sync = new Sync();
		waitIfZeroSync = new Sync();
	}
	
	/**
	 * Constructs a {@code CountUpAndDownLatch} initialized with the given count.
	 * 
	 * @param count
	 *          the number of times {@link #countDown} must be invoked before threads can pass through {@link #await}
	 * @throws IllegalArgumentException
	 *           if {@code count} is negative
	 */
	public CountUpAndDownLatch(final int count) {
		if (count < 0) {
			throw new IllegalArgumentException("count < 0");
		}
		sync = new Sync(count);
		waitIfZeroSync = new Sync();
	}
	
	/**
	 * Increment the count by one.
	 */
	public void increment() {
		sync.releaseShared(1);
		waitIfZeroSync.releaseShared(-1);
	}
	
	/**
	 * @see CountDownLatch#countDown()
	 */
	public void countDown() {
		sync.releaseShared(-1);
	}
	
	/**
	 * If our state is already at zero, we will wait until it becomes positive again before counting down.
	 * 
	 * @see CountDownLatch#countDown()
	 * @see CountDownLatch#await()
	 * @param countDownIfInterrupted
	 *          if true, we will countDown after we are interrupted, before we throw the interruption up.
	 * @throws InterruptedException
	 */
	public void countDownOrWaitIfZero(final boolean countDownIfInterrupted) throws InterruptedException {
		countDownOrWaitIfZero(countDownIfInterrupted, 0L, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * If our state is already at zero, we will wait until it becomes positive again before counting down.
	 * 
	 * @see CountDownLatch#countDown()
	 * @see CountDownLatch#await(long,TimeUnit)
	 * @param countDownIfInterrupted
	 *          if true, we will countDown after we are interrupted, before we throw the interruption up.
	 * @param timeout
	 *          length of time to wait for the state to become positive
	 * @param unit
	 *          TimeUnit
	 * @return true if we waited successfully, false if interrupted
	 * @throws InterruptedException
	 */
	public boolean countDownOrWaitIfZero(final boolean countDownIfInterrupted, final long timeout, final TimeUnit unit)
				throws InterruptedException {
		return applyDeltaOrWaitIfZero(-1, countDownIfInterrupted, timeout, unit);
	}
	
	/**
	 * @see CountDownLatch#countDown()
	 * @param delta
	 *          the amount to increment (or if negative, decrement countDown)
	 */
	public void applyDelta(final int delta) {
		sync.releaseShared(delta);
		if (delta > 0) {
			waitIfZeroSync.releaseShared(-delta);
		}
	}
	
	/**
	 * If delta is negative, and our state is already at zero, we will wait until it becomes positive again before counting down.
	 * 
	 * @see CountDownLatch#countDown()
	 * @see CountDownLatch#await()
	 * @param delta
	 *          the amount to increment (or if negative, decrement countDown)
	 * @param applyDeltaIfInterrupted
	 *          if true, we will apply the delta after we are interrupted, before we throw the interruption up.
	 * @throws InterruptedException
	 */
	public void applyDeltaOrWaitIfZero(final int delta, final boolean applyDeltaIfInterrupted) throws InterruptedException {
		applyDeltaOrWaitIfZero(delta, applyDeltaIfInterrupted, 0L, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * If delta is negative, and our state is already at zero, we will wait until it becomes positive again before counting down.
	 * 
	 * @see CountDownLatch#countDown()
	 * @see CountDownLatch#await(long,TimeUnit)
	 * @param delta
	 *          the amount to increment (or if negative, decrement countDown)
	 * @param applyDeltaIfInterrupted
	 *          if true, we will apply the delta after we are interrupted, before we throw the interruption up.
	 * @param timeout
	 *          length of time to wait for the state to become positive
	 * @param unit
	 *          TimeUnit
	 * @return true if we waited successfully, false if interrupted
	 * @throws InterruptedException
	 */
	public boolean applyDeltaOrWaitIfZero(final int delta, final boolean applyDeltaIfInterrupted, final long timeout, final TimeUnit unit)
				throws InterruptedException {
		boolean didNotTimeOut = true;
		if (delta < 0 && sync.getCount() <= 0) {
			if (waitIfZeroSync.getCount() <= 0) {
				waitIfZeroSync.releaseShared(1);
			}
			try {
				if (timeout <= 0L) {
					waitIfZeroSync.acquireSharedInterruptibly(1);
				} else {
					didNotTimeOut = waitIfZeroSync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
				}
			} catch (final InterruptedException e) {
				if (applyDeltaIfInterrupted) {
					applyDelta(delta);
				}
				throw e;
			}
		}
		applyDelta(delta);
		return didNotTimeOut;
	}
	
	/**
	 * countDown until zero.
	 */
	public void releaseAll() {
		final int count = sync.getCount();
		if (count > 0) {
			applyDelta(-count);
		}
	}
	
	/**
	 * @see CountDownLatch#getCount()
	 */
	public int getCount() {
		return sync.getCount();
	}
	
	/**
	 * @see CountDownLatch#await()
	 */
	public void await() throws InterruptedException {
		sync.acquireSharedInterruptibly(1);
	}
	
	/**
	 * @see CountDownLatch#await(long,TimeUnit)
	 */
	public boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
		return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
	}
	
	/**
	 * Returns a string identifying this latch, as well as its state.
	 * The state, in brackets, includes the String "Count =" followed by the current count.
	 */
	@Override
	public String toString() {
		return super.toString() + "[Count = " + sync.getCount() + "]";
	}
	
	/**
	 * Synchronization control for CountUpAndDownLatch.
	 * Uses AQS state to represent count.
	 */
	private static final class Sync extends AbstractQueuedSynchronizer {
		private static final long serialVersionUID = -7639904478060101736L;
		
		private Sync() {
		}
		
		private Sync(final int initialState) {
			setState(initialState);
		}
		
		int getCount() {
			return getState();
		}
		
		@Override
		protected int tryAcquireShared(final int acquires) {
			return getState() == 0 ? 1 : -1;
		}
		
		@Override
		protected boolean tryReleaseShared(final int delta) {
			// Decrement count; signal when transition to zero
			for (;;) {
				final int c = getState();
				int nextc = c + delta;
				if (c == 0 && nextc <= 0) {
					return false;
				}
				if (nextc < 0) {
					nextc = 0;
				}
				if (compareAndSetState(c, nextc)) {
					return nextc == 0;
				}
			}
		}
	}
}
