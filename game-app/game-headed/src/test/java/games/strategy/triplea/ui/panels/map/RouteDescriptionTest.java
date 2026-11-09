package games.strategy.triplea.ui.panels.map;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import java.awt.Image;
import java.awt.Point;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

final class RouteDescriptionTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class EqualsTest {
    private final Route route = new Route(Mockito.mock(Territory.class));
    private final Point start = new Point();
    private final Point end = new Point();
    @Mock private Image image;

    @Test
    void shouldReturnFalseWhenOtherIsNotInstanceOfRouteDescription() {
      final RouteDescription reference = new RouteDescription(route, start, end, image);

      assertThat(reference.equals(new Object())).isFalse();
    }

    @Test
    void shouldReturnFalseWhenReferenceRouteIsNullAndOtherRouteIsNotNull() {
      final RouteDescription reference = new RouteDescription(null, start, end, image);
      final RouteDescription other = new RouteDescription(route, start, end, image);

      assertThat(reference.equals(other)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenReferenceRouteIsNotNullAndOtherRouteIsNull() {
      final RouteDescription reference = new RouteDescription(route, start, end, image);
      final RouteDescription other = new RouteDescription(null, start, end, image);

      assertThat(reference.equals(other)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenReferenceRouteIsNotEqualToOtherRoute() {
      final RouteDescription reference = new RouteDescription(route, start, end, image);
      final Route otherRoute = new Route(new Territory("territoryName", new GameData()));
      final RouteDescription other = new RouteDescription(otherRoute, start, end, image);

      assertThat(reference.equals(other)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenReferenceStartIsNullAndOtherStartIsNotNull() {
      final RouteDescription reference = new RouteDescription(route, null, end, image);
      final RouteDescription other = new RouteDescription(route, start, end, image);

      assertThat(reference.equals(other)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenReferenceStartIsNotNullAndOtherStartIsNull() {
      final RouteDescription reference = new RouteDescription(route, start, end, image);
      final RouteDescription other = new RouteDescription(route, null, end, image);

      assertThat(reference.equals(other)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenReferenceStartIsNotEqualToOtherStart() {
      final RouteDescription reference = new RouteDescription(route, start, end, image);
      final Point otherStart = new Point(start.x + 1, start.y);
      final RouteDescription other = new RouteDescription(route, otherStart, end, image);

      assertThat(reference.equals(other)).isFalse();
    }
  }
}
