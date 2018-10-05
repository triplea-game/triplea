package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.data.Territory;
import games.strategy.util.IntegerMap;

class MoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 5352248885420819215L;

  Serializable superState;
  // add other variables here:
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public boolean m_needToInitialize;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public boolean m_needToDoRockets;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  public IntegerMap<Territory> m_PUsLost;
}
