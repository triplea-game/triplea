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


/*
 * ThreadPool.java
 *
 * Created on January 25, 2002, 1:06 PM
 */

package games.strategy.thread;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple thread pool.
 * 
 * @author Sean Bridges
 */
public class ThreadPool
{
    private final String m_name;

    //the max number of threads we can have
    private final int m_maxThreadCount;
    //how many threads we have
    private AtomicInteger m_threadCount = new AtomicInteger();
    //how many threads arent busy
    private int m_availableThreads = 0;
    
    private final List<Runnable> m_pendingTasks = new LinkedList<Runnable>();
    private final AtomicInteger m_runningTaskCount = new AtomicInteger();

    private final Object m_taskLock = new Object();
    private final Object m_doneLock = new Object();
    private volatile boolean m_run = true;

    /**
     * Creates a new instance of ThreadPool max is the maximum number of threads the pool can have. The pool may have fewer threads at any given time.
     */
    public ThreadPool(int max, String name)
    {
        if (max < 1)
            throw new IllegalArgumentException("Max must be >= 1, instead its:" + max);
        if(name == null)
            name = "Unamed";
        
        m_maxThreadCount = max;
        m_name = name;
    }

    /**
     * Create a new thread.
     */
    private void grow()
    {
        ThreadTracker tracker = new ThreadTracker();
        Thread thread = new Thread(tracker, getClass().getName() + ":" + m_name + ":" + m_threadCount.incrementAndGet());

        thread.start();
    }

    /**
     * Run the given task. This method returns immediatly
     * If the thread pool has been shut down the task will not 
     * run.
     */
    public void runTask(Runnable task)
    {
        if(!m_run)
            return;
        synchronized (m_taskLock)
        {
            if (m_availableThreads == 0)
            {
                if (m_threadCount.get() < m_maxThreadCount)
                {
                    grow();
                }
            }
            m_pendingTasks.add(task);
            m_taskLock.notifyAll();
        }
    }

    /**
     * returns when all tasks run through the runTask method have finished.
     */
    public void waitForAll()
    {
        while (true)
        {
            synchronized (m_doneLock)
            {
                synchronized (m_taskLock)
                {
                    if (m_pendingTasks.isEmpty() && m_runningTaskCount.get() == 0)
                        return;
                }

                try
                {
                    m_doneLock.wait();
                } catch (InterruptedException e)
                {
                }
            }
        }
    }

    /**
     * Shutdown the thread pool. Currently running tasks will finish, but new tasks will not start. All threads will shutdown after finishing any tasks they may be currently running. A call to shutDown() followed by waitForAll() will ensure that no threads are running.
     */
    public void shutDown()
    {
        m_run = false;
        synchronized(m_taskLock)
        {
            m_taskLock.notifyAll();
        }
    }

    private class ThreadTracker implements Runnable
    {
        public void run()
        {           
            while (m_run)
            {
                Runnable task = getTask();
                if (task == null)
                    continue;

                try
                {
                    task.run();
                } catch (Throwable t)
                {
                    t.printStackTrace();
                }
                
                synchronized (m_taskLock)
                {
                    m_runningTaskCount.decrementAndGet();
                }
                
                synchronized (m_doneLock)
                {
                    m_doneLock.notifyAll();
                }
            }//end while run

            m_threadCount.decrementAndGet();

        }

        private Runnable getTask()
        {
            synchronized (m_taskLock)
            {
                if (!m_run)
                    return null;

                if (m_pendingTasks.isEmpty())
                {
                    try
                    {
                        m_availableThreads++;
                        ThreadPool.this.m_taskLock.wait();

                    } catch (InterruptedException ie)
                    {
                    } finally
                    {
                        m_availableThreads--;
                    }
                    return null;
                } else
                {
                    Runnable task = m_pendingTasks.get(0);
                    m_pendingTasks.remove(0);
                    m_runningTaskCount.incrementAndGet();
                    return task;
                }

            }
        }
    }
}
