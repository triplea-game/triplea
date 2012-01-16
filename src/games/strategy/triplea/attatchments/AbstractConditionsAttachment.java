package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This class is designed to hold common code for holding "conditions". Any attachment that can hold conditions (ie: RulesAttachments), should extend this instead of DefaultAttachment.
 * 
 * @author veqryn [Mark Christopher Duncan]
 * 
 */
public abstract class AbstractConditionsAttachment extends DefaultAttachment implements ICondition
{
	private static final long serialVersionUID = -9008441256118867078L;
	
	protected List<RulesAttachment> m_conditions = new ArrayList<RulesAttachment>(); // list of conditions that this condition can contain
	protected String m_conditionType = "AND"; // m_conditionType modifies the relationship of m_conditions
	protected boolean m_invert = false; // will logically negate the entire condition, including contained conditions
	protected String m_chance = "1:1"; // chance (x out of y) that this action is successful when attempted, default = 1:1 = always successful
	
	public AbstractConditionsAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param conditions
	 * @throws GameParseException
	 */
	public void setConditions(final String conditions) throws GameParseException
	{
		final Collection<PlayerID> playerIDs = getData().getPlayerList().getPlayers();
		for (final String subString : conditions.split(":"))
		{
			RulesAttachment condition = null;
			for (final PlayerID p : playerIDs)
			{
				condition = (RulesAttachment) p.getAttachment(subString);
				if (condition != null)
					break;
				/*try {
					m_conditions = RulesAttachment.get(p, conditionName);
				} catch (IllegalStateException ise) {
				}
				if (m_conditions != null)
					break;*/
			}
			if (condition == null)
				throw new GameParseException("Attachment, " + this.getName() + ", Could not find rule. name:" + subString);
			if (m_conditions == null)
				m_conditions = new ArrayList<RulesAttachment>();
			m_conditions.add(condition);
		}
	}
	
	public List<RulesAttachment> getConditions()
	{
		return m_conditions;
	}
	
	public void clearConditions()
	{
		m_conditions.clear();
	}
	
	public boolean getInvert()
	{
		return m_invert;
	}
	
	public void setInvert(final String s)
	{
		m_invert = getBool(s);
	}
	
	public void setConditionType(final String s) throws GameParseException
	{
		if (!(s.equals("and") || s.equals("AND") || s.equals("or") || s.equals("OR") || s.equals("XOR") || s.equals("xor")))
		{
			final String[] nums = s.split("-");
			if (nums.length == 1)
			{
				if (Integer.parseInt(nums[0]) < 0)
					throw new GameParseException(
								"Rules & Conditions: conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y");
			}
			else if (nums.length == 2)
			{
				if (Integer.parseInt(nums[0]) < 0 || Integer.parseInt(nums[1]) < 0 || !(Integer.parseInt(nums[0]) < Integer.parseInt(nums[1])))
					throw new GameParseException(
								"Rules & Conditions: conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y");
			}
			else
				throw new GameParseException(
							"Rules & Conditions: conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y");
		}
		m_conditionType = s;
	}
	
	public String getConditionType()
	{
		return m_conditionType;
	}
	
	/**
	 * Accounts for Invert and conditionType.
	 */
	public boolean isSatisfied(final HashMap<ICondition, Boolean> testedConditions)
	{
		return isSatisfied(testedConditions, null);
	}
	
	/**
	 * Accounts for Invert and conditionType. IDelegateBridge is not used so can be null.
	 */
	public boolean isSatisfied(final HashMap<ICondition, Boolean> testedConditions, final IDelegateBridge aBridge)
	{
		if (testedConditions == null)
			throw new IllegalStateException("testedCondititions can not be null");
		if (testedConditions.containsKey(this))
			return testedConditions.get(this);
		return RulesAttachment.areConditionsMet(new ArrayList<ICondition>(this.getConditions()), testedConditions, this.getConditionType()) != this.getInvert();
	}
	
	public static Match<AbstractConditionsAttachment> isSatisfiedAbstractConditionsAttachmentMatch(final HashMap<ICondition, Boolean> testedConditions)
	{
		return new Match<AbstractConditionsAttachment>()
		{
			@Override
			public boolean match(final AbstractConditionsAttachment ca)
			{
				return ca.isSatisfied(testedConditions);
			}
		};
	}
	
	public void setChance(final String chance) throws GameParseException
	{
		final String[] s = chance.split(":");
		try
		{
			final int i = getInt(s[0]);
			final int j = getInt(s[1]);
			if (i > j || i < 1 || j < 1 || i > 120 || j > 120)
				throw new GameParseException("PoliticalActionAttachment: chance should have a format of \"x:y\" where x is <= y and both x and y are >=1 and <=120");
		} catch (final IllegalArgumentException iae)
		{
			throw new GameParseException("PoliticalActionAttachment: Invalid chance declaration: " + chance + " format: \"1:10\" for 10% chance");
		}
		m_chance = chance;
	}
	
	/**
	 * @return the number you need to roll to get the action to succeed format "1:10" for 10% chance
	 */
	public String getChance()
	{
		return m_chance;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
