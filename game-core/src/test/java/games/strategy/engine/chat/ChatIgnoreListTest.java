package games.strategy.engine.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatIgnoreListTest {
  @BeforeEach
  void setUp() throws BackingStoreException {
    // clear this
    clearStore();
  }

  @AfterEach
  void tearDown() throws BackingStoreException {
    clearStore();
  }

  private static void clearStore() throws BackingStoreException {
    final Preferences prefs = ChatIgnoreList.getPrefNode();
    prefs.clear();
    prefs.flush();
  }

  @Test
  void testLoadStore() {
    ChatIgnoreList list = new ChatIgnoreList();
    assertFalse(list.shouldIgnore("test"));
    list.add("test");
    assertTrue(list.shouldIgnore("test"));
    list = new ChatIgnoreList();
    assertTrue(list.shouldIgnore("test"));
  }
}
