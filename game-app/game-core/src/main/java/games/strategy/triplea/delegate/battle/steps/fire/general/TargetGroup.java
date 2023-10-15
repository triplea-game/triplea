package games.strategy.triplea.delegate.battle.steps.fire.general;

import com.google.common.collect.Sets;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;

/** Group of firing units and their targets. */
@Getter(AccessLevel.PACKAGE)
class TargetGroup {

  private final Set<UnitType> firingUnitTypes;
  private final Set<UnitType> targetUnitTypes;

  TargetGroup(final UnitType firingUnitType, final Set<UnitType> targetUnitTypes) {
    firingUnitTypes = Sets.newHashSet(firingUnitType);
    this.targetUnitTypes = targetUnitTypes;
  }

  public List<Unit> getFiringUnits(final Collection<Unit> units) {
    return CollectionUtils.getMatches(units, Matches.unitIsOfTypes(firingUnitTypes));
  }

  public Collection<Unit> getTargetUnits(final Collection<Unit> units) {
    return CollectionUtils.getMatches(units, Matches.unitIsOfTypes(targetUnitTypes));
  }

  /**
   * Find all target groupings for firing units and enemy units based on canNotTarget and
   * canNotBeTargetedBy attributes. Also takes into account if any destroyers are present which
   * cancel canNotBeTargetedBy. Sort all the target groups so those with the least number of targets
   * appear first.
   */
  public static List<TargetGroup> newTargetGroups(
      final Collection<Unit> units, final Collection<Unit> enemyUnits) {
    final Set<UnitType> unitTypes = UnitUtils.getUnitTypesFromUnitList(units);
    final boolean destroyerPresent = unitTypes.stream().anyMatch(Matches.unitTypeIsDestroyer());
    final Set<UnitType> enemyUnitTypes = UnitUtils.getUnitTypesFromUnitList(enemyUnits);
    final List<TargetGroup> targetGroups = new ArrayList<>();
    for (final UnitType unitType : unitTypes) {
      final Set<UnitType> targets = findTargets(unitType, destroyerPresent, enemyUnitTypes);
      if (targets.isEmpty()) {
        continue;
      }
      final Optional<TargetGroup> targetGroup = findTargetsInTargetGroups(targets, targetGroups);
      if (targetGroup.isPresent()) {
        targetGroup.get().getFiringUnitTypes().add(unitType);
      } else {
        targetGroups.add(new TargetGroup(unitType, targets));
      }
    }
    return sortTargetGroups(targetGroups);
  }

  private static Set<UnitType> findTargets(
      UnitType unitType, boolean destroyerPresent, Set<UnitType> enemyUnitTypes) {
    Set<UnitType> cannotTarget = unitType.getUnitAttachment().getCanNotTarget();
    // Note: uses a single stream instead of a sequence of removeAll() calls for performance.
    return enemyUnitTypes.stream()
        .filter(
            targetUnitType -> {
              if (cannotTarget.contains(targetUnitType)) {
                return false;
              }
              if (destroyerPresent) {
                return true;
              }
              return !targetUnitType.getUnitAttachment().getCanNotBeTargetedBy().contains(unitType);
            })
        .collect(Collectors.toSet());
  }

  private static Optional<TargetGroup> findTargetsInTargetGroups(
      final Set<UnitType> targets, final List<TargetGroup> targetGroups) {
    return targetGroups.stream()
        .filter(targetGroup -> targetGroup.getTargetUnitTypes().equals(targets))
        .findAny();
  }

  private static List<TargetGroup> sortTargetGroups(final List<TargetGroup> targetGroups) {
    return targetGroups.stream()
        .sorted(Comparator.comparingInt(targetGroup -> targetGroup.getTargetUnitTypes().size()))
        .collect(Collectors.toList());
  }
}
