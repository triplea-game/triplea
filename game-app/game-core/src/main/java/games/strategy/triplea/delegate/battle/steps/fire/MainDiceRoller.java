package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ACTIVE;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.battle.AirControlTracker;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.io.Serializable;
import java.util.function.BiFunction;

/** Rolls dice for normal (basically, anything that isn't AA) dice requests. */
public class MainDiceRoller
    implements BiFunction<IDelegateBridge, RollDiceStep, DiceRoll>, Serializable {
  private static final long serialVersionUID = 11934707918127558L;

  @Override
  public DiceRoll apply(final IDelegateBridge bridge, final RollDiceStep step) {
    final BattleState battleState = step.getBattleState();
    final var gameData = battleState.getGameData();
    final int offenseGroundStrengthModifier =
        AirControlTracker.get(gameData)
            .getGroundAttackBonus(
                battleState.getBattleSite(),
                battleState.getPlayer(BattleState.Side.OFFENSE),
                gameData);

    return RollDiceFactory.rollBattleDice(
        step.getFiringGroup().getFiringUnits(),
        battleState.getPlayer(step.getSide()),
        bridge,
        DiceRoll.getAnnotation(
            step.getFiringGroup().getFiringUnits(),
            battleState.getPlayer(step.getSide()),
            battleState.getBattleSite(),
            battleState.getStatus().getRound()),
        CombatValueBuilder.mainCombatValue()
            .enemyUnits(battleState.filterUnits(ACTIVE, step.getSide().getOpposite()))
            .friendlyUnits(battleState.filterUnits(ACTIVE, step.getSide()))
            .side(step.getSide())
            .gameSequence(gameData.getSequence())
            .supportAttachments(gameData.getUnitTypeList().getSupportRules())
            .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(gameData.getProperties()))
            .gameDiceSides(gameData.getDiceSides())
            .territoryEffects(battleState.getTerritoryEffects())
            .offenseGroundStrengthModifier(offenseGroundStrengthModifier)
            .build());
  }
}
