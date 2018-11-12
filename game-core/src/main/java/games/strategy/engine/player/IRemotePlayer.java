package games.strategy.engine.player;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.message.IRemote;

/**
 * Used for both IGamePlayer (used by the server, etc.) and specific game players such as ITripleaPlayer and
 * IGridGamePlayer (used by
 * delegates for communication, etc.)
 */
public interface IRemotePlayer extends IRemote {
  /**
   * Returns the id of this player. This id is initialized by the initialize method in IGamePlayer.
   */
  PlayerId getPlayerId();
}
