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
package games.strategy.triplea.ui.display;

import games.strategy.engine.display.IDisplayBridge;
import games.strategy.triplea.ui.TripleAFrame;

/**
 * 
 *
 *
 * @author Sean Bridges
 */
public class TripleaDisplay implements ITripleaDisplay
{
    private IDisplayBridge m_displayBridge;
    private final TripleAFrame m_ui;
    
    /**
     * @param ui
     */
    public TripleaDisplay(final TripleAFrame ui)
    {
        m_ui = ui;
    }
    /* 
     * @see games.strategy.engine.display.IDisplay#initialize(games.strategy.engine.display.IDisplayBridge)
     */
    public void initialize(IDisplayBridge bridge)
    {
       m_displayBridge = bridge;
        
    }
    
    
    
    
}
