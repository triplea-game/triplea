package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleActions;
import java.util.Collection;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class StepParameters {

  public final @NonNull Collection<Unit> attackingUnits;
  public final @NonNull Collection<Unit> defendingUnits;
  public final @NonNull BattleActions battleActions;
}
