package games.strategy.engine.data;

import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TechnologyFrontier extends GameDataComponent implements Iterable<TechAdvance>
{
	
	private static final long serialVersionUID = -5245743727479551766L;
	private List<TechAdvance> m_techs = new ArrayList<TechAdvance>();
	private List<TechAdvance> m_cachedTechs;
	private final String m_name;
	
	public TechnologyFrontier(String name, GameData data)
	{
		super(data);
		m_name = name;
	}
	
	public void addAdvance(TechAdvance t)
	{
		m_cachedTechs = null;
		m_techs.add(t);
		Util.reorder(m_techs, getData().getTechnologyFrontier().getTechs());
		
	}
	
	public void addAdvance(List<TechAdvance> list)
	{
		for (TechAdvance t : list)
			addAdvance(t);
	}
	
	public void removeAdvance(TechAdvance t)
	{
		if (!m_techs.contains(t))
		{
			throw new IllegalStateException("Advance not present:" + t);
		}
		m_cachedTechs = null;
		m_techs.remove(t);
	}
	
	public TechAdvance getAdvanceByProperty(String property)
	{
		for (TechAdvance ta : m_techs)
			if (ta.getProperty().equals(property))
				return ta;
		return null;
	}
	
	public TechAdvance getAdvanceByName(String name)
	{
		for (TechAdvance ta : m_techs)
			if (ta.getName().equals(name))
				return ta;
		return null;
	}
	
	public List<TechAdvance> getTechs()
	{
		if (m_cachedTechs == null)
			m_cachedTechs = Collections.unmodifiableList(m_techs);
		return m_cachedTechs;
	}
	
	@Override
	public Iterator<TechAdvance> iterator()
	{
		return getTechs().iterator();
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public boolean isEmpty()
	{
		return m_techs.isEmpty();
	}
	
	@Override
	public String toString()
	{
		return m_name;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof TechnologyFrontier))
			return false;
		
		TechnologyFrontier other = (TechnologyFrontier) o;
		
		return this.m_name.equals(other.getName());
	}
	
}
