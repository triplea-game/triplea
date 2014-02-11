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
 * PlayerList.java
 * 
 * Created on October 17, 2001, 9:21 PM
 */
package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 */
public class PlayerList extends GameDataComponent implements Iterable<PlayerID>
{
	private static final long serialVersionUID = -3895068111754745446L;
	// maps String playerName -> PlayerID
	private final Map<String, PlayerID> m_players = new LinkedHashMap<String, PlayerID>();
	
	/**
	 * Creates new PlayerCollection
	 * 
	 * @param data
	 *            game data
	 */
	public PlayerList(final GameData data)
	{
		super(data);
	}
	
	/*public PlayerList(final PlayerList playerList)
	{
		super(playerList.getData());
		for (final PlayerID player : playerList.getPlayers())
		{
			this.addPlayerID(player);
		}
	}
	
	public PlayerList(final Collection<PlayerID> playerList, final GameData data)
	{
		super(data);
		for (final PlayerID player : playerList)
		{
			this.addPlayerID(player);
		}
	}*/
	
	protected void addPlayerID(final PlayerID player)
	{
		m_players.put(player.getName(), player);
	}
	
	public int size()
	{
		return m_players.size();
	}
	
	public PlayerID getPlayerID(final String name)
	{
		if (PlayerID.NULL_PLAYERID.getName().equals(name))
			return PlayerID.NULL_PLAYERID;
		return m_players.get(name);
	}
	
	public String[] getNames()
	{
		final String[] values = new String[size()];
		m_players.keySet().toArray(values);
		return values;
	}
	
	/**
	 * 
	 * @return a new arraylist copy of the players
	 */
	public Collection<PlayerID> getPlayers()
	{
		return new ArrayList<PlayerID>(m_players.values());
	}
	
	/**
	 * an iterator of a new arraylist copy of the players
	 */
	public Iterator<PlayerID> iterator()
	{
		return getPlayers().iterator();
	}
	
	public Collection<String> getPlayersThatMayBeDisabled()
	{
		final Collection<String> disableable = new HashSet<String>();
		for (final PlayerID p : m_players.values())
		{
			// already disabled players can not be reenabled
			if (p.getCanBeDisabled() && !p.getIsDisabled())
				disableable.add(p.getName());
		}
		return disableable;
	}
	
	public HashMap<String, Boolean> getPlayersEnabledListing()
	{
		final HashMap<String, Boolean> playersEnabledListing = new HashMap<String, Boolean>();
		for (final PlayerID p : m_players.values())
		{
			playersEnabledListing.put(p.getName(), !p.getIsDisabled());
		}
		return playersEnabledListing;
	}
}
