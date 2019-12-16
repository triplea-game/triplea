package games.strategy.triplea.util;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.AbstractMoveDelegate.MoveType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.UnitComparator;
import games.strategy.triplea.delegate.data.MoveValidationResult;
import games.strategy.triplea.delegate.data.MustMoveWithDetails;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;

/**
 * Utility class for filtering a unit list to the subset of those units that can move on a route.
 */
public final class MovableUnitsFilter {
  /** The result of the filter operation. */
  @Getter
  public static class FilterOperationResult {
    /** A profile defines the sounds to use for various chat events. */
    public enum Status {
      ALL_UNITS_CAN_MOVE,
      SOME_UNITS_CAN_MOVE,
      NO_UNITS_CAN_MOVE
    }

    // The filtered units and dependents that can move on the route.
    private final List<Unit> unitsWithDependents;
    // The status of the move.
    private final Status status;
    // A warning or error message, if status is not ALL_UNITS_CAN_MOVE.
    private Optional<String> warningOrErrorMessage;

    public FilterOperationResult(
        final MoveValidationResult allUnitsResult,
        final MoveValidationResultWithDependents lastResult) {
      this.unitsWithDependents = lastResult.getUnitsWithDependents();
      if (allUnitsResult.isMoveValid()) {
        this.status = Status.ALL_UNITS_CAN_MOVE;
      } else if (lastResult.getResult().isMoveValid()) {
        this.status = Status.SOME_UNITS_CAN_MOVE;
      } else {
        this.status = Status.NO_UNITS_CAN_MOVE;
      }
      if (this.status != Status.ALL_UNITS_CAN_MOVE) {
        String message = allUnitsResult.getError();
        if (message == null) {
          message = allUnitsResult.getDisallowedUnitWarning(0);
        }
        if (message == null) {
          message = allUnitsResult.getUnresolvedUnitWarning(0);
        }
        this.warningOrErrorMessage = Optional.ofNullable(message);
      }
    }
  }

  @AllArgsConstructor
  @Getter
  private static class MoveValidationResultWithDependents {
    private final MoveValidationResult result;
    private final List<Unit> unitsWithDependents;
  }

  private final PlayerId player;
  private final GameData data;
  private final Route route;
  private final boolean nonCombat;
  private final MoveType moveType;
  private final List<UndoableMove> undoableMoves;

  public MovableUnitsFilter(
      final PlayerId player,
      final Route route,
      final boolean nonCombat,
      final MoveType moveType,
      final List<UndoableMove> undoableMoves) {
    this.player = Preconditions.checkNotNull(player);
    this.data = player.getData();
    this.route = Preconditions.checkNotNull(route);
    this.nonCombat = nonCombat;
    this.moveType = moveType;
    this.undoableMoves = Preconditions.checkNotNull(undoableMoves);
  }

  /**
   * Filters the units and their dependents to a subset of units that can move on the route.
   *
   * @param units The units to filter.
   * @param dependentUnits The dependent units map.
   * @return The result.
   */
  public FilterOperationResult filterUnitsThatCanMove(
      final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependentUnits) {
    final Collection<Unit> transportsToLoad =
        getPossibleTransportsToLoad(units, dependentUnits, route);

    List<Unit> best = getInitialUnitList(units);
    MoveValidationResultWithDependents lastResult =
        validateMoveWithDependents(best, dependentUnits, transportsToLoad);
    final MoveValidationResult allUnitsResult = lastResult.getResult();

    if (!allUnitsResult.isMoveValid()) {
      // if the player is invading only consider units that can invade
      if (!nonCombat
          && route.isUnload()
          && Matches.isTerritoryEnemy(player, data).test(route.getEnd())) {
        best = CollectionUtils.getMatches(best, Matches.unitCanInvade());
        lastResult = validateMoveWithDependents(best, dependentUnits, transportsToLoad);
      }
      final boolean hasLandTransports =
          TechAttachment.isMechanizedInfantry(player)
              && best.stream().anyMatch(Matches.unitIsLandTransport());
      final Predicate<Unit> isLandTransportable = Matches.unitIsLandTransportable();

      while (!best.isEmpty() && !lastResult.getResult().isMoveValid()) {
        final Unit firstSkipUnit = best.get(0);
        int startIndex = 1;
		// Check if we can skip more than unit if they are equivalent (e.g. all units of
		// the same type that have equivalent movement left). Don't do this if there are
		// land transports (mech infantry), though.
        while (startIndex < best.size()
            && (!hasLandTransports || !isLandTransportable.test(best.get(startIndex)))
            && unitsAreEquivalentWithSameMovementLeft(firstSkipUnit, best.get(startIndex))) {
          startIndex++;
        }
        best = best.subList(startIndex, best.size());
        lastResult = validateMoveWithDependents(best, dependentUnits, transportsToLoad);
      }
    }

    return new FilterOperationResult(allUnitsResult, lastResult);
  }

