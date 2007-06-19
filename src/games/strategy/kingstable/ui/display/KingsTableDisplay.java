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

package games.strategy.kingstable.ui.display;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.kingstable.ui.KingsTableFrame;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class KingsTableDisplay implements IKingsTableDisplay
{
    private IDisplayBridge m_displayBridge;
    private final KingsTableFrame m_ui;
    
    /**
     * @param ui
     */
    public KingsTableDisplay(final KingsTableFrame ui)
    {
        m_ui = ui;
    }
    /* 
     * @see games.strategy.engine.display.IDisplay#initialize(games.strategy.engine.display.IDisplayBridge)
     */
    public void initialize(IDisplayBridge bridge)
    {
       m_displayBridge = bridge;
       m_displayBridge.toString();
        
    }
 
    public void shutDown()
    {
        m_ui.stopGame();
    }
    
    public void setStatus(String status) 
    {
        m_ui.setStatus(status);
    }
    
    public void setGameOver(boolean gameOver)
    {
        m_ui.setGameOver(gameOver);
    }
    
    public void performPlay(Territory start, Territory end, Collection<Territory> captured)
    {   m_ui.performPlay(start,end,captured);
        //m_ui.repaintGridSquare(start);
        //m_ui.repaintGridSquare(end);
    }

}
