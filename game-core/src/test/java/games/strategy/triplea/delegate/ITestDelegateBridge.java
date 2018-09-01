package games.strategy.triplea.delegate;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.random.IRandomSource;

/**
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 */
interface ITestDelegateBridge extends IDelegateBridge {
  void setStepName(String name);

  void setRandomSource(IRandomSource randomSource);

  void setRemote(IRemotePlayer remote);
}
