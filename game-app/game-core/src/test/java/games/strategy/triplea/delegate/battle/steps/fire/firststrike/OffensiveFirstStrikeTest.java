package games.strategy.triplea.delegate.battle.steps.fire.firststrike;

import static games.strategy.triplea.Constants.ALLIED_AIR_INDEPENDENT;
import static games.strategy.triplea.delegate.battle.steps.fire.firststrike.BattleStateBuilder.givenBattleState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
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
class OffensiveFirstStrikeTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  void willNotExecuteIfNoOffensiveFirstStrikeAvailable() {
    final BattleState battleState =
        givenBattleState(List.of(BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE));

    final OffensiveFirstStrike offensiveFirstStrike =
        new OffensiveFirstStrike(battleState, battleActions);
    assertThat(offensiveFirstStrike.getAllStepDetails(), is(empty()));

    offensiveFirstStrike.execute(executionStack, delegateBridge);
    verify(executionStack, never()).push(any());
  }

  @ParameterizedTest
  @MethodSource
  void getStep(final List<BattleStateVariation> parameters, final Order stepOrder) {

    final BattleState battleState = givenBattleState(parameters);
    when(battleState.getGameData().getProperties().get(ALLIED_AIR_INDEPENDENT, false))
        .thenReturn(true);

    final OffensiveFirstStrike offensiveFirstStrike =
        new OffensiveFirstStrike(battleState, battleActions);
    assertThat(offensiveFirstStrike.getAllStepDetails(), hasSize(3));
    assertThat(offensiveFirstStrike.getOrder(), is(stepOrder));

    offensiveFirstStrike.execute(executionStack, delegateBridge);
    verify(executionStack, times(3)).push(any());
  }

  static List<Arguments> getStep() {
    return List.of(
        Arguments.of(
            List.of(BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE), Order.FIRST_STRIKE_OFFENSIVE),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            Order.FIRST_STRIKE_OFFENSIVE_REGULAR),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            Order.FIRST_STRIKE_OFFENSIVE),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            Order.FIRST_STRIKE_OFFENSIVE));
  }
}
