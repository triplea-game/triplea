package games.strategy.triplea.delegate;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attatchments.TechAttachment;

public class GenericTechAdvance extends TechAdvance
{
	private final String m_name;
	private final TechAdvance m_advance;
	/**
	 * 
	 */
	private static final long serialVersionUID = -5985281030083508185L;
	
	public GenericTechAdvance(final String n, final TechAdvance t)
	{
		m_name = n;
		m_advance = t;
	}
	
	@Override
	public String getName()
	{
		return m_name;
	}
	
	@Override
	public String getProperty()
	{
		if (m_advance != null)
			return m_advance.getProperty();
		else
			return m_name;
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
		if (m_advance != null)
			m_advance.perform(id, bridge);
	}
	
	public TechAdvance getAdvance()
	{
		return m_advance;
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		if (m_advance != null)
			return m_advance.hasTech(ta);
		return ta.hasGenericTech(m_name);
	}
}
