package games.strategy.triplea.ai.proAI.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.util.CollectionUtils;

/**
 * Pro AI battle utilities.
 */
public class ProTerritoryValueUtils {

  private static final int MIN_FACTORY_CHECK_DISTANCE = 9;
  private static final int MAX_FACTORY_CHECK_DISTANCE = 30;

  public static double findTerritoryAttackValue(final PlayerID player, final Territory t) {
    final GameData data = ProData.getData();
    final int isEnemyFactory = ProMatches.territoryHasInfraFactoryAndIsEnemyLand(player, data).test(t) ? 1 : 0;
    double value = 3 * TerritoryAttachment.getProduction(t) * (isEnemyFactory + 1);
    if (!t.isWater() && t.getOwner().isNull()) {
      final double strength =
          ProBattleUtils.estimateStrength(t, new ArrayList<>(t.getUnits().getUnits()), new ArrayList<>(), false);

      // Estimate TUV swing as number of casualties * cost
      final double tuvSwing = -(strength / 8) * ProData.minCostPerHitPoint;
      value += tuvSwing;
    }

    return value;
  }

  public static Map<Territory, Double> findTerritoryValues(final PlayerID player,
      final List<Territory> territoriesThatCantBeHeld, final List<Territory> territoriesToAttack) {

    return findTerritoryValues(player, territoriesThatCantBeHeld, territoriesToAttack,
        new HashSet<>(ProData.getData().getMap().getTerritories()));
  }

  public static Map<Territory, Double> findTerritoryValues(final PlayerID player,
      final List<Territory> territoriesThatCantBeHeld, final List<Territory> territoriesToAttack,
      final Set<Territory> territoriesToCheck) {

    final int maxLandMassSize = findMaxLandMassSize(player);

    final Map<Territory, Double> enemyCapitalsAndFactoriesMap =
        findEnemyCapitalsAndFactoriesValue(player, maxLandMassSize, territoriesThatCantBeHeld, territoriesToAttack);

    final Map<Territory, Double> territoryValueMap = new HashMap<>();
    for (final Territory t : territoriesToCheck) {
      if (!t.isWater()) {
        final double value = findLandValue(t, player, maxLandMassSize, enemyCapitalsAndFactoriesMap,
            territoriesThatCantBeHeld, territoriesToAttack);
        territoryValueMap.put(t, value);
      }
    }

    for (final Territory t : territoriesToCheck) {
      if (t.isWater()) {
        final double value = findWaterValue(t, player, maxLandMassSize, enemyCapitalsAndFactoriesMap,
            territoriesThatCantBeHeld, territoriesToAttack, territoryValueMap);
        territoryValueMap.put(t, value);
      }
    }

    return territoryValueMap;
  }

