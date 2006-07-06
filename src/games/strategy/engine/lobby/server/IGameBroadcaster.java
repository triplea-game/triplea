package games.strategy.engine.lobby.server;

import games.strategy.engine.message.*;
import games.strategy.net.GUID;

public interface IGameBroadcaster extends IChannelSubscribor 
{
    public static final String GAME_BROADCASTER_CHANNEL = "games.strategy.engine.lobby.server.IGameBroadcaster.CHANNEL";
    
    public void gameAdded(GUID gameId, GameDescription description);
    
    public void gameUpdated(GUID gameId, GameDescription description);
    
}
