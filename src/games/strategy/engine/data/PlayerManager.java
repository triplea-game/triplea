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

package games.strategy.engine.data;

import games.strategy.net.INode;

import java.util.*;

/**
 * Tracks what Node in the networks is playing which roles in the game.
 * 
 * @author sgb
 */
public class PlayerManager
{
    private final Map<String, INode> m_playerMapping;
    
    public PlayerManager(Map<String, INode> map)
    {
        m_playerMapping = new HashMap<String, INode>(map);
    }
    
    public Map<String, INode> getPlayerMapping()
    {
        return new HashMap<String, INode>(m_playerMapping);
    }
    
    public INode getNode(String playerName)
    {
        return m_playerMapping.get(playerName);
    }
    
    /**
     * Is the given node playing as anyone. 
     */
    public boolean isPlaying(INode node)
    {
        return m_playerMapping.containsValue(node);
    }
    
    public Set<String> getPlayers()
    {
        return new HashSet<String>(m_playerMapping.keySet());
    }
    
    public Set<String> getPlayedBy(String playerName)
    {
        Set<String> rVal = new HashSet<String>();
        for(String player : m_playerMapping.keySet())
        {
            if(m_playerMapping.get(player).getName().equals(playerName) )
            {
                rVal.add(player);
            }
        }
        return rVal;
    }
    
    
    /**
     * Get a player from an opposing side, if possible, else
     * get a player playing at a remote computer, if possible 
     */
    public PlayerID getRemoteOpponent(INode localNode, GameData data)
    {
        //find a local player
        PlayerID local = null;
        for(String player : m_playerMapping.keySet())
        {
            if(m_playerMapping.get(player).equals(localNode))
            {
                local = data.getPlayerList().getPlayerID(player); 
                break;
            }
        }
        
        //we arent playing anyone, return any
        if(local == null)
        {
            String remote = m_playerMapping.keySet().iterator().next();
            return data.getPlayerList().getPlayerID(remote);
        }
        
        String any = null;
        for(String player : m_playerMapping.keySet())
        {
            if(!m_playerMapping.get(player).equals(localNode))
            {
                any = player;
                PlayerID remotePlayerID = data.getPlayerList().getPlayerID(player);
                if(!data.getAllianceTracker().isAllied(local, remotePlayerID))
                {
                    return remotePlayerID;
                }
            }
        }
        
        //no un allied players were found, any will do
        return data.getPlayerList().getPlayerID(any);
    
    }
    

    
    
}
