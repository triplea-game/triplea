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
 * TerritoryListener.java
 *
 * Created on November 6, 2001, 2:22 PM
 */

package games.strategy.engine.data.events;

import games.strategy.engine.data.Territory;

/**
 *
 * A TerritoryListener will be notified of events that affect a Territory.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface TerritoryListener 
{
	public void unitsChanged(Territory territory);
	public void ownerChanged(Territory territory);
}
