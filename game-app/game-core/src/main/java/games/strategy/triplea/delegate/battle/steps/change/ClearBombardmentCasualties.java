package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_BOMBARDMENT_CASUALTIES;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ClearBombardmentCasualties implements BattleStep {
  private static final long serialVersionUID = -5723287846470464298L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return canBombardmentOccur() && clearCasualties()
        ? List.of(new StepDetails(REMOVE_BOMBARDMENT_CASUALTIES, this))
        : List.of();
  }

  @Override
  public BattleStep.Order getOrder() {
    return Order.NAVAL_BOMBARDMENT_REMOVE_CASUALTIES;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (canBombardmentOccur() && clearCasualties()) {
      battleActions.clearWaitingToDieAndDamagedChangesInto(bridge, BattleState.Side.DEFENSE);
    }
  }

  private boolean clearCasualties() {
    return !Properties.getNavalBombardCasualtiesReturnFire(
        battleState.getGameData().getProperties());
  }

  private boolean canBombardmentOccur() {
    return battleState.getStatus().isFirstRound()
        && !battleState.getBombardingUnits().isEmpty()
        && !battleState.getBattleSite().isWater();
  }
}
