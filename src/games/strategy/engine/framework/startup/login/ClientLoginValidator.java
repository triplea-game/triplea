package games.strategy.engine.framework.startup.login;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.lobby.server.userDB.BannedMacController;
import games.strategy.engine.lobby.server.userDB.MutedIpController;
import games.strategy.engine.lobby.server.userDB.MutedMacController;
import games.strategy.net.ILoginValidator;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Version;
import java.net.InetSocketAddress;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 * 
 * If we require a password, then we challenge the client with a salt value, the salt
 * being different for each login attempt. .  The client hashes the password entered by 
 * the user with this salt, and sends it back to us.  This prevents the password from
 * travelling over the network in plain text, and also prevents someone listening on 
 * the connection from getting enough information to log in (since the salt will change
 * on the next login attempt)
 * 
 * @author sgb
 */
public class ClientLoginValidator implements ILoginValidator
{
    public static final String SALT_PROPERTY = "Salt";
    public static final String PASSWORD_REQUIRED_PROPERTY = "Password Required";
    static final String YOU_HAVE_BEEN_BANNED = "The host has banned you from this game";
    static final String UNABLE_TO_OBTAIN_MAC = "Unable to obtain mac address";
    static final String INVALID_MAC = "Invalid mac address";

    //A hack, till I think of something better
    private static ClientLoginValidator s_instance;
    public static ClientLoginValidator getInstance()
    {
        return s_instance;
    }
    public ClientLoginValidator()
    {
        s_instance = this;
    }
    
    private String m_password;
    
    /**
     * Set the password required for the game, or to null if no password is required.
     * 
     */
    public void setGamePassword(String password)
    {
        m_password = password;
    }

    public Map<String,String> getChallengeProperties(String userName, SocketAddress remoteAddress)
    {
         
        Map<String,String> challengeProperties = new HashMap<String,String>();
        
        challengeProperties.put("Sever Version", EngineVersion.VERSION.toString());
        
        
        
        if(m_password != null)
        {
            
            /**
             * Get a new random salt. 
             */
            
            String encryptedPassword = MD5Crypt.crypt(m_password);
            challengeProperties.put(SALT_PROPERTY, MD5Crypt.getSalt(MD5Crypt.MAGIC, encryptedPassword));
            challengeProperties.put(PASSWORD_REQUIRED_PROPERTY, Boolean.TRUE.toString());
        }
        else
        {
            challengeProperties.put(PASSWORD_REQUIRED_PROPERTY, Boolean.FALSE.toString());
        }
        
        
        return challengeProperties;
    }
    private final Object m_cachedListLock = new Object();
    //We need to cache the mac addresses of players, because otherwise the client would have to send their mac address with each message, which would cause more network lag
    private HashMap<String, String> m_cachedMacAddresses = new HashMap<String, String>();
    public HashMap<String, String> GetMacAddressesOfPlayers()
    {
        synchronized(m_cachedListLock)
        {
            return m_cachedMacAddresses;
        }
    }
    private HashSet<String> m_bannedIpAddresses = new HashSet<String>();
    public void NotifyIPBanningOfPlayer(String ip)
    {
        synchronized (m_cachedListLock)
        {
            m_bannedIpAddresses.add(ip);
        }
    }
    private HashSet<String> m_bannedMacAddresses = new HashSet<String>();
    public void NotifyMacBanningOfPlayer(String mac)
    {
        synchronized (m_cachedListLock)
        {
            m_bannedMacAddresses.add(mac);
        }
    }

    private HashSet<String> m_mutedIpAddresses = new HashSet<String>();
    public HashSet<String> GetIpAddressesOfMutedPlayers()
    {
        synchronized (m_cachedListLock)
        {
            return m_mutedIpAddresses;
        }
    }
    public void NotifyIPMutingOfPlayer(String ip)
    {
        synchronized (m_cachedListLock)
        {
            m_mutedIpAddresses.add(ip);
        }
    }
    private HashSet<String> m_mutedMacAddresses = new HashSet<String>();
    public HashSet<String> GetMacAddressesOfMutedPlayers()
    {
        synchronized (m_cachedListLock)
        {
            return m_mutedMacAddresses;
        }
    }
    public void NotifyMacMutingOfPlayer(String mac)
    {
        synchronized (m_cachedListLock)
        {
            m_mutedMacAddresses.add(mac);
        }
    }
    public String verifyConnection(Map<String, String> propertiesSentToClient, Map<String, String> propertiesReadFromClient, String clientName, String clientMac, SocketAddress remoteAddress)
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

        String remoteIp = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
        if(clientMac == null)
        {
            return UNABLE_TO_OBTAIN_MAC;
        }
        if(!clientMac.matches("[0-9A-F.]+") || clientMac.length() != 17) //Must match this form exactly[00.1E.F3.C8.FC.E6] and have an exact length of 17
        {
            return INVALID_MAC;
        }

        synchronized(m_cachedListLock)
        {
            if (m_bannedIpAddresses.contains(remoteIp))
            {
                return YOU_HAVE_BEEN_BANNED;
            }
            if (m_bannedMacAddresses.contains(clientMac))
            {
                return YOU_HAVE_BEEN_BANNED;
            }

            m_cachedMacAddresses.put(clientName, clientMac);
        }
        
        

        if(propertiesSentToClient.get(PASSWORD_REQUIRED_PROPERTY).equals(Boolean.TRUE.toString()) )
        {
            String readPassword = propertiesReadFromClient.get(ClientLogin.PASSWORD_PROPERTY);
            if(readPassword == null)
            {
                return "No password";
            }
            
                        
            if(!readPassword.equals( MD5Crypt.crypt(m_password, propertiesSentToClient.get(SALT_PROPERTY)) ))
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
