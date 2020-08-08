package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.NAVAL_BOMBARDMENT;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_NAVAL_BOMBARDMENT_CASUALTIES;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NavalBombardment implements BattleStep {

  private static final long serialVersionUID = 3338296388191048761L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    final List<String> steps = new ArrayList<>();
    if (!valid()) {
      return steps;
    }
    steps.add(NAVAL_BOMBARDMENT);
    steps.add(SELECT_NAVAL_BOMBARDMENT_CASUALTIES);
    return steps;
  }

  @Override
  public Order getOrder() {
    return Order.NAVAL_BOMBARDMENT;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (valid()) {
      battleActions.fireNavalBombardment(bridge);
    }
  }

  private boolean valid() {
    return battleState.getBattleRound() == 1
        && !battleState.getBombardingUnits().isEmpty()
        && !battleState.getBattleSite().isWater();
  }
}
