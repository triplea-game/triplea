package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.generalFightStepStrings;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.mergeSteps;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.FakeBattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FireRoundStepsFactoryTest {

  @Test
  @DisplayName("The display names for an entire fire round is fire, select, remove")
  void getNamesWithOneFiringGroup() {
    final GamePlayer attacker = mock(GamePlayer.class);
    when(attacker.getName()).thenReturn("attacker");
    final GamePlayer defender = mock(GamePlayer.class);
    when(defender.getName()).thenReturn("defender");

    final List<String> names =
        getStepNames(
            FireRoundStepsFactory.builder()
                .battleActions(mock(BattleActions.class))
                .returnFire(MustFightBattle.ReturnFire.ALL)
                .diceRoller((arg1, arg2) -> new DiceRoll())
                .firingGroupSplitter(
                    (arg1) ->
                        FiringGroup.groupBySuicideOnHit(
                            "army", List.of(givenAnyUnit()), List.of(mock(Unit.class))))
                .casualtySelector((arg1, arg2) -> new CasualtyDetails())
                .side(OFFENSE)
                .battleState(
                    FakeBattleState.givenBattleStateBuilder()
                        .attacker(attacker)
                        .defender(defender)
                        .build())
                .build()
                .createSteps());

    assertThat(names, is(generalFightStepStrings(attacker, defender, "army")));
  }

  private List<String> getStepNames(final List<BattleStep> steps) {
    return steps.stream()
        .flatMap(step -> step.getAllStepDetails().stream())
        .map(BattleStep.StepDetails::getName)
        .collect(Collectors.toList());
  }

  @Test
  @DisplayName("With two groups, there should be two sets of fire round strings")
  void getNamesWithTwoFiringGroups() {
    final GamePlayer attacker = mock(GamePlayer.class);
    when(attacker.getName()).thenReturn("attacker");
    final GamePlayer defender = mock(GamePlayer.class);
    when(defender.getName()).thenReturn("defender");

    final List<String> names =
        getStepNames(
            FireRoundStepsFactory.builder()
                .battleActions(mock(BattleActions.class))
                .returnFire(MustFightBattle.ReturnFire.ALL)
                .diceRoller((arg1, arg2) -> new DiceRoll())
                .firingGroupSplitter(
                    (arg1) ->
                        List.of(
                            FiringGroup.groupBySuicideOnHit(
                                    "army", List.of(givenAnyUnit()), List.of(mock(Unit.class)))
                                .get(0),
                            FiringGroup.groupBySuicideOnHit(
                                    "spies", List.of(givenAnyUnit()), List.of(mock(Unit.class)))
                                .get(0)))
                .casualtySelector((arg1, arg2) -> new CasualtyDetails())
                .side(OFFENSE)
                .battleState(
                    FakeBattleState.givenBattleStateBuilder()
                        .attacker(attacker)
                        .defender(defender)
                        .build())
                .build()
                .createSteps());

    assertThat(
        names,
        is(
            mergeSteps(
                generalFightStepStrings(attacker, defender, "army"),
                generalFightStepStrings(attacker, defender, "spies"))));
  }

  @Test
  @DisplayName("With the default name UNITS, the step names should not include UNITS")
  void getNamesWithDefaultUnitName() {
    final GamePlayer attacker = mock(GamePlayer.class);
    when(attacker.getName()).thenReturn("attacker");
    final GamePlayer defender = mock(GamePlayer.class);
    when(defender.getName()).thenReturn("defender");

    final List<String> names =
        getStepNames(
            FireRoundStepsFactory.builder()
                .battleActions(mock(BattleActions.class))
                .returnFire(MustFightBattle.ReturnFire.ALL)
                .diceRoller((arg1, arg2) -> new DiceRoll())
                .firingGroupSplitter(
                    (arg1) ->
                        FiringGroup.groupBySuicideOnHit(
                            UNITS, List.of(givenAnyUnit()), List.of(mock(Unit.class))))
                .casualtySelector((arg1, arg2) -> new CasualtyDetails())
                .side(OFFENSE)
                .battleState(
                    FakeBattleState.givenBattleStateBuilder()
                        .attacker(attacker)
                        .defender(defender)
                        .build())
                .build()
                .createSteps());

    assertThat(
        "units should not be in the name", names, is(generalFightStepStrings(attacker, defender)));
  }
}
