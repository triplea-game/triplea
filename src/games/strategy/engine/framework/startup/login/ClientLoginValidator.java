package games.strategy.engine.framework.startup.login;

import java.net.SocketAddress;
import java.util.*;

import games.strategy.engine.EngineVersion;
import games.strategy.net.ILoginValidator;
import games.strategy.util.*;

public class ClientLoginValidator implements ILoginValidator
{
    public static final String SALT_PROPERTY = "Salt";
    public static final String PASSWORD_REQUIRED_PROPERTY = "Password Required";
    
    
    private String m_encryptedPassword;
    
    /**
     * Set the password required for the game, or to null if no password is required.
     * 
     */
    public void setGamePassword(String password)
    {
        if(password == null)
        {
            m_encryptedPassword = null;
        }
        else
        {
            m_encryptedPassword = MD5Crypt.crypt(password);
        }
    }

    public Map<String,String> getChallengeProperties(String userName, SocketAddress remoteAddress)
    {
        Map<String,String> challengeProperties = new HashMap<String,String>();
        
        challengeProperties.put("Sever Version", EngineVersion.VERSION.toString());
        String encryptedPassword = m_encryptedPassword;
        if(encryptedPassword != null)
        {
            challengeProperties.put(SALT_PROPERTY, MD5Crypt.getSalt(MD5Crypt.MAGIC, encryptedPassword));
        }
        challengeProperties.put(PASSWORD_REQUIRED_PROPERTY, Boolean.valueOf(encryptedPassword != null).toString());
        
        return challengeProperties;
    }

    public String verifyConnection(Map<String, String> propertiesReadFromClient, String clientName, SocketAddress remoteAddress)
    {
        String versionString = propertiesReadFromClient.get(ClientLogin.ENGINE_VERSION_PROPERTY);
        if(versionString == null || versionString.length() > 20 || versionString.trim().length() == 0)
            return "Invalid version " + versionString;
        
        //check for version
        Version clientVersion = new Version(versionString);
        if(!clientVersion.equals(EngineVersion.VERSION))
        {
            String error =  "Client is using " + clientVersion + " but server requires version " + EngineVersion.VERSION;
            return error;
        }
        
        
        //check for password if we reuqire it
        String encryptedPassword = m_encryptedPassword;
        if(encryptedPassword != null)
        {
            String readPassword = propertiesReadFromClient.get(ClientLogin.PASSWORD_PROPERTY);
            if(readPassword == null)
            {
                return "No password";
            }
            else if(!readPassword.equals(encryptedPassword))
            {
                
                try
                {
                    //sleep on average 2 seconds
                    //try to prevent flooding to guess the 
                    //password
                    Thread.sleep((int) (4000 * Math.random()));
                } catch (InterruptedException e)
                {
                    //ignore
                }

                
                return "Invalid password";
            }
        }
        
        return null;
    }

    
    
}
