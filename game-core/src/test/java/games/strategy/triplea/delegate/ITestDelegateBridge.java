package games.strategy.triplea.delegate;

import games.strategy.engine.delegate.IDelegateBridge;

/**
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 */
interface ITestDelegateBridge extends IDelegateBridge {
  void setStepName(String name);
}
