package org.triplea.ai.flowfield.odds;

import static org.triplea.ai.flowfield.Constants.LANCHESTER_ATTRITION_FACTOR;

import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import java.util.Collection;
import java.util.List;
import lombok.Value;

@Value
public class BattleDetails {
  public static final BattleDetails EMPTY_DETAILS = new BattleDetails();
  Collection<Unit> enemyUnits;
  Collection<Unit> allyUnits;
  double value;
  Collection<TerritoryEffect> territoryEffects;

  private BattleDetails() {
    this.enemyUnits = List.of();
    this.allyUnits = List.of();
    this.value = 0;
    this.territoryEffects = List.of();
  }

  public BattleDetails(
      final Collection<Unit> allyUnits,
      final Collection<Unit> enemyUnits,
      final CombatValueBuilder.MainBuilder offenseCombatBuilder,
      final CombatValueBuilder.MainBuilder defenseCombatBuilder,
      final Collection<TerritoryEffect> territoryEffects) {
    this.enemyUnits = enemyUnits;
    this.allyUnits = allyUnits;
    this.territoryEffects = territoryEffects;

    final double offensePower =
        new LanchesterBattleCalculator.LanchesterDetails(
                PowerStrengthAndRolls.build(
                    this.allyUnits,
                    offenseCombatBuilder
                        .friendlyUnits(this.allyUnits)
                        .enemyUnits(List.of())
                        .territoryEffects(territoryEffects)
                        .build()),
                LANCHESTER_ATTRITION_FACTOR)
            .getInitialTotalPower();
    final double defensePower =
        new LanchesterBattleCalculator.LanchesterDetails(
                PowerStrengthAndRolls.build(
                    this.enemyUnits,
                    defenseCombatBuilder
                        .friendlyUnits(this.enemyUnits)
                        .enemyUnits(List.of())
                        .territoryEffects(territoryEffects)
                        .build()),
                LANCHESTER_ATTRITION_FACTOR)
            .getInitialTotalPower();
    this.value = defensePower - offensePower;
  }

  public boolean isEmpty() {
    return this.allyUnits.isEmpty() && this.enemyUnits.isEmpty();
  }
}
