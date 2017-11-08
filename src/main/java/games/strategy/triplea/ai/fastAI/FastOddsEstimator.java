package games.strategy.triplea.ai.fastAI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.oddscalc.OddsCalculatorParameters;

public class FastOddsEstimator implements IOddsCalculator {

  @Override
  public AggregateResults calculate(final OddsCalculatorParameters parameters) {
    final double winPercentage = ProBattleUtils.estimateStrengthDifference(
        parameters.location,
        new ArrayList<>(parameters.attacking),
        new ArrayList<>(parameters.defending));
    final int battleRoundsFought = 3;
    List<Unit> remainingAttackingUnits = new ArrayList<>();
    List<Unit> remainingDefendingUnits = new ArrayList<>();
    if (winPercentage > 50) {
      remainingAttackingUnits.addAll(parameters.attacking);
      Collections.sort(remainingAttackingUnits, ProPurchaseUtils.getCostComparator().reversed());
      final int numRemainingUnits =
          (int) Math.ceil(parameters.attacking.size() * (Math.min(100, winPercentage) - 50) / 50);
      remainingAttackingUnits = remainingAttackingUnits.subList(0, numRemainingUnits);
    } else {
      remainingDefendingUnits.addAll(parameters.defending);
      Collections.sort(remainingDefendingUnits, ProPurchaseUtils.getCostComparator().reversed());
      final int numRemainingUnits =
          (int) Math.ceil(parameters.defending.size() * (50 - Math.max(0, winPercentage)) / 50);
      remainingDefendingUnits = remainingDefendingUnits.subList(0, numRemainingUnits);
    }
    return new AggregateEstimate(battleRoundsFought, winPercentage / 100, remainingAttackingUnits,
        remainingDefendingUnits);
  }

  @Override
  public void cancel() {

  }
}
