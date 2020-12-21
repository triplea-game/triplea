package games.strategy.triplea.ai.pro.util;

import com.google.common.collect.Streams;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.triplea.java.collections.CollectionUtils;

/** Pro AI utilities (these are very general and maybe should be moved into delegate or engine). */
public final class ProUtils {
  private ProUtils() {}

  /** Returns a list of all players in turn order excluding {@code player}. */
  public static List<GamePlayer> getOtherPlayersInTurnOrder(final GamePlayer player) {
    final GameDataInjections data = player.getData();
    final List<GamePlayer> players = new ArrayList<>();
    final GameSequence sequence = data.getSequence();
    final int startIndex = sequence.getStepIndex();
    for (int i = 0; i < sequence.size(); i++) {
      int currentIndex = startIndex + i;
      if (currentIndex >= sequence.size()) {
        currentIndex -= sequence.size();
      }
      final GameStep step = sequence.getStep(currentIndex);
      final GamePlayer stepPlayer = step.getPlayerId();
      if (step.getName().endsWith("CombatMove")
          && stepPlayer != null
          && !stepPlayer.equals(player)
          && !players.contains(stepPlayer)) {
        players.add(step.getPlayerId());
      }
    }
    return players;
  }

  public static List<GamePlayer> getAlliedPlayersInTurnOrder(final GamePlayer player) {
    final GameDataInjections data = player.getData();
    final List<GamePlayer> players = getOtherPlayersInTurnOrder(player);
    players.removeIf(
        currentPlayer -> !data.getRelationshipTracker().isAllied(player, currentPlayer));
    return players;
  }

  public static List<GamePlayer> getEnemyPlayersInTurnOrder(final GamePlayer player) {
    final GameDataInjections data = player.getData();
    final List<GamePlayer> players = getOtherPlayersInTurnOrder(player);
    players.removeIf(
        currentPlayer -> data.getRelationshipTracker().isAllied(player, currentPlayer));
    return players;
  }

  public static boolean isPlayersTurnFirst(
      final List<GamePlayer> playersInOrder, final GamePlayer player1, final GamePlayer player2) {
    for (final GamePlayer p : playersInOrder) {
      if (p.equals(player1)) {
        return true;
      } else if (p.equals(player2)) {
        return false;
      }
    }
    return true;
  }

  public static List<GamePlayer> getEnemyPlayers(final GamePlayer player) {
    final GameDataInjections data = player.getData();
    final List<GamePlayer> enemyPlayers = new ArrayList<>();
    for (final GamePlayer players : data.getPlayerList().getPlayers()) {
      if (!data.getRelationshipTracker().isAllied(player, players)) {
        enemyPlayers.add(players);
      }
    }
    return enemyPlayers;
  }

  private static List<GamePlayer> getAlliedPlayers(final GamePlayer player) {
    final GameDataInjections data = player.getData();
    final List<GamePlayer> alliedPlayers = new ArrayList<>();
    for (final GamePlayer players : data.getPlayerList().getPlayers()) {
      if (data.getRelationshipTracker().isAllied(player, players)) {
        alliedPlayers.add(players);
      }
    }
    return alliedPlayers;
  }

  /** Given a player, finds all non-allied (enemy) players. */
  public static List<GamePlayer> getPotentialEnemyPlayers(final GamePlayer player) {
    final GameDataInjections data = player.getData();
    final List<GamePlayer> otherPlayers = data.getPlayerList().getPlayers();
    for (final Iterator<GamePlayer> it = otherPlayers.iterator(); it.hasNext(); ) {
      final GamePlayer otherPlayer = it.next();
      final RelationshipType relation =
          data.getRelationshipTracker().getRelationshipType(player, otherPlayer);
      if (Matches.relationshipTypeIsAllied().test(relation)
          || isPassiveNeutralPlayer(otherPlayer)) {
        it.remove();
      }
    }
    return otherPlayers;
  }

  /** Computes PU production amount a given player currently has based on a given game data. */
  public static double getPlayerProduction(final GamePlayer player, final GameDataInjections data) {
    int production = 0;
    for (final Territory place : data.getMap().getTerritories()) {
      // Match will Check if terr is a Land Convoy Route and check ownership of neighboring Sea
      // Zone, or if contested
      if (place.getOwner().equals(player)
          && Matches.territoryCanCollectIncomeFrom(player, data).test(place)) {
        production += TerritoryAttachment.getProduction(place);
      }
    }
    production *= Properties.getPuMultiplier(data.getProperties());
    return production;
  }

  /**
   * Gets list of enemy capitals for a given player that are currently still held by those enemy
   * players.
   */
  public static List<Territory> getLiveEnemyCapitals(
      final GameDataInjections data, final GamePlayer player) {
    final List<Territory> enemyCapitals = new ArrayList<>();
    final List<GamePlayer> enemyPlayers = getEnemyPlayers(player);
    for (final GamePlayer otherPlayer : enemyPlayers) {
      enemyCapitals.addAll(
          TerritoryAttachment.getAllCurrentlyOwnedCapitals(otherPlayer, data.getMap()));
    }
    enemyCapitals.retainAll(
        CollectionUtils.getMatches(
            enemyCapitals,
            Matches.territoryIsNotImpassableToLandUnits(player, data.getProperties())));
    enemyCapitals.retainAll(
        CollectionUtils.getMatches(
            enemyCapitals, Matches.isTerritoryOwnedBy(getPotentialEnemyPlayers(player))));
    return enemyCapitals;
  }

