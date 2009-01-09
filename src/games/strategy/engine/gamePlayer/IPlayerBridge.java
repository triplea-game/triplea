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
 * PlayerBridge.java
 *
 * Created on October 27, 2001, 5:23 PM
 */

package games.strategy.engine.gamePlayer;

import java.util.Properties;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.message.IRemote;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Communication with the GamePlayer goes through the PlayerBridge to 
 * make the game network transparent.
 *
 *
 */
public interface IPlayerBridge 
{
    /**
     * Return the game data
     */
    public GameData getGameData();

    /**
     * Get a remote reference to the current delegate, the type of the reference
     * is declared by the delegates getRemoteType() method
     */
    public IRemote getRemote();

    /**
     * Get a remote reference to the named delegate, the type of the reference
     * is declared by the delegates getRemoteType() method
     */
    public IRemote getRemote(String name);

    /** 
     * Get the name of the current step being exectued.
     */
    public String getStepName();

    /**
     * Get the properties for the current step.
     */
    public Properties getStepProperties();
    
    /**
     * is the game over?
     */
    public boolean isGameOver();
}