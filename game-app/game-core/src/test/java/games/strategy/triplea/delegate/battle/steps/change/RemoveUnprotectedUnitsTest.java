package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsSea;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitSeaTransport;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoveUnprotectedUnitsTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;
  @Mock Territory battleSite;

  @Nested
  class GetNames {

    @Test
    void noNameIfNotWater() {
      when(battleSite.isWater()).thenReturn(false);
      final BattleState battleState = givenBattleStateBuilder().battleSite(battleSite).build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      assertThat(removeUnprotectedUnits.getAllStepDetails(), is(empty()));
    }

    @Test
    void noNameIfWaterButRestrictedCasualties() {
      when(battleSite.isWater()).thenReturn(true);
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(false).build();
      final BattleState battleState =
          givenBattleStateBuilder().battleSite(battleSite).gameData(gameData).build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      assertThat(removeUnprotectedUnits.getAllStepDetails(), is(empty()));
    }

    @Test
    void noNameIfWaterAndRestrictedCasualtiesButNoTransports() {
      when(battleSite.isWater()).thenReturn(true);
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();
      final BattleState battleState =
          givenBattleStateBuilder()
              .battleSite(battleSite)
              .gameData(gameData)
              .attackingUnits(List.of(givenAnyUnit()))
              .defendingUnits(List.of(givenAnyUnit()))
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      assertThat(removeUnprotectedUnits.getAllStepDetails(), is(empty()));
    }

    @Test
    void nameIfWaterAndRestrictedCasualtiesAndOffensiveTransports() {
      when(battleSite.isWater()).thenReturn(true);
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();
      final BattleState battleState =
          givenBattleStateBuilder()
              .battleSite(battleSite)
              .gameData(gameData)
              .attackingUnits(List.of(givenUnitSeaTransport()))
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      assertThat(removeUnprotectedUnits.getAllStepDetails(), hasSize(1));
    }

    @Test
    void nameIfWaterAndRestrictedCasualtiesAndDefensiveTransports() {
      when(battleSite.isWater()).thenReturn(true);
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();
      final BattleState battleState =
          givenBattleStateBuilder()
              .battleSite(battleSite)
              .gameData(gameData)
              .defendingUnits(List.of(givenUnitSeaTransport()))
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      assertThat(removeUnprotectedUnits.getAllStepDetails(), hasSize(1));
    }
  }

  @Nested
  class UnescortedTransports {

    @Test
    void noActionIfCasualtiesNotRestricted() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();
      final BattleState battleState = givenBattleStateBuilder().gameData(gameData).build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    @Test
    void noActionIfRetreatIsPossibleAndForAttacker() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();
      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of(mock(Territory.class)))
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    @Test
    void noActionIfNoRetreatButNoTransports() {
      final GameData gameData =
          givenGameData()
              .withTransportCasualtiesRestricted(true)
              .withAlliedRelationship(defender, attacker, false)
              .withAlliedRelationship(attacker, attacker, true)
              .build();
      final Unit unit = givenUnitSeaTransport();
      when(unit.getOwner()).thenReturn(attacker);
      when(battleSite.getUnits()).thenReturn(List.of(unit));

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    @Test
    void noActionIfNoRetreatAndTransportsButEscorted() {
      final GameData gameData =
          givenGameData()
              .withTransportCasualtiesRestricted(true)
              .withAlliedRelationship(defender, attacker, false)
              .withAlliedRelationship(attacker, attacker, true)
              .build();
      final Unit unit = givenUnitSeaTransport();
      when(unit.getOwner()).thenReturn(attacker);
      final Unit unit2 = givenUnitIsSea();
      when(unit2.getOwner()).thenReturn(attacker);
      when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    @Test
    void noActionIfNoRetreatAndUnescortedTransportsButNoEnemies() {
      final GamePlayer otherPlayer = mock(GamePlayer.class);
      final GameData gameData =
          givenGameData()
              .withTransportCasualtiesRestricted(true)
              .withAlliedRelationship(defender, attacker, false)
              .withAlliedRelationship(defender, otherPlayer, false)
              .withAlliedRelationship(attacker, attacker, true)
              .withAlliedRelationship(attacker, otherPlayer, false)
              .build();
      final Unit unit = givenUnitSeaTransport();
      when(unit.getOwner()).thenReturn(attacker);
      final Unit unit2 = givenUnitIsSea();
      when(unit2.getOwner()).thenReturn(otherPlayer);
      when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    @Test
    void actionForOffenderIfNoRetreatAndUnescortedTransportsAndEnemies() {
      final GameData gameData =
          givenGameData()
              .withTransportCasualtiesRestricted(true)
              .withAlliedRelationship(defender, attacker, false)
              .withAlliedRelationship(attacker, attacker, true)
              .withAlliedRelationship(attacker, defender, false)
              .withWarRelationship(attacker, attacker, false)
              .withWarRelationship(attacker, defender, true)
              .build();
      final Unit unit = givenUnitSeaTransport();
      when(unit.getOwner()).thenReturn(attacker);
      final Unit unit2 = givenUnitIsSea();
      when(unit2.getOwner()).thenReturn(defender);
      final UnitAttachment attachment =
          (UnitAttachment) unit2.getType().getAttachment(UNIT_ATTACHMENT_NAME);
      when(attachment.getAttack(attacker)).thenReturn(1);
      when(attachment.getMovement(attacker)).thenReturn(1);
      when(attachment.getTransportCapacity()).thenReturn(-1);
      when(unit2.getMovementLeft()).thenReturn(BigDecimal.ZERO.subtract(BigDecimal.ONE));
      when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(delegateBridge).addChange(any());
      verify(battleActions)
          .removeUnits(anyCollection(), eq(delegateBridge), any(), eq(BattleState.Side.OFFENSE));
    }

    @Test
    void actionForDefenderIfNoRetreatAndUnescortedTransportsAndEnemies() {
      final GameData gameData =
          givenGameData()
              .withTransportCasualtiesRestricted(true)
              .withAlliedRelationship(defender, defender, true)
              .withAlliedRelationship(defender, attacker, false)
              .withAlliedRelationship(attacker, defender, false)
              .withWarRelationship(defender, defender, false)
              .withWarRelationship(defender, attacker, true)
              .build();
      final Unit unit = givenUnitSeaTransport();
      when(unit.getOwner()).thenReturn(defender);
      final Unit unit2 = givenUnitIsSea();
      when(unit2.getOwner()).thenReturn(attacker);
      final UnitAttachment attachment =
          (UnitAttachment) unit2.getType().getAttachment(UNIT_ATTACHMENT_NAME);
      when(attachment.getAttack(defender)).thenReturn(1);
      when(attachment.getMovement(defender)).thenReturn(1);
      when(attachment.getTransportCapacity()).thenReturn(-1);
      when(unit2.getMovementLeft()).thenReturn(BigDecimal.ZERO.subtract(BigDecimal.ONE));
      when(battleSite.getUnits()).thenReturn(List.of(unit, unit2));

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(delegateBridge).addChange(any());
      verify(battleActions)
          .removeUnits(anyCollection(), eq(delegateBridge), any(), eq(BattleState.Side.DEFENSE));
    }
  }

  @Nested
  class InactiveUnits {
    @Test
    void noActionIfRetreatIsPossibleAndForAttacker() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();
      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of(mock(Territory.class)))
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    @Test
    void noActionIfNoRetreatButNoAttackingUnits() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .attacker(attacker)
              .defender(defender)
              .attackingUnits(List.of(givenAnyUnit()))
              .defendingUnits(List.of())
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    @Test
    void noActionIfNoRetreatButNoDefendingUnits() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .attacker(attacker)
              .defender(defender)
              .attackingUnits(List.of())
              .defendingUnits(List.of(givenAnyUnit()))
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    @Test
    void noActionIfNoRetreatAndUnitsButAllCanRoll() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();

      when(battleSite.isWater()).thenReturn(false);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .attackingUnits(List.of(givenUnitHasOffenseCombatAbility(attacker)))
              .defendingUnits(List.of(givenUnitHasDefenseCombatAbility(defender)))
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    Unit givenUnitHasOffenseCombatAbility(final GamePlayer player) {
      final Unit unit = givenAnyUnit();
      final UnitAttachment attachment =
          (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
      when(attachment.getAttack(player)).thenReturn(1);
      when(unit.getOwner()).thenReturn(player);
      return unit;
    }

    Unit givenUnitHasDefenseCombatAbility(final GamePlayer player) {
      final Unit unit = givenAnyUnit();
      final UnitAttachment attachment =
          (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
      when(attachment.getDefense(player)).thenReturn(1);
      when(unit.getOwner()).thenReturn(player);
      return unit;
    }

    @Test
    void noActionIfNoRetreatAndUnitsButNeitherSideCanRoll() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();

      when(battleSite.isWater()).thenReturn(false);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .attackingUnits(List.of(givenAnyUnit()))
              .defendingUnits(List.of(givenAnyUnit()))
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions, never()).removeUnits(anyCollection(), any(), any(), any());
    }

    @Test
    void actionForAttackerIfNoRetreatAndUnitsAndCanNotRollButOtherSideCan() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();

      when(battleSite.isWater()).thenReturn(false);

      final Unit unitToDie = givenAnyUnit();

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .attackingUnits(List.of(unitToDie))
              .defendingUnits(List.of(givenUnitHasDefenseCombatAbility(defender)))
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions)
          .removeUnits(
              argThat(arg -> arg.contains(unitToDie)),
              eq(delegateBridge),
              eq(battleSite),
              eq(BattleState.Side.OFFENSE));
    }

    @Test
    void actionForDefenderIfNoRetreatAndUnitsAndCanNotRollButOtherSideCan() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();

      when(battleSite.isWater()).thenReturn(false);

      final Unit unitToDie = givenAnyUnit();

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .attackingUnits(List.of(givenUnitHasOffenseCombatAbility(attacker)))
              .defendingUnits(List.of(unitToDie))
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions)
          .removeUnits(
              argThat(arg -> arg.contains(unitToDie)),
              eq(delegateBridge),
              eq(battleSite),
              eq(BattleState.Side.DEFENSE));
    }

    @Test
    void actionForDefenderIfNoRetreatAndUnitsAndCanNotRollAndOtherSideCanButUnitsAreInfra() {
      final GameData gameData = givenGameData().withTransportCasualtiesRestricted(true).build();

      when(battleSite.isWater()).thenReturn(false);

      final Unit unitToNotDie = givenAnyUnit();
      final UnitAttachment unitAttachment =
          (UnitAttachment) unitToNotDie.getType().getAttachment(UNIT_ATTACHMENT_NAME);
      when(unitAttachment.getIsInfrastructure()).thenReturn(true);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attackerRetreatTerritories(List.of())
              .battleSite(battleSite)
              .attacker(attacker)
              .defender(defender)
              .attackingUnits(List.of(givenUnitHasOffenseCombatAbility(attacker)))
              .defendingUnits(List.of(unitToNotDie))
              .gameData(gameData)
              .build();
      final RemoveUnprotectedUnits removeUnprotectedUnits =
          new RemoveUnprotectedUnits(battleState, battleActions);
      removeUnprotectedUnits.execute(executionStack, delegateBridge);
      verify(battleActions)
          .removeUnits(
              eq(List.of()), eq(delegateBridge), eq(battleSite), eq(BattleState.Side.DEFENSE));
    }
  }
}
