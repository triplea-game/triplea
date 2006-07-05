package games.strategy.engine.lobby.client;

import games.strategy.engine.message.*;
import games.strategy.net.IMessenger;

public class LobbyClient
{
    
    private final IMessenger m_messenger;
    private final IRemoteMessenger m_remoteMessenger;
    private final IChannelMessenger m_channelMessenger;
    
    private final boolean m_isAnonymousLogin;
    
    public LobbyClient(IMessenger messenger, boolean anonymousLogin)
    {
        m_messenger = messenger;
        m_isAnonymousLogin = anonymousLogin;
        UnifiedMessenger um = new UnifiedMessenger(m_messenger);
        m_remoteMessenger = new RemoteMessenger(um);
        m_channelMessenger = new ChannelMessenger(um);
    }


    public boolean isAnonymousLogin()
    {
        return m_isAnonymousLogin;
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
