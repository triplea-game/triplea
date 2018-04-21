package games.strategy.triplea.ai.pro.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.xml.TestMapGameData;

public class ProUtilsTest {

  @Test
  public void testIsPassiveNeutralPlayer() throws Exception {
    final GameData data = TestMapGameData.GLOBAL1940.getGameData();
    final PlayerID russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isPassiveNeutralPlayer(russians));
    final PlayerID neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isPassiveNeutralPlayer(neutralTrue));
    final PlayerID pirates = data.getPlayerList().getPlayerId("Pirates");
    assertFalse(ProUtils.isPassiveNeutralPlayer(pirates));
  }

  @Test
  public void testIsNeutralPlayer() throws Exception {
    final GameData data = TestMapGameData.GLOBAL1940.getGameData();
    final PlayerID russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isNeutralPlayer(russians));
    final PlayerID neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isNeutralPlayer(neutralTrue));
    final PlayerID pirates = data.getPlayerList().getPlayerId("Pirates");
    assertTrue(ProUtils.isNeutralPlayer(pirates));
  }

}
