package games.strategy.engine.data;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.random.IRandomSource;
import games.strategy.triplea.ui.display.ITripleADisplay;

/**
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 */
public interface ITestDelegateBridge extends IDelegateBridge {
  /**
   * Changing the player has the effect of commiting the current transaction.
   * Player is initialized to the player specified in the xml data.
   */
  void setPlayerId(PlayerID playerId);

  void setStepName(String name);

  void setStepName(String name, boolean doNotChangeSequence);

  void setRandomSource(IRandomSource randomSource);

  void setRemote(IRemotePlayer remote);

  void setDisplay(ITripleADisplay display);
}
