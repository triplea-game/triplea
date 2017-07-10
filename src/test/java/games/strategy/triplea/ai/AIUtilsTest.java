package games.strategy.triplea.ai;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;

public class AIUtilsTest {
  private GameData gameData;

  @Before
  public void setUp() throws Exception {
    gameData = TestMapGameData.REVISED.getGameData();
  }

  @Test
  public void testCost() {
    final UnitType infantry = GameDataTestUtil.infantry(gameData);
    final PlayerID british = GameDataTestUtil.british(gameData);
    assertEquals(3, AIUtils.getCost(infantry, british, gameData));
  }

  @Test
  public void testSortByCost() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final List<Unit> sorted = new ArrayList<>(germany.getUnits().getUnits());
    Collections.sort(sorted, AIUtils.getCostComparator());
    assertEquals(sorted.get(0).getUnitType().getName(), Constants.UNIT_TYPE_INFANTRY);
  }
}
