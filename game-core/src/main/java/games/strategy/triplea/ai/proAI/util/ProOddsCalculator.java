package games.strategy.triplea.ai.proAI.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.data.ProBattleResult;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.CollectionUtils;

/**
 * Pro AI odds calculator.
 */
public class ProOddsCalculator {

  private final IOddsCalculator calc;
  private boolean isCanceled = false;

  public ProOddsCalculator(final IOddsCalculator calc) {
    this.calc = calc;
  }

  public void setData(final GameData data) {
    calc.setGameData(data);
  }

  public void cancelCalcs() {
    calc.cancel();
    isCanceled = true;
  }

  public ProBattleResult estimateAttackBattleResults(final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits) {

    final ProBattleResult result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
    if (result != null) {
      return result;
    }

    // Determine if attackers have no chance
    final double strengthDifference = ProBattleUtils.estimateStrengthDifference(t, attackingUnits, defendingUnits);
    if (strengthDifference < 45) {
      return new ProBattleResult(0, -999, false, new ArrayList<>(), defendingUnits, 1);
    }
    return callBattleCalculator(t, attackingUnits, defendingUnits, bombardingUnits);
  }

  public ProBattleResult estimateDefendBattleResults(final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits) {

    final ProBattleResult result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
    if (result != null) {
      return result;
    }

    // Determine if defenders have no chance
    final double strengthDifference = ProBattleUtils.estimateStrengthDifference(t, attackingUnits, defendingUnits);
    if (strengthDifference > 55) {
      final boolean isLandAndCanOnlyBeAttackedByAir =
          !t.isWater() && !attackingUnits.isEmpty() && attackingUnits.stream().allMatch(Matches.unitIsAir());
      return new ProBattleResult(100 + strengthDifference, 999 + strengthDifference, !isLandAndCanOnlyBeAttackedByAir,
          attackingUnits, new ArrayList<>(), 1);
    }
    return callBattleCalculator(t, attackingUnits, defendingUnits, bombardingUnits);
  }

  public ProBattleResult calculateBattleResults(final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits) {

    final ProBattleResult result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
    if (result != null) {
      return result;
    }
    return callBattleCalculator(t, attackingUnits, defendingUnits, bombardingUnits);
  }

  private static ProBattleResult checkIfNoAttackersOrDefenders(final Territory t, final List<Unit> attackingUnits,
      final List<Unit> defendingUnits) {
    final GameData data = ProData.getData();

    final boolean hasNoDefenders = defendingUnits.stream().noneMatch(Matches.unitIsNotInfrastructure());
    final boolean isLandAndCanOnlyBeAttackedByAir =
        !t.isWater() && !attackingUnits.isEmpty() && attackingUnits.stream().allMatch(Matches.unitIsAir());
    if ((attackingUnits.size() == 0) || (hasNoDefenders && isLandAndCanOnlyBeAttackedByAir)) {
      return new ProBattleResult();
    } else if (hasNoDefenders) {
      return new ProBattleResult(100, 0.1, true, attackingUnits, new ArrayList<>(), 0);
    } else if (Properties.getSubRetreatBeforeBattle(data) && !defendingUnits.isEmpty()
        && defendingUnits.stream().allMatch(Matches.unitIsSub())
        && attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())) {
      return new ProBattleResult();
    }
    return null;
  }


  public ProBattleResult callBattleCalculator(final Territory t, final List<Unit> attackingUnits,
      final List<Unit> defendingUnits, final Set<Unit> bombardingUnits) {
    return callBattleCalculator(t, attackingUnits, defendingUnits, bombardingUnits, false);
  }

  public ProBattleResult callBattleCalculator(final Territory t, final List<Unit> attackingUnits,
      final List<Unit> defendingUnits, final Set<Unit> bombardingUnits, final boolean retreatWhenOnlyAirLeft) {
    final GameData data = ProData.getData();

    if (isCanceled || attackingUnits.isEmpty() || defendingUnits.isEmpty()) {
      return new ProBattleResult();
    }

    final int minArmySize = Math.min(attackingUnits.size(), defendingUnits.size());
    final int runCount = Math.max(16, 100 - minArmySize);
    final PlayerID attacker = attackingUnits.get(0).getOwner();
    final PlayerID defender = defendingUnits.get(0).getOwner();
    if (retreatWhenOnlyAirLeft) {
      calc.setRetreatWhenOnlyAirLeft(true);
    }
    final AggregateResults results = calc.setCalculateDataAndCalculate(attacker, defender,
        t, attackingUnits, defendingUnits, new ArrayList<>(bombardingUnits),
        TerritoryEffectHelper.getEffects(t), runCount);
    if (retreatWhenOnlyAirLeft) {
      calc.setRetreatWhenOnlyAirLeft(false);
    }

    // Find battle result statistics
    final double winPercentage = results.getAttackerWinPercent() * 100;
    final List<Unit> averageAttackersRemaining = results.getAverageAttackingUnitsRemaining();
    final List<Unit> averageDefendersRemaining = results.getAverageDefendingUnitsRemaining();
    final List<Unit> mainCombatAttackers =
        CollectionUtils.getMatches(attackingUnits, Matches.unitCanBeInBattle(true, !t.isWater(), 1, false, true, true));
    final List<Unit> mainCombatDefenders = CollectionUtils.getMatches(defendingUnits,
        Matches.unitCanBeInBattle(false, !t.isWater(), 1, false, true, true));
    double tuvSwing = results.getAverageTuvSwing(attacker, mainCombatAttackers, defender, mainCombatDefenders, data);
    if (Matches.territoryIsNeutralButNotWater().test(t)) { // Set TUV swing for neutrals
      final double attackingUnitValue = TuvUtils.getTuv(mainCombatAttackers, ProData.unitValueMap);
      final double remainingUnitValue =
          results.getAverageTuvOfUnitsLeftOver(ProData.unitValueMap, ProData.unitValueMap).getFirst();
      tuvSwing = remainingUnitValue - attackingUnitValue;
    }
    final List<Unit> defendingTransportedUnits =
        CollectionUtils.getMatches(defendingUnits, Matches.unitIsBeingTransported());
    if (t.isWater() && !defendingTransportedUnits.isEmpty()) { // Add TUV swing for transported units
      final double transportedUnitValue = TuvUtils.getTuv(defendingTransportedUnits, ProData.unitValueMap);
      tuvSwing += (transportedUnitValue * winPercentage) / 100;
    }

    // Create battle result object
    final List<Territory> territoryList = new ArrayList<>();
    territoryList.add(t);
    return (!territoryList.isEmpty() && territoryList.stream().allMatch(Matches.territoryIsLand()))
        ? new ProBattleResult(winPercentage, tuvSwing,
            averageAttackersRemaining.stream().anyMatch(Matches.unitIsLand()), averageAttackersRemaining,
            averageDefendersRemaining, results.getAverageBattleRoundsFought())
        : new ProBattleResult(winPercentage, tuvSwing, !averageAttackersRemaining.isEmpty(),
            averageAttackersRemaining, averageDefendersRemaining, results.getAverageBattleRoundsFought());
  }

}
