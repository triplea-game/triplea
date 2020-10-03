package games.strategy.triplea.delegate.battle.steps.fire;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

/**
 * Group of units that are firing on targets
 *
 * <p>If the group is suicideOnHit, then all of the units should have the same unit type
 */
@Value
public class FiringGroup {

  private static final UnitType NON_SUICIDE_MULTIMAP_KEY = new UnitType("nonsuicide", null);

  String displayName;
  String groupName;
  Collection<Unit> firingUnits;
  Collection<Unit> targetUnits;
  boolean suicideOnHit;

  private FiringGroup(
      final String displayName,
      final String groupName,
      final Collection<Unit> firingUnits,
      final Collection<Unit> targetUnits) {
    this.displayName = displayName;
    this.groupName = groupName;
    this.firingUnits = firingUnits;
    this.targetUnits = targetUnits;
    this.suicideOnHit = this.firingUnits.stream().anyMatch(Matches.unitIsSuicideOnHit());
  }

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
    final Multimap<UnitType, Unit> separatedBySuicide = separateSuicideOnHit(firingUnits);

    return separatedBySuicide.asMap().values().stream()
        .map(
            units ->
                new FiringGroup(
                    generateName(name, units, separatedBySuicide), name, units, targetUnits))
        .collect(Collectors.toList());
  }

  /**
   * Separate the suicide on hit units from the others and group them by their type. The suicide on
   * hit units need to fire separately so that they can be removed if they hit.
   */
  private static Multimap<UnitType, Unit> separateSuicideOnHit(final Collection<Unit> units) {

    final Multimap<UnitType, Unit> map = ArrayListMultimap.create();
    for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit())) {
      final UnitType type = unit.getType();
      map.put(type, unit);
    }

    final Collection<Unit> remainingUnits =
        CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit().negate());
    if (!remainingUnits.isEmpty()) {
      map.putAll(NON_SUICIDE_MULTIMAP_KEY, remainingUnits);
    }
    return map;
  }

  private static String generateName(
      final String originalName,
      final Collection<Unit> firingUnits,
      final Multimap<UnitType, Unit> separatedBySuicide) {

    if (separatedBySuicide.keySet().size() == 1) {
      return originalName;

    } else if (separatedBySuicide.keySet().size() == 2
        && separatedBySuicide.containsKey(NON_SUICIDE_MULTIMAP_KEY)) {
      // special case where one firing group is suicideOnHit and the other is not
      return firingUnits.stream().allMatch(Matches.unitIsSuicideOnHit())
          ? originalName + " suicide"
          : originalName;

    } else {
      return firingUnits.stream().allMatch(Matches.unitIsSuicideOnHit())
          ? originalName + " suicide " + firingUnits.iterator().next().getType().getName()
          : originalName;
    }
  }
}
