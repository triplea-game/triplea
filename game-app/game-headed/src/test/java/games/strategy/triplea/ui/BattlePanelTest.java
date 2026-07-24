package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.awt.Dimension;
import org.junit.jupiter.api.Test;

final class BattlePanelTest {
  @Test
  void shouldAllowBattleWindowToShrinkBelowContentFitSize() {
    final Dimension contentFitSize = new Dimension(1508, 900);

    final Dimension minimumSize = BattlePanel.minimumBattleWindowSize(contentFitSize);

    assertThat(minimumSize, is(new Dimension(800, 600)));
  }

  @Test
  void shouldNotSetMinimumSizeLargerThanContentFitSize() {
    final Dimension contentFitSize = new Dimension(760, 570);

    final Dimension minimumSize = BattlePanel.minimumBattleWindowSize(contentFitSize);

    assertThat(minimumSize, is(contentFitSize));
  }
}