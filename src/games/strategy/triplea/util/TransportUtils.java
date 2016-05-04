package games.strategy.triplea.util;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class TransportUtils {

  /**
   * Returns a map of unit -> transport (null if no mapping can be done either because there is not sufficient transport
   * capacity or because a unit is not with its transport)
   */
  public static Map<Unit, Unit> mapTransports(final Route route, final Collection<Unit> units,
      final Collection<Unit> transportsToLoad) {
    if (route.isLoad()) {
      return mapTransportsToLoad(units, transportsToLoad);
    }
    if (route.isUnload()) {
      return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
    }
    return mapTransportsAlreadyLoaded(units, units);
  }

  /**
   * Returns a map of unit -> transport. Unit must already be loaded in the transport. If no units are loaded in the
   * transports then an empty Map will be returned.
   */
  private static Map<Unit, Unit> mapTransportsAlreadyLoaded(final Collection<Unit> units,
      final Collection<Unit> transports) {
    final Collection<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
    final Collection<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
    final Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
    final Iterator<Unit> land = canBeTransported.iterator();
    while (land.hasNext()) {
      final Unit currentTransported = land.next();
      final Unit transport = TransportTracker.transportedBy(currentTransported);

      // already being transported, make sure it is in transports
      if (transport == null) {
        continue;
      }
      if (!canTransport.contains(transport)) {
        continue;
      }
      mapping.put(currentTransported, transport);
    }
    return mapping;
  }

  /**
   * Returns a map of unit -> transport. Tries to find transports to load all units. If it can't succeed returns an
   * empty Map.
   */
  public static Map<Unit, Unit> mapTransportsToLoad(final Collection<Unit> units, final Collection<Unit> transports) {
    final List<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
    int transportIndex = 0;
    final Comparator<Unit> transportCostComparator = new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        final int cost1 = UnitAttachment.get((o1).getUnitType()).getTransportCost();
        final int cost2 = UnitAttachment.get((o2).getUnitType()).getTransportCost();
        return cost2 - cost1;
      }
    };

    // fill the units with the highest cost first which allows easy loading of 2 infantry and 2 tanks on 2 transports in
    // WW2V2 rules
    Collections.sort(canBeTransported, transportCostComparator);
    final List<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
    final Comparator<Unit> transportCapacityComparator = new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        final int capacityLeft1 = TransportTracker.getAvailableCapacity(o1);
        final int capacityLeft2 = TransportTracker.getAvailableCapacity(o1);
        if (capacityLeft1 != capacityLeft2) {
          return capacityLeft1 - capacityLeft2;
        }
        final int capacity1 = UnitAttachment.get((o1).getUnitType()).getTransportCapacity();
        final int capacity2 = UnitAttachment.get((o2).getUnitType()).getTransportCapacity();
        return capacity1 - capacity2;
      }
    };

    // fill transports with the lowest capacity first
    Collections.sort(canTransport, transportCapacityComparator);
    final Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
    final IntegerMap<Unit> addedLoad = new IntegerMap<Unit>();
    final Comparator<Unit> previouslyLoadedToLast = transportsThatPreviouslyUnloadedComeLast();
    for (final Unit land : canBeTransported) {
      final UnitAttachment landUA = UnitAttachment.get(land.getType());
      final int cost = landUA.getTransportCost();
      boolean loaded = false;

      // we want to try to distribute units evenly to all the transports
      // if the user has 2 infantry, and selects two transports to load
      // we should put 1 infantry in each transport.
      // the algorithm below does not guarantee even distribution in all cases
      // but it solves most of the cases
      final List<Unit> shiftedToEnd = Util.shiftElementsToEnd(canTransport, transportIndex);

      // review the following loop in light of bug ticket 2827064- previously unloaded trns perhaps shouldn't be
      // included.
      Collections.sort(shiftedToEnd, previouslyLoadedToLast);
      final Iterator<Unit> transportIter = shiftedToEnd.iterator();
      while (transportIter.hasNext() && !loaded) {
        transportIndex++;
        if (transportIndex >= canTransport.size()) {
          transportIndex = 0;
        }
        final Unit transport = transportIter.next();
        int capacity = TransportTracker.getAvailableCapacity(transport);
        capacity -= addedLoad.getInt(transport);
        if (capacity >= cost) {
          addedLoad.add(transport, cost);
          mapping.put(land, transport);
          loaded = true;
        }
      }
    }
    return mapping;
  }

  private static Comparator<Unit> transportsThatPreviouslyUnloadedComeLast() {
    return new Comparator<Unit>() {
      @Override
      public int compare(final Unit t1, final Unit t2) {
        if (t1 == t2 || t1.equals(t2)) {
          return 0;
        }
        final boolean t1previous = TransportTracker.hasTransportUnloadedInPreviousPhase(t1);
        final boolean t2previous = TransportTracker.hasTransportUnloadedInPreviousPhase(t2);
        if (t1previous == t2previous) {
          return 0;
        }
        if (t1previous == false) {
          return -1;
        }
        return 1;
      }
    };
  }

  public static List<Unit> findUnitsToLoadOnAirTransports(final Collection<Unit> units,
      final Collection<Unit> transports) {
    final Collection<Unit> airTransports = Match.getMatches(transports, Matches.UnitIsAirTransport);

    final Comparator<Unit> c = new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        final int cost1 = UnitAttachment.get((o1).getUnitType()).getTransportCost();
        final int cost2 = UnitAttachment.get((o2).getUnitType()).getTransportCost();
        // descending transportCost
        return cost2 - cost1;
      }
    };
    Collections.sort((List<Unit>) units, c);

    // Define the max of all units that could be loaded
    final List<Unit> totalLoad = new ArrayList<Unit>();

    // Get a list of the unit categories
    final Collection<UnitCategory> unitTypes = UnitSeperator.categorize(units, null, false, true);
    final Collection<UnitCategory> transportTypes = UnitSeperator.categorize(airTransports, null, false, false);
    for (final UnitCategory unitType : unitTypes) {
      final int transportCost = unitType.getTransportCost();
      for (final UnitCategory transportType : transportTypes) {
        final int transportCapacity = UnitAttachment.get(transportType.getType()).getTransportCapacity();
        if (transportCost > 0 && transportCapacity >= transportCost) {
          final int transportCount = Match.countMatches(airTransports, Matches.unitIsOfType(transportType.getType()));
          final int ttlTransportCapacity = transportCount * (int) Math.floor(transportCapacity / transportCost);
          totalLoad.addAll(Match.getNMatches(units, ttlTransportCapacity, Matches.unitIsOfType(unitType.getType())));
        }
      }
    }
    return totalLoad;
  }

  public static int getTransportCost(final Collection<Unit> units) {
    if (units == null) {
      return 0;
    }
    int cost = 0;
    final Iterator<Unit> iter = units.iterator();
    while (iter.hasNext()) {
      final Unit item = iter.next();
      cost += UnitAttachment.get(item.getType()).getTransportCost();
    }
    return cost;
  }

}
