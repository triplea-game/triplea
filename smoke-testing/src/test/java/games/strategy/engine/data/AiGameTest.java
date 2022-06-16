package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.triplea.delegate.EndRoundDelegate;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    while (!game.isGameOver()) {
      game.runNextStep();
      if (game.getData().getSequence().getRound() > 50) {
        log.warn("No winner after 50 rounds");
        break;
      }
    }
    assertThat(game.isGameOver(), is(true));
    assertThat(game.getData().getSequence().getRound(), greaterThan(2));
    EndRoundDelegate endDelegate = (EndRoundDelegate) game.getData().getDelegate("endRound");
    assertThat(endDelegate.getWinners(), not(empty()));
    log.info("Game completed at round: " + game.getData().getSequence().getRound());
    log.info("Game winners: " + endDelegate.getWinners());
  }

  @Test
  void testAiGameWithConsumedUnits() throws Exception {
    ServerGame game = loadImperialism1974();
    game.getData().preGameDisablePlayers(p -> !p.getName().equals("Blue"));
    game.setUpGameForRunningSteps();

    GamePlayer blue = game.getData().getPlayerList().getPlayerId("Blue");
    assertThat(blue.isNull(), is(false));
    // Check that after wealth place, we have 1 wealth and 10 armies.
    GameTestUtils.runStepsUntil(game, "BlueWealthPlace");
    assertThat(GameTestUtils.countUnitsOfType(blue, "wealth"), is(1));
    assertThat(GameTestUtils.countUnitsOfType(blue, "new_army"), is(0));
    assertThat(GameTestUtils.countUnitsOfType(blue, "army"), is(10));
    // Now, execute steps until Place, which includes Purchase.
    GameTestUtils.runStepsUntil(game, "BluePlace");
    // Check that the AI has built a new army using a wealth unit.
    assertThat(GameTestUtils.countUnitsOfType(blue, "wealth"), is(0));
    assertThat(GameTestUtils.countUnitsOfType(blue, "new_army"), is(1));
    assertThat(GameTestUtils.countUnitsOfType(blue, "army"), is(10));
  }

  @Test
  void testAiGameMovingConsumedUnitsToFactories() throws Exception {
    ServerGame game = loadImperialism1974();
    GameData gameData = game.getData();
    gameData.preGameDisablePlayers(p -> !p.getName().equals("Blue"));
    game.setUpGameForRunningSteps();

    GamePlayer blue = gameData.getPlayerList().getPlayerId("Blue");
    assertThat(blue.isNull(), is(false));
    GameTestUtils.runStepsUntil(game, "BlueWealthPlace");

    // Now, set up some units on another continent to verify how the AI will handle them.
    Territory oaxaca = GameTestUtils.getTerritory(gameData, "Oaxaca");
    oaxaca.setOwner(blue);
    GameTestUtils.addUnits(oaxaca, blue, "army", "port");
    Territory stLucia = GameTestUtils.getTerritory(gameData, "St Lucia");
    stLucia.setOwner(blue);
    GameTestUtils.addUnits(stLucia, blue, "army", "wealth");
    Territory andres = GameTestUtils.getTerritory(gameData, "Andres");
    andres.setOwner(blue);
    GameTestUtils.addUnits(andres, blue, "army");
    Territory culiacan = GameTestUtils.getTerritory(gameData, "Culiacan");
    culiacan.setOwner(blue);
    GameTestUtils.addUnits(culiacan, blue, "army", "wealth");

    GameTestUtils.runStepsUntil(game, "BlueEndTurn");
    // Verify that the St Lucia wealth was moved to Andres (on the way to Oaxaca that has a port).
    assertThat(GameTestUtils.countUnitsOfType(andres, blue, "wealth"), is(1));
    assertThat(GameTestUtils.countUnitsOfType(stLucia, blue, "wealth"), is(0));
    // Verify that the Culiacan wealth was moved to Oaxaca (that has a port).
    assertThat(GameTestUtils.countUnitsOfType(oaxaca, blue, "wealth"), is(1));
    assertThat(GameTestUtils.countUnitsOfType(culiacan, blue, "wealth"), is(0));
  }

  private ServerGame loadImperialism1974() throws Exception {
    GameSelectorModel gameSelector =
        GameTestUtils.loadGameFromURI(
            "imperialism_1974_board_game", "map/games/imperialism_1974_board_game.xml");
    return GameTestUtils.setUpGameWithAis(gameSelector);
  }
}
