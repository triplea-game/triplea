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
    game.startGame();
    assertThat(game.isGameOver(), is(true));
    assertThat(game.getData().getSequence().getRound(), greaterThan(2));
    EndRoundDelegate endDelegate = (EndRoundDelegate) game.getData().getDelegate("endRound");
    assertThat(endDelegate.getWinners(), not(empty()));
    log.info("Game completed at round: " + game.getData().getSequence().getRound());
    log.info("Game winners: " + endDelegate.getWinners());
  }
}
