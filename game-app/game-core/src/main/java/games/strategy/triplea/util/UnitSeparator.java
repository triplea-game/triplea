package games.strategy.triplea.util;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.mapdata.MapData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import lombok.Builder;

/** Separates a group of units into distinct categories. */
public class UnitSeparator {
  private UnitSeparator() {}

  @Builder(toBuilder = true)
  public static class SeparatorCategories {
    /**
     * if not null, then will group units with the same dependents (compares type, owner, and
     * amount)
     */
    @Builder.Default @Nullable final Map<Unit, Collection<Unit>> dependents = null;

    /** whether to categorize by movement */
    @Builder.Default final boolean movement = false;

    /** whether to categorize by movement for air units only */
    @Builder.Default final boolean movementForAirUnitsOnly = false;

    /** whether to categorize by transport cost */
    @Builder.Default final boolean transportCost = false;

    /** whether to categorize transports by movement */
    @Builder.Default final boolean transportMovement = false;

    /** whether to categorize by whether the unit can retreat or not */
    @Builder.Default final boolean retreatPossibility = false;
  }

  /**
   * Finds unit categories, removes not displayed, and then sorts them into logical order to display
   * based on: 1. Unit owner: territory owner, not at war with territory owner, player order in XML
   * 2. Unit type: 0 movement, can't combat move, sea, air if sea territory, land, air if land
   * territory 3. Within each of those groups sort the units by XML order in UnitList
   */
  public static List<UnitCategory> getSortedUnitCategories(
      final Territory t, final MapData mapData) {
    final GameState data = t.getData();
    final List<UnitCategory> categories = new ArrayList<>(UnitSeparator.categorize(t.getUnits()));
    categories.removeIf(uc -> !mapData.shouldDrawUnit(uc.getType().getName()));
    final List<UnitType> xmlUnitTypes = new ArrayList<>(data.getUnitTypeList().getAllUnitTypes());
    categories.sort(
        Comparator.comparing(
                UnitCategory::getOwner,
                Comparator.comparing((final GamePlayer p) -> !p.equals(t.getOwner()))
                    .thenComparing(p -> Matches.isAtWar(p).test(t.getOwner()))
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
    return categorize(units, SeparatorCategories.builder().build());
  }

  /**
   * Break the units into discrete categories. Do this based on unit owner and optionally other
   * categories
   *
   * @return a Collection of UnitCategories
   */
  public static Set<UnitCategory> categorize(
      final Collection<Unit> units, final SeparatorCategories separatorCategories) {
    // somewhat odd, but we map UnitCategory->UnitCategory, key and value are the same
    // we do this to take advantage of .equals() on objects that are equal in a special way
    final Map<UnitCategory, UnitCategory> categories = new HashMap<>();
    for (final Unit current : units) {
      BigDecimal unitMovement = new BigDecimal(-1);
      if (separatorCategories.movement
          || (separatorCategories.transportMovement && Matches.unitIsSeaTransport().test(current))
          || (separatorCategories.movementForAirUnitsOnly
              && isAirWithHitPointsRemaining(current))) {
        unitMovement = current.getMovementLeft();
      }
      int unitTransportCost = -1;
      if (separatorCategories.transportCost) {
        unitTransportCost = current.getUnitAttachment().getTransportCost();
      }
      Collection<Unit> currentDependents = null;
      if (separatorCategories.dependents != null) {
        currentDependents = separatorCategories.dependents.get(current);
      }
      boolean canRetreat = true;
      if (separatorCategories.retreatPossibility) {
        // only time a unit can't retreat is if the unit was amphibious
        canRetreat = !current.getWasAmphibious();
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
              unitTransportCost,
              canRetreat);
      // we test to see if we have the key using equals, then since
      // key maps to key, we retrieve it to add the unit to the correct category
      final UnitCategory stored = categories.get(entry);
      if (stored != null) {
        stored.addUnit(current);
      } else {
        categories.put(entry, entry);
      }
    }
    return new TreeSet<>(categories.keySet());
  }

  private static boolean isAirWithHitPointsRemaining(final Unit unit) {
    return unit.getUnitAttachment().getIsAir() && unit.getUnitAttachment().getHitPoints() > 1;
  }
}
