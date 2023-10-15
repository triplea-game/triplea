package games.strategy.triplea.delegate.move.validation;

import com.google.common.collect.Iterables;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Triple;
import org.triplea.util.Tuple;

public class UnitStackingLimitFilter {
  public static final String MOVEMENT_LIMIT = "movementLimit";
  public static final String ATTACKING_LIMIT = "attackingLimit";
  public static final String PLACEMENT_LIMIT = "placementLimit";

  /**
   * Returns the subset of units that are valid with respect to any stacking limits in effect. The
   * returned list is mutable and serializable.
   *
   * <p>Note: The passed list of units should have already been filtered for placement restrictions
   * as otherwise this could return a subset of units that cannot be placed for other reasons.
   */
  public static List<Unit> filterUnits(
      final Collection<Unit> units,
      final String limitType,
      final GamePlayer owner,
      final Territory t) {
    return filterUnits(units, limitType, owner, t, List.of());
  }

  /**
   * Same as above, but allows passing `existingUnitsToBePlaced` that have already been selected.
   */
  public static List<Unit> filterUnits(
      final Collection<Unit> units,
      final String limitType,
      final GamePlayer owner,
      final Territory t,
      final Collection<Unit> existingUnitsToBePlaced) {
    final PlayerAttachment pa = PlayerAttachment.get(owner);
    final Function<UnitAttachment, Tuple<Integer, String>> stackingLimitGetter;
    final Set<Triple<Integer, String, Set<UnitType>>> playerStackingLimits;
    switch (limitType) {
      case MOVEMENT_LIMIT:
        stackingLimitGetter = UnitAttachment::getMovementLimit;
        playerStackingLimits = (pa == null ? Set.of() : pa.getMovementLimit());
        break;
      case ATTACKING_LIMIT:
        stackingLimitGetter = UnitAttachment::getAttackingLimit;
        playerStackingLimits = (pa == null ? Set.of() : pa.getAttackingLimit());
        break;
      case PLACEMENT_LIMIT:
        stackingLimitGetter = UnitAttachment::getPlacementLimit;
        playerStackingLimits = (pa == null ? Set.of() : pa.getPlacementLimit());
        break;
      default:
        throw new IllegalArgumentException("Invalid limitType: " + limitType);
    }

    // Note: This must check each unit individually and track the ones that passed in order to
    // correctly handle stacking limits that apply to multiple unit types.
    final var unitsAllowedSoFar = new ArrayList<>(existingUnitsToBePlaced);
    for (final Unit unit : units) {
      UnitType ut = unit.getType();
      Tuple<Integer, String> stackingLimit = stackingLimitGetter.apply(ut.getUnitAttachment());
      int maxAllowed =
          getMaximumNumberOfThisUnitTypeToReachStackingLimit(
              ut, t, owner, stackingLimit, playerStackingLimits, unitsAllowedSoFar);
      if (maxAllowed > 0) {
        unitsAllowedSoFar.add(unit);
      }
    }
    // Remove the existing units from the list before returning it. Don't return a sublist as it's
    // not serializable.
    unitsAllowedSoFar.subList(0, existingUnitsToBePlaced.size()).clear();
    return unitsAllowedSoFar;
  }

  /**
   * Returns the maximum number of units of the specified type that can be placed in the specified
   * territory according to the specified stacking limit (movement, attack, or placement).
   *
   * @return {@link Integer#MAX_VALUE} if there is no stacking limit for the specified conditions.
   */
  private static int getMaximumNumberOfThisUnitTypeToReachStackingLimit(
      final UnitType ut,
      final Territory t,
      final GamePlayer owner,
      final Tuple<Integer, String> stackingLimit,
      final Set<Triple<Integer, String, Set<UnitType>>> playerStackingLimits,
      final Collection<Unit> pendingUnits) {
    int max = Integer.MAX_VALUE;
    final UnitAttachment ua = ut.getUnitAttachment();
    // Concat the territory units with the pending units without copying.
    final var existingUnits = Iterables.concat(t.getUnits(), pendingUnits);
    // Apply stacking limits coming from the PlayerAttachment.
    for (final Triple<Integer, String, Set<UnitType>> limit : playerStackingLimits) {
      final var unitTypes = limit.getThird();
      if (!unitTypes.contains(ut)) {
        continue;
      }
      final String stackingType = limit.getSecond();
      Predicate<Unit> stackingMatch = Matches.unitIsOfTypes(unitTypes);
      if (stackingType.equals("owned")) {
        stackingMatch = stackingMatch.and(Matches.unitIsOwnedBy(owner));
      } else if (stackingType.equals("allied")) {
        stackingMatch = stackingMatch.and(Matches.alliedUnit(owner));
      }
      final int totalInTerritory = CollectionUtils.countMatches(existingUnits, stackingMatch);
      final Integer limitMax = limit.getFirst();
      max = Math.min(max, limitMax - totalInTerritory);
    }

    if (stackingLimit == null) {
      return max;
    }
    max = Math.min(max, ua.getStackingLimitMax(stackingLimit));
    final Predicate<Unit> stackingMatch;
    final String stackingType = stackingLimit.getSecond();
    switch (stackingType) {
      case "owned":
        stackingMatch = Matches.unitIsOfType(ut).and(Matches.unitIsOwnedBy(owner));
        break;
      case "allied":
        stackingMatch = Matches.unitIsOfType(ut).and(Matches.isUnitAllied(owner));
        break;
      default:
        stackingMatch = Matches.unitIsOfType(ut);
        break;
    }
    final int totalInTerritory = CollectionUtils.countMatches(existingUnits, stackingMatch);
    return Math.max(0, max - totalInTerritory);
  }
}
