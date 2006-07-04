package games.strategy.engine.chat;

import games.strategy.engine.message.*;
import games.strategy.net.*;
import games.strategy.util.Tuple;

import java.util.*;
import java.util.logging.Logger;

public class ChatController implements IChatController
{

    private final static Logger s_logger = Logger.getLogger(ChatController.class.getName());
    
    private static final String CHAT_REMOTE = "games.strategy.engine.chat.ChatRemote";
    private static final String CHAT_CHANNEL = "games.strategy.engine.chat.ChatController";
    
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
    
    
    public static String getChatControlerRemoteName(String chatName)
    {
        return CHAT_REMOTE  + chatName;
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
        
        m_remoteMessenger.registerRemote(IChatController.class, this, getChatControlerRemoteName(name));
        m_channelMessenger.createChannel(IChatChannel.class,  getChatChannelName(m_chatChannel));
        
        
        m_messenger.addConnectionChangeListener(m_connectionChangeListener);
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
            

            m_channelMessenger.destroyChannel(m_chatChannel);
            m_remoteMessenger.unregisterRemote(getChatControlerRemoteName(m_chatName));
            
        }
        m_messenger.removeConnectionChangeListener(m_connectionChangeListener);
    }

    
    private IChatChannel getChatBroadcaster()
    {
        IChatChannel chatter = (IChatChannel) m_channelMessenger.getChannelBroadcastor(m_chatChannel);
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
        synchronized(m_mutex)
        {
            m_chatters.remove(node);
            m_version++;
            
            getChatBroadcaster().speakerRemoved(node, m_version);
            
            s_logger.info("Chatter:" + node + " has left chat:" + m_chatName);
        }
    }

    
}
