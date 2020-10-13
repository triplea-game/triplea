package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ACTIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.AA_GUNS_FIRE_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.CASUALTIES_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_PREFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_PREFIX;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.fire.RollDice;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.triplea.sound.SoundUtils;

@AllArgsConstructor
public abstract class AaFireAndCasualtyStep implements BattleStep {
  private static final long serialVersionUID = -3195299749378932928L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    final List<String> steps = new ArrayList<>();
    if (!valid()) {
      return steps;
    }
    for (final String typeAa : UnitAttachment.getAllOfTypeAas(aaGuns())) {
      steps.add(firingPlayer().getName() + " " + typeAa + AA_GUNS_FIRE_SUFFIX);
      steps.add(firedAtPlayer().getName() + SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
      steps.add(firedAtPlayer().getName() + REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
    }
    return steps;
  }

  protected boolean valid() {
    return !aaGuns().isEmpty();
  }

  abstract GamePlayer firingPlayer();

  abstract GamePlayer firedAtPlayer();

  abstract Collection<Unit> aaGuns();

  public static class RollAaDice implements BiFunction<IDelegateBridge, RollDice, DiceRoll> {

    @Override
    public DiceRoll apply(final IDelegateBridge bridge, final RollDice step) {
      final DiceRoll dice =
          DiceRoll.rollAa(
              step.getFiringGroup().getTargetUnits(),
              step.getFiringGroup().getFiringUnits(),
              step.getBattleState().filterUnits(ACTIVE, step.getSide().getOpposite()),
              step.getBattleState().filterUnits(ACTIVE, step.getSide()),
              bridge,
              step.getBattleState().getBattleSite(),
              step.getSide() == DEFENSE);

      SoundUtils.playFireBattleAa(
          step.getBattleState().getPlayer(step.getSide()),
          step.getFiringGroup().getGroupName(),
          dice.getHits() > 0,
          bridge);
      return dice;
    }
  }
}
