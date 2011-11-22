package games.strategy.engine.chat;

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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ChatController implements IChatController
{
	private final static Logger s_logger = Logger.getLogger(ChatController.class.getName());
	private static final String CHAT_REMOTE = "_ChatRmt";
	private static final String CHAT_CHANNEL = "_ChatCtrl";
	private final IMessenger m_messenger;
	private final IRemoteMessenger m_remoteMessenger;
	private final IChannelMessenger m_channelMessenger;
	private final String m_chatName;
	private final List<INode> m_chatters = new ArrayList<INode>();
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
				if (m_chatters.contains(to))
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
	
	public ChatController(final String name, final IMessenger messenger, final IRemoteMessenger remoteMessenger, final IChannelMessenger channelMessenger)
	{
		m_chatName = name;
		m_messenger = messenger;
		m_remoteMessenger = remoteMessenger;
		m_channelMessenger = channelMessenger;
		m_chatChannel = getChatChannelName(name);
		m_remoteMessenger.registerRemote(this, getChatControlerRemoteName(name));
		((IServerMessenger) m_messenger).addConnectionChangeListener(m_connectionChangeListener);
	}
	
	public ChatController(final String name, final Messengers messenger)
	{
		this(name, messenger.getMessenger(), messenger.getRemoteMessenger(), messenger.getChannelMessenger());
	}
	
	// clean up
	public void deactivate()
	{
		synchronized (m_mutex)
		{
			final IChatChannel chatter = getChatBroadcaster();
			for (final INode node : m_chatters)
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
	public Tuple<List<INode>, Long> joinChat()
	{
		final INode node = MessageContext.getSender();
		s_logger.info("Chatter:" + node + " is joining chat:" + m_chatName);
		synchronized (m_mutex)
		{
			m_chatters.add(node);
			m_version++;
			getChatBroadcaster().speakerAdded(node, m_version);
			final ArrayList<INode> copy = new ArrayList<INode>(m_chatters);
			return new Tuple<List<INode>, Long>(copy, new Long(m_version));
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
