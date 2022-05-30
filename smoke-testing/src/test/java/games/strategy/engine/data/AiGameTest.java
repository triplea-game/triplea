package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.delegate.Matches;
import java.io.IOException;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.triplea.java.collections.CollectionUtils;

/**
 * End-to-end test that starts all-AI games on a selected set of maps and verifies they conclude
 * with a victory for one of the sides. This ensures the AI code doesn't run into errors and can
 * make progress towards victory conditions.
 */
@Slf4j
public class AiGameTest {
  @BeforeAll
  public static void setUp() throws IOException {
    GameTestUtils.setUp();
  }

  @ParameterizedTest
  @CsvSource({
    "map_making_tutorial,map/games/Test1.xml",
  })
  void testAiGame(String mapName, String mapXmlPath) throws Exception {
    GameSelectorModel gameSelector = GameTestUtils.loadGameFromURI(mapName, mapXmlPath);
    ServerGame game = GameTestUtils.setUpGameWithAis(gameSelector);
    game.setStopGameOnDelegateExecutionStop(true);
    game.startGame();
    assertThat(game.isGameOver(), is(true));
    assertThat(game.getData().getSequence().getRound(), greaterThan(2));
    EndRoundDelegate endDelegate = (EndRoundDelegate) game.getData().getDelegate("endRound");
    assertThat(endDelegate.getWinners(), not(empty()));
    log.info("Game completed at round: " + game.getData().getSequence().getRound());
    log.info("Game winners: " + endDelegate.getWinners());
  }

  @Test
  void testAiGameWithConsumedUnits() throws Exception {
    GameSelectorModel gameSelector =
        GameTestUtils.loadGameFromURI(
            "imperialism_1974_board_game", "map/games/imperialism_1974_board_game.xml");
    ServerGame game = GameTestUtils.setUpGameWithAis(gameSelector);
    game.getData().preGameDisablePlayers(p -> !p.getName().equals("Blue"));
    game.setStopGameOnDelegateExecutionStop(true);
    game.setUpGameForRunningSteps();
    GamePlayer blue = game.getData().getPlayerList().getPlayerId("Blue");
    assertThat(blue.isNull(), is(false));
    // Check that after wealth place, we have 1 wealth and 10 armies.
    runStepsUntil(game, "BlueWealthPlace");
    assertThat(countUnitsOfType(game.getData(), blue, "wealth"), is(1));
    assertThat(countUnitsOfType(game.getData(), blue, "new_army"), is(0));
    assertThat(countUnitsOfType(game.getData(), blue, "army"), is(10));
    // Now, execute steps until Place, which includes Purchase.
    runStepsUntil(game, "BluePlace");
    // Check that the AI has built a new army using a wealth unit.
    assertThat(countUnitsOfType(game.getData(), blue, "wealth"), is(0));
    assertThat(countUnitsOfType(game.getData(), blue, "new_army"), is(1));
    assertThat(countUnitsOfType(game.getData(), blue, "army"), is(10));
  }

  private void runStepsUntil(ServerGame game, String stopAfterStepName) {
    while (true) {
      boolean stop = game.getData().getSequence().getStep().getName().equals(stopAfterStepName);
      game.runNextStep();
      if (stop) {
        return;
      }
    }
  }

  private int countUnitsOfType(GameData data, GamePlayer player, String unitTypeName) {
    UnitType unitType = data.getUnitTypeList().getUnitType(unitTypeName);
    assertThat(unitType, notNullValue());
    Predicate<Unit> matcher = Matches.unitIsOwnedBy(player).and(Matches.unitIsOfType(unitType));
    // Note: We don't use game.getUnits() because units are never removed from there.
    // We also don't use player.getUnits() because those are just the units-to-place.
    int count = 0;
    for (Territory t : data.getMap().getTerritories()) {
      count += CollectionUtils.countMatches(t.getUnits(), matcher);
    }
    return count;
  }
}
