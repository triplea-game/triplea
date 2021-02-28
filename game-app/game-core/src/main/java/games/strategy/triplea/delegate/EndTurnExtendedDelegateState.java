package games.strategy.triplea.delegate;

import java.io.Serializable;

class EndTurnExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = -3939461840835898284L;

  Serializable superState;
  // add other variables here:
  boolean needToInitialize;
  boolean hasPostedTurnSummary;
}
