package games.strategy.triplea.ai.fastAI;

import java.util.Collection;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

public class AggregateEstimate extends AggregateResults {

  private static final long serialVersionUID = -3139949663921560523L;

  private final int battleRoundsFought;
  private final double winPercentage;
  private final List<Unit> remainingAttackingUnits;
  private final List<Unit> remainingDefendingUnits;

  AggregateEstimate(final int battleRoundsFought, final double winPercentage,
      final List<Unit> remainingAttackingUnits, final List<Unit> remainingDefendingUnits) {
    super(1);
    this.battleRoundsFought = battleRoundsFought;
    this.winPercentage = winPercentage;
    this.remainingAttackingUnits = remainingAttackingUnits;
    this.remainingDefendingUnits = remainingDefendingUnits;
  }

  @Override
  public List<Unit> getAverageAttackingUnitsRemaining() {
    return remainingAttackingUnits;
  }

  @Override
  public List<Unit> getAverageDefendingUnitsRemaining() {
    return remainingDefendingUnits;
  }

  @Override
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    final double attackerTuv = TuvUtils.getTuv(remainingAttackingUnits, attackerCostsForTuv);
    final double defenderTuv = TuvUtils.getTuv(remainingDefendingUnits, defenderCostsForTuv);
    return Tuple.of(attackerTuv, defenderTuv);
  }

  @Override
  public double getAverageTuvSwing(final PlayerID attacker, final Collection<Unit> attackers, final PlayerID defender,
      final Collection<Unit> defenders, final GameData data) {
    final IntegerMap<UnitType> attackerCostsForTuv = TuvUtils.getCostsForTuv(attacker, data);
    final IntegerMap<UnitType> defenderCostsForTuv = TuvUtils.getCostsForTuv(defender, data);
    final int attackerTotalTuv = TuvUtils.getTuv(attackers, attackerCostsForTuv);
    final int defenderTotalTuv = TuvUtils.getTuv(defenders, defenderCostsForTuv);
    final Tuple<Double, Double> average = getAverageTuvOfUnitsLeftOver(attackerCostsForTuv, defenderCostsForTuv);
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
