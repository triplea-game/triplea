package games.strategy.triplea.delegate;

import games.strategy.engine.data.Territory;
import java.io.Serializable;
import org.triplea.java.collections.IntegerMap;

class MoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 5352248885420819215L;

  Serializable superState;
  // add other variables here:
  boolean needToInitialize;
  boolean needToDoRockets;
  IntegerMap<Territory> pusLost;
}
