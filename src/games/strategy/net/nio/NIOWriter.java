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

package games.strategy.net.nio;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * A thread that writes socket data using NIO .<br>
 * 
 * Data is written in packets that are enqued on our buffer.  
 * 
 * Packets are sent to the sockets in the order that they are received.
 * 
 * @author sgb
 */
public class NIOWriter
{
    private static final Logger s_logger = Logger.getLogger(NIOWriter.class.getName()); 

    private final Selector m_selector;
    private final IErrorReporter m_errorReporter;
    //this is the data we are writing
    private final Map<SocketChannel, List<SocketWriteData>> m_writing = new HashMap<SocketChannel, List<SocketWriteData>>();
    //these are the sockets we arent selecting on, but should now
    private List<SocketChannel> m_socketsToWake = new ArrayList<SocketChannel>();
    //the writing thread and threads adding data to write synchronize on this lock
    private final Object m_mutex = new Object();

    private long m_totalBytes = 0;
    
    private volatile boolean m_running = true;
    
    public NIOWriter(IErrorReporter reporter, String threadSuffix) 
    {
        m_errorReporter = reporter;
        try
        {
            m_selector = Selector.open();
        } catch(IOException e) 
        {
            s_logger.log(Level.SEVERE, "Could not create Selector", e);
            throw new IllegalStateException(e);
        }
        
        Thread t = new Thread(new Runnable()
        {
        
            public void run()
            {
                loop();
            }
        }, "NIO Writer - " + threadSuffix);
        
        t.start();
    }
    
    
    public void shutDown() 
    {
        m_running = false;
        try
        {
            m_selector.close();
        } catch (IOException e)
        {
            s_logger.log(Level.WARNING, "error closing selector", e);
        }
    }
    
    private void addNewSocketsToSelector()
    {
        List<SocketChannel> socketsToWriteCopy;
        synchronized(m_mutex)
        {
            if(m_socketsToWake.isEmpty())
                return;
            
            socketsToWriteCopy = m_socketsToWake;
            m_socketsToWake = new ArrayList<SocketChannel>();
        }
        
        for(SocketChannel channel : socketsToWriteCopy)
        {
            try
            {
                channel.register(m_selector, SelectionKey.OP_WRITE);                
            } catch (ClosedChannelException e)
            {
                s_logger.log(Level.FINEST, "socket already closed", e);
            }
            
        }
        
    }

    private void loop()
    {
        while(m_running) 
        {

            try
            {
                
                if(s_logger.isLoggable(Level.FINEST))
                {
                    s_logger.finest("selecting...");
                }
    
                
                try
                {
                    m_selector.select();
                }
                //exceptions can be thrown here, nothing we can do
                //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4729342
                catch (Exception e)
                {
                    s_logger.log(Level.INFO, "error reading selection", e);
                }
                if(!m_running)
                    continue;
                
                //select any new sockets that can be written to
                addNewSocketsToSelector();
                
                Set<SelectionKey> selected = m_selector.selectedKeys();
                
                if(s_logger.isLoggable(Level.FINEST))
                {
                    s_logger.finest("selected:" + selected.size());
                }
    
                
                
                Iterator<SelectionKey> iter = selected.iterator();
                while(iter.hasNext())
                {
                    SelectionKey key = iter.next();
                    iter.remove();
                    if(key.isValid() &&  key.isWritable()) 
                    {                    
                        
                        SocketChannel channel = (SocketChannel) key.channel();
                     
                        SocketWriteData packet = getData(channel);
                        
                        if(packet != null)
                        {
                            
                            try
                            {
                                if(s_logger.isLoggable(Level.FINEST))
                                {
                                    s_logger.finest("writing packet:" + packet + " to:" + channel.socket().getRemoteSocketAddress());
                                }
    
                                
                                boolean done = packet.write(channel);
                                
                                if(done) 
                                {
                                    
                                    m_totalBytes += packet.size();
                                    if (s_logger.isLoggable(Level.FINE))
                                    {
                                        String remote = "null";
                                        Socket s = channel.socket();
                                        
                                        SocketAddress sa = null;
                                        if(s != null)
                                            sa = s.getRemoteSocketAddress();
                                        if(sa != null)
                                            remote = sa.toString();
                                        
                                        s_logger.log(Level.FINE, " done writing to:" + remote + " size:" + packet.size() + " writeCalls;" + packet.getWriteCalls()  + " total:" + m_totalBytes);
                                    }
                                    
                                    removeLast(channel);
                                }
                                
                            } catch (Exception e)
                            {
                                s_logger.log(Level.FINER, "exception writing",e);
                                m_errorReporter.error(channel, e);
                                key.cancel();
                            }
                        }
                        else
                        {
                            //nothing to write
                            //cancel the key, otherwise we will
                            //spin forever as the socket will always be writable
                            key.cancel();
                        }
                    }
                }
            }
            catch(Exception e)
            {
                //catch unhandles exceptions to that the writer
                //thread doesnt die
                s_logger.log(Level.WARNING, "error in writer", e);
            }

            
        }
        
    }

    /**
     * Remove the data for this channel
     */
    public void closed(SocketChannel channel)
    {
        removeAll(channel);
    }
    
    private void removeAll(SocketChannel to)
    {
        synchronized(m_mutex)
        {
            m_writing.remove(to);
        }
    }
    
    private void removeLast(SocketChannel to)
    {
        synchronized(m_mutex)
        {
            List<SocketWriteData> values = m_writing.get(to);
            if(values == null)
            {
                s_logger.log(Level.SEVERE, "NO socket data to:" + to + " all:" + values);
                return;
            }
            
            values.remove(0);
            //remove empty lists, so we can detect that we need to wake up the socket
            if(values.isEmpty())
                m_writing.remove(to);
        }
    }
    
    private SocketWriteData getData(SocketChannel to) 
    {
        synchronized(m_mutex)
        {
            if(!m_writing.containsKey(to))
                return null;
            List<SocketWriteData> values = m_writing.get(to);
            if(values.isEmpty())
                return null;
            return values.get(0);
        }
    }
    
    public void enque(SocketWriteData data, SocketChannel channel)
    {
        synchronized(m_mutex)
        {
            if(!m_running)
                return;
            
            if(m_writing.containsKey(channel))
            {
                m_writing.get(channel).add(data);
            }
            else
            {
                List<SocketWriteData> values = new ArrayList<SocketWriteData>();
                values.add(data);
                m_writing.put(channel, values);
                m_socketsToWake.add(channel);
                m_selector.wakeup();
            }
        }
    }
    
    
    
    
    
    
    
}
