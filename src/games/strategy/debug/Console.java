/*
 * Console.java
 *
 * Created on January 2, 2002, 5:46 PM
 */

package games.strategy.debug;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author  Sean Bridges
 */
public class Console extends JFrame
{
	
	private static Console s_console;
	
	public static Console getConsole()
	{
		if(s_console == null)
			s_console = new Console();
		return s_console;
	}
	
	public static void main(String[] args)
	{
		Console c = getConsole();
		c.displayStandardError();
		c.displayStandardOutput();
		c.show();
	}
	
	private JTextArea m_text = new JTextArea(20,50); 
	private JToolBar m_actions = new JToolBar(JToolBar.HORIZONTAL);
	
	/** Creates a new instance of Console */
    public Console() 
	{
		super("Console");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		getContentPane().setLayout(new BorderLayout());
		
		m_text.setLineWrap(true);
		m_text.setWrapStyleWord(true);
		JScrollPane scroll = new JScrollPane(m_text);
		getContentPane().add(scroll, BorderLayout.CENTER);
		
		getContentPane().add(m_actions, BorderLayout.SOUTH);
		
		m_actions.setFloatable(false);
		m_actions.add(m_threadDiagnoseAction);
		m_actions.add(m_memoryAction);
		m_actions.add(m_propertiesAction);
		
		pack();
    }
	
	public void append(String s)
	{
		m_text.append(s);
	}
	
	
	/**
	 * Displays standard error to the console
	 */
	public void displayStandardError()
	{
		SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream();
		ThreadReader reader = new ThreadReader(out, m_text);
		Thread thread = new Thread(reader, "Console std err reader");
		thread.start();
		
		PrintStream print = new PrintStream(out);
		System.setErr(print);
	}
	
	public void displayStandardOutput()
	{
		SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream();
		ThreadReader reader = new ThreadReader(out, m_text);
		Thread thread = new Thread(reader, "Console std out reader");
		thread.start();
		
		PrintStream print = new PrintStream(out);
		System.setOut(print);
	}
	
	private AbstractAction m_threadDiagnoseAction = new AbstractAction("Enumerate Threads")
	{
		public void actionPerformed(ActionEvent e)
		{
			Thread[] threads = new Thread[ Thread.activeCount()];
			int count = Thread.enumerate(threads);
			
			StringBuffer buf = new StringBuffer();
			buf.append("**\n");
			for(int i = 0; i < count; i++)
			{
				appendThreadInfo(buf, threads[i]);
			}
			append(buf.toString());
		
		}
		
		private void appendThreadInfo(StringBuffer buf, Thread thread)
		{
			buf.append("Name:").append(thread.getName()).append("\n");
			buf.append("Priority:").append(thread.getPriority()).append("\n");
			buf.append("Alive:").append(thread.isAlive()).append("\n");
			buf.append("\n");
		}
	};
	

	private AbstractAction m_memoryAction = new AbstractAction("Memory")
	{
		public void actionPerformed(ActionEvent e)
		{
			StringBuffer buf = new StringBuffer();
			buf.append("****\n");
			buf.append("Total memory:" + Runtime.getRuntime().totalMemory());
			buf.append("\n");
			buf.append("Free memory:" + Runtime.getRuntime().freeMemory());
			buf.append("\n");
			append(buf.toString());
		}
	};
	
	private AbstractAction m_propertiesAction = new AbstractAction("Properties")
	{
		public void actionPerformed(ActionEvent e)
		{
			StringBuffer buf = new StringBuffer();
			Properties props = System.getProperties();
			java.util.List keys = new ArrayList(props.keySet());
			
			Collections.sort(keys);
			
			Iterator iter = keys.iterator();
			while(iter.hasNext())
			{
				String property = (String) iter.next();
				String value = props.getProperty(property);
				buf.append(property).append(" ").append(value).append("\n"); 
			}
			
			append(buf.toString());
		}
	};


}


class ThreadReader implements Runnable
{
	private JTextArea m_text;
	private SynchedByteArrayOutputStream m_in;
	
	ThreadReader(SynchedByteArrayOutputStream in, JTextArea text)
	{
		m_in = in;
		m_text = text;
	}
	
	public void run()
	{
		while(true)
		{
			m_text.append(m_in.readFully());
		}
	}
}

/**
 * Allows data written to a byte output stream to be read
 * safely friom a seperate thread.
 *
 * Only readFully() is currently threadSafe for reading.
 * 
 */
class SynchedByteArrayOutputStream extends ByteArrayOutputStream
{
	private Object lock = new Object();
	
	public void write(byte b) throws IOException
	{
		synchronized(lock)
		{
			super.write(b);
			lock.notifyAll();
		}
	}
	
	public void write(byte[] b, int off, int len) 
	{
		synchronized(lock)
		{
			super.write(b, off, len);
			lock.notifyAll();
		}
	}

	
	/** 
	 * Read all data written to the stream. 
	 * Blocks until data is available.
	 * This is currently the only threadsafe method for reading.
	 */
	public String readFully()
	{
		synchronized(lock)
		{
			if(super.size() == 0)
			{
				try
				{
					lock.wait();
				} catch(InterruptedException ie)
				{}
			}
			String s = toString();
			reset();
			return s;
			
		}
	}
}