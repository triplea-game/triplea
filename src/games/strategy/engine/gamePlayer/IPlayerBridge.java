package games.strategy.engine.gamePlayer;

import java.util.Properties;

import games.strategy.engine.data.GameData;
import games.strategy.engine.message.IRemote;

/**
 * Communication with the GamePlayer goes through the PlayerBridge to
 * make the game network transparent.
 */
public interface IPlayerBridge {
  /**
   * Return the game data
   */
  public GameData getGameData();

  /**
   * Get a remote reference to the current delegate, the type of the reference
   * is declared by the delegates getRemoteType() method
   */
  public IRemote getRemoteDelegate();

  /**
   * Get a remote reference to the named delegate, the type of the reference
   * is declared by the delegates getRemoteType() method
   */
  public IRemote getRemotePersistentDelegate(String name);

  /**
   * Get the name of the current step being exectued.
   */
  public String getStepName();

  /*
   * Get the name of the current delegate being executed. TODO: add this in to next release
   * public String getDelegateName();
   */
  /**
   * Get the properties for the current step.
   */
  public Properties getStepProperties();

  /**
   * is the game over?
   */
  public boolean isGameOver();
}
