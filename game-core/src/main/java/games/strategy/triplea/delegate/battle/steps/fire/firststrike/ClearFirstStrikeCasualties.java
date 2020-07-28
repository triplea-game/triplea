package games.strategy.triplea.delegate.battle.steps.fire.firststrike;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_SNEAK_ATTACK_CASUALTIES;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClearFirstStrikeCasualties implements BattleStep {

  enum State {
    SNEAK_ATTACK,
    NO_SNEAK_ATTACK,
  }

  private static final long serialVersionUID = 995118832242624142L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  protected final State offenseState;
  protected final State defenseState;

  public ClearFirstStrikeCasualties(
      final BattleState battleState, final BattleActions battleActions) {
    this.battleState = battleState;
    this.battleActions = battleActions;
    this.offenseState = calculateOffenseState();
    this.defenseState = calculateDefenseState();
  }

  private State calculateOffenseState() {
    if (battleState.getAttackingUnits().stream().anyMatch(Matches.unitIsFirstStrike())) {
      final boolean canSneakAttack =
          battleState.getDefendingUnits().stream().noneMatch(Matches.unitIsDestroyer());
      if (canSneakAttack) {
        return State.SNEAK_ATTACK;
      }
    }
    return State.NO_SNEAK_ATTACK;
  }

  private State calculateDefenseState() {
    if (battleState.getDefendingUnits().stream()
        .anyMatch(Matches.unitIsFirstStrikeOnDefense(battleState.getGameData()))) {
      final GameData gameData = battleState.getGameData();
      // WWW2V2 always gives defending subs sneak attack
      final boolean canSneakAttack =
          battleState.getAttackingUnits().stream().noneMatch(Matches.unitIsDestroyer())
              && (Properties.getWW2V2(gameData)
                  || Properties.getDefendingSubsSneakAttack(gameData));
      if (canSneakAttack) {
        return State.SNEAK_ATTACK;
      }
    }
    return State.NO_SNEAK_ATTACK;
  }

  @Override
  public List<String> getNames() {
    final List<String> steps = new ArrayList<>();
    if (offenseHasSneakAttack() || defenseHasSneakAttack()) {
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }
    return steps;
  }

  @Override
  public Order getOrder() {
    return Order.FIRST_STRIKE_REMOVE_CASUALTIES;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (!offenseHasSneakAttack() && !defenseHasSneakAttack()) {
      return;
    }

    boolean clearAttackingDead = true;
    boolean clearDefendingDead = true;
    if (Properties.getWW2V2(battleState.getGameData())) {
      // WWW2V2 subs always fire in a surprise attack phase even if their casualties will
      // be able to fire back.
      // So only clear the casualties if the subs have a true surprise attack.
      if (this.offenseState == State.SNEAK_ATTACK && this.defenseState != State.SNEAK_ATTACK) {
        clearAttackingDead = false;
      } else if (this.offenseState != State.SNEAK_ATTACK
          && this.defenseState == State.SNEAK_ATTACK) {
        clearDefendingDead = false;
      }
    }

    final Collection<Unit> unitsToRemove = new ArrayList<>();
    if (clearAttackingDead) {
      unitsToRemove.addAll(battleState.getAttackingWaitingToDie());
    }
    if (clearDefendingDead) {
      unitsToRemove.addAll(battleState.getDefendingWaitingToDie());
    }
    battleActions.remove(unitsToRemove, bridge, battleState.getBattleSite(), null);
    if (clearAttackingDead) {
      battleState.clearAttackingWaitingToDie();
    }
    if (clearDefendingDead) {
      battleState.clearDefendingWaitingToDie();
    }
  }

  private boolean offenseHasSneakAttack() {
    return this.offenseState == State.SNEAK_ATTACK;
  }

  private boolean defenseHasSneakAttack() {
    return this.defenseState == State.SNEAK_ATTACK;
  }
}
