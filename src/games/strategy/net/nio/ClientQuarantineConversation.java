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


import games.strategy.net.IConnectionLogin;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientQuarantineConversation extends QuarantineConversation 
{
    
    private static final Logger s_logger = Logger.getLogger(ClientQuarantineConversation.class.getName());
    
    private enum STEP {READ_CHALLENGE, READ_ERROR,  READ_NAMES, READ_ADDRESS};

    private final IConnectionLogin m_login;
    private final SocketChannel m_channel;
    private final NIOSocket m_socket;
    private STEP m_step = STEP.READ_CHALLENGE;
    private String m_localName;
    private String m_serverName;
    private InetSocketAddress m_networkVisibleAddress;
    private InetSocketAddress m_serverLocalAddress;
    
    
    private Map<String,String> m_challengeProperties;
    private Map<String,String> m_challengeResponse;
    
    private final CountDownLatch m_showLatch = new CountDownLatch(1);
    private final CountDownLatch m_doneShowLatch = new CountDownLatch(1);
    
    private volatile boolean m_closed = false;
    
    private volatile String m_errorMessage;
    
    public ClientQuarantineConversation(final IConnectionLogin login, final SocketChannel channel, final NIOSocket socket, String localName)
    {
        m_login = login;
        m_localName = localName;
        
        m_socket = socket;
        m_channel = channel;
        
        //end the local name
        send(m_localName);
        
    }

    public String getLocalName()
    {
        return m_localName;
    }
    
    public String getErrorMessage()
    {
        return m_errorMessage;
    }


    public String getServerName()
    {
        return m_serverName;
    }

    
    public void showCredentials()
    {
        /**
         * We need to do this in the thread that created the socket, since
         * the thread that creates the socket will often be, or will block the
         * swing event thread, but the getting of a username/password
         * must be done in the swing event thread.
         * 
         * So we have complex code to switch back and forth.
         */
        
        try
        {
            m_showLatch.await();
        } catch (InterruptedException e)
        {}
        
        if(m_login != null && m_challengeProperties != null)
        {
            try
            {
                if(m_closed)
                    return;
                m_challengeResponse = m_login.getProperties(m_challengeProperties);
            }
            finally
            {
                m_doneShowLatch.countDown();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public ACTION message(Object o)
    {
        try
        {
            switch(m_step)
            {
                case  READ_CHALLENGE :
                    //read name, send challenge
                    Map<String,String> challenge = (Map) o;
                    
                    if (s_logger.isLoggable(Level.FINER))
                    {
                        s_logger.log(Level.FINER, "read challenge:" + challenge);
                    }
                    
                    if(challenge != null)
                    {
                        
                        m_challengeProperties = challenge;
                        m_showLatch.countDown();
                        
                        try
                        {
                            m_doneShowLatch.await();
                        } catch (InterruptedException e)
                        {
                            //ignore
                        }
                        
                        if(m_closed)
                            return ACTION.NONE;
                        
                        
                        if (s_logger.isLoggable(Level.FINER))
                        {
                            s_logger.log(Level.FINER, "writing response" + m_challengeResponse);
                        }
                        
                        send((Serializable) m_challengeResponse);
                    }
                    else
                    {
                        m_showLatch.countDown();
                        if (s_logger.isLoggable(Level.FINER))
                        {
                            s_logger.log(Level.FINER, "sending null response");
                        }
                        
                        send(null);
                    }
                    
                    m_step = STEP.READ_ERROR;
                    return ACTION.NONE;
                    
                case READ_ERROR :

                    
                    if(o != null)
                    {
                        if (s_logger.isLoggable(Level.FINER))
                        {
                            s_logger.log(Level.FINER, "error:" + o);
                        }
                        

                        m_errorMessage = (String) o;
                        //acknowledge the error
                        send(null);
                        
                        return ACTION.TERMINATE;
                    }
                    m_step = STEP.READ_NAMES;
                    return ACTION.NONE;
                    
                case READ_NAMES :
                    
                    
                    String[] strings = ((String[]) o);
                    if (s_logger.isLoggable(Level.FINER))
                    {
                        s_logger.log(Level.FINER, "new local name:" + strings[0]);
                    }
                    
                    m_localName = strings[0];
                    m_serverName = strings[1];
                    m_step = STEP.READ_ADDRESS;
                    return ACTION.NONE;
                    
                case READ_ADDRESS :
                    
                    //this is the adress that others see us as
                    InetSocketAddress[] address = (InetSocketAddress[]) o;
                    //this is the address the server thinks he is
                    m_networkVisibleAddress = address[0];
                    m_serverLocalAddress = address[1];
                    
                    if (s_logger.isLoggable(Level.FINE))
                    {
                        s_logger.log(Level.FINE, "Server local address:" + m_serverLocalAddress );
                        s_logger.log(Level.FINE, "channel remote address:" + m_channel.socket().getRemoteSocketAddress() );
                        s_logger.log(Level.FINE, "network visible address:" + m_networkVisibleAddress );
                        s_logger.log(Level.FINE, "channel local adresss:" + m_channel.socket().getLocalSocketAddress() );
                        
                    }
                    
                    return ACTION.UNQUARANTINE;
                     
                default :
                    throw new IllegalStateException("Invalid state");
            }
        }
        catch(Throwable t)
        {
            m_closed = true;
            m_showLatch.countDown();
            m_doneShowLatch.countDown();
            
            s_logger.log(Level.SEVERE, "error with connection", t);
            return ACTION.TERMINATE;
        }
        
    }

    private void send(Serializable object)
    {
        //this messenger is quarantined, so to and from dont matter
        MessageHeader header = new MessageHeader(Node.NULL_NODE, Node.NULL_NODE, object); 
                
        m_socket.send(m_channel, header);
    }
    
    public InetSocketAddress getNetworkVisibleSocketAdress()
    {
        return m_networkVisibleAddress;
    }

    public InetSocketAddress getServerLocalAddress()
    {
        return m_serverLocalAddress;
    }

    @Override
    public void close()
    {
        m_closed = true;
        m_showLatch.countDown();
        m_doneShowLatch.countDown();
        
    }

    

}
