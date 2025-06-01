package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvade;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static games.strategy.triplea.delegate.battle.steps.retreat.OffensiveSubsRetreatTest.givenRealUnitCanEvade;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.sound.ISound;

@ExtendWith(MockitoExtension.class)
class DefensiveSubsRetreatTest extends AbstractClientSettingTestCase {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;
  @Mock Territory battleSite;
  @Mock GamePlayer defender;

  @Test
  void hasNamesWhenNotSubmersibleButHasRetreatTerritories() {
    final Territory retreatTerritory = mock(Territory.class);
    when(retreatTerritory.isWater()).thenReturn(true);

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

    assertThat(defensiveSubsRetreat.getAllStepDetails(), hasSize(1));
  }

  @Test
  void hasNamesWhenHasNoRetreatTerritoriesButIsSubmersible() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .defendingUnits(List.of(givenUnitCanEvade()))
            .gameData(givenGameData().withSubmersibleSubs(true).build())
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getAllStepDetails(), hasSize(1));
  }

  @Test
  void hasNameWhenDestroyerIsOnOffenseAndWithdrawIsAfterBattle() {
    final BattleState battleState =
        givenBattleStateBuilder()
            // it shouldn't even care if the attacking unit is a destroyer
            .attackingUnits(List.of(mock(Unit.class)))
            .defendingUnits(List.of(givenUnitCanEvade()))
            .gameData(givenGameData().withSubmersibleSubs(true).build())
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getAllStepDetails(), hasSize(1));
  }

  @Test
  void hasNameWhenDestroyerIsOnOffenseAndWithdrawIsBeforeBattle() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitDestroyer()))
            .defendingUnits(List.of(givenUnitCanEvade()))
            .gameData(givenGameData().withSubmersibleSubs(true).build())
            .build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getAllStepDetails(), hasSize(1));
  }

  @Test
  void hasNoNamesWhenCanNotSubmergeAndNoRetreatTerritories() {
    final BattleState battleState =
        givenBattleStateBuilder().gameData(givenGameData().build()).build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    assertThat(defensiveSubsRetreat.getAllStepDetails(), is(empty()));
  }

  @Nested
  class SubmergeHappens {

    @Mock IDisplay display;
    @Mock ISound sound;
    @Mock IDelegateHistoryWriter historyWriter;

    @BeforeEach
    public void setupMocks() {
      when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);
      when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(sound);
      when(delegateBridge.getHistoryWriter()).thenReturn(historyWriter);
      when(defender.getName()).thenReturn("defender");
    }

    @Test
    void retreatHappensWhenHasNoRetreatTerritoriesButIsSubmersible() {
      final GameData gameData = givenGameData().withSubmersibleSubs(true).build();

      final Unit unit = givenRealUnitCanEvade(gameData, defender);

      final Collection<Unit> retreatingUnits = List.of(unit);
      final BattleState battleState =
          spy(
              givenBattleStateBuilder()
                  .battleSite(battleSite)
                  .defender(defender)
                  .defendingUnits(retreatingUnits)
                  .gameData(gameData)
                  .build());

      when(battleActions.querySubmergeTerritory(
              battleState, delegateBridge, defender, List.of(battleSite), "defender retreat subs?"))
          .thenReturn(Optional.of(battleSite));

      final DefensiveSubsRetreat defensiveSubsRetreat =
          new DefensiveSubsRetreat(battleState, battleActions);

      defensiveSubsRetreat.execute(executionStack, delegateBridge);

      verify(battleState).retreatUnits(DEFENSE, retreatingUnits);
    }

    @Test
    void retreatHappensWhenHasNoRetreatTerritoriesButDefendingIsSubmersible() {
      final GameData gameData =
          givenGameData()
              .withSubmersibleSubs(false)
              .withSubmarinesDefendingMaySubmergeOrRetreat(true)
              .build();

      final Unit unit = givenRealUnitCanEvade(gameData, defender);

      final Collection<Unit> retreatingUnits = List.of(unit);
      final BattleState battleState =
          spy(
              givenBattleStateBuilder()
                  .battleSite(battleSite)
                  .defender(defender)
                  .defendingUnits(retreatingUnits)
                  .gameData(gameData)
                  .build());

      when(battleActions.querySubmergeTerritory(
              battleState, delegateBridge, defender, List.of(battleSite), "defender retreat subs?"))
          .thenReturn(Optional.of(battleSite));

      final DefensiveSubsRetreat defensiveSubsRetreat =
          new DefensiveSubsRetreat(battleState, battleActions);

      defensiveSubsRetreat.execute(executionStack, delegateBridge);

      verify(battleState).retreatUnits(DEFENSE, retreatingUnits);
    }
  }

  @Nested
  class RetreatHappens {

    @Mock IDisplay display;
    @Mock ISound sound;
    @Mock IDelegateHistoryWriter historyWriter;
    @Mock UnitCollection battleSiteCollection;

    @BeforeEach
    public void setupMocks() {
      when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);
      when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(sound);
      when(delegateBridge.getHistoryWriter()).thenReturn(historyWriter);
      when(defender.getName()).thenReturn("defender");
      when(battleSite.getUnitCollection()).thenReturn(battleSiteCollection);
      when(battleSiteCollection.getHolder()).thenReturn(battleSite);
    }

    @Test
    void retreatHappensWhenNotSubmersibleButHasRetreatTerritories() {
      final GameData gameData = givenGameData().build();

      final Unit unit = givenRealUnitCanEvade(gameData, defender);
      final Collection<Unit> retreatingUnits = List.of(unit);

      final Territory retreatTerritory = mock(Territory.class);
      when(retreatTerritory.isWater()).thenReturn(true);
      final UnitCollection retreatTerritoryCollection = mock(UnitCollection.class);
      when(retreatTerritory.getUnitCollection()).thenReturn(retreatTerritoryCollection);
      when(retreatTerritoryCollection.getHolder()).thenReturn(retreatTerritory);

      final BattleState battleState =
          spy(
              givenBattleStateBuilder()
                  .defendingUnits(retreatingUnits)
                  .defender(defender)
                  .battleSite(battleSite)
                  .gameData(
                      givenGameData()
                          .withTerritoryHasNeighbors(battleSite, Set.of(retreatTerritory))
                          .build())
                  .build());

      when(battleActions.queryRetreatTerritory(
              battleState,
              delegateBridge,
              defender,
              List.of(retreatTerritory),
              "defender retreat subs?"))
          .thenReturn(Optional.of(retreatTerritory));

      final DefensiveSubsRetreat defensiveSubsRetreat =
          new DefensiveSubsRetreat(battleState, battleActions);
      defensiveSubsRetreat.execute(executionStack, delegateBridge);

      verify(battleState).retreatUnits(DEFENSE, retreatingUnits);
    }

    @Test
    void retreatHappensWhenDefendingIsSubmersibleAndHasRetreatTerritories() {
      final Territory retreatTerritory = mock(Territory.class);
      when(retreatTerritory.isWater()).thenReturn(true);
      final UnitCollection retreatTerritoryCollection = mock(UnitCollection.class);
      when(retreatTerritory.getUnitCollection()).thenReturn(retreatTerritoryCollection);
      when(retreatTerritoryCollection.getHolder()).thenReturn(retreatTerritory);

      final GameData gameData =
          givenGameData()
              .withSubmersibleSubs(false)
              .withSubmarinesDefendingMaySubmergeOrRetreat(true)
              .withTerritoryHasNeighbors(battleSite, Set.of(retreatTerritory))
              .build();

      final Unit unit = givenRealUnitCanEvade(gameData, defender);
      final Collection<Unit> retreatingUnits = List.of(unit);

      final BattleState battleState =
          spy(
              givenBattleStateBuilder()
                  .defendingUnits(retreatingUnits)
                  .defender(defender)
                  .battleSite(battleSite)
                  .gameData(gameData)
                  .build());

      when(battleActions.queryRetreatTerritory(
              battleState,
              delegateBridge,
              defender,
              List.of(retreatTerritory, battleSite),
              "defender retreat subs?"))
          .thenReturn(Optional.of(retreatTerritory));

      final DefensiveSubsRetreat defensiveSubsRetreat =
          new DefensiveSubsRetreat(battleState, battleActions);
      defensiveSubsRetreat.execute(executionStack, delegateBridge);

      verify(battleState).retreatUnits(DEFENSE, retreatingUnits);
    }

    @Test
    void retreatHappensWhenDefendingIsSubmersibleAndHasRetreatTerritoriesAndIsHeadless() {

      final Territory retreatTerritory = mock(Territory.class);
      final UnitCollection retreatTerritoryCollection = mock(UnitCollection.class);
      when(retreatTerritory.getUnitCollection()).thenReturn(retreatTerritoryCollection);
      when(retreatTerritoryCollection.getHolder()).thenReturn(retreatTerritory);

      final GameData gameData =
          givenGameData()
              .withSubmersibleSubs(false)
              .withSubmarinesDefendingMaySubmergeOrRetreat(true)
              .withTerritoryHasNeighbors(battleSite, Set.of(retreatTerritory))
              .build();

      final Unit unit = givenRealUnitCanEvade(gameData, defender);
      final Collection<Unit> retreatingUnits = List.of(unit);

      final BattleState battleState =
          spy(
              givenBattleStateBuilder()
                  .defendingUnits(retreatingUnits)
                  .defender(defender)
                  .battleSite(battleSite)
                  .headless(true)
                  .gameData(gameData)
                  .build());

      when(battleActions.queryRetreatTerritory(
              battleState,
              delegateBridge,
              defender,
              List.of(retreatTerritory, battleSite),
              "defender retreat subs?"))
          .thenReturn(Optional.of(retreatTerritory));

      final DefensiveSubsRetreat defensiveSubsRetreat =
          new DefensiveSubsRetreat(battleState, battleActions);
      defensiveSubsRetreat.execute(executionStack, delegateBridge);

      verify(battleState).retreatUnits(DEFENSE, retreatingUnits);
    }
  }

  @Test
  void retreatDoesNotHappenWhenBattleIsOver() {
    final BattleState battleState =
        givenBattleStateBuilder().defendingUnits(List.of(mock(Unit.class))).over(true).build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
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

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
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

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
  }

  @Test
  void retreatDoesNotHappenWhenCanNotSubmergeAndNoRetreatTerritories() {
    final BattleState battleState =
        givenBattleStateBuilder().gameData(givenGameData().build()).build();
    final DefensiveSubsRetreat defensiveSubsRetreat =
        new DefensiveSubsRetreat(battleState, battleActions);

    defensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
  }
}
