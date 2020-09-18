package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvade;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefensiveSubsRetreatTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;
  @Mock Territory battleSite;

  @Test
  void hasNamesWhenNotSubmersibleButHasRetreatTerritories() {
    final Territory retreatTerritory = mock(Territory.class);
    when(retreatTerritory.isWater()).thenReturn(true);
    when(retreatTerritory.getUnitCollection()).thenReturn(mock(UnitCollection.class));

    final BattleState battleState =
        givenBattleStateBuilder()
            .battleSite(battleSite)
            .defendingUnits(List.of(givenUnitCanEvade()))
            .gameData(
                givenGameData()
                    .withTerritoryHasNeighbors(battleSite, Set.of(retreatTerritory))
                    .build())
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getNames(), hasSize(1));
  }

  @Test
  void hasNamesWhenHasNoRetreatTerritoriesButIsSubmersible() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .defendingUnits(List.of(givenUnitCanEvade()))
            .gameData(
                givenGameData().withSubRetreatBeforeBattle(false).withSubmersibleSubs(true).build())
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getNames(), hasSize(1));
  }

  @Test
  void hasNameWhenDestroyerIsOnOffenseAndWithdrawIsAfterBattle() {
    final BattleState battleState =
        givenBattleStateBuilder()
            // it shouldn't even care if the attacking unit is a destroyer
            .attackingUnits(List.of(mock(Unit.class)))
            .defendingUnits(List.of(givenUnitCanEvade()))
            .gameData(
                givenGameData().withSubRetreatBeforeBattle(false).withSubmersibleSubs(true).build())
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getNames(), hasSize(1));
  }

  @Test
  void hasNoNamesWhenDestroyerIsOnOffenseAndWithdrawIsBeforeBattle() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitDestroyer()))
            .defendingUnits(List.of(givenUnitCanEvade()))
            .gameData(
                givenGameData().withSubmersibleSubs(true).withSubRetreatBeforeBattle(true).build())
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getNames(), is(empty()));
  }

  @Test
  void hasNoNamesWhenCanNotSubmergeAndNoRetreatTerritories() {
    final BattleState battleState =
        givenBattleStateBuilder().gameData(givenGameData().build()).build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getNames(), is(empty()));
  }

  @Test
  void retreatHappensWhenNotSubmersibleButHasRetreatTerritories() {
    final Territory retreatTerritory = mock(Territory.class);
    when(retreatTerritory.isWater()).thenReturn(true);
    when(retreatTerritory.getUnitCollection()).thenReturn(mock(UnitCollection.class));

    final BattleState battleState =
        givenBattleStateBuilder()
            .defendingUnits(List.of(givenUnitCanEvade()))
            .battleSite(battleSite)
            .gameData(
                givenGameData()
                    .withTerritoryHasNeighbors(battleSite, Set.of(retreatTerritory))
                    .build())
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getNames(), hasSize(1));
  }

  @Test
  void retreatHappensWhenHasNoRetreatTerritoriesButIsSubmersible() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .defendingUnits(List.of(givenUnitCanEvade()))
            .gameData(givenGameData().withSubmersibleSubs(true).build())
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions)
        .queryRetreat(eq(true), eq(MustFightBattle.RetreatType.SUBS), eq(delegateBridge), any());
  }

  @Test
  void retreatDoesNotHappenWhenBattleIsOver() {
    final BattleState battleState =
        givenBattleStateBuilder().defendingUnits(List.of(mock(Unit.class))).over(true).build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void retreatDoesNotHappenWhenDestroyerIsOnOffense() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .defendingUnits(List.of(mock(Unit.class)))
            .attackingUnits(List.of(givenUnitDestroyer()))
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void retreatDoesNotHappenWhenWaitingToDieDestroyerIsOnOffense() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .defendingUnits(List.of(mock(Unit.class)))
            .attackingWaitingToDie(List.of(givenUnitDestroyer()))
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void retreatDoesNotHappenWhenCanNotSubmergeAndNoRetreatTerritories() {
    final BattleState battleState =
        givenBattleStateBuilder().gameData(givenGameData().build()).build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never()).queryRetreat(anyBoolean(), any(), any(), any());
  }

  @Test
  void gameIsOverIfAfterRegularBattleAndAllDefendingUnitsAreEvadersAndWithdraw() {

    final BattleState battleState =
        spy(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubmersibleSubs(true)
                        .withSubRetreatBeforeBattle(false)
                        .build())
                .build());

    doReturn(List.of()).when(battleState).getUnits(eq(BattleState.Side.OFFENSE));
    // first return an evader so it can retreat it
    // then return nothing to indicate that the evader retreated during the queryRetreat call
    doReturn(List.of(givenUnitCanEvade()), List.of())
        .when(battleState)
        .getUnits(eq(BattleState.Side.DEFENSE));

    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions)
        .queryRetreat(eq(true), eq(MustFightBattle.RetreatType.SUBS), eq(delegateBridge), any());

    verify(battleActions).endBattle(eq(delegateBridge));
    verify(battleActions).attackerWins(eq(delegateBridge));
  }

  @Test
  void gameIsNotOverIfAfterRegularBattleAndNotAllDefendingUnitsAreEvadersAndWithdraw() {

    final BattleState battleState =
        spy(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubmersibleSubs(true)
                        .withSubRetreatBeforeBattle(false)
                        .build())
                .build());

    doReturn(List.of()).when(battleState).getUnits(eq(BattleState.Side.OFFENSE));
    // first return an evader so it can retreat it
    // then return nothing to indicate that the evader retreated during the queryRetreat call
    doReturn(List.of(givenUnitCanEvade(), mock(Unit.class)), List.of(givenAnyUnit()))
        .when(battleState)
        .getUnits(eq(BattleState.Side.DEFENSE));

    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions)
        .queryRetreat(eq(true), eq(MustFightBattle.RetreatType.SUBS), eq(delegateBridge), any());

    verify(battleActions, never()).endBattle(eq(delegateBridge));
    verify(battleActions, never()).attackerWins(eq(delegateBridge));
  }

  @Test
  void gameIsNotOverIfBeforeRegularBattleAndAllDefendingUnitsAreEvadersAndWithdraw() {

    final BattleState battleState =
        spy(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubmersibleSubs(true)
                        .withSubRetreatBeforeBattle(true)
                        .build())
                .build());

    doReturn(List.of()).when(battleState).getUnits(eq(BattleState.Side.OFFENSE));
    // first return an evader so it can retreat it
    doReturn(List.of(givenUnitCanEvade()))
        // then return nothing to indicate that the evader retreated during the queryRetreat call
        .doReturn(List.of())
        .when(battleState)
        .getUnits(eq(BattleState.Side.DEFENSE));

    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions)
        .queryRetreat(eq(true), eq(MustFightBattle.RetreatType.SUBS), eq(delegateBridge), any());

    verify(battleActions, never()).endBattle(eq(delegateBridge));
    verify(battleActions, never()).attackerWins(eq(delegateBridge));
  }
}
