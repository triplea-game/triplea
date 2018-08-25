package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.awt.Image;
import java.awt.Point;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.Route;

final class RouteDescriptionTest {
  @Nested
  final class EqualsTest {
    private final RouteDescription reference = new RouteDescription(
        new Route(),
        new Point(),
        new Point(),
        mock(Image.class));

    @Test
    void shouldReturnFalseWhenOtherIsNotInstanceOfRouteDescription() {
      assertThat(reference.equals(new Object()), is(false));
    }
  }
}
