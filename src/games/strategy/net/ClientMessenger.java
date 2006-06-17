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

package games.strategy.net;

import java.io.*;
import java.util.*;
import java.net.*;

import games.strategy.util.ListenerList;

public class ClientMessenger implements IMessenger
{
    private final INode m_node;

    private Set<INode> m_allNodes;

    private final ListenerList<IMessageListener> m_listeners = new ListenerList<IMessageListener>();

    private final Connection m_connection;

    private final ListenerList<IMessengerErrorListener> m_errorListeners = new ListenerList<IMessengerErrorListener>();

    private final ListenerList<IConnectionChangeListener> m_connectionListeners = new ListenerList<IConnectionChangeListener>();

    private String m_connectionRefusedError;

    /**
     * Note, the name paramater passed in here may not match the name of the
     * ClientMessenger after it has been constructed.
     */
    public ClientMessenger(String host, int port, String name) throws IOException, UnknownHostException, CouldNotLogInException
    {
        this(host, port, name, new DefaultObjectStreamFactory());
    }

    /**
     * Note, the name paramater passed in here may not match the name of the
     * ClientMessenger after it has been constructed.
     */
    public ClientMessenger(String host, int port, String name, IObjectStreamFactory streamFact) throws IOException, UnknownHostException, CouldNotLogInException
    {
        this(host, port, name, streamFact, null);
    }
    
    public ClientMessenger(String host, int port, String name, IObjectStreamFactory streamFact, IConnectionLogin login) throws IOException, UnknownHostException, CouldNotLogInException
    {
        Socket socket = new Socket(host, port);
        SocketStreams streams = new SocketStreams(socket);

        ClientLoginHelper clientLoginHelper = new ClientLoginHelper(login, streams, name);
        if (!clientLoginHelper.login())
        {
            socket.close();
            throw new CouldNotLogInException();
        }

        m_node = new Node(clientLoginHelper.getClientName(), socket.getLocalAddress(), socket.getLocalPort());

        m_connection = new Connection(socket, m_node, m_connectionListener, streamFact, true, streams);

        // wait for the init message
        while (m_allNodes == null && m_connection.isConnected())
        {
            try
            {
                synchronized (this)
                {
                    wait(100);
                }
            } catch (InterruptedException ie)
            {
            }
        }

        if (!m_connection.isConnected())
        {
            if (m_connectionRefusedError != null)
            {
                throw new IOException("Connection refused:" + m_connectionRefusedError);
            } else
            {
                throw new IOException("Connection lost");
            }
        }
    }

    private void serverMessageReceived(ServerMessage msg)
    {
        if (msg instanceof NodeChangeServerMessage)
        {
            nodeChangeMessageReceived((NodeChangeServerMessage) msg);
        } else if (msg instanceof ClientInitServerMessage)
        {
            initMessageReceived((ClientInitServerMessage) msg);
        } else if (msg instanceof ConnectionRefusedMessage)
        {
            m_connectionRefusedError = ((ConnectionRefusedMessage) msg).getError();
            shutDown();
        } else
            throw new IllegalArgumentException("Unknown server messgae:" + msg);
    }

    private void nodeChangeMessageReceived(NodeChangeServerMessage msg)
    {
        INode node = msg.getNode();
        synchronized (this)
        {
            if (msg.getAdd())
            {
                m_allNodes.add(node);
            } else
            {
                m_allNodes.remove(node);
            }
        }

        notifyConnectionsChanged(msg.getAdd(), node);
    }

    private synchronized void initMessageReceived(ClientInitServerMessage msg)
    {
        m_allNodes = msg.getAllNodes();
        this.notifyAll();
    }

    /*
     * @see IMessenger#send(Serializable, INode)
     */
    public synchronized void send(Serializable msg, INode to)
    {
        MessageHeader header = new MessageHeader(to, m_node, msg);
        m_connection.send(header);
    }

    /*
     * @see IMessenger#broadcast(Serializable)
     */
    public synchronized void broadcast(Serializable msg)
    {
        MessageHeader header = new MessageHeader(m_node, msg);
        m_connection.send(header);

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

    public void addErrorListener(IMessengerErrorListener listener)
    {
        m_errorListeners.add(listener);
    }

    public void removeErrorListener(IMessengerErrorListener listener)
    {
        m_errorListeners.remove(listener);
    }

    /*
     * @see IMessenger#getNodes()
     */
    public synchronized Set<INode> getNodes()
    {
        // if the init message hasnt reached us yet, stall
        return new HashSet<INode>(m_allNodes);
    }

    /*
     * @see IMessenger#isConnected()
     */
    public boolean isConnected()
    {
        return m_connection.isConnected();
    }

    public void shutDown()
    {
        // it may be that we recieve this message before the connection has been
        // set up
        // ie in the constructor to m_connection which starts another thread
        while (m_connection == null)
        {
            Thread.yield();
            System.out.println("Client Messenger waiting for connection to be set");
        }
        m_connection.shutDown();
        m_allNodes = Collections.emptySet();
    }

    private IConnectionListener m_connectionListener = new IConnectionListener()
    {
        public void messageReceived(Serializable message, Connection connection)
        {
            ClientMessenger.this.messageReceived((MessageHeader) message);
        }

        public void fatalError(Exception error, Connection connection, List unsent)
        {
            Iterator<IMessengerErrorListener> iter = m_errorListeners.iterator();
            while (iter.hasNext())
            {
                IMessengerErrorListener errorListener = iter.next();
                errorListener.messengerInvalid(ClientMessenger.this, error, unsent);
            }
        }
    };

    private void messageReceived(MessageHeader msg)
    {
        if (msg.getMessage() instanceof ServerMessage)
            serverMessageReceived((ServerMessage) msg.getMessage());
        else
        {
            Iterator<IMessageListener> iter = m_listeners.iterator();
            while (iter.hasNext())
            {
                IMessageListener listener = iter.next();
                listener.messageReceived(msg.getMessage(), msg.getFrom());
            }
        }
    }

    public void flush()
    {
        m_connection.flush();
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

    /**
     * Get the local node
     */
    public INode getLocalNode()
    {
        return m_node;
    }

    public INode getServerNode()
    {
        return m_connection.getRemoteNode();
    }

    public boolean isServer()
    {
        return false;
    }
}
