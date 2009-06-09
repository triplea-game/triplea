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
 * Console.java
 *
 * Created on January 2, 2002, 5:46 PM
 */

package games.strategy.debug;

import games.strategy.engine.EngineVersion;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Method;
import java.util.*;

import javax.swing.*;

/**
 *
 * @author  Sean Bridges
 */
public class Console extends JFrame
{
	
	private static Console s_console;
    
    
     private static final ThreadMXBean threadMxBean =
          ManagementFactory.getThreadMXBean();
	
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
		c.setVisible(true);
	}
	
	private JTextArea m_text = new JTextArea(20,50); 
	private JToolBar m_actions = new JToolBar(SwingConstants.HORIZONTAL);
	
	/** Creates a new instance of Console */
    public Console() 
	{
		super("An error has occured!");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
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
		m_actions.add(m_copyAction);
		
		pack();
    }
	
	public void append(String s)
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
		SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream(System.err);
		ThreadReader reader = new ThreadReader(out, m_text, true);
		Thread thread = new Thread(reader, "Console std err reader");
		thread.setDaemon(true);
		thread.start();
		
		PrintStream print = new PrintStream(out);
		System.setErr(print);
	}
	
	public void displayStandardOutput()
	{
		SynchedByteArrayOutputStream out = new SynchedByteArrayOutputStream(System.out);
		ThreadReader reader = new ThreadReader(out, m_text, false);
		Thread thread = new Thread(reader, "Console std out reader");
		thread.setDaemon(true);
		thread.start();
		
		PrintStream print = new PrintStream(out);
		System.setOut(print);
	}
	
	private Action m_copyAction = new AbstractAction("Copy to clipboard")
	{
	  public void actionPerformed(ActionEvent e)
	  {
	      String text = m_text.getText();
	      StringSelection select = new StringSelection(text);
	      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(select, select);
	  }
	};
	
	private AbstractAction m_threadDiagnoseAction = new AbstractAction("Enumerate Threads")
	{
		public void actionPerformed(ActionEvent e)
		{
		    System.out.println(getThreadDumps());
		}
		
	};
	

	private AbstractAction m_memoryAction = new AbstractAction("Memory")
	{
		public void actionPerformed(ActionEvent e)
		{
            System.gc();
            System.runFinalization();
            System.gc();
            
			append(getMemory());
		}

       
	};
    
    public static String getThreadDumps() 
    {
        StringBuilder result = new StringBuilder();
        result.append("THREAD DUMP\n");
    
        ThreadInfo[] threadInfo = threadMxBean.getThreadInfo(threadMxBean.getAllThreadIds(), Integer.MAX_VALUE);
        
        
        for(ThreadInfo info : threadInfo) 
        {
            if(info != null)
            {
                result.append("thread<" +info.getThreadId() + ","  + info.getThreadName() + ">\n")
                .append("state:" + info.getThreadState())
                .append("\n");
                
                if(info.getLockName() != null) 
                {
                    result.append("locked on:" + info.getLockName()).
                    append(" locked owned by:<" + info.getLockOwnerId() + "," + info.getLockOwnerName() +">\n");    
                }
                
                StackTraceElement[] stackTrace = info.getStackTrace();
                for(int i = 0;  i < stackTrace.length; i++)
                {
                    result.append("  ");
                    result.append(stackTrace[i]);
                    result.append("\n");
                }
                result.append("\n");
            }
        }
        
        long[] deadlocks;
        try {
            //invoke a 1.6 method if available
            Method m = threadMxBean.getClass().getMethod("findDeadlockedThreads");
            Object o = m.invoke(threadMxBean);
            deadlocks = (long[]) o;
        } catch(Throwable t) {
            //fall back to 1.5
            deadlocks = threadMxBean.findMonitorDeadlockedThreads();
        }
        
        if(deadlocks != null) 
        {
            result.append("DEADLOCKS!!");
            for(long l : deadlocks) 
            {
                result.append(l).append("\n");
            }
        }
        
        return result.toString();
    }
    
    
     public static String getMemory()
     {
         System.gc();
         StringBuilder buf = new StringBuilder("MEMORY\n");
            buf.append("****\n");
            buf.append("Total memory:" + Runtime.getRuntime().totalMemory());
            buf.append("\n");
            buf.append("Free memory:" + Runtime.getRuntime().freeMemory());
            buf.append("\n");
         buf.append("Max memory:" + Runtime.getRuntime().maxMemory());
         buf.append("\n");
         return buf.toString();
     }
	
   
	private AbstractAction m_propertiesAction = new AbstractAction("Properties")
	{
		
        public void actionPerformed(ActionEvent e)
		{
			String s = getProperties();
			append(s);
		}
	};
    
    @SuppressWarnings("unchecked")
    public static String getProperties()
    {
        StringBuilder buf = new StringBuilder("SYSTEM PROPERTIES\n");
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
        
        return buf.toString();
    }
    
    
    public static String getDebugReport() 
    {
        StringBuilder result = new StringBuilder(500);
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
        StringBuilder builder = new StringBuilder("WINDOWS\n");
        
        for(Frame f : Frame.getFrames()) 
        {
            if(f.isVisible()) 
            {
                builder.append("window:").
                append("class " + f.getClass()).
                append(" size " + f.getSize()).
                append(" title " +f.getTitle()).
                append("\n");
            }           
        }
        
        return builder.toString();
    }
    

}


class ThreadReader implements Runnable
{
	private JTextArea m_text;
	private SynchedByteArrayOutputStream m_in;
	private boolean m_displayConsoleOnWrite; 
	
	ThreadReader(SynchedByteArrayOutputStream in, JTextArea text, boolean displayConsoleOnWrite)
	{
		m_in = in;
		m_text = text;
		m_displayConsoleOnWrite = displayConsoleOnWrite;
	}
	
	public void run()
	{
		while(true)
		{
			m_text.append(m_in.readFully());
			if(m_displayConsoleOnWrite)
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
	private Object lock = new Object();
	private PrintStream m_mirror;
	
	SynchedByteArrayOutputStream(PrintStream mirror)
	{
	    m_mirror = mirror;
	}
	
	public void write(byte b) throws IOException
	{
		synchronized(lock)
		{
		    m_mirror.write(b);
			super.write(b);
			lock.notifyAll();
		}
	}
	
	public void write(byte[] b, int off, int len) 
	{
		synchronized(lock)
		{
			super.write(b, off, len);
			m_mirror.write(b,off,len);
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
