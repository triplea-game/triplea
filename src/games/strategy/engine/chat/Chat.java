/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * Chat.java
 * 
 * Created on January 14, 2002, 11:10 AM
 */
package games.strategy.engine.chat;

import games.strategy.engine.chat.IChatController.Tag;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * chat logic.
 * <p>
 * 
 * A chat can be bound to multiple chat panels.
 * <p>
 * 
 * @author Sean Bridges
 */
public class Chat
{
	private final List<IChatListener> m_listeners = new CopyOnWriteArrayList<IChatListener>();
	private final Messengers m_messengers;
	private final String m_chatChannelName;
	private final String m_chatName;
	private final SentMessagesHistory m_sentMessages;
	private volatile long m_chatInitVersion = -1;
	// mutex used for access synchronization to nodes
	private final Object m_mutexNodes = new Object(); // TODO: check if this mutex is used for something else as well
	private List<INode> m_nodes;
	// this queue is filled ONLY in init phase when m_chatInitVersion is default (-1) and nodes should not be changed until end of initialization
	private final Object m_mutexQueue = new Object(); // synchronizes access to queue
	private List<Runnable> m_queuedInitMessages = new ArrayList<Runnable>();
	private final List<ChatMessage> m_chatHistory = new ArrayList<ChatMessage>();
	private final StatusManager m_statusManager;
	private final ChatIgnoreList m_ignoreList = new ChatIgnoreList();
	private final HashMap<INode, LinkedHashSet<String>> m_notesMap = new HashMap<INode, LinkedHashSet<String>>();
	private static final String TAG_MODERATOR = "[Mod]";
	private final CHAT_SOUND_PROFILE m_chatSoundProfile;
	
	
	public enum CHAT_SOUND_PROFILE
	{
		LOBBY_CHATROOM, GAME_CHATROOM, NO_SOUND
	}
	
	private void addToNotesMap(final INode node, final Tag tag)
	{
		if (tag == Tag.NONE)
			return;
		final LinkedHashSet<String> current = getTagText(tag);
		m_notesMap.put(node, current);
	}
	
	private static LinkedHashSet<String> getTagText(final Tag tag)
	{
		final LinkedHashSet<String> rVal = new LinkedHashSet<String>();
		if (tag == Tag.NONE)
			return null;
		if (tag == Tag.MODERATOR)
			rVal.add(TAG_MODERATOR);
		// add more here....
		return rVal;
	}
	
	public String getNotesForNode(final INode node)
	{
		final LinkedHashSet<String> notes = m_notesMap.get(node);
		if (notes == null)
			return null;
		final StringBuilder sb = new StringBuilder("");
		for (final String note : notes)
		{
			sb.append(" ");
			sb.append(note);
		}
		return sb.toString();
	}
	
	/** Creates a new instance of Chat */
	public Chat(final String chatName, final Messengers messengers, final CHAT_SOUND_PROFILE chatSoundProfile)
	{
		m_chatSoundProfile = chatSoundProfile;
		m_messengers = messengers;
		m_statusManager = new StatusManager(messengers);
		m_chatChannelName = ChatController.getChatChannelName(chatName);
		m_chatName = chatName;
		m_sentMessages = new SentMessagesHistory();
		init();
	}
	
	public Chat(final IMessenger messenger, final String chatName, final IChannelMessenger channelMessenger, final IRemoteMessenger remoteMessenger, final CHAT_SOUND_PROFILE chatSoundProfile)
	{
		this(chatName, new Messengers(messenger, remoteMessenger, channelMessenger), chatSoundProfile);
	}
	
	public SentMessagesHistory getSentMessagesHistory()
	{
		return m_sentMessages;
	}
	
	public void addChatListener(final IChatListener listener)
	{
		m_listeners.add(listener);
		updateConnections();
	}
	
	public StatusManager getStatusManager()
	{
		return m_statusManager;
	}
	
	public void removeChatListener(final IChatListener listener)
	{
		m_listeners.remove(listener);
	}
	
	public Object getMutex()
	{
		return m_mutexNodes;
	}
	
