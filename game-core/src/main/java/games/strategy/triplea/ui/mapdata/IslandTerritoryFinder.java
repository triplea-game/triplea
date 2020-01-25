package games.strategy.triplea.ui.mapdata;

import games.strategy.ui.Util;
import java.awt.Polygon;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
class IslandTerritoryFinder {

  /**
   * Finds all island territories given a set of territory names and their corresponding polygons.
   * An island is defined as a land territory that is totally contained within a single sea
   * territory.
   *
   * <p>Note: Sea territories can overlap, an land territory may be an island for multiple sea
   * territories.
   *
   * <p>Warning: territories may consist of multiple polygons, only the first polygon is checked.
   *
   * @param polygons Mapping of territory names to the polygons representing that territories.
   * @return A mapping of sea territory names to the name of land territories that are islands
   *     contained by each corresponding sea territory.
   */
  static Map<String, Set<String>> findIslands(final Map<String, List<Polygon>> polygons) {
    final Set<String> seaTerritories = filterSeaTerritories(polygons.keySet());
    final Set<String> landTerritories = filterNotSeaTerritories(polygons.keySet());

    // map sea territories to: sea territory name -> islands contained by that sea territory
    return seaTerritories.stream()
        .collect(
            Collectors.toMap(sea -> sea, findIslandsForSeaTerritory(landTerritories, polygons)));
  }

  private static Set<String> filterSeaTerritories(final Set<String> territoryNames) {
    return filterTerritories(territoryNames, Util::isTerritoryNameIndicatingWater);
  }

  private static Set<String> filterNotSeaTerritories(final Set<String> territoryNames) {
    return filterTerritories(territoryNames, Predicate.not(Util::isTerritoryNameIndicatingWater));
  }

  /** Returns a subset of territories matching a given filter. */
  private static Set<String> filterTerritories(
      final Set<String> territoryNames, final Predicate<String> territoryFilter) {
    return territoryNames.stream().filter(territoryFilter).collect(Collectors.toSet());
  }

  /** Find all land territories contained by a given sea territory. */
  private static Function<String, Set<String>> findIslandsForSeaTerritory(
      final Set<String> landTerritories, final Map<String, List<Polygon>> polygons) {

    return seaTerritory ->
        landTerritories.stream()
            .filter(landIsContainedBySeaTerritory(seaTerritory, polygons))
            .collect(Collectors.toSet());
  }

  /** Checks that a given land territory would be contained by a given sea territory. */
  private Predicate<String> landIsContainedBySeaTerritory(
      final String seaTerritory, final Map<String, List<Polygon>> polygons) {

    // Function where given a territory name, give us a polygon for that territory
    final Function<String, Polygon> polygonLookup =
        territoryName -> polygons.get(territoryName).iterator().next();

    return land -> {
      final Polygon seaPoly = polygonLookup.apply(seaTerritory);
      final Polygon landPoly = polygonLookup.apply(land);
      return seaPoly.contains(landPoly.getBounds());
    };
  }
}
