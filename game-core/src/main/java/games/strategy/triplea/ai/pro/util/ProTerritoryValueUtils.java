package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.util.BreadthFirstSearch;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;

/** Pro AI battle utilities. */
public final class ProTerritoryValueUtils {

  protected static final int MIN_FACTORY_CHECK_DISTANCE = 9;

  private ProTerritoryValueUtils() {}

  /**
   * Returns the relative value of attacking the specified territory compared to other territories.
   */
  public static double findTerritoryAttackValue(
      final ProData proData, final GamePlayer player, final Territory t) {
    final GameData data = proData.getData();
    final int isEnemyFactory =
        ProMatches.territoryHasInfraFactoryAndIsEnemyLand(player, data).test(t) ? 1 : 0;
    double value = 3.0 * TerritoryAttachment.getProduction(t) * (isEnemyFactory + 1);
    if (ProUtils.isNeutralLand(t)) {
      final double strength =
          ProBattleUtils.estimateStrength(
              t, new ArrayList<>(t.getUnits()), new ArrayList<>(), false);

      // Estimate TUV swing as number of casualties * cost
      final double tuvSwing = -(strength / 8) * proData.getMinCostPerHitPoint();
      value += tuvSwing;
    }

    return value;
  }

  /** Returns the value of each territory in {@code territoriesToCheck}. */
  public static Map<Territory, Double> findTerritoryValues(
      final ProData proData,
      final GamePlayer player,
      final List<Territory> territoriesThatCantBeHeld,
      final List<Territory> territoriesToAttack,
      final Set<Territory> territoriesToCheck) {

    final int maxLandMassSize = findMaxLandMassSize(player);
    final Map<Territory, Double> enemyCapitalsAndFactoriesMap =
        findEnemyCapitalsAndFactoriesValue(
            player, maxLandMassSize, territoriesThatCantBeHeld, territoriesToAttack);

    final Map<Territory, Double> territoryValueMap = new HashMap<>();
    for (final Territory t : territoriesToCheck) {
      if (!t.isWater()) {
        final double value =
            findLandValue(
                proData,
                t,
                player,
                maxLandMassSize,
                enemyCapitalsAndFactoriesMap,
                territoriesThatCantBeHeld,
                territoriesToAttack);
        territoryValueMap.put(t, value);
      }
    }

    for (final Territory t : territoriesToCheck) {
      if (t.isWater()) {
        final double value =
            findWaterValue(
                proData,
                t,
                player,
                maxLandMassSize,
                enemyCapitalsAndFactoriesMap,
                territoriesThatCantBeHeld,
                territoriesToAttack,
                territoryValueMap);
        territoryValueMap.put(t, value);
      }
    }

    return territoryValueMap;
  }

