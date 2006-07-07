/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.engine.chat;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import games.strategy.net.*;

public class StatusManager
{
    private final List<IStatusListener> m_listeners = new CopyOnWriteArrayList<IStatusListener>();

    private Map<INode, String> m_status = new HashMap<INode, String>();

    private final Messengers m_messengers;

    private final Object m_mutex = new Object();

    private IStatusChannel m_statusChannelSubscribor;

    public StatusManager(final Messengers messengers)
    {
        m_messengers = messengers;

        m_statusChannelSubscribor = new IStatusChannel()
        {
            public void statusChanged(INode node, String status)
            {
                synchronized (m_mutex)
                {
                    if (status == null)
                    {
                        m_status.remove(node);
                    } else
                    {
                        m_status.put(node, status);
                    }

                }

                notifyStatusChanged(node, status);
            }

        };

        if (messengers.getMessenger().isServer() && !messengers.getRemoteMessenger().hasRemote(IStatusController.STATUS_CONTROLLER))
        {
            StatusController controller = new StatusController(messengers);
            messengers.getRemoteMessenger().registerRemote(IStatusController.class, controller, IStatusController.STATUS_CONTROLLER);
            messengers.getChannelMessenger().createChannel(IStatusChannel.class,  IStatusChannel.STATUS_CHANNEL);
        }

        m_messengers.getChannelMessenger().registerChannelSubscriber(m_statusChannelSubscribor, IStatusChannel.STATUS_CHANNEL);
        
        IStatusController controller = (IStatusController) m_messengers.getRemoteMessenger().getRemote(IStatusController.STATUS_CONTROLLER);
        Map<INode, String> values = controller.getAllStatus();

        synchronized (m_mutex)
        {
            m_status.putAll(values);
            // at this point we are just being constructed, so we have no
            // listeners
            // and we do not need to notify if anything has changed
        }



        
    }

    public void shutDown()
    {
        m_messengers.getChannelMessenger().unregisterChannelSubscriber(m_statusChannelSubscribor, IStatusChannel.STATUS_CHANNEL);
    }

    /**
     * Get the status for the given node.
     */
    public String getStatus(INode node)
    {
        synchronized (m_mutex)
        {
            return m_status.get(node);
        }

    }
    
    public void setStatus(String status)
    {
        IStatusController controller = (IStatusController) m_messengers.getRemoteMessenger().getRemote(IStatusController.STATUS_CONTROLLER);
        controller.setStatus(status);
    }

    public void addStatusListener(IStatusListener listener)
    {
        m_listeners.add(listener);
    }

    public void removeStatusListener(IStatusListener listener)
    {
        m_listeners.remove(listener);
    }

    private void notifyStatusChanged(INode node, String newStatus)
    {
        for (IStatusListener listener : m_listeners)
        {
            listener.statusChanged(node, newStatus);
        }
    }

}
