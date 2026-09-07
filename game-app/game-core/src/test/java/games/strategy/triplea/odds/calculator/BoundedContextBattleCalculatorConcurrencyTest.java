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
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Proves the lock-free bake's <em>shape</em>, not its staleness semantics (design §3): a background
 * thread mutates the live game while the main thread hammers {@code calculate}, and every call must
 * still return a non-null result without throwing. It does not assert on the odds — a stale bake is
 * acceptable by design; a crash from iterating a mutating collection is not.
 */
class BoundedContextBattleCalculatorConcurrencyTest {

  private static final int CALCULATE_ITERATIONS = 40;

  @Test
  void calculateSurvivesConcurrentGameMutation() throws InterruptedException {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    // An unrelated territory the churn thread pounds, kept off the battle so it cannot skew the
    // odds.
    final Territory russia = territory("Russia", gameData);

    final BoundedContextBattleCalculator calculator = new BoundedContextBattleCalculator();
    calculator.setGameData(gameData);

    final AtomicBoolean stop = new AtomicBoolean(false);
    final Thread churn =
        new Thread(
            () -> {
              while (!stop.get()) {
                final var add =
                    ChangeFactory.addUnits(russia, infantry(gameData).create(1, russians));
                gameData.performChange(add);
                gameData.performChange(add.invert());
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
