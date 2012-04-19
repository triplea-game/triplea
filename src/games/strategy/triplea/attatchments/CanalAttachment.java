package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CanalAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = -1991066817386812634L;
	private String m_canalName = null;
	private HashSet<Territory> m_landTerritories = null;
	private HashSet<UnitType> m_excludedUnits = null;
	
	public CanalAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	public static Set<Territory> getAllCanalSeaZones(final String canalName, final GameData data)
	{
		final Set<Territory> rVal = new HashSet<Territory>();
		for (final Territory t : data.getMap())
		{
			final Set<CanalAttachment> canalAttachments = get(t);
			if (canalAttachments.isEmpty())
				continue;
			for (final CanalAttachment canalAttachment : canalAttachments)
			{
				if (canalAttachment.getCanalName().equals(canalName))
				{
					rVal.add(t);
				}
			}
		}
		if (rVal.size() != 2)
			throw new IllegalStateException("Wrong number of sea zones for canal (exactly 2 sea zones may have the same canalName):" + rVal);
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
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCanalName(final String name)
	{
		if (name == null)
		{
			m_canalName = null;
			return;
		}
		m_canalName = name;
	}
	
	public String getCanalName()
	{
		return m_canalName;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setLandTerritories(final String landTerritories)
	{
		if (landTerritories == null)
		{
			m_landTerritories = null;
			return;
		}
		final HashSet<Territory> terrs = new HashSet<Territory>();
		for (final String name : landTerritories.split(":"))
		{
			final Territory territory = getData().getMap().getTerritory(name);
			if (territory == null)
				throw new IllegalStateException("Canals: No territory called: " + name + thisErrorMsg());
			terrs.add(territory);
		}
		m_landTerritories = terrs;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setLandTerritories(final HashSet<Territory> value)
	{
		m_landTerritories = value;
	}
	
	public HashSet<Territory> getLandTerritories()
	{
		return m_landTerritories;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param landTerritories
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setExcludedUnits(final String value)
	{
		if (value == null)
		{
			m_excludedUnits = null;
			return;
		}
		if (m_excludedUnits == null)
			m_excludedUnits = new HashSet<UnitType>();
		if (value.equalsIgnoreCase("NONE"))
			return;
		if (value.equalsIgnoreCase("ALL"))
		{
			m_excludedUnits.addAll(getData().getUnitTypeList().getAllUnitTypes());
			return;
		}
		for (final String name : value.split(":"))
		{
			final UnitType ut = getData().getUnitTypeList().getUnitType(name);
			if (ut == null)
				throw new IllegalStateException("Canals: No UnitType called: " + name + thisErrorMsg());
			m_excludedUnits.add(ut);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setExcludedUnits(final HashSet<UnitType> value)
	{
		m_excludedUnits = value;
	}
	
	public HashSet<UnitType> getExcludedUnits(final GameData data)
	{
		if (m_excludedUnits == null)
		{
			return new HashSet<UnitType>(Match.getMatches(getData().getUnitTypeList().getAllUnitTypes(), Matches.UnitTypeIsAir));
		}
		return m_excludedUnits;
	}
	
	public void clearExcludedUnits()
	{
		m_excludedUnits.clear();
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		if (m_canalName == null)
			throw new GameParseException("Canals must have a canalName set!" + thisErrorMsg());
		if (m_landTerritories == null || m_landTerritories.size() == 0)
			throw new GameParseException("Canal named " + m_canalName + " must have landTerritories set!" + thisErrorMsg());
	}
}
