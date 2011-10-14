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

import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.TechAdvance;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sean Bridges
 */

public class TechAttachment extends DefaultAttachment
{
	
	// attatches to a PlayerID
	
	public static TechAttachment get(PlayerID id)
	{
		TechAttachment attatchment = (TechAttachment) id.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		// dont crash, as a map xml may not set the tech attachment for all players, so just create a new tech attachment for them
		if (attatchment == null)
			return new TechAttachment();
		return attatchment;
	}
	
	public static TechAttachment get(PlayerID id, String nameOfAttachment)
	{
		if (!nameOfAttachment.equals(Constants.TECH_ATTACHMENT_NAME))
			throw new IllegalStateException("TechAttachment may not yet get attachments not named:" + Constants.TECH_ATTACHMENT_NAME);
		TechAttachment attatchment = (TechAttachment) id.getAttachment(nameOfAttachment);
		// dont crash, as a map xml may not set the tech attachment for all players, so just create a new tech attachment for them
		if (attatchment == null)
			return new TechAttachment();
		return attatchment;
	}
	
	private int m_techCost;
	private boolean m_heavyBomber;
	private boolean m_longRangeAir;
	private boolean m_jetPower;
	private boolean m_rocket;
	private boolean m_industrialTechnology;
	private boolean m_superSub;
	private boolean m_destroyerBombard;
	private boolean m_improvedArtillerySupport;
	private boolean m_paratroopers;
	private boolean m_increasedFactoryProduction;
	private boolean m_warBonds;
	private boolean m_mechanizedInfantry;
	private boolean m_aARadar;
	private boolean m_shipyards;
	private final Map<String, Boolean> m_GenericTech = new HashMap<String, Boolean>();
	
	public void setTechCost(String s)
	{
		m_techCost = getInt(s);
	}
	
	public void setHeavyBomber(String s)
	{
		m_heavyBomber = getBool(s);
	}
	
	public void setDestroyerBombard(String s)
	{
		m_destroyerBombard = getBool(s);
	}
	
	public void setLongRangeAir(String s)
	{
		m_longRangeAir = getBool(s);
	}
	
	public void setJetPower(String s)
	{
		m_jetPower = getBool(s);
	}
	
	public void setRocket(String s)
	{
		m_rocket = getBool(s);
	}
	
	public void setIndustrialTechnology(String s)
	{
		m_industrialTechnology = getBool(s);
	}
	
	public void setSuperSub(String s)
	{
		m_superSub = getBool(s);
	}
	
	public void setImprovedArtillerySupport(String s)
	{
		m_improvedArtillerySupport = getBool(s);
	}
	
	public void setParatroopers(String s)
	{
		m_paratroopers = getBool(s);
	}
	
	public void setIncreasedFactoryProduction(String s)
	{
		m_increasedFactoryProduction = getBool(s);
	}
	
	public void setWarBonds(String s)
	{
		m_warBonds = getBool(s);
	}
	
	public void setMechanizedInfantry(String s)
	{
		m_mechanizedInfantry = getBool(s);
	}
	
	public void setAARadar(String s)
	{
		m_aARadar = getBool(s);
	}
	
	public void setShipyards(String s)
	{
		m_shipyards = getBool(s);
	}
	
	public String getHeavyBomber()
	{
		return "" + m_heavyBomber;
	}
	
	public String getTechCost()
	{
		return "" + (m_techCost > 0 ? m_techCost : Constants.TECH_ROLL_COST);
	}
	
	public String getLongRangeAir()
	{
		return "" + m_longRangeAir;
	}
	
	public String getJetPower()
	{
		return "" + m_jetPower;
	}
	
	public String getRocket()
	{
		return "" + m_rocket;
	}
	
	public String getIndustrialTechnology()
	{
		return "" + m_industrialTechnology;
	}
	
	public String getSuperSub()
	{
		return "" + m_superSub;
	}
	
	public String getDestroyerBombard()
	{
		return "" + m_destroyerBombard;
	}
	
	public String getImprovedArtillerySupport()
	{
		return "" + m_improvedArtillerySupport;
	}
	
	public String getParatroopers()
	{
		return "" + m_paratroopers;
	}
	
	public String getIncreasedFactoryProduction()
	{
		return "" + m_increasedFactoryProduction;
	}
	
	public String getWarBonds()
	{
		return "" + m_warBonds;
	}
	
	public String getMechanizedInfantry()
	{
		return "" + m_mechanizedInfantry;
	}
	
	public String getAARadar()
	{
		return "" + m_aARadar;
	}
	
	public String getShipyards()
	{
		return "" + m_shipyards;
	}
	
	public TechAttachment()
	{
	}
	
	public boolean hasHeavyBomber()
	{
		return m_heavyBomber;
	}
	
	public boolean hasLongRangeAir()
	{
		return m_longRangeAir;
	}
	
	public boolean hasJetPower()
	{
		return m_jetPower;
	}
	
	public boolean hasRocket()
	{
		return m_rocket;
	}
	
	public boolean hasIndustrialTechnology()
	{
		return m_industrialTechnology;
	}
	
	public boolean hasSuperSub()
	{
		return m_superSub;
	}
	
	public boolean hasDestroyerBombard()
	{
		return m_destroyerBombard;
	}
	
	public boolean hasImprovedArtillerySupport()
	{
		return m_improvedArtillerySupport;
	}
	
	public boolean hasParatroopers()
	{
		return m_paratroopers;
	}
	
	public boolean hasIncreasedFactoryProduction()
	{
		return m_increasedFactoryProduction;
	}
	
	public boolean hasWarBonds()
	{
		return m_warBonds;
	}
	
	public boolean hasMechanizedInfantry()
	{
		return m_mechanizedInfantry;
	}
	
	public boolean hasAARadar()
	{
		return m_aARadar;
	}
	
	public boolean hasShipyards()
	{
		return m_shipyards;
	}
	
	@Override
	public void setData(GameData data)
	{
		super.setData(data);
		for (TechAdvance ta : data.getTechnologyFrontier())
		{
			if (ta instanceof GenericTechAdvance)
				if (((GenericTechAdvance) ta).getAdvance() == null)
					m_GenericTech.put(ta.getProperty(), Boolean.FALSE);
		}
	}
	
	public Boolean hasGenericTech(String name)
	{
		return m_GenericTech.get(name);
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param name
	 * @param value
	 */
	public void setGenericTech(String name, Boolean value)
	{
		m_GenericTech.put(name, value);
	}
	
	public Map<String, Boolean> getGenericTech()
	{
		return m_GenericTech;
	}
	
	public void clearGenericTech()
	{
		m_GenericTech.clear();
	}
	
}
