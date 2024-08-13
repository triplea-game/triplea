package games.strategy.triplea.delegate;

import com.google.common.collect.Sets;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.util.TransportUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.CollectionUtils;

/**
 * Tracks which transports are carrying which units. Also tracks the capacity that has been
 * unloaded. To reset the unloaded call clearUnloadedCapacity().
 */
@UtilityClass
public class TransportTracker {
  private static void assertTransport(final Unit u) {
    if (u.getUnitAttachment().getTransportCapacity() == -1) {
      throw new IllegalStateException("Not a transport:" + u);
    }
  }

  /**
   * @return Unmodifiable map of transport -> collection of transported units.
   */
  public static Map<Unit, Collection<Unit>> transporting(final Collection<Unit> units) {
    return transporting(units, Unit::getTransporting);
  }

  private static Map<Unit, Collection<Unit>> transporting(
      final Collection<Unit> units,
      final Function<Unit, Collection<Unit>> getUnitsTransportedByTransport) {
    final Map<Unit, Collection<Unit>> returnVal = new HashMap<>();
    for (final Unit transported : units) {
      final Unit transport = transported.getTransportedBy();
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
   * {@link #transporting(Collection)} except that it considers only items in {@code units} as the
   * possible units to transport
   */
  public static Map<Unit, Collection<Unit>> transportingWithAllPossibleUnits(
      final Collection<Unit> units) {
    return transporting(units, transport -> transport.getTransporting(units));
  }

  /**
   * Returns a map of transport -> collection of transported units. This method is identical to
   * {@link #transporting(Collection)} except that it considers only units in {@code territory} as
   * the possible units to transport
   */
  public static Map<Unit, Collection<Unit>> transportingInTerritory(
      final Collection<Unit> units, final Territory territory) {
    return transporting(units, transport -> transport.getTransporting(territory));
  }

  public static Collection<Unit> transportingAndUnloaded(final Unit transport) {
    final Collection<Unit> units = new ArrayList<>(transport.getTransporting());
    units.addAll(transport.getUnloaded());
    return units;
  }

  static Change unloadTransportChange(
      final Unit unit, final Territory territory, final boolean dependentBattle) {
    final CompositeChange change = new CompositeChange();
    final Unit transport = unit.getTransportedBy();
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
    change.add(ChangeFactory.unitPropertyChange(unit, territory, Unit.UNLOADED_TO));
    if (!GameStepPropertiesHelper.isNonCombatMove(unit.getData(), true)) {
      change.add(ChangeFactory.unitPropertyChange(unit, true, Unit.UNLOADED_IN_COMBAT_PHASE));
      change.add(ChangeFactory.unitPropertyChange(unit, true, Unit.UNLOADED_AMPHIBIOUS));
      change.add(ChangeFactory.unitPropertyChange(transport, true, Unit.UNLOADED_IN_COMBAT_PHASE));
      change.add(ChangeFactory.unitPropertyChange(transport, true, Unit.UNLOADED_AMPHIBIOUS));
    }
    if (!dependentBattle) {
      // TODO: this is causing issues with Scrambling. if the units were unloaded, then scrambling
      // creates a battle,
      // there is no longer any way to have the units removed if those transports die.
      change.add(ChangeFactory.unitPropertyChange(unit, null, Unit.TRANSPORTED_BY));
    }
    change.add(ChangeFactory.unitPropertyChange(transport, newUnloaded, Unit.UNLOADED));
    return change;
  }

  public static Change unloadAirTransportChange(
      final Unit unit, final Territory territory, final boolean dependentBattle) {
    final CompositeChange change = new CompositeChange();
    final Unit transport = unit.getTransportedBy();
    if (transport == null) {
      return change;
    }
    assertTransport(transport);
    change.add(ChangeFactory.unitPropertyChange(unit, territory, Unit.UNLOADED_TO));
    if (!GameStepPropertiesHelper.isNonCombatMove(unit.getData(), true)) {
      change.add(ChangeFactory.unitPropertyChange(unit, true, Unit.UNLOADED_IN_COMBAT_PHASE));
      change.add(ChangeFactory.unitPropertyChange(transport, true, Unit.UNLOADED_IN_COMBAT_PHASE));
    }
    if (!dependentBattle) {
      // TODO: this is causing issues with Scrambling. if the units were unloaded, then scrambling
      // creates a battle,
      // there is no longer any way to have the units removed if those transports die.
      change.add(ChangeFactory.unitPropertyChange(unit, null, Unit.TRANSPORTED_BY));
    }
    // dependencies for battle calc and casualty selection include unloaded. therefore even if we
    // have unloaded this
    // unit, it will die if air transport dies IF we have the unloaded flat set. so don't set it.
    // TODO: fix this bullshit by re-writing entire transportation engine
    // change.add(ChangeFactory.unitPropertyChange(transport, newUnloaded, TripleAUnit.UNLOADED));
    return change;
  }

  public static void reloadTransports(final Collection<Unit> units, final CompositeChange change) {
    final Collection<Unit> transports =
        CollectionUtils.getMatches(units, Matches.unitCanTransport());
    // Put units back on their transports
    for (final Unit transport : transports) {
      for (final Unit load : transport.getUnloaded()) {
        final Change loadChange = TransportTracker.loadTransportChange(transport, load);
        change.add(loadChange);
      }
    }
  }

  static Change loadTransportChange(final Unit transport, final Unit unit) {
    assertTransport(transport);
    final CompositeChange change = new CompositeChange();
    // clear the loaded by
    change.add(ChangeFactory.unitPropertyChange(unit, transport, Unit.TRANSPORTED_BY));
    final Collection<Unit> newCarrying = new ArrayList<>(transport.getTransporting());
    if (newCarrying.contains(unit)) {
      throw new IllegalStateException("Already carrying, transport:" + transport + " unt:" + unit);
    }
    newCarrying.add(unit);
    change.add(ChangeFactory.unitPropertyChange(unit, Boolean.TRUE, Unit.LOADED_THIS_TURN));
    change.add(ChangeFactory.unitPropertyChange(transport, true, Unit.LOADED_THIS_TURN));
    // If the transport was in combat, flag it as being loaded AFTER combat
    if (transport.getWasInCombat()) {
      change.add(ChangeFactory.unitPropertyChange(transport, true, Unit.LOADED_AFTER_COMBAT));
    }
    return change;
  }

  /** Given a unit, computes the transport capacity value available for that unit. */
  public static int getAvailableCapacity(final Unit unit) {
    final UnitAttachment ua = unit.getUnitAttachment();
    // Check if there are transports available, also check for destroyer capacity (Tokyo Express)
    if (ua.getTransportCapacity() == -1
        || (Properties.getPacificTheater(unit.getData().getProperties())
            && ua.isDestroyer()
            && !unit.getOwner().getName().equals(Constants.PLAYER_NAME_JAPANESE))) {
      return 0;
    }
    final int capacity = ua.getTransportCapacity();
    final int used = TransportUtils.getTransportCost(unit.getTransporting());
    final int unloaded = TransportUtils.getTransportCost(unit.getUnloaded());
    return capacity - used - unloaded;
  }

  public static Collection<Unit> getUnitsLoadedOnAlliedTransportsThisTurn(
      final Collection<Unit> units) {
    final Collection<Unit> loadedUnits = new ArrayList<>();
    for (final Unit unit : units) {
      // a unit loaded onto an allied transport cannot be unloaded in the same turn, so if we check
      // both wasLoadedThisTurn and the transport that transports us, we can tell if we were loaded
      // onto an allied transport if we are no longer being transported, then we must have been
      // transported on our own transport an allied transport if the owner of the transport is not
      // the owner of the unit
      if (unit.getWasLoadedThisTurn()
          && unit.getTransportedBy() != null
          && !unit.getTransportedBy().isOwnedBy(unit.getOwner())) {
        loadedUnits.add(unit);
      }
    }
    return loadedUnits;
  }

  /** Detects if a unit has unloaded units in a previous game phase. */
  public static boolean hasTransportUnloadedInPreviousPhase(final Unit transport) {
    return GameStepPropertiesHelper.isNonCombatMove(transport.getData(), true)
        && transport.getUnloaded().stream().anyMatch(Unit::getWasUnloadedInCombatPhase);
  }

  /**
   * In some versions, a transport can never unload into multiple territories in a given turn. In
   * WW2V1 a transport can unload to multiple territories in non-combat phase, provided they are
   * both adjacent to the sea zone.
   */
  public static boolean isTransportUnloadRestrictedToAnotherTerritory(
      final Unit transport, final Territory territory) {
    final Collection<Unit> unloaded = transport.getUnloaded();
    if (unloaded.isEmpty()) {
      return false;
    }
    // See if transport has unloaded anywhere yet
    final GameState data = transport.getData();
    for (final Unit unit : unloaded) {
      if (Properties.getWW2V2(data.getProperties())
          || Properties.getTransportUnloadRestricted(data.getProperties())) {
        // cannot unload to two different territories
        if (!unit.getUnloadedTo().equals(territory)) {
          return true;
        }
      } else {
        // cannot unload to two different territories in combat phase
        if (!GameStepPropertiesHelper.isNonCombatMove(transport.getData(), true)
            && !unit.getUnloadedTo().equals(territory)) {
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
  public static @Nullable Territory getTerritoryTransportHasUnloadedTo(final Unit transport) {
    final Collection<Unit> unloaded = transport.getUnloaded();
    if (unloaded.isEmpty()) {
      return null;
    }
    return CollectionUtils.getAny(unloaded).getUnloadedTo();
  }

  /** If a transport has been in combat, it cannot both load AND unload in NCM. */
  public static boolean isTransportUnloadRestrictedInNonCombat(final Unit transport) {
    return GameStepPropertiesHelper.isNonCombatMove(transport.getData(), true)
        && transport.getWasInCombat()
        && transport.getWasLoadedAfterCombat();
  }

  /** For ww2v3+ and LHTR, if a transport has been in combat then it can't load in NCM. */
  public static boolean isTransportLoadRestrictedAfterCombat(final Unit transport) {
    final GameData data = transport.getData();
    return (Properties.getWW2V3(data.getProperties())
            || Properties.getLhtrCarrierProductionRules(data.getProperties()))
        && GameStepPropertiesHelper.isNonCombatMove(data, true)
        && transport.getWasInCombat();
  }

  public static AlliedAirTransportChange markTransportedByForAlliedAirOnCarrier(
      final Collection<Unit> units, final GamePlayer player) {
    final CompositeChange change = new CompositeChange();
    final Collection<Unit> alliedAir = new ArrayList<>();
    MoveValidator.carrierMustMoveWith(units, units, player)
        .forEach(
            (carrier, dependencies) -> {
              final UnitAttachment ua = carrier.getUnitAttachment();
              if (ua.getCarrierCapacity() == -1) {
                return;
              }

              dependencies.stream()
                  .filter(Matches.unitIsAir())
                  .forEach(
                      airUnit -> {
                        change.add(
                            ChangeFactory.unitPropertyChange(
                                airUnit, carrier, Unit.TRANSPORTED_BY));
                        alliedAir.add(airUnit);
                      });
            });
    return new AlliedAirTransportChange(change, alliedAir);
  }

  @Value
  public static class AlliedAirTransportChange {
    CompositeChange change;
    Collection<Unit> alliedAir;
  }

  public static CompositeChange clearTransportedByForAlliedAirOnCarrier(
      final Collection<Unit> attackingUnits,
      final Territory battleSite,
      final GamePlayer attacker,
      final GameState data) {
    final CompositeChange change = new CompositeChange();
    // Clear the transported_by for successfully won battles where there was an allied air unit held
    // as cargo by an carrier unit
    final Collection<Unit> carriers =
        CollectionUtils.getMatches(attackingUnits, Matches.unitIsCarrier());
    if (!carriers.isEmpty() && !Properties.getAlliedAirIndependent(data.getProperties())) {
      final Predicate<Unit> alliedFighters =
          Matches.isUnitAllied(attacker)
              .and(Matches.unitIsOwnedBy(attacker).negate())
              .and(Matches.unitIsAir())
              .and(Matches.unitCanLandOnCarrier());
      final Collection<Unit> alliedAirInTerr =
          CollectionUtils.getMatches(
              Sets.union(
                  Sets.newHashSet(attackingUnits), Sets.newHashSet(battleSite.getUnitCollection())),
              alliedFighters);
      for (final Unit fighter : alliedAirInTerr) {
        if (fighter.getTransportedBy() != null) {
          final Unit carrierTransportingThisUnit = fighter.getTransportedBy();
          if (!Matches.unitHasWhenCombatDamagedEffect(
                  UnitAttachment.UNITS_MAY_NOT_LEAVE_ALLIED_CARRIER)
              .test(carrierTransportingThisUnit)) {
            change.add(ChangeFactory.unitPropertyChange(fighter, null, Unit.TRANSPORTED_BY));
          }
        }
      }
    }
    return change;
  }
}
