package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenSeaBattleSite;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NavalBombardmentTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  @DisplayName("Has bombardment units and first round")
  void bombardmentHappensIfHasBombardmentUnitsAndIsFirstRound() {
    final BattleState battleState =
        givenBattleStateBuilder().bombardingUnits(List.of(mock(Unit.class))).battleRound(1).build();
    final NavalBombardment navalBombardment = new NavalBombardment(battleState, battleActions);
    assertThat(navalBombardment.getNames(), hasSize(2));
    navalBombardment.execute(executionStack, delegateBridge);
    verify(battleActions).fireNavalBombardment(delegateBridge);
  }

  @Test
  void bombardmentDoesNotHappenIfNotFirstRound() {
    final BattleState battleState =
        givenBattleStateBuilder().bombardingUnits(List.of(mock(Unit.class))).battleRound(2).build();
    final NavalBombardment navalBombardment = new NavalBombardment(battleState, battleActions);
    assertThat(navalBombardment.getNames(), is(empty()));
    navalBombardment.execute(executionStack, delegateBridge);
    verify(battleActions, never()).fireNavalBombardment(delegateBridge);
  }

  @Test
  void bombardmentDoesNotHappenIfNoBombardmentUnitsAndFirstRound() {
    final BattleState battleState =
        givenBattleStateBuilder().bombardingUnits(List.of()).battleRound(1).build();
    final NavalBombardment navalBombardment = new NavalBombardment(battleState, battleActions);
    assertThat(navalBombardment.getNames(), is(empty()));
    navalBombardment.execute(executionStack, delegateBridge);
    verify(battleActions, never()).fireNavalBombardment(delegateBridge);
  }

  @Test
  void bombardmentDoesNotHappenIfSeaBattle() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .bombardingUnits(List.of(mock(Unit.class)))
            .battleRound(1)
            .battleSite(givenSeaBattleSite())
            .build();
    final NavalBombardment navalBombardment = new NavalBombardment(battleState, battleActions);
    assertThat(navalBombardment.getNames(), is(empty()));
    navalBombardment.execute(executionStack, delegateBridge);
    verify(battleActions, never()).fireNavalBombardment(delegateBridge);
  }
}
