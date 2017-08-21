package games.strategy.triplea.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.IntegerMap;

public class TuvUtilsTest {
  private GameData gameData;

  @Before
  public void setUp() throws Exception {
    gameData = TestMapGameData.GLOBAL1940.getGameData();
  }

  @Test
  public void testCostsForTuv() {
    final PlayerID british = GameDataTestUtil.british(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(british, gameData);
    assertEquals(3, result.getInt(GameDataTestUtil.infantry(gameData)));
  }

  @Test
  public void testCostsForTuvWithConsumesUnit() {
    final PlayerID british = GameDataTestUtil.british(gameData);
    final IntegerMap<UnitType> result = TuvUtils.getCostsForTuv(british, gameData);
    System.out.println(result);
    assertEquals(20, result.getInt(GameDataTestUtil.factoryUpgrade(gameData)));
  }

}
