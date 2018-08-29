package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collections;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class GameMapTest {
  private final GameData gameData = new GameData();
  private final GameMap gameMap = new GameMap(gameData);

  @Nested
  final class GetCompositeRouteTest {
    @Test
    void shouldReturnRouteToSelfWhenStartEqualsEnd() {
      final Territory start = new Territory("territory", gameData);
      final Territory end = new Territory("territory", gameData);

      assertThat(gameMap.getCompositeRoute(start, end, Collections.emptyMap()), is(new Route(start)));
    }
  }

  @Nested
  final class GetRouteTest {
    @Test
    void shouldReturnRouteToSelfWhenStartEqualsEnd() {
      final Territory start = new Territory("territory", gameData);
      final Territory end = new Territory("territory", gameData);

      assertThat(gameMap.getRoute(start, end), is(new Route(start)));
    }
  }
}
