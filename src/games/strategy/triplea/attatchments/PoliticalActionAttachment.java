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

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An attachment, attached to a player that will describe which political
 * actions a player may take.
 * 
 * @author Edwin van der Wal
 * 
 */
public class PoliticalActionAttachment extends AbstractConditionsAttachment implements ICondition
{
	private static final long serialVersionUID = 4392770599777282477L;
	public static final String ATTEMPTS_LEFT_THIS_TURN = "attemptsLeftThisTurn";
	
	public PoliticalActionAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
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
	
	// list of relationship changes to be performed if this action is performed sucessfully
	private ArrayList<String> m_relationshipChange = new ArrayList<String>();
	// a key referring to politicaltexts.properties for all the UI messages belonging to this action.
	private String m_text = "";
	// cost in PU to attempt this action
	private int m_costPU = 0;
	// how many times can you perform this action each round?
	private int m_attemptsPerTurn = 1;
	// how many times are left to perform this action each round?
	@InternalDoNotExport
	private int m_attemptsLeftThisTurn = 1; // Do Not Export (do not include in IAttachment).
	// which players should accept this action? this could be the player who is the target of this action in the case of proposing a treaty or the players in your 'alliance' in case you want to declare war...
	// especially for actions that when france declares war on germany and it automatically causes UK to declare war as well. it is good to set "actionAccept" to "UK" so UK can accept this action to go through.
	private ArrayList<PlayerID> m_actionAccept = new ArrayList<PlayerID>();
	
	public static Match<PoliticalActionAttachment> isSatisfiedMatch(final HashMap<ICondition, Boolean> testedConditions)
	{
		return new Match<PoliticalActionAttachment>()
		{
			@Override
			public boolean match(final PoliticalActionAttachment paa)
			{
				return paa.isSatisfied(testedConditions);
			}
		};
	}
	
	/**
	 * @return true if there is no condition to this action or if the condition is satisfied
	 */
	public boolean canPerform(final HashMap<ICondition, Boolean> testedConditions)
	{
		return m_conditions == null || isSatisfied(testedConditions);
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param relChange
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setRelationshipChange(final String relChange) throws GameParseException
	{
		final String[] s = relChange.split(":");
		if (s.length != 3)
			throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n Use: player1:player2:newRelation\n" + thisErrorMsg());
		if (getData().getPlayerList().getPlayerID(s[0]) == null)
			throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n player: " + s[0] + " unknown in: " + getName() + thisErrorMsg());
		if (getData().getPlayerList().getPlayerID(s[1]) == null)
			throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n player: " + s[1] + " unknown in: " + getName() + thisErrorMsg());
		if (!Matches.isValidRelationshipName(getData()).match(s[2]))
			throw new GameParseException("Invalid relationshipChange declaration: " + relChange + " \n relationshipType: " + s[2] + " unknown in: " + getName() + thisErrorMsg());
		m_relationshipChange.add(relChange);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setRelationshipChange(final ArrayList<String> value)
	{
		m_relationshipChange = value;
	}
	
	public ArrayList<String> getRelationshipChange()
	{
		return m_relationshipChange;
	}
	
	public void clearRelationshipChange()
	{
		m_relationshipChange.clear();
	}
	
	public void resetRelationshipChange()
	{
		m_relationshipChange = new ArrayList<String>();
	}
	
	/**
	 * @param text
	 *            the Key that is used in politicstext.properties for all the texts
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
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
	
	public void resetText()
	{
		m_text = "";
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
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCostPU(final String s)
	{
		m_costPU = getInt(s);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setCostPU(final Integer s)
	{
		m_costPU = s;
	}
	
	/**
	 * @return the amount you need to pay to perform the action
	 */
	public int getCostPU()
	{
		return m_costPU;
	}
	
	public void resetCostPU()
	{
		m_costPU = 0;
	}
	
	/**
	 * @param s
	 *            the amount of times you can try this Action per Round
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttemptsPerTurn(final String s)
	{
		m_attemptsPerTurn = getInt(s);
		setAttemptsLeftThisTurn(m_attemptsPerTurn);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setAttemptsPerTurn(final Integer s)
	{
		m_attemptsPerTurn = s;
		setAttemptsLeftThisTurn(m_attemptsPerTurn);
	}
	
	/**
	 * @return the amount of times you can try this Action per Round
	 */
	public int getAttemptsPerTurn()
	{
		return m_attemptsPerTurn;
	}
	
	public void resetAttemptsPerTurn()
	{
		m_attemptsPerTurn = 1;
	}
	
	/**
	 * @param attempts
	 *            left this turn
	 */
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setAttemptsLeftThisTurn(final int attempts)
	{
		m_attemptsLeftThisTurn = attempts;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setAttemptsLeftThisTurn(final Integer attempts)
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
	 * @throws GameParseException
	 */
	@GameProperty(xmlProperty = true, gameProperty = true, adds = true)
	public void setActionAccept(final String value) throws GameParseException
	{
		final String[] temp = value.split(":");
		for (final String name : temp)
		{
			final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_actionAccept.add(tempPlayer);
			else
				throw new GameParseException("No player named: " + name + thisErrorMsg());
		}
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setActionAccept(final ArrayList<PlayerID> value)
	{
		m_actionAccept = value;
	}
	
	/**
	 * @return a list of players that must accept this action before it takes effect.
	 */
	public ArrayList<PlayerID> getActionAccept()
	{
		return m_actionAccept;
	}
	
	public void clearActionAccept()
	{
		m_actionAccept.clear();
	}
	
	public void resetActionAccept()
	{
		m_actionAccept = new ArrayList<PlayerID>();
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
		otherPlayers.remove((getAttachedTo()));
		return otherPlayers;
	}
	
	/**
	 * @param player
	 * @return gets the valid actions for this player.
	 */
	public static Collection<PoliticalActionAttachment> getValidActions(final PlayerID player, final HashMap<ICondition, Boolean> testedConditions, final GameData data)
	{
		if (!games.strategy.triplea.Properties.getUsePolitics(data) || !player.amNotDeadYet(data))
			return new ArrayList<PoliticalActionAttachment>();
		return Match.getMatches(getPoliticalActionAttachments(player), new CompositeMatchAnd<PoliticalActionAttachment>(
					Matches.PoliticalActionCanBeAttempted(testedConditions), Matches.politicalActionAffectsAtLeastOneAlivePlayer(player, data)));
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
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		super.validate(data);
		if (m_relationshipChange.isEmpty())
			throw new GameParseException("value: relationshipChange can't be empty" + thisErrorMsg());
		if (m_text.equals("") || m_text.length() <= 0)
			throw new GameParseException("value: text can't be empty" + thisErrorMsg());
	}
}
