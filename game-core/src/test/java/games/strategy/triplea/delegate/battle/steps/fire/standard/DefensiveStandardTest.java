package games.strategy.triplea.delegate.battle.steps.fire.standard;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.GameData;
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
class DefensiveStandardTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
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
              .gameData(gameData)
              .build();
      final DefensiveStandard defensiveStandard = new DefensiveStandard(battleState, battleActions);
      assertThat(defensiveStandard.getNames(), hasSize(2));
    }

    @Test
    void hasNoNamesIfStandardUnitIsNotAvailable() {
      final GameData gameData =
          givenGameData().withDefendingSuicideAndMunitionUnitsDoNotFire(false).build();
      final BattleState battleState =
          givenBattleStateBuilder()
              .defendingUnits(List.of(givenUnitFirstStrike()))
              .gameData(gameData)
              .build();
      final DefensiveStandard defensiveStandard = new DefensiveStandard(battleState, battleActions);
      assertThat(defensiveStandard.getNames(), hasSize(0));
    }
  }

  @Nested
  class FireAa {
    @Test
    void onlyFireStandardUnits() {
      final GameData gameData =
          givenGameData().withDefendingSuicideAndMunitionUnitsDoNotFire(false).build();
      final DefensiveStandard defensiveStandard =
          new DefensiveStandard(
              givenBattleStateBuilder().defendingAa(List.of()).gameData(gameData).build(),
              battleActions);

      defensiveStandard.execute(executionStack, delegateBridge);

      verify(battleActions)
          .findTargetGroupsAndFire(
              eq(MustFightBattle.ReturnFire.ALL),
              any(),
              eq(true),
              any(),
              any(),
              anyCollection(),
              anyCollection(),
              anyCollection(),
              anyCollection());
    }
  }
}
