package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsInfrastructure;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitTransport;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckGeneralBattleEndTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;

  private CheckGeneralBattleEnd givenCheckGeneralBattleEnd(final BattleState battleState) {
    return new CheckGeneralBattleEnd(battleState, battleActions);
  }

  @Test
  void defenderWinsIfNoOffenseUnits() {
    final BattleState battleState = givenBattleStateBuilder().attackingUnits(List.of()).build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions).endBattle(IBattle.WhoWon.DEFENDER, delegateBridge);
  }

  @Test
  void defenderWinsIfOnlyOffenseInfrastructureUnits() {
    final BattleState battleState =
        givenBattleStateBuilder().attackingUnits(List.of(givenUnitIsInfrastructure())).build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions).endBattle(IBattle.WhoWon.DEFENDER, delegateBridge);
  }

  @Test
  void attackerWinsIfHasUnitsAndDefenderHasNone() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(givenGameData().build())
            .attackingUnits(List.of(givenAnyUnit()))
            .defendingUnits(List.of())
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions).endBattle(IBattle.WhoWon.ATTACKER, delegateBridge);
  }

  @Test
  void attackerWinsIfHasUnitsAndDefenderIsOnlyInfrastructureUnits() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(givenGameData().build())
            .attackingUnits(List.of(givenAnyUnit()))
            .defendingUnits(List.of(givenUnitIsInfrastructure()))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions).endBattle(IBattle.WhoWon.ATTACKER, delegateBridge);
  }

  @Test
  void nobodyWinsIfBothHaveZeroPowerUnits() {
    final Unit attackerUnit = givenAnyUnit();
    final Unit defenderUnit = givenAnyUnit();
    when(attackerUnit.getOwner()).thenReturn(attacker);
    when(defenderUnit.getOwner()).thenReturn(defender);
    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(givenGameData().build())
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions).endBattle(IBattle.WhoWon.DRAW, delegateBridge);
  }

  @Test
  void nobodyWinsIfBothHaveUnitsButMaxRound() {
    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(givenGameData().build())
            .attackingUnits(List.of(givenAnyUnit()))
            .defendingUnits(List.of(givenAnyUnit()))
            .battleRound(10)
            .maxBattleRounds(10)
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions).endBattle(IBattle.WhoWon.DRAW, delegateBridge);
  }

  @Test
  void battleIsNotDoneIfOffenseHasUnitWithPower() {
    final Unit attackerUnit = givenUnitWithAttackPower(attacker);
    final Unit defenderUnit = givenAnyUnit();

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(givenGameData().withDiceSides(6).build())
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions, never()).endBattle(any(), any());
  }

  private Unit givenUnitWithAttackPower(final GamePlayer player) {
    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(player);
    final UnitAttachment unitAttachment =
        (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
    when(unitAttachment.getAttack(player)).thenReturn(1);
    when(unitAttachment.getAttackRolls(player)).thenReturn(1);
    return unit;
  }

  @Test
  void battleIsNotDoneIfDefenseHasUnitWithPower() {
    final Unit attackerUnit = givenAnyUnit();
    when(attackerUnit.getOwner()).thenReturn(attacker);
    final Unit defenderUnit = givenUnitWithDefensePower(defender);

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(givenGameData().withDiceSides(6).build())
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions, never()).endBattle(any(), any());
  }

  private Unit givenUnitWithDefensePower(final GamePlayer player) {
    final Unit unit = givenAnyUnit();
    when(unit.getOwner()).thenReturn(player);
    final UnitAttachment unitAttachment =
        (UnitAttachment) unit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
    when(unitAttachment.getDefense(player)).thenReturn(1);
    when(unitAttachment.getDefenseRolls(player)).thenReturn(1);
    return unit;
  }

  @Test
  void stalemateRetreatPossibleIfAttackerAllCanRetreatOnStalemate() {
    final Unit attackerUnit = givenAnyUnit();
    final Unit defenderUnit = givenAnyUnit();
    when(attackerUnit.getOwner()).thenReturn(attacker);
    when(defenderUnit.getOwner()).thenReturn(defender);

    final UnitAttachment unitAttachment =
        (UnitAttachment) attackerUnit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
    when(unitAttachment.getCanRetreatOnStalemate()).thenReturn(true);

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(givenGameData().build())
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions, never()).endBattle(IBattle.WhoWon.DRAW, delegateBridge);
  }

  @Test
  void stalemateRetreatNotPossibleIfAttackerNotAllCanRetreatOnStalemate() {
    final Unit attackerUnit = givenAnyUnit();
    when(attackerUnit.getOwner()).thenReturn(attacker);
    final UnitAttachment unitAttachment =
        (UnitAttachment) attackerUnit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
    when(unitAttachment.getCanRetreatOnStalemate()).thenReturn(true);

    final Unit attackerUnit2 = givenAnyUnit();
    when(attackerUnit2.getOwner()).thenReturn(attacker);

    final Unit defenderUnit = givenAnyUnit();
    when(defenderUnit.getOwner()).thenReturn(defender);

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(givenGameData().build())
            .attackingUnits(List.of(attackerUnit, attackerUnit2))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions).endBattle(IBattle.WhoWon.DRAW, delegateBridge);
  }

  @Test
  void stalemateRetreatPossibleIfOnlyNonCombatTransports() {
    final Unit attackerUnit = givenUnitTransport();
    final Unit defenderUnit = givenUnitTransport();
    when(attackerUnit.getOwner()).thenReturn(attacker);
    when(defenderUnit.getOwner()).thenReturn(defender);

    final UnitAttachment unitAttachment =
        (UnitAttachment) attackerUnit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
    when(unitAttachment.getCanRetreatOnStalemate()).thenReturn(null);

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(
                givenGameData()
                    .withLhtrHeavyBombers(false)
                    .withTransportCasualtiesRestricted(true)
                    .build())
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions, never()).endBattle(IBattle.WhoWon.DRAW, delegateBridge);
  }
}
