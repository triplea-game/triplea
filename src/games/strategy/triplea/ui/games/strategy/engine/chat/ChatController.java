package games.strategy.engine.chat;

import games.strategy.engine.message.*;
import games.strategy.net.*;
import games.strategy.util.Tuple;

import java.util.*;
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

        public void connectionAdded(INode to)
        {}

        public void connectionRemoved(INode to)
        {
            synchronized(m_mutex)
            {
                if(m_chatters.contains(to))
                {
                    leaveChatInternal(to);
                }
            }
            
        }
        
        
    };
    
    
    public static RemoteName getChatControlerRemoteName(String chatName)
    {
        return new RemoteName(CHAT_REMOTE  + chatName, IChatController.class);
    }
    
    public static String getChatChannelName(String chatName)
    {
        return  CHAT_CHANNEL + chatName;
    }
    
    
    
    public ChatController(String name, IMessenger messenger, IRemoteMessenger remoteMessenger, IChannelMessenger channelMessenger)
    {
        m_chatName = name;
        m_messenger = messenger;
        m_remoteMessenger = remoteMessenger;
        m_channelMessenger = channelMessenger;
        m_chatChannel = getChatChannelName(name);
        
        m_remoteMessenger.registerRemote(this, getChatControlerRemoteName(name));
        
        
        
        ((IServerMessenger) m_messenger).addConnectionChangeListener(m_connectionChangeListener);
    }
    
  
    public ChatController(String name, Messengers messenger)
    {
        this(name, messenger.getMessenger(), messenger.getRemoteMessenger(), messenger.getChannelMessenger());
    }

    //clean up
    public void deactivate()
    {
        synchronized(m_mutex)
        {
            IChatChannel chatter = getChatBroadcaster();
            for(INode node : m_chatters)
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
        IChatChannel chatter = (IChatChannel) m_channelMessenger.getChannelBroadcastor(new RemoteName(m_chatChannel, IChatChannel.class));
        return chatter;
    }
    
    
    //a player has joined
    public Tuple<List<INode>, Long> joinChat()
    {
        INode node = MessageContext.getSender();
        s_logger.info("Chatter:" + node + " is joining chat:" + m_chatName);
        
        synchronized(m_mutex)
        {
            m_chatters.add(node);
            m_version++;
            
            getChatBroadcaster().speakerAdded(node, m_version);
            ArrayList<INode> copy = new ArrayList<INode>(m_chatters);
            return new Tuple<List<INode>, Long>(copy, new Long(m_version) );
        }
        
        
    }
    
    //a player has left
    public void leaveChat()
    {
        leaveChatInternal(MessageContext.getSender());
    }
    
    protected void leaveChatInternal(INode node)
    {
        long version;
        synchronized(m_mutex)
        {
            m_chatters.remove(node);
            m_version++;
            version = m_version;
        }
        
        getChatBroadcaster().speakerRemoved(node, version);        
        s_logger.info("Chatter:" + node + " has left chat:" + m_chatName);

    }

    
}
