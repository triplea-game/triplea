package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import javax.swing.SwingConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapScrollPanelTest {

  @Test
  @DisplayName("A scroll step moves about two lines of text, a page step the visible height")
  void scrollsInStepsThatFitTallRows() {
    final MapScrollPanel panel = new MapScrollPanel();
    final Rectangle visible = new Rectangle(0, 0, 300, 500);

    final int step = panel.getScrollableUnitIncrement(visible, SwingConstants.VERTICAL, 1);
    final int page = panel.getScrollableBlockIncrement(visible, SwingConstants.VERTICAL, 1);

    assertThat(step).as("three steps per wheel notch scroll about one map row").isEqualTo(40);
    assertThat(page).as("Page Down scrolls the visible height").isEqualTo(500);
  }
}
