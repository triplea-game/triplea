package games.strategy.triplea.delegate.battle.steps.fire;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/** Build the steps for the fire round (roll dice, select casualties, and mark casualties) */
@UtilityClass
public class FireRoundStepsBuilder {

  @Builder
  public static class Parameters {
    @NonNull final BattleState battleState;
    @NonNull final BattleActions battleActions;
    @NonNull final Function<BattleState, List<FiringGroup>> firingGroupSplitter;
    @NonNull final BattleState.Side side;
    @NonNull final MustFightBattle.ReturnFire returnFire;
    @NonNull final BiFunction<IDelegateBridge, RollDice, DiceRoll> roll;
    @NonNull final BiFunction<IDelegateBridge, SelectCasualties, CasualtyDetails> selectCasualties;
  }

  public static List<BattleStep> buildSteps(final Parameters parameters) {
    final List<BattleStep> steps = new ArrayList<>();

    final List<FiringGroup> firingGroups =
        new ArrayList<>(parameters.firingGroupSplitter.apply(parameters.battleState));
    firingGroups.sort(
        Comparator.comparing(FiringGroup::getDisplayName)
            .thenComparing(FiringGroup::isSuicideOnHit));

    for (final FiringGroup firingGroup : firingGroups) {
      final FireRoundState fireRoundState = new FireRoundState();
      steps.add(
          new RollDice(
              parameters.battleState,
              parameters.side,
              firingGroup,
              fireRoundState,
              parameters.roll));
      steps.add(
          new SelectCasualties(
              parameters.battleState,
              parameters.side,
              firingGroup,
              fireRoundState,
              parameters.selectCasualties));
      steps.add(
          new MarkCasualties(
              parameters.battleState,
              parameters.battleActions,
              parameters.side,
              firingGroup,
              fireRoundState,
              parameters.returnFire));
    }

    return steps;
  }
}
