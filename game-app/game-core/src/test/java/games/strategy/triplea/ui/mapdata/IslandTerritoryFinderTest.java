package games.strategy.triplea.ui.mapdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.hamcrest.core.Is.is;

import java.awt.Polygon;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.util.ToolsUtil;

final class IslandTerritoryFinderTest {

  private static final String SEA_TERR = ToolsUtil.TERRITORY_SEA_ZONE_INFIX;
  private static final String SEA_TERR_1 = ToolsUtil.TERRITORY_SEA_ZONE_INFIX + " 1";
  private static final String LAND_TERR = "Land";
  private static final String LAND_TERR_1 = "Land 1";

  private final Polygon inner = new Polygon();
  private final Polygon adjacentInner = new Polygon();
  private final Polygon outer = new Polygon();
  private final Polygon adjacentContainingRectange = new Polygon();

  @BeforeEach
  void setUp() {
    // inner is a 1x1 square
    inner.addPoint(1, 1);
    inner.addPoint(1, 2);
    inner.addPoint(2, 2);
    inner.addPoint(2, 1);

    // adjacent-inner is a 1x1 square
    adjacentInner.addPoint(2, 2);
    adjacentInner.addPoint(2, 3);
    adjacentInner.addPoint(3, 3);
    adjacentInner.addPoint(3, 2);

    // outer is a 3x3 square containing inner and adjacent-inner
    outer.addPoint(0, 0);
    outer.addPoint(0, 3);
    outer.addPoint(3, 3);
    outer.addPoint(3, 0);

    // 1x2 rectangle containing 'adjacent inner'
    adjacentContainingRectange.addPoint(2, 2);
    adjacentContainingRectange.addPoint(2, 3);
    adjacentContainingRectange.addPoint(4, 3);
    adjacentContainingRectange.addPoint(4, 2);
  }

  @Test
  @DisplayName("Basic case with a single island contained by a sea territory")
  void simpleFindIsland() {
    final Map<String, Set<String>> seaToIslands =
        IslandTerritoryFinder.findIslands(
            Map.of(SEA_TERR, List.of(outer), LAND_TERR, List.of(inner)));

    assertThat(seaToIslands, is(aMapWithSize(1)));
    assertThat(seaToIslands, hasEntry(SEA_TERR, Set.of(LAND_TERR)));

    // inversion of land and sea should yield an empty map
    final Map<String, Set<String>> inversion =
        IslandTerritoryFinder.findIslands(
            Map.of(SEA_TERR, List.of(inner), LAND_TERR, List.of(outer)));

    assertThat(inversion, is(aMapWithSize(1)));
    assertThat(inversion, hasEntry(SEA_TERR, Set.of()));
  }

  @Test
  @DisplayName("If island coordinates match the sea coordinates, then the island is contained")
  void overlapTerritoriesAreIslands() {
    final Map<String, Set<String>> seaToIslands =
        IslandTerritoryFinder.findIslands(
            Map.of(SEA_TERR, List.of(inner), LAND_TERR, List.of(inner)));

    assertThat(seaToIslands, is(aMapWithSize(1)));
    assertThat(seaToIslands, hasEntry(SEA_TERR, Set.of(LAND_TERR)));
  }

  @Test
  @DisplayName("Sea containing two islands")
  void seaContainingTwoIslands() {
    final Map<String, Set<String>> seaToIslands =
        IslandTerritoryFinder.findIslands(
            Map.of(
                SEA_TERR,
                List.of(outer),
                LAND_TERR,
                List.of(inner),
                LAND_TERR_1,
                List.of(adjacentInner)));

    assertThat(seaToIslands, is(aMapWithSize(1)));
    assertThat(seaToIslands, hasEntry(SEA_TERR, Set.of(LAND_TERR, LAND_TERR_1)));
  }

  @Test
  @DisplayName("Two sea territories containing the same island")
  void twoSeaTerritoriesContainingTheSameIsland() {
    final Map<String, Set<String>> seaToIslands =
        IslandTerritoryFinder.findIslands(
            Map.of(
                SEA_TERR,
                List.of(outer),
                SEA_TERR_1,
                List.of(adjacentContainingRectange),
                LAND_TERR,
                List.of(adjacentInner)));

    assertThat(seaToIslands, is(aMapWithSize(2)));
    assertThat(seaToIslands, hasEntry(SEA_TERR, Set.of(LAND_TERR)));
    assertThat(seaToIslands, hasEntry(SEA_TERR_1, Set.of(LAND_TERR)));
  }

  @Test
  @DisplayName("No Overlap Case")
  void noOverlap() {
    final Map<String, Set<String>> seaToIslands =
        IslandTerritoryFinder.findIslands(
            Map.of(SEA_TERR, List.of(inner), LAND_TERR, List.of(adjacentInner)));

    assertThat(seaToIslands, is(aMapWithSize(1)));
    assertThat(seaToIslands, hasEntry(SEA_TERR, Set.of()));
  }

  @Test
  @DisplayName("Sea territory overlapping only one island")
  void onlyOneIslandIsOverlapped() {
    final Map<String, Set<String>> seaToIslands =
        IslandTerritoryFinder.findIslands(
            Map.of(
                SEA_TERR,
                List.of(adjacentContainingRectange),
                LAND_TERR,
                List.of(adjacentInner),
                LAND_TERR_1,
                List.of(inner)));

    assertThat(seaToIslands, is(aMapWithSize(1)));
    assertThat(seaToIslands, hasEntry(SEA_TERR, Set.of(LAND_TERR)));
  }
}
