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
package games.strategy.common.player.ai;

import java.util.Collection;

/**
 * Abstract class representing game state, for use by AI game algorithms.
 *
 * @param <Play> class capable of representing a game play
 * @author Lane Schwartz
 * @version $LastChangedDate: 2007-12-14 08:58:18 -0600 (Fri, 14 Dec 2007) $
 * @see "Chapter 6 of Artificial Intelligence, 2nd ed. by Stuart Russell & Peter Norvig"
 */
public abstract class GameState<Play>
{
    /**
     * Get the state which will result from performing the specified play.
     * 
     * @param play a legal game play
     * @return the state which will result from performing the specified play
     */
    public abstract GameState<Play> getSuccessor(Play play);

    /**
     * Get the play which resulted in this state.
     * @return the play which resulted in this state
     */
    public abstract Play getMove();

    /**
     * Get the collection of all states which can be reached from this state by performing a legal play.
     * 
     * @return <code>Collection</code> of successor states
     */
    public abstract Collection<GameState<Play>> successors();

    /** 
     * Get the utility (or heuristic evaluation score) for this state.
     * @return the utility (or heuristic evaluation score) for this state
     */
    public abstract float getUtility();

    /** 
     * Test to see if the current state represents an endgame state.
     * @return <code>true</code> this state represents an endgame state, <code>false</code> otherwise.
     */
    public abstract boolean gameIsOver();
    
    /** 
     * Test to see if the current state represents a pseudu-terminal state.
     * This method is used during alpha-beta pruning.
     * <p>
     * If this method returns <code>true</code>, 
     * then <code>successors()</code> must return a non-empty <code>Collection</code>.
     * <p>
     * Likewise, if this method returns <code>false</code>,
     * then <code>successors()</code> must return an empty <code>Collection</code>.
     * <p>
     * All endgame states are pseudo-terminal states.
     * Additionally, any state which the AI search algorithm should not search beyond are pseudo-terminal states.
     * @return <code>true</code> this state represents a pseudo-terminal state, <code>false</code> otherwise.
     */
    public abstract boolean cutoffTest();
    
}
