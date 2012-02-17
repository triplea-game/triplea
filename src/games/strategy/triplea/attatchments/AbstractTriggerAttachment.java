package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.HashMap;
import java.util.List;

/**
 * @author Abstraction done by Erik von der Osten, original TriggerAttachment writen by Squid Daddy and Mark Christopher Duncan
 * 
 */
public abstract class AbstractTriggerAttachment extends AbstractConditionsAttachment implements ICondition
{
	private static final long serialVersionUID = 5866039180681962697L;
	
	public static final String AFTER = "after";
	public static final String BEFORE = "before";
	
	// "setTrigger" is also a valid setter, and it just calls "setConditions" in AbstractConditionsAttachment. Kept for backwards compatibility.
	private int m_uses = -1;
	@InternalDoNotExport
	private boolean m_usedThisRound = false; // Do Not Export (do not include in IAttachment).
	
	private String m_notification = null;
	
	private Tuple<String, String> m_when = null;
	
	public AbstractTriggerAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	public static CompositeChange triggerSetUsedForThisRound(final PlayerID player, final IDelegateBridge aBridge)
	{
		final CompositeChange change = new CompositeChange();
		for (final TriggerAttachment ta : TriggerAttachment.getTriggers(player, aBridge.getData(), null))
		{
			if (ta.getUsedThisRound())
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
	@GameProperty(xmlProperty = true, gameProperty = false, adds = true)
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
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setUses(final int u)
	{
		m_uses = u;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setUsedThisRound(final String s)
	{
		m_usedThisRound = getBool(s);
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setUsedThisRound(final boolean usedThisRound)
	{
		m_usedThisRound = usedThisRound;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
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
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWhen(final String when) throws GameParseException
	{
		if (when == null)
		{
			m_when = null;
			return;
		}
		final String[] s = when.split(":");
		if (s.length != 2)
			throw new GameParseException("when must exist in 2 parts: \"before/after:stepName\"." + thisErrorMsg());
		if (!(s[0].equals(AFTER) || s[0].equals(BEFORE)))
			throw new GameParseException("when must start with: " + BEFORE + " or " + AFTER + thisErrorMsg());
		m_when = new Tuple<String, String>(s[0], s[1]);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setWhen(final Tuple<String, String> value)
	{
		m_when = value;
	}
	
	public Tuple<String, String> getWhen()
	{
		return m_when;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setNotification(final String sNotification)
	{
		if (sNotification == null)
		{
			m_notification = null;
			return;
		}
		m_notification = sNotification;
	}
	
	public String getNotification()
	{
		return m_notification;
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
	
	protected boolean testChance(final IDelegateBridge aBridge)
	{
		// "chance" should ALWAYS be checked last!
		final int hitTarget = getInt(m_chance.split(":")[0]);
		final int diceSides = getInt(m_chance.split(":")[1]);
		if (hitTarget == diceSides)
			return true;
		
		final int rollResult = aBridge.getRandom(diceSides, "Attempting the Trigger: " + MyFormatter.attachmentNameToText(this.getName())) + 1;
		final boolean testChance = rollResult <= hitTarget;
		final String notificationMessage = "Rolling (" + hitTarget + " out of " + diceSides + ") result: " + rollResult + " = " + (testChance ? "Success!" : "Failure!") + " (for "
					+ MyFormatter.attachmentNameToText(this.getName()) + ")";
		aBridge.getHistoryWriter().startEvent(notificationMessage);
		((ITripleaPlayer) aBridge.getRemote(aBridge.getPlayerID())).reportMessage(notificationMessage, notificationMessage);
		return testChance;
	}
	
	public static Match<TriggerAttachment> isSatisfiedMatch(final HashMap<ICondition, Boolean> testedConditions)
	{
		return new Match<TriggerAttachment>()
		{
			@Override
			public boolean match(final TriggerAttachment t)
			{
				return t.isSatisfied(testedConditions);
			}
		};
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
	public static Match<TriggerAttachment> whenOrDefaultMatch(final String beforeOrAfter, final String stepName)
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
	
	public static Match<TriggerAttachment> availableUses = new Match<TriggerAttachment>()
	{
		@Override
		public boolean match(final TriggerAttachment t)
		{
			return t.getUses() != 0;
		}
	};
	
	public static Match<TriggerAttachment> notificationMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			@Override
			public boolean match(final TriggerAttachment t)
			{
				return availableUses.match(t) && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getNotification() != null;
			}
		};
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
	
	protected static int getEachMultiple(final AbstractTriggerAttachment t)
	{
		int eachMultiple = 1;
		for (final RulesAttachment condition : t.getConditions())
		{
			final int tempEach = condition.getEachMultiple();
			if (tempEach > eachMultiple)
				eachMultiple = tempEach;
		}
		return eachMultiple;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		super.validate(data);
		if (m_conditions == null)
			throw new GameParseException("must contain at least one condition: " + thisErrorMsg());
	}
}
