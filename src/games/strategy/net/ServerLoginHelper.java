package games.strategy.net;

import java.io.*;
import java.net.SocketAddress;
import java.util.*;
import java.util.logging.*;

class ServerLoginHelper
{
    /**
     * The sequence of events is
     * 1) client writes user name
     * 1) server writes challenge string (or null if no challenge, at which point the communication ends)
     * 2) client writes credentials in response to challenge string
     * 3) server reads credentials, sends null and then client name if login suceeds, otherwise an error message and the connection is closed
     */
    private final static Logger s_logger = Logger.getLogger(ServerLoginHelper.class.getName());
    
    private final SocketAddress m_remoteAddress;
    private final ILoginValidator m_loginValidator;
    private final SocketStreams m_streams;
    private String m_clientName;
    private final ServerMessenger m_serverMessenger;

    
    public ServerLoginHelper(SocketAddress remoteAddress, ILoginValidator loginValidator, SocketStreams streams, ServerMessenger messenger)
    {
        m_remoteAddress = remoteAddress;
        m_loginValidator = loginValidator;
        m_streams = streams;
        m_serverMessenger = messenger;
    }
    

    @SuppressWarnings("unchecked")
    public boolean canConnect()
    {
        try
        {
            ObjectOutputStream out = new ObjectOutputStream(m_streams.getBufferedOut());
            //write the object output streams magic number
            out.flush();

            ObjectInputStream in = new ObjectInputStream(m_streams.getBufferedIn());
            m_clientName = (String) in.readObject();
            
            //the degenerate case
            if(m_loginValidator == null)
            {
                out.writeObject(null);
                out.flush();

                //cast to string to avoid toString() call on random object if
                //it isn't null
                String read = (String) in.readObject();
                if(read != null)
                    throw new IllegalArgumentException("something non null read in response to null challenge " + read);
                
                System.out.println("Server done");
                return true;
            }
            
            
            
            
            Map<String,String> challenge = m_loginValidator.getChallengeProperties(m_clientName, m_remoteAddress);
            if(challenge == null)
                throw new IllegalStateException("Challenge can't be null");
            
            out.writeObject(challenge);
            out.flush();
            
            
            Map credentials = (Map) in.readObject();
            Set<Map.Entry> entries = credentials.entrySet();
            for(Map.Entry entry : entries)
            {
                //check what we read is a string
                if(!(entry.getKey() instanceof String) && !(entry.getValue() instanceof String))
                {
                    throw new IllegalStateException("Value must be a String");
                }
            }
            
            String error = m_loginValidator.verifyConnection(challenge, (Map<String,String>) credentials, m_clientName, m_remoteAddress);
            if(error == null)
            {
                out.writeObject(null);
                
                m_clientName = m_serverMessenger.getUniqueName(m_clientName);
                out.writeObject(m_clientName);
                out.flush();
                return true;
            }

            
            out.writeObject(error);
            out.flush();

            return false;
            
            
        }
        catch(Exception e)
        {
            s_logger.log(Level.SEVERE, e.getMessage(), e);
            return false;
        }
        
        
    }
    
    public String getClientName()
    {
        return m_clientName;
    }

}
