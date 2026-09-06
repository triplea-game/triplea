package games.strategy.engine.data;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class RouteTest {
  @Nested
  final class EqualsTest {
    private final GameData gameData = new GameData();
    private final Territory territory1 = new Territory("territory1", gameData);
    private final Territory territory2 = new Territory("territory2", gameData);
    private final Route reference = new Route(territory1, territory2);

    @Test
    void shouldReturnTrueWhenOtherIsSameInstance() {
      assertThat(reference.equals(reference)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenOtherIsEqual() {
      final Route other = new Route(territory1, territory2);

      assertThat(reference.equals(other)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenOtherIsNull() {
      assertThat(reference.equals(null)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenOtherIsNotInstanceOfRoute() {
      assertThat(reference.equals(new Object())).isFalse();
    }

    @Test
    void shouldReturnFalseWhenOtherHasDifferentStepCount() {
      final Route other = new Route(territory1);

      assertThat(reference.equals(other)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenOtherHasSameStepCountButHasDifferentStartTerritory() {
      final Route other = new Route(territory2, territory1);

      assertThat(reference.equals(other)).isFalse();
    }

    @Test
    void
        shouldReturnFalseWhenOtherHasSameStepCountAndSameStartTerritoryButDifferentTerritoryList() {
      final Route other = new Route(territory1, new Territory("territory3", gameData));

      assertThat(reference.equals(other)).isFalse();
    }
  }
}