  /** Returns the value of each sea territory in {@link ProData#getData()}. */
  public static Map<Territory, Double> findSeaTerritoryValues(
      final GamePlayer player,
      final List<Territory> territoriesThatCantBeHeld,
      final List<Territory> territoriesToCheck) {

    // Determine value for water territories
    final Map<Territory, Double> territoryValueMap = new HashMap<>();
    final GameData data = player.getData();
    for (final Territory t : territoriesToCheck) {
      if (!territoriesThatCantBeHeld.contains(t)
          && t.isWater()
          && !data.getMap().getNeighbors(t, Matches.territoryIsWater()).isEmpty()) {

        // Determine sea value based on nearby convoy production
        double nearbySeaProductionValue = 0;
        final Set<Territory> nearbySeaTerritories =
            data.getMap()
                .getNeighbors(t, 4, ProMatches.territoryCanMoveSeaUnits(player, data, true));
        final List<Territory> nearbyEnemySeaTerritories =
            CollectionUtils.getMatches(
                nearbySeaTerritories,
                ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
        for (final Territory nearbyEnemySeaTerritory : nearbyEnemySeaTerritories) {
          final Route route =
              data.getMap()
                  .getRouteForUnits(
                      t,
                      nearbyEnemySeaTerritory,
                      ProMatches.territoryCanMoveSeaUnits(player, data, true),
                      Set.of(),
                      player);
          if (route == null) {
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
            CollectionUtils.getMatches(
                nearbySeaTerritories, Matches.territoryHasEnemyUnits(player, data));
        for (final Territory nearbyEnemySeaTerritory : nearbyEnemySeaUnitTerritories) {
          final Route route =
              data.getMap()
                  .getRouteForUnits(
                      t,
                      nearbyEnemySeaTerritory,
                      ProMatches.territoryCanMoveSeaUnits(player, data, true),
                      Set.of(),
                      player);
          if (route == null) {
            continue;
          }
          final int distance = route.numberOfSteps();
          if (distance > 0) {
            nearbyEnemySeaUnitValue +=
                nearbyEnemySeaTerritory
                        .getUnitCollection()
                        .countMatches(Matches.unitIsEnemyOf(data, player))
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

  protected static int findMaxLandMassSize(final GamePlayer player) {
    final GameData data = player.getData();
    final Predicate<Territory> cond =
        ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties());

    final var visited = new HashSet<Territory>();

    int maxLandMassSize = 1;
    for (final Territory t : data.getMap().getTerritories()) {
      if (!t.isWater() && !visited.contains(t)) {
        visited.add(t);
        final int[] landMassSize = new int[1];
        new BreadthFirstSearch(t, cond)
            .traverse(
                new BreadthFirstSearch.Visitor() {
                  @Override
                  public void visit(final Territory territory) {
                    visited.add(territory);
                    landMassSize[0]++;
                  }
                });
        if (landMassSize[0] > maxLandMassSize) {
          maxLandMassSize = landMassSize[0];
        }
      }
    }

    return maxLandMassSize;
  }

  private static Map<Territory, Double> findEnemyCapitalsAndFactoriesValue(
      final GamePlayer player,
      final int maxLandMassSize,
      final List<Territory> territoriesThatCantBeHeld,
      final List<Territory> territoriesToAttack) {

    // Get all enemy factories and capitals (check if most territories have factories and if so
    // remove them)
    final GameData data = player.getData();
    final List<Territory> allTerritories = data.getMap().getTerritories();
    final Set<Territory> enemyCapitalsAndFactories =
        new HashSet<>(
            CollectionUtils.getMatches(
                allTerritories,
                ProMatches.territoryHasInfraFactoryAndIsOwnedByPlayersOrCantBeHeld(
                    player, ProUtils.getPotentialEnemyPlayers(player), territoriesThatCantBeHeld)));
    final int numPotentialEnemyTerritories =
        CollectionUtils.countMatches(
            allTerritories, Matches.isTerritoryOwnedBy(ProUtils.getPotentialEnemyPlayers(player)));
    if (enemyCapitalsAndFactories.size() * 2 >= numPotentialEnemyTerritories) {
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
      if (ta != null && ta.isCapital()) {
        playerProduction = ProUtils.getPlayerProduction(t.getOwner(), data);
      }

      // Calculate value
      final int isNeutral = ProUtils.isNeutralLand(t) ? 1 : 0;
      final int landMassSize =
          1
              + data.getMap()
                  .getNeighbors(
                      t,
                      6,
                      ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()))
                  .size();
      final double value =
          Math.sqrt(factoryProduction + Math.sqrt(playerProduction))
              * 32
              / (1 + 3.0 * isNeutral)
              * landMassSize
              / maxLandMassSize;
      enemyCapitalsAndFactoriesMap.put(t, value);
    }

    return enemyCapitalsAndFactoriesMap;
  }

  private static double findLandValue(
      final ProData proData,
      final Territory t,
      final GamePlayer player,
      final int maxLandMassSize,
      final Map<Territory, Double> enemyCapitalsAndFactoriesMap,
      final List<Territory> territoriesThatCantBeHeld,
      final List<Territory> territoriesToAttack) {

    if (territoriesThatCantBeHeld.contains(t)) {
      return 0.0;
    }

    // Determine value based on enemy factory land distance
    final List<Double> values = new ArrayList<>();
    final GameData data = proData.getData();
    final Collection<Territory> nearbyEnemyCapitalsAndFactories =
        findNearbyEnemyCapitalsAndFactories(t, enemyCapitalsAndFactoriesMap.keySet());
    for (final Territory enemyCapitalOrFactory : nearbyEnemyCapitalsAndFactories) {
      final int distance =
          data.getMap()
              .getDistance(
                  t,
                  enemyCapitalOrFactory,
                  ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()),
                  ProMatches.noCanalsBetweenTerritories(player, data));
      if (distance > 0) {
        values.add(enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(2, distance));
      }
    }
    values.sort(Collections.reverseOrder());
    double capitalOrFactoryValue = 0;
    for (int i = 0; i < values.size(); i++) {
      capitalOrFactoryValue +=
          values.get(i) / Math.pow(2, i); // Decrease each additional factory value by half
    }

    // Determine value based on nearby territory production
    double nearbyEnemyValue = 0;
    final Set<Territory> nearbyTerritories =
        data.getMap()
            .getNeighbors(
                t,
                2,
                ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()),
                ProMatches.noCanalsBetweenTerritories(player, data));
    final List<Territory> nearbyEnemyTerritories =
        CollectionUtils.getMatches(
            nearbyTerritories,
            ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
    nearbyEnemyTerritories.removeAll(territoriesToAttack);
    for (final Territory nearbyEnemyTerritory : nearbyEnemyTerritories) {
      final int distance =
          data.getMap()
              .getDistance(
                  t,
                  nearbyEnemyTerritory,
                  ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()),
                  ProMatches.noCanalsBetweenTerritories(player, data));
      if (distance > 0) {
        double value = TerritoryAttachment.getProduction(nearbyEnemyTerritory);
        if (ProUtils.isNeutralLand(nearbyEnemyTerritory)) {
          // find neutral value
          value = findTerritoryAttackValue(proData, player, nearbyEnemyTerritory) / 3;
        } else if (ProMatches.territoryIsAlliedLandAndHasNoEnemyNeighbors(player, data)
            .test(nearbyEnemyTerritory)) {
          value *= 0.1; // reduce value for can't hold amphib allied territories
        }
        if (value > 0) {
          nearbyEnemyValue += (value / Math.pow(2, distance));
        }
      }
    }
    final int landMassSize =
        1
            + data.getMap()
                .getNeighbors(
                    t,
                    6,
                    ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()),
                    ProMatches.noCanalsBetweenTerritories(player, data))
                .size();
    double value = nearbyEnemyValue * landMassSize / maxLandMassSize + capitalOrFactoryValue;
    if (ProMatches.territoryHasInfraFactoryAndIsLand().test(t)) {
      value *= 1.1; // prefer territories with factories
    }

    return value;
  }

  private static double findWaterValue(
      final ProData proData,
      final Territory t,
      final GamePlayer player,
      final int maxLandMassSize,
      final Map<Territory, Double> enemyCapitalsAndFactoriesMap,
      final List<Territory> territoriesThatCantBeHeld,
      final List<Territory> territoriesToAttack,
      final Map<Territory, Double> territoryValueMap) {

    final GameData data = proData.getData();
    if (territoriesThatCantBeHeld.contains(t)
        || data.getMap().getNeighbors(t, Matches.territoryIsWater()).isEmpty()) {
      return 0.0;
    }

    // Determine value based on enemy factory distance
    final List<Double> values = new ArrayList<>();
    final Collection<Territory> nearbyEnemyCapitalsAndFactories =
        findNearbyEnemyCapitalsAndFactories(t, enemyCapitalsAndFactoriesMap.keySet());
    for (final Territory enemyCapitalOrFactory : nearbyEnemyCapitalsAndFactories) {
      final Route route =
          data.getMap()
              .getRouteForUnits(
                  t,
                  enemyCapitalOrFactory,
                  ProMatches.territoryCanMoveSeaUnits(player, data, true),
                  Set.of(),
                  player);
      if (route == null) {
        continue;
      }
      final int distance = route.numberOfSteps();
      if (distance > 0) {
        values.add(enemyCapitalsAndFactoriesMap.get(enemyCapitalOrFactory) / Math.pow(2, distance));
      }
    }
    values.sort(Collections.reverseOrder());
    double capitalOrFactoryValue = 0;
    for (int i = 0; i < values.size(); i++) {
      capitalOrFactoryValue +=
          values.get(i) / Math.pow(2, i); // Decrease each additional factory value by half
    }

    // Determine value based on nearby territory production
    double nearbyLandValue = 0;
    final Set<Territory> nearbyTerritories =
        data.getMap()
            .getNeighborsIgnoreEnd(t, 3, ProMatches.territoryCanMoveSeaUnits(player, data, true));
    final List<Territory> nearbyLandTerritories =
        CollectionUtils.getMatches(
            nearbyTerritories,
            ProMatches.territoryCanPotentiallyMoveLandUnits(player, data.getProperties()));
    nearbyLandTerritories.removeAll(territoriesToAttack);
    for (final Territory nearbyLandTerritory : nearbyLandTerritories) {
      final Route route =
          data.getMap()
              .getRouteForUnits(
                  t,
                  nearbyLandTerritory,
                  ProMatches.territoryCanMoveSeaUnits(player, data, true),
                  Set.of(),
                  player);
      if (route == null) {
        continue;
      }
      final int distance = route.numberOfSteps();
      if (distance > 0 && distance <= 3) {
        if (ProMatches.territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld)
            .test(nearbyLandTerritory)) {
          double value = TerritoryAttachment.getProduction(nearbyLandTerritory);
          if (ProUtils.isNeutralLand(nearbyLandTerritory)) {
            value = findTerritoryAttackValue(proData, player, nearbyLandTerritory);
          }
          nearbyLandValue += value;
        }
        if (!territoryValueMap.containsKey(nearbyLandTerritory)) {
          final double value =
              findLandValue(
                  proData,
                  nearbyLandTerritory,
                  player,
                  maxLandMassSize,
                  enemyCapitalsAndFactoriesMap,
                  territoriesThatCantBeHeld,
                  territoriesToAttack);
          territoryValueMap.put(nearbyLandTerritory, value);
        }
        nearbyLandValue += territoryValueMap.get(nearbyLandTerritory);
      }
    }

    return capitalOrFactoryValue / 100 + nearbyLandValue / 10;
  }

  /**
   * Finds enemy capitals / factors from a list that are "nearby" a given territory.
   *
   * <p>If any of the target territories exist within a distance of 9, returns the subset that do.
   * Otherwise, proceeds to check the territories at each subsequent distance until at least one
   * capital is found.
   *
   * <p>Note: This is an optimized version of a previous, much slower algorithm that has been
   * designed to keep the original behavior, but tuned for speed.
   *
   * @param startTerritory The territory from where to start the search.
   * @param enemyCapitalsAndFactories The territories to search for.
   * @return Subset of enemyCapitalsAndFactories that were found.
   */
  protected static Collection<Territory> findNearbyEnemyCapitalsAndFactories(
      final Territory startTerritory, final Set<Territory> enemyCapitalsAndFactories) {
    final var found = new HashSet<Territory>();
    new BreadthFirstSearch(startTerritory)
        .traverse(
            new BreadthFirstSearch.Visitor() {
              @Override
              public void visit(final Territory territory) {
                if (enemyCapitalsAndFactories.contains(territory)) {
                  found.add(territory);
                }
              }

              @Override
              public boolean shouldContinueSearch(final int distanceSearched) {
                return distanceSearched < MIN_FACTORY_CHECK_DISTANCE || found.isEmpty();
              }
            });
    return found;
  }
}
