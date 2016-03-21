package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pro AI battle utilities.
 */
public class ProTerritoryValueUtils {

  public static double findTerritoryAttackValue(final PlayerID player, final Territory t) {
    final GameData data = ProData.getData();

    final int isEnemyFactory = ProMatches.territoryHasInfraFactoryAndIsEnemyLand(player, data).match(t) ? 1 : 0;
    double value = 3 * TerritoryAttachment.getProduction(t) * (isEnemyFactory + 1);
    if (!t.isWater() && t.getOwner().isNull()) {
      final double strength =
          ProBattleUtils.estimateStrength(t, new ArrayList<>(t.getUnits().getUnits()), new ArrayList<>(), false);

      // Estimate TUV swing as number of casualties * cost
      final double TUVSwing = -(strength / 8) * ProData.minCostPerHitPoint;
      value += TUVSwing;
    }
    return value;
  }

  public static Map<Territory, Double> findTerritoryValues(final PlayerID player,
      final List<Territory> territoriesThatCantBeHeld, final List<Territory> territoriesToAttack) {
    final GameData data = ProData.getData();
    final List<Territory> allTerritories = data.getMap().getTerritories();

    // Get all enemy factories and capitals (check if most territories have factories and if so remove them)
    final Set<Territory> enemyCapitalsAndFactories = new HashSet<>();
    enemyCapitalsAndFactories.addAll(Match.getMatches(
        allTerritories,
        ProMatches.territoryHasInfraFactoryAndIsOwnedByPlayersOrCantBeHeld(player, data,
            ProUtils.getPotentialEnemyPlayers(player), territoriesThatCantBeHeld)));
    final int numPotentialEnemyTerritories =
        Match.countMatches(allTerritories, Matches.isTerritoryOwnedBy(ProUtils.getPotentialEnemyPlayers(player)));
    if (enemyCapitalsAndFactories.size() * 2 >= numPotentialEnemyTerritories) {
      enemyCapitalsAndFactories.clear();
    }
    enemyCapitalsAndFactories.addAll(ProUtils.getLiveEnemyCapitals(data, player));
    enemyCapitalsAndFactories.removeAll(territoriesToAttack);

    // Find max land mass size
    int maxLandMassSize = 1;
    for (final Territory t : allTerritories) {
      if (!t.isWater()) {
        final int landMassSize =
            1 + data.getMap().getNeighbors(t, 6, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true))
                .size();
        if (landMassSize > maxLandMassSize) {
          maxLandMassSize = landMassSize;
        }
      }
    }

    // Loop through factories/capitals and find value
    final Map<Territory, Double> enemyCapitalsAndFactoriesMap = new HashMap<>();
    for (final Territory t : enemyCapitalsAndFactories) {

      // Get factory production if factory
      int factoryProduction = 0;
      if (ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t)) {
        factoryProduction = TerritoryAttachment.getProduction(t);
      }

