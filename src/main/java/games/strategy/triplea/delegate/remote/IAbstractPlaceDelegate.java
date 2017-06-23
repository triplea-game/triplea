package games.strategy.triplea.delegate.remote;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.UndoablePlacement;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;

public interface IAbstractPlaceDelegate extends IAbstractMoveDelegate<UndoablePlacement> {
  /**
   * @param units
   *        units to place.
   * @param at
   *        territory to place
   * @return an error code if the placement was not successful
   */
  String placeUnits(Collection<Unit> units, Territory at, BidMode bidMode);

  default String placeUnits(Collection<Unit> units, Territory at) {
    return placeUnits(units, at, BidMode.NOT_BID);
  }

  enum BidMode {
    BID, NOT_BID
  }

  /**
   * Query what units can be produced in a given territory.
   * ProductionResponse may indicate an error string that there
   * can be no units placed in a given territory
   *
   * @param units
   *        place-able units
   * @param at
   *        referring territory
   * @return object that contains place-able units
   */
  PlaceableUnits getPlaceableUnits(Collection<Unit> units, Territory at);

  /**
   * @return The number of placements made so far.
   *         this is not the number of units placed, but the number
   *         of times we have made successful placements.
   */
  int getPlacementsMade();

  /**
   * Get what air units must move before the end of the players turn.
   *
   * @return a list of Territories with air units that must move
   */
  Collection<Territory> getTerritoriesWhereAirCantLand();
}
