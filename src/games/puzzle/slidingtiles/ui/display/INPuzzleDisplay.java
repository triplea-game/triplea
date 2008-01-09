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


package games.puzzle.slidingtiles.ui.display;

import games.strategy.engine.display.IDisplay;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public interface INPuzzleDisplay extends IDisplay
{
    /**
     * Graphically notify the user of the current game status.
     * @param error the status message to display
     */ 
    public void setStatus(String status);
    
    /**
     * Set the game over status for this display to <code>true</code>.
     */
    public void setGameOver();
    
    /**
     * Ask the user interface for this display to update.
     */
    public void performPlay();
    
    /**
     * Initialize the board.
     */
    public void initializeBoard();
    
}
