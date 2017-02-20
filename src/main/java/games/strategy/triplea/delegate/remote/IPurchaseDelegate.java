package games.strategy.triplea.delegate.remote;

import java.util.Map;

import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.util.IntegerMap;

public interface IPurchaseDelegate extends IRemote, IDelegate {
  /**
   * @param productionRules
   *        - units maps ProductionRule -> count
   * @return null if units bought, otherwise an error message
   */
  String purchase(IntegerMap<ProductionRule> productionRules);

  String purchaseRepair(Map<Unit, IntegerMap<RepairRule>> productionRules);
}
