package games.strategy.engine.data;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;

public class SerializationTest {
  private GameData gameDataSource;
  private GameData gameDataSink;

  @Before
  public void setUp() throws Exception {
    gameDataSource = TestMapGameData.TEST.getGameData();
    gameDataSink = TestMapGameData.TEST.getGameData();
  }

  private Object serialize(final Object anObject) throws Exception {
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    final ObjectOutputStream output = new GameObjectOutputStream(sink);
    output.writeObject(anObject);
    output.flush();
    final InputStream source = new ByteArrayInputStream(sink.toByteArray());
    final ObjectInputStream input =
        new GameObjectInputStream(new GameObjectStreamFactory(gameDataSource), source);
    final Object obj = input.readObject();
    input.close();
    output.close();
    return obj;
  }

  @Test
  public void testWritePlayerID() throws Exception {
    final PlayerID id = gameDataSource.getPlayerList().getPlayerID("chretian");
    final PlayerID readID = (PlayerID) serialize(id);
    final PlayerID localID = gameDataSink.getPlayerList().getPlayerID("chretian");
    assertTrue(localID != readID);
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
