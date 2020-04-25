package games.strategy.triplea.ai.fast;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.util.ProBattleUtils;
import games.strategy.triplea.ai.pro.util.ProPurchaseUtils;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.IBattleCalculator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class FastOddsEstimator implements IBattleCalculator {

  private final ProData proData;

  FastOddsEstimator(final ProData proData) {
    this.proData = proData;
  }

  @Override
  public void setGameData(final GameData data) {}

  @Override
  public AggregateResults calculate(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits,
      final Collection<TerritoryEffect> territoryEffects,
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

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {}

  @Override
  public void setAmphibious(final boolean bool) {}

  @Override
  public void setRetreatAfterRound(final int value) {}

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {}

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {}

  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {}

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {}

  @Override
  public int getThreadCount() {
    return 1;
  }
}
