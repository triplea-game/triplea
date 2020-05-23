package games.strategy.triplea.delegate.battle.grouptarget;

import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class Bombard {

  private @NonNull final Collection<Unit> firingUnits;
  private @NonNull final Collection<Unit> attackableUnits;
  private final boolean defending;

  public List<FiringGroup> getFiringGroups() {
    final List<FiringGroup> groupsAndTargets = new ArrayList<>();
    Regular.getFiringGroupsWorker(defending, groupsAndTargets, firingUnits, attackableUnits);
    return groupsAndTargets;
  }
}
