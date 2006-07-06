package games.strategy.engine.lobby.server;

import java.util.*;

import games.strategy.engine.message.*;
import games.strategy.net.*;

public class GameController implements IGameController
{
    private final Object m_mutex = new Object(); 
    private final Map<GUID, GameDescription> m_allGames = new HashMap<GUID, GameDescription>();
    private final IGameBroadcaster m_broadcaster;
    
    

    public GameController(final IGameBroadcaster broadcaster)
    {
        m_broadcaster = broadcaster;
    }

    public void postGame(GUID gameID, GameDescription description)
    {
        INode from = MessageContext.getSender();
        if(!from.equals(description.getHostedBy()))
        {
            throw new IllegalStateException("Game from the wrong host");
        }
        
        
        synchronized(m_mutex)
        {
            m_allGames.put(gameID, description);
        }
        
        m_broadcaster.gameAdded(gameID, description);
        
    }

    public void updateGame(GUID gameID, GameDescription description)
    {
        INode from = MessageContext.getSender();
        if(!from.equals(description.getHostedBy()))
        {
            throw new IllegalStateException("Game from the wrong host");
        }
        
        synchronized(m_mutex)
        {
            GameDescription oldDescription = m_allGames.get(gameID);
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
        remote.registerRemote(IGameController.class, this, GAME_CONTROLLER_REMOTE);
        
    }

}
