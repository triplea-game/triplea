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

package games.puzzle.slidingtiles.ui;

import games.strategy.common.ui.IPlayData;
import games.strategy.engine.data.Territory;

/**
 * Represents a play in an n-puzzle game.
 * 
 * A play has a start Territory and an end territory,
 * which correspond to the piece to be moved, and the destination for the move.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2008-01-09 08:52:33 -0600 (Wed, 09 Jan 2008) $
 */
public class PlayData implements IPlayData
{
    private Territory m_start;
    private Territory m_end;
    
    /**
     * Construct a new play, with the given start location and end location.
     * 
     * @param start <code>Territory</code> where the play should start
     * @param end <code>Territory</code> where the play should end
     */
    public PlayData(Territory start, Territory end)
    {
        m_start = start;
        m_end = end;
    }
    
    
    /**
     * Returns the start location for this play.
     * @return <code>Territory</code> where this play starts.
     */
    public Territory getStart()
    {
        return m_start;
    }

    
    /**
     * Returns the end location for this play.
     * @return <code>Territory</code> where this play ends.
     */
    public Territory getEnd() 
    {
        return m_end;
    }
}
