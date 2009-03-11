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

package games.strategy.common.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;

/**
 * 
 * @author Lane Schwartz
 */
public abstract class AbstractBasePlayer implements IGamePlayer
{
    protected final String m_name;
    protected PlayerID m_id;
    protected IPlayerBridge m_bridge;
    
    /** 
     * @param name - the name of the player.
     */
    public AbstractBasePlayer(String name)
    {
        m_name = name;
    }
    
    
    public void initialize(IPlayerBridge bridge, PlayerID id)
    {
        m_bridge = bridge;
        m_id = id;
    }
    
    /**
     * Get the GameData for the game.
     */
    protected final GameData getGameData()
    {
        return m_bridge.getGameData();
    }
    
    /**
     * Get the IPlayerBridge for this game player.
     */
    protected final IPlayerBridge getPlayerBridge()
    {
        return m_bridge;
    }
    
    
    public final String getName()
    {
        return m_name;
    }

    public final PlayerID getID()
    {
        return m_id;
    }

    
    /**
     * The given phase has started.  We parse the phase name and call the apropiate method.
     */
    public abstract void start(String stepName);
    
    //public abstract Class<?> getRemotePlayerType();
    
}
