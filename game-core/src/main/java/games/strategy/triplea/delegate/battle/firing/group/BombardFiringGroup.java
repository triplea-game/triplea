package games.strategy.triplea.delegate.battle.firing.group;

import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;

/** Builds groups of bombarding units and their targets. */
@Builder
public class BombardFiringGroup {

  private @NonNull final Collection<Unit> firingUnits;
  private @NonNull final Collection<Unit> attackableUnits;
  private @NonNull final Boolean defending;

  public List<FiringGroup> getFiringGroups() {
    return RegularFiringGroup.getFiringGroupsWorker(defending, firingUnits, attackableUnits);
  }
}