	private void init()
	{
		(new Runnable()
		{
			public void run()
			{
				// the order of events is significant.
				//
				//
				// 1 register our channel listener
				// once the channel is registered, we are guaranteed that
				// when we receive the response from our init(...) message, our channel
				// subscriber has been added, and will see any messages broadcasted by the server
				//
				// 2 call the init message on the server remote. Any add or join messages sent from the server
				// will queue until we receive the init return value (they queue since they see the init version is -1)
				//
				// 3 when we receive the init message response, acquire the lock, and initialize our state
				// and run any queued messages. Queued messages may be ignored if the
				// server version is incorrect.
				//
				// this all seems a lot more involved than it needs to be.
				final IChatController controller = (IChatController) m_messengers.getRemoteMessenger().getRemote(ChatController.getChatControlerRemoteName(m_chatName));
				m_messengers.getChannelMessenger().registerChannelSubscriber(m_chatChannelSubscribor, new RemoteName(m_chatChannelName, IChatChannel.class));
				final Tuple<Map<INode, Tag>, Long> init = controller.joinChat();
				final Map<INode, Tag> chatters = init.getFirst();
				synchronized (m_mutexNodes)
				{
					m_nodes = new ArrayList<INode>(chatters.keySet());
				}
				m_chatInitVersion = init.getSecond().longValue();
				(new Runnable()
				{
					public void run()
					{
						synchronized (m_mutexQueue)
						{
							m_queuedInitMessages.add(0, new Runnable()
							{
								public void run()
								{
									assignNodeTags(chatters);
								}
							});
							for (final Runnable job : m_queuedInitMessages)
							{
								job.run();
							}
							m_queuedInitMessages = null;
						}
						updateConnections();
					}
				}).run();
			}
		}).run();
	}
	
	/**
	 * Call only when mutex for node is locked!
	 * 
	 * @param chatters
	 *            map from node to tag
	 */
	private void assignNodeTags(final Map<INode, Tag> chatters)
	{
		for (final INode node : chatters.keySet())
		{
			final Tag tag = chatters.get(node);
			addToNotesMap(node, tag);
		}
	}
	
	/**
	 * Stop receiving events from the messenger.
	 */
	public void shutdown()
	{
		m_messengers.getChannelMessenger().unregisterChannelSubscriber(m_chatChannelSubscribor, new RemoteName(m_chatChannelName, IChatChannel.class));
		if (m_messengers.getMessenger().isConnected())
		{
			final RemoteName chatControllerName = ChatController.getChatControlerRemoteName(m_chatName);
			final IChatController controller = (IChatController) m_messengers.getRemoteMessenger().getRemote(chatControllerName);
			controller.leaveChat();
		}
	}
	
	public void sendSlap(final String playerName)
	{
		final IChatChannel remote = (IChatChannel) m_messengers.getChannelMessenger().getChannelBroadcastor(new RemoteName(m_chatChannelName, IChatChannel.class));
		remote.slapOccured(playerName);
	}
	
	void sendMessage(final String message, final boolean meMessage)
	{
		final IChatChannel remote = (IChatChannel) m_messengers.getChannelMessenger().getChannelBroadcastor(new RemoteName(m_chatChannelName, IChatChannel.class));
		if (meMessage)
			remote.meMessageOccured(message);
		else
			remote.chatOccured(message);
		m_sentMessages.append(message);
	}
	
	private void updateConnections()
	{
		synchronized (m_mutexNodes)
		{
			if (m_nodes == null)
				return;
			final List<INode> playerNames = new ArrayList<INode>(m_nodes);
			Collections.sort(playerNames);
			for (final IChatListener listener : m_listeners)
			{
				listener.updatePlayerList(playerNames);
			}
		}
	}
	
	public void setIgnored(final INode node, final boolean isIgnored)
	{
		if (isIgnored)
		{
			m_ignoreList.add(node.getName());
		}
		else
		{
			m_ignoreList.remove(node.getName());
		}
	}
	
	public boolean isIgnored(final INode node)
	{
		return m_ignoreList.shouldIgnore(node.getName());
	}
	
	public INode getLocalNode()
	{
		return m_messengers.getMessenger().getLocalNode();
	}
	
	public INode getServerNode()
	{
		return m_messengers.getMessenger().getServerNode();
	}
	
	private final List<INode> m_playersThatLeft_Last10 = new ArrayList<INode>();
	
	public List<INode> GetPlayersThatLeft_Last10()
	{
		return new ArrayList<INode>(m_playersThatLeft_Last10);
	}
	
	public List<INode> GetOnlinePlayers()
	{
		return new ArrayList<INode>(m_nodes);
	}
	
