package games.strategy.triplea.delegate;

import com.google.common.collect.Sets;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.util.TransportUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;

/**
 * Tracks which transports are carrying which units. Also tracks the capacity that has been
 * unloaded. To reset the unloaded call clearUnloadedCapacity().
 */
public class TransportTracker {
  private TransportTracker() {}

  private static int getCost(final Collection<Unit> units) {
    return TransportUtils.getTransportCost(units);
  }

  private static void assertTransport(final Unit u) {
    if (UnitAttachment.get(u.getType()).getTransportCapacity() == -1) {
      throw new IllegalStateException("Not a transport:" + u);
    }
  }

  /** @return Unmodifiable collection of units that the given transport is transporting. */
  public static List<Unit> transporting(final Unit transport) {
    return ((TripleAUnit) transport).getTransporting();
  }

  /** @return Unmodifiable collection of units that the given transport is transporting. */
  public static List<Unit> transporting(
      final Unit transport, final Collection<Unit> transportedUnitsPossible) {
    return ((TripleAUnit) transport).getTransporting(transportedUnitsPossible);
  }

  /** @return Unmodifiable map of transport -> collection of transported units. */
  public static Map<Unit, Collection<Unit>> transporting(final Collection<Unit> units) {
    return transporting(units, TransportTracker::transporting);
  }

  private static Map<Unit, Collection<Unit>> transporting(
      final Collection<Unit> units,
      final Function<Unit, Collection<Unit>> getUnitsTransportedByTransport) {
    final Map<Unit, Collection<Unit>> returnVal = new HashMap<>();
    for (final Unit transported : units) {
      final Unit transport = transportedBy(transported);
      Collection<Unit> transporting = null;
      if (transport != null) {
        transporting = getUnitsTransportedByTransport.apply(transport);
      }
      if (transporting != null) {
        returnVal.put(transport, transporting);
      }
    }
    return Collections.unmodifiableMap(returnVal);
  }

  /**
   * Returns a map of transport -> collection of transported units. This method is identical to
   * {@link #transporting(Collection)} except that it considers all elements in {@code units} as the
   * possible units to transport (see {@link #transporting(Unit, Collection)}).
   */
  static Map<Unit, Collection<Unit>> transportingWithAllPossibleUnits(
      final Collection<Unit> units) {
    return transporting(units, transport -> transporting(transport, units));
  }

  public static boolean isTransporting(final Unit transport) {
    return !transporting(transport).isEmpty();
  }

  /**
   * Returns the collection of units that the given transport has unloaded this turn. Could be
   * empty.
   */
  public static Collection<Unit> unloaded(final Unit transport) {
    return ((TripleAUnit) transport).getUnloaded();
  }

  public static Collection<Unit> transportingAndUnloaded(final Unit transport) {
    final Collection<Unit> units = new ArrayList<>(transporting(transport));
    units.addAll(unloaded(transport));
    return units;
  }

  /** Return the transport that holds the given unit. Could be null. */
  public static Unit transportedBy(final Unit unit) {
    return ((TripleAUnit) unit).getTransportedBy();
  }

  static Change unloadTransportChange(
      final TripleAUnit unit, final Territory territory, final boolean dependentBattle) {
    final CompositeChange change = new CompositeChange();
    final TripleAUnit transport = (TripleAUnit) transportedBy(unit);
    if (transport == null) {
      return change;
    }
    assertTransport(transport);
    if (!transport.getTransporting().contains(unit)) {
      throw new IllegalStateException(
          "Not being carried, unit:" + unit + " transport:" + transport);
    }
    final List<Unit> newUnloaded = new ArrayList<>(transport.getUnloaded());
    newUnloaded.add(unit);
    change.add(ChangeFactory.unitPropertyChange(unit, territory, TripleAUnit.UNLOADED_TO));
    if (!GameStepPropertiesHelper.isNonCombatMove(unit.getData(), true)) {
      change.add(
          ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
      change.add(ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.UNLOADED_AMPHIBIOUS));
      change.add(
          ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
      change.add(
          ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.UNLOADED_AMPHIBIOUS));
    }
    if (!dependentBattle) {
      // TODO: this is causing issues with Scrambling. if the units were unloaded, then scrambling
      // creates a battle,
      // there is no longer any way to have the units removed if those transports die.
      change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
    }
    change.add(ChangeFactory.unitPropertyChange(transport, newUnloaded, TripleAUnit.UNLOADED));
    return change;
  }

