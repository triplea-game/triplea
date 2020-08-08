package games.strategy.triplea.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.mapdata.MapData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Separates a group of units into distinct categories. */
public class UnitSeparator {
  private UnitSeparator() {}

  /**
   * Finds unit categories, removes not displayed, and then sorts them into logical order to display
   * based on: 1. Unit owner: territory owner, not at war with territory owner, player order in XML
   * 2. Unit type: 0 movement, can't combat move, sea, air if sea territory, land, air if land
   * territory 3. Within each of those groups sort the units by XML order in UnitList
   */
  public static List<UnitCategory> getSortedUnitCategories(
      final Territory t, final MapData mapData) {
    final GameData data = t.getData();
    final List<UnitCategory> categories = new ArrayList<>(UnitSeparator.categorize(t.getUnits()));
    categories.removeIf(uc -> !mapData.shouldDrawUnit(uc.getType().getName()));
    final List<UnitType> xmlUnitTypes = new ArrayList<>(data.getUnitTypeList().getAllUnitTypes());
    categories.sort(
        Comparator.comparing(
                UnitCategory::getOwner,
                Comparator.comparing((final GamePlayer p) -> !p.equals(t.getOwner()))
                    .thenComparing(p -> Matches.isAtWar(p, data).test(t.getOwner()))
                    .thenComparing(data.getPlayerList().getPlayers()::indexOf))
            .thenComparing(uc -> Matches.unitTypeCanMove(uc.getOwner()).test(uc.getType()))
            .thenComparing(
                UnitCategory::getType,
                Comparator.comparing(
                        (final UnitType ut) ->
                            !Matches.unitTypeCanNotMoveDuringCombatMove().test(ut))
                    .thenComparing(ut -> !Matches.unitTypeIsSea().test(ut))
                    .thenComparing(ut -> !(t.isWater() && Matches.unitTypeIsAir().test(ut)))
                    .thenComparing(ut -> !Matches.unitTypeIsLand().test(ut))
                    .thenComparing(xmlUnitTypes::indexOf)));
    return categories;
  }

  public static Set<UnitCategory> categorize(final Collection<Unit> units) {
    return categorize(units, null, false, false);
  }

  public static Set<UnitCategory> categorize(
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> dependent,
      final boolean categorizeMovement,
      final boolean categorizeTransportCost,
      final boolean sort) {
    return categorize(
        units,
        dependent,
        categorizeMovement,
        categorizeTransportCost, /* ctgzTrnMovement */
        false,
        sort);
  }

  /**
   * Break the units into discrete categories. Do this based on unit owner, and optionally dependent
   * units and movement
   *
   * @param dependent - can be null
   * @param categorizeMovement - whether to categorize by movement
   * @param categorizeTrnMovement - whether to categorize transports by movement
   * @param sort If true then sort the categories in UnitCategory order; if false, then leave
   *     categories in original order (based on units).
   * @return a Collection of UnitCategories
   */
  public static Set<UnitCategory> categorize(
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> dependent,
      final boolean categorizeMovement,
      final boolean categorizeTransportCost,
      final boolean categorizeTrnMovement,
      final boolean sort) {
    // somewhat odd, but we map UnitCategory->UnitCategory, key and value are the same
    // we do this to take advantage of .equals() on objects that are equal in a special way
    final Map<UnitCategory, UnitCategory> categories;
    if (sort) {
      categories = new HashMap<>();
    } else {
      categories = new LinkedHashMap<>();
    }
    for (final Unit current : units) {
      BigDecimal unitMovement = new BigDecimal(-1);
      if (categorizeMovement
          || (categorizeTrnMovement && Matches.unitIsTransport().test(current))) {
        unitMovement = current.getMovementLeft();
      }
      int unitTransportCost = -1;
      if (categorizeTransportCost) {
        unitTransportCost = UnitAttachment.get(current.getType()).getTransportCost();
      }
      Collection<Unit> currentDependents = null;
      if (dependent != null) {
        currentDependents = dependent.get(current);
      }
      final boolean disabled = Matches.unitIsDisabled().test(current);
      final UnitCategory entry =
          new UnitCategory(
              current,
              currentDependents,
              unitMovement,
              current.getHits(),
              current.getUnitDamage(),
              disabled,
              unitTransportCost);
      // we test to see if we have the key using equals, then since
      // key maps to key, we retrieve it to add the unit to the correct category
      if (categories.containsKey(entry)) {
        final UnitCategory stored = categories.get(entry);
        stored.addUnit(current);
      } else {
        categories.put(entry, entry);
      }
    }
    return sort ? new TreeSet<>(categories.keySet()) : new LinkedHashSet<>(categories.keySet());
  }

  /**
   * Legacy interface. Break the units into discrete categories. Do this based on unit owner, and
   * optionally dependent units and movement
   *
   * @param dependent - can be null
   * @param categorizeMovement - whether to categorize by movement
   * @return a Collection of UnitCategories
   */
  public static Set<UnitCategory> categorize(
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> dependent,
      final boolean categorizeMovement,
      final boolean categorizeTransportCost) {
    // sort by default
    return categorize(units, dependent, categorizeMovement, categorizeTransportCost, true);
  }
}
