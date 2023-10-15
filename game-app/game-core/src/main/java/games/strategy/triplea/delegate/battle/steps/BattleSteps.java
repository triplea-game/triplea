package games.strategy.triplea.delegate.battle.steps;

import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.BattleStepStrings;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

/** Get the steps that will occur in the battle */
@Builder
public class BattleSteps implements BattleStepStrings {

  final BattleState battleState;
  final BattleActions battleActions;

  public List<BattleStep.StepDetails> get() {
    return BattleStep.getAll(battleState, battleActions).stream()
        .sorted(Comparator.comparing(BattleStep::getOrder))
        .flatMap(step -> step.getAllStepDetails().stream())
        .collect(Collectors.toList());
  }
}
