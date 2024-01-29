package games.strategy.triplea.delegate;

import games.strategy.engine.data.ProductionRule;
import java.io.Serializable;
import org.triplea.java.collections.IntegerMap;

class PurchaseExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 2326864364534284490L;

  Serializable superState;
  // add other variables here:
  boolean needToInitialize;
  IntegerMap<ProductionRule> pendingProductionRules;
}
