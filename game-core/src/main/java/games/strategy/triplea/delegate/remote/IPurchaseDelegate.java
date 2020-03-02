package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import games.strategy.engine.message.RemoteActionCode;
import java.util.Map;
import org.triplea.java.collections.IntegerMap;

/** Logic for purchasing and repairing units. */
public interface IPurchaseDelegate extends IAbstractForumPosterDelegate {
  /**
   * Purchases the specified units.
   *
   * @param productionRules - units maps ProductionRule -> count.
   * @return null if units bought, otherwise an error message
   */
  @RemoteActionCode(10)
  String purchase(IntegerMap<ProductionRule> productionRules);

  /** Returns an error code, or null if all is good. */
  @RemoteActionCode(11)
  String purchaseRepair(Map<Unit, IntegerMap<RepairRule>> productionRules);
}
