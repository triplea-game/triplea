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
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An attachment, attached to a player that will describe which political
 * actions a player may take.
 * 
 * @author Edwin van der Wal
 * 
 */

public class PoliticalActionAttachment extends DefaultAttachment {
	
	public static Collection<PoliticalActionAttachment> getPoliticalActionAttachments(PlayerID player){
		ArrayList<PoliticalActionAttachment> returnList = new ArrayList<PoliticalActionAttachment>();
		for(IAttachment att : player.getAttachments().values()) {
			try {
				returnList.add((PoliticalActionAttachment) att);
			} catch (ClassCastException cce) {
				// the attachment is not a PoliticalActionAttachment but some
				// other PlayerAttachment like RulesAttachment or
				// TriggerAttachment
			}
		}
		return returnList;
	}

	// the condition that needs to be true for this action to be active, this
	// can be a metacondition or null for no condition. no condition means
	// always active.
	private RulesAttachment m_condition = null;
	// list of relationship changes to be performed if this action is performed
	// sucessfully
	private final List<String> m_relationshipChange = new ArrayList<String>();
	// a key referring to politicaltexts.properties for all the UI messages
	// belonging to this action.
	private String m_text = "";
	// chance (x out of y) that this action is successful when attempted,
	// default = 1:1 = always succesful
	private String m_chance = "1:1";
	// cost in PU to attempt this action
	private int m_costPU = 0;
	// how many times can you perform this action each round?
	private int m_attemptsPerTurn = 1;
	// how many times are left to perform this action each round?
	private int m_attemptsLeftThisTurn = 1; // don't export this one
	// which players should accept this action? this could be the player who is
	// the target of this action in the case of proposing a treaty
	// or the players in your 'alliance' in case you want to declare war...
	// especially for actions that when france declares war on germany and it
	// automatically causes UK to declare war as well.
	// it is good to set "actionAccept" to "UK" so UK can accept this action to
	// go through.
	private final Collection<PlayerID> m_actionAccept = new ArrayList<PlayerID>();
	
	
	@Override
	public void validate(GameData data) throws GameParseException {
		if(m_relationshipChange.isEmpty())
			throw new GameParseException("PoliticalActionAttachment: "+getName()+" value: relationshipChange can't be empty");
		if(m_text.equals(""))
			throw new GameParseException("PoliticalActionAttachment: "+getName()+" value: text can't be empty");
	}
	
	
	/**
	 * @param conditionName The condition that needs to be satisfied for this action to be able to be performed by a player
	 * @throws GameParseException
	 */
	public void setCondition(String conditionName) throws GameParseException {
		for (PlayerID p : getData().getPlayerList().getPlayers()) {
			try {
				m_condition = RulesAttachment.get(p, conditionName);
			} catch (IllegalStateException ise) {
				/*
				 * No condition found with that name for that specific player...
				 * This is supposed to happen, ignore this exception... Only if
				 * all players throw this exception we will throw a
				 * GameParseException further down...
				 * 
				 * Veq: Is there a smarter way to check if a RulesAttachment
				 * exists at all and then get a reference to it? instead of
				 * iterating through all players getting their RulesAttachments
				 * and then referencing the one found?
				 */
			}
			if (m_condition != null)
				break;
		}
		if (m_condition == null)
			throw new GameParseException("PoliticalActionAttachment: Could not find rule. name:" + conditionName);
	}
	
