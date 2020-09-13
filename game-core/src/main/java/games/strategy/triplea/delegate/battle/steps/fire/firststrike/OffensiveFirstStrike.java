package games.strategy.triplea.delegate.battle.steps.fire.firststrike;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.FIRST_STRIKE_UNITS_FIRE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_FIRST_STRIKE_CASUALTIES;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.ArrayList;
import java.util.List;

public class OffensiveFirstStrike implements BattleStep {

  private enum State {
    NOT_APPLICABLE,
    REGULAR,
    FIRST_STRIKE,
  }

  private static final long serialVersionUID = -2154415762808582704L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  protected final State state;

  protected transient ReturnFire returnFire = ReturnFire.ALL;

  public OffensiveFirstStrike(final BattleState battleState, final BattleActions battleActions) {
    this.battleState = battleState;
    this.battleActions = battleActions;
    this.state = calculateState();
  }

  /** Constructor for save compatibility */
  public OffensiveFirstStrike(
      final BattleState battleState,
      final BattleActions battleActions,
      final ReturnFire returnFire) {
    this.battleState = battleState;
    this.battleActions = battleActions;
    this.state = State.FIRST_STRIKE;
    this.returnFire = returnFire;
  }

  private State calculateState() {
    if (battleState.getUnits(BattleState.Side.OFFENSE).stream()
        .noneMatch(Matches.unitIsFirstStrike())) {
      return State.NOT_APPLICABLE;
    }

    // ww2v2 rules require subs to always fire in a sub phase
    if (Properties.getWW2V2(battleState.getGameData())) {
      return State.FIRST_STRIKE;
    }

    final boolean canSneakAttack =
        battleState.getUnits(BattleState.Side.DEFENSE).stream()
            .noneMatch(Matches.unitIsDestroyer());
    if (canSneakAttack) {
      return State.FIRST_STRIKE;
    }
    return State.REGULAR;
  }

  @Override
  public List<String> getNames() {
    final List<String> steps = new ArrayList<>();
    if (this.state == State.NOT_APPLICABLE) {
      return steps;
    }

    steps.add(battleState.getAttacker().getName() + FIRST_STRIKE_UNITS_FIRE);
    steps.add(battleState.getDefender().getName() + SELECT_FIRST_STRIKE_CASUALTIES);

    return steps;
  }

  @Override
  public Order getOrder() {
    if (this.state == State.REGULAR) {
      return Order.FIRST_STRIKE_OFFENSIVE_REGULAR;
    }
    return Order.FIRST_STRIKE_OFFENSIVE;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (this.state == State.NOT_APPLICABLE) {
      return;
    }
    battleActions.findTargetGroupsAndFire(
        returnFire,
        battleState.getDefender().getName() + SELECT_FIRST_STRIKE_CASUALTIES,
        false,
        battleState.getAttacker(),
        Matches.unitIsFirstStrike(),
        battleState.getUnits(BattleState.Side.OFFENSE),
        battleState.getAttackingWaitingToDie(),
        battleState.getUnits(BattleState.Side.DEFENSE),
        battleState.getDefendingWaitingToDie());
  }
}
