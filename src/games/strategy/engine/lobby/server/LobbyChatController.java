/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.lobby.server;

import games.strategy.engine.chat.IChatController;
import games.strategy.engine.chat.ChatController;
import games.strategy.engine.message.*;
import games.strategy.net.*;
import java.util.List;
import games.strategy.util.Tuple;
/**
 * LobbyChatController.java
 *
 * Created on May 24, 2006, 6:06 PM
 * 
 * @author Harry
 */
public class LobbyChatController extends ChatController
{
    LobbyServer m_owner;
    /** Creates a new instance of LobbyChatController */
    public LobbyChatController(String name, IMessenger messenger, IRemoteMessenger remoteMessenger, IChannelMessenger channelMessenger,LobbyServer owner)
    {
        super(name,messenger,remoteMessenger,channelMessenger);
        remoteMessenger.unregisterRemote(getChatControlerRemoteName(name));
        remoteMessenger.registerRemote(IChatController.class,this,getChatControlerRemoteName(name));
        m_owner = owner;
    }
    public Tuple<List<INode>, Long> joinChat()
    {
        INode node = MessageContext.getSender();
        synchronized(m_mutex)
        {
            //log
            System.out.println(node.getName() + " has joined!");
        }
        return super.joinChat();
    }
    
    //a player has left
    public void leaveChat()
    {
        INode node = MessageContext.getSender();
        synchronized(m_mutex)
        {
            //log
            System.out.println(node.getName() + " has left!");
        }
        m_owner.forceRemoveServer(node);
        leaveChatInternal(node);
    }
}
