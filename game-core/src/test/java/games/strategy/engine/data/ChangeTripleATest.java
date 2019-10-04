package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.io.IoUtils;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChangeTripleATest {
  private GameData gameData;
  private Territory can;

  @BeforeEach
  void setUp() throws Exception {
    gameData = TestMapGameData.BIG_WORLD_1942.getGameData();
    can = gameData.getMap().getTerritory("Western Canada");
    assertEquals(2, can.getUnitCollection().getUnitCount());
  }

  private Change serialize(final Change change) throws Exception {
    final byte[] bytes =
        IoUtils.writeToMemory(
            os -> {
              try (ObjectOutputStream output = new GameObjectOutputStream(os)) {
                output.writeObject(change);
              }
            });
    return IoUtils.readFromMemory(
        bytes,
        is -> {
          try (ObjectInputStream input =
              new GameObjectInputStream(new GameObjectStreamFactory(gameData), is)) {
            return (Change) input.readObject();
          } catch (final ClassNotFoundException e) {
            throw new IOException(e);
          }
        });
  }

  @Test
  void testUnitsAddTerritory() {
    // add some units
    final Change change =
        ChangeFactory.addUnits(can, GameDataTestUtil.infantry(gameData).create(10, null));
    gameData.performChange(change);
    assertEquals(12, can.getUnitCollection().getUnitCount());
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(2, can.getUnitCollection().getUnitCount());
  }

  @Test
  void testUnitsRemoveTerritory() {
    // remove some units
    final Collection<Unit> units =
        can.getUnitCollection().getUnits(GameDataTestUtil.infantry(gameData), 1);
    final Change change = ChangeFactory.removeUnits(can, units);
    gameData.performChange(change);
    assertEquals(1, can.getUnitCollection().getUnitCount());
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(2, can.getUnitCollection().getUnitCount());
  }

  @Test
  void testSerializeUnitsRemoteTerritory() throws Exception {
    // remove some units
    final Collection<Unit> units =
        can.getUnitCollection().getUnits(GameDataTestUtil.infantry(gameData), 1);
    Change change = ChangeFactory.removeUnits(can, units);
    change = serialize(change);
    gameData.performChange(change);
    assertEquals(1, can.getUnitCollection().getUnitCount());
    // invert the change
    gameData.performChange(change.invert());
    assertEquals(2, can.getUnitCollection().getUnitCount());
  }
}
