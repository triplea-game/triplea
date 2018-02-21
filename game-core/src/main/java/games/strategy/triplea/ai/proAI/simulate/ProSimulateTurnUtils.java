package games.strategy.triplea.ai.proAI.simulate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.data.ProBattleResult;
import games.strategy.triplea.ai.proAI.data.ProTerritory;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProOddsCalculator;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.CollectionUtils;

/**
 * Pro AI simulate turn utilities.
 */
public class ProSimulateTurnUtils {

  public static void simulateBattles(final GameData data, final PlayerID player, final IDelegateBridge delegateBridge,
      final ProOddsCalculator calc) {

    ProLogger.info("Starting battle simulation phase");

    final BattleDelegate battleDelegate = DelegateFinder.battleDelegate(data);
    final Map<BattleType, Collection<Territory>> battleTerritories = battleDelegate.getBattles().getBattles();
    for (final Entry<BattleType, Collection<Territory>> entry : battleTerritories.entrySet()) {
      for (final Territory t : entry.getValue()) {
        final IBattle battle =
            battleDelegate.getBattleTracker().getPendingBattle(t, entry.getKey().isBombingRun(), entry.getKey());
        final List<Unit> attackers = (List<Unit>) battle.getAttackingUnits();
        attackers.retainAll(t.getUnits().getUnits());
        final List<Unit> defenders = (List<Unit>) battle.getDefendingUnits();
        defenders.retainAll(t.getUnits().getUnits());
        final Set<Unit> bombardingUnits = new HashSet<>(battle.getBombardingUnits());
        ProLogger.debug("---" + t);
        ProLogger.debug("attackers=" + attackers);
        ProLogger.debug("defenders=" + defenders);
        ProLogger.debug("bombardingUnits=" + bombardingUnits);
        final ProBattleResult result = calc.callBattleCalculator(t, attackers, defenders, bombardingUnits);
        final List<Unit> remainingUnits = result.getAverageAttackersRemaining();
        ProLogger.debug("remainingUnits=" + remainingUnits);

        // Make updates to data
        final List<Unit> attackersToRemove = new ArrayList<>(attackers);
        attackersToRemove.removeAll(remainingUnits);
        final List<Unit> defendersToRemove =
            CollectionUtils.getMatches(defenders, Matches.unitIsInfrastructure().negate());
        final List<Unit> infrastructureToChangeOwner =
            CollectionUtils.getMatches(defenders, Matches.unitIsInfrastructure());
        ProLogger.debug("attackersToRemove=" + attackersToRemove);
        ProLogger.debug("defendersToRemove=" + defendersToRemove);
        ProLogger.debug("infrastructureToChangeOwner=" + infrastructureToChangeOwner);
        final Change attackerskilledChange = ChangeFactory.removeUnits(t, attackersToRemove);
        delegateBridge.addChange(attackerskilledChange);
        final Change defenderskilledChange = ChangeFactory.removeUnits(t, defendersToRemove);
        delegateBridge.addChange(defenderskilledChange);
        BattleTracker.captureOrDestroyUnits(t, player, player, delegateBridge, null);
        if (!checkIfCapturedTerritoryIsAlliedCapital(t, data, player, delegateBridge)) {
          delegateBridge.addChange(ChangeFactory.changeOwner(t, player));
        }
        battleDelegate.getBattleTracker().getConquered().add(t);
        battleDelegate.getBattleTracker().removeBattle(battle);
        final Territory updatedTerritory = data.getMap().getTerritory(t.getName());
        ProLogger.debug(
            "after changes owner=" + updatedTerritory.getOwner() + ", units=" + updatedTerritory.getUnits().getUnits());
      }
    }
  }

