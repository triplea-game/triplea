package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_SUBMERGE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_WITHDRAW;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE;
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
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
public class DefensiveSubsRetreat implements BattleStep {

  private static final long serialVersionUID = 1249467218938096244L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    // Check if defending subs can submerge before battle
    if (Properties.getSubRetreatBeforeBattle(battleState.getGameData())) {
      if (battleState.getAttackingUnits().stream().noneMatch(Matches.unitIsDestroyer())
          && battleState.getDefendingUnits().stream().anyMatch(Matches.unitCanEvade())) {
        return List.of(battleState.getDefender().getName() + SUBS_SUBMERGE);
      }
    }

    // retreat defending subs
    if (battleState.getDefendingUnits().stream().anyMatch(Matches.unitCanEvade())) {
      if (Properties.getSubmersibleSubs(battleState.getGameData())) {
        // TODO: BUG? Should the presence of destroyers be checked?
        if (!Properties.getSubRetreatBeforeBattle(battleState.getGameData())) {
          return List.of(battleState.getDefender().getName() + SUBS_SUBMERGE);
        }
      } else {
        if (RetreatChecks.canDefenderRetreatSubs(
            battleState.getAttackingUnits(),
            battleState.getAttackingWaitingToDie(),
            battleState.getDefendingUnits(),
            battleState.getGameData(),
            battleState::getEmptyOrFriendlySeaNeighbors)) {
          return List.of(battleState.getDefender().getName() + SUBS_WITHDRAW);
        }
      }
    }

    return List.of();
  }

  @Override
  public Order getOrder() {
    if (Properties.getSubRetreatBeforeBattle(battleState.getGameData())) {
      return SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE;
    } else {
      return SUB_DEFENSIVE_RETREAT_AFTER_BATTLE;
    }
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (battleState.isOver()) {
      return;
    }
    defenderRetreatSubs(bridge);

    if (getOrder() == SUB_DEFENSIVE_RETREAT_AFTER_BATTLE) {
      // If no defenders left, then battle is over. The reason we test a "second" time here,
      // is because otherwise the attackers can retreat even though the battle is over (illegal).
      if (battleState.getDefendingUnits().isEmpty()) {
        battleActions.endBattle(bridge);
        battleActions.attackerWins(bridge);
      }
    }
  }

  private void defenderRetreatSubs(final IDelegateBridge bridge) {
    if (!RetreatChecks.canDefenderRetreatSubs(
        battleState.getAttackingUnits(),
        battleState.getAttackingWaitingToDie(),
        battleState.getDefendingUnits(),
        battleState.getGameData(),
        battleState::getEmptyOrFriendlySeaNeighbors)) {
      return;
    }
    if (!battleState.isOver() && battleState.getDefendingUnits().stream().anyMatch(Matches.unitCanEvade())) {
      battleActions.queryRetreat(
          true,
          RetreatType.SUBS,
          bridge,
          battleState.getEmptyOrFriendlySeaNeighbors(
              CollectionUtils.getMatches(battleState.getDefendingUnits(), Matches.unitCanEvade())));
    }
  }
}
