package games.strategy.engine.data;

import games.strategy.triplea.delegate.TechAdvance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TechnologyFrontierList extends GameDataComponent{

/**
	 * 
	 */
	private static final long serialVersionUID = 2958122401265284935L;
	private final Map<String, TechnologyFrontier> m_technologyFrontiers = new HashMap<String, TechnologyFrontier>();
	
    public TechnologyFrontierList(GameData data) 
	{
		super(data);
    }
	
	protected void addTechnologyFrontier(TechnologyFrontier tf)
	{
		m_technologyFrontiers.put(tf.getName(), tf);
	}
	
	public int size()
	{
		return m_technologyFrontiers.size();
	}
	
	public TechnologyFrontier getTechnologyFrontier(String name)
	{
		return m_technologyFrontiers.get(name);
	}
	
	public Set<String> getTechnologyFrontierNames()
	{
	    return m_technologyFrontiers.keySet();
	}
	
	public List<TechAdvance> getAdvances() {
		List<TechAdvance> techs = new ArrayList<TechAdvance>();
			for(TechnologyFrontier t:m_technologyFrontiers.values()){
				techs.addAll(t.getTechs());
			}
		return techs;
	}

	public List<TechnologyFrontier> getFrontiers() {
		return new ArrayList<TechnologyFrontier>(m_technologyFrontiers.values());
	}
	public boolean isEmpty() {
		return size() == 0;
	}
}
