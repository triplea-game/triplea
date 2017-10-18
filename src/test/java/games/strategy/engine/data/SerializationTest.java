package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.io.IoUtils;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;

public class SerializationTest {
  private GameData gameDataSource;
  private GameData gameDataSink;

  @BeforeEach
  public void setUp() throws Exception {
    gameDataSource = TestMapGameData.TEST.getGameData();
    gameDataSink = TestMapGameData.TEST.getGameData();
  }

  private Object serialize(final Object anObject) throws Exception {
    final byte[] bytes = IoUtils.writeToMemory(os -> {
      try (ObjectOutputStream output = new GameObjectOutputStream(os)) {
        output.writeObject(anObject);
      }
    });
    return IoUtils.readFromMemory(bytes, is -> {
      try (ObjectInputStream input = new GameObjectInputStream(new GameObjectStreamFactory(gameDataSource), is)) {
        return input.readObject();
      } catch (final ClassNotFoundException e) {
        throw new IOException(e);
      }
    });
  }

  @Test
  public void testWritePlayerId() throws Exception {
    final PlayerID id = gameDataSource.getPlayerList().getPlayerId("chretian");
    final PlayerID readId = (PlayerID) serialize(id);
    final PlayerID localId = gameDataSink.getPlayerList().getPlayerId("chretian");
    assertTrue(localId != readId);
  }

  @Test
  public void testWriteUnitType() throws Exception {
    final Object orig = gameDataSource.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF);
    final Object read = serialize(orig);
    final Object local = gameDataSink.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF);
    assertTrue(local != read);
  }

  @Test
  public void testWriteTerritory() throws Exception {
    final Object orig = gameDataSource.getMap().getTerritory("canada");
    final Object read = serialize(orig);
    final Object local = gameDataSink.getMap().getTerritory("canada");
    assertTrue(local != read);
  }

  @Test
  public void testWriteProductionRulte() throws Exception {
    final Object orig = gameDataSource.getProductionRuleList().getProductionRule("infForSilver");
    final Object read = serialize(orig);
    final Object local = gameDataSink.getProductionRuleList().getProductionRule("infForSilver");
    assertTrue(local != read);
  }
}
