/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;

import java.util.Collection;

/**
 * @author Sean Bridges
 */
public interface IAbstractPlaceDelegate extends IAbstractMoveDelegate
{
	/**
	 * @param units
	 *            units to place
	 * @param at
	 *            territory to place
	 * @return an error code if the placement was not successful
	 * 
	 */
	public String placeUnits(Collection<Unit> units, Territory at);
	
	/**
	 * Query what units can be produced in a given territory.
	 * ProductionResponse may indicate an error string that there
	 * can be no units placed in a given territory
	 * 
	 * @param units
	 *            place-able units
	 * @param at
	 *            referring territory
	 * @return object that contains place-able units
	 */
	public PlaceableUnits getPlaceableUnits(Collection<Unit> units, Territory at);
	
	/**
	 * 
	 * @return the number of placements made so far.
	 *         this is not the number of units placed, but the number
	 *         of times we have made successful placements
	 */
	public int getPlacementsMade();
	
	/**
	 * Get what air units must move before the end of the players turn
	 * 
	 * @return a list of Territories with air units that must move
	 */
	public Collection<Territory> getTerritoriesWhereAirCantLand();
}