  static Change unloadAirTransportChange(
      final TripleAUnit unit, final Territory territory, final boolean dependentBattle) {
    final CompositeChange change = new CompositeChange();
    final TripleAUnit transport = (TripleAUnit) transportedBy(unit);
    if (transport == null) {
      return change;
    }
    assertTransport(transport);
    if (!transport.getTransporting().contains(unit)) {
      throw new IllegalStateException(
          "Not being carried, unit:" + unit + " transport:" + transport);
    }
    change.add(ChangeFactory.unitPropertyChange(unit, territory, TripleAUnit.UNLOADED_TO));
    if (!GameStepPropertiesHelper.isNonCombatMove(unit.getData(), true)) {
      change.add(
          ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
      // change.add(ChangeFactory.unitPropertyChange(unit, true, TripleAUnit.UNLOADED_AMPHIBIOUS));
      change.add(
          ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
      // change.add(ChangeFactory.unitPropertyChange(transport, true,
      // TripleAUnit.UNLOADED_AMPHIBIOUS));
    }
    if (!dependentBattle) {
      // TODO: this is causing issues with Scrambling. if the units were unloaded, then scrambling
      // creates a battle,
      // there is no longer any way to have the units removed if those transports die.
      change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
    }
    // dependencies for battle calc and casualty selection include unloaded. therefore even if we
    // have unloaded this
    // unit, it will die if air transport dies IF we have the unloaded flat set. so don't set it.
    // TODO: fix this bullshit by re-writing entire transportation engine
    // change.add(ChangeFactory.unitPropertyChange(transport, newUnloaded, TripleAUnit.UNLOADED));
    return change;
  }

  static void reloadTransports(final Collection<Unit> units, final CompositeChange change) {
    final Collection<Unit> transports =
        CollectionUtils.getMatches(units, Matches.unitCanTransport());
    // Put units back on their transports
    for (final Unit transport : transports) {
      final Collection<Unit> unloaded = TransportTracker.unloaded(transport);
      for (final Unit load : unloaded) {
        final Change loadChange =
            TransportTracker.loadTransportChange((TripleAUnit) transport, load);
        change.add(loadChange);
      }
    }
  }

  static Change loadTransportChange(final TripleAUnit transport, final Unit unit) {
    assertTransport(transport);
    final CompositeChange change = new CompositeChange();
    // clear the loaded by
    change.add(ChangeFactory.unitPropertyChange(unit, transport, TripleAUnit.TRANSPORTED_BY));
    final Collection<Unit> newCarrying = new ArrayList<>(transport.getTransporting());
    if (newCarrying.contains(unit)) {
      throw new IllegalStateException("Already carrying, transport:" + transport + " unt:" + unit);
    }
    newCarrying.add(unit);
    change.add(ChangeFactory.unitPropertyChange(unit, Boolean.TRUE, TripleAUnit.LOADED_THIS_TURN));
    change.add(ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.LOADED_THIS_TURN));
    // If the transport was in combat, flag it as being loaded AFTER combat
    if (transport.getWasInCombat()) {
      change.add(
          ChangeFactory.unitPropertyChange(transport, true, TripleAUnit.LOADED_AFTER_COMBAT));
    }
    return change;
  }

  /** Given a unit, computes the transport capacity value available for that unit. */
  public static int getAvailableCapacity(final Unit unit) {
    final UnitAttachment ua = UnitAttachment.get(unit.getType());
    // Check if there are transports available, also check for destroyer capacity (Tokyo Express)
    if (ua.getTransportCapacity() == -1
        || (unit.getData().getProperties().get(Constants.PACIFIC_THEATER, false)
            && ua.getIsDestroyer()
            && !unit.getOwner().getName().equals(Constants.PLAYER_NAME_JAPANESE))) {
      return 0;
    }
    final int capacity = ua.getTransportCapacity();
    final int used = getCost(transporting(unit));
    final int unloaded = getCost(unloaded(unit));
    return capacity - used - unloaded;
  }

  static Collection<Unit> getUnitsLoadedOnAlliedTransportsThisTurn(final Collection<Unit> units) {
    final Collection<Unit> loadedUnits = new ArrayList<>();
    for (final Unit u : units) {
      // a unit loaded onto an allied transport cannot be unloaded in the same turn, so if we check
      // both
      // wasLoadedThisTurn and the transport that transports us, we can tell if we were loaded onto
      // an allied transport
      // if we are no longer being transported, then we must have been transported on our own
      // transport
      final TripleAUnit taUnit = (TripleAUnit) u;
      // an allied transport if the owner of the transport is not the owner of the unit
      if (taUnit.getWasLoadedThisTurn()
          && taUnit.getTransportedBy() != null
          && !taUnit.getTransportedBy().getOwner().equals(taUnit.getOwner())) {
        loadedUnits.add(u);
      }
    }
    return loadedUnits;
  }

  /** Detects if a unit has unloaded units in a previous game phase. */
  public static boolean hasTransportUnloadedInPreviousPhase(final Unit transport) {
    final Collection<Unit> unloaded = ((TripleAUnit) transport).getUnloaded();
    // See if transport has unloaded anywhere yet
    for (final Unit u : unloaded) {
      final TripleAUnit taUnit = (TripleAUnit) u;
      // cannot unload in two different phases
      if (GameStepPropertiesHelper.isNonCombatMove(transport.getData(), true)
          && taUnit.getWasUnloadedInCombatPhase()) {
        return true;
      }
    }
    return false;
  }

  private static boolean isWW2V2(final GameData data) {
    return Properties.getWW2V2(data);
  }

  private static boolean isTransportUnloadRestricted(final GameData data) {
    return Properties.getTransportUnloadRestricted(data);
  }

  /**
   * In some versions, a transport can never unload into multiple territories in a given turn. In
   * WW2V1 a transport can unload to multiple territories in non-combat phase, provided they are
   * both adjacent to the sea zone.
   */
  static boolean isTransportUnloadRestrictedToAnotherTerritory(
      final Unit transport, final Territory territory) {
    final Collection<Unit> unloaded = ((TripleAUnit) transport).getUnloaded();
    if (unloaded.isEmpty()) {
      return false;
    }
    // See if transport has unloaded anywhere yet
    final GameData data = transport.getData();
    for (final Unit u : unloaded) {
      final TripleAUnit taUnit = (TripleAUnit) u;
      if (isWW2V2(data) || isTransportUnloadRestricted(data)) {
        // cannot unload to two different territories
        if (!taUnit.getUnloadedTo().equals(territory)) {
          return true;
        }
      } else {
        // cannot unload to two different territories in combat phase
        if (!GameStepPropertiesHelper.isNonCombatMove(transport.getData(), true)
            && !taUnit.getUnloadedTo().equals(territory)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * This method should be called after isTransportUnloadRestrictedToAnotherTerritory() returns
   * false, in order to populate the error message. However, we only need to call this method to
   * determine why we can't unload an additional unit. Since transports only hold up to two units,
   * we only need to return one territory, not multiple territories.
   */
  static Territory getTerritoryTransportHasUnloadedTo(final Unit transport) {
    final Collection<Unit> unloaded = ((TripleAUnit) transport).getUnloaded();
    if (unloaded.isEmpty()) {
      return null;
    }
    return ((TripleAUnit) unloaded.iterator().next()).getUnloadedTo();
  }

  /** If a transport has been in combat, it cannot both load AND unload in NCM. */
  static boolean isTransportUnloadRestrictedInNonCombat(final Unit transport) {
    final TripleAUnit taUnit = (TripleAUnit) transport;
    return GameStepPropertiesHelper.isNonCombatMove(transport.getData(), true)
        && taUnit.getWasInCombat()
        && taUnit.getWasLoadedAfterCombat();
  }

  /** For ww2v3+ and LHTR, if a transport has been in combat then it can't load in NCM. */
  static boolean isTransportLoadRestrictedAfterCombat(final Unit transport) {
    final TripleAUnit taUnit = (TripleAUnit) transport;
    final GameData data = transport.getData();
    return (Properties.getWW2V3(data) || Properties.getLhtrCarrierProductionRules(data))
        && GameStepPropertiesHelper.isNonCombatMove(data, true)
        && taUnit.getWasInCombat();
  }

  static CompositeChange clearTransportedByForAlliedAirOnCarrier(
      final Collection<Unit> attackingUnits,
      final Territory battleSite,
      final PlayerId attacker,
      final GameData data) {
    final CompositeChange change = new CompositeChange();
    // Clear the transported_by for successfully won battles where there was an allied air unit held
    // as cargo by an
    // carrier unit
    final Collection<Unit> carriers =
        CollectionUtils.getMatches(attackingUnits, Matches.unitIsCarrier());
    if (!carriers.isEmpty() && !Properties.getAlliedAirIndependent(data)) {
      final Predicate<Unit> alliedFighters =
          Matches.isUnitAllied(attacker, data)
              .and(Matches.unitIsOwnedBy(attacker).negate())
              .and(Matches.unitIsAir())
              .and(Matches.unitCanLandOnCarrier());
      final Collection<Unit> alliedAirInTerr =
          CollectionUtils.getMatches(
              Sets.union(
                  Sets.newHashSet(attackingUnits), Sets.newHashSet(battleSite.getUnitCollection())),
              alliedFighters);
      for (final Unit fighter : alliedAirInTerr) {
        final TripleAUnit taUnit = (TripleAUnit) fighter;
        if (taUnit.getTransportedBy() != null) {
          final Unit carrierTransportingThisUnit = taUnit.getTransportedBy();
          if (!Matches.unitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER)
              .test(carrierTransportingThisUnit)) {
            change.add(ChangeFactory.unitPropertyChange(fighter, null, TripleAUnit.TRANSPORTED_BY));
          }
        }
      }
    }
    return change;
  }
}
