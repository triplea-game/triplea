package games.strategy.triplea.ui.unit.scroller;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.UnitCategory;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.java.PredicateBuilder;

/** Contains logic operations needed by the unit scroller. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class UnitScrollerModel {
  static List<Unit> getMoveableUnits(
      final Territory t,
      final UnitScroller.MovePhase movePhase,
      final GamePlayer player,
      final Collection<Unit> skippedUnits) {
    Preconditions.checkNotNull(t);

    return getPlayerUnitsInTerritory(t, movePhase, player).stream()
        .filter(Matches.unitHasMovementLeft())
        .filter(Predicate.not(skippedUnits::contains))
        .collect(Collectors.toList());
  }

  static List<Unit> getPlayerUnitsInTerritory(
      final Territory t, final UnitScroller.MovePhase movePhase, final GamePlayer player) {
    Preconditions.checkNotNull(t);

    final Predicate<Unit> movableInCurrentPhaseAndOwnedByMe =
        PredicateBuilder.of(Matches.unitIsOwnedBy(player))
            .andIf(
                movePhase == UnitScroller.MovePhase.COMBAT, Matches.unitCanMoveDuringCombatMove())
            .build();
    return t.getUnitCollection().getMatches(movableInCurrentPhaseAndOwnedByMe);
  }

  static int computeUnitsToMoveCount(
      final Collection<Territory> territories,
      final UnitScroller.MovePhase movePhase,
      final GamePlayer player,
      final Collection<Unit> skippedUnits) {
    return territories.stream()
        .map(t -> getMoveableUnits(t, movePhase, player, skippedUnits))
        .mapToInt(List::size)
        .sum();
  }

  static List<UnitCategory> getUniqueUnitCategories(
      final GamePlayer player, final List<Unit> units) {
    return units.stream()
        .map(unit -> new UnitCategory(unit.getType(), player))
        .distinct()
        .collect(Collectors.toList());
  }
}
