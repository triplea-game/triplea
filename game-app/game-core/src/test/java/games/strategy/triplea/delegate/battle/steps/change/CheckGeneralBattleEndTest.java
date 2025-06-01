package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsInfrastructure;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitSeaTransport;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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

  @BeforeEach
  void setUp() {
    final GameData gameData = new GameData();

    lenient().when(attacker.getName()).thenReturn("attacker");
    lenient().when(attacker.getData()).thenReturn(gameData);
    lenient().when(defender.getName()).thenReturn("defender");
    lenient().when(defender.getData()).thenReturn(gameData);
  }

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
  void battleDoesNotEndIfOffenseHasUnitWithPower() {
    final Unit attackerUnit = givenUnitWithAttackPower(attacker);
    final Unit defenderUnit = givenAnyUnit();
    lenient().when(defenderUnit.getOwner()).thenReturn(defender);

    final BattleState battleState =
        givenBattleStateBuilder(attacker, defender)
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
  void battleDoesNotEndIfDefenseHasUnitWithPower() {
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
    when(unitAttachment.getCanRetreatOnStalemate()).thenReturn(Optional.of(true));

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
    when(unitAttachment.getCanRetreatOnStalemate()).thenReturn(Optional.of(true));

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
    final Unit attackerUnit = givenUnitSeaTransport();
    final Unit defenderUnit = givenUnitSeaTransport();
    when(attackerUnit.getOwner()).thenReturn(attacker);
    when(defenderUnit.getOwner()).thenReturn(defender);

    final UnitAttachment unitAttachment =
        (UnitAttachment) attackerUnit.getType().getAttachment(UNIT_ATTACHMENT_NAME);
    when(unitAttachment.getCanRetreatOnStalemate()).thenReturn(Optional.empty());

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(
                givenGameData()
                    .withAlliedAirIndependent(false)
                    .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                    .withTransportCasualtiesRestricted(true)
                    .withLhtrHeavyBombers(false)
                    .build())
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions, never()).endBattle(IBattle.WhoWon.DRAW, delegateBridge);
  }

  @Test
  void nobodyWinsIfBothCanNotTargetEachOtherInGeneralCombat() {
    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType attackerUnitType = new UnitType("attacker", gameData);
    final UnitAttachment attackerUnitAttachment =
        new UnitAttachment("attacker", attackerUnitType, gameData);
    attackerUnitAttachment.setAttack(1).setAttackRolls(1);
    attackerUnitType.addAttachment(UNIT_ATTACHMENT_NAME, attackerUnitAttachment);

    final UnitType defenderUnitType = new UnitType("defender", gameData);
    final UnitAttachment defenderUnitAttachment =
        new UnitAttachment("defender", defenderUnitType, gameData);
    defenderUnitAttachment.setDefense(1).setDefenseRolls(1);
    defenderUnitType.addAttachment(UNIT_ATTACHMENT_NAME, defenderUnitAttachment);

    attackerUnitAttachment.setCanNotTarget(Set.of(defenderUnitType));
    defenderUnitAttachment.setCanNotTarget(Set.of(attackerUnitType));

    final Unit attackerUnit = attackerUnitType.createTemp(1, attacker).get(0);
    final Unit defenderUnit = defenderUnitType.createTemp(1, defender).get(0);

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(gameData)
            .attacker(attacker)
            .defender(defender)
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions, times(1).description("No one can hit each other so it is a stalemate"))
        .endBattle(IBattle.WhoWon.DRAW, delegateBridge);
  }

  @Test
  void nobodyWinsIfBothCanNotTargetEachOtherInGeneralCombat2() {
    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType attackerUnitType = new UnitType("attacker", gameData);
    final UnitAttachment attackerUnitAttachment =
        new UnitAttachment("attacker", attackerUnitType, gameData);
    attackerUnitAttachment.setAttack(0).setAttackRolls(0);
    attackerUnitType.addAttachment(UNIT_ATTACHMENT_NAME, attackerUnitAttachment);

    final UnitType defenderUnitType = new UnitType("defender", gameData);
    final UnitAttachment defenderUnitAttachment =
        new UnitAttachment("defender", defenderUnitType, gameData);
    defenderUnitAttachment.setDefense(1).setDefenseRolls(1);
    // A unit that can't target attacker.
    defenderUnitAttachment.setCanNotTarget(Set.of(attackerUnitType));
    defenderUnitType.addAttachment(UNIT_ATTACHMENT_NAME, defenderUnitAttachment);

    final UnitType defenderUnitType2 = new UnitType("defender2", gameData);
    final UnitAttachment defenderUnitAttachment2 =
        new UnitAttachment("defender2", defenderUnitType2, gameData);
    // A unit that can target attacker, but has no defense power.
    defenderUnitAttachment2.setDefense(0).setDefenseRolls(1);
    defenderUnitType2.addAttachment(UNIT_ATTACHMENT_NAME, defenderUnitAttachment2);

    final Unit attackerUnit = attackerUnitType.createTemp(1, attacker).get(0);
    final Unit defenderUnit = defenderUnitType.createTemp(1, defender).get(0);
    final Unit defenderUnit2 = defenderUnitType2.createTemp(1, defender).get(0);

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(gameData)
            .attacker(attacker)
            .defender(defender)
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit, defenderUnit2))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(battleActions, times(1).description("No one can hit each other so it is a stalemate"))
        .endBattle(IBattle.WhoWon.DRAW, delegateBridge);
  }

  @Test
  void battleIsNotOverIfOffenseCanStillTargetInGeneralCombat() {
    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType attackerUnitType = new UnitType("attacker", gameData);
    final UnitAttachment attackerUnitAttachment =
        new UnitAttachment("attacker", attackerUnitType, gameData);
    attackerUnitAttachment.setAttack(1).setAttackRolls(1);
    attackerUnitType.addAttachment(UNIT_ATTACHMENT_NAME, attackerUnitAttachment);

    final UnitType defenderUnitType = new UnitType("defender", gameData);
    final UnitAttachment defenderUnitAttachment =
        new UnitAttachment("defender", defenderUnitType, gameData);
    defenderUnitAttachment.setDefense(1).setDefenseRolls(1);
    defenderUnitType.addAttachment(UNIT_ATTACHMENT_NAME, defenderUnitAttachment);

    defenderUnitAttachment.setCanNotTarget(Set.of(attackerUnitType));

    final Unit attackerUnit = attackerUnitType.createTemp(1, attacker).get(0);
    final Unit defenderUnit = defenderUnitType.createTemp(1, defender).get(0);

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(gameData)
            .attacker(attacker)
            .defender(defender)
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(
            battleActions,
            never().description("attacker can still hit the defender so no endBattle"))
        .endBattle(any(), any());
  }

  @Test
  void battleIsNotOverIfDefenseCanStillTargetInGeneralCombat() {
    final GameData gameData = givenGameData().withDiceSides(6).build();

    final UnitType attackerUnitType = new UnitType("attacker", gameData);
    final UnitAttachment attackerUnitAttachment =
        new UnitAttachment("attacker", attackerUnitType, gameData);
    attackerUnitAttachment.setAttack(1).setAttackRolls(1);
    attackerUnitType.addAttachment(UNIT_ATTACHMENT_NAME, attackerUnitAttachment);

    final UnitType defenderUnitType = new UnitType("defender", gameData);
    final UnitAttachment defenderUnitAttachment =
        new UnitAttachment("defender", defenderUnitType, gameData);
    defenderUnitAttachment.setDefense(1).setDefenseRolls(1);
    defenderUnitType.addAttachment(UNIT_ATTACHMENT_NAME, defenderUnitAttachment);

    attackerUnitAttachment.setCanNotTarget(Set.of(defenderUnitType));

    final Unit attackerUnit = attackerUnitType.createTemp(1, attacker).get(0);
    final Unit defenderUnit = defenderUnitType.createTemp(1, defender).get(0);

    final BattleState battleState =
        givenBattleStateBuilder()
            .gameData(gameData)
            .attacker(attacker)
            .defender(defender)
            .attackingUnits(List.of(attackerUnit))
            .defendingUnits(List.of(defenderUnit))
            .build();

    final CheckGeneralBattleEnd checkGeneralBattleEnd = givenCheckGeneralBattleEnd(battleState);
    checkGeneralBattleEnd.execute(executionStack, delegateBridge);

    verify(
            battleActions,
            never().description("defender can still hit the attacker so no endBattle"))
        .endBattle(any(), any());
  }
}
