package games.strategy.triplea.delegate.battle.steps.retreat.sub;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenSeaUnitCanEvadeAndCanNotBeTargetedByRandomUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmergeSubsVsOnlyAirStepTest extends AbstractClientSettingTestCase {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @ParameterizedTest(name = "[{index}] {0} is {2}")
  @MethodSource
  void stepName(final String displayName, final BattleState battleState, final boolean expected) {
    final SubmergeSubsVsOnlyAirStep submergeSubsVsOnlyAirStep =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    assertThat(submergeSubsVsOnlyAirStep.getAllStepDetails(), hasSize(expected ? 1 : 0));
  }

  static List<Arguments> stepName() {
    return List.of(
        Arguments.of(
            "Attacking evaders vs NO air",
            givenBattleStateBuilder()
                // if there is no air, it doesn't check if the attacker is an evader
                .attackingUnits(List.of(givenAnyUnit()))
                .defendingUnits(List.of(givenAnyUnit(), mock(Unit.class)))
                .build(),
            false),
        Arguments.of(
            "Defending evaders vs NO air",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenAnyUnit(), mock(Unit.class)))
                // if there is no air, it doesn't check if the defender is an evader
                .defendingUnits(List.of(givenAnyUnit()))
                .build(),
            false),
        Arguments.of(
            "Attacking evaders vs SOME air",
            givenBattleStateBuilder()
                // if there is some but not all air, it doesn't check if the attacker is an evader
                .attackingUnits(List.of(givenAnyUnit()))
                .defendingUnits(List.of(givenUnitIsAir(), givenAnyUnit()))
                .build(),
            false),
        Arguments.of(
            "Defending evaders vs SOME air",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitIsAir(), givenAnyUnit()))
                // if there is some but not all air, it doesn't check if the defender is an evader
                .defendingUnits(List.of(givenAnyUnit()))
                .build(),
            false),
        Arguments.of(
            "Attacking evaders vs ALL air",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenSeaUnitCanEvadeAndCanNotBeTargetedByRandomUnit()))
                .defendingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .build(),
            true),
        Arguments.of(
            "Defending evaders vs ALL air",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .defendingUnits(List.of(givenSeaUnitCanEvadeAndCanNotBeTargetedByRandomUnit()))
                .build(),
            true));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource
  void submergeUnits(
      final String displayName,
      final BattleState battleState,
      final List<Unit> expectedSubmergingSubs,
      final BattleState.Side expectedSide) {

    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(mock(IDisplay.class));
    when(delegateBridge.getHistoryWriter()).thenReturn(mock(IDelegateHistoryWriter.class));

    final BattleState battleStateSpy = spy(battleState);

    final SubmergeSubsVsOnlyAirStep submergeSubsVsOnlyAirStep =
        new SubmergeSubsVsOnlyAirStep(battleStateSpy, battleActions);

    submergeSubsVsOnlyAirStep.execute(executionStack, delegateBridge);

    verify(battleStateSpy).retreatUnits(expectedSide, expectedSubmergingSubs);
  }

  static List<Arguments> submergeUnits() {
    final UnitType unitType = mock(UnitType.class);
    final UnitAttachment unitAttachment = mock(UnitAttachment.class);
    when(unitType.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment);
    when(unitAttachment.getCanEvade()).thenReturn(true);
    when(unitAttachment.getCanNotBeTargetedBy()).thenReturn(Set.of(mock(UnitType.class)));
    final Unit sub = new Unit(unitType, mock(GamePlayer.class), givenGameData().build());
    return List.of(
        Arguments.of(
            "Attacking subs submerge",
            givenBattleStateBuilder()
                .attackingUnits(List.of(sub, givenAnyUnit()))
                .defendingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .build(),
            List.of(sub),
            OFFENSE),
        Arguments.of(
            "Defending subs submerge",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .defendingUnits(List.of(sub, givenAnyUnit()))
                .build(),
            List.of(sub),
            DEFENSE));
  }

  @Test
  void noAttackingEvadersNoSubmerge() {

    final BattleState battleStateSpy =
        spy(
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenAnyUnit()))
                .defendingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .build());

    final SubmergeSubsVsOnlyAirStep submergeSubsVsOnlyAirStep =
        new SubmergeSubsVsOnlyAirStep(battleStateSpy, battleActions);

    submergeSubsVsOnlyAirStep.execute(executionStack, delegateBridge);

    verify(battleStateSpy, never()).retreatUnits(any(), any());
  }

  @Test
  void noDefendingEvadersNoSubmerge() {

    final BattleState battleStateSpy =
        spy(
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .defendingUnits(List.of(givenAnyUnit()))
                .build());

    final SubmergeSubsVsOnlyAirStep submergeSubsVsOnlyAirStep =
        new SubmergeSubsVsOnlyAirStep(battleStateSpy, battleActions);

    submergeSubsVsOnlyAirStep.execute(executionStack, delegateBridge);

    verify(battleStateSpy, never()).retreatUnits(any(), any());
  }
}
