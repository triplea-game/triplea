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

package games.puzzle.tictactoe.player;

import games.strategy.common.player.AbstractBaseAI;

/**
 * Abstract class for a King's Table AI agent.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2007-06-19 13:39:15 -0500 (Tue, 19 Jun 2007) $
 */
public abstract class AbstractAI extends AbstractBaseAI implements ITicTacToePlayer
{

    public AbstractAI(String name)
    {
        super(name);
    }
         
    protected abstract void play();

    
    /**
     * The given phase has started.  Parse the phase name and call the appropiate method.
     */ 
    public void start(String stepName)
    {
        if (stepName.endsWith("Play"))
            play();
    }
    
    public final Class<ITicTacToePlayer> getRemotePlayerType()
    {
        return ITicTacToePlayer.class;
    }
    
}
