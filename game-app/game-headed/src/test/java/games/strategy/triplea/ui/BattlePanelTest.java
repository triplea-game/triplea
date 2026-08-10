package games.strategy.triplea.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.Dimension;
import org.junit.jupiter.api.Test;

final class BattlePanelTest {
  @Test
  void shouldAllowBattleWindowToShrinkBelowContentFitSize() {
    final Dimension contentFitSize = new Dimension(1508, 900);

    assertNotEquals(BattlePanel.MINIMUM_BATTLE_WINDOW_DEFAULT_WIDTH, contentFitSize.width);
    assertNotEquals(BattlePanel.MINIMUM_BATTLE_WINDOW_DEFAULT_HEIGHT, contentFitSize.height);

    final Dimension minimumSize = BattlePanel.getMinimumBattleWindowSize(contentFitSize);

    assertEquals(
        new Dimension(
            BattlePanel.MINIMUM_BATTLE_WINDOW_DEFAULT_WIDTH,
            BattlePanel.MINIMUM_BATTLE_WINDOW_DEFAULT_HEIGHT),
        minimumSize);
  }

  @Test
  void keepMinimumWindowDimensionWhenSmallerThanDefault() {
    final Dimension contentFitSize = new Dimension(760, 570);

    final Dimension minimumSize = BattlePanel.getMinimumBattleWindowSize(contentFitSize);

    assertEquals(contentFitSize, minimumSize);
  }
}
