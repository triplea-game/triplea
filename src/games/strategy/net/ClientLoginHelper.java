package games.strategy.net;

import java.io.*;
import java.util.*;

class ClientLoginHelper
{

    private final IConnectionLogin m_login;
    private final SocketStreams m_streams;
    private String m_clientName;
  
    
    public ClientLoginHelper(IConnectionLogin login, SocketStreams streams, String clientName)
    {
        m_login = login;
        m_streams = streams;
        m_clientName = clientName;
    }
    
    @SuppressWarnings("unchecked")
    public boolean login()
    {
        try
        {
            ObjectOutputStream out = new ObjectOutputStream(m_streams.getBufferedOut());
            out.writeObject(m_clientName);
            //write the object output streams magic number
            out.flush();

            ObjectInputStream in = new ObjectInputStream(m_streams.getBufferedIn());
            
            
            Map challenge = (Map) in.readObject();
           
            
            //the degenerate case
            if(challenge == null)
            {
                out.writeObject(null);
                out.flush();
                return true;
            }
            
            Set<Map.Entry> entries = challenge.entrySet();
            for(Map.Entry entry : entries)
            {
                //check what we read is a string
                if(!(entry.getKey() instanceof String) && !(entry.getValue() instanceof String))
                {
                    throw new IllegalStateException("Value must be a String");
                }
            }
            
            
            if(m_login == null)
                throw new IllegalStateException("Challenged, but no login generator");
            

            Map<String,String> props =m_login.getProperties(challenge);
            
            
            out.writeObject(props);
            out.flush();
            
            String response = (String) in.readObject();
            if(response == null)
            {
                m_clientName = (String) in.readObject();
                return true;
            }
                
            
            m_login.notifyFailedLogin(response);
            return false;
            
            
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        
      
    }
    
    public String getClientName()
    {
        return m_clientName;
    }
    
}
