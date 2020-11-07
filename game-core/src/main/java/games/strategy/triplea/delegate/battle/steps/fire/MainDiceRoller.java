package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import java.util.function.BiFunction;

/** Rolls dice for normal (basically, anything that isn't AA) dice requests */
public class MainDiceRoller implements BiFunction<IDelegateBridge, RollDiceStep, DiceRoll> {

  @Override
  public DiceRoll apply(final IDelegateBridge bridge, final RollDiceStep step) {
    return DiceRoll.rollDice(
        step.getFiringGroup().getFiringUnits(),
        step.getBattleState().getPlayer(step.getSide()),
        bridge,
        DiceRoll.getAnnotation(
            step.getFiringGroup().getFiringUnits(),
            step.getBattleState().getPlayer(step.getSide()),
            step.getBattleState().getBattleSite(),
            step.getBattleState().getStatus().getRound()),
        CombatValue.buildMainCombatValue(
            step.getBattleState().filterUnits(ALIVE, step.getSide().getOpposite()),
            step.getBattleState().filterUnits(ALIVE, step.getSide()),
            step.getSide() == DEFENSE,
            step.getBattleState().getGameData(),
            step.getBattleState().getTerritoryEffects()));
  }
}
