package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_SUBMERGE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_WITHDRAW;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle.RetreatType;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
public class DefensiveSubsRetreat implements BattleStep {

  private static final long serialVersionUID = 1249467218938096244L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    if (!isEvaderPresent() || !isRetreatPossible()) {
      return List.of();
    }

    if (getOrder() == SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE && isDestroyerPresent()) {
      // only check for destroyers if subs can retreat before battle
      // because the destroyer could be killed during the battle which would
      // allow the sub to withdraw at the end of the battle
      return List.of();
    }

    if (Properties.getSubmersibleSubs(battleState.getGameData())) {
      return List.of(battleState.getDefender().getName() + SUBS_SUBMERGE);
    } else {
      return List.of(battleState.getDefender().getName() + SUBS_WITHDRAW);
    }
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
    if (battleState.isOver()
        || isDestroyerPresent()
        || !isEvaderPresent()
        || !isRetreatPossible()) {
      return;
    }

    battleActions.queryRetreat(true, RetreatType.SUBS, bridge, getEmptyOrFriendlySeaNeighbors());

    // If no defenders left, then battle is over. The reason we test a "second" time here,
    // is because otherwise the battle will try and do one more round and nothing will
    // happen in that round.
    if (getOrder() == SUB_DEFENSIVE_RETREAT_AFTER_BATTLE
        && battleState.getUnits(BattleState.Side.DEFENSE).isEmpty()) {
      battleActions.endBattle(bridge);
      battleActions.attackerWins(bridge);
    }
  }

  private boolean isEvaderPresent() {
    return battleState.getUnits(BattleState.Side.DEFENSE).stream().anyMatch(Matches.unitCanEvade());
  }

  private boolean isDestroyerPresent() {
    return battleState.getUnits(BattleState.Side.OFFENSE).stream()
            .anyMatch(Matches.unitIsDestroyer())
        || battleState.getWaitingToDie(BattleState.Side.OFFENSE).stream()
            .anyMatch(Matches.unitIsDestroyer());
  }

  private boolean isRetreatPossible() {
    return Properties.getSubmersibleSubs(battleState.getGameData())
        || !getEmptyOrFriendlySeaNeighbors().isEmpty();
  }

  public Collection<Territory> getEmptyOrFriendlySeaNeighbors() {
    final Collection<Territory> possible =
        battleState.getGameData().getMap().getNeighbors(battleState.getBattleSite());
    if (battleState.isHeadless()) {
      return possible;
    }
    final Collection<Unit> unitsToRetreat =
        CollectionUtils.getMatches(
            battleState.getUnits(BattleState.Side.DEFENSE), Matches.unitCanEvade());

    // make sure we can move through the any canals
    final Predicate<Territory> canalMatch =
        t -> {
          final Route r = new Route(battleState.getBattleSite(), t);
          return new MoveValidator(battleState.getGameData())
                  .validateCanal(r, unitsToRetreat, battleState.getDefender())
              == null;
        };
    final Predicate<Territory> match =
        Matches.territoryIsWater()
            .and(
                Matches.territoryHasNoEnemyUnits(
                    battleState.getDefender(), battleState.getGameData()))
            .and(canalMatch);
    return CollectionUtils.getMatches(possible, match);
  }
}
