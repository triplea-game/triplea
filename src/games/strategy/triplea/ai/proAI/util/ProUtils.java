package games.strategy.triplea.ai.proAI.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.util.Match;
import games.strategy.util.ThreadUtil;

/**
 * Pro AI utilities (these are very general and maybe should be moved into delegate or engine).
 */
public class ProUtils {

  public static Map<Unit, Territory> createUnitTerritoryMap() {
    final Map<Unit, Territory> unitTerritoryMap = new HashMap<>();
    for (final Territory t : ProData.getData().getMap().getTerritories()) {
      for (final Unit u : t.getUnits().getUnits()) {
        unitTerritoryMap.put(u, t);
      }
    }
    return unitTerritoryMap;
  }

  public static List<PlayerID> getOtherPlayersInTurnOrder(final PlayerID player) {
    final GameData data = ProData.getData();
    final List<PlayerID> players = new ArrayList<>();
    final GameSequence sequence = data.getSequence();
    final int startIndex = sequence.getStepIndex();
    for (int i = 0; i < sequence.size(); i++) {
      int currentIndex = startIndex + i;
      if (currentIndex >= sequence.size()) {
        currentIndex -= sequence.size();
      }
      final GameStep step = sequence.getStep(currentIndex);
      final PlayerID stepPlayer = step.getPlayerID();
      if (step.getName().endsWith("CombatMove") && stepPlayer != null && !stepPlayer.equals(player)
          && !players.contains(stepPlayer)) {
        players.add(step.getPlayerID());
      }
    }
    return players;
  }

  public static List<PlayerID> getAlliedPlayersInTurnOrder(final PlayerID player) {
    final GameData data = ProData.getData();
    final List<PlayerID> players = getOtherPlayersInTurnOrder(player);
    for (final Iterator<PlayerID> it = players.iterator(); it.hasNext();) {
      final PlayerID currentPlayer = it.next();
      if (!data.getRelationshipTracker().isAllied(player, currentPlayer)) {
        it.remove();
      }
    }
    return players;
  }

  public static List<PlayerID> getEnemyPlayersInTurnOrder(final PlayerID player) {
    final GameData data = ProData.getData();
    final List<PlayerID> players = getOtherPlayersInTurnOrder(player);
    for (final Iterator<PlayerID> it = players.iterator(); it.hasNext();) {
      final PlayerID currentPlayer = it.next();
      if (data.getRelationshipTracker().isAllied(player, currentPlayer)) {
        it.remove();
      }
    }
    return players;
  }

  public static boolean isPlayersTurnFirst(final List<PlayerID> playersInOrder, final PlayerID player1,
      final PlayerID player2) {
    for (final PlayerID p : playersInOrder) {
      if (p.equals(player1)) {
        return true;
      } else if (p.equals(player2)) {
        return false;
      }
    }
    return true;
  }

  public static List<PlayerID> getEnemyPlayers(final PlayerID player) {
    final GameData data = ProData.getData();
    final List<PlayerID> enemyPlayers = new ArrayList<>();
    for (final PlayerID players : data.getPlayerList().getPlayers()) {
      if (!data.getRelationshipTracker().isAllied(player, players)) {
        enemyPlayers.add(players);
      }
    }
    return enemyPlayers;
  }

  public static List<PlayerID> getAlliedPlayers(final PlayerID player) {
    final GameData data = ProData.getData();
    final List<PlayerID> alliedPlayers = new ArrayList<>();
    for (final PlayerID players : data.getPlayerList().getPlayers()) {
      if (data.getRelationshipTracker().isAllied(player, players)) {
        alliedPlayers.add(players);
      }
    }
    return alliedPlayers;
  }

  public static List<PlayerID> getPotentialEnemyPlayers(final PlayerID player) {
    final GameData data = ProData.getData();
    final List<PlayerID> otherPlayers = data.getPlayerList().getPlayers();
    for (final Iterator<PlayerID> it = otherPlayers.iterator(); it.hasNext();) {
      final PlayerID otherPlayer = it.next();
      final RelationshipType relation = data.getRelationshipTracker().getRelationshipType(player, otherPlayer);
      if (Matches.RelationshipTypeIsAllied.match(relation) || isNeutralPlayer(otherPlayer)) {
        it.remove();
      }
    }
    return otherPlayers;
  }

  public static double getPlayerProduction(final PlayerID player, final GameData data) {
    int rVal = 0;
    for (final Territory place : data.getMap().getTerritories()) {
      // Match will Check if terr is a Land Convoy Route and check ownership of neighboring Sea Zone, or if contested
      if (place.getOwner().equals(player) && Matches.territoryCanCollectIncomeFrom(player, data).match(place)) {
        rVal += TerritoryAttachment.getProduction(place);
      }
    }
    rVal *= Properties.getPU_Multiplier(data);
    return rVal;
  }

