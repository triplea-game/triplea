package games.strategy.triplea.delegate.battle.firing.group;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import lombok.Value;

/**
 * Breaks list of units into groups of non suicide on hit units and each type of suicide on hit
 * units since each type of suicide on hit units need to roll separately to know which ones get
 * hits.
 */
@Value(staticConstructor = "of")
public class FiringGroup {
  private final @NonNull Collection<Unit> firingUnits;
  private final @NonNull Collection<Unit> validTargets;
  private final @NonNull Boolean suicide;
  // type is currently only used by Aa firing groups and is the typeAa from the GameData
  private final @NonNull String type;

  static List<FiringGroup> newFiringUnitGroups(
      final Collection<Unit> units,
      final Function<Collection<Unit>, Collection<Unit>> getValidTargets,
      final String type) {

    // split the units by suicideOnHit and unitType (if suicideOnHit)
    final Multimap<UnitType, Unit> suicideUnitsByType = HashMultimap.create();
    final Collection<Unit> nonSuicideFiringGroup = new ArrayList<>();
    for (final Unit unit : units) {
      final UnitType unitType = unit.getType();
      if (UnitAttachment.get(unitType).getIsSuicideOnHit()) {
        suicideUnitsByType.put(unitType, unit);
      } else {
        nonSuicideFiringGroup.add(unit);
      }
    }

    final List<FiringGroup> result = new ArrayList<>();
    // add all of the suicide hit groups first
    result.addAll(buildSuicideFiringGroups(getValidTargets, type, suicideUnitsByType));

    // add the non suicide hit group last
    result.addAll(buildNonSuicideFiringGroup(getValidTargets, type, nonSuicideFiringGroup));
    return result;
  }

  private static List<FiringGroup> buildSuicideFiringGroups(
      final Function<Collection<Unit>, Collection<Unit>> getValidTargets,
      final String type,
      final Multimap<UnitType, Unit> suicideUnitsByType) {

    final List<FiringGroup> result = new ArrayList<>();
    for (final Collection<Unit> suicideFiringGroup : suicideUnitsByType.asMap().values()) {
      final Collection<Unit> validTargets = getValidTargets.apply(suicideFiringGroup);
      if (validTargets.isEmpty() || suicideFiringGroup.isEmpty()) {
        continue;
      }
      result.add(FiringGroup.of(suicideFiringGroup, validTargets, true, type));
    }
    return result;
  }

  private static List<FiringGroup> buildNonSuicideFiringGroup(
      final Function<Collection<Unit>, Collection<Unit>> getValidTargets,
      final String type,
      final Collection<Unit> nonSuicideFiringGroup) {
    final List<FiringGroup> result = new ArrayList<>();
    if (!nonSuicideFiringGroup.isEmpty()) {
      final Collection<Unit> validTargets = getValidTargets.apply(nonSuicideFiringGroup);
      if (!validTargets.isEmpty()) {
        result.add(FiringGroup.of(nonSuicideFiringGroup, validTargets, false, type));
      }
    }
    return result;
  }
}
