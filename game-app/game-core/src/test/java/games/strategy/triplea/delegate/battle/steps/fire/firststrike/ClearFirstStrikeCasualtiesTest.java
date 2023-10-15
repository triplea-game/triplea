package games.strategy.triplea.delegate.battle.steps.fire.firststrike;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.steps.fire.firststrike.BattleStateBuilder.givenBattleState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.BattleStateBuilder.BattleStateVariation;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClearFirstStrikeCasualtiesTest {

  @Mock BattleActions battleActions;
  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;

  @ParameterizedTest
  @MethodSource
  void getStep(final List<BattleStateVariation> parameters, final List<BattleState.Side> sides) {

    final BattleState battleState = givenBattleState(parameters);

    final ClearFirstStrikeCasualties clearFirstStrikeCasualties =
        new ClearFirstStrikeCasualties(battleState, battleActions);

    assertThat(clearFirstStrikeCasualties.getAllStepDetails(), hasSize(sides.isEmpty() ? 0 : 1));

    clearFirstStrikeCasualties.execute(executionStack, delegateBridge);

    switch (sides.size()) {
      case 0:
      default:
        verify(battleActions, never()).clearWaitingToDieAndDamagedChangesInto(any(), any());
        break;
      case 1:
        verify(battleActions)
            .clearWaitingToDieAndDamagedChangesInto(eq(delegateBridge), eq(sides.get(0)));
        break;
      case 2:
        verify(battleActions)
            .clearWaitingToDieAndDamagedChangesInto(
                eq(delegateBridge), eq(sides.get(0)), eq(sides.get(1)));
        break;
    }
  }

  static List<Arguments> getStep() {
    return List.of(
        Arguments.of(
            List.of(BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE), List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            List.of(DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of()),
        Arguments.of(List.of(BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE), List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            List.of(OFFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(OFFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(OFFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE, DEFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENSE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of()));
  }
}
