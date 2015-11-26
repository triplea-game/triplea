package games.strategy.triplea.ai.proAI.data;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProMoveOptions {

  private final Map<Territory, ProTerritory> maxMoveMap;
  private final Map<Territory, List<ProTerritory>> moveMaps;

  public ProMoveOptions() {
    maxMoveMap = new HashMap<Territory, ProTerritory>();
    moveMaps = new HashMap<Territory, List<ProTerritory>>();
  }

  public ProMoveOptions(final List<Map<Territory, ProTerritory>> moveMapList, final PlayerID player,
      final boolean isAttacker) {
    this();
    populateMaxMoveMap(moveMapList, player, isAttacker);
    populateMoveMaps(moveMapList);
  }

  public ProTerritory getMax(final Territory t) {
    return maxMoveMap.get(t);
  }

  public List<ProTerritory> getAll(final Territory t) {
    return moveMaps.get(t);
  }

  @Override
  public String toString() {
    return maxMoveMap.toString();
  }

  private void populateMaxMoveMap(final List<Map<Territory, ProTerritory>> moveMaps, final PlayerID player,
      final boolean isAttacker) {

    // Get players in turn order
    final List<PlayerID> players = ProUtils.getOtherPlayersInTurnOrder(player);

    for (final Map<Territory, ProTerritory> moveMap : moveMaps) {
      for (final Territory t : moveMap.keySet()) {

        // Get current player
        PlayerID movePlayer = null;
        final Set<Unit> currentUnits = new HashSet<Unit>(moveMap.get(t).getMaxUnits());
        currentUnits.addAll(moveMap.get(t).getMaxAmphibUnits());
        if (!currentUnits.isEmpty()) {
          movePlayer = currentUnits.iterator().next().getOwner();
        } else {
          continue;
        }

        // Check if mover's turn comes before territory owner's
        if (!ProUtils.isPlayersTurnFirst(players, movePlayer, t.getOwner())) {
          continue;
        }

        // Add to max move map if its empty or its strength is greater than existing
        if (!maxMoveMap.containsKey(t)) {
          maxMoveMap.put(t, moveMap.get(t));
        } else {
          final Set<Unit> maxUnits = new HashSet<Unit>(maxMoveMap.get(t).getMaxUnits());
          maxUnits.addAll(maxMoveMap.get(t).getMaxAmphibUnits());
          double maxStrength = 0;
          if (!maxUnits.isEmpty()) {
            maxStrength =
                ProBattleUtils.estimateStrength(maxUnits.iterator().next().getOwner(), t,
                    new ArrayList<Unit>(maxUnits), new ArrayList<Unit>(), isAttacker);
          }
          final double currentStrength =
              ProBattleUtils.estimateStrength(currentUnits.iterator().next().getOwner(), t, new ArrayList<Unit>(
                  currentUnits), new ArrayList<Unit>(), isAttacker);
          final boolean currentHasLandUnits = Match.someMatch(currentUnits, Matches.UnitIsLand);
          final boolean maxHasLandUnits = Match.someMatch(maxUnits, Matches.UnitIsLand);
          if ((currentHasLandUnits && ((!maxHasLandUnits && !t.isWater()) || currentStrength > maxStrength))
              || ((!maxHasLandUnits || t.isWater()) && currentStrength > maxStrength)) {
            maxMoveMap.put(t, moveMap.get(t));
          }
        }
      }
    }
  }

  private void populateMoveMaps(final List<Map<Territory, ProTerritory>> moveMapList) {
    for (final Map<Territory, ProTerritory> moveMap : moveMapList) {
      for (final Territory t : moveMap.keySet()) {
        if (!moveMaps.containsKey(t)) {
          final List<ProTerritory> list = new ArrayList<ProTerritory>();
          list.add(moveMap.get(t));
          moveMaps.put(t, list);
        } else {
          moveMaps.get(t).add(moveMap.get(t));
        }
      }
    }
  }

}
