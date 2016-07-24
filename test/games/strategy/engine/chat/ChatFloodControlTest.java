package games.strategy.engine.chat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatFloodControlTest {
  private final ChatFloodControl fc = new ChatFloodControl();

  @Test
  public void testSimple() {
    assertTrue(fc.allow("", System.currentTimeMillis()));
  }

  @Test
  public void testDeny() {
    for (int i = 0; i < ChatFloodControl.EVENTS_PER_WINDOW; i++) {
      fc.allow("", System.currentTimeMillis());
    }
    assertFalse(fc.allow("", System.currentTimeMillis()));
  }

  @Test
  public void testReney() {
    for (int i = 0; i < 100; i++) {
      fc.allow("", System.currentTimeMillis());
    }
    assertTrue(fc.allow("", System.currentTimeMillis() + 1000 * 60 * 60));
  }
}
