package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.REMOVED_CASUALTY;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
class RetreaterGeneral implements Retreater {

  private final BattleState battleState;

  @Override
  public Collection<Unit> getRetreatUnits() {
    final Collection<Unit> retreatUnits = new HashSet<>(battleState.filterUnits(ALIVE, OFFENSE));
    // some units might have been removed from the battle (such as infra) so grab all units at the
    // battle site
    retreatUnits.addAll(
        battleState
            .getBattleSite()
            .getUnitCollection()
            .getMatches(
                Matches.unitIsOwnedBy(battleState.getPlayer(OFFENSE))
                    .and(Matches.unitIsSubmerged().negate())));
    retreatUnits.removeAll(battleState.filterUnits(REMOVED_CASUALTY));
    return retreatUnits;
  }

  @Override
  public Collection<Territory> getPossibleRetreatSites(final Collection<Unit> retreatUnits) {
    final Collection<Territory> allRetreatTerritories = battleState.getAttackerRetreatTerritories();
    return retreatUnits.stream().anyMatch(Matches.unitIsSea())
        ? CollectionUtils.getMatches(allRetreatTerritories, Matches.territoryIsWater())
        : new ArrayList<>(allRetreatTerritories);
  }

  @Override
  public MustFightBattle.RetreatType getRetreatType() {
    return MustFightBattle.RetreatType.DEFAULT;
  }

  @Override
  public RetreatChanges computeChanges(final Territory retreatTo) {
    final Collection<Unit> retreatUnits = getRetreatUnits();

    final CompositeChange change = new CompositeChange();
    final List<RetreatHistoryChild> historyChildren = new ArrayList<>();

    change.add(computeDependentUnitChanges(retreatTo, retreatUnits));

    final Collection<Unit> airRetreating =
        CollectionUtils.getMatches(
            retreatUnits,
            Matches.unitIsAir().and(Matches.unitIsOwnedBy(battleState.getPlayer(OFFENSE))));

    if (!airRetreating.isEmpty()) {
      battleState.retreatUnits(OFFENSE, airRetreating);
      final String transcriptText = MyFormatter.unitsToText(airRetreating) + " retreated";
      historyChildren.add(RetreatHistoryChild.of(transcriptText, new ArrayList<>(airRetreating)));
    }

    final Collection<Unit> nonAirRetreating = new HashSet<>(retreatUnits);
    nonAirRetreating.removeAll(airRetreating);
    nonAirRetreating.addAll(battleState.getDependentUnits(nonAirRetreating));

    if (!nonAirRetreating.isEmpty()) {
      battleState.retreatUnits(OFFENSE, nonAirRetreating);
      historyChildren.add(
          RetreatHistoryChild.of(
              MyFormatter.unitsToText(nonAirRetreating) + " retreated to " + retreatTo.getName(),
              new ArrayList<>(nonAirRetreating)));
      change.add(ChangeFactory.moveUnits(battleState.getBattleSite(), retreatTo, nonAirRetreating));
    }

    return RetreatChanges.of(change, historyChildren);
  }

  private Change computeDependentUnitChanges(
      final Territory retreatTo, final Collection<Unit> retreatUnits) {
    final Collection<IBattle> dependentBattles = battleState.getDependentBattles();
    if (dependentBattles.isEmpty()) {
      // If there are no dependent battles, check landings in allied territories
      return retreatNonCombatTransportedItems(retreatUnits, retreatTo);
    } else {
      // Else retreat the units from combat when their transport retreats
      return retreatCombatTransportedItems(retreatUnits, retreatTo, dependentBattles);
    }
  }

  private Change retreatNonCombatTransportedItems(
      final Collection<Unit> units, final Territory retreatTo) {
    final CompositeChange change = new CompositeChange();
    final Collection<Unit> transports =
        CollectionUtils.getMatches(units, Matches.unitIsSeaTransport());
    for (final Unit transport : transports) {
      final Collection<Unit> retreated = battleState.getTransportDependents(List.of(transport));
      if (!retreated.isEmpty()) {
        final Territory retreatedFrom =
            TransportTracker.getTerritoryTransportHasUnloadedTo(transport);
        if (retreatedFrom != null) {
          TransportTracker.reloadTransports(transports, change);
          change.add(ChangeFactory.moveUnits(retreatedFrom, retreatTo, retreated));
        }
      }
    }
    return change;
  }

  private Change retreatCombatTransportedItems(
      final Collection<Unit> units,
      final Territory retreatTo,
      final Collection<IBattle> dependentBattles) {
    final CompositeChange change = new CompositeChange();
    for (final IBattle dependent : dependentBattles) {
      final Route route = new Route(battleState.getBattleSite(), dependent.getTerritory());
      final Collection<Unit> retreatedUnits = dependent.getDependentUnits(units);
      change.add(dependent.removeAttack(route, retreatedUnits));
      TransportTracker.reloadTransports(units, change);
      change.add(ChangeFactory.moveUnits(dependent.getTerritory(), retreatTo, retreatedUnits));
    }
    return change;
  }
}
