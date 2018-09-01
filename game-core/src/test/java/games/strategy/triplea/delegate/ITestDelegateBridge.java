package games.strategy.triplea.delegate;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomSource;
import games.strategy.triplea.player.ITripleAPlayer;

/**
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 */
interface ITestDelegateBridge extends IDelegateBridge {
  void setStepName(String name);

  void setRandomSource(IRandomSource randomSource);

  /**
   * Returns the remote for the current player.
   *
   * @return A mock that can be configured using standard Mockito idioms.
   */
  @Override
  ITripleAPlayer getRemotePlayer();
}
