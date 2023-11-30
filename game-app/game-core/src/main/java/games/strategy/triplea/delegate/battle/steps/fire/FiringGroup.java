package games.strategy.triplea.delegate.battle.steps.fire;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.CollectionUtils;

/**
 * Group of units that are firing on targets
 *
 * <p>If the group is suicideOnHit, then all of the units must have the same unit type. This is
 * because suicideOnHit units need to die when they hit a unit. If a firing group has multiple unit
 * types, then it is harder to determine which unit type hit and so must commit suicide. By forcing
 * the firing group to have all of the same unit type, it the units to commit suicide can be easily
 * figured out since all of the units are the same.
 */
@Value
public class FiringGroup implements Serializable {
  private static final long serialVersionUID = -6236125682894804895L;

  String displayName;
  String groupName;
  @Getter Collection<Unit> firingUnits;
  @Getter Collection<Unit> targetUnits;
  boolean suicideOnHit;

  @Value(staticConstructor = "of")
  private static class SuicideAndNonSuicide {
    Multimap<UnitType, Unit> suicideGroups;
    Collection<Unit> nonSuicideGroup;

    Collection<Collection<Unit>> values() {
      final Collection<Collection<Unit>> values = new ArrayList<>(suicideGroups.asMap().values());
      if (!nonSuicideGroup.isEmpty()) {
        values.add(nonSuicideGroup);
      }
      return values;
    }

    int groupCount() {
      return suicideGroups.keySet().size() + (nonSuicideGroup.isEmpty() ? 0 : 1);
    }
  }

  private FiringGroup(
      final String displayName,
      final String groupName,
      final Collection<Unit> firingUnits,
      final Collection<Unit> targetUnits) {
    this.displayName = displayName;
    this.groupName = groupName;
    this.firingUnits = firingUnits;
    this.targetUnits = targetUnits;
    this.suicideOnHit = this.firingUnits.stream().allMatch(Matches.unitIsSuicideOnHit());
  }

  // This converts firing groups from old saves
  @RemoveOnNextMajorRelease
  @Deprecated
  public FiringGroup(
      final String displayName,
      final Collection<Unit> firingUnits,
      final Collection<Unit> targetUnits,
      final boolean suicideOnHit) {
    this.displayName = displayName;
    this.groupName = displayName;
    this.firingUnits = firingUnits;
    this.targetUnits = targetUnits;
    this.suicideOnHit = suicideOnHit;
  }

  /**
   * Retain targeted units for future firing
   *
   * <p>Units unfortunately don't track their own status so the list of targets needs to be updated
   * as the battle progresses.
   *
   * <p>By the time this firing group gets a chance to fire, a previous firing group might have made
   * the targets un-targetable (such as killing them).
   *
   * <p>The caller will be tracking the unit status and so can call this with the list of targetable
   * units to ensure that this firing group will only target units that are available to be fired
   * at.
   *
   * @param aliveUnits Units that need to be retained as targets
   */
  public void retainAliveTargets(final Collection<Unit> aliveUnits) {
    targetUnits.retainAll(new HashSet<>(aliveUnits));
  }

  /**
   * Groups the suicideOnHit firingUnits by unit type and groups the non-suicideOnHit firingUnits
   * all together.
   *
   * <p>See the class documentation on why suicideOnHit matters.
   */
  public static List<FiringGroup> groupBySuicideOnHit(
      final String name, final Collection<Unit> firingUnits, final Collection<Unit> targetUnits) {
    final SuicideAndNonSuicide separatedBySuicide = separateSuicideOnHit(firingUnits);

    return separatedBySuicide.values().stream()
        .map(
            units ->
                new FiringGroup(
                    generateName(name, units, separatedBySuicide), name, units, targetUnits))
        .collect(Collectors.toList());
  }

  private static SuicideAndNonSuicide separateSuicideOnHit(final Collection<Unit> units) {
    final Predicate<Unit> isSuicideOnHit = Matches.unitIsSuicideOnHit();

    final Multimap<UnitType, Unit> map = ArrayListMultimap.create();
    final List<Unit> remainingUnits = new ArrayList<>();
    for (final Unit unit : units) {
      if (isSuicideOnHit.test(unit)) {
        map.put(unit.getType(), unit);
      } else {
        remainingUnits.add(unit);
      }
    }

    return SuicideAndNonSuicide.of(map, remainingUnits);
  }

  private static String generateName(
      final String originalName,
      final Collection<Unit> firingUnits,
      final SuicideAndNonSuicide separatedBySuicide) {

    if (separatedBySuicide.groupCount() == 1) {
      // there is only one firing group so no need to give unique suffices
      return originalName;

    } else if (separatedBySuicide.groupCount() == 2
        && !separatedBySuicide.nonSuicideGroup.isEmpty()
        && firingUnits.stream().allMatch(Matches.unitIsSuicideOnHit())) {
      // there are two firing groups and one of them is non suicide. So the suicide group
      // doesn't need to have its type name added to it.
      return originalName + " suicide";
    }

    // add a suffix to suicide groups that includes their type name to differentiate them
    return firingUnits.stream().allMatch(Matches.unitIsSuicideOnHit())
        ? originalName + " suicide " + CollectionUtils.getAny(firingUnits).getType().getName()
        : originalName;
  }
}
