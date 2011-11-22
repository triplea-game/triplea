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
package games.strategy.engine.data;

import games.strategy.net.INode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks what Node in the networks is playing which roles in the game.
 * 
 * @author sgb
 */
public class PlayerManager
{
	private final Map<String, INode> m_playerMapping;
	
	public PlayerManager(final Map<String, INode> map)
	{
		m_playerMapping = new HashMap<String, INode>(map);
	}
	
	public Map<String, INode> getPlayerMapping()
	{
		return new HashMap<String, INode>(m_playerMapping);
	}
	
	public Set<INode> getNodes()
	{
		return new HashSet<INode>(m_playerMapping.values());
	}
	
	public INode getNode(final String playerName)
	{
		return m_playerMapping.get(playerName);
	}
	
	/**
	 * @param node
	 *            referring node
	 * @return whether the given node playing as anyone
	 */
	public boolean isPlaying(final INode node)
	{
		return m_playerMapping.containsValue(node);
	}
	
	public Set<String> getPlayers()
	{
		return new HashSet<String>(m_playerMapping.keySet());
	}
	
	public Set<String> getPlayedBy(final String playerName)
	{
		final Set<String> rVal = new HashSet<String>();
		for (final String player : m_playerMapping.keySet())
		{
			if (m_playerMapping.get(player).getName().equals(playerName))
			{
				rVal.add(player);
			}
		}
		return rVal;
	}
	
	/**
	 * Get a player from an opposing side, if possible, else
	 * get a player playing at a remote computer, if possible
	 * 
	 * @param localNode
	 *            local node
	 * @param data
	 *            game data
	 * @return player found
	 */
	public PlayerID getRemoteOpponent(final INode localNode, final GameData data)
	{
		// find a local player
		PlayerID local = null;
		for (final String player : m_playerMapping.keySet())
		{
			if (m_playerMapping.get(player).equals(localNode))
			{
				local = data.getPlayerList().getPlayerID(player);
				break;
			}
		}
		// we arent playing anyone, return any
		if (local == null)
		{
			final String remote = m_playerMapping.keySet().iterator().next();
			return data.getPlayerList().getPlayerID(remote);
		}
		String any = null;
		for (final String player : m_playerMapping.keySet())
		{
			if (!m_playerMapping.get(player).equals(localNode))
			{
				any = player;
				final PlayerID remotePlayerID = data.getPlayerList().getPlayerID(player);
				if (!data.getRelationshipTracker().isAllied(local, remotePlayerID))
				{
					return remotePlayerID;
				}
			}
		}
		// no un allied players were found, any will do
		return data.getPlayerList().getPlayerID(any);
	}
}
