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

import games.strategy.engine.framework.*;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.net.*;

/**
 * Has a subset of the historyWriters functionality.
*  Delegates should only have access to these functions.
*  The rest of the history writers functions should only
*  be used by the GameData
 */

public class DelegateHistoryWriter
{
    private final IChannelMessenger m_messenger;

    public DelegateHistoryWriter(IChannelMessenger messenger)
    {
        m_messenger = messenger;
    }

    private IGameModifiedChannel getGameModifiedChannel()
    {
        return (IGameModifiedChannel) m_messenger.getChannelBroadcastor(IGame.GAME_MODIFICATION_CHANNEL);
    }
    
    public void startEvent(String eventName)
    {
        getGameModifiedChannel().startHistoryEvent(eventName);

    }

    public void addChildToEvent(String child)
    {
      addChildToEvent(child, null);
    }

    public void addChildToEvent(String child, Object renderingData)
    {
        getGameModifiedChannel().addChildToEvent(child, renderingData);

    }

    /**
     * Set the redering data for the current event.
     */
    public void setRenderingData(Object renderingData)
    {
        getGameModifiedChannel().setRenderingData(renderingData);
    }


}
