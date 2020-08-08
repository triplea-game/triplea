package games.strategy.triplea.delegate;

import java.io.Serializable;

class PurchaseExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 2326864364534284490L;

  Serializable superState;
  // add other variables here:
  boolean needToInitialize;
}
