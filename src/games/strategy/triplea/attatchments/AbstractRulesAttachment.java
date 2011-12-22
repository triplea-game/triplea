package games.strategy.triplea.attatchments;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
	
	protected boolean m_countEach = false; // determines if we will be counting each for the purposes of m_objectiveValue
	protected int m_eachMultiple = 1; // the multiple that will be applied to m_objectiveValue if m_countEach is true
	protected int m_objectiveValue = 0; // only used if the attachment begins with "objectiveAttachment"
	protected int m_uses = -1; // only matters for objectiveValue, does not affect the condition
	protected Map<Integer, Integer> m_turns = null; // condition for what turn it is
	protected int m_territoryCount = -1; // used with the next Territory conditions to determine the number of territories needed to be valid (ex: m_alliedOwnershipTerritories)
	
	public AbstractRulesAttachment()
	{
		super();
	}
	
	/**
	 * Convenience method, for use with rules attachments, objectives, and condition attachments. Should return RulesAttachments.
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
	 * Convenience method, for use returning any RulesAttachment that begins with "objectiveAttachment"
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
		for (final Map.Entry<String, IAttachment> entry : map.entrySet())
		{
			final IAttachment attachment = entry.getValue();
			if (attachment instanceof RulesAttachment)
			{
				if (attachment.getName().startsWith(Constants.RULES_OBJECTIVE_PREFIX))
				{
					natObjs.add((RulesAttachment) attachment);
				}
			}
		}
		return natObjs;
	}
	
	@Override
	public void setChance(final String chance) throws GameParseException
	{
		throw new GameParseException("RulesAttachment: chance not allowed for use with RulesAttachments, instead use it with Triggers or PoliticalActions");
	}
	
	public void setObjectiveValue(final String value)
	{
		m_objectiveValue = getInt(value);
	}
	
	public int getObjectiveValue()
	{
		return m_objectiveValue;
	}
	
	protected void setTerritoryCount(final String value)
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
	
	protected void setEachMultiple(final int value)
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
	
	protected void setCountEach(final boolean value)
	{
		m_countEach = value;
	}
	
	protected boolean getCountEach()
	{
		return m_countEach;
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
	
	public void setTurns(final String turns) throws GameParseException
	{
		m_turns = new HashMap<Integer, Integer>();
		final String[] s = turns.split(":");
		if (s.length < 1)
			throw new GameParseException("Rules & Conditions: Empty turn list");
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
	
	public Map<Integer, Integer> getTurns()
	{
		return m_turns;
	}
	
	protected boolean checkTurns(final GameData data)
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
	 * 
	 * @author veqryn
	 */
	protected String getTerritoriesBasedOnStringName(final String name, final PlayerID player, final GameData data)
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
	 * 
	 * @author veqryn
	 */
	protected String getTerritoryListAsStringBasedOnInputFromXML(final String[] terrs, final PlayerID player, final GameData data)
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
	 * Anything that implements ICondition (currently RulesAttachment, TriggerAttachment, and PoliticalActionAttachment)
	 * can use this to get all the conditions that must be checked for the object to be 'satisfied'. <br>
	 * Since anything implementing ICondition can contain other ICondition, this must recursively search through all conditions and contained conditions to get the final list.
	 * 
	 * @param startingListOfConditions
	 * @return
	 * @author veqryn
	 */
	public static HashSet<ICondition> getAllConditionsRecursive(final HashSet<ICondition> startingListOfConditions, HashSet<ICondition> allConditionsNeededSoFar)
	{
		if (allConditionsNeededSoFar == null)
			allConditionsNeededSoFar = new HashSet<ICondition>();
		allConditionsNeededSoFar.addAll(startingListOfConditions);
		for (final ICondition condition : startingListOfConditions)
		{
			for (final ICondition subCondition : condition.getConditions())
			{
				if (!allConditionsNeededSoFar.contains(subCondition))
					allConditionsNeededSoFar.addAll(getAllConditionsRecursive(new HashSet<ICondition>(Collections.singleton(subCondition)), allConditionsNeededSoFar));
			}
		}
		return allConditionsNeededSoFar;
	}
	
	/**
	 * Takes the list of ICondition that getAllConditionsRecursive generates, and tests each of them, mapping them one by one to their boolean value.
	 * 
	 * @param rules
	 * @param data
	 * @return
	 * @author veqryn
	 */
	public static HashMap<ICondition, Boolean> testAllConditionsRecursive(final HashSet<ICondition> rules, HashMap<ICondition, Boolean> allConditionsTestedSoFar, final IDelegateBridge aBridge)
	{
		if (allConditionsTestedSoFar == null)
			allConditionsTestedSoFar = new HashMap<ICondition, Boolean>();
		
		for (final ICondition c : rules)
		{
			if (!allConditionsTestedSoFar.containsKey(c))
			{
				testAllConditionsRecursive(new HashSet<ICondition>(c.getConditions()), allConditionsTestedSoFar, aBridge);
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
	 * @author veqryn
	 */
	public static boolean areConditionsMet(final List<ICondition> rulesToTest, final HashMap<ICondition, Boolean> testedConditions, final String conditionType)
	{
		boolean met = false;
		if (conditionType.equals("AND") || conditionType.equals("and"))
		{
			for (final ICondition c : rulesToTest)
			{
				met = testedConditions.get(c);
				if (!met)
					break;
			}
		}
		else if (conditionType.equals("OR") || conditionType.equals("or"))
		{
			for (final ICondition c : rulesToTest)
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
			for (final ICondition c : rulesToTest)
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
				for (final ICondition c : rulesToTest)
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
				for (final ICondition c : rulesToTest)
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
	
	protected void validateNames(final String[] terrList)
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
	 * Called after the attachment is created.
	 * (edit: actually this isn't being called AT ALL)
	 * 
	 * @throws GameParseException
	 *             validation failed
	 */
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		super.validate(data);
	}
}
