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
	private static final long serialVersionUID = -2284878450555315947L;
	private final boolean m_optional;
	private final boolean m_canBeDisabled;
	private boolean m_isDisabled = false;
	private final UnitCollection m_unitsHeld;
	private final ResourceCollection m_resources;
	private ProductionFrontier m_productionFrontier;
	private RepairFrontier m_repairFrontier;
	private final TechnologyFrontierList m_technologyFrontiers;
	private String m_whoAmI = "null:no_one";
	
	/** Creates new Player */
	public PlayerID(final String name, final boolean optional, final boolean canBeDisabled, final GameData data)
	{
		super(name, data);
		m_optional = optional;
		m_canBeDisabled = canBeDisabled;
		m_unitsHeld = new UnitCollection(this, data);
		m_resources = new ResourceCollection(data);
		m_technologyFrontiers = new TechnologyFrontierList(data);
	}
	
	public boolean getOptional()
	{
		return m_optional;
	}
	
	public boolean getCanBeDisabled()
	{
		return m_canBeDisabled;
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
	
	public void setProductionFrontier(final ProductionFrontier frontier)
	{
		m_productionFrontier = frontier;
	}
	
	public ProductionFrontier getProductionFrontier()
	{
		return m_productionFrontier;
	}
	
	public void setRepairFrontier(final RepairFrontier frontier)
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
	
	public static final PlayerID NULL_PLAYERID = new PlayerID("Neutral", true, false, null)
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
	 * 
	 * @param humanOrAI_and_playerName
	 */
	public void setWhoAmI(final String humanOrAI_colon_playerName)
	{
		// so for example, it should be "AI:Moore N. Able (AI)".
		final String[] s = humanOrAI_colon_playerName.split(":");
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
	
	public void setIsDisabled(final boolean isDisabled)
	{
		m_isDisabled = isDisabled;
	}
	
	public boolean getIsDisabled()
	{
		return m_isDisabled;
	}
	
	/**
	 * If I have no units with movement,
	 * And I own zero factories or have have no owned land,
	 * then I am basically dead, and therefore should not participate in things like politics.
	 * 
	 * @return
	 */
	public boolean amNotDeadYet(final GameData data)
	{
		boolean hasFactory = false;
		boolean ownsLand = false;
		for (final Territory t : data.getMap().getTerritories())
		{
			if (t.getUnits().someMatch(new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(this), Matches.unitHasAttackValueOfAtLeast(1), Matches.UnitCanMove, Matches.UnitIsLand)))
				return true;
			if (t.getOwner().equals(this))
				ownsLand = true;
			if (t.getUnits().someMatch(new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(this), Matches.UnitCanProduceUnits)))
				hasFactory = true;
			if (ownsLand && hasFactory)
				return true;
		}
		return false;
	}
	
	public static LinkedHashMap<String, String> currentPlayers(final GameData data)
	{
		final LinkedHashMap<String, String> rVal = new LinkedHashMap<String, String>();
		if (data == null)
			return rVal;
		for (final PlayerID player : data.getPlayerList().getPlayers())
		{
			rVal.put(player.getName(), player.getWhoAmI().split(":")[1]);
		}
		return rVal;
	}
	
	/*public static String whoIsAI(GameData data)
	{
		StringBuilder buf = new StringBuilder("AIs are playing the following players: ");
		boolean areThereAIs = false;
		for (PlayerID player : data.getPlayerList().getPlayers())
		{
			if (player.isAI())
			{
				areThereAIs = true;
				buf.append(", " + player.getName());
			}
		}
		if (!areThereAIs)
			return null;
		return buf.toString().replaceFirst(", ", "");
	}*/
	
	/**
	 * Do not use this ever. The Null PlayerID has no GameData associated with it, so you WILL get a Null Pointer error.
	 */
	@Deprecated
	@Override
	public GameData getData()
	{
		return super.getData();
	}
}
