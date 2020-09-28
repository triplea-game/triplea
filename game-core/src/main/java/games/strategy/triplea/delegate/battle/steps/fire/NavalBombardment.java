package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.sound.SoundPath;

@AllArgsConstructor
public class NavalBombardment implements BattleStep {

  private static final long serialVersionUID = 3338296388191048761L;

  private static final BattleState.Side side = OFFENSE;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    return !valid()
        ? List.of()
        : getSteps().stream()
            .flatMap(step -> step.getNames().stream())
            .collect(Collectors.toList());
  }

  @Override
  public Order getOrder() {
    return Order.NAVAL_BOMBARDMENT;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (!valid()) {
      return;
    }
    final Collection<Unit> bombardingUnits = battleState.getBombardingUnits();

    if (!bombardingUnits.isEmpty()) {
      // bombarding units can't move after bombarding even if there are no units to bombard
      final Change change = ChangeFactory.markNoMovementChange(bombardingUnits);
      bridge.addChange(change);
    }

    final List<BattleStep> steps = getSteps();

    if (!steps.isEmpty()) {
      bridge
          .getSoundChannelBroadcaster()
          .playSoundForAll(SoundPath.CLIP_BATTLE_BOMBARD, battleState.getPlayer(side));

      // steps go in reverse order on the stack
      Collections.reverse(steps);
      steps.forEach(stack::push);
    }
  }

  private List<BattleStep> getSteps() {
    return FireStepsBuilder.buildSteps(
        FireStepsBuilder.Parameters.builder()
            .battleState(battleState)
            .battleActions(battleActions)
            .firingGroupFilter(FiringGroupFilterBombard.of())
            .side(side)
            .returnFire(
                Properties.getNavalBombardCasualtiesReturnFire(battleState.getGameData())
                    ? MustFightBattle.ReturnFire.ALL
                    : MustFightBattle.ReturnFire.NONE)
            .roll(new RollNormal())
            .selectCasualties(new SelectNormalCasualties())
            .build());
  }

  private boolean valid() {
    return battleState.getStatus().isFirstRound()
        && !battleState.getBombardingUnits().isEmpty()
        && !battleState.getBattleSite().isWater();
  }
}
