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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Kevin Comcowich and Veqryn (Mark Christopher Duncan)
 * @version 1.2
 */
public class RulesAttachment extends AbstractConditionsAttachment implements IConditions
{
	private static final long serialVersionUID = 7301965634079412516L;
	
	// START of National Objective and Condition related variables.
	private boolean m_countEach = false; // determines if we will be counting each for the purposes of m_objectiveValue
	private int m_eachMultiple = 1; // the multiple that will be applied to m_objectiveValue if m_countEach is true
	private int m_objectiveValue = 0; // only used if the attachment begins with "objectiveAttachment"
	private int m_uses = -1; // only matters for objectiveValue, does not affect the condition
	
	private Map<Integer, Integer> m_turns = null; // condition for what turn it is
	
	private List<TechAdvance> m_techs = null; // condition for having techs
	private int m_techCount = -1;
	
	private final List<String> m_relationship = new ArrayList<String>(); // condition for having specific relationships
	private Set<PlayerID> m_atWarPlayers = null; // condition for being at war
	private int m_atWarCount = -1;
	
	private int m_territoryCount = -1; // used with the next 9 Territory conditions to determine the number of territories needed to be valid
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
	// END of National Objective and Condition related variables.
	
	// START of Rules variables. The next 12 variables are related to a "rulesAttatchment" that changes certain rules for the attached player. They are not related to conditions at all.
	private String m_movementRestrictionType = null;
	private String[] m_movementRestrictionTerritories;
	private boolean m_placementAnyTerritory = false; // allows placing units in any owned land
	private boolean m_placementAnySeaZone = false; // allows placing units in any sea by owned land
	private boolean m_placementCapturedTerritory = false; // allows placing units in a captured territory
	private boolean m_unlimitedProduction = false; // turns of the warning to the player when they produce more than they can place
	private boolean m_placementInCapitalRestricted = false; // can only place units in the capital
	private boolean m_dominatingFirstRoundAttack = false; // enemy units will defend at 1
	private boolean m_negateDominatingFirstRoundAttack = false; // negates m_dominatingFirstRoundAttack
	private final IntegerMap<UnitType> m_productionPerXTerritories = new IntegerMap<UnitType>(); // automatically produces 1 unit of a certain type per every X territories owned
	private int m_placementPerTerritory = -1; // stops the user from placing units in any territory that already contains more than this number of owned units
	private int m_maxPlacePerTerritory = -1; // maximum number of units that can be placed in each territory.
	
	// END of Rules variables.
	// It would wreck most map xmls to move the rulesAttatchment's to another class, so don't move them out of here please!
	// However, any new rules attachments should be put into the "PlayerAttachment" class.
	
	/**
	 * Convenience method, will not return objectives and conditions, only the rules attachment (like what China in ww2v3 has).
	 * These attachments returned are not conditions to be tested, they are special rules affecting a player
	 * (for example: being able to produce without factories, or not being able to move out of specific territories).
	 * 
	 * @param player
	 *            PlayerID
	 * @return new rule attachment
	 */
	public static RulesAttachment get(final PlayerID player)
	{
		final RulesAttachment rVal = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (rVal == null)
			throw new IllegalStateException("Rules & Conditions: No rule attachment for:" + player.getName());
		return rVal;
	}
	
	/**
	 * Convenience method, for use with rules attachments, objectives, and condition attachments.
	 * 
	 * @param player
	 *            PlayerID
	 * @param nameOfAttachment
	 *            exact full name of attachment
	 * @return new rule attachment
	 */
	public static RulesAttachment get(final PlayerID player, final String nameOfAttachment)
	{
		final RulesAttachment rVal = (RulesAttachment) player.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("Rules & Conditions: No rule attachment for:" + player.getName() + " with name: " + nameOfAttachment);
		return rVal;
	}
	
