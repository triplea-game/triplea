package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitSeaTransport;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitWasAmphibious;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.sound.ISound;

@ExtendWith(MockitoExtension.class)
class OffensiveGeneralRetreatTest extends AbstractClientSettingTestCase {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;
  @Mock Territory battleSite;

  @Nested
  class GetNames {

    @Nested
    class AirAmphibiousRetreat {

      @Test
      void noIfAmphibiousEvenWithWW2V2AndHasPlanes() {
        final Unit unit = givenAnyUnit();
        final BattleState battleState =
            givenBattleStateBuilder()
                .amphibious(false)
                .attackingUnits(List.of(unit))
                .gameData(givenGameData().withWW2V2(true).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), is(empty()));
        final UnitAttachment unitAttachment =
            (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
        // ensure it didn't even try to check if there are planes
        verify(unitAttachment, never()).getIsAir();
      }

      @Test
      void yesIfWW2V2AndHasPlanes() {
        final Unit unit = givenUnitIsAir();
        final BattleState battleState =
            givenBattleStateBuilder()
                .amphibious(true)
                .attackingUnits(List.of(unit))
                .gameData(givenGameData().withWW2V2(true).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), hasSize(1));
        final UnitAttachment unitAttachment =
            (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
        verify(unitAttachment).getIsAir();
      }

