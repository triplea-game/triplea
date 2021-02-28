package games.strategy.triplea.delegate;

import games.strategy.engine.data.GamePlayer;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

class EndRoundExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 8770361633528374127L;

  Serializable superState;
  // add other variables here:
  boolean gameOver = false;
  Collection<GamePlayer> winners = new ArrayList<>();
}
