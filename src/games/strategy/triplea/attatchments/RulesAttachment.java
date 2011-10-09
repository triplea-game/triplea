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


import games.strategy.engine.data.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.*;
import games.strategy.util.*;

import java.util.*;



/**
 *
 * @author Kevin Comcowich and Veqryn (Mark Christopher Duncan)
 * @version 1.2
 */
public class RulesAttachment extends DefaultAttachment
{
	/**
	 *
	 */
	private static final long serialVersionUID = 7301965634079412516L;

	/**
	 * Convenience method, will not return objectives and conditions, only the rules attachment (like what China in ww2v3 has).
	 * @param r rule
	 * @return new rule attachment
	 */
	public static RulesAttachment get(Rule r)
	{
		// why the heck are we using "Rule" here?  there are no attachments onto Rule objects, as far as I know RulesAttachments get attached to players only
		RulesAttachment rVal = (RulesAttachment) r.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (rVal == null)
			throw new IllegalStateException("Rules & Conditions: No rule attachment for:" + r.getName());
		return rVal;
	}

	/**
	 * Convenience method, will not return objectives and conditions, only the rules attachment (like what China in ww2v3 has).
	 * @param player PlayerID
	 * @return new rule attachment
	 */
	public static RulesAttachment get(PlayerID player)
	{
		RulesAttachment rVal = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (rVal == null)
			throw new IllegalStateException("Rules & Conditions: No rule attachment for:" + player.getName());
		return rVal;
	}
	
	/**
	 * Convenience method, for use with rules attachments, objectives, and condition attachments.
	 * @param player PlayerID
	 * @param nameOfAttachment exact full name of attachment
	 * @return new rule attachment
	 */
	public static RulesAttachment get(PlayerID player, String nameOfAttachment)
	{
		RulesAttachment rVal = (RulesAttachment) player.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("Rules & Conditions: No rule attachment for:" + player.getName() + " with name: " + nameOfAttachment);
		return rVal;
	}

	// Players
	// not used?
	// private PlayerID m_ruleOwner = null;
	
	// meta-conditions rules attachment lists
	private List<RulesAttachment> m_conditions = new ArrayList<RulesAttachment>();

	// Strings
	//private String m_alliedExclusion = null;
	//private String m_enemyExclusion = null;
	private String m_allowedUnitType = null;
	private String m_movementRestrictionType = null;
	private String m_conditionType = "AND";

	// Territory lists
	private String[] m_alliedOwnershipTerritories;
	private String[] m_alliedExclusionTerritories;
	private String[] m_directExclusionTerritories;
	private String[] m_enemyExclusionTerritories;
	private String[] m_enemySurfaceExclusionTerritories;
	private String[] m_directOwnershipTerritories;
	private String[] m_directPresenceTerritories;
	private String[] m_alliedPresenceTerritories;
	private String[] m_enemyPresenceTerritories;

	private String[] m_movementRestrictionTerritories;

	// booleans
	private boolean m_placementAnyTerritory = false; // only covers land
	private boolean m_placementAnySeaZone = false; // only covers sea zones by owned land
	private boolean m_placementCapturedTerritory = false;
	private boolean m_unlimitedProduction = false;
	private boolean m_placementInCapitalRestricted = false;
	private boolean m_dominatingFirstRoundAttack = false;
	private boolean m_negateDominatingFirstRoundAttack = false;
	private boolean m_invert = false;

	// Integers
	private int m_territoryCount = -1;
	private int m_objectiveValue = 0;
	private int m_perOwnedTerritories = -1;
	private int m_placementPerTerritory = -1;
	private int m_maxPlacePerTerritory = -1;
	private int m_atWarCount = -1;
	private int m_uses = -1;
	private int m_techCount = -1;

	// production per X territories
	private IntegerMap<UnitType> m_productionPerXTerritories = new IntegerMap<UnitType>();
	private IntegerMap<UnitType> m_unitPresence = new IntegerMap<UnitType>();

