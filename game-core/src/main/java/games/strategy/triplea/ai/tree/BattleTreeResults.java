package games.strategy.triplea.ai.tree;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.util.TuvUtils;
import java.util.Collection;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

public class BattleTreeResults extends AggregateResults {
  private final BattleStep root;
  private final double winProbabality;
  private final double badProbability;

  public BattleTreeResults(final BattleStep root) {
    super(1);
    this.root = root;
    this.winProbabality = root.getWinProbability();
    this.badProbability = root.getBadProbability();
  }

  private double scale(final double amount, final double max) {
    return amount / max;
  }

  @Override
  public double getAttackerWinPercent() {
    return scale(winProbabality, 1.0 - badProbability);
  }

  @Override
  public Collection<Unit> getAverageAttackingUnitsRemaining() {
    return getAverageAttackingUnitsRemaining(0.5);
  }

  Collection<Unit> getAverageAttackingUnitsRemaining(final double chance) {
    return CollectionUtils.getMatches(
        root.getAverageUnits().getFriendlyWithChance(chance), Matches.unitIsNotInfrastructure());
  }

  @Override
  public Collection<Unit> getAverageDefendingUnitsRemaining() {
    return getAverageDefendingUnitsRemaining(0.5);
  }

  Collection<Unit> getAverageDefendingUnitsRemaining(final double chance) {
    return CollectionUtils.getMatches(
        root.getAverageUnits().getEnemyWithChance(chance), Matches.unitIsNotInfrastructure());
  }

  @Override
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(
      final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    double attackerTuv = 0;
    double defenderTuv = 0;
    int count = 0;
    for (int i = 1; i <= 20; i++) {
      count++;
      attackerTuv +=
          TuvUtils.getTuv(
              getAverageAttackingUnitsRemaining((double) i / 20.0), attackerCostsForTuv);
      defenderTuv +=
          TuvUtils.getTuv(
              getAverageDefendingUnitsRemaining((double) i / 20.0), defenderCostsForTuv);
    }
    return Tuple.of(attackerTuv / count, defenderTuv / count);
  }

  @Override
  public double getAverageTuvSwing(
      final GamePlayer attacker,
      final Collection<Unit> attackers,
      final GamePlayer defender,
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
  public double getAverageBattleRoundsFought() {
    return root.getAverageRounds();
  }
}
