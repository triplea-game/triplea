package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.FIRE_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.List;
import java.util.function.BiFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Step where the dice are rolled for the firing units */
@RequiredArgsConstructor
public class RollDiceStep implements BattleStep {

  private static final long serialVersionUID = 3248059314449726590L;

  @Getter private final BattleState battleState;

  /** The side of the firing player */
  @Getter private final BattleState.Side side;

  @Getter private final FiringGroup firingGroup;

  private final FireRoundState fireRoundState;

  private final BiFunction<IDelegateBridge, RollDiceStep, DiceRoll> rollDice;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return List.of(new StepDetails(getName(), this));
  }

  private String getName() {
    return battleState.getPlayer(side).getName()
        // If the firing group's name is the default "UNITS", then the displayed string will be
        // "Germans units fire". In that case, it would be better to say "Germans fire".
        // If the firing group's name is something else, then the displayed string will be
        // something like "Germans submarines fire" which is ok.
        + (firingGroup.getDisplayName().equals(UNITS) ? "" : " " + firingGroup.getDisplayName())
        + FIRE_SUFFIX;
  }

  @Override
  public Order getOrder() {
    return Order.FIRE_ROUND_ROLL_DICE;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {

    // retain the targets that are still alive since the targets might have been shot
    // in an earlier firing round
    firingGroup.retainAliveTargets(battleState.filterUnits(ALIVE, side.getOpposite()));

    final DiceRoll dice = rollDice.apply(bridge, this);

    fireRoundState.setDice(dice);
  }
}
