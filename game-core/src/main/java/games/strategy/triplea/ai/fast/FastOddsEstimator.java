package games.strategy.triplea.ai.fast;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.IProDataUnitValue;
import games.strategy.triplea.ai.pro.util.ProBattleUtils;
import games.strategy.triplea.ai.pro.util.ProPurchaseUtils;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.IBattleCalculator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FastOddsEstimator implements IBattleCalculator {

  private final IProDataUnitValue proData;

  public FastOddsEstimator(final IProDataUnitValue proData) {
    this.proData = proData;
  }

  @Override
  public AggregateResults calculate(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits,
      final Collection<TerritoryEffect> territoryEffects,
      final boolean retreatWhenOnlyAirLeft,
      final int runCount) {
    final double winPercentage =
        ProBattleUtils.estimateStrengthDifference(
            proData, location, new ArrayList<>(attackingUnits), new ArrayList<>(defendingUnits));
    List<Unit> remainingAttackingUnits = new ArrayList<>();
    List<Unit> remainingDefendingUnits = new ArrayList<>();
    if (winPercentage > 50) {
      remainingAttackingUnits.addAll(attackingUnits);
      remainingAttackingUnits.sort(ProPurchaseUtils.getCostComparator(proData).reversed());
      final int numRemainingUnits =
          (int) Math.ceil(attackingUnits.size() * (Math.min(100, winPercentage) - 50) / 50);
      remainingAttackingUnits = remainingAttackingUnits.subList(0, numRemainingUnits);
    } else {
      remainingDefendingUnits.addAll(defendingUnits);
      remainingDefendingUnits.sort(ProPurchaseUtils.getCostComparator(proData).reversed());
      final int numRemainingUnits =
          (int) Math.ceil(defendingUnits.size() * (50 - Math.max(0, winPercentage)) / 50);
      remainingDefendingUnits = remainingDefendingUnits.subList(0, numRemainingUnits);
    }
    final int battleRoundsFought = 3;
    return new AggregateEstimate(
        battleRoundsFought, winPercentage / 100, remainingAttackingUnits, remainingDefendingUnits);
  }
}
