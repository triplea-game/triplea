package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.util.IntegerMap;

@MapSupport
public class BidPurchaseDelegate extends PurchaseDelegate {
  private int m_bid;
  private int m_spent;
  private boolean m_hasBid = false;

  private static int getBidAmount(final GameData data, final PlayerID currentPlayer) {
    final String propertyName = currentPlayer.getName() + " bid";
    final int bid = data.getProperties().get(propertyName, 0);
    return bid;
  }

  public static boolean doesPlayerHaveBid(final GameData data, final PlayerID player) {
    return getBidAmount(data, player) != 0;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    if (!doesPlayerHaveBid(getData(), m_player)) {
      return false;
    }
    if ((m_player.getProductionFrontier() == null || m_player.getProductionFrontier().getRules().isEmpty())
        && (m_player.getRepairFrontier() == null || m_player.getRepairFrontier().getRules().isEmpty())) {
      return false;
    }
    return canWePurchaseOrRepair();
  }

  @Override
  protected boolean canWePurchaseOrRepair() {
    final ResourceCollection bidCollection = new ResourceCollection(getData());
    // TODO: allow bids to have more than just PUs
    bidCollection.addResource(getData().getResourceList().getResource(Constants.PUS), m_bid);
    if (m_player.getProductionFrontier() != null && m_player.getProductionFrontier().getRules() != null) {
      for (final ProductionRule rule : m_player.getProductionFrontier().getRules()) {
        if (bidCollection.has(rule.getCosts())) {
          return true;
        }
      }
    }
    if (m_player.getRepairFrontier() != null && m_player.getRepairFrontier().getRules() != null) {
      for (final RepairRule rule : m_player.getRepairFrontier().getRules()) {
        if (bidCollection.has(rule.getCosts())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * subclasses can over ride this method to use different restrictions as to what a player can buy
   */
  @Override
  protected boolean canAfford(final IntegerMap<Resource> costs, final PlayerID player) {
    final ResourceCollection bidCollection = new ResourceCollection(getData());
    // TODO: allow bids to have more than just PUs
    bidCollection.addResource(getData().getResourceList().getResource(Constants.PUS), m_bid);
    return bidCollection.has(costs);
  }

  @Override
  public void start() {
    super.start();
    if (m_hasBid) {
      return;
    }
    m_bid = getBidAmount(m_bridge.getData(), m_bridge.getPlayerID());
    m_spent = 0;
  }

  @Override
  protected String removeFromPlayer(final PlayerID player, final IntegerMap<Resource> resources,
      final CompositeChange change) {
    m_spent = resources.getInt(super.getData().getResourceList().getResource(Constants.PUS));
    return (m_bid - m_spent) + " PU unused";
  }

  /**
   * Called before the delegate will stop running.
   */
  @Override
  public void end() {
    super.end();
    final int unspent = m_bid - m_spent;
    if (unspent == 0) {
      return;
    }
    m_bridge.getHistoryWriter()
        .startEvent(m_bridge.getPlayerID().getName() + " retains " + unspent + " PUS not spent in bid phase");
    final Change unspentChange = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(),
        super.getData().getResourceList().getResource(Constants.PUS), unspent);
    m_bridge.addChange(unspentChange);
    m_hasBid = false;
  }

  @Override
  public Serializable saveState() {
    final BidPurchaseExtendedDelegateState state = new BidPurchaseExtendedDelegateState();
    state.superState = super.saveState();
    state.m_bid = m_bid;
    state.m_hasBid = m_hasBid;
    state.m_spent = this.m_spent;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final BidPurchaseExtendedDelegateState s = (BidPurchaseExtendedDelegateState) state;
    super.loadState(s.superState);
    m_bid = s.m_bid;
    m_spent = s.m_spent;
    m_hasBid = s.m_hasBid;
  }
}


class BidPurchaseExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 6896164200767186673L;
  Serializable superState;
  int m_bid;
  int m_spent;
  boolean m_hasBid;
}
