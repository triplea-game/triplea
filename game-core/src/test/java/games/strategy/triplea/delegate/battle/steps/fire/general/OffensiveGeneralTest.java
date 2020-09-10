package games.strategy.triplea.delegate.battle.steps.fire.general;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OffensiveGeneralTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Nested
  class GetNames {
    @Test
    void hasNamesIfStandardUnitAvailable() {
      final BattleState battleState =
          givenBattleStateBuilder().attackingUnits(List.of(givenAnyUnit())).build();
      final OffensiveGeneral offensiveGeneral = new OffensiveGeneral(battleState, battleActions);
      assertThat(offensiveGeneral.getNames(), hasSize(2));
    }

    @Test
    void hasNoNamesIfStandardUnitIsNotAvailable() {
      final BattleState battleState =
          givenBattleStateBuilder().attackingUnits(List.of(givenUnitFirstStrike())).build();
      final OffensiveGeneral offensiveGeneral = new OffensiveGeneral(battleState, battleActions);
      assertThat(offensiveGeneral.getNames(), is(empty()));
    }
  }
}
