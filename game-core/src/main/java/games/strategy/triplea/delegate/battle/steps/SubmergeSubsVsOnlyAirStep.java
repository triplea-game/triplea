package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBMERGE_SUBS_VS_AIR_ONLY;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Tuple;

/**
 * Units that canNotBeTargetedByAll can submerge if there are only Air units in the battle
 *
 * <p>This step always occurs at the start of the battle so PRE_ROUND and IN_ROUND checks are the
 * same
 */
public class SubmergeSubsVsOnlyAirStep extends BattleStep {

  private static final Predicate<Unit> canNotBeTargetedByAllMatch =
      Matches.unitCanEvade().and(Matches.unitCanNotBeTargetedByAll());

  public SubmergeSubsVsOnlyAirStep(final StepParameters parameters) {
    super(parameters);
  }

  /**
   * NOTE: This type exists solely for tests to interrogate the execution stack looking for an
   * action of this type. This is temporary and will be removed once the battle step refactoring is
   * done
   */
  public abstract static class SubmergeSubsVsOnlyAir extends BattleAtomic {}

  @Override
  public IExecutable getExecutable() {
    return new SubmergeSubsVsOnlyAir() {
      private static final long serialVersionUID = 99990L;

      @Override
      protected BattleStep getStep() {
        return new SubmergeSubsVsOnlyAirStep(parameters.battleActions.getStepParameters());
      }
    };
  }

  @Override
  public List<String> getNames() {
    return List.of(SUBMERGE_SUBS_VS_AIR_ONLY);
  }

  @Override
  public boolean valid(final State state) {
    return (isOnlyAirVsSubs(parameters.attackingUnits, parameters.defendingUnits)
        || isOnlyAirVsSubs(parameters.defendingUnits, parameters.attackingUnits));
  }

  @Override
  protected void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    final Tuple<List<Unit>, Boolean> subsAndSide = submergeSubsVsOnlyAir();
    if (subsAndSide.getFirst().isEmpty()) {
      // valid() should prevent this case but check for it anyways
      return;
    }
    parameters.battleActions.submergeUnits(subsAndSide.getFirst(), subsAndSide.getSecond(), bridge);
  }

  private Tuple<List<Unit>, Boolean> submergeSubsVsOnlyAir() {
    if (isOnlyAirVsSubs(parameters.attackingUnits, parameters.defendingUnits)) {
      // submerge the defending units
      return Tuple.of(
          CollectionUtils.getMatches(parameters.defendingUnits, canNotBeTargetedByAllMatch), true);
    } else if (isOnlyAirVsSubs(parameters.defendingUnits, parameters.attackingUnits)) {
      // submerge the attacking units
      return Tuple.of(
          CollectionUtils.getMatches(parameters.attackingUnits, canNotBeTargetedByAllMatch), false);
    }
    return Tuple.of(List.of(), false);
  }

  private boolean isOnlyAirVsSubs(
      final Collection<Unit> possibleAirUnits, final Collection<Unit> possibleEvadingUnits) {
    return !possibleAirUnits.isEmpty()
        && possibleAirUnits.stream().allMatch(Matches.unitIsAir())
        && possibleEvadingUnits.stream().anyMatch(canNotBeTargetedByAllMatch);
  }
}
