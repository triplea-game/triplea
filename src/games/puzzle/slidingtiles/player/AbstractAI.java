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

package games.puzzle.slidingtiles.player;

import games.strategy.common.player.AbstractBaseAI;

/**
 * Abstract class for an N-Puzzle AI agent.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public abstract class AbstractAI extends AbstractBaseAI implements INPuzzlePlayer
{

    public AbstractAI(String name)
    {
        super(name);
    }
         
    protected abstract void play();

    
    /**
     * The given phase has started.  Parse the phase name and call the appropriate method.
     */ 
    public void start(String stepName)
    {
        if (stepName.endsWith("Play"))
            play();
    }
    
    public final Class<INPuzzlePlayer> getRemotePlayerType()
    {
        return INPuzzlePlayer.class;
    }
    
}
