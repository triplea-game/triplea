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
package games.strategy.triplea.attatchments;

import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An attachment, attached to a player that will describe which political
 * actions a player may take.
 * 
 * @author Edwin van der Wal
 * 
 */
public class PoliticalActionAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = 4392770599777282477L;
	public static final String ATTEMPTS_LEFT_THIS_TURN = "attemptsLeftThisTurn";
	
	public static Collection<PoliticalActionAttachment> getPoliticalActionAttachments(final PlayerID player)
	{
		final ArrayList<PoliticalActionAttachment> returnList = new ArrayList<PoliticalActionAttachment>();
		final Map<String, IAttachment> map = player.getAttachments();
		final Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext())
		{
			final IAttachment a = map.get(iter.next());
			if (a instanceof PoliticalActionAttachment) // we could also or instead check that the attachment is prefixed with CONSTANTS.POLITICALACTION_ATTACHMENT_PREFIX = "politicalActionAttachment"
				returnList.add((PoliticalActionAttachment) a);
		}
		/*for(IAttachment att : player.getAttachments().values()) {
			try {
				returnList.add((PoliticalActionAttachment) att);
			} catch (ClassCastException cce) {
				// the attachment is not a PoliticalActionAttachment but some
				// other PlayerAttachment like RulesAttachment or
				// TriggerAttachment
			}
		}*/
		return returnList;
	}
	
	public static PoliticalActionAttachment get(final PlayerID player, final String nameOfAttachment)
	{
		final PoliticalActionAttachment rVal = (PoliticalActionAttachment) player.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("PoliticalActionAttachment: No attachment for:" + player.getName() + " with name: " + nameOfAttachment);
		return rVal;
	}
	
	// the condition that needs to be true for this action to be active, this can be a metacondition or null for no condition. no condition means always active.
	private List<RulesAttachment> m_conditions = null;
	// the type, when there are multiple conditions listed
	private String m_conditionType = "AND";
	// do we invert the conditions?
	private boolean m_invert = false;
	// list of relationship changes to be performed if this action is performed sucessfully
	private final List<String> m_relationshipChange = new ArrayList<String>();
	// a key referring to politicaltexts.properties for all the UI messages belonging to this action.
	private String m_text = "";
	// chance (x out of y) that this action is successful when attempted, default = 1:1 = always succesful
	private String m_chance = "1:1";
	// cost in PU to attempt this action
	private int m_costPU = 0;
	// how many times can you perform this action each round?
	private int m_attemptsPerTurn = 1;
	// how many times are left to perform this action each round?
	private int m_attemptsLeftThisTurn = 1; // don't export this one
	// which players should accept this action? this could be the player who is the target of this action in the case of proposing a treaty or the players in your 'alliance' in case you want to declare war...
	// especially for actions that when france declares war on germany and it automatically causes UK to declare war as well. it is good to set "actionAccept" to "UK" so UK can accept this action to go through.
	private final Collection<PlayerID> m_actionAccept = new ArrayList<PlayerID>();
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		if (m_relationshipChange.isEmpty())
			throw new GameParseException("PoliticalActionAttachment: " + getName() + " value: relationshipChange can't be empty");
		if (m_text.equals(""))
			throw new GameParseException("PoliticalActionAttachment: " + getName() + " value: text can't be empty");
	}
	
	/**
	 * @param conditionName
	 *            The condition that needs to be satisfied for this action to be able to be performed by a player
	 * @throws GameParseException
	 */
	public void setConditions(final String conditions) throws GameParseException
	{
		final String[] s = conditions.split(":");
		for (int i = 0; i < s.length; i++)
		{
			RulesAttachment condition = null;
			for (final PlayerID p : getData().getPlayerList().getPlayers())
			{
				condition = (RulesAttachment) p.getAttachment(s[i]);
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
				throw new GameParseException("PoliticalActionAttachment: Could not find rule. name:" + s[i]);
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
	
	public void setInvert(final String s)
	{
		m_invert = getBool(s);
	}
	
	public boolean getInvert()
	{
		return m_invert;
	}
	
	/**
	 * This will account for Invert and conditionType
	 */
	private static boolean isMet(final PoliticalActionAttachment paa, final GameData data)
	{
		return RulesAttachment.areConditionsMet(paa.getConditions(), paa.getConditionType(), data) != paa.getInvert();
	}
	
	/**
	 * @return true if there is no condition to this action or if the condition is satisfied
	 */
	public boolean canPerform()
	{
		return m_conditions == null || isMet(this, getData());
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param relChange
	 * @throws GameParseException
	 */
	public void setRelationshipChange(final String relChange) throws GameParseException
	{
		final String[] s = relChange.split(":");
		if (s.length != 3)
			throw new GameParseException("PoliticalActionAttachment: Invalid relationshipChange declaration: " + relChange + " \n Use: player1:player2:newRelation\n");
		if (getData().getPlayerList().getPlayerID(s[0]) == null)
			throw new GameParseException("PoliticalActionAttachment: Invalid relationshipChange declaration: " + relChange + " \n player: " + s[0] + " unknown in: " + getName());
		if (getData().getPlayerList().getPlayerID(s[1]) == null)
			throw new GameParseException("PoliticalActionAttachment: Invalid relationshipChange declaration: " + relChange + " \n player: " + s[1] + " unknown in: " + getName());
		if (!Matches.isValidRelationshipName(getData()).match(s[2]))
			throw new GameParseException("PoliticalActionAttachment: Invalid relationshipChange declaration: " + relChange + " \n relationshipType: " + s[2] + " unknown in: " + getName());
		m_relationshipChange.add(relChange);
	}
	
	public List<String> getRelationshipChange()
	{
		return m_relationshipChange;
	}
	
	public void clearRelationshipChange()
	{
		m_relationshipChange.clear();
	}
	
	/**
	 * @param text
	 *            the Key that is used in politicstext.properties for all the texts
	 */
	public void setText(final String text)
	{
		m_text = text;
	}
	
	/**
	 * @return the Key that is used in politicstext.properties for all the texts
	 */
	public String getText()
	{
		return m_text;
	}
	
	/**
	 * @param s
	 *            the number you need to roll to get the action to succeed format "1:10" for 10% chance
	 * @throws GameParseException
	 */
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
	private String getChance()
	{
		return m_chance;
	}
	
	public int toHit()
	{
		return getInt(getChance().split(":")[0]);
	}
	
	public int diceSides()
	{
		return getInt(getChance().split(":")[1]);
	}
	
	/**
	 * @param s
	 *            the amount you need to pay to perform the action
	 */
	public void setCostPU(final String s)
	{
		m_costPU = getInt(s);
	}
	
	/**
	 * @return the amount you need to pay to perform the action
	 */
	public int getCostPU()
	{
		return m_costPU;
	}
	
	/**
	 * @param s
	 *            the amount of times you can try this Action per Round
	 */
	public void setAttemptsPerTurn(final String s)
	{
		m_attemptsPerTurn = getInt(s);
		setAttemptsLeftThisTurn(m_attemptsPerTurn);
	}
	
	/**
	 * @return the amount of times you can try this Action per Round
	 */
	public int getAttemptsPerTurn()
	{
		return m_attemptsPerTurn;
	}
	
	/**
	 * @param attempts
	 *            left this turn
	 */
	public void setAttemptsLeftThisTurn(final int attempts)
	{
		m_attemptsLeftThisTurn = attempts;
	}
	
	/**
	 * @return attempts that are left this turn
	 */
	public int getAttemptsLeftThisTurn()
	{
		return m_attemptsLeftThisTurn;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param value
	 */
	public void setActionAccept(final String value)
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_actionAccept.add(tempPlayer);
			else
				throw new IllegalStateException("PoliticalActionAttachment: No player named: " + name);
		}
	}
	
	/**
	 * @return a list of players that must accept this action before it takes effect.
	 */
	public Collection<PlayerID> getActionAccept()
	{
		return m_actionAccept;
	}
	
	public void clearActionAccept()
	{
		m_actionAccept.clear();
	}
	
	/**
	 * 
	 * @return a set of all other players involved in this PoliticalAction
	 */
	public Set<PlayerID> getOtherPlayers()
	{
		final HashSet<PlayerID> otherPlayers = new HashSet<PlayerID>();
		for (final String relationshipChange : m_relationshipChange)
		{
			final String[] s = relationshipChange.split(":");
			otherPlayers.add(getData().getPlayerList().getPlayerID(s[0]));
			otherPlayers.add(getData().getPlayerList().getPlayerID(s[1]));
		}
		otherPlayers.remove((getAttatchedTo()));
		return otherPlayers;
	}
	
	/**
	 * @param player
	 * @return gets the valid actions for this player.
	 */
	public static Collection<PoliticalActionAttachment> getValidActions(final PlayerID player, final GameData data)
	{
		if (!games.strategy.triplea.Properties.getUsePolitics(data) || !player.amNotDeadYet(data))
			return new ArrayList<PoliticalActionAttachment>();
		return Match.getMatches(getPoliticalActionAttachments(player),
					new CompositeMatchAnd<PoliticalActionAttachment>(Matches.PoliticalActionCanBeAttempted, Matches.politicalActionAffectsAtLeastOneAlivePlayer(player, data)));
	}
	
	public void resetAttempts(final IDelegateBridge aBridge)
	{
		if (m_attemptsLeftThisTurn != m_attemptsPerTurn)
		{
			aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, m_attemptsPerTurn, PoliticalActionAttachment.ATTEMPTS_LEFT_THIS_TURN));
		}
	}
	
	public void useAttempt(final IDelegateBridge aBridge)
	{
		aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, (m_attemptsLeftThisTurn - 1), PoliticalActionAttachment.ATTEMPTS_LEFT_THIS_TURN));
	}
	
	public boolean hasAttemptsLeft()
	{
		return m_attemptsLeftThisTurn > 0;
	}
}
