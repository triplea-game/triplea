package games.strategy.triplea.ai.proAI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

public class ProAttackOptions {

  private ProUtils utils;
  private ProBattleUtils battleUtils;
  private Map<Territory, ProAttackTerritoryData> maxAttackMap;
  private Map<Territory, List<ProAttackTerritoryData>> attackMaps;

  public ProAttackOptions(ProUtils utils, ProBattleUtils battleUtils) {
    this.utils = utils;
    this.battleUtils = battleUtils;
    maxAttackMap = new HashMap<Territory, ProAttackTerritoryData>();
    attackMaps = new HashMap<Territory, List<ProAttackTerritoryData>>();
  }

  public ProAttackOptions(ProUtils utils, ProBattleUtils battleUtils,
      List<Map<Territory, ProAttackTerritoryData>> attackMapList, PlayerID player) {
    this(utils, battleUtils);
    populateMaxAttackMap(attackMapList, player);
    populateAttackMaps(attackMapList);
  }

  public ProAttackTerritoryData getMax(Territory t) {
    return maxAttackMap.get(t);
  }

  public List<ProAttackTerritoryData> getAll(Territory t) {
    return attackMaps.get(t);
  }

  @Override
  public String toString() {
    return maxAttackMap.toString();
  }

  private void populateMaxAttackMap(List<Map<Territory, ProAttackTerritoryData>> attackMaps, PlayerID player) {

    // Get players in turn order
    List<PlayerID> players = utils.getOtherPlayersInTurnOrder(player);

    for (final Map<Territory, ProAttackTerritoryData> attackMap2 : attackMaps) {
      for (final Territory t : attackMap2.keySet()) {

        // Get attack player
        PlayerID attackPlayer = null;
        final Set<Unit> currentUnits = new HashSet<Unit>(attackMap2.get(t).getMaxUnits());
        currentUnits.addAll(attackMap2.get(t).getMaxAmphibUnits());
        if (!currentUnits.isEmpty()) {
          attackPlayer = currentUnits.iterator().next().getOwner();
        } else {
          continue;
        }

        // Check if attacker's turn comes before territory owner's
        boolean isAttackerBeforeDefender = true;
        for (PlayerID p : players) {
          if (p.equals(attackPlayer)) {
            break;
          } else if (p.equals(t.getOwner())) {
            isAttackerBeforeDefender = false;
          }
        }
        if (!isAttackerBeforeDefender) {
          continue;
        }

        // Add to max attack map if its empty or its strength is greater than existing
        if (!maxAttackMap.containsKey(t)) {
          maxAttackMap.put(t, attackMap2.get(t));
        } else {
          final Set<Unit> maxUnits = new HashSet<Unit>(maxAttackMap.get(t).getMaxUnits());
          maxUnits.addAll(maxAttackMap.get(t).getMaxAmphibUnits());
          double maxStrength = 0;
          if (!maxUnits.isEmpty()) {
            maxStrength =
                battleUtils.estimateStrength(maxUnits.iterator().next().getOwner(), t, new ArrayList<Unit>(maxUnits),
                    new ArrayList<Unit>(), true);
          }
          double currentStrength =
              battleUtils.estimateStrength(currentUnits.iterator().next().getOwner(), t, new ArrayList<Unit>(
                  currentUnits), new ArrayList<Unit>(), true);
          final boolean currentHasLandUnits = Match.someMatch(currentUnits, Matches.UnitIsLand);
          final boolean maxHasLandUnits = Match.someMatch(maxUnits, Matches.UnitIsLand);
          if ((currentHasLandUnits && ((!maxHasLandUnits && !t.isWater()) || currentStrength > maxStrength))
              || ((!maxHasLandUnits || t.isWater()) && currentStrength > maxStrength)) {
            maxAttackMap.put(t, attackMap2.get(t));
          }
        }
      }
    }
  }

  private void populateAttackMaps(List<Map<Territory, ProAttackTerritoryData>> attackMapList) {
    for (final Map<Territory, ProAttackTerritoryData> attackMap : attackMapList) {
      for (final Territory t : attackMap.keySet()) {
        if (!attackMaps.containsKey(t)) {
          List<ProAttackTerritoryData> list = new ArrayList<ProAttackTerritoryData>();
          list.add(attackMap.get(t));
          attackMaps.put(t, list);
        } else {
          attackMaps.get(t).add(attackMap.get(t));
        }
      }
    }
  }

}
