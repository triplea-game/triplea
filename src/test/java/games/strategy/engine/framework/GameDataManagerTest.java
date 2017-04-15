package games.strategy.engine.framework;

import static games.strategy.engine.data.Matchers.equalToGameData;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestGameDataFactory;

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

  @Test
  public void shouldBeAbleToRoundTripSerializableGameData() throws Exception {
    final GameData expected = TestGameDataFactory.newValidGameData();

    final byte[] bytes = saveSerializableGame(expected);
    final GameData actual = loadSerializableGame(bytes);

    assertThat(actual, is(equalToGameData(expected)));
  }

  private static byte[] saveSerializableGame(final GameData gameData) throws Exception {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      GameDataManager.saveSerializableGame(baos, gameData);
      return baos.toByteArray();
    }
  }

  private static GameData loadSerializableGame(final byte[] bytes) throws Exception {
    try (final ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
      return GameDataManager.loadSerializableGame(bais);
    }
  }

  @Test
  public void loadSerializableGame_ShouldNotCloseInputStream() throws Exception {
    try (final InputStream is = spy(newSerializableGameInputStream())) {
      GameDataManager.loadSerializableGame(is);

      verify(is, never()).close();
    }
  }

  private static InputStream newSerializableGameInputStream() throws Exception {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    final byte[] bytes = saveSerializableGame(gameData);
    return new ByteArrayInputStream(bytes);
  }

  @Test
  public void saveSerializableGame_ShouldNotCloseOutputStream() throws Exception {
    final OutputStream os = mock(OutputStream.class);
    final GameData gameData = TestGameDataFactory.newValidGameData();

    GameDataManager.saveSerializableGame(os, gameData);

    verify(os, never()).close();
  }
}
