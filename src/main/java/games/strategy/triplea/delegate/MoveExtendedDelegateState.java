package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.data.Territory;
import games.strategy.util.IntegerMap;

class MoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 5352248885420819215L;
  Serializable superState;
  // add other variables here:
  public boolean m_firstRun = true;
  public boolean m_needToInitialize;
  public boolean m_needToDoRockets;
  public IntegerMap<Territory> m_PUsLost;
}
