package games.strategy.engine.gamePlayer;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IRemote;

/**
 * Used for both IGamePlayer (used by the server, etc.) and specific game players such as ITripleaPlayer and IGridGamePlayer (used by delegates for communication, etc.)
 * 
 * @author veqryn
 * 
 */
public interface IRemotePlayer extends IRemote
{
	/**
	 * 
	 * @return the id of this player. This id is initialized by the initialize method in IGamePlayer.
	 */
	public PlayerID getPlayerID();
}
