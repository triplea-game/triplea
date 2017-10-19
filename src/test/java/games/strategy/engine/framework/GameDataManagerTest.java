package games.strategy.engine.framework;

import static games.strategy.engine.data.Matchers.equalToGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestGameDataFactory;
import games.strategy.io.IoUtils;
import games.strategy.persistence.serializable.ProxyRegistry;

public class GameDataManagerTest {
  @Test
  public void testLoadStoreKeepsGameUuid() throws IOException {
    final GameData data = new GameData();
    final byte[] bytes = IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, data));
    final GameData loaded = IoUtils.readFromMemory(bytes, GameDataManager::loadGame);
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
    return IoUtils.writeToMemory(os -> GameDataManager.saveGameInProxySerializationFormat(
        os,
        gameData,
        Collections.emptyMap(),
        newProxyRegistry()));
  }

  private static ProxyRegistry newProxyRegistry() {
    return ProxyRegistry.newInstance(TestProxyFactoryCollectionBuilder.forGameData().build());
  }

  private static GameData loadGameInProxySerializationFormat(final byte[] bytes) throws Exception {
    return IoUtils.readFromMemory(bytes, GameDataManager::loadGameInProxySerializationFormat);
  }

  @Test
  public void loadGameInProxySerializationFormat_ShouldNotCloseInputStream() throws Exception {
    try (InputStream is = spy(newInputStreamWithGameInProxySerializationFormat())) {
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
