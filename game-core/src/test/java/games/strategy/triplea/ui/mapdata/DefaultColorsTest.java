package games.strategy.triplea.ui.mapdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

final class DefaultColorsTest {
  private final DefaultColors defaultColors = new DefaultColors();

  @Test
  void nextColor_ShouldReturnNextColorWhenColorsAvailable() {
    assertThat(defaultColors.nextColor(), is(DefaultColors.COLORS.get(0)));
  }

  @Test
  void nextColor_ShouldThrowExceptionWhenNoColorsAvailable() {
    assertThrows(
        NoSuchElementException.class,
        () ->
            IntStream.range(0, DefaultColors.COLORS.size() + 1)
                .forEach(i -> defaultColors.nextColor()));
  }
}
