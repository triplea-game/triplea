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
 * TerritoryAttachment.java
 * 
 * Created on November 8, 2001, 3:08 PM
 */
package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.BattleRecordsList;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Kevin Comcowich and Veqryn (Mark Christopher Duncan)
 * @version 1.2
 */
public class RulesAttachment extends AbstractPlayerRulesAttachment implements ICondition
{
	private static final long serialVersionUID = 7301965634079412516L;
	
	private List<TechAdvance> m_techs = null; // condition for having techs
	@InternalDoNotExport
	private int m_techCount = -1; // Do Not Export (do not include in IAttachment).
	
	private final List<String> m_relationship = new ArrayList<String>(); // condition for having specific relationships
	private Set<PlayerID> m_atWarPlayers = null; // condition for being at war
	@InternalDoNotExport
	private int m_atWarCount = -1; // Do Not Export (do not include in IAttachment).
	private String m_destroyedTUV = null; // condition for having destroyed at least X enemy non-neutral TUV (total unit value) [according to the prices the defender pays for the units]
	
	// these next 9 variables use m_territoryCount for determining the number needed.
	private String[] m_alliedOwnershipTerritories; // ownership related
	private String[] m_directOwnershipTerritories;
	private String[] m_alliedExclusionTerritories; // exclusion of units
	private String[] m_directExclusionTerritories;
	private String[] m_enemyExclusionTerritories;
	private String[] m_enemySurfaceExclusionTerritories;
	private String[] m_directPresenceTerritories; // presence of units
	private String[] m_alliedPresenceTerritories;
	private String[] m_enemyPresenceTerritories;
	private final IntegerMap<String> m_unitPresence = new IntegerMap<String>(); // used with above 3 to determine the type of unit that must be present
	
