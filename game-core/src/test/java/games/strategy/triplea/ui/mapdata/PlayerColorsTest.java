package games.strategy.triplea.ui.mapdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

final class PlayerColorsTest {
  private final PlayerColors playerColors = new PlayerColors();

  @Test
  void nextColor_ShouldReturnNextColorWhenColorsAvailable() {
    assertThat(playerColors.nextColor(), is(PlayerColors.COLORS.get(0)));
  }

  @Test
  void nextColor_ShouldThrowExceptionWhenNoColorsAvailable() {
    assertThrows(
        NoSuchElementException.class,
        () ->
            IntStream.range(0, PlayerColors.COLORS.size() + 1)
                .forEach(i -> playerColors.nextColor()));
  }
}
