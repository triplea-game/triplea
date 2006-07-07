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

import java.util.*;
import java.util.logging.Logger;

import games.strategy.engine.message.*;
import games.strategy.net.*;

public class LobbyGameController implements ILobbyGameController
{
    private final static Logger s_logger = Logger.getLogger(LobbyGameController.class.getName());
    
    private final Object m_mutex = new Object(); 
    private final Map<GUID, GameDescription> m_allGames = new HashMap<GUID, GameDescription>();
    private final ILobbyGameBroadcaster m_broadcaster;
    private final IMessenger m_messenger;
    
    

    public LobbyGameController(final ILobbyGameBroadcaster broadcaster, IMessenger messenger)
    {
        m_broadcaster = broadcaster;
        m_messenger = messenger;
        
        m_messenger.addConnectionChangeListener(new IConnectionChangeListener()
        {
        
            public void connectionRemoved(INode to)
            {             
                connectionLost(to);
            }
        
            public void connectionAdded(INode to)
            {}
        
        });
        
    }

    private void connectionLost(INode to)
    {
        List<GUID> removed = new ArrayList<GUID>();
        synchronized(m_mutex)
        {
            Iterator<GUID> keys = m_allGames.keySet().iterator();
            while(keys.hasNext())
            {
                GUID key = keys.next();
                GameDescription game = m_allGames.get(key);
                if(game.getHostedBy().equals(to))
                {
                    keys.remove();
                    removed.add(key);
                }
            }
            
        }
        
        for(GUID guid : removed)
        {
            m_broadcaster.gameRemoved(guid);
        }
        
    }

    public void postGame(GUID gameID, GameDescription description)
    {
        INode from = MessageContext.getSender();
        assertCorrectHost(description, from);
        
        
        synchronized(m_mutex)
        {
            m_allGames.put(gameID, description);
        }
        
        m_broadcaster.gameAdded(gameID, description);
        
    }

    private void assertCorrectHost(GameDescription description, INode from)
    {
        if(!from.getAddress().getHostAddress().equals(description.getHostedBy().getAddress().getHostAddress() ))
        {
            s_logger.severe("Game modified from wrong host, from:" + from + " game host:" + description.getHostedBy());
            throw new IllegalStateException("Game from the wrong host");
        }
    }

    public void updateGame(GUID gameID, GameDescription description)
    {
        INode from = MessageContext.getSender();
        assertCorrectHost(description, from);
        
        synchronized(m_mutex)
        {
            GameDescription oldDescription = m_allGames.get(gameID);
            
            //out of order updates
            //ignore, we already have the latest
            if(oldDescription.getVersion() > description.getVersion())
                return;
            
            if(!oldDescription.getHostedBy().equals(description.getHostedBy()))
            {
                throw new IllegalStateException("Game modified by wrong host");
            }
            m_allGames.put(gameID, oldDescription);
        }
        
        m_broadcaster.gameUpdated(gameID, description);
        
    }

    public Map<GUID, GameDescription> listGames()
    {
        synchronized(m_mutex)
        {
            Map<GUID, GameDescription> rVal = new HashMap<GUID, GameDescription>(m_allGames);
            return rVal;
        }
    }

    public void register(IRemoteMessenger remote)
    {
        remote.registerRemote(ILobbyGameController.class, this, GAME_CONTROLLER_REMOTE);
        
    }

}
