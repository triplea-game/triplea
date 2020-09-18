package games.strategy.triplea.delegate.battle.steps.change;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveGeneralRetreat;
import org.triplea.java.RemoveOnNextMajorRelease;

/**
 * This step was broken up into multiple steps, but old save games will only have this step so they
 * need to continue to perform the multiple steps in this one step.
 */
@RemoveOnNextMajorRelease
public class CheckGeneralBattleEndOld extends CheckGeneralBattleEnd {

  public CheckGeneralBattleEndOld(
      final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (hasSideLost(BattleState.Side.OFFENSE)) {
      getBattleActions().endBattle(bridge);
      getBattleActions().defenderWins(bridge);

    } else if (hasSideLost(BattleState.Side.DEFENSE)) {
      new RemoveUnprotectedUnits(getBattleState(), getBattleActions())
          .removeUnprotectedUnits(bridge, BattleState.Side.DEFENSE);
      getBattleActions().endBattle(bridge);
      getBattleActions().attackerWins(bridge);

    } else if (isStalemate()) {
      if (canAttackerRetreatInStalemate()) {
        new OffensiveGeneralRetreat(getBattleState(), getBattleActions()).retreatUnits(bridge);
      }
      getBattleActions().endBattle(bridge);
      getBattleActions().nobodyWins(bridge);
    }
  }
}
