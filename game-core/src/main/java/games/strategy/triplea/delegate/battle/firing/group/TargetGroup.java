package games.strategy.triplea.delegate.battle.firing.group;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;

/** Group of firing units and their targets. */
@Getter(AccessLevel.PRIVATE)
class TargetGroup {

  private final Set<UnitType> firingUnitTypes;
  private final Set<UnitType> targetUnitTypes;

  public TargetGroup(final UnitType firingUnitType, final Set<UnitType> targetUnitTypes) {
    firingUnitTypes = Set.of(firingUnitType);
    this.targetUnitTypes = targetUnitTypes;
  }

  Collection<Unit> getFiringUnits(final Collection<Unit> units) {
    return CollectionUtils.getMatches(units, Matches.unitIsOfTypes(firingUnitTypes));
  }

  Collection<Unit> getTargetUnits(final Collection<Unit> units) {
    return CollectionUtils.getMatches(units, Matches.unitIsOfTypes(targetUnitTypes));
  }

  /**
   * Find all target groupings for firing units and enemy units based on canNotTarget and
   * canNotBeTargetedBy attributes. Also takes into account if any destroyers are present which
   * cancel canNotBeTargetedBy. Sort all the target groups so those with the least number of targets
   * appear first.
   */
  static List<TargetGroup> newTargetGroups(
      final Collection<Unit> units, final Collection<Unit> enemyUnits) {

    final Set<UnitType> unitTypes =
        units.stream().map(unit -> unit.getType()).collect(Collectors.toSet());
    final Set<UnitType> enemyUnitTypes =
        enemyUnits.stream().map(unit -> unit.getType()).collect(Collectors.toSet());
    final List<TargetGroup> targetGroups = new ArrayList<TargetGroup>();
    for (final UnitType unitType : unitTypes) {
      final Set<UnitType> targets = findTargets(unitType, unitTypes, enemyUnitTypes);
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
      final UnitType unitType, final Set<UnitType> unitTypes, final Set<UnitType> enemyUnitTypes) {
    final Set<UnitType> targets = new HashSet<>(enemyUnitTypes);
    targets.removeAll(UnitAttachment.get(unitType).getCanNotTarget());
    return unitTypes.stream().anyMatch(Matches.unitTypeIsDestroyer())
        ? targets
        : targets.stream()
            .filter(
                target -> !UnitAttachment.get(target).getCanNotBeTargetedBy().contains(unitType))
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
