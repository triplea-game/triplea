package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.data.PlayerId;

class RandomStartExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 607794506772555083L;

  Serializable superState;
  // add other variables here:
  PlayerId currentPickingPlayer;
}
