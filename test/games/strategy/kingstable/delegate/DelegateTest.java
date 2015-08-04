package games.strategy.kingstable.delegate;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import junit.framework.TestCase;


public class DelegateTest extends TestCase {
  protected GameData m_data;
  protected PlayerID black;
  protected PlayerID white;
  protected Territory[][] territories;
  protected UnitType pawn;
  protected UnitType king;

  /**
   * Creates new DelegateTest
   */
  public DelegateTest(final String name) {
    super(name);
    // System.out.println("constructor");
  }

  @Override
  public void setUp() throws Exception {
    // get the xml file
    final URL url = this.getClass().getResource("DelegateTest.xml");
    final InputStream input = url.openStream();
    m_data = (new GameParser()).parse(input, new AtomicReference<String>(), false);
    input.close();
    black = m_data.getPlayerList().getPlayerID("Black");
    white = m_data.getPlayerList().getPlayerID("White");
    territories = new Territory[m_data.getMap().getXDimension()][m_data.getMap().getYDimension()];
    for (int x = 0; x < m_data.getMap().getXDimension(); x++) {
      for (int y = 0; y < m_data.getMap().getYDimension(); y++) {
        territories[x][y] = m_data.getMap().getTerritoryFromCoordinates(x, y);
      }
    }
    pawn = m_data.getUnitTypeList().getUnitType("pawn");
    king = m_data.getUnitTypeList().getUnitType("king");
    // System.out.println("setup");
  }

  /*
   *
   * public void testSample()
   * {
   * System.out.println("samelp");
   * }
   */
  public void assertValid(final String string) {
    assertNull(string);
  }

  public void assertError(final String string) {
    assertNotNull(string);
  }

  public void testTest() {
    assertValid(null);
    assertError("Can not do this");
  }
}
