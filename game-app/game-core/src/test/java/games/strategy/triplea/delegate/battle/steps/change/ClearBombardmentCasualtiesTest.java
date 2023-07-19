package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.MockGameData;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClearBombardmentCasualtiesTest {
  @Mock BattleActions battleActions;
  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;

  @ParameterizedTest
  @ValueSource(booleans = {true, false}) // six numbers
  void bombardCasualtiesRemoved(boolean navalBombardCasualtiesReturnFire) {
    final List<Unit> bombardingUnits = List.of(givenAnyUnit());
    final List<Unit> attackers = List.of(givenAnyUnit());
    final List<Unit> defenders = List.of(givenAnyUnit());
    final MockGameData gameData =
        MockGameData.givenGameData()
            .withNavalBombardCasualtiesReturnFire(navalBombardCasualtiesReturnFire);
    final BattleState battleState =
        givenBattleStateBuilder()
            .battleRound(1)
            .bombardingUnits(bombardingUnits)
            .attackingUnits(attackers)
            .defendingUnits(defenders)
            .gameData(gameData.build())
            .build();

    final var clearBombardmentCasualties =
        new ClearBombardmentCasualties(battleState, battleActions);
    clearBombardmentCasualties.execute(executionStack, delegateBridge);
    if (!navalBombardCasualtiesReturnFire) {
      verify(battleActions).clearWaitingToDieAndDamagedChangesInto(delegateBridge, DEFENSE);
    }
    verifyNoMoreInteractions(battleActions);
  }
}
