package games.strategy.triplea.delegate.battle.firinggroups;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private final @NonNull Collection<Unit> firingGroup;
  private final @NonNull Collection<Unit> validTargets;
  private final boolean suicide;
  // type is currently only used by Aa firing groups
  private final @NonNull String type;

  static List<FiringGroup> newFiringUnitGroups(
      final Collection<Unit> units,
      final Function<Collection<Unit>, Collection<Unit>> getValidTargets,
      final String type) {

    // split the units by suicideOnHit and unitType (if suicideOnHit)
    final Map<UnitType, Collection<Unit>> suicideUnitsByType = new HashMap<>();
    final Collection<Unit> nonSuicideFiringGroup = new ArrayList<>();
    for (final Unit unit : units) {
      final UnitType unitType = unit.getType();
      if (UnitAttachment.get(unitType).getIsSuicideOnHit()) {
        suicideUnitsByType.computeIfAbsent(unitType, key -> new ArrayList<>()).add(unit);
      } else {
        nonSuicideFiringGroup.add(unit);
      }
    }

    final List<FiringGroup> result = new ArrayList<>();
    // add all of the suicide hit groups first
    buildSuicideFiringGroups(getValidTargets, type, suicideUnitsByType, result);

    // add the non suicide hit group last
    buildNonSuicideFiringGroup(getValidTargets, type, nonSuicideFiringGroup, result);
    return result;
  }

  private static void buildSuicideFiringGroups(
      final Function<Collection<Unit>, Collection<Unit>> getValidTargets,
      final String type,
      final Map<UnitType, Collection<Unit>> suicideUnitsByType,
      final List<FiringGroup> result) {
    for (final Collection<Unit> suicideFiringGroup : suicideUnitsByType.values()) {
      final Collection<Unit> validTargets = getValidTargets.apply(suicideFiringGroup);
      if (validTargets.isEmpty() || suicideFiringGroup.isEmpty()) {
        continue;
      }
      result.add(FiringGroup.of(suicideFiringGroup, validTargets, true, type));
    }
  }

  private static void buildNonSuicideFiringGroup(
      final Function<Collection<Unit>, Collection<Unit>> getValidTargets,
      final String type,
      final Collection<Unit> nonSuicideFiringGroup,
      final List<FiringGroup> result) {
    if (!nonSuicideFiringGroup.isEmpty()) {
      final Collection<Unit> validTargets = getValidTargets.apply(nonSuicideFiringGroup);
      if (!validTargets.isEmpty()) {
        result.add(FiringGroup.of(nonSuicideFiringGroup, validTargets, false, type));
      }
    }
  }
}
