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
		System.out.println("running");
		ThreadPool pool = new ThreadPool(50);
		Task task = new Task();
		pool.runTask(task);
		pool.waitForAll();
		assertTrue(task.isDone());
	}

	public void testSingleThread()
	{
		ThreadPool pool = new ThreadPool(1);
		Collection tasks = new ArrayList();
		
		for(int i = 0; i < 30; i++)
		{
			Runnable task = new Task();
			tasks.add(task);
			pool.runTask(task);			
		
		}
		
		pool.waitForAll();
		
		Iterator iter = tasks.iterator();
		while(iter.hasNext())
		{
			assertTrue( ((Task) iter.next()).isDone());
		}
		pool.shutDown();
	}
	
	
	public void testSimple()
	{
		ThreadPool pool = new ThreadPool(5);
		Collection tasks = new ArrayList();
		
		for(int i = 0; i < 30; i++)
		{
			Runnable task = new Task();
			tasks.add(task);
			pool.runTask(task);
		}
		
		pool.waitForAll();
		
		Iterator iter = tasks.iterator();
		while(iter.hasNext())
		{
			assertTrue( ((Task) iter.next()).isDone());
		}
		pool.shutDown();
	}
	
	public void testBlocked()
	{
		ThreadPool pool = new ThreadPool(10);
		
		ArrayList blockedTasks = new ArrayList();
		for(int i = 0; i < 30; i++)
		{
			BlockedTask task = new BlockedTask();
			blockedTasks.add(task);
			pool.runTask(task);
		}

		pool.waitForAll();
		
		Iterator iter = blockedTasks.iterator();
		while(iter.hasNext())
		{
			BlockedTask task = (BlockedTask) iter.next();
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
				
				wait( (int) (Math.random() * 400));
			}
			catch(InterruptedException ie) {}
			super.run();
		}
	}
	
}
