package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsCombatAa;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.battle.steps.fire.FiringGroup;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiringGroupSplitterAaTest {

  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;

  @Test
  void oneFiringUnitVsOneTargetableUnitMakesOneFiringGroup() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenUnitIsCombatAa(Set.of(targetUnit.getType()), attacker, OFFENSE);
    when(fireUnit.getOwner()).thenReturn(attacker);

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterAa.of(OFFENSE)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withWarRelationship(defender, attacker, true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit))
                    .defendingUnits(List.of(targetUnit))
                    .build());

    assertThat(firingGroups, hasSize(1));
    assertThat(firingGroups.get(0).getFiringUnits(), contains(fireUnit));
    assertThat(firingGroups.get(0).getTargetUnits(), contains(targetUnit));
    assertThat(firingGroups.get(0).isSuicideOnHit(), is(false));
  }

  @Test
  void oneWaitingToDieFiringUnitVsOneTargetableUnitMakesOneFiringGroup() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenUnitIsCombatAa(Set.of(targetUnit.getType()), attacker, OFFENSE);
    when(fireUnit.getOwner()).thenReturn(attacker);

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterAa.of(OFFENSE)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withWarRelationship(defender, attacker, true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingWaitingToDie(List.of(fireUnit))
                    .defendingUnits(List.of(targetUnit))
                    .defendingWaitingToDie(List.of(mock(Unit.class)))
                    .build());

    assertThat(firingGroups, hasSize(1));
    assertThat(firingGroups.get(0).getFiringUnits(), contains(fireUnit));
    assertThat(firingGroups.get(0).getTargetUnits(), contains(targetUnit));
    assertThat(firingGroups.get(0).isSuicideOnHit(), is(false));
  }

  @Test
  void nonAaUnitsAreNotIncludedInFiringGroup() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenUnitIsCombatAa(Set.of(targetUnit.getType()), attacker, OFFENSE);
    when(fireUnit.getOwner()).thenReturn(attacker);

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterAa.of(OFFENSE)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withWarRelationship(defender, attacker, true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit, givenAnyUnit()))
                    .defendingUnits(List.of(targetUnit, givenAnyUnit()))
                    .build());

    assertThat(firingGroups, hasSize(1));
    assertThat(firingGroups.get(0).getFiringUnits(), contains(fireUnit));
    assertThat(firingGroups.get(0).getTargetUnits(), contains(targetUnit));
    assertThat(firingGroups.get(0).isSuicideOnHit(), is(false));
  }

  @Test
  void aaUnitWithMultipleTargetAaCombineThemIntoJustOneGroup() {
    final Unit targetUnit = givenAnyUnit();
    final Unit targetUnit2 = givenAnyUnit();
    final Unit fireUnit =
        givenUnitIsCombatAa(Set.of(targetUnit.getType(), targetUnit2.getType()), attacker, OFFENSE);
    when(fireUnit.getOwner()).thenReturn(attacker);

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterAa.of(OFFENSE)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withWarRelationship(defender, attacker, true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit))
                    .defendingUnits(List.of(targetUnit, targetUnit2))
                    .build());

    assertThat(firingGroups, hasSize(1));
    assertThat(firingGroups.get(0).getFiringUnits(), contains(fireUnit));
    assertThat(firingGroups.get(0).getTargetUnits(), contains(targetUnit, targetUnit2));
    assertThat(firingGroups.get(0).isSuicideOnHit(), is(false));
  }

  @Test
  void twoAaUnitsWithDifferentAaTypeCreateSeparateFiringGroups() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit =
        givenUnitIsCombatAa(Set.of(targetUnit.getType()), attacker, OFFENSE, "Type1");
    when(fireUnit.getOwner()).thenReturn(attacker);

    final Unit targetUnit2 = givenAnyUnit();
    final Unit fireUnit2 =
        givenUnitIsCombatAa(Set.of(targetUnit2.getType()), attacker, OFFENSE, "Type2");
    when(fireUnit2.getOwner()).thenReturn(attacker);

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterAa.of(OFFENSE)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withWarRelationship(defender, attacker, true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit, fireUnit2))
                    .defendingUnits(List.of(targetUnit, targetUnit2))
                    .build());

    firingGroups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    assertThat(firingGroups, hasSize(2));
    assertThat(firingGroups.get(0).getFiringUnits(), contains(fireUnit));
    assertThat(firingGroups.get(0).getTargetUnits(), contains(targetUnit));
    assertThat(firingGroups.get(0).isSuicideOnHit(), is(false));

    assertThat(firingGroups.get(1).getFiringUnits(), contains(fireUnit2));
    assertThat(firingGroups.get(1).getTargetUnits(), contains(targetUnit2));
    assertThat(firingGroups.get(1).isSuicideOnHit(), is(false));
  }

  @Test
  @DisplayName(
      "Two AA units with different aaType will create separate firing groups"
          + "even if they share the same target unit")
  void twoAaUnitsWithDifferentAaTypeCreateSeparateFiringGroupsWithSameTargetUnit() {
    final Unit commonTargetUnit = givenAnyUnit();

    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit =
        givenUnitIsCombatAa(
            Set.of(targetUnit.getType(), commonTargetUnit.getType()), attacker, OFFENSE, "Type1");
    when(fireUnit.getOwner()).thenReturn(attacker);

    final Unit targetUnit2 = givenAnyUnit();
    final Unit fireUnit2 =
        givenUnitIsCombatAa(
            Set.of(targetUnit2.getType(), commonTargetUnit.getType()), attacker, OFFENSE, "Type2");
    when(fireUnit2.getOwner()).thenReturn(attacker);

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterAa.of(OFFENSE)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withWarRelationship(defender, attacker, true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit, fireUnit2))
                    .defendingUnits(List.of(targetUnit, targetUnit2, commonTargetUnit))
                    .build());

    firingGroups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    assertThat(firingGroups, hasSize(2));
    assertThat(firingGroups.get(0).getFiringUnits(), contains(fireUnit));
    assertThat(firingGroups.get(0).getTargetUnits(), contains(targetUnit, commonTargetUnit));
    assertThat(firingGroups.get(0).isSuicideOnHit(), is(false));

    assertThat(firingGroups.get(1).getFiringUnits(), contains(fireUnit2));
    assertThat(firingGroups.get(1).getTargetUnits(), contains(targetUnit2, commonTargetUnit));
    assertThat(firingGroups.get(1).isSuicideOnHit(), is(false));
  }

  @Test
  void separateFiringGroupBySuicideOnHit() {
    final Unit targetUnit = givenAnyUnit();
    final Unit fireUnit = givenUnitIsCombatAa(Set.of(targetUnit.getType()), attacker, OFFENSE);
    when(fireUnit.getOwner()).thenReturn(attacker);

    final Unit fireUnit2 = givenUnitIsCombatAa(Set.of(targetUnit.getType()), attacker, OFFENSE);
    when(fireUnit2.getOwner()).thenReturn(attacker);
    final UnitAttachment unitAttachment =
        (UnitAttachment) fireUnit2.getType().getAttachment(UNIT_ATTACHMENT_NAME);
    when(unitAttachment.isSuicideOnHit()).thenReturn(true);

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterAa.of(OFFENSE)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withWarRelationship(defender, attacker, true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit, fireUnit2))
                    .defendingUnits(List.of(targetUnit))
                    .build());

    firingGroups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    assertThat(firingGroups, hasSize(2));
    assertThat(firingGroups.get(0).getFiringUnits(), contains(fireUnit));
    assertThat(firingGroups.get(0).getTargetUnits(), contains(targetUnit));
    assertThat(firingGroups.get(0).isSuicideOnHit(), is(false));

    assertThat(firingGroups.get(1).getFiringUnits(), contains(fireUnit2));
    assertThat(firingGroups.get(1).getTargetUnits(), contains(targetUnit));
    assertThat(firingGroups.get(1).isSuicideOnHit(), is(true));
  }

  @Test
  void firingGroupIgnoresTransportedUnits() {
    final Unit targetUnit = givenAnyUnit();
    final Unit targetUnit2 = givenAnyUnit();
    when(targetUnit2.getTransportedBy()).thenReturn(mock(Unit.class));
    final Unit fireUnit =
        givenUnitIsCombatAa(Set.of(targetUnit.getType(), targetUnit2.getType()), attacker, OFFENSE);
    when(fireUnit.getOwner()).thenReturn(attacker);

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterAa.of(OFFENSE)
            .apply(
                givenBattleStateBuilder()
                    .gameData(givenGameData().withWarRelationship(defender, attacker, true).build())
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(fireUnit))
                    .defendingUnits(List.of(targetUnit, targetUnit2))
                    .build());

    assertThat(firingGroups, hasSize(1));
    assertThat(firingGroups.get(0).getFiringUnits(), contains(fireUnit));
    assertThat(firingGroups.get(0).getTargetUnits(), contains(targetUnit));
    assertThat(firingGroups.get(0).isSuicideOnHit(), is(false));
  }

  @Test
  void defensiveFiringGroupTargetsAirbornUnits() {
    final GameData gameData =
        givenGameData()
            .withWarRelationship(attacker, defender, true)
            .withTechnologyFrontier()
            .build();

    final Unit targetUnit = givenAnyUnit();
    final Unit targetUnit2 = givenAnyUnit();
    when(targetUnit2.getAirborne()).thenReturn(true);

    // attach airborne technology to the attacker so the defender can shoot them down
    final TechAdvance techAdvance = mock(TechAdvance.class);
    final TechAbilityAttachment techAbilityAttachment =
        new TechAbilityAttachment("", techAdvance, gameData);
    techAbilityAttachment
        .getProperty("airborneTargettedByAA")
        .ifPresent(
            property -> {
              try {
                property.setValue(Map.of("AntiAirGun", Set.of(targetUnit2.getType())));
              } catch (final MutableProperty.InvalidValueException e) {
                // should not happen
              }
            });
    when(techAdvance.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME))
        .thenReturn(techAbilityAttachment);
    when(gameData.getTechnologyFrontier().getTechs()).thenReturn(List.of(techAdvance));

    final TechAttachment techAttachment = mock(TechAttachment.class);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAdvance.hasTech(techAttachment)).thenReturn(true);

    final Unit fireUnit = givenUnitIsCombatAa(Set.of(targetUnit.getType()), defender, DEFENSE);
    when(fireUnit.getOwner()).thenReturn(defender);

    final List<FiringGroup> firingGroups =
        FiringGroupSplitterAa.of(DEFENSE)
            .apply(
                givenBattleStateBuilder()
                    .gameData(gameData)
                    .attacker(attacker)
                    .defender(defender)
                    .attackingUnits(List.of(targetUnit, targetUnit2))
                    .defendingUnits(List.of(fireUnit))
                    .build());

    assertThat(firingGroups, hasSize(1));
    assertThat(firingGroups.get(0).getFiringUnits(), contains(fireUnit));
    assertThat(firingGroups.get(0).getTargetUnits(), contains(targetUnit, targetUnit2));
    assertThat(firingGroups.get(0).isSuicideOnHit(), is(false));
  }
}
