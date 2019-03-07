package games.strategy.engine.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChatFloodControlTest {
  private static final long INITIAL_CLEAR_TIME = 100;
  private final ChatFloodControl testObj = new ChatFloodControl(INITIAL_CLEAR_TIME);

  @Test
  void testSimple() {
    assertTrue(testObj.allow("", System.currentTimeMillis()));
  }

  @Test
  void testDeny() {
    final long now = 123;
    for (int i = 0; i < ChatFloodControl.EVENTS_PER_WINDOW; i++) {
      assertTrue(testObj.allow("", now));
    }
    assertFalse(testObj.allow("", now));
  }

  @Test
  void throttlingReleasedAfterTimePeriod() {
    final long now = 100;
    for (int i = 0; i < 100; i++) {
      testObj.allow("", now);
    }
    assertTrue(testObj.allow("", INITIAL_CLEAR_TIME + ChatFloodControl.WINDOW + 1));
  }
}
