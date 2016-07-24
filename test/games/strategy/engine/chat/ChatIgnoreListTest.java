package games.strategy.engine.chat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChatIgnoreListTest {
  @Before
  public void setUp() throws BackingStoreException {
    // clear this
    clearStore();
  }

  @After
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
