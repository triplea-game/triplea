package org.triplea.swing;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;

final class ScreenShareSizeTest {
  private static final Rectangle SCREEN = new Rectangle(0, 0, 2000, 1000);

  @Test
  void sizeIsTheRequestedShareOfTheScreen() {
    final Dimension size = ScreenShareSize.compute(new Dimension(100, 100), SCREEN, 0.5, 0.75);

    assertThat(size).isEqualTo(new Dimension(1000, 750));
  }

  @Test
  void sizeIsNeverBelowTheMinimum() {
    final Dimension size = ScreenShareSize.compute(new Dimension(1200, 900), SCREEN, 0.5, 0.75);

    assertThat(size).isEqualTo(new Dimension(1200, 900));
  }

  @Test
  void minimumLargerThanScreenIsCappedToTheScreen() {
    final Dimension size = ScreenShareSize.compute(new Dimension(2500, 1200), SCREEN, 0.5, 0.5);

    assertThat(size).isEqualTo(new Dimension(2000, 1000));
  }
}
