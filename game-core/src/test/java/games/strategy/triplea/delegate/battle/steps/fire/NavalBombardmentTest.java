package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenSeaBattleSite;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NavalBombardmentTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @ParameterizedTest(name = "[{index}] {0} is {2}")
  @MethodSource
  void testWhatIsValid(
      final String displayName, final BattleState battleState, final boolean expected) {
    final NavalBombardment underTest = new NavalBombardment(battleState, battleActions);
    assertThat(underTest.valid(), is(expected));
    if (expected) {
      assertThat(underTest.getNames(), hasSize(2));
      underTest.execute(executionStack, delegateBridge);
      verify(battleActions).fireNavalBombardment(delegateBridge);
    } else {
      assertThat(underTest.getNames(), hasSize(0));
      underTest.execute(executionStack, delegateBridge);
      verify(battleActions, never()).fireNavalBombardment(delegateBridge);
    }
  }

  static List<Arguments> testWhatIsValid() {
    return List.of(
        Arguments.of(
            "Has bombardment units and first round",
            givenBattleStateBuilder().bombardingUnits(List.of(givenAnyUnit())).round(1).build(),
            true),
        Arguments.of(
            "Has bombardment units and subsequent round",
            givenBattleStateBuilder().bombardingUnits(List.of(givenAnyUnit())).round(2).build(),
            false),
        Arguments.of(
            "Has no bombardment units and first round",
            givenBattleStateBuilder().bombardingUnits(List.of()).round(1).build(),
            false),
        Arguments.of(
            "Is impossible sea battle with bombardment",
            givenBattleStateBuilder()
                .bombardingUnits(List.of(givenAnyUnit()))
                .round(1)
                .battleSite(givenSeaBattleSite())
                .build(),
            false));
  }
}
