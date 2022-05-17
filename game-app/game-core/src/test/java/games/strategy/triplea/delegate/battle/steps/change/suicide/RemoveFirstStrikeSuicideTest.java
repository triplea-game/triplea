package games.strategy.triplea.delegate.battle.steps.change.suicide;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrikeSuicideOnAttack;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrikeSuicideOnDefense;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.MockGameData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RemoveFirstStrikeSuicideTest {
  @Mock BattleActions battleActions;
  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;

  @Test
  void suicideUnitsRemoved() {
    when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(mock(IDisplay.class));

    final List<Unit> attackers = List.of(givenAnyUnit(), givenUnitFirstStrikeSuicideOnAttack());
    final List<Unit> defenders = List.of(givenAnyUnit(), givenUnitFirstStrikeSuicideOnDefense());
    final MockGameData gameData = MockGameData.givenGameData();
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(attackers)
            .defendingUnits(defenders)
            .gameData(gameData.build())
            .build();

    final var removeFirstStrikeSuicide = new RemoveFirstStrikeSuicide(battleState, battleActions);
    removeFirstStrikeSuicide.execute(executionStack, delegateBridge);
    verify(battleActions)
        .removeUnits(
            List.of(attackers.get(1)), delegateBridge, battleState.getBattleSite(), OFFENSE);
    verify(battleActions)
        .removeUnits(
            List.of(defenders.get(1)), delegateBridge, battleState.getBattleSite(), DEFENSE);
    verifyNoMoreInteractions(battleActions);
  }
}
