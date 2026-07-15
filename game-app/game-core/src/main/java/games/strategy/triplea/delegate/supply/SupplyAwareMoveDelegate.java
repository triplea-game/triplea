package games.strategy.triplea.delegate.supply;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.List;
import java.util.Optional;

/** Move delegate that rejects movement by land units whose road supply is cut. */
public final class SupplyAwareMoveDelegate extends MoveDelegate {
  public static final String OUT_OF_SUPPLY_UNITS_CANNOT_MOVE =
      "Out-of-supply land units cannot move";

  @Override
  public Optional<String> performMove(final MoveDescription move) {
    final GameData data = getData();
    if (SupplyNetworkResolver.isEnabled(data)
        && !EditDelegate.getEditMode(data.getProperties())
        && !move.getRoute().hasNoSteps()) {
      final GamePlayer movingPlayer = getUnitsOwner(move.getUnits());
      final List<Unit> outOfSupply =
          SupplyNetworkResolver.getOutOfSupplyUnits(
              move.getUnits(), move.getRoute().getStart(), movingPlayer, data);
      if (!outOfSupply.isEmpty()) {
        return Optional.of(
            OUT_OF_SUPPLY_UNITS_CANNOT_MOVE + ": " + MyFormatter.unitsToTextNoOwner(outOfSupply));
      }
    }
    return super.performMove(move);
  }
}
