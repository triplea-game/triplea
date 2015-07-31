package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * The Purpose of this class is to hold shared and simple methods used by RulesAttachment
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public abstract class AbstractRulesAttachment extends AbstractConditionsAttachment implements ICondition
{
	private static final long serialVersionUID = -6977650137928964759L;
	
	@InternalDoNotExport
	protected boolean m_countEach = false; // Do Not Export (do not include in IAttachment). Determines if we will be counting each for the purposes of m_objectiveValue
	@InternalDoNotExport
	protected int m_eachMultiple = 1; // Do Not Export (do not include in IAttachment). The multiple that will be applied to m_objectiveValue if m_countEach is true
	@InternalDoNotExport
	protected int m_territoryCount = -1; // Do Not Export (do not include in IAttachment). Used with the next Territory conditions to determine the number of territories needed to be valid (ex: m_alliedOwnershipTerritories)
	
	protected ArrayList<PlayerID> m_players = new ArrayList<PlayerID>(); // A list of players that can be used with directOwnershipTerritories, directExclusionTerritories, directPresenceTerritories, or any of the other territory lists
	
	protected int m_objectiveValue = 0; // only used if the attachment begins with "objectiveAttachment"
	protected int m_uses = -1; // only matters for objectiveValue, does not affect the condition
	protected HashMap<Integer, Integer> m_turns = null; // condition for what turn it is
	protected boolean m_switch = true; // for on/off conditions
	protected String m_gameProperty = null; // allows custom GameProperties
	
	public AbstractRulesAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param names
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setPlayers(final String names) throws GameParseException
	{
		final PlayerList pl = getData().getPlayerList();
		for (final String p : names.split(":"))
		{
			final PlayerID player = pl.getPlayerID(p);
			if (player == null)
				throw new GameParseException("Could not find player. name:" + p + thisErrorMsg());
			m_players.add(player);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setPlayers(final ArrayList<PlayerID> value)
	{
		m_players = value;
	}
	
	public ArrayList<PlayerID> getPlayers()
	{
		if (m_players.isEmpty())
			return new ArrayList<PlayerID>(Collections.singletonList((PlayerID) getAttachedTo()));
		else
			return m_players;
	}
	
	public void clearPlayers()
	{
		m_players.clear();
	}
	
	public void resetPlayers()
	{
		m_players = new ArrayList<PlayerID>();
	}
	
	@Override
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setChance(final String chance) throws GameParseException
	{
		throw new GameParseException("chance not allowed for use with RulesAttachments, instead use it with Triggers or PoliticalActions" + thisErrorMsg());
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setObjectiveValue(final String value)
	{
		m_objectiveValue = getInt(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setObjectiveValue(final Integer value)
	{
		m_objectiveValue = value;
	}
	
	public int getObjectiveValue()
	{
		return m_objectiveValue;
	}
	
	public void resetObjectiveValue()
	{
		m_objectiveValue = 0;
	}
	
	/**
	 * Internal use only, is not set by xml or property utils.
	 * Is used to determine the number of territories we need to satisfy a specific territory based condition check.
	 * It is set multiple times during each check [isSatisfied], as there might be multiple types of territory checks being done. So it is just a temporary value.
	 * 
	 * @param value
	 */
	@InternalDoNotExport
	protected void setTerritoryCount(final String value)
	{
		if (value.equals("each"))
		{
			m_territoryCount = 1;
			m_countEach = true;
		}
		else
			m_territoryCount = getInt(value);
	}
	
	public int getTerritoryCount()
	{
		return m_territoryCount;
	}
	
	/**
	 * Used to determine if there is a multiple on this national objective (if the user specified 'each' in the count.
	 * For example, you may want to have the player receive 3 PUs for controlling each territory, in a list of territories.
	 * 
	 * @return
	 */
	public int getEachMultiple()
	{
		if (!getCountEach())
			return 1;
		return m_eachMultiple;
	}
	
	protected boolean getCountEach()
	{
		return m_countEach;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUses(final String s)
	{
		m_uses = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUses(final Integer u)
	{
		m_uses = u;
	}
	
	/**
	 * "uses" on RulesAttachments apply ONLY to giving money (PUs) to the player, they do NOT apply to the condition, and therefore should not be tested for in isSatisfied.
	 * 
	 * @return
	 */
	public int getUses()
	{
		return m_uses;
	}
	
	public void resetUses()
	{
		m_uses = -1;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setSwitch(final String value)
	{
		m_switch = getBool(value);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setSwitch(final Boolean value)
	{
		m_switch = value;
	}
	
	public boolean getSwitch()
	{
		return m_switch;
	}
	
	public void resetSwitch()
	{
		m_switch = true;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setGameProperty(final String value)
	{
		m_gameProperty = value;
	}
	
	public String getGameProperty()
	{
		return m_gameProperty;
	}
	
	public boolean getGamePropertyState(final GameData data)
	{
		if (m_gameProperty == null)
			return false;
		return data.getProperties().get(m_gameProperty, false);
	}
	
	public void resetGameProperty()
	{
		m_gameProperty = null;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTurns(final String turns) throws GameParseException
	{
		if (turns == null)
		{
			m_turns = null;
			return;
		}
		m_turns = new HashMap<Integer, Integer>();
		final String[] s = turns.split(":");
		if (s.length < 1)
			throw new GameParseException("Empty turn list" + thisErrorMsg());
		for (final String subString : s)
		{
			int start, end;
			try
			{
				start = getInt(subString);
				end = start;
			} catch (final Exception e)
			{
				final String[] s2 = subString.split("-");
				if (s2.length != 2)
					throw new GameParseException("Invalid syntax for turn range, must be 'int-int'" + thisErrorMsg());
				start = getInt(s2[0]);
				if (s2[1].equals("+"))
				{
					end = Integer.MAX_VALUE;
				}
				else
					end = getInt(s2[1]);
			}
			final Integer t = Integer.valueOf(start);
			final Integer u = Integer.valueOf(end);
			m_turns.put(t, u);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTurns(final HashMap<Integer, Integer> value)
	{
		m_turns = value;
	}
	
	public HashMap<Integer, Integer> getTurns()
	{
		return m_turns;
	}
	
	public void resetTurns()
	{
		m_turns = null;
	}
	
	protected boolean checkTurns(final GameData data)
	{
		final int turn = data.getSequence().getRound();
		for (final Integer t : m_turns.keySet())
		{
			if (turn >= t && turn <= m_turns.get(t))
				return true;
		}
		return false;
	}
	
	/**
	 * Takes a string like "original", "originalNoWater", "enemy", "controlled", "controlledNoWater", "all", "map", and turns it into an actual list of territories.
	 * Also sets territoryCount.
	 * 
	 * @author veqryn
	 */
	protected Set<Territory> getTerritoriesBasedOnStringName(final String name, final Collection<PlayerID> players, final GameData data)
	{
		final GameMap gameMap = data.getMap();
		if (name.equals("original") || name.equals("enemy")) // get all originally owned territories
		{
			final Set<Territory> originalTerrs = new HashSet<Territory>();
			for (final PlayerID player : players)
			{
				originalTerrs.addAll(OriginalOwnerTracker.getOriginallyOwned(data, player));
			}
			setTerritoryCount(String.valueOf(originalTerrs.size()));
			return originalTerrs;
		}
		else if (name.equals("originalNoWater")) // get all originally owned territories, but no water or impassibles
		{
			final Set<Territory> originalTerrs = new HashSet<Territory>();
			for (final PlayerID player : players)
			{
				originalTerrs.addAll(Match.getMatches(OriginalOwnerTracker.getOriginallyOwned(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player, data))); // TODO: does this account for occupiedTerrOf???
			}
			setTerritoryCount(String.valueOf(originalTerrs.size()));
			return originalTerrs;
		}
		else if (name.equals("controlled"))
		{
			final Set<Territory> ownedTerrs = new HashSet<Territory>();
			for (final PlayerID player : players)
			{
				ownedTerrs.addAll(gameMap.getTerritoriesOwnedBy(player));
			}
			setTerritoryCount(String.valueOf(ownedTerrs.size()));
			return ownedTerrs;
		}
		else if (name.equals("controlledNoWater"))
		{
			final Set<Territory> ownedTerrsNoWater = new HashSet<Territory>();
			for (final PlayerID player : players)
			{
				ownedTerrsNoWater.addAll(Match.getMatches(gameMap.getTerritoriesOwnedBy(player), Matches.TerritoryIsNotImpassableToLandUnits(player, data)));
			}
			setTerritoryCount(String.valueOf(ownedTerrsNoWater.size()));
			return ownedTerrsNoWater;
		}
		else if (name.equals("all"))
		{
			final Set<Territory> allTerrs = new HashSet<Territory>();
			for (final PlayerID player : players)
			{
				allTerrs.addAll(gameMap.getTerritoriesOwnedBy(player));
				allTerrs.addAll(OriginalOwnerTracker.getOriginallyOwned(data, player));
			}
			setTerritoryCount(String.valueOf(allTerrs.size()));
			return allTerrs;
		}
		else if (name.equals("map"))
		{
			final Set<Territory> allTerrs = new HashSet<Territory>(gameMap.getTerritories());
			setTerritoryCount(String.valueOf(allTerrs.size()));
			return allTerrs;
		}
		else
		{ // The list just contained 1 territory
			final Set<Territory> terr = new HashSet<Territory>();
			final Territory t = data.getMap().getTerritory(name);
			if (t == null)
				throw new IllegalStateException("No territory called:" + name + thisErrorMsg());
			terr.add(t);
			setTerritoryCount(String.valueOf(1));
			return terr;
		}
	}
	
	/**
	 * Takes the raw data from the xml, and turns it into an actual territory list.
	 * Will also set territoryCount.
	 * 
	 * @author veqryn
	 * @throws GameParseException
	 */
	protected Set<Territory> getTerritoryListBasedOnInputFromXML(final String[] terrs, final Collection<PlayerID> players, final GameData data)
	{
		// If there's only 1, it might be a 'group' (original, controlled, controlledNoWater, all)
		if (terrs.length == 1)
		{
			return getTerritoriesBasedOnStringName(terrs[0], players, data);
		}
		else if (terrs.length == 2)
		{
			if (!terrs[1].equals("controlled") && !terrs[1].equals("controlledNoWater") && !terrs[1].equals("original") && !terrs[1].equals("originalNoWater") && !terrs[1].equals("all")
						&& !terrs[1].equals("map") && !terrs[1].equals("enemy"))
			{
				return getListedTerritories(terrs, true, true); // Get the list of territories
			}
			else
			{
				final Set<Territory> rVal = getTerritoriesBasedOnStringName(terrs[1], players, data);
				setTerritoryCount(String.valueOf(terrs[0])); // set it a second time, since getTerritoriesBasedOnStringName also sets it (so do it after the method call).
				return rVal;
			}
		}
		else
		{
			return getListedTerritories(terrs, true, true); // Get the list of territories
		}
	}
	
	protected void validateNames(final String[] terrList) throws GameParseException
	{
		/*if (terrList != null && (!terrList.equals("controlled") && !terrList.equals("controlledNoWater") && !terrList.equals("original") && !terrList.equals("originalNoWater") && !terrList.equals("all") && !terrList.equals("map") && !terrList.equals("enemy")))
		{
			if (terrList.length != 2)
				getListedTerritories(terrList);
			else if (terrList.length == 2 && (!terrList[1].equals("controlled") && !terrList[1].equals("controlledNoWater") && !terrList[1].equals("original") && !terrList.equals("originalNoWater") && !terrList[1].equals("all") && !terrList[1].equals("map") && !terrList[1].equals("enemy")))
				getListedTerritories(terrList);
		}*/
		if (terrList != null && terrList.length > 0)
			getListedTerritories(terrList, true, true);
		// removed checks for length & group commands because it breaks the setTerritoryCount feature.
	}
	
	/**
	 * Validate that all listed territories actually exist. Will return an empty list of territories if sent a list that is empty or contains only a "" string.
	 * 
	 * @param list
	 * @return
	 * @throws GameParseException
	 */
	public Set<Territory> getListedTerritories(final String[] list, final boolean testFirstItemForCount, final boolean mustSetTerritoryCount)
	{
		final Set<Territory> rVal = new HashSet<Territory>();
		// this list is null, empty, or contains "", so return a blank list of territories
		if (list == null || list.length == 0 || (list.length == 1 && (list[0] == null || list[0].length() == 0)))
			return rVal;
		boolean haveSetCount = false;
		for (int i = 0; i < list.length; i++)
		{
			final String name = list[i];
			if (testFirstItemForCount && i == 0)
			{
				// See if the first entry contains the number of territories needed to meet the criteria
				try
				{
					// check if this is an integer, and if so set territory count
					getInt(name);
					if (mustSetTerritoryCount)
					{
						haveSetCount = true;
						setTerritoryCount(name);
					}
					continue;
				} catch (final Exception e)
				{
				}
			}
			if (name.equals("each"))
			{
				m_countEach = true;
				if (mustSetTerritoryCount)
				{
					haveSetCount = true;
					setTerritoryCount(String.valueOf(1));
				}
				continue;
			}
			// Skip looking for the territory if the original list contains one of the 'group' commands
			if (name.equals("controlled") || name.equals("controlledNoWater") || name.equals("original") || name.equals("originalNoWater") || name.equals("all")
						|| name.equals("map") || name.equals("enemy"))
				break;
			// Validate all territories exist
			final Territory territory = getData().getMap().getTerritory(name);
			if (territory == null)
				throw new IllegalStateException("No territory called:" + name + thisErrorMsg());
			rVal.add(territory);
		}
		if (mustSetTerritoryCount && !haveSetCount)
		{
			setTerritoryCount(String.valueOf(rVal.size())); // if we have not set it, then set it to be the size of this list
		}
		return rVal;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		super.validate(data);
		// TODO Auto-generated method stub
	}
}
