package games.strategy.triplea.ai.proAI.util;

import java.util.logging.Level;

import games.strategy.engine.data.ProductionRule;
import games.strategy.util.IntegerMap;

/**
 * Pro AI metrics.
 */
public class ProMetricUtils {
  private static IntegerMap<ProductionRule> totalPurchaseMap = new IntegerMap<ProductionRule>();

  public static void collectPurchaseStats(final IntegerMap<ProductionRule> purchaseMap) {
    totalPurchaseMap.add(purchaseMap);
    ProLogUtils.log(Level.FINER, totalPurchaseMap.toString());
  }
}
