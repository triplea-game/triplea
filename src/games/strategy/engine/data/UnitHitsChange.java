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
package games.strategy.engine.data;

import games.strategy.util.IntegerMap;
import games.strategy.util.Util;

import java.util.Collection;
import java.util.Set;

@SuppressWarnings("serial")
public class UnitHitsChange extends Change
{
	private final IntegerMap<Unit> m_hits;
	private final IntegerMap<Unit> m_undoHits;
	
	private UnitHitsChange(final IntegerMap<Unit> hits, final IntegerMap<Unit> undoHits)
	{
		m_hits = hits;
		m_undoHits = undoHits;
	}
	
	public Collection<Unit> getUnits()
	{
		return m_hits.keySet();
	}
	
	UnitHitsChange(final IntegerMap<Unit> hits)
	{
		m_hits = hits.copy();
		m_undoHits = new IntegerMap<Unit>();
		for (final Unit item : m_hits.keySet())
		{
			m_undoHits.put(item, item.getHits());
		}
	}
	
	@Override
	protected void perform(final GameData data)
	{
		for (final Unit item : m_hits.keySet())
		{
			item.setHits(m_hits.getInt(item));
		}
		final Set<Unit> units = m_hits.keySet();
		for (final Territory element : data.getMap().getTerritories())
		{
			if (Util.someIntersect(element.getUnits().getUnits(), units))
			{
				element.notifyChanged();
			}
		}
	}
	
	@Override
	public Change invert()
	{
		return new UnitHitsChange(m_undoHits, m_hits);
	}
}
