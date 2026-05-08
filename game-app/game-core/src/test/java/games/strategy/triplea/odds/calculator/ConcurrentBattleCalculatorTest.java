package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ConcurrentBattleCalculatorTest extends AbstractClientSettingTestCase {

  // Regression test for issue #14246. When createWorkers throws (e.g. BattleCalculator's
  // constructor fails after deserialization) AFTER a prior successful setGameData, the
  // calculator must not retain isDataSet=true with an empty workers list — otherwise the next
  // calculate() call hits RunCountDistributor's parallelism>0 precondition with
  // "The parallelism level has to be positive!".
  @Test
  void calculate_returnsEmptyResults_whenWorkerConstructionThrowsAfterPriorSuccess() {
    final AtomicBoolean shouldFail = new AtomicBoolean(false);
    final Function<byte[], BattleCalculator> factory =
        bytes -> {
          if (shouldFail.get()) {
            throw new IllegalStateException("simulated worker construction failure");
          }
          return new BattleCalculator(bytes);
        };
    final ConcurrentBattleCalculator calc = new ConcurrentBattleCalculator(factory);

    final GameData gameData = TestMapGameData.REVISED.getGameData();

    // First setGameData succeeds: isDataSet becomes true and workers is populated.
    assertTrue(calc.setGameData(gameData).join());

    // Second setGameData fails inside createWorkers — exception is swallowed by exceptionally,
    // future resolves to false. Without the fix, isDataSet stays true while workers is empty.
    shouldFail.set(true);
    assertFalse(calc.setGameData(gameData).join());

    final Territory germany = gameData.getMap().getTerritoryOrNull("Germany");
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final List<Unit> attackingUnits = infantry(gameData).create(2, russians);

    final AggregateResults results =
        assertDoesNotThrow(
            () ->
                calc.calculate(
                    russians,
                    germans,
                    germany,
                    attackingUnits,
                    germany.getUnits(),
                    List.of(),
                    TerritoryEffectHelper.getEffects(germany),
                    false,
                    10));

    assertThat(results.getResults(), empty());
  }
}
