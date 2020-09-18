package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_UNESCORTED_TRANSPORTS;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
public class RemoveUnprotectedUnits implements BattleStep {

  private static final long serialVersionUID = 4357860848979564096L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    if (battleState.getBattleSite().isWater()
        && Properties.getTransportCasualtiesRestricted(battleState.getGameData())
        && (battleState.getUnits(BattleState.Side.OFFENSE).stream()
                .anyMatch(Matches.unitIsTransport())
            || battleState.getUnits(BattleState.Side.DEFENSE).stream()
                .anyMatch(Matches.unitIsTransport()))) {
      return List.of(REMOVE_UNESCORTED_TRANSPORTS);
    }
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.REMOVE_UNPROTECTED_UNITS;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    removeUnprotectedUnits(bridge, BattleState.Side.DEFENSE);
    removeUnprotectedUnits(bridge, BattleState.Side.OFFENSE);
  }

  @RemoveOnNextMajorRelease("This doesn't need to be public in the next major release")
  public void removeUnprotectedUnits(final IDelegateBridge bridge, final BattleState.Side side) {
    if (!Properties.getTransportCasualtiesRestricted(battleState.getGameData())) {
      return;
    }
    // if we are the attacker, we can retreat instead of dying
    if (attackerHasRetreat(side)) {
      return;
    }
    checkUndefendedTransports(bridge, side);
    checkUnprotectedUnits(bridge, side);
  }

  private boolean attackerHasRetreat(final BattleState.Side side) {
    return side == BattleState.Side.OFFENSE
        && (!battleState.getAttackerRetreatTerritories().isEmpty()
            || battleState.getUnits(BattleState.Side.OFFENSE).stream()
                .anyMatch(Matches.unitIsAir()));
  }

  private void checkUndefendedTransports(
      final IDelegateBridge bridge, final BattleState.Side side) {
    final GamePlayer player =
        side == BattleState.Side.OFFENSE ? battleState.getAttacker() : battleState.getDefender();

    final List<Unit> alliedTransports = getAlliedTransports(player);
    if (alliedTransports.isEmpty()) {
      return;
    }

    final Collection<Unit> alliedUnits = getAlliedUnits(player);
    if (alliedTransports.size() != alliedUnits.size()) {
      return;
    }

    final Collection<Unit> enemyUnits = getEnemyUnitsThatCanFire(player);
    // if no enemies, then the transports can't be shot
    if (enemyUnits.isEmpty()) {
      return;
    }

    final Change change =
        ChangeFactory.markNoMovementChange(
            CollectionUtils.getMatches(enemyUnits, Matches.unitIsSea()));
    bridge.addChange(change);
    battleActions.remove(
        alliedUnits, bridge, battleState.getBattleSite(), side == BattleState.Side.DEFENSE);
  }

  private List<Unit> getAlliedTransports(final GamePlayer player) {
    final Predicate<Unit> matchAllied =
        Matches.unitIsTransport()
            .and(Matches.unitIsNotCombatTransport())
            .and(Matches.isUnitAllied(player, battleState.getGameData()))
            .and(Matches.unitIsSea());
    return CollectionUtils.getMatches(battleState.getBattleSite().getUnits(), matchAllied);
  }

  private Collection<Unit> getAlliedUnits(final GamePlayer player) {
    final Predicate<Unit> alliedUnitsMatch =
        Matches.isUnitAllied(player, battleState.getGameData())
            .and(Matches.unitIsNotLand())
            .and(Matches.unitIsSubmerged().negate());
    return CollectionUtils.getMatches(battleState.getBattleSite().getUnits(), alliedUnitsMatch);
  }

  private Collection<Unit> getEnemyUnitsThatCanFire(final GamePlayer player) {
    final Predicate<Unit> enemyUnitsMatch =
        Matches.unitIsNotLand()
            .and(Matches.enemyUnit(player, battleState.getGameData()))
            .and(Matches.unitIsSubmerged().negate())
            .and(Matches.unitCanAttack(player));
    return CollectionUtils.getMatches(battleState.getBattleSite().getUnits(), enemyUnitsMatch);
  }

  private void checkUnprotectedUnits(final IDelegateBridge bridge, final BattleState.Side side) {
    if (battleState.getUnits(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE).isEmpty()) {
      return;
    }

    if (areFightingOrSupportingUnitsLeft(side)) {
      return;
    }

    if (!areFightingOrSupportingUnitsLeft(side.getOpposite())) {
      return;
    }

    final Collection<Unit> unprotectedUnits = getUnprotectedUnits(side);
    battleActions.remove(
        unprotectedUnits, bridge, battleState.getBattleSite(), side == BattleState.Side.DEFENSE);
  }

  private boolean areFightingOrSupportingUnitsLeft(final BattleState.Side side) {
    return battleState.getUnits(side).stream().anyMatch(unitIsActiveAndCanFight(side));
  }

  private Predicate<Unit> unitIsActiveAndCanFight(final BattleState.Side side) {
    return Matches.unitIsActiveInTerritory(battleState.getBattleSite())
        .and(Matches.unitIsSupporterOrHasCombatAbility(side == BattleState.Side.OFFENSE));
  }

  private Collection<Unit> getUnprotectedUnits(final BattleState.Side side) {
    return CollectionUtils.getMatches(
        battleState.getUnits(side),
        Matches.unitIsActiveInTerritory(battleState.getBattleSite())
            .and(Matches.unitIsNotInfrastructure()));
  }
}
