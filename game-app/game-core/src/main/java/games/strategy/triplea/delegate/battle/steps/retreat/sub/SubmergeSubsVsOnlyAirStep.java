package games.strategy.triplea.delegate.battle.steps.retreat.sub;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBMERGE_SUBS_VS_AIR_ONLY;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.retreat.EvaderRetreat;
import java.util.List;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

/** canNotBeTargetedByAll Units can submerge if there are only Air units in the battle */
@AllArgsConstructor
public class SubmergeSubsVsOnlyAirStep implements BattleStep {

  private static final long serialVersionUID = 99990L;

  private static final Predicate<Unit> canNotBeTargetedByAllMatch =
      Matches.unitCanEvade().and(Matches.unitCanNotBeTargetedByAll());

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return valid() ? List.of(new StepDetails(SUBMERGE_SUBS_VS_AIR_ONLY, this)) : List.of();
  }

  @Override
  public Order getOrder() {
    return Order.SUBMERGE_SUBS_VS_ONLY_AIR;
  }

  private boolean valid() {
    return (sideOnlyHasAirThatCanNotTargetSubs(OFFENSE)
        || sideOnlyHasAirThatCanNotTargetSubs(DEFENSE));
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    final BattleState.Side submergingSide;
    if (sideOnlyHasAirThatCanNotTargetSubs(OFFENSE)) {
      submergingSide = DEFENSE;
    } else if (sideOnlyHasAirThatCanNotTargetSubs(DEFENSE)) {
      submergingSide = OFFENSE;
    } else {
      return;
    }

    EvaderRetreat.submergeEvaders(
        EvaderRetreat.Parameters.builder()
            .battleState(battleState)
            .battleActions(battleActions)
            .units(
                CollectionUtils.getMatches(
                    battleState.filterUnits(ALIVE, submergingSide), canNotBeTargetedByAllMatch))
            .side(submergingSide)
            .bridge(bridge)
            .build());
  }

  private boolean sideOnlyHasAirThatCanNotTargetSubs(final BattleState.Side sideWithAir) {
    return !battleState.filterUnits(ALIVE, sideWithAir).isEmpty()
        && battleState.filterUnits(ALIVE, sideWithAir).stream().allMatch(Matches.unitIsAir())
        && !battleState.filterUnits(ALIVE, sideWithAir.getOpposite()).isEmpty()
        && battleState.filterUnits(ALIVE, sideWithAir.getOpposite()).stream()
            .anyMatch(canNotBeTargetedByAllMatch);
  }
}
