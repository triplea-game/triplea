package games.strategy.triplea.ui.mapdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public final class DefaultColorsTest {
  private final DefaultColors defaultColors = new DefaultColors();

  @Test
  public void nextColor_ShouldReturnNextColorWhenColorsAvailable() {
    assertThat(defaultColors.nextColor(), is(notNullValue()));
  }

  @Test
  public void nextColor_ShouldThrowExceptionWhenNoColorsAvailable() {
    assertThrows(
        NoSuchElementException.class,
        () -> IntStream.iterate(0, IntUnaryOperator.identity()).forEach(i -> defaultColors.nextColor()));
  }
}