	private final IChatChannel m_chatChannelSubscribor = new IChatChannel()
	{
		private void assertMessageFromServer()
		{
			final INode senderNode = MessageContext.getSender();
			final INode serverNode = m_messengers.getMessenger().getServerNode();
			// this will happen if the message is queued
			// but to queue a message, we must first test where it came from
			// so it is safe in this case to return ok
			if (senderNode == null)
				return;
			if (!senderNode.equals(serverNode))
				throw new IllegalStateException("The node:" + senderNode + " sent a message as the server!");
		}
		
		public void chatOccured(final String message)
		{
			final INode from = MessageContext.getSender();
			if (isIgnored(from))
			{
				return;
			}
			synchronized (m_mutexNodes)
			{
				m_chatHistory.add(new ChatMessage(message, from.getName(), false));
				for (final IChatListener listener : m_listeners)
				{
					listener.addMessage(message, from.getName(), false);
				}
				// limit the number of messages in our history.
				while (m_chatHistory.size() > 1000)
				{
					m_chatHistory.remove(0);
				}
			}
		}
		
		public void meMessageOccured(final String message)
		{
			final INode from = MessageContext.getSender();
			if (isIgnored(from))
			{
				return;
			}
			synchronized (m_mutexNodes)
			{
				m_chatHistory.add(new ChatMessage(message, from.getName(), true));
				for (final IChatListener listener : m_listeners)
				{
					listener.addMessage(message, from.getName(), true);
				}
			}
		}
		
		public void speakerAdded(final INode node, final Tag tag, final long version)
		{
			assertMessageFromServer();
			if (m_chatInitVersion == -1)
			{
				synchronized (m_mutexQueue)
				{
					if (m_queuedInitMessages == null)
						speakerAdded(node, tag, version);
					else
						m_queuedInitMessages.add(new Runnable()
						{
							public void run()
							{
								speakerAdded(node, tag, version);
							}
						});
				}
				return;
			}
			if (version > m_chatInitVersion)
			{
				synchronized (m_mutexNodes)
				{
					m_nodes.add(node);
					addToNotesMap(node, tag);
					updateConnections();
				}
				for (final IChatListener listener : m_listeners)
				{
					listener.addStatusMessage(node.getName() + " has joined");
					if (m_chatSoundProfile == CHAT_SOUND_PROFILE.GAME_CHATROOM)
					{
						ClipPlayer.play(SoundPath.CLIP_CHAT_JOIN_GAME, null);
					}
				}
			}
		}
		
		public void speakerRemoved(final INode node, final long version)
		{
			assertMessageFromServer();
			if (m_chatInitVersion == -1)
			{
				synchronized (m_mutexQueue)
				{
					if (m_queuedInitMessages == null)
						speakerRemoved(node, version);
					else
						m_queuedInitMessages.add(new Runnable()
						{
							public void run()
							{
								speakerRemoved(node, version);
							}
						});
				}
				return;
			}
			if (version > m_chatInitVersion)
			{
				synchronized (m_mutexNodes)
				{
					m_nodes.remove(node);
					m_notesMap.remove(node);
					updateConnections();
				}
				for (final IChatListener listener : m_listeners)
				{
					listener.addStatusMessage(node.getName() + " has left");
				}
				m_playersThatLeft_Last10.add(node);
				if (m_playersThatLeft_Last10.size() > 10)
					m_playersThatLeft_Last10.remove(0);
			}
		}
		
		public void speakerTagUpdated(final INode node, final Tag tag)
		{
			synchronized (m_mutexNodes)
			{
				m_notesMap.remove(node);
				addToNotesMap(node, tag);
				updateConnections();
			}
		}
		
		public void slapOccured(final String to)
		{
			final INode from = MessageContext.getSender();
			if (isIgnored(from))
			{
				return;
			}
			synchronized (m_mutexNodes)
			{
				if (to.equals(m_messengers.getChannelMessenger().getLocalNode().getName()))
				{
					for (final IChatListener listener : m_listeners)
					{
						final String message = "You were slapped by " + from.getName();
						m_chatHistory.add(new ChatMessage(message, from.getName(), false));
						listener.addMessageWithSound(message, from.getName(), false, SoundPath.CLIP_CHAT_SLAP);
					}
				}
				else if (from.equals(m_messengers.getChannelMessenger().getLocalNode()))
				{
					for (final IChatListener listener : m_listeners)
					{
						final String message = "You just slapped " + to;
						m_chatHistory.add(new ChatMessage(message, from.getName(), false));
						listener.addMessageWithSound(message, from.getName(), false, SoundPath.CLIP_CHAT_SLAP);
					}
				}
			}
		}
		
		public void ping()
		{
			// System.out.println("Pinged");
		}
	};
	
	/**
	 * 
	 * While using this, you should synchronize on getMutex().
	 * 
	 * @return the messages that have occured so far.
	 */
	public List<ChatMessage> getChatHistory()
	{
		return m_chatHistory;
	}
}


class ChatMessage
{
	private final String m_message;
	private final String m_from;
	private final boolean m_isMeMessage;
	
	public ChatMessage(final String message, final String from, final boolean isMeMessage)
	{
		m_message = message;
		m_from = from;
		m_isMeMessage = isMeMessage;
	}
	
	public String getFrom()
	{
		return m_from;
	}
	
	public boolean isMeMessage()
	{
		return m_isMeMessage;
	}
	
	public String getMessage()
	{
		return m_message;
	}
}
