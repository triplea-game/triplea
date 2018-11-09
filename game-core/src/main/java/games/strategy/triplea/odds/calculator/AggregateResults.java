package games.strategy.triplea.odds.calculator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.BattleResults;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;
import lombok.Getter;
import lombok.Setter;

/**
 * A container for the results of multiple battle simulation runs.
 */
public class AggregateResults {
  private final List<BattleResults> results;
  @Getter
  @Setter
  private long time;

  public AggregateResults(final int expectedCount) {
    results = new ArrayList<>(expectedCount);
  }

  public void addResult(final BattleResults result) {
    results.add(result);
  }

  public void addResults(final Collection<BattleResults> results) {
    this.results.addAll(results);
  }

  public List<BattleResults> getResults() {
    return results;
  }

  private Optional<BattleResults> getBattleResultsClosestToAverage() {
    return results.stream()
        .min(Comparator.comparingDouble(
            result -> Math.abs(result.getAttackingCombatUnitsLeft() - getAverageAttackingUnitsLeft())
                + Math.abs(result.getDefendingCombatUnitsLeft() - getAverageDefendingUnitsLeft())));
  }

  public List<Unit> getAverageAttackingUnitsRemaining() {
    return getBattleResultsClosestToAverage()
        .map(BattleResults::getRemainingAttackingUnits)
        .orElseGet(ArrayList::new);
  }

  public List<Unit> getAverageDefendingUnitsRemaining() {
    return getBattleResultsClosestToAverage()
        .map(BattleResults::getRemainingDefendingUnits)
        .orElseGet(ArrayList::new);
  }

  double getAverageAttackingUnitsLeft() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream()
        .mapToDouble(BattleResults::getAttackingCombatUnitsLeft)
        .sum() / results.size();
  }

  /**
   * First is Attacker, Second is Defender.
   */
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    if (results.isEmpty()) {
      return Tuple.of(0.0, 0.0);
    }
    double attackerTuv = 0;
    double defenderTuv = 0;
    for (final BattleResults result : results) {
      attackerTuv += TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv);
      defenderTuv += TuvUtils.getTuv(result.getRemainingDefendingUnits(), defenderCostsForTuv);
    }
    return Tuple.of(attackerTuv / results.size(), defenderTuv / results.size());
  }

  public double getAverageTuvSwing(final PlayerId attacker, final Collection<Unit> attackers, final PlayerId defender,
      final Collection<Unit> defenders, final GameData data) {
    if (results.isEmpty()) {
      return 0.0;
    }
    final IntegerMap<UnitType> attackerCostsForTuv = TuvUtils.getCostsForTuv(attacker, data);
    final IntegerMap<UnitType> defenderCostsForTuv = TuvUtils.getCostsForTuv(defender, data);
    final int attackerTuv = TuvUtils.getTuv(attackers, attackerCostsForTuv);
    final int defenderTuv = TuvUtils.getTuv(defenders, defenderCostsForTuv);
    // could we possibly cause a bug by comparing UnitType's from one game data, to a different game data's UnitTypes?
    final Tuple<Double, Double> average = getAverageTuvOfUnitsLeftOver(attackerCostsForTuv, defenderCostsForTuv);
    final double attackerLost = attackerTuv - average.getFirst();
    final double defenderLost = defenderTuv - average.getSecond();
    return defenderLost - attackerLost;
  }

  double getAverageAttackingUnitsLeftWhenAttackerWon() {
    if (results.isEmpty()) {
      return 0.0;
    }
    double count = 0;
    double total = 0;
    for (final BattleResults result : results) {
      if (result.attackerWon()) {
        count += result.getAttackingCombatUnitsLeft();
        total += 1;
      }
    }
    if (total <= 0) {
      return 0;
    }
    return count / total;
  }

  double getAverageDefendingUnitsLeft() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream()
        .mapToDouble(BattleResults::getDefendingCombatUnitsLeft)
        .sum() / results.size();
  }

  double getAverageDefendingUnitsLeftWhenDefenderWon() {
    if (results.isEmpty()) {
      return 0.0;
    }
    double count = 0;
    double total = 0;
    for (final BattleResults result : results) {
      if (result.defenderWon()) {
        count += result.getDefendingCombatUnitsLeft();
        total += 1;
      }
    }
    if (total <= 0) {
      return 0;
    }
    return count / total;
  }

  public double getAttackerWinPercent() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream()
        .filter(BattleResults::attackerWon)
        .count() / (double) results.size();
  }

  double getDefenderWinPercent() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream()
        .filter(BattleResults::defenderWon)
        .count() / (double) results.size();
  }

  public double getAverageBattleRoundsFought() {
    if (results.isEmpty()) {
      return 0.0;
    }
    final long count = results.stream()
        .mapToInt(BattleResults::getBattleRoundsFought)
        .sum();
    if (count == 0) {
      // If this is a 'fake' aggregate result, return 1.0
      return 1.0;
    }
    return count / (double) results.size();
  }

  double getDrawPercent() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream()
        .filter(BattleResults::draw)
        .count() / (double) results.size();
  }

  public int getRollCount() {
    return results.size();
  }
}
