package games.strategy.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Polygon;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class UtilTest {

  /**
   * @param run Runnable to be executed
   * @throws Throwable target exception raised in the Runnable executed in Event Dispatch Thread
   */
  static void runInEvenDispatchThreadAndRethrow(final Runnable run) throws Throwable {
    try {
      SwingUtilities.invokeAndWait(run);
    } catch (final InvocationTargetException e) {
      throw e.getTargetException();
    }
  }

  @Test
  void ensureOnEventDispatchThread() {
    assertDoesNotThrow(() -> runInEvenDispatchThreadAndRethrow(Util::ensureOnEventDispatchThread));
    assertThrows(IllegalStateException.class, Util::ensureOnEventDispatchThread); // negative test
  }

  @Test
  void ensureNotOnEventDispatchThread() {
    assertAll(Util::ensureNotOnEventDispatchThread); // positive test
    assertThrows(
        IllegalStateException.class,
        () -> runInEvenDispatchThreadAndRethrow(Util::ensureNotOnEventDispatchThread));
  }

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
