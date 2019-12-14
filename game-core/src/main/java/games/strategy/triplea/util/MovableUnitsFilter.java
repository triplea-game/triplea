package games.strategy.triplea.util;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.AbstractMoveDelegate.MoveType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.UnitComparator;
import games.strategy.triplea.delegate.data.MoveValidationResult;
import games.strategy.triplea.delegate.data.MustMoveWithDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;

/**
 * Utility class for filtering a unit list to the subset of those units that can move on a route.
 */
public final class MovableUnitsFilter {
  /** The result of the filter operation. */
  @Getter
  public static class Result {
    // The filtered units and dependents that can move on the route.
    private final List<Unit> unitsWithDependents;
    // An error message, if no units could move on the route.
    @Nullable private String errorMessage;
    // A warning message, if some, but not all units could move on the route.
    @Nullable private String warningMessage;

    public Result(
        final MoveValidationResult allUnitsResult,
        final MoveValidationResultWithDependents lastResult) {
      this.unitsWithDependents = lastResult.getUnitsWithDependents();
      if (!allUnitsResult.isMoveValid()) {
        @Nullable String message = getErrorOrWarningMessage(allUnitsResult);
        if (!lastResult.getResult().isMoveValid()) {
          this.errorMessage = message;
        } else {
          this.warningMessage = message;
        }
      }
    }

    private static @Nullable String getErrorOrWarningMessage(final MoveValidationResult result) {
      @Nullable String message = result.getError();
      if (message == null) {
        message = result.getDisallowedUnitWarning(0);
      }
      if (message == null) {
        message = result.getUnresolvedUnitWarning(0);
      }
      return message;
    }
  }

  @AllArgsConstructor
  @Getter
  private static class MoveValidationResultWithDependents {
    private final MoveValidationResult result;
    private final List<Unit> unitsWithDependents;
  }

  private final GameData data;
  private final PlayerId player;
  private final Route route;
  private final boolean nonCombat;
  private final MoveType moveType;
  private final List<UndoableMove> undoableMoves;

  public MovableUnitsFilter(
      final GameData data,
      final PlayerId player,
      final Route route,
      final boolean nonCombat,
      final MoveType moveType,
      final List<UndoableMove> undoableMoves) {
    this.data = Preconditions.checkNotNull(data);
    this.player = Preconditions.checkNotNull(player);
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
  public Result filterUnitsThatCanMove(
      final Collection<Unit> units, final Map<Unit, Collection<Unit>> dependentUnits) {
    final Collection<Unit> transportsToLoad =
        getPossibleTransportsToLoad(units, dependentUnits, route);
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
      while (!best.isEmpty() && !lastResult.getResult().isMoveValid()) {
        best = best.subList(1, best.size());
        lastResult = validateMoveWithDependents(best, dependentUnits, transportsToLoad);
      }
    }

    return new Result(allUnitsResult, lastResult);
  }

  private Collection<Unit> getPossibleTransportsToLoad(
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> dependentUnits,
      final Route route) {
    // TODO kev check for already loaded airTransports
    if (MoveValidator.isLoad(units, dependentUnits, route, data, player)) {
      final UnitCollection unitsAtEnd = route.getEnd().getUnitCollection();
      return unitsAtEnd.getMatches(Matches.unitIsTransport().and(Matches.alliedUnit(player, data)));
    }
    return List.of();
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
        MoveValidator.getMustMoveWith(
            route.getStart(), route.getStart().getUnits(), dependentUnits, data, player);
    final List<Unit> bestWithDependents = new ArrayList<>(best);
    for (final Unit u : best) {
      final Collection<Unit> mustMoveWith = mustMoveWithDetails.getMustMoveWith().get(u);
      if (mustMoveWith != null) {
        for (final Unit m : mustMoveWith) {
          if (!bestWithDependents.contains(m)) {
            bestWithDependents.addAll(mustMoveWith);
          }
        }
      }
    }
    return bestWithDependents;
  }
}
