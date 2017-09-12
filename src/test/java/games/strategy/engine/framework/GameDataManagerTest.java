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
import java.util.Collections;

import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.persistence.serializable.ProxyRegistry;

public class GameDataManagerTest {
  @Test
  public void testLoadStoreKeepsGameUuid() throws IOException {
    final GameData data = new GameData();
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    GameDataManager.saveGame(sink, data);
    final GameData loaded = GameDataManager.loadGame(new ByteArrayInputStream(sink.toByteArray()));
    assertEquals(loaded.getProperties().get(GameData.GAME_UUID), data.getProperties().get(GameData.GAME_UUID));
  }

  @Test
  public void shouldBeAbleToRoundTripGameDataInProxySerializationFormat() throws Exception {
    final GameData expected = TestGameDataFactory.newValidGameData();

    final byte[] bytes = saveGameInProxySerializationFormat(expected);
    final GameData actual = loadGameInProxySerializationFormat(bytes);

    assertThat(actual, is(equalToGameData(expected)));
  }

  private static byte[] saveGameInProxySerializationFormat(final GameData gameData) throws Exception {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      GameDataManager.saveGameInProxySerializationFormat(baos, gameData, Collections.emptyMap(), newProxyRegistry());
      return baos.toByteArray();
    }
  }

  private static ProxyRegistry newProxyRegistry() {
    return ProxyRegistry.newInstance(TestProxyFactoryCollectionBuilder.forGameData().build());
  }

  private static GameData loadGameInProxySerializationFormat(final byte[] bytes) throws Exception {
    try (final ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
      return GameDataManager.loadGameInProxySerializationFormat(bais);
    }
  }

  @Test
  public void loadGameInProxySerializationFormat_ShouldNotCloseInputStream() throws Exception {
    try (final InputStream is = spy(newInputStreamWithGameInProxySerializationFormat())) {
      GameDataManager.loadGameInProxySerializationFormat(is);

      verify(is, never()).close();
    }
  }

  private static InputStream newInputStreamWithGameInProxySerializationFormat() throws Exception {
    final GameData gameData = TestGameDataFactory.newValidGameData();
    final byte[] bytes = saveGameInProxySerializationFormat(gameData);
    return new ByteArrayInputStream(bytes);
  }

  @Test
  public void saveGameInProxySerializationFormat_ShouldNotCloseOutputStream() throws Exception {
    final OutputStream os = mock(OutputStream.class);
    final GameData gameData = TestGameDataFactory.newValidGameData();

    GameDataManager.saveGameInProxySerializationFormat(os, gameData, Collections.emptyMap(), newProxyRegistry());

    verify(os, never()).close();
  }
}
