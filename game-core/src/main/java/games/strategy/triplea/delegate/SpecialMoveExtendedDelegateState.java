package games.strategy.triplea.delegate;

import java.io.Serializable;

class SpecialMoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 7781410008392307104L;

  Serializable superState;
  boolean needToInitialize;
}
