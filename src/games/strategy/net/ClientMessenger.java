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

import games.strategy.util.ListenerList;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientMessenger implements IMessenger
{
    private final INode m_node;

    private final ListenerList<IMessageListener> m_listeners = new ListenerList<IMessageListener>();

    private final Connection m_connection;

    private final ListenerList<IMessengerErrorListener> m_errorListeners = new ListenerList<IMessengerErrorListener>();
    
    private CountDownLatch m_initLatch = new CountDownLatch(1);
    private String m_connectionRefusedError;

    /**
     * Note, the name paramater passed in here may not match the name of the
     * ClientMessenger after it has been constructed.
     */
    public ClientMessenger(String host, int port, String name, IConnectionLogin login) throws IOException, UnknownHostException, CouldNotLogInException
    {
        this(host, port, name, new DefaultObjectStreamFactory(), login);
    }

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
        socket.setKeepAlive(true);
        SocketStreams streams = new SocketStreams(socket);

        ClientLoginHelper clientLoginHelper = new ClientLoginHelper(login, streams, name);
        if (!clientLoginHelper.login())
        {
            socket.close();
            throw new CouldNotLogInException();
        }

        m_node = new Node(clientLoginHelper.getClientName(), socket.getLocalAddress(), socket.getLocalPort());

        m_connection = new Connection(socket, m_node, m_connectionListener, streamFact, true, streams);

        
        //make sure we recieve a message
        //this will mean the server is ready to send us messages
        try
        {
            m_initLatch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e)
        {}

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
    }

    private IConnectionListener m_connectionListener = new IConnectionListener()
    {
        public void messageReceived(Serializable message, Connection connection)
        {
            ClientMessenger.this.messageReceived((MessageHeader) message);
        }

        public void fatalError(Exception error, Connection connection, List unsent)
        {
            //if we havnet already finished
            m_initLatch.countDown();
            
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
        //we have been initialized
        if(msg.getMessage() instanceof ServerMessage)
        {
            m_initLatch.countDown();
            return;
        }
        
        Iterator<IMessageListener> iter = m_listeners.iterator();
        while (iter.hasNext())
        {
            IMessageListener listener = iter.next();
            listener.messageReceived(msg.getMessage(), msg.getFrom());
        }
    }

    public void flush()
    {
        m_connection.flush();
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
