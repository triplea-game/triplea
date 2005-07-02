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

import java.util.*;

/**
 * 
 * chat logic.
 * 
 * @author Sean Bridges
 */
public class Chat implements IChatter
{
    private final String CHAT_CHANNEL = "games.strategy.engine.chat.CHAT_CHANNEL";
    private final ChatFrame m_frame;
    private final IMessenger m_messenger;
    private final IChannelMessenger m_channelMessenger;
    
	public static final String ME = "/me ";

    public static boolean isThirdPerson(String msg)
    {
		return msg.toLowerCase().startsWith(ME);
	}

    /** Creates a new instance of Chat */
    public Chat(IMessenger messenger, ChatFrame frame, IChannelMessenger channelMessenger)
    {

        m_frame = frame;
        m_messenger = messenger;
        m_channelMessenger = channelMessenger;
        
        if(!m_channelMessenger.hasChannel(CHAT_CHANNEL))
        {
            m_channelMessenger.createChannel(IChatter.class, CHAT_CHANNEL);
        }
        m_channelMessenger.registerChannelSubscriber(this, CHAT_CHANNEL);
        
        m_messenger.addConnectionChangeListener(m_connectionChangeListener);
        updateConnections();
    }

    /**
     * Stop receiving events from the messenger.
     */
    public void shutdown()
    {
        m_channelMessenger.unregisterChannelSubscriber(this, CHAT_CHANNEL);
        m_messenger.removeConnectionChangeListener(m_connectionChangeListener);
    }

    public void slap(String playerName)
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
            IChatter remote = (IChatter) m_channelMessenger.getChannelBroadcastor(CHAT_CHANNEL);
            remote.slap(m_channelMessenger.getLocalNode(), destination);
        }
    }
    
    public void slap(INode from, INode to)
    {
        if(to.equals(m_channelMessenger.getLocalNode()))
        {
            m_frame.addMessage("You were slapped by " + from.getName(), from.getName(), false);
        }
        else if(from.equals(m_channelMessenger.getLocalNode()))
        {
            m_frame.addMessage("You just slapped " + to.getName(), from.getName(), false);
        }
    }
    
    void sendMessage(String message, boolean meMessage)
    {
        
        IChatter remote = (IChatter) m_channelMessenger.getChannelBroadcastor(CHAT_CHANNEL);
        if(meMessage)
            remote.meMessage(message, m_channelMessenger.getLocalNode());
        else
            remote.chat(message, m_channelMessenger.getLocalNode());
    }


    private synchronized void updateConnections()
    {

        Set<INode> players = m_messenger.getNodes();
        List<String> playerNames = new ArrayList<String>(players.size());

        Iterator<INode> iter = players.iterator();
        while (iter.hasNext())
        {
            INode node = iter.next();
            String name = node.getName();
            playerNames.add(name);
        }

        Collections.sort(playerNames);
        m_frame.updatePlayerList(playerNames);
    }

    private IConnectionChangeListener m_connectionChangeListener = new IConnectionChangeListener()
    {

        public void connectionsChanged()
        {

            updateConnections();
        }
    };

    
    public void chat(String message, INode from)
    {
        m_frame.addMessage(message, from.getName(), false);
    }
    
    public void meMessage(String message, INode from)
    {
        m_frame.addMessage(message, from.getName(), true);
    }
    
}




interface IChatter extends IChannelSubscribor
{
    public void chat(String message, INode from);
    public void meMessage(String message, INode from);
    public void slap(INode from, INode to);
}