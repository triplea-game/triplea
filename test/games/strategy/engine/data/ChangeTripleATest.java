package games.strategy.engine.data;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.LoadGameUtil;

public class ChangeTripleATest {
  private GameData m_data;
  private Territory can;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("big_world_1942_test.xml");
    can = m_data.getMap().getTerritory("Western Canada");
    assertEquals(can.getUnits().getUnitCount(), 2);
  }

  private Change serialize(final Change aChange) throws Exception {
    final ByteArrayOutputStream sink = new ByteArrayOutputStream();
    final ObjectOutputStream output = new GameObjectOutputStream(sink);
    output.writeObject(aChange);
    output.flush();
    // System.out.println("bytes:" + sink.toByteArray().length);
    final InputStream source = new ByteArrayInputStream(sink.toByteArray());
    final ObjectInputStream input =
        new GameObjectInputStream(new games.strategy.engine.framework.GameObjectStreamFactory(m_data), source);
    final Change newChange = (Change) input.readObject();
    input.close();
    output.close();
    return newChange;
  }

  @Test
  public void testUnitsAddTerritory() {
    // add some units
    final Change change =
        ChangeFactory.addUnits(can, GameDataTestUtil.infantry(m_data).create(10, null));
    m_data.performChange(change);
    assertEquals(can.getUnits().getUnitCount(), 12);
    // invert the change
    m_data.performChange(change.invert());
    assertEquals(can.getUnits().getUnitCount(), 2);
  }

  @Test
  public void testUnitsRemoveTerritory() {
    // remove some units
    final Collection<Unit> units = can.getUnits().getUnits(GameDataTestUtil.infantry(m_data), 1);
    final Change change = ChangeFactory.removeUnits(can, units);
    m_data.performChange(change);
    assertEquals(can.getUnits().getUnitCount(), 1);
    // invert the change
    m_data.performChange(change.invert());
    assertEquals(can.getUnits().getUnitCount(), 2);
  }

  @Test
  public void testSerializeUnitsRemoteTerritory() throws Exception {
    // remove some units
    final Collection<Unit> units = can.getUnits().getUnits(GameDataTestUtil.infantry(m_data), 1);
    Change change = ChangeFactory.removeUnits(can, units);
    change = serialize(change);
    m_data.performChange(change);
    assertEquals(can.getUnits().getUnitCount(), 1);
    // invert the change
    m_data.performChange(change.invert());
    assertEquals(can.getUnits().getUnitCount(), 2);
  }
}