  public static Map<Territory, ProTerritory> transferMoveMap(final Map<Territory, ProTerritory> moveMap,
      final GameData toData, final PlayerID player) {

    ProLogger.info("Transferring move map");

    final Map<Unit, Territory> unitTerritoryMap = ProData.unitTerritoryMap;

    final Map<Territory, ProTerritory> result = new HashMap<>();
    final List<Unit> usedUnits = new ArrayList<>();
    for (final Entry<Territory, ProTerritory> territoryProTerritoryEntry : moveMap.entrySet()) {
      final Territory toTerritory = toData.getMap().getTerritory((territoryProTerritoryEntry.getKey()).getName());
      final ProTerritory patd = new ProTerritory(toTerritory);
      result.put(toTerritory, patd);
      final Map<Unit, List<Unit>> amphibAttackMap = territoryProTerritoryEntry.getValue().getAmphibAttackMap();
      final Map<Unit, Boolean> isTransportingMap = territoryProTerritoryEntry.getValue().getIsTransportingMap();
      final Map<Unit, Territory> transportTerritoryMap = territoryProTerritoryEntry.getValue().getTransportTerritoryMap();
      final Map<Unit, Territory> bombardMap = territoryProTerritoryEntry.getValue().getBombardTerritoryMap();
      ProLogger.debug("Transferring " + territoryProTerritoryEntry.getKey() + " to " + toTerritory);
      final List<Unit> amphibUnits = new ArrayList<>();
      for (final Entry<Unit, List<Unit>> unitListEntry : amphibAttackMap.entrySet()) {
        final Unit toTransport;
        final List<Unit> toUnits = new ArrayList<>();
        if (isTransportingMap.get(unitListEntry.getKey())) {
          toTransport = transferLoadedTransport(unitListEntry.getKey(), unitListEntry.getValue(), unitTerritoryMap, usedUnits,
              toData, player);
          toUnits.addAll(TransportTracker.transporting(toTransport));
        } else {
          toTransport = transferUnit(unitListEntry.getKey(), unitTerritoryMap, usedUnits, toData, player);
          for (final Unit u : unitListEntry.getValue()) {
            final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
            toUnits.add(toUnit);
          }
        }
        patd.addUnits(toUnits);
        patd.putAmphibAttackMap(toTransport, toUnits);
        amphibUnits.addAll(unitListEntry.getValue());
        if (transportTerritoryMap.get(unitListEntry.getKey()) != null) {
          patd.getTransportTerritoryMap().put(toTransport,
              toData.getMap().getTerritory(transportTerritoryMap.get(unitListEntry.getKey()).getName()));
        }
        ProLogger.trace("---Transferring transport=" + unitListEntry.getKey() + " with units=" + unitListEntry
            .getValue()
            + " unloadTerritory=" + transportTerritoryMap.get(unitListEntry.getKey()) + " to transport=" + toTransport
            + " with units=" + toUnits + " unloadTerritory=" + patd.getTransportTerritoryMap().get(toTransport));
      }
      for (final Unit u : territoryProTerritoryEntry.getValue().getUnits()) {
        if (!amphibUnits.contains(u)) {
          final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
          patd.addUnit(toUnit);
          ProLogger.trace("---Transferring unit " + u + " to " + toUnit);
        }
      }
      for (final Unit u : territoryProTerritoryEntry.getValue().getBombers()) {
        final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
        patd.getBombers().add(toUnit);
        ProLogger.trace("---Transferring bomber " + u + " to " + toUnit);
      }
      for (final Entry<Unit, Territory> unitTerritoryEntry : bombardMap.entrySet()) {
        final Unit toUnit = transferUnit(unitTerritoryEntry.getKey(), unitTerritoryMap, usedUnits, toData, player);
        patd.getBombardTerritoryMap().put(toUnit, toData.getMap().getTerritory(unitTerritoryEntry.getValue().getName()));
        ProLogger.trace("---Transferring bombard=" + unitTerritoryEntry.getKey()
            + ", bombardFromTerritory=" + unitTerritoryEntry.getValue() + " to bomard="
            + toUnit + ", bombardFromTerritory=" + patd.getBombardTerritoryMap().get(toUnit));
      }
    }
    return result;
  }

  private static boolean checkIfCapturedTerritoryIsAlliedCapital(final Territory t, final GameData data,
      final PlayerID player, final IDelegateBridge delegateBridge) {

    final PlayerID terrOrigOwner = OriginalOwnerTracker.getOriginalOwner(t);
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    final TerritoryAttachment ta = TerritoryAttachment.get(t);
    if (ta != null && ta.getCapital() != null && terrOrigOwner != null
        && TerritoryAttachment.getAllCapitals(terrOrigOwner, data).contains(t)
        && relationshipTracker.isAllied(terrOrigOwner, player)) {

      // Give capital and any allied territories back to original owner
      final Collection<Territory> originallyOwned = OriginalOwnerTracker.getOriginallyOwned(data, terrOrigOwner);
      final List<Territory> friendlyTerritories =
          CollectionUtils.getMatches(originallyOwned, Matches.isTerritoryAllied(terrOrigOwner, data));
      friendlyTerritories.add(t);
      for (final Territory item : friendlyTerritories) {
        if (item.getOwner() == terrOrigOwner) {
          continue;
        }
        final Change takeOverFriendlyTerritories = ChangeFactory.changeOwner(item, terrOrigOwner);
        delegateBridge.addChange(takeOverFriendlyTerritories);
        final Collection<Unit> units =
            CollectionUtils.getMatches(item.getUnits().getUnits(), Matches.unitIsInfrastructure());
        if (!units.isEmpty()) {
          final Change takeOverNonComUnits = ChangeFactory.changeOwner(units, terrOrigOwner, t);
          delegateBridge.addChange(takeOverNonComUnits);
        }
      }
      return true;
    }
    return false;
  }

  private static Unit transferUnit(final Unit u, final Map<Unit, Territory> unitTerritoryMap,
      final List<Unit> usedUnits, final GameData toData, final PlayerID player) {

    final Territory unitTerritory = unitTerritoryMap.get(u);
    final List<Unit> toUnits = toData.getMap().getTerritory(unitTerritory.getName()).getUnits()
        .getMatches(ProMatches.unitIsOwnedAndMatchesTypeAndNotTransporting(player, u.getType()));
    for (final Unit toUnit : toUnits) {
      if (!usedUnits.contains(toUnit)) {
        usedUnits.add(toUnit);
        return toUnit;
      }
    }
    return null;
  }

  private static Unit transferLoadedTransport(final Unit transport, final List<Unit> transportingUnits,
      final Map<Unit, Territory> unitTerritoryMap, final List<Unit> usedUnits, final GameData toData,
      final PlayerID player) {

    final Territory unitTerritory = unitTerritoryMap.get(transport);
    final List<Unit> toTransports = toData.getMap().getTerritory(unitTerritory.getName()).getUnits()
        .getMatches(ProMatches.unitIsOwnedAndMatchesTypeAndIsTransporting(player, transport.getType()));
    for (final Unit toTransport : toTransports) {
      if (!usedUnits.contains(toTransport)) {
        final List<Unit> toTransportingUnits = (List<Unit>) TransportTracker.transporting(toTransport);
        if (transportingUnits.size() == toTransportingUnits.size()) {
          boolean canTransfer = true;
          for (int i = 0; i < transportingUnits.size(); i++) {
            if (!transportingUnits.get(i).getType().equals(toTransportingUnits.get(i).getType())) {
              canTransfer = false;
              break;
            }
          }
          if (canTransfer) {
            usedUnits.add(toTransport);
            usedUnits.addAll(toTransportingUnits);
            return toTransport;
          }
        }
      }
    }
    return null;
  }
}
