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
import java.util.ArrayList;
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
    final Collection<Unit> suicideAttackers =
        CollectionUtils.getMatches(
            battleState.filterUnits(ALIVE, OFFENSE),
            unitMatch.and(Matches.unitIsSuicideOnAttack()));
    final Collection<Unit> suicideDefenders =
        CollectionUtils.getMatches(
            battleState.filterUnits(ALIVE, DEFENSE),
            unitMatch.and(Matches.unitIsSuicideOnDefense()));
    bridge
        .getDisplayChannelBroadcaster()
        .deadUnitNotification(
            battleState.getBattleId(),
            battleState.getPlayer(OFFENSE),
            suicideAttackers,
            getDependents(suicideAttackers));
    bridge
        .getDisplayChannelBroadcaster()
        .deadUnitNotification(
            battleState.getBattleId(),
            battleState.getPlayer(DEFENSE),
            suicideDefenders,
            getDependents(suicideDefenders));
    final List<Unit> deadUnits = new ArrayList<>(suicideAttackers);
    deadUnits.addAll(suicideDefenders);
    battleActions.remove(deadUnits, bridge, battleState.getBattleSite(), null);
  }

  private Map<Unit, Collection<Unit>> getDependents(final Collection<Unit> units) {
    final Map<Unit, Collection<Unit>> dependents = new HashMap<>();
    for (final Unit unit : units) {
      dependents.put(unit, battleState.getDependentUnits(List.of(unit)));
    }
    return dependents;
  }
}
