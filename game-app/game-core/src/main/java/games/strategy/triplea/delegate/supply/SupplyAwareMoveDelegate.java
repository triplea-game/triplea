package games.strategy.triplea.delegate.supply;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.delegate.StackCapacityResolver;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Move delegate that enforces Small Front supply and terrain stack-capacity rules. */
public final class SupplyAwareMoveDelegate extends MoveDelegate {
  public static final String OUT_OF_SUPPLY_UNITS_CANNOT_MOVE =
      "Out-of-supply land units cannot move";
  public static final String STACK_CAPACITY_EXCEEDED = "Terrain stack capacity exceeded";

  @Override
  public Optional<String> performMove(final MoveDescription move) {
    final GameData data = getData();
    if (!EditDelegate.getEditMode(data.getProperties()) && !move.getRoute().hasNoSteps()) {
      final GamePlayer movingPlayer = getUnitsOwner(move.getUnits());
      if (SupplyNetworkResolver.isEnabled(data)) {
        final List<Unit> outOfSupply =
            SupplyNetworkResolver.getOutOfSupplyUnits(
                move.getUnits(), move.getRoute().getStart(), movingPlayer, data);
        if (!outOfSupply.isEmpty()) {
          return Optional.of(
              OUT_OF_SUPPLY_UNITS_CANNOT_MOVE + ": " + MyFormatter.unitsToTextNoOwner(outOfSupply));
        }
      }
      final Optional<String> capacityError = validateStackCapacity(move, movingPlayer);
      if (capacityError.isPresent()) {
        return capacityError;
      }
    }
    return super.performMove(move);
  }

  /** Returns an error when the final destination cannot accept the complete moving force. */
  public static Optional<String> validateStackCapacity(
      final MoveDescription move, final GamePlayer movingPlayer) {
    if (move.getRoute().hasNoSteps()) {
      return Optional.empty();
    }
    final Territory destination = move.getRoute().getEnd();
    final List<Unit> allowed =
        StackCapacityResolver.filterUnitsToFit(
            move.getUnits(), movingPlayer, destination, List.of());
    if (allowed.size() == move.getUnits().size()) {
      return Optional.empty();
    }
    final List<Unit> blocked = new ArrayList<>(move.getUnits());
    blocked.removeAll(allowed);
    return Optional.of(
        STACK_CAPACITY_EXCEEDED
            + " in "
            + destination.getName()
            + ": "
            + MyFormatter.unitsToTextNoOwner(blocked));
  }
}
