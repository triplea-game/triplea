package games.strategy.engine.data.battle.phase;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.unit.ability.CombatUnitAbility;
import games.strategy.engine.data.unit.ability.ConvertUnitAbility;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattlePhaseStep;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BattlePhaseListTest {

  BattlePhaseList battlePhaseList;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;
  @Mock GameData gameData;

  @BeforeEach
  void initializeBattlePhaseList() {
    battlePhaseList = new BattlePhaseList();
  }

  @Nested
  class BattleStepCalculation {

    @Test
    void noUnitAbilitiesHasNoSteps() {
      final BattleState battleState = givenBattleStateBuilder().build();

      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat("The phases have no abilities so no steps should be created.", steps, is(empty()));
    }

    @Test
    void unitAbilityButNoUnitTypesAttachedCreatesNoSteps() {
      final BattleState battleState = givenBattleStateBuilder().attacker(attacker).build();

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, CombatUnitAbility.builder().name("1").build());
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat("Ability has no unit types attached, so no steps", steps, is(empty()));
    }

    @Test
    void unitAbilityWithAttachedUnitTypesButNoActualUnitsCreatesNoSteps() {
      final BattleState battleState = givenBattleStateBuilder().attacker(attacker).build();

      final UnitType unitType = new UnitType("infantry", gameData);

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(
                    attacker,
                    CombatUnitAbility.builder()
                        .name("1")
                        .attachedUnitTypes(List.of(unitType))
                        .build());
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "Ability unit types attached but they aren't in the battle state, so no steps",
          steps,
          is(empty()));
    }

    @Test
    void phaseWithOneAbilityMakesOneStep() {
      final UnitType unitType = new UnitType("infantry", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .attackingUnits(unitType.createTemp(1, attacker))
              .build();

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(
                    attacker,
                    CombatUnitAbility.builder()
                        .name("1")
                        .attachedUnitTypes(List.of(unitType))
                        .build());
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "Phase has one ability with attached unit types and that unit type is in the battle,"
              + " so create a step",
          steps,
          hasSize(1));
    }

    @Test
    void phaseWithTwoAbilitiesMakesOneStep() {
      final UnitType unitType = new UnitType("infantry", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .attackingUnits(unitType.createTemp(1, attacker))
              .build();

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(
                    attacker,
                    CombatUnitAbility.builder()
                        .name("1")
                        .attachedUnitTypes(List.of(unitType))
                        .build());
                battlePhase.addAbility(
                    attacker,
                    CombatUnitAbility.builder()
                        .name("2")
                        .attachedUnitTypes(List.of(unitType))
                        .build());
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "Phase has two abilities with attached unit types and that unit type is in the battle,"
              + " so create a step for the phase. Only one step is needed for the phase.",
          steps,
          hasSize(1));
    }

    @Test
    void twoPhasesWithAnAbilityMakeTwoSteps() {
      final UnitType unitType = new UnitType("infantry", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .attackingUnits(unitType.createTemp(1, attacker))
              .build();

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(
                    attacker,
                    CombatUnitAbility.builder()
                        .name("1")
                        .attachedUnitTypes(List.of(unitType))
                        .build());
              });
      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(
                    attacker,
                    CombatUnitAbility.builder()
                        .name("2")
                        .attachedUnitTypes(List.of(unitType))
                        .build());
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat("Two phases each with their own ability get their own steps", steps, hasSize(2));
    }

    @Test
    void phaseWithBothSidesMakeTwoSteps() {
      final UnitType unitType = new UnitType("infantry", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .defender(defender)
              .attackingUnits(unitType.createTemp(1, attacker))
              .defendingUnits(unitType.createTemp(1, defender))
              .build();

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(
                    attacker,
                    CombatUnitAbility.builder()
                        .name("1")
                        .attachedUnitTypes(List.of(unitType))
                        .build());
                battlePhase.addAbility(
                    defender,
                    CombatUnitAbility.builder()
                        .name("2")
                        .attachedUnitTypes(List.of(unitType))
                        .build());
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "One phase has an attacker ability and a defender ability and both sides have units,"
              + " so create 1 step for each side for a total of 2 steps.",
          steps,
          hasSize(2));
    }

    @Test
    void convertUnitsAbilityInSamePhase() {
      final UnitType infantryUnitType = new UnitType("infantry", gameData);
      final UnitType artilleryUnitType = new UnitType("artillery", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .attackingUnits(
                  List.of(
                      infantryUnitType.createTemp(1, attacker).get(0),
                      artilleryUnitType.createTemp(1, attacker).get(0)))
              .build();

      final CombatUnitAbility initialAbility =
          CombatUnitAbility.builder()
              .name("1")
              .attachedUnitTypes(List.of(infantryUnitType))
              .build();
      // the converted ability should not have any attached unit types initially
      final CombatUnitAbility convertedAbility = CombatUnitAbility.builder().name("2").build();

      battlePhaseList.addAbilityOrMergeAttached(
          attacker,
          ConvertUnitAbility.builder()
              .name("convert")
              .attachedUnitTypes(List.of(artilleryUnitType))
              .teams(List.of(ConvertUnitAbility.Team.FRIENDLY))
              .from(initialAbility)
              .to(convertedAbility)
              .build());

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, initialAbility);
                battlePhase.addAbility(attacker, convertedAbility);
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "Only one step should exist since the infantry was converted from the general phase"
              + " to the first strike phase",
          steps,
          hasSize(1));
      final BattlePhaseStep step = (BattlePhaseStep) steps.iterator().next();
      assertThat(
          "There should only be one unit ability in this step.",
          step.getUnitAbilities(),
          hasSize(1));
      final BattlePhaseList.UnitAbilityAndUnits unitAbilityAndUnits =
          step.getUnitAbilities().iterator().next();
      assertThat(
          "And the only one unit ability should be the ability that was the result"
              + " of the conversion",
          unitAbilityAndUnits.getUnitAbility(),
          is(convertedAbility));
    }

    @Test
    void convertUnitsAbilityInDifferentPhase() {
      final UnitType infantryUnitType = new UnitType("infantry", gameData);
      final UnitType artilleryUnitType = new UnitType("artillery", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .attackingUnits(
                  List.of(
                      infantryUnitType.createTemp(1, attacker).get(0),
                      artilleryUnitType.createTemp(1, attacker).get(0)))
              .build();

      final CombatUnitAbility initialAbility =
          CombatUnitAbility.builder()
              .name("1")
              .attachedUnitTypes(List.of(infantryUnitType))
              .build();
      // the converted ability should not have any attached unit types initially
      final CombatUnitAbility convertedAbility = CombatUnitAbility.builder().name("2").build();

      battlePhaseList.addAbilityOrMergeAttached(
          attacker,
          ConvertUnitAbility.builder()
              .name("convert")
              .attachedUnitTypes(List.of(artilleryUnitType))
              .teams(List.of(ConvertUnitAbility.Team.FRIENDLY))
              .from(initialAbility)
              .to(convertedAbility)
              .build());

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, convertedAbility);
              });

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, initialAbility);
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "Only one step should exist since the infantry was converted from the general phase"
              + " to the first strike phase",
          steps,
          hasSize(1));
      final BattlePhaseStep step = (BattlePhaseStep) steps.iterator().next();
      assertThat(
          "The step should be first strike since it was converted",
          step.getBattlePhaseName(),
          is(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE));
    }

    @Test
    void removeUnitsAbilityThroughConversion() {
      final UnitType infantryUnitType = new UnitType("infantry", gameData);
      final UnitType artilleryUnitType = new UnitType("artillery", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .attackingUnits(
                  List.of(
                      infantryUnitType.createTemp(1, attacker).get(0),
                      artilleryUnitType.createTemp(1, attacker).get(0)))
              .build();

      final CombatUnitAbility initialAbility =
          CombatUnitAbility.builder()
              .name("1")
              .attachedUnitTypes(List.of(infantryUnitType))
              .build();

      battlePhaseList.addAbilityOrMergeAttached(
          attacker,
          ConvertUnitAbility.builder()
              .name("convert")
              .attachedUnitTypes(List.of(artilleryUnitType))
              .teams(List.of(ConvertUnitAbility.Team.FRIENDLY))
              .from(initialAbility)
              .build());

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, initialAbility);
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "No steps should exist as the infantry's ability was converted to empty",
          steps,
          is(empty()));
    }

    @Test
    void convertAbilityHasNoAvailableUnitsToActivateIt() {
      final UnitType infantryUnitType = new UnitType("infantry", gameData);
      final UnitType artilleryUnitType = new UnitType("artillery", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .attackingUnits(infantryUnitType.createTemp(1, attacker))
              .build();

      final CombatUnitAbility initialAbility =
          CombatUnitAbility.builder()
              .name("1")
              .attachedUnitTypes(List.of(infantryUnitType))
              .build();
      // the converted ability should not have any attached unit types initially
      final CombatUnitAbility convertedAbility = CombatUnitAbility.builder().name("2").build();

      battlePhaseList.addAbilityOrMergeAttached(
          attacker,
          ConvertUnitAbility.builder()
              .name("convert")
              .attachedUnitTypes(List.of(artilleryUnitType))
              .teams(List.of(ConvertUnitAbility.Team.FRIENDLY))
              .from(initialAbility)
              .to(convertedAbility)
              .build());

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, convertedAbility);
              });

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, initialAbility);
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "There are no artillery present, so the infantry should still be in general phase",
          steps,
          hasSize(1));
      final BattlePhaseStep step = (BattlePhaseStep) steps.iterator().next();
      assertThat(
          "There are no artillery present, so the infantry should still be in general phase",
          step.getBattlePhaseName(),
          is(BattlePhaseList.DEFAULT_GENERAL_PHASE));
    }

    @Test
    void convertEnemyUnitsAbilityInDifferentPhase() {
      final UnitType infantryUnitType = new UnitType("infantry", gameData);
      final UnitType artilleryUnitType = new UnitType("artillery", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .attacker(defender)
              .attackingUnits(infantryUnitType.createTemp(1, attacker))
              .defendingUnits(artilleryUnitType.createTemp(1, defender))
              .build();

      final CombatUnitAbility initialAbility =
          CombatUnitAbility.builder()
              .name("1")
              .attachedUnitTypes(List.of(infantryUnitType))
              .build();
      // the converted ability should not have any attached unit types initially
      final CombatUnitAbility convertedAbility = CombatUnitAbility.builder().name("2").build();

      battlePhaseList.addAbilityOrMergeAttached(
          defender,
          ConvertUnitAbility.builder()
              .name("convert")
              .attachedUnitTypes(List.of(artilleryUnitType))
              .teams(List.of(ConvertUnitAbility.Team.FOE))
              .from(initialAbility)
              .to(convertedAbility)
              .build());

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, initialAbility);
              });

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, convertedAbility);
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "Only one step should exist since the infantry was converted from"
              + " the first strike phase to the general phase",
          steps,
          hasSize(1));
      final BattlePhaseStep step = (BattlePhaseStep) steps.iterator().next();
      assertThat(
          "The step should be general since it was converted",
          step.getBattlePhaseName(),
          is(BattlePhaseList.DEFAULT_GENERAL_PHASE));
    }

    @Test
    void convertUnitsAbilityDoesNotHappenIfTheConverterIsOnTheWrongSide() {
      final UnitType infantryUnitType = new UnitType("infantry", gameData);
      final UnitType artilleryUnitType = new UnitType("artillery", gameData);

      final BattleState battleState =
          givenBattleStateBuilder()
              .attacker(attacker)
              .attacker(defender)
              .attackingUnits(
                  List.of(
                      infantryUnitType.createTemp(1, attacker).get(0),
                      artilleryUnitType.createTemp(1, defender).get(0)))
              .build();

      final CombatUnitAbility initialAbility =
          CombatUnitAbility.builder()
              .name("1")
              .attachedUnitTypes(List.of(infantryUnitType))
              .build();
      // the converted ability should not have any attached unit types initially
      final CombatUnitAbility convertedAbility = CombatUnitAbility.builder().name("2").build();

      battlePhaseList.addAbilityOrMergeAttached(
          defender,
          ConvertUnitAbility.builder()
              .name("convert")
              .attachedUnitTypes(List.of(artilleryUnitType))
              .teams(List.of(ConvertUnitAbility.Team.FOE))
              .from(initialAbility)
              .to(convertedAbility)
              .build());

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, initialAbility);
              });

      battlePhaseList
          .getPhase(BattlePhaseList.DEFAULT_GENERAL_PHASE)
          .ifPresent(
              battlePhase -> {
                battlePhase.addAbility(attacker, convertedAbility);
              });
      final Collection<BattleStep> steps = battlePhaseList.getBattleSteps(battleState);
      assertThat(
          "Only one step should exist since the infantry was NOT converted", steps, hasSize(1));
      final BattlePhaseStep step = (BattlePhaseStep) steps.iterator().next();
      assertThat(
          "The step should be first strike since the artillery is a FRIENDLY but the"
              + " convert ability is only for FOE",
          step.getBattlePhaseName(),
          is(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE));
    }
  }
}
