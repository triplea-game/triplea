/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.random;


import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * A pbem dice roller that reads its configuration from a properties file
 * 
 * 
 * @author sgb
 */
public class PropertiesDiceRoller implements IRemoteDiceServer
{
    
    private final Properties m_props;

    public PropertiesDiceRoller(Properties props)
    {
        m_props = props;
    }
    
    
    
    public String getName()
    {
        return m_props.getProperty("name");
    }
    
    public String toString()
    {
        return getName();
    }

    
    public String postRequest(String player1, String player2, int max, int numDice, String text, String gameID) throws IOException
    {
        if(gameID.trim().length() == 0)
            gameID = "TripleA";
        String message = gameID + ":" + text;
        
        int maxLength = Integer.valueOf(m_props.getProperty("message.maxlength"));
        
        if (message.length() > maxLength)
            message = message.substring(0, maxLength -1);
                
        PostMethod post = new PostMethod(m_props.getProperty("path"));
        NameValuePair[] data = {
          new NameValuePair("numdice", "" + numDice),
          new NameValuePair("numsides", "" + max),
          new NameValuePair("modroll", "No"),
          new NameValuePair("numroll", "" + 1),
          new NameValuePair("subject", message),
          new NameValuePair("roller", player1),
          new NameValuePair("gm", player2),
          new NameValuePair("send", "true"),          
        };
        
        //get around firewalls
        post.setRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
        
        post.setRequestBody(data);
       
        HttpClient client = new HttpClient();
        try
        {
            client.getHostConfiguration().setHost(m_props.getProperty("host"));
            client.executeMethod(post);
            
            String result = post.getResponseBodyAsString();
            System.out.println(post.getStatusCode());
            return result;
        }
        finally
        {
            post.releaseConnection();
        }
    }

    
   
    /**
     * 
     * @throws IOException
     *             if there was an error parsing the string
     */
    public int[] getDice(String string, int count) throws IOException
    {
        String rollStartString;
        String rollEndString;
        if (count == 1)
        {
            rollStartString =  m_props.getProperty("roll.single.start"); 
            rollEndString =  m_props.getProperty("roll.single.end"); 
        } else
        {
            
            rollStartString =  m_props.getProperty("roll.multiple.start"); 
            rollEndString =  m_props.getProperty("roll.multiple.end"); 
        }

        int startIndex = string.indexOf(rollStartString);
        if (startIndex == -1)
        {
            throw new IOException("Cound not find start index, text returned is:" + string);

        }
        startIndex += rollStartString.length();

        int endIndex = string.indexOf(rollEndString, startIndex);
        if (endIndex == -1)
        {
            throw new IOException("Cound not find end index");
        }

        StringTokenizer tokenizer = new StringTokenizer(string.substring(startIndex, endIndex), " ,", false);

        int[] rVal = new int[count];
        for (int i = 0; i < count; i++)
        {
            try
            {
                //-1 since we are 0 based
                rVal[i] = Integer.parseInt(tokenizer.nextToken()) - 1;
            } catch (NumberFormatException ex)
            {
                ex.printStackTrace();
                throw new IOException(ex.getMessage());
            }
        }

        return rVal;
    }
    
    
}

