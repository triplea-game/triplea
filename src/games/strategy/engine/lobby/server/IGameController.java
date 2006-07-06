package games.strategy.engine.lobby.server;

import java.util.Map;

import games.strategy.engine.message.IRemote;
import games.strategy.net.GUID;

public interface IGameController extends IRemote
{
    public static final String GAME_CONTROLLER_REMOTE = "games.strategy.engine.lobby.server.IGameController.GAME_CONTROLLER_REMOTE";

    public void postGame(GUID gameID, GameDescription description);
    
    public void updateGame(GUID gameID, GameDescription description);
    
    public Map<GUID, GameDescription> listGames();
    
}
