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

import games.strategy.engine.message.HubInvocationResults;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.SpokeInvocationResults;
import games.strategy.engine.message.SpokeInvoke;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.INode;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;
import games.strategy.net.nio.QuarantineConversation.ACTION;

import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 
 * A thread to Decode messages from a reader.
 * 
 * @author sgb
 */
public class Decoder
{
    
    private static final Logger s_logger = Logger.getLogger(Decoder.class.getName());

    
    private final NIOReader m_reader;
    private volatile boolean m_running = true;
    private final IErrorReporter m_errorReporter;
    private final IObjectStreamFactory m_objectStreamFactory;
    private final NIOSocket m_nioSocket;
    
    
    
    /**
     * These sockets are quarantined.  They have not logged in, and messages
     * read from them are not passed outside of the quarantine conversation.
     */
    private ConcurrentHashMap<SocketChannel, QuarantineConversation> m_quarantine = new ConcurrentHashMap<SocketChannel, QuarantineConversation>();
    
    private final Thread m_thread;
    
    public Decoder( NIOSocket nioSocket,final NIOReader reader, IErrorReporter reporter, IObjectStreamFactory objectStreamFactory,String threadSuffix)
    {
        m_reader = reader;
        m_errorReporter = reporter;
        m_objectStreamFactory = objectStreamFactory;
        m_nioSocket = nioSocket;
        
        m_thread = new Thread(new Runnable()
        {
        
            public void run()
            {
                loop();
            }

            
        }, "Decoder -" + threadSuffix);
        m_thread.start();
    }


    public void shutDown()
    {
        m_running = false;
        m_thread.interrupt();
    }


    private void loop()
    {
        while(m_running)
        {
            try
            {
                SocketReadData data;
                try
                {
                    data = m_reader.take();
                } catch (InterruptedException e)
                {
                    continue;
                }
                
                if(data == null || !m_running)
                    continue;
                
                if(s_logger.isLoggable(Level.FINEST))
                {
                    s_logger.finest("Decoding packet:" + data);
                }
                
                ByteArrayInputStream stream = new ByteArrayInputStream(data.getData());
                
                
                try
                {
                    MessageHeader header = readMessageHeader(data.getChannel(), m_objectStreamFactory.create(stream));
                    
                    if (s_logger.isLoggable(Level.FINEST))
                    {
                        s_logger.log(Level.FINEST, "header decoded:" + header);
                    }
                    
                    //make sure we are still open
                    Socket s = data.getChannel().socket();
                    if(!m_running || s == null || s.isInputShutdown())
                        continue;
                    
                    QuarantineConversation converstation = m_quarantine.get(data.getChannel());
                    if(converstation != null)
                    {
                        sendQuarantine(data.getChannel(), converstation, header);
                    }
                    else
                    {
                        if(m_nioSocket.getLocalNode() == null)
                            throw new IllegalStateException("we are writing messages, but no local node");
                        
                        if(header.getFrom() == null)
                            throw new IllegalArgumentException("Null from:" + header);
                        
                        if (s_logger.isLoggable(Level.FINER))
                        {
                            s_logger.log(Level.FINER, "decoded  msg:" + header.getMessage() + " size:" + data.size());
                        }

                        
                        m_nioSocket.messageReceived(header, data.getChannel());
                    }
                } catch(Exception ioe)
                {
                    //we are reading from memory here
                    //there should be no network errors, something
                    //is odd
                    s_logger.log(Level.SEVERE, "error reading object", ioe);
                    m_errorReporter.error(data.getChannel(), ioe);
                }
            }
            catch(Exception e)
            {
                //catch unhandles exceptions to that the decoder
                //thread doesnt die
                s_logger.log(Level.WARNING, "error in decoder", e);
            }

        }
    }


    private void sendQuarantine(SocketChannel channel,QuarantineConversation conversation, MessageHeader header)
    {
        ACTION a = conversation.message(header.getMessage());
        if(a == ACTION.TERMINATE)
        {
            if (s_logger.isLoggable(Level.FINER))
            {
                s_logger.log(Level.FINER, "Terminating quarantined connection to:" + channel.socket().getRemoteSocketAddress());
            }
            
            conversation.close();
            //we need to indicate the channel was closed
            m_errorReporter.error(channel, new CouldNotLogInException());
        }
        else if(a == ACTION.UNQUARANTINE)
        {
            if (s_logger.isLoggable(Level.FINER))
            {
                s_logger.log(Level.FINER, "Accepting quarantined connection to:" + channel.socket().getRemoteSocketAddress());
            }
            
            
            m_nioSocket.unquarantine(channel, conversation);
            m_quarantine.remove(channel);
        }        
    }


    private MessageHeader readMessageHeader(SocketChannel channel, ObjectInputStream objectInput) throws IOException, ClassNotFoundException
    {
          
        INode to;
        if(objectInput.read() == 1)
        {
            to = null;
        }
        else
        {
            if(objectInput.read() == 1)
            {
                //this may be null if we
                //have not yet fully joined the network
                to = m_nioSocket.getLocalNode();
            }
            else
            {
                to = new Node();
                ((Node)to).readExternal(objectInput);
            }
        }
        
        
        INode from;
        int readMark = objectInput.read();
        if(readMark == 1)
        {
            from = m_nioSocket.getRemoteNode(channel);
        }
        else if(readMark == 2)
        {
            from = null;
        }
        else
        {
            from = new Node();
            ((Node)from).readExternal(objectInput);
        }
        
        Serializable message;
        byte type = (byte) objectInput.read();
        if(type != Byte.MAX_VALUE)
        {
            Externalizable template = getTemplate(type);
            template.readExternal(objectInput);
            message = template;
        }
        else
        {
            message = (Serializable) objectInput.readObject();
        }
        return new MessageHeader(to,from,message);
    }
    
    public static Externalizable getTemplate(byte type)
    {
        switch (type)
        {
        case 1:
            return new HubInvoke();
        case 2:
            return new SpokeInvoke();
        case 3:
            return new HubInvocationResults();
        case 4:
            return new SpokeInvocationResults();
        default:
            throw new IllegalStateException("not recognized, " + type);
        }
    
    }
    
    /**
     * Most messages we pass will be one of the types below
     * since each of these is externalizable, we can 
     * reduce network traffic considerably by skipping the 
     * writing of the full identifiers, and simply write a single
     * byte to show the type. 
     */
    public static byte getType(Object msg)
    {
        if(msg instanceof HubInvoke)
            return 1;
        else if(msg instanceof SpokeInvoke)
            return 2;
        else if(msg instanceof HubInvocationResults)
            return 3;
        else if(msg instanceof SpokeInvocationResults)
            return 4;
    
        return Byte.MAX_VALUE;
        
    }
    
    public void add(SocketChannel channel, QuarantineConversation conversation)
    {
        m_quarantine.put(channel, conversation);
    }


    public void closed(SocketChannel channel)
    {
        //remove if it exists
        QuarantineConversation conversation = m_quarantine.remove(channel);
        if(conversation != null)
            conversation.close();
        
    }
    
    
}
