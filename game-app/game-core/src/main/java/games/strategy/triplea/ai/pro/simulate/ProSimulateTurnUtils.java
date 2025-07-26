package games.strategy.triplea.ai.pro.simulate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.annotation.Nullable;
import org.triplea.java.collections.CollectionUtils;

/** Pro AI simulate turn utilities. */
public final class ProSimulateTurnUtils {
  private ProSimulateTurnUtils() {}

  /**
   * Simulates all pending battles in {@code data}. The simulation results are written as changes to
   * {@code delegateBridge}.
   */
  public static void simulateBattles(
      final ProData proData,
      final GameData data,
      final GamePlayer player,
      final IDelegateBridge delegateBridge,
      final ProOddsCalculator calc) {

    ProLogger.info("Starting battle simulation phase");

    final BattleDelegate battleDelegate = data.getBattleDelegate();
    final Map<BattleType, Collection<Territory>> battleTerritories =
        battleDelegate.getBattleListing().getBattlesMap();
    for (final Entry<BattleType, Collection<Territory>> entry : battleTerritories.entrySet()) {
      for (final Territory t : entry.getValue()) {
        final IBattle battle =
            battleDelegate.getBattleTracker().getPendingBattle(t, entry.getKey());
        final Collection<Unit> attackers = new ArrayList<>(battle.getAttackingUnits());
        attackers.retainAll(t.getUnits());
        final Collection<Unit> defenders = new ArrayList<>(battle.getDefendingUnits());
        defenders.retainAll(t.getUnits());
        final Collection<Unit> bombardingUnits = battle.getBombardingUnits();
        ProLogger.debug("---" + t);
        ProLogger.debug("attackers=" + attackers);
        ProLogger.debug("defenders=" + defenders);
        ProLogger.debug("bombardingUnits=" + bombardingUnits);

        final ProBattleResult result =
            calc.callBattleCalc(proData, t, attackers, defenders, bombardingUnits);
        final Collection<Unit> remainingAttackers = result.getAverageAttackersRemaining();
        final Collection<Unit> remainingDefenders = result.getAverageDefendersRemaining();
        ProLogger.debug("remainingAttackers=" + remainingAttackers);
        ProLogger.debug("remainingDefenders=" + remainingDefenders);

        // Make updates to data
        final List<Unit> attackersToRemove = new ArrayList<>(attackers);
        attackersToRemove.removeAll(remainingAttackers);
        final List<Unit> defendersToRemove =
            CollectionUtils.getMatches(defenders, Matches.unitIsInfrastructure().negate());
        defendersToRemove.removeAll(remainingDefenders);
        final List<Unit> infrastructureToChangeOwner =
            CollectionUtils.getMatches(defenders, Matches.unitIsInfrastructure());
        ProLogger.debug("attackersToRemove=" + attackersToRemove);
        ProLogger.debug("defendersToRemove=" + defendersToRemove);
        ProLogger.debug("infrastructureToChangeOwner=" + infrastructureToChangeOwner);
        final Change attackersKilledChange = ChangeFactory.removeUnits(t, attackersToRemove);
        delegateBridge.addChange(attackersKilledChange);
        final Change defendersKilledChange = ChangeFactory.removeUnits(t, defendersToRemove);
        delegateBridge.addChange(defendersKilledChange);
        BattleTracker.captureOrDestroyUnits(t, player, player, delegateBridge, null);
        if (!checkIfCapturedTerritoryIsAlliedCapital(t, data, player, delegateBridge)) {
          delegateBridge.addChange(ChangeFactory.changeOwner(t, player));
        }
        battleDelegate.getBattleTracker().getConquered().add(t);
        battleDelegate.getBattleTracker().removeBattle(battle, data);
        // note that the Territory object has already changed
        ProLogger.debug("after changes owner=" + t.getOwner() + ", units=" + t.getUnits());
      }
    }
  }

