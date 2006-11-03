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

import games.strategy.net.INode;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The threads needed for a group of sockets using NIO.
 * 
 * One thread reds socket data, one thread writes socket data
 * and one thread deserializes (decodes) packets read by the read 
 * thread.
 * 
 * serializing (encoding) objects to be written across the network is done 
 * by threads calling this object.
 * 
 * @author sgb
 */
public class NIOSocket implements IErrorReporter
{
    
    private static final Logger s_logger = Logger.getLogger(NIOSocket.class.getName());
    
    
    private final Encoder m_encoder;
    private final Decoder m_decoder;
    private final NIOWriter m_writer;
    private final NIOReader m_reader;
    private final NIOSocketListener m_listener;
    
    public NIOSocket(IObjectStreamFactory factory, NIOSocketListener listener, String name)
    {
        
        m_listener = listener;
        
        m_writer = new NIOWriter(this, name);
        m_reader = new NIOReader(this, name);
        
        m_decoder = new Decoder(this, m_reader, this, factory, name);
        m_encoder = new Encoder(this, m_writer,  factory);
    }

    
    INode getLocalNode()
    {
        return m_listener.getLocalNode(); 
    }
    
    INode getRemoteNode(SocketChannel channel)
    {
        return m_listener.getRemoteNode(channel);
    }
    
    
    
    /**
     *  Stop our threads.
     *  
     *  This does not close the sockets we are connected to.
     *
     */
    public void shutDown()
    {
        m_writer.shutDown();
        m_reader.shutDown();
        m_decoder.shutDown();
        
    }
    

    public void send(SocketChannel to, MessageHeader header)
    {
        if(to == null)
            throw new IllegalArgumentException("to cant be null!");
        if(header == null)
            throw new IllegalArgumentException("header cant be null");
        
        m_encoder.write( to, header);
        
    }
    
    
    /**
     * Add this channel.
     * 
     *  The channel will either be unquarantined, or an error will be reported
     */
    public void add(SocketChannel channel, QuarantineConversation conversation)
    {
        if(channel.isBlocking())
            throw new IllegalArgumentException("Channel is blocking");
        
        //add the decoder first, so it can quarantine the messages!
        m_decoder.add(channel, conversation);
        m_reader.add(channel);
    }


    void unquarantine(SocketChannel channel, QuarantineConversation conversation)
    {
        m_listener.socketUnqaurantined(channel, conversation);
    }


    public void error(SocketChannel channel, Exception e)
    {
        close(channel);
        m_listener.socketError(channel, e);
    }

    /**
     * Close the channel, and clean up any data associated with it
     */
    public void close(SocketChannel channel)
    {
        try
        {
            channel.close();
            channel.socket().close();
        } catch (IOException e1)
        {
           s_logger.log(Level.FINE, "error closing channel", e1);
        }

        
        m_decoder.closed(channel);
        m_writer.closed(channel);
        m_reader.closed(channel);
    }


    void messageReceived(MessageHeader header, SocketChannel channel)
    {
        m_listener.messageReceived(header, channel);
        
    }
}
