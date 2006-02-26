package games.strategy.engine.random;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class IronyRemoteDiceServer implements IRemoteDiceServer
{
    
    public String getName()
    {
        return "www.irony.com";
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
        
        
        //text must be limited to 91 characters, not quite sure why
        //if we dont do this the dice roller will fail
        if (message.length() > 99)
            message = message.substring(0, 98);

        //new url http://dice.tripleawarclub.org/MARTI.php
        URL url = new URL("http://www.irony.com/cgi-bin/mroll-query");
        URLConnection urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        DataOutputStream out = new DataOutputStream(urlConn.getOutputStream());
        String content = "numdice=" + URLEncoder.encode(numDice + "", "UTF-8") + "&numsides=" + URLEncoder.encode(Integer.toString(max), "UTF-8")
                + "&modroll=" + URLEncoder.encode("No", "UTF-8") +
                //how many times to repeat
                "&numroll=" + URLEncoder.encode("1", "UTF-8") +

                "&subject=" + URLEncoder.encode(message, "UTF-8") + "&roller=" + URLEncoder.encode(player1, "UTF-8") + "&gm="
                + URLEncoder.encode(player2, "UTF-8");

        out.writeBytes(content);
        out.flush();
        out.close();

        BufferedReader input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
        try
        {
            StringBuilder results = new StringBuilder();

            while (input.ready())
            {
                results.append(input.readLine());
            }
            return results.toString();
        } finally
        {
            try
            {
                input.close();
            } catch (Exception e)
            {

            }
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
            rollStartString = "Roll 1: <b>";
            rollEndString = "</b>";
        } else
        {
            rollStartString = "<p>Roll 1:";
            rollEndString = "=";
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
