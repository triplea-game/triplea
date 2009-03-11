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

package games.strategy.kingstable.attachments;

import games.strategy.engine.data.DefaultAttachment;

/**
 * Territory attachment for King's Table.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2007-06-19 13:39:15 -0500 (Tue, 19 Jun 2007) $
 */
public class TerritoryAttachment extends DefaultAttachment
{
    private boolean m_isKingsSquare = false;
    private boolean m_isKingsExit = false;
    
    /** Creates new TerritoryAttatchment */
    public TerritoryAttachment()
    {
    }
    
    public void setKingsSquare(String value)
    {
        m_isKingsSquare = getBool(value);
    }

    public boolean isKingsSquare()
    {
        return m_isKingsSquare;
    }

    public void setKingsExit(String value)
    {
        m_isKingsExit = getBool(value);
    }
    
    public boolean isKingsExit()
    {
        return m_isKingsExit;
    }
}
