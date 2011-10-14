package games.strategy.engine.data;

import games.strategy.triplea.delegate.TechAdvance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TechnologyFrontierList extends GameDataComponent
{
	
	private static final long serialVersionUID = 2958122401265284935L;
	private final List<TechnologyFrontier> m_technologyFrontiers = new ArrayList<TechnologyFrontier>();
	
	public TechnologyFrontierList(GameData data)
	{
		super(data);
	}
	
	protected void addTechnologyFrontier(TechnologyFrontier tf)
	{
		m_technologyFrontiers.add(tf);
	}
	
	public int size()
	{
		return m_technologyFrontiers.size();
	}
	
	public TechnologyFrontier getTechnologyFrontier(String name)
	{
		for (TechnologyFrontier tf : m_technologyFrontiers)
			if (tf.getName().equals(name))
				return tf;
		return null;
	}
	
	public List<TechAdvance> getAdvances()
	{
		List<TechAdvance> techs = new ArrayList<TechAdvance>();
		for (TechnologyFrontier t : m_technologyFrontiers)
		{
			techs.addAll(t.getTechs());
		}
		return techs;
	}
	
	public List<TechnologyFrontier> getFrontiers()
	{
		return Collections.unmodifiableList(m_technologyFrontiers);
	}
	
	public boolean isEmpty()
	{
		return size() == 0;
	}
}
