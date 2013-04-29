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
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.triplea.Constants;
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
public class PoliticalActionAttachment extends AbstractUserActionAttachment implements ICondition
{
	private static final long serialVersionUID = 4392770599777282477L;
	
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
			if (a.getName().startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX) && a instanceof PoliticalActionAttachment)
				returnList.add((PoliticalActionAttachment) a);
		}
		return returnList;
	}
	
	public static PoliticalActionAttachment get(final PlayerID player, final String nameOfAttachment)
	{
		return get(player, nameOfAttachment, null);
	}
	
	public static PoliticalActionAttachment get(final PlayerID player, final String nameOfAttachment, final Collection<PlayerID> playersToSearch)
	{
		PoliticalActionAttachment rVal = (PoliticalActionAttachment) player.getAttachment(nameOfAttachment);
		if (rVal == null)
		{
			if (playersToSearch == null)
			{
				throw new IllegalStateException("PoliticalActionAttachment: No attachment for:" + player.getName() + " with name: " + nameOfAttachment);
			}
			else
			{
				for (final PlayerID otherPlayer : playersToSearch)
				{
					if (otherPlayer == player)
						continue;
					rVal = (PoliticalActionAttachment) otherPlayer.getAttachment(nameOfAttachment);
					if (rVal != null)
						return rVal;
				}
				throw new IllegalStateException("PoliticalActionAttachment: No attachment for:" + player.getName() + " with name: " + nameOfAttachment);
			}
		}
		return rVal;
	}
	
	// list of relationship changes to be performed if this action is performed sucessfully
	private ArrayList<String> m_relationshipChange = new ArrayList<String>();
	
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
					Matches.AbstractUserActionAttachmentCanBeAttempted(testedConditions), Matches.politicalActionAffectsAtLeastOneAlivePlayer(player, data)));
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		super.validate(data);
		if (m_relationshipChange.isEmpty())
			throw new GameParseException("value: relationshipChange can't be empty" + thisErrorMsg());
	}
}
