package games.strategy.engine.framework.lookandfeel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.awt.Color;
import org.junit.jupiter.api.Test;

final class LookAndFeelTest {
  @Test
  void testIsColorDark() throws Exception {
    assertThat(LookAndFeel.isColorDark(Color.BLACK), is(true));
    assertThat(LookAndFeel.isColorDark(Color.DARK_GRAY), is(true));
    assertThat(LookAndFeel.isColorDark(Color.WHITE), is(false));
    assertThat(LookAndFeel.isColorDark(Color.YELLOW), is(false));
  }
}
