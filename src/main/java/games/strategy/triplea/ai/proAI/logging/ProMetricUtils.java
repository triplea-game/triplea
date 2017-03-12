package games.strategy.triplea.ai.proAI.logging;

import games.strategy.engine.data.ProductionRule;
import games.strategy.util.IntegerMap;

/**
 * Pro AI metrics.
 */
public class ProMetricUtils {
  private static IntegerMap<ProductionRule> totalPurchaseMap = new IntegerMap<>();

  public static void collectPurchaseStats(final IntegerMap<ProductionRule> purchaseMap) {
    totalPurchaseMap.add(purchaseMap);
    ProLogger.debug(totalPurchaseMap.toString());
  }
}
