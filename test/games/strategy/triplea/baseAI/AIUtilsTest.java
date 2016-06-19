package games.strategy.triplea.baseAI;

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
import games.strategy.triplea.ai.AIUtils;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.LoadGameUtil;

public class AIUtilsTest {
  private GameData m_data;

  @Before
  public void setUp() throws Exception {
    m_data = LoadGameUtil.loadTestGame("revised_test.xml");
  }

  @Test
  public void testCost() {
    final UnitType infantry = GameDataTestUtil.infantry(m_data);
    final PlayerID british = GameDataTestUtil.british(m_data);
    assertEquals(3, AIUtils.getCost(infantry, british, m_data));
  }

  @Test
  public void testSortByCost() {
    final Territory germany = m_data.getMap().getTerritory("Germany");
    final List<Unit> sorted = new ArrayList<>(germany.getUnits().getUnits());
    Collections.sort(sorted, AIUtils.getCostComparator());
    assertEquals(sorted.get(0).getUnitType().getName(), Constants.UNIT_TYPE_INFANTRY);
  }
}
