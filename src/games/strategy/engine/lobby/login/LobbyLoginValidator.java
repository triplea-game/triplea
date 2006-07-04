package games.strategy.engine.lobby.login;

import java.net.SocketAddress;
import java.util.*;

import games.strategy.net.ILoginValidator;

public class LobbyLoginValidator implements ILoginValidator
{

    public Map<String, String> getChallengeProperties(String userName, SocketAddress remoteAddress)
    {
        return new HashMap<String,String>();
    }

    public String verifyConnection(Map<String, String> propertiesSentToClient, Map<String, String> propertiesReadFromClient, String clientName, SocketAddress remoteAddress)
    {
        return "Error";
    }

}
