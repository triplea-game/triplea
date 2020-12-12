package org.triplea.ai.flowfield.odds;

import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.power.calculator.TotalPowerAndTotalRolls;
import lombok.Value;

/**
 * Calculates which side wins and how many units will be left on the winning side
 *
 * <p>It is an approximate calculation based on Lanchester's Laws
 * (https://en.wikipedia.org/wiki/Lanchester's_laws)
 *
 * <p>TODO FLOWFIELD AI: add AA and First Strike
 */
@Value
public class LanchesterBattleCalculator {

  BattleState.Side won;
  long remainingUnits;

  public LanchesterBattleCalculator(
      final TotalPowerAndTotalRolls offense,
      final TotalPowerAndTotalRolls defense,
      final double attritionOrder) {
    final double avgOffensePower =
        offense.getActiveUnits().stream()
            .mapToDouble(
                unit ->
                    unit.getPower()
                        * (unit.getUnit().getUnitAttachment().getHitPoints()
                            - unit.getUnit().getHits()))
            .average()
            .orElse(0.0);
    final double avgDefensePower =
        defense.getActiveUnits().stream()
            .mapToDouble(
                unit ->
                    unit.getPower()
                        * (unit.getUnit().getUnitAttachment().getHitPoints()
                            - unit.getUnit().getHits()))
            .average()
            .orElse(0.0);

    final double initialOffense =
        avgOffensePower * Math.pow(offense.getActiveUnits().size(), attritionOrder);
    final double initialDefense =
        avgDefensePower * Math.pow(defense.getActiveUnits().size(), attritionOrder);

    if (initialOffense > initialDefense) {
      won = BattleState.Side.OFFENSE;
      remainingUnits =
          Math.round(
              Math.pow((initialOffense - initialDefense) / avgOffensePower, 1 / attritionOrder));
    } else {
      won = BattleState.Side.DEFENSE;
      remainingUnits =
          Math.round(
              Math.pow((initialDefense - initialOffense) / avgDefensePower, 1 / attritionOrder));
    }
  }
}
