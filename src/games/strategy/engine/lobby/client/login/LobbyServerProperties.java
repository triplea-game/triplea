package games.strategy.engine.lobby.client.login;



import java.io.*;
import java.net.URL;
import java.util.Properties;
import java.util.logging.*;

/**
 * Server properties.<p>
 * 
 * Generally there is one lobby server, but that server may move.<p>
 * 
 * To keep track of this, we always have a properties file in a constant location that points
 * to the current lobby server.<p>
 * 
 * The properties file may indicate that the server is not available using the 
 * ERROR_MESSAGE key.<p>
 * 
 * 
 * @author sgb
 */
public class LobbyServerProperties
{
    private final static Logger s_logger = Logger.getLogger(LobbyServerProperties.class.getName());
    
    private final String m_host;
    private final int m_port;
    private final String m_serverErrorMessage;

    
    
    public LobbyServerProperties(final String host, final int port, final String serverErrorMessage)
    {
        m_host = host;
        m_port = port;
        m_serverErrorMessage = serverErrorMessage;
    }

    /**
     * Read the server properties from a given url. 
     * If an error occurs during read, then ErrorMessage will be populated.
     */
    public LobbyServerProperties(URL url)
    {
        Properties props = new Properties();
        
        try
        {
            InputStream input = url.openStream();
                    
            try
            {
                props.load(input);
            } finally
            {
                input.close();
            }            
        }
        catch(IOException ioe)
        {
            s_logger.log(Level.WARNING, ioe.getMessage(), ioe );
            props.put("ERROR_MESSAGE", ioe.getMessage());
        }
                
        m_host = props.getProperty("HOST");
        m_port = Integer.parseInt(props.getProperty("PORT", "-1"));
        m_serverErrorMessage = props.getProperty("ERROR_MESSAGE", "");
    }
    
    public String getHost()
    {
        return m_host;
    }
    
    
    public int getPort()
    {
        return m_port;
    }
    
    /**
     * 
     * @return the error message for the server.
     */
    public String getServerErrorMessage()
    {
        return m_serverErrorMessage;
    }
    
    /**
     * 
     * @return if the server is available.  If the server is not available then getServerErrorMessage will give a reason.
     */
    public boolean isServerAvailable()
    {
        return m_serverErrorMessage.trim().length() <= 0;
    }
    
    
    public static void main(String[] args) throws Exception
    {
        URL url = new URL("http://triplea.sourceforge.net/lobby/server.properties");
        LobbyServerProperties props = new LobbyServerProperties(url);
        System.out.println(props.getHost());
        System.out.println(props.getPort());
        System.out.println(props.getServerErrorMessage());
    }
    
}
