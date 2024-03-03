package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nullable;
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
  @Nullable
  String purchase(IntegerMap<ProductionRule> productionRules);

  /** Returns an error code, or null if all is good. */
  @RemoteActionCode(11)
  @Nullable
  String purchaseRepair(Map<Unit, IntegerMap<RepairRule>> productionRules);

  @RemoteActionCode(14)
  @Override
  void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  @RemoteActionCode(7)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(13)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(15)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(5)
  @Override
  String getName();

  @RemoteActionCode(3)
  @Override
  String getDisplayName();

  @RemoteActionCode(2)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(12)
  @Override
  Serializable saveState();

  @RemoteActionCode(8)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(6)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
