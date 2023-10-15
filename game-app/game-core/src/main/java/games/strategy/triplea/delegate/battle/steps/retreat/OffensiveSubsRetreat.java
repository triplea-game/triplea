package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ACTIVE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_SUBMERGE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_WITHDRAW;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.RetreatChecks;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
public class OffensiveSubsRetreat implements BattleStep {

  private static final long serialVersionUID = -244024585102561887L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<StepDetails> getAllStepDetails() {
    // even though the #execute method checks for destroyers, we don't do it here
    // because this is called at the beginning of the round and any destroyer that exists
    // might die before the #execute is called
    if (isEvaderNotPresent() || isRetreatNotPossible()) {
      return List.of();
    }

    return List.of(new StepDetails(getName(), this));
  }

  private String getName() {
    if (Properties.getSubmersibleSubs(battleState.getGameData().getProperties())) {
      return battleState.getPlayer(OFFENSE).getName() + SUBS_SUBMERGE;
    } else {
      return battleState.getPlayer(OFFENSE).getName() + SUBS_WITHDRAW;
    }
  }

  @Override
  public Order getOrder() {
    if (Properties.getSubRetreatBeforeBattle(battleState.getGameData().getProperties())) {
      return SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE;
    } else {
      return SUB_OFFENSIVE_RETREAT_AFTER_BATTLE;
    }
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (battleState.getStatus().isOver()
        || isDestroyerPresent()
        || isEvaderNotPresent()
        || isRetreatNotPossible()
        || isAutoWinScenario()) {
      return;
    }

    final Collection<Unit> unitsToRetreat =
        CollectionUtils.getMatches(battleState.filterUnits(ALIVE, OFFENSE), Matches.unitCanEvade());
    if (unitsToRetreat.isEmpty()) {
      return;
    }

    EvaderRetreat.retreatUnits(
        EvaderRetreat.Parameters.builder()
            .battleState(battleState)
            .battleActions(battleActions)
            .side(OFFENSE)
            .bridge(bridge)
            .units(unitsToRetreat)
            .build(),
        Properties.getSubmersibleSubs(battleState.getGameData().getProperties())
            ? List.of(battleState.getBattleSite())
            : battleState.getAttackerRetreatTerritories(),
        getName());
  }

  private boolean isDestroyerPresent() {
    return battleState.filterUnits(ACTIVE, DEFENSE).stream().anyMatch(Matches.unitIsDestroyer());
  }

  private boolean isEvaderNotPresent() {
    return battleState.filterUnits(ALIVE, OFFENSE).stream().noneMatch(Matches.unitCanEvade());
  }

  private boolean isRetreatNotPossible() {
    return !Properties.getSubmersibleSubs(battleState.getGameData().getProperties())
        && !RetreatChecks.canAttackerRetreat(
            battleState.filterUnits(ALIVE, DEFENSE),
            battleState.getGameData(),
            battleState::getAttackerRetreatTerritories,
            battleState.getStatus().isAmphibious());
  }

  private boolean isAutoWinScenario() {
    return RetreatChecks.onlyDefenselessTransportsLeft(
        battleState.filterUnits(ALIVE, DEFENSE), battleState.getGameData());
  }
}
