package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.data.Territory;
import games.strategy.util.IntegerMap;

class MoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 5352248885420819215L;

  Serializable superState;
  // add other variables here:
  public boolean needToInitialize;
  public boolean needToDoRockets;
  public IntegerMap<Territory> pusLost;
}
