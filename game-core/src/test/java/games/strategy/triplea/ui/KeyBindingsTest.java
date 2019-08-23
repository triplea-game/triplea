package games.strategy.triplea.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.Test;

class KeyBindingsTest {
  private static final KeyBindings testObj =
      () -> {
        throw new UnsupportedOperationException("test fake, not supported");
      };

  @Test
  void fromKeyEventCode() {
    assertThrows(IllegalArgumentException.class, () -> testObj.fromKeyEventCode(1110));
  }

  @Test
  void fromKeyEventCodeSuccessCases() {
    assertThat(testObj.fromKeyEventCode(KeyEvent.VK_F), is(KeyStroke.getKeyStroke('F', 0)));
    assertThat(testObj.fromKeyEventCode(KeyEvent.VK_S), is(KeyStroke.getKeyStroke('S', 0)));
    assertThat(testObj.fromKeyEventCode(KeyEvent.VK_C), is(KeyStroke.getKeyStroke('C', 0)));
    assertThat(testObj.fromKeyEventCode(KeyEvent.VK_E), is(KeyStroke.getKeyStroke('E', 0)));
    assertThat(testObj.fromKeyEventCode(KeyEvent.VK_SPACE), is(KeyStroke.getKeyStroke(' ', 0)));
  }
}
