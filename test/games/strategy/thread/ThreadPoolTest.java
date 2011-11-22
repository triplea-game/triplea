/*
 * ThreadPoolTest.java
 * 
 * Created on January 25, 2002, 3:34 PM
 */
package games.strategy.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * 
 * @author Sean Bridges
 */
public class ThreadPoolTest extends TestCase
{
	/** Creates a new instance of ThreadPoolTest */
	public ThreadPoolTest(final String s)
	{
		super(s);
	}
	
	public static Test suite()
	{
		final TestSuite suite = new TestSuite();
		suite.addTestSuite(ThreadPoolTest.class);
		return suite;
	}
	
	public void testRunOneTask()
	{
		final ThreadPool pool = new ThreadPool(50, "test");
		final Task task = new Task();
		pool.runTask(task);
		pool.waitForAll();
		assertTrue(task.isDone());
	}
	
	public void testSingleThread()
	{
		final ThreadPool pool = new ThreadPool(1, "test");
		final Collection<Runnable> tasks = new ArrayList<Runnable>();
		for (int i = 0; i < 30; i++)
		{
			final Runnable task = new Task();
			tasks.add(task);
			pool.runTask(task);
		}
		pool.waitForAll();
		final Iterator<Runnable> iter = tasks.iterator();
		while (iter.hasNext())
		{
			assertTrue(((Task) iter.next()).isDone());
		}
		pool.shutDown();
	}
	
	public void testSimple()
	{
		final ThreadPool pool = new ThreadPool(5, "test");
		final Collection<Task> tasks = new ArrayList<Task>();
		for (int i = 0; i < 3000; i++)
		{
			final Task task = new Task();
			tasks.add(task);
			pool.runTask(task);
		}
		assertEquals(5, pool.getThreadCount());
		pool.waitForAll();
		final Iterator<Task> iter = tasks.iterator();
		while (iter.hasNext())
		{
			assertTrue(iter.next().isDone());
		}
		pool.shutDown();
	}
	
	public void testBlocked()
	{
		final Collection<Thread> threads = new ArrayList<Thread>();
		for (int j = 0; j < 20; j++)
		{
			final Runnable r = new Runnable()
			{
				public void run()
				{
					threadTestBlock();
				}
			};
			final Thread t = new Thread(r);
			threads.add(t);
			t.start();
		}
		final Iterator<Thread> iter = threads.iterator();
		while (iter.hasNext())
		{
			try
			{
				iter.next().join();
			} catch (final InterruptedException ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	private void threadTestBlock()
	{
		final ThreadPool pool = new ThreadPool(10, "test");
		final ArrayList<BlockedTask> blockedTasks = new ArrayList<BlockedTask>();
		for (int i = 0; i < 50; i++)
		{
			final BlockedTask task = new BlockedTask();
			blockedTasks.add(task);
			pool.runTask(task);
		}
		pool.waitForAll();
		final Iterator<BlockedTask> iter = blockedTasks.iterator();
		while (iter.hasNext())
		{
			final BlockedTask task = iter.next();
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
			Thread.sleep(0, 1);
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		}
		done = true;
	}
}


class BlockedTask extends Task
{
	@Override
	public void run()
	{
		synchronized (this)
		{
			try
			{
				wait(400);
			} catch (final InterruptedException ie)
			{
			}
			super.run();
		}
	}
}
