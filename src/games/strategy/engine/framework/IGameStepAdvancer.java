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
package games.strategy.engine.framework;

import games.strategy.engine.data.PlayerID;
import games.strategy.net.IRemote;

/**
 *
 * @author Sean Bridges
 */
public interface IGameStepAdvancer extends IRemote
{
    /**
     * A server calls this methods on client game when a player
     * starts a certain step.
     *
     * The method should not return until the player has finished the step.
     */
    
    public void startPlayerStep(String stepName, PlayerID player);
}
