package games.strategy.triplea.delegate;

import java.io.Serializable;

import org.triplea.java.collections.IntegerMap;

import games.strategy.engine.data.Territory;

class MoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 5352248885420819215L;

  Serializable superState;
  // add other variables here:
  boolean needToInitialize;
  boolean needToDoRockets;
  IntegerMap<Territory> pusLost;
}
