package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.ATTACKER_WITHDRAW;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.MustFightBattle.RetreatType;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.RetreatChecks;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.sound.SoundUtils;

@AllArgsConstructor
public class OffensiveGeneralRetreat implements BattleStep {

  private static final long serialVersionUID = -9192684621899682418L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    return isRetreatPossible() ? List.of(getName()) : List.of();
  }

  private boolean isRetreatPossible() {
    return canAttackerRetreat()
        || canAttackerRetreatSeaPlanes()
        || (battleState.isAmphibious()
            && (canAttackerRetreatPartialAmphib() || canAttackerRetreatAmphibPlanes()));
  }

  private boolean canAttackerRetreat() {
    return RetreatChecks.canAttackerRetreat(
        battleState.getUnits(BattleState.Side.DEFENSE),
        battleState.getGameData(),
        battleState::getAttackerRetreatTerritories,
        battleState.isAmphibious());
  }

  private boolean canAttackerRetreatSeaPlanes() {
    return battleState.getBattleSite().isWater()
        && battleState.getUnits(BattleState.Side.OFFENSE).stream().anyMatch(Matches.unitIsAir());
  }

  private boolean canAttackerRetreatPartialAmphib() {
    if (!Properties.getPartialAmphibiousRetreat(battleState.getGameData())) {
      return false;
    }
    // Only include land units when checking for allow amphibious retreat
    return battleState.getUnits(BattleState.Side.OFFENSE).stream()
        .filter(Matches.unitIsLand())
        .anyMatch(Matches.unitWasNotAmphibious());
  }

  private boolean canAttackerRetreatAmphibPlanes() {
    final GameData gameData = battleState.getGameData();
    return (Properties.getWW2V2(gameData)
            || Properties.getAttackerRetreatPlanes(gameData)
            || Properties.getPartialAmphibiousRetreat(gameData))
        && battleState.getUnits(BattleState.Side.OFFENSE).stream().anyMatch(Matches.unitIsAir());
  }

  private String getName() {
    return battleState.getAttacker().getName() + ATTACKER_WITHDRAW;
  }

  @Override
  public Order getOrder() {
    return Order.OFFENSIVE_GENERAL_RETREAT;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    retreatUnits(bridge);
  }

  @RemoveOnNextMajorRelease("This doesn't need to be public in the next major release")
  public void retreatUnits(final IDelegateBridge bridge) {
    if (battleState.isOver()) {
      return;
    }

    if (battleState.isAmphibious()) {
      if (canAttackerRetreatPartialAmphib()) {
        retreatPartialAmphib(bridge);
      } else if (canAttackerRetreatAmphibPlanes()) {
        retreatAmphibPlanes(bridge);
      }
    } else if (canAttackerRetreat()) {
      retreatAllUnits(bridge);
    }
  }

  private void retreatPartialAmphib(final IDelegateBridge bridge) {
    final Collection<Unit> retreatUnits =
        CollectionUtils.getMatches(
            battleState.getUnits(BattleState.Side.OFFENSE), Matches.unitWasNotAmphibious());
    final Collection<Territory> allRetreatTerritories = battleState.getAttackerRetreatTerritories();
    final Collection<Territory> possibleRetreatSites =
        retreatUnits.stream().anyMatch(Matches.unitIsLand())
            ? CollectionUtils.getMatches(allRetreatTerritories, Matches.territoryIsLand())
            : new ArrayList<>(allRetreatTerritories);
    final String text = battleState.getAttacker().getName() + " retreat non-amphibious units?";
    bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleState.getBattleId(), getName());
    final Territory retreatTo =
        battleActions.queryRetreatTerritory(
            battleState, bridge, battleState.getAttacker(), possibleRetreatSites, text);
    if (retreatTo == null) {
      return;
    }
    if (!battleState.isHeadless()) {
      SoundUtils.playRetreatType(
          battleState.getAttacker(), retreatUnits, RetreatType.PARTIAL_AMPHIB, bridge);
    }

    // the air units don't retreat to the `retreatTo` territory
    final Collection<Unit> airRetreating =
        CollectionUtils.getMatches(retreatUnits, Matches.unitIsAir());

    final Collection<Unit> nonAirRetreating = new ArrayList<>(retreatUnits);
    nonAirRetreating.removeAll(airRetreating);
    nonAirRetreating.addAll(battleState.getDependentUnits(nonAirRetreating));

    battleState.retreatUnits(BattleState.Side.OFFENSE, nonAirRetreating);
    battleState.retreatUnits(BattleState.Side.OFFENSE, airRetreating);

    bridge.addChange(
        ChangeFactory.moveUnits(battleState.getBattleSite(), retreatTo, nonAirRetreating));

    addHistoryRetreatToTerritory(retreatTo, bridge, nonAirRetreating);
    addHistoryRetreat(bridge, airRetreating);

    notifyRetreat(retreatUnits, bridge);
    broadcastRetreat(bridge, " retreats non-amphibious units");
  }

  private void addHistoryRetreatToTerritory(
      final Territory to, final IDelegateBridge bridge, final Collection<Unit> units) {
    final String transcriptText = MyFormatter.unitsToText(units) + " retreated to " + to.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(units));
  }

  private void addHistoryRetreat(final IDelegateBridge bridge, final Collection<Unit> units) {
    final String transcriptText = MyFormatter.unitsToText(units) + " retreated";
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(units));
  }

  private void notifyRetreat(final Collection<Unit> retreating, final IDelegateBridge bridge) {
    if (battleState.getUnits(BattleState.Side.OFFENSE).isEmpty()) {
      battleActions.endBattle(IBattle.WhoWon.DEFENDER, bridge);
    } else {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleState.getBattleId(), retreating);
    }
  }

  private void broadcastRetreat(final IDelegateBridge bridge, final String messageShortSuffix) {
    broadcastRetreat(bridge, messageShortSuffix, messageShortSuffix);
  }

  private void broadcastRetreat(
      final IDelegateBridge bridge,
      final String messageShortSuffix,
      final String messageLongSuffix) {
    final String messageShort = battleState.getAttacker().getName() + messageShortSuffix;
    final String messageLong = battleState.getAttacker().getName() + messageLongSuffix;
    bridge
        .getDisplayChannelBroadcaster()
        .notifyRetreat(messageShort, messageLong, getName(), battleState.getAttacker());
  }

  private void retreatAmphibPlanes(final IDelegateBridge bridge) {
    final Collection<Unit> retreatUnits =
        CollectionUtils.getMatches(
            battleState.getUnits(BattleState.Side.OFFENSE), Matches.unitIsAir());
    final Collection<Territory> possibleRetreatSites = List.of(battleState.getBattleSite());
    final String text = battleState.getAttacker().getName() + " retreat planes?";
    bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleState.getBattleId(), getName());
    final Territory retreatTo =
        battleActions.queryRetreatTerritory(
            battleState, bridge, battleState.getAttacker(), possibleRetreatSites, text);
    if (retreatTo == null) {
      return;
    }
    if (!battleState.isHeadless()) {
      SoundUtils.playRetreatType(
          battleState.getAttacker(), retreatUnits, RetreatType.PLANES, bridge);
    }
    addHistoryRetreat(bridge, retreatUnits);

    battleState.retreatUnits(BattleState.Side.OFFENSE, retreatUnits);
    notifyRetreat(retreatUnits, bridge);
    broadcastRetreat(bridge, " retreats planes");
  }

  private void retreatAllUnits(final IDelegateBridge bridge) {
    final Collection<Unit> retreatUnits =
        new HashSet<>(battleState.getUnits(BattleState.Side.OFFENSE));
    // some units might have been removed from the battle (such as infra) so grab all units at the
    // battle site
    retreatUnits.addAll(
        battleState
            .getBattleSite()
            .getUnitCollection()
            .getMatches(
                Matches.unitIsOwnedBy(battleState.getAttacker())
                    .and(Matches.unitIsSubmerged().negate())));
    retreatUnits.removeAll(battleState.getKilled());

    final Collection<Territory> allRetreatTerritories = battleState.getAttackerRetreatTerritories();
    final Collection<Territory> possibleRetreatSites =
        retreatUnits.stream().anyMatch(Matches.unitIsSea())
            ? CollectionUtils.getMatches(allRetreatTerritories, Matches.territoryIsWater())
            : new ArrayList<>(allRetreatTerritories);

    final String text = battleState.getAttacker().getName() + " retreat?";
    bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleState.getBattleId(), getName());
    final Territory retreatTo =
        battleActions.queryRetreatTerritory(
            battleState, bridge, battleState.getAttacker(), possibleRetreatSites, text);
    if (retreatTo == null) {
      return;
    }
    if (!battleState.isHeadless()) {
      SoundUtils.playRetreatType(
          battleState.getAttacker(), retreatUnits, RetreatType.DEFAULT, bridge);
    }

    // attacker's air units don't retreat to the `retreatTo` territory
    final Collection<Unit> airRetreating =
        CollectionUtils.getMatches(
            retreatUnits,
            Matches.unitIsAir().and(Matches.unitIsOwnedBy(battleState.getAttacker())));
    final Collection<Unit> nonAirRetreating = new HashSet<>(retreatUnits);
    nonAirRetreating.removeAll(airRetreating);
    nonAirRetreating.addAll(battleState.getDependentUnits(nonAirRetreating));

    final CompositeChange change = new CompositeChange();
    change.add(ChangeFactory.moveUnits(battleState.getBattleSite(), retreatTo, nonAirRetreating));

    final Collection<IBattle> dependentBattles = battleState.getDependentBattles();
    if (dependentBattles.isEmpty()) {
      // If there are no dependent battles, check landings in allied territories
      change.add(retreatNonCombatTransportedItems(retreatUnits, retreatTo));
    } else {
      // Else retreat the units from combat when their transport retreats
      change.add(retreatCombatTransportedItems(retreatUnits, retreatTo, dependentBattles));
    }

    bridge.addChange(change);
    battleState.retreatUnits(BattleState.Side.OFFENSE, retreatUnits);

    addHistoryRetreatToTerritory(retreatTo, bridge, nonAirRetreating);
    addHistoryRetreat(bridge, airRetreating);
    battleActions.endBattle(IBattle.WhoWon.DEFENDER, bridge);
    broadcastRetreat(bridge, " retreats", " retreats all units to " + retreatTo.getName());
  }

  private Change retreatNonCombatTransportedItems(
      final Collection<Unit> units, final Territory retreatTo) {
    final CompositeChange change = new CompositeChange();
    final Collection<Unit> transports =
        CollectionUtils.getMatches(units, Matches.unitIsTransport());
    final Collection<Unit> retreated = battleState.getTransportDependents(transports);
    if (!retreated.isEmpty()) {
      for (final Unit unit : transports) {
        final Territory retreatedFrom = TransportTracker.getTerritoryTransportHasUnloadedTo(unit);
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
      dependent.removeAttack(route, retreatedUnits);
      TransportTracker.reloadTransports(units, change);
      change.add(ChangeFactory.moveUnits(dependent.getTerritory(), retreatTo, retreatedUnits));
    }
    return change;
  }
}