	private Set<PlayerID> m_atWarPlayers = null;
	// using map as tuple set, describes ranges from Integer-Integer
	private Map<Integer, Integer> m_turns = null;
	private List<TechAdvance> m_techs = null;
	// list of all relationships that should be in place for this condition to be valid array of "Germany:Italy:Allied", "UK:USA:Allied" etc.
	private List<String> m_relationship = new ArrayList<String>();

	/** Creates new RulesAttachment */
	public RulesAttachment()
	{
	}

	/*public void setRuleOwner(PlayerID player)
	{
		m_ruleOwner = player;
	}

	public PlayerID getRuleOwner()
	{
		return m_ruleOwner;
	}*/

	public void setObjectiveValue(String value)
	{
		m_objectiveValue = getInt(value);
	}

	public int getObjectiveValue()
	{
		return m_objectiveValue;
	}
	
	/**
	 * Adds to, not sets.  Anything that adds to instead of setting needs a clear function as well.
	 * @param conditions
	 * @throws GameParseException
	 */
	public void setConditions(String conditions) throws GameParseException{
		String[] s = conditions.split(":");
		for(int i = 0;i<s.length;i++) {
			RulesAttachment condition = null;
			for(PlayerID p:getData().getPlayerList().getPlayers()){
				condition = (RulesAttachment) p.getAttachment(s[i]);
				if( condition != null)
					break;
			}
			if(condition == null)
				throw new GameParseException("Rules & Conditions: Could not find rule (conditions which hold other conditions should be listed after the conditions they contain). name:" + s[i]);
			m_conditions.add(condition);
		}
	}
	
	public List<RulesAttachment> getConditions() {
		return m_conditions;
	}
	
	public void clearConditions() {
		m_conditions.clear();
	}
	
	public String getConditionType() {
		return m_conditionType;
	}
	
	public void setConditionType(String s) throws GameParseException{
		if (!(s.equals("and") || s.equals("AND") || s.equals("or") || s.equals("OR") || s.equals("XOR") || s.equals("xor")))
		{
			String[] nums = s.split("-");
			if (nums.length == 1)
			{
				if (Integer.parseInt(nums[0]) < 0)
					throw new GameParseException("Rules & Conditions: conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y");
			}
			else if (nums.length == 2)
			{
				if (Integer.parseInt(nums[0]) < 0 || Integer.parseInt(nums[1]) < 0 || !(Integer.parseInt(nums[0]) < Integer.parseInt(nums[1])))
					throw new GameParseException("Rules & Conditions: conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y");
			}
			else
				throw new GameParseException("Rules & Conditions: conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y");
		}
		m_conditionType = s;
	}

