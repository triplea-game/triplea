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
/*
 * Console.java
 * 
 * Created on January 2, 2002, 5:46 PM
 */
package games.strategy.debug;

import games.strategy.engine.EngineVersion;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * 
 * @author Sean Bridges
 */
public class Console extends JFrame
{
	private static final long serialVersionUID = -3489030525309243438L;
	private static Console s_console;
	private static final ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
	
	public static Console getConsole()
	{
		if (s_console == null)
			s_console = new Console();
		return s_console;
	}
	
	public static void main(final String[] args)
	{
		final Console c = getConsole();
		c.displayStandardError();
		c.displayStandardOutput();
		c.setVisible(true);
	}
	
	private final JTextArea m_text = new JTextArea(20, 50);
	private final JToolBar m_actions = new JToolBar(SwingConstants.HORIZONTAL);
	
	/** Creates a new instance of Console */
	public Console()
	{
		super("An error has occured!");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		m_text.setLineWrap(true);
		m_text.setWrapStyleWord(true);
		final JScrollPane scroll = new JScrollPane(m_text);
		getContentPane().add(scroll, BorderLayout.CENTER);
		getContentPane().add(m_actions, BorderLayout.SOUTH);
		m_actions.setFloatable(false);
		m_actions.add(m_threadDiagnoseAction);
		m_actions.add(m_memoryAction);
		m_actions.add(m_propertiesAction);
		m_actions.add(m_copyAction);
		pack();
	}
	
	public void append(final String s)
	{
		m_text.append(s);
	}
	
	public void dumpStacks()
	{
		m_threadDiagnoseAction.actionPerformed(null);
	}
	
	public String getText()
	{
		return m_text.getText();
	}
	
	/**
	 * Displays standard error to the console
	 */
	public void displayStandardError()
	{
		final SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream(System.err);
		final ThreadReader reader = new ThreadReader(out, m_text, true);
		final Thread thread = new Thread(reader, "Console std err reader");
		thread.setDaemon(true);
		thread.start();
		final PrintStream print = new PrintStream(out);
		System.setErr(print);
	}
	
	public void displayStandardOutput()
	{
		final SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream(System.out);
		final ThreadReader reader = new ThreadReader(out, m_text, false);
		final Thread thread = new Thread(reader, "Console std out reader");
		thread.setDaemon(true);
		thread.start();
		final PrintStream print = new PrintStream(out);
		System.setOut(print);
	}
	
	private final Action m_copyAction = new AbstractAction("Copy to clipboard")
	{
		private static final long serialVersionUID = 1573097546768015070L;
		
		public void actionPerformed(final ActionEvent e)
		{
			final String text = m_text.getText();
			final StringSelection select = new StringSelection(text);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(select, select);
		}
	};
	private final AbstractAction m_threadDiagnoseAction = new AbstractAction("Enumerate Threads")
	{
		private static final long serialVersionUID = 4414139104815149199L;
		
		public void actionPerformed(final ActionEvent e)
		{
			System.out.println(getThreadDumps());
		}
	};
	private final AbstractAction m_memoryAction = new AbstractAction("Memory")
	{
		private static final long serialVersionUID = 1053036985791697566L;
		
		public void actionPerformed(final ActionEvent e)
		{
			System.gc();
			System.runFinalization();
			System.gc();
			append(getMemory());
		}
	};
	
	public static String getThreadDumps()
	{
		final StringBuilder result = new StringBuilder();
		result.append("THREAD DUMP\n");
		final ThreadInfo[] threadInfo = threadMxBean.getThreadInfo(threadMxBean.getAllThreadIds(), Integer.MAX_VALUE);
		for (final ThreadInfo info : threadInfo)
		{
			if (info != null)
			{
				result.append("thread<" + info.getThreadId() + "," + info.getThreadName() + ">\n").append("state:" + info.getThreadState()).append("\n");
				if (info.getLockName() != null)
				{
					result.append("locked on:" + info.getLockName()).append(" locked owned by:<" + info.getLockOwnerId() + "," + info.getLockOwnerName() + ">\n");
				}
				final StackTraceElement[] stackTrace = info.getStackTrace();
				for (int i = 0; i < stackTrace.length; i++)
				{
					result.append("  ");
					result.append(stackTrace[i]);
					result.append("\n");
				}
				result.append("\n");
			}
		}
		long[] deadlocks;
		try
		{
			// invoke a 1.6 method if available
			final Method m = threadMxBean.getClass().getMethod("findDeadlockedThreads");
			final Object o = m.invoke(threadMxBean);
			deadlocks = (long[]) o;
		} catch (final Throwable t)
		{
			// fall back to 1.5
			deadlocks = threadMxBean.findMonitorDeadlockedThreads();
		}
		if (deadlocks != null)
		{
			result.append("DEADLOCKS!!");
			for (final long l : deadlocks)
			{
				result.append(l).append("\n");
			}
		}
		return result.toString();
	}
	
