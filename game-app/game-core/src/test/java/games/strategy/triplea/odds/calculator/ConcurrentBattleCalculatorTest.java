package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
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

    assertThat(results.getResults()).isEmpty();
  }

  /**
   * With the flag ON, {@link BattleCalculatorFactory} hands the AI the engine-free bounded-context
   * calculator, so this pins that the factory-selected path runs end to end over many parallel
   * runs.
   *
   * <p>The bounded calc fans runs across workers on a {@code PlainRandomSource} with no injection
   * seam, so this cannot pin an exact {@code alwaysHits} outcome the way {@code
   * BattleCalculatorTest} does; it asserts coherence and direction instead. A lopsided 10-vs-2
   * infantry attack is attacker-favored regardless of luck, so a near-certain attacker win over
   * many runs shows the flag-on path ran end to end and its odds read sanely.
   */
  @Test
  void flagOnRoutesFactoryToBoundedContextCalc() {
    ClientSetting.useBoundedContextBattleCalc.setValue(true);
    final IBattleCalculator calc = BattleCalculatorFactory.newBattleCalculator();
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    assertTrue(calc.setGameData(gameData).join());

    final Territory germany = gameData.getMap().getTerritoryOrNull("Germany");
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);

    final AggregateResults results =
        calc.calculate(
            russians,
            germans,
            germany,
            infantry(gameData).create(10, russians),
            infantry(gameData).create(2, germans),
            List.of(),
            TerritoryEffectHelper.getEffects(germany),
            false,
            50);

    assertThat(results.getRollCount()).isEqualTo(50);
    assertThat(results.getAttackerWinPercent()).isGreaterThan(0.9);
    assertThat(results.getDefenderWinPercent()).isLessThan(0.1);
  }
}
