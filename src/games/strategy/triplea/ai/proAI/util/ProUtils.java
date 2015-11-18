package games.strategy.triplea.ai.proAI.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.util.Match;

/**
 * Pro AI utilities (these are very general and maybe should be moved into delegate or engine).
 */
public class ProUtils {

  private final ProAI ai;

  public ProUtils(final ProAI ai) {
    this.ai = ai;
  }

  public Map<Unit, Territory> createUnitTerritoryMap(final PlayerID player) {
    final List<Territory> allTerritories = ai.getGameData().getMap().getTerritories();
    final List<Territory> myUnitTerritories =
        Match.getMatches(allTerritories, Matches.territoryHasUnitsOwnedBy(player));
    final Map<Unit, Territory> unitTerritoryMap = new HashMap<Unit, Territory>();
    for (final Territory t : myUnitTerritories) {
      final List<Unit> myUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
      for (final Unit u : myUnits) {
        unitTerritoryMap.put(u, t);
      }
    }
    return unitTerritoryMap;
  }

  public List<PlayerID> getEnemyPlayersInTurnOrder(final PlayerID player) {
    final GameData data = ai.getGameData();
    final List<PlayerID> enemyPlayers = new ArrayList<PlayerID>();
    GameSequence sequence = data.getSequence();
    int startIndex = sequence.getStepIndex();
    for (int i = 0; i < sequence.size(); i++) {
      int currentIndex = startIndex + i;
      if (currentIndex >= sequence.size()) {
        currentIndex -= sequence.size();
      }
      GameStep step = sequence.getStep(currentIndex);
      PlayerID stepPlayer = step.getPlayerID();
      if (step.getName().endsWith("CombatMove") && stepPlayer != null && !enemyPlayers.contains(stepPlayer)
          && !data.getRelationshipTracker().isAllied(player, stepPlayer)) {
        enemyPlayers.add(step.getPlayerID());
      }
    }
    return enemyPlayers;
  }

  public List<PlayerID> getEnemyPlayers(final PlayerID player) {
    final GameData data = ai.getGameData();
    final List<PlayerID> enemyPlayers = new ArrayList<PlayerID>();
    for (final PlayerID players : data.getPlayerList().getPlayers()) {
      if (!data.getRelationshipTracker().isAllied(player, players)) {
        enemyPlayers.add(players);
      }
    }
    return enemyPlayers;
  }

  public List<PlayerID> getAlliedPlayers(final PlayerID player) {
    final GameData data = ai.getGameData();
    final List<PlayerID> alliedPlayers = new ArrayList<PlayerID>();
    for (final PlayerID players : data.getPlayerList().getPlayers()) {
      if (data.getRelationshipTracker().isAllied(player, players)) {
        alliedPlayers.add(players);
      }
    }
    return alliedPlayers;
  }

  public List<PlayerID> getPotentialEnemyPlayers(final PlayerID player) {
    final GameData data = ai.getGameData();
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

  public double getPlayerProduction(final PlayerID player, final GameData data) {
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

  public List<Territory> getLiveEnemyCapitals(final GameData data, final PlayerID player) {
    final List<Territory> enemyCapitals = new ArrayList<Territory>();
    final List<PlayerID> ePlayers = getEnemyPlayers(player);
    for (final PlayerID otherPlayer : ePlayers) {
      enemyCapitals.addAll(TerritoryAttachment.getAllCurrentlyOwnedCapitals(otherPlayer, data));
    }
    enemyCapitals.retainAll(Match.getMatches(enemyCapitals, Matches.TerritoryIsNotImpassableToLandUnits(player, data)));
    enemyCapitals.retainAll(Match.getMatches(enemyCapitals,
        Matches.isTerritoryOwnedBy(getPotentialEnemyPlayers(player))));
    return enemyCapitals;
  }

  public List<Territory> getLiveAlliedCapitals(final GameData data, final PlayerID player) {
    final List<Territory> capitals = new ArrayList<Territory>();
    final List<PlayerID> players = getAlliedPlayers(player);
    for (final PlayerID alliedPlayer : players) {
      capitals.addAll(TerritoryAttachment.getAllCurrentlyOwnedCapitals(alliedPlayer, data));
    }
    capitals.retainAll(Match.getMatches(capitals, Matches.TerritoryIsNotImpassableToLandUnits(player, data)));
    capitals.retainAll(Match.getMatches(capitals, Matches.isTerritoryAllied(player, data)));
    return capitals;
  }

  public int getClosestEnemyLandTerritoryDistance(final GameData data, final PlayerID player, final Territory t) {
    final Set<Territory> landTerritories =
        data.getMap().getNeighbors(t, 9, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true));
    final List<Territory> enemyLandTerritories =
        Match.getMatches(landTerritories, Matches.isTerritoryOwnedBy(getPotentialEnemyPlayers(player)));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyLandTerritories) {
      final int distance =
          data.getMap().getDistance(t, enemyLandTerritory,
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

  public int getClosestEnemyOrNeutralLandTerritoryDistance(final GameData data, final PlayerID player,
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
      int distance =
          data.getMap().getDistance(t, enemyLandTerritory,
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

  public int getClosestEnemyLandTerritoryDistanceOverWater(final GameData data, final PlayerID player, final Territory t) {
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
  public boolean isFFA(final GameData data, final PlayerID player) {
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    final Set<PlayerID> enemies = relationshipTracker.getEnemies(player);
    for (final PlayerID enemy : enemies) {
      if (relationshipTracker.isAtWarWithAnyOfThesePlayers(enemy, enemies)) {
        return true;
      }
    }
    return false;
  }

  public boolean isNeutralPlayer(final PlayerID player) {
    final GameData data = ai.getGameData();
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
  public void pause() {
    try {
      Thread.sleep(AbstractUIContext.getAIPauseDuration());
    } catch (final InterruptedException e) {
      e.printStackTrace();
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
