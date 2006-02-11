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
package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.message.IRemote;
import games.strategy.net.*;

/**
 * 
 *
 *
 * @author Sean Bridges
 */
public interface IServerStartupRemote extends IRemote
{
    /**
     * 
     * @return a listing of the players in the game
     */
    public PlayerListing getPlayerListing();
    
    public void takePlayer(INode who, String playerName);
    
    public void releasePlayer(INode who, String playerName);
    
    /**
     * Has the game already started?
     * If true, the server will call our ObserverWaitingToJoin to start the game.
     * Note, the return value may come back after our ObserverWaitingToJoin has been created 
     */
    public boolean isGameStarted(INode newNode);
    
    
}