	/**
	 * @return true if there is no condition to this action or if the condition is satisfied
	 */
	public boolean canPerform() {
		return m_condition == null || m_condition.isSatisfied(getData());
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param relChange
	 * @throws GameParseException
	 */
	public void setRelationshipChange(String relChange) throws GameParseException
	{
		String[] s = relChange.split(":");
		if (s.length != 3)
			throw new GameParseException("PoliticalActions: Invalid relationshipChange declaration: " + relChange + " \n Use: player1:player2:newRelation\n");
		if (getData().getPlayerList().getPlayerID(s[0]) == null)
			throw new GameParseException("PoliticalActions: Invalid relationshipChange declaration: " + relChange + " \n player: " + s[0] + " unknown in: " + getName());
		
		if (getData().getPlayerList().getPlayerID(s[1]) == null)
			throw new GameParseException("PoliticalActions: Invalid relationshipChange declaration: " + relChange + " \n player: " + s[1] + " unknown in: " + getName());
		
		if (Matches.isValidRelationshipName(getData()).invert().match(s[2]))
			throw new GameParseException("PoliticalActions: Invalid relationshipChange declaration: " + relChange + " \n relationshipType: " + s[2] + " unknown in: " + getName());
		
		m_relationshipChange.add(relChange);
	}
	
	public List<String> getRelationshipChange()
	{
		return m_relationshipChange;
	}
	/**
	 * @param text the Key that is used in politicstext.properties for all the texts
	 */
	public void setText(String text) {
		m_text = text;
	}
	
	/**
	 * @return the Key that is used in politicstext.properties for all the texts
	 */
	public String getText() {
		return m_text;
	}
	
	
	/**
	 * @param s the number you need to roll to get the action to succeed format "1:10" for 10% chance
	 * @throws GameParseException
	 */
	public void setChance(String chance) throws GameParseException {
		String[] s = chance.split(":");
		try {
			getInt(s[0]);
			getInt(s[1]);
		} catch (IllegalArgumentException iae) {
			throw new GameParseException("PoliticalActions: Invalid chance declaration: " + chance + " format: \"1:10\" for 10% chance");
		}
		m_chance = chance;
	}
	

	/**
	 * @return  the number you need to roll to get the action to succeed format "1:10" for 10% chance
	 */
	private String getChance() {
		return m_chance;
	}
	
	public int toHit() {
		return getInt(getChance().split(":")[0]);
	}
	
	public int diceSides() {
		return getInt(getChance().split(":")[1]);
	}
	
	/**
	 * @param s the amount you need to pay to perform the action
	 */
	public void setCostPU(String s) {
		m_costPU = getInt(s);
	}

	/**
	 * @return the amount you need to pay to perform the action
	 */
	public int getCostPU() {
		return m_costPU;
	}

	/**
	 * @param s
	 *            the amount of times you can try this Action per Round
	 */
	public void setAttemptsPerTurn(String s) {
		m_attemptsPerTurn = getInt(s);
		setAttemptsLeftThisTurn(s);
	}

	/**
	 * @return the amount of times you can try this Action per Round
	 */
	public int getAttemptsPerTurn() {
		return m_attemptsPerTurn;
	}

	/**
	 * @param attempts
	 *            left this turn
	 */
	public void setAttemptsLeftThisTurn(String attempts) {
		m_attemptsLeftThisTurn = getInt(attempts);
	}

	/**
	 * @return attempts that are left this turn
	 */
	public int getAttemptsLeftThisTurn() {
		return m_attemptsLeftThisTurn;
	}

	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear
	 * function as well.
	 * 
	 * @param value
	 */
	public void setActionAccept(String value)
	{
		String[] temp = value.split(":");
		for (String name : temp)
		{
			PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
			if (tempPlayer != null)
				m_actionAccept.add(tempPlayer);
			else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false"))
				m_actionAccept.clear();
			else
				throw new IllegalStateException("PoliticalActionAttachment: No player named: " + name);
		}
	}
	
	/**
	 * @return a list of players that must accept this action before it takes effect.
	 */
	public Collection<PlayerID> getActionAccept() {
		return m_actionAccept;
	}

	/**
	 * 
	 * @return a set of all other players involved in this PoliticalAction
	 */
	public Set<PlayerID> getOtherPlayers() {
		HashSet<PlayerID> otherPlayers = new HashSet<PlayerID>();
		for(String relationshipChange:m_relationshipChange) {
			String[] s = relationshipChange.split(":");
			otherPlayers.add(getData().getPlayerList().getPlayerID(s[0]));
			otherPlayers.add(getData().getPlayerList().getPlayerID(s[1]));
		}
		otherPlayers.remove( (getAttatchedTo()));
		return otherPlayers;
	}

	/**
	 * @param player
	 * @return gets the valid actions for this player.
	 */
	public static Collection<PoliticalActionAttachment> getValidActions(PlayerID player) {
			return Match.getMatches(getPoliticalActionAttachments(player), Matches.PoliticalActionCanBeAttempted);
	}

	public void resetAttempts(IDelegateBridge aBridge) {
		if (m_attemptsLeftThisTurn != m_attemptsPerTurn) {
			aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, new Integer(m_attemptsPerTurn).toString(), "attemptsLeftThisTurn"));
		}
	}

	public void useAttempt(IDelegateBridge aBridge) {
		aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, new Integer(m_attemptsLeftThisTurn - 1).toString(), "attemptsLeftThisTurn"));
	}

	public boolean hasUses() {
		return m_attemptsLeftThisTurn > 0;
	}

}
