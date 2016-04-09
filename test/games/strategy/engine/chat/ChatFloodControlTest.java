package games.strategy.engine.chat;

import junit.framework.TestCase;

public class ChatFloodControlTest extends TestCase { 
  private final ChatFloodControl fc = new ChatFloodControl();

  @Override
  public void setUp() {}

  public void testSimple() {
    assertTrue(fc.allow("", System.currentTimeMillis()));
  }

  public void testDeny() {
    for (int i = 0; i < ChatFloodControl.EVENTS_PER_WINDOW; i++) {
      fc.allow("", System.currentTimeMillis());
    }
    assertFalse(fc.allow("", System.currentTimeMillis()));
  }

  public void testReney() {
    for (int i = 0; i < 100; i++) {
      fc.allow("", System.currentTimeMillis());
    }
    assertTrue(fc.allow("", System.currentTimeMillis() + 1000 * 60 * 60));
  }
}
