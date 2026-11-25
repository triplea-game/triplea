package games.strategy.engine.framework.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class LaunchWindowSizeTest {
  private static final Dimension MINIMUM = new Dimension(900, 780);

  @Test
  void firstLaunchCoversMostOfTheScreen() {
    final Dimension size =
        LaunchWindowSize.compute(Optional.empty(), new Rectangle(0, 0, 2000, 1200), MINIMUM);

    assertThat(size).isEqualTo(new Dimension(1700, 1020));
  }

  @Test
  void firstLaunchOnSmallScreenIsNotSmallerThanMinimum() {
    final Dimension size =
        LaunchWindowSize.compute(Optional.empty(), new Rectangle(0, 0, 1000, 900), MINIMUM);

    assertThat(size).isEqualTo(new Dimension(900, 780));
  }

  @Test
  void minimumIsCappedToTinyScreen() {
    final Dimension size =
        LaunchWindowSize.compute(Optional.empty(), new Rectangle(0, 0, 800, 600), MINIMUM);

    assertThat(size).isEqualTo(new Dimension(800, 600));
  }

  @Test
  void savedSizeIsReused() {
    final Dimension size =
        LaunchWindowSize.compute(
            Optional.of(new Dimension(1100, 950)), new Rectangle(0, 0, 2000, 1200), MINIMUM);

    assertThat(size).isEqualTo(new Dimension(1100, 950));
  }

  @Test
  void savedSizeLargerThanScreenIsShrunkToFit() {
    final Dimension size =
        LaunchWindowSize.compute(
            Optional.of(new Dimension(2500, 1400)), new Rectangle(0, 0, 1920, 1040), MINIMUM);

    assertThat(size).isEqualTo(new Dimension(1920, 1040));
  }
}
