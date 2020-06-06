package games.strategy.triplea.delegate.battle.steps;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import games.strategy.triplea.delegate.battle.BattleActions;
import java.util.List;
import org.mockito.stubbing.Answer;

public final class MockStepParameters {
  private MockStepParameters() {}

  public static StepParameters.StepParametersBuilder givenStepParameters() {
    return givenStepParameters(mock(BattleActions.class));
  }

  public static StepParameters.StepParametersBuilder givenStepParameters(final BattleActions battleActions) {
    final StepParameters.StepParametersBuilder builder =
        StepParameters.builder()
            .attackingUnits(List.of())
            .defendingUnits(List.of())
            .battleActions(battleActions);

    // run build when it is called so that changes to the parameters in the test methods will take
    // affect
    lenient()
        .when(battleActions.getStepParameters())
        .then((Answer<StepParameters>) invocation -> builder.build());
    return builder;
  }
}
