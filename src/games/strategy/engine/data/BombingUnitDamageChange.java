package games.strategy.engine.data;

import games.strategy.triplea.TripleAUnit;
import games.strategy.util.IntegerMap;
import games.strategy.util.Util;

import java.util.Collection;
import java.util.Set;

/**
 * 
 * @author veqryn
 * 
 */
public class BombingUnitDamageChange extends Change
{
	private static final long serialVersionUID = -6425858423179501847L;
	private final IntegerMap<Unit> m_hits;
	private final IntegerMap<Unit> m_undoHits;
	
	private BombingUnitDamageChange(final IntegerMap<Unit> hits, final IntegerMap<Unit> undoHits)
	{
		m_hits = hits;
		m_undoHits = undoHits;
	}
	
	public Collection<Unit> getUnits()
	{
		return m_hits.keySet();
	}
	
	BombingUnitDamageChange(final IntegerMap<Unit> hits)
	{
		for (final Unit u : hits.keySet())
		{
			if (!(u instanceof TripleAUnit))
				throw new IllegalArgumentException("BombingUnitDamage can only apply to a TripleAUnit object");
		}
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
			((TripleAUnit) item).setUnitDamage(m_hits.getInt(item));
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
		return new BombingUnitDamageChange(m_undoHits, m_hits);
	}
}
