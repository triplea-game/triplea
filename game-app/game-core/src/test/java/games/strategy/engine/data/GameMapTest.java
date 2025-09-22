package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

public class GameMapTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final Territory caucasus = gameData.getMap().getTerritoryOrNull("Caucasus");
  private final Territory germany = gameData.getMap().getTerritoryOrNull("Germany");
  private final Territory russia = gameData.getMap().getTerritoryOrNull("Russia");
  private final Territory uk = gameData.getMap().getTerritoryOrNull("United Kingdom");

  private int getLandDistance(Territory from, Territory to) {
    return gameData.getMap().getLandDistance(from, to);
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
    assertThat(getLandDistance(caucasus, caucasus), is(0));
  }
}
