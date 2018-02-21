package games.strategy.triplea.ai.proAI.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.delegate.Matches;

public class ProOtherMoveOptions {

  private final Map<Territory, ProTerritory> maxMoveMap;
  private final Map<Territory, List<ProTerritory>> moveMaps;

  public ProOtherMoveOptions() {
    maxMoveMap = new HashMap<>();
    moveMaps = new HashMap<>();
  }

  public ProOtherMoveOptions(final List<Map<Territory, ProTerritory>> moveMapList, final PlayerID player,
      final boolean isAttacker) {
    maxMoveMap = createMaxMoveMap(moveMapList, player, isAttacker);
    moveMaps = createMoveMaps(moveMapList);
  }

  public ProTerritory getMax(final Territory t) {
    return maxMoveMap.get(t);
  }

  List<ProTerritory> getAll(final Territory t) {
    final List<ProTerritory> result = moveMaps.get(t);
    if (result != null) {
      return result;
    }
    return new ArrayList<>();
  }

  @Override
  public String toString() {
    return maxMoveMap.toString();
  }

  private static Map<Territory, ProTerritory> createMaxMoveMap(final List<Map<Territory, ProTerritory>> moveMaps,
      final PlayerID player, final boolean isAttacker) {


    final Map<Territory, ProTerritory> result = new HashMap<>();
    final List<PlayerID> players = ProUtils.getOtherPlayersInTurnOrder(player);
    for (final Map<Territory, ProTerritory> moveMap : moveMaps) {
      for (final Map.Entry<Territory, ProTerritory> territoryProTerritoryEntry : moveMap.entrySet()) {

        // Get current player
        final PlayerID movePlayer;
        final Set<Unit> currentUnits = new HashSet<>(territoryProTerritoryEntry.getValue().getMaxUnits());
        currentUnits.addAll(territoryProTerritoryEntry.getValue().getMaxAmphibUnits());
        if (!currentUnits.isEmpty()) {
          movePlayer = currentUnits.iterator().next().getOwner();
        } else {
          continue;
        }

        // Skip if checking allied moves and their turn doesn't come before territory owner's
        if (ProData.getData().getRelationshipTracker().isAllied(player, movePlayer)
            && !ProUtils.isPlayersTurnFirst(players, movePlayer, (territoryProTerritoryEntry.getKey()).getOwner())) {
          continue;
        }

        // Add to max move map if its empty or its strength is greater than existing
        if (!result.containsKey(territoryProTerritoryEntry.getKey())) {
          result.put(territoryProTerritoryEntry.getKey(), territoryProTerritoryEntry.getValue());
        } else {
          final Set<Unit> maxUnits = new HashSet<>(result.get(territoryProTerritoryEntry.getKey()).getMaxUnits());
          maxUnits.addAll(result.get(territoryProTerritoryEntry.getKey()).getMaxAmphibUnits());
          double maxStrength = 0;
          if (!maxUnits.isEmpty()) {
            maxStrength = ProBattleUtils.estimateStrength(territoryProTerritoryEntry.getKey(), new ArrayList<>(maxUnits), new ArrayList<>(), isAttacker);
          }
          final double currentStrength =
              ProBattleUtils.estimateStrength(territoryProTerritoryEntry.getKey(), new ArrayList<>(currentUnits), new ArrayList<>(), isAttacker);
          final boolean currentHasLandUnits = currentUnits.stream().anyMatch(Matches.unitIsLand());
          final boolean maxHasLandUnits = maxUnits.stream().anyMatch(Matches.unitIsLand());
          if ((currentHasLandUnits && ((!maxHasLandUnits && !(territoryProTerritoryEntry.getKey()).isWater()) || currentStrength > maxStrength))
              || ((!maxHasLandUnits || (territoryProTerritoryEntry.getKey()).isWater()) && currentStrength > maxStrength)) {
            result.put(territoryProTerritoryEntry.getKey(), territoryProTerritoryEntry.getValue());
          }
        }
      }
    }
    return result;
  }

  private static Map<Territory, List<ProTerritory>> createMoveMaps(
      final List<Map<Territory, ProTerritory>> moveMapList) {

    final Map<Territory, List<ProTerritory>> result = new HashMap<>();
    for (final Map<Territory, ProTerritory> moveMap : moveMapList) {
      for (final Map.Entry<Territory, ProTerritory> territoryProTerritoryEntry : moveMap.entrySet()) {
        if (!result.containsKey(territoryProTerritoryEntry.getKey())) {
          final List<ProTerritory> list = new ArrayList<>();
          list.add(territoryProTerritoryEntry.getValue());
          result.put(territoryProTerritoryEntry.getKey(), list);
        } else {
          result.get(territoryProTerritoryEntry.getKey()).add(territoryProTerritoryEntry.getValue());
        }
      }
    }
    return result;
  }

}
