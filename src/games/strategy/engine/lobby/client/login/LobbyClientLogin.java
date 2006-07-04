package games.strategy.engine.lobby.client.login;

import java.util.*;

import games.strategy.net.IConnectionLogin;

public class LobbyClientLogin implements IConnectionLogin
{

    public Map<String, String> getProperties(Map<String, String> challengProperties)
    {
        return new HashMap<String,String>();
    }

    public void notifyFailedLogin(String message)
    {
    
        
    }

}
