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

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;

/**
 * Has a subset of the historyWriters functionality.
*  Delegates should only have access to these functions.
*  The rest of the history writers functions should only
*  be used by the GameData
 */

public class TripleADelegateHistoryWriter implements IDelegateHistoryWriter
{
    IDelegateBridge m_bridge;
    GameData m_data;

    public TripleADelegateHistoryWriter(IDelegateBridge bridge, GameData data)
    {
        m_bridge = bridge;
        m_data = data;
    }

    public String getEventPrefix()
    {
        if (EditDelegate.getEditMode(m_data))
            return "EDIT: ";
        return "";
    }

    public void startEvent(String eventName)
    {
        if (eventName.startsWith("COMMENT: "))
            m_bridge.getHistoryWriter().startEvent(eventName); 
        else
            m_bridge.getHistoryWriter().startEvent(getEventPrefix()+eventName); 
    }

    public void addChildToEvent(String child)
    {
        if (child.startsWith("COMMENT: "))
            m_bridge.getHistoryWriter().addChildToEvent(child, null); 
        else
            m_bridge.getHistoryWriter().addChildToEvent(getEventPrefix()+child, null); 
    }

    public void addChildToEvent(String child, Object renderingData)
    {
        if (child.startsWith("COMMENT: "))
            m_bridge.getHistoryWriter().addChildToEvent(child, renderingData); 
        else
            m_bridge.getHistoryWriter().addChildToEvent(getEventPrefix()+child, renderingData); 
    }

    /**
     * Set the rendering data for the current event.
     */
    public void setRenderingData(Object renderingData)
    {
        m_bridge.getHistoryWriter().setRenderingData(renderingData);
    }


}
