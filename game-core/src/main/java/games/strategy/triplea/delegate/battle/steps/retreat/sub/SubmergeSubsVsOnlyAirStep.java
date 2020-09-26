package games.strategy.triplea.delegate.battle.steps.retreat.sub;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleStatus.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBMERGE_SUBS_VS_AIR_ONLY;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.retreat.EvaderRetreat;
import java.util.Collection;
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
  public List<String> getNames() {
    return valid() ? List.of(SUBMERGE_SUBS_VS_AIR_ONLY) : List.of();
  }

  @Override
  public Order getOrder() {
    return Order.SUBMERGE_SUBS_VS_ONLY_AIR;
  }

  private boolean valid() {
    return (isOnlyAirVsSubs(
            battleState.getUnits(ALIVE, OFFENSE), battleState.getUnits(ALIVE, DEFENSE))
        || isOnlyAirVsSubs(
            battleState.getUnits(ALIVE, DEFENSE), battleState.getUnits(ALIVE, OFFENSE)));
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    final Collection<Unit> submergingSubs;
    final BattleState.Side side;
    if (isOnlyAirVsSubs(
        battleState.getUnits(ALIVE, OFFENSE), battleState.getUnits(ALIVE, DEFENSE))) {
      // submerge the defending units
      submergingSubs =
          CollectionUtils.getMatches(
              battleState.getUnits(ALIVE, DEFENSE), canNotBeTargetedByAllMatch);
      side = DEFENSE;
    } else if (isOnlyAirVsSubs(
        battleState.getUnits(ALIVE, DEFENSE), battleState.getUnits(ALIVE, OFFENSE))) {
      // submerge the attacking units
      submergingSubs =
          CollectionUtils.getMatches(
              battleState.getUnits(ALIVE, OFFENSE), canNotBeTargetedByAllMatch);
      side = OFFENSE;
    } else {
      return;
    }
    EvaderRetreat.submergeEvaders(
        EvaderRetreat.Parameters.builder()
            .battleState(battleState)
            .battleActions(battleActions)
            .units(submergingSubs)
            .side(side)
            .bridge(bridge)
            .build());
  }

  private boolean isOnlyAirVsSubs(
      final Collection<Unit> possibleAirUnits, final Collection<Unit> possibleEvadingUnits) {
    return !possibleAirUnits.isEmpty()
        && possibleAirUnits.stream().allMatch(Matches.unitIsAir())
        && possibleEvadingUnits.stream().anyMatch(canNotBeTargetedByAllMatch);
  }
}
