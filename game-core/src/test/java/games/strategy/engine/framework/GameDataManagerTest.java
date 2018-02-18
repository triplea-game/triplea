package games.strategy.engine.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.io.IoUtils;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;

public class GameDataManagerTest extends AbstractClientSettingTestCase {
  @Test
  public void testLoadStoreKeepsGameUuid() throws IOException {
    final GameData data = new GameData();
    final byte[] bytes = IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, data));
    final GameData loaded = IoUtils.readFromMemory(bytes, GameDataManager::loadGame);
    assertEquals(loaded.getProperties().get(GameData.GAME_UUID), data.getProperties().get(GameData.GAME_UUID));
  }
}
