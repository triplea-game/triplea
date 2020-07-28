package games.strategy.triplea.delegate.battle.steps.fire.firststrike;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveSubsRetreatTest.MockGameData;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
class BattleStateBuilder {

  enum BattleStateVariation {
    HAS_ATTACKING_FIRST_STRIKE,
    HAS_DEFENDING_FIRST_STRIKE,
    HAS_ATTACKING_DESTROYER,
    HAS_DEFENDING_DESTROYER,
    HAS_WW2V2,
    HAS_DEFENDING_SUBS_SNEAK_ATTACK
  }

  static BattleState givenBattleState(final List<BattleStateVariation> parameters) {
    final List<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.add(givenAnyUnit());
    if (parameters.contains(BattleStateVariation.HAS_ATTACKING_DESTROYER)) {
      attackingUnits.add(givenUnitDestroyer());
    }
    if (parameters.contains(BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE)) {
      attackingUnits.add(givenUnitFirstStrike());
    }

    final List<Unit> defendingUnits = new ArrayList<>();
    defendingUnits.add(givenAnyUnit());
    if (parameters.contains(BattleStateVariation.HAS_DEFENDING_DESTROYER)) {
      defendingUnits.add(givenUnitDestroyer());
    }
    if (parameters.contains(BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE)) {
      defendingUnits.add(givenUnitFirstStrike());
    }

    final MockGameData gameData = MockGameData.givenGameData();
    gameData.withWW2V2(parameters.contains(BattleStateVariation.HAS_WW2V2));
    if (parameters.contains(BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK)) {
      gameData.withDefendingSubsSneakAttack(true);
    }

    return givenBattleStateBuilder()
        .attackingUnits(attackingUnits)
        .defendingUnits(defendingUnits)
        .gameData(gameData.build())
        .build();
  }
}
