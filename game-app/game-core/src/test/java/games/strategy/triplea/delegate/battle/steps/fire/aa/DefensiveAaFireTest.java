package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsCombatAa;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefensiveAaFireTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;

  @Nested
  class GetNames {
    @Test
    void hasNamesIfAaIsAvailable() {
      final Unit targetUnit = givenAnyUnit();
      final Unit aaUnit = givenUnitIsCombatAa(Set.of(targetUnit.getType()), defender, DEFENSE);
      when(aaUnit.getOwner()).thenReturn(defender);
      final BattleState battleState =
          givenBattleStateBuilder()
              .gameData(givenGameData().withWarRelationship(attacker, defender, true).build())
              .attacker(attacker)
              .defender(defender)
              .attackingUnits(List.of(targetUnit))
              .defendingUnits(List.of(aaUnit))
              .build();
      final DefensiveAaFire defensiveAaFire = new DefensiveAaFire(battleState, battleActions);
      assertThat(defensiveAaFire.getAllStepDetails(), hasSize(3));
    }

    @Test
    void hasNoNamesIfNoAaIsAvailable() {
      final BattleState battleState = givenBattleStateBuilder().defendingUnits(List.of()).build();
      final DefensiveAaFire defensiveAaFire = new DefensiveAaFire(battleState, battleActions);
      assertThat(defensiveAaFire.getAllStepDetails(), is(empty()));
    }
  }

  @Nested
  class FireAa {
    @Test
    void firedIfAaAreAvailable() {
      final Unit targetUnit = givenAnyUnit();
      final Unit aaUnit = givenUnitIsCombatAa(Set.of(targetUnit.getType()), defender, DEFENSE);
      when(aaUnit.getOwner()).thenReturn(defender);
      final DefensiveAaFire defensiveAaFire =
          new DefensiveAaFire(
              givenBattleStateBuilder()
                  .gameData(givenGameData().withWarRelationship(attacker, defender, true).build())
                  .attacker(attacker)
                  .defender(defender)
                  .attackingUnits(List.of(targetUnit))
                  .defendingUnits(List.of(aaUnit))
                  .build(),
              battleActions);

      defensiveAaFire.execute(executionStack, delegateBridge);

      verify(executionStack, times(3)).push(any());
    }

    @Test
    void notFiredIfNoAaAreAvailable() {
      final DefensiveAaFire defensiveAaFire =
          new DefensiveAaFire(
              givenBattleStateBuilder().defendingUnits(List.of()).build(), battleActions);

      defensiveAaFire.execute(executionStack, delegateBridge);

      verify(executionStack, never()).push(any());
    }
  }
}
