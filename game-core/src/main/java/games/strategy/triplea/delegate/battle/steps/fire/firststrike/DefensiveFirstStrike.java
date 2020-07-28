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

public class DefensiveFirstStrike implements BattleStep {

  private enum State {
    NOT_APPLICABLE,
    REGULAR,
    FIRST_STRIKE,
  }

  private static final long serialVersionUID = 3646211932844911163L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  protected final State state;

  protected final transient ReturnFire returnFire;

  public DefensiveFirstStrike(final BattleState battleState, final BattleActions battleActions) {
    this.battleState = battleState;
    this.battleActions = battleActions;
    this.state = calculateState();
    this.returnFire = ReturnFire.ALL;
  }

  /** Constructor for save compatibility */
  public DefensiveFirstStrike(
      final BattleState battleState,
      final BattleActions battleActions,
      final ReturnFire returnFire) {
    this.battleState = battleState;
    this.battleActions = battleActions;
    this.state = State.FIRST_STRIKE;
    this.returnFire = returnFire;
  }

  private State calculateState() {
    if (battleState.getDefendingUnits().stream()
        .noneMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()))) {
      return State.NOT_APPLICABLE;
    }

    // ww2v2 rules require subs to always fire in a sub phase
    if (Properties.getWW2V2(battleState.getGameData())) {
      return State.FIRST_STRIKE;
    }

    final boolean canSneakAttack =
        battleState.getAttackingUnits().stream().noneMatch(Matches.unitIsDestroyer())
            && Properties.getDefendingSubsSneakAttack(battleState.getGameData());
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

    steps.add(battleState.getDefender().getName() + FIRST_STRIKE_UNITS_FIRE);
    steps.add(battleState.getAttacker().getName() + SELECT_FIRST_STRIKE_CASUALTIES);

    return steps;
  }

  @Override
  public Order getOrder() {
    if (this.state == State.REGULAR) {
      return Order.FIRST_STRIKE_DEFENSIVE_REGULAR;
    }
    return Order.FIRST_STRIKE_DEFENSIVE;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (this.state == State.NOT_APPLICABLE) {
      return;
    }
    battleActions.findTargetGroupsAndFire(
        returnFire,
        battleState.getAttacker().getName() + SELECT_FIRST_STRIKE_CASUALTIES,
        true,
        battleState.getDefender(),
        Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()),
        battleState.getDefendingUnits(),
        battleState.getDefendingWaitingToDie(),
        battleState.getAttackingUnits(),
        battleState.getAttackingWaitingToDie());
  }
}
