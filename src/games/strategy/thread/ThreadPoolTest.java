/*
 * ThreadPoolTest.java
 *
 * Created on January 25, 2002, 3:34 PM
 */

package games.strategy.thread;

import junit.framework.*;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 */
public class ThreadPoolTest extends TestCase
{

  /** Creates a new instance of ThreadPoolTest */
    public ThreadPoolTest(String s)
  {
    super(s);
    }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTestSuite(ThreadPoolTest.class);
    return suite;
  }

  public void testRunOneTask()
  {
    ThreadPool pool = new ThreadPool(50, "test");
    Task task = new Task();
    pool.runTask(task);
    pool.waitForAll();
    assertTrue(task.isDone());
  }

  public void testSingleThread()
  {
    ThreadPool pool = new ThreadPool(1, "test");
    Collection<Runnable> tasks = new ArrayList<Runnable>();

    for(int i = 0; i < 30; i++)
    {
      Runnable task = new Task();
      tasks.add(task);
      pool.runTask(task);

    }

    pool.waitForAll();

    Iterator<Runnable> iter = tasks.iterator();
    while(iter.hasNext())
    {
      assertTrue( ((Task) iter.next()).isDone());
    }
    pool.shutDown();
  }


  public void testSimple()
  {
    ThreadPool pool = new ThreadPool(5, "test");
    Collection<Task> tasks = new ArrayList<Task>();

    for(int i = 0; i < 3000; i++)
    {
      Task task = new Task();
      tasks.add(task);
      pool.runTask(task);
    }

    assertEquals(5, pool.getThreadCount());
    
    pool.waitForAll();

    Iterator<Task> iter = tasks.iterator();
    while(iter.hasNext())
    {
      assertTrue(iter.next().isDone());
    }
    pool.shutDown();
  }



  public void testBlocked()
  {
    Collection<Thread> threads = new ArrayList<Thread>();

    for(int j = 0; j < 20; j++)
    {
      Runnable r = new Runnable() {

        public void run()
        {
          threadTestBlock();
        }
      };
      Thread t = new Thread(r);
      threads.add(t);
      t.start();
    }


    Iterator<Thread> iter = threads.iterator();
    while(iter.hasNext())
    {
      try {
        iter.next().join();
      }
      catch (InterruptedException ex) {
        ex.printStackTrace();
      }
    }
  }

  private void threadTestBlock()
  {
    ThreadPool pool = new ThreadPool(10, "test");

    ArrayList<BlockedTask> blockedTasks = new ArrayList<BlockedTask>();
    for(int i = 0; i < 50; i++)
    {
      BlockedTask task = new BlockedTask();
      blockedTasks.add(task);
      pool.runTask(task);
    }

    pool.waitForAll();

    Iterator<BlockedTask> iter = blockedTasks.iterator();
    while(iter.hasNext())
    {
      BlockedTask task = iter.next();
      assertTrue(task.isDone());
    }

    pool.shutDown();

  }



}

class Task implements Runnable
{
  private boolean done = false;

  public synchronized boolean isDone()
  {
    return done;
  }

  public void run()
  {
	try 
	{
		Thread.sleep(0,1);
	} catch (InterruptedException e) 
	{
		e.printStackTrace();
	}
    done = true;
  }
}

class BlockedTask extends Task
{
  public void run()
  {
    synchronized(this)
    {
      try
      {

        wait(400);
      }
      catch(InterruptedException ie) {}
      super.run();
    }
  }

}
