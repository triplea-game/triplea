package games.strategy.engine.data;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.random.IRandomSource;

/**
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 */
public interface ITestDelegateBridge extends IDelegateBridge {
  /**
   * Player is initialized to the player specified in the xml data.
   */
  void setPlayerId(PlayerID playerId);

  void setStepName(String name);

  void setRandomSource(IRandomSource randomSource);

  void setRemote(IRemotePlayer remote);
}
