package games.strategy.triplea.delegate.battle.steps.change.suicide;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
abstract class RemoveUnits implements BattleStep {
  private static final long serialVersionUID = 1322821208196377684L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  protected void removeUnits(final IDelegateBridge bridge, final Predicate<Unit> unitMatch) {
    removeSuicideUnits(bridge, unitMatch, OFFENSE);
    removeSuicideUnits(bridge, unitMatch, DEFENSE);
  }

  private void removeSuicideUnits(
      final IDelegateBridge bridge, final Predicate<Unit> unitMatch, final BattleState.Side side) {
    final Collection<Unit> suicideUnits =
        CollectionUtils.getMatches(
            battleState.filterUnits(ALIVE, side), unitMatch.and(Matches.unitIsSuicideOnAttack()));
    bridge
        .getDisplayChannelBroadcaster()
        .deadUnitNotification(
            battleState.getBattleId(),
            battleState.getPlayer(side),
            suicideUnits,
            getDependents(suicideUnits));
    battleActions.removeUnits(suicideUnits, bridge, battleState.getBattleSite(), side);
  }

  private Map<Unit, Collection<Unit>> getDependents(final Collection<Unit> units) {
    final Map<Unit, Collection<Unit>> dependents = new HashMap<>();
    for (final Unit unit : units) {
      dependents.put(unit, battleState.getDependentUnits(List.of(unit)));
    }
    return dependents;
  }
}
