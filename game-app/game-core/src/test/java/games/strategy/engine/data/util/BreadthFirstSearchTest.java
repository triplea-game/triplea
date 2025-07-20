package games.strategy.engine.data.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

public class BreadthFirstSearchTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final Territory caucasus = gameData.getMap().getTerritoryOrThrow("Caucasus");
  private final Territory germany = gameData.getMap().getTerritoryOrThrow("Germany");
  private final Territory russia = gameData.getMap().getTerritoryOrThrow("Russia");
  private final Territory uk = gameData.getMap().getTerritoryOrThrow("United Kingdom");

  private int getLandDistance(Territory from, Territory to) {
    var territoryFinder = new BreadthFirstSearch.TerritoryFinder(to);
    new BreadthFirstSearch(from, Matches.territoryIsLand()).traverse(territoryFinder);
    return territoryFinder.getDistanceFound();
  }

  @Test
  void testLandDistance() {
    assertThat(getLandDistance(caucasus, russia), is(1));
    assertThat(getLandDistance(caucasus, germany), is(3));
  }

  @Test
  void testLandDistanceNotFound() {
    assertThat(getLandDistance(caucasus, uk), is(-1));
  }

  @Test
  void testLandDistanceSameTerritory() {
    // Note: This is testing the limitation described in the API doc.
    // This is a test for the low-level helper class, but the high level API, which is tested by
    // GameMapTest.testLandDistanceSameTerritory() returns the expected result of 0.
    assertThat(getLandDistance(caucasus, caucasus), is(-1));
  }
}
