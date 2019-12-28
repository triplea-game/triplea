package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.IBattleCalculator;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.triplea.java.collections.CollectionUtils;

/** Pro AI odds calculator. */
public class ProOddsCalculator {

  private final IBattleCalculator calc;
  private boolean isCanceled = false;

  public ProOddsCalculator(final IBattleCalculator calc) {
    this.calc = calc;
  }

  public void setData(final GameData data) {
    calc.setGameData(data);
  }

  public void cancelCalcs() {
    calc.cancel();
    isCanceled = true;
  }

  /**
   * Simulates the specified battle. Prior to the simulation, an estimate is made of the attacker's
   * chance to win the battle. If the estimate indicates the attacker has almost no chance to win,
   * the simulation is not performed, and an appropriate result indicating the defender's success is
   * returned.
   */
  public ProBattleResult estimateAttackBattleResults(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits) {

    final ProBattleResult result =
        checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits, true);
    if (result != null) {
      return result;
    }

    // Determine if attackers have no chance
    final double strengthDifference =
        ProBattleUtils.estimateStrengthDifference(t, attackingUnits, defendingUnits);
    if (strengthDifference < 45) {
      return new ProBattleResult(0, -999, false, new ArrayList<>(), defendingUnits, 1);
    }
    return callBattleCalc(t, attackingUnits, defendingUnits, bombardingUnits);
  }

  /**
   * Simulates the specified battle. Prior to the simulation, an estimate is made of the defender's
   * chance to win the battle. If the estimate indicates the defender has almost no chance to win,
   * the simulation is not performed, and an appropriate result indicating the attacker's success is
   * returned.
   */
  public ProBattleResult estimateDefendBattleResults(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits) {

    final ProBattleResult result =
        checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits, true);
    if (result != null) {
      return result;
    }

    // Determine if defenders have no chance
    final double strengthDifference =
        ProBattleUtils.estimateStrengthDifference(t, attackingUnits, defendingUnits);
    if (strengthDifference > 55) {
      final boolean isLandAndCanOnlyBeAttackedByAir =
          !t.isWater()
              && !attackingUnits.isEmpty()
              && attackingUnits.stream().allMatch(Matches.unitIsAir());
      return new ProBattleResult(
          100 + strengthDifference,
          999 + strengthDifference,
          !isLandAndCanOnlyBeAttackedByAir,
          attackingUnits,
          new ArrayList<>(),
          1);
    }
    return callBattleCalc(t, attackingUnits, defendingUnits, bombardingUnits);
  }

  public ProBattleResult calculateBattleResultsNoSubmerge(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits) {
    return calculateBattleResults(t, attackingUnits, defendingUnits, bombardingUnits, false);
  }

  public ProBattleResult calculateBattleResults(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits) {
    return calculateBattleResults(t, attackingUnits, defendingUnits, bombardingUnits, true);
  }

  private ProBattleResult calculateBattleResults(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits,
      final boolean checkSubmerge) {

    final ProBattleResult result =
        checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits, checkSubmerge);
    if (result != null) {
      return result;
    }
    return callBattleCalc(t, attackingUnits, defendingUnits, bombardingUnits, checkSubmerge);
  }

  private static ProBattleResult checkIfNoAttackersOrDefenders(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final boolean checkSubmerge) {
    final boolean hasNoDefenders =
        defendingUnits.stream().noneMatch(Matches.unitIsNotInfrastructure());
    final boolean isLandAndCanOnlyBeAttackedByAir =
        !t.isWater()
            && !attackingUnits.isEmpty()
            && attackingUnits.stream().allMatch(Matches.unitIsAir());
    if (attackingUnits.isEmpty() || (hasNoDefenders && isLandAndCanOnlyBeAttackedByAir)) {
      return new ProBattleResult();
    } else if (hasNoDefenders) {
      final List<Unit> mainCombatDefenders =
          CollectionUtils.getMatches(
              defendingUnits, Matches.unitCanBeInBattle(false, !t.isWater(), 1, true));
      final double tuv = TuvUtils.getTuv(mainCombatDefenders, ProData.unitValueMap);
      return new ProBattleResult(100, 0.1 + tuv, true, attackingUnits, new ArrayList<>(), 0);
    } else if (canSubmergeBeforeBattle(attackingUnits, defendingUnits, checkSubmerge)) {
      return new ProBattleResult();
    }
    return null;
  }

  private static boolean canSubmergeBeforeBattle(
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final boolean checkSubmerge) {
    final GameData data = ProData.getData();
    return checkSubmerge
        && Properties.getSubRetreatBeforeBattle(data)
        && defendingUnits.stream().allMatch(Matches.unitCanEvade())
        && attackingUnits.stream().noneMatch(Matches.unitIsDestroyer());
  }

  public ProBattleResult callBattleCalcWithRetreatAir(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits) {
    return callBattleCalc(t, attackingUnits, defendingUnits, bombardingUnits, true, true);
  }

  public ProBattleResult callBattleCalc(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits) {
    return callBattleCalc(t, attackingUnits, defendingUnits, bombardingUnits, true);
  }

  private ProBattleResult callBattleCalc(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits,
      final boolean checkSubmerge) {
    return callBattleCalc(t, attackingUnits, defendingUnits, bombardingUnits, checkSubmerge, false);
  }

  /** Simulates the specified battle. */
  private ProBattleResult callBattleCalc(
      final Territory t,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits,
      final boolean checkSubmerge,
      final boolean retreatWhenOnlyAirLeft) {
    final GameData data = ProData.getData();

    if (isCanceled || attackingUnits.isEmpty() || defendingUnits.isEmpty()) {
      return new ProBattleResult();
    }

    final int minArmySize = Math.min(attackingUnits.size(), defendingUnits.size());
    final int runCount = Math.max(16, 100 - minArmySize);
    final GamePlayer attacker = attackingUnits.iterator().next().getOwner();
    final GamePlayer defender = defendingUnits.iterator().next().getOwner();
    if (retreatWhenOnlyAirLeft) {
      calc.setRetreatWhenOnlyAirLeft(true);
    }
    final AggregateResults results =
        calc.setCalculateDataAndCalculate(
            attacker,
            defender,
            t,
            attackingUnits,
            defendingUnits,
            new ArrayList<>(bombardingUnits),
            TerritoryEffectHelper.getEffects(t),
            runCount);
    if (retreatWhenOnlyAirLeft) {
      calc.setRetreatWhenOnlyAirLeft(false);
    }

    // Find battle result statistics
    final double winPercentage = results.getAttackerWinPercent() * 100;
    final Collection<Unit> averageAttackersRemaining = results.getAverageAttackingUnitsRemaining();
    final Collection<Unit> averageDefendersRemaining = results.getAverageDefendingUnitsRemaining();
    final List<Unit> mainCombatAttackers =
        CollectionUtils.getMatches(
            attackingUnits, Matches.unitCanBeInBattle(true, !t.isWater(), 1, true));
    final List<Unit> mainCombatDefenders =
        CollectionUtils.getMatches(
            defendingUnits, Matches.unitCanBeInBattle(false, !t.isWater(), 1, true));
    double tuvSwing =
        results.getAverageTuvSwing(
            attacker, mainCombatAttackers, defender, mainCombatDefenders, data);

    // Set TUV swing for neutrals
    if (Matches.territoryIsNeutralButNotWater().test(t)) {
      final double attackingUnitValue = TuvUtils.getTuv(mainCombatAttackers, ProData.unitValueMap);
      final double remainingUnitValue =
          results
              .getAverageTuvOfUnitsLeftOver(ProData.unitValueMap, ProData.unitValueMap)
              .getFirst();
      tuvSwing = remainingUnitValue - attackingUnitValue;
    }

    // Add TUV swing for transported units
    final List<Unit> defendingTransportedUnits =
        CollectionUtils.getMatches(defendingUnits, Matches.unitIsBeingTransported());
    if (t.isWater() && !defendingTransportedUnits.isEmpty()) {
      final double transportedUnitValue =
          TuvUtils.getTuv(defendingTransportedUnits, ProData.unitValueMap);
      tuvSwing += transportedUnitValue * winPercentage / 100;
    }

    // Remove TUV and add to remaining units for defenders that can submerge before battle
    if (tuvSwing > 0 && canSubmergeBeforeBattle(attackingUnits, defendingUnits, checkSubmerge)) {
      final List<Unit> defendingSubsKilled =
          CollectionUtils.getMatches(defendingUnits, Matches.unitCanEvade());
      defendingSubsKilled.removeAll(averageDefendersRemaining);
      averageDefendersRemaining.addAll(defendingSubsKilled);
      final int subTuv = TuvUtils.getTuv(defendingSubsKilled, ProData.unitValueMap);
      tuvSwing = Math.max(0, tuvSwing - subTuv);
    }

    // Create battle result object
    final List<Territory> territoryList = new ArrayList<>();
    territoryList.add(t);
    return (!territoryList.isEmpty() && territoryList.stream().allMatch(Matches.territoryIsLand()))
        ? new ProBattleResult(
            winPercentage,
            tuvSwing,
            averageAttackersRemaining.stream().anyMatch(Matches.unitIsLand()),
            averageAttackersRemaining,
            averageDefendersRemaining,
            results.getAverageBattleRoundsFought())
        : new ProBattleResult(
            winPercentage,
            tuvSwing,
            !averageAttackersRemaining.isEmpty(),
            averageAttackersRemaining,
            averageDefendersRemaining,
            results.getAverageBattleRoundsFought());
  }
}
