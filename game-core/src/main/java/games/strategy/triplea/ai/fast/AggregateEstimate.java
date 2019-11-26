package games.strategy.triplea.ai.fast;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.util.TuvUtils;
import java.util.Collection;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

class AggregateEstimate extends AggregateResults {
  private final int battleRoundsFought;
  private final double winPercentage;
  private final Collection<Unit> remainingAttackingUnits;
  private final Collection<Unit> remainingDefendingUnits;

  AggregateEstimate(
      final int battleRoundsFought,
      final double winPercentage,
      final Collection<Unit> remainingAttackingUnits,
      final Collection<Unit> remainingDefendingUnits) {
    super(1);
    this.battleRoundsFought = battleRoundsFought;
    this.winPercentage = winPercentage;
    this.remainingAttackingUnits = remainingAttackingUnits;
    this.remainingDefendingUnits = remainingDefendingUnits;
  }

  @Override
  public Collection<Unit> getAverageAttackingUnitsRemaining() {
    return remainingAttackingUnits;
  }

  @Override
  public Collection<Unit> getAverageDefendingUnitsRemaining() {
    return remainingDefendingUnits;
  }

  @Override
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(
      final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    final double attackerTuv = TuvUtils.getTuv(remainingAttackingUnits, attackerCostsForTuv);
    final double defenderTuv = TuvUtils.getTuv(remainingDefendingUnits, defenderCostsForTuv);
    return Tuple.of(attackerTuv, defenderTuv);
  }

  @Override
  public double getAverageTuvSwing(
      final PlayerId attacker,
      final Collection<Unit> attackers,
      final PlayerId defender,
      final Collection<Unit> defenders,
      final GameData data) {
    final IntegerMap<UnitType> attackerCostsForTuv = TuvUtils.getCostsForTuv(attacker, data);
    final IntegerMap<UnitType> defenderCostsForTuv = TuvUtils.getCostsForTuv(defender, data);
    final int attackerTotalTuv = TuvUtils.getTuv(attackers, attackerCostsForTuv);
    final int defenderTotalTuv = TuvUtils.getTuv(defenders, defenderCostsForTuv);
    final Tuple<Double, Double> average =
        getAverageTuvOfUnitsLeftOver(attackerCostsForTuv, defenderCostsForTuv);
    final double attackerLost = attackerTotalTuv - average.getFirst();
    final double defenderLost = defenderTotalTuv - average.getSecond();
    return defenderLost - attackerLost;
  }

  @Override
  public double getAttackerWinPercent() {
    return winPercentage;
  }

  @Override
  public double getAverageBattleRoundsFought() {
    return battleRoundsFought;
  }
}
