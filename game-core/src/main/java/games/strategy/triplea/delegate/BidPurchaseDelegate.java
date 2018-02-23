package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.util.IntegerMap;

@MapSupport
public class BidPurchaseDelegate extends PurchaseDelegate {
  private int bid;
  private int spent;
  private boolean hasBid = false;

  private static int getBidAmount(final GameData data, final PlayerID currentPlayer) {
    final String propertyName = currentPlayer.getName() + " bid";
    return data.getProperties().get(propertyName, 0);
  }

  public static boolean doesPlayerHaveBid(final GameData data, final PlayerID player) {
    return getBidAmount(data, player) != 0;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    if (!doesPlayerHaveBid(getData(), player)) {
      return false;
    }
    if (((player.getProductionFrontier() == null) || player.getProductionFrontier().getRules().isEmpty())
        && ((player.getRepairFrontier() == null) || player.getRepairFrontier().getRules().isEmpty())) {
      return false;
    }
    return canWePurchaseOrRepair();
  }

  @Override
  protected boolean canWePurchaseOrRepair() {
    final ResourceCollection bidCollection = new ResourceCollection(getData());
    // TODO: allow bids to have more than just PUs
    bidCollection.addResource(getData().getResourceList().getResource(Constants.PUS), bid);
    if ((player.getProductionFrontier() != null) && (player.getProductionFrontier().getRules() != null)) {
      for (final ProductionRule rule : player.getProductionFrontier().getRules()) {
        if (bidCollection.has(rule.getCosts())) {
          return true;
        }
      }
    }
    if ((player.getRepairFrontier() != null) && (player.getRepairFrontier().getRules() != null)) {
      for (final RepairRule rule : player.getRepairFrontier().getRules()) {
        if (bidCollection.has(rule.getCosts())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  protected boolean canAfford(final IntegerMap<Resource> costs, final PlayerID player) {
    final ResourceCollection bidCollection = new ResourceCollection(getData());
    // TODO: allow bids to have more than just PUs
    bidCollection.addResource(getData().getResourceList().getResource(Constants.PUS), bid);
    return bidCollection.has(costs);
  }

  @Override
  public void start() {
    super.start();
    if (hasBid) {
      return;
    }
    bid = getBidAmount(bridge.getData(), bridge.getPlayerId());
    spent = 0;
  }

  @Override
  protected String removeFromPlayer(final IntegerMap<Resource> resources, final CompositeChange change) {
    spent = resources.getInt(super.getData().getResourceList().getResource(Constants.PUS));
    return (bid - spent) + " PU unused";
  }

  @Override
  public void end() {
    super.end();
    final int unspent = bid - spent;
    if (unspent == 0) {
      return;
    }
    bridge.getHistoryWriter()
        .startEvent(bridge.getPlayerId().getName() + " retains " + unspent + " PUS not spent in bid phase");
    final Change unspentChange = ChangeFactory.changeResourcesChange(bridge.getPlayerId(),
        super.getData().getResourceList().getResource(Constants.PUS), unspent);
    bridge.addChange(unspentChange);
    hasBid = false;
  }

  @Override
  public Serializable saveState() {
    final BidPurchaseExtendedDelegateState state = new BidPurchaseExtendedDelegateState();
    state.superState = super.saveState();
    state.m_bid = bid;
    state.m_hasBid = hasBid;
    state.m_spent = this.spent;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final BidPurchaseExtendedDelegateState s = (BidPurchaseExtendedDelegateState) state;
    super.loadState(s.superState);
    bid = s.m_bid;
    spent = s.m_spent;
    hasBid = s.m_hasBid;
  }
}
