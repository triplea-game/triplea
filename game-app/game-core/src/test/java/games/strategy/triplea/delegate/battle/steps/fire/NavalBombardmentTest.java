package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenSeaBattleSite;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.sound.ISound;

@ExtendWith(MockitoExtension.class)
class NavalBombardmentTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  @DisplayName("Has bombardment units and first round")
  void bombardmentHappensIfHasBombardmentUnitsAndIsFirstRound() {
    final UnitType unitType = spy(new UnitType("type", givenGameData().build()));
    when(unitType.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(mock(UnitAttachment.class));
    final Unit bombarder = spy(unitType.createTemp(1, mock(GamePlayer.class)).get(0));

    when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(mock(ISound.class));

    final BattleState battleState =
        givenBattleStateBuilder()
            .bombardingUnits(List.of(bombarder))
            .defendingUnits(List.of(givenAnyUnit()))
            .battleRound(1)
            .build();

    final NavalBombardment navalBombardment = new NavalBombardment(battleState, battleActions);
    assertThat(navalBombardment.getAllStepDetails(), hasSize(3));

    navalBombardment.execute(executionStack, delegateBridge);
    verify(executionStack, times(3)).push(any());
  }

  @Test
  void bombardmentDoesNotHappenIfNotFirstRound() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .bombardingUnits(List.of(mock(Unit.class)))
            .defendingUnits(List.of(givenAnyUnit()))
            .battleRound(2)
            .build();
    final NavalBombardment navalBombardment = new NavalBombardment(battleState, battleActions);
    assertThat(navalBombardment.getAllStepDetails(), is(empty()));
    navalBombardment.execute(executionStack, delegateBridge);
    verify(executionStack, never()).push(any());
  }

  @Test
  void bombardmentDoesNotHappenIfNoBombardmentUnitsAndFirstRound() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .bombardingUnits(List.of())
            .defendingUnits(List.of(givenAnyUnit()))
            .battleRound(1)
            .build();
    final NavalBombardment navalBombardment = new NavalBombardment(battleState, battleActions);
    assertThat(navalBombardment.getAllStepDetails(), is(empty()));
    navalBombardment.execute(executionStack, delegateBridge);
    verify(executionStack, never()).push(any());
  }

  @Test
  void bombardmentDoesNotHappenIfSeaBattle() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .bombardingUnits(List.of(mock(Unit.class)))
            .defendingUnits(List.of(givenAnyUnit()))
            .battleRound(1)
            .battleSite(givenSeaBattleSite())
            .build();
    final NavalBombardment navalBombardment = new NavalBombardment(battleState, battleActions);
    assertThat(navalBombardment.getAllStepDetails(), is(empty()));
    navalBombardment.execute(executionStack, delegateBridge);
    verify(executionStack, never()).push(any());
  }
}
