/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * PlaceDelegate.java
 *
 * Created on November 2, 2001, 12:29 PM
 */

package games.strategy.twoIfBySea.delegate;

import games.strategy.engine.data.*;
import games.strategy.triplea.delegate.*;
import games.strategy.util.Match;

import java.util.Collection;

/**
 *
 * Logic for placing units.  <p>
 *
 * @author  Sean Bridges
 *
 */
public class PlaceDelegate extends AbstractPlaceDelegate
{

	/**
	 *
	 * @return gets the production of the territory, ignores wether the territory was an original factory
	 */
	protected int getProduction(Territory territory)
	{
		Collection<Unit> allUnits = territory.getUnits().getUnits();
		int factoryCount = Match.countMatches(allUnits, Matches.UnitIsFactory);
		return 5 * factoryCount;
	}

}