  /**
   * Simulates the transfer of units between two territories for each entry in {@code moveMap}.
   *
   * @return A collection of the results for each simulated transfer.
   */
  public static Map<Territory, ProTerritory> transferMoveMap(
      final ProData proData,
      final Map<Territory, ProTerritory> moveMap,
      final GameState toData,
      final GamePlayer player) {

    ProLogger.info("Transferring move map");

    final Map<Unit, Territory> unitTerritoryMap = proData.getUnitTerritoryMap();

    final Map<Territory, ProTerritory> result = new HashMap<>();
    final List<Unit> usedUnits = new ArrayList<>();
    for (final Territory fromTerritory : moveMap.keySet()) {
      final Territory toTerritory = toData.getMap().getTerritoryOrThrow(fromTerritory.getName());
      final ProTerritory patd = new ProTerritory(toTerritory, proData);
      result.put(toTerritory, patd);
      final Map<Unit, List<Unit>> amphibAttackMap = moveMap.get(fromTerritory).getAmphibAttackMap();
      final Map<Unit, Boolean> isTransportingMap =
          moveMap.get(fromTerritory).getIsTransportingMap();
      final Map<Unit, Territory> transportTerritoryMap =
          moveMap.get(fromTerritory).getTransportTerritoryMap();
      final Map<Unit, Territory> bombardMap = moveMap.get(fromTerritory).getBombardTerritoryMap();
      ProLogger.debug("Transferring " + fromTerritory + " to " + toTerritory);
      final List<Unit> amphibUnits = new ArrayList<>();
      for (final Unit transport : amphibAttackMap.keySet()) {
        final Unit toTransport;
        final List<Unit> toUnits = new ArrayList<>();
        if (isTransportingMap.get(transport)) {
          toTransport =
              transferLoadedTransport(
                  transport,
                  amphibAttackMap.get(transport),
                  unitTerritoryMap,
                  usedUnits,
                  toData,
                  player);
          if (toTransport == null) {
            continue;
          }
          toUnits.addAll(toTransport.getTransporting());
        } else {
          toTransport = transferUnit(transport, unitTerritoryMap, usedUnits, toData, player);
          if (toTransport == null) {
            continue;
          }
          for (final Unit u : amphibAttackMap.get(transport)) {
            final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
            if (toUnit != null) {
              toUnits.add(toUnit);
            }
          }
        }
        patd.addUnits(toUnits);
        patd.putAmphibAttackMap(toTransport, toUnits);
        amphibUnits.addAll(amphibAttackMap.get(transport));
        if (transportTerritoryMap.get(transport) != null) {
          patd.getTransportTerritoryMap()
              .put(
                  toTransport,
                  toData
                      .getMap()
                      .getTerritoryOrThrow(transportTerritoryMap.get(transport).getName()));
        }
        ProLogger.trace(
            "---Transferring transport="
                + transport
                + " with units="
                + amphibAttackMap.get(transport)
                + " unloadTerritory="
                + transportTerritoryMap.get(transport)
                + " to transport="
                + toTransport
                + " with units="
                + toUnits
                + " unloadTerritory="
                + patd.getTransportTerritoryMap().get(toTransport));
      }
      for (final Unit u : moveMap.get(fromTerritory).getUnits()) {
        if (!amphibUnits.contains(u)) {
          final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
          if (toUnit != null) {
            patd.addUnit(toUnit);
            ProLogger.trace("---Transferring unit " + u + " to " + toUnit);
          }
        }
      }
      for (final Unit u : moveMap.get(fromTerritory).getBombers()) {
        final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
        if (toUnit != null) {
          patd.getBombers().add(toUnit);
          ProLogger.trace("---Transferring bomber " + u + " to " + toUnit);
        }
      }
      for (final Unit u : bombardMap.keySet()) {
        final Unit toUnit = transferUnit(u, unitTerritoryMap, usedUnits, toData, player);
        if (toUnit != null) {
          final Territory bombardFromTerritory = bombardMap.get(u);
          final Territory bombardToTerritory =
              toData.getMap().getTerritoryOrThrow(bombardFromTerritory.getName());
          patd.getBombardTerritoryMap().put(toUnit, bombardToTerritory);
          ProLogger.trace(
              "---Transferring bombard="
                  + u
                  + ", bombardFromTerritory="
                  + bombardFromTerritory
                  + " to bombard="
                  + toUnit
                  + ", bombardToTerritory="
                  + bombardToTerritory);
        }
      }
    }
    return result;
  }