	public static String getMemory()
	{
		System.gc();
		final StringBuilder buf = new StringBuilder("MEMORY\n");
		buf.append("****\n");
		buf.append("Total memory:" + Runtime.getRuntime().totalMemory());
		buf.append("\n");
		buf.append("Free memory:" + Runtime.getRuntime().freeMemory());
		buf.append("\n");
		buf.append("Max memory:" + Runtime.getRuntime().maxMemory());
		buf.append("\n");
		return buf.toString();
	}
	
	private final AbstractAction m_propertiesAction = new AbstractAction("Properties")
	{
		private static final long serialVersionUID = -8186358504886470902L;
		
		public void actionPerformed(final ActionEvent e)
		{
			final String s = getProperties();
			append(s);
		}
	};
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String getProperties()
	{
		final StringBuilder buf = new StringBuilder("SYSTEM PROPERTIES\n");
		final Properties props = System.getProperties();
		final java.util.List keys = new ArrayList(props.keySet());
		Collections.sort(keys);
		final Iterator iter = keys.iterator();
		while (iter.hasNext())
		{
			final String property = (String) iter.next();
			final String value = props.getProperty(property);
			buf.append(property).append(" ").append(value).append("\n");
		}
		return buf.toString();
	}
	
	public static String getDebugReport()
	{
		final StringBuilder result = new StringBuilder(500);
		result.append("CONSOLE_OUTPUT");
		result.append(Console.getConsole().getText());
		result.append("\n");
		result.append(getThreadDumps());
		result.append(getProperties());
		result.append(getMemory());
		result.append(getOpenAppWindows());
		result.append("ENGINE VERSION").append(EngineVersion.VERSION).append("\n");
		return result.toString();
	}
	
	public static String getOpenAppWindows()
	{
		final StringBuilder builder = new StringBuilder("WINDOWS\n");
		for (final Frame f : Frame.getFrames())
		{
			if (f.isVisible())
			{
				builder.append("window:").append("class " + f.getClass()).append(" size " + f.getSize()).append(" title " + f.getTitle()).append("\n");
			}
		}
		return builder.toString();
	}
}


class ThreadReader implements Runnable
{
	private final JTextArea m_text;
	private final SynchedByteArrayOutputStream m_in;
	private final boolean m_displayConsoleOnWrite;
	
	ThreadReader(final SynchedByteArrayOutputStream in, final JTextArea text, final boolean displayConsoleOnWrite)
	{
		m_in = in;
		m_text = text;
		m_displayConsoleOnWrite = displayConsoleOnWrite;
	}
	
	public void run()
	{
		while (true)
		{
			m_text.append(m_in.readFully());
			if (m_displayConsoleOnWrite)
				Console.getConsole().setVisible(true);
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
	private final Object lock = new Object();
	private final PrintStream m_mirror;
	
	SynchedByteArrayOutputStream(final PrintStream mirror)
	{
		m_mirror = mirror;
	}
	
	public void write(final byte b) throws IOException
	{
		synchronized (lock)
		{
			m_mirror.write(b);
			super.write(b);
			lock.notifyAll();
		}
	}
	
	@Override
	public void write(final byte[] b, final int off, final int len)
	{
		synchronized (lock)
		{
			super.write(b, off, len);
			m_mirror.write(b, off, len);
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
		synchronized (lock)
		{
			if (super.size() == 0)
			{
				try
				{
					lock.wait();
				} catch (final InterruptedException ie)
				{
				}
			}
			final String s = toString();
			reset();
			return s;
		}
	}
}
