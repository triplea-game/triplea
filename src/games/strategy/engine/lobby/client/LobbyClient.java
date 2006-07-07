package games.strategy.engine.lobby.client;

import games.strategy.engine.message.*;
import games.strategy.net.*;

public class LobbyClient
{
    private final Messengers m_messengers;
    
    private final boolean m_isAnonymousLogin;
    
    public LobbyClient(IMessenger messenger, boolean anonymousLogin)
    {
        m_messengers = new Messengers(messenger);
        m_isAnonymousLogin = anonymousLogin;
    }


    public boolean isAnonymousLogin()
    {
        return m_isAnonymousLogin;
    }
    
    public IChannelMessenger getChannelMessenger()
    {
        return m_messengers.getChannelMessenger();
    }


    public IMessenger getMessenger()
    {
        return m_messengers.getMessenger();
    }


    public IRemoteMessenger getRemoteMessenger()
    {
        return m_messengers.getRemoteMessenger();
    }


    public Messengers getMessengers()
    {
        return m_messengers;
    }

    
}
