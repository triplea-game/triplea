/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * PlayerList.java
 *
 * Created on October 17, 2001, 9:21 PM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public class PlayerList extends GameDataComponent
{

	//maps String playerName -> PlayerID
	private final Map m_players = new HashMap();
	
	/** Creates new PlayerCollection */
    public PlayerList(GameData data) 
	{
		super(data);
		m_players.put(PlayerID.NULL_PLAYERID.getName(), PlayerID.NULL_PLAYERID);
    }
	
	protected void addPlayerID(PlayerID player)
	{
		m_players.put(player.getName(), player);
	}
	
	public int size()
	{
		return m_players.size();
	}
	
	public PlayerID getPlayerID(String name)
	{
		return (PlayerID) m_players.get(name);
	}
	
	public String[] getNames()
	{
		String[] values = new String[size()];
		m_players.keySet().toArray(values);
		return values;
	}
	
	public Collection getPlayers()
	{
		return m_players.values();
	}
}

