package games.strategy.triplea.delegate.battle.firinggroups;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.TargetGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;

@Builder
public class Regular {

  private @NonNull final Collection<Unit> allFiringUnits;
  private @NonNull final Collection<Unit> allEnemyUnits;
  private final boolean defending;

  public List<FiringGroup> getFiringGroups() {

    final List<FiringGroup> groupsAndTargets = new ArrayList<>();

    final List<TargetGroup> targetGroups =
        TargetGroup.newTargetGroups(allFiringUnits, allEnemyUnits);
    for (final TargetGroup targetGroup : targetGroups) {
      final Collection<Unit> firingUnits = targetGroup.getFiringUnits(allFiringUnits);
      final Collection<Unit> attackableUnits = targetGroup.getTargetUnits(allEnemyUnits);
      groupsAndTargets.addAll(getFiringGroupsWorker(defending, firingUnits, attackableUnits));
    }
    return groupsAndTargets;
  }

  static List<FiringGroup> getFiringGroupsWorker(
      final boolean defending,
      final Collection<Unit> firingUnits,
      final Collection<Unit> attackableUnits) {
    final Collection<Unit> targetUnits =
        CollectionUtils.getMatches(
            attackableUnits,
            PredicateBuilder.of(Matches.unitIsNotInfrastructure())
                .andIf(defending, Matches.unitIsSuicideOnAttack().negate())
                .andIf(!defending, Matches.unitIsSuicideOnDefense().negate())
                .build());

    if (targetUnits.isEmpty() || firingUnits.isEmpty()) {
      return List.of();
    }
    return FiringGroup.newFiringUnitGroups(firingUnits, firingGroup -> targetUnits, "");
  }
}
