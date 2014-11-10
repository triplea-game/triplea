/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.thread;

import games.strategy.thread.LockUtil.ErrorReporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.TestCase;

public class LockUtilTest extends TestCase
{
	private static final LockUtil S_LOCKUTIL = new LockUtil();
	private final TestErrorReporter m_reporter = new TestErrorReporter();
	
	@Override
	public void setUp()
	{
		S_LOCKUTIL.setErrorReporter(m_reporter);
	}
	
	public void testEmpty()
	{
		assertFalse(S_LOCKUTIL.isLockHeld(new ReentrantLock()));
	}
	
	public void testMultipleLocks()
	{
		final List<Lock> locks = new ArrayList<Lock>();
		for (int i = 0; i < 10; i++)
		{
			locks.add(new ReentrantLock());
		}
		for (final Lock l : locks)
		{
			S_LOCKUTIL.acquireLock(l);
			assertTrue(S_LOCKUTIL.isLockHeld(l));
		}
		for (final Lock l : locks)
		{
			S_LOCKUTIL.releaseLock(l);
			assertFalse(S_LOCKUTIL.isLockHeld(l));
		}
		assertFalse(m_reporter.errorOccured());
		// repeat the sequence, make sure no errors
		for (final Lock l : locks)
		{
			S_LOCKUTIL.acquireLock(l);
		}
		assertFalse(m_reporter.errorOccured());
	}
	
	public void testFail()
	{
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
	
	public void testAcquireTwice()
	{
		final ReentrantLock l1 = new ReentrantLock();
		S_LOCKUTIL.acquireLock(l1);
		S_LOCKUTIL.acquireLock(l1);
		S_LOCKUTIL.releaseLock(l1);
		S_LOCKUTIL.releaseLock(l1);
		assertTrue(l1.getHoldCount() == 0);
		assertFalse(S_LOCKUTIL.isLockHeld(l1));
	}
}


class TestErrorReporter extends ErrorReporter
{
	private boolean m_errorOccured = false;
	
	@Override
	public void reportError(final Lock from, final Lock to)
	{
		m_errorOccured = true;
	}
	
	public boolean errorOccured()
	{
		return m_errorOccured;
	}
};
