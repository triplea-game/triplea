package games.strategy.triplea.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class TuvCostsCalculatorTest {
  private final GameData gameData = TestMapGameData.TWW.getGameData();
  private final TuvCostsCalculator calculator = new TuvCostsCalculator();

  @Test
  void testCostsForTuv() {
    final GamePlayer germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = calculator.getCostsForTuv(germans);
    assertEquals(3, result.getInt(GameDataTestUtil.germanInfantry(gameData)));
  }

  @Test
  void testCostsForTuvWithConsumesUnit() {
    final GamePlayer germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = calculator.getCostsForTuv(germans);
    assertEquals(11, result.getInt(GameDataTestUtil.germanFactory(gameData)));
  }

  @Test
  void testCostsForTuvWithConsumesUnitChain() {
    final GamePlayer germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = calculator.getCostsForTuv(germans);
    assertEquals(12, result.getInt(GameDataTestUtil.germanFortification(gameData)));
  }

  @Test
  void testCostsForTuvWithXmlPropertySet() {
    final GamePlayer germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = calculator.getCostsForTuv(germans);
    assertEquals(25, result.getInt(GameDataTestUtil.germanBattleship(gameData)));
  }
}
