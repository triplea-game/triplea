package games.strategy.triplea.delegate;

import java.io.Serializable;

class TechActivationExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 1742776261442260882L;

  Serializable superState;
  boolean needToInitialize;
}