  private static boolean checkIfCapturedTerritoryIsAlliedCapital(
      final Territory t,
      final GameState data,
      final GamePlayer player,
      final IDelegateBridge delegateBridge) {

    final Optional<GamePlayer> optionalTerrOrigOwner = OriginalOwnerTracker.getOriginalOwner(t);
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    if (TerritoryAttachment.get(t).map(TerritoryAttachment::isCapital).orElse(false)
        && optionalTerrOrigOwner.isPresent()
        && TerritoryAttachment.getAllCapitals(optionalTerrOrigOwner.get(), data.getMap())
            .contains(t)
        && relationshipTracker.isAllied(optionalTerrOrigOwner.get(), player)) {
      final GamePlayer terrOrigOwner = optionalTerrOrigOwner.get();
      // Give capital and any allied territories back to original owner
      final Collection<Territory> originallyOwned =
          OriginalOwnerTracker.getOriginallyOwned(data, terrOrigOwner);
      final List<Territory> friendlyTerritories =
          CollectionUtils.getMatches(originallyOwned, Matches.isTerritoryAllied(terrOrigOwner));
      friendlyTerritories.add(t);
      for (final Territory item : friendlyTerritories) {
        if (item.isOwnedBy(terrOrigOwner)) {
          continue;
        }
        final Change takeOverFriendlyTerritories = ChangeFactory.changeOwner(item, terrOrigOwner);
        delegateBridge.addChange(takeOverFriendlyTerritories);
        final Collection<Unit> units =
            CollectionUtils.getMatches(item.getUnits(), Matches.unitIsInfrastructure());
        if (!units.isEmpty()) {
          final Change takeOverNonComUnits = ChangeFactory.changeOwner(units, terrOrigOwner, t);
          delegateBridge.addChange(takeOverNonComUnits);
        }
      }
      return true;
    }
    return false;
  }

  private static @Nullable Unit transferUnit(
      final Unit u,
      final Map<Unit, Territory> unitTerritoryMap,
      final List<Unit> usedUnits,
      final GameState toData,
      final GamePlayer player) {

    final Territory unitTerritory = unitTerritoryMap.get(u);
    final List<Unit> toUnits =
        toData
            .getMap()
            .getTerritoryOrThrow(unitTerritory.getName())
            .getMatches(
                ProMatches.unitIsOwnedAndMatchesTypeAndNotTransporting(player, u.getType()));
    for (final Unit toUnit : toUnits) {
      if (!usedUnits.contains(toUnit)) {
        usedUnits.add(toUnit);
        return toUnit;
      }
    }
    return null;
  }

  private static @Nullable Unit transferLoadedTransport(
      final Unit transport,
      final List<Unit> transportingUnits,
      final Map<Unit, Territory> unitTerritoryMap,
      final List<Unit> usedUnits,
      final GameState toData,
      final GamePlayer player) {

    final Territory unitFromTerritory = unitTerritoryMap.get(transport);
    final List<Unit> toTransports =
        toData
            .getMap()
            .getTerritoryOrThrow(unitFromTerritory.getName())
            .getMatches(
                ProMatches.unitIsOwnedAndMatchesTypeAndIsTransporting(player, transport.getType()));
    for (final Unit toTransport : toTransports) {
      if (!usedUnits.contains(toTransport)) {
        final List<Unit> toTransportingUnits = toTransport.getTransporting();
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
