package games.strategy.triplea.delegate.battle;

import com.google.common.collect.Sets;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;

/** Group of firing units and their targets. */
@Getter(AccessLevel.PRIVATE)
public class TargetGroup {

  private final Set<UnitType> firingUnitTypes;
  private final Set<UnitType> targetUnitTypes;

  public TargetGroup(final UnitType firingUnitType, final Set<UnitType> targetUnitTypes) {
    firingUnitTypes = Sets.newHashSet(firingUnitType);
    this.targetUnitTypes = targetUnitTypes;
  }

  public Collection<Unit> getFiringUnits(final Collection<Unit> units) {
    return CollectionUtils.getMatches(units, Matches.unitIsOfTypes(firingUnitTypes));
  }

  public Collection<Unit> getTargetUnits(final Collection<Unit> units) {
    return CollectionUtils.getMatches(units, Matches.unitIsOfTypes(targetUnitTypes));
  }

  /**
   * Find all target groupings for firing units and enemy units based on canNotTarget and
   * canNotBeTargetedBy attributes. Also takes into account if any destroyers are present which
   * cancel canNotBeTargetedBy. Sort all the target groups so most restrictive appear first.
   */
  public static List<TargetGroup> newTargetGroups(
      final Collection<Unit> units, final Collection<Unit> enemyUnits) {

    final Set<UnitType> unitTypes =
        units.stream().map(unit -> unit.getType()).collect(Collectors.toSet());
    final Set<UnitType> enemyUnitTypes =
        enemyUnits.stream().map(unit -> unit.getType()).collect(Collectors.toSet());
    final List<TargetGroup> firingGroups = new ArrayList<TargetGroup>();
    for (final UnitType unitType : unitTypes) {
      final Set<UnitType> targets = findTargets(unitType, unitTypes, enemyUnitTypes);
      boolean isAdded = false;
      for (final TargetGroup firingGroup : firingGroups) {
        if (firingGroup.getTargetUnitTypes().equals(targets)) {
          firingGroup.getFiringUnitTypes().add(unitType);
          isAdded = true;
          break;
        }
      }
      if (!isAdded) {
        firingGroups.add(new TargetGroup(unitType, targets));
      }
    }
    return sortFiringGroups(firingGroups);
  }

  private static Set<UnitType> findTargets(
      final UnitType unitType, final Set<UnitType> unitTypes, final Set<UnitType> enemyUnitTypes) {
    final Set<UnitType> targets = new HashSet<>(enemyUnitTypes);
    targets.removeAll(UnitAttachment.get(unitType).getCanNotTarget());
    if (!unitTypes.stream().anyMatch(Matches.unitTypeIsDestroyer())) {
      for (final UnitType target : targets) {
        if (UnitAttachment.get(target).getCanNotBeTargetedBy().contains(unitType)) {
          targets.remove(target);
        }
      }
    }
    return targets;
  }

  private static List<TargetGroup> sortFiringGroups(final List<TargetGroup> firingGroups) {
    return firingGroups.stream()
        .sorted(Comparator.comparingInt(firingGroup -> firingGroup.getTargetUnitTypes().size()))
        .collect(Collectors.toList());
  }
}
