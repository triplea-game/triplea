package games.strategy.engine.data;

import games.strategy.triplea.delegate.TechAdvance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TechnologyFrontier extends GameDataComponent implements Iterable<TechAdvance> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5245743727479551766L;
	private final List<TechAdvance> m_techs = new ArrayList<TechAdvance>();
	private List<TechAdvance> m_cachedTechs;
	private final String m_name;

	
    public TechnologyFrontier(String name, GameData data)
	{
		super(data);
		m_name = name;
    }

	public void addAdvance(TechAdvance t)
	{
        if(m_techs.contains(t))
          throw new IllegalStateException("Advance already added:" + t);

		m_techs.add(t);
		
	}
	
	public void addAdvance(List<TechAdvance> list)
	{
		for(TechAdvance t:list)
			addAdvance(t);
	}
	public void removeAdvance(TechAdvance t)
	{
        if(!m_techs.contains(t))
            throw new IllegalStateException("Advance not present:" + t);

  		m_techs.remove(t);
  		
	}

	public List<TechAdvance> getTechs()
	{
		if(m_cachedTechs == null)
			m_cachedTechs = Collections.unmodifiableList(m_techs);
		return m_cachedTechs;
	}

    public Iterator<TechAdvance> iterator()
    {
        return getTechs().iterator();
    }
    
    public String getName()
    {
      return m_name;
    }

    public boolean equals(Object o)
    {
      if (o == null || ! (o instanceof Named))
        return false;

      Named other = (Named) o;

      return this.m_name.equals(other.getName());
    }

    public int hashCode()
    {
      return m_name.hashCode();
    }
    
    public boolean isEmpty() {
    	return m_techs.isEmpty();
    }
    public String toString()
    {
      return m_name;
    }
}
