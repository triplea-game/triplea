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
package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.Collection;

public class ProMatches
{
	
	/**
	 * 
	 * @param t
	 *            - Territory we are testing for required units
	 * @return - Whether the territory contains one of the required combos of units
	 *         (and if 'doNotCountNeighbors' is false, and unit is Sea unit, will return true if an adjacent land territory has one of the required combos as well).
	 */
	public static Match<Unit> unitWhichRequiresUnitsHasRequiredUnits(final PlayerID player, final Territory t)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unitWhichRequiresUnits)
			{
				if (!Matches.UnitRequiresUnitsOnCreation.match(unitWhichRequiresUnits))
					return true;
				final Collection<Unit> unitsAtStartOfTurnInProducer = t.getUnits().getUnits();
				if (Matches.UnitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInProducer).match(unitWhichRequiresUnits))
					return true;
				if (t.isWater() && Matches.UnitIsSea.match(unitWhichRequiresUnits))
				{
					for (final Territory neighbor : t.getData().getMap().getNeighbors(t, Matches.TerritoryIsLand))
					{
						final Collection<Unit> unitsAtStartOfTurnInCurrent = neighbor.getUnits().getUnits();
						if (Matches.UnitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInCurrent).match(unitWhichRequiresUnits))
							return true;
					}
				}
				return false;
			}
		};
	}
	
}
