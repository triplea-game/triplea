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

/*
 * PlayerID.java
 * 
 * Created on October 13, 2001, 9:34 AM
 */

package games.strategy.engine.data;

import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchAnd;
import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class PlayerID extends NamedAttachable implements NamedUnitHolder, Serializable
{
	private final boolean m_optional;
	private final UnitCollection m_unitsHeld;
	private final ResourceCollection m_resources;
	private ProductionFrontier m_productionFrontier;
	private RepairFrontier m_repairFrontier;
	private final TechnologyFrontierList m_technologyFrontiers;
	private String m_whoAmI = "null:no_one";
	
	/** Creates new Player */
	public PlayerID(String name, boolean optional, GameData data)
	{
		super(name, data);
		m_optional = optional;
		m_unitsHeld = new UnitCollection(this, getData());
		m_resources = new ResourceCollection(getData());
		m_technologyFrontiers = new TechnologyFrontierList(getData());
	}
	
	public boolean getOptional()
	{
		return m_optional;
	}
	
	public UnitCollection getUnits()
	{
		return m_unitsHeld;
	}
	
	public ResourceCollection getResources()
	{
		return m_resources;
	}
	
	public TechnologyFrontierList getTechnologyFrontierList()
	{
		return m_technologyFrontiers;
	}
	
	public void setProductionFrontier(ProductionFrontier frontier)
	{
		m_productionFrontier = frontier;
	}
	
	public ProductionFrontier getProductionFrontier()
	{
		return m_productionFrontier;
	}
	
	public void setRepairFrontier(RepairFrontier frontier)
	{
		m_repairFrontier = frontier;
	}
	
	public RepairFrontier getRepairFrontier()
	{
		return m_repairFrontier;
	}
	
	public void notifyChanged()
	{
	}
	
	public boolean isNull()
	{
		return false;
	}
	
	public static final PlayerID NULL_PLAYERID = new PlayerID("Neutral", true, null)
	{
		// compatible with 0.9.0.2 saved games
		private static final long serialVersionUID = -6596127754502509049L;
		
		@Override
		public boolean isNull()
		{
			return true;
		}
	};
	
	@Override
	public String toString()
	{
		return "PlayerID named:" + getName();
	}
	
	public String getType()
	{
		return UnitHolder.PLAYER;
	}
	
	/**
	 * First string is "Human" or "AI", while second string is the name of the player, like "Moore N. Able (AI)". Separate with a colon.
	 * @param humanOrAI_and_playerName
	 */
	public void setWhoAmI(String humanOrAI_colon_playerName)
	{
		// so for example, it should be "AI:Moore N. Able (AI)".
		String[] s = humanOrAI_colon_playerName.split(":");
		if (s.length != 2)
			throw new IllegalStateException("whoAmI must have two strings, separated by a colon");
		if (!(s[0].equalsIgnoreCase("AI") || s[0].equalsIgnoreCase("Human") || s[0].equalsIgnoreCase("null")))
			throw new IllegalStateException("whoAmI first part must be, ai or human or client");
		m_whoAmI = humanOrAI_colon_playerName;
	}
	
	public String getWhoAmI()
	{
		return m_whoAmI;
	}
	
	public boolean isAI()
	{
		return m_whoAmI.split(":")[0].equalsIgnoreCase("AI");
	}
	
	/**
	 * If I have no units with movement,
	 * And I own zero factories or have have no owned land,
	 * then I am basically dead, and therefore should not participate in things like politics.
	 * @return
	 */
	public boolean amNotDeadYet()
	{
		boolean hasFactory = false;
		boolean ownsLand = false;
		for (Territory t : getData().getMap().getTerritories())
		{
			if (t.getUnits().someMatch(new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(this), Matches.unitHasAttackValueOfAtLeast(1), Matches.UnitIsNotStatic(this), Matches.UnitIsLand)))
				return true;
			if (t.getOwner().equals(this))
				ownsLand = true;
			if (t.getUnits().someMatch(new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(this), Matches.UnitIsFactoryOrCanProduceUnits)))
				hasFactory = true;
			if (ownsLand && hasFactory)
				return true;
		}
		return false;
	}
	
	public static LinkedHashMap<String, String> currentPlayers(GameData data)
	{
		LinkedHashMap<String,String> rVal = new LinkedHashMap<String,String>();
		for (PlayerID player : data.getPlayerList().getPlayers())
		{
			rVal.put(player.getName(), player.getWhoAmI().split(":")[1]);
		}
		return rVal;
	}
	
}
