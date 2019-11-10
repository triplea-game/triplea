package games.strategy.engine.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.GameData;
import java.io.OutputStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.io.IoUtils;

final class GameDataManagerTest {
  @Nested
  final class RoundTripTest {
    @Test
    void shouldPreserveGameName() throws Exception {
      final GameData data = new GameData();
      final byte[] bytes = IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, data));
      final GameData loaded = IoUtils.readFromMemory(bytes, GameDataManager::loadGame);
      assertEquals(loaded.getGameName(), data.getGameName());
    }
  }

  @Nested
  final class SaveGameTest {
    @Test
    void shouldCloseOutputStream() throws Exception {
      final OutputStream os = mock(OutputStream.class);

      GameDataManager.saveGame(os, new GameData());

      verify(os).close();
    }
  }
}
