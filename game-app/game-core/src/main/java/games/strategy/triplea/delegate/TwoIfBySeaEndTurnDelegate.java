package games.strategy.triplea.delegate;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;

/**
 * Logic for ending a turn in a Two If By Sea game.
 *
 * @deprecated Required for map compatibility. Remove upon next map-incompatible release.
 */
@Deprecated
public class TwoIfBySeaEndTurnDelegate extends AbstractEndTurnDelegate {
  protected boolean gameOver = false;

  @Override
  protected String doNationalObjectivesAndOtherEndTurnEffects(final IDelegateBridge bridge) {
    final GameState data = getData();
    final PlayerList playerList = data.getPlayerList();
    final GamePlayer british = playerList.getPlayerId(Constants.PLAYER_NAME_BRITISH);
    final GamePlayer japanese = playerList.getPlayerId(Constants.PLAYER_NAME_JAPANESE);
    // Quick check to see who still owns their own capital
    final boolean britain =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapitalOrThrow(british, data.getMap())
            .getOwner()
            .equals(british);
    final boolean japan =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapitalOrThrow(
                japanese, data.getMap())
            .getOwner()
            .equals(japanese);
    if (!gameOver) {
      if (britain && !japan) {
        gameOver = true;
        bridge.getHistoryWriter().startEvent("British win.");
      }
      if (!britain && japan) {
        gameOver = true;
        bridge.getHistoryWriter().startEvent("Japanese win.");
      }
    }
    return "";
  }

  @Override
  protected String addOtherResources(final IDelegateBridge bridge) {
    return "";
  }
}
