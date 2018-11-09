package games.strategy.triplea.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.IntegerMap;

public class TuvUtilsTest {
  private GameData gameData;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.TWW.getGameData();
  }

  @Test
  public void testCostsForTuv() {
    final PlayerId germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(germans, gameData);
    assertEquals(3, result.getInt(GameDataTestUtil.germanInfantry(gameData)));
  }

  @Test
  public void testCostsForTuvWithConsumesUnit() {
    final PlayerId germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(germans, gameData);
    assertEquals(11, result.getInt(GameDataTestUtil.germanFactory(gameData)));
  }

  @Test
  public void testCostsForTuvWithConsumesUnitChain() {
    final PlayerId germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(germans, gameData);
    assertEquals(12, result.getInt(GameDataTestUtil.germanFortification(gameData)));
  }

  @Test
  public void testCostsForTuvWithXmlPropertySet() {
    final PlayerId germans = GameDataTestUtil.germany(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(germans, gameData);
    assertEquals(25, result.getInt(GameDataTestUtil.germanBattleship(gameData)));
  }

}
