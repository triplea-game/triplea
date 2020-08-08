package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_SUBMERGE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_WITHDRAW;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle.RetreatType;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.RetreatChecks;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OffensiveSubsRetreat implements BattleStep {

  private static final long serialVersionUID = -244024585102561887L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    if (!isEvaderPresent() || !isRetreatPossible()) {
      return List.of();
    }

    if (getOrder() == SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE && isDestroyerPresent()) {
      // only check for destroyers if subs can retreat before battle
      // because the destroyer could be killed during the battle which would
      // allow the sub to withdraw at the end of the battle
      return List.of();
    }

    if (Properties.getSubmersibleSubs(battleState.getGameData())) {
      return List.of(battleState.getAttacker().getName() + SUBS_SUBMERGE);
    } else {
      return List.of(battleState.getAttacker().getName() + SUBS_WITHDRAW);
    }
  }

  @Override
  public Order getOrder() {
    if (Properties.getSubRetreatBeforeBattle(battleState.getGameData())) {
      return SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE;
    } else {
      return SUB_OFFENSIVE_RETREAT_AFTER_BATTLE;
    }
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (battleState.isOver()
        || isDestroyerPresent()
        || !isEvaderPresent()
        || !isRetreatPossible()
        || isAutoWinScenario()) {
      return;
    }
    battleActions.queryRetreat(
        false, RetreatType.SUBS, bridge, battleState.getAttackerRetreatTerritories());
  }

  private boolean isDestroyerPresent() {
    return battleState.getDefendingUnits().stream().anyMatch(Matches.unitIsDestroyer())
        || battleState.getDefendingWaitingToDie().stream().anyMatch(Matches.unitIsDestroyer());
  }

  private boolean isEvaderPresent() {
    return battleState.getAttackingUnits().stream().anyMatch(Matches.unitCanEvade());
  }

  private boolean isRetreatPossible() {
    return Properties.getSubmersibleSubs(battleState.getGameData())
        || RetreatChecks.canAttackerRetreat(
            battleState.getDefendingUnits(),
            battleState.getGameData(),
            battleState::getAttackerRetreatTerritories,
            battleState.isAmphibious());
  }

  private boolean isAutoWinScenario() {
    return RetreatChecks.onlyDefenselessDefendingTransportsLeft(
        battleState.getDefendingUnits(), battleState.getGameData());
  }
}
