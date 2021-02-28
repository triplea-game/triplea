package org.triplea.ai.flowfield.odds;

import com.google.common.base.Preconditions;
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
      final double attritionFactor) {
    final LanchesterDetails offenseDetails = new LanchesterDetails(offense, attritionFactor);
    final LanchesterDetails defenseDetails = new LanchesterDetails(defense, attritionFactor);

    if (offenseDetails.initialTotalPower > defenseDetails.initialTotalPower) {
      won = BattleState.Side.OFFENSE;
      remainingUnits = offenseDetails.calculateRemainingUnits(defenseDetails);
    } else {
      won = BattleState.Side.DEFENSE;
      remainingUnits = defenseDetails.calculateRemainingUnits(offenseDetails);
    }
  }

  @Value
  static class LanchesterDetails {
    double avgPower;
    double initialTotalPower;
    double attritionFactor;

    LanchesterDetails(final TotalPowerAndTotalRolls powerCalculator, final double attritionFactor) {
      this.attritionFactor = attritionFactor;
      avgPower =
          powerCalculator.getActiveUnits().stream()
              .mapToDouble(
                  unit ->
                      unit.getPower()
                          * (unit.getUnit().getUnitAttachment().getHitPoints()
                              - unit.getUnit().getHits()))
              .average()
              .orElse(0.0);

      initialTotalPower =
          avgPower * Math.pow(powerCalculator.getActiveUnits().size(), this.attritionFactor);
    }

    long calculateRemainingUnits(final LanchesterDetails other) {
      Preconditions.checkArgument(
          other.attritionFactor == this.attritionFactor,
          "Both LanchesterDetails need the same attrition factor");
      return Math.round(
          Math.pow((initialTotalPower - other.initialTotalPower) / avgPower, 1 / attritionFactor));
    }
  }
}
