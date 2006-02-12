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

import games.strategy.engine.message.*;
import games.strategy.net.*;
import games.strategy.util.Tuple;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * chat logic.  each chat is bound to one chatPanel
 * 
 * @author Sean Bridges
 */
public class Chat 
{    
    private final List<IChatListener>  m_listeners = new CopyOnWriteArrayList<IChatListener>();
    private final IMessenger m_messenger;
    private final IChannelMessenger m_channelMessenger;
    private final IRemoteMessenger m_remoteMesenger;
    private final String m_chatChannelName;
    private final String m_chatName;
    private final SentMessagesHistory m_sentMessages;
    private long m_chatInitVersion = -1;
    
    private final Object m_mutex = new Object();
    private List<INode> m_nodes;
    
    private List<Runnable> m_queuedInitMessages = new ArrayList<Runnable>();
    private List<ChatMessage> m_chatHistory = new ArrayList<ChatMessage>();


    /** Creates a new instance of Chat */
    public Chat(IMessenger messenger, String chatName, IChannelMessenger channelMessenger, IRemoteMessenger remoteMessenger)
    {
        m_messenger = messenger;
        m_channelMessenger = channelMessenger;
        m_remoteMesenger = remoteMessenger;
        m_chatChannelName = ChatController.getChatChannelName(chatName);
        m_chatName = chatName;
        m_sentMessages = new SentMessagesHistory();
        
    }
    
    public SentMessagesHistory getSentMessagesHistory()
    {
        return m_sentMessages;
    }
    
    public void addChatListener(IChatListener listener)
    {
        m_listeners.add(listener);
        updateConnections();
    }
    
    public void removeChatListener(IChatListener listener)
    {
        m_listeners.remove(listener);
    }
    
    public Object getMutex()
    {
        return m_mutex;
    }
    
    void init()
    {
         String  chatControllerName = ChatController.getChatControlerRemoteName(m_chatName);
         if(!m_remoteMesenger.hasRemote(chatControllerName))
         {
             m_remoteMesenger.waitForRemote(chatControllerName, 2000);
         }
         
         
         //the order of events is significant.
         //
         // 
         //1  register our channel listener
         //    once the channel is registered, we are guarantted that
         //    when we recieve the response from our init(...) message, our channel
         //    subscribor has been added, and will see any messages broadcasted by the server
         //
         //2  call the init message on the server remote. Any add or join messages sent from the server
         //   will queue until we recieve the init return value (they queue since they see the init version is -1)
         //
         //3  when we receive the init message response, acquire the lock, and initialize our state
         //   and run any queued messages.  Queued messages may be ignored if the 
         //   server version is incorrect.
         //
         // this all seems a lot more involved than it needs to be.
         
        IChatController controller = (IChatController) m_remoteMesenger.getRemote(chatControllerName);
        
        if(!m_channelMessenger.hasChannel(m_chatChannelName))
        {
            m_channelMessenger.createChannel(IChatChannel.class, m_chatChannelName);
        }
        m_channelMessenger.registerChannelSubscriber(m_chatChannelSubscribor, m_chatChannelName);   
        
        Tuple<List<INode>, Long> init = controller.joinChat(m_messenger.getLocalNode());
        if(init == null)
            return;
        
        
        synchronized(m_mutex)
        {
            m_chatInitVersion = init.getSecond().longValue();
            m_nodes = init.getFirst();
            
            for(Runnable job: m_queuedInitMessages)
            {
                job.run();
            }
            m_queuedInitMessages = null;
            
        }
        updateConnections();
    }
    

    /**
     * Stop receiving events from the messenger.
     */
    public void shutdown()
    {
       
        m_channelMessenger.unregisterChannelSubscriber(m_chatChannelSubscribor, ChatController.getChatChannelName(m_chatChannelName));
        
        
        if(m_messenger.isConnected())
        {
            String  chatControllerName = ChatController.getChatControlerRemoteName(m_chatName);
            IChatController controller = (IChatController) m_remoteMesenger.getRemote(chatControllerName);
            controller.leaveChat(m_messenger.getLocalNode());
        }
        
        
    }

