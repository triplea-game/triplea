package games.strategy.triplea.delegate.battle.steps.fire.standard;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OffensiveStandardTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Nested
  class GetNames {
    @Test
    void hasNamesIfStandardUnitAvailable() {
      final BattleState battleState =
          givenBattleStateBuilder().attackingUnits(List.of(givenAnyUnit())).build();
      final OffensiveStandard offensiveStandard = new OffensiveStandard(battleState, battleActions);
      assertThat(offensiveStandard.getNames(), hasSize(2));
    }

    @Test
    void hasNoNamesIfStandardUnitIsNotAvailable() {
      final BattleState battleState =
          givenBattleStateBuilder().attackingUnits(List.of(givenUnitFirstStrike())).build();
      final OffensiveStandard offensiveStandard = new OffensiveStandard(battleState, battleActions);
      assertThat(offensiveStandard.getNames(), is(empty()));
    }
  }
}
