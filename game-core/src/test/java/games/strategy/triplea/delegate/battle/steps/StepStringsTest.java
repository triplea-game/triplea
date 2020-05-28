package games.strategy.triplea.delegate.battle.steps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StepStringsTest {

  private static final GameData GLOBAL_1940_GAME_DATA = TestMapGameData.GLOBAL1940.getGameData();
  private static final GamePlayer BRITISH = GameDataTestUtil.british(GLOBAL_1940_GAME_DATA);
  private static final GamePlayer GERMANS = GameDataTestUtil.germans(GLOBAL_1940_GAME_DATA);
  private static final Territory SEA_ZONE =
      GameDataTestUtil.territory("1 Sea Zone", GLOBAL_1940_GAME_DATA);
  private static final Territory FRANCE =
      GameDataTestUtil.territory("France", GLOBAL_1940_GAME_DATA);

  @Test
  @DisplayName("Verify what an empty battle looks like")
  void emptyBattle() {
    final List<String> steps = StepStrings.determineStepStrings(
        false,
        false,
        true,
        BRITISH,
        GERMANS,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        SEA_ZONE,
        GLOBAL_1940_GAME_DATA,
        List.of(),
        units -> List.of(),
        false,
        false,
        false,
        false,
        false,
        false);

    assertThat(steps, is(List.of(StepStrings.REMOVE_CASUALTIES)));
  }
}