      @Test
      void yesIfAttackerAirCanRetreatAndHasPlanes() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .amphibious(true)
                .attackingUnits(List.of(givenUnitIsAir()))
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withPartialAmphibiousRetreat(false)
                        .withAttackerRetreatPlanes(true)
                        .build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), hasSize(1));
      }

      @Test
      void yesIfPartialAmphibiousRetreatAndHasPlanes() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .amphibious(true)
                .attackingUnits(List.of(givenUnitIsAir()))
                .gameData(
                    givenGameData().withWW2V2(false).withPartialAmphibiousRetreat(true).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), hasSize(1));
      }

      @Test
      void noIfWW2V2AndNoPlanes() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .amphibious(true)
                .attackingUnits(List.of(givenAnyUnit()))
                .gameData(givenGameData().withWW2V2(true).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), is(empty()));
      }

      @Test
      void noIfAttackerAirCanRetreatAndNoPlanes() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .amphibious(true)
                .attackingUnits(List.of(givenAnyUnit()))
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withPartialAmphibiousRetreat(false)
                        .withAttackerRetreatPlanes(true)
                        .build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), is(empty()));
      }
    }

    @Nested
    class PartialAmphibiousRetreat {

      @Test
      void yesIfNonAmphibUnits() {
        final Unit amphibiousUnit = givenUnitWasAmphibious();
        final Unit nonAmphibiousUnit = givenAnyUnit();
        final BattleState battleState =
            givenBattleStateBuilder()
                .amphibious(true)
                .attackingUnits(List.of(amphibiousUnit, nonAmphibiousUnit))
                .gameData(givenGameData().withPartialAmphibiousRetreat(true).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), hasSize(1));
      }

      @Test
      void noIfNoNonAmphibUnits() {
        final Unit amphibiousUnit = givenUnitWasAmphibious();
        final BattleState battleState =
            givenBattleStateBuilder()
                .amphibious(true)
                .attackingUnits(List.of(amphibiousUnit))
                .gameData(givenGameData().withPartialAmphibiousRetreat(true).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), is(empty()));
      }

      @Test
      void noIfPartialNotAllowed() {
        final Unit amphibiousUnit = mock(Unit.class);
        final BattleState battleState =
            givenBattleStateBuilder()
                .amphibious(true)
                .attackingUnits(List.of(amphibiousUnit))
                .gameData(givenGameData().withPartialAmphibiousRetreat(false).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), is(empty()));
        // ensure that it doesn't even check for amphibious units
        verify(amphibiousUnit, never()).getWasAmphibious();
      }
    }

    @Test
    void airUnitsAtSeaAlwaysCanRetreat() {
      when(battleSite.isWater()).thenReturn(true);
      final BattleState battleState =
          givenBattleStateBuilder()
              .battleSite(battleSite)
              .attackingUnits(List.of(givenUnitIsAir()))
              .gameData(givenGameData().build())
              .build();

      final OffensiveGeneralRetreat offensiveGeneralRetreat =
          new OffensiveGeneralRetreat(battleState, battleActions);
      assertThat(offensiveGeneralRetreat.getAllStepDetails(), hasSize(1));
    }

    @Nested
    class NonAmphibiousRetreat {

      @Test
      void noIfNoRetreatTerritories() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .attackerRetreatTerritories(List.of())
                .gameData(givenGameData().build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), is(empty()));
      }

      @Test
      void yesIfRetreatTerritories() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .attackerRetreatTerritories(List.of(mock(Territory.class)))
                .gameData(givenGameData().build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), hasSize(1));
      }

      @Test
      void noIfDefenselessTransportsAndRestrictedCasualties() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .defendingUnits(List.of(givenUnitSeaTransport()))
                .attackerRetreatTerritories(List.of(mock(Territory.class)))
                .gameData(givenGameData().withTransportCasualtiesRestricted(true).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), is(empty()));
      }

      @Test
      void yesIfNoRestrictedCasualtiesAndRetreatTerritories() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .attackingUnits(List.of(mock(Unit.class)))
                .attackerRetreatTerritories(List.of(mock(Territory.class)))
                .gameData(givenGameData().withTransportCasualtiesRestricted(false).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        assertThat(offensiveGeneralRetreat.getAllStepDetails(), hasSize(1));
      }
    }
  }

  @Nested
  class Execute {

    @Mock GamePlayer attacker;
    @Mock IDisplay display;
    @Mock ISound sound;
    @Mock IDelegateHistoryWriter historyWriter;
    @Mock UnitCollection battleSiteCollection;

    @Test
    void noIfOver() {
      final BattleState battleState = givenBattleStateBuilder().over(true).build();

      final OffensiveGeneralRetreat offensiveGeneralRetreat =
          new OffensiveGeneralRetreat(battleState, battleActions);
      offensiveGeneralRetreat.execute(executionStack, delegateBridge);
      verify(battleActions, never())
          .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
    }

    @Nested
    class AmphibRetreatPlanesAllowed {

      @BeforeEach
      public void setupMocks() {
        when(delegateBridge.getDisplayChannelBroadcaster()).thenReturn(display);
        when(delegateBridge.getSoundChannelBroadcaster()).thenReturn(sound);
        when(delegateBridge.getHistoryWriter()).thenReturn(historyWriter);
        when(attacker.getName()).thenReturn("attacker");
      }

      @Test
      void yesIfWW2V2AndHasPlanesDuringAmphibAttack() {
        final Unit unit = givenUnitIsAir();
        when(unit.getOwner()).thenReturn(attacker);
        final Collection<Unit> retreatingUnits = List.of(unit);
        final BattleState battleState =
            spy(
                givenBattleStateBuilder()
                    .battleSite(battleSite)
                    .attacker(attacker)
                    .amphibious(true)
                    .attackingUnits(retreatingUnits)
                    .gameData(givenGameData().withWW2V2(true).build())
                    .build());

        when(battleActions.queryRetreatTerritory(
                battleState,
                delegateBridge,
                attacker,
                List.of(battleSite),
                "attacker retreat planes?"))
            .thenReturn(battleSite);

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);

        verify(battleState).retreatUnits(OFFENSE, retreatingUnits);
      }

      @Test
      void yesIfAttackerAirCanRetreatAndHasPlanesDuringAmphibAttack() {
        final Unit unit = givenUnitIsAir();
        when(unit.getOwner()).thenReturn(attacker);
        final Collection<Unit> retreatingUnits = List.of(unit);
        final BattleState battleState =
            spy(
                givenBattleStateBuilder()
                    .battleSite(battleSite)
                    .attacker(attacker)
                    .amphibious(true)
                    .attackingUnits(retreatingUnits)
                    .gameData(
                        givenGameData()
                            .withWW2V2(false)
                            .withPartialAmphibiousRetreat(false)
                            .withAttackerRetreatPlanes(true)
                            .build())
                    .build());

        when(battleActions.queryRetreatTerritory(
                battleState,
                delegateBridge,
                attacker,
                List.of(battleSite),
                "attacker retreat planes?"))
            .thenReturn(battleSite);

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);

        verify(battleState).retreatUnits(OFFENSE, retreatingUnits);
      }

      @Test
      void yesIfPartialAmphibiousRetreatAndHasPlanes() {
        final Unit unit = givenUnitIsAir();
        when(unit.getOwner()).thenReturn(attacker);
        final Collection<Unit> retreatingUnits = List.of(unit);
        final BattleState battleState =
            spy(
                givenBattleStateBuilder()
                    .battleSite(battleSite)
                    .attacker(attacker)
                    .amphibious(true)
                    .attackingUnits(retreatingUnits)
                    .gameData(
                        givenGameData().withWW2V2(false).withPartialAmphibiousRetreat(true).build())
                    .build());

        when(battleActions.queryRetreatTerritory(
                battleState,
                delegateBridge,
                attacker,
                List.of(battleSite),
                "attacker retreat planes?"))
            .thenReturn(battleSite);

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleState).retreatUnits(OFFENSE, retreatingUnits);
      }
    }

    @Nested
    class AmphibRetreatPartialAllowed {

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
      void yesIfNonAmphibUnitsDuringAmphibAttack() {
        final Unit amphibiousUnit = givenUnitWasAmphibious();
        final Unit nonAmphibiousUnit = givenAnyUnit();
        when(nonAmphibiousUnit.getOwner()).thenReturn(attacker);
        final Collection<Unit> retreatingUnits = Set.of(nonAmphibiousUnit);
        final Territory retreatSite = mock(Territory.class);
        final UnitCollection retreatSiteCollection = mock(UnitCollection.class);
        when(retreatSite.getUnitCollection()).thenReturn(retreatSiteCollection);
        when(retreatSiteCollection.getHolder()).thenReturn(retreatSite);
        final BattleState battleState =
            spy(
                givenBattleStateBuilder()
                    .battleSite(battleSite)
                    .attacker(attacker)
                    .amphibious(true)
                    .attackerRetreatTerritories(List.of(retreatSite))
                    .attackingUnits(List.of(amphibiousUnit, nonAmphibiousUnit))
                    .gameData(givenGameData().withPartialAmphibiousRetreat(true).build())
                    .build());

        when(battleActions.queryRetreatTerritory(
                battleState,
                delegateBridge,
                attacker,
                List.of(retreatSite),
                "attacker retreat non-amphibious units?"))
            .thenReturn(retreatSite);

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleState).retreatUnits(OFFENSE, retreatingUnits);
      }
    }

    @Nested
    class AmphibRetreatNotAllowed {
      @Test
      void noIfWW2V2AndNoPlanes() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .battleSite(battleSite)
                .attacker(attacker)
                .amphibious(true)
                .attackingUnits(List.of(givenAnyUnit()))
                .gameData(givenGameData().withWW2V2(true).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleActions, never())
            .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
      }

      @Test
      void noIfAttackerAirCanRetreatAndNoPlanes() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .battleSite(battleSite)
                .attacker(attacker)
                .amphibious(true)
                .attackingUnits(List.of(givenAnyUnit()))
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withPartialAmphibiousRetreat(false)
                        .withAttackerRetreatPlanes(true)
                        .build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleActions, never())
            .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
      }

      @Test
      void noIfNoNonAmphibUnitsDuringAmphibAttack() {
        final Unit amphibiousUnit = givenUnitWasAmphibious();
        final BattleState battleState =
            givenBattleStateBuilder()
                .battleSite(battleSite)
                .attacker(attacker)
                .amphibious(true)
                .attackingUnits(List.of(amphibiousUnit))
                .gameData(givenGameData().withPartialAmphibiousRetreat(true).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleActions, never())
            .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
      }

      @Test
      void noIfPartialNotAllowedDuringAmphibAttack() {
        final Unit amphibiousUnit = mock(Unit.class);
        final BattleState battleState =
            givenBattleStateBuilder()
                .battleSite(battleSite)
                .attacker(attacker)
                .amphibious(true)
                .attackingUnits(List.of(amphibiousUnit))
                .gameData(givenGameData().withPartialAmphibiousRetreat(false).build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleActions, never())
            .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
      }
    }

    @Nested
    class NonAmphibRetreatAllowed {

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
      void yesIfRetreatTerritories() {
        final Unit unit = givenAnyUnit();
        when(unit.getOwner()).thenReturn(attacker);
        final Collection<Unit> retreatingUnits = Set.of(unit);
        final Territory retreatSite = mock(Territory.class);
        final UnitCollection retreatSiteCollection = mock(UnitCollection.class);
        when(retreatSite.getUnitCollection()).thenReturn(retreatSiteCollection);
        when(retreatSiteCollection.getHolder()).thenReturn(retreatSite);
        final BattleState battleState =
            spy(
                givenBattleStateBuilder()
                    .battleSite(battleSite)
                    .attacker(attacker)
                    .attackingUnits(retreatingUnits)
                    .attackerRetreatTerritories(List.of(retreatSite))
                    .gameData(givenGameData().build())
                    .build());

        when(battleActions.queryRetreatTerritory(
                battleState, delegateBridge, attacker, List.of(retreatSite), "attacker retreat?"))
            .thenReturn(retreatSite);

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleState).retreatUnits(OFFENSE, retreatingUnits);
      }

      @Test
      void yesIfNoRestrictedCasualtiesAndRetreatTerritories() {
        final Unit unit = givenAnyUnit();
        when(unit.getOwner()).thenReturn(attacker);
        final Collection<Unit> retreatingUnits = Set.of(unit);
        final Territory retreatSite = mock(Territory.class);
        final UnitCollection retreatSiteCollection = mock(UnitCollection.class);
        when(retreatSite.getUnitCollection()).thenReturn(retreatSiteCollection);
        when(retreatSiteCollection.getHolder()).thenReturn(retreatSite);
        final BattleState battleState =
            spy(
                givenBattleStateBuilder()
                    .battleSite(battleSite)
                    .attacker(attacker)
                    .attackingUnits(retreatingUnits)
                    .attackerRetreatTerritories(List.of(retreatSite))
                    .gameData(
                        givenGameData()
                            .withWW2V2(false)
                            .withTransportCasualtiesRestricted(false)
                            .build())
                    .build());

        when(battleActions.queryRetreatTerritory(
                battleState, delegateBridge, attacker, List.of(retreatSite), "attacker retreat?"))
            .thenReturn(retreatSite);

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleState).retreatUnits(OFFENSE, retreatingUnits);
      }
    }

    @Nested
    class NonAmphibRetreatNotAllowed {

      @Test
      void noIfNoRetreatTerritories() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .battleSite(battleSite)
                .attacker(attacker)
                .attackerRetreatTerritories(List.of())
                .gameData(givenGameData().build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleActions, never())
            .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
      }

      @Test
      void noIfDefenselessTransportsAndRestrictedCasualties() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .battleSite(battleSite)
                .attacker(attacker)
                .defendingUnits(List.of(givenUnitSeaTransport()))
                .attackerRetreatTerritories(List.of(mock(Territory.class)))
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withTransportCasualtiesRestricted(true)
                        .build())
                .build();

        final OffensiveGeneralRetreat offensiveGeneralRetreat =
            new OffensiveGeneralRetreat(battleState, battleActions);
        offensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleActions, never())
            .queryRetreatTerritory(any(), any(), any(), anyCollection(), anyString());
      }
    }
  }
}
