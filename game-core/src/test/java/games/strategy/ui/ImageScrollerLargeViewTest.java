package games.strategy.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ImageScrollerLargeViewTest {
  @Nested
  final class NormalizeScaleTest {
    static final double ERROR = 1.0E-9;

    @Test
    void shouldDiscretizeEachUnitInto256Values() {
      assertThat(ImageScrollerLargeView.normalizeScale(1.0), is(closeTo(1.0, ERROR)));
      assertThat(ImageScrollerLargeView.normalizeScale(0.71), is(closeTo(0.70703125, ERROR)));
      assertThat(ImageScrollerLargeView.normalizeScale(0.5), is(closeTo(0.5, ERROR)));
      assertThat(ImageScrollerLargeView.normalizeScale(0.36), is(closeTo(0.359375, ERROR)));
      assertThat(ImageScrollerLargeView.normalizeScale(0.15), is(closeTo(0.1484375, ERROR)));
    }

    @Test
    void shouldConstrainValuesGreaterThanMaximumScale() {
      assertThat(ImageScrollerLargeView.normalizeScale(2.0), is(closeTo(1.0, ERROR)));
    }

    @Test
    void shouldConstrainValuesLessThanMinimumScale() {
      assertThat(ImageScrollerLargeView.normalizeScale(0.0), is(closeTo(0.1484375, ERROR)));
    }
  }
}
