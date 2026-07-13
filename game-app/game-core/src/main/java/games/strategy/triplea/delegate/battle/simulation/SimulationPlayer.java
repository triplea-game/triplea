package games.strategy.triplea.delegate.battle.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Player implementation that converts engine callbacks into simulation decisions. */
final class SimulationPlayer extends AbstractAi {
  private final BattleDecisionController decisionController;

  SimulationPlayer(final BattleDecisionController decisionController, final GamePlayer gamePlayer) {
    super(gamePlayer.getName(), "BattleSimulation");
    this.decisionController = decisionController;
    initialize(null, gamePlayer);
  }

  @Override
  public CasualtyDetails selectCasualties(
      final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents,
      final int count,
      final String message,
      final DiceRoll dice,
      final GamePlayer hit,
      final Collection<Unit> friendlyUnits,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final CasualtyList defaultCasualties,
      final UUID battleId,
      final Territory battleSite,
      final boolean allowMultipleHitsPerUnit) {
    return decisionController.requestCasualties(
        selectFrom, count, message, hit, defaultCasualties, allowMultipleHitsPerUnit);
  }

  @Override
  public Optional<Territory> retreatQuery(
      final UUID battleId,
      final boolean submerge,
      final Territory battleTerritory,
      final Collection<Territory> possibleTerritories,
      final String message) {
    return decisionController.requestRetreat(
        getGamePlayer(), submerge, possibleTerritories, message);
  }

  @Override
  public void confirmEnemyCasualties(
      final UUID battleId, final String message, final GamePlayer hitPlayer) {}

  @Override
  public void confirmOwnCasualties(final UUID battleId, final String message) {}

  @Override
  public void reportError(final String error) {}

  @Override
  public void reportMessage(final String message, final String title) {}

  @Override
  protected void purchase(
      final boolean purchaseForBid,
      final int pusToSpend,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final GamePlayer player) {}

  @Override
  protected void tech(
      final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {}

  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDelegate,
      final GameData data,
      final GamePlayer player) {}

  @Override
  protected void place(
      final boolean placeForBid,
      final IAbstractPlaceDelegate placeDelegate,
      final GameState data,
      final GamePlayer player) {}
}
