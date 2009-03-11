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
package games.strategy.engine.display;

import games.strategy.engine.message.IChannelSubscribor;

/**
 * A Display is a view of the game.
 * 
 * Displays listen on the display channel for game events.  There may be many displays
 * on a single vm, and conversly a display may interact with many IGamePlayers
 *
 *
 * @author Sean Bridges
 */
public interface IDisplay extends IChannelSubscribor
{
    /**
     * before recieving messages, this method will be called by the game engine.
     * @param bridge
     */
    public void initialize(IDisplayBridge bridge);
    
    public void shutDown();
}
