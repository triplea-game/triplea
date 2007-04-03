/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.TestCase;

public class LockUtilTest extends TestCase
{

    private TestErrorReporter m_reporter = new TestErrorReporter();

    @Override
    public void setUp()
    {
        LockUtil.setErrorReporter(m_reporter);
    }

    public void testEmpty() 
    {
        assertFalse(LockUtil.isLockHeld(new ReentrantLock()));
    }
    
    public void testMultipleLocks()
    {

        List<Lock> locks = new ArrayList<Lock>();

        for (int i = 0; i < 10; i++)
        {
            locks.add(new ReentrantLock());
        }
        for (Lock l : locks)
        {
            LockUtil.acquireLock(l);
            assertTrue(LockUtil.isLockHeld(l));
        }
        for (Lock l : locks)
        {
            LockUtil.releaseLock(l);
            assertFalse(LockUtil.isLockHeld(l));
        }

        assertFalse(m_reporter.errorOccured());

        // repeat the sequence, make sure no errors
        for (Lock l : locks)
        {
            LockUtil.acquireLock(l);
        }

        assertFalse(m_reporter.errorOccured());

    }

    public void testFail()
    {
        Lock l1 = new ReentrantLock();
        Lock l2 = new ReentrantLock();

        // acquire in the correct order
        LockUtil.acquireLock(l1);
        LockUtil.acquireLock(l2);
        // release
        LockUtil.releaseLock(l2);
        LockUtil.releaseLock(l1);

        assertFalse(m_reporter.errorOccured());

        // acquire locks in the wrong order
        LockUtil.acquireLock(l2);
        LockUtil.acquireLock(l1);

        assertTrue(m_reporter.errorOccured());
    }

    public void testAcquireTwice()
    {
        ReentrantLock l1 = new ReentrantLock();
        LockUtil.acquireLock(l1);
        LockUtil.acquireLock(l1);

        LockUtil.releaseLock(l1);
        LockUtil.releaseLock(l1);

        assertTrue(l1.getHoldCount() == 0);
        assertFalse(LockUtil.isLockHeld(l1));
    }

}

class TestErrorReporter extends ErrorReporter
{
    private boolean m_errorOccured = false;

    public void reportError(Lock from, Lock to)
    {
        m_errorOccured = true;
    }

    public boolean errorOccured()
    {
        return m_errorOccured;
    }

};
