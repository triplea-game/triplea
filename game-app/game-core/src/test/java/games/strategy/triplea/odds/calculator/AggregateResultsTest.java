package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.util.TuvCostsCalculator;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

public class AggregateResultsTest {
  @Test
  void testNoResultsAdded() {
    final AggregateResults results = new AggregateResults(1);

    // The methods for the TUV need some additional objects.  Note that even in an zero-result TUV
    // swing simulation, some pre-computation is done with this objects, i.e. they must be non-null.
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final GamePlayer attacker = russians(gameData);
    final List<Unit> attackingUnits = infantry(gameData).create(100, attacker);
    final GamePlayer defender = germans(gameData);
    final List<Unit> defendingUnits = infantry(gameData).create(100, defender);
    final TuvCostsCalculator tuvCalculator = new TuvCostsCalculator();
    final IntegerMap<UnitType> attackerCostsForTuv = tuvCalculator.getCostsForTuv(attacker);
    final IntegerMap<UnitType> defenderCostsForTuv = tuvCalculator.getCostsForTuv(defender);

    final Tuple<Double, Double> t =
        results.getAverageTuvOfUnitsLeftOver(attackerCostsForTuv, defenderCostsForTuv);
    assertIsNaN(t.getFirst());
    assertIsNaN(t.getSecond());
    assertIsNaN(
        results.getAverageTuvSwing(attacker, attackingUnits, defender, defendingUnits, gameData));
    assertIsNaN(results.getAverageAttackingUnitsLeft());
    assertIsNaN(results.getAverageAttackingUnitsLeftWhenAttackerWon());
    assertIsNaN(results.getAverageDefendingUnitsLeft());
    assertIsNaN(results.getAverageDefendingUnitsLeftWhenDefenderWon());
    assertIsNaN(results.getAttackerWinPercent());
    assertIsNaN(results.getDefenderWinPercent());
    assertIsNaN(results.getDrawPercent());
    assertIsNaN(results.getAverageBattleRoundsFought());
  }

  private static void assertIsNaN(final double d) {
    assertTrue(Double.isNaN(d));
  }
}
