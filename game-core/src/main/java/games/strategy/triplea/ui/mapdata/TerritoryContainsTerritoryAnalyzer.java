package games.strategy.triplea.ui.mapdata;

import games.strategy.ui.Util;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
class TerritoryContainsTerritoryAnalyzer {
  private void initializeContains() {
    for (final String seaTerritory : getTerritories()) {
      if (!Util.isTerritoryNameIndicatingWater(seaTerritory)) {
        continue;
      }
      final List<String> contained = new ArrayList<>();
      for (final String landTerritory : getTerritories()) {
        if (Util.isTerritoryNameIndicatingWater(landTerritory)) {
          continue;
        }
        final Polygon landPoly = getPolygons(landTerritory).iterator().next();
        final Polygon seaPoly = getPolygons(seaTerritory).iterator().next();
        if (seaPoly.contains(landPoly.getBounds())) {
          contained.add(landTerritory);
        }
      }
      if (!contained.isEmpty()) {
        contains.put(seaTerritory, contained);
      }
    }
  }
}
