package games.strategy.triplea.delegate.battle.steps.retreat.sub;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBMERGE_SUBS_VS_AIR_ONLY;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;

/**
 * Units that canNotBeTargetedByAll can submerge if there are only Air units in the battle
 */
public class SubmergeSubsVsOnlyAirStep extends BattleStep {

  private static final Predicate<Unit> canNotBeTargetedByAllMatch =
      Matches.unitCanEvade().and(Matches.unitCanNotBeTargetedByAll());

  public SubmergeSubsVsOnlyAirStep(
      final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  /**
   * NOTE: This type exists solely for tests to interrogate the execution stack looking for an
   * action of this type. This is temporary and will be removed once the battle step refactoring is
   * done
   */
  public abstract class SubmergeSubsVsOnlyAir extends BattleAtomic {}

  @Override
  public BattleAtomic getExecutable() {
    return new SubmergeSubsVsOnlyAir() {
      private static final long serialVersionUID = 99990L;
    };
  }

  @Override
  public List<String> getNames() {
    return List.of(SUBMERGE_SUBS_VS_AIR_ONLY);
  }

  @Override
  public boolean valid() {
    return (isOnlyAirVsSubs(battleState.getAttackingUnits(), battleState.getDefendingUnits())
        || isOnlyAirVsSubs(battleState.getDefendingUnits(), battleState.getAttackingUnits()));
  }

  @Override
  protected void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    final Collection<Unit> submergingSubs;
    final boolean defender;
    if (isOnlyAirVsSubs(battleState.getAttackingUnits(), battleState.getDefendingUnits())) {
      // submerge the defending units
      submergingSubs =
          CollectionUtils.getMatches(battleState.getDefendingUnits(), canNotBeTargetedByAllMatch);
      defender = true;
    } else if (isOnlyAirVsSubs(battleState.getDefendingUnits(), battleState.getAttackingUnits())) {
      // submerge the attacking units
      submergingSubs =
          CollectionUtils.getMatches(battleState.getAttackingUnits(), canNotBeTargetedByAllMatch);
      defender = false;
    } else {
      return;
    }
    battleActions.submergeUnits(submergingSubs, defender, bridge);
  }

  private boolean isOnlyAirVsSubs(
      final Collection<Unit> possibleAirUnits, final Collection<Unit> possibleEvadingUnits) {
    return !possibleAirUnits.isEmpty()
        && possibleAirUnits.stream().allMatch(Matches.unitIsAir())
        && possibleEvadingUnits.stream().anyMatch(canNotBeTargetedByAllMatch);
  }
}
