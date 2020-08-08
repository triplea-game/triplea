package games.strategy.triplea.ai.pro.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ProUtilsTest {

  @Test
  void testIsPassiveNeutralPlayer() {
    final GameData data = TestMapGameData.GLOBAL1940.getGameData();
    final GamePlayer russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isPassiveNeutralPlayer(russians));
    final GamePlayer neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isPassiveNeutralPlayer(neutralTrue));
    final GamePlayer pirates = data.getPlayerList().getPlayerId("Pirates");
    assertFalse(ProUtils.isPassiveNeutralPlayer(pirates));
  }

  @Test
  void testIsNeutralPlayer() {
    final GameData data = TestMapGameData.GLOBAL1940.getGameData();
    final GamePlayer russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isNeutralPlayer(russians));
    final GamePlayer neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isNeutralPlayer(neutralTrue));
    final GamePlayer pirates = data.getPlayerList().getPlayerId("Pirates");
    assertTrue(ProUtils.isNeutralPlayer(pirates));
  }
}
