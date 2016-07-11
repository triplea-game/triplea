package games.strategy.engine.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class ChatIgnoreListTest {
  @BeforeEach
  public void setUp() throws BackingStoreException {
    // clear this
    clearStore();
  }

  @AfterEach
  public void tearDown() throws BackingStoreException {
    clearStore();
  }

  private void clearStore() throws BackingStoreException {
    final Preferences prefs = ChatIgnoreList.getPrefNode();
    prefs.clear();
    prefs.flush();
  }

  @Test
  public void testLoadStore() {
    ChatIgnoreList list = new ChatIgnoreList();
    assertFalse(list.shouldIgnore("test"));
    list.add("test");
    assertTrue(list.shouldIgnore("test"));
    list = new ChatIgnoreList();
    assertTrue(list.shouldIgnore("test"));
  }
}
