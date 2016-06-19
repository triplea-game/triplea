package games.strategy.triplea.delegate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.IntegerMap;

public class Pacific_1940_Test {
  private GameData m_data;

  @Before
  protected void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("ww2pac40_test.xml");
  }

  @SuppressWarnings("unused")
  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, m_data);
  }

  public void test() {
    // TODO
  }

  /*
   * Add Utilities here
   */
  @SuppressWarnings("unused")
  private Collection<Unit> getUnits(final IntegerMap<UnitType> units, final PlayerID from) {
    final Iterator<UnitType> iter = units.keySet().iterator();
    final Collection<Unit> rVal = new ArrayList<>(units.totalValues());
    while (iter.hasNext()) {
      final UnitType type = iter.next();
      rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
    }
    return rVal;
  }

  /*
   * Add assertions here
   */
  public void assertValid(final String string) {
    assertNull(string, string);
  }

  public void assertError(final String string) {
    assertNotNull(string, string);
  }
}
