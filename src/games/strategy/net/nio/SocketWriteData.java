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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A packet of data to be written over the network.<p>
 * 
 * Packets do not correspond to ip packets.  A packet is just the data
 * for one serialized object. <p>
 *
 * The packet is written over the network as 32 bits indicating the size in bytes, then the data itself.
 * 
 * @author sgb
 */
public class SocketWriteData
{
    private static final Logger s_logger = Logger.getLogger(SocketWriteData.class.getName());
    
    private static final AtomicInteger s_counter = new AtomicInteger();

    private final ByteBuffer m_size;
    private final ByteBuffer m_content;
    private final int m_number = s_counter.incrementAndGet();
    
    //how many times we called write before we finished writing ourselves
    private int m_writeCalls = 0;
    
    
    public SocketWriteData(byte[] data, int count)
    {
        m_content = ByteBuffer.allocate(count);
        m_content.put(data, 0, count);
        m_size = ByteBuffer.allocate(4);
        m_size.putInt(count);
        m_size.flip();
        m_content.flip();
    }
    
    
    public int size()
    {
        return m_size.capacity() + m_content.capacity();
    }
    
    public int getWriteCalls()
    {
        return m_writeCalls;
    }
    
    /**
     * 
     * @return true if the write has written the entire message
     */
    public boolean write(SocketChannel channel) throws IOException 
    {
        m_writeCalls++;
        if(m_size.hasRemaining())
        {
            int count = channel.write(m_size);
            
            if(s_logger.isLoggable(Level.FINEST))
            {
                s_logger.finest("wrote size_buffer bytes:" + count);
            }

            
            //we could not write everything
            if(m_size.hasRemaining())
                return false;
        }
        
        int count = channel.write(m_content);
        
        if(s_logger.isLoggable(Level.FINEST))
        {
            s_logger.finest("wrote content bytes:" + count);
        }
        
        return !m_content.hasRemaining();
        
    }
    
    public String toString()
    {
        return "<id:" + m_number + " size:" + m_content.capacity() + ">";
    }
    
    

}
