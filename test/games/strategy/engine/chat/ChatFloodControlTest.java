package games.strategy.engine.chat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatFloodControlTest {
  private final ChatFloodControl testObj = new ChatFloodControl();

  @Test
  public void testSimple() {
    assertTrue(testObj.allow("", System.currentTimeMillis()));
  }

  @Test
  public void testDeny() {
    for (int i = 0; i < ChatFloodControl.EVENTS_PER_WINDOW; i++) {
      testObj.allow("", System.currentTimeMillis());
    }
    assertFalse(testObj.allow("", System.currentTimeMillis()));
  }

  @Test
  public void testReney() {
    for (int i = 0; i < 100; i++) {
      testObj.allow("", System.currentTimeMillis());
    }
    assertTrue(testObj.allow("", System.currentTimeMillis() + 1000 * 60 * 60));
  }
}
