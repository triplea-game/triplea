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

/*
 * ServerMessenger.java
 *
 * Created on December 11, 2001, 7:43 PM
 */

package games.strategy.net;

import games.strategy.net.nio.NIOSocket;
import games.strategy.net.nio.NIOSocketListener;
import games.strategy.net.nio.QuarantineConversation;
import games.strategy.net.nio.ServerQuarantineConversation;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Messenger that can have many clients connected to it.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class ServerMessenger implements IServerMessenger, NIOSocketListener
{
    private static Logger s_logger = Logger.getLogger(ServerMessenger.class.getName());
    
    private final Selector m_acceptorSelector;
    private final ServerSocketChannel m_socketChannel;
    private final Node m_node;
    private boolean m_shutdown = false;
    private final NIOSocket m_nioSocket;
    
    
    private final CopyOnWriteArrayList<IMessageListener> m_listeners = new CopyOnWriteArrayList<IMessageListener>();
    private final CopyOnWriteArrayList<IMessengerErrorListener> m_errorListeners = new CopyOnWriteArrayList<IMessengerErrorListener>();
    private final CopyOnWriteArrayList<IConnectionChangeListener> m_connectionListeners = new CopyOnWriteArrayList<IConnectionChangeListener>();
    
    
    private boolean m_acceptNewConnection = false;
    private ILoginValidator m_loginValidator;
    
    //all our nodes
    private ConcurrentHashMap<INode, SocketChannel> m_nodeToChannel = new ConcurrentHashMap<INode, SocketChannel>();
    private ConcurrentHashMap<SocketChannel, INode> m_channelToNode = new ConcurrentHashMap<SocketChannel, INode>();
       
    public ServerMessenger(String name, int portNumber, IObjectStreamFactory streamFactory) throws IOException
    {
        m_socketChannel = ServerSocketChannel.open();
        m_socketChannel.configureBlocking(false);
        m_socketChannel.socket().setReuseAddress(true);
        m_socketChannel.socket().bind(new InetSocketAddress(portNumber), 10);
        
        m_nioSocket = new NIOSocket(streamFactory, this, "Server");
        
        m_acceptorSelector = Selector.open();
        
        if (IPFinder.findInetAddress() != null)
            m_node = new Node(name, IPFinder.findInetAddress(), portNumber);
        else
            m_node = new Node(name, InetAddress.getLocalHost(), portNumber);

        
        Thread t = new Thread(new ConnectionHandler(), "Server Messenger Connection Handler");
        t.start();
    }
    
    public void setLoginValidator(ILoginValidator loginValidator)
    {
        m_loginValidator = loginValidator;
    }
    
    public ILoginValidator getLoginValidator()
    {
        return m_loginValidator;
    }

    /** Creates new ServerMessenger */
    public ServerMessenger(String name, int portNumber) throws IOException
    {
        this(name, portNumber, new DefaultObjectStreamFactory());
    }

    /*
     * @see IMessenger#addMessageListener(Class, IMessageListener)
     */
    public void addMessageListener(IMessageListener listener)
    {
        m_listeners.add(listener);
    }

    /*
     * @see IMessenger#removeMessageListener(Class, IMessageListener)
     */
    public void removeMessageListener(IMessageListener listener)
    {
        m_listeners.remove(listener);
    }

    /**
     * Get a list of nodes.
     */
    public Set<INode> getNodes()
    {
        
        Set<INode> rVal = new HashSet<INode>(m_nodeToChannel.keySet());
        rVal.add(m_node);
        return rVal;
        
    }

    public synchronized void shutDown()
    {
        
        if (!m_shutdown)
        {
            m_shutdown = true;

            m_nioSocket.shutDown();

            try
            {
                m_socketChannel.close();
            } catch (Exception e)
            {
                //ignore
            }

            if(m_acceptorSelector != null)
                m_acceptorSelector.wakeup();
        }
    }

    public boolean isConnected()
    {
        return !m_shutdown;
    }

    /**
     * Send a message to the given node.
     */
    public void send(Serializable msg, INode to)
    {
        if(m_shutdown)
            return;
        
        if (s_logger.isLoggable(Level.FINEST))
        {
            s_logger.log(Level.FINEST, "Sending" + msg + " to:" + to);
        }

        
        MessageHeader header = new MessageHeader(to, m_node, msg);
        m_nioSocket.send(m_nodeToChannel.get(to) , header);
        
    }


    /**
     * Send a message to all nodes.
     */
    public void broadcast(Serializable msg)
    {
        MessageHeader header = new MessageHeader(m_node, msg);
        forwardBroadcast(header);
    }

    
    public void messageReceived(MessageHeader msg,  SocketChannel channel)
    {

        if (msg.getFor() == null)
        {
            forwardBroadcast(msg);
            notifyListeners(msg);
        }
        else if (msg.getFor().equals(m_node))
        {
            notifyListeners(msg);
        }
        else 
        {
            forward(msg);
        }
    }

    private void forward(MessageHeader msg)
    {
        if (m_shutdown)
            return;
        
        SocketChannel socketChannel = m_nodeToChannel.get(msg.getFor());
        if(socketChannel == null)
            throw new IllegalStateException("No channel for:" + msg.getFor() + " all channels:" + socketChannel);
        m_nioSocket.send(socketChannel, msg);        
    }

    private void forwardBroadcast(MessageHeader msg)
    {
        if (m_shutdown)
            return;

        SocketChannel fromChannel = m_nodeToChannel.get(msg.getFrom());
        
        List<SocketChannel> nodes = new ArrayList<SocketChannel>(m_nodeToChannel.values());
        
        if (s_logger.isLoggable(Level.FINEST))
        {
            s_logger.log(Level.FINEST, "broadcasting to" + nodes);
        }
        
        for(SocketChannel channel : nodes)
        {
            if(channel != fromChannel)
                m_nioSocket.send(channel, msg);
        }
    }

    private boolean isNameTaken(String nodeName)
    {
        
        for (INode node : getNodes())
        {
            if (node.getName().equalsIgnoreCase(nodeName))
                return true;
        }
        return false;
    }

    public String getUniqueName(String currentName)
    {
     
        if (currentName.length() > 50)
        {
            currentName = currentName.substring(0, 50);
            
        }
        if (currentName.length() < 2)
        {
            currentName = "aa" + currentName;
        }

        synchronized(m_node)
        {
            if (isNameTaken(currentName))
            {
                
                int i = 1;
                while (true)
                {
                    String newName = currentName + " (" + i + ")";
                    if (!isNameTaken(newName))
                    {
                        currentName = newName;
                        break;
                    }
                    i++;
                }
            }
        }

       return currentName;
    }



    private void notifyListeners(MessageHeader msg)
    {
        Iterator<IMessageListener> iter = m_listeners.iterator();
        while (iter.hasNext())
        {
            IMessageListener listener = iter.next();
            listener.messageReceived(msg.getMessage(), msg.getFrom());
        }
    }

    public void addErrorListener(IMessengerErrorListener listener)
    {
        m_errorListeners.add(listener);
    }

    public void removeErrorListener(IMessengerErrorListener listener)
    {
        m_errorListeners.remove(listener);
    }

    public void addConnectionChangeListener(IConnectionChangeListener listener)
    {
        m_connectionListeners.add(listener);
    }

    public void removeConnectionChangeListener(IConnectionChangeListener listener)
    {
        m_connectionListeners.remove(listener);
    }

    private void notifyConnectionsChanged(boolean added, INode node)
    {
        Iterator<IConnectionChangeListener> iter = m_connectionListeners.iterator();
        while (iter.hasNext())
        {
            if (added)
            {
                iter.next().connectionAdded(node);
            } else
            {
                iter.next().connectionRemoved(node);
            }

        }
    }

    public void setAcceptNewConnections(boolean accept)
    {
        m_acceptNewConnection = accept;
    }

    public boolean isAcceptNewConnections()
    {
        return m_acceptNewConnection;
    }

    /**
     * Get the local node
     */
    public INode getLocalNode()
    {
        return m_node;
    }

    private class ConnectionHandler implements Runnable
    {
        public void run()
        {
            
            
            try
            {
                m_socketChannel.register(m_acceptorSelector, SelectionKey.OP_ACCEPT);
            } catch (ClosedChannelException e)
            {
                s_logger.log(Level.SEVERE, "socket closed",e);
               shutDown();
            }

            
            while (!m_shutdown)
            {
                try
                {
                    m_acceptorSelector.select();
                } catch (IOException e)
                {
                    s_logger.log(Level.SEVERE, "Could not accept on server", e);
                    shutDown();
                }
                
                if(m_shutdown)
                    continue;
                
                Set<SelectionKey> keys = m_acceptorSelector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();
                while(iter.hasNext())
                {
                    SelectionKey key = iter.next();
                    iter.remove();
                 
                    if(key.isAcceptable() && key.isValid())
                    {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

                        // Accept the connection and make it non-blocking
                        SocketChannel socketChannel = null;
                        try
                        {
                            socketChannel = serverSocketChannel.accept();
                            socketChannel.configureBlocking(false);
                            socketChannel.socket().setKeepAlive(true);
                        } catch (IOException e)
                        {
                            s_logger.log(Level.FINE, "Could not accept channel", e);
                            
                            try
                            {
                                if(socketChannel != null)
                                    socketChannel.close();
                            } catch (IOException e2)
                            {
                                s_logger.log(Level.FINE, "Could not close channel", e2);
                            }
                            continue;
                            
                        }
                        
                        //we are not accepting connections
                        if(!m_acceptNewConnection)
                        {
                            try
                            {
                                socketChannel.close();
                            } catch (IOException e)
                            {
                                s_logger.log(Level.FINE, "Could not close channel", e);
                            }
                            continue;
                        }

                        m_nioSocket.add(socketChannel, new ServerQuarantineConversation(m_loginValidator, socketChannel, m_nioSocket,  ServerMessenger.this));
                    }
                }
                
            }
        }
    }

  

    public boolean isServer()
    {
        return true;
    }

    public void removeConnection(INode node)
    {
        if(node.equals(m_node))
            throw new IllegalArgumentException("Cant remove ourself!");
        
        
        
        SocketChannel channel = m_nodeToChannel.remove(node);
        
        
        m_channelToNode.remove(channel);
        
        m_nioSocket.close(channel);
        notifyConnectionsChanged(false, node);
        s_logger.info("Connection removed:" + node);        
    }

    public INode getServerNode()
    {
        return m_node;
    }

    
    
    

    public void socketError(SocketChannel channel, Exception error)
    {
        if(channel == null)
            throw new IllegalArgumentException("Null channel");
        
        //already closed, dont report it again
        if(!m_channelToNode.containsKey(channel))
            return;
        
        removeConnection(m_channelToNode.get(channel));
    }

    public void socketUnqaurantined(SocketChannel channel, QuarantineConversation conversation)
    {
         ServerQuarantineConversation con = (ServerQuarantineConversation) conversation;
         INode remote = new Node(con.getRemoteName(), (InetSocketAddress) channel.socket().getRemoteSocketAddress() );
         
         if(s_logger.isLoggable(Level.FINER))
         {
           s_logger.log(Level.FINER, "Unquarntined node:" + remote);
         }
         
         m_nodeToChannel.put(remote, channel);
         m_channelToNode.put(channel, remote);
         
         notifyConnectionsChanged(true, remote);
         s_logger.info("Connection added to:" + remote);
    }

    public INode getRemoteNode(SocketChannel channel)
    {
        return m_channelToNode.get(channel);
    }


}