	/**
	 * Convenience method, for use returning any rules attachment that begins with "objectiveAttachment"
	 * National Objectives are just conditions that also give money to a player during the end turn delegate. They can be used for testing by triggers as well.
	 * Conditions that do not give money are not prefixed with "objectiveAttachment",
	 * and the trigger attachment that uses these kinds of conditions gets them a different way because they are specifically named inside that trigger.
	 * 
	 * @param player
	 * @param data
	 * @return
	 */
	public static Set<RulesAttachment> getNationalObjectives(final PlayerID player, final GameData data)
	{
		final Set<RulesAttachment> natObjs = new HashSet<RulesAttachment>();
		final Map<String, IAttachment> map = player.getAttachments();
		final Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext())
		{
			final IAttachment attachment = map.get(iter.next());
			final String name = attachment.getName();
			if (name.startsWith(Constants.RULES_OBJECTIVE_PREFIX) && attachment instanceof RulesAttachment)
			{
				natObjs.add((RulesAttachment) attachment);
			}
		}
		return natObjs;
	}
	
	/** Creates new RulesAttachment */
	public RulesAttachment()
	{
	}
	
	public void setObjectiveValue(final String value)
	{
		m_objectiveValue = getInt(value);
	}
	
	public int getObjectiveValue()
	{
		return m_objectiveValue;
	}
	
	/**
	 * Condition to check if a certain relationship exists between 2 players.
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 *            should be a string containing: "player:player:relationship"
	 * @throws GameParseException
	 */
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
	public void setAlliedExclusionTerritories(final String value)
	{
		m_alliedExclusionTerritories = value.split(":");
		validateNames(m_alliedExclusionTerritories);
	}
	
	public String[] getAlliedExclusionTerritories()
	{
		return m_alliedExclusionTerritories;
	}
	
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
	public void setEnemyExclusionTerritories(final String value)
	{
		m_enemyExclusionTerritories = value.split(":");
		validateNames(m_enemyExclusionTerritories);
	}
	
	public String[] getEnemyExclusionTerritories()
	{
		return m_enemyExclusionTerritories;
	}
	
	public void setDirectPresenceTerritories(final String value)
	{
		m_directPresenceTerritories = value.split(":");
		validateNames(m_directPresenceTerritories);
	}
	
	public String[] getDirectPresenceTerritories()
	{
		return m_directPresenceTerritories;
	}
	
	public void setAlliedPresenceTerritories(final String value)
	{
		m_alliedPresenceTerritories = value.split(":");
		validateNames(m_alliedPresenceTerritories);
	}
	
	public String[] getAlliedPresenceTerritories()
	{
		return m_alliedPresenceTerritories;
	}
	
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
	public void setEnemySurfaceExclusionTerritories(final String value)
	{
		m_enemySurfaceExclusionTerritories = value.split(":");
		validateNames(m_enemySurfaceExclusionTerritories);
	}
	
	public String[] getEnemySurfaceExclusionTerritories()
	{
		return m_enemySurfaceExclusionTerritories;
	}
	
	public void setDirectOwnershipTerritories(final String value)
	{
		m_directOwnershipTerritories = value.split(":");
		validateNames(m_directOwnershipTerritories);
	}
	
	public String[] getDirectOwnershipTerritories()
	{
		return m_directOwnershipTerritories;
	}
	
	private void setTerritoryCount(final String value)
	{
		if (value.equals("each"))
		{
			m_territoryCount = 1;
			setCountEach(true);
		}
		else
			m_territoryCount = getInt(value);
	}
	
	public int getTerritoryCount()
	{
		return m_territoryCount;
	}
	
	private void setEachMultiple(final int value)
	{
		m_eachMultiple = value;
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
	
	private void setCountEach(final boolean value)
	{
		m_countEach = value;
	}
	
	private boolean getCountEach()
	{
		return m_countEach;
	}
	
	public void setMovementRestrictionTerritories(final String value)
	{
		m_movementRestrictionTerritories = value.split(":");
		validateNames(m_movementRestrictionTerritories);
	}
	
	public String[] getMovementRestrictionTerritories()
	{
		return m_movementRestrictionTerritories;
	}
	
	public void setMovementRestrictionType(final String value) throws GameParseException
	{
		if (!(value.equals("disallowed") || value.equals("allowed")))
			throw new GameParseException("RulesAttachment: movementRestrictionType must be allowed or disallowed");
		m_movementRestrictionType = value;
	}
	
	public String getMovementRestrictionType()
	{
		return m_movementRestrictionType;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	public void setProductionPerXTerritories(final String value)
	{
		final String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new IllegalStateException("Rules Attachments: productionPerXTerritories can not be empty or have more than two fields");
		String unitTypeToProduce;
		if (s.length == 1)
			unitTypeToProduce = Constants.INFANTRY_TYPE;
		else
			unitTypeToProduce = s[1];
		// validate that this unit exists in the xml
		final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
		if (ut == null)
			throw new IllegalStateException("Rules Attachments: No unit called: " + unitTypeToProduce);
		final int n = getInt(s[0]);
		if (n <= 0)
			throw new IllegalStateException("Rules Attachments: productionPerXTerritories must be a positive integer");
		m_productionPerXTerritories.put(ut, n);
	}
	
	public IntegerMap<UnitType> getProductionPerXTerritories()
	{
		return m_productionPerXTerritories;
	}
	
	public void clearProductionPerXTerritories()
	{
		m_productionPerXTerritories.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
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
	
	public void setPlacementPerTerritory(final String value)
	{
		m_placementPerTerritory = getInt(value);
	}
	
	public int getPlacementPerTerritory()
	{
		return m_placementPerTerritory;
	}
	
	public void setMaxPlacePerTerritory(final String value)
	{
		m_maxPlacePerTerritory = getInt(value);
	}
	
	public int getMaxPlacePerTerritory()
	{
		return m_maxPlacePerTerritory;
	}
	
	public void setPlacementAnyTerritory(final String value)
	{
		m_placementAnyTerritory = getBool(value);
	}
	
	public boolean getPlacementAnyTerritory()
	{
		return m_placementAnyTerritory;
	}
	
	public void setPlacementAnySeaZone(final String value)
	{
		m_placementAnySeaZone = getBool(value);
	}
	
	public boolean getPlacementAnySeaZone()
	{
		return m_placementAnySeaZone;
	}
	
	public void setPlacementCapturedTerritory(final String value)
	{
		m_placementCapturedTerritory = getBool(value);
	}
	
	public boolean getPlacementCapturedTerritory()
	{
		return m_placementCapturedTerritory;
	}
	
	public void setPlacementInCapitalRestricted(final String value)
	{
		m_placementInCapitalRestricted = getBool(value);
	}
	
	public boolean getPlacementInCapitalRestricted()
	{
		return m_placementInCapitalRestricted;
	}
	
	public void setUnlimitedProduction(final String value)
	{
		m_unlimitedProduction = getBool(value);
	}
	
	public boolean getUnlimitedProduction()
	{
		return m_unlimitedProduction;
	}
	
	public void setDominatingFirstRoundAttack(final String value)
	{
		m_dominatingFirstRoundAttack = getBool(value);
	}
	
	public boolean getDominatingFirstRoundAttack()
	{
		return m_dominatingFirstRoundAttack;
	}
	
	public void setNegateDominatingFirstRoundAttack(final String value)
	{
		m_negateDominatingFirstRoundAttack = getBool(value);
	}
	
	public boolean getNegateDominatingFirstRoundAttack()
	{
		return m_negateDominatingFirstRoundAttack;
	}
	
	public int getAtWarCount()
	{
		return m_atWarCount;
	}
	
	public int getTechCount()
	{
		return m_techCount;
	}
	
	public void setAtWarCount(final String s)
	{
		m_atWarCount = getInt(s);
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
	
	public void setUses(final String s)
	{
		m_uses = getInt(s);
	}
	
	public void setUses(final Integer u)
	{
		m_uses = u;
	}
	
	public Set<PlayerID> getAtWarPlayers()
	{
		return m_atWarPlayers;
	}
	
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
	
	public void setTurns(final String turns) throws GameParseException
	{
		m_turns = new HashMap<Integer, Integer>();
		final String[] s = turns.split(":");
		if (s.length < 1)
			throw new GameParseException("Rules & Conditions: Empty turn list");
		for (int i = 0; i < s.length; i++)
		{
			int start, end;
			try
			{
				start = getInt(s[i]);
				end = start;
			} catch (final Exception e)
			{
				final String[] s2 = s[i].split("-");
				if (s2.length != 2)
					throw new GameParseException("Rules & Conditions: Invalid syntax for range, must be 'int-int'");
				start = getInt(s2[0]);
				if (s2[1].equals("+"))
				{
					end = Integer.MAX_VALUE;
				}
				else
					end = getInt(s2[1]);
			}
			final Integer t = new Integer(start);
			final Integer u = new Integer(end);
			m_turns.put(t, u);
		}
	}
	
	private boolean checkTurns(final GameData data)
	{
		final int turn = data.getSequence().getRound();
		for (final Integer t : m_turns.keySet())
		{
			if (turn >= t.intValue() && turn <= m_turns.get(t).intValue())
				return true;
		}
		return false;
	}
	
	/**
	 * takes a string like "original", "enemy", "controlled", "controlledNoWater", "all", "map", and turns it into an actual list of territories in the form of strings
	 * (veqryn)
	 */
	private String getTerritoriesBasedOnStringName(final String name, final PlayerID player, final GameData data)
	{
		String value = new String();
		if (name.equals("original") || name.equals("enemy"))
		{ // get all originally owned territories
			final OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
			final Collection<Territory> originalTerrs = origOwnerTracker.getOriginallyOwned(data, player);
			setTerritoryCount(String.valueOf(originalTerrs.size()));
			// Colon delimit the collection as it would exist in the XML
			for (final Territory item : originalTerrs)
				value = value + ":" + item;
		}
		else if (name.equals("controlled"))
		{
			final Collection<Territory> ownedTerrs = data.getMap().getTerritoriesOwnedBy(player);
			setTerritoryCount(String.valueOf(ownedTerrs.size()));
			// Colon delimit the collection as it would exist in the XML
			for (final Territory item : ownedTerrs)
				value = value + ":" + item;
		}
		else if (name.equals("controlledNoWater"))
		{
			final Collection<Territory> ownedTerrs = data.getMap().getTerritoriesOwnedBy(player);
			final Collection<Territory> ownedTerrsNoWater = Match.getMatches(ownedTerrs, Matches.TerritoryIsNotImpassableToLandUnits(player, data));
			setTerritoryCount(String.valueOf(ownedTerrsNoWater.size()));
			// Colon delimit the collection as it would exist in the XML
			for (final Territory item : ownedTerrsNoWater)
				value = value + ":" + item;
		}
		else if (name.equals("all"))
		{
			final Collection<Territory> allTerrs = data.getMap().getTerritoriesOwnedBy(player);
			final OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
			allTerrs.addAll(origOwnerTracker.getOriginallyOwned(data, player));
			setTerritoryCount(String.valueOf(allTerrs.size()));
			// Colon delimit the collection as it would exist in the XML
			for (final Territory item : allTerrs)
				value = value + ":" + item;
		}
		else if (name.equals("map"))
		{
			final Collection<Territory> allTerrs = data.getMap().getTerritories();
			setTerritoryCount(String.valueOf(allTerrs.size()));
			// Colon delimit the collection as it would exist in the XML
			for (final Territory item : allTerrs)
				value = value + ":" + item;
		}
		else
		{ // The list just contained 1 territory
			setTerritoryCount(String.valueOf(1));
			value = name;
		}
		return value;
	}
	
	/**
	 * takes the raw data from the xml, and turns it into an actual territory list, in the form of strings
	 * (veqryn)
	 */
	private String getTerritoryListAsStringBasedOnInputFromXML(final String[] terrs, final PlayerID player, final GameData data)
	{
		String value = new String();
		// If there's only 1, it might be a 'group' (original, controlled, controlledNoWater, all)
		if (terrs.length == 1)
		{
			value = getTerritoriesBasedOnStringName(terrs[0], player, data);
		}
		else if (terrs.length == 2)
		{
			if (!terrs[1].equals("controlled") && !terrs[1].equals("controlledNoWater") && !terrs[1].equals("original") && !terrs[1].equals("all") && !terrs[1].equals("map")
						&& !terrs[1].equals("enemy"))
			{
				// Get the list of territories
				final Collection<Territory> listedTerrs = getListedTerritories(terrs);
				// Colon delimit the collection as it exists in the XML
				for (final Territory item : listedTerrs)
					value = value + ":" + item;
			}
			else
			{
				value = getTerritoriesBasedOnStringName(terrs[1], player, data);
				setTerritoryCount(String.valueOf(terrs[0]));
			}
		}
		else
		{
			// Get the list of territories
			final Collection<Territory> listedTerrs = getListedTerritories(terrs);
			// Colon delimit the collection as it exists in the XML
			for (final Territory item : listedTerrs)
				value = value + ":" + item;
		}
		// Remove the leading colon
		if (value.length() > 0 && value.startsWith(":"))
			value = value.replaceFirst(":", "");
		return value;
	}
	
	/**
	 * Anything that implements IConditions (currently RulesAttachment, TriggerAttachment, and PoliticalActionAttachment)
	 * can use this to get all the conditions that must be checked for the object to be 'satisfied'. <br>
	 * Since anything implementing IConditions can contain other IConditions, this must recursively search through all conditions and contained conditions to get the final list.
	 * 
	 * @param startingListOfConditions
	 * @return
	 */
	public static HashSet<IConditions> getAllConditionsRecursive(final HashSet<IConditions> startingListOfConditions, HashSet<IConditions> allConditionsNeededSoFar)
	{
		if (allConditionsNeededSoFar == null)
			allConditionsNeededSoFar = new HashSet<IConditions>();
		allConditionsNeededSoFar.addAll(startingListOfConditions);
		for (final IConditions condition : startingListOfConditions)
		{
			for (final IConditions subCondition : condition.getConditions())
			{
				if (!allConditionsNeededSoFar.contains(subCondition))
					allConditionsNeededSoFar.addAll(getAllConditionsRecursive(new HashSet<IConditions>(Collections.singleton(subCondition)), allConditionsNeededSoFar));
			}
		}
		return allConditionsNeededSoFar;
	}
	
	/**
	 * Takes the list of IConditions that getAllConditionsRecursive generates, and tests each of them, mapping them one by one to their boolean value.
	 * 
	 * @param rules
	 * @param data
	 * @return
	 */
	public static HashMap<IConditions, Boolean> testAllConditionsRecursive(final HashSet<IConditions> rules, HashMap<IConditions, Boolean> allConditionsTestedSoFar, final IDelegateBridge aBridge)
	{
		if (allConditionsTestedSoFar == null)
			allConditionsTestedSoFar = new HashMap<IConditions, Boolean>();
		
		for (final IConditions c : rules)
		{
			if (!allConditionsTestedSoFar.containsKey(c))
			{
				testAllConditionsRecursive(new HashSet<IConditions>(c.getConditions()), allConditionsTestedSoFar, aBridge);
				allConditionsTestedSoFar.put(c, c.isSatisfied(allConditionsTestedSoFar, aBridge));
			}
		}
		
		return allConditionsTestedSoFar;
	}
	
	/**
	 * Accounts for all listed rules, according to the conditionType.
	 * Takes the mapped conditions generated by testAllConditions and uses it to know which conditions are true and which are false. There is no testing of conditions done in this method.
	 * 
	 * @param rules
	 * @param conditionType
	 * @param data
	 * @return
	 */
	public static boolean areConditionsMet(final List<IConditions> rulesToTest, final HashMap<IConditions, Boolean> testedConditions, final String conditionType)
	{
		boolean met = false;
		if (conditionType.equals("AND") || conditionType.equals("and"))
		{
			for (final IConditions c : rulesToTest)
			{
				met = testedConditions.get(c);
				if (!met)
					break;
			}
		}
		else if (conditionType.equals("OR") || conditionType.equals("or"))
		{
			for (final IConditions c : rulesToTest)
			{
				met = testedConditions.get(c);
				if (met)
					break;
			}
		}
		else if (conditionType.equals("XOR") || conditionType.equals("xor"))
		{
			// XOR is confusing with more than 2 conditions, so we will just say that one has to be true, while all others must be false
			boolean isOneTrue = false;
			for (final IConditions c : rulesToTest)
			{
				met = testedConditions.get(c);
				if (isOneTrue && met)
				{
					isOneTrue = false;
					break;
				}
				else if (met)
					isOneTrue = true;
			}
			met = isOneTrue;
		}
		else
		{
			final String[] nums = conditionType.split("-");
			if (nums.length == 1)
			{
				final int start = Integer.parseInt(nums[0]);
				int count = 0;
				for (final IConditions c : rulesToTest)
				{
					met = testedConditions.get(c);
					if (met)
						count++;
				}
				met = (count == start);
			}
			else if (nums.length == 2)
			{
				final int start = Integer.parseInt(nums[0]);
				final int end = Integer.parseInt(nums[1]);
				int count = 0;
				for (final IConditions c : rulesToTest)
				{
					met = testedConditions.get(c);
					if (met)
						count++;
				}
				met = (count >= start && count <= end);
			}
		}
		return met;
	}
	
	@Override
	public boolean isSatisfied(final HashMap<IConditions, Boolean> testedConditions)
	{
		if (testedConditions == null)
			throw new IllegalStateException("testedConditions can not be null");
		if (!testedConditions.containsKey(this))
			throw new IllegalStateException("testedConditions is incomplete and does not contain " + this.toString());
		return testedConditions.get(this);
	}
	
	@Override
	public boolean isSatisfied(HashMap<IConditions, Boolean> testedConditions, final IDelegateBridge aBridge)
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
				testedConditions = testAllConditionsRecursive(getAllConditionsRecursive(new HashSet<IConditions>(m_conditions), null), null, aBridge);
			objectiveMet = areConditionsMet(new ArrayList<IConditions>(m_conditions), testedConditions, m_conditionType);
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
					final Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						final PlayerID currPlayer = playersIter.next();
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
					final Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						final PlayerID currPlayer = playersIter.next();
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
					final Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						final PlayerID currPlayer = playersIter.next();
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
					final Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						final PlayerID currPlayer = playersIter.next();
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
					final Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						final PlayerID currPlayer = playersIter.next();
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
					final Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						final PlayerID currPlayer = playersIter.next();
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
		
		// "chance" should ALWAYS be checked last!
		final int hitTarget = getInt(m_chance.split(":")[0]);
		final int diceSides = getInt(m_chance.split(":")[1]);
		if (objectiveMet && hitTarget != diceSides)
		{
			final int rollResult = aBridge.getRandom(diceSides, "Attempting the Condition: " + MyFormatter.attachmentNameToText(this.getName())) + 1;
			objectiveMet = rollResult <= hitTarget;
			final String notificationMessage = "Rolling (" + hitTarget + " out of " + diceSides + ") result: " + rollResult + " " + (objectiveMet ? "Success!" : "Failure!") + " (for "
						+ MyFormatter.attachmentNameToText(this.getName()) + ")";
			aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(notificationMessage));
			((ITripleaPlayer) aBridge.getRemote(aBridge.getPlayerID())).reportMessage(notificationMessage, notificationMessage);
		}
		
		return objectiveMet != m_invert;
	}
	
	/**
	 * Called after the attachment is created.
	 * (edit: actually this isn't being called AT ALL)
	 * 
	 * @throws GameParseException
	 *             validation failed
	 */
	public void validate() throws GameParseException
	{
		validateNames(m_alliedOwnershipTerritories);
		validateNames(m_enemyExclusionTerritories);
		validateNames(m_enemySurfaceExclusionTerritories);
		validateNames(m_alliedExclusionTerritories);
		validateNames(m_directExclusionTerritories);
		validateNames(m_directOwnershipTerritories);
		validateNames(m_movementRestrictionTerritories);
		validateNames(m_directPresenceTerritories);
		validateNames(m_alliedPresenceTerritories);
		validateNames(m_enemyPresenceTerritories);
	}
	
	private void validateNames(final String[] terrList)
	{
		/*if (terrList != null && (!terrList.equals("controlled") && !terrList.equals("controlledNoWater") && !terrList.equals("original") && !terrList.equals("all") && !terrList.equals("map") && !terrList.equals("enemy")))
		{
			if (terrList.length != 2)
				getListedTerritories(terrList);
			else if (terrList.length == 2 && (!terrList[1].equals("controlled") && !terrList[1].equals("controlledNoWater") && !terrList[1].equals("original") && !terrList[1].equals("all") && !terrList[1].equals("map") && !terrList[1].equals("enemy")))
				getListedTerritories(terrList);
		}*/
		if (terrList != null && terrList.length > 0)
			getListedTerritories(terrList);
		// removed checks for length & group commands because it breaks the setTerritoryCount feature.
	}
	
	/**
	 * Validate that all listed territories actually exist. Will return an empty list of territories if sent a list that is empty or contains only a "" string.
	 * 
	 * @param list
	 * @return
	 */
	public Collection<Territory> getListedTerritories(final String[] list)
	{
		final List<Territory> rVal = new ArrayList<Territory>();
		// this list is empty, or contains "", so return a blank list of territories
		if (list.length == 0 || (list.length == 1 && list[0].length() < 1))
			return rVal;
		for (final String name : list)
		{
			// See if the first entry contains the number of territories needed to meet the criteria
			try
			{
				// Leave the temp field- it checks if the list just starts with a territory by failing the TRY
				@SuppressWarnings("unused")
				final int temp = getInt(name);
				setTerritoryCount(name);
				continue;
			} catch (final Exception e)
			{
			}
			if (name.equals("each"))
			{
				setTerritoryCount(String.valueOf(1));
				setCountEach(true);
				continue;
			}
			// Skip looking for the territory if the original list contains one of the 'group' commands
			if (name.equals("controlled") || name.equals("controlledNoWater") || name.equals("original") || name.equals("all") || name.equals("map") || name.equals("enemy"))
				break;
			// Validate all territories exist
			final Territory territory = getData().getMap().getTerritory(name);
			if (territory == null)
				throw new IllegalStateException("Rules & Conditions: No territory called:" + name);
			rVal.add(territory);
		}
		return rVal;
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
		final Iterator<Territory> ownedTerrIter = Territories.iterator();
		while (ownedTerrIter.hasNext())
		{
			final Territory terr = ownedTerrIter.next();
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
			setEachMultiple(numberMet);
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
			setEachMultiple(numberMet);
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
		final Iterator<Territory> listedTerrIter = listedTerrs.iterator();
		while (listedTerrIter.hasNext())
		{
			final Territory listedTerr = listedTerrIter.next();
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
			setEachMultiple(numberMet);
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
		final Iterator<Territory> listedTerrIter = listedTerrs.iterator();
		while (listedTerrIter.hasNext())
		{
			final Territory listedTerr = listedTerrIter.next();
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
			setEachMultiple(numberMet);
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
			setEachMultiple(found);
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
			setEachMultiple(found);
		return found >= m_techCount;
	}
}
