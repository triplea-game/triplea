package games.strategy.engine.lobby.server.login;

import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.userDB.BadWordController;
import games.strategy.engine.lobby.server.userDB.BannedIpController;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.net.ILoginValidator;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Version;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class LobbyLoginValidator implements ILoginValidator
{
    static final String THATS_NOT_A_NICE_NAME = "Thats not a nice name";
    static final String YOUR_IP_HAS_BEEN_BANNED = "Your ip has been banned";

    
    private final static Logger s_logger = Logger.getLogger(LobbyLoginValidator.class.getName());

    public static final String LOBBY_VERSION = "LOBBY_VERSION";
    public static final String REGISTER_NEW_USER_KEY = "REGISTER_USER";
    public static final String ANONYMOUS_LOGIN = "ANONYMOUS_LOGIN";
    public static final String LOGIN_KEY = "LOGIN";
    public static final String HASHED_PASSWORD_KEY = "HASHEDPWD";
    
    public static final String EMAIL_KEY = "EMAIL";
    public static final String SALT_KEY = "SALT";
    
    public Map<String, String> getChallengeProperties(String userName, SocketAddress remoteAddress)
    {
        //we need to give the user the salt key for the username
        String password = new DBUserController().getPassword(userName);
        Map<String, String> rVal = new HashMap<String,String>();
        if(password != null)
            rVal.put(SALT_KEY, MD5Crypt.getSalt(MD5Crypt.MAGIC, password) );
        
        return rVal;
    }
    
    public String verifyConnection(Map<String, String> propertiesSentToClient, Map<String, String> propertiesReadFromClient, String clientName, SocketAddress remoteAddress)
    {
        String error = verifyConnectionInternal(propertiesSentToClient, propertiesReadFromClient, clientName, remoteAddress);
        
        if(error != null)
        {
            s_logger.info("Bad login attemp from " + remoteAddress + " for user " + clientName + " error:" + error);
            AccessLog.failedLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress(), error);
        }
        else
        {
            s_logger.info("Successful login from:" + remoteAddress + " for user:" + clientName);
            AccessLog.successfulLogin(clientName, ((InetSocketAddress) remoteAddress).getAddress());
        }
        
        return error;
    }

    private String verifyConnectionInternal(Map<String, String> propertiesSentToClient, Map<String, String> propertiesReadFromClient, String clientName, SocketAddress remoteAddress)
    {
        String clientVersionString = propertiesReadFromClient.get(LOBBY_VERSION);
        if(clientVersionString == null)
            return "No Client Version";
        Version clientVersion = new Version(clientVersionString);
        
        if(!clientVersion.equals(LobbyServer.LOBBY_VERSION))
        {
            return "Wrong version, we require" + LobbyServer.LOBBY_VERSION.toString() + " but trying to log in with " + clientVersionString;
        }
        
        for(String s : getBadWords()) 
        {
            if(clientName.toLowerCase().contains(s.toLowerCase())) 
            {
                return THATS_NOT_A_NICE_NAME;
            }
        }
        
        String remoteIp = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
        if(new BannedIpController().isIpBanned(remoteIp)) {
            return YOUR_IP_HAS_BEEN_BANNED;
        }
        
       
        if(propertiesReadFromClient.containsKey(REGISTER_NEW_USER_KEY))
        {
            return createUser(propertiesReadFromClient, clientName);
        }
        if(propertiesReadFromClient.containsKey(ANONYMOUS_LOGIN))
        {
            return anonymousLogin(propertiesReadFromClient, clientName);
        }
        else
        {
           return validatePassword(propertiesSentToClient, propertiesReadFromClient, clientName);
        }
    }

    private List<String> getBadWords()
    {
        return new BadWordController().list();
    }
    

    private String validatePassword(Map<String, String> propertiesSentToClient, Map<String, String> propertiesReadFromClient, String clientName)
    {
        DBUserController userController = new DBUserController();
        if(!userController.login(clientName, propertiesReadFromClient.get(HASHED_PASSWORD_KEY)))
        {
            if(userController.doesUserExist(clientName))
            {
                return "Incorrect password";
            }
            else
            {
                return "Username does not exist";
            }
        }
        else
        {
            return null;
        }
        
    }

    private String anonymousLogin(Map<String, String> propertiesReadFromClient,String  userName)
    {
       if(userName.toLowerCase().indexOf("admin") != -1)
           return "You can't log in anonymously with admin in the name";
        
       if(!new DBUserController().doesUserExist(userName))
       {
           return null;
       }
       return "Can't login anonymously, username already exists";
    }

    private String createUser(Map<String, String> propertiesReadFromClient, String userName)
    {
        String email = propertiesReadFromClient.get(EMAIL_KEY);
        String hashedPassword = propertiesReadFromClient.get(HASHED_PASSWORD_KEY);
        
        DBUserController controller = new DBUserController();
        String error = controller.validate(userName, email, hashedPassword); 
        if(error != null)
        {
            return error;
        }
        
        try
        {
            controller.createUser(userName, email, hashedPassword, false);
            return null;
        }
        catch(IllegalStateException ise)
        {
            return ise.getMessage();
        }
        
    }

}
