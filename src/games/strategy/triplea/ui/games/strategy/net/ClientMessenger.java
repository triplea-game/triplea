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

import games.strategy.net.nio.ClientQuarantineConversation;
import games.strategy.net.nio.NIOSocket;
import games.strategy.net.nio.NIOSocketListener;
import games.strategy.net.nio.QuarantineConversation;
import games.strategy.util.ListenerList;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

public class ClientMessenger implements IMessenger , NIOSocketListener
{
    private INode m_node;

    private final ListenerList<IMessageListener> m_listeners = new ListenerList<IMessageListener>();

    private final ListenerList<IMessengerErrorListener> m_errorListeners = new ListenerList<IMessengerErrorListener>();
    
    private CountDownLatch m_initLatch = new CountDownLatch(1);
    private Exception m_connectionRefusedError;
    private final NIOSocket m_socket;
 
    private final SocketChannel m_socketChannel;
    private INode m_serverNode;
    private volatile boolean m_shutDown = false;

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

    /**
     * Note, the name paramater passed in here may not match the name of the
     * ClientMessenger after it has been constructed.
     */
    public ClientMessenger(String host, int port, String name, IObjectStreamFactory streamFact, IConnectionLogin login) throws IOException, UnknownHostException, CouldNotLogInException
    {
        m_socketChannel = SocketChannel.open();
        m_socketChannel.configureBlocking(false);
        InetSocketAddress remote = new InetSocketAddress(host,port);
        boolean connected = m_socketChannel.connect(remote);
        
        //give up after 10 seconds
        int waitTimeMilliseconds = 0;
        while(!connected && waitTimeMilliseconds < 10000)
        {
            connected = m_socketChannel.finishConnect();
            try
            {
                Thread.sleep(50);
                waitTimeMilliseconds+= 50;
            } catch (InterruptedException e)
            {
            }
        }
        
        if(!connected) {
            m_socketChannel.close();
            throw new IOException("Connection refused");
        }
        
        Socket socket = m_socketChannel.socket();
        socket.setKeepAlive(true);
        
        
        m_socket = new NIOSocket(streamFact, this, name);
        ClientQuarantineConversation conversation = new ClientQuarantineConversation(login, m_socketChannel, m_socket, name);
        m_socket.add(m_socketChannel, conversation);
        
        
        //allow the credentials to be shown in this thread
        conversation.showCredentials();
        
        //wait for the quarantine to end
        try
        {
            m_initLatch.await();
        } catch (InterruptedException e)
        {
            m_connectionRefusedError = e;
            try
            {
                m_socketChannel.close();
            }
            catch(IOException e2)
            {
                //ignore
            }
        }
        
        if(conversation.getErrorMessage() != null || m_connectionRefusedError != null)
        {
            //our socket channel should already be closed
            m_socket.shutDown();
            
            if(conversation.getErrorMessage() != null)
            {
                login.notifyFailedLogin(conversation.getErrorMessage());
                throw new CouldNotLogInException();
            }
            else if(m_connectionRefusedError instanceof CouldNotLogInException)
                throw (CouldNotLogInException) m_connectionRefusedError;
            else if(m_connectionRefusedError != null)
                throw new IOException(m_connectionRefusedError.getMessage());
        }
        
         

    }

   
    /*
     * @see IMessenger#send(Serializable, INode)
     */
    public synchronized void send(Serializable msg, INode to)
    {
        //use our nodes address, this is our network visible address
        MessageHeader header = new MessageHeader(to, m_node, msg);
        m_socket.send(m_socketChannel, header);
    }

    /*
     * @see IMessenger#broadcast(Serializable)
     */
    public synchronized void broadcast(Serializable msg)
    {
        MessageHeader header = new MessageHeader(m_node, msg);
        m_socket.send(m_socketChannel, header);
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
        return m_socketChannel.isConnected();
    }

    public void shutDown()
    {
        m_shutDown = true;
        m_socket.shutDown();
        try
        {
            m_socketChannel.close();
        } catch (IOException e)
        {
            //ignore
        }
    }

    public void messageReceived(MessageHeader msg, SocketChannel channel)
    {
        if(msg.getFor() != null &&! msg.getFor().equals(m_node))
        {
            throw new IllegalStateException("msg not for me:" + msg);
        }
        
        Iterator<IMessageListener> iter = m_listeners.iterator();
        while (iter.hasNext())
        {
            IMessageListener listener = iter.next();
            listener.messageReceived(msg.getMessage(), msg.getFrom());
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
        return m_serverNode;
    }

    public boolean isServer()
    {
        return false;
    }

    public void socketUnqaurantined(SocketChannel channel, QuarantineConversation converstaion2)
    {
        ClientQuarantineConversation conversation = (ClientQuarantineConversation) converstaion2;
        
        //all ids are based on the socket adress of nodes in the network
        //but the adress of a node changes depending on who is looking at it
        //ie, sometimes it is the loopback adress if connecting locally,
        //sometimes the client or server will be behind a NAT
        //so all node ids are defined as what the server sees the adress as
        
        //we are still in the decode thread at this point, set our nodes now 
        //before the socket is unquarantined
        m_node = new Node(conversation.getLocalName() , conversation.getNetworkVisibleSocketAdress());
        m_serverNode = new Node(conversation.getServerName(), conversation.getServerLocalAddress());
        
        m_initLatch.countDown();
        
    }

    public void socketError(SocketChannel channel, Exception error)
    {
        if(m_shutDown)
            return;
        
        //if an error occurs during set up
        //we need to return in the constructor
        //otherwise this is harmless
        m_connectionRefusedError = error;
        
        
        
        Iterator<IMessengerErrorListener> iter = m_errorListeners.iterator();
        while (iter.hasNext())
        {
            IMessengerErrorListener errorListener = iter.next();
            errorListener.messengerInvalid(ClientMessenger.this, error);
        }
        
        shutDown();
        m_initLatch.countDown();
        
    }

    public INode getRemoteNode(SocketChannel channel)
    {
        //we only have one channel
        return m_serverNode;
    }

    public InetSocketAddress getRemoteServerSocketAddress()
    {
        return (InetSocketAddress) m_socketChannel.socket().getRemoteSocketAddress();
    }
}
