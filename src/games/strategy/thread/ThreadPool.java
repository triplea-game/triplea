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

import java.util.concurrent.LinkedBlockingQueue;
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
    private final AtomicInteger m_threadCount = new AtomicInteger();

    //how many threads arent busy
    private final AtomicInteger m_availableThreads = new AtomicInteger();
    
    //how many tasks are queued or running, 
    //usually = m_threadCount - m_availableThreads + m_pendingTasks.size()
    //but since we dont synchronize we need another atomic variable to hold this value
    private final AtomicInteger m_unfinishedTaskCount = new AtomicInteger();
    
    //queued tasks
    private final LinkedBlockingQueue<Runnable> m_pendingTasks = new LinkedBlockingQueue<Runnable>();
    
    //is the thread pool active
    private volatile boolean m_isRunning = true;

    
    /**
     * Creates a new instance of ThreadPool max is the maximum number of threads the pool can have. The pool may have fewer threads at any given time.
     */
    public ThreadPool(int max, String name)
    {
        if (max < 1)
            throw new IllegalArgumentException("Max must be >= 1, instead its:" + max);
        if(name == null)
            m_name = "Unamed";
        else
        	m_name = name;
        
        m_maxThreadCount = max;
        
    }

    /**
     * Create a new thread.
     */
    private void grow()
    {
    	int threadCount = m_threadCount.incrementAndGet();

    	//we cant grow
    	if(threadCount > m_maxThreadCount)
    	{
    		m_threadCount.decrementAndGet();
    		return;
    	}
    	
    	ThreadTracker tracker = new ThreadTracker();
		Thread thread = new Thread(tracker, getClass().getName() + ":" + m_name + ":" + threadCount);
		thread.start();
		
    }

    /**
     * Run the given task. This method returns immediatly
     * If the thread pool has been shut down the task will not 
     * run.
     */
    public void runTask(Runnable task)
    {
        if(!m_isRunning)
            return;

        m_unfinishedTaskCount.incrementAndGet();
        
        if(m_availableThreads.get() == 0 && m_threadCount.get() < m_maxThreadCount)
        {
        	grow();
        }
        
        
        if(!m_pendingTasks.offer(task))
        {
        	//this should never happen, but if it does, we should
        	//do something
    	    throw new IllegalStateException("Could not offer to queue");
        }
    }

    /**
     * returns when all tasks run through the runTask method have finished.
     */
    public void waitForAll()
    {
        while (m_unfinishedTaskCount.get() != 0)
        {
        	try 
        	{
				Thread.sleep(5);
			} catch (InterruptedException e) 
			{
				//ignore
			}
        	
        }
    }
    
    int getThreadCount()
    {
    	return m_threadCount.get(); 
    }

    /**
     * Shutdown the thread pool. Currently running tasks will finish, but new tasks will not start. 
     * All threads will shutdown after finishing any tasks they may be currently running. 
     * A call to shutDown() followed by waitForAll() will ensure that no threads are running.
     */
    public void shutDown()
    {
        m_isRunning = false;
        
        //remove whats in the queue
        while(m_pendingTasks.poll() != null)
        	m_unfinishedTaskCount.decrementAndGet();
        
       Runnable dummy = new Runnable() 
       {	
		 public void run() {}
	   }; 
	   
	   //we need to wake up the threads so that they will notice that m_run is false
	   //add dummy elements so that the threads will wake
       for(int i = 0; i < m_maxThreadCount; i++)
       {
    	   m_pendingTasks.offer(dummy);
       }
    }

    private class ThreadTracker implements Runnable
    {
    	
        public void run()
        {    
            while (m_isRunning)
            {
                Runnable task = getTask();
                if (task == null)
                    continue;
                
                //clear the interupted state of this thread
                Thread.interrupted();
                
                if(m_isRunning)
                	runTask(task);
                
            }//end while run

            m_threadCount.decrementAndGet();

        }

		private void runTask(Runnable task) 
		{
			try
			{
			    task.run();
			} catch (Throwable t)
			{
			    t.printStackTrace();
			}
			m_unfinishedTaskCount.decrementAndGet();
			
		}

        private Runnable getTask()
        {
        	m_availableThreads.incrementAndGet();
        	Runnable task;
			try 
			{
				task = m_pendingTasks.take();
			} catch (InterruptedException e) 
			{
				return null;			
			}
        	m_availableThreads.decrementAndGet();
        	
        	return task;
        }
    }
}
