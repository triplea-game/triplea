package games.strategy.triplea.delegate.battle.steps.fire.firststrike;

import static games.strategy.triplea.delegate.battle.steps.fire.firststrike.BattleStateBuilder.givenBattleState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.BattleStep.Order;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.BattleStateBuilder.BattleStateVariation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefensiveFirstStrikeTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  void willNotExecuteIfNoDefensiveFirstStrikeAvailable() {
    final BattleState battleState =
        givenBattleState(List.of(BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE));

    final DefensiveFirstStrike defensiveFirstStrike =
        new DefensiveFirstStrike(battleState, battleActions);
    assertThat(defensiveFirstStrike.getAllStepDetails(), is(empty()));

    defensiveFirstStrike.execute(executionStack, delegateBridge);
    verify(executionStack, never()).push(any());
  }

  @ParameterizedTest
  @MethodSource
  void getStep(final List<BattleStateVariation> parameters, final BattleStep.Order stepOrder) {

    final BattleState battleState = givenBattleState(parameters);

    final DefensiveFirstStrike defensiveFirstStrike =
        new DefensiveFirstStrike(battleState, battleActions);
    assertThat(defensiveFirstStrike.getAllStepDetails(), hasSize(3));
    assertThat(defensiveFirstStrike.getOrder(), is(stepOrder));

    defensiveFirstStrike.execute(executionStack, delegateBridge);
    verify(executionStack, times(3)).push(any());
  }

  static List<Arguments> getStep() {
    return List.of(
        Arguments.of(
            List.of(BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE),
            Order.FIRST_STRIKE_DEFENSIVE_REGULAR),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            Order.FIRST_STRIKE_DEFENSIVE_REGULAR),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            Order.FIRST_STRIKE_DEFENSIVE),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            Order.FIRST_STRIKE_DEFENSIVE),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            Order.FIRST_STRIKE_DEFENSIVE),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            Order.FIRST_STRIKE_DEFENSIVE_REGULAR),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            Order.FIRST_STRIKE_DEFENSIVE),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            Order.FIRST_STRIKE_DEFENSIVE));
  }
}
