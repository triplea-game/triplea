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

import games.strategy.engine.display.IDisplayBridge;
import games.puzzle.slidingtiles.ui.NPuzzleFrame;

/**
 * Display for an n-puzzle game.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2008-01-09 08:52:33 -0600 (Wed, 09 Jan 2008) $
 */
public class NPuzzleDisplay implements INPuzzleDisplay
{
    private IDisplayBridge m_displayBridge;
    private final NPuzzleFrame m_ui;

    
    /**
     * Construct a new display for an n-puzzle game.
     * 
     * The display
     * @param ui
     * @see games.strategy.engine.display.IDisplay
     */
    public NPuzzleDisplay(final NPuzzleFrame ui)
    {
        m_ui = ui;
    }
    
    
    /** 
     * @see games.strategy.engine.display.IDisplay#initialize(games.strategy.engine.display.IDisplayBridge)
     */
    public void initialize(IDisplayBridge bridge)
    {
       m_displayBridge = bridge;
       m_displayBridge.toString();
        
    }
 
    /**
     * Process a user request to exit the program.
     * 
     * @see games.strategy.engine.display.IDisplay#shutdown()
     */
    public void shutDown()
    {
        m_ui.stopGame();
    }
    
    /**
     * Graphically notify the user of the current game status.
     * @param error the status message to display
     */ 
    public void setStatus(String status) 
    {
        m_ui.setStatus(status);
    }
    
    /**
     * Set the game over status for this display to <code>true</code>.
     */
    public void setGameOver()
    {
        m_ui.setGameOver();
    }
    
    
    /**
     * Ask the user interface for this display to update.
     */
    public void performPlay()
    {   
    	m_ui.performPlay();
    }

    public void initializeBoard()
    {
        m_ui.initializeTiles();
    }
}
