package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import games.strategy.engine.data.PlayerID;

class EndRoundExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 8770361633528374127L;

  Serializable superState;
  // add other variables here:
  public boolean gameOver = false;
  public Collection<PlayerID> winners = new ArrayList<>();
}