	/**
	 * Condition to check if a certain relationship exists between 2 players.
	 * Adds to, not sets.  Anything that adds to instead of setting needs a clear function as well.
	 * @param value should be a string containing: "player:player:relationship"
	 * @throws GameParseException
	 */
	public void setRelationship(String value) throws GameParseException {
		String[] s = value.split(":");
		if(s.length != 3)
			throw new GameParseException("Rules & Conditions: relationship should have value=\"playername:playername:relationshiptype\"");
		if(getData().getPlayerList().getPlayerID(s[0]) == null)
			throw new GameParseException("Rules & Conditions: playername: "+s[0]+" isn't valid in condition with relationship: "+value+" for RulesAttachment "+getName());
		if(getData().getPlayerList().getPlayerID(s[1]) == null)
			throw new GameParseException("Rules & Conditions: playername: "+s[1]+" isn't valid in condition with relationship: "+value+" for RulesAttachment "+getName());
		if(     !(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED) ||
				s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL)||
				s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR) ||
				Matches.isValidRelationshipName(getData()).match(s[2])))
			throw new GameParseException("Rules & Conditions: relationship: "+s[2]+" isn't valid in condition with relationship: "+value+" for RulesAttachment "+getName());
		m_relationship.add(value);
	}
	
	public List<String> getRelationship()
	{
		return m_relationship;
	}
	
	public void clearRelationship()
	{
		m_relationship.clear();
	}

	public void setAlliedOwnershipTerritories(String value)
	{
		m_alliedOwnershipTerritories = value.split(":");
		validateNames(m_alliedOwnershipTerritories);
	}

	public String[] getAlliedOwnershipTerritories()
	{
		return m_alliedOwnershipTerritories;
	}

	// exclusion types = controlled, controlledNoWater, original, all, or list
	public void setAlliedExclusionTerritories(String value)
	{
		m_alliedExclusionTerritories = value.split(":");
		validateNames(m_alliedExclusionTerritories);
	}

	public String[] getAlliedExclusionTerritories()
	{
		return m_alliedExclusionTerritories;
	}

	public void setDirectExclusionTerritories(String value)
	{
		m_directExclusionTerritories = value.split(":");
		validateNames(m_directExclusionTerritories);
	}

	public String[] getDirectExclusionTerritories()
	{
		return m_directExclusionTerritories;
	}

	// exclusion types = original or list
	public void setEnemyExclusionTerritories(String value)
	{
		m_enemyExclusionTerritories = value.split(":");
		validateNames(m_enemyExclusionTerritories);
	}

	public String[] getEnemyExclusionTerritories()
	{
		return m_enemyExclusionTerritories;
	}

	public void setDirectPresenceTerritories(String value)
	{
		m_directPresenceTerritories = value.split(":");
		validateNames(m_directPresenceTerritories);
	}

	public String[] getDirectPresenceTerritories()
	{
		return m_directPresenceTerritories;
	}

	public void setAlliedPresenceTerritories(String value)
	{
		m_alliedPresenceTerritories = value.split(":");
		validateNames(m_alliedPresenceTerritories);
	}

	public String[] getAlliedPresenceTerritories()
	{
		return m_alliedPresenceTerritories;
	}

	public void setEnemyPresenceTerritories(String value)
	{
		m_enemyPresenceTerritories = value.split(":");
		validateNames(m_enemyPresenceTerritories);
	}

	public String[] getEnemyPresenceTerritories()
	{
		return m_enemyPresenceTerritories;
	}

	// exclusion types = original or list
	public void setEnemySurfaceExclusionTerritories(String value)
	{
		m_enemySurfaceExclusionTerritories = value.split(":");
		validateNames(m_enemySurfaceExclusionTerritories);
	}

	public String[] getEnemySurfaceExclusionTerritories()
	{
		return m_enemySurfaceExclusionTerritories;
	}

	public void setDirectOwnershipTerritories(String value)
	{
		m_directOwnershipTerritories = value.split(":");
		validateNames(m_directOwnershipTerritories);
	}

	public String[] getDirectOwnershipTerritories()
	{
		return m_directOwnershipTerritories;
	}

	public void setTerritoryCount(String value)
	{
		m_territoryCount = getInt(value);
	}

	public int getTerritoryCount()
	{
		return m_territoryCount;
	}

	public void setPerOwnedTerritories(String value)
	{
		m_perOwnedTerritories = getInt(value);
	}

	public int getPerOwnedTerritories()
	{
		return m_perOwnedTerritories;
	}

	public void setAllowedUnitType(String value)
	{
		m_allowedUnitType = value;
	}

	public String getAllowedUnitType()
	{
		return m_allowedUnitType;
	}

	public void setMovementRestrictionTerritories(String value)
	{
		m_movementRestrictionTerritories = value.split(":");
		validateNames(m_movementRestrictionTerritories);
	}

	public String[] getMovementRestrictionTerritories()
	{
		return m_movementRestrictionTerritories;
	}

	public void setMovementRestrictionType(String value) throws GameParseException
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
	 * Adds to, not sets.  Anything that adds to instead of setting needs a clear function as well.
	 * @param value
	 */
	public void setProductionPerXTerritories(String value)
	{
		String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new IllegalStateException("Rules Attachments: productionPerXTerritories can not be empty or have more than two fields");

		String unitTypeToProduce;
		if (s.length == 1)
			unitTypeToProduce = Constants.INFANTRY_TYPE;
		else
			unitTypeToProduce = s[1];

		// validate that this unit exists in the xml
		UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
		if (ut == null)
			throw new IllegalStateException("Rules Attachments: No unit called: " + unitTypeToProduce);

		int n = getInt(s[0]);
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
	 * Adds to, not sets.  Anything that adds to instead of setting needs a clear function as well.
	 * @param value
	 */
	public void setUnitPresence(String value)
	{
		String[] s = value.split(":");
		if (s.length <= 0 || s.length > 2)
			throw new IllegalStateException("Rules Attachments: unitPresence can not be empty or have more than two fields");

		String unitTypeToProduce = s[1];

		// validate that this unit exists in the xml
		UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
		if (ut == null && !(unitTypeToProduce.equals("any") || unitTypeToProduce.equals("ANY")))
			throw new IllegalStateException("Rules Attachments: No unit called: " + unitTypeToProduce);

		int n = getInt(s[0]);
		if (n <= 0)
			throw new IllegalStateException("Rules Attachments: unitPresence must be a positive integer");

		// null UnitType will mean any unit for the purposes of this
		m_unitPresence.put(ut, n);
	}

	public IntegerMap<UnitType> getUnitPresence()
	{
		return m_unitPresence;
	}

	public void clearUnitPresence()
	{
		m_unitPresence.clear();
	}

	public void setPlacementPerTerritory(String value)
	{
		m_placementPerTerritory = getInt(value);
	}

	public int getPlacementPerTerritory()
	{
		return m_placementPerTerritory;
	}

	public void setMaxPlacePerTerritory(String value)
	{
		m_maxPlacePerTerritory = getInt(value);
	}

	public int getMaxPlacePerTerritory()
	{
		return m_maxPlacePerTerritory;
	}

	public void setPlacementAnyTerritory(String value)
	{
		m_placementAnyTerritory = getBool(value);
	}

	public boolean getPlacementAnyTerritory()
	{
		return m_placementAnyTerritory;
	}
	
	public void setPlacementAnySeaZone(String value)
	{
		m_placementAnySeaZone = getBool(value);
	}
	
	public boolean getPlacementAnySeaZone()
	{
		return m_placementAnySeaZone;
	}

	public void setPlacementCapturedTerritory(String value)
	{
		m_placementCapturedTerritory = getBool(value);
	}

	public boolean getPlacementCapturedTerritory()
	{
		return m_placementCapturedTerritory;
	}

	public void setPlacementInCapitalRestricted(String value)
	{
		m_placementInCapitalRestricted = getBool(value);
	}

	public boolean getPlacementInCapitalRestricted()
	{
		return m_placementInCapitalRestricted;
	}

	public void setUnlimitedProduction(String value)
	{
		m_unlimitedProduction = getBool(value);
	}

	public boolean getUnlimitedProduction()
	{
		return m_unlimitedProduction;
	}

	public void setDominatingFirstRoundAttack(String value)
	{
		m_dominatingFirstRoundAttack = getBool(value);
	}

	public boolean getDominatingFirstRoundAttack()
	{
		return m_dominatingFirstRoundAttack;
	}

	public void setNegateDominatingFirstRoundAttack(String value)
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

	public void setAtWarCount(String s)
	{
		m_atWarCount = getInt(s);
	}

	public int getUses()
	{
		return m_uses;
	}

	public void setUses(String s)
	{
		m_uses = getInt(s);
	}

	public void setUses(Integer u)
	{
		m_uses = u;
	}

	public boolean getInvert()
	{
		return m_invert;
	}

	public void setInvert(String s)
	{
		m_invert = getBool(s);
	}

	public Set<PlayerID> getAtWarPlayers()
	{
		return m_atWarPlayers;
	}

	public void setAtWarPlayers(String players) throws GameParseException
	{
		String[] s = players.split(":");
		int count = -1;
		if (s.length < 1)
			throw new GameParseException("Rules & Conditions: Empty enemy list");
		try
		{
			count = getInt(s[0]);
			m_atWarCount = count;
		} catch (Exception e)
		{
			m_atWarCount = 0;
		}
		if (s.length < 1 || s.length == 1 && count != -1)
			throw new GameParseException("Rules & Conditions: Empty enemy list");
		m_atWarPlayers = new HashSet<PlayerID>();
		for (int i = count == -1 ? 0 : 1; i < s.length; i++)
		{
			PlayerID player = getData().getPlayerList().getPlayerID(s[i]);
			if (player == null)
				throw new GameParseException("Rules & Conditions: Could not find player. name:" + s[i]);
			m_atWarPlayers.add(player);
		}
	}

	public void setTechs(String techs) throws GameParseException
	{
		{
			String[] s = techs.split(":");
			int count = -1;
			if (s.length < 1)
				throw new GameParseException("Rules & Conditions: Empty tech list");
			try
			{
				count = getInt(s[0]);
				m_techCount = count;
			} catch (Exception e)
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

	public void setTurns(String turns) throws GameParseException
	{
		m_turns = new HashMap<Integer, Integer>();
		String[] s = turns.split(":");
		if (s.length < 1)
			throw new GameParseException("Rules & Conditions: Empty turn list");
		for (int i = 0; i < s.length; i++)
		{
			int start, end;
			try
			{
				start = getInt(s[i]);
				end = start;
			} catch (Exception e)
			{
				String[] s2 = s[i].split("-");
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
			Integer t = new Integer(start);
			Integer u = new Integer(end);
			m_turns.put(t, u);
		}
	}

	private boolean checkTurns(GameData data)
	{
		int turn = data.getSequence().getRound();
		for (Integer t : m_turns.keySet())
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
	private String getTerritoriesBasedOnStringName(String name, PlayerID player, GameData data)
	{
		String value = new String();

		if (name.equals("original") || name.equals("enemy"))
		{ // get all originally owned territories
			OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
			Collection<Territory> originalTerrs = origOwnerTracker.getOriginallyOwned(data, player);
			setTerritoryCount(String.valueOf(originalTerrs.size()));
			// Colon delimit the collection as it would exist in the XML
			for (Territory item : originalTerrs)
				value = value + ":" + item;
		}
		else if (name.equals("controlled"))
		{
			Collection<Territory> ownedTerrs = data.getMap().getTerritoriesOwnedBy(player);
			setTerritoryCount(String.valueOf(ownedTerrs.size()));
			// Colon delimit the collection as it would exist in the XML
			for (Territory item : ownedTerrs)
				value = value + ":" + item;
		}
		else if (name.equals("controlledNoWater"))
		{
			Collection<Territory> ownedTerrs = data.getMap().getTerritoriesOwnedBy(player);
			Collection<Territory> ownedTerrsNoWater = Match.getMatches(ownedTerrs, Matches.TerritoryIsNotImpassableToLandUnits(player));
			setTerritoryCount(String.valueOf(ownedTerrsNoWater.size()));
			// Colon delimit the collection as it would exist in the XML
			for (Territory item : ownedTerrsNoWater)
				value = value + ":" + item;
		}
		else if (name.equals("all"))
		{
			Collection<Territory> allTerrs = data.getMap().getTerritoriesOwnedBy(player);
			OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
			allTerrs.addAll(origOwnerTracker.getOriginallyOwned(data, player));
			setTerritoryCount(String.valueOf(allTerrs.size()));
			// Colon delimit the collection as it would exist in the XML
			for (Territory item : allTerrs)
				value = value + ":" + item;
		}
		else if (name.equals("map"))
		{
			Collection<Territory> allTerrs = data.getMap().getTerritories();
			setTerritoryCount(String.valueOf(allTerrs.size()));
			// Colon delimit the collection as it would exist in the XML
			for (Territory item : allTerrs)
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
	private String getTerritoryListAsStringBasedOnInputFromXML(String[] terrs, PlayerID player, GameData data)
	{
		String value = new String();

		// If there's only 1, it might be a 'group' (original, controlled, controlledNoWater, all)
		if (terrs.length == 1)
		{
			value = getTerritoriesBasedOnStringName(terrs[0], player, data);
		}
		else if (terrs.length == 2)
		{
			if (!terrs[1].equals("controlled") && !terrs[1].equals("controlledNoWater") && !terrs[1].equals("original") && !terrs[1].equals("all") && !terrs[1].equals("map") && !terrs[1].equals("enemy"))
			{
				// Get the list of territories
				Collection<Territory> listedTerrs = getListedTerritories(terrs);
				// Colon delimit the collection as it exists in the XML
				for (Territory item : listedTerrs)
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
			Collection<Territory> listedTerrs = getListedTerritories(terrs);
			// Colon delimit the collection as it exists in the XML
			for (Territory item : listedTerrs)
				value = value + ":" + item;
		}

		// Remove the leading colon
		if (value.length() > 0 && value.startsWith(":"))
			value = value.replaceFirst(":", "");

		return value;
	}
	
	/**
	 * This will account for Invert and conditionType while looking at all the conditions, 
	 * but if invert is true we will reverse at the end since RulesAttachment.isSatisfied() will test for invert again later.
	 * Similar to TriggerAttachment.isMet()
	 */
	private boolean isMetForMetaConditions(GameData data) {
		boolean met = false;
		String conditionType = getConditionType();
		if (conditionType.equals("AND") || conditionType.equals("and"))
		{
			for (RulesAttachment c : getConditions()) {
				met = c.isSatisfied(data) != m_invert;
				if (!met)
					break;
			}
		}
		else if (conditionType.equals("OR") || conditionType.equals("or"))
		{
			for (RulesAttachment c : getConditions()) {
				met = c.isSatisfied(data) != m_invert;
				if (met)
					break;
			}
		}
		else if (conditionType.equals("XOR") || conditionType.equals("xor"))
		{
			// XOR is confusing with more than 2 conditions, so we will just say that one has to be true, while all others must be false
			boolean isOneTrue = false;
			for (RulesAttachment c : getConditions()) {
				met = c.isSatisfied(data) != m_invert;
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
			String[] nums = conditionType.split("-");
			if (nums.length == 1)
			{
				int start = Integer.parseInt(nums[0]);
				int count = 0;
				for (RulesAttachment c : getConditions()) {
					met = c.isSatisfied(data) != m_invert;
					if (met)
						count++;
				}
				met = (count == start);
			}
			else if (nums.length == 2)
			{
				int start = Integer.parseInt(nums[0]);
				int end = Integer.parseInt(nums[1]);
				int count = 0;
				for (RulesAttachment c : getConditions()) {
					met = c.isSatisfied(data) != m_invert;
					if (met)
						count++;
				}
				met = (count >= start && count <= end);
			}
		}
		
		if (m_invert)
			return !met;
		else
			return met;
	}

	public boolean isSatisfied(GameData data)
	{
		boolean objectiveMet = true;
		PlayerID player = (PlayerID) getAttatchedTo();
		
		//
		// check meta conditions (conditions which hold other conditions)
		//
		if (objectiveMet && m_conditions.size() > 0)
		{
			objectiveMet = isMetForMetaConditions(data);
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
					Collection<PlayerID> players = data.getPlayerList().getPlayers();
					Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						PlayerID currPlayer = playersIter.next();
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
					Collection<PlayerID> players = data.getPlayerList().getPlayers();
					Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						PlayerID currPlayer = playersIter.next();
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
					Collection<PlayerID> players = data.getPlayerList().getPlayers();
					Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						PlayerID currPlayer = playersIter.next();
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
					Collection<PlayerID> players = data.getPlayerList().getPlayers();
					Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						PlayerID currPlayer = playersIter.next();
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
					Collection<PlayerID> players = data.getPlayerList().getPlayers();
					Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						PlayerID currPlayer = playersIter.next();
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
					Collection<PlayerID> players = data.getPlayerList().getPlayers();
					Iterator<PlayerID> playersIter = players.iterator();
					while (playersIter.hasNext())
					{
						PlayerID currPlayer = playersIter.next();
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
		if (objectiveMet && m_relationship.size()>0)
		{
			objectiveMet = checkRelationships();
		}

		return objectiveMet != m_invert;
	}



	/**
	 * Called after the attachment is created.
	 * (edit: actually this isn't being called AT ALL)
	 * @throws GameParseException validation failed
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




	private void validateNames(String[] terrList)
	{
		/*if (terrList != null && (!terrList.equals("controlled") && !terrList.equals("controlledNoWater") && !terrList.equals("original") && !terrList.equals("all") && !terrList.equals("map") && !terrList.equals("enemy")))
		{
			if (terrList.length != 2)
				getListedTerritories(terrList);
			else if (terrList.length == 2 && (!terrList[1].equals("controlled") && !terrList[1].equals("controlledNoWater") && !terrList[1].equals("original") && !terrList[1].equals("all") && !terrList[1].equals("map") && !terrList[1].equals("enemy")))
				getListedTerritories(terrList);
		}*/
		if(terrList != null && terrList.length >0)
			getListedTerritories(terrList);
		//removed checks for length & group commands because it breaks the setTerritoryCount feature.
	}

	/**
	 * Validate that all listed territories actually exist.  Will return an empty list of territories if sent a list that is empty or contains only a "" string.
	 * @param list
	 * @return
	 */
	public Collection<Territory> getListedTerritories(String[] list)
	{
		List<Territory> rVal = new ArrayList<Territory>();
		
		// this list is empty, or contains "", so return a blank list of territories
		if (list.length == 0 || (list.length == 1 && list[0].length()<1))
			return rVal;

		for (String name : list)
		{
			// See if the first entry contains the number of territories needed to meet the criteria
			try
			{
				// Leave the temp field- it checks if the list just starts with a territory by failing the TRY
				@SuppressWarnings("unused")
				int temp = getInt(name);
				setTerritoryCount(name);
				continue;
			} catch (Exception e)
			{
			}

			// Skip looking for the territory if the original list contains one of the 'group' commands
			if (name.equals("controlled") || name.equals("controlledNoWater") || name.equals("original") || name.equals("all") || name.equals("map") || name.equals("enemy"))
				break;

			// Validate all territories exist
			Territory territory = getData().getMap().getTerritory(name);
			if (territory == null)
				throw new IllegalStateException("Rules & Conditions: No territory called:" + name);
			rVal.add(territory);
		}
		return rVal;
	}
	/**
	 * checks if all relationshiprequirements are set
	 * @return whether all relationships as are required are set correctly.
	 */
	private boolean checkRelationships() {
		for(String aRelationCheck:m_relationship) {
			String[] relationCheck = aRelationCheck.split(":");
			PlayerID p1 = getData().getPlayerList().getPlayerID(relationCheck[0]);
			PlayerID p2 = getData().getPlayerList().getPlayerID(relationCheck[1]);
			RelationshipType currentRelationship = getData().getRelationshipTracker().getRelationshipType(p1, p2);
			if (! (relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED) && Matches.RelationshipIsAllied.match(currentRelationship) ||
						   relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL) && Matches.RelationshipIsNeutral.match(currentRelationship) ||
						   relationCheck[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR) && Matches.RelationshipIsAtWar.match(currentRelationship) ||
						   currentRelationship.equals(getData().getRelationshipTypeList().getRelationshipType(relationCheck[2]))))
				return false;
		}
		return true;
	}

	/**
	 * Checks for the collection of territories to see if they have units owned by the exclType alliance.
	 */
	private boolean checkUnitPresence(boolean satisfied, Collection<Territory> Territories, String exclType, int numberNeeded, PlayerID player, GameData data)
	{
		int numberMet = 0;
		satisfied = false;

		boolean useSpecific = false;
		if (getUnitPresence() != null && !getUnitPresence().keySet().isEmpty())
			useSpecific = true;

		Iterator<Territory> ownedTerrIter = Territories.iterator();
		while (ownedTerrIter.hasNext())
		{
			Territory terr = ownedTerrIter.next();
			Collection<Unit> allUnits = terr.getUnits().getUnits();

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
						break;
					}
				}
				else if (useSpecific)
				{
					IntegerMap<UnitType> unitsMap = getUnitPresence();
					Set<UnitType> units = unitsMap.keySet();
					boolean hasEnough = false;
					for (UnitType ut : units)
					{
						int unitsNeeded = unitsMap.getInt(ut);
						if (ut == null)
						{
							if (allUnits.size() >= unitsNeeded)
								hasEnough = true;
							else
								hasEnough = false;
						}
						else
						{
							if (Match.getMatches(allUnits, Matches.unitIsOfType(ut)).size() >= unitsNeeded)
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
							break;
						}
					}
				}
			}
		}
		return satisfied;
	}

	/**
	 * Checks for the collection of territories to see if they have units owned by the exclType alliance.
	 * It doesn't yet threshold the data
	 */
	private boolean checkUnitExclusions(boolean satisfied, Collection<Territory> Territories, String exclType, int numberNeeded, PlayerID player, GameData data)
	{
		int numberMet = 0;
		satisfied = false;

		boolean useSpecific = false;
		if (getUnitPresence() != null && !getUnitPresence().keySet().isEmpty())
			useSpecific = true;

		Iterator<Territory> ownedTerrIter = Territories.iterator();
		// Go through the owned territories and see if there are any units owned by allied/enemy based on exclType
		while (ownedTerrIter.hasNext())
		{
			// get all the units in the territory
			Territory terr = ownedTerrIter.next();
			Collection<Unit> allUnits = terr.getUnits().getUnits();

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
					break;
				}
			}
			else if (useSpecific)
			{
				IntegerMap<UnitType> unitsMap = getUnitPresence();
				Set<UnitType> units = unitsMap.keySet();
				boolean hasLess = false;
				for (UnitType ut : units)
				{
					int unitsMax = unitsMap.getInt(ut);
					if (ut == null)
					{
						if (allUnits.size() <= unitsMax)
							hasLess = true;
						else
							hasLess = false;
					}
					else
					{
						if (Match.getMatches(allUnits, Matches.unitIsOfType(ut)).size() <= unitsMax)
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
						break;
					}
				}
			}
		}
		return satisfied;
	}

	/**
	 * Checks for allied ownership of the collection of territories. Once the needed number threshold is reached, the satisfied flag is set
	 * to true and returned
	 */
	private boolean checkAlliedOwnership(boolean satisfied, Collection<Territory> listedTerrs, int numberNeeded, PlayerID player, GameData data)
	{
		int numberMet = 0;
		satisfied = false;

		Iterator<Territory> listedTerrIter = listedTerrs.iterator();

		while (listedTerrIter.hasNext())
		{
			Territory listedTerr = listedTerrIter.next();
			// if the territory owner is an ally
			if (data.getRelationshipTracker().isAllied(listedTerr.getOwner(), player))
			{
				numberMet += 1;
				if (numberMet >= numberNeeded)
				{
					satisfied = true;
					break;
				}
			}
		}
		return satisfied;
	}

	/**
	 * astabada
	 * Checks for direct ownership of the collection of territories. Once the needed number threshold is reached, the satisfied flag is set
	 * to true and returned
	 */
	private boolean checkDirectOwnership(boolean satisfied, Collection<Territory> listedTerrs, int numberNeeded, PlayerID player)
	{
		int numberMet = 0;
		satisfied = false;

		Iterator<Territory> listedTerrIter = listedTerrs.iterator();

		while (listedTerrIter.hasNext())
		{
			Territory listedTerr = listedTerrIter.next();
			// if the territory owner is an ally
			if (listedTerr.getOwner() == player)
			{
				numberMet += 1;
				if (numberMet >= numberNeeded)
				{
					satisfied = true;
					break;
				}
			}
		}
		return satisfied;
	}

	private boolean checkAtWar(PlayerID player, Set<PlayerID> enemies, int count, GameData data)
	{
		int found = 0;
		for (PlayerID e : enemies)
			if (data.getRelationshipTracker().isAtWar(player, e))
				found++;
		if (count == 0)
			return count == found;
		return found >= count;
	}

	private boolean checkTechs(PlayerID player, GameData data)
	{
		int found = 0;
		for (TechAdvance a : TechTracker.getTechAdvances(player, data))
			if (m_techs.contains(a))
				found++;
		if (m_techCount == 0)
			return m_techCount == found;
		return found >= m_techCount;
	}
}
