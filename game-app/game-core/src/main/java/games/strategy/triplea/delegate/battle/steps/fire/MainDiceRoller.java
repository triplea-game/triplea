package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ACTIVE;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.dice.calculator.RandomDiceGenerator;
import games.strategy.triplea.delegate.dice.calculator.SameLuckDiceGenerator;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.io.Serializable;
import java.util.function.BiFunction;

/** Rolls dice for normal (basically, anything that isn't AA) dice requests */
public class MainDiceRoller
    implements BiFunction<IDelegateBridge, RollDiceStep, DiceRoll>, Serializable {
  private static final long serialVersionUID = 11934707918127558L;

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
        buildCombatValue(step),
        diceGenerator(bridge, step));
  }

  private static RandomDiceGenerator diceGenerator(
      final IDelegateBridge bridge, final RollDiceStep step) {
    if (Properties.getSameLuck(step.getBattleState().getGameData().getProperties())) {
      // Share the round's single remainder die between attacker and defender fire.
      return new SameLuckDiceGenerator(
          bridge::getRandom, step.getBattleState().getSameLuckDiePool());
    }
    return bridge::getRandom;
  }

  private static CombatValue buildCombatValue(final RollDiceStep step) {
    return CombatValueBuilder.mainCombatValue()
        .enemyUnits(step.getBattleState().filterUnits(ACTIVE, step.getSide().getOpposite()))
        .friendlyUnits(step.getBattleState().filterUnits(ACTIVE, step.getSide()))
        .side(step.getSide())
        .gameSequence(step.getBattleState().getGameData().getSequence())
        .supportAttachments(step.getBattleState().getGameData().getUnitTypeList().getSupportRules())
        .lhtrHeavyBombers(
            Properties.getLhtrHeavyBombers(step.getBattleState().getGameData().getProperties()))
        .gameDiceSides(step.getBattleState().getGameData().getDiceSides())
        .territoryEffects(step.getBattleState().getTerritoryEffects())
        .build();
  }
}
