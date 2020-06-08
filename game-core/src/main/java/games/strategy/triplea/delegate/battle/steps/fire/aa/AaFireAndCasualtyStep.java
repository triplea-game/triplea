package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.AA_GUNS_FIRE_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.CASUALTIES_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_PREFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_PREFIX;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class AaFireAndCasualtyStep implements BattleStep {

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

  @Override
  public boolean valid() {
    return !aaGuns().isEmpty();
  }

  abstract GamePlayer firingPlayer();

  abstract GamePlayer firedAtPlayer();

  abstract Collection<Unit> aaGuns();
}
