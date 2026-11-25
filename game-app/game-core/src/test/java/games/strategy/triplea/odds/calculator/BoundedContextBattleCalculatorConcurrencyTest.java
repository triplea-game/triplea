package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Proves the lock-free bake's <em>shape</em>, not its staleness semantics (design §3): a background
 * thread flips a game property the bake actually reads (low-luck) via a locked {@code
 * performChange} while the main thread hammers {@code calculate} reading that same property without
 * a lock. Every call must still return a non-null result without throwing — a stale bake is
 * acceptable by design; a crash from racing the live game read is not. It exercises the bake's
 * lock-free scalar reads, not the collection-iteration retry (which a single-mutation race, not
 * this steady churn, would probe).
 */
class BoundedContextBattleCalculatorConcurrencyTest {

  private static final int CALCULATE_ITERATIONS = 40;

  @Test
  void calculateSurvivesConcurrentGameMutation() throws InterruptedException {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);

    final BoundedContextBattleCalculator calculator = new BoundedContextBattleCalculator();
    calculator.setGameData(gameData);

    final AtomicBoolean stop = new AtomicBoolean(false);
    final Thread churn =
        new Thread(
            () -> {
              boolean lowLuck = false;
              while (!stop.get()) {
                lowLuck = !lowLuck;
                gameData.performChange(
                    ChangeFactory.setProperty(Constants.LOW_LUCK, lowLuck, gameData));
              }
            });
    churn.setDaemon(true);
    churn.start();

    try {
      for (int i = 0; i < CALCULATE_ITERATIONS; i++) {
        final AggregateResults results =
            assertDoesNotThrow(
                () ->
                    calculator.calculate(
                        russians,
                        germans,
                        germany,
                        infantry(gameData).create(6, russians),
                        infantry(gameData).create(4, germans),
                        List.of(),
                        TerritoryEffectHelper.getEffects(germany),
                        false,
                        20));
        assertNotNull(results);
      }
    } finally {
      stop.set(true);
      churn.join();
    }
  }
}
