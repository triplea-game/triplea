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

import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Encodes data to be written by a writer
 * 
 * @author sgb
 */
public class Encoder
{
    private static final Logger s_logger = Logger.getLogger(Encoder.class.getName());
    
    
    private final NIOWriter m_writer;
    private final IObjectStreamFactory m_objectStreamFactory;
    private final NIOSocket m_nioSocket;
    
    
    public Encoder(final NIOSocket nioSocket, final NIOWriter writer, final IObjectStreamFactory objectStreamFactory)
    {
        m_nioSocket = nioSocket;
        m_writer = writer;
        m_objectStreamFactory = objectStreamFactory;
    }


    public void write(final SocketChannel to, final MessageHeader header)
    {
        
        if (s_logger.isLoggable(Level.FINEST))
        {
            s_logger.log(Level.FINEST, "Encoding msg:" + header + " to:" + to);
        }
        
        if(header.getFrom() == null)
            throw new IllegalArgumentException("No from node");
        if(to == null)
            throw new IllegalArgumentException("No to channel!");
        
        ByteArrayOutputStream2 sink = new ByteArrayOutputStream2(512);
        
        SocketWriteData data;
        try
        {
            write(header, m_objectStreamFactory.create(sink), to);
            data = new SocketWriteData(sink.getBuffer(), sink.size());
        
        }
        catch(Exception e)
        {
            //we arent doing any io, just writing in memory
            //so something is very wrong
            s_logger.log(Level.SEVERE, "Error writing object:" + header, e);
            return;
        }
        
        if (s_logger.isLoggable(Level.FINER))
        {
            s_logger.log(Level.FINER, "encoded  msg:" + header.getMessage() + " size:" + data.size());
        }
        
        m_writer.enque(data, to);
    }
    
    
    private void write(MessageHeader header, ObjectOutputStream out, SocketChannel remote) throws IOException
    {
        if(header.getFrom() == null)
            throw new IllegalArgumentException("null from");
        
        //a broadcast
        if(header.getFor() == null)
        {
            out.write(1);
        }
        else
        {
            //to a node
            out.write(0);
            //the common case, skip writing the address
            if(header.getFor().equals(m_nioSocket.getRemoteNode(remote)))
            {
                out.write(1);
            }
            else
            {
                //this message is going to be relayed, write the destination
                out.write(0);
                ((Node) header.getFor() ).writeExternal(out);
            }
        }
        
        
        if(header.getFrom().equals(m_nioSocket.getLocalNode()))
        {
            out.write(1);
        }
        else if(m_nioSocket.getLocalNode() == null)
        {
            out.write(2);
        }
        else
        {
            out.write(0);
            ((Node) header.getFrom()).writeExternal(out);
        }
        
        
        byte type =  Decoder.getType(header.getMessage());
        out.write(type);
        if(type != Byte.MAX_VALUE)
        {
            ((Externalizable) header.getMessage()).writeExternal(out);
        }
        else
        {
            out.writeObject(header.getMessage());    
        }
        out.reset();
    }
    
    
    



}
