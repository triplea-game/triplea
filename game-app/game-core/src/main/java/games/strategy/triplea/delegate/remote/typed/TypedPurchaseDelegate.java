package games.strategy.triplea.delegate.remote.typed;

import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import java.util.Map;
import javax.annotation.Nullable;
import org.triplea.java.collections.IntegerMap;

/**
 * Typed-message-backed {@link IPurchaseDelegate} handed to the AI in place of the reflective
 * current-delegate proxy. The AI's real (non-simulation) purchase commits forward over the
 * messenger latch; the forum-poster and lifecycle methods are only reached on the server-side
 * delegate and stay unsupported. The AI's local-simulation delegates keep calling the real place
 * delegate directly and never reach this adapter.
 */
public class TypedPurchaseDelegate extends AbstractTypedCurrentDelegate
    implements IPurchaseDelegate {
  public TypedPurchaseDelegate(final PlayerBridge playerBridge) {
    super(playerBridge);
  }

  @Override
  @Nullable
  public String purchase(final IntegerMap<ProductionRule> productionRules) {
    return invokeCurrent(new PurchaseRequest(productionRules), PurchaseResponse.TYPE).getError();
  }

  @Override
  @Nullable
  public String purchaseRepair(final Map<Unit, IntegerMap<RepairRule>> productionRules) {
    return invokeCurrent(new PurchaseRepairRequest(productionRules), PurchaseRepairResponse.TYPE)
        .getError();
  }

  @Override
  public boolean postTurnSummary(final PbemMessagePoster poster, final String title) {
    throw notForwarded("postTurnSummary");
  }

  @Override
  public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary) {
    throw notForwarded("setHasPostedTurnSummary");
  }

  @Override
  public boolean getHasPostedTurnSummary() {
    throw notForwarded("getHasPostedTurnSummary");
  }
}
