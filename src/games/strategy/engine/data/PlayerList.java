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

