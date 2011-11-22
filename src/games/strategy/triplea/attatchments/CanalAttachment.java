package games.strategy.triplea.attatchments;

import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CanalAttachment extends DefaultAttachment
{
	private String m_canalName;
	private String[] m_landTerritories;
	
	public static Set<Territory> getAllCanalSeaZones(final String canalName, final GameData data)
	{
		final Set<Territory> rVal = new HashSet<Territory>();
		for (final Territory t : data.getMap())
		{
			final Set<CanalAttachment> canalAttachments = get(t);
			if (canalAttachments.isEmpty())
				continue;
			final Iterator<CanalAttachment> iter = canalAttachments.iterator();
			while (iter.hasNext())
			{
				final CanalAttachment canalAttachment = iter.next();
				if (canalAttachment.getCanalName().equals(canalName))
				{
					rVal.add(t);
				}
			}
		}
		if (rVal.size() != 2)
			throw new IllegalStateException("Wrong number of sea zones for canal:" + rVal);
		return rVal;
	}
	
	public static Set<CanalAttachment> get(final Territory t)
	{
		final Set<CanalAttachment> rVal = new HashSet<CanalAttachment>();
		final Map<String, IAttachment> map = t.getAttachments();
		final Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext())
		{
			final IAttachment attachment = map.get(iter.next());
			final String name = attachment.getName();
			if (name.startsWith(Constants.CANAL_ATTACHMENT_PREFIX))
			{
				rVal.add((CanalAttachment) attachment);
			}
		}
		return rVal;
	}
	
	public static CanalAttachment get(final Territory t, final String nameOfAttachment)
	{
		final CanalAttachment rVal = (CanalAttachment) t.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("CanalAttachment: No canal attachment for:" + t.getName() + " with name: " + nameOfAttachment);
		return rVal;
	}
	
	public void setCanalName(final String name)
	{
		m_canalName = name;
	}
	
	public String getCanalName()
	{
		return m_canalName;
	}
	
	public void setLandTerritories(final String landTerritories)
	{
		m_landTerritories = landTerritories.split(":");
	}
	
	/**
	 * Called after the attatchment is created.
	 */
	public void validate() throws GameParseException
	{
		if (m_canalName == null || m_landTerritories == null || m_landTerritories.length == 0)
			throw new IllegalStateException("Canal error for:" + m_canalName + " not all variables set, land:" + m_landTerritories);
		getLandTerritories();
	}
	
	public Collection<Territory> getLandTerritories()
	{
		final List<Territory> rVal = new ArrayList<Territory>();
		for (final String name : m_landTerritories)
		{
			final Territory territory = getData().getMap().getTerritory(name);
			if (territory == null)
				throw new IllegalStateException("Canals: No territory called:" + territory);
			rVal.add(territory);
		}
		return rVal;
	}
}
