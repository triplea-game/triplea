package games.strategy.triplea.delegate.battle.steps.fire.firststrike;

import static games.strategy.triplea.delegate.battle.steps.fire.firststrike.BattleStateBuilder.givenBattleState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
  void getStep(
      final List<BattleStateVariation> parameters,
      final boolean willExecute,
      final boolean clearAttackingDead,
      final boolean clearDefendingDead) {

    final BattleState battleState = spy(givenBattleState(parameters));
    if (clearAttackingDead) {
      doNothing().when(battleState).clearAttackingWaitingToDie();
    }
    if (clearDefendingDead) {
      doNothing().when(battleState).clearDefendingWaitingToDie();
    }

    final ClearFirstStrikeCasualties clearFirstStrikeCasualties =
        new ClearFirstStrikeCasualties(battleState, battleActions);
    final List<String> names = clearFirstStrikeCasualties.getNames();

    assertThat(names, hasSize(willExecute ? 1 : 0));

    clearFirstStrikeCasualties.execute(executionStack, delegateBridge);

    verify(battleActions, times(willExecute ? 1 : 0))
        .remove(anyCollection(), eq(delegateBridge), any(), eq(null));
    verify(battleState, times(clearAttackingDead ? 1 : 0)).getAttackingWaitingToDie();
    verify(battleState, times(clearAttackingDead ? 1 : 0)).clearAttackingWaitingToDie();
    verify(battleState, times(clearDefendingDead ? 1 : 0)).getDefendingWaitingToDie();
    verify(battleState, times(clearDefendingDead ? 1 : 0)).clearDefendingWaitingToDie();
  }

  static List<Arguments> getStep() {
    return List.of(
        Arguments.of(List.of(BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE), true, true, true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            true,
            false,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            true,
            false,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            false,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            false,
            false,
            false),
        Arguments.of(List.of(BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE), false, false, false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            true,
            true,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            true,
            true,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            true,
            false,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            true,
            true,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            true),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            false,
            false,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            true,
            true,
            false),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            false,
            false,
            false));
  }
}
