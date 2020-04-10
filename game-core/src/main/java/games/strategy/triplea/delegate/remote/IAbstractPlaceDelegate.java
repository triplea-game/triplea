package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.triplea.delegate.UndoablePlacement;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import java.util.Collection;

/** Logic for placing units within a territory. */
public interface IAbstractPlaceDelegate extends IAbstractMoveDelegate<UndoablePlacement> {
  /**
   * Places the specified units in the specified territory.
   *
   * @param units units to place.
   * @param at territory to place
   * @return an error code if the placement was not successful
   */
  @RemoteActionCode(13)
  String placeUnits(Collection<Unit> units, Territory at, BidMode bidMode);

  /** Convenience method for testing. Never called over the network. */
  default String placeUnits(final Collection<Unit> units, final Territory at) {
    return placeUnits(units, at, BidMode.NOT_BID);
  }

  /** Indicates whether or not bidding is enabled during placement. */
  enum BidMode {
    BID,
    NOT_BID
  }

  /**
   * Query what units can be produced in a given territory. ProductionResponse may indicate an error
   * string that there can be no units placed in a given territory
   *
   * @param units place-able units
   * @param at referring territory
   * @return object that contains place-able units
   */
  @RemoteActionCode(6)
  PlaceableUnits getPlaceableUnits(Collection<Unit> units, Territory at);

  /**
   * Returns the number of placements made so far. this is not the number of units placed, but the
   * number of times we have made successful placements.
   */
  @RemoteActionCode(7)
  int getPlacementsMade();

  /**
   * Get what air units must move before the end of the players turn.
   *
   * @return a list of Territories with air units that must move
   */
  @RemoteActionCode(9)
  Collection<Territory> getTerritoriesWhereAirCantLand();

  @RemoteActionCode(17)
  @Override
  String undoMove(int moveIndex);
}