  public static Map<Territory, Double> findSeaTerritoryValues(final PlayerID player,
      final List<Territory> territoriesThatCantBeHeld) {

    // Determine value for water territories
    final Map<Territory, Double> territoryValueMap = new HashMap<>();
    final GameData data = ProData.getData();
    for (final Territory t : data.getMap().getTerritories()) {
      if (!territoriesThatCantBeHeld.contains(t) && t.isWater()
          && !data.getMap().getNeighbors(t, Matches.territoryIsWater()).isEmpty()) {

        // Determine sea value based on nearby convoy production
        double nearbySeaProductionValue = 0;
        final Set<Territory> nearbySeaTerritories =
            data.getMap().getNeighbors(t, 4, ProMatches.territoryCanMoveSeaUnits(player, data, true));
        final List<Territory> nearbyEnemySeaTerritories = CollectionUtils.getMatches(nearbySeaTerritories,
            ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
        for (final Territory nearbyEnemySeaTerritory : nearbyEnemySeaTerritories) {
          final Route route = data.getMap().getRoute_IgnoreEnd(t, nearbyEnemySeaTerritory,
              ProMatches.territoryCanMoveSeaUnits(player, data, true));
          if ((route == null) || (MoveValidator.validateCanal(route, null, player, data) != null)) {
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
            CollectionUtils.getMatches(nearbySeaTerritories, Matches.territoryHasEnemyUnits(player, data));
        for (final Territory nearbyEnemySeaTerritory : nearbyEnemySeaUnitTerritories) {
          final Route route = data.getMap().getRoute_IgnoreEnd(t, nearbyEnemySeaTerritory,
              ProMatches.territoryCanMoveSeaUnits(player, data, true));
          if ((route == null) || (MoveValidator.validateCanal(route, null, player, data) != null)) {
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
        final double value = (100 * nearbySeaProductionValue) + nearbyEnemySeaUnitValue;
        territoryValueMap.put(t, value);
      } else if (t.isWater()) {
        territoryValueMap.put(t, 0.0);
      }
    }

    return territoryValueMap;
  }

  private static int findMaxLandMassSize(final PlayerID player) {
    int maxLandMassSize = 1;
    final GameData data = ProData.getData();
    for (final Territory t : data.getMap().getTerritories()) {
      if (!t.isWater()) {
        final int landMassSize = 1 + data.getMap()
            .getNeighbors(t, 6, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data)).size();
        if (landMassSize > maxLandMassSize) {
          maxLandMassSize = landMassSize;
        }
      }
    }

    return maxLandMassSize;
  }

  private static Map<Territory, Double> findEnemyCapitalsAndFactoriesValue(final PlayerID player,
      final int maxLandMassSize, final List<Territory> territoriesThatCantBeHeld,
      final List<Territory> territoriesToAttack) {

    // Get all enemy factories and capitals (check if most territories have factories and if so remove them)
    final Set<Territory> enemyCapitalsAndFactories = new HashSet<>();
    final GameData data = ProData.getData();
    final List<Territory> allTerritories = data.getMap().getTerritories();
    enemyCapitalsAndFactories.addAll(
        CollectionUtils.getMatches(allTerritories, ProMatches.territoryHasInfraFactoryAndIsOwnedByPlayersOrCantBeHeld(
            player, ProUtils.getPotentialEnemyPlayers(player), territoriesThatCantBeHeld)));
    final int numPotentialEnemyTerritories = CollectionUtils.countMatches(allTerritories,
        Matches.isTerritoryOwnedBy(ProUtils.getPotentialEnemyPlayers(player)));
    if ((enemyCapitalsAndFactories.size() * 2) >= numPotentialEnemyTerritories) {
      enemyCapitalsAndFactories.clear();
    }
    enemyCapitalsAndFactories.addAll(ProUtils.getLiveEnemyCapitals(data, player));
    enemyCapitalsAndFactories.removeAll(territoriesToAttack);

    // Find value for each enemy capital and factory
    final Map<Territory, Double> enemyCapitalsAndFactoriesMap = new HashMap<>();
    for (final Territory t : enemyCapitalsAndFactories) {

      // Get factory production if factory
      int factoryProduction = 0;
      if (ProMatches.territoryHasInfraFactoryAndIsLand().test(t)) {
        factoryProduction = TerritoryAttachment.getProduction(t);
      }

      // Get player production if capital
      double playerProduction = 0;
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if ((ta != null) && ta.isCapital()) {
        playerProduction = ProUtils.getPlayerProduction(t.getOwner(), data);
      }

      // Calculate value
      final int isNeutral = t.getOwner().isNull() ? 1 : 0;
      final int landMassSize = 1 + data.getMap()
          .getNeighbors(t, 6, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data)).size();
      final double value = (((Math.sqrt(factoryProduction + Math.sqrt(playerProduction)) * 32) / (1 + (3 * isNeutral)))
          * landMassSize) / maxLandMassSize;
      enemyCapitalsAndFactoriesMap.put(t, value);
    }

    return enemyCapitalsAndFactoriesMap;
  }

  private static double findLandValue(final Territory t, final PlayerID player, final int maxLandMassSize,
      final Map<Territory, Double> enemyCapitalsAndFactoriesMap, final List<Territory> territoriesThatCantBeHeld,
      final List<Territory> territoriesToAttack) {

    if (territoriesThatCantBeHeld.contains(t)) {
      return 0.0;
    }

    // Determine value based on enemy factory land distance
    final List<Double> values = new ArrayList<>();
    final GameData data = ProData.getData();
    final Set<Territory> nearbyEnemyCapitalsAndFactories =
        findNearbyEnemyCapitalsAndFactories(t, enemyCapitalsAndFactoriesMap);
    for (final Territory enemyCapitalOrFactory : nearbyEnemyCapitalsAndFactories) {
      final int distance = data.getMap().getDistance(t, enemyCapitalOrFactory,
          ProMatches.territoryCanPotentiallyMoveLandUnits(player, data));
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
        data.getMap().getNeighbors(t, 2, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data));
    final List<Territory> nearbyEnemyTerritories = CollectionUtils.getMatches(nearbyTerritories,
        ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
    nearbyEnemyTerritories.removeAll(territoriesToAttack);
    for (final Territory nearbyEnemyTerritory : nearbyEnemyTerritories) {
      final int distance = data.getMap().getDistance(t, nearbyEnemyTerritory,
          ProMatches.territoryCanPotentiallyMoveLandUnits(player, data));
      if (distance > 0) {
        double value = TerritoryAttachment.getProduction(nearbyEnemyTerritory);
        if (nearbyEnemyTerritory.getOwner().isNull()) {
          value = findTerritoryAttackValue(player, nearbyEnemyTerritory) / 3; // find neutral value
        } else if (ProMatches.territoryIsAlliedLandAndHasNoEnemyNeighbors(player, data).test(nearbyEnemyTerritory)) {
          value *= 0.1; // reduce value for can't hold amphib allied territories
        }
        if (value > 0) {
          nearbyEnemyValue += (value / Math.pow(2, distance));
        }
      }
    }
    final int landMassSize = 1
        + data.getMap().getNeighbors(t, 6, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data)).size();
    double value = ((nearbyEnemyValue * landMassSize) / maxLandMassSize) + capitalOrFactoryValue;
    if (ProMatches.territoryHasInfraFactoryAndIsLand().test(t)) {
      value *= 1.1; // prefer territories with factories
    }

    return value;
  }

  private static double findWaterValue(final Territory t, final PlayerID player, final int maxLandMassSize,
      final Map<Territory, Double> enemyCapitalsAndFactoriesMap, final List<Territory> territoriesThatCantBeHeld,
      final List<Territory> territoriesToAttack, final Map<Territory, Double> territoryValueMap) {

    final GameData data = ProData.getData();
    if (territoriesThatCantBeHeld.contains(t) || data.getMap().getNeighbors(t, Matches.territoryIsWater()).isEmpty()) {
      return 0.0;
    }

    // Determine value based on enemy factory distance
    final List<Double> values = new ArrayList<>();
    final Set<Territory> nearbyEnemyCapitalsAndFactories =
        findNearbyEnemyCapitalsAndFactories(t, enemyCapitalsAndFactoriesMap);
    for (final Territory enemyCapitalOrFactory : nearbyEnemyCapitalsAndFactories) {
      final Route route = data.getMap().getRoute_IgnoreEnd(t, enemyCapitalOrFactory,
          ProMatches.territoryCanMoveSeaUnits(player, data, true));
      if ((route == null) || (MoveValidator.validateCanal(route, null, player, data) != null)) {
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
        CollectionUtils.getMatches(nearbyTerritories, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data));
    nearbyLandTerritories.removeAll(territoriesToAttack);
    for (final Territory nearbyLandTerritory : nearbyLandTerritories) {
      final Route route = data.getMap().getRoute_IgnoreEnd(t, nearbyLandTerritory,
          ProMatches.territoryCanMoveSeaUnits(player, data, true));
      if ((route == null) || (MoveValidator.validateCanal(route, null, player, data) != null)) {
        continue;
      }
      final int distance = route.numberOfSteps();
      if ((distance > 0) && (distance <= 3)) {
        if (ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld)
            .test(nearbyLandTerritory)) {
          double value = TerritoryAttachment.getProduction(nearbyLandTerritory);
          if (nearbyLandTerritory.getOwner().isNull()) {
            value = findTerritoryAttackValue(player, nearbyLandTerritory);
          }
          nearbyLandValue += value;
        }
        if (!territoryValueMap.containsKey(nearbyLandTerritory)) {
          final double value = findLandValue(nearbyLandTerritory, player, maxLandMassSize, enemyCapitalsAndFactoriesMap,
              territoriesThatCantBeHeld, territoriesToAttack);
          territoryValueMap.put(nearbyLandTerritory, value);
        }
        nearbyLandValue += territoryValueMap.get(nearbyLandTerritory);
      }
    }
    final double value = (capitalOrFactoryValue / 100) + (nearbyLandValue / 10);

    return value;
  }

  private static Set<Territory> findNearbyEnemyCapitalsAndFactories(final Territory t,
      final Map<Territory, Double> enemyCapitalsAndFactoriesMap) {

    Set<Territory> nearbyEnemyCapitalsAndFactories = new HashSet<>();
    for (int i = MIN_FACTORY_CHECK_DISTANCE; i <= MAX_FACTORY_CHECK_DISTANCE; i++) {
      nearbyEnemyCapitalsAndFactories = ProData.getData().getMap().getNeighbors(t, i);
      nearbyEnemyCapitalsAndFactories.retainAll(enemyCapitalsAndFactoriesMap.keySet());
      if (!nearbyEnemyCapitalsAndFactories.isEmpty()) {
        break;
      }
    }

    return nearbyEnemyCapitalsAndFactories;
  }

}