    public void sendSlap(String playerName)
    {
        
        Iterator<INode> iter = m_messenger.getNodes().iterator();
        INode destination = null;
        while (iter.hasNext())
        {
            INode node = iter.next();
            String name = node.getName();
            if (name.equals(playerName))
            {
                destination = node;
                break;
            }
        }
        if (destination != null)
        {          
            IChatChannel remote = (IChatChannel) m_channelMessenger.getChannelBroadcastor(ChatController.getChatChannelName(m_chatChannelName));
            remote.slapOccured(m_channelMessenger.getLocalNode(), destination);
        }
    }
    
    void sendMessage(String message, boolean meMessage)
    {
        IChatChannel remote = (IChatChannel) m_channelMessenger.getChannelBroadcastor(m_chatChannelName);
        if(meMessage)
            remote.meMessageOccured(message, m_channelMessenger.getLocalNode());
        else
            remote.chatOccured(message, m_channelMessenger.getLocalNode());
        
        m_sentMessages.append(message);
    }


    private void updateConnections()
    {

        synchronized(m_mutex)
        {
            if(m_nodes == null)
                return;
            
            List<String> playerNames = new ArrayList<String>(m_nodes.size());
            
            Iterator<INode> iter = m_nodes.iterator();
            while (iter.hasNext())
            {
                INode node = iter.next();
                String name = node.getName();
                playerNames.add(name);
            }

            Collections.sort(playerNames);
            
            for(IChatListener listener: m_listeners)
            {
                listener.updatePlayerList(playerNames);
            }
        }
    }

   public INode getLocalNode()
   {
       return m_messenger.getLocalNode();
   }

    private IChatChannel m_chatChannelSubscribor = new IChatChannel()
    {
        public void chatOccured(String message, INode from)
        {
            synchronized(m_mutex)
            {
                m_chatHistory.add(new ChatMessage(message, from.getName(), false ));
                for(IChatListener listener: m_listeners)
                {
                    listener.addMessage(message, from.getName(), false);
                }
            }
        }
        
        public void meMessageOccured(String message, INode from)
        {
            synchronized(m_mutex)
            {
                m_chatHistory.add(new ChatMessage(message, from.getName(), true));
                for(IChatListener listener: m_listeners)
                {
                    listener.addMessage(message, from.getName(), true);
                }
            }
        }

        public void speakerAdded(final INode node, final long version)
        {
            synchronized(m_mutex)
            {
                if(m_chatInitVersion == -1)
                {
                    m_queuedInitMessages.add(new Runnable()
                    {
                    
                        public void run()
                        {
                               speakerAdded(node, version);
                        }
                    
                    });
                    return;
                    
                }
                
                if(version > m_chatInitVersion)
                {
                    m_nodes.add(node);
                    updateConnections();
                }
            }
            
        }

        public void speakerRemoved(final INode node, final long version)
        {
            synchronized(m_mutex)
            {
                
                if(m_chatInitVersion == -1)
                {
                    m_queuedInitMessages.add(new Runnable()
                    {
                    
                        public void run()
                        {
                               speakerRemoved(node, version);
                        }
                    
                    });
                    return;
                    
                }
                
                
                if(version > m_chatInitVersion)
                {
                    m_nodes.remove(node);
                    updateConnections();
                }
                
                
            }
        }
        
        public void slapOccured(INode from, INode to)
        {
            synchronized(m_mutex)
            {
                if(to.equals(m_channelMessenger.getLocalNode()))
                {
                    for(IChatListener listener: m_listeners)
                    {
                        listener.addMessage("You were slapped by " + from.getName(), from.getName(), false);
                    }
                }
                else if(from.equals(m_channelMessenger.getLocalNode()))
                {
                    for(IChatListener listener: m_listeners)
                    {
                        listener.addMessage("You just slapped " + to.getName(), from.getName(), false);
                    }
                }
            }
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
    
    public ChatMessage(String message, String from, boolean isMeMessage)
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

