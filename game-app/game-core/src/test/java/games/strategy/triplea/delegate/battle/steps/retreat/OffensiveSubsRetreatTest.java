package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvade;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitSeaTransport;
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
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.MockGameData;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.sound.ISound;

@ExtendWith(MockitoExtension.class)
public class OffensiveSubsRetreatTest extends AbstractClientSettingTestCase {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock IDisplay display;
  @Mock ISound sound;
  @Mock IDelegateHistoryWriter historyWriter;
  @Mock BattleActions battleActions;
  @Mock Territory battleSite;
  @Mock UnitCollection battleSiteCollection;
  @Mock GamePlayer attacker;
  final UUID battleId = UUID.randomUUID();

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

    assertThat(offensiveSubsRetreat.getAllStepDetails(), hasSize(1));
  }

  @Test
  void hasNamesWhenHasNoRetreatTerritoriesButIsSubmersible() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .gameData(MockGameData.givenGameData().withSubmersibleSubs(true).build())
            .attackerRetreatTerritories(List.of())
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    assertThat(offensiveSubsRetreat.getAllStepDetails(), hasSize(1));
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

    assertThat(offensiveSubsRetreat.getAllStepDetails(), hasSize(1));
  }

  @Test
  void hasNameWhenDestroyerIsOnDefenseAndWithdrawIsBeforeBattle() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .attackingUnits(List.of(givenUnitCanEvade()))
            .defendingUnits(List.of(givenUnitDestroyer()))
            .gameData(MockGameData.givenGameData().withSubmersibleSubs(true).build())
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    assertThat(
        "The destroyer could be killed during the AA phase which would allow the sub to retreat.",
        offensiveSubsRetreat.getAllStepDetails(),
        hasSize(1));
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

    assertThat(offensiveSubsRetreat.getAllStepDetails(), is(empty()));
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
            .defendingUnits(List.of(givenUnitSeaTransport()))
            .attackerRetreatTerritories(List.of(mock(Territory.class)))
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    assertThat(offensiveSubsRetreat.getAllStepDetails(), is(empty()));
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

    assertThat(offensiveSubsRetreat.getAllStepDetails(), is(empty()));
  }

  static Unit givenRealUnitCanEvade(final GameData gameData, final GamePlayer player) {
    final UnitType unitType = mock(UnitType.class);
    final UnitAttachment unitAttachment = mock(UnitAttachment.class);
    when(unitType.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment);
    when(unitAttachment.getCanEvade()).thenReturn(true);
    return new Unit(unitType, player, gameData);
  }

  @Nested
  class SubmergeHappens {

    @BeforeEach
    public void setupMocks() {
      when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);
      when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(sound);
      when(delegateBridge.getHistoryWriter()).thenReturn(historyWriter);
      when(attacker.getName()).thenReturn("attacker");
    }

    @Test
    void submergeHappensWhenIsSubmersible() {
      final GameData gameData =
          MockGameData.givenGameData()
              .withTransportCasualtiesRestricted(false)
              .withSubmersibleSubs(true)
              .build();

      final Unit unit = givenRealUnitCanEvade(gameData, attacker);
      final Collection<Unit> retreatingUnits = List.of(unit);

      final BattleState battleState =
          spy(
              givenBattleStateBuilder()
                  .battleId(battleId)
                  .battleSite(battleSite)
                  .attacker(attacker)
                  .attackingUnits(retreatingUnits)
                  .gameData(gameData)
                  .attackerRetreatTerritories(List.of())
                  .build());

      when(battleActions.querySubmergeTerritory(
              battleState, delegateBridge, attacker, List.of(battleSite), "attacker retreat subs?"))
          .thenReturn(Optional.of(battleSite));

      final OffensiveSubsRetreat offensiveSubsRetreat =
          new OffensiveSubsRetreat(battleState, battleActions);
      offensiveSubsRetreat.execute(executionStack, delegateBridge);

      verify(battleState).retreatUnits(OFFENSE, retreatingUnits);
    }

    @Test
    void retreatHappensWhenTransportsOnDefenseButNotDefenseless() {
      final GameData gameData =
          MockGameData.givenGameData()
              .withTransportCasualtiesRestricted(true)
              .withSubmersibleSubs(true)
              .build();

      final Unit unit = givenRealUnitCanEvade(gameData, attacker);
      final Collection<Unit> retreatingUnits = List.of(unit);

      final BattleState battleState =
          spy(
              givenBattleStateBuilder()
                  .battleId(battleId)
                  .battleSite(battleSite)
                  .attacker(attacker)
                  .attackingUnits(retreatingUnits)
                  .gameData(gameData)
                  .defendingUnits(List.of(givenUnitSeaTransport(), givenAnyUnit()))
                  .build());

      when(battleActions.querySubmergeTerritory(
              battleState, delegateBridge, attacker, List.of(battleSite), "attacker retreat subs?"))
          .thenReturn(Optional.of(battleSite));

      final OffensiveSubsRetreat offensiveSubsRetreat =
          new OffensiveSubsRetreat(battleState, battleActions);

      offensiveSubsRetreat.execute(executionStack, delegateBridge);

      verify(battleState).retreatUnits(OFFENSE, retreatingUnits);
    }
  }

  @Nested
  class RetreatHappens {

    @BeforeEach
    public void setupMocks() {
      when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);
      when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(sound);
      when(delegateBridge.getHistoryWriter()).thenReturn(historyWriter);
      when(attacker.getName()).thenReturn("attacker");
      when(battleSite.getUnitCollection()).thenReturn(battleSiteCollection);
      when(battleSiteCollection.getHolder()).thenReturn(battleSite);
    }

    @Test
    void retreatHappensWhenNotSubmersibleButHasRetreatTerritories() {
      final GameData gameData = MockGameData.givenGameData().build();

      final Unit unit = givenRealUnitCanEvade(gameData, attacker);
      final Collection<Unit> retreatingUnits = List.of(unit);

      final Territory retreatTerritory = mock(Territory.class);
      final UnitCollection retreatTerritoryCollection = mock(UnitCollection.class);
      when(retreatTerritory.getUnitCollection()).thenReturn(retreatTerritoryCollection);
      when(retreatTerritoryCollection.getHolder()).thenReturn(retreatTerritory);

      final BattleState battleState =
          spy(
              givenBattleStateBuilder()
                  .battleSite(battleSite)
                  .attacker(attacker)
                  .attackingUnits(retreatingUnits)
                  .gameData(gameData)
                  .attackerRetreatTerritories(List.of(retreatTerritory))
                  .build());

      when(battleActions.queryRetreatTerritory(
              battleState,
              delegateBridge,
              attacker,
              List.of(retreatTerritory),
              "attacker retreat subs?"))
          .thenReturn(Optional.of(retreatTerritory));

      final OffensiveSubsRetreat offensiveSubsRetreat =
          new OffensiveSubsRetreat(battleState, battleActions);

      offensiveSubsRetreat.execute(executionStack, delegateBridge);

      verify(battleState).retreatUnits(OFFENSE, retreatingUnits);
    }
  }

  @Test
  void retreatDoesNotHappenWhenBattleIsOver() {
    final BattleState battleState =
        givenBattleStateBuilder().attackingUnits(List.of(mock(Unit.class))).over(true).build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
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

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
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

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
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

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
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
            .defendingUnits(List.of(givenUnitSeaTransport()))
            .attackerRetreatTerritories(List.of(mock(Territory.class)))
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
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
            .defendingUnits(List.of(givenUnitSeaTransport()))
            .build();
    final OffensiveSubsRetreat offensiveSubsRetreat =
        new OffensiveSubsRetreat(battleState, battleActions);

    offensiveSubsRetreat.execute(executionStack, delegateBridge);

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
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

    verify(battleActions, never())
        .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
  }
}
