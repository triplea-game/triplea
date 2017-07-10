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
import games.strategy.util.Match;

public class UnitComparator {
  static Comparator<Unit> getLowestToHighestMovementComparator() {
    return (u1, u2) -> {
      final int left1 = TripleAUnit.get(u1).getMovementLeft();
      final int left2 = TripleAUnit.get(u2).getMovementLeft();
      if (left1 == left2) {
        return 0;
      }
      if (left1 > left2) {
        return 1;
      }
      return -1;
    };
  }

  public static Comparator<Unit> getHighestToLowestMovementComparator() {
    return (u1, u2) -> {
      final int left1 = TripleAUnit.get(u1).getMovementLeft();
      final int left2 = TripleAUnit.get(u2).getMovementLeft();
      if (left1 == left2) {
        return 0;
      }
      if (left1 < left2) {
        return 1;
      }
      return -1;
    };
  }

  public static Comparator<Unit> getIncreasingCapacityComparator(final List<Unit> transports) {
    return getCapacityComparator(transports, true);
  }

  private static Comparator<Unit> getDecreasingCapacityComparator(final List<Unit> transports) {
    return getCapacityComparator(transports, false);
  }

  private static Comparator<Unit> getCapacityComparator(final List<Unit> transports, final boolean increasing) {
    // this makes it more efficient
    final IntegerMap<Unit> capacityMap = new IntegerMap<>(transports.size() + 1, 1);
    for (final Unit transport : transports) {
      final Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
      capacityMap.add(transport, TransportUtils.getTransportCost(transporting));
    }
    return (t1, t2) -> {
      final int cost1 = capacityMap.getInt(t1);
      final int cost2 = capacityMap.getInt(t2);
      if (increasing) {
        return cost1 - cost2;
      } else {
        return cost2 - cost1;
      }
    };
  }

  /**
   * Return a Comparator that will order the specified transports in preferred load order.
   */
  public static Comparator<Unit> getLoadableTransportsComparator(final List<Unit> transports, final Route route,
      final PlayerID player) {
    final Comparator<Unit> decreasingCapacityComparator = getDecreasingCapacityComparator(transports);
    final Match<Unit> incapableTransportMatch = Matches.transportCannotUnload(route.getEnd());
    return (u1, u2) -> {
      final TripleAUnit t1 = TripleAUnit.get(u1);
      final TripleAUnit t2 = TripleAUnit.get(u2);

      // Check if transport is incapable due to game state
      final boolean isIncapable1 = incapableTransportMatch.match(t1);
      final boolean isIncapable2 = incapableTransportMatch.match(t2);
      if (!isIncapable1 && isIncapable2) {
        return -1;
      }
      if (isIncapable1 && !isIncapable2) {
        return 1;
      }

      // Use allied transports as a last resort
      final boolean isAlliedTrn1 = !t1.getOwner().equals(player);
      final boolean isAlliedTrn2 = !t2.getOwner().equals(player);
      if (!isAlliedTrn1 && isAlliedTrn2) {
        return -1;
      }
      if (isAlliedTrn1 && !isAlliedTrn2) {
        return 1;
      }

      // Sort by decreasing transport capacity
      final int compareCapacity = decreasingCapacityComparator.compare(t1, t2);
      if (compareCapacity != 0) {
        return compareCapacity;
      }

      // Sort by decreasing movement
      final int left1 = t1.getMovementLeft();
      final int left2 = t1.getMovementLeft();
      if (left1 != left2) {
        return left2 - left1;
      }

      return Integer.compare(t1.hashCode(), t2.hashCode());
    };
  }

  /**
   * Return a Comparator that will order the specified transports in preferred unload order.
   */
  public static Comparator<Unit> getUnloadableTransportsComparator(final List<Unit> transports, final Route route,
      final PlayerID player, final boolean noTies) {
    final Comparator<Unit> decreasingCapacityComparator = getDecreasingCapacityComparator(transports);
    final Match<Unit> incapableTransportMatch = Matches.transportCannotUnload(route.getEnd());
    return (t1, t2) -> {

      // Check if transport is incapable due to game state
      final boolean isIncapable1 = incapableTransportMatch.match(t1);
      final boolean isIncapable2 = incapableTransportMatch.match(t2);
      if (!isIncapable1 && isIncapable2) {
        return -1;
      }
      if (isIncapable1 && !isIncapable2) {
        return 1;
      }

      // Prioritize allied transports
      final boolean isAlliedTrn1 = !t1.getOwner().equals(player);
      final boolean isAlliedTrn2 = !t2.getOwner().equals(player);
      if (isAlliedTrn1 && !isAlliedTrn2) {
        return -1;
      }
      if (!isAlliedTrn1 && isAlliedTrn2) {
        return 1;
      }

      // Sort by decreasing transport capacity
      final int compareCapacity = decreasingCapacityComparator.compare(t1, t2);
      if (compareCapacity != 0) {
        return compareCapacity;
      }

      // Sort by increasing movement
      final int left1 = TripleAUnit.get(t1).getMovementLeft();
      final int left2 = TripleAUnit.get(t2).getMovementLeft();
      if (left1 != left2) {
        return left1 - left2;
      }

      // If noTies is set, sort by hashcode so that result is deterministic
      if (noTies) {
        return Integer.compare(t1.hashCode(), t2.hashCode());
      } else {
        return 0;
      }
    };
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
        if (left1 >= route.getMovementCost(u1) && left2 < route.getMovementCost(u2)) {
          return -1;
        }
        if (left1 < route.getMovementCost(u1) && left2 >= route.getMovementCost(u2)) {
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
        if (route != null && route.isLoad()) {
          return left2 - left1;
        } else {
          return left1 - left2;
        }
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
    // compare transports
    final Comparator<Unit> unloadableTransportsComparator =
        getUnloadableTransportsComparator(units, route, player, false);
    // if noTies is set, sort by hashcode so that result is deterministic
    final Comparator<Unit> movableUnitsComparator = getMovableUnitsComparator(units, route);
    return (u1, u2) -> {
      final Unit t1 = TripleAUnit.get(u1).getTransportedBy();
      final Unit t2 = TripleAUnit.get(u2).getTransportedBy();
      // check if unloadable units are in a transport
      if (t1 != null && t2 == null) {
        return -1;
      }
      if (t1 == null && t2 != null) {
        return 1;
      }
      if (t1 != null && t2 != null) {
        final int compareTransports = unloadableTransportsComparator.compare(t1, t2);
        if (compareTransports != 0) {
          return compareTransports;
        }
      }
      // we are sorting air units, or no difference found yet
      // if noTies is set, sort by hashcode so that result is deterministic
      return movableUnitsComparator.compare(u1, u2);
    };
  }

  static Comparator<Unit> getDecreasingAttackComparator(final PlayerID player) {
    return (u1, u2) -> {
      final UnitAttachment ua1 = UnitAttachment.get(u1.getType());
      final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
      final int attack1 = ua1.getAttack(player);
      final int attack2 = ua2.getAttack(player);
      if (attack1 == attack2) {
        return 0;
      }
      if (attack1 < attack2) {
        return 1;
      }
      return -1;
    };
  }
}
