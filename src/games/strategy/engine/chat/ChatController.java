package games.strategy.engine.chat;

import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.util.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ChatController implements IChatController
{
	private final static Logger s_logger = Logger.getLogger(ChatController.class.getName());
	private static final String CHAT_REMOTE = "_ChatRmt";
	private static final String CHAT_CHANNEL = "_ChatCtrl";
	private final IMessenger m_messenger;
	private final IRemoteMessenger m_remoteMessenger;
	private final ModeratorController m_moderatorController;
	private final IChannelMessenger m_channelMessenger;
	private final String m_chatName;
	private final Map<INode, Tag> m_chatters = new HashMap<INode, Tag>();
	protected final Object m_mutex = new Object();
	private final String m_chatChannel;
	private long m_version;
	private final IConnectionChangeListener m_connectionChangeListener = new IConnectionChangeListener()
	{
		public void connectionAdded(final INode to)
		{
		}
		
		public void connectionRemoved(final INode to)
		{
			synchronized (m_mutex)
			{
				if (m_chatters.keySet().contains(to))
				{
					leaveChatInternal(to);
				}
			}
		}
	};
	
	public static RemoteName getChatControlerRemoteName(final String chatName)
	{
		return new RemoteName(CHAT_REMOTE + chatName, IChatController.class);
	}
	
	public static String getChatChannelName(final String chatName)
	{
		return CHAT_CHANNEL + chatName;
	}
	
	public ChatController(final String name, final IMessenger messenger, final IRemoteMessenger remoteMessenger, final IChannelMessenger channelMessenger, final ModeratorController moderatorController)
	{
		m_chatName = name;
		m_messenger = messenger;
		m_remoteMessenger = remoteMessenger;
		m_moderatorController = moderatorController;
		m_channelMessenger = channelMessenger;
		m_chatChannel = getChatChannelName(name);
		m_remoteMessenger.registerRemote(this, getChatControlerRemoteName(name));
		((IServerMessenger) m_messenger).addConnectionChangeListener(m_connectionChangeListener);
	}
	
	public ChatController(final String name, final Messengers messenger, final ModeratorController moderatorController)
	{
		this(name, messenger.getMessenger(), messenger.getRemoteMessenger(), messenger.getChannelMessenger(), moderatorController);
	}
	
	// clean up
	public void deactivate()
	{
		synchronized (m_mutex)
		{
			final IChatChannel chatter = getChatBroadcaster();
			for (final INode node : m_chatters.keySet())
			{
				m_version++;
				chatter.speakerRemoved(node, m_version);
			}
			m_remoteMessenger.unregisterRemote(getChatControlerRemoteName(m_chatName));
		}
		((IServerMessenger) m_messenger).removeConnectionChangeListener(m_connectionChangeListener);
	}
	
	private IChatChannel getChatBroadcaster()
	{
		final IChatChannel chatter = (IChatChannel) m_channelMessenger.getChannelBroadcastor(new RemoteName(m_chatChannel, IChatChannel.class));
		return chatter;
	}
	
	// a player has joined
	public Tuple<Map<INode, Tag>, Long> joinChat()
	{
		final INode node = MessageContext.getSender();
		s_logger.info("Chatter:" + node + " is joining chat:" + m_chatName);
		final Tag tag;
		if (m_moderatorController.isPlayerAdmin(node))
			tag = Tag.MODERATOR;
		else
			tag = Tag.NONE;
		synchronized (m_mutex)
		{
			m_chatters.put(node, tag);
			m_version++;
			getChatBroadcaster().speakerAdded(node, tag, m_version);
			final Map<INode, Tag> copy = new HashMap<INode, Tag>(m_chatters);
			return new Tuple<Map<INode, Tag>, Long>(copy, Long.valueOf(m_version));
		}
	}
	
	// a player has left
	public void leaveChat()
	{
		leaveChatInternal(MessageContext.getSender());
	}
	
	protected void leaveChatInternal(final INode node)
	{
		long version;
		synchronized (m_mutex)
		{
			m_chatters.remove(node);
			m_version++;
			version = m_version;
		}
		getChatBroadcaster().speakerRemoved(node, version);
		s_logger.info("Chatter:" + node + " has left chat:" + m_chatName);
	}
}