	/** Creates new RulesAttachment */
	public RulesAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDestroyedTUV(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length != 2)
			throw new GameParseException("destroyedTUV must have 2 fields, value=currentRound/allRounds, count= the amount of TUV that this player must destroy");
		final int i = getInt(s[0]);
		if (i < -1)
			throw new GameParseException("destroyedTUV count can not be less than -1 [with -1 meaning the condition is not active]");
		if (!(s[1].equals("currentRound") || s[1].equals("allRounds")))
			throw new GameParseException("destroyedTUV value must be currentRound or allRounds");
		m_destroyedTUV = value;
	}
	
	public String getDestroyedTUV()
	{
		return m_destroyedTUV;
	}
	
	/**
	 * Condition to check if a certain relationship exists between 2 players.
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 *            should be a string containing: "player:player:relationship"
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setRelationship(final String value) throws GameParseException
	{
		final String[] s = value.split(":");
		if (s.length < 3 || s.length > 4)
			throw new GameParseException("Rules & Conditions: relationship should have value=\"playername1:playername2:relationshiptype:numberOfRoundsExisting\"");
		if (getData().getPlayerList().getPlayerID(s[0]) == null)
			throw new GameParseException("Rules & Conditions: playername: " + s[0] + " isn't valid in condition with relationship: " + value + " for RulesAttachment " + getName());
		if (getData().getPlayerList().getPlayerID(s[1]) == null)
			throw new GameParseException("Rules & Conditions: playername: " + s[1] + " isn't valid in condition with relationship: " + value + " for RulesAttachment " + getName());
		if (!(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED) || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL) || s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR) || Matches
					.isValidRelationshipName(getData()).match(s[2])))
			throw new GameParseException("Rules & Conditions: relationship: " + s[2] + " isn't valid in condition with relationship: " + value + " for RulesAttachment " + getName());
		if (s.length == 4 && Integer.parseInt(s[3]) < -1)
			throw new GameParseException("Rules & Conditions: numberOfRoundsExisting should be a number between -1 and 100000.  -1 should be default value if you don't know what to put");
		m_relationship.add((s.length == 3) ? (value + ":-1") : value);
	}
	
	public List<String> getRelationship()
	{
		return m_relationship;
	}
	
	public void clearRelationship()
	{
		m_relationship.clear();
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAlliedOwnershipTerritories(final String value)
	{
		m_alliedOwnershipTerritories = value.split(":");
		validateNames(m_alliedOwnershipTerritories);
	}
	
	public String[] getAlliedOwnershipTerritories()
	{
		return m_alliedOwnershipTerritories;
	}
	
	// exclusion types = controlled, controlledNoWater, original, all, or list
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAlliedExclusionTerritories(final String value)
	{
		m_alliedExclusionTerritories = value.split(":");
		validateNames(m_alliedExclusionTerritories);
	}
	
	public String[] getAlliedExclusionTerritories()
	{
		return m_alliedExclusionTerritories;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDirectExclusionTerritories(final String value)
	{
		m_directExclusionTerritories = value.split(":");
		validateNames(m_directExclusionTerritories);
	}
	
	public String[] getDirectExclusionTerritories()
	{
		return m_directExclusionTerritories;
	}
	
	// exclusion types = original or list
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setEnemyExclusionTerritories(final String value)
	{
		m_enemyExclusionTerritories = value.split(":");
		validateNames(m_enemyExclusionTerritories);
	}
	
	public String[] getEnemyExclusionTerritories()
	{
		return m_enemyExclusionTerritories;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDirectPresenceTerritories(final String value)
	{
		m_directPresenceTerritories = value.split(":");
		validateNames(m_directPresenceTerritories);
	}
	
	public String[] getDirectPresenceTerritories()
	{
		return m_directPresenceTerritories;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAlliedPresenceTerritories(final String value)
	{
		m_alliedPresenceTerritories = value.split(":");
		validateNames(m_alliedPresenceTerritories);
	}
	
	public String[] getAlliedPresenceTerritories()
	{
		return m_alliedPresenceTerritories;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setEnemyPresenceTerritories(final String value)
	{
		m_enemyPresenceTerritories = value.split(":");
		validateNames(m_enemyPresenceTerritories);
	}
	
	public String[] getEnemyPresenceTerritories()
	{
		return m_enemyPresenceTerritories;
	}
	
	// exclusion types = original or list
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setEnemySurfaceExclusionTerritories(final String value)
	{
		m_enemySurfaceExclusionTerritories = value.split(":");
		validateNames(m_enemySurfaceExclusionTerritories);
	}
	
	public String[] getEnemySurfaceExclusionTerritories()
	{
		return m_enemySurfaceExclusionTerritories;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setDirectOwnershipTerritories(final String value)
	{
		m_directOwnershipTerritories = value.split(":");
		validateNames(m_directOwnershipTerritories);
	}
	
	public String[] getDirectOwnershipTerritories()
	{
		return m_directOwnershipTerritories;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setUnitPresence(String value)
	{
		final String[] s = value.split(":");
		if (s.length <= 1)
			throw new IllegalStateException("Rules Attachments: unitPresence must have at least 2 fields. Format value=unit1 count=number, or value=unit1:unit2:unit3 count=number");
		final int n = getInt(s[0]);
		if (n <= 0)
			throw new IllegalStateException("Rules Attachments: unitPresence must be a positive integer");
		for (int i = 1; i < s.length; i++)
		{
			final String unitTypeToProduce = s[i];
			// validate that this unit exists in the xml
			final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
			if (ut == null && !(unitTypeToProduce.equals("any") || unitTypeToProduce.equals("ANY")))
				throw new IllegalStateException("Rules Attachments: No unit called: " + unitTypeToProduce);
		}
		value = value.replaceFirst(s[0] + ":", "");
		m_unitPresence.put(value, n);
	}
	
	public IntegerMap<String> getUnitPresence()
	{
		return m_unitPresence;
	}
	
	public void clearUnitPresence()
	{
		m_unitPresence.clear();
	}
	
	public int getAtWarCount()
	{
		return m_atWarCount;
	}
	
	public int getTechCount()
	{
		return m_techCount;
	}
	
	public Set<PlayerID> getAtWarPlayers()
	{
		return m_atWarPlayers;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAtWarPlayers(final String players) throws GameParseException
	{
		final String[] s = players.split(":");
		int count = -1;
		if (s.length < 1)
			throw new GameParseException("Rules & Conditions: Empty enemy list");
		try
		{
			count = getInt(s[0]);
			m_atWarCount = count;
		} catch (final Exception e)
		{
			m_atWarCount = 0;
		}
		if (s.length < 1 || s.length == 1 && count != -1)
			throw new GameParseException("Rules & Conditions: Empty enemy list");
		m_atWarPlayers = new HashSet<PlayerID>();
		for (int i = count == -1 ? 0 : 1; i < s.length; i++)
		{
			final PlayerID player = getData().getPlayerList().getPlayerID(s[i]);
			if (player == null)
				throw new GameParseException("Rules & Conditions: Could not find player. name:" + s[i]);
			m_atWarPlayers.add(player);
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setTechs(final String techs) throws GameParseException
	{
		{
			final String[] s = techs.split(":");
			int count = -1;
			if (s.length < 1)
				throw new GameParseException("Rules & Conditions: Empty tech list");
			try
			{
				count = getInt(s[0]);
				m_techCount = count;
			} catch (final Exception e)
			{
				m_techCount = 0;
			}
			if (s.length < 1 || s.length == 1 && count != -1)
				throw new GameParseException("Rules & Conditions: Empty tech list");
			m_techs = new ArrayList<TechAdvance>();
			for (int i = count == -1 ? 0 : 1; i < s.length; i++)
			{
				TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(s[i]);
				if (ta == null)
					ta = getData().getTechnologyFrontier().getAdvanceByName(s[i]);
				if (ta == null)
					throw new GameParseException("Rules & Conditions: Technology not found :" + s);
				m_techs.add(ta);
			}
		}
	}
	
	@Override
	public boolean isSatisfied(final HashMap<ICondition, Boolean> testedConditions)
	{
		if (testedConditions == null)
			throw new IllegalStateException("testedConditions can not be null");
		if (!testedConditions.containsKey(this))
			throw new IllegalStateException("testedConditions is incomplete and does not contain " + this.toString());
		return testedConditions.get(this);
	}
	
	@Override
	public boolean isSatisfied(HashMap<ICondition, Boolean> testedConditions, final IDelegateBridge aBridge)
	{
		if (testedConditions != null)
		{
			if (testedConditions.containsKey(this))
				return testedConditions.get(this);
		}
		boolean objectiveMet = true;
		final PlayerID player = (PlayerID) getAttatchedTo();
		final GameData data = aBridge.getData();
		//
		// check meta conditions (conditions which hold other conditions)
		//
		if (objectiveMet && m_conditions.size() > 0)
		{
			if (testedConditions == null)
				testedConditions = testAllConditionsRecursive(getAllConditionsRecursive(new HashSet<ICondition>(m_conditions), null), null, aBridge);
			objectiveMet = areConditionsMet(new ArrayList<ICondition>(m_conditions), testedConditions, m_conditionType);
		}
		//
		// check turn limits
		//
		if (objectiveMet && m_turns != null)
			objectiveMet = checkTurns(data);
		//
		// Check for unit presence (Veqryn)
		//
		if (objectiveMet && getDirectPresenceTerritories() != null)
		{
			// Get the listed territories
			String[] terrs = getDirectPresenceTerritories();
			String value = new String();
			value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			// create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitPresence(objectiveMet, getListedTerritories(terrs), "direct", getTerritoryCount(), player, data);
		}
		//
		// Check for unit presence (Veqryn)
		//
		if (objectiveMet && getAlliedPresenceTerritories() != null)
		{
			// Get the listed territories
			String[] terrs = getAlliedPresenceTerritories();
			String value = new String();
			value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			// create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitPresence(objectiveMet, getListedTerritories(terrs), "allied", getTerritoryCount(), player, data);
		}
		//
		// Check for unit presence (Veqryn)
		//
		if (objectiveMet && getEnemyPresenceTerritories() != null)
		{
			// Get the listed territories
			String[] terrs = getEnemyPresenceTerritories();
			String value = new String();
			value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			// create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitPresence(objectiveMet, getListedTerritories(terrs), "enemy", getTerritoryCount(), player, data);
		}
		//
		// Check for direct unit exclusions (veqryn)
		//
		if (objectiveMet && getDirectExclusionTerritories() != null)
		{
			// Get the listed territories
			String[] terrs = getDirectExclusionTerritories();
			String value = new String();
			value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			// create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitExclusions(objectiveMet, getListedTerritories(terrs), "direct", getTerritoryCount(), player, data);
		}
		//
		// Check for allied unit exclusions
		//
		if (objectiveMet && getAlliedExclusionTerritories() != null)
		{
			// Get the listed territories
			String[] terrs = getAlliedExclusionTerritories();
			String value = new String();
			value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			// create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitExclusions(objectiveMet, getListedTerritories(terrs), "allied", getTerritoryCount(), player, data);
		}
		//
		// Check for enemy unit exclusions (ANY UNITS)
		//
		if (objectiveMet && getEnemyExclusionTerritories() != null)
		{
			// Get the listed territories
			String[] terrs = getEnemyExclusionTerritories();
			String value = new String();
			value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			// create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitExclusions(objectiveMet, getListedTerritories(terrs), "enemy", getTerritoryCount(), player, data);
		}
		// Check for enemy unit exclusions (SURFACE UNITS with ATTACK POWER)
		if (objectiveMet && getEnemySurfaceExclusionTerritories() != null)
		{
			// Get the listed territories
			String[] terrs = getEnemySurfaceExclusionTerritories();
			String value = new String();
			value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			// create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitExclusions(objectiveMet, getListedTerritories(terrs), "enemy_surface", getTerritoryCount(), player, data);
		}
		//
		// Check for Territory Ownership rules
		//
		if (objectiveMet && getAlliedOwnershipTerritories() != null)
		{
			// Get the listed territories
			String[] terrs = getAlliedOwnershipTerritories();
			String value = new String();
			if (terrs.length == 1)
			{
				if (terrs[0].equals("original"))
				{
					final Collection<PlayerID> players = data.getPlayerList().getPlayers();
					for (final PlayerID currPlayer : players)
					{
						if (data.getRelationshipTracker().isAllied(currPlayer, player))
						{
							value = value + ":" + getTerritoryListAsStringBasedOnInputFromXML(terrs, currPlayer, data);
						}
					}
					// Remove the leading colon
					value = value.replaceFirst(":", "");
				}
				else if (terrs[0].equals("enemy"))
				{
					final Collection<PlayerID> players = data.getPlayerList().getPlayers();
					for (final PlayerID currPlayer : players)
					{
						if (!data.getRelationshipTracker().isAllied(currPlayer, player))
						{
							value = value + ":" + getTerritoryListAsStringBasedOnInputFromXML(terrs, currPlayer, data);
						}
					}
					// Remove the leading colon
					value = value.replaceFirst(":", "");
				}
				else
					value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			}
			else if (terrs.length == 2)
			{
				if (terrs[1].equals("original"))
				{
					final Collection<PlayerID> players = data.getPlayerList().getPlayers();
					for (final PlayerID currPlayer : players)
					{
						if (data.getRelationshipTracker().isAllied(currPlayer, player))
						{
							value = value + ":" + getTerritoryListAsStringBasedOnInputFromXML(terrs, currPlayer, data);
						}
					}
					// Remove the leading colon
					value = value.replaceFirst(":", "");
				}
				else if (terrs[1].equals("enemy"))
				{
					final Collection<PlayerID> players = data.getPlayerList().getPlayers();
					for (final PlayerID currPlayer : players)
					{
						if (!data.getRelationshipTracker().isAllied(currPlayer, player))
						{
							value = value + ":" + getTerritoryListAsStringBasedOnInputFromXML(terrs, currPlayer, data);
						}
					}
					// Remove the leading colon
					value = value.replaceFirst(":", "");
				}
				else
					value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			}
			else
				value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			// create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkAlliedOwnership(objectiveMet, getListedTerritories(terrs), getTerritoryCount(), player, data);
		}
		//
		// Check for Direct Territory Ownership rules
		//
		if (objectiveMet && getDirectOwnershipTerritories() != null)
		{
			// Get the listed territories
			String[] terrs = getDirectOwnershipTerritories();
			String value = new String();
			if (terrs.length == 1)
			{
				if (terrs[0].equals("original"))
				{
					value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
				}
				else if (terrs[0].equals("enemy"))
				{
					final Collection<PlayerID> players = data.getPlayerList().getPlayers();
					for (final PlayerID currPlayer : players)
					{
						if (!data.getRelationshipTracker().isAllied(currPlayer, player))
						{
							value = value + ":" + getTerritoryListAsStringBasedOnInputFromXML(terrs, currPlayer, data);
						}
					}
					// Remove the leading colon
					value = value.replaceFirst(":", "");
				}
				else
					value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			}
			else if (terrs.length == 2)
			{
				if (terrs[1].equals("original"))
				{
					value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
				}
				else if (terrs[1].equals("enemy"))
				{
					final Collection<PlayerID> players = data.getPlayerList().getPlayers();
					for (final PlayerID currPlayer : players)
					{
						if (!data.getRelationshipTracker().isAllied(currPlayer, player))
						{
							value = value + ":" + getTerritoryListAsStringBasedOnInputFromXML(terrs, currPlayer, data);
						}
					}
					// Remove the leading colon
					value = value.replaceFirst(":", "");
				}
				else
					value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			}
			else
				value = getTerritoryListAsStringBasedOnInputFromXML(terrs, player, data);
			// create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkDirectOwnership(objectiveMet, getListedTerritories(terrs), getTerritoryCount(), player);
		}
		if (objectiveMet && getAtWarPlayers() != null)
		{
			objectiveMet = checkAtWar(player, getAtWarPlayers(), getAtWarCount(), data);
		}
		if (objectiveMet && m_techs != null)
		{
			objectiveMet = checkTechs(player, data);
		}
		// check for relationships
		if (objectiveMet && m_relationship.size() > 0)
		{
			objectiveMet = checkRelationships();
		}
		// check for battle stats
		if (objectiveMet && m_destroyedTUV != null)
		{
			final String[] s = m_destroyedTUV.split(":");
			final int requiredDestroyedTUV = getInt(s[0]);
			if (requiredDestroyedTUV >= 0)
			{
				final boolean justCurrentRound = s[1].equals("currentRound");
				final int destroyedTUVforThisRoundSoFar = BattleRecordsList.getTUVdamageCausedByPlayer(player, data.getBattleRecordsList(),
							0, data.getSequence().getRound(), justCurrentRound, false);
				if (requiredDestroyedTUV > destroyedTUVforThisRoundSoFar)
					objectiveMet = false;
				if (getCountEach())
					m_eachMultiple = destroyedTUVforThisRoundSoFar;
			}
		}
		
		// "chance" should ALWAYS be checked last!
		final int hitTarget = getInt(m_chance.split(":")[0]);
		final int diceSides = getInt(m_chance.split(":")[1]);
		if (objectiveMet && hitTarget != diceSides)
		{
			final int rollResult = aBridge.getRandom(diceSides, "Attempting the Condition: " + MyFormatter.attachmentNameToText(this.getName())) + 1;
			objectiveMet = rollResult <= hitTarget;
			final String notificationMessage = "Rolling (" + hitTarget + " out of " + diceSides + ") result: " + rollResult + " = " + (objectiveMet ? "Success!" : "Failure!") + " (for "
						+ MyFormatter.attachmentNameToText(this.getName()) + ")";
			aBridge.getHistoryWriter().startEvent(notificationMessage);
			((ITripleaPlayer) aBridge.getRemote(aBridge.getPlayerID())).reportMessage(notificationMessage, notificationMessage);
		}
		
		return objectiveMet != m_invert;
	}
	
	/**
	 * checks if all relationship requirements are set
	 * 
	 * @return whether all relationships as are required are set correctly.
	 */
	private boolean checkRelationships()
	{
		for (final String aRelationCheck : m_relationship)
		{
			final String[] relationCheck = aRelationCheck.split(":");
			final PlayerID p1 = getData().getPlayerList().getPlayerID(relationCheck[0]);
			final PlayerID p2 = getData().getPlayerList().getPlayerID(relationCheck[1]);
			final int relationshipsExistance = Integer.parseInt(relationCheck[3]);
			final Relationship currentRelationship = getData().getRelationshipTracker().getRelationship(p1, p2);
			final RelationshipType currentRelationshipType = currentRelationship.getRelationshipType();
			if (!relationShipExistsLongEnnough(currentRelationship, relationshipsExistance))
			{
				return false;
			}
			if (!(relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED) && Matches.RelationshipTypeIsAllied.match(currentRelationshipType)
						|| relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL) && Matches.RelationshipTypeIsNeutral.match(currentRelationshipType)
						|| relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR) && Matches.RelationshipTypeIsAtWar.match(currentRelationshipType) || currentRelationshipType
						.equals(getData().getRelationshipTypeList().getRelationshipType(relationCheck[2]))))
				return false;
		}
		return true;
	}
	
	private boolean relationShipExistsLongEnnough(final Relationship r, final int relationshipsExistance)
	{
		int roundCurrentRelationshipWasCreated = r.getRoundCreated();
		roundCurrentRelationshipWasCreated += games.strategy.triplea.Properties.getRelationshipsLastExtraRounds(getData());
		if (getData().getSequence().getRound() - roundCurrentRelationshipWasCreated < relationshipsExistance)
			return false;
		return true;
	}
	
	/**
	 * Checks for the collection of territories to see if they have units owned by the exclType alliance.
	 */
	private boolean checkUnitPresence(boolean satisfied, final Collection<Territory> Territories, final String exclType, final int numberNeeded, final PlayerID player, final GameData data)
	{
		int numberMet = 0;
		satisfied = false;
		boolean useSpecific = false;
		if (getUnitPresence() != null && !getUnitPresence().keySet().isEmpty())
			useSpecific = true;
		for (final Territory terr : Territories)
		{
			final Collection<Unit> allUnits = terr.getUnits().getUnits();
			if (exclType.equals("direct"))
			{
				allUnits.removeAll(Match.getMatches(allUnits, Matches.unitIsOwnedBy(player).invert()));
			}
			else if (exclType.equals("allied"))
			{
				allUnits.retainAll(Match.getMatches(allUnits, Matches.alliedUnit(player, data)));
			}
			else if (exclType.equals("enemy"))
			{
				allUnits.retainAll(Match.getMatches(allUnits, Matches.enemyUnit(player, data)));
			}
			else
				return false;
			if (allUnits.size() > 0)
			{
				if (!useSpecific)
				{
					numberMet += 1;
					if (numberMet >= numberNeeded)
					{
						satisfied = true;
						if (!getCountEach())
							break;
					}
				}
				else if (useSpecific)
				{
					final IntegerMap<String> unitComboMap = getUnitPresence();
					final Set<String> unitCombos = unitComboMap.keySet();
					boolean hasEnough = false;
					for (final String uc : unitCombos)
					{
						final int unitsNeeded = unitComboMap.getInt(uc);
						if (uc == null || uc.equals("ANY") || uc.equals("any"))
						{
							if (allUnits.size() >= unitsNeeded)
								hasEnough = true;
							else
								hasEnough = false;
						}
						else
						{
							final Set<UnitType> typesAllowed = data.getUnitTypeList().getUnitTypes(uc.split(":"));
							if (Match.getMatches(allUnits, Matches.unitIsOfTypes(typesAllowed)).size() >= unitsNeeded)
								hasEnough = true;
							else
								hasEnough = false;
						}
						if (!hasEnough)
							break;
					}
					if (hasEnough)
					{
						numberMet += 1;
						if (numberMet >= numberNeeded)
						{
							satisfied = true;
							if (!getCountEach())
								break;
						}
					}
				}
			}
		}
		if (getCountEach())
			m_eachMultiple = numberMet;
		return satisfied;
	}
	
	/**
	 * Checks for the collection of territories to see if they have units owned by the exclType alliance.
	 * It doesn't yet threshold the data
	 */
	private boolean checkUnitExclusions(boolean satisfied, final Collection<Territory> Territories, final String exclType, final int numberNeeded, final PlayerID player, final GameData data)
	{
		int numberMet = 0;
		satisfied = false;
		boolean useSpecific = false;
		if (getUnitPresence() != null && !getUnitPresence().keySet().isEmpty())
			useSpecific = true;
		final Iterator<Territory> ownedTerrIter = Territories.iterator();
		// Go through the owned territories and see if there are any units owned by allied/enemy based on exclType
		while (ownedTerrIter.hasNext())
		{
			// get all the units in the territory
			final Territory terr = ownedTerrIter.next();
			final Collection<Unit> allUnits = terr.getUnits().getUnits();
			if (exclType.equals("allied"))
			{ // any allied units in the territory
				allUnits.removeAll(Match.getMatches(allUnits, Matches.unitIsOwnedBy(player)));
				allUnits.retainAll(Match.getMatches(allUnits, Matches.alliedUnit(player, data)));
			}
			else if (exclType.equals("direct"))
			{
				allUnits.removeAll(Match.getMatches(allUnits, Matches.unitIsOwnedBy(player).invert()));
			}
			else if (exclType.equals("enemy"))
			{ // any enemy units in the territory
				allUnits.retainAll(Match.getMatches(allUnits, Matches.enemyUnit(player, data)));
			}
			else if (exclType.equals("enemy_surface"))
			{ // any enemy units (not trn/sub) in the territory
				allUnits.retainAll(Match.getMatches(allUnits, new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsNotSub, Matches.UnitIsNotTransportButCouldBeCombatTransport)));
			}
			else
				return false;
			if (allUnits.size() == 0)
			{
				numberMet += 1;
				if (numberMet >= numberNeeded)
				{
					satisfied = true;
					if (!getCountEach())
						break;
				}
			}
			else if (useSpecific)
			{
				final IntegerMap<String> unitComboMap = getUnitPresence();
				final Set<String> unitCombos = unitComboMap.keySet();
				boolean hasLess = false;
				for (final String uc : unitCombos)
				{
					final int unitsMax = unitComboMap.getInt(uc);
					if (uc == null || uc.equals("ANY") || uc.equals("any"))
					{
						if (allUnits.size() <= unitsMax)
							hasLess = true;
						else
							hasLess = false;
					}
					else
					{
						final Set<UnitType> typesAllowed = data.getUnitTypeList().getUnitTypes(uc.split(":"));
						if (Match.getMatches(allUnits, Matches.unitIsOfTypes(typesAllowed)).size() <= unitsMax)
							hasLess = true;
						else
							hasLess = false;
					}
					if (!hasLess)
						break;
				}
				if (hasLess)
				{
					numberMet += 1;
					if (numberMet >= numberNeeded)
					{
						satisfied = true;
						if (!getCountEach())
							break;
					}
				}
			}
		}
		if (getCountEach())
			m_eachMultiple = numberMet;
		return satisfied;
	}
	
	/**
	 * Checks for allied ownership of the collection of territories. Once the needed number threshold is reached, the satisfied flag is set
	 * to true and returned
	 */
	private boolean checkAlliedOwnership(boolean satisfied, final Collection<Territory> listedTerrs, final int numberNeeded, final PlayerID player, final GameData data)
	{
		int numberMet = 0;
		satisfied = false;
		for (final Territory listedTerr : listedTerrs)
		{
			// if the territory owner is an ally
			if (data.getRelationshipTracker().isAllied(listedTerr.getOwner(), player))
			{
				numberMet += 1;
				if (numberMet >= numberNeeded)
				{
					satisfied = true;
					if (!getCountEach())
						break;
				}
			}
		}
		if (getCountEach())
			m_eachMultiple = numberMet;
		return satisfied;
	}
	
	/**
	 * astabada
	 * Checks for direct ownership of the collection of territories. Once the needed number threshold is reached, the satisfied flag is set
	 * to true and returned
	 */
	private boolean checkDirectOwnership(boolean satisfied, final Collection<Territory> listedTerrs, final int numberNeeded, final PlayerID player)
	{
		int numberMet = 0;
		satisfied = false;
		for (final Territory listedTerr : listedTerrs)
		{
			// if the territory owner is an ally
			if (listedTerr.getOwner() == player)
			{
				numberMet += 1;
				if (numberMet >= numberNeeded)
				{
					satisfied = true;
					if (!getCountEach())
						break;
				}
			}
		}
		if (getCountEach())
			m_eachMultiple = numberMet;
		return satisfied;
	}
	
	private boolean checkAtWar(final PlayerID player, final Set<PlayerID> enemies, final int count, final GameData data)
	{
		int found = 0;
		for (final PlayerID e : enemies)
			if (data.getRelationshipTracker().isAtWar(player, e))
				found++;
		if (count == 0)
			return count == found;
		if (getCountEach())
			m_eachMultiple = found;
		return found >= count;
	}
	
	private boolean checkTechs(final PlayerID player, final GameData data)
	{
		int found = 0;
		for (final TechAdvance a : TechTracker.getTechAdvances(player, data))
			if (m_techs.contains(a))
				found++;
		if (m_techCount == 0)
			return m_techCount == found;
		if (getCountEach())
			m_eachMultiple = found;
		return found >= m_techCount;
	}
	
	/**
	 * Called after the attachment is created.
	 * 
	 * @throws GameParseException
	 *             validation failed
	 */
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		super.validate(data);
		validateNames(m_alliedOwnershipTerritories);
		validateNames(m_enemyExclusionTerritories);
		validateNames(m_enemySurfaceExclusionTerritories);
		validateNames(m_alliedExclusionTerritories);
		validateNames(m_directExclusionTerritories);
		validateNames(m_directOwnershipTerritories);
		validateNames(m_directPresenceTerritories);
		validateNames(m_alliedPresenceTerritories);
		validateNames(m_enemyPresenceTerritories);
	}
}
