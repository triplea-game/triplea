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
 * @author  Sean Bridges
 */
public class ThreadPool
{
  private final int m_maxCount;
  private List m_availableThreads = new LinkedList();
  private List m_allThreads = new LinkedList();
  private List m_pendingTasks = new LinkedList();
  private List m_runningTasks = new LinkedList();

  private Object m_taskLock = new Object();
  private Object m_doneLock = new Object();


  /**
   * Creates a new instance of ThreadPool
   * max is the maximum number of threads the pool can have.
   * The pool may have fewer threads at any given time.
   */
  public ThreadPool(int max)
  {
    if(max < 1)
      throw new IllegalArgumentException("Max must be >= 1, instead its:" + max);
    m_maxCount = max;
  }

  /**
   * Create a new thread.
   */
  private void grow()
  {
    ThreadTracker tracker = new ThreadTracker();
    Thread thread = new Thread(tracker, getClass().getName() + ":" + m_allThreads.size());

    m_allThreads.add(tracker);

    thread.start();
  }

  /**
   * Run the given task.  This method returns immediatly
   */
  public void runTask(Runnable task)
  {
    synchronized(m_taskLock)
    {
      if(m_availableThreads.isEmpty())
      {
        if(m_allThreads.size() < m_maxCount)
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
    synchronized(m_doneLock)
    {
      synchronized(m_taskLock)
      {
        if(m_pendingTasks.isEmpty() && m_runningTasks.isEmpty())
          return;
      }

      try
      {
        m_doneLock.wait();
      } catch(InterruptedException e)
      {}
    }
    waitForAll();

  }

  /**
   * Shutdown the thread pool.
   * Currently running tasks will finish, but new tasks will not start.
   * All threads will shutdown after finishing any tasks they may
   * be currently running.
   * A call to shutDown() followed by waitForAll() will ensure
   * that no threads are running.
   */

  public void shutDown()
  {
    synchronized(m_taskLock)
    {
      m_pendingTasks.clear();
      Iterator iter = m_allThreads.iterator();
      while(iter.hasNext())
      {
        ThreadTracker tracker = (ThreadTracker) iter.next();
        tracker.stop();
      }
    }
  }

  private class ThreadTracker implements Runnable
  {
    private boolean m_run = true;
    private volatile Thread m_thread;

    public void run()
    {
      m_thread = Thread.currentThread();
      while(m_run)
      {
        Runnable task = getTask();
        if(task == null)
          continue;

        try
        {
          task.run();
        } catch(Throwable t)
        {
          t.printStackTrace();
        }
        //NOTE - get the done lock first,
        //wait for all gets the locks in the order done, task
        //so to avoid deadlock we must do things in the same order here
        synchronized(m_doneLock)
        {

          synchronized(m_taskLock)
          {
            m_runningTasks.remove(task);
          }

          m_doneLock.notifyAll();
        }
      }//end while run

      synchronized(m_taskLock)
      {
        m_allThreads.remove(this);
      }
    }

    public void stop()
    {
        if(!m_run)
            throw new IllegalStateException("not running");
      m_run = false;
      if(m_thread != null)
          m_thread.interrupt();
    }

    private Runnable getTask()
    {
      synchronized(m_taskLock)
      {
        if(! m_run )
          return null;

        if(m_pendingTasks.isEmpty())
        {
          try
          {
            m_availableThreads.add(this);
            ThreadPool.this.m_taskLock.wait();

          } catch(InterruptedException ie)
          {}
          finally
          {
            m_availableThreads.remove(this);
          }
          return getTask();
        }
        else
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
