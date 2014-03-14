/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.net;

import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.nio.ClientQuarantineConversation;
import games.strategy.net.nio.NIOSocket;
import games.strategy.net.nio.NIOSocketListener;
import games.strategy.net.nio.QuarantineConversation;
import games.strategy.util.ListenerList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ClientMessenger implements IClientMessenger, NIOSocketListener
{
	private INode m_node;
	private final ListenerList<IMessageListener> m_listeners = new ListenerList<IMessageListener>();
	private final ListenerList<IMessengerErrorListener> m_errorListeners = new ListenerList<IMessengerErrorListener>();
	private final CountDownLatch m_initLatch = new CountDownLatch(1);
	private Exception m_connectionRefusedError;
	private final NIOSocket m_socket;
	private final SocketChannel m_socketChannel;
	private INode m_serverNode;
	private volatile boolean m_shutDown = false;
	
	/**
	 * Note, the name paramater passed in here may not match the name of the
	 * ClientMessenger after it has been constructed.
	 */
	public ClientMessenger(final String host, final int port, final String name, final String mac, final IConnectionLogin login) throws IOException, UnknownHostException, CouldNotLogInException
	{
		this(host, port, name, mac, new DefaultObjectStreamFactory(), login);
	}
	
	/**
	 * Note, the name paramater passed in here may not match the name of the
	 * ClientMessenger after it has been constructed.
	 */
	public ClientMessenger(final String host, final int port, final String name, final String mac) throws IOException, UnknownHostException, CouldNotLogInException
	{
		this(host, port, name, mac, new DefaultObjectStreamFactory());
	}
	
	/**
	 * Note, the name paramater passed in here may not match the name of the
	 * ClientMessenger after it has been constructed.
	 */
	public ClientMessenger(final String host, final int port, final String name, final String mac, final IObjectStreamFactory streamFact) throws IOException, UnknownHostException,
				CouldNotLogInException
	{
		this(host, port, name, mac, streamFact, null);
	}
	
	/**
	 * Note, the name paramater passed in here may not match the name of the
	 * ClientMessenger after it has been constructed.
	 */
	public ClientMessenger(final String host, final int port, final String name, final String mac, final IObjectStreamFactory streamFact, final IConnectionLogin login) throws IOException,
				UnknownHostException, CouldNotLogInException
	{
		m_socketChannel = SocketChannel.open();
		m_socketChannel.configureBlocking(false);
		final InetSocketAddress remote = new InetSocketAddress(host, port);
		if (!m_socketChannel.connect(remote))
		{
			// give up after 10 seconds
			int waitTimeMilliseconds = 0;
			while (true)
			{
				if (waitTimeMilliseconds > 10000)
				{
					m_socketChannel.close();
					throw new IOException("Connection refused");
				}
				if (m_socketChannel.finishConnect())
					break;
				try
				{
					Thread.sleep(50);
					waitTimeMilliseconds += 50;
				} catch (final InterruptedException e)
				{
				}
			}
		}
		final Socket socket = m_socketChannel.socket();
		socket.setKeepAlive(true);
		m_socket = new NIOSocket(streamFact, this, name);
		final ClientQuarantineConversation conversation = new ClientQuarantineConversation(login, m_socketChannel, m_socket, name, mac);
		m_socket.add(m_socketChannel, conversation);
		// allow the credentials to be shown in this thread
		conversation.showCredentials();
		// wait for the quarantine to end
		try
		{
			m_initLatch.await();
		} catch (final InterruptedException e)
		{
			m_connectionRefusedError = e;
			try
			{
				m_socketChannel.close();
			} catch (final IOException e2)
			{
				// ignore
			}
		}
		if (conversation.getErrorMessage() != null || m_connectionRefusedError != null)
		{
			// our socket channel should already be closed
			m_socket.shutDown();
			if (conversation.getErrorMessage() != null)
			{
				login.notifyFailedLogin(conversation.getErrorMessage());
				throw new CouldNotLogInException();
			}
			else if (m_connectionRefusedError instanceof CouldNotLogInException)
				throw (CouldNotLogInException) m_connectionRefusedError;
			else if (m_connectionRefusedError != null)
				throw new IOException(m_connectionRefusedError.getMessage());
		}
	}
	
	/*
	 * @see IMessenger#send(Serializable, INode)
	 */
	public synchronized void send(final Serializable msg, final INode to)
	{
		// use our nodes address, this is our network visible address
		final MessageHeader header = new MessageHeader(to, m_node, msg);
		m_socket.send(m_socketChannel, header);
	}
	
	/*
	 * @see IMessenger#broadcast(Serializable)
	 */
	public synchronized void broadcast(final Serializable msg)
	{
		final MessageHeader header = new MessageHeader(m_node, msg);
		m_socket.send(m_socketChannel, header);
	}
	
	/*
	 * @see IMessenger#addMessageListener(Class, IMessageListener)
	 */
	public void addMessageListener(final IMessageListener listener)
	{
		m_listeners.add(listener);
	}
	
	/*
	 * @see IMessenger#removeMessageListener(Class, IMessageListener)
	 */
	public void removeMessageListener(final IMessageListener listener)
	{
		m_listeners.remove(listener);
	}
	
	public void addErrorListener(final IMessengerErrorListener listener)
	{
		m_errorListeners.add(listener);
	}
	
	public void removeErrorListener(final IMessengerErrorListener listener)
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
		} catch (final IOException e)
		{
			// ignore
		}
	}
	
	public boolean isShutDown()
	{
		return m_shutDown;
	}
	
	public void messageReceived(final MessageHeader msg, final SocketChannel channel)
	{
		if (msg.getFor() != null && !msg.getFor().equals(m_node))
		{
			throw new IllegalStateException("msg not for me:" + msg);
		}
		for (final IMessageListener listener : m_listeners)
		{
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
	
	public void socketUnqaurantined(final SocketChannel channel, final QuarantineConversation converstaion2)
	{
		final ClientQuarantineConversation conversation = (ClientQuarantineConversation) converstaion2;
		// all ids are based on the socket adress of nodes in the network
		// but the adress of a node changes depending on who is looking at it
		// ie, sometimes it is the loopback adress if connecting locally,
		// sometimes the client or server will be behind a NAT
		// so all node ids are defined as what the server sees the adress as
		// we are still in the decode thread at this point, set our nodes now
		// before the socket is unquarantined
		m_node = new Node(conversation.getLocalName(), conversation.getNetworkVisibleSocketAdress());
		m_serverNode = new Node(conversation.getServerName(), conversation.getServerLocalAddress());
		m_initLatch.countDown();
	}
	
	public void socketError(final SocketChannel channel, final Exception error)
	{
		if (m_shutDown)
			return;
		// if an error occurs during set up
		// we need to return in the constructor
		// otherwise this is harmless
		m_connectionRefusedError = error;
		for (final IMessengerErrorListener errorListener : m_errorListeners)
		{
			errorListener.messengerInvalid(ClientMessenger.this, error);
		}
		shutDown();
		m_initLatch.countDown();
	}
	
	public INode getRemoteNode(final SocketChannel channel)
	{
		// we only have one channel
		return m_serverNode;
	}
	
	public InetSocketAddress getRemoteServerSocketAddress()
	{
		return (InetSocketAddress) m_socketChannel.socket().getRemoteSocketAddress();
	}
	
	private void bareBonesSendMessageToServer(final String methodName, final Object... messages)
	{
		final List<Object> args = new ArrayList<Object>();
		final Class[] argTypes = new Class[messages.length];
		for (int i = 0; i < messages.length; i++)
		{
			final Object message = messages[i];
			args.add(message);
			argTypes[i] = args.get(i).getClass();
		}
		final RemoteName rn = ServerModel.SERVER_REMOTE_NAME;
		final RemoteMethodCall call = new RemoteMethodCall(rn.getName(), methodName, args.toArray(), argTypes, rn.getClazz());
		final HubInvoke hubInvoke = new HubInvoke(null, false, call);
		send(hubInvoke, getServerNode());
	}
	
	public void changeServerGameTo(final String gameName)
	{
		bareBonesSendMessageToServer("changeServerGameTo", gameName);
	}
	
	public void changeToLatestAutosave(final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave)
	{
		bareBonesSendMessageToServer("changeToLatestAutosave", typeOfAutosave);
	}
	
	public void changeToGameSave(final byte[] bytes, final String fileName)
	{
		bareBonesSendMessageToServer("changeToGameSave", bytes, fileName);
	}
	
	public void changeToGameSave(final File saveGame, final String fileName)
	{
		final byte[] bytes = getBytesFromFile(saveGame);
		if (bytes == null || bytes.length == 0)
			return;
		changeToGameSave(bytes, fileName);
	}
	
	private static byte[] getBytesFromFile(final File file)
	{
		if (file == null || !file.exists())
			return null;
		// Get the size of the file
		final long length = file.length();
		if (length > Integer.MAX_VALUE)
		{
			return null;
		}
		// Create the byte array to hold the data
		final byte[] bytes = new byte[(int) length];
		InputStream is = null;
		try
		{
			is = new FileInputStream(file);
			is.read(bytes);
			/* Read in the bytes
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length
						&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)
			{
				offset += numRead;
			}
			// Ensure all the bytes have been read in
			if (offset < bytes.length)
			{
				is.close();
				throw new IOException("Could not completely read file " + file.getName());
			}*/
		} catch (final IOException e)
		{
			e.printStackTrace();
		} finally
		{
			// Close the input stream and return bytes
			if (is != null)
			{
				try
				{
					is.close();
				} catch (final IOException e)
				{
				}
			}
		}
		return bytes;
	}
	
	@Override
	public String toString()
	{
		return "ClientMessenger LocalNode:" + m_node + " ServerNodes:" + m_serverNode;
	}
}
