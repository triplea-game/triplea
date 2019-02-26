package games.strategy.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ImageScrollerLargeViewTest {
  @Nested
  final class DiscretizeScaleTest {
    @Test
    void shouldMapScaleToNearestDiscreteValueThatIsMultipleOfInverseOfTileSize() {
      final int tileSize = 256;
      final double error = 1.0E-9;

      assertThat(ImageScrollerLargeView.discretizeScale(1.0, tileSize), is(closeTo(1.0, error)));
      assertThat(ImageScrollerLargeView.discretizeScale(0.71, tileSize), is(closeTo(0.70703125, error)));
      assertThat(ImageScrollerLargeView.discretizeScale(0.5, tileSize), is(closeTo(0.5, error)));
      assertThat(ImageScrollerLargeView.discretizeScale(0.36, tileSize), is(closeTo(0.359375, error)));
      assertThat(ImageScrollerLargeView.discretizeScale(0.15, tileSize), is(closeTo(0.1484375, error)));
    }
  }
}
