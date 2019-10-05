package games.strategy.engine.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.message.IRemote;

/**
 * Communication with the GamePlayer goes through the PlayerBridge to make the game network
 * transparent.
 */
public interface IPlayerBridge {
  /** Return the game data. */
  GameData getGameData();

  /**
   * Get a remote reference to the current delegate, the type of the reference is declared by the
   * delegates getRemoteType() method.
   */
  IRemote getRemoteDelegate();

  /**
   * Get a remote reference to the named delegate, the type of the reference is declared by the
   * delegates getRemoteType() method.
   */
  IRemote getRemotePersistentDelegate(String name);

  /** Get the name of the current step being executed. */
  String getStepName();

  /** Indicates the game is over. */
  boolean isGameOver();
}
