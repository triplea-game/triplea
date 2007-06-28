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
 * @version $LastChangedDate$
 * @see "Chapter 6 of Artificial Intelligence, 2nd ed. by Stuart Russell & Peter Norvig"
 */
public abstract class GameState<Play>
{
    public abstract GameState getSuccessor(Play move);

    public abstract Play getMove();

    public abstract Collection<GameState> successors();

    public abstract float getUtility();

    public abstract boolean gameIsOver();
    
}
