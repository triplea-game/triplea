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

package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.Formatter;

/**
 *
 * Logic for placing units.  <p>
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Known limitations.
 *
 * Doesnt take into account limits on number of factories that can be produced.
 *
 * The situation where one has two non original factories a,b each with production 2.
 * If sea zone e neighbors a,b and sea zone f neighbors b.  Then producing 2 in e
 * could make it such that you cannot produce in f
 * The reason is that the production in e could be assigned to the
 * factory in b, leaving no capacity to produce in f.
 * If anyone ever accidently runs into this situation then they can
 * undo the production, produce in f first, and then produce in e.
 */
public class PlaceDelegate extends AbstractPlaceDelegate
{

	/**
	 *
	 * @return gets the production of the territory, ignores wether the territory was an original factory
	 */
	protected int getProduction(Territory territory)
	{
		TerritoryAttatchment ta = TerritoryAttatchment.get(territory);
		return ta.getProduction();
	}

}


