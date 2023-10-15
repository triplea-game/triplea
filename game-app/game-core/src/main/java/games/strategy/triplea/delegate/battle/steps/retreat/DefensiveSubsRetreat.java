package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.CASUALTY;
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
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import java.util.ArrayList;
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
      return battleState.getPlayer(DEFENSE).getName() + SUBS_SUBMERGE;
    } else {
      return battleState.getPlayer(DEFENSE).getName() + SUBS_WITHDRAW;
    }
  }

  @Override
  public Order getOrder() {
    if (Properties.getSubRetreatBeforeBattle(battleState.getGameData().getProperties())) {
      return SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE;
    } else {
      return SUB_DEFENSIVE_RETREAT_AFTER_BATTLE;
    }
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (battleState.getStatus().isOver()
        || isDestroyerPresent()
        || isEvaderNotPresent()
        || isRetreatNotPossible()) {
      return;
    }

    final Collection<Unit> unitsToRetreat =
        CollectionUtils.getMatches(battleState.filterUnits(ALIVE, DEFENSE), Matches.unitCanEvade());
    if (unitsToRetreat.isEmpty()) {
      return;
    }

    final Collection<Territory> retreatTerritories;
    if (Properties.getSubmersibleSubs(battleState.getGameData().getProperties())) {
      retreatTerritories = List.of(battleState.getBattleSite());
    } else {
      retreatTerritories = new ArrayList<>(getEmptyOrFriendlySeaNeighbors());
      if (Properties.getSubmarinesDefendingMaySubmergeOrRetreat(
          battleState.getGameData().getProperties())) {
        retreatTerritories.add(battleState.getBattleSite());
      }
    }

    EvaderRetreat.retreatUnits(
        EvaderRetreat.Parameters.builder()
            .battleState(battleState)
            .battleActions(battleActions)
            .side(DEFENSE)
            .bridge(bridge)
            .units(unitsToRetreat)
            .build(),
        retreatTerritories,
        getName());
  }

  private boolean isEvaderNotPresent() {
    return battleState.filterUnits(ALIVE, DEFENSE).stream().noneMatch(Matches.unitCanEvade());
  }

  private boolean isDestroyerPresent() {
    return battleState.filterUnits(ALIVE, OFFENSE).stream().anyMatch(Matches.unitIsDestroyer())
        || battleState.filterUnits(CASUALTY, OFFENSE).stream().anyMatch(Matches.unitIsDestroyer());
  }

  private boolean isRetreatNotPossible() {
    return !(Properties.getSubmersibleSubs(battleState.getGameData().getProperties())
            || Properties.getSubmarinesDefendingMaySubmergeOrRetreat(
                battleState.getGameData().getProperties()))
        && getEmptyOrFriendlySeaNeighbors().isEmpty();
  }

  public Collection<Territory> getEmptyOrFriendlySeaNeighbors() {
    final Collection<Territory> possible =
        battleState.getGameData().getMap().getNeighbors(battleState.getBattleSite());
    if (battleState.getStatus().isHeadless()) {
      return possible;
    }
    final Collection<Unit> unitsToRetreat =
        CollectionUtils.getMatches(battleState.filterUnits(ALIVE, DEFENSE), Matches.unitCanEvade());

    // make sure we can move through the any canals
    final Predicate<Territory> canalMatch =
        t -> {
          final Route r = new Route(battleState.getBattleSite(), t);
          return new MoveValidator(battleState.getGameData(), false)
                  .validateCanal(r, unitsToRetreat, battleState.getPlayer(DEFENSE))
              == null;
        };
    final Predicate<Territory> match =
        Matches.territoryIsWater()
            .and(Matches.territoryHasNoEnemyUnits(battleState.getPlayer(DEFENSE)))
            .and(canalMatch);
    return CollectionUtils.getMatches(possible, match);
  }
}
