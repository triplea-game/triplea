package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import games.strategy.engine.data.PlayerID;

class EndRoundExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 8770361633528374127L;

  Serializable superState;
  // add other variables here:
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public boolean m_gameOver = false;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public Collection<PlayerID> m_winners = new ArrayList<>();
}
