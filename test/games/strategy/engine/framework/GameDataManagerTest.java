package games.strategy.engine.framework;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.SerializationTest;

public class GameDataManagerTest {

  @Before
  public void setUp() throws Exception {
    // get the xml file
    final URL url = SerializationTest.class.getResource("Test.xml");
    // get the source data
    final InputStream input = url.openStream();
    (new GameParser()).parse(input, new AtomicReference<>(), false);
  }

  @Test
  public void testLoadStoreKeepsGamUUID() throws IOException {
    final GameData data = new GameData();
    final GameDataManager m = new GameDataManager();
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    m.saveGame(sink, data);
    final GameData loaded = m.loadGame(new ByteArrayInputStream(sink.toByteArray()), null);
    assertEquals(loaded.getProperties().get(GameData.GAME_UUID), data.getProperties().get(GameData.GAME_UUID));
  }
}
