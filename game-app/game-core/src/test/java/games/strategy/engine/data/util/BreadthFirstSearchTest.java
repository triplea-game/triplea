package games.strategy.engine.data.util;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

public class BreadthFirstSearchTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final Territory caucasus = gameData.getMap().getTerritoryOrNull("Caucasus");
  private final Territory germany = gameData.getMap().getTerritoryOrNull("Germany");
  private final Territory russia = gameData.getMap().getTerritoryOrNull("Russia");
  private final Territory uk = gameData.getMap().getTerritoryOrNull("United Kingdom");

  private int getLandDistance(Territory from, Territory to) {
    var territoryFinder = BreadthFirstSearch.createTerritoryFinder(to);
    new BreadthFirstSearch(from, Matches.territoryIsLand()).traverse(territoryFinder);
    return territoryFinder.getDistanceFound();
  }

  @Test
  void testLandDistance() {
    assertThat(getLandDistance(caucasus, russia)).isEqualTo(1);
    assertThat(getLandDistance(caucasus, germany)).isEqualTo(3);
  }

  @Test
  void testLandDistanceNotFound() {
    assertThat(getLandDistance(caucasus, uk)).isEqualTo(-1);
  }

  @Test
  void testLandDistanceSameTerritory() {
    // Note: This is testing the limitation described in the API doc.
    // This is a test for the low-level helper class, but the high level API, which is tested by
    // GameMapTest.testLandDistanceSameTerritory() returns the expected result of 0.
    assertThat(getLandDistance(caucasus, caucasus)).isEqualTo(-1);
  }
}
