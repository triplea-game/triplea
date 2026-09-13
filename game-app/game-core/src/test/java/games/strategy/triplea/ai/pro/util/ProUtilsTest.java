package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.japanese;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProUtilsTest {

  @Test
  void testIsPassiveNeutralPlayer() {
    final GameState data = TestMapGameData.GLOBAL1940.getGameData();
    final GamePlayer russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isPassiveNeutralPlayer(russians));
    final GamePlayer neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isPassiveNeutralPlayer(neutralTrue));
    final GamePlayer pirates = data.getPlayerList().getPlayerId("Pirates");
    assertFalse(ProUtils.isPassiveNeutralPlayer(pirates));
  }

  @Test
  void testIsNeutralPlayer() {
    final GameState data = TestMapGameData.GLOBAL1940.getGameData();
    final GamePlayer russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isNeutralPlayer(russians));
    final GamePlayer neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isNeutralPlayer(neutralTrue));
    final GamePlayer pirates = data.getPlayerList().getPlayerId("Pirates");
    assertTrue(ProUtils.isNeutralPlayer(pirates));
  }

  @Test
  void testSummarizeUnits() {
    final GameState data = TestMapGameData.GLOBAL1940.getGameData();
    final List<Unit> units = new ArrayList<>();
    units.addAll(infantry(data).create(5, germans(data)));
    units.addAll(armour(data).create(1, germans(data)));
    units.addAll(infantry(data).create(2, japanese(data)));
    assertThat(ProUtils.summarizeUnits(units))
        .isEqualTo(
            "[armour owned by Germans, 5 infantry owned by Germans, 2 infantry owned by"
                + " Japanese]");
  }
}
