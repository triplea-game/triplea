package games.strategy.triplea.delegate.battle.steps.fire;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

/**
 * Group of units that are firing on targets
 *
 * <p>If the group is suicideOnHit, then all of the units should have the same unit type
 */
@Value
public class FiringGroup {
  String displayName;
  String groupName;
  Collection<Unit> firingUnits;
  Collection<Unit> targetUnits;
  boolean suicideOnHit;

  public Collection<Unit> getTargetUnits() {
    return Collections.unmodifiableCollection(targetUnits);
  }

  public Collection<Unit> getFiringUnits() {
    return Collections.unmodifiableCollection(firingUnits);
  }

  /**
   * Keeps alive units around
   *
   * <p>Units unfortunately don't track their own status so the list of targets needs to be updated
   * as the battle progresses.
   *
   * @param aliveUnits Units that are still alive
   */
  public void retainAliveTargets(final Collection<Unit> aliveUnits) {
    targetUnits.retainAll(aliveUnits);
  }

  /**
   * Splits up the firingUnits by suicideOnHit status and groups them by unit type
   *
   * @param name Name of the firing units
   * @param firingUnits Collection of units that are firing
   * @param targetUnits Collection of units that are being hit
   * @return List of FiringGroup
   */
  public static List<FiringGroup> groupBySuicideOnHit(
      final String name, final Collection<Unit> firingUnits, final Collection<Unit> targetUnits) {
    final List<FiringGroup> groups = new ArrayList<>();
    final List<Collection<Unit>> separatedBySuicide = separateSuicideOnHit(firingUnits);

    // ensure each firing group has a unique name by adding prefixes
    // if the firing groups have different types of suicide units
    if (separatedBySuicide.size() == 1) {
      groups.add(
          new FiringGroup(
              name,
              name,
              separatedBySuicide.get(0),
              targetUnits,
              Matches.unitIsSuicideOnHit().test(separatedBySuicide.get(0).iterator().next())));

    } else if (separatedBySuicide.size() == 2
        && Matches.unitIsSuicideOnHit().test(separatedBySuicide.get(0).iterator().next())
            != Matches.unitIsSuicideOnHit().test(separatedBySuicide.get(1).iterator().next())) {
      groups.addAll(
          generateFiringGroupsWithOneSuicideAndOneNonSuicide(
              name, targetUnits, separatedBySuicide));

    } else {
      groups.addAll(generateFiringGroups(name, targetUnits, separatedBySuicide));
    }
    return groups;
  }

  /**
   * Separate the suicide on hit units from the others and group them by their type. The suicide on
   * hit units need to fire separately so that they can be removed if they hit.
   */
  private static List<Collection<Unit>> separateSuicideOnHit(final Collection<Unit> units) {

    final Map<UnitType, Collection<Unit>> map = new HashMap<>();
    for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit())) {
      final UnitType type = unit.getType();
      if (map.containsKey(type)) {
        map.get(type).add(unit);
      } else {
        final Collection<Unit> unitList = new ArrayList<>();
        unitList.add(unit);
        map.put(type, unitList);
      }
    }

    final List<Collection<Unit>> result = new ArrayList<>(map.values());
    final Collection<Unit> remainingUnits =
        CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit().negate());
    if (!remainingUnits.isEmpty()) {
      result.add(remainingUnits);
    }
    return result;
  }

  /** Handle the case where there are only two groups and one is suicide and the other is not */
  private static List<FiringGroup> generateFiringGroupsWithOneSuicideAndOneNonSuicide(
      final String name,
      final Collection<Unit> targetUnits,
      final List<Collection<Unit>> separatedBySuicide) {
    final List<FiringGroup> groups = new ArrayList<>();
    for (final Collection<Unit> newFiringUnits : separatedBySuicide) {
      final boolean isSuicideOnHit =
          Matches.unitIsSuicideOnHit().test(newFiringUnits.iterator().next());
      final String nameWithSuffix = name + (isSuicideOnHit ? " suicide" : "");
      groups.add(
          new FiringGroup(nameWithSuffix, name, newFiringUnits, targetUnits, isSuicideOnHit));
    }
    return groups;
  }

  private static List<FiringGroup> generateFiringGroups(
      final String name,
      final Collection<Unit> targetUnits,
      final List<Collection<Unit>> separatedBySuicide) {
    final List<FiringGroup> groups = new ArrayList<>();
    for (final Collection<Unit> newFiringUnits : separatedBySuicide) {
      final Unit firstUnit = newFiringUnits.iterator().next();
      final boolean isSuicideOnHit = Matches.unitIsSuicideOnHit().test(firstUnit);
      final String nameWithSuffix =
          name + (isSuicideOnHit ? " suicide " + firstUnit.getType().getName() : "");
      groups.add(
          new FiringGroup(nameWithSuffix, name, newFiringUnits, targetUnits, isSuicideOnHit));
    }
    return groups;
  }
}
