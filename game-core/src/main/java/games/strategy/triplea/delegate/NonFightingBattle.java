package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
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
      final PlayerId attacker,
      final BattleTracker battleTracker,
      final GameData data) {
    super(battleSite, attacker, battleTracker, data);
  }

  @Override
  public Change addAttackChange(
      final Route route, final Collection<Unit> units, final Map<Unit, Set<Unit>> targets) {  
    addDependentTransportingUnits(units);
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    this.attackingFrom.add(attackingFrom);
    attackingUnits.addAll(units);
    final Collection<Unit> attackingFromMapUnits =
        attackingFromMap.computeIfAbsent(attackingFrom, k -> new ArrayList<>());
    attackingFromMapUnits.addAll(units);
    // are we amphibious
    if (route.getStart().isWater()
        && route.getEnd() != null
        && !route.getEnd().isWater()
        && units.stream().anyMatch(Matches.unitIsLand())) {
      getAmphibiousAttackTerritories().add(route.getTerritoryBeforeEnd());
      amphibiousLandAttackers.addAll(CollectionUtils.getMatches(units, Matches.unitIsLand()));
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
    // if any attacking non air units then win
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
    final Predicate<Unit> attackingLand =
        Matches.alliedUnit(attacker, gameData).and(Matches.unitIsLand());
    return battleSite.getUnitCollection().anyMatch(attackingLand);
  }

  @Override
  public void removeAttack(final Route route, final Collection<Unit> units) {
    attackingUnits.removeAll(units);
    // the route could be null, in the case of a unit in a territory where a sub is submerged.
    if (route == null) {
      return;
    }
    final Territory attackingFrom = route.getTerritoryBeforeEnd();
    Collection<Unit> attackingFromMapUnits = attackingFromMap.get(attackingFrom);
    // handle possible null pointer
    if (attackingFromMapUnits == null) {
      attackingFromMapUnits = new ArrayList<>();
    }
    attackingFromMapUnits.removeAll(units);
    if (attackingFromMapUnits.isEmpty()) {
      this.attackingFrom.remove(attackingFrom);
    }
    // deal with amphibious assaults
    if (attackingFrom.isWater()) {
      if (route.getEnd() != null
          && !route.getEnd().isWater()
          && units.stream().anyMatch(Matches.unitIsLand())) {
        amphibiousLandAttackers.removeAll(CollectionUtils.getMatches(units, Matches.unitIsLand()));
      }
      // if none of the units is a land unit, the attack from that territory is no longer an
      // amphibious assault
      if (attackingFromMapUnits.stream().noneMatch(Matches.unitIsLand())) {
        getAmphibiousAttackTerritories().remove(attackingFrom);
        // do we have any amphibious attacks left?
        isAmphibious = !getAmphibiousAttackTerritories().isEmpty();
      }
    }
    for (final Collection<Unit> dependent : dependentUnits.values()) {
      dependent.removeAll(units);
    }
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
    if (lost.size() != 0) {
      final String transcriptText =
          MyFormatter.unitsToText(lost) + " lost in " + battleSite.getName();
      bridge.getHistoryWriter().addChildToEvent(transcriptText, lost);
      final Change change = ChangeFactory.removeUnits(battleSite, lost);
      bridge.addChange(change);
    }
  }

  void addDependentUnits(final Map<Unit, Collection<Unit>> dependencies) {
    for (final Map.Entry<Unit, Collection<Unit>> entry : dependencies.entrySet()) {
      dependentUnits
          .computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>())
          .addAll(new ArrayList<>(entry.getValue()));
    }
  }
}
