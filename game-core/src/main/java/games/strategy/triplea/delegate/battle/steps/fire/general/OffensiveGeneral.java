package games.strategy.triplea.delegate.battle.steps.fire.general;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleStatus.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleStatus.CASUALTY;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.FIRE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_CASUALTIES;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OffensiveGeneral implements BattleStep {

  private static final long serialVersionUID = 5770484176987786287L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    final List<String> steps = new ArrayList<>();

    if (battleState.getUnits(ALIVE, OFFENSE).stream()
        .anyMatch(Matches.unitIsFirstStrike().negate())) {
      steps.add(battleState.getPlayer(OFFENSE).getName() + FIRE);
      steps.add(battleState.getPlayer(DEFENSE).getName() + SELECT_CASUALTIES);
    }

    return steps;
  }

  @Override
  public Order getOrder() {
    return Order.GENERAL_OFFENSIVE;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    battleActions.findTargetGroupsAndFire(
        ReturnFire.ALL,
        battleState.getPlayer(DEFENSE).getName() + SELECT_CASUALTIES,
        false,
        battleState.getPlayer(OFFENSE),
        Matches.unitIsFirstStrike().negate(),
        battleState.getUnits(ALIVE, OFFENSE),
        battleState.getUnits(CASUALTY, OFFENSE),
        battleState.getUnits(ALIVE, DEFENSE),
        battleState.getUnits(CASUALTY, DEFENSE));
  }
}
