package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.CASUALTIES_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.CASUALTIES_WITHOUT_SPACE_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_PREFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.UNITS;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import java.util.function.BiFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Step where the player selects which units were hit by the dice roll */
@RequiredArgsConstructor
public class SelectCasualties implements BattleStep {

  private static final long serialVersionUID = -8657290250623679619L;

  @Getter private final BattleState battleState;

  /** The side of the firing player */
  @Getter private final BattleState.Side side;

  @Getter private final FiringGroup firingGroup;

  @Getter private final FireRoundState fireRoundState;

  private final BiFunction<IDelegateBridge, SelectCasualties, CasualtyDetails> selectCasualties;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return List.of(new StepDetails(getName(), this));
  }

  private String getName() {
    return battleState.getPlayer(side.getOpposite()).getName()
        + SELECT_PREFIX
        // displaying UNITS makes the text feel redundant so hide it if that is the group name
        + (firingGroup.getDisplayName().equals(UNITS)
            ? CASUALTIES_WITHOUT_SPACE_SUFFIX
            : firingGroup.getDisplayName() + CASUALTIES_SUFFIX);
  }

  @Override
  public Order getOrder() {
    return Order.FIRE_ROUND_SELECT_CASUALTIES;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    final DiceRoll diceRoll = fireRoundState.getDice();
    final String stepName =
        MarkCasualties.getPossibleOldNameForNotifyingBattleDisplay(
            battleState, firingGroup, side, getName());
    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      bridge.sendMessage(
          new IDisplay.NotifyDiceMessage(diceRoll, stepName, diceRoll.getPlayerName()));
    } else {
      bridge.getDisplayChannelBroadcaster().notifyDice(diceRoll, stepName);
    }

    final CasualtyDetails details = selectCasualties.apply(bridge, this);
    fireRoundState.setCasualties(details);
    BattleDelegate.markDamaged(details.getDamaged(), bridge, battleState.getBattleSite());
  }
}
