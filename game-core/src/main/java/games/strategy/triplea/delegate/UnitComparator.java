package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.util.IntegerMap;

public class UnitComparator {
  static Comparator<Unit> getLowestToHighestMovementComparator() {
    return Comparator.comparing(TripleAUnit::get,
        Comparator.comparingInt(TripleAUnit::getMovementLeft));
  }

  public static Comparator<Unit> getHighestToLowestMovementComparator() {
    return getLowestToHighestMovementComparator().reversed();
  }

  public static Comparator<Unit> getIncreasingCapacityComparator(final List<Unit> transports) {
    // this makes it more efficient
    final IntegerMap<Unit> capacityMap = new IntegerMap<>(transports.size() + 1, 1);
    for (final Unit transport : transports) {
      final Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
      capacityMap.add(transport, TransportUtils.getTransportCost(transporting));
    }
    return Comparator.comparingInt(capacityMap::getInt);
  }

  private static Comparator<Unit> getDecreasingCapacityComparator(final List<Unit> transports) {
    return getIncreasingCapacityComparator(transports).reversed();
  }

  /**
   * Return a Comparator that will order the specified transports in preferred load order.
   */
  public static Comparator<Unit> getLoadableTransportsComparator(final List<Unit> transports, final Route route,
      final PlayerID player) {
    return Comparator.comparing(Matches.transportCannotUnload(route.getEnd())::test)
        .thenComparing(Unit::getOwner, Comparator.comparing(player::equals).reversed())
        .thenComparing(getDecreasingCapacityComparator(transports))
        .thenComparing(TripleAUnit::get, Comparator.comparingInt(TripleAUnit::getMovementLeft).reversed())
        .thenComparingInt(Object::hashCode);
  }

  /**
   * Return a Comparator that will order the specified transports in preferred unload order.
   */
  public static Comparator<Unit> getUnloadableTransportsComparator(final List<Unit> transports, final Route route,
      final PlayerID player, final boolean noTies) {
    return Comparator.comparing(Matches.transportCannotUnload(route.getEnd())::test)
        .thenComparing(Unit::getOwner, Comparator.comparing(player::equals))
        .thenComparing(getDecreasingCapacityComparator(transports))
        .thenComparing(TripleAUnit::get, Comparator.comparingInt(TripleAUnit::getMovementLeft))
        .thenComparingInt(t -> noTies ? t.hashCode() : 0);
  }

  /**
   * Return a Comparator that will order the specified units in preferred move order.
   */
  public static Comparator<Unit> getMovableUnitsComparator(final List<Unit> units, final Route route) {
    final Comparator<Unit> decreasingCapacityComparator = getDecreasingCapacityComparator(units);
    return (u1, u2) -> {

      // Ensure units have enough movement
      final int left1 = TripleAUnit.get(u1).getMovementLeft();
      final int left2 = TripleAUnit.get(u2).getMovementLeft();
      if (route != null) {
        if ((left1 >= route.getMovementCost(u1)) && (left2 < route.getMovementCost(u2))) {
          return -1;
        }
        if ((left1 < route.getMovementCost(u1)) && (left2 >= route.getMovementCost(u2))) {
          return 1;
        }
      }

      // Prefer transports for which dependents are also selected
      final Collection<Unit> transporting1 = TripleAUnit.get(u1).getTransporting();
      final Collection<Unit> transporting2 = TripleAUnit.get(u2).getTransporting();
      final int hasDepends1 = units.containsAll(transporting1) ? 1 : 0;
      final int hasDepends2 = units.containsAll(transporting2) ? 1 : 0;
      if (hasDepends1 != hasDepends2) {
        return hasDepends1 - hasDepends2;
      }

      // Sort by decreasing transport capacity (only valid for transports)
      final int compareCapacity = decreasingCapacityComparator.compare(u1, u2);
      if (compareCapacity != 0) {
        return compareCapacity;
      }

      // Sort by increasing movement normally, but by decreasing movement during loading
      if (left1 != left2) {
        return ((route != null) && route.isLoad()) ? (left2 - left1) : (left1 - left2);
      }

      return Integer.compare(u1.hashCode(), u2.hashCode());
    };
  }

  /**
   * Return a Comparator that will order the specified units in preferred unload order.
   * If needed it may also inspect the transport holding the units.
   */
  public static Comparator<Unit> getUnloadableUnitsComparator(final List<Unit> units, final Route route,
      final PlayerID player) {
    return Comparator.comparing(TripleAUnit::get,
        Comparator.nullsLast(getUnloadableTransportsComparator(units, route, player, false)))
        .thenComparing(getMovableUnitsComparator(units, route));
  }

  static Comparator<Unit> getDecreasingAttackComparator(final PlayerID player) {
    return Comparator.comparing(Unit::getType,
        Comparator.comparing(UnitAttachment::get,
            Comparator.<UnitAttachment>comparingInt(u -> u.getAttack(player)).reversed()));
  }
}
