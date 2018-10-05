package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.data.PlayerID;

class RandomStartExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 607794506772555083L;

  Serializable superState;
  // add other variables here:
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public PlayerID m_currentPickingPlayer;
}
