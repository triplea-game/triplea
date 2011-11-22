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
/*
 * BuyMessage.java
 * 
 * Created on November 6, 2001, 8:26 PM
 */
package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

import java.util.Collection;

/**
 * 
 * @author Erik von der Osten
 */
@SuppressWarnings("serial")
public class PlacementDescription extends AbstractMoveDescription
{
	private final Territory m_territory;
	
	public PlacementDescription(final Collection<Unit> units, final Territory territory)
	{
		super(units);
		m_territory = territory;
	}
	
	public Territory getTerritory()
	{
		return m_territory;
	}
	
	@Override
	public String toString()
	{
		return "Placement message territory:" + m_territory + " units:" + getUnits();
	}
}
