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
/*
 * ServerMessenger.java
 * 
 * Created on December 11, 2001, 7:43 PM
 */
package games.strategy.net;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.IChatChannel;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.userDB.MutedIpController;
import games.strategy.engine.lobby.server.userDB.MutedMacController;
import games.strategy.engine.lobby.server.userDB.MutedUsernameController;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.SpokeInvoke;
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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
	// all our nodes
	private final ConcurrentHashMap<INode, SocketChannel> m_nodeToChannel = new ConcurrentHashMap<INode, SocketChannel>();
	private final ConcurrentHashMap<SocketChannel, INode> m_channelToNode = new ConcurrentHashMap<SocketChannel, INode>();
	
	// A hack, till I think of something better
	public ServerMessenger(final String name, final int portNumber, final IObjectStreamFactory streamFactory) throws IOException
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
		final Thread t = new Thread(new ConnectionHandler(), "Server Messenger Connection Handler");
		t.start();
	}
	
	public void setLoginValidator(final ILoginValidator loginValidator)
	{
		m_loginValidator = loginValidator;
	}
	
	public ILoginValidator getLoginValidator()
	{
		return m_loginValidator;
	}
	
	/** Creates new ServerMessenger */
	public ServerMessenger(final String name, final int portNumber) throws IOException
	{
		this(name, portNumber, new DefaultObjectStreamFactory());
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
	
	/**
	 * Get a list of nodes.
	 */
	public Set<INode> getNodes()
	{
		final Set<INode> rVal = new HashSet<INode>(m_nodeToChannel.keySet());
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
			} catch (final Exception e)
			{
				// ignore
			}
			if (m_acceptorSelector != null)
				m_acceptorSelector.wakeup();
		}
	}
	
	public synchronized boolean isShutDown()
	{
		return m_shutdown;
	}
	
	public boolean isConnected()
	{
		return !m_shutdown;
	}
	
	/**
	 * Send a message to the given node.
	 */
	public void send(final Serializable msg, final INode to)
	{
		if (m_shutdown)
			return;
		if (s_logger.isLoggable(Level.FINEST))
		{
			s_logger.log(Level.FINEST, "Sending" + msg + " to:" + to);
		}
		final MessageHeader header = new MessageHeader(to, m_node, msg);
		final SocketChannel socketChannel = m_nodeToChannel.get(to);
		// the socket was removed
		if (socketChannel == null)
		{
			if (s_logger.isLoggable(Level.FINER))
			{
				s_logger.log(Level.FINER, "no channel for node:" + to + " dropping message:" + msg);
			}
			// the socket has not been added yet
			return;
		}
		m_nioSocket.send(socketChannel, header);
	}
	
	/**
	 * Send a message to all nodes.
	 */
	public void broadcast(final Serializable msg)
	{
		final MessageHeader header = new MessageHeader(m_node, msg);
		forwardBroadcast(header);
	}
	
	private boolean isLobby()
	{
		return m_loginValidator instanceof LobbyLoginValidator;
	}
	
	private boolean isGame()
	{
		return !isLobby();
	}
	
	private final Object m_cachedListLock = new Object();
	private final HashMap<String, String> m_cachedMacAddresses = new HashMap<String, String>();
	
	public String GetPlayerMac(final String name)
	{
		synchronized (m_cachedListLock)
		{
			String mac = m_cachedMacAddresses.get(name);
			if (mac == null)
				mac = m_playersThatLeftMacs_Last10.get(name);
			return mac;
		}
	}
	
	// We need to cache whether players are muted, because otherwise the database would have to be accessed each time a message was sent, which can be very slow
	private final List<String> m_liveMutedUsernames = new ArrayList<String>();
	
	public boolean IsUsernameMuted(final String username)
	{
		synchronized (m_cachedListLock)
		{
			return m_liveMutedUsernames.contains(username);
		}
	}
	
	public void NotifyUsernameMutingOfPlayer(final String username, final Date muteExpires)
	{
		synchronized (m_cachedListLock)
		{
			if (!m_liveMutedUsernames.contains(username))
				m_liveMutedUsernames.add(username);
			if (muteExpires != null)
				ScheduleUsernameUnmuteAt(username, muteExpires.getTime());
		}
	}
	
	private final List<String> m_liveMutedIpAddresses = new ArrayList<String>();
	
	public boolean IsIpMuted(final String ip)
	{
		synchronized (m_cachedListLock)
		{
			return m_liveMutedIpAddresses.contains(ip);
		}
	}
	
	public void NotifyIPMutingOfPlayer(final String ip, final Date muteExpires)
	{
		synchronized (m_cachedListLock)
		{
			if (!m_liveMutedIpAddresses.contains(ip))
				m_liveMutedIpAddresses.add(ip);
			if (muteExpires != null)
				ScheduleIpUnmuteAt(ip, muteExpires.getTime());
		}
	}
	
	private final List<String> m_liveMutedMacAddresses = new ArrayList<String>();
	
	public boolean IsMacMuted(final String mac)
	{
		synchronized (m_cachedListLock)
		{
			return m_liveMutedMacAddresses.contains(mac);
		}
	}
	
	public void NotifyMacMutingOfPlayer(final String mac, final Date muteExpires)
	{
		synchronized (m_cachedListLock)
		{
			if (!m_liveMutedMacAddresses.contains(mac))
				m_liveMutedMacAddresses.add(mac);
			if (muteExpires != null)
				ScheduleMacUnmuteAt(mac, muteExpires.getTime());
		}
	}
	
	private void ScheduleUsernameUnmuteAt(final String username, final long checkTime)
	{
		final Timer unmuteUsernameTimer = new Timer("Username unmute timer");
		unmuteUsernameTimer.schedule(GetUsernameUnmuteTask(username), new Date(checkTime));
	}
	
	private void ScheduleIpUnmuteAt(final String ip, final long checkTime)
	{
		final Timer unmuteIpTimer = new Timer("IP unmute timer");
		unmuteIpTimer.schedule(GetIpUnmuteTask(ip), new Date(checkTime));
	}
	
	private void ScheduleMacUnmuteAt(final String mac, final long checkTime)
	{
		final Timer unmuteMacTimer = new Timer("Mac unmute timer");
		unmuteMacTimer.schedule(GetMacUnmuteTask(mac), new Date(checkTime));
	}
	
	public void NotifyPlayerLogin(final String uniquePlayerName, final String ip, final String mac)
	{
		synchronized (m_cachedListLock)
		{
			m_cachedMacAddresses.put(uniquePlayerName, mac);
			if (isLobby())
			{
				final String realName = uniquePlayerName.split(" ")[0];
				if (!m_liveMutedUsernames.contains(realName))
				{
					final long muteTill = new MutedUsernameController().getUsernameUnmuteTime(realName);
					if (muteTill != -1 && muteTill <= System.currentTimeMillis())
					{
						m_liveMutedUsernames.add(realName); // Signal the player as muted
						ScheduleUsernameUnmuteAt(realName, muteTill);
					}
				}
				if (!m_liveMutedIpAddresses.contains(ip))
				{
					final long muteTill = new MutedIpController().getIpUnmuteTime(ip);
					if (muteTill != -1 && muteTill <= System.currentTimeMillis())
					{
						m_liveMutedIpAddresses.add(ip); // Signal the player as muted
						ScheduleIpUnmuteAt(ip, muteTill);
					}
				}
				if (!m_liveMutedMacAddresses.contains(mac))
				{
					final long muteTill = new MutedMacController().getMacUnmuteTime(mac);
					if (muteTill != -1 && muteTill <= System.currentTimeMillis())
					{
						m_liveMutedMacAddresses.add(mac); // Signal the player as muted
						ScheduleMacUnmuteAt(mac, muteTill);
					}
				}
			}
		}
	}
	
	private final HashMap<String, String> m_playersThatLeftMacs_Last10 = new HashMap<String, String>();
	
	public HashMap<String, String> GetPlayersThatLeftMacs_Last10()
	{
		return m_playersThatLeftMacs_Last10;
	}
	
	private void NotifyPlayerRemoval(final INode node)
	{
		synchronized (m_cachedListLock)
		{
			m_playersThatLeftMacs_Last10.put(node.getName(), m_cachedMacAddresses.get(node.getName()));
			if (m_playersThatLeftMacs_Last10.size() > 10)
				m_playersThatLeftMacs_Last10.remove(m_playersThatLeftMacs_Last10.entrySet().iterator().next().toString());
			m_cachedMacAddresses.remove(node.getName());
		}
	}
	
	public static final String YOU_HAVE_BEEN_MUTED_LOBBY = "?YOUR LOBBY CHATTING HAS BEEN TEMPORARILY 'MUTED' BY THE ADMINS, TRY AGAIN LATER"; // Special character to stop spoofing by server
	public static final String YOU_HAVE_BEEN_MUTED_GAME = "?YOUR CHATTING IN THIS GAME HAS BEEN 'MUTED' BY THE HOST"; // Special character to stop spoofing by host
	
	public void messageReceived(final MessageHeader msg, final SocketChannel channel)
	{
		final INode expectedReceive = m_channelToNode.get(channel);
		if (!expectedReceive.equals(msg.getFrom()))
		{
			throw new IllegalStateException("Expected: " + expectedReceive + " not: " + msg.getFrom());
		}
		if (msg.getMessage() instanceof HubInvoke) // Chat messages are always HubInvoke's
		{
			if (isLobby() && ((HubInvoke) msg.getMessage()).call.getRemoteName().equals("_ChatCtrl_LOBBY_CHAT"))
			{
				final String realName = msg.getFrom().getName().split(" ")[0];
				if (IsUsernameMuted(realName))
				{
					bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_LOBBY, msg.getFrom());
					return;
				}
				else if (IsIpMuted(msg.getFrom().getAddress().getHostAddress()))
				{
					bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_LOBBY, msg.getFrom());
					return;
				}
				else if (IsMacMuted(GetPlayerMac(msg.getFrom().getName())))
				{
					bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_LOBBY, msg.getFrom());
					return;
				}
			}
			else if (isGame() && ((HubInvoke) msg.getMessage()).call.getRemoteName().equals("_ChatCtrlgames.strategy.engine.framework.ui.ServerStartup.CHAT_NAME"))
			{
				final String realName = msg.getFrom().getName().split(" ")[0];
				if (IsUsernameMuted(realName))
				{
					bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_GAME, msg.getFrom());
					return;
				}
				else if (IsIpMuted(msg.getFrom().getAddress().getHostAddress()))
				{
					bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_GAME, msg.getFrom());
					return;
				}
				if (IsMacMuted(GetPlayerMac(msg.getFrom().getName())))
				{
					bareBonesSendChatMessage(YOU_HAVE_BEEN_MUTED_GAME, msg.getFrom());
					return;
				}
			}
		}
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
	
	private void bareBonesSendChatMessage(final String message, final INode to)
	{
		final List<Object> args = new ArrayList<Object>();
		final Class[] argTypes = new Class[1];
		args.add(message);
		argTypes[0] = args.get(0).getClass();
		RemoteName rn;
		if (isLobby())
			rn = new RemoteName(ChatController.getChatChannelName("_LOBBY_CHAT"), IChatChannel.class);
		else
			rn = new RemoteName(ChatController.getChatChannelName("games.strategy.engine.framework.ui.ServerStartup.CHAT_NAME"), IChatChannel.class);
		final RemoteMethodCall call = new RemoteMethodCall(rn.getName(), "chatOccured", args.toArray(), argTypes, rn.getClazz());
		final SpokeInvoke spokeInvoke = new SpokeInvoke(null, false, call, getServerNode());
		send(spokeInvoke, to);
	}
	
	// The following code is used in hosted lobby games by the host for player mini-banning and mini-muting
	private final List<String> m_miniBannedUsernames = new ArrayList<String>();
	
	public boolean IsUsernameMiniBanned(final String username)
	{
		synchronized (m_cachedListLock)
		{
			return m_miniBannedUsernames.contains(username);
		}
	}
	
	public void NotifyUsernameMiniBanningOfPlayer(final String username, final Date expires)
	{
		synchronized (m_cachedListLock)
		{
			if (!m_miniBannedUsernames.contains(username))
				m_miniBannedUsernames.add(username);
			if (expires != null)
			{
				final Timer unbanUsernameTimer = new Timer("Username unban timer");
				unbanUsernameTimer.schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						synchronized (m_cachedListLock)
						{
							m_miniBannedUsernames.remove(username);
						}
					}
				}, new Date(expires.getTime()));
			}
		}
	}
	
	private final List<String> m_miniBannedIpAddresses = new ArrayList<String>();
	
	public boolean IsIpMiniBanned(final String ip)
	{
		synchronized (m_cachedListLock)
		{
			return m_miniBannedIpAddresses.contains(ip);
		}
	}
	
	public void NotifyIPMiniBanningOfPlayer(final String ip, final Date expires)
	{
		synchronized (m_cachedListLock)
		{
			if (!m_miniBannedIpAddresses.contains(ip))
				m_miniBannedIpAddresses.add(ip);
			if (expires != null)
			{
				final Timer unbanIpTimer = new Timer("IP unban timer");
				unbanIpTimer.schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						synchronized (m_cachedListLock)
						{
							m_miniBannedIpAddresses.remove(ip);
						}
					}
				}, new Date(expires.getTime()));
			}
		}
	}
	
	private final List<String> m_miniBannedMacAddresses = new ArrayList<String>();
	
	public boolean IsMacMiniBanned(final String mac)
	{
		synchronized (m_cachedListLock)
		{
			return m_miniBannedMacAddresses.contains(mac);
		}
	}
	
	public void NotifyMacMiniBanningOfPlayer(final String mac, final Date expires)
	{
		synchronized (m_cachedListLock)
		{
			if (!m_miniBannedMacAddresses.contains(mac))
				m_miniBannedMacAddresses.add(mac);
			if (expires != null)
			{
				final Timer unbanMacTimer = new Timer("Mac unban timer");
				unbanMacTimer.schedule(new TimerTask()
				{
					@Override
					public void run()
					{
						synchronized (m_cachedListLock)
						{
							m_miniBannedMacAddresses.remove(mac);
						}
					}
				}, new Date(expires.getTime()));
			}
		}
	}
	
	private void forward(final MessageHeader msg)
	{
		if (m_shutdown)
			return;
		final SocketChannel socketChannel = m_nodeToChannel.get(msg.getFor());
		if (socketChannel == null)
			throw new IllegalStateException("No channel for:" + msg.getFor() + " all channels:" + socketChannel);
		m_nioSocket.send(socketChannel, msg);
	}
	
	private void forwardBroadcast(final MessageHeader msg)
	{
		if (m_shutdown)
			return;
		final SocketChannel fromChannel = m_nodeToChannel.get(msg.getFrom());
		final List<SocketChannel> nodes = new ArrayList<SocketChannel>(m_nodeToChannel.values());
		if (s_logger.isLoggable(Level.FINEST))
		{
			s_logger.log(Level.FINEST, "broadcasting to" + nodes);
		}
		for (final SocketChannel channel : nodes)
		{
			if (channel != fromChannel)
				m_nioSocket.send(channel, msg);
		}
	}
	
	private boolean isNameTaken(final String nodeName)
	{
		for (final INode node : getNodes())
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
		synchronized (m_node)
		{
			if (isNameTaken(currentName))
			{
				int i = 1;
				while (true)
				{
					final String newName = currentName + " (" + i + ")";
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
	
	private void notifyListeners(final MessageHeader msg)
	{
		final Iterator<IMessageListener> iter = m_listeners.iterator();
		while (iter.hasNext())
		{
			final IMessageListener listener = iter.next();
			listener.messageReceived(msg.getMessage(), msg.getFrom());
		}
	}
	
	public void addErrorListener(final IMessengerErrorListener listener)
	{
		m_errorListeners.add(listener);
	}
	
	public void removeErrorListener(final IMessengerErrorListener listener)
	{
		m_errorListeners.remove(listener);
	}
	
	public void addConnectionChangeListener(final IConnectionChangeListener listener)
	{
		m_connectionListeners.add(listener);
	}
	
	public void removeConnectionChangeListener(final IConnectionChangeListener listener)
	{
		m_connectionListeners.remove(listener);
	}
	
	private void notifyConnectionsChanged(final boolean added, final INode node)
	{
		final Iterator<IConnectionChangeListener> iter = m_connectionListeners.iterator();
		while (iter.hasNext())
		{
			if (added)
			{
				iter.next().connectionAdded(node);
			}
			else
			{
				iter.next().connectionRemoved(node);
			}
		}
	}
	
	public void setAcceptNewConnections(final boolean accept)
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
			} catch (final ClosedChannelException e)
			{
				s_logger.log(Level.SEVERE, "socket closed", e);
				shutDown();
			}
			while (!m_shutdown)
			{
				try
				{
					m_acceptorSelector.select();
				} catch (final IOException e)
				{
					s_logger.log(Level.SEVERE, "Could not accept on server", e);
					shutDown();
				}
				if (m_shutdown)
					continue;
				final Set<SelectionKey> keys = m_acceptorSelector.selectedKeys();
				final Iterator<SelectionKey> iter = keys.iterator();
				while (iter.hasNext())
				{
					final SelectionKey key = iter.next();
					iter.remove();
					if (key.isAcceptable() && key.isValid())
					{
						final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
						// Accept the connection and make it non-blocking
						SocketChannel socketChannel = null;
						try
						{
							socketChannel = serverSocketChannel.accept();
							if (socketChannel == null)
							{
								continue;
							}
							socketChannel.configureBlocking(false);
							socketChannel.socket().setKeepAlive(true);
						} catch (final IOException e)
						{
							s_logger.log(Level.FINE, "Could not accept channel", e);
							try
							{
								if (socketChannel != null)
									socketChannel.close();
							} catch (final IOException e2)
							{
								s_logger.log(Level.FINE, "Could not close channel", e2);
							}
							continue;
						}
						// we are not accepting connections
						if (!m_acceptNewConnection)
						{
							try
							{
								socketChannel.close();
							} catch (final IOException e)
							{
								s_logger.log(Level.FINE, "Could not close channel", e);
							}
							continue;
						}
						final ServerQuarantineConversation conversation = new ServerQuarantineConversation(m_loginValidator, socketChannel, m_nioSocket, ServerMessenger.this);
						m_nioSocket.add(socketChannel, conversation);
					}
					else if (!key.isValid())
					{
						key.cancel();
					}
				}
			}
		}
	}
	
	private TimerTask GetUsernameUnmuteTask(final String username)
	{
		return new TimerTask()
		{
			@Override
			public void run()
			{ // lobby has a database we need to check, normal hosted games do not
				if ((isLobby() && new MutedUsernameController().getUsernameUnmuteTime(username) == -1) || (isGame())) // If the mute has expired
				{
					synchronized (m_cachedListLock)
					{
						m_liveMutedUsernames.remove(username); // Remove the username from the list of live username's muted
					}
				}
			}
		};
	}
	
	private TimerTask GetIpUnmuteTask(final String ip)
	{
		return new TimerTask()
		{
			@Override
			public void run()
			{ // lobby has a database we need to check, normal hosted games do not
				if ((isLobby() && new MutedIpController().getIpUnmuteTime(ip) == -1) || (isGame())) // If the mute has expired
				{
					synchronized (m_cachedListLock)
					{
						m_liveMutedIpAddresses.remove(ip); // Remove the ip from the list of live ip's muted
					}
				}
			}
		};
	}
	
	private TimerTask GetMacUnmuteTask(final String mac)
	{
		return new TimerTask()
		{
			@Override
			public void run()
			{ // lobby has a database we need to check, normal hosted games do not
				if ((isLobby() && new MutedMacController().getMacUnmuteTime(mac) == -1) || (isGame())) // If the mute has expired
				{
					synchronized (m_cachedListLock)
					{
						m_liveMutedMacAddresses.remove(mac); // Remove the mac from the list of live mac's muted
					}
				}
			}
		};
	}
	
	public boolean isServer()
	{
		return true;
	}
	
	public void removeConnection(final INode node)
	{
		if (node.equals(m_node))
			throw new IllegalArgumentException("Cant remove ourself!");
		NotifyPlayerRemoval(node);
		SocketChannel channel = m_nodeToChannel.remove(node);
		if (channel == null)
		{
			channel = m_nodeToChannel.remove(node);
		}
		if (channel == null)
		{
			s_logger.info("Could not remove connection to node:" + node);
			return;
		}
		m_channelToNode.remove(channel);
		m_nioSocket.close(channel);
		notifyConnectionsChanged(false, node);
		s_logger.info("Connection removed:" + node);
	}
	
	public INode getServerNode()
	{
		return m_node;
	}
	
	public void socketError(final SocketChannel channel, final Exception error)
	{
		if (channel == null)
			throw new IllegalArgumentException("Null channel");
		// already closed, dont report it again
		final INode node = m_channelToNode.get(channel);
		if (node != null)
			removeConnection(node);
	}
	
	public void socketUnqaurantined(final SocketChannel channel, final QuarantineConversation conversation)
	{
		final ServerQuarantineConversation con = (ServerQuarantineConversation) conversation;
		final INode remote = new Node(con.getRemoteName(), (InetSocketAddress) channel.socket().getRemoteSocketAddress());
		if (s_logger.isLoggable(Level.FINER))
		{
			s_logger.log(Level.FINER, "Unquarntined node:" + remote);
		}
		m_nodeToChannel.put(remote, channel);
		m_channelToNode.put(channel, remote);
		notifyConnectionsChanged(true, remote);
		s_logger.info("Connection added to:" + remote);
	}
	
	public INode getRemoteNode(final SocketChannel channel)
	{
		return m_channelToNode.get(channel);
	}
	
	public InetSocketAddress getRemoteServerSocketAddress()
	{
		return m_node.getSocketAddress();
	}
	
	@Override
	public String toString()
	{
		return "ServerMessenger LocalNode:" + m_node + " ClientNodes:" + m_nodeToChannel.keySet();
	}
}
