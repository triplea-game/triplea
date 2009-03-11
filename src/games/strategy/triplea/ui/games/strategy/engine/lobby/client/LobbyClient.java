package games.strategy.engine.lobby.client;

import games.strategy.debug.HeartBeat;
import games.strategy.engine.lobby.server.IModeratorController;
import games.strategy.engine.lobby.server.ModeratorController;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.Messengers;

public class LobbyClient
{
    private final Messengers m_messengers;
    
    private final boolean m_isAnonymousLogin;
    private Boolean isAdmin;
    
    public LobbyClient(IMessenger messenger, boolean anonymousLogin)
    {
        m_messengers = new Messengers(messenger);
        m_isAnonymousLogin = anonymousLogin;
        
        //add a heart beat server, to allow the server to ping us
        //we only respond to the server
        HeartBeat heartBeatServer = new HeartBeat(m_messengers.getMessenger().getServerNode());
        
        m_messengers.getRemoteMessenger().registerRemote(heartBeatServer, HeartBeat.getHeartBeatName(m_messengers.getMessenger().getLocalNode()));
    }


    public boolean isAdmin() 
    {
        if(isAdmin == null) 
        {
            IModeratorController controller = (IModeratorController) m_messengers.getRemoteMessenger().getRemote(ModeratorController.getModeratorControllerName());
            isAdmin = controller.isAdmin();
        }
        return isAdmin;
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
