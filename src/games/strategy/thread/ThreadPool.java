/*
 * ThreadPool.java
 *
 * Created on January 25, 2002, 1:06 PM
 */

package games.strategy.thread;

import java.util.*;

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
    private int m_threadCount = 0;
    //how many threads arent busy
    private int m_availableThreads = 0;
    
    private final List m_pendingTasks = new LinkedList();
    private final List m_runningTasks = new LinkedList();

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
        Thread thread = new Thread(tracker, getClass().getName() + ":" + m_name + ":" + m_threadCount);

        m_threadCount++;

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
                if (m_threadCount < m_maxThreadCount)
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
                    if (m_pendingTasks.isEmpty() && m_runningTasks.isEmpty())
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
                    m_runningTasks.remove(task);
                }
                
                synchronized (m_doneLock)
                {
                    m_doneLock.notifyAll();
                }
            }//end while run

            synchronized (m_taskLock)
            {
                m_threadCount--;
            }
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
                    Runnable task = (Runnable) m_pendingTasks.get(0);
                    m_pendingTasks.remove(0);
                    m_runningTasks.add(task);
                    return task;
                }

            }
        }
    }
}
