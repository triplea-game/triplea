package games.strategy.engine.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

public class SerializationTest extends TestCase {
  private GameData m_dataSource;
  private GameData m_dataSink;

  @Override
  public void setUp() throws Exception {
    // get the xml file
    final URL url = this.getClass().getResource("Test.xml");
    // get the source data
    InputStream input = url.openStream();
    m_dataSource = (new GameParser()).parse(input, new AtomicReference<>(), false);
    // get the sink data
    input = url.openStream();
    m_dataSink = (new GameParser()).parse(input, new AtomicReference<>(), false);
  }

  private Object serialize(final Object anObject) throws Exception {
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    final ObjectOutputStream output = new GameObjectOutputStream(sink);
    output.writeObject(anObject);
    output.flush();
    final InputStream source = new ByteArrayInputStream(sink.toByteArray());
    final ObjectInputStream input =
        new GameObjectInputStream(new games.strategy.engine.framework.GameObjectStreamFactory(m_dataSource), source);
    final Object obj = input.readObject();
    input.close();
    output.close();
    return obj;
  }

  /** Creates a new instance of SerializationTest */
  public SerializationTest(final String s) {
    super(s);
  }

  public void testWritePlayerID() throws Exception {
    final PlayerID id = m_dataSource.getPlayerList().getPlayerID("chretian");
    final PlayerID readID = (PlayerID) serialize(id);
    final PlayerID localID = m_dataSink.getPlayerList().getPlayerID("chretian");
    assertTrue(localID != readID);
  }

  public void testWriteUnitType() throws Exception {
    final Object orig = m_dataSource.getUnitTypeList().getUnitType("inf");
    final Object read = serialize(orig);
    final Object local = m_dataSink.getUnitTypeList().getUnitType("inf");
    assertTrue(local != read);
  }

  public void testWriteTerritory() throws Exception {
    final Object orig = m_dataSource.getMap().getTerritory("canada");
    final Object read = serialize(orig);
    final Object local = m_dataSink.getMap().getTerritory("canada");
    assertTrue(local != read);
  }

  public void testWriteProductionRulte() throws Exception {
    final Object orig = m_dataSource.getProductionRuleList().getProductionRule("infForSilver");
    final Object read = serialize(orig);
    final Object local = m_dataSink.getProductionRuleList().getProductionRule("infForSilver");
    assertTrue(local != read);
  }
}