      // Get player production if capital
      double playerProduction = 0;
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta != null && ta.isCapital()) {
        playerProduction = ProUtils.getPlayerProduction(t.getOwner(), data);
      }

      // Check if neutral
      final int isNeutral = t.getOwner().isNull() ? 1 : 0;

      // Calculate value
      final int landMassSize =
          1 + data.getMap().getNeighbors(t, 6, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true))
              .size();
      final double value =
          Math.sqrt(factoryProduction + Math.sqrt(playerProduction)) * 32 / (1 + 3 * isNeutral) * landMassSize
              / maxLandMassSize;
      enemyCapitalsAndFactoriesMap.put(t, value);
    }

    // Determine value for land territories
    final Map<Territory, Double> territoryValueMap = new HashMap<>();
    for (final Territory t : allTerritories) {
      if (!t.isWater() && !territoriesThatCantBeHeld.contains(t)) {

        // Determine value based on enemy factory land distance
        final List<Double> values = new ArrayList<>();
        for (final Territory enemyCapitalOrFactory : enemyCapitalsAndFactoriesMap.keySet()) {
          final int distance =
              data.getMap().getDistance(t, enemyCapitalOrFactory,
                  ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true));
          if (distance > 0) {
            values.add(enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(2, distance));
          }
        }
        Collections.sort(values, Collections.reverseOrder());
        double capitalOrFactoryValue = 0;
        for (int i = 0; i < values.size(); i++) {
          capitalOrFactoryValue += values.get(i) / Math.pow(2, i); // Decrease each additional factory value by half
        }

        // Determine value based on nearby territory production
        double nearbyEnemyValue = 0;
        final Set<Territory> nearbyTerritories =
            data.getMap().getNeighbors(t, 2, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true));
        final List<Territory> nearbyEnemyTerritories =
            Match.getMatches(nearbyTerritories,
                ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
        nearbyEnemyTerritories.removeAll(territoriesToAttack);
        for (final Territory nearbyEnemyTerritory : nearbyEnemyTerritories) {
          final int distance =
              data.getMap().getDistance(t, nearbyEnemyTerritory,
                  ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true));
          if (distance > 0) {
            double value = TerritoryAttachment.getProduction(nearbyEnemyTerritory);
            if (nearbyEnemyTerritory.getOwner().isNull()) {
              value = findTerritoryAttackValue(player, nearbyEnemyTerritory) / 3; // find neutral value
            } else if (ProMatches.territoryIsAlliedLandAndHasNoEnemyNeighbors(player, data).match(nearbyEnemyTerritory)) {
              value *= 0.1; // reduce value for can't hold amphib allied territories
            }
            if (value > 0) {
              nearbyEnemyValue += (value / Math.pow(2, distance));
            }
          }
        }
        final int landMassSize =
            1 + data.getMap().getNeighbors(t, 6, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, true))
                .size();
        double value = nearbyEnemyValue * landMassSize / maxLandMassSize + capitalOrFactoryValue;
        if (ProMatches.territoryHasInfraFactoryAndIsLand(player).match(t)) {
          value *= 1.1; // prefer territories with factories
        }
        territoryValueMap.put(t, value);
      } else if (!t.isWater()) {
        territoryValueMap.put(t, 0.0);
      }
    }

    // Determine value for water territories
    for (final Territory t : allTerritories) {
      if (!territoriesThatCantBeHeld.contains(t) && t.isWater()
          && !data.getMap().getNeighbors(t, Matches.TerritoryIsWater).isEmpty()) {

        // Determine value based on enemy factory distance
        final List<Double> values = new ArrayList<>();
        for (final Territory enemyCapitalOrFactory : enemyCapitalsAndFactoriesMap.keySet()) {
          final Route route =
              data.getMap().getRoute_IgnoreEnd(t, enemyCapitalOrFactory,
                  ProMatches.territoryCanMoveSeaUnits(player, data, true));
          if (route == null || MoveValidator.validateCanal(route, null, player, data) != null) {
            continue;
          }
          final int distance = route.numberOfSteps();
          if (distance > 0) {
            values.add(enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(2, distance));
          }
        }
        Collections.sort(values, Collections.reverseOrder());
        double capitalOrFactoryValue = 0;
        for (int i = 0; i < values.size(); i++) {
          capitalOrFactoryValue += values.get(i) / Math.pow(2, i); // Decrease each additional factory value by half
        }

        // Determine value based on nearby territory production
        double nearbyLandValue = 0;
        final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, 3);
        final List<Territory> nearbyLandTerritories =
            Match.getMatches(nearbyTerritories, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data, false));
        nearbyLandTerritories.removeAll(territoriesToAttack);
        for (final Territory nearbyLandTerritory : nearbyLandTerritories) {
          final Route route =
              data.getMap().getRoute_IgnoreEnd(t, nearbyLandTerritory,
                  ProMatches.territoryCanMoveSeaUnits(player, data, true));
          if (route == null || MoveValidator.validateCanal(route, null, player, data) != null) {
            continue;
          }
          final int distance = route.numberOfSteps();
          if (distance > 0 && distance <= 3) {
            if (ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld).match(
                nearbyLandTerritory)) {
              double value = TerritoryAttachment.getProduction(nearbyLandTerritory);
              if (nearbyLandTerritory.getOwner().isNull()) {
                value = findTerritoryAttackValue(player, nearbyLandTerritory);
              }
              nearbyLandValue += value;
            }
            nearbyLandValue += territoryValueMap.get(nearbyLandTerritory);
          }
        }
        final double value = capitalOrFactoryValue / 100 + nearbyLandValue / 10;
        territoryValueMap.put(t, value);
      } else if (t.isWater()) {
        territoryValueMap.put(t, 0.0);
      }
    }
    return territoryValueMap;
  }

  public static Map<Territory, Double> findSeaTerritoryValues(final PlayerID player,
      final List<Territory> territoriesThatCantBeHeld) {
    final GameData data = ProData.getData();
    final List<Territory> allTerritories = data.getMap().getTerritories();

    // Determine value for water territories
    final Map<Territory, Double> territoryValueMap = new HashMap<>();
    for (final Territory t : allTerritories) {
      if (!territoriesThatCantBeHeld.contains(t) && t.isWater()
          && !data.getMap().getNeighbors(t, Matches.TerritoryIsWater).isEmpty()) {

        // Determine sea value based on nearby convoy production
        double nearbySeaProductionValue = 0;
        final Set<Territory> nearbySeaTerritories =
            data.getMap().getNeighbors(t, 4, ProMatches.territoryCanMoveSeaUnits(player, data, true));
        final List<Territory> nearbyEnemySeaTerritories =
            Match.getMatches(nearbySeaTerritories,
                ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
        for (final Territory nearbyEnemySeaTerritory : nearbyEnemySeaTerritories) {
          final Route route =
              data.getMap().getRoute_IgnoreEnd(t, nearbyEnemySeaTerritory,
                  ProMatches.territoryCanMoveSeaUnits(player, data, true));
          if (route == null || MoveValidator.validateCanal(route, null, player, data) != null) {
            continue;
          }
          final int distance = route.numberOfSteps();
          if (distance > 0) {
            nearbySeaProductionValue +=
                TerritoryAttachment.getProduction(nearbyEnemySeaTerritory) / Math.pow(2, distance);
          }
        }

        // Determine sea value based on nearby enemy sea units
        double nearbyEnemySeaUnitValue = 0;
        final List<Territory> nearbyEnemySeaUnitTerritories =
            Match.getMatches(nearbySeaTerritories, Matches.territoryHasEnemyUnits(player, data));
        for (final Territory nearbyEnemySeaTerritory : nearbyEnemySeaUnitTerritories) {
          final Route route =
              data.getMap().getRoute_IgnoreEnd(t, nearbyEnemySeaTerritory,
                  ProMatches.territoryCanMoveSeaUnits(player, data, true));
          if (route == null || MoveValidator.validateCanal(route, null, player, data) != null) {
            continue;
          }
          final int distance = route.numberOfSteps();
          if (distance > 0) {
            nearbyEnemySeaUnitValue +=
                nearbyEnemySeaTerritory.getUnits().countMatches(Matches.unitIsEnemyOf(data, player))
                    / Math.pow(2, distance);
          }
        }

        // Set final values
        final double value = 100 * nearbySeaProductionValue + nearbyEnemySeaUnitValue;
        territoryValueMap.put(t, value);
      } else if (t.isWater()) {
        territoryValueMap.put(t, 0.0);
      }
    }
    return territoryValueMap;
  }
}