  public static List<Territory> getLiveEnemyCapitals(final GameData data, final PlayerID player) {
    final List<Territory> enemyCapitals = new ArrayList<>();
    final List<PlayerID> ePlayers = getEnemyPlayers(player);
    for (final PlayerID otherPlayer : ePlayers) {
      enemyCapitals.addAll(TerritoryAttachment.getAllCurrentlyOwnedCapitals(otherPlayer, data));
    }
    enemyCapitals.retainAll(Match.getMatches(enemyCapitals, Matches.TerritoryIsNotImpassableToLandUnits(player, data)));
    enemyCapitals
        .retainAll(Match.getMatches(enemyCapitals, Matches.isTerritoryOwnedBy(getPotentialEnemyPlayers(player))));
    return enemyCapitals;
  }

  public static List<Territory> getLiveAlliedCapitals(final GameData data, final PlayerID player) {
    final List<Territory> capitals = new ArrayList<>();
    final List<PlayerID> players = getAlliedPlayers(player);
    for (final PlayerID alliedPlayer : players) {
      capitals.addAll(TerritoryAttachment.getAllCurrentlyOwnedCapitals(alliedPlayer, data));
    }
    capitals.retainAll(Match.getMatches(capitals, Matches.TerritoryIsNotImpassableToLandUnits(player, data)));
    capitals.retainAll(Match.getMatches(capitals, Matches.isTerritoryAllied(player, data)));
    return capitals;
  }

  public static int getClosestEnemyLandTerritoryDistance(final GameData data, final PlayerID player,
      final Territory t) {
    final Set<Territory> landTerritories =
        data.getMap().getNeighbors(t, 9, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true));
    final List<Territory> enemyLandTerritories =
        Match.getMatches(landTerritories, Matches.isTerritoryOwnedBy(getPotentialEnemyPlayers(player)));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyLandTerritories) {
      final int distance = data.getMap().getDistance(t, enemyLandTerritory,
          ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true));
      if (distance < minDistance) {
        minDistance = distance;
      }
    }
    if (minDistance < 10) {
      return minDistance;
    } else {
      return -1;
    }
  }

  public static int getClosestEnemyOrNeutralLandTerritoryDistance(final GameData data, final PlayerID player,
      final Territory t, final Map<Territory, Double> territoryValueMap) {
    final Set<Territory> landTerritories =
        data.getMap().getNeighbors(t, 9, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true));
    final List<Territory> enemyLandTerritories =
        Match.getMatches(landTerritories, Matches.isTerritoryOwnedBy(getEnemyPlayers(player)));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyLandTerritories) {
      if (territoryValueMap.get(enemyLandTerritory) <= 0) {
        continue;
      }
      int distance = data.getMap().getDistance(t, enemyLandTerritory,
          ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true));
      if (enemyLandTerritory.getOwner().isNull()) {
        distance++;
      }
      if (distance < minDistance) {
        minDistance = distance;
      }
    }
    if (minDistance < 10) {
      return minDistance;
    } else {
      return -1;
    }
  }

  public static int getClosestEnemyLandTerritoryDistanceOverWater(final GameData data, final PlayerID player,
      final Territory t) {
    final Set<Territory> neighborTerritories = data.getMap().getNeighbors(t, 9);
    final List<Territory> enemyOrAdjacentLandTerritories =
        Match.getMatches(neighborTerritories, ProMatches.territoryIsOrAdjacentToEnemyNotNeutralLand(player, data));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyOrAdjacentLandTerritories) {
      final int distance =
          data.getMap().getDistance_IgnoreEndForCondition(t, enemyLandTerritory, Matches.TerritoryIsWater);
      if (distance > 0 && distance < minDistance) {
        minDistance = distance;
      }
    }
    if (minDistance < 10) {
      return minDistance;
    } else {
      return -1;
    }
  }

  /**
   * Returns whether the game is a FFA based on whether any of the player's enemies
   * are enemies of each other.
   */
  public static boolean isFFA(final GameData data, final PlayerID player) {
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    final Set<PlayerID> enemies = relationshipTracker.getEnemies(player);
    for (final PlayerID enemy : enemies) {
      if (relationshipTracker.isAtWarWithAnyOfThesePlayers(enemy, enemies)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isNeutralPlayer(final PlayerID player) {
    final GameData data = ProData.getData();
    for (final GameStep gameStep : data.getSequence()) {
      if (player.equals(gameStep.getPlayerID())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Pause the game to allow the human player to see what is going on.
   */
  public static void pause() {
    try {
      ThreadUtil.sleep(AbstractUIContext.getAIPauseDuration());
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
  }
}
