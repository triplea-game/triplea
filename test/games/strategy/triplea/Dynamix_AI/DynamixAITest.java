package games.strategy.triplea.Dynamix_AI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.ai.AIUtils;
import games.strategy.triplea.ai.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.xml.LoadGameUtil;
import junit.framework.TestCase;


public class DynamixAITest extends TestCase {
  private GameData m_data;
  @SuppressWarnings("unused")
  private Dynamix_AI m_ai;

  @Override
  protected void setUp() throws Exception {
    m_data = LoadGameUtil.loadGame("Great Lakes War Test", "Great Lakes War v1.4 test.xml");
    m_ai = new Dynamix_AI("Superior", TripleA.DYNAMIX_COMPUTER_PLAYER_TYPE);
  }

  @Override
  protected void tearDown() throws Exception {
    m_data = null;
  }

  public void testCost() {
    final UnitType infantry = m_data.getUnitTypeList().getUnitType("infantry");
    final PlayerID superior = m_data.getPlayerList().getPlayerID("Superior");
    assertEquals(3, AIUtils.getCost(infantry, superior, m_data));
  }
}
