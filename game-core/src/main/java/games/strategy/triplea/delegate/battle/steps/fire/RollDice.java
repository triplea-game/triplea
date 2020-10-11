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
public class RollDice implements BattleStep {

  private static final long serialVersionUID = 3248059314449726590L;

  @Getter private final BattleState battleState;

  /** The side of the firing player */
  @Getter private final BattleState.Side side;

  @Getter private final FiringGroup firingGroup;

  private final FireRoundState fireRoundState;

  private final BiFunction<IDelegateBridge, RollDice, DiceRoll> rollDice;

  @Override
  public List<String> getNames() {
    return List.of(getName());
  }

  private String getName() {
    return battleState.getPlayer(side).getName()
        // displaying UNITS makes the text feel redundant so hide it if that is the group name
        + (firingGroup.getDisplayName().equals(UNITS) ? "" : " " + firingGroup.getDisplayName())
        + FIRE_SUFFIX;
  }

  @Override
  public Order getOrder() {
    return Order.ROLL_DICE;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {

    // remove any target unit that was hit by other units
    firingGroup.retainAliveTargets(battleState.filterUnits(ALIVE, side.getOpposite()));

    final DiceRoll dice = rollDice.apply(bridge, this);

    fireRoundState.setDice(dice);
  }
}
