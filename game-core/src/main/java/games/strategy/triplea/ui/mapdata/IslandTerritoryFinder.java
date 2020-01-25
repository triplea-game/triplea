package games.strategy.triplea.ui.mapdata;

import games.strategy.ui.Util;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.HashMap;
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
   * An island is defined as a land territory that is contained within a sea territory.
   *
   * <p>Warning: territories may consist of multiple polygons, only the first polygon is checked.
   *
   * @param polygons Mapping of territory names to the polygons representing that territories.
   * @return A mapping of sea territory names to the name of land territories that are islands
   *     contained by each corresponding sea territory.
   */
  static Map<String, Set<String>> findIslands(final Map<String, List<Polygon>> polygons) {
    final Set<String> seaTerritories =
        filterTerritories(polygons.keySet(), Util::isTerritoryNameIndicatingWater);

    final Set<String> landTerritories =
        filterTerritories(polygons.keySet(), Predicate.not(Util::isTerritoryNameIndicatingWater));

    final Function<String, Polygon> polygonLookup =
        territoryName -> polygons.get(territoryName).iterator().next();

    final Map<String, Set<String>> contains = new HashMap<>();

    for (final String seaTerritory : seaTerritories) {
      final Set<String> islands =
          findIslandsForSeaTerritory(seaTerritory, landTerritories, polygonLookup);

      if (!islands.isEmpty()) {
        contains.put(seaTerritory, islands);
      }
    }
    return contains;
  }

  /** Returns a subset of territories matching a given filter. */
  private static Set<String> filterTerritories(
      final Set<String> territoryNames, final Predicate<String> territoryFilter) {
    return territoryNames.stream().filter(territoryFilter).collect(Collectors.toSet());
  }

  /** Find all land territories contained by a given sea territory. */
  private static Set<String> findIslandsForSeaTerritory(
      final String seaTerritory,
      final Set<String> landTerritories,
      final Function<String, Polygon> polygonLookup) {

    final Polygon seaPoly = polygonLookup.apply(seaTerritory);

    return landTerritories.stream()
        .filter(isLandContainedBySeaPolygon(seaPoly, polygonLookup))
        .collect(Collectors.toSet());
  }

  /** Checks that a given land territory would be contained by a given sea polygon. */
  private Predicate<String> isLandContainedBySeaPolygon(
      final Polygon seaPoly, final Function<String, Polygon> polygonLookup) {
    return land -> {
      final Rectangle landBounds = polygonLookup.apply(land).getBounds();
      return seaPoly.contains(landBounds);
    };
  }
}
