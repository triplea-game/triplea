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
 * PlayerListinge.java
 * 
 * Created on February 1, 2002, 2:34 PM
 */
package games.strategy.engine.framework.message;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.util.Version;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * data from the server indicating what players are available to be
 * taken, and what players are being played.
 * 
 * This object also contains versioning info which the client should
 * check to ensure that it is playing the same game as the server.
 * 
 * @author Sean Bridges
 */
public class PlayerListing implements Serializable
{
	// keep compatability with older versions
	static final long serialVersionUID = -8913538086737733980L;
	/**
	 * Maps String player name -> node Name
	 * if node name is null then the player is available to play.
	 */
	private final Map<String, String> m_playerToNodeListing;
	private final Map<String, Boolean> m_playersEnabledListing;
	private final Map<String, String> m_localPlayerTypes;
	// private final Map<String, String> m_remotePlayerTypes = new HashMap<String, String>();
	private final Collection<String> m_playersAllowedToBeDisabled;
	private final Version m_gameVersion;
	private final String m_gameName;
	private final String m_gameRound;
	private final Map<String, Collection<String>> m_playerNamesAndAlliancesInTurnOrder;
	
	/**
	 * Creates a new instance of PlayerListingMessage
	 */
	public PlayerListing(final Map<String, String> playerToNodeListing, final Map<String, Boolean> playersEnabledListing, final Map<String, String> localPlayerTypes, final Version gameVersion,
				final String gameName, final String gameRound, final Collection<String> playersAllowedToBeDisabled,
				final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrderLinkedHashMap)
	{
		m_playerToNodeListing = playerToNodeListing == null ? new HashMap<String, String>() : new HashMap<String, String>(playerToNodeListing);
		m_playersEnabledListing = playersEnabledListing == null ? new HashMap<String, Boolean>() : new HashMap<String, Boolean>(playersEnabledListing);
		m_localPlayerTypes = localPlayerTypes == null ? new HashMap<String, String>() : new HashMap<String, String>(localPlayerTypes);
		m_playersAllowedToBeDisabled = playersAllowedToBeDisabled == null ? new HashSet<String>() : new HashSet<String>(playersAllowedToBeDisabled);
		m_gameVersion = gameVersion;
		m_gameName = gameName;
		m_gameRound = gameRound;
		m_playerNamesAndAlliancesInTurnOrder = new LinkedHashMap<String, Collection<String>>();
		if (playerNamesAndAlliancesInTurnOrderLinkedHashMap != null)
		{
			for (final Entry<String, Collection<String>> entry : playerNamesAndAlliancesInTurnOrderLinkedHashMap.entrySet())
			{
				m_playerNamesAndAlliancesInTurnOrder.put(entry.getKey(), new HashSet<String>(entry.getValue()));
			}
		}
	}
	
	public Collection<String> getPlayersAllowedToBeDisabled()
	{
		return m_playersAllowedToBeDisabled;
	}
	
	public Map<String, String> getPlayerToNodeListing()
	{
		return m_playerToNodeListing;
	}
	
	public Map<String, Boolean> getPlayersEnabledListing()
	{
		return m_playersEnabledListing;
	}
	
	public Map<String, Collection<String>> getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap()
	{
		return m_playerNamesAndAlliancesInTurnOrder;
	}
	
	public String getGameName()
	{
		return m_gameName;
	}
	
	public Version getGameVersion()
	{
		return m_gameVersion;
	}
	
	@Override
	public String toString()
	{
		return "PlayerListingMessage:" + m_playerToNodeListing;
	}
	
	public Set<String> getPlayers()
	{
		return m_playerToNodeListing.keySet();
	}
	
	public String getGameRound()
	{
		return m_gameRound;
	}
	
	public static Map<String, Collection<String>> collectPlayerNamesAndAlliancesInTurnOrder(final GameData data)
	{
		
		final LinkedHashMap<String, Collection<String>> map = new LinkedHashMap<String, Collection<String>>();
		for (final PlayerID player : data.getPlayerList().getPlayers())
		{
			map.put(player.getName(), data.getAllianceTracker().getAlliancesPlayerIsIn(player));
		}
		return map;
	}
	
	public Map<String, String> getLocalPlayerTypes()
	{
		return m_localPlayerTypes;
	}
}
