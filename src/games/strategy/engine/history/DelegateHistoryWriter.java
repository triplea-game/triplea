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

package games.strategy.engine.history;

import games.strategy.net.*;

/**
 * Has a subset of the historyWriters functionality.
*  Delegates should only have access to these functions.
*  The rest of the history writers functions should only
*  be used by the GameData
 */

public class DelegateHistoryWriter
{
    private final HistoryWriter m_historyWriter;
    private final IServerMessenger m_messenger;

    public DelegateHistoryWriter(HistoryWriter writer, IServerMessenger messenger)
    {
        m_historyWriter = writer;
        m_messenger = messenger;
    }

    public void startEvent(String eventName)
    {
        RemoteHistoryMessage msg = new RemoteHistoryMessage(eventName);
        msg.perform(m_historyWriter);
        m_messenger.broadcast(msg);
    }

    public void addChildToEvent(String child)
    {
      addChildToEvent(child, null);
    }

    public void addChildToEvent(String child, Object renderingData)
    {
        RemoteHistoryMessage msg = new RemoteHistoryMessage(child, renderingData);
        msg.perform(m_historyWriter);
        m_messenger.broadcast(msg);
    }

    /**
     * Set the redering data for the current event.
     */
    public void setRenderingData(Object renderingData)
    {
        RemoteHistoryMessage msg = new RemoteHistoryMessage(renderingData);
        msg.perform(m_historyWriter);
        m_messenger.broadcast(msg);
    }


}
