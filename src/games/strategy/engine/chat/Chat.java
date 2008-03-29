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

import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * chat logic.<p>
 * 
 * A chat can be bound to multiple chat panels.<p>
 * 
 * @author Sean Bridges
 */
public class Chat 
{    
    private final List<IChatListener>  m_listeners = new CopyOnWriteArrayList<IChatListener>();
    private final Messengers m_messengers;
    private final String m_chatChannelName;
    private final String m_chatName;
    private final SentMessagesHistory m_sentMessages;
    private long m_chatInitVersion = -1;
    
    private final Object m_mutex = new Object();
    private List<INode> m_nodes;
    
    private List<Runnable> m_queuedInitMessages = new ArrayList<Runnable>();
    private List<ChatMessage> m_chatHistory = new ArrayList<ChatMessage>();
    private final StatusManager m_statusManager;

    /** Creates a new instance of Chat */
    public Chat(String chatName, Messengers messengers)
    {
        m_messengers = messengers;
        m_statusManager = new StatusManager(messengers);
        
        m_chatChannelName = ChatController.getChatChannelName(chatName);
        m_chatName = chatName;
        m_sentMessages = new SentMessagesHistory();
        
        init();
        
    }
    
    public Chat(IMessenger messenger, String chatName, IChannelMessenger channelMessenger, IRemoteMessenger remoteMessenger)
    {
        this(chatName, new Messengers(messenger, remoteMessenger, channelMessenger));
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
    
    public StatusManager getStatusManager()
    {
        return m_statusManager;
    }
    
    public void removeChatListener(IChatListener listener)
    {
        m_listeners.remove(listener);
    }
    
    public Object getMutex()
    {
        return m_mutex;
    }
    
    private void init()
    {
         
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
        
        
        IChatController controller = (IChatController) m_messengers.getRemoteMessenger().getRemote(ChatController.getChatControlerRemoteName(m_chatName));
        
        m_messengers.getChannelMessenger().registerChannelSubscriber(m_chatChannelSubscribor, new RemoteName(m_chatChannelName, IChatChannel.class));   
        
        Tuple<List<INode>, Long> init = controller.joinChat();
        
        
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
       
        m_messengers.getChannelMessenger().unregisterChannelSubscriber(m_chatChannelSubscribor, new RemoteName( m_chatChannelName, IChatChannel.class));
        
        
        if(m_messengers.getMessenger().isConnected())
        {
            RemoteName  chatControllerName = ChatController.getChatControlerRemoteName(m_chatName);
            IChatController controller = (IChatController) m_messengers.getRemoteMessenger().getRemote(chatControllerName);
            controller.leaveChat();
        }
        
        
    }

    public void sendSlap(String playerName)
    {
        
        IChatChannel remote = (IChatChannel) m_messengers.getChannelMessenger().getChannelBroadcastor(new RemoteName(m_chatChannelName, IChatChannel.class));
        remote.slapOccured(playerName);

    }
    
    void sendMessage(String message, boolean meMessage)
    {
        IChatChannel remote = (IChatChannel) m_messengers.getChannelMessenger().getChannelBroadcastor(new RemoteName(m_chatChannelName, IChatChannel.class));
        if(meMessage)
            remote.meMessageOccured(message);
        else
            remote.chatOccured(message);
        
        m_sentMessages.append(message);
    }


    private void updateConnections()
    {

        synchronized(m_mutex)
        {
            if(m_nodes == null)
                return;
            
            List<INode> playerNames = new ArrayList<INode>(m_nodes);
            
           

            Collections.sort(playerNames);
            
            for(IChatListener listener: m_listeners)
            {
                listener.updatePlayerList(playerNames);
            }
        }
    }

   public INode getLocalNode()
   {
       return m_messengers.getMessenger().getLocalNode();
   }

    private IChatChannel m_chatChannelSubscribor = new IChatChannel()
    {
        
        private void assertMessageFromServer()
        {
            
            INode senderNode = MessageContext.getSender();
            INode serverNode = m_messengers.getMessenger().getServerNode();
            
            //this will happen if the message is queued
            //but to queue a message, we must first test where it came from
            //so it is safe in this case to return ok
            if(senderNode == null)
                return;
            
            if(!senderNode.equals(serverNode))
                throw new IllegalStateException("The node:" + senderNode + " sent a message as the server!");
        }
        
        
        public void chatOccured(String message)
        {
            INode from = MessageContext.getSender();
            synchronized(m_mutex)
            {
                m_chatHistory.add(new ChatMessage(message, from.getName(), false ));
                for(IChatListener listener: m_listeners)
                {
                    listener.addMessage(message, from.getName(), false);
                }
                
                //limit the number of messages in our history.
                while(m_chatHistory.size() > 1000)
                {
                    m_chatHistory.remove(0);
                }
            }
        }
        
        public void meMessageOccured(String message)
        {
            INode from = MessageContext.getSender();
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
            assertMessageFromServer();
            
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
                    
                    for(IChatListener listener: m_listeners)
                    {
                        listener.addStatusMessage(node.getName() + " has joined");
                    }
                    
                }
            }
            
        }

        public void speakerRemoved(final INode node, final long version)
        {
            assertMessageFromServer();
            
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
                    
                    for(IChatListener listener: m_listeners)
                    {
                        listener.addStatusMessage(node.getName() + " has left");
                    }
                    
                }
                
                
            }
        }
        
        public void slapOccured(String to)
        {
            INode from = MessageContext.getSender();
            
            synchronized(m_mutex)
            {
                if(to.equals(m_messengers.getChannelMessenger().getLocalNode().getName()))
                {
                    for(IChatListener listener: m_listeners)
                    {
                        String message = "You were slapped by " + from.getName();
                        m_chatHistory.add(new ChatMessage(message, from.getName(), false ));
                        listener.addMessage(message, from.getName(), false);
                    }
                }
                else if(from.equals(m_messengers.getChannelMessenger().getLocalNode()))
                {
                    for(IChatListener listener: m_listeners)
                    {
                        String message = "You just slapped " + to;
                        m_chatHistory.add(new ChatMessage(message, from.getName(), false ));
                        listener.addMessage(message, from.getName(), false);
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

