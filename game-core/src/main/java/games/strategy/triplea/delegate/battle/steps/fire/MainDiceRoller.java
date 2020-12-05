package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.util.function.BiFunction;

/** Rolls dice for normal (basically, anything that isn't AA) dice requests */
public class MainDiceRoller implements BiFunction<IDelegateBridge, RollDiceStep, DiceRoll> {

  @Override
  public DiceRoll apply(final IDelegateBridge bridge, final RollDiceStep step) {
    return RollDiceFactory.rollBattleDice(
        step.getFiringGroup().getFiringUnits(),
        step.getBattleState().getPlayer(step.getSide()),
        bridge,
        DiceRoll.getAnnotation(
            step.getFiringGroup().getFiringUnits(),
            step.getBattleState().getPlayer(step.getSide()),
            step.getBattleState().getBattleSite(),
            step.getBattleState().getStatus().getRound()),
        CombatValueBuilder.mainCombatValue()
            .enemyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide().getOpposite()))
            .friendlyUnits(step.getBattleState().filterUnits(ALIVE, step.getSide()))
            .side(step.getSide())
            .gameSequence(step.getBattleState().getGameData().getSequence())
            .supportAttachments(
                step.getBattleState().getGameData().getUnitTypeList().getSupportRules())
            .lhtrHeavyBombers(
                Properties.getLhtrHeavyBombers(step.getBattleState().getGameData().getProperties()))
            .gameDiceSides(step.getBattleState().getGameData().getDiceSides())
            .territoryEffects(step.getBattleState().getTerritoryEffects())
            .build());
  }
}
