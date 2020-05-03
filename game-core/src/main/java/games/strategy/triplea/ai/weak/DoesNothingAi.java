package games.strategy.triplea.ai.weak;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;

/**
 * An AI implementation that takes no action except to purchase and place units according to very
 * simple rules.
 */
public class DoesNothingAi extends AbstractAi {

  public DoesNothingAi(final String name) {
    super(name);
  }

  @Override
  public PlayerType getPlayerType() {
    return PlayerType.DOES_NOTHING_AI;
  }

  @Override
  protected void purchase(
      final boolean purchaseForBid,
      final int pusToSpend,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final GamePlayer player) {
    pause();
  }

  @Override
  protected void tech(
      final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {
    pause();
  }

  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {
    pause();
  }

  @Override
  protected void place(
      final boolean placeForBid,
      final IAbstractPlaceDelegate placeDelegate,
      final GameData data,
      final GamePlayer player) {
    pause();
  }

  @Override
  public void politicalActions() {
    pause();
  }

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
