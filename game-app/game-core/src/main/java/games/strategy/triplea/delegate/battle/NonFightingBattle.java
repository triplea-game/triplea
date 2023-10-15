package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.change.HistoryChangeFactory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.BattleRecord;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;

/**
 * Battle in which no fighting occurs. Example is a naval invasion into an empty country, but the
 * battle cannot be fought until a naval battle occurs.
 */
public class NonFightingBattle extends DependentBattle {
  private static final long serialVersionUID = -1699534010648145123L;

  public NonFightingBattle(
      final Territory battleSite,
      final GamePlayer attacker,
      final BattleTracker battleTracker,
      final GameData data) {
    super(battleSite, attacker, battleTracker, data);
  }

  @Override
  public Change addAttackChange(
      final Route route, final Collection<Unit> units, final Map<Unit, Set<Unit>> targets) {
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    attackingUnits.addAll(units);
    attackingFromMap.computeIfAbsent(attackingFrom, k -> new ArrayList<>()).addAll(units);
    // are we amphibious
    if (route.getStart().isWater()
        && !route.getEnd().isWater()
        && units.stream().anyMatch(Matches.unitIsLand())) {
      getAmphibiousAttackTerritories().add(route.getTerritoryBeforeEnd());
      isAmphibious = true;
    }
    return ChangeFactory.EMPTY_CHANGE;
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    if (!battleTracker.getDependentOn(this).isEmpty()) {
      throw new IllegalStateException("Must fight battles that this battle depends on first");
    }
    // create event
    bridge.getHistoryWriter().startEvent("Battle in " + battleSite, battleSite);
    // if any attacking non-air units then win
    final boolean someAttacking = hasAttackingUnits();
    if (someAttacking) {
      whoWon = WhoWon.ATTACKER;
      battleResultDescription = BattleRecord.BattleResultDescription.BLITZED;
      battleTracker.takeOver(battleSite, attacker, bridge, null, null);
      battleTracker.addToConquered(battleSite);
    } else {
      whoWon = WhoWon.DEFENDER;
      battleResultDescription = BattleRecord.BattleResultDescription.LOST;
    }
    battleTracker
        .getBattleRecords()
        .addResultToBattle(
            attacker,
            battleId,
            defender,
            attackerLostTuv,
            defenderLostTuv,
            battleResultDescription,
            new BattleResults(this, gameData));
    battleTracker.removeBattle(this, gameData);
    isOver = true;
  }

  boolean hasAttackingUnits() {
    final Predicate<Unit> attackingLand = Matches.alliedUnit(attacker).and(Matches.unitIsLand());
    return battleSite.anyUnitsMatch(attackingLand);
  }

  @Override
  public boolean isEmpty() {
    return !hasAttackingUnits();
  }

  @Override
  public void unitsLostInPrecedingBattle(
      final Collection<Unit> units, final IDelegateBridge bridge, final boolean withdrawn) {
    if (withdrawn) {
      return;
    }
    Collection<Unit> lost = new ArrayList<>(getDependentUnits(units));
    lost.addAll(CollectionUtils.intersection(units, attackingUnits));
    lost = CollectionUtils.getMatches(lost, Matches.unitIsInTerritory(battleSite));
    if (!lost.isEmpty()) {
      HistoryChangeFactory.removeUnitsFromTerritory(battleSite, lost).perform(bridge);
    }
  }
}
