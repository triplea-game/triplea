package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvade;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitTransport;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.MockGameData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffensiveSubsRetreatTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  void hasNamesWhenNotSubmersibleButHasRetreatTerritories() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(MockGameData.givenGameData().build())
            .attackerRetreatTerritories(List.of(mock(Territory.class)))
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    assertThat(offensiveSubsRetreat.getNames(), hasSize(1));
  }

  @Test
  void hasNamesWhenHasNoRetreatTerritoriesButIsSubmersible() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(
                MockGameData.givenGameData()
                    .withSubRetreatBeforeBattle(false)
                    .withSubmersibleSubs(true)
                    .build())
            .attackerRetreatTerritories(List.of())
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    assertThat(offensiveSubsRetreat.getNames(), hasSize(1));
  }

  @Test
  void hasNameWhenDestroyerIsOnDefenseAndWithdrawIsAfterBattle() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            // it shouldn't even care if the defending unit is a destroyer
            .defendingUnits(List.of(mock(Unit.class)))
            .gameData(MockGameData.givenGameData().withSubmersibleSubs(true).build())
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    assertThat(offensiveSubsRetreat.getNames(), hasSize(1));
  }

  @Test
  void hasNoNamesWhenDestroyerIsOnDefenseAndWithdrawIsBeforeBattle() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .defendingUnits(List.of(givenUnitDestroyer()))
            .gameData(
                MockGameData.givenGameData()
                    .withSubmersibleSubs(true)
                    .withSubRetreatBeforeBattle(true)
                    .build())
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    assertThat(offensiveSubsRetreat.getNames(), is(empty()));
  }

  @Test
  void hasNoNamesWhenAmphibiousAssault() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(MockGameData.givenGameData().build())
            .amphibious(true)
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    assertThat(offensiveSubsRetreat.getNames(), is(empty()));
  }

  @Test
  void hasNoNamesWhenDefenselessTransportsEvenIfCanWithdraw() {
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

    assertThat(offensiveSubsRetreat.getNames(), is(empty()));
  }

  @Test
  void hasNoNamesWhenCanNotSubmergeAndNoRetreatTerritories() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(MockGameData.givenGameData().build())
            .attackerRetreatTerritories(List.of())
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    assertThat(offensiveSubsRetreat.getNames(), is(empty()));
  }

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

    assertThat(offensiveSubsRetreat.getNames(), hasSize(1));
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
}
