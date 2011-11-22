package games.strategy.triplea.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingPrintStream extends PrintStream
{
	public LoggingPrintStream(final String loggerName, final Level level)
	{
		super(new LoggingOutputStream(loggerName, level));
	}
}


class LoggingOutputStream extends ByteArrayOutputStream
{
	private final Object m_lock = new Object();
	private final Logger m_logger;
	private final Level m_level;
	
	public LoggingOutputStream(final String name, final Level level)
	{
		m_logger = Logger.getLogger(name);
		m_level = level;
	}
	
	@Override
	public void write(final int b)
	{
		synchronized (m_lock)
		{
			super.write(b);
			dump();
		}
	}
	
	@Override
	public void write(final byte[] b, final int off, final int len)
	{
		synchronized (m_lock)
		{
			super.write(b, off, len);
			dump();
		}
	}
	
	private void dump()
	{
		final String content = toString();
		if (content.indexOf("\n") != -1)
		{
			reset();
			m_logger.log(m_level, content);
		}
	}
}