  /** Gets a list of friendly capitals still held by friendly powers. */
  public static List<Territory> getLiveAlliedCapitals(
      final GameDataInjections data, final GamePlayer player) {
    final List<Territory> capitals = new ArrayList<>();
    final List<GamePlayer> players = getAlliedPlayers(player);
    for (final GamePlayer alliedPlayer : players) {
      capitals.addAll(
          TerritoryAttachment.getAllCurrentlyOwnedCapitals(alliedPlayer, data.getMap()));
    }
    capitals.retainAll(
        CollectionUtils.getMatches(
            capitals, Matches.territoryIsNotImpassableToLandUnits(player, data.getProperties())));
    capitals.retainAll(
        CollectionUtils.getMatches(capitals, Matches.isTerritoryAllied(player, data)));
    return capitals;
  }

  /**
   * Returns the distance to the closest enemy land territory to {@code t}.
   *
   * @return -1 if there is no enemy land territory within a distance of 10 of {@code t}.
   */
  public static int getClosestEnemyLandTerritoryDistance(
      final GameDataInjections data, final GamePlayer player, final Territory t) {
    final Set<Territory> landTerritories =
        data.getMap()
            .getNeighbors(
                t,
                9,
                ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()));
    final List<Territory> enemyLandTerritories =
        CollectionUtils.getMatches(
            landTerritories, Matches.isTerritoryOwnedBy(getPotentialEnemyPlayers(player)));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyLandTerritories) {
      final int distance =
          data.getMap()
              .getDistance(
                  t,
                  enemyLandTerritory,
                  ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()));
      if (distance < minDistance) {
        minDistance = distance;
      }
    }
    return (minDistance < 10) ? minDistance : -1;
  }

  /**
   * Returns the distance to the closest enemy or neutral land territory to {@code t}.
   *
   * @return -1 if there is no enemy or neutral land territory within a distance of 10 of {@code t}.
   */
  public static int getClosestEnemyOrNeutralLandTerritoryDistance(
      final GameDataInjections data,
      final GamePlayer player,
      final Territory t,
      final Map<Territory, Double> territoryValueMap) {
    final Set<Territory> landTerritories =
        data.getMap()
            .getNeighbors(
                t,
                9,
                ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()));
    final List<Territory> enemyLandTerritories =
        CollectionUtils.getMatches(
            landTerritories, Matches.isTerritoryOwnedBy(getEnemyPlayers(player)));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyLandTerritories) {
      if (territoryValueMap.get(enemyLandTerritory) <= 0) {
        continue;
      }
      int distance =
          data.getMap()
              .getDistance(
                  t,
                  enemyLandTerritory,
                  ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()));
      if (ProUtils.isNeutralLand(enemyLandTerritory)) {
        distance++;
      }
      if (distance < minDistance) {
        minDistance = distance;
      }
    }
    return (minDistance < 10) ? minDistance : -1;
  }

  /**
   * Returns the distance to the closest enemy land territory to {@code t} assuming movement only
   * through water territories.
   *
   * @return -1 if there is no enemy land territory within a distance of 10 of {@code t} when moving
   *     only through water territories.
   */
  public static int getClosestEnemyLandTerritoryDistanceOverWater(
      final GameDataInjections data, final GamePlayer player, final Territory t) {
    final Set<Territory> neighborTerritories = data.getMap().getNeighbors(t, 9);
    final List<Territory> enemyOrAdjacentLandTerritories =
        CollectionUtils.getMatches(
            neighborTerritories,
            ProMatches.territoryIsOrAdjacentToEnemyNotNeutralLand(player, data));
    int minDistance = 10;
    for (final Territory enemyLandTerritory : enemyOrAdjacentLandTerritories) {
      final int distance =
          data.getMap()
              .getDistanceIgnoreEndForCondition(t, enemyLandTerritory, Matches.territoryIsWater());
      if (distance > 0 && distance < minDistance) {
        minDistance = distance;
      }
    }
    return (minDistance < 10) ? minDistance : -1;
  }

  /**
   * Returns whether the game is a FFA based on whether any of the player's enemies are enemies of
   * each other.
   */
  public static boolean isFfa(final GameDataInjections data, final GamePlayer player) {
    final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
    final Set<GamePlayer> enemies = relationshipTracker.getEnemies(player);
    final Set<GamePlayer> enemiesWithoutNeutrals =
        enemies.stream().filter(p -> !isNeutralPlayer(p)).collect(Collectors.toSet());
    return enemiesWithoutNeutrals.stream()
        .anyMatch(e -> relationshipTracker.isAtWarWithAnyOfThesePlayers(e, enemiesWithoutNeutrals));
  }

  public static boolean isNeutralLand(final Territory t) {
    return !t.isWater() && ProUtils.isNeutralPlayer(t.getOwner());
  }

  /**
   * Determines whether a player is neutral by checking if all players in its alliance can be
   * considered neutral as defined by: isPassiveNeutralPlayer OR (isHidden and defaultType is AI or
   * DoesNothing).
   */
  public static boolean isNeutralPlayer(final GamePlayer player) {
    if (player.isNull()) {
      return true;
    }
    final Set<GamePlayer> allies =
        player.getData().getRelationshipTracker().getAllies(player, true);
    return allies.stream()
        .allMatch(
            a ->
                isPassiveNeutralPlayer(a)
                    || (a.isHidden() && (a.isDefaultTypeAi() || a.isDefaultTypeDoesNothing())));
  }

  /** Returns true if the player is Null or doesn't have a combat move phase. */
  public static boolean isPassiveNeutralPlayer(final GamePlayer player) {
    if (player.isNull()) {
      return true;
    }
    return Streams.stream(player.getData().getSequence())
        .noneMatch(
            s ->
                player.equals(s.getPlayerId())
                    && s.getName().endsWith("CombatMove")
                    && !s.getName().endsWith("NonCombatMove"));
  }
}
