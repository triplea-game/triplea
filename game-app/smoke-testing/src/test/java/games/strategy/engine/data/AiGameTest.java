package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import games.strategy.engine.framework.ServerGame;
import games.strategy.triplea.delegate.EndRoundDelegate;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

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

  @Test
  void testAiGame() {
    ServerGame game = GameTestUtils.setUpGameWithAis("Test1.xml");
    game.setStopGameOnDelegateExecutionStop(true);
    while (!game.isGameOver()) {
      if (game.getData().getSequence().getRound() > 100) {
        log.warn("No winner after 100 rounds");
        break;
      }
      game.runNextStep();
    }
    assertThat(game.isGameOver(), is(true));
    assertThat(game.getData().getSequence().getRound(), greaterThan(2));
    EndRoundDelegate endDelegate = (EndRoundDelegate) game.getData().getEndRoundDelegate();
    assertThat(endDelegate.getWinners(), not(empty()));
    log.info("Game completed at round: " + game.getData().getSequence().getRound());
    log.info("Game winners: " + endDelegate.getWinners());
  }

  @RepeatedTest(10)
  // Run the first round of all-AI game several times. Ensure no errors and no winner so early.
  void testFirstRoundTenTimes() throws Exception {
    ServerGame game = GameTestUtils.setUpGameWithAis("Test1.xml");
    game.setStopGameOnDelegateExecutionStop(true);
    while (!game.isGameOver() && game.getData().getSequence().getRound() < 2) {
      game.runNextStep();
    }
    log.debug("First round stats: " + getResourceSummary(game.getData()));
    assertThat(
        "Expecting first round game to not be over so early: " + getResourceSummary(game.getData()),
        game.isGameOver(),
        is(false));
    // Need to call stopGame() to ensure ProAI resets its static ConcurrentBattleCalculator, else
    // the next test will use the wrong GameData for simulation.
    game.stopGame();
  }

  @Test
  void testAiGameWithConsumedUnits() {
    ServerGame game = GameTestUtils.setUpGameWithAis("imperialism_1974_board_game.xml");
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
  void testAiGameMovingConsumedUnitsToFactories() {
    ServerGame game = GameTestUtils.setUpGameWithAis("imperialism_1974_board_game.xml");
    GameData gameData = game.getData();
    gameData.preGameDisablePlayers(p -> !p.getName().equals("Blue"));
    game.setUpGameForRunningSteps();

    GamePlayer blue = gameData.getPlayerList().getPlayerId("Blue");
    assertThat(blue.isNull(), is(false));
    GameTestUtils.runStepsUntil(game, "BlueWealthPlace");

    // Now, set up some units on another continent to verify how the AI will handle them.
    Territory oaxaca = GameTestUtils.getTerritoryOrThrow(gameData, "Oaxaca");
    oaxaca.setOwner(blue);
    GameTestUtils.addUnits(oaxaca, blue, "army", "port");
    Territory stLucia = GameTestUtils.getTerritoryOrThrow(gameData, "St Lucia");
    stLucia.setOwner(blue);
    GameTestUtils.addUnits(stLucia, blue, "army", "wealth");
    Territory andres = GameTestUtils.getTerritoryOrThrow(gameData, "Andres");
    andres.setOwner(blue);
    GameTestUtils.addUnits(andres, blue, "army");
    Territory culiacan = GameTestUtils.getTerritoryOrThrow(gameData, "Culiacan");
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

  private String getResourceSummary(GameData gameData) {
    StringBuilder summary = new StringBuilder();
    for (GamePlayer p : gameData.getPlayerList()) {
      summary
          .append((summary.length() == 0) ? "" : ", ")
          .append(p.getName())
          .append(": ")
          .append(p.getResources());
    }
    return summary.toString();
  }
}
