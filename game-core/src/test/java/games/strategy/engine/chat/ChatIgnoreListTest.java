package games.strategy.engine.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.lobby.PlayerName;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatIgnoreListTest {
  private static final PlayerName PLAYER_NAME = PlayerName.of("test");

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
    assertFalse(list.shouldIgnore(PLAYER_NAME));
    list.add(PLAYER_NAME);
    assertTrue(list.shouldIgnore(PLAYER_NAME));
    list = new ChatIgnoreList();
    assertTrue(list.shouldIgnore(PLAYER_NAME));
  }
}
