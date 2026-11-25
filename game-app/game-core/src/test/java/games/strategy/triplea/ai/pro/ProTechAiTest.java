package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProTechAiTest {
  private final GameData data = TestMapGameData.WW2V3_1941.getGameData();

  @Test
  void landUnitsCountTowardLandStrength() {
    final List<Unit> units = infantry(data).createTemp(2, russians(data));

    assertThat(ProTechAi.strength(units, true, false, false)).isGreaterThan(0F);
    assertThat(ProTechAi.strength(units, false, false, false)).isGreaterThan(0F);
  }

  @Test
  void landUnitsDoNotCountTowardSeaStrength() {
    final List<Unit> units = infantry(data).createTemp(2, russians(data));

    assertThat(ProTechAi.strength(units, true, true, false)).isEqualTo(0F);
  }
}
