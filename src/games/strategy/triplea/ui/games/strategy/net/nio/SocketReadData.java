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
 * A packet of data being read over the network.<p>
 * 
 * A Packet does not correspond to a network packet, rather
 * it is the bytes for 1 serialized java object.<p>
 *  
 * @author sgb
 */
class SocketReadData
{
    public static final int MAX_MESSAGE_SIZE = 1000 * 1000 * 10;

    private static final Logger s_logger = Logger.getLogger(SocketReadData.class.getName());
    
    private static final AtomicInteger s_counter = new AtomicInteger();

    //as a sanity check to make sure
    //we are talking to another tripea instance
    //that the upper bits of the packet
    //size we send is 0x9b
    public static final int MAGIC = 0x9b000000;
    
    private int m_targetSize = -1;
    //we read into here the first four
    //bytes to find out size
    private ByteBuffer m_sizeBuffer;
    //we read into here after knowing out size
    private ByteBuffer m_contentBuffer;
    private final SocketChannel m_channel;
    
    private final int m_number = s_counter.incrementAndGet();

    private int m_readCalls;
    
    public SocketReadData(final SocketChannel channel)
    {
        this.m_channel = channel;
    }

    /**
     * Read data from the channel, returning true if this packet is done. <p>
     * 
     * If we detect the socket is closed, we will throw an IOExcpetion
     */
    public boolean read(SocketChannel channel) throws IOException
    {
        m_readCalls++;
        
        //we dont know our size, read it
        if(m_targetSize < 0) 
        {
            //our first read
            //find out how big this packet is
            if(m_sizeBuffer == null) 
            {
                m_sizeBuffer = ByteBuffer.allocate(4);                
            }
            
            int size = channel.read(m_sizeBuffer);

            if(s_logger.isLoggable(Level.FINEST))
            {
                s_logger.finest("read size_buffer bytes:" + size);
            }
            
            if(size == -1)
                throw new IOException("Socket closed");
            
            //we have read all four bytes of our size
            if(!m_sizeBuffer.hasRemaining())
            {
                m_sizeBuffer.flip();
                m_targetSize = m_sizeBuffer.getInt();
                
                if((m_targetSize & 0xFF000000) != MAGIC)
                    throw new IOException("Did not write magic!");
                m_targetSize = m_targetSize & 0x00ffffff;
                
                //limit messages to 10MB 
                if(m_targetSize <= 0 || m_targetSize > MAX_MESSAGE_SIZE) 
                {
                    throw new IOException("Invalid triplea packet size:" + m_targetSize);
                }
                
                m_contentBuffer = ByteBuffer.allocate(m_targetSize);
                m_sizeBuffer = null;
            }
            else
            {
                //we ddnt read all 4 bytes, return
                return false;
            }
        }
        
        //http://javaalmanac.com/egs/java.nio/DetectClosed.html
        int size = channel.read(m_contentBuffer);
        
        if(s_logger.isLoggable(Level.FINEST))
        {
            s_logger.finest("read content bytes:" + size);
        }
        
        if(size == -1)
            throw new IOException("Socket closed");

        return !m_contentBuffer.hasRemaining();
    }
    
    public SocketChannel getChannel()
    {
        return m_channel;
    }

    /**
     * Get the data as a byte[].
     * 
     * This method can only be called once.
     *
     */
    public byte[] getData()
    {
        byte[] rVal = new byte[m_contentBuffer.capacity()];
        m_contentBuffer.flip();
        m_contentBuffer.get(rVal);
        m_contentBuffer = null;
        return rVal;
    }
    
    public int size()
    {
        //add 4 to count the bytes used to send our size
        return m_targetSize + 4;
    }
    
    public int getReadCalls()
    {
        return m_readCalls;
    }

    
    public String toString()
    {
        return "<id:" + m_number + " size:" + m_targetSize + ">";
    }
    
    
    
    
}