package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
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
import org.junit.jupiter.api.Test;

class FireStepsBuilderTest {

  @Test
  void getNamesWithOneFiringGroup() {
    final GamePlayer attacker = mock(GamePlayer.class);
    when(attacker.getName()).thenReturn("attacker");
    final GamePlayer defender = mock(GamePlayer.class);
    when(defender.getName()).thenReturn("defender");
    final List<String> names =
        getStepNames(
            FireStepsBuilder.buildSteps(
                FireStepsBuilder.Parameters.builder()
                    .battleActions(mock(BattleActions.class))
                    .returnFire(MustFightBattle.ReturnFire.ALL)
                    .roll((arg1, arg2) -> new DiceRoll())
                    .firingGroupFilter(
                        (arg1) ->
                            List.of(
                                new FiringGroup(
                                    "army",
                                    "army",
                                    List.of(mock(Unit.class)),
                                    List.of(mock(Unit.class)),
                                    false)))
                    .selectCasualties((arg1, arg2) -> new CasualtyDetails())
                    .side(OFFENSE)
                    .battleState(
                        FakeBattleState.givenBattleStateBuilder()
                            .attacker(attacker)
                            .defender(defender)
                            .build())
                    .build()));

    final List<String> expected =
        List.of(
            "attacker army fire",
            "defender select army casualties",
            "defender remove army casualties");
    assertThat(names, is(expected));
  }

  private List<String> getStepNames(final List<BattleStep> steps) {
    return steps.stream().flatMap(step -> step.getNames().stream()).collect(Collectors.toList());
  }

  @Test
  void getNamesWithTwoFiringGroups() {
    final GamePlayer attacker = mock(GamePlayer.class);
    when(attacker.getName()).thenReturn("attacker");
    final GamePlayer defender = mock(GamePlayer.class);
    when(defender.getName()).thenReturn("defender");
    final List<String> names =
        getStepNames(
            FireStepsBuilder.buildSteps(
                FireStepsBuilder.Parameters.builder()
                    .battleActions(mock(BattleActions.class))
                    .returnFire(MustFightBattle.ReturnFire.ALL)
                    .roll((arg1, arg2) -> new DiceRoll())
                    .firingGroupFilter(
                        (arg1) ->
                            List.of(
                                new FiringGroup(
                                    "army",
                                    "army",
                                    List.of(mock(Unit.class)),
                                    List.of(mock(Unit.class)),
                                    false),
                                new FiringGroup(
                                    "spies",
                                    "spies",
                                    List.of(mock(Unit.class)),
                                    List.of(mock(Unit.class)),
                                    false)))
                    .selectCasualties((arg1, arg2) -> new CasualtyDetails())
                    .side(OFFENSE)
                    .battleState(
                        FakeBattleState.givenBattleStateBuilder()
                            .attacker(attacker)
                            .defender(defender)
                            .build())
                    .build()));

    final List<String> expected =
        List.of(
            "attacker army fire",
            "defender select army casualties",
            "defender remove army casualties",
            "attacker spies fire",
            "defender select spies casualties",
            "defender remove spies casualties");
    assertThat(names, is(expected));
  }
}
