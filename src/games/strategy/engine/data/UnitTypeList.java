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
 * UnitTypeList.java
 * 
 * Created on October 17, 2001, 9:21 PM
 */
package games.strategy.engine.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          A collection of unit types
 */
public class UnitTypeList extends GameDataComponent implements Iterable<UnitType>
{
	private static final long serialVersionUID = 9002927658524651749L;
	private final Map<String, UnitType> m_unitTypes = new HashMap<String, UnitType>();
	
	/**
	 * Creates new UnitTypeCollection
	 * 
	 * @param data
	 *            game data
	 */
	public UnitTypeList(final GameData data)
	{
		super(data);
	}
	
	protected void addUnitType(final UnitType type)
	{
		m_unitTypes.put(type.getName(), type);
	}
	
	public UnitType getUnitType(final String name)
	{
		return m_unitTypes.get(name);
	}
	
	/**
	 * Will return null if even a single name is not on the unit list.
	 * 
	 * @param names
	 * @return
	 */
	public Set<UnitType> getUnitTypes(final String[] names)
	{
		final Set<UnitType> types = new HashSet<UnitType>();
		for (final String name : names)
		{
			final UnitType type = m_unitTypes.get(name);
			if (type == null)
				return null;
			types.add(type);
		}
		return types;
	}
	
	public int size()
	{
		return m_unitTypes.size();
	}
	
	public Iterator<UnitType> iterator()
	{
		return m_unitTypes.values().iterator();
	}
	
	public Set<UnitType> getAllUnitTypes()
	{
		return new LinkedHashSet<UnitType>(m_unitTypes.values());
	}
}
