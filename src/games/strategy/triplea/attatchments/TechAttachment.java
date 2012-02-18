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
package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.TechAdvance;

import java.util.HashMap;

/**
 * @author Sean Bridges
 */
@SuppressWarnings("serial")
public class TechAttachment extends DefaultAttachment
{
	// attaches to a PlayerID
	public static TechAttachment get(final PlayerID id)
	{
		final TechAttachment attachment = (TechAttachment) id.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		// dont crash, as a map xml may not set the tech attachment for all players, so just create a new tech attachment for them
		if (attachment == null)
		{
			return new TechAttachment();
		}
		return attachment;
	}
	
	public static TechAttachment get(final PlayerID id, final String nameOfAttachment)
	{
		if (!nameOfAttachment.equals(Constants.TECH_ATTACHMENT_NAME))
			throw new IllegalStateException("TechAttachment may not yet get attachments not named:" + Constants.TECH_ATTACHMENT_NAME);
		final TechAttachment attachment = (TechAttachment) id.getAttachment(nameOfAttachment);
		// dont crash, as a map xml may not set the tech attachment for all players, so just create a new tech attachment for them
		if (attachment == null)
		{
			return new TechAttachment();
		}
		return attachment;
	}
	
	private int m_techCost = 5;
	private boolean m_heavyBomber = false;
	private boolean m_longRangeAir = false;
	private boolean m_jetPower = false;
	private boolean m_rocket = false;
	private boolean m_industrialTechnology = false;
	private boolean m_superSub = false;
	private boolean m_destroyerBombard = false;
	private boolean m_improvedArtillerySupport = false;
	private boolean m_paratroopers = false;
	private boolean m_increasedFactoryProduction = false;
	private boolean m_warBonds = false;
	private boolean m_mechanizedInfantry = false;
	private boolean m_aARadar = false;
	private boolean m_shipyards = false;
	@InternalDoNotExport
	private HashMap<String, Boolean> m_GenericTech = new HashMap<String, Boolean>(); // do not export at this point. currently map xml can not define a player having a custom tech at start of game
	
	public TechAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
		setGenericTechs();
	}
	
	/**
	 * Since many maps do not include a tech attachment for each player (and no maps include tech attachments for the Null Player),
	 * we must ensure a default tech attachment is available for all these players. It is preferred to use the full constructor. Do not delete this.
	 * TODO: create tech attachments all players that don't have one, as the map is initialized.
	 */
	@Deprecated
	public TechAttachment()
	{
		super(Constants.TECH_ATTACHMENT_NAME, null, null);
		// TODO: not having game data, and not having generic techs, causes problems. Fix by creating real tech attachments for all players who are missing them, at the beginning of the game.
	}
	
	// setters
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTechCost(final String s)
	{
		m_techCost = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTechCost(final Integer s)
	{
		m_techCost = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setHeavyBomber(final String s)
	{
		m_heavyBomber = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setHeavyBomber(final Boolean s)
	{
		m_heavyBomber = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDestroyerBombard(final String s)
	{
		m_destroyerBombard = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDestroyerBombard(final Boolean s)
	{
		m_destroyerBombard = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setLongRangeAir(final String s)
	{
		m_longRangeAir = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setLongRangeAir(final Boolean s)
	{
		m_longRangeAir = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setJetPower(final String s)
	{
		m_jetPower = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setJetPower(final Boolean s)
	{
		m_jetPower = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRocket(final String s)
	{
		m_rocket = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRocket(final Boolean s)
	{
		m_rocket = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIndustrialTechnology(final String s)
	{
		m_industrialTechnology = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIndustrialTechnology(final Boolean s)
	{
		m_industrialTechnology = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setSuperSub(final String s)
	{
		m_superSub = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setSuperSub(final Boolean s)
	{
		m_superSub = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setImprovedArtillerySupport(final String s)
	{
		m_improvedArtillerySupport = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setImprovedArtillerySupport(final Boolean s)
	{
		m_improvedArtillerySupport = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setParatroopers(final String s)
	{
		m_paratroopers = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setParatroopers(final Boolean s)
	{
		m_paratroopers = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIncreasedFactoryProduction(final String s)
	{
		m_increasedFactoryProduction = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setIncreasedFactoryProduction(final Boolean s)
	{
		m_increasedFactoryProduction = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWarBonds(final String s)
	{
		m_warBonds = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWarBonds(final Boolean s)
	{
		m_warBonds = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMechanizedInfantry(final String s)
	{
		m_mechanizedInfantry = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setMechanizedInfantry(final Boolean s)
	{
		m_mechanizedInfantry = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAARadar(final String s)
	{
		m_aARadar = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAARadar(final Boolean s)
	{
		m_aARadar = s;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setShipyards(final String s)
	{
		m_shipyards = getBool(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setShipyards(final Boolean s)
	{
		m_shipyards = s;
	}
	
	// getters
	public int getTechCost()
	{
		return m_techCost;
	}
	
	public boolean getHeavyBomber()
	{
		return m_heavyBomber;
	}
	
	public boolean getLongRangeAir()
	{
		return m_longRangeAir;
	}
	
	public boolean getJetPower()
	{
		return m_jetPower;
	}
	
	public boolean getRocket()
	{
		return m_rocket;
	}
	
	public boolean getIndustrialTechnology()
	{
		return m_industrialTechnology;
	}
	
	public boolean getSuperSub()
	{
		return m_superSub;
	}
	
	public boolean getDestroyerBombard()
	{
		return m_destroyerBombard;
	}
	
	public boolean getImprovedArtillerySupport()
	{
		return m_improvedArtillerySupport;
	}
	
	public boolean getParatroopers()
	{
		return m_paratroopers;
	}
	
	public boolean getIncreasedFactoryProduction()
	{
		return m_increasedFactoryProduction;
	}
	
	public boolean getWarBonds()
	{
		return m_warBonds;
	}
	
	public boolean getMechanizedInfantry()
	{
		return m_mechanizedInfantry;
	}
	
	public boolean getAARadar()
	{
		return m_aARadar;
	}
	
	public boolean getShipyards()
	{
		return m_shipyards;
	}
	
	// custom techs
	/**
	 * Internal use only, is not set by xml or property utils.
	 */
	@InternalDoNotExport
	private void setGenericTechs()
	{
		for (final TechAdvance ta : getData().getTechnologyFrontier())
		{
			if (ta instanceof GenericTechAdvance)
				if (((GenericTechAdvance) ta).getAdvance() == null)
					m_GenericTech.put(ta.getProperty(), Boolean.FALSE);
		}
	}
	
	public Boolean hasGenericTech(final String name)
	{
		return m_GenericTech.get(name);
	}
	
	/**
	 * Internal use only, is not set by xml or property utils.
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param name
	 * @param value
	 */
	@InternalDoNotExport
	public void setGenericTech(final String name, final Boolean value)
	{
		m_GenericTech.put(name, value);
	}
	
	@InternalDoNotExport
	public void setGenericTech(final HashMap<String, Boolean> value)
	{
		m_GenericTech = value;
	}
	
	public HashMap<String, Boolean> getGenericTech()
	{
		return m_GenericTech;
	}
	
	public void clearGenericTech()
	{
		m_GenericTech.clear();
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
