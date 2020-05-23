package games.strategy.triplea.delegate.battle.grouptarget;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import org.triplea.java.collections.CollectionUtils;

@Value(staticConstructor = "of")
public class FiringGroup {
  Collection<Unit> firingGroup;
  Collection<Unit> validTargets;
  boolean suicide;
  String aaType;

  /**
   * Breaks list of units into groups of non suicide on hit units and each type of suicide on hit
   * units since each type of suicide on hit units need to roll separately to know which ones get
   * hits.
   */
  static List<Collection<Unit>> newFiringUnitGroups(final Collection<Unit> units) {

    // Sort suicide on hit units by type
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

    // Add all suicide on hit groups and the remaining units
    final List<Collection<Unit>> result = new ArrayList<>(map.values());
    final Collection<Unit> remainingUnits =
        CollectionUtils.getMatches(units, Matches.unitIsSuicideOnHit().negate());
    if (!remainingUnits.isEmpty()) {
      result.add(remainingUnits);
    }
    return result;
  }
}
