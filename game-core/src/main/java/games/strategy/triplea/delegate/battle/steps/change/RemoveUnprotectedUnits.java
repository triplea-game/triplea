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
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
public class RemoveUnprotectedUnits implements BattleStep {

  private static final long serialVersionUID = 4357860848979564096L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    final List<String> steps = new ArrayList<>();
    if (battleState.getBattleSite().isWater()
        && Properties.getTransportCasualtiesRestricted(battleState.getGameData())
        && (battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsTransport())
            || battleState.getDefendingUnits().stream().anyMatch(Matches.unitIsTransport()))) {
      steps.add(REMOVE_UNESCORTED_TRANSPORTS);
    }
    return steps;
  }

  @Override
  public Order getOrder() {
    return Order.REMOVE_UNPROTECTED_UNITS;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    checkAndRemoveUnits(bridge, BattleState.Side.DEFENSE);
    checkAndRemoveUnits(bridge, BattleState.Side.OFFENSE);
  }

  public void checkAndRemoveUnits(final IDelegateBridge bridge, final BattleState.Side side) {
    if (Properties.getTransportCasualtiesRestricted(battleState.getGameData())) {
      checkUndefendedTransports(bridge, side);
      checkUnprotectedUnits(bridge, side);
    }
  }

  private void checkUndefendedTransports(
      final IDelegateBridge bridge, final BattleState.Side side) {
    final GamePlayer player =
        side == BattleState.Side.OFFENSE ? battleState.getAttacker() : battleState.getDefender();
    // if we are the attacker, we can retreat instead of dying
    if (attackerHasRetreat(side)) {
      return;
    }
    final List<Unit> alliedTransports = getAlliedTransports(player);
    // If no transports, just return
    if (alliedTransports.isEmpty()) {
      return;
    }
    final Collection<Unit> alliedUnits = getAlliedUnits(player);
    // If transports are unescorted, check opposing forces to see if the Trns die automatically
    if (alliedTransports.size() == alliedUnits.size()) {
      final Collection<Unit> enemyUnits = getEnemyUnitsThatCanFire(player);
      // If there are attackers set their movement to 0 and kill the transports
      if (!enemyUnits.isEmpty()) {
        final Change change =
            ChangeFactory.markNoMovementChange(
                CollectionUtils.getMatches(enemyUnits, Matches.unitIsSea()));
        bridge.addChange(change);
        battleActions.remove(
            alliedUnits, bridge, battleState.getBattleSite(), side == BattleState.Side.DEFENSE);
      }
    }
  }

  private boolean attackerHasRetreat(final BattleState.Side side) {
    return side == BattleState.Side.OFFENSE
        && (!battleState.getAttackerRetreatTerritories().isEmpty()
            || battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsAir()));
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
    // if we are the attacker, we can retreat instead of dying
    if (attackerHasRetreat(side)) {
      return;
    }
    if (battleState
        .getUnits(EnumSet.of(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE))
        .isEmpty()) {
      return;
    }
    final Collection<Unit> unprotectedUnits = getUnprotectedUnits(side);
    final boolean hasUnitsThatCanRollLeft = areFightingOrSupportingUnitsLeft(side);
    final boolean enemyHasUnitsThatCanRollLeft =
        areFightingOrSupportingUnitsLeft(side.getOpposite());
    if (!hasUnitsThatCanRollLeft && enemyHasUnitsThatCanRollLeft) {
      battleActions.remove(
          unprotectedUnits, bridge, battleState.getBattleSite(), side == BattleState.Side.DEFENSE);
    }
  }

  private boolean areFightingOrSupportingUnitsLeft(final BattleState.Side side) {
    final boolean hasUnitsThatCanRollLeft;
    hasUnitsThatCanRollLeft =
        battleState.getUnits(EnumSet.of(side)).stream()
            .anyMatch(
                getActiveUnits()
                    .and(
                        Matches.unitIsSupporterOrHasCombatAbility(
                            side == BattleState.Side.OFFENSE)));
    return hasUnitsThatCanRollLeft;
  }

  private Predicate<Unit> getActiveUnits() {
    return Matches.unitIsSubmerged()
        .negate()
        .and(
            Matches.territoryIsLand().test(battleState.getBattleSite())
                ? Matches.unitIsSea().negate()
                : Matches.unitIsLand().negate());
  }

  private Collection<Unit> getUnprotectedUnits(final BattleState.Side side) {
    final Collection<Unit> unitsToKill;
    unitsToKill =
        CollectionUtils.getMatches(
            battleState.getUnits(EnumSet.of(side)),
            getActiveUnits().and(Matches.unitIsNotInfrastructure()));
    return unitsToKill;
  }
}
