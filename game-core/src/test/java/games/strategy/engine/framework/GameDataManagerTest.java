package games.strategy.engine.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.GameData;
import java.io.OutputStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.io.IoUtils;
import org.triplea.util.Version;

final class GameDataManagerTest {
  @Nested
  final class RoundTripTest {
    @Test
    void shouldPreserveGameName() throws Exception {
      final GameData data = new GameData();
      final byte[] bytes =
          IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, data, new Version(2, 0, 0)));
      final GameData loaded =
          IoUtils.readFromMemory(
                  bytes, input -> GameDataManager.loadGame(new Version(2, 0, 0), input))
              .orElseThrow();
      assertEquals(loaded.getGameName(), data.getGameName());
    }
  }

  @Nested
  final class SaveGameTest {
    @Test
    void shouldCloseOutputStream() throws Exception {
      final OutputStream os = mock(OutputStream.class);

      GameDataManager.saveGame(os, new GameData(), new Version(2, 0, 0));

      verify(os).close();
    }
  }
}