  // Whether the two units are equivalent for the purposes of movement.
  private boolean unitsAreEquivalentWithSameMovementLeft(final Unit u1, final Unit u2) {
    final BigDecimal left1 = TripleAUnit.get(u1).getMovementLeft();
    final BigDecimal left2 = TripleAUnit.get(u2).getMovementLeft();
    return u1.isEquivalent(u2) && left1.equals(left2);
  }

  private Collection<Unit> getPossibleTransportsToLoad(
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> dependentUnits,
      final Route route) {
    // TODO kev check for already loaded airTransports
    if (MoveValidator.isLoad(units, dependentUnits, route, player)) {
      final UnitCollection unitsAtEnd = route.getEnd().getUnitCollection();
      return unitsAtEnd.getMatches(Matches.unitIsTransport().and(Matches.alliedUnit(player, data)));
    }
    return List.of();
  }

  private List<Unit> getInitialUnitList(final Collection<Unit> units) {
    List<Unit> best = new ArrayList<>(units);
    // if the player selects a land unit and other units then
    // only consider the non land units
    if (route.getStart().isWater() && route.getEnd().isWater() && !route.isLoad()) {
      best = CollectionUtils.getMatches(best, Matches.unitIsLand().negate());
    }
    if (route.isUnload()) {
      best = CollectionUtils.getMatches(best, Matches.unitIsNotSea());
    }
    if (!best.isEmpty()) {
      best.sort(getUnitComparator(best).reversed());
    }
    return best;
  }

  private Comparator<Unit> getUnitComparator(final List<Unit> units) {
    // sort units based on which transports are allowed to unload
    if (route.isUnload() && units.stream().anyMatch(Matches.unitIsLand())) {
      return UnitComparator.getUnloadableUnitsComparator(units, route, player);
    } else {
      return UnitComparator.getMovableUnitsComparator(units, route);
    }
  }

  private MoveValidationResultWithDependents validateMoveWithDependents(
      final List<Unit> units,
      final Map<Unit, Collection<Unit>> dependentUnits,
      final Collection<Unit> transportsToLoad) {
    final List<Unit> unitsWithDependents = addMustMoveWith(units, dependentUnits);
    final MoveValidationResult result;
    data.acquireReadLock();
    try {
      final Map<Unit, Unit> unitsToTransports =
          transportsToLoad.isEmpty()
              ? Map.of()
              : TransportUtils.mapTransports(route, units, transportsToLoad);
      final MoveDescription move =
          new MoveDescription(unitsWithDependents, route, unitsToTransports, dependentUnits);
      result =
          AbstractMoveDelegate.validateMove(moveType, move, player, nonCombat, undoableMoves, data);
    } finally {
      data.releaseReadLock();
    }

    return new MoveValidationResultWithDependents(
        result, result.isMoveValid() ? unitsWithDependents : List.of());
  }

  private List<Unit> addMustMoveWith(
      final List<Unit> best, final Map<Unit, Collection<Unit>> dependentUnits) {
    final MustMoveWithDetails mustMoveWithDetails =
        MoveValidator.getMustMoveWith(route.getStart(), dependentUnits, player);
    final List<Unit> bestWithDependents = new ArrayList<>(best);
    for (final Unit u : best) {
      final Collection<Unit> mustMoveWith = mustMoveWithDetails.getMustMoveWithForUnit(u);
      for (final Unit m : mustMoveWith) {
        if (!bestWithDependents.contains(m)) {
          bestWithDependents.addAll(mustMoveWith);
        }
      }
    }
    return bestWithDependents;
  }
}
