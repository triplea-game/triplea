package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.Constants.TERRITORYEFFECT_ATTACHMENT_NAME;
import static games.strategy.triplea.Constants.TERRITORY_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
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

/*
 * Remember, this step is not for choosing the retreat territory; it only executes the
 * retreat to the territory previously chosen in DefenderFightOrRetreat. So these tests
 * should only be evaluating the one territory stored in defendersRetreatTo.
 */

@ExtendWith(MockitoExtension.class)
class DefensiveGeneralRetreatTest extends AbstractClientSettingTestCase {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;
  @Mock Territory battleSite;

  // Production land territories always carry a TerritoryAttachment; mock retreat sites
  // need one stubbed so territory-effect lookups during retreat don't throw.
  private static Territory givenLandRetreatSite() {
    final Territory retreatSite = mock(Territory.class);
    final UnitCollection collection = mock(UnitCollection.class);
    when(retreatSite.getUnitCollection()).thenReturn(collection);
    when(collection.getHolder()).thenReturn(retreatSite);
    when(retreatSite.getAttachment(TERRITORYEFFECT_ATTACHMENT_NAME)).thenReturn(null);
    return retreatSite;
  }

  // Land retreat site whose territory effect blocks the given unit's type from entering.
  // The site is filtered out before retreat executes, so collection stubs are not exercised.
  private static Territory givenLandRetreatSiteBlockedFor(final Unit unit) {
    final Territory retreatSite = mock(Territory.class);
    final Territory battleSite = mock(Territory.class);
    final TerritoryEffect effect = mock(TerritoryEffect.class);
    final TerritoryEffectAttachment effectAttachment = mock(TerritoryEffectAttachment.class);
    lenient().doReturn(List.of(unit.getType())).when(effectAttachment).getUnitsNotAllowed();
    lenient()
        .doReturn(effectAttachment)
        .when(effect)
        .getAttachment(TERRITORYEFFECT_ATTACHMENT_NAME);
    final TerritoryAttachment territoryAttachment = mock(TerritoryAttachment.class);
    lenient().when(territoryAttachment.getTerritoryEffect()).thenReturn(List.of(effect));
    lenient()
        .when(retreatSite.getAttachment(TERRITORY_ATTACHMENT_NAME))
        .thenReturn(territoryAttachment);
    final UnitCollection collection = mock(UnitCollection.class);
    return retreatSite;
  }

  @Nested
  class GetNames {
    @Nested
    class NonAmphibiousRetreat {
      @Test
      void noIfNoRetreatTerritory() {
        final BattleState battleState =
            givenBattleStateBuilder()
                .defendersRetreatTo(null)
                .gameData(givenGameData().build())
                .build();

        final DefensiveGeneralRetreat defensiveGeneralRetreat =
            new DefensiveGeneralRetreat(battleState, battleActions);
        assertThat(defensiveGeneralRetreat.getAllStepDetails(), is(empty()));
      }
    }

    @Test
    void yesIfRetreatTerritory() {
      final BattleState battleState =
          givenBattleStateBuilder()
              .defendersRetreatTo(mock(Territory.class))
              .gameData(givenGameData().build())
              .build();

      final DefensiveGeneralRetreat defensiveGeneralRetreat =
          new DefensiveGeneralRetreat(battleState, battleActions);
      assertThat(defensiveGeneralRetreat.getAllStepDetails(), hasSize(1));
    }
  }

  @Nested
  class Execute {
    @Mock GamePlayer defender;
    @Mock IDisplay display;
    @Mock ISound sound;
    @Mock IDelegateHistoryWriter historyWriter;
    @Mock UnitCollection battleSiteCollection;

    @Test
    void noIfOver() {
      final Unit unit = givenAnyUnit();
      final Collection<Unit> retreatingUnits = Set.of(unit);
      final BattleState battleStateSpy =
          spy(
              givenBattleStateBuilder()
                  .over(true)
                  .defendingUnits(retreatingUnits)
                  .defendersRetreatTo(mock(Territory.class))
                  .gameData(givenGameData().build())
                  .build());

      final DefensiveGeneralRetreat defensiveGeneralRetreat =
          new DefensiveGeneralRetreat(battleStateSpy, battleActions);
      defensiveGeneralRetreat.execute(executionStack, delegateBridge);
      verify(battleStateSpy, never()).retreatUnits(DEFENSE, retreatingUnits);
    }

    @Nested
    class NonAmphibRetreatAllowed {

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
      void yesIfRetreatTerritory() {
        final Unit unit = givenAnyUnit();
        when(unit.getOwner()).thenReturn(defender);
        when(unit.getUnitAttachment().getMovement(defender)).thenReturn(1);
        when(unit.getUnitAttachment().canNotMoveDuringCombatMove()).thenReturn(false);
        when(unit.getUnitAttachment().getCanDefensiveRetreat()).thenReturn(true);
        final Collection<Unit> retreatingUnits = Set.of(unit);
        final Territory retreatSite = givenLandRetreatSite();
        final BattleState battleStateSpy =
            spy(
                givenBattleStateBuilder()
                    .battleSite(battleSite)
                    .defender(defender)
                    .defendingUnits(retreatingUnits)
                    .defendersRetreatTo(retreatSite)
                    .gameData(givenGameData().build())
                    .build());

        final DefensiveGeneralRetreat defensiveGeneralRetreat =
            new DefensiveGeneralRetreat(battleStateSpy, battleActions);
        defensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleStateSpy).retreatUnits(DEFENSE, retreatingUnits);
      }
    }

    @Nested
    class NonAmphibRetreatNotAllowed {

      @Test
      void noIfNoRetreatTerritory() {
        final Unit unit = givenAnyUnit();
        final Collection<Unit> retreatingUnits = Set.of(unit);
        final BattleState battleStateSpy =
            spy(
                givenBattleStateBuilder()
                    .battleSite(battleSite)
                    .defender(defender)
                    .defendingUnits(retreatingUnits)
                    .defendersRetreatTo(null)
                    .gameData(givenGameData().build())
                    .build());

        final DefensiveGeneralRetreat defensiveGeneralRetreat =
            new DefensiveGeneralRetreat(battleStateSpy, battleActions);
        defensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleStateSpy, never()).retreatUnits(DEFENSE, retreatingUnits);
      }

      @Test
      void noIfTerritoryEffectBlocksRetreat() {
        final Unit unit = givenAnyUnit();
        when(unit.getOwner()).thenReturn(defender);
        when(defender.getName()).thenReturn("defender");
        when(battleSite.getUnitCollection()).thenReturn(battleSiteCollection);
        final Collection<Unit> retreatingUnits = Set.of(unit);
        final Territory blockedSite = givenLandRetreatSiteBlockedFor(unit);

        final BattleState battleStateSpy =
            spy(
                givenBattleStateBuilder()
                    .battleSite(battleSite)
                    .defender(defender)
                    .defendingUnits(retreatingUnits)
                    .defendersRetreatTo(blockedSite)
                    .gameData(givenGameData().build())
                    .build());

        final DefensiveGeneralRetreat defensiveGeneralRetreat =
            new DefensiveGeneralRetreat(battleStateSpy, battleActions);
        defensiveGeneralRetreat.execute(executionStack, delegateBridge);
        verify(battleStateSpy, never()).retreatUnits(DEFENSE, retreatingUnits);
      }
    }
  }
}
