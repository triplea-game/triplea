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

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author Sean Bridges
 */
@SuppressWarnings("serial")
public class MoveDescription extends AbstractMoveDescription
{
	private final Route m_route;
	private final Collection<Unit> m_transportsThatCanBeLoaded;
	private final Map<Unit, Collection<Unit>> m_dependentUnits;
	
	public MoveDescription(final Collection<Unit> units, final Route route, final Collection<Unit> transportsThatCanBeLoaded, final Map<Unit, Collection<Unit>> dependentUnits)
	{
		super(units);
		m_route = route;
		m_transportsThatCanBeLoaded = transportsThatCanBeLoaded;
		if (dependentUnits != null && !dependentUnits.isEmpty())
		{
			m_dependentUnits = new HashMap<Unit, Collection<Unit>>();
			for (final Entry<Unit, Collection<Unit>> entry : dependentUnits.entrySet())
			{
				m_dependentUnits.put(entry.getKey(), new HashSet<Unit>(entry.getValue()));
			}
		}
		else
		{
			m_dependentUnits = null;
		}
	}
	
	public MoveDescription(final Collection<Unit> units, final Route route, final Collection<Unit> transportsThatCanBeLoaded)
	{
		super(units);
		m_route = route;
		m_transportsThatCanBeLoaded = transportsThatCanBeLoaded;
		m_dependentUnits = null;
	}
	
	public MoveDescription(final Collection<Unit> units, final Route route)
	{
		super(units);
		m_route = route;
		m_transportsThatCanBeLoaded = null;
		m_dependentUnits = null;
	}
	
	public Route getRoute()
	{
		return m_route;
	}
	
	@Override
	public String toString()
	{
		return "Move message route:" + m_route + " units:" + getUnits();
	}
	
	public Collection<Unit> getTransportsThatCanBeLoaded()
	{
		if (m_transportsThatCanBeLoaded == null)
			return Collections.emptyList();
		return m_transportsThatCanBeLoaded;
	}
	
	public Map<Unit, Collection<Unit>> getDependentUnits()
	{
		if (m_dependentUnits == null)
			return new HashMap<Unit, Collection<Unit>>();
		return m_dependentUnits;
	}
}
