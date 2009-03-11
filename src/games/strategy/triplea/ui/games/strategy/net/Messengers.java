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

package games.strategy.net;

import games.strategy.engine.message.*;

/**
 * 
 * Convenience grouping of a messenger, remote messenger and channel messenger.
 * 
 * @author sgb
 */
public class Messengers
{
    private final IMessenger m_messenger;
    private final IRemoteMessenger m_remoteMessenger;
    private final IChannelMessenger m_channelMessenger;
    
    public Messengers(IMessenger messenger)
    {
        m_messenger = messenger;
        UnifiedMessenger unifiedMessenger = new UnifiedMessenger(messenger);
        m_channelMessenger = new ChannelMessenger(unifiedMessenger);
        m_remoteMessenger = new RemoteMessenger(unifiedMessenger);
    }
    
    public Messengers(final IMessenger messenger, final IRemoteMessenger remoteMessenger, final IChannelMessenger channelMessenger)
    {
        m_messenger = messenger;
        m_remoteMessenger = remoteMessenger;
        m_channelMessenger = channelMessenger;
    }


    public IChannelMessenger getChannelMessenger()
    {
        return m_channelMessenger;
    }


    public IMessenger getMessenger()
    {
        return m_messenger;
    }


    public IRemoteMessenger getRemoteMessenger()
    {
        return m_remoteMessenger;
    }
    
    
    
    
}
