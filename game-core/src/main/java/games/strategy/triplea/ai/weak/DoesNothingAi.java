package games.strategy.triplea.ai.weak;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.ai.AbstractBuiltInAi;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;

/**
 * An AI implementation that takes no action except to purchase and place units according to very
 * simple rules.
 */
public class DoesNothingAi extends AbstractBuiltInAi {

  public DoesNothingAi(final String name) {
    super(name);
  }

  @Override
  public PlayerTypes.Type getPlayerType() {
    return PlayerTypes.DOES_NOTHING_AI;
  }

  @Override
  protected void purchase(
      final boolean purchaseForBid,
      final int pusToSpend,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final GamePlayer player) {
    // spend whatever we have
    if (!player.getResources().isEmpty()) {
      new WeakAi(this.getName())
          .purchase(purchaseForBid, pusToSpend, purchaseDelegate, data, player);
    }
  }

  @Override
  protected void tech(
      final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {}

  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {}

  @Override
  protected void place(
      final boolean placeForBid,
      final IAbstractPlaceDelegate placeDelegate,
      final GameData data,
      final GamePlayer player) {
    // place whatever we have
    if (!player.getUnitCollection().isEmpty()) {
      new WeakAi(this.getName()).place(placeForBid, placeDelegate, data, player);
    }
  }

  @Override
  public void politicalActions() {}

  @Override
  protected void endTurn(
      final IAbstractForumPosterDelegate endTurnForumPosterDelegate, final GamePlayer player) {
    // destroy whatever we have
    final ResourceCollection resourceCollection = player.getResources();
    final Change removeChange = ChangeFactory.removeResourceCollection(player, resourceCollection);
    // shameless cheating... (do NOT do this, normally you are never supposed to access the
    // IDelegateBridge from outside
    // of a delegate)
    final IDelegateBridge bridge = endTurnForumPosterDelegate.getBridge();
    // resourceCollection is not yet a valid renderingObject
    bridge
        .getHistoryWriter()
        .startEvent(player.getName() + " removes resources: " + resourceCollection, null);
    bridge.addChange(removeChange);
  }

  @Override
  public boolean acceptAction(
      final GamePlayer playerSendingProposal,
      final String acceptanceQuestion,
      final boolean politics) {
    return true;
  }
}
