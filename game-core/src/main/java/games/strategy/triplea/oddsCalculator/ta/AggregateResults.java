package games.strategy.triplea.oddsCalculator.ta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;
import lombok.Getter;
import lombok.Setter;

public class AggregateResults implements Serializable {
  private static final long serialVersionUID = -556699626060414738L;
  // can be empty!
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

  /**
   * This could be null if we have zero results.
   */
  public BattleResults getBattleResultsClosestToAverage() {
    double closestBattleDif = Integer.MAX_VALUE;
    BattleResults closestBattle = null;
    for (final BattleResults results : results) {
      double dif = Math.abs(results.getAttackingCombatUnitsLeft() - getAverageAttackingUnitsLeft());
      dif += Math.abs(results.getDefendingCombatUnitsLeft() - getAverageDefendingUnitsLeft());
      if (dif < closestBattleDif) {
        closestBattleDif = dif;
        closestBattle = results;
      }
    }
    // can be null!
    return closestBattle;
  }

  public List<Unit> getAverageAttackingUnitsRemaining() {
    // can be null!
    final BattleResults results = getBattleResultsClosestToAverage();
    return results == null ? new ArrayList<>() : results.getRemainingAttackingUnits();
  }

  public List<Unit> getAverageDefendingUnitsRemaining() {
    // can be null!
    final BattleResults results = getBattleResultsClosestToAverage();
    return results == null ? new ArrayList<>() : results.getRemainingDefendingUnits();
  }

  double getAverageAttackingUnitsLeft() {
    if (results.isEmpty()) { // can be empty!
      return 0.0;
    }
    double count = 0;
    for (final BattleResults result : results) {
      count += result.getAttackingCombatUnitsLeft();
    }
    return count / results.size();
  }

  /**
   * First is Attacker, Second is Defender.
   */
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    if (results.isEmpty()) { // can be empty!
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

  public double getAverageTuvSwing(final PlayerID attacker, final Collection<Unit> attackers, final PlayerID defender,
      final Collection<Unit> defenders, final GameData data) {
    if (results.isEmpty()) { // can be empty!
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
    if (results.isEmpty()) { // can be empty!
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
    if (results.isEmpty()) { // can be empty!
      return 0.0;
    }
    double count = 0;
    for (final BattleResults result : results) {
      count += result.getDefendingCombatUnitsLeft();
    }
    return count / results.size();
  }

  double getAverageDefendingUnitsLeftWhenDefenderWon() {
    if (results.isEmpty()) { // can be empty!
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
    if (results.isEmpty()) { // can be empty!
      return 0.0;
    }
    double count = 0;
    for (final BattleResults result : results) {
      if (result.attackerWon()) {
        count++;
      }
    }
    return count / results.size();
  }

  double getDefenderWinPercent() {
    if (results.isEmpty()) { // can be empty!
      return 0.0;
    }
    double count = 0;
    for (final BattleResults result : results) {
      if (result.defenderWon()) {
        count++;
      }
    }
    return count / results.size();
  }

  public double getAverageBattleRoundsFought() {
    if (results.isEmpty()) { // can be empty!
      return 0.0;
    }
    double count = 0;
    for (final BattleResults result : results) {
      count += result.getBattleRoundsFought();
    }
    if (count == 0) {
      // If this is a 'fake' aggregate result, return 1.0
      return 1.0;
    }
    return count / results.size();
  }

  double getDrawPercent() {
    if (results.isEmpty()) { // can be empty!
      return 0.0;
    }
    double count = 0;
    for (final BattleResults result : results) {
      if (result.draw()) {
        count++;
      }
    }
    return count / results.size();
  }

  public int getRollCount() {
    return results.size();
  }
}
