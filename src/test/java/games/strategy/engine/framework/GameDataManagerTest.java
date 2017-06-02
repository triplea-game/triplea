package games.strategy.engine.framework;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import games.strategy.engine.data.GameData;

public class GameDataManagerTest {

  @Test
  public void testLoadStoreKeepsGameUuid() throws IOException {
    final GameData data = new GameData();
    final GameDataManager m = new GameDataManager();
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    m.saveGame(sink, data);
    final GameData loaded = m.loadGame(new ByteArrayInputStream(sink.toByteArray()), null);
    assertEquals(loaded.getProperties().get(GameData.GAME_UUID), data.getProperties().get(GameData.GAME_UUID));
  }
}
