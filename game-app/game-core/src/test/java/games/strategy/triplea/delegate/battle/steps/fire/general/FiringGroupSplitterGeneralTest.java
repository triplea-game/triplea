package games.strategy.triplea.delegate.battle.steps.fire.general;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_FIRE_NON_SUBS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenSeaBattleSite;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsSea;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.steps.fire.FiringGroup;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiringGroupSplitterGeneralTest {

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

  @Test
  void oneFiringUnitVsOneTargetableUnitMakesOneFiringGroup() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenAnyUnit();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.NORMAL, UNITS)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withAlliedAirIndependent(true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit))
                    .defendingUnits(List.of(targetUnit))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getDisplayName()).isEqualTo(UNITS);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
    assertThat(firingGroups.get(0).getTargetUnits()).containsExactly(targetUnit);
    assertThat(firingGroups.get(0).isSuicideOnHit()).isFalse();
  }

  @Test
  void oneWaitingToDieFiringUnitVsOneTargetableUnitMakesOneFiringGroup() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenAnyUnit();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.NORMAL, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withAlliedAirIndependent(true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingWaitingToDie(List.of(fireUnit))
                    .defendingUnits(List.of(targetUnit))
                    .defendingWaitingToDie(List.of(mock(Unit.class)))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
    assertThat(firingGroups.get(0).getTargetUnits()).containsExactly(targetUnit);
    assertThat(firingGroups.get(0).isSuicideOnHit()).isFalse();
  }

  @Test
  void offensiveNormalTypeExcludesFirstStrike() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenAnyUnit();
    final Unit fireUnit2 = givenUnitFirstStrike();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.NORMAL, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withAlliedAirIndependent(true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit, fireUnit2))
                    .defendingUnits(List.of(targetUnit))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
  }

  @Test
  void defensiveNormalTypeExcludesFirstStrike() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenAnyUnit();
    final Unit fireUnit2 = givenUnitFirstStrike();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(DEFENSE, FiringGroupSplitterGeneral.Type.NORMAL, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(targetUnit))
                    .defendingUnits(List.of(fireUnit, fireUnit2))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
  }

  @Test
  void offensiveFirstStrikeTypeExcludesNonFirstStrike() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenUnitFirstStrike();
    final Unit fireUnit2 = givenAnyUnit();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.FIRST_STRIKE, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withAlliedAirIndependent(true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit, fireUnit2))
                    .defendingUnits(List.of(targetUnit))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
  }

  @Test
  void defensiveFirststrikeTypeExcludesNonFirstStrike() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenUnitFirstStrike();
    final Unit fireUnit2 = givenAnyUnit();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(DEFENSE, FiringGroupSplitterGeneral.Type.FIRST_STRIKE, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(targetUnit))
                    .defendingUnits(List.of(fireUnit, fireUnit2))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
  }

  @Test
  void excludeUnitsOfAlliesIfAlliedAirIndependentIsFalse() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenAnyUnit();
    when(fireUnit.getOwner()).thenReturn(attacker);
    final Unit fireUnit2 = givenAnyUnit();
    when(fireUnit2.getOwner()).thenReturn(mock(GamePlayer.class));

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.NORMAL, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withAlliedAirIndependent(false).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit, fireUnit2))
                    .defendingUnits(List.of(targetUnit))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
    assertThat(firingGroups.get(0).getTargetUnits()).containsExactly(targetUnit);
    assertThat(firingGroups.get(0).isSuicideOnHit()).isFalse();
  }

  @Test
  void doNotExcludeUnitsOfAlliesIfAlliedAirIndependentIsFalseButItIsDefense() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenAnyUnit();
    final Unit fireUnit2 = givenAnyUnit();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(DEFENSE, FiringGroupSplitterGeneral.Type.NORMAL, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(targetUnit))
                    .defendingUnits(List.of(fireUnit, fireUnit2))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit, fireUnit2);
    assertThat(firingGroups.get(0).getTargetUnits()).containsExactly(targetUnit);
    assertThat(firingGroups.get(0).isSuicideOnHit()).isFalse();
  }

  @Test
  void excludeSuicideOnDefenseTargetsIfOffense() {
    final Unit targetUnit = givenAnyUnit();
    final UnitAttachment targetUnitAttachment = targetUnit.getUnitAttachment();
    // this isn't actually called, so mark it as lenient in case the code later changes to call it
    // inadvertently
    lenient().when(targetUnitAttachment.getIsSuicideOnAttack()).thenReturn(true);
    final Unit suicideUnit = givenAnyUnit();
    final UnitAttachment suicideUnitAttachment = suicideUnit.getUnitAttachment();
    when(suicideUnitAttachment.getIsSuicideOnDefense()).thenReturn(true);
    final Unit fireUnit = givenAnyUnit();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.NORMAL, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withAlliedAirIndependent(true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit))
                    .defendingUnits(List.of(targetUnit, suicideUnit))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
    assertThat(firingGroups.get(0).getTargetUnits()).containsExactly(targetUnit);
    assertThat(firingGroups.get(0).isSuicideOnHit()).isFalse();
  }

  @Test
  void excludeSuicideOnAttackTargetsIfDefense() {
    final Unit targetUnit = givenAnyUnit();
    final UnitAttachment targetUnitAttachment = targetUnit.getUnitAttachment();
    // this isn't actually called, so mark it as lenient in case the code later changes to call it
    // inadvertently
    lenient().when(targetUnitAttachment.getIsSuicideOnDefense()).thenReturn(true);
    final Unit suicideUnit = givenAnyUnit();
    final UnitAttachment suicideUnitAttachment = suicideUnit.getUnitAttachment();
    when(suicideUnitAttachment.getIsSuicideOnAttack()).thenReturn(true);
    final Unit fireUnit = givenAnyUnit();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(DEFENSE, FiringGroupSplitterGeneral.Type.NORMAL, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(targetUnit, suicideUnit))
                    .defendingUnits(List.of(fireUnit))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
    assertThat(firingGroups.get(0).getTargetUnits()).containsExactly(targetUnit);
    assertThat(firingGroups.get(0).isSuicideOnHit()).isFalse();
  }

  @Test
  void excludeInfrastructureTargets() {
    final Unit targetUnit = givenAnyUnit();
    final Unit infrastructureUnit = givenAnyUnit();
    final UnitAttachment infrastructureUnitAttachment = infrastructureUnit.getUnitAttachment();
    when(infrastructureUnitAttachment.isInfrastructure()).thenReturn(true);
    final Unit fireUnit = givenAnyUnit();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.NORMAL, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(
                        givenGameData()
                            .withUnitTypeList(
                                List.of(
                                    targetUnit.getType(),
                                    infrastructureUnit.getType(),
                                    fireUnit.getType()))
                            .withAlliedAirIndependent(true)
                            .build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit))
                    .defendingUnits(List.of(targetUnit, infrastructureUnit))
                    .build());

    assertThat(firingGroups).hasSize(1);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
    assertThat(firingGroups.get(0).getTargetUnits()).containsExactly(targetUnit);
    assertThat(firingGroups.get(0).isSuicideOnHit()).isFalse();
  }

  @Test
  void noFiringGroupIfAllTargetsAreExcluded() {
    final Unit targetUnit = givenAnyUnit();
    final UnitAttachment infrastructureUnitAttachment = targetUnit.getUnitAttachment();
    when(infrastructureUnitAttachment.isInfrastructure()).thenReturn(true);
    final Unit fireUnit = givenAnyUnit();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.NORMAL, "")
            .apply(
                givenBattleStateBuilder()
                    .gameData(
                        givenGameData()
                            .withUnitTypeList(List.of(targetUnit.getType(), fireUnit.getType()))
                            .withAlliedAirIndependent(true)
                            .build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit))
                    .defendingUnits(List.of(targetUnit))
                    .build());

    assertThat(firingGroups).isEmpty();
  }

  private Unit givenUnitWithCannotTarget(String name, Set<UnitType> cannotTarget) {
    final Unit fireUnit = givenAnyUnit();
    lenient().when(fireUnit.getType().getName()).thenReturn(name);
    lenient().when(fireUnit.toString()).thenReturn(name);
    when(fireUnit.getUnitAttachment().getCanNotTarget()).thenReturn(cannotTarget);
    return fireUnit;
  }

  @Test
  void twoFiringGroupsWithCanNotTarget() {
    final Unit targetUnit = givenAnyUnit();
    final UnitType targetUnitType = targetUnit.getType();
    final Unit targetUnit2 = givenAnyUnit();
    final UnitType targetUnit2Type = targetUnit2.getType();
    final Unit targetUnit3 = givenAnyUnit();
    final UnitType targetUnit3Type = targetUnit3.getType();

    final Unit fireUnit =
        givenUnitWithCannotTarget("fireUnit", Set.of(targetUnit2Type, targetUnit3Type));
    final Unit fireUnit2 = givenUnitWithCannotTarget("fireUnit2", Set.of(targetUnitType));
    final Unit fireUnit3 = givenUnitWithCannotTarget("fireUnit3", Set.of(targetUnitType));

    // Iterate over several different orderings of attackers to ensure step names are deterministic
    // regardless of the order of units passed in.
    List<Unit> attackersOrdering1 = List.of(fireUnit, fireUnit2, fireUnit3);
    List<Unit> attackersOrdering2 = List.of(fireUnit, fireUnit3, fireUnit2);
    for (List<Unit> attackingUnits : List.of(attackersOrdering1, attackersOrdering2)) {
      final List<FiringGroup> firingGroups =
          FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.NORMAL, UNITS)
              .apply(
                  givenBattleStateBuilder()
                      .gameData(givenGameData().withAlliedAirIndependent(true).build())
                      .attacker(attacker)
                      .defender(defender)
                      .attackingUnits(attackingUnits)
                      .defendingUnits(List.of(targetUnit, targetUnit2, targetUnit3))
                      .build());

      assertThat(firingGroups).hasSize(2);
      assertThat(firingGroups.get(0).getDisplayName()).isEqualTo(UNITS + " fireUnit");
      assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(fireUnit);
      assertThat(firingGroups.get(0).getTargetUnits()).containsExactly(targetUnit);
      assertThat(firingGroups.get(0).isSuicideOnHit()).isFalse();

      assertThat(firingGroups.get(1).getDisplayName()).isEqualTo(UNITS + " fireUnit2");
      assertThat(firingGroups.get(1).getFiringUnits())
          .containsExactlyInAnyOrder(fireUnit2, fireUnit3);
      assertThat(firingGroups.get(1).getTargetUnits()).containsExactly(targetUnit2, targetUnit3);
      assertThat(firingGroups.get(1).isSuicideOnHit()).isFalse();
    }
  }

  @Test
  void twoGroupsWhenAirAndSeaVsSub() {
    final Unit airUnit = givenUnitIsAir();
    final UnitType airUnitType = airUnit.getType();

    final Unit attackingSeaUnit = givenUnitIsSea();

    final Unit subUnit = givenUnitIsSea();
    final UnitAttachment subUnitAttachment = subUnit.getUnitAttachment();
    when(subUnitAttachment.getCanNotBeTargetedBy()).thenReturn(Set.of(airUnitType));
    final Unit defendingSeaUnit = givenUnitIsSea();

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterGeneral.of(OFFENSE, FiringGroupSplitterGeneral.Type.NORMAL, UNITS)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withAlliedAirIndependent(true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(airUnit, attackingSeaUnit))
                    .defendingUnits(List.of(subUnit, defendingSeaUnit))
                    .battleSite(givenSeaBattleSite())
                    .build());

    assertThat(firingGroups).hasSize(2);
    assertThat(firingGroups.get(0).getDisplayName()).isEqualTo(AIR_FIRE_NON_SUBS);
    assertThat(firingGroups.get(0).getFiringUnits()).containsExactly(airUnit);
    assertThat(firingGroups.get(0).getTargetUnits()).containsExactly(defendingSeaUnit);
    assertThat(firingGroups.get(0).isSuicideOnHit()).isFalse();

    assertThat(firingGroups.get(1).getDisplayName()).isEqualTo(UNITS);
    assertThat(firingGroups.get(1).getFiringUnits()).containsExactly(attackingSeaUnit);
    assertThat(firingGroups.get(1).getTargetUnits()).containsExactly(subUnit, defendingSeaUnit);
    assertThat(firingGroups.get(1).isSuicideOnHit()).isFalse();
  }
}
