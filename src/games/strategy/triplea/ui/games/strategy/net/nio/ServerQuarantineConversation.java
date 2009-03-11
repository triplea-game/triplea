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

import games.strategy.net.ILoginValidator;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;
import games.strategy.net.ServerMessenger;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerQuarantineConversation extends QuarantineConversation 
{
    
    /**
     * Communication sequence
     * 1) server reads client name
     * 2) server sends challenge (or null if no challenge is to be nade)
     * 3) server reads response (or null if no challenge)
     * 4) server send null then client name and node info on sucess, or an error message if there is an error
     * 5) if the client reads an error message, the client sends an acknowledgement (we need to make sur the client gets the message before closing the socket)
     */
    
    private static final Logger s_logger = Logger.getLogger(ServerQuarantineConversation.class.getName());
    
    private enum STEP {READ_NAME, CHALLENGE, ACK_ERROR};

    private final ILoginValidator m_validator;
    private final SocketChannel m_channel;
    private final NIOSocket m_socket;
    private STEP m_step = STEP.READ_NAME;
    private String m_remoteName;
    private Map<String, String> challenge;
    
    private final ServerMessenger m_serverMessenger;
    
    
    public ServerQuarantineConversation(final ILoginValidator validator, final SocketChannel channel, final NIOSocket socket, ServerMessenger serverMessenger)
    {
        m_validator = validator;
        
        m_socket = socket;
        m_channel = channel;
     
        m_serverMessenger = serverMessenger;
        
    }

    public String getRemoteName()
    {
        return m_remoteName;
    }


    @SuppressWarnings("unchecked")
    public ACTION message(Object o)
    {
        try
        {
            
            switch(m_step)
            {
                case  READ_NAME :
                    //read name, send challent
                    m_remoteName = (String) o;
     
                    if(s_logger.isLoggable(Level.FINER))
                    {
                        s_logger.log(Level.FINER, "read name:" + m_remoteName);
                    }
                    
                    if(m_validator != null)
                        challenge = m_validator.getChallengeProperties(m_remoteName, m_channel.socket().getRemoteSocketAddress());
                    
                    if(s_logger.isLoggable(Level.FINER))
                    {
                        s_logger.log(Level.FINER, "writing challenge:" + challenge);
                    }

                    send((Serializable) challenge);
                    m_step = STEP.CHALLENGE;
                    return ACTION.NONE;
                    
                case CHALLENGE :
                     Map<String,String> response = (Map) o;

                     if(s_logger.isLoggable(Level.FINER))
                     {
                         s_logger.log(Level.FINER,  "read challenge response:" + response);
                     }  
                                         
                     
                     if(m_validator != null)
                     {
                         String error = m_validator.verifyConnection(challenge, response, m_remoteName, m_channel.socket().getRemoteSocketAddress());
                         
                         if (s_logger.isLoggable(Level.FINER))
                         {
                             s_logger.log(Level.FINER, "error:" + error);
                         }
                         
                         send(error);

                         if(error != null)
                         {
                             m_step = STEP.ACK_ERROR;
                             return ACTION.NONE;
                         }
                     }
                     else
                     {
                         send(null);
                     }

                     //get a unique name
                     m_remoteName = m_serverMessenger.getUniqueName(m_remoteName);
                     
                     if (s_logger.isLoggable(Level.FINER))
                     {
                        s_logger.log(Level.FINER, "sending name:" + m_remoteName);
                     }
                     
                     //send the node its name and our name
                     send(new String[] {m_remoteName, m_serverMessenger.getLocalNode().getName()});
                     //send the node its and our address as we see it
                     send(new InetSocketAddress[] {(InetSocketAddress) m_channel.socket().getRemoteSocketAddress(), m_serverMessenger.getLocalNode().getSocketAddress()});
                     //we are good
                     return ACTION.UNQUARANTINE;
                     
                case ACK_ERROR :
                    return ACTION.TERMINATE;
                    
                default :
                    throw new IllegalStateException("Invalid state");
            }
        }
        catch(Throwable t)
        {
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

    @Override
    public void close()
    {
    }

    

}
