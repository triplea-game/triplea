package games.strategy.triplea.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class TuvUtilsTest {
  private GameData gameData = TestMapGameData.TWW.getGameData();

  @Test
  void testCostsForTuv() {
    final PlayerId germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(germans, gameData);
    assertEquals(3, result.getInt(GameDataTestUtil.germanInfantry(gameData)));
  }

  @Test
  void testCostsForTuvWithConsumesUnit() {
    final PlayerId germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(germans, gameData);
    assertEquals(11, result.getInt(GameDataTestUtil.germanFactory(gameData)));
  }

  @Test
  void testCostsForTuvWithConsumesUnitChain() {
    final PlayerId germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(germans, gameData);
    assertEquals(12, result.getInt(GameDataTestUtil.germanFortification(gameData)));
  }

  @Test
  void testCostsForTuvWithXmlPropertySet() {
    final PlayerId germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(germans, gameData);
    assertEquals(25, result.getInt(GameDataTestUtil.germanBattleship(gameData)));
  }
}
