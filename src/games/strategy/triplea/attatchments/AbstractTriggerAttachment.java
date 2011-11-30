package games.strategy.triplea.attatchments;

import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Abstraction done by Erik von der Osten, original TriggerAttachment writen by Squid Daddy and Mark Christopher Duncan
 * 
 */
public class AbstractTriggerAttachment extends DefaultAttachment
{
	public static final String AFTER = "after";
	public static final String BEFORE = "before";
	
	private List<RulesAttachment> m_conditions = null;
	private String m_conditionType = "AND";
	private boolean m_invert = false;
	
	private int m_uses = -1;
	private boolean m_usedThisRound = false;
	
	private String m_notification = null;
	
	private Tuple<String, String> m_when = null;
	
	public AbstractTriggerAttachment()
	{
		super();
	}
	
	/**
	 * Convenience method.
	 * 
	 * @param player
	 * @param nameOfAttachment
	 * @return a new trigger attachment
	 */
	public static AbstractTriggerAttachment get(final PlayerID player, final String nameOfAttachment)
	{
		final AbstractTriggerAttachment rVal = (AbstractTriggerAttachment) player.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("Triggers: No trigger attachment for:" + player.getName() + " with name: " + nameOfAttachment);
		return rVal;
	}
	
	/**
	 * @param player
	 * @param data
	 * @param cond
	 * @return set of trigger attachments (If you use null for the match condition, you will get all triggers for this player)
	 */
	public static Set<TriggerAttachment> getTriggers(final PlayerID player, final GameData data, final Match<TriggerAttachment> cond)
	{
		final Set<TriggerAttachment> trigs = new HashSet<TriggerAttachment>();
		final Map<String, IAttachment> map = player.getAttachments();
		final Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext())
		{
			final IAttachment a = map.get(iter.next());
			if (a instanceof TriggerAttachment)
			{
				if (cond == null || cond.match((TriggerAttachment) a))
					trigs.add((TriggerAttachment) a);
			}
		}
		return trigs;
	}
	
	public static CompositeChange triggerSetUsedForThisRound(final PlayerID player, final IDelegateBridge aBridge)
	{
		final CompositeChange change = new CompositeChange();
		for (final AbstractTriggerAttachment ta : getTriggers(player, aBridge.getData(), null))
		{
			if (ta.m_usedThisRound)
			{
				final int currentUses = ta.getUses();
				if (currentUses > 0)
				{
					change.add(ChangeFactory.attachmentPropertyChange(ta, new Integer(currentUses - 1).toString(), "uses"));
					change.add(ChangeFactory.attachmentPropertyChange(ta, false, "usedThisRound"));
				}
			}
		}
		return change;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @deprecated please use setConditions, getConditions, clearConditions, instead.
	 * @param triggers
	 * @throws GameParseException
	 */
	@Deprecated
	public void setTrigger(final String conditions) throws GameParseException
	{
		setConditions(conditions);
	}
	
	/**
	 * @deprecated please use setConditions, getConditions, clearConditions, instead.
	 * @return
	 */
	@Deprecated
	public List<RulesAttachment> getTrigger()
	{
		return getConditions();
	}
	
	/**
	 * @deprecated please use setConditions, getConditions, clearConditions, instead.
	 */
	@Deprecated
	public void clearTrigger()
	{
		clearConditions();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param conditions
	 * @throws GameParseException
	 */
	public void setConditions(final String conditions) throws GameParseException
	{
		for (final String subString : conditions.split(":"))
		{
			RulesAttachment condition = null;
			for (final PlayerID p : getData().getPlayerList().getPlayers())
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
				throw new GameParseException("Triggers: Could not find rule. name:" + subString);
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
	
	public void setUses(final String s)
	{
		m_uses = getInt(s);
	}
	
	public void setUses(final Integer u)
	{
		m_uses = u;
	}
	
	public void setUses(final int u)
	{
		m_uses = u;
	}
	
	public void setUsedThisRound(final String s)
	{
		m_usedThisRound = getBool(s);
	}
	
	public void setUsedThisRound(final boolean usedThisRound)
	{
		m_usedThisRound = usedThisRound;
	}
	
	public void setUsedThisRound(final Boolean usedThisRound)
	{
		m_usedThisRound = usedThisRound;
	}
	
	public boolean getUsedThisRound()
	{
		return m_usedThisRound;
	}
	
	public int getUses()
	{
		return m_uses;
	}
	
	public boolean getInvert()
	{
		return m_invert;
	}
	
	public void setInvert(final String s)
	{
		m_invert = getBool(s);
	}
	
	public void setWhen(final String when) throws GameParseException
	{
		final String[] s = when.split(":");
		if (s.length != 2)
			throw new GameParseException("Triggers: when must exist in 2 parts: \"before/after:stepName\".");
		if (!(s[0].equals(AFTER) || s[0].equals(BEFORE)))
			throw new GameParseException("Triggers: notificaition must start with: " + BEFORE + " or " + AFTER);
		m_when = new Tuple<String, String>(s[0], s[1]);
	}
	
	public Tuple<String, String> getWhen()
	{
		return m_when;
	}
	
	public void setNotification(final String sNotification)
	{
		m_notification = sNotification;
	}
	
	public String getNotification()
	{
		return m_notification;
	}
	
	public String getConditionType()
	{
		return m_conditionType;
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
	
	protected void use(final IDelegateBridge aBridge)
	{
		// instead of using up a "use" with every action, we will instead use up a "use" if the trigger is fired during this round
		// this is in order to let a trigger that contains multiple actions, fire all of them in a single use
		if (!m_usedThisRound && m_uses > 0)
		{
			aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, true, "usedThisRound"));
		}
		/*if (m_uses > 0)
		{
			aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, new Integer(m_uses - 1).toString(), "uses"));
		}*/
	}
	
	/**
	 * This will account for Invert and conditionType
	 */
	protected static boolean isMet(final AbstractTriggerAttachment t, final GameData data)
	{
		return RulesAttachment.areConditionsMet(t.getConditions(), t.getConditionType(), data) != t.getInvert();
	}
	
	/**
	 * If t.getWhen(), beforeOrAfter, and stepName, are all null, then this returns true.
	 * Otherwise, all must be not null, and when's values must match the arguments.
	 * 
	 * @param beforeOrAfter
	 *            can be null, or must be "before" or "after"
	 * @param stepName
	 *            can be null, or must be exact name of a specific stepName
	 * @return true if when and both args are null, and true if all are not null and when matches the args, otherwise false
	 */
	protected static Match<TriggerAttachment> whenOrDefaultMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			@Override
			public boolean match(final TriggerAttachment t)
			{
				if (beforeOrAfter == null && stepName == null && t.getWhen() == null)
					return true;
				else if (beforeOrAfter != null && stepName != null && t.getWhen() != null && beforeOrAfter.equals(t.getWhen().getFirst()) && stepName.equals(t.getWhen().getSecond()))
					return true;
				return false;
			}
		};
	}
	
	protected static Match<TriggerAttachment> notificationMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			@Override
			public boolean match(final TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getNotification() != null;
			}
		};
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		if (m_conditions == null)
			throw new GameParseException("Triggers: Invalid Unit attatchment" + this);
	}
	
	protected static String getValueFromStringArrayForAllSubStrings(final String[] s)
	{
		final StringBuilder sb = new StringBuilder();
		for (final String subString : s)
		{
			sb.append(":");
			sb.append(subString);
		}
		// remove leading colon
		if (sb.length() > 0 && sb.substring(0, 1).equals(":"))
			sb.replace(0, 1, "");
		return sb.toString();
	}
	
	protected static String getValueFromStringArrayForAllExceptLastSubstring(final String[] s)
	{
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length - 1; i++)
		{
			sb.append(":");
			sb.append(s[i]);
		}
		// remove leading colon
		if (sb.length() > 0 && sb.substring(0, 1).equals(":"))
			sb.replace(0, 1, "");
		return sb.toString();
	}
}
