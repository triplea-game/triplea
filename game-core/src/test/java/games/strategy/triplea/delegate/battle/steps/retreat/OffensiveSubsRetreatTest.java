package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.Constants.SUBMERSIBLE_SUBS;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvade;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitTransport;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OffensiveSubsRetreatTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  void retreatHappensWhenNotSubmersibleButHasRetreatTerritories() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(MockGameData.givenGameData().build())
            .attackerRetreatTerritories(List.of(mock(Territory.class)))
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions)
        .queryRetreat(eq(false), eq(MustFightBattle.RetreatType.SUBS), eq(delegateBridge), any());
  }

  @Test
  void retreatHappensWhenHasNoRetreatTerritoriesButIsSubmersible() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(
                MockGameData.givenGameData()
                    .withTransportCasualtiesRestricted(false)
                    .withSubmersibleSubs(true)
                    .build())
            .attackerRetreatTerritories(List.of())
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions)
        .queryRetreat(eq(false), eq(MustFightBattle.RetreatType.SUBS), eq(delegateBridge), any());
  }

  @Test
  void retreatHappensWhenTransportsOnDefenseButNotDefenseless() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(
                MockGameData.givenGameData()
                    .withTransportCasualtiesRestricted(true)
                    .withSubmersibleSubs(true)
                    .build())
            .defendingUnits(List.of(givenUnitTransport(), givenAnyUnit()))
            .build();

    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions)
        .queryRetreat(eq(false), eq(MustFightBattle.RetreatType.SUBS), eq(delegateBridge), any());
  }

  @Test
  void retreatDoesNotHappenWhenBattleIsOver() {
    final BattleState battleState =
        givenBattleStateBuilder().attackingUnits(List.of(mock(Unit.class))).over(true).build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void retreatDoesNotHappenWhenDestroyerIsOnDefense() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(mock(Unit.class)))
            .defendingUnits(List.of(givenUnitDestroyer()))
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void retreatDoesNotHappenWhenWaitingToDieDestroyerIsOnDefense() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(mock(Unit.class)))
            .defendingWaitingToDie(List.of(givenUnitDestroyer()))
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void retreatDoesNotHappenWhenAmphibiousAssault() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(MockGameData.givenGameData().build())
            .amphibious(true)
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void retreatDoesNotHappenWhenDefenselessTransportsEvenIfCanWithdraw() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(
                MockGameData.givenGameData()
                    .withTransportCasualtiesRestricted(true)
                    .withSubmersibleSubs(false)
                    .build())
            .defendingUnits(List.of(givenUnitTransport()))
            .attackerRetreatTerritories(List.of(mock(Territory.class)))
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void retreatDoesNotHappenWhenDefenselessTransportsEvenIfCanSubmerge() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(
                MockGameData.givenGameData()
                    .withTransportCasualtiesRestricted(true)
                    .withSubmersibleSubs(true)
                    .build())
            .defendingUnits(List.of(givenUnitTransport()))
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void retreatDoesNotHappenWhenCanNotSubmergeAndNoRetreatTerritories() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(MockGameData.givenGameData().build())
            .attackerRetreatTerritories(List.of())
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  static class MockGameData {
    private final GameData gameData = mock(GameData.class);
    private final GameProperties gameProperties = mock(GameProperties.class);

    private MockGameData() {
      lenient().when(gameData.getProperties()).thenReturn(gameProperties);
    }

    static MockGameData givenGameData() {
      return new MockGameData();
    }

    GameData build() {
      return gameData;
    }

    MockGameData withTransportCasualtiesRestricted(final boolean value) {
      when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(value);
      return this;
    }

    MockGameData withSubmersibleSubs(final boolean value) {
      when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(value);
      return this;
    }
  }
}
