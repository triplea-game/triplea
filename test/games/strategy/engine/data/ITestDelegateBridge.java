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
  public void setPlayerID(PlayerID aPlayer);

  public void setStepName(String name);

  public void setStepName(String name, boolean doNotChangeSequence);

  public void setRandomSource(IRandomSource randomSource);

  public void setRemote(IRemotePlayer remote);

  public void setDisplay(ITripleADisplay display);
}
