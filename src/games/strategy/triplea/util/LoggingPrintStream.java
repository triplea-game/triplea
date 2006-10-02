package games.strategy.triplea.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingPrintStream extends PrintStream
{
    
    public LoggingPrintStream(String loggerName, Level level)
    {
        super(new LoggingOutputStream(loggerName, level));
    }
    
}

class LoggingOutputStream extends ByteArrayOutputStream
{
    private final Object m_lock = new Object();
    
    private final Logger m_logger;
    private final Level m_level;
    
    public LoggingOutputStream(String name, Level level)
    {
        m_logger = Logger.getLogger(name);
        m_level = level;
    }
    
    @Override
    public void write(int b)
    {
        synchronized(m_lock)
        {
            super.write(b);    
            dump();
        }
        
    }
    
    @Override
    public void write(byte[] b, int off, int len) 
    {
        synchronized(m_lock)
        {
            super.write(b, off, len);
            dump();
        }
    }

    private void dump()
    {
        String content = toString();
        if(content.indexOf("\n") != -1)
        {
            reset();
            m_logger.log(m_level, content);
        }
    }

}

