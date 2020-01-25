package games.strategy.triplea.ui.mapdata;

import games.strategy.ui.Util;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
class TerritoryContainsTerritoryAnalyzer {

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
  static Map<String, List<String>> findIslands(final Map<String, List<Polygon>> polygons) {
    final Set<String> territoryNames = polygons.keySet();
    final Function<String, Polygon> polygonLookup =
        territoryName -> polygons.get(territoryName).iterator().next();

    final Map<String, List<String>> contains = new HashMap<>();

    for (final String seaTerritory : territoryNames) {
      if (!Util.isTerritoryNameIndicatingWater(seaTerritory)) {
        continue;
      }
      final List<String> contained = new ArrayList<>();
      for (final String landTerritory : territoryNames) {
        if (Util.isTerritoryNameIndicatingWater(landTerritory)) {
          continue;
        }
        final Polygon landPoly = polygonLookup.apply(landTerritory);
        final Polygon seaPoly = polygonLookup.apply(seaTerritory);
        if (seaPoly.contains(landPoly.getBounds())) {
          contained.add(landTerritory);
        }
      }
      if (!contained.isEmpty()) {
        contains.put(seaTerritory, contained);
      }
    }
    return contains;
  }
}
