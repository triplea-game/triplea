package games.strategy.triplea.baseAI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ai.AIUtils;
import games.strategy.triplea.xml.LoadGameUtil;
import junit.framework.TestCase;

public class AIUtilsTest extends TestCase {
  private GameData m_data;

  @Override
  protected void setUp() throws Exception {
    m_data = LoadGameUtil.loadGame("World War II Revised Test", "revised_test.xml");
  }

  @Override
  protected void tearDown() throws Exception {
    m_data = null;
  }

  public void testCost() {
    final UnitType infantry = m_data.getUnitTypeList().getUnitType("infantry");
    final PlayerID british = m_data.getPlayerList().getPlayerID("British");
    assertEquals(3, AIUtils.getCost(infantry, british, m_data));
  }

  public void testSortByCost() {
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final List<Unit> sorted = new ArrayList<Unit>(germany.getUnits().getUnits());
    Collections.sort(sorted, AIUtils.getCostComparator());
    assertEquals(sorted.get(0).getUnitType().getName(), "infantry");
  }
}
