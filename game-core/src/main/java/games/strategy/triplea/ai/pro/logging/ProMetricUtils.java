package games.strategy.triplea.ai.pro.logging;

import org.triplea.java.collections.IntegerMap;

import games.strategy.engine.data.ProductionRule;

/**
 * Pro AI metrics.
 */
public final class ProMetricUtils {
  private static final IntegerMap<ProductionRule> totalPurchaseMap = new IntegerMap<>();

  private ProMetricUtils() {}

  public static void collectPurchaseStats(final IntegerMap<ProductionRule> purchaseMap) {
    totalPurchaseMap.add(purchaseMap);
    ProLogger.debug(totalPurchaseMap.toString());
  }
}
