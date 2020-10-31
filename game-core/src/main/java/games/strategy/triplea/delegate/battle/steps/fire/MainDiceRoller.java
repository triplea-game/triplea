package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ACTIVE;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.DiceRoll;
import java.util.function.BiFunction;

/** Rolls dice for normal (basically, anything that isn't AA) dice requests */
public class MainDiceRoller implements BiFunction<IDelegateBridge, RollDiceStep, DiceRoll> {

  @Override
  public DiceRoll apply(final IDelegateBridge bridge, final RollDiceStep step) {
    return DiceRoll.rollDice(
        step.getFiringGroup().getFiringUnits(),
        step.getSide() == DEFENSE,
        step.getBattleState().getPlayer(step.getSide()),
        bridge,
        step.getBattleState().getBattleSite(),
        DiceRoll.getAnnotation(
            step.getFiringGroup().getFiringUnits(),
            step.getBattleState().getPlayer(step.getSide()),
            step.getBattleState().getBattleSite(),
            step.getBattleState().getStatus().getRound()),
        step.getBattleState().getTerritoryEffects(),
        step.getBattleState().filterUnits(ACTIVE, step.getSide().getOpposite()),
        step.getBattleState().filterUnits(ACTIVE, step.getSide()));
  }
}
