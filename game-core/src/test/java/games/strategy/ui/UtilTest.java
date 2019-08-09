package games.strategy.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.awt.Polygon;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class UtilTest {
  @Nested
  final class TranslatePolygonTest {
    private final Polygon polygon = new Polygon(new int[] {1, 2, 3}, new int[] {4, 5, 6}, 3);

    @Test
    void shouldTranslatePolygonBySpecifiedDisplacement() {
      final Polygon translatedPolygon = Util.translatePolygon(polygon, 2, -5);

      assertThat(translatedPolygon.xpoints, is(new int[] {3, 4, 5}));
      assertThat(translatedPolygon.ypoints, is(new int[] {-1, 0, 1}));
      assertThat(translatedPolygon.npoints, is(3));
    }

    @Test
    void shouldNotModifyOriginalPolygon() {
      Util.translatePolygon(polygon, 2, -5);

      assertThat(polygon.xpoints, is(new int[] {1, 2, 3}));
      assertThat(polygon.ypoints, is(new int[] {4, 5, 6}));
      assertThat(polygon.npoints, is(3));
    }
  }
}
