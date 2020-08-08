package games.strategy.triplea.delegate;

import java.io.Serializable;

class InitializationExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = -9000446777655823735L;

  Serializable superState;
  boolean needToInitialize;
}
