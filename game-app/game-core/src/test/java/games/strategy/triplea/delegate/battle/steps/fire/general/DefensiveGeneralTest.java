package games.strategy.triplea.delegate.battle.steps.fire.general;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefensiveGeneralTest {

  @Mock BattleActions battleActions;

  @Nested
  class GetNames {
    @Test
    void hasNamesIfStandardUnitAvailable() {
      final GameData gameData =
          givenGameData().withDefendingSuicideAndMunitionUnitsDoNotFire(false).build();
      final BattleState battleState =
          givenBattleStateBuilder()
              .defendingUnits(List.of(givenAnyUnit()))
              .attackingUnits(List.of(givenAnyUnit()))
              .gameData(gameData)
              .build();
      final DefensiveGeneral defensiveGeneral = new DefensiveGeneral(battleState, battleActions);
      assertThat(defensiveGeneral.getAllStepDetails(), hasSize(3));
    }

    @Test
    void hasNoNamesIfStandardUnitIsNotAvailable() {
      final GameData gameData =
          givenGameData().withDefendingSuicideAndMunitionUnitsDoNotFire(false).build();
      final BattleState battleState =
          givenBattleStateBuilder()
              .defendingUnits(List.of(givenUnitFirstStrike()))
              .attackingUnits(List.of(givenAnyUnit()))
              .gameData(gameData)
              .build();
      final DefensiveGeneral defensiveGeneral = new DefensiveGeneral(battleState, battleActions);
      assertThat(defensiveGeneral.getAllStepDetails(), is(empty()));
    }
  }
}